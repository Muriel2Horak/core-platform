package cz.muriel.core.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.security.policy.PolicyEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 🔄 Workflow Service - State Management & Transitions
 */
@Service @RequiredArgsConstructor @Slf4j
public class WorkflowService {

  private final JdbcTemplate jdbcTemplate;
  @SuppressWarnings("unused")
  private final MetamodelRegistry metamodelRegistry; // Reserved for schema validation
  @SuppressWarnings("unused")
  private final PolicyEngine policyEngine; // Reserved for future guard evaluation
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Get current state of entity
   */
  public WorkflowModels.EntityState getCurrentState(String entityType, String entityId,
      String tenantId) {
    String sql = "SELECT * FROM entity_state WHERE entity_type = ? AND entity_id = ? AND tenant_id = ?";

    List<WorkflowModels.EntityState> states = jdbcTemplate.query(sql, new EntityStateRowMapper(),
        entityType, entityId, tenantId);

    return states.isEmpty() ? null : states.get(0);
  }

  /**
   * Get allowed transitions for current state
   */
  public List<WorkflowModels.StateTransition> getAllowedTransitions(Authentication auth,
      String entityType, String entityId, String tenantId) {
    WorkflowModels.EntityState currentState = getCurrentState(entityType, entityId, tenantId);
    String fromCode = currentState != null ? currentState.getStateCode() : null;

    String sql = "SELECT * FROM state_transition WHERE entity_type = ? AND "
        + "(from_code = ? OR (from_code IS NULL AND ? IS NULL))";

    List<WorkflowModels.StateTransition> transitions = jdbcTemplate.query(sql,
        new StateTransitionRowMapper(), entityType, fromCode, fromCode);

    // Filter by guard conditions
    return transitions.stream().filter(t -> evaluateGuard(auth, t.getGuard())).toList();
  }

  /**
   * Apply transition
   */
  @Transactional
  public WorkflowModels.TransitionResult applyTransition(Authentication auth, String entityType,
      String entityId, String tenantId, String transitionCode) {
    // Get transition definition
    String sql = "SELECT * FROM state_transition WHERE code = ? AND entity_type = ?";
    List<WorkflowModels.StateTransition> transitions = jdbcTemplate.query(sql,
        new StateTransitionRowMapper(), transitionCode, entityType);

    if (transitions.isEmpty()) {
      return WorkflowModels.TransitionResult.builder().success(false)
          .message("Transition not found: " + transitionCode).build();
    }

    WorkflowModels.StateTransition transition = transitions.get(0);

    // Validate guard
    if (!evaluateGuard(auth, transition.getGuard())) {
      return WorkflowModels.TransitionResult.builder().success(false)
          .message("Transition not allowed: guard condition failed").build();
    }

    // Get current state
    WorkflowModels.EntityState currentState = getCurrentState(entityType, entityId, tenantId);
    String fromCode = currentState != null ? currentState.getStateCode() : null;

    // Validate from state
    if ((transition.getFromCode() == null && fromCode != null)
        || (transition.getFromCode() != null && !transition.getFromCode().equals(fromCode))) {
      return WorkflowModels.TransitionResult.builder().success(false)
          .message("Invalid transition: current state is " + fromCode).build();
    }

    // Apply transition
    Instant now = Instant.now();
    String userId = getUserId(auth);

    if (currentState == null) {
      // Insert new state
      jdbcTemplate.update(
          "INSERT INTO entity_state (entity_type, entity_id, tenant_id, state_code, since) VALUES (?, ?, ?, ?, ?)",
          entityType, entityId, tenantId, transition.getToCode(), now);
    } else {
      // Update existing state
      jdbcTemplate.update(
          "UPDATE entity_state SET state_code = ?, since = ? WHERE entity_type = ? AND entity_id = ? AND tenant_id = ?",
          transition.getToCode(), now, entityType, entityId, tenantId);
    }

    // Log transition
    jdbcTemplate.update(
        "INSERT INTO entity_state_log (entity_type, entity_id, tenant_id, from_code, to_code, changed_by, changed_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
        entityType, entityId, tenantId, fromCode, transition.getToCode(), userId, now);

    // Calculate SLA status
    WorkflowModels.SlaStatus slaStatus = calculateSlaStatus(now, transition.getSlaMinutes());

    WorkflowModels.EntityState newState = WorkflowModels.EntityState.builder()
        .entityType(entityType).entityId(entityId).tenantId(tenantId)
        .stateCode(transition.getToCode()).since(now).build();

    log.info("State transition applied: entity={}/{}, transition={}, from={}, to={}, by={}",
        entityType, entityId, transitionCode, fromCode, transition.getToCode(), userId);

    return WorkflowModels.TransitionResult.builder().success(true)
        .message("Transition applied successfully").newState(newState).slaStatus(slaStatus).build();
  }

  /**
   * Calculate SLA status
   */
  public WorkflowModels.SlaStatus calculateSlaStatus(Instant since, Integer slaMinutes) {
    if (slaMinutes == null) {
      return WorkflowModels.SlaStatus.NONE;
    }

    long elapsedMinutes = Duration.between(since, Instant.now()).toMinutes();
    long warnThreshold = (long) (slaMinutes * 0.8);

    if (elapsedMinutes >= slaMinutes) {
      return WorkflowModels.SlaStatus.BREACH;
    } else if (elapsedMinutes >= warnThreshold) {
      return WorkflowModels.SlaStatus.WARN;
    } else {
      return WorkflowModels.SlaStatus.OK;
    }
  }

  private boolean evaluateGuard(Authentication auth, Map<String, Object> guard) {
    if (guard == null || guard.isEmpty()) {
      return true;
    }

    String expression = (String) guard.get("expression");
    if (expression == null) {
      return true;
    }

    // Simple role check: hasRole('ROLE_NAME')
    if (expression.startsWith("hasRole('") && expression.endsWith("')")) {
      String role = expression.substring(9, expression.length() - 2);
      return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    // TODO: More complex expressions via PolicyEngine
    return true;
  }

  private String getUserId(Authentication auth) {
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      return jwt.getSubject();
    }
    return auth != null ? auth.getName() : "system";
  }

  // Row mappers

  private class EntityStateRowMapper implements RowMapper<WorkflowModels.EntityState> {
    @Override
    public WorkflowModels.EntityState mapRow(ResultSet rs, int rowNum) throws SQLException {
      return WorkflowModels.EntityState.builder().entityType(rs.getString("entity_type"))
          .entityId(rs.getString("entity_id")).tenantId(rs.getString("tenant_id"))
          .stateCode(rs.getString("state_code")).since(rs.getTimestamp("since").toInstant())
          .build();
    }
  }

  private class StateTransitionRowMapper implements RowMapper<WorkflowModels.StateTransition> {
    @Override
    public WorkflowModels.StateTransition mapRow(ResultSet rs, int rowNum) throws SQLException {
      Map<String, Object> guard = null;
      String guardJson = rs.getString("guard");
      if (guardJson != null) {
        try {
          @SuppressWarnings("unchecked")
          Map<String, Object> guardMap = objectMapper.readValue(guardJson, Map.class);
          guard = guardMap;
        } catch (Exception e) {
          log.warn("Failed to parse guard JSON: {}", guardJson, e);
        }
      }

      return WorkflowModels.StateTransition.builder().entityType(rs.getString("entity_type"))
          .fromCode(rs.getString("from_code")).toCode(rs.getString("to_code"))
          .code(rs.getString("code")).guard(guard).slaMinutes((Integer) rs.getObject("sla_minutes"))
          .build();
    }
  }
}
