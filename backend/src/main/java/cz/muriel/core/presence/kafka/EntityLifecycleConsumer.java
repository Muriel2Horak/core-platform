package cz.muriel.core.presence.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.kafka.annotation.CriticalRetry;
import cz.muriel.core.presence.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumer for entity lifecycle events
 * 
 * Responsibilities: - On MUTATING: Mark entity as "stale" in Redis (prevent
 * concurrent edits) - On MUTATED: Clear "stale" flag, increment version in
 * Redis - Broadcast updates via WebSocket (handled by notification service)
 * 
 * Retry Strategy (S7): @CriticalRetry - 5 attempts, 1s→60s max - Total: ~123s
 */
@Slf4j @Component @RequiredArgsConstructor @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class EntityLifecycleConsumer {

  private final PresenceService presenceService;
  private final ObjectMapper objectMapper;

  @CriticalRetry @KafkaListener(topics = "core.entities.lifecycle.mutating", groupId = "core-platform.presence-mutating", containerFactory = "kafkaListenerContainerFactory")
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

  @CriticalRetry @KafkaListener(topics = "core.entities.lifecycle.mutated", groupId = "core-platform.presence-mutated", containerFactory = "kafkaListenerContainerFactory")
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

  // Note: DLT handling is now centralized in DltManager
  // (listens to all *.dlt topics via @KafkaListener pattern)
}
