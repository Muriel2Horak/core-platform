package cz.muriel.core.controller.admin;

import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metamodel.schema.ai.GlobalAiConfig;
import cz.muriel.core.metamodel.schema.ai.AiVisibilityMode;
import cz.muriel.core.service.YamlPersistenceService;
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
  private final YamlPersistenceService yamlPersistenceService;

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
   * Process:
   * 1. Validate config (enforce META_ONLY mode)
   * 2. Update in-memory config
   * 3. Persist to global-config.yaml
   * 4. Hot reload will be triggered in next phase
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

      // Update in-memory config
      globalConfig.setAi(aiConfig);

      // Persist to YAML
      try {
        yamlPersistenceService.persistGlobalConfig(globalConfig);
        log.info("✅ AI config persisted to YAML successfully");
      } catch (Exception e) {
        log.error("❌ Failed to persist AI config to YAML", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Failed to persist config: " + e.getMessage()));
      }

      // TODO (next phase): Trigger hot reload
      // TODO (next phase): Publish config change event to Kafka

      log.info("✅ AI config updated and persisted: enabled={}, mode={}", 
          aiConfig.getEnabled(), aiConfig.getMode());
      
      return ResponseEntity.ok(Map.of(
          "status", "success", 
          "message", "AI config updated and persisted successfully",
          "note", "Hot reload will be triggered in next phase"));

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
