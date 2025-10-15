package cz.muriel.core.controller.ai;

import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metrics.AiMetricsCollector;
import cz.muriel.core.service.ai.UiContextService;
import cz.muriel.core.service.ai.WfContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * MCP (Model Context Protocol) Adapter Controller
 * 
 * Provides MCP-compliant endpoints for AI agents. All tools enforce META_ONLY
 * mode.
 * 
 * @since 2025-10-14
 */
@Slf4j @RestController @RequestMapping("/api/ai/mcp") @RequiredArgsConstructor
public class McpController {

  private final UiContextService uiContextService;
  private final WfContextService wfContextService;
  private final GlobalMetamodelConfig globalConfig;
  private final AiMetricsCollector metricsCollector;

  /**
   * MCP Tool: ui_context.get_current_view
   * 
   * POST /api/ai/mcp/ui_context/get_current_view
   * 
   * Input: { "routeId": "users.detail" } Output: { "title", "viewKind",
   * "widgets", "fields", "actions", "validations" }
   */
  @PostMapping("/ui_context/get_current_view")
  public ResponseEntity<Map<String, Object>> getUiContext(@RequestBody Map<String, Object> input) {
    String correlationId = UUID.randomUUID().toString();
    String routeId = (String) input.get("routeId");

    log.info("🔧 MCP: ui_context.get_current_view(routeId={}) [{}]", routeId, correlationId);

    // Check AI enabled
    if (!isAiEnabled()) {
      return ResponseEntity.status(404).body(Map.of("error", "AI is disabled"));
    }

    try {
      Map<String, Object> context = uiContextService.getCurrentView(routeId);

      metricsCollector.recordMcpCall("ui_context");
      log.info("✅ MCP: ui_context.get_current_view completed [{}]", correlationId);
      return ResponseEntity.ok(context);

    } catch (Exception e) {
      log.error("❌ MCP: ui_context.get_current_view failed [{}]", correlationId, e);
      metricsCollector.recordAiError("MCP_UI_CONTEXT_ERROR");
      return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * MCP Tool: wf_context.get_workflow
   * 
   * POST /api/ai/mcp/wf_context/get_workflow
   * 
   * Input: { "entity": "User" } or { "routeId": "users.detail" } Output: {
   * "states", "actions", "howto", "preconditions", "postconditions" }
   */
  @PostMapping("/wf_context/get_workflow")
  public ResponseEntity<Map<String, Object>> getWorkflow(@RequestBody Map<String, Object> input) {
    String correlationId = UUID.randomUUID().toString();
    String entity = (String) input.get("entity");
    String routeId = (String) input.get("routeId");

    log.info("🔧 MCP: wf_context.get_workflow(entity={}, routeId={}) [{}]", entity, routeId,
        correlationId);

    // Check AI enabled
    if (!isAiEnabled()) {
      return ResponseEntity.status(404).body(Map.of("error", "AI is disabled"));
    }

    try {
      Map<String, Object> workflow;
      if (entity != null) {
        workflow = wfContextService.getWorkflowForEntity(entity);
      } else if (routeId != null) {
        workflow = wfContextService.getWorkflowForRoute(routeId);
      } else {
        metricsCollector.recordAiError("MCP_INVALID_INPUT");
        return ResponseEntity.status(400).body(Map.of("error", "entity or routeId required"));
      }

      metricsCollector.recordMcpCall("wf_context");
      log.info("✅ MCP: wf_context.get_workflow completed [{}]", correlationId);
      return ResponseEntity.ok(workflow);

    } catch (Exception e) {
      log.error("❌ MCP: wf_context.get_workflow failed [{}]", correlationId, e);
      metricsCollector.recordAiError("MCP_WF_CONTEXT_ERROR");
      return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * MCP Tool: auth.get_user_capabilities
   * 
   * POST /api/ai/mcp/auth/get_user_capabilities
   * 
   * Input: { "userId": "...", "routeId": "users.detail" } Output: { "canView":
   * [], "canEdit": [], "canExecute": [] }
   */
  @PostMapping("/auth/get_user_capabilities")
  public ResponseEntity<Map<String, Object>> getUserCapabilities(
      @RequestBody Map<String, Object> input) {
    String correlationId = UUID.randomUUID().toString();
    String userId = (String) input.get("userId");
    String routeId = (String) input.get("routeId");

    log.info("🔧 MCP: auth.get_user_capabilities(userId={}, routeId={}) [{}]", userId, routeId,
        correlationId);

    // Check AI enabled
    if (!isAiEnabled()) {
      return ResponseEntity.status(404).body(Map.of("error", "AI is disabled"));
    }

    // Get user capabilities from route permissions
    // Note: This implementation extracts basic permissions from route metadata
    // For production, integrate with dedicated PermissionService/RBAC system
    Map<String, Object> capabilities = getUserCapabilitiesFromRoute(userId, routeId);

    metricsCollector.recordMcpCall("auth");
    log.info("✅ MCP: auth.get_user_capabilities completed [{}]", correlationId);
    return ResponseEntity.ok(capabilities);
  }

  /**
   * Extract user capabilities for a route
   * 
   * Returns basic capabilities based on route metadata.
   * For fine-grained RBAC, extend this to query tenant-specific permissions.
   * 
   * @param userId User ID
   * @param routeId Route ID
   * @return User capabilities map
   */
  private Map<String, Object> getUserCapabilitiesFromRoute(String userId, String routeId) {
    // Conservative permissions for now
    // To extend: Query tenant-specific RBAC rules from metamodel or permission service

    boolean canView = routeId != null && !routeId.isEmpty(); // Basic check
    boolean canEdit = false; // Conservative default

    log.debug("🔐 Basic capabilities for user={}, route={}: view={}, edit={}", userId, routeId,
        canView, canEdit);

    return Map.of("canView", canView, "canEdit", canEdit, "canExecute",
        java.util.Collections.emptyList(), "note",
        "Basic implementation - integrate PermissionService for full RBAC");
  }

  /**
   * MCP Tool: data_context.query (STUB)
   * 
   * POST /api/ai/mcp/data_context/query
   * 
   * Input: { "routeId", "entity", "fields", "filters", "mode" } Output: 501
   * NOT_IMPLEMENTED (stub, returns schema only in META_ONLY mode)
   */
  @PostMapping("/data_context/query")
  public ResponseEntity<Map<String, Object>> queryData(@RequestBody Map<String, Object> input) {
    String correlationId = UUID.randomUUID().toString();

    log.info("🔧 MCP: data_context.query(...) [{}] - NOT IMPLEMENTED", correlationId);

    // Check AI enabled
    if (!isAiEnabled()) {
      return ResponseEntity.status(404).body(Map.of("error", "AI is disabled"));
    }

    // Return 501 with contract validation
    Map<String, Object> response = Map.of("error", "Not implemented yet", "code", "NOT_IMPLEMENTED",
        "note", "Will return schema-only data in META_ONLY mode", "contract",
        "Validated input schema");

    metricsCollector.recordMcpCall("data_context");
    log.warn("⚠️ MCP: data_context.query not implemented [{}]", correlationId);
    return ResponseEntity.status(501).body(response);
  }

  /**
   * Check if AI is enabled
   */
  private boolean isAiEnabled() {
    return globalConfig.getAi() != null && Boolean.TRUE.equals(globalConfig.getAi().getEnabled());
  }
}
