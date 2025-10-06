package cz.muriel.core.service;

import cz.muriel.core.entity.RoleEntity;
import cz.muriel.core.entity.GroupEntity;
import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.repository.RoleRepository;
import cz.muriel.core.repository.GroupRepository;
import cz.muriel.core.repository.UserDirectoryRepository;
import cz.muriel.core.auth.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * 🔄 V5 Enhanced Keycloak Sync Service Handles synchronization of users, roles,
 * and groups from Keycloak
 */
@Service @RequiredArgsConstructor @Slf4j @Transactional
public class KeycloakSyncService {

  private final RoleRepository roleRepository;
  private final GroupRepository groupRepository;
  private final UserDirectoryRepository userDirectoryRepository;
  private final KeycloakAdminService keycloakAdminService;

  // =====================================================
  // 🎭 ROLE SYNCHRONIZATION
  // =====================================================

  /**
   * Synchronize role from Keycloak Called when role_changed notification is
   * received
   */
  public void syncRoleFromKeycloak(String keycloakRoleId, String tenantKey, String operation) {
    log.debug("Syncing role: id={}, tenant={}, operation={}", keycloakRoleId, tenantKey, operation);

    if ("DELETE".equals(operation)) {
      handleRoleDelete(keycloakRoleId, tenantKey);
      return;
    }

    try {
      // Fetch role details from Keycloak
      JsonNode roleData = keycloakAdminService.getRoleById(keycloakRoleId);

      if (roleData == null) {
        log.warn("Role not found in Keycloak: {}", keycloakRoleId);
        return;
      }

      upsertRole(roleData, tenantKey);

    } catch (Exception e) {
      log.error("Failed to sync role from Keycloak: roleId={}", keycloakRoleId, e);
    }
  }

  /**
   * Upsert role entity from Keycloak data
   */
  private void upsertRole(JsonNode roleData, String tenantKey) {
    String keycloakRoleId = roleData.path("id").asText();
    String roleName = roleData.path("name").asText();

    Optional<RoleEntity> existingRole = roleRepository
        .findByKeycloakRoleIdAndTenantKey(keycloakRoleId, tenantKey);

    RoleEntity role = existingRole.orElse(new RoleEntity());
    role.setKeycloakRoleId(keycloakRoleId);
    role.setTenantKey(tenantKey);
    role.setName(roleName);
    role.setDescription(roleData.path("description").asText(null));
    role.setComposite(roleData.path("composite").asBoolean(false));

    // Determine role type (REALM vs CLIENT)
    if (roleData.has("clientRole") && roleData.path("clientRole").asBoolean()) {
      role.setRoleType(RoleEntity.RoleType.CLIENT);
      role.setClientId(roleData.path("containerId").asText(null));
    } else {
      role.setRoleType(RoleEntity.RoleType.REALM);
    }

    roleRepository.save(role);
    log.debug("Role synchronized: {} ({})", roleName, keycloakRoleId);
  }

  /**
   * Handle role deletion
   */
  private void handleRoleDelete(String keycloakRoleId, String tenantKey) {
    roleRepository.deleteByKeycloakRoleIdAndTenantKey(keycloakRoleId, tenantKey);
    log.debug("Role deleted: {} from tenant {}", keycloakRoleId, tenantKey);
  }

  // =====================================================
  // 📁 GROUP SYNCHRONIZATION
  // =====================================================

  /**
   * Synchronize group from Keycloak Called when group_changed notification is
   * received
   */
  public void syncGroupFromKeycloak(String keycloakGroupId, String tenantKey, String operation) {
    log.debug("Syncing group: id={}, tenant={}, operation={}", keycloakGroupId, tenantKey,
        operation);

    if ("DELETE".equals(operation)) {
      handleGroupDelete(keycloakGroupId, tenantKey);
      return;
    }

    try {
      // Fetch group details from Keycloak
      JsonNode groupData = keycloakAdminService.getGroupById(keycloakGroupId);

      if (groupData == null) {
        log.warn("Group not found in Keycloak: {}", keycloakGroupId);
        return;
      }

      upsertGroup(groupData, tenantKey);

    } catch (Exception e) {
      log.error("Failed to sync group from Keycloak: groupId={}", keycloakGroupId, e);
    }
  }

  /**
   * Upsert group entity from Keycloak data
   */
  private void upsertGroup(JsonNode groupData, String tenantKey) {
    String keycloakGroupId = groupData.path("id").asText();
    String groupName = groupData.path("name").asText();
    String groupPath = groupData.path("path").asText();

    Optional<GroupEntity> existingGroup = groupRepository
        .findByKeycloakGroupIdAndTenantKey(keycloakGroupId, tenantKey);

    GroupEntity group = existingGroup.orElse(new GroupEntity());
    group.setKeycloakGroupId(keycloakGroupId);
    group.setTenantKey(tenantKey);
    group.setName(groupName);
    group.setPath(groupPath);

    // Handle parent group relationship
    if (groupData.has("parentId") && !groupData.path("parentId").isNull()) {
      String parentKeycloakId = groupData.path("parentId").asText();
      Optional<GroupEntity> parentGroup = groupRepository
          .findByKeycloakGroupIdAndTenantKey(parentKeycloakId, tenantKey);
      parentGroup.ifPresent(group::setParentGroup);
    }

    groupRepository.save(group);
    log.debug("Group synchronized: {} ({})", groupName, keycloakGroupId);
  }

  /**
   * Handle group deletion
   */
  private void handleGroupDelete(String keycloakGroupId, String tenantKey) {
    groupRepository.deleteByKeycloakGroupIdAndTenantKey(keycloakGroupId, tenantKey);
    log.debug("Group deleted: {} from tenant {}", keycloakGroupId, tenantKey);
  }

  // =====================================================
  // 🔗 USER-ROLE MAPPING SYNCHRONIZATION
  // =====================================================

  /**
   * Synchronize user's role assignments Called when user_role_changed
   * notification is received
   */
  public void syncUserRoles(String keycloakUserId, String tenantKey) {
    log.debug("Syncing user roles: userId={}, tenant={}", keycloakUserId, tenantKey);

    try {
      // Find user in local DB
      Optional<UserDirectoryEntity> userOpt = userDirectoryRepository
          .findByKeycloakUserId(keycloakUserId);

      if (userOpt.isEmpty()) {
        log.debug("User not found in local DB, will be synced later: {}", keycloakUserId);
        return;
      }

      UserDirectoryEntity user = userOpt.get();

      // Fetch current roles from Keycloak
      var keycloakRoles = keycloakAdminService.getUserRoles(keycloakUserId);

      // Clear existing role mappings
      user.getRoles().clear();

      // Add current roles
      for (String roleName : keycloakRoles) {
        Optional<RoleEntity> roleOpt = roleRepository.findByNameAndTenantKey(roleName, tenantKey);

        roleOpt.ifPresent(role -> user.getRoles().add(role));
      }

      userDirectoryRepository.save(user);
      log.debug("User roles synchronized: {} roles for user {}", user.getRoles().size(),
          keycloakUserId);

    } catch (Exception e) {
      log.error("Failed to sync user roles: userId={}", keycloakUserId, e);
    }
  }

  // =====================================================
  // 👥 USER-GROUP MAPPING SYNCHRONIZATION
  // =====================================================

  /**
   * Synchronize user's group memberships Called when user_group_changed
   * notification is received
   */
  public void syncUserGroups(String keycloakUserId, String tenantKey) {
    log.debug("Syncing user groups: userId={}, tenant={}", keycloakUserId, tenantKey);

    try {
      // Find user in local DB
      Optional<UserDirectoryEntity> userOpt = userDirectoryRepository
          .findByKeycloakUserId(keycloakUserId);

      if (userOpt.isEmpty()) {
        log.debug("User not found in local DB, will be synced later: {}", keycloakUserId);
        return;
      }

      UserDirectoryEntity user = userOpt.get();

      // Fetch current groups from Keycloak
      var keycloakGroups = keycloakAdminService.getUserGroups(keycloakUserId);

      // Clear existing group memberships
      user.getGroups().clear();

      // Add current groups
      for (JsonNode groupNode : keycloakGroups) {
        String groupId = groupNode.path("id").asText();
        Optional<GroupEntity> groupOpt = groupRepository.findByKeycloakGroupIdAndTenantKey(groupId,
            tenantKey);

        groupOpt.ifPresent(group -> user.getGroups().add(group));
      }

      userDirectoryRepository.save(user);
      log.debug("User groups synchronized: {} groups for user {}", user.getGroups().size(),
          keycloakUserId);

    } catch (Exception e) {
      log.error("Failed to sync user groups: userId={}", keycloakUserId, e);
    }
  }
}
