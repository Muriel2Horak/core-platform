package cz.muriel.core.controller.admin;

import cz.muriel.core.service.KeycloakBulkSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 🔄 Admin REST API pro bulk synchronizaci z Keycloaku
 * 
 * Umožňuje manuální synchronizaci uživatelů, rolí a skupin z Keycloak do
 * aplikace.
 */
@RestController @RequestMapping("/api/admin/keycloak-sync") @RequiredArgsConstructor @Slf4j @PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class KeycloakSyncAdminController {

  private final KeycloakBulkSyncService bulkSyncService;

  /**
   * 🔄 Spustí plnou synchronizaci všech uživatelů pro tenant
   * 
   * POST /api/admin/keycloak-sync/users/{tenantKey}
   */
  @PostMapping("/users/{tenantKey}")
  public ResponseEntity<?> syncUsersForTenant(@PathVariable String tenantKey) {
    try {
      log.info("🔄 Starting user sync for tenant: {}", tenantKey);

      // Spustí asynchronní sync
      String syncId = bulkSyncService.syncUsersAsync(tenantKey);

      return ResponseEntity.accepted().body(Map.of("status", "started", "syncId", syncId, "message",
          "User synchronization started for tenant: " + tenantKey));

    } catch (Exception e) {
      log.error("❌ Failed to start user sync for tenant: {}", tenantKey, e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  /**
   * 🔄 Spustí plnou synchronizaci všech rolí pro tenant
   * 
   * POST /api/admin/keycloak-sync/roles/{tenantKey}
   */
  @PostMapping("/roles/{tenantKey}")
  public ResponseEntity<?> syncRolesForTenant(@PathVariable String tenantKey) {
    try {
      log.info("🔄 Starting role sync for tenant: {}", tenantKey);

      String syncId = bulkSyncService.syncRolesAsync(tenantKey);

      return ResponseEntity.accepted().body(Map.of("status", "started", "syncId", syncId, "message",
          "Role synchronization started for tenant: " + tenantKey));

    } catch (Exception e) {
      log.error("❌ Failed to start role sync for tenant: {}", tenantKey, e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  /**
   * 🔄 Spustí plnou synchronizaci všech skupin pro tenant
   * 
   * POST /api/admin/keycloak-sync/groups/{tenantKey}
   */
  @PostMapping("/groups/{tenantKey}")
  public ResponseEntity<?> syncGroupsForTenant(@PathVariable String tenantKey) {
    try {
      log.info("🔄 Starting group sync for tenant: {}", tenantKey);

      String syncId = bulkSyncService.syncGroupsAsync(tenantKey);

      return ResponseEntity.accepted().body(Map.of("status", "started", "syncId", syncId, "message",
          "Group synchronization started for tenant: " + tenantKey));

    } catch (Exception e) {
      log.error("❌ Failed to start group sync for tenant: {}", tenantKey, e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  /**
   * 🔄 Spustí plnou synchronizaci všeho (uživatelé + role + skupiny)
   * 
   * POST /api/admin/keycloak-sync/all/{tenantKey}
   */
  @PostMapping("/all/{tenantKey}")
  public ResponseEntity<?> syncAllForTenant(@PathVariable String tenantKey) {
    try {
      log.info("🔄 Starting full sync for tenant: {}", tenantKey);

      String syncId = bulkSyncService.syncAllAsync(tenantKey);

      return ResponseEntity.accepted().body(Map.of("status", "started", "syncId", syncId, "message",
          "Full synchronization started for tenant: " + tenantKey));

    } catch (Exception e) {
      log.error("❌ Failed to start full sync for tenant: {}", tenantKey, e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  /**
   * 📊 Získá progress synchronizace
   * 
   * GET /api/admin/keycloak-sync/progress/{syncId}
   */
  @GetMapping("/progress/{syncId}")
  public ResponseEntity<?> getSyncProgress(@PathVariable String syncId) {
    try {
      Map<String, Object> progress = bulkSyncService.getSyncProgress(syncId);

      if (progress == null) {
        return ResponseEntity.notFound().build();
      }

      return ResponseEntity.ok(progress);

    } catch (Exception e) {
      log.error("❌ Failed to get sync progress for: {}", syncId, e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  /**
   * 📋 Získá seznam všech aktivních synchronizací
   * 
   * GET /api/admin/keycloak-sync/active
   */
  @GetMapping("/active")
  public ResponseEntity<?> getActiveSyncs() {
    try {
      return ResponseEntity.ok(bulkSyncService.getActiveSyncs());
    } catch (Exception e) {
      log.error("❌ Failed to get active syncs", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  /**
   * 📊 Získá statistiky synchronizace
   * 
   * GET /api/admin/keycloak-sync/stats
   */
  @GetMapping("/stats")
  public ResponseEntity<?> getSyncStats() {
    try {
      return ResponseEntity.ok(bulkSyncService.getSyncStats());
    } catch (Exception e) {
      log.error("❌ Failed to get sync stats", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }
}
