package cz.muriel.core.streaming.metrics;

import cz.muriel.core.streaming.repository.CommandQueueRepository;
import cz.muriel.core.streaming.repository.OutboxFinalRepository;
import cz.muriel.core.streaming.repository.WorkStateRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 📊 Streaming Metrics Collector
 * 
 * Exports metrics for monitoring streaming infrastructure health
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "streaming.enabled", havingValue = "true")
public class StreamingMetrics {

    private final MeterRegistry meterRegistry;
    private final CommandQueueRepository commandQueueRepository;
    private final OutboxFinalRepository outboxFinalRepository;
    private final WorkStateRepository workStateRepository;

    public StreamingMetrics(
            MeterRegistry meterRegistry,
            CommandQueueRepository commandQueueRepository,
            OutboxFinalRepository outboxFinalRepository,
            WorkStateRepository workStateRepository) {
        this.meterRegistry = meterRegistry;
        this.commandQueueRepository = commandQueueRepository;
        this.outboxFinalRepository = outboxFinalRepository;
        this.workStateRepository = workStateRepository;

        // Register gauges for queue depths
        registerGauges();
    }

    /**
     * Register gauges that are automatically polled
     */
    private void registerGauges() {
        // Command queue depth by priority
        List<String> priorities = Arrays.asList("critical", "high", "normal", "bulk");
        for (String priority : priorities) {
            meterRegistry.gauge("core.stream.cmd.queue.depth",
                Tags.of("priority", priority),
                commandQueueRepository,
                repo -> repo.countPendingByPriority(priority)
            );
        }

        // Outbox unsent total
        meterRegistry.gauge("core.stream.outbox.unsent.total",
            outboxFinalRepository,
            OutboxFinalRepository::countUnsent
        );

        // Work state counts
        meterRegistry.gauge("core.stream.workstate.updating",
            Tags.of("status", "updating"),
            workStateRepository,
            repo -> repo.countByStatus("updating")
        );

        meterRegistry.gauge("core.stream.workstate.error",
            Tags.of("status", "error"),
            workStateRepository,
            repo -> repo.countByStatus("error")
        );
    }

    /**
     * Record command processing success
     */
    public void recordWorkerSuccess(String entity, String priority) {
        meterRegistry.counter("core.stream.worker.success.total",
            "entity", entity,
            "priority", priority
        ).increment();
    }

    /**
     * Record command processing error
     */
    public void recordWorkerError(String entity, String priority, String errorType) {
        meterRegistry.counter("core.stream.worker.error.total",
            "entity", entity,
            "priority", priority,
            "error_type", errorType
        ).increment();
    }

    /**
     * Record DLQ event
     */
    public void recordDLQ(String entity, String source) {
        meterRegistry.counter("core.stream.dlq.total",
            "entity", entity,
            "source", source // "worker" or "dispatcher"
        ).increment();
    }

    /**
     * Record dispatcher publish success
     */
    public void recordDispatchSuccess(String entity) {
        meterRegistry.counter("core.stream.dispatch.published.total",
            "entity", entity
        ).increment();
    }

    /**
     * Record dispatcher publish error
     */
    public void recordDispatchError(String entity, String errorType) {
        meterRegistry.counter("core.stream.dispatch.error.total",
            "entity", entity,
            "error_type", errorType
        ).increment();
    }

    /**
     * Record quota throttle event
     */
    public void recordQuotaThrottle(String entity, String priority) {
        meterRegistry.counter("core.stream.quota.throttle.total",
            "entity", entity,
            "priority", priority
        ).increment();
    }

    /**
     * Record lock expiry
     */
    public void recordLockExpiring() {
        meterRegistry.counter("core.stream.locks.expiring.total").increment();
    }

    /**
     * Record latency from command accepted to applied
     */
    public void recordLatency(String entity, long latencyMs) {
        meterRegistry.timer("core.stream.latency.accepted.applied.seconds",
            "entity", entity
        ).record(java.time.Duration.ofMillis(latencyMs));
    }

    /**
     * Record current inflight operations
     */
    public void recordInflightCurrent(String entity, int count) {
        meterRegistry.gauge("core.stream.inflight.current",
            Tags.of("entity", entity),
            count
        );
    }

    /**
     * Scheduled: Update periodic metrics
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void updateMetrics() {
        try {
            // Queue depth total (all priorities)
            long totalPending = commandQueueRepository.countByStatus("pending");
            meterRegistry.gauge("core.stream.cmd.queue.total", totalPending);

            // Command queue by status
            meterRegistry.gauge("core.stream.cmd.queue.processing",
                commandQueueRepository.countByStatus("processing")
            );
            meterRegistry.gauge("core.stream.cmd.queue.failed",
                commandQueueRepository.countByStatus("failed")
            );
            meterRegistry.gauge("core.stream.cmd.queue.dlq",
                commandQueueRepository.countByStatus("dlq")
            );

        } catch (Exception e) {
            log.warn("Failed to update streaming metrics", e);
        }
    }
}
