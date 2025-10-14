package cz.muriel.core.workflow;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.TransitionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔄 W5: Workflow Runtime Service
 * 
 * Provides graph visualization, state details, history timeline, and forecast.
 * 
 * @since 2025-10-14
 */
@Service @RequiredArgsConstructor @Slf4j
public class WorkflowRuntimeService {

  private final JdbcTemplate jdbcTemplate;
  private final WorkflowService workflowService;
  private final MetamodelRegistry metamodelRegistry;

  // ============================================
  // GRAPH API
  // ============================================

  /**
   * Get workflow graph for entity Returns visual representation with nodes,
   * edges, current state highlighted
   */
  public WorkflowModels.WorkflowGraph getWorkflowGraph(Authentication auth, String entityType,
      String entityId, String tenantId) {

    log.debug("Getting workflow graph for {}/{} in tenant {}", entityType, entityId, tenantId);

    // Get current state
    WorkflowModels.EntityState currentState = workflowService.getCurrentState(entityType, entityId,
        tenantId);
    String currentStateCode = currentState != null ? currentState.getStateCode() : null;

    // Get metamodel schema
    Optional<EntitySchema> schemaOpt = metamodelRegistry.getSchema(entityType);
    if (schemaOpt.isEmpty() || schemaOpt.get().getStates() == null
        || schemaOpt.get().getStates().isEmpty()) {
      log.warn("No workflow states defined for entity type: {}", entityType);
      return WorkflowModels.WorkflowGraph.builder().entityType(entityType).entityId(entityId)
          .currentState(currentStateCode).nodes(Collections.emptyList())
          .edges(Collections.emptyList()).build();
    }

    EntitySchema schema = schemaOpt.get();

    // Build nodes from states
    List<WorkflowModels.GraphNode> nodes = schema.getStates().stream()
        .map(
            state -> WorkflowModels.GraphNode.builder().id(state.getCode()).code(state.getCode())
                .label(state.getLabel() != null ? state.getLabel() : state.getCode()).type("state")
                .current(state.getCode().equals(currentStateCode))
                .metadata(Map.of("description",
                    state.getDescription() != null ? state.getDescription() : ""))
                .build())
        .collect(Collectors.toList());

    // Build edges from transitions
    List<WorkflowModels.GraphEdge> edges = new ArrayList<>();
    if (schema.getTransitions() != null) {
      List<WorkflowModels.StateTransition> allowedTransitions = workflowService
          .getAllowedTransitions(auth, entityType, entityId, tenantId);
      Set<String> allowedCodes = allowedTransitions.stream()
          .map(WorkflowModels.StateTransition::getCode).collect(Collectors.toSet());

      for (TransitionConfig transition : schema.getTransitions()) {
        boolean isAllowed = allowedCodes.contains(transition.getCode());
        String whyNot = null;

        if (!isAllowed) {
          // Determine why transition is not allowed
          if (currentStateCode == null) {
            whyNot = "No current state";
          } else if (transition.getFrom() != null
              && !transition.getFrom().equals(currentStateCode)) {
            whyNot = "Current state is " + currentStateCode + ", requires " + transition.getFrom();
          } else {
            whyNot = "Guard condition not satisfied";
          }
        }

        edges.add(WorkflowModels.GraphEdge.builder().id(transition.getCode())
            .source(transition.getFrom() != null ? transition.getFrom() : "START")
            .target(transition.getTo())
            .label(transition.getLabel() != null ? transition.getLabel() : transition.getCode())
            .transitionCode(transition.getCode()).allowed(isAllowed).whyNot(whyNot)
            .slaMinutes(transition.getSlaMinutes()).build());
      }
    }

    return WorkflowModels.WorkflowGraph.builder().entityType(entityType).entityId(entityId)
        .currentState(currentStateCode).nodes(nodes).edges(edges).build();
  }

  // ============================================
  // STATE DETAIL API
  // ============================================

  /**
   * Get detailed state information including allowed and blocked transitions
   */
  public WorkflowModels.WorkflowStateDetail getStateDetail(Authentication auth, String entityType,
      String entityId, String tenantId) {

    log.debug("Getting state detail for {}/{} in tenant {}", entityType, entityId, tenantId);

    // Get current state
    WorkflowModels.EntityState currentState = workflowService.getCurrentState(entityType, entityId,
        tenantId);

    // Get all transitions for this entity type
    List<WorkflowModels.StateTransition> allowedTransitions = workflowService
        .getAllowedTransitions(auth, entityType, entityId, tenantId);

    // Get metamodel to find all possible transitions
    Optional<EntitySchema> schemaOpt = metamodelRegistry.getSchema(entityType);
    List<TransitionConfig> allTransitions = schemaOpt.isPresent()
        && schemaOpt.get().getTransitions() != null ? schemaOpt.get().getTransitions()
            : Collections.emptyList();

    Set<String> allowedCodes = allowedTransitions.stream()
        .map(WorkflowModels.StateTransition::getCode).collect(Collectors.toSet());

    // Build allowed transitions
    List<WorkflowModels.AllowedTransition> allowed = allowedTransitions.stream()
        .map(t -> WorkflowModels.AllowedTransition.builder().code(t.getCode())
            .label(findTransitionLabel(allTransitions, t.getCode())).toState(t.getToCode())
            .slaMinutes(t.getSlaMinutes()).build())
        .collect(Collectors.toList());

    // Build blocked transitions
    String currentCode = currentState != null ? currentState.getStateCode() : null;
    List<WorkflowModels.BlockedTransition> blocked = allTransitions.stream()
        .filter(t -> !allowedCodes.contains(t.getCode()))
        .filter(t -> t.getFrom() == null || t.getFrom().equals(currentCode)) // Only show relevant
                                                                             // blocked ones
        .map(t -> {
          String reason = determineBlockReason(t, currentCode, auth);
          return WorkflowModels.BlockedTransition.builder().code(t.getCode())
              .label(t.getLabel() != null ? t.getLabel() : t.getCode()).toState(t.getTo())
              .reason(reason).build();
        }).collect(Collectors.toList());

    // Calculate SLA status
    WorkflowModels.SlaStatus slaStatus = WorkflowModels.SlaStatus.NONE;
    Long stateAgeMs = 0L;

    if (currentState != null) {
      stateAgeMs = Duration.between(currentState.getSince(), Instant.now()).toMillis();

      // Get SLA for current state from entity_state table
      String slaSql = "SELECT sla_minutes FROM entity_state WHERE entity_type = ? AND entity_id = ? AND tenant_id = ?";
      List<Integer> slaMinutes = jdbcTemplate.query(slaSql,
          (rs, rowNum) -> rs.getObject("sla_minutes", Integer.class), entityType, entityId,
          tenantId);

      if (!slaMinutes.isEmpty() && slaMinutes.get(0) != null) {
        Integer sla = slaMinutes.get(0);
        long slaMs = sla * 60L * 1000L;
        long warnMs = (long) (slaMs * 0.8); // 80% = warning

        if (stateAgeMs > slaMs) {
          slaStatus = WorkflowModels.SlaStatus.BREACH;
        } else if (stateAgeMs > warnMs) {
          slaStatus = WorkflowModels.SlaStatus.WARN;
        } else {
          slaStatus = WorkflowModels.SlaStatus.OK;
        }
      }
    }

    return WorkflowModels.WorkflowStateDetail.builder().currentState(currentState)
        .allowedTransitions(allowed).blockedTransitions(blocked).slaStatus(slaStatus)
        .stateAgeMs(stateAgeMs).build();
  }

  // ============================================
  // HISTORY API
  // ============================================

  /**
   * Get workflow history timeline with durations
   */
  public WorkflowModels.WorkflowHistory getHistory(String entityType, String entityId,
      String tenantId) {
    log.debug("Getting workflow history for {}/{} in tenant {}", entityType, entityId, tenantId);

    String sql = """
        SELECT
            from_code,
            to_code,
            transition_code,
            changed_by,
            changed_at,
            LAG(changed_at) OVER (ORDER BY changed_at DESC) AS prev_changed_at
        FROM entity_state_log
        WHERE entity_type = ? AND entity_id = ? AND tenant_id = ?
        ORDER BY changed_at DESC
        """;

    List<WorkflowModels.HistoryEntry> entries = jdbcTemplate.query(sql, (rs, rowNum) -> {
      Instant timestamp = rs.getTimestamp("changed_at").toInstant();
      Instant prevTimestamp = rs.getTimestamp("prev_changed_at") != null
          ? rs.getTimestamp("prev_changed_at").toInstant()
          : null;

      Long durationMs = prevTimestamp != null
          ? Duration.between(timestamp, prevTimestamp).toMillis()
          : Duration.between(timestamp, Instant.now()).toMillis();

      return WorkflowModels.HistoryEntry.builder()
          .eventType(WorkflowModels.WorkflowEventType.ACTION_APPLIED)
          .fromState(rs.getString("from_code")).toState(rs.getString("to_code"))
          .transitionCode(rs.getString("transition_code")).timestamp(timestamp)
          .durationMs(durationMs).actor(rs.getString("changed_by"))
          .slaStatus(calculateSlaStatus(timestamp, durationMs)).build();
    }, entityType, entityId, tenantId);

    Long totalDuration = entries.stream()
        .mapToLong(e -> e.getDurationMs() != null ? e.getDurationMs() : 0L).sum();

    return WorkflowModels.WorkflowHistory.builder().entityType(entityType).entityId(entityId)
        .entries(entries).totalDurationMs(totalDuration).build();
  }

  // ============================================
  // FORECAST API
  // ============================================

  /**
   * Get workflow forecast - next possible steps and active timers
   */
  public WorkflowModels.WorkflowForecast getForecast(Authentication auth, String entityType,
      String entityId, String tenantId) {

    log.debug("Getting workflow forecast for {}/{} in tenant {}", entityType, entityId, tenantId);

    // Get current state
    WorkflowModels.EntityState currentState = workflowService.getCurrentState(entityType, entityId,
        tenantId);
    String currentStateCode = currentState != null ? currentState.getStateCode() : null;

    // Get allowed transitions
    List<WorkflowModels.StateTransition> transitions = workflowService.getAllowedTransitions(auth,
        entityType, entityId, tenantId);

    // Get metamodel for labels
    Optional<EntitySchema> schemaOpt = metamodelRegistry.getSchema(entityType);
    List<TransitionConfig> allTransitions = schemaOpt.isPresent()
        && schemaOpt.get().getTransitions() != null ? schemaOpt.get().getTransitions()
            : Collections.emptyList();

    List<WorkflowModels.ForecastStep> nextSteps = transitions.stream()
        .map(t -> WorkflowModels.ForecastStep.builder().transitionCode(t.getCode())
            .label(findTransitionLabel(allTransitions, t.getToCode())).toState(t.getToCode())
            .automatic(false) // Determined by workflow configuration in W7
            .estimatedSlaMinutes(t.getSlaMinutes()).build())
        .collect(Collectors.toList());

    // Get pending timers
    String timerSql = """
        SELECT id, timer_type, scheduled_at, action
        FROM workflow_timers
        WHERE entity_type = ? AND entity_id = ? AND tenant_id = ? AND status = 'PENDING'
        ORDER BY scheduled_at ASC
        """;

    List<WorkflowModels.PendingTimer> pendingTimers = jdbcTemplate.query(timerSql, (rs, rowNum) -> {
      Instant scheduledAt = rs.getTimestamp("scheduled_at").toInstant();
      Long remainingMs = Duration.between(Instant.now(), scheduledAt).toMillis();

      return WorkflowModels.PendingTimer.builder().id(UUID.fromString(rs.getString("id")))
          .type(WorkflowModels.TimerType.valueOf(rs.getString("timer_type")))
          .scheduledAt(scheduledAt).action(rs.getString("action"))
          .remainingMs(Math.max(0, remainingMs)).build();
    }, entityType, entityId, tenantId);

    return WorkflowModels.WorkflowForecast.builder().currentState(currentStateCode)
        .nextSteps(nextSteps).pendingTimers(pendingTimers).build();
  }

  // ============================================
  // HELPER METHODS
  // ============================================

  private String findTransitionLabel(List<TransitionConfig> transitions, String code) {
    return transitions.stream().filter(t -> t.getCode().equals(code)).findFirst()
        .map(t -> t.getLabel() != null ? t.getLabel() : t.getCode()).orElse(code);
  }

  private String determineBlockReason(TransitionConfig transition, String currentState,
      Authentication auth) {
    if (currentState == null) {
      return "No current state";
    }
    if (transition.getFrom() != null && !transition.getFrom().equals(currentState)) {
      return "Current state is " + currentState + ", requires " + transition.getFrom();
    }
    if (transition.getGuard() != null) {
      return "Insufficient permissions or guard condition not met";
    }
    return "Unknown reason";
  }

  /**
   * Calculate SLA status based on timestamp and duration
   */
  private WorkflowModels.SlaStatus calculateSlaStatus(Instant timestamp, Long durationMs) {
    if (durationMs == null) {
      return WorkflowModels.SlaStatus.OK;
    }
    // SLA exceeded if duration > 24 hours (configurable per workflow)
    long maxDurationMs = 24 * 60 * 60 * 1000L;
    if (durationMs > maxDurationMs) {
      return WorkflowModels.SlaStatus.BREACH;
    }
    // Warning if > 80% of SLA
    if (durationMs > maxDurationMs * 0.8) {
      return WorkflowModels.SlaStatus.WARN;
    }
    return WorkflowModels.SlaStatus.OK;
  }
}
