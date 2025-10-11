package cz.muriel.core.presence.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.presence.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Consumer for entity lifecycle events
 * 
 * Responsibilities: - On MUTATING: Mark entity as "stale" in Redis (prevent
 * concurrent edits) - On MUTATED: Clear "stale" flag, increment version in
 * Redis - Broadcast updates via WebSocket (handled by notification service)
 * 
 * Retry Strategy: - 3 retries with exponential backoff (1s, 3s, 9s) - Dead
 * Letter Queue (DLQ) for failures
 */
@Slf4j @Component @RequiredArgsConstructor @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class EntityLifecycleConsumer {

  private final PresenceService presenceService;
  private final ObjectMapper objectMapper;

  @RetryableTopic(attempts = "4", // 1 original + 3 retries
      backoff = @Backoff(delay = 1000, multiplier = 3.0), kafkaTemplate = "kafkaTemplate", dltTopicSuffix = ".dlt", include = Exception.class) @KafkaListener(topics = "core.entities.lifecycle.mutating", groupId = "presence-service", containerFactory = "kafkaListenerContainerFactory")
  public void consumeMutating(@Payload String payload,
      @Header(KafkaHeaders.RECEIVED_KEY) String key, @Header(KafkaHeaders.OFFSET) long offset) {
    try {
      JsonNode event = objectMapper.readTree(payload);

      String tenantId = event.get("tenantId").asText();
      String entity = event.get("entity").asText();
      String id = event.get("id").asText();
      String userId = event.get("userId").asText();

      // Mark entity as stale (being modified)
      presenceService.setStale(tenantId, entity, id, true, userId);

      log.info("Processed MUTATING event: {}:{} by {} (offset: {})", entity, id, userId, offset);

    } catch (Exception e) {
      log.error("Error processing MUTATING event: key={}, offset={}", key, offset, e);
      throw new RuntimeException("Failed to process MUTATING event", e);
    }
  }

  @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 3.0), kafkaTemplate = "kafkaTemplate", dltTopicSuffix = ".dlt", include = Exception.class) @KafkaListener(topics = "core.entities.lifecycle.mutated", groupId = "presence-service", containerFactory = "kafkaListenerContainerFactory")
  public void consumeMutated(@Payload String payload, @Header(KafkaHeaders.RECEIVED_KEY) String key,
      @Header(KafkaHeaders.OFFSET) long offset) {
    try {
      JsonNode event = objectMapper.readTree(payload);

      String tenantId = event.get("tenantId").asText();
      String entity = event.get("entity").asText();
      String id = event.get("id").asText();
      String userId = event.get("userId").asText();
      long version = event.has("version") ? event.get("version").asLong() : 0;

      // Clear stale flag
      presenceService.setStale(tenantId, entity, id, false, null);

      // Increment version
      presenceService.incrementVersion(tenantId, entity, id);

      log.info("Processed MUTATED event: {}:{} version {} by {} (offset: {})", entity, id, version,
          userId, offset);

    } catch (Exception e) {
      log.error("Error processing MUTATED event: key={}, offset={}", key, offset, e);
      throw new RuntimeException("Failed to process MUTATED event", e);
    }
  }

  /**
   * Dead Letter Queue handler for failed events
   */
  @KafkaListener(topics = { "core.entities.lifecycle.mutating.dlt",
      "core.entities.lifecycle.mutated.dlt" }, groupId = "presence-service-dlq")
  public void handleDlt(@Payload String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
    log.error("Event sent to DLQ: topic={}, error={}, payload={}", topic, errorMessage, payload);

    // TODO: Implement alerting/monitoring for DLQ events
    // TODO: Consider storing in DB for manual replay
  }
}
