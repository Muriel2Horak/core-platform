package cz.muriel.core.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 🔄 Workflow Models (W5 Enhanced)
 */
public class WorkflowModels {

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class EntityState {
    private String entityType;
    private String entityId;
    private String tenantId;
    private String stateCode;
    private Instant since;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class StateTransition {
    private String entityType;
    private String fromCode;
    private String toCode;
    private String code;
    private Map<String, Object> guard;
    private Integer slaMinutes;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class StateLog {
    private Long id;
    private String entityType;
    private String entityId;
    private String tenantId;
    private String fromCode;
    private String toCode;
    private String changedBy;
    private Instant changedAt;
    private Map<String, Object> metadata;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class TransitionResult {
    private boolean success;
    private String message;
    private EntityState newState;
    private SlaStatus slaStatus;
  }

  public enum SlaStatus {
    NONE, OK, WARN, BREACH
  }

  // ============================================
  // W5: NEW MODELS
  // ============================================

  /**
   * W5: Workflow Instance - runtime execution context
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class WorkflowInstance {
    private UUID id;
    private String tenantId;
    private String entityType;
    private String entityId;
    private UUID workflowVersionId;
    private String currentStateCode;
    private Instant startedAt;
    private Instant completedAt;
    private WorkflowStatus status;
    private String errorMessage;
    private Map<String, Object> context;
    private String createdBy;
    private Instant updatedAt;
  }

  /**
   * W5: Workflow Event - immutable event log
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class WorkflowEvent {
    private Long id;
    private UUID eventId;
    private String tenantId;
    private String entityType;
    private String entityId;
    private UUID workflowInstanceId;
    private WorkflowEventType eventType;
    private String fromStateCode;
    private String toStateCode;
    private String transitionCode;
    private String actor;
    private Instant timestamp;
    private Long durationMs;
    private Map<String, Object> metadata;
    private String errorDetails;
  }

  /**
   * W5: Workflow Timer - scheduled timers
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class WorkflowTimer {
    private UUID id;
    private String tenantId;
    private String entityType;
    private String entityId;
    private UUID workflowInstanceId;
    private TimerType timerType;
    private String stateCode;
    private Instant scheduledAt;
    private Instant firedAt;
    private TimerStatus status;
    private String action;
    private Map<String, Object> metadata;
    private Instant createdAt;
  }

  /**
   * W5: Workflow Graph - visual representation
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class WorkflowGraph {
    private String entityType;
    private String entityId;
    private String currentState;
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class GraphNode {
    private String id;
    private String code;
    private String label;
    private String type; // state, start, end
    private boolean current;
    private Map<String, Object> metadata;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class GraphEdge {
    private String id;
    private String source;
    private String target;
    private String label;
    private String transitionCode;
    private boolean allowed;
    private String whyNot; // Reason why transition is not allowed
    private Integer slaMinutes;
  }

  /**
   * W5: Workflow State Detail - current state with allowed actions
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class WorkflowStateDetail {
    private EntityState currentState;
    private List<AllowedTransition> allowedTransitions;
    private List<BlockedTransition> blockedTransitions;
    private SlaStatus slaStatus;
    private Long stateAgeMs;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class AllowedTransition {
    private String code;
    private String label;
    private String toState;
    private Integer slaMinutes;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class BlockedTransition {
    private String code;
    private String label;
    private String toState;
    private String reason; // Why not allowed
  }

  /**
   * W5: Workflow History - timeline with durations
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class WorkflowHistory {
    private String entityType;
    private String entityId;
    private List<HistoryEntry> entries;
    private Long totalDurationMs;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class HistoryEntry {
    private WorkflowEventType eventType;
    private String fromState;
    private String toState;
    private String transitionCode;
    private Instant timestamp;
    private Long durationMs; // Duration in this state/action
    private String actor;
    private SlaStatus slaStatus;
  }

  /**
   * W5: Workflow Forecast - next steps and pending timers
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class WorkflowForecast {
    private String currentState;
    private List<ForecastStep> nextSteps;
    private List<PendingTimer> pendingTimers;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class ForecastStep {
    private String transitionCode;
    private String label;
    private String toState;
    private boolean automatic; // Will happen automatically or requires user action
    private Integer estimatedSlaMinutes;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class PendingTimer {
    private UUID id;
    private TimerType type;
    private Instant scheduledAt;
    private String action;
    private Long remainingMs;
  }

  // ============================================
  // W5: ENUMS
  // ============================================

  public enum WorkflowStatus {
    RUNNING, COMPLETED, FAILED, CANCELLED
  }

  public enum WorkflowEventType {
    ENTER_STATE, EXIT_STATE, ACTION_APPLIED, TIMER_FIRED, ERROR
  }

  public enum TimerType {
    DELAY, SLA_WARNING, SLA_BREACH, ESCALATION
  }

  public enum TimerStatus {
    PENDING, FIRED, CANCELLED
  }
}
