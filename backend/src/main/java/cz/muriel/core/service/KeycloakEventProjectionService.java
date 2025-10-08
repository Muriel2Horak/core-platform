package cz.muriel.core.service;

import cz.muriel.core.cdc.ChangeEvent;
import cz.muriel.core.dto.UserDto;
import cz.muriel.core.entity.Tenant;
import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.repository.UserDirectoryRepository;
import cz.muriel.core.repository.KeycloakEventLogRepository;
import cz.muriel.core.entity.KeycloakEventLog;
import cz.muriel.core.auth.KeycloakAdminService;
import cz.muriel.core.entity.RoleEntity;
import cz.muriel.core.entity.GroupEntity;
import cz.muriel.core.repository.RoleEntityRepository;
import cz.muriel.core.repository.GroupEntityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 🔄 V5 Keycloak Event Projection Service - CDC ONLY
 * 
 * ✅ CLEAN: Používá čistě CDC data z change_events tabulky ❌ ODSTRANĚNO:
 * KeycloakWebhookEventDto dependency
 */
@Service @RequiredArgsConstructor @Slf4j @Transactional
public class KeycloakEventProjectionService {

  private final UserDirectoryRepository userDirectoryRepository;
  private final KeycloakEventLogRepository eventLogRepository;
  private final TenantService tenantService;
  private final KeycloakAdminService keycloakAdminService;
  private final RoleEntityRepository roleRepository;
  private final GroupEntityRepository groupRepository;
  private final ObjectMapper objectMapper;

  /**
   * ✅ NOVÁ METODA: Process CDC event directly from change_events table
   * 
   * @param eventType - e.g. "USER_CREATED", "USER_UPDATED"
   * @param entityId - Keycloak entity ID (user_id, role_id, group_id)
   * @param realmId - Keycloak realm_id
   * @param tenantKey - Mapped tenant key
   * @param payloadJson - JSON payload with full entity data
   */
  public void processCdcEvent(String eventType, String entityId, String realmId, String tenantKey,
      String payloadJson) {
    MDC.put("tenant", tenantKey);

    try {
      // Check for idempotence
      String eventHash = calculateCdcEventHash(eventType, entityId, realmId);
      if (eventLogRepository.existsByEventHash(eventHash)) {
        log.debug("Event already processed (duplicate): {}", eventType);
        return;
      }

      // Verify tenant exists
      Optional<Tenant> tenant = tenantService.findTenantByKey(tenantKey);
      if (tenant.isEmpty()) {
        log.warn("🚫 Event rejected - tenant not found: {}", tenantKey);
        return;
      }

      log.debug("🔄 Processing CDC event: type={}, entity={}, tenant={}", eventType, entityId,
          tenantKey);

      // Parse payload
      JsonNode payload = null;
      if (payloadJson != null && !payloadJson.isEmpty()) {
        try {
          payload = objectMapper.readTree(payloadJson);
        } catch (Exception e) {
          log.warn("Failed to parse CDC payload: {}", e.getMessage());
        }
      }

      // Process based on event type
      processCdcEventByType(eventType, entityId, tenant.get(), payload);

      // Log event as processed
      KeycloakEventLog eventLog = new KeycloakEventLog();
      eventLog.setEventHash(eventHash);
      eventLog.setCreatedAt(LocalDateTime.now());
      eventLogRepository.save(eventLog);

      log.info("✅ CDC event processed: type={}, entity={}, tenant={}", eventType, entityId,
          tenantKey);

    } catch (Exception e) {
      log.error("❌ Failed to process CDC event: type={}, entity={}", eventType, entityId, e);
      throw e;
    } finally {
      MDC.remove("tenant");
    }
  }

  /**
   * ✅ Route CDC events to appropriate handlers
   */
  private void processCdcEventByType(String eventType, String entityId, Tenant tenant,
      JsonNode payload) {
    switch (eventType) {
    case "USER_CREATED", "USER_UPDATED" -> syncUserFromKeycloak(entityId, tenant);
    case "USER_DELETED" -> softDeleteUser(entityId, tenant);
    case "ROLE_CREATED", "ROLE_UPDATED" -> syncRoleFromKeycloak(entityId, tenant);
    case "ROLE_DELETED" -> deleteRoleById(entityId, tenant);
    case "GROUP_CREATED", "GROUP_UPDATED" -> syncGroupFromKeycloak(entityId, tenant);
    case "GROUP_DELETED" -> deleteGroupById(entityId, tenant);
    default -> log.debug("Ignoring CDC event type: {}", eventType);
    }
  }

  // =====================================================
  // 👤 USER SYNCHRONIZATION (CDC)
  // =====================================================

  private void syncUserFromKeycloak(String userId, Tenant tenant) {
    try {
      // Fetch full user data from Keycloak Admin API
      UserDto userDto = keycloakAdminService.getUserById(userId);
      if (userDto == null) {
        log.warn("User not found in Keycloak: {}", userId);
        return;
      }

      String username = userDto.getUsername();

      // Find existing user
      Optional<UserDirectoryEntity> existingUser = userDirectoryRepository
          .findByKeycloakUserId(userId);

      if (existingUser.isEmpty()) {
        existingUser = userDirectoryRepository.findByUsernameIgnoreCase(username);
      }

      UserDirectoryEntity user;
      if (existingUser.isPresent()) {
        user = existingUser.get();
        log.debug("Updating existing user: {}", username);
      } else {
        user = new UserDirectoryEntity();
        user.setTenantKey(tenant.getKey());
        user.setCreatedAt(LocalDateTime.now());
        log.debug("Creating new user: {}", username);
      }

      // Update user fields from Keycloak data
      user.setKeycloakUserId(userId);
      user.setUsername(username);
      user.setEmail(userDto.getEmail());
      user.setFirstName(userDto.getFirstName());
      user.setLastName(userDto.getLastName());
      user.setActive(userDto.isEnabled());
      user.setUpdatedAt(LocalDateTime.now());

      if (user.getActive()) {
        user.setDeletedAt(null); // Clear soft delete if re-enabling
      }

      // Build display name
      String displayName = buildDisplayName(user.getFirstName(), user.getLastName());
      user.setDisplayName(displayName);

      // ✅ Extract custom attributes from UserDto (již implementováno v
      // KeycloakAdminService)
      // UserDto již obsahuje všechny organizační atributy z Keycloaku
      extractUserAttributesFromDto(user, userDto);

      userDirectoryRepository.save(user);
      log.info("✅ User synced: {}", username);

    } catch (Exception e) {
      log.error("Failed to sync user from Keycloak: {}", userId, e);
    }
  }

  /**
   * ✅ Extract user attributes from UserDto to UserDirectoryEntity UserDto již
   * obsahuje všechny atributy načtené z Keycloaku pomocí KeycloakAdminService
   */
  private void extractUserAttributesFromDto(UserDirectoryEntity user, UserDto userDto) {
    // 🏢 Organizační struktura
    user.setDepartment(userDto.getDepartment());
    user.setPosition(userDto.getPosition());
    user.setManagerUsername(userDto.getManager());
    user.setCostCenter(userDto.getCostCenter());
    user.setLocation(userDto.getLocation());
    user.setPhone(userDto.getPhone());

    // 👥 Zástupství
    user.setDeputy(userDto.getDeputy());
    user.setDeputyFrom(userDto.getDeputyFrom());
    user.setDeputyTo(userDto.getDeputyTo());
    user.setDeputyReason(userDto.getDeputyReason());

    // 🔗 RESOLVE manager entity reference from username
    if (userDto.getManager() != null && !userDto.getManager().isEmpty()) {
      try {
        Optional<UserDirectoryEntity> managerEntity = userDirectoryRepository
            .findByTenantKeyAndUsername(user.getTenantKey(), userDto.getManager());
        managerEntity.ifPresentOrElse(manager -> {
          user.setManager(manager);
          log.debug("✅ Manager entity resolved: {} -> {}", userDto.getManager(), manager.getId());
        }, () -> {
          user.setManager(null);
          log.warn("⚠️ Manager not found in UserDirectory: {}", userDto.getManager());
        });
      } catch (Exception e) {
        log.error("❌ Failed to resolve manager entity: {}", e.getMessage());
        user.setManager(null);
      }
    } else {
      user.setManager(null);
    }

    log.debug("Extracted custom attributes for user: {}", user.getUsername());
  }

  /**
   * ✅ Extract custom user attributes from Keycloak (JsonNode version - for direct
   * API calls)
   * 
   * @deprecated Use extractUserAttributesFromDto() instead - UserDto already
   * contains all attributes
   */
  @Deprecated @SuppressWarnings("unused")
  private void extractUserAttributes(UserDirectoryEntity user, JsonNode attributes) {
    // Helper to extract first value from array attributes
    java.util.function.Function<String, String> getFirst = key -> {
      JsonNode node = attributes.path(key);
      if (node.isArray() && node.size() > 0) {
        return node.get(0).asText(null);
      }
      return null;
    };

    user.setPhoneNumber(getFirst.apply("phoneNumber"));
    user.setDepartment(getFirst.apply("department"));
    user.setTitle(getFirst.apply("title"));
    user.setPosition(getFirst.apply("position"));
    user.setManagerUsername(getFirst.apply("manager"));
    user.setCostCenter(getFirst.apply("costCenter"));
    user.setLocation(getFirst.apply("location"));
    user.setPhone(getFirst.apply("phone"));
    user.setDeputy(getFirst.apply("deputy"));
    user.setDeputyReason(getFirst.apply("deputyReason"));

    // Date parsing
    String deputyFromStr = getFirst.apply("deputyFrom");
    if (deputyFromStr != null) {
      try {
        user.setDeputyFrom(java.time.LocalDate.parse(deputyFromStr));
      } catch (Exception e) {
        log.warn("Failed to parse deputyFrom: {}", deputyFromStr);
      }
    }

    String deputyToStr = getFirst.apply("deputyTo");
    if (deputyToStr != null) {
      try {
        user.setDeputyTo(java.time.LocalDate.parse(deputyToStr));
      } catch (Exception e) {
        log.warn("Failed to parse deputyTo: {}", deputyToStr);
      }
    }
  }

  private void softDeleteUser(String userId, Tenant tenant) {
    Optional<UserDirectoryEntity> user = userDirectoryRepository.findByKeycloakUserId(userId);

    if (user.isPresent()) {
      UserDirectoryEntity userEntity = user.get();
      userEntity.setActive(false);
      userEntity.setDeletedAt(LocalDateTime.now());
      userEntity.setUpdatedAt(LocalDateTime.now());

      userDirectoryRepository.save(userEntity);
      log.info("✅ User soft deleted: {}", userEntity.getUsername());
    }
  }

  // =====================================================
  // 🔗 ROLE SYNCHRONIZATION (CDC)
  // =====================================================

  private void syncRoleFromKeycloak(String roleId, Tenant tenant) {
    try {
      JsonNode roleNode = keycloakAdminService.getRoleById(roleId);
      if (roleNode == null) {
        log.warn("Role not found in Keycloak: {}", roleId);
        return;
      }

      String roleName = roleNode.path("name").asText();

      Optional<RoleEntity> existingRole = roleRepository.findByKeycloakRoleIdAndTenantKey(roleId,
          tenant.getKey());

      RoleEntity role = existingRole.orElse(new RoleEntity());
      role.setKeycloakRoleId(roleId);
      role.setName(roleName);
      role.setDescription(roleNode.path("description").asText(null));
      role.setTenantKey(tenant.getKey());
      role.setComposite(roleNode.path("composite").asBoolean(false));

      role = roleRepository.save(role);
      log.info("✅ Role synced: {}", roleName);

      // Sync composites if needed
      if (Boolean.TRUE.equals(role.getComposite())) {
        syncRoleComposites(role, roleId, tenant);
      }

    } catch (Exception e) {
      log.error("Failed to sync role: {}", roleId, e);
    }
  }

  private void syncRoleComposites(RoleEntity parentRole, String roleId, Tenant tenant) {
    try {
      JsonNode composites = keycloakAdminService.getRoleComposites(roleId);

      if (composites == null || !composites.isArray() || composites.isEmpty()) {
        return;
      }

      parentRole.getChildRoles().clear();

      for (JsonNode compositeNode : composites) {
        String compositeId = compositeNode.path("id").asText();
        String compositeName = compositeNode.path("name").asText();

        Optional<RoleEntity> compositeRole = roleRepository
            .findByKeycloakRoleIdAndTenantKey(compositeId, tenant.getKey());

        if (compositeRole.isEmpty()) {
          RoleEntity newComposite = new RoleEntity();
          newComposite.setKeycloakRoleId(compositeId);
          newComposite.setName(compositeName);
          newComposite.setDescription(compositeNode.path("description").asText(null));
          newComposite.setTenantKey(tenant.getKey());
          newComposite.setComposite(compositeNode.path("composite").asBoolean(false));

          compositeRole = Optional.of(roleRepository.save(newComposite));
        }

        parentRole.addChildRole(compositeRole.get());
      }

      roleRepository.save(parentRole);
      log.info("✅ Synced {} composite roles for: {}", composites.size(), parentRole.getName());

    } catch (Exception e) {
      log.error("Failed to sync composite roles: {}", parentRole.getName(), e);
    }
  }

  private void deleteRoleById(String roleId, Tenant tenant) {
    Optional<RoleEntity> role = roleRepository.findByKeycloakRoleIdAndTenantKey(roleId,
        tenant.getKey());

    if (role.isPresent()) {
      roleRepository.delete(role.get());
      log.info("✅ Role deleted: {}", role.get().getName());
    }
  }

  // =====================================================
  // 📁 GROUP SYNCHRONIZATION (CDC)
  // =====================================================

  private void syncGroupFromKeycloak(String groupId, Tenant tenant) {
    try {
      JsonNode groupNode = keycloakAdminService.getGroupById(groupId);
      if (groupNode == null) {
        log.warn("Group not found in Keycloak: {}", groupId);
        return;
      }

      String groupName = groupNode.path("name").asText();
      String groupPath = groupNode.path("path").asText("/" + groupName);

      Optional<GroupEntity> existingGroup = groupRepository
          .findByKeycloakGroupIdAndTenantKey(groupId, tenant.getKey());

      GroupEntity group = existingGroup.orElse(new GroupEntity());
      group.setKeycloakGroupId(groupId);
      group.setName(groupName);
      group.setPath(groupPath);
      group.setTenantKey(tenant.getKey());

      // Determine parent from path
      if (groupPath.lastIndexOf("/") > 0) {
        String parentPath = groupPath.substring(0, groupPath.lastIndexOf("/"));
        Optional<GroupEntity> parent = groupRepository.findByPathAndTenantKey(parentPath,
            tenant.getKey());
        parent.ifPresent(group::setParentGroup);
      } else {
        group.setParentGroup(null);
      }

      group = groupRepository.save(group);
      log.info("✅ Group synced: {} (path: {})", groupName, groupPath);

      syncGroupChildren(group, groupId, tenant);

    } catch (Exception e) {
      log.error("Failed to sync group: {}", groupId, e);
    }
  }

  private void syncGroupChildren(GroupEntity parentGroup, String groupId, Tenant tenant) {
    try {
      JsonNode children = keycloakAdminService.getGroupChildren(groupId);

      if (children == null || !children.isArray() || children.isEmpty()) {
        return;
      }

      for (JsonNode childNode : children) {
        String childId = childNode.path("id").asText();
        String childName = childNode.path("name").asText();
        String childPath = childNode.path("path").asText();

        Optional<GroupEntity> existingChild = groupRepository
            .findByKeycloakGroupIdAndTenantKey(childId, tenant.getKey());

        GroupEntity child = existingChild.orElse(new GroupEntity());
        child.setKeycloakGroupId(childId);
        child.setName(childName);
        child.setPath(childPath);
        child.setTenantKey(tenant.getKey());
        child.setParentGroup(parentGroup);

        groupRepository.save(child);
        log.debug("✅ Synced child group: {}", childName);

        syncGroupChildren(child, childId, tenant);
      }

    } catch (Exception e) {
      log.error("Failed to sync child groups: {}", parentGroup.getName(), e);
    }
  }

  private void deleteGroupById(String groupId, Tenant tenant) {
    Optional<GroupEntity> group = groupRepository.findByKeycloakGroupIdAndTenantKey(groupId,
        tenant.getKey());

    if (group.isPresent()) {
      groupRepository.delete(group.get());
      log.info("✅ Group deleted: {}", group.get().getName());
    }
  }

  // =====================================================
  // 🔧 HELPER METHODS
  // =====================================================

  private String buildDisplayName(String firstName, String lastName) {
    if (firstName != null && lastName != null) {
      return firstName + " " + lastName;
    } else if (firstName != null) {
      return firstName;
    } else if (lastName != null) {
      return lastName;
    }
    return null;
  }

  private String calculateCdcEventHash(String eventType, String entityId, String realmId) {
    try {
      String hashInput = String.format("%s:%s:%s:%d", eventType, entityId, realmId,
          System.currentTimeMillis() / 10000);

      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(hashInput.getBytes(StandardCharsets.UTF_8));

      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }

  /**
   * ✅ NOVÁ metoda pro CDC eventy - přímé zpracování dat z databáze
   */
  public void processCdcEvent(ChangeEvent changeEvent) {
    log.debug("Processing CDC event: operation={}, table={}", changeEvent.getOperation(),
        changeEvent.getTableName());

    try {
      switch (changeEvent.getOperation()) {
      case "INSERT", "UPDATE" -> syncUserProjection(changeEvent);
      case "DELETE" -> handleUserDeletion(changeEvent);
      default -> log.warn("Unknown CDC operation: {}", changeEvent.getOperation());
      }
    } catch (Exception e) {
      log.error("Failed to process CDC event: {}", e.getMessage(), e);
    }
  }

  /**
   * ✅ Wrapper metoda pro zpracování JsonNode CDC payloadu Používá se v
   * KeycloakUserSyncService pro jednodušší integraci
   */
  public void processCdcEvent(JsonNode cdcPayload) {
    try {
      String eventType = cdcPayload.path("eventType").asText();
      String resourceType = cdcPayload.path("resourceType").asText();

      if (!"USER".equals(resourceType)) {
        log.debug("Skipping non-USER event: {}", resourceType);
        return;
      }

      String userId = cdcPayload.path("resourcePath").asText();
      if (userId.contains("/")) {
        userId = userId.substring(userId.lastIndexOf('/') + 1);
      }

      // Extrahovat tenant key z payloadu nebo použít výchozí
      String tenantKey = cdcPayload.path("tenantKey").asText("test-tenant");
      String realmId = cdcPayload.path("realmId").asText();

      log.info("📊 Processing CDC event from JsonNode: type={}, userId={}, tenant={}", eventType,
          userId, tenantKey);

      // Mapování eventType na správný formát
      String mappedEventType = switch (eventType) {
      case "CREATE" -> "USER_CREATED";
      case "UPDATE" -> "USER_UPDATED";
      case "DELETE" -> "USER_DELETED";
      default -> eventType;
      };

      // Delegovat na plnou metodu processCdcEvent
      processCdcEvent(mappedEventType, userId, realmId, tenantKey, cdcPayload.toString());

    } catch (Exception ex) {
      log.error("❌ Failed to process CDC event from JsonNode", ex);
      throw new RuntimeException("CDC event processing failed", ex);
    }
  }

  private void syncUserProjection(ChangeEvent event) {
    String keycloakUserId = event.getFieldValue("keycloak_user_id");
    String tenantKey = event.getFieldValue("tenant_key");

    if (keycloakUserId == null || tenantKey == null) {
      log.warn("Missing keycloak_user_id or tenant_key in CDC event");
      return;
    }

    log.debug("Syncing user projection: userId={}, tenant={}", keycloakUserId, tenantKey);

    // Najdi nebo vytvoř UserDirectoryEntity
    UserDirectoryEntity user = userDirectoryRepository
        .findByTenantKeyAndKeycloakUserId(tenantKey, keycloakUserId).orElseGet(() -> {
          UserDirectoryEntity newUser = new UserDirectoryEntity();
          newUser.setKeycloakUserId(keycloakUserId);
          newUser.setTenantKey(tenantKey);
          return newUser;
        });

    // Aktualizuj základní data z CDC
    String username = event.getFieldValue("username");
    String email = event.getFieldValue("email");
    String firstName = event.getFieldValue("first_name");
    String lastName = event.getFieldValue("last_name");

    if (username != null)
      user.setUsername(username);
    if (email != null)
      user.setEmail(email);
    if (firstName != null)
      user.setFirstName(firstName);
    if (lastName != null)
      user.setLastName(lastName);

    // Ulož
    userDirectoryRepository.save(user);

    log.info("✅ User projection synced: userId={}, username={}, tenant={}", keycloakUserId,
        username, tenantKey);
  }

  private void handleUserDeletion(ChangeEvent event) {
    String keycloakUserId = event.getFieldValue("keycloak_user_id");
    String tenantKey = event.getFieldValue("tenant_key");

    if (keycloakUserId == null || tenantKey == null) {
      log.warn("Missing keycloak_user_id or tenant_key in CDC deletion event");
      return;
    }

    log.debug("Handling user deletion: userId={}, tenant={}", keycloakUserId, tenantKey);

    userDirectoryRepository.findByTenantKeyAndKeycloakUserId(tenantKey, keycloakUserId)
        .ifPresent(user -> {
          userDirectoryRepository.delete(user);
          log.info("✅ User projection deleted: userId={}, tenant={}", keycloakUserId, tenantKey);
        });
  }
}
