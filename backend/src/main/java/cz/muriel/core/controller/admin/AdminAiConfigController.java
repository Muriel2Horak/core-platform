package cz.muriel.core.controller.admin;

import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metamodel.schema.ai.GlobalAiConfig;
import cz.muriel.core.metamodel.schema.ai.AiVisibilityMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin AI Configuration Controller
 * 
 * Step F: Admin nastavení - backend endpoints
 * 
 * RBAC: - GET /api/admin/ai/config: PLATFORM_ADMIN, OPS, TENANT_ADMIN
 * (read-only) - PUT /api/admin/ai/config: PLATFORM_ADMIN, OPS only
 * 
 * Features: - Get global AI configuration - Update global AI configuration -
 * Hot reload (future: trigger metamodel reload)
 */
@Slf4j @RestController @RequestMapping("/api/admin/ai") @RequiredArgsConstructor
public class AdminAiConfigController {

  private final GlobalMetamodelConfig globalConfig;

  /**
   * Get global AI configuration
   * 
   * Accessible by: PLATFORM_ADMIN, OPS, TENANT_ADMIN TenantAdmin gets read-only
   * view
   */
  @GetMapping("/config") @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS', 'TENANT_ADMIN')")
  public ResponseEntity<GlobalAiConfig> getAiConfig() {
    log.info("📖 GET /api/admin/ai/config - Fetching global AI configuration");

    try {
      GlobalAiConfig aiConfig = globalConfig.getAi();

      if (aiConfig == null) {
        // Return default config if not set
        log.warn("⚠️ No AI config found in global-config.yaml, returning defaults");
        aiConfig = new GlobalAiConfig();
        aiConfig.setEnabled(false);
        aiConfig.setMode(AiVisibilityMode.META_ONLY);
      }

      log.info("✅ AI config fetched: enabled={}, mode={}", aiConfig.getEnabled(),
          aiConfig.getMode());
      return ResponseEntity.ok(aiConfig);

    } catch (Exception e) {
      log.error("❌ Failed to fetch AI config", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Update global AI configuration
   * 
   * Accessible by: PLATFORM_ADMIN, OPS only
   * 
   * NOTE: In this version, this endpoint only validates the config. Actual
   * persistence to global-config.yaml is not implemented yet. Future versions
   * will: 1. Persist to global-config.yaml 2. Trigger hot reload of metamodel 3.
   * Publish config change event to Kafka
   */
  @PutMapping("/config") @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS')")
  public ResponseEntity<Map<String, String>> updateAiConfig(@RequestBody GlobalAiConfig aiConfig) {
    log.info("💾 PUT /api/admin/ai/config - Updating AI configuration: enabled={}, mode={}",
        aiConfig.getEnabled(), aiConfig.getMode());

    try {
      // Validate config
      if (aiConfig.getMode() != null && !aiConfig.getMode().equals(AiVisibilityMode.META_ONLY)) {
        log.warn("⚠️ Attempted to set AI mode to {}, forcing META_ONLY", aiConfig.getMode());
        aiConfig.setMode(AiVisibilityMode.META_ONLY);
      }

      // TODO: Persist to global-config.yaml
      // TODO: Trigger metamodel hot reload
      // TODO: Publish config change event

      log.info("✅ AI config validated (persistence not implemented yet)");
      return ResponseEntity.ok(Map.of("status", "validated", "message",
          "AI config validated successfully. Persistence not implemented yet.", "note",
          "Future versions will persist to global-config.yaml and trigger hot reload"));

    } catch (Exception e) {
      log.error("❌ Failed to update AI config", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get AI configuration health/status
   * 
   * Accessible by: any authenticated user
   */
  @GetMapping("/status") @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> getAiStatus() {
    log.debug("🔍 GET /api/admin/ai/status - Checking AI status");

    try {
      GlobalAiConfig aiConfig = globalConfig.getAi();

      boolean enabled = aiConfig != null && aiConfig.getEnabled() != null && aiConfig.getEnabled();
      String mode = aiConfig != null && aiConfig.getMode() != null ? aiConfig.getMode().toString()
          : "META_ONLY";

      return ResponseEntity
          .ok(Map.of("enabled", enabled, "mode", mode, "status", enabled ? "active" : "disabled"));

    } catch (Exception e) {
      log.error("❌ Failed to check AI status", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }
}
