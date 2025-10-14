package com.platform.workflow.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 📊 W12: Workflow Monitoring Service
 * 
 * Real-time analytics and monitoring: - Instance counts per state - Transition
 * success/failure rates - Average execution times - Stuck instances detection -
 * SLA violations
 * 
 * @since 2025-01-14
 */
@Service @RequiredArgsConstructor @Slf4j
public class WorkflowMonitoringService {

  private final JdbcTemplate jdbcTemplate;

  /**
   * Get instance distribution across states
   */
  public List<StateDistribution> getStateDistribution(String entityType, String tenantId) {
    String sql = """
        SELECT current_state, COUNT(*) as count
        FROM workflow_instances
        WHERE entity_type = ? AND tenant_id = ? AND completed_at IS NULL
        GROUP BY current_state
        ORDER BY count DESC
        """;

    return jdbcTemplate.query(sql,
        (rs, rowNum) -> new StateDistribution(rs.getString("current_state"), rs.getLong("count")),
        entityType, tenantId);
  }

  /**
   * Get transition success rate
   */
  public TransitionMetrics getTransitionMetrics(String entityType, String tenantId, Instant since) {
    String sql = """
        SELECT
            COUNT(*) as total_transitions,
            COUNT(*) FILTER (WHERE error_message IS NULL) as successful,
            COUNT(*) FILTER (WHERE error_message IS NOT NULL) as failed,
            AVG(EXTRACT(EPOCH FROM (completed_at - started_at))) as avg_duration_seconds
        FROM workflow_events
        WHERE entity_type = ? AND tenant_id = ? AND started_at >= ?
        """;

    return jdbcTemplate.queryForObject(sql,
        (rs, rowNum) -> new TransitionMetrics(rs.getLong("total_transitions"),
            rs.getLong("successful"), rs.getLong("failed"), rs.getDouble("avg_duration_seconds")),
        entityType, tenantId, since);
  }

  /**
   * Find stuck instances (no activity for N hours)
   */
  public List<StuckInstance> findStuckInstances(String entityType, String tenantId,
      int hoursThreshold) {
    Instant threshold = Instant.now().minus(hoursThreshold, ChronoUnit.HOURS);

    String sql = """
        SELECT
            wi.id,
            wi.entity_id,
            wi.current_state,
            wi.updated_at,
            EXTRACT(EPOCH FROM (NOW() - wi.updated_at)) / 3600 as hours_stuck
        FROM workflow_instances wi
        WHERE wi.entity_type = ?
          AND wi.tenant_id = ?
          AND wi.completed_at IS NULL
          AND wi.updated_at < ?
        ORDER BY wi.updated_at ASC
        LIMIT 100
        """;

    return jdbcTemplate.query(sql,
        (rs, rowNum) -> new StuckInstance(rs.getLong("id"), rs.getString("entity_id"),
            rs.getString("current_state"), rs.getTimestamp("updated_at").toInstant(),
            rs.getDouble("hours_stuck")),
        entityType, tenantId, threshold);
  }

  /**
   * Get SLA violations
   */
  public List<SLAViolation> getSLAViolations(String entityType, String tenantId, int slaMinutes) {
    String sql = """
        SELECT
            wi.id,
            wi.entity_id,
            wi.current_state,
            wi.started_at,
            EXTRACT(EPOCH FROM (NOW() - wi.started_at)) / 60 as minutes_running
        FROM workflow_instances wi
        WHERE wi.entity_type = ?
          AND wi.tenant_id = ?
          AND wi.completed_at IS NULL
          AND wi.started_at < NOW() - INTERVAL '1 minute' * ?
        ORDER BY wi.started_at ASC
        """;

    return jdbcTemplate.query(sql,
        (rs, rowNum) -> new SLAViolation(rs.getLong("id"), rs.getString("entity_id"),
            rs.getString("current_state"), rs.getTimestamp("started_at").toInstant(),
            rs.getDouble("minutes_running"), slaMinutes),
        entityType, tenantId, slaMinutes);
  }

  /**
   * Get hourly throughput (instances completed per hour)
   */
  public List<ThroughputMetric> getHourlyThroughput(String entityType, String tenantId, int hours) {
    String sql = """
        SELECT
            DATE_TRUNC('hour', completed_at) as hour,
            COUNT(*) as completed_count
        FROM workflow_instances
        WHERE entity_type = ?
          AND tenant_id = ?
          AND completed_at >= NOW() - INTERVAL '1 hour' * ?
        GROUP BY hour
        ORDER BY hour DESC
        """;

    return jdbcTemplate.query(sql,
        (rs, rowNum) -> new ThroughputMetric(rs.getTimestamp("hour").toInstant(),
            rs.getLong("completed_count")),
        entityType, tenantId, hours);
  }

  /**
   * Get error rate by state
   */
  public List<StateErrorRate> getErrorRateByState(String entityType, String tenantId,
      Instant since) {
    String sql = """
        SELECT
            from_state,
            COUNT(*) as total_attempts,
            COUNT(*) FILTER (WHERE error_message IS NOT NULL) as errors,
            ROUND(100.0 * COUNT(*) FILTER (WHERE error_message IS NOT NULL) / COUNT(*), 2) as error_rate
        FROM workflow_events
        WHERE entity_type = ? AND tenant_id = ? AND started_at >= ?
        GROUP BY from_state
        HAVING COUNT(*) > 10
        ORDER BY error_rate DESC
        """;

    return jdbcTemplate.query(
        sql, (rs, rowNum) -> new StateErrorRate(rs.getString("from_state"),
            rs.getLong("total_attempts"), rs.getLong("errors"), rs.getDouble("error_rate")),
        entityType, tenantId, since);
  }

  /**
   * Get overall health dashboard
   */
  public WorkflowHealth getWorkflowHealth(String entityType, String tenantId) {
    Instant last24h = Instant.now().minus(24, ChronoUnit.HOURS);

    // Active instances
    Long activeInstances = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM workflow_instances WHERE entity_type = ? AND tenant_id = ? AND completed_at IS NULL",
        Long.class, entityType, tenantId);

    // Completed last 24h
    Long completedLast24h = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM workflow_instances WHERE entity_type = ? AND tenant_id = ? AND completed_at >= ?",
        Long.class, entityType, tenantId, last24h);

    // Stuck instances (>2h)
    List<StuckInstance> stuck = findStuckInstances(entityType, tenantId, 2);

    // SLA violations (>60min)
    List<SLAViolation> violations = getSLAViolations(entityType, tenantId, 60);

    // Transition metrics
    TransitionMetrics metrics = getTransitionMetrics(entityType, tenantId, last24h);

    double successRate = metrics.totalTransitions() > 0
        ? (double) metrics.successful() / metrics.totalTransitions() * 100
        : 0;

    return new WorkflowHealth(entityType, tenantId, activeInstances, completedLast24h, stuck.size(),
        violations.size(), successRate, metrics.avgDurationSeconds(), Instant.now());
  }

  // ===== DTOs =====

  public record StateDistribution(String state, Long count) {
  }

  public record TransitionMetrics(Long totalTransitions, Long successful, Long failed,
      Double avgDurationSeconds) {
  }

  public record StuckInstance(Long instanceId, String entityId, String currentState,
      Instant lastUpdated, Double hoursStuck) {
  }

  public record SLAViolation(Long instanceId, String entityId, String currentState,
      Instant startedAt, Double minutesRunning, Integer slaMinutes) {
  }

  public record ThroughputMetric(Instant hour, Long completedCount) {
  }

  public record StateErrorRate(String state, Long totalAttempts, Long errors, Double errorRate) {
  }

  public record WorkflowHealth(String entityType, String tenantId, Long activeInstances,
      Long completedLast24h, Integer stuckInstances, Integer slaViolations, Double successRate,
      Double avgDurationSeconds, Instant timestamp) {
  }
}
