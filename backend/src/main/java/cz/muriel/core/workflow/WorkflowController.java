package cz.muriel.core.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 🔄 Workflow REST Controller
 */
@RestController @RequestMapping("/api/entities") @RequiredArgsConstructor @Slf4j
public class WorkflowController {

  private final WorkflowService workflowService;

  /**
   * Get current state
   */
  @GetMapping("/{entityType}/{entityId}/state")
  public ResponseEntity<Map<String, Object>> getCurrentState(@PathVariable String entityType,
      @PathVariable String entityId, Authentication auth) {
    String tenantId = extractTenantId(auth);

    WorkflowModels.EntityState state = workflowService.getCurrentState(entityType, entityId,
        tenantId);

    if (state == null) {
      return ResponseEntity.ok(Map.of("entityType", entityType, "entityId", entityId, "stateCode",
          "none", "slaStatus", "NONE"));
    }

    // Calculate SLA for current state
    // We need to get the transition that led to this state to get SLA minutes
    WorkflowModels.SlaStatus slaStatus = WorkflowModels.SlaStatus.NONE;

    return ResponseEntity.ok(Map.of("entityType", state.getEntityType(), "entityId",
        state.getEntityId(), "stateCode", state.getStateCode(), "since",
        state.getSince().toString(), "slaStatus", slaStatus.name()));
  }

  /**
   * Get allowed transitions
   */
  @GetMapping("/{entityType}/{entityId}/transitions")
  public ResponseEntity<List<Map<String, Object>>> getAllowedTransitions(
      @PathVariable String entityType, @PathVariable String entityId, Authentication auth) {
    String tenantId = extractTenantId(auth);

    List<WorkflowModels.StateTransition> transitions = workflowService.getAllowedTransitions(auth,
        entityType, entityId, tenantId);

    List<Map<String, Object>> response = transitions.stream()
        .map(t -> Map.of("code", t.getCode(), "fromCode",
            t.getFromCode() != null ? t.getFromCode() : "null", "toCode", t.getToCode(),
            "slaMinutes", (Object) (t.getSlaMinutes() != null ? t.getSlaMinutes() : "none")))
        .toList();

    return ResponseEntity.ok(response);
  }

  /**
   * Apply transition
   */
  @PostMapping("/{entityType}/{entityId}/transition/{transitionCode}")
  public ResponseEntity<Map<String, Object>> applyTransition(@PathVariable String entityType,
      @PathVariable String entityId, @PathVariable String transitionCode, Authentication auth) {
    String tenantId = extractTenantId(auth);

    WorkflowModels.TransitionResult result = workflowService.applyTransition(auth, entityType,
        entityId, tenantId, transitionCode);

    if (!result.isSuccess()) {
      return ResponseEntity.badRequest()
          .body(Map.of("success", false, "message", result.getMessage()));
    }

    return ResponseEntity.ok(Map.of("success", true, "message", result.getMessage(), "newState",
        Map.of("stateCode", result.getNewState().getStateCode(), "since",
            result.getNewState().getSince().toString()),
        "slaStatus", result.getSlaStatus().name()));
  }

  private String extractTenantId(Authentication auth) {
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String issuer = jwt.getIssuer().toString();
      if (issuer.contains("/realms/")) {
        return issuer.substring(issuer.lastIndexOf("/realms/") + 8);
      }
    }
    return "unknown";
  }
}
