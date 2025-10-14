package cz.muriel.core.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

/**
 * 🔄 W5: Workflow Runtime REST Controller
 * 
 * Provides endpoints for workflow graph, state detail, history, and forecast.
 * 
 * @since 2025-10-14
 */
@RestController @RequestMapping("/api/workflows") @RequiredArgsConstructor @Slf4j
public class WorkflowRuntimeController {

  private final WorkflowRuntimeService workflowRuntimeService;

  /**
   * GET /api/workflows/{entity}/{id}/graph Returns workflow graph visualization
   * with nodes, edges, current state
   */
  @GetMapping("/{entity}/{id}/graph")
  public ResponseEntity<WorkflowModels.WorkflowGraph> getWorkflowGraph(@PathVariable String entity,
      @PathVariable String id, Authentication auth) {

    String tenantId = extractTenantId(auth);
    log.debug("GET /api/workflows/{}/{}/graph by tenant {}", entity, id, tenantId);

    WorkflowModels.WorkflowGraph graph = workflowRuntimeService.getWorkflowGraph(auth, entity, id,
        tenantId);
    return ResponseEntity.ok(graph);
  }

  /**
   * GET /api/workflows/{entity}/{id}/state Returns current state with
   * allowed/blocked transitions and "why not" reasons
   */
  @GetMapping("/{entity}/{id}/state")
  public ResponseEntity<WorkflowModels.WorkflowStateDetail> getState(@PathVariable String entity,
      @PathVariable String id, Authentication auth) {

    String tenantId = extractTenantId(auth);
    log.debug("GET /api/workflows/{}/{}/state by tenant {}", entity, id, tenantId);

    WorkflowModels.WorkflowStateDetail stateDetail = workflowRuntimeService.getStateDetail(auth,
        entity, id, tenantId);
    return ResponseEntity.ok(stateDetail);
  }

  /**
   * GET /api/workflows/{entity}/{id}/history Returns workflow history timeline
   * with durations
   */
  @GetMapping("/{entity}/{id}/history")
  public ResponseEntity<WorkflowModels.WorkflowHistory> getHistory(@PathVariable String entity,
      @PathVariable String id, Authentication auth) {

    String tenantId = extractTenantId(auth);
    log.debug("GET /api/workflows/{}/{}/history by tenant {}", entity, id, tenantId);

    WorkflowModels.WorkflowHistory history = workflowRuntimeService.getHistory(entity, id,
        tenantId);
    return ResponseEntity.ok(history);
  }

  /**
   * GET /api/workflows/{entity}/{id}/forecast Returns next possible steps and
   * active timers/SLA
   */
  @GetMapping("/{entity}/{id}/forecast")
  public ResponseEntity<WorkflowModels.WorkflowForecast> getForecast(@PathVariable String entity,
      @PathVariable String id, Authentication auth) {

    String tenantId = extractTenantId(auth);
    log.debug("GET /api/workflows/{}/{}/forecast by tenant {}", entity, id, tenantId);

    WorkflowModels.WorkflowForecast forecast = workflowRuntimeService.getForecast(auth, entity, id,
        tenantId);
    return ResponseEntity.ok(forecast);
  }

  // ============================================
  // HELPER METHODS
  // ============================================

  private String extractTenantId(Authentication auth) {
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      var jwt = jwtAuth.getToken();
      return jwt.getClaimAsString("tenant_id");
    }
    return "default-tenant";
  }
}
