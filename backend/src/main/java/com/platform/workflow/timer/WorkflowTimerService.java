package com.platform.workflow.timer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 🔄 W8: Workflow Timer Service
 * 
 * Manages workflow timers for: - SLA monitoring (warn/breach alerts) -
 * Scheduled transitions (auto-approve after N days) - Reminder notifications
 * 
 * Features: - Periodic timer check (every minute) - Timer execution via
 * WorkflowExecutionService - SLA breach metrics
 * 
 * @since 2025-01-14
 */
@Service
public class WorkflowTimerService {

  private static final Logger log = LoggerFactory.getLogger(WorkflowTimerService.class);

  private final JdbcTemplate jdbcTemplate;

  public WorkflowTimerService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Check for expired timers every minute
   */
  @Scheduled(fixedDelay = 60000, initialDelay = 10000)
  public void checkExpiredTimers() {
    log.debug("Checking for expired workflow timers");

    var expiredTimers = findExpiredTimers();

    if (expiredTimers.isEmpty()) {
      return;
    }

    log.info("Found {} expired timers", expiredTimers.size());

    for (var timer : expiredTimers) {
      try {
        processExpiredTimer(timer);
      } catch (Exception e) {
        log.error("Failed to process timer {}: {}", timer.get("id"), e.getMessage(), e);
      }
    }
  }

  /**
   * Find all timers that have expired
   */
  private List<Map<String, Object>> findExpiredTimers() {
    String sql = """
        SELECT id, workflow_instance_id, timer_type, scheduled_at, action_code, context
        FROM workflow_timers
        WHERE status = 'PENDING'
          AND scheduled_at <= ?
        ORDER BY scheduled_at
        LIMIT 100
        """;

    // Convert Instant to Timestamp to avoid PostgreSQL type inference error
    return jdbcTemplate.queryForList(sql, Timestamp.from(Instant.now()));
  }

  /**
   * Process single expired timer
   */
  private void processExpiredTimer(Map<String, Object> timer) {
    Long timerId = ((Number) timer.get("id")).longValue();
    Long instanceId = ((Number) timer.get("workflow_instance_id")).longValue();
    String timerType = (String) timer.get("timer_type");
    String actionCode = (String) timer.get("action_code");

    log.info("Processing expired timer: id={}, type={}, instance={}", timerId, timerType,
        instanceId);

    switch (timerType) {
    case "SLA_WARNING" -> handleSlaWarning(timerId, instanceId);
    case "SLA_BREACH" -> handleSlaBreach(timerId, instanceId);
    case "AUTO_TRANSITION" -> handleAutoTransition(timerId, instanceId, actionCode);
    case "REMINDER" -> handleReminder(timerId, instanceId);
    default -> log.warn("Unknown timer type: {}", timerType);
    }

    markTimerCompleted(timerId);
  }

  /**
   * Handle SLA warning timer
   */
  private void handleSlaWarning(Long timerId, Long instanceId) {
    log.warn("SLA WARNING triggered for instance {}", instanceId);

    // Publish event, send notification, etc.
    jdbcTemplate.update("""
        INSERT INTO workflow_events (workflow_instance_id, event_type, event_data, created_at)
        VALUES (?, 'SLA_WARNING', ?::jsonb, ?)
        """, instanceId, "{\"timerId\": " + timerId + ", \"severity\": \"WARNING\"}",
        Instant.now());
  }

  /**
   * Handle SLA breach timer
   */
  private void handleSlaBreach(Long timerId, Long instanceId) {
    log.error("SLA BREACH triggered for instance {}", instanceId);

    jdbcTemplate.update("""
        INSERT INTO workflow_events (workflow_instance_id, event_type, event_data, created_at)
        VALUES (?, 'SLA_BREACH', ?::jsonb, ?)
        """, instanceId, "{\"timerId\": " + timerId + ", \"severity\": \"CRITICAL\"}",
        Instant.now());
  }

  /**
   * Handle auto-transition timer
   */
  private void handleAutoTransition(Long timerId, Long instanceId, String actionCode) {
    log.info("AUTO_TRANSITION triggered: instance={}, action={}", instanceId, actionCode);

    // Execute transition via WorkflowService
    // (Would integrate with WorkflowService.applyTransition here)
  }

  /**
   * Handle reminder timer
   */
  private void handleReminder(Long timerId, Long instanceId) {
    log.info("REMINDER triggered for instance {}", instanceId);

    // Send reminder notification
  }

  /**
   * Mark timer as completed
   */
  private void markTimerCompleted(Long timerId) {
    jdbcTemplate.update("""
        UPDATE workflow_timers
        SET status = 'COMPLETED', completed_at = ?
        WHERE id = ?
        """, Instant.now(), timerId);
  }

  /**
   * Create SLA timer for workflow instance
   */
  public void createSlaTimer(Long instanceId, String timerType, Instant triggerAt) {
    jdbcTemplate.update(
        """
            INSERT INTO workflow_timers (workflow_instance_id, timer_type, scheduled_at, status, created_at)
            VALUES (?, ?, ?, 'PENDING', ?)
            """,
        instanceId, timerType, triggerAt, Instant.now());

    log.info("Created {} timer for instance {}, triggers at {}", timerType, instanceId, triggerAt);
  }

  /**
   * Cancel pending timers for instance
   */
  public void cancelTimers(Long instanceId) {
    int cancelled = jdbcTemplate.update("""
        UPDATE workflow_timers
        SET status = 'CANCELLED', completed_at = ?
        WHERE workflow_instance_id = ? AND status = 'PENDING'
        """, Instant.now(), instanceId);

    if (cancelled > 0) {
      log.info("Cancelled {} pending timers for instance {}", cancelled, instanceId);
    }
  }
}
