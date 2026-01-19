package cz.muriel.core.service;

import cz.muriel.core.entity.SyncExecution;
import cz.muriel.core.entity.Tenant;
import cz.muriel.core.entities.MetamodelCrudService;
import cz.muriel.core.repository.SyncExecutionRepository;
import cz.muriel.core.repository.TenantRepository;
import cz.muriel.core.security.SystemAuthentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔄 Bulk synchronizace z Keycloak s progress tracking (Metamodel-based)
 * 
 * Asynchronní synchronizace uživatelů, rolí a skupin z Keycloak do aplikace s
 * real-time progress reporting.
 */
@Service @RequiredArgsConstructor @Slf4j
public class KeycloakBulkSyncService {

  private final Keycloak keycloak;
  private final TenantRepository tenantRepository;
  private final MetamodelCrudService metamodelService;
  private final JdbcTemplate jdbcTemplate;
  private final KeycloakSyncService syncService;
  private final SyncExecutionRepository syncExecutionRepository;
  private final TenantService tenantService;

  // Progress tracking pro aktivní synchronizace
  private final Map<String, SyncProgress> activeSyncs = new ConcurrentHashMap<>();

  /**
   * 🔄 Asynchronní synchronizace uživatelů
   */
  @Async
  public String syncUsersAsync(String tenantKey) {
    String syncId = UUID.randomUUID().toString();
    String initiatedBy = SecurityContextHolder.getContext().getAuthentication().getName();

    // Convert tenant key to ID
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);

    // Vytvoříme DB záznam
    SyncExecution execution = SyncExecution.builder().id(syncId).type("users").tenantId(tenantId)
        .status(SyncExecution.SyncStatus.RUNNING).initiatedBy(initiatedBy).build();
    syncExecutionRepository.save(execution);

    SyncProgress progress = new SyncProgress(syncId, "users", tenantKey);
    activeSyncs.put(syncId, progress);

    try {
      Tenant tenant = tenantRepository.findByKey(tenantKey)
          .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

      RealmResource realm = keycloak.realm(tenant.getRealm());
      List<UserRepresentation> users = realm.users().list();

      progress.setTotal(users.size());
      progress.setStatus("running");

      int processed = 0;
      for (UserRepresentation user : users) {
        try {
          // ✨ Use shared sync service (same logic as CDC sync)
          syncService.syncUserFromKeycloak(user.getId(), tenantKey, "CREATE_OR_UPDATE");
          processed++;
          progress.setProcessed(processed);

        } catch (Exception e) {
          log.error("❌ Failed to sync user {}: {}", user.getUsername(), e.getMessage());
          progress.addError("User " + user.getUsername() + ": " + e.getMessage());
        }
      }

      progress.setStatus("completed");
      progress.setEndTime(LocalDateTime.now());
      updateSyncExecution(progress);
      log.info("✅ User sync completed for tenant {}: {}/{} users", tenantKey, processed,
          users.size());

    } catch (Exception e) {
      progress.setStatus("failed");
      progress.setEndTime(LocalDateTime.now());
      progress.addError("Sync failed: " + e.getMessage());
      updateSyncExecution(progress);
      log.error("❌ User sync failed for tenant: {}", tenantKey, e);
    }

    return syncId;
  }

  /**
   * 🔄 Asynchronní synchronizace rolí
   */
  @Async
  public String syncRolesAsync(String tenantKey) {
    String syncId = UUID.randomUUID().toString();
    String initiatedBy = SecurityContextHolder.getContext().getAuthentication().getName();

    // Convert tenant key to ID
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);

    SyncExecution execution = SyncExecution.builder().id(syncId).type("roles").tenantId(tenantId)
        .status(SyncExecution.SyncStatus.RUNNING).initiatedBy(initiatedBy).build();
    syncExecutionRepository.save(execution);

    SyncProgress progress = new SyncProgress(syncId, "roles", tenantKey);
    activeSyncs.put(syncId, progress);

    try {
      Tenant tenant = tenantRepository.findByKey(tenantKey)
          .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

      RealmResource realm = keycloak.realm(tenant.getRealm());
      List<RoleRepresentation> roles = realm.roles().list();

      progress.setTotal(roles.size());
      progress.setStatus("running");

      int processed = 0;
      for (RoleRepresentation role : roles) {
        try {
          syncService.syncRoleFromKeycloak(role.getId(), tenantKey, "CREATE_OR_UPDATE");
          processed++;
          progress.setProcessed(processed);

        } catch (Exception e) {
          log.error("❌ Failed to sync role {}: {}", role.getName(), e.getMessage());
          progress.addError("Role " + role.getName() + ": " + e.getMessage());
        }
      }

      progress.setStatus("completed");
      progress.setEndTime(LocalDateTime.now());
      updateSyncExecution(progress);
      log.info("✅ Role sync completed for tenant {}: {}/{} roles", tenantKey, processed,
          roles.size());

    } catch (Exception e) {
      progress.setStatus("failed");
      progress.setEndTime(LocalDateTime.now());
      progress.addError("Sync failed: " + e.getMessage());
      updateSyncExecution(progress);
      log.error("❌ Role sync failed for tenant: {}", tenantKey, e);
    }

    return syncId;
  }

  /**
   * 🔄 Asynchronní synchronizace skupin
   */
  @Async
  public String syncGroupsAsync(String tenantKey) {
    String syncId = UUID.randomUUID().toString();
    String initiatedBy = SecurityContextHolder.getContext().getAuthentication().getName();

    // Convert tenant key to ID
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);

    SyncExecution execution = SyncExecution.builder().id(syncId).type("groups").tenantId(tenantId)
        .status(SyncExecution.SyncStatus.RUNNING).initiatedBy(initiatedBy).build();
    syncExecutionRepository.save(execution);

    SyncProgress progress = new SyncProgress(syncId, "groups", tenantKey);
    activeSyncs.put(syncId, progress);

    try {
      Tenant tenant = tenantRepository.findByKey(tenantKey)
          .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

      RealmResource realm = keycloak.realm(tenant.getRealm());
      List<GroupRepresentation> groups = realm.groups().groups();

      progress.setTotal(groups.size());
      progress.setStatus("running");

      int processed = 0;
      for (GroupRepresentation group : groups) {
        try {
          syncService.syncGroupFromKeycloak(group.getId(), tenantKey, "CREATE_OR_UPDATE");
          processed++;
          progress.setProcessed(processed);

        } catch (Exception e) {
          log.error("❌ Failed to sync group {}: {}", group.getName(), e.getMessage());
          progress.addError("Group " + group.getName() + ": " + e.getMessage());
        }
      }

      progress.setStatus("completed");
      progress.setEndTime(LocalDateTime.now());
      updateSyncExecution(progress);
      log.info("✅ Group sync completed for tenant {}: {}/{} groups", tenantKey, processed,
          groups.size());

    } catch (Exception e) {
      progress.setStatus("failed");
      progress.setEndTime(LocalDateTime.now());
      progress.addError("Sync failed: " + e.getMessage());
      updateSyncExecution(progress);
      log.error("❌ Group sync failed for tenant: {}", tenantKey, e);
    }

    return syncId;
  }

  /**
   * 🔄 Asynchronní synchronizace všeho (users + roles + groups)
   */
  @Async
  public String syncAllAsync(String tenantKey) {
    String syncId = UUID.randomUUID().toString();
    String initiatedBy = SecurityContextHolder.getContext().getAuthentication().getName();

    // Convert tenant key to ID
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);

    SyncExecution execution = SyncExecution.builder().id(syncId).type("all").tenantId(tenantId)
        .status(SyncExecution.SyncStatus.RUNNING).initiatedBy(initiatedBy).build();
    syncExecutionRepository.save(execution);

    SyncProgress progress = new SyncProgress(syncId, "all", tenantKey);
    activeSyncs.put(syncId, progress);
    progress.setStatus("running");

    try {
      // Sync roles first (users and groups may depend on roles)
      log.info("🔄 Step 1/3: Syncing roles for tenant: {}", tenantKey);
      doSyncRoles(tenantKey, progress);

      // Sync groups
      log.info("🔄 Step 2/3: Syncing groups for tenant: {}", tenantKey);
      doSyncGroups(tenantKey, progress);

      // Sync users last
      log.info("🔄 Step 3/3: Syncing users for tenant: {}", tenantKey);
      doSyncUsers(tenantKey, progress);

      progress.setStatus("completed");
      progress.setEndTime(LocalDateTime.now());
      updateSyncExecution(progress);
      log.info("✅ Full sync completed for tenant: {} (processed: {})", tenantKey,
          progress.getProcessed());

    } catch (Exception e) {
      progress.setStatus("failed");
      progress.setEndTime(LocalDateTime.now());
      progress.addError("Full sync failed: " + e.getMessage());
      updateSyncExecution(progress);
      log.error("❌ Full sync failed for tenant: {}", tenantKey, e);
    }

    return syncId;
  }

  /**
   * 🔄 Synchronní synchronizace uživatelů (pro použití v rámci full sync)
   */
  private void doSyncUsers(String tenantKey, SyncProgress progress) {
    try {
      log.info("🔄 Fetching users from Keycloak for tenant: {}", tenantKey);

      Tenant tenant = tenantRepository.findByKey(tenantKey)
          .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

      RealmResource realm = keycloak.realm(tenant.getRealm());
      List<UserRepresentation> users = realm.users().list();

      int startTotal = progress.getTotal();
      progress.setTotal(startTotal + users.size());

      for (UserRepresentation user : users) {
        // ✨ Use shared sync service (same logic as CDC sync)
        syncService.syncUserFromKeycloak(user.getId(), tenantKey, "CREATE_OR_UPDATE");
        progress.setProcessed(progress.getProcessed() + 1);
      }

      log.info("✅ Users synced for tenant: {} (count: {})", tenantKey, users.size());
    } catch (Exception e) {
      progress.addError("Users sync error: " + e.getMessage());
      throw e;
    }
  }

  /**
   * 🔄 Synchronní synchronizace rolí (pro použití v rámci full sync)
   */
  private void doSyncRoles(String tenantKey, SyncProgress progress) {
    try {
      log.info("🔄 Fetching roles from Keycloak for tenant: {}", tenantKey);

      Tenant tenant = tenantRepository.findByKey(tenantKey)
          .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

      RealmResource realm = keycloak.realm(tenant.getRealm());
      List<RoleRepresentation> roles = realm.roles().list();

      int startTotal = progress.getTotal();
      progress.setTotal(startTotal + roles.size());

      for (RoleRepresentation role : roles) {
        syncService.syncRoleFromKeycloak(role.getId(), tenantKey, "CREATE_OR_UPDATE");
        progress.setProcessed(progress.getProcessed() + 1);
      }

      log.info("✅ Roles synced for tenant: {} (count: {})", tenantKey, roles.size());
    } catch (Exception e) {
      progress.addError("Roles sync error: " + e.getMessage());
      throw e;
    }
  }

  /**
   * 🔄 Synchronní synchronizace skupin (pro použití v rámci full sync)
   */
  private void doSyncGroups(String tenantKey, SyncProgress progress) {
    try {
      log.info("🔄 Fetching groups from Keycloak for tenant: {}", tenantKey);

      Tenant tenant = tenantRepository.findByKey(tenantKey)
          .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

      RealmResource realm = keycloak.realm(tenant.getRealm());
      List<GroupRepresentation> groups = realm.groups().groups();

      int startTotal = progress.getTotal();
      progress.setTotal(startTotal + groups.size());

      for (GroupRepresentation group : groups) {
        syncService.syncGroupFromKeycloak(group.getId(), tenantKey, "CREATE_OR_UPDATE");
        progress.setProcessed(progress.getProcessed() + 1);
      }

      log.info("✅ Groups synced for tenant: {} (count: {})", tenantKey, groups.size());
    } catch (Exception e) {
      progress.addError("Groups sync error: " + e.getMessage());
      throw e;
    }
  }

  /**
   * 📊 Získá progress konkrétní synchronizace
   */
  public Map<String, Object> getSyncProgress(String syncId) {
    SyncProgress progress = activeSyncs.get(syncId);
    if (progress == null) {
      return null;
    }
    return progress.toMap();
  }

  /**
   * 📋 Získá seznam všech aktivních synchronizací
   */
  public List<Map<String, Object>> getActiveSyncs() {
    return activeSyncs.values().stream().map(SyncProgress::toMap).toList();
  }

  /**
   * 📊 Získá statistiky synchronizace
   */
  public Map<String, Object> getSyncStats() {
    long running = activeSyncs.values().stream().filter(p -> "running".equals(p.getStatus()))
        .count();

    long completed = activeSyncs.values().stream().filter(p -> "completed".equals(p.getStatus()))
        .count();

    long failed = activeSyncs.values().stream().filter(p -> "failed".equals(p.getStatus())).count();

    return Map.of("total", activeSyncs.size(), "running", running, "completed", completed, "failed",
        failed);
  }

  /**
   * Synchronizuje uživatele do UserDirectory
   */
  @Transactional
  private void syncUserToDirectory(UserRepresentation user, String tenantKey) {
    // Convert tenant key to ID
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);

    // Find existing user
    String sql = "SELECT * FROM users_directory WHERE tenant_id = ? AND keycloak_user_id = ?";
    List<Map<String, Object>> existing = jdbcTemplate.queryForList(sql, tenantId, user.getId());

    Map<String, Object> userMap = existing.isEmpty() ? new HashMap<>() : existing.get(0);
    boolean isNew = existing.isEmpty();

    // 🆔 No need to generate UUID manually - MetamodelCrudService will
    // auto-generate UUID v7
    if (isNew) {
      userMap.put("tenant_id", tenantId);
      userMap.put("created_at", LocalDateTime.now());
      userMap.put("is_federated", false); // Default: local user, not federated
      log.debug("Creating new user: {}", user.getUsername());
    }

    // Set fields from Keycloak
    userMap.put("keycloak_user_id", user.getId());
    userMap.put("username", user.getUsername());
    userMap.put("email", user.getEmail());
    userMap.put("first_name", user.getFirstName());
    userMap.put("last_name", user.getLastName());
    userMap.put("active", user.isEnabled()); // ✅ CRITICAL: Set active status
    userMap.put("updated_at", LocalDateTime.now());

    // Clear soft delete if re-enabling
    if (Boolean.TRUE.equals(userMap.get("active"))) {
      userMap.put("deleted_at", null);
    }

    // Build display name
    String displayName = buildDisplayName(user.getFirstName(), user.getLastName());
    userMap.put("display_name", displayName);

    // Save via metamodel with SystemAuthentication
    if (isNew) {
      metamodelService.create("User", userMap, new SystemAuthentication());
    } else {
      metamodelService.update("User", userMap.get("id").toString(), 0L, userMap,
          new SystemAuthentication());
    }

    log.debug("✅ User synced: {} ({})", user.getUsername(), user.getId());
  }

  /**
   * Build display name from first and last name
   */
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

  /**
   * Helper: Aktualizuj DB zaznam na zaklade progress
   */
  private void updateSyncExecution(SyncProgress progress) {
    syncExecutionRepository.findById(progress.getSyncId()).ifPresent(execution -> {
      execution.setTotalItems(progress.getTotal());
      execution.setProcessedItems(progress.getProcessed());
      execution.setErrors(new ArrayList<>(progress.errors));

      if ("completed".equals(progress.getStatus())) {
        execution.setStatus(SyncExecution.SyncStatus.COMPLETED);
        execution.setEndTime(LocalDateTime.now());
      } else if ("failed".equals(progress.getStatus())) {
        execution.setStatus(SyncExecution.SyncStatus.FAILED);
        execution.setEndTime(LocalDateTime.now());
      }

      syncExecutionRepository.save(execution);
    });
  }

  /**
   * Progress tracking class
   */
  private static class SyncProgress {
    private final String syncId;
    private final String type;
    private final String tenantKey;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status = "pending";
    private int total = 0;
    private int processed = 0;
    private final List<String> errors = new ArrayList<>();

    public SyncProgress(String syncId, String type, String tenantKey) {
      this.syncId = syncId;
      this.type = type;
      this.tenantKey = tenantKey;
      this.startTime = LocalDateTime.now();
    }

    public String getSyncId() {
      return syncId;
    }

    public void setTotal(int total) {
      this.total = total;
    }

    public int getTotal() {
      return total;
    }

    public void setProcessed(int processed) {
      this.processed = processed;
    }

    public int getProcessed() {
      return processed;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public void setEndTime(LocalDateTime endTime) {
      this.endTime = endTime;
    }

    public void addError(String error) {
      this.errors.add(error);
    }

    public String getStatus() {
      return status;
    }

    public Map<String, Object> toMap() {
      return Map.of("syncId", syncId, "type", type, "tenantKey", tenantKey, "status", status,
          "total", total, "processed", processed, "progress",
          total > 0 ? (processed * 100 / total) : 0, "startTime", startTime.toString(), "endTime",
          endTime != null ? endTime.toString() : null, "errors", errors);
    }
  }
}
