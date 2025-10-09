package cz.muriel.core.controller.admin;

import cz.muriel.core.service.GrafanaUserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 📊 Grafana Synchronization Admin Controller
 * 
 * Provides manual sync endpoints for Grafana user management
 */
@Slf4j @RestController @RequestMapping("/api/admin/grafana") @RequiredArgsConstructor
public class GrafanaSyncController {

  private final GrafanaUserSyncService grafanaUserSyncService;

  /**
   * 🔄 Manually trigger full Grafana user synchronization
   * 
   * Synchronizes ALL users with MONITORING roles to Grafana
   */
  @PostMapping("/sync-all") @PreAuthorize("hasAuthority('CORE_ROLE_ADMIN')")
  public ResponseEntity<Map<String, Object>> syncAllUsers(
      @RequestParam(required = false) String realm) {
    log.info("🔄 Manual Grafana sync triggered for realm: {}", realm != null ? realm : "admin");

    try {
      Map<String, Object> result = grafanaUserSyncService
          .syncAllMonitoringUsers(realm != null ? realm : "admin");

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("❌ Failed to sync Grafana users", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("success", false, "error", e.getMessage()));
    }
  }

  /**
   * 🔍 Check Grafana sync status
   */
  @GetMapping("/status") @PreAuthorize("hasAuthority('CORE_ROLE_ADMIN')")
  public ResponseEntity<Map<String, Object>> getSyncStatus() {
    try {
      Map<String, Object> status = grafanaUserSyncService.getSyncStatus();
      return ResponseEntity.ok(status);
    } catch (Exception e) {
      log.error("❌ Failed to get Grafana sync status", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("success", false, "error", e.getMessage()));
    }
  }
}
