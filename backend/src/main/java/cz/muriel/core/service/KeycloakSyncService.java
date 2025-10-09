package cz.muriel.core.service;

import cz.muriel.core.auth.KeycloakAdminService;
import cz.muriel.core.entities.MetamodelCrudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

/**
 * 🔄 V6 Metamodel-Based Keycloak Sync Service 
 * Handles synchronization of users, roles, and groups from Keycloak using metamodel API
 */
@Service @RequiredArgsConstructor @Slf4j @Transactional
public class KeycloakSyncService {

  private final MetamodelCrudService metamodelService;
  private final KeycloakAdminService keycloakAdminService;
  private final TenantService tenantService;
  private final JdbcTemplate jdbcTemplate;

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
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);

    // Find existing role using metamodel
    Map<String, Object> existingRole = findRoleByKeycloakId(keycloakRoleId, tenantId);

    Map<String, Object> roleMap = existingRole != null ? existingRole : new HashMap<>();
    
    // Set fields
    roleMap.put("keycloak_role_id", keycloakRoleId);
    roleMap.put("tenant_id", tenantId);
    roleMap.put("name", roleName);
    roleMap.put("description", roleData.path("description").asText(null));
    roleMap.put("composite", roleData.path("composite").asBoolean(false));

    // Determine role type (REALM vs CLIENT)
    if (roleData.has("clientRole") && roleData.path("clientRole").asBoolean()) {
      roleMap.put("role_type", "CLIENT");
      roleMap.put("client_id", roleData.path("containerId").asText(null));
    } else {
      roleMap.put("role_type", "REALM");
    }

    // Save via metamodel (will generate deterministic UUID if new)
    if (existingRole == null) {
      metamodelService.create("Role", roleMap, null);
    } else {
      metamodelService.update("Role", roleMap.get("id").toString(), 0L, roleMap, null);
    }
    
    log.debug("Role synchronized: {} ({})", roleName, keycloakRoleId);
  }

  /**
   * Handle role deletion
   */
  private void handleRoleDelete(String keycloakRoleId, String tenantKey) {
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);
    Map<String, Object> role = findRoleByKeycloakId(keycloakRoleId, tenantId);
    
    if (role != null) {
      metamodelService.delete("Role", role.get("id").toString(), null);
      log.debug("Role deleted: {} from tenant {}", keycloakRoleId, tenantKey);
    }
  }
  
  /**
   * Helper to find role by Keycloak ID
   */
  private Map<String, Object> findRoleByKeycloakId(String keycloakRoleId, UUID tenantId) {
    String sql = "SELECT * FROM roles WHERE keycloak_role_id = ? AND tenant_id = ?";
    List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, keycloakRoleId, tenantId);
    return results.isEmpty() ? null : results.get(0);
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
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);

    // Find existing group using metamodel
    Map<String, Object> existingGroup = findGroupByKeycloakId(keycloakGroupId, tenantId);

    Map<String, Object> groupMap = existingGroup != null ? existingGroup : new HashMap<>();
    
    // Set fields
    groupMap.put("keycloak_group_id", keycloakGroupId);
    groupMap.put("tenant_id", tenantId);
    groupMap.put("name", groupName);
    groupMap.put("path", groupPath);

    // Handle parent group relationship
    if (groupData.has("parentId") && !groupData.path("parentId").isNull()) {
      String parentKeycloakId = groupData.path("parentId").asText();
      Map<String, Object> parentGroup = findGroupByKeycloakId(parentKeycloakId, tenantId);
      if (parentGroup != null) {
        groupMap.put("parent_group_id", parentGroup.get("id"));
      }
    }

    // Save via metamodel
    if (existingGroup == null) {
      metamodelService.create("Group", groupMap, null);
    } else {
      metamodelService.update("Group", groupMap.get("id").toString(), 0L, groupMap, null);
    }
    
    log.debug("Group synchronized: {} ({})", groupName, keycloakGroupId);
  }

  /**
   * Handle group deletion
   */
  private void handleGroupDelete(String keycloakGroupId, String tenantKey) {
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);
    Map<String, Object> group = findGroupByKeycloakId(keycloakGroupId, tenantId);
    
    if (group != null) {
      metamodelService.delete("Group", group.get("id").toString(), null);
      log.debug("Group deleted: {} from tenant {}", keycloakGroupId, tenantKey);
    }
  }
  
  /**
   * Helper to find group by Keycloak ID
   */
  private Map<String, Object> findGroupByKeycloakId(String keycloakGroupId, UUID tenantId) {
    String sql = "SELECT * FROM groups WHERE keycloak_group_id = ? AND tenant_id = ?";
    List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, keycloakGroupId, tenantId);
    return results.isEmpty() ? null : results.get(0);
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
      UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);
      
      // Find user in local DB
      Map<String, Object> user = findUserByKeycloakId(keycloakUserId, tenantId);

      if (user == null) {
        log.debug("User not found in local DB, will be synced later: {}", keycloakUserId);
        return;
      }

      // Fetch current roles from Keycloak
      var keycloakRoles = keycloakAdminService.getUserRoles(keycloakUserId);

      // Clear existing role mappings
      String deleteRolesSql = "DELETE FROM user_roles WHERE user_id = ?";
      jdbcTemplate.update(deleteRolesSql, user.get("id"));

      // Add current roles
      for (String roleName : keycloakRoles) {
        Map<String, Object> role = findRoleByName(roleName, tenantId);
        if (role != null) {
          String insertSql = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";
          jdbcTemplate.update(insertSql, user.get("id"), role.get("id"));
        }
      }

      log.debug("User roles synchronized: {} roles for user {}", keycloakRoles.size(), keycloakUserId);

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
      UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);
      
      // Find user in local DB
      Map<String, Object> user = findUserByKeycloakId(keycloakUserId, tenantId);

      if (user == null) {
        log.debug("User not found in local DB, will be synced later: {}", keycloakUserId);
        return;
      }

      // Fetch current groups from Keycloak
      var keycloakGroups = keycloakAdminService.getUserGroups(keycloakUserId);

      // Clear existing group memberships
      String deleteGroupsSql = "DELETE FROM user_groups WHERE user_id = ?";
      jdbcTemplate.update(deleteGroupsSql, user.get("id"));

      // Add current groups
      for (JsonNode groupNode : keycloakGroups) {
        String groupId = groupNode.path("id").asText();
        Map<String, Object> group = findGroupByKeycloakId(groupId, tenantId);
        
        if (group != null) {
          String insertSql = "INSERT INTO user_groups (user_id, group_id) VALUES (?, ?)";
          jdbcTemplate.update(insertSql, user.get("id"), group.get("id"));
        }
      }

      log.debug("User groups synchronized: {} groups for user {}", keycloakGroups.size(), keycloakUserId);

    } catch (Exception e) {
      log.error("Failed to sync user groups: userId={}", keycloakUserId, e);
    }
  }
  
  /**
   * Helper to find user by Keycloak ID
   */
  private Map<String, Object> findUserByKeycloakId(String keycloakUserId, UUID tenantId) {
    String sql = "SELECT * FROM users_directory WHERE keycloak_user_id = ? AND tenant_id = ?";
    List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, keycloakUserId, tenantId);
    return results.isEmpty() ? null : results.get(0);
  }
  
  /**
   * Helper to find role by name
   */
  private Map<String, Object> findRoleByName(String roleName, UUID tenantId) {
    String sql = "SELECT * FROM roles WHERE name = ? AND tenant_id = ?";
    List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, roleName, tenantId);
    return results.isEmpty() ? null : results.get(0);
  }
}
