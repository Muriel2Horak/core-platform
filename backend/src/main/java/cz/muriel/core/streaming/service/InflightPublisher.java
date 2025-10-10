package cz.muriel.core.streaming.service;

import cz.muriel.core.config.StreamingConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * 🚀 Inflight Publisher
 * 
 * Publishes pre-event notifications to entity.inflight.{entity} topics
 * These messages indicate that an entity is being updated (short retention)
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "streaming.enabled", havingValue = "true")
public class InflightPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StreamingConfig streamingConfig;

    public InflightPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            StreamingConfig streamingConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.streamingConfig = streamingConfig;
    }

    /**
     * Publish inflight "updating" event when command is accepted
     * 
     * @param entity Entity name
     * @param entityId Entity ID
     * @param correlationId Correlation ID for tracing
     * @param operation Operation type (CREATE, UPDATE, DELETE)
     */
    public void publishUpdating(String entity, UUID entityId, UUID correlationId, String operation) {
        try {
            String topicName = buildInflightTopicName(entity);
            String key = buildPartitionKey(entity, entityId);
            String payload = buildInflightPayload(entity, entityId, correlationId, operation, "updating");

            kafkaTemplate.send(topicName, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("📤 Published inflight event: {} {} (correlation: {})", 
                            entity, operation, correlationId);
                    } else {
                        log.warn("Failed to publish inflight event for {}/{}: {}", 
                            entity, entityId, ex.getMessage());
                        // Note: Inflight events are best-effort, failures are logged but not critical
                    }
                });

        } catch (Exception e) {
            log.warn("Failed to publish inflight event", e);
            // Non-critical, continue processing
        }
    }

    /**
     * Publish inflight "completed" event when operation finishes
     */
    public void publishCompleted(String entity, UUID entityId, UUID correlationId, String operation) {
        try {
            String topicName = buildInflightTopicName(entity);
            String key = buildPartitionKey(entity, entityId);
            String payload = buildInflightPayload(entity, entityId, correlationId, operation, "completed");

            kafkaTemplate.send(topicName, key, payload);
            log.debug("📤 Published inflight completed: {} {}", entity, operation);

        } catch (Exception e) {
            log.warn("Failed to publish inflight completed event", e);
        }
    }

    /**
     * Publish inflight "failed" event when operation fails
     */
    public void publishFailed(String entity, UUID entityId, UUID correlationId, String operation, String error) {
        try {
            String topicName = buildInflightTopicName(entity);
            String key = buildPartitionKey(entity, entityId);
            String payload = buildInflightPayload(entity, entityId, correlationId, operation, "failed", error);

            kafkaTemplate.send(topicName, key, payload);
            log.debug("📤 Published inflight failed: {} {}", entity, operation);

        } catch (Exception e) {
            log.warn("Failed to publish inflight failed event", e);
        }
    }

    /**
     * Build inflight topic name
     */
    private String buildInflightTopicName(String entity) {
        String prefix = streamingConfig.getTopic().getPrefix();
        return String.format("%s.entity.inflight.%s", prefix, entity.toLowerCase());
    }

    /**
     * Build partition key for ordered delivery per entity
     */
    private String buildPartitionKey(String entity, UUID entityId) {
        return entity + "#" + entityId;
    }

    /**
     * Build inflight message payload
     */
    private String buildInflightPayload(
            String entity, 
            UUID entityId, 
            UUID correlationId, 
            String operation, 
            String status) {
        return buildInflightPayload(entity, entityId, correlationId, operation, status, null);
    }

    /**
     * Build inflight message payload with error
     */
    private String buildInflightPayload(
            String entity, 
            UUID entityId, 
            UUID correlationId, 
            String operation, 
            String status,
            String error) {
        
        StringBuilder json = new StringBuilder("{");
        json.append("\"entity\":\"").append(entity).append("\",");
        json.append("\"entity_id\":\"").append(entityId).append("\",");
        json.append("\"correlation_id\":\"").append(correlationId).append("\",");
        json.append("\"operation\":\"").append(operation).append("\",");
        json.append("\"status\":\"").append(status).append("\",");
        json.append("\"timestamp\":\"").append(Instant.now()).append("\"");
        
        if (error != null) {
            json.append(",\"error\":\"").append(error.replace("\"", "\\\"")).append("\"");
        }
        
        json.append("}");
        return json.toString();
    }
}
