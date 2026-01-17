package cz.muriel.core.controller.ai;

import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metrics.AiMetricsCollector;
import cz.muriel.core.service.ai.ContextAssembler;
import cz.muriel.core.service.TenantService;
import cz.muriel.core.streaming.service.WorkStateService;
import cz.muriel.core.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/**
 * AI Context Controller
 * 
 * Provides AI context endpoint for in-app agents. Returns META_ONLY context by
 * default (no actual data values).
 * 
 * @since 2025-10-14
 */
@Slf4j @RestController @RequestMapping("/api/ai") @RequiredArgsConstructor
public class AiContextController {

  private final ContextAssembler contextAssembler;
  private final GlobalMetamodelConfig globalConfig;
  private final AiMetricsCollector metricsCollector;
  private final ObjectProvider<WorkStateService> workStateServiceProvider;
  private final TenantService tenantService;

  /**
   * Get AI context for route
   * 
   * GET /api/ai/context?routeId=users.detail&tenantId=...&entity=User&entityId=...
   * 
   * Returns: - 200 OK with context (META_ONLY) - 404 if AI disabled - 423 Locked
   * if strict reads and entity updating
   * 
   * @param routeId Route identifier
   * @param tenantId Tenant ID
   * @param strict Strict reads flag (optional)
   * @param entity Entity name (optional, inferred from routeId when missing)
   * @param entityId Entity ID (optional)
   * @return AI context
   */
  @GetMapping("/context")
  public ResponseEntity<Map<String, Object>> getContext(@RequestParam String routeId,
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false, defaultValue = "false") boolean strict,
      @RequestParam(required = false) String entity,
      @RequestParam(required = false) UUID entityId) {
    log.info("📥 AI context request: route={}, tenant={}, strict={}, entity={}, entityId={}",
        routeId, tenantId, strict, entity, entityId);

    // Check if AI is enabled
    if (globalConfig.getAi() == null || !Boolean.TRUE.equals(globalConfig.getAi().getEnabled())) {
      log.warn("❌ AI is disabled");
      metricsCollector.recordAiError("AI_DISABLED");
      return ResponseEntity.status(404)
          .body(Map.of("error", "AI is disabled", "code", "AI_DISABLED"));
    }

    // Use current tenant if not specified
    if (tenantId == null) {
      tenantId = getTenantIdFromSecurityContext();
    }
    ensureTenantExists(tenantId);

    // Strict reads check: reject if entity is being updated
    if (strict) {
      if (entityId == null) {
        log.warn("⚠️ Strict reads requested but entityId missing; skipping lock check");
      } else {
        String resolvedEntity = entity != null ? entity : inferEntityFromRoute(routeId);
        WorkStateService workStateService = workStateServiceProvider.getIfAvailable();
        if (workStateService == null) {
          log.warn("⚠️ Strict reads requested but streaming is disabled; skipping lock check");
        } else {
          workStateService.enforceStrictReads(resolvedEntity, entityId);
        }
      }
    }

    try {
      Map<String, Object> context = contextAssembler.assembleContext(routeId, tenantId);

      // Record metrics
      String mode = globalConfig.getAi().getMode() != null ? globalConfig.getAi().getMode().name()
          : "META_ONLY";
      metricsCollector.recordAiRequest(tenantId != null ? tenantId.toString() : "unknown", routeId,
          mode);

      log.info("✅ AI context returned: route={}", routeId);
      return ResponseEntity.ok(context);

    } catch (IllegalStateException e) {
      log.error("❌ AI context failed: {}", e.getMessage());
      metricsCollector.recordAiError("AI_UNAVAILABLE");
      return ResponseEntity.status(503)
          .body(Map.of("error", e.getMessage(), "code", "AI_UNAVAILABLE"));

    } catch (IllegalArgumentException e) {
      log.error("❌ Invalid route: {}", e.getMessage());
      metricsCollector.recordAiError("INVALID_ROUTE");
      return ResponseEntity.status(400)
          .body(Map.of("error", e.getMessage(), "code", "INVALID_ROUTE"));

    } catch (Exception e) {
      log.error("❌ AI context error", e);
      metricsCollector.recordAiError("INTERNAL_ERROR");
      return ResponseEntity.status(500)
          .body(Map.of("error", "Internal error", "code", "INTERNAL_ERROR"));
    }
  }

  /**
   * Health check for AI service
   * 
   * GET /api/ai/health
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    boolean enabled = globalConfig.getAi() != null
        && Boolean.TRUE.equals(globalConfig.getAi().getEnabled());

    String mode = enabled && globalConfig.getAi().getMode() != null
        ? globalConfig.getAi().getMode().name()
        : "N/A";

    return ResponseEntity.ok(Map.of("status", enabled ? "enabled" : "disabled", "mode", mode,
        "timestamp", System.currentTimeMillis()));
  }

  /**
   * Extract tenant ID from JWT security context
   * 
   * @return Tenant ID from JWT claims
   * @throws ResponseStatusException if tenant ID not found or invalid
   */
  private UUID getTenantIdFromSecurityContext() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null || !auth.isAuthenticated()) {
      log.error("❌ No authentication in security context");
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
          "Authentication required - tenant ID not available");
    }

    try {
      return TenantContextHolder.getTenantId(auth).orElseThrow(() -> {
        log.error("❌ No tenant_id claim in JWT for user: {}", auth.getName());
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Tenant ID not found in security context - missing tenant_id claim");
      });
    } catch (IllegalArgumentException e) {
      log.error("❌ Invalid tenant_id format in JWT: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid tenant ID format in security context");
    }
  }

  private void ensureTenantExists(UUID tenantId) {
    if (tenantId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant ID is required");
    }
    try {
      tenantService.getTenantKeyFromId(tenantId);
    } catch (IllegalArgumentException e) {
      log.error("❌ Unknown tenant ID: {}", tenantId);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Tenant not found in registry");
    }
  }

  private String inferEntityFromRoute(String routeId) {
    String[] parts = routeId != null ? routeId.split("\\.") : new String[] {};
    if (parts.length == 0 || parts[0].isBlank()) {
      return null;
    }
    String entityName = parts[0];
    if (entityName.endsWith("s") && entityName.length() > 1) {
      entityName = entityName.substring(0, entityName.length() - 1);
    }
    return Character.toUpperCase(entityName.charAt(0)) + entityName.substring(1);
  }
}
