package cz.muriel.core.controller.ai;

import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metrics.AiMetricsCollector;
import cz.muriel.core.service.ai.ContextAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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

  /**
   * Get AI context for route
   * 
   * GET /api/ai/context?routeId=users.detail&tenantId=...
   * 
   * Returns: - 200 OK with context (META_ONLY) - 404 if AI disabled - 423 Locked
   * if strict reads and entity updating
   * 
   * @param routeId Route identifier
   * @param tenantId Tenant ID
   * @param strict Strict reads flag (optional)
   * @return AI context
   */
  @GetMapping("/context")
  public ResponseEntity<Map<String, Object>> getContext(@RequestParam String routeId,
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false, defaultValue = "false") boolean strict) {
    log.info("📥 AI context request: route={}, tenant={}, strict={}", routeId, tenantId, strict);

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

    // Strict reads check: reject if entity is being updated
    if (strict) {
      // Note: Strict mode requires entityType and entityId parameters
      // Current implementation only has routeId, which doesn't contain entity ID
      // To implement: Add entityType and entityId parameters, then check:
      // if (editLockService.isLocked(tenantId, entityType, entityId)) {
      // throw new ResponseStatusException(HttpStatus.LOCKED, "Entity is being
      // edited");
      // }
      log.warn(
          "⚠️ Strict reads requested but requires entityType/entityId parameters (not implemented yet)");
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

    // Extract from JWT claims
    if (auth.getPrincipal() instanceof Jwt jwt) {
      String tenantIdStr = jwt.getClaimAsString("tenant_id");

      if (tenantIdStr == null || tenantIdStr.isBlank()) {
        log.error("❌ No tenant_id claim in JWT for user: {}", auth.getName());
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Tenant ID not found in security context - missing tenant_id claim");
      }

      try {
        UUID tenantId = UUID.fromString(tenantIdStr);
        log.debug("✅ Extracted tenant ID from JWT: {}", tenantId);
        return tenantId;
      } catch (IllegalArgumentException e) {
        log.error("❌ Invalid tenant_id format in JWT: {}", tenantIdStr);
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Invalid tenant ID format in security context");
      }
    }

    // Fallback: try to get from authentication details
    log.warn("⚠️ Authentication principal is not JWT, attempting fallback");
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
        "Unable to extract tenant ID from security context - JWT expected");
  }
}
