package cz.muriel.core.controller.admin;

import cz.muriel.core.metamodel.MetamodelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin Metamodel Controller
 * 
 * Provides metamodel management endpoints:
 * - Hot reload metamodel from YAML files
 * - Get metamodel status
 * 
 * RBAC: PLATFORM_ADMIN, OPS only
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/metamodel")
@RequiredArgsConstructor
public class AdminMetamodelController {

  private final MetamodelRegistry metamodelRegistry;

  /**
   * Hot reload metamodel from YAML files
   * 
   * Accessible by: PLATFORM_ADMIN, OPS only
   * 
   * Use cases:
   * - After AI config change in global-config.yaml
   * - After manual metamodel schema changes
   * - For testing/debugging
   */
  @PostMapping("/reload")
  @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS')")
  public ResponseEntity<Map<String, Object>> reloadMetamodel() {
    log.info("🔄 POST /api/admin/metamodel/reload - Hot reloading metamodel...");

    try {
      // Trigger reload
      long startTime = System.currentTimeMillis();
      metamodelRegistry.reload();
      long duration = System.currentTimeMillis() - startTime;

      // Get schema count
      int schemaCount = metamodelRegistry.getAllSchemas().size();

      log.info("✅ Metamodel reloaded successfully: {} schemas in {}ms", schemaCount, duration);

      return ResponseEntity.ok(Map.of(
          "status", "success",
          "message", "Metamodel reloaded successfully",
          "schemaCount", schemaCount,
          "durationMs", duration));

    } catch (Exception e) {
      log.error("❌ Failed to reload metamodel", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of(
              "status", "error",
              "message", "Failed to reload metamodel: " + e.getMessage()));
    }
  }

  /**
   * Get metamodel status
   * 
   * Accessible by: any authenticated user
   */
  @GetMapping("/status")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> getMetamodelStatus() {
    log.debug("🔍 GET /api/admin/metamodel/status - Checking metamodel status");

    try {
      int schemaCount = metamodelRegistry.getAllSchemas().size();

      return ResponseEntity.ok(Map.of(
          "status", "loaded",
          "schemaCount", schemaCount,
          "schemas", metamodelRegistry.getAllSchemas().keySet()));

    } catch (Exception e) {
      log.error("❌ Failed to get metamodel status", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }
}
