package com.example.platform.workflow.monitoring;

import com.platform.workflow.monitoring.WorkflowMonitoringService;
import com.platform.workflow.monitoring.WorkflowMonitoringService.*;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 📊 W12: Workflow Monitoring REST API
 * 
 * Real-time monitoring and analytics endpoints:
 * - State distribution (instances per state)
 * - Transition metrics (success rate, avg duration)
 * - Stuck instances detection
 * - SLA violations
 * - Throughput metrics
 * - Error rates by state
 * - Overall health dashboard
 * 
 * @since 2025-01-14
 */
@RestController
@RequestMapping("/api/v1/workflows/monitoring")
@RequiredArgsConstructor
@Slf4j
public class WorkflowMonitoringController {

    private final WorkflowMonitoringService monitoringService;

    /**
     * Get state distribution (how many instances in each state)
     */
    @GetMapping("/distribution/{entityType}")
    @Timed(value = "workflow.monitoring.distribution", description = "Time to get state distribution")
    public ResponseEntity<List<StateDistribution>> getStateDistribution(
            @PathVariable String entityType,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        List<StateDistribution> distribution = monitoringService.getStateDistribution(entityType, tenantId);
        return ResponseEntity.ok(distribution);
    }

    /**
     * Get transition metrics (success/failure rates, avg duration)
     */
    @GetMapping("/metrics/{entityType}")
    @Timed(value = "workflow.monitoring.metrics", description = "Time to get transition metrics")
    public ResponseEntity<TransitionMetrics> getTransitionMetrics(
            @PathVariable String entityType,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "24") int hours) {

        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        TransitionMetrics metrics = monitoringService.getTransitionMetrics(entityType, tenantId, since);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Find stuck instances (no activity for N hours)
     */
    @GetMapping("/stuck/{entityType}")
    @Timed(value = "workflow.monitoring.stuck", description = "Time to find stuck instances")
    public ResponseEntity<List<StuckInstance>> findStuckInstances(
            @PathVariable String entityType,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "2") int hoursThreshold) {

        List<StuckInstance> stuck = monitoringService.findStuckInstances(entityType, tenantId, hoursThreshold);
        return ResponseEntity.ok(stuck);
    }

    /**
     * Get SLA violations (instances running longer than threshold)
     */
    @GetMapping("/sla-violations/{entityType}")
    @Timed(value = "workflow.monitoring.sla_violations", description = "Time to get SLA violations")
    public ResponseEntity<List<SLAViolation>> getSLAViolations(
            @PathVariable String entityType,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "60") int slaMinutes) {

        List<SLAViolation> violations = monitoringService.getSLAViolations(entityType, tenantId, slaMinutes);
        return ResponseEntity.ok(violations);
    }

    /**
     * Get hourly throughput (instances completed per hour)
     */
    @GetMapping("/throughput/{entityType}")
    @Timed(value = "workflow.monitoring.throughput", description = "Time to get throughput metrics")
    public ResponseEntity<List<ThroughputMetric>> getHourlyThroughput(
            @PathVariable String entityType,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "24") int hours) {

        List<ThroughputMetric> throughput = monitoringService.getHourlyThroughput(entityType, tenantId, hours);
        return ResponseEntity.ok(throughput);
    }

    /**
     * Get error rate by state
     */
    @GetMapping("/error-rate/{entityType}")
    @Timed(value = "workflow.monitoring.error_rate", description = "Time to get error rate by state")
    public ResponseEntity<List<StateErrorRate>> getErrorRateByState(
            @PathVariable String entityType,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "24") int hours) {

        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<StateErrorRate> errorRates = monitoringService.getErrorRateByState(entityType, tenantId, since);
        return ResponseEntity.ok(errorRates);
    }

    /**
     * Get overall workflow health dashboard
     */
    @GetMapping("/health/{entityType}")
    @Timed(value = "workflow.monitoring.health", description = "Time to get workflow health")
    public ResponseEntity<WorkflowHealth> getWorkflowHealth(
            @PathVariable String entityType,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        WorkflowHealth health = monitoringService.getWorkflowHealth(entityType, tenantId);
        return ResponseEntity.ok(health);
    }
}
