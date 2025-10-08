package cz.muriel.core.service;

import cz.muriel.core.entity.Tenant;
import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.repository.TenantRepository;
import cz.muriel.core.repository.UserDirectoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔄 Bulk synchronizace z Keycloak s progress tracking
 * 
 * Asynchronní synchronizace uživatelů, rolí a skupin z Keycloak do aplikace s
 * real-time progress reporting.
 */
@Service @RequiredArgsConstructor @Slf4j
public class KeycloakBulkSyncService {

  private final Keycloak keycloak;
  private final TenantRepository tenantRepository;
  private final UserDirectoryRepository userDirectoryRepository;
  private final KeycloakSyncService syncService;

  // Progress tracking pro aktivní synchronizace
  private final Map<String, SyncProgress> activeSyncs = new ConcurrentHashMap<>();

  /**
   * 🔄 Asynchronní synchronizace uživatelů
   */
  @Async
  public String syncUsersAsync(String tenantKey) {
    String syncId = UUID.randomUUID().toString();

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
          // Synchronize user to UserDirectory
          syncUserToDirectory(user, tenantKey);
          processed++;
          progress.setProcessed(processed);

        } catch (Exception e) {
          log.error("❌ Failed to sync user {}: {}", user.getUsername(), e.getMessage());
          progress.addError("User " + user.getUsername() + ": " + e.getMessage());
        }
      }

      progress.setStatus("completed");
      progress.setEndTime(LocalDateTime.now());
      log.info("✅ User sync completed for tenant {}: {}/{} users", tenantKey, processed,
          users.size());

    } catch (Exception e) {
      progress.setStatus("failed");
      progress.setEndTime(LocalDateTime.now());
      progress.addError("Sync failed: " + e.getMessage());
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
      log.info("✅ Role sync completed for tenant {}: {}/{} roles", tenantKey, processed,
          roles.size());

    } catch (Exception e) {
      progress.setStatus("failed");
      progress.setEndTime(LocalDateTime.now());
      progress.addError("Sync failed: " + e.getMessage());
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
      log.info("✅ Group sync completed for tenant {}: {}/{} groups", tenantKey, processed,
          groups.size());

    } catch (Exception e) {
      progress.setStatus("failed");
      progress.setEndTime(LocalDateTime.now());
      progress.addError("Sync failed: " + e.getMessage());
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

    SyncProgress progress = new SyncProgress(syncId, "all", tenantKey);
    activeSyncs.put(syncId, progress);
    progress.setStatus("running");

    try {
      // Sync roles first (users and groups may depend on roles)
      log.info("🔄 Step 1/3: Syncing roles for tenant: {}", tenantKey);
      String rolesSyncId = syncRolesAsync(tenantKey);
      waitForSyncCompletion(rolesSyncId, progress);

      // Sync groups
      log.info("🔄 Step 2/3: Syncing groups for tenant: {}", tenantKey);
      String groupsSyncId = syncGroupsAsync(tenantKey);
      waitForSyncCompletion(groupsSyncId, progress);

      // Sync users last
      log.info("🔄 Step 3/3: Syncing users for tenant: {}", tenantKey);
      String usersSyncId = syncUsersAsync(tenantKey);
      waitForSyncCompletion(usersSyncId, progress);

      progress.setStatus("completed");
      progress.setEndTime(LocalDateTime.now());
      log.info("✅ Full sync completed for tenant: {}", tenantKey);

    } catch (Exception e) {
      progress.setStatus("failed");
      progress.setEndTime(LocalDateTime.now());
      progress.addError("Full sync failed: " + e.getMessage());
      log.error("❌ Full sync failed for tenant: {}", tenantKey, e);
    }

    return syncId;
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
   * 🕐 Čeká na dokončení synchronizace (pro full sync)
   */
  private void waitForSyncCompletion(String syncId, SyncProgress parentProgress)
      throws InterruptedException {
    while (true) {
      SyncProgress childProgress = activeSyncs.get(syncId);
      if (childProgress == null || "completed".equals(childProgress.getStatus())
          || "failed".equals(childProgress.getStatus())) {

        if (childProgress != null && "failed".equals(childProgress.getStatus())) {
          parentProgress.addError("Sub-sync failed: " + syncId);
        }
        break;
      }
      Thread.sleep(500); // Poll every 500ms
    }
  }

  /**
   * 🔄 Synchronizuje uživatele do UserDirectory
   */
  @Transactional
  private void syncUserToDirectory(UserRepresentation user, String tenantKey) {
    Optional<UserDirectoryEntity> existing = userDirectoryRepository
        .findByTenantKeyAndKeycloakUserId(tenantKey, user.getId());

    UserDirectoryEntity entity = existing.orElse(new UserDirectoryEntity());
    entity.setKeycloakUserId(user.getId());
    entity.setTenantKey(tenantKey);
    entity.setUsername(user.getUsername());
    entity.setEmail(user.getEmail());
    entity.setFirstName(user.getFirstName());
    entity.setLastName(user.getLastName());
    // Note: enabled field might not exist on UserDirectoryEntity - check entity
    // definition

    userDirectoryRepository.save(entity);
    log.debug("✅ User synced: {} ({})", user.getUsername(), user.getId());
  }

  /**
   * 📊 Progress tracking class
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

    public void setTotal(int total) {
      this.total = total;
    }

    public void setProcessed(int processed) {
      this.processed = processed;
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
