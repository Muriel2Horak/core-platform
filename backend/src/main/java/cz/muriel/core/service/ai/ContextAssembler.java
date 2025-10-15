package cz.muriel.core.service.ai;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metamodel.schema.ai.AiVisibilityMode;
import cz.muriel.core.metamodel.schema.ai.GlobalAiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Context Assembler for AI agents
 * 
 * Assembles complete AI context (UI + Workflow + Auth) for given route.
 * Enforces META_ONLY policy - no actual data values.
 * 
 * @since 2025-10-14
 */
@Slf4j @Service @RequiredArgsConstructor
public class ContextAssembler {

  private final MetamodelRegistry metamodelRegistry;
  private final UiContextService uiContextService;
  private final WfContextService wfContextService;
  private final GlobalMetamodelConfig globalConfig;

  /**
   * Assemble full AI context for route
   * 
   * @param routeId Route identifier
   * @param tenantId Tenant ID
   * @return Complete AI context (META_ONLY)
   */
  public Map<String, Object> assembleContext(String routeId, UUID tenantId) {
    String correlationId = UUID.randomUUID().toString();

    log.info("🤖 Assembling AI context: route={}, tenant={}, correlation={}", routeId, tenantId,
        correlationId);

    // Check if AI is enabled
    GlobalAiConfig aiConfig = globalConfig.getAi();
    if (aiConfig == null || !Boolean.TRUE.equals(aiConfig.getEnabled())) {
      throw new IllegalStateException("AI is disabled");
    }

    // Enforce META_ONLY mode
    AiVisibilityMode mode = aiConfig.getMode();
    if (mode == null) {
      mode = AiVisibilityMode.META_ONLY;
    }

    if (mode != AiVisibilityMode.META_ONLY) {
      log.warn("⚠️ AI mode {} requested, but forcing META_ONLY for safety", mode);
      mode = AiVisibilityMode.META_ONLY;
    }

    Map<String, Object> context = new LinkedHashMap<>();

    // UI context
    try {
      Map<String, Object> uiContext = uiContextService.getCurrentView(routeId);
      context.put("screen", uiContext.get("screen"));
      context.put("fields", uiContext.get("fields"));
      context.put("validations", uiContext.get("validations"));
    } catch (Exception e) {
      log.error("Failed to get UI context for route: {}", routeId, e);
      context.put("screen", Map.of("error", "UI context unavailable"));
    }

    // Workflow context
    try {
      Map<String, Object> wfContext = wfContextService.getWorkflowForRoute(routeId);
      context.put("workflow", wfContext);
    } catch (Exception e) {
      log.debug("No workflow context for route: {}", routeId);
      // Workflow is optional, don't fail
    }

    // Auth context
    Map<String, Object> authContext = buildAuthContext();
    context.put("auth", authContext);

    // Metadata
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("tenantId", tenantId);
    metadata.put("userId", getCurrentUserId());
    metadata.put("correlationId", correlationId);
    metadata.put("timestamp", System.currentTimeMillis());

    Map<String, Object> policy = new LinkedHashMap<>();
    policy.put("visibility", mode.name());
    policy.put("maxFields",
        aiConfig.getPolicies() != null ? aiConfig.getPolicies().getMaxFields() : 30);
    policy.put("maxRecords",
        aiConfig.getPolicies() != null ? aiConfig.getPolicies().getMaxRecords() : 20);
    metadata.put("policy", policy);

    context.put("metadata", metadata);

    log.info("✅ AI context assembled: route={}, correlation={}", routeId, correlationId);

    return context;
  }

  /**
   * Build auth context (capabilities only, no sensitive data)
   */
  private Map<String, Object> buildAuthContext() {
    Map<String, Object> authContext = new LinkedHashMap<>();

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      authContext.put("authenticated", true);
      // Don't include actual authorities - just a flag
      authContext.put("hasPermissions", !auth.getAuthorities().isEmpty());
    } else {
      authContext.put("authenticated", false);
    }

    // Placeholder for actual capabilities
    // Will be implemented when we integrate with permission system
    authContext.put("canView", true);
    authContext.put("canEdit", false);
    authContext.put("canExecute", Collections.emptyList());

    return authContext;
  }

  /**
   * Get current user ID (non-PII identifier)
   */
  private String getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      // Return name/principal but mask it to avoid PII
      String name = auth.getName();
      if (name != null && name.length() > 8) {
        return name.substring(0, 4) + "****" + name.substring(name.length() - 4);
      }
      return "user****";
    }
    return "anonymous";
  }
}
