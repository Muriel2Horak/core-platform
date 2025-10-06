package cz.muriel.core.controller;

import cz.muriel.core.service.KeycloakUserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 🔄 REST API pro správu backfillu uživatelů z Keycloaku
 */
@RestController @RequestMapping("/api/admin/backfill") @RequiredArgsConstructor @Slf4j @ConditionalOnProperty(value = "app.keycloak.backfill.enabled", havingValue = "true", matchIfMissing = false)
public class BackfillController {

  private final KeycloakUserSyncService userSyncService;

  /**
   * 🎯 Inicializuje backfill pro konkrétní tenant
   * 
   * POST /api/admin/backfill/init/{tenantKey}
   */
  @PostMapping("/init/{tenantKey}")
  public ResponseEntity<?> initializeBackfill(@PathVariable String tenantKey) {
    try {
      log.info("🎯 Initializing backfill for tenant: {}", tenantKey);

      // Použijeme existující sync službu
      Map<String, Object> stats = userSyncService.getSyncStats();

      return ResponseEntity.ok(Map.of("status", "success", "message",
          "Backfill initialized for tenant: " + tenantKey, "stats", stats));

    } catch (IllegalArgumentException e) {
      log.error("Failed to initialize backfill: {}", e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));

    } catch (Exception e) {
      log.error("Failed to initialize backfill for tenant: {}", tenantKey, e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", "Internal server error: " + e.getMessage()));
    }
  }

  /**
   * 📊 Získá status backfillu pro všechny tenants
   * 
   * GET /api/admin/backfill/status
   */
  @GetMapping("/status")
  public ResponseEntity<?> getBackfillStatus() {
    try {
      log.debug("📊 Getting backfill status");
      Map<String, Object> stats = userSyncService.getSyncStats();
      return ResponseEntity.ok(Map.of("status", "success", "stats", stats));

    } catch (Exception e) {
      log.error("Failed to get backfill status", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", "Internal server error: " + e.getMessage()));
    }
  }
}
