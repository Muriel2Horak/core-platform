package cz.muriel.core.service;

import cz.muriel.core.cdc.ChangeEvent;
import cz.muriel.core.dto.UserDto;
import cz.muriel.core.entity.Tenant;
import cz.muriel.core.entities.MetamodelCrudService;
import cz.muriel.core.repository.KeycloakEventLogRepository;
import cz.muriel.core.entity.KeycloakEventLog;
import cz.muriel.core.auth.KeycloakAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

/**
 * 🔄 V6 Keycloak Event Projection Service - CDC ONLY (Metamodel-based)
 * 
 * ✅ REFACTORED: Migrated to metamodel API - uses Map<String, Object> instead of JPA entities
 * ✅ CLEAN: Používá čistě CDC data z change_events tabulky
 */
@Service @RequiredArgsConstructor @Slf4j @Transactional
public class KeycloakEventProjectionService {

  private final MetamodelCrudService metamodelService;
  private final JdbcTemplate jdbcTemplate;
  private final KeycloakEventLogRepository eventLogRepository;
  private final TenantService tenantService;
  private final KeycloakAdminService keycloakAdminService;
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

      // Find existing user via SQL
      String findSql = "SELECT * FROM users_directory WHERE keycloak_user_id = ?";
      List<Map<String, Object>> existingUsers = jdbcTemplate.queryForList(findSql, userId);
      
      if (existingUsers.isEmpty()) {
        // Try by username as fallback
        findSql = "SELECT * FROM users_directory WHERE LOWER(username) = LOWER(?)";
        existingUsers = jdbcTemplate.queryForList(findSql, username);
      }

      Map<String, Object> user;
      boolean isNew = existingUsers.isEmpty();
      
      if (isNew) {
        user = new HashMap<>();
        user.put("tenant_id", tenant.getId());
        user.put("created_at", LocalDateTime.now());
        log.debug("Creating new user: {}", username);
      } else {
        user = new HashMap<>(existingUsers.get(0));
        log.debug("Updating existing user: {}", username);
      }

      // Update user fields from Keycloak data
      user.put("keycloak_user_id", userId);
      user.put("username", username);
      user.put("email", userDto.getEmail());
      user.put("first_name", userDto.getFirstName());
      user.put("last_name", userDto.getLastName());
      user.put("active", userDto.isEnabled());
      user.put("updated_at", LocalDateTime.now());

      if (Boolean.TRUE.equals(user.get("active"))) {
        user.put("deleted_at", null); // Clear soft delete if re-enabling
      }

      // Build display name
      String displayName = buildDisplayName((String)user.get("first_name"), (String)user.get("last_name"));
      user.put("display_name", displayName);

      // ✅ Extract custom attributes from UserDto
      extractUserAttributesFromDto(user, userDto);

      // Save via metamodel
      if (isNew) {
        metamodelService.create("User", user, null);
      } else {
        metamodelService.update("User", user.get("id").toString(), 0L, user, null);
      }
      
      log.info("✅ User synced: {}", username);

    } catch (Exception e) {
      log.error("Failed to sync user from Keycloak: {}", userId, e);
    }
  }

  /**
   * ✅ Extract user attributes from UserDto to User Map
   * Obsahuje všechny atributy načtené z Keycloaku pomocí KeycloakAdminService
   */
  private void extractUserAttributesFromDto(Map<String, Object> user, UserDto userDto) {
    // 🏢 Organizační struktura
    user.put("department", userDto.getDepartment());
    user.put("position", userDto.getPosition());
    user.put("manager_username", userDto.getManager());
    user.put("cost_center", userDto.getCostCenter());
    user.put("location", userDto.getLocation());
    user.put("phone", userDto.getPhone());

    // 👥 Zástupství
    user.put("deputy_username", userDto.getDeputy());
    user.put("deputy_from", userDto.getDeputyFrom());
    user.put("deputy_to", userDto.getDeputyTo());
    user.put("deputy_reason", userDto.getDeputyReason());

    // 🔗 RESOLVE manager entity reference from username
    if (userDto.getManager() != null && !userDto.getManager().isEmpty()) {
      try {
        UUID tenantId = (UUID) user.get("tenant_id");
        String sql = "SELECT * FROM users_directory WHERE tenant_id = ? AND username = ?";
        List<Map<String, Object>> managers = jdbcTemplate.queryForList(sql, tenantId, userDto.getManager());
        
        if (!managers.isEmpty()) {
          Map<String, Object> manager = managers.get(0);
          user.put("manager_id", manager.get("id"));
          log.debug("✅ Manager entity resolved: {} -> {}", userDto.getManager(), manager.get("id"));
        } else {
          user.put("manager_id", null);
          log.warn("⚠️ Manager not found in UserDirectory: {}", userDto.getManager());
        }
      } catch (Exception e) {
        log.error("❌ Failed to resolve manager entity: {}", e.getMessage());
        user.put("manager_id", null);
      }
    } else {
      user.put("manager_id", null);
    }

    log.debug("Extracted custom attributes for user: {}", user.get("username"));
  }

  /**
   * ✅ Extract custom user attributes from Keycloak (JsonNode version)
   * @deprecated Use extractUserAttributesFromDto() instead
   */
  @Deprecated
  @SuppressWarnings("unused")
  private void extractUserAttributes(Map<String, Object> user, JsonNode attributes) {
    // Helper to extract first value from array attributes
    java.util.function.Function<String, String> getFirst = key -> {
      JsonNode node = attributes.path(key);
      if (node.isArray() && node.size() > 0) {
        return node.get(0).asText(null);
      }
      return null;
    };

    user.put("phone_number", getFirst.apply("phoneNumber"));
    user.put("department", getFirst.apply("department"));
    user.put("title", getFirst.apply("title"));
    user.put("position", getFirst.apply("position"));
    user.put("manager_username", getFirst.apply("manager"));
    user.put("cost_center", getFirst.apply("costCenter"));
    user.put("location", getFirst.apply("location"));
    user.put("phone", getFirst.apply("phone"));
    user.put("deputy_username", getFirst.apply("deputy"));
    user.put("deputy_reason", getFirst.apply("deputyReason"));

    // Date parsing
    String deputyFromStr = getFirst.apply("deputyFrom");
    if (deputyFromStr != null) {
      try {
        user.put("deputy_from", LocalDate.parse(deputyFromStr));
      } catch (Exception e) {
        log.warn("Failed to parse deputyFrom: {}", deputyFromStr);
      }
    }

    String deputyToStr = getFirst.apply("deputyTo");
    if (deputyToStr != null) {
      try {
        user.put("deputy_to", LocalDate.parse(deputyToStr));
      } catch (Exception e) {
        log.warn("Failed to parse deputyTo: {}", deputyToStr);
      }
    }
  }

  private void softDeleteUser(String userId, Tenant tenant) {
    String sql = "SELECT * FROM users_directory WHERE keycloak_user_id = ?";
    List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, userId);

    if (!users.isEmpty()) {
      Map<String, Object> user = new HashMap<>(users.get(0));
      user.put("active", false);
      user.put("deleted_at", LocalDateTime.now());
      user.put("updated_at", LocalDateTime.now());

      metamodelService.update("User", user.get("id").toString(), 0L, user, null);
      log.info("✅ User soft deleted: {}", user.get("username"));
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

      // Find existing role
      String sql = "SELECT * FROM roles WHERE keycloak_role_id = ? AND tenant_id = ?";
      List<Map<String, Object>> existing = jdbcTemplate.queryForList(sql, roleId, tenant.getId());

      Map<String, Object> role = existing.isEmpty() ? new HashMap<>() : new HashMap<>(existing.get(0));
      role.put("keycloak_role_id", roleId);
      role.put("name", roleName);
      role.put("description", roleNode.path("description").asText(null));
      role.put("tenant_id", tenant.getId());
      role.put("composite", roleNode.path("composite").asBoolean(false));

      // Save via metamodel
      if (existing.isEmpty()) {
        role = metamodelService.create("Role", role, null);
      } else {
        role = metamodelService.update("Role", role.get("id").toString(), 0L, role, null);
      }
      
      log.info("✅ Role synced: {}", roleName);

      // Sync composites if needed
      if (Boolean.TRUE.equals(role.get("composite"))) {
        syncRoleComposites(role, roleId, tenant);
      }

    } catch (Exception e) {
      log.error("Failed to sync role: {}", roleId, e);
    }
  }

  private void syncRoleComposites(Map<String, Object> parentRole, String roleId, Tenant tenant) {
    try {
      JsonNode composites = keycloakAdminService.getRoleComposites(roleId);

      if (composites == null || !composites.isArray() || composites.isEmpty()) {
        return;
      }

      // Clear existing composite mappings (junction table)
      UUID parentRoleId = (UUID) parentRole.get("id");
      String deleteSql = "DELETE FROM role_composites WHERE parent_role_id = ?";
      jdbcTemplate.update(deleteSql, parentRoleId);

      for (JsonNode compositeNode : composites) {
        String compositeKeycloakId = compositeNode.path("id").asText();
        String compositeName = compositeNode.path("name").asText();

        // Find or create composite role
        String findSql = "SELECT * FROM roles WHERE keycloak_role_id = ? AND tenant_id = ?";
        List<Map<String, Object>> existingComposites = jdbcTemplate.queryForList(findSql, compositeKeycloakId, tenant.getId());

        Map<String, Object> compositeRole;
        if (existingComposites.isEmpty()) {
          // Create new composite role
          compositeRole = new HashMap<>();
          compositeRole.put("keycloak_role_id", compositeKeycloakId);
          compositeRole.put("name", compositeName);
          compositeRole.put("description", compositeNode.path("description").asText(null));
          compositeRole.put("tenant_id", tenant.getId());
          compositeRole.put("composite", compositeNode.path("composite").asBoolean(false));
          
          compositeRole = metamodelService.create("Role", compositeRole, null);
        } else {
          compositeRole = existingComposites.get(0);
        }

        // Add to junction table
        String insertSql = "INSERT INTO role_composites (parent_role_id, child_role_id) VALUES (?, ?)";
        jdbcTemplate.update(insertSql, parentRoleId, compositeRole.get("id"));
      }

      log.info("✅ Synced {} composite roles for: {}", composites.size(), parentRole.get("name"));

    } catch (Exception e) {
      log.error("Failed to sync composite roles: {}", parentRole.get("name"), e);
    }
  }

  private void deleteRoleById(String roleId, Tenant tenant) {
    String sql = "SELECT * FROM roles WHERE keycloak_role_id = ? AND tenant_id = ?";
    List<Map<String, Object>> roles = jdbcTemplate.queryForList(sql, roleId, tenant.getId());

    if (!roles.isEmpty()) {
      Map<String, Object> role = roles.get(0);
      metamodelService.delete("Role", role.get("id").toString(), null);
      log.info("✅ Role deleted: {}", role.get("name"));
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

      // Find existing group
      String sql = "SELECT * FROM groups WHERE keycloak_group_id = ? AND tenant_id = ?";
      List<Map<String, Object>> existing = jdbcTemplate.queryForList(sql, groupId, tenant.getId());

      Map<String, Object> group = existing.isEmpty() ? new HashMap<>() : new HashMap<>(existing.get(0));
      group.put("keycloak_group_id", groupId);
      group.put("name", groupName);
      group.put("path", groupPath);
      group.put("tenant_id", tenant.getId());

      // Determine parent from path
      if (groupPath.lastIndexOf("/") > 0) {
        String parentPath = groupPath.substring(0, groupPath.lastIndexOf("/"));
        String findParentSql = "SELECT * FROM groups WHERE path = ? AND tenant_id = ?";
        List<Map<String, Object>> parents = jdbcTemplate.queryForList(findParentSql, parentPath, tenant.getId());
        
        if (!parents.isEmpty()) {
          group.put("parent_group_id", parents.get(0).get("id"));
        } else {
          group.put("parent_group_id", null);
        }
      } else {
        group.put("parent_group_id", null);
      }

      // Save via metamodel
      if (existing.isEmpty()) {
        group = metamodelService.create("Group", group, null);
      } else {
        group = metamodelService.update("Group", group.get("id").toString(), 0L, group, null);
      }
      
      log.info("✅ Group synced: {} (path: {})", groupName, groupPath);

      syncGroupChildren(group, groupId, tenant);

    } catch (Exception e) {
      log.error("Failed to sync group: {}", groupId, e);
    }
  }

  private void syncGroupChildren(Map<String, Object> parentGroup, String groupId, Tenant tenant) {
    try {
      JsonNode children = keycloakAdminService.getGroupChildren(groupId);

      if (children == null || !children.isArray() || children.isEmpty()) {
        return;
      }

      for (JsonNode childNode : children) {
        String childId = childNode.path("id").asText();
        String childName = childNode.path("name").asText();
        String childPath = childNode.path("path").asText();

        // Find or create child group
        String sql = "SELECT * FROM groups WHERE keycloak_group_id = ? AND tenant_id = ?";
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(sql, childId, tenant.getId());

        Map<String, Object> child = existing.isEmpty() ? new HashMap<>() : new HashMap<>(existing.get(0));
        child.put("keycloak_group_id", childId);
        child.put("name", childName);
        child.put("path", childPath);
        child.put("tenant_id", tenant.getId());
        child.put("parent_group_id", parentGroup.get("id"));

        // Save via metamodel
        if (existing.isEmpty()) {
          child = metamodelService.create("Group", child, null);
        } else {
          child = metamodelService.update("Group", child.get("id").toString(), 0L, child, null);
        }
        
        log.debug("✅ Synced child group: {}", childName);

        // Recursively sync children
        syncGroupChildren(child, childId, tenant);
      }

    } catch (Exception e) {
      log.error("Failed to sync child groups: {}", parentGroup.get("name"), e);
    }
  }

  private void deleteGroupById(String groupId, Tenant tenant) {
    String sql = "SELECT * FROM groups WHERE keycloak_group_id = ? AND tenant_id = ?";
    List<Map<String, Object>> groups = jdbcTemplate.queryForList(sql, groupId, tenant.getId());

    if (!groups.isEmpty()) {
      Map<String, Object> group = groups.get(0);
      metamodelService.delete("Group", group.get("id").toString(), null);
      log.info("✅ Group deleted: {}", group.get("name"));
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

    // Convert tenant key to ID
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);

    // Najdi nebo vytvoř User map
    String sql = "SELECT * FROM users_directory WHERE tenant_id = ? AND keycloak_user_id = ?";
    List<Map<String, Object>> existing = jdbcTemplate.queryForList(sql, tenantId, keycloakUserId);

    Map<String, Object> user;
    boolean isNew = existing.isEmpty();
    
    if (isNew) {
      user = new HashMap<>();
      user.put("keycloak_user_id", keycloakUserId);
      user.put("tenant_id", tenantId);
    } else {
      user = new HashMap<>(existing.get(0));
    }

    // Aktualizuj základní data z CDC
    String username = event.getFieldValue("username");
    String email = event.getFieldValue("email");
    String firstName = event.getFieldValue("first_name");
    String lastName = event.getFieldValue("last_name");

    if (username != null) user.put("username", username);
    if (email != null) user.put("email", email);
    if (firstName != null) user.put("first_name", firstName);
    if (lastName != null) user.put("last_name", lastName);

    // Ulož via metamodel
    if (isNew) {
      metamodelService.create("User", user, null);
    } else {
      metamodelService.update("User", user.get("id").toString(), 0L, user, null);
    }

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

    // Convert tenant key to ID
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);

    // Find and delete user
    String sql = "SELECT * FROM users_directory WHERE tenant_id = ? AND keycloak_user_id = ?";
    List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, tenantId, keycloakUserId);
    
    if (!users.isEmpty()) {
      Map<String, Object> user = users.get(0);
      metamodelService.delete("User", user.get("id").toString(), null);
      log.info("✅ User projection deleted: userId={}, tenant={}", keycloakUserId, tenantKey);
    }
  }
}
