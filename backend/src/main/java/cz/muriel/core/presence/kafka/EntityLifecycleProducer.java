package cz.muriel.core.presence.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Producer for entity lifecycle events
 * 
 * Topics:
 * - core.entities.lifecycle.mutating → Entity is about to be modified (WRITE pipeline started)
 * - core.entities.lifecycle.mutated → Entity modification completed (WRITE pipeline finished)
 * 
 * Event Schema:
 * {
 *   "eventType": "MUTATING" | "MUTATED",
 *   "tenantId": "t1",
 *   "entity": "Order",
 *   "id": "123",
 *   "userId": "user-456",
 *   "timestamp": 1697123456789,
 *   "version": 5  // only for MUTATED
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class EntityLifecycleProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  private static final String TOPIC_MUTATING = "core.entities.lifecycle.mutating";
  private static final String TOPIC_MUTATED = "core.entities.lifecycle.mutated";

  /**
   * Publish MUTATING event (entity is about to be modified)
   * 
   * This should be called BEFORE starting write pipeline
   */
  public void publishMutating(String tenantId, String entity, String id, String userId) {
    Map<String, Object> event = new HashMap<>();
    event.put("eventType", "MUTATING");
    event.put("tenantId", tenantId);
    event.put("entity", entity);
    event.put("id", id);
    event.put("userId", userId);
    event.put("timestamp", System.currentTimeMillis());

    String key = buildKey(tenantId, entity, id);
    kafkaTemplate.send(TOPIC_MUTATING, key, event);

    log.info("Published MUTATING event: {}:{} by user {}", entity, id, userId);
  }

  /**
   * Publish MUTATED event (entity modification completed)
   * 
   * This should be called AFTER write pipeline completes successfully
   */
  public void publishMutated(String tenantId, String entity, String id, String userId, long version) {
    Map<String, Object> event = new HashMap<>();
    event.put("eventType", "MUTATED");
    event.put("tenantId", tenantId);
    event.put("entity", entity);
    event.put("id", id);
    event.put("userId", userId);
    event.put("timestamp", System.currentTimeMillis());
    event.put("version", version);

    String key = buildKey(tenantId, entity, id);
    kafkaTemplate.send(TOPIC_MUTATED, key, event);

    log.info("Published MUTATED event: {}:{} version {} by user {}", entity, id, version, userId);
  }

  private String buildKey(String tenantId, String entity, String id) {
    return String.format("%s:%s:%s", tenantId, entity, id);
  }
}
