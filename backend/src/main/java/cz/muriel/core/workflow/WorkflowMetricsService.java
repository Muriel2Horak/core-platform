package cz.muriel.core.workflow;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 🔄 W5: Workflow Metrics Service
 * 
 * Tracks workflow metrics using Micrometer: - State transition durations -
 * Transition error rates - Number of transitions per entity type
 * 
 * @since 2025-10-14
 */
@Service @Slf4j
public class WorkflowMetricsService {

  private final MeterRegistry meterRegistry;

  public WorkflowMetricsService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /**
   * Record state transition duration
   */
  public void recordTransitionDuration(String entityType, String fromState, String toState,
      Duration duration) {

    Timer.builder("workflow.transition.duration")
        .description("Duration of workflow state transitions").tag("entity_type", entityType)
        .tag("from_state", fromState != null ? fromState : "START").tag("to_state", toState)
        .register(meterRegistry).record(duration);

    log.debug("Recorded transition duration: {} -> {} = {}ms", fromState, toState,
        duration.toMillis());
  }

  /**
   * Record state duration (time spent in a state)
   */
  public void recordStateDuration(String entityType, String stateCode, Duration duration) {
    Timer.builder("workflow.state.duration")
        .description("Duration spent in a specific workflow state").tag("entity_type", entityType)
        .tag("state", stateCode).register(meterRegistry).record(duration);

    log.debug("Recorded state duration: {} in {} = {}ms", stateCode, entityType,
        duration.toMillis());
  }

  /**
   * Increment transition counter
   */
  public void incrementTransitionCount(String entityType, String transitionCode, boolean success) {
    Counter.builder("workflow.transition.count").description("Number of workflow transitions")
        .tag("entity_type", entityType).tag("transition", transitionCode)
        .tag("status", success ? "success" : "failed").register(meterRegistry).increment();

    log.debug("Incremented transition count: {} / {} = {}", entityType, transitionCode,
        success ? "success" : "failed");
  }

  /**
   * Record transition error
   */
  public void recordTransitionError(String entityType, String transitionCode, String errorType) {
    Counter.builder("workflow.transition.error").description("Workflow transition errors")
        .tag("entity_type", entityType).tag("transition", transitionCode)
        .tag("error_type", errorType).register(meterRegistry).increment();

    log.warn("Recorded transition error: {} / {} / {}", entityType, transitionCode, errorType);
  }

  /**
   * Record SLA breach
   */
  public void recordSlaBreach(String entityType, String stateCode) {
    Counter.builder("workflow.sla.breach").description("SLA breaches in workflow states")
        .tag("entity_type", entityType).tag("state", stateCode).register(meterRegistry).increment();

    log.warn("Recorded SLA breach: {} / {}", entityType, stateCode);
  }

  /**
   * Record SLA warning (80% threshold)
   */
  public void recordSlaWarning(String entityType, String stateCode) {
    Counter.builder("workflow.sla.warning")
        .description("SLA warnings in workflow states (80% threshold)")
        .tag("entity_type", entityType).tag("state", stateCode).register(meterRegistry).increment();

    log.debug("Recorded SLA warning: {} / {}", entityType, stateCode);
  }

  /**
   * Record active workflow instances
   */
  public void recordActiveWorkflows(String entityType, int count) {
    meterRegistry.gauge("workflow.active.count",
        java.util.List.of(io.micrometer.core.instrument.Tag.of("entity_type", entityType)), count);
  }

  /**
   * Record workflow completion
   */
  public void recordWorkflowCompletion(String entityType, String status, Duration totalDuration) {
    Counter.builder("workflow.completion").description("Workflow completions")
        .tag("entity_type", entityType).tag("status", status) // COMPLETED, FAILED, CANCELLED
        .register(meterRegistry).increment();

    Timer.builder("workflow.total.duration").description("Total workflow execution duration")
        .tag("entity_type", entityType).tag("status", status).register(meterRegistry)
        .record(totalDuration);

    log.info("Recorded workflow completion: {} / {} = {}ms", entityType, status,
        totalDuration.toMillis());
  }
}
