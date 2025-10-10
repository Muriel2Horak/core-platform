package cz.muriel.core.streaming.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 📥 Event Consumer (Example)
 * 
 * Consumes events from entity.events.* topics
 * This is an example consumer - implement actual business logic as needed
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "streaming.enabled", havingValue = "true")
public class EventConsumer {

    /**
     * Example: Consume User events
     * 
     * In production, you might have different consumers for:
     * - Real-time notifications/webhooks
     * - Analytics/reporting pipelines
     * - Search index updates
     * - Cache invalidation
     */
    @KafkaListener(
        topics = "#{@streamingConfig.getTopic().getPrefix() + '.entity.events.user'}",
        groupId = "core-platform-events",
        concurrency = "2",
        autoStartup = "${streaming.consumer.auto-start:false}" // Disabled by default
    )
    public void consumeUserEvents(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.info("📩 Received User event: key={}, partition={}, offset={}", 
                record.key(), record.partition(), record.offset());
            log.debug("Payload: {}", record.value());

            // TODO: Implement actual event processing
            // - Parse JSON payload
            // - Update search indexes
            // - Trigger webhooks
            // - Send notifications
            // - Update caches
            
            // Acknowledge message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process User event at offset {}: {}", 
                record.offset(), e.getMessage(), e);
            // Don't acknowledge - will be retried
            throw e;
        }
    }

    /**
     * Example: Consume inflight events for monitoring
     */
    @KafkaListener(
        topics = "#{@streamingConfig.getTopic().getPrefix() + '.entity.inflight.user'}",
        groupId = "core-platform-inflight",
        concurrency = "1",
        autoStartup = "${streaming.consumer.auto-start:false}" // Disabled by default
    )
    public void consumeInflightEvents(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.debug("📍 Inflight event: {}", record.value());
            
            // TODO: Implement inflight event processing
            // - Track in-progress operations
            // - Update real-time dashboards
            // - Monitor operation latency
            
            ack.acknowledge();

        } catch (Exception e) {
            log.warn("Failed to process inflight event: {}", e.getMessage());
            ack.acknowledge(); // Acknowledge anyway - inflight events are best-effort
        }
    }

    /**
     * Consume DLQ messages for alerting
     */
    @KafkaListener(
        topics = "#{@streamingConfig.getTopic().getPrefix() + '.dlq.events'}",
        groupId = "core-platform-dlq",
        concurrency = "1",
        autoStartup = "${streaming.consumer.auto-start:false}"
    )
    public void consumeDLQEvents(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.error("🚨 DLQ message received: key={}, payload={}", 
                record.key(), record.value());
            
            // TODO: Implement DLQ handling
            // - Send alerts to ops team
            // - Store in database for manual inspection
            // - Attempt automatic recovery if possible
            
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process DLQ message: {}", e.getMessage(), e);
            ack.acknowledge(); // Always acknowledge DLQ to avoid infinite loops
        }
    }
}
