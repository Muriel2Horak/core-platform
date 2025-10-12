package cz.muriel.core.reporting.preagg;

import cz.muriel.core.kafka.annotation.HighPriorityRetry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka consumer for triggering Cube.js pre-aggregation refresh on entity
 * mutations.
 * 
 * <p>
 * Listens to entity lifecycle events and triggers pre-aggregation refresh for
 * affected Cube.js schemas. Uses debouncing to avoid excessive refreshes.
 * 
 * <p>
 * Configuration:
 * <ul>
 * <li>app.cube.preagg.debounceMs - Debounce window (default: 30000ms =
 * 30s)</li>
 * <li>app.cube.preagg.enabled - Enable/disable worker (default: true)</li>
 * </ul>
 * 
 * <p>
 * Retry Strategy (S7): @HighPriorityRetry - 4 attempts, 2s→30s max - Total:
 * ~30s
 * 
 * @see cz.muriel.core.reporting.preagg.CubePreAggService
 */
@Slf4j @Service
public class PreAggRefreshWorker {

  private final CubePreAggService cubePreAggService;
  private final boolean enabled;
  private final long debounceMs;

  // Debounce tracking: entityType -> lastRefreshTimestamp
  private final Map<String, Long> lastRefreshTimes = new ConcurrentHashMap<>();

  public PreAggRefreshWorker(CubePreAggService cubePreAggService,
      @Value("${app.cube.preagg.enabled:true}") boolean enabled,
      @Value("${app.cube.preagg.debounceMs:30000}") long debounceMs) {
    this.cubePreAggService = cubePreAggService;
    this.enabled = enabled;
    this.debounceMs = debounceMs;

    log.info("PreAggRefreshWorker initialized: enabled={}, debounceMs={}", enabled, debounceMs);
  }

  /**
   * Handle entity mutation events.
   * 
   * <p>
   * Kafka message format:
   * 
   * <pre>
   * {
   *   "eventType": "ENTITY_CREATED" | "ENTITY_UPDATED" | "ENTITY_DELETED",
   *   "entityType": "User",
   *   "entityId": "123",
   *   "tenantId": "tenant-1",
   *   "timestamp": 1234567890,
   *   "metadata": { ... }
   * }
   * </pre>
   */
  @HighPriorityRetry @KafkaListener(topics = "core.entities.lifecycle.mutated", groupId = "core-platform.reporting-preagg", containerFactory = "kafkaListenerContainerFactory")
  public void handleEntityMutation(Map<String, Object> event, Acknowledgment ack) {
    if (!enabled) {
      log.debug("PreAggRefreshWorker disabled, skipping event");
      ack.acknowledge();
      return;
    }

    try {
      String eventType = (String) event.get("eventType");
      String entityType = (String) event.get("entityType");
      String entityId = String.valueOf(event.get("entityId"));
      String tenantId = (String) event.get("tenantId");

      log.debug("Received entity mutation: type={}, entity={}/{}, tenant={}", eventType, entityType,
          entityId, tenantId);

      // Check debounce window
      if (shouldDebounce(entityType)) {
        log.debug("Debouncing pre-agg refresh for entityType={} (last refresh was <{}ms ago)",
            entityType, debounceMs);
        ack.acknowledge();
        return;
      }

      // Trigger pre-aggregation refresh for affected cube schema
      boolean refreshed = cubePreAggService.refreshForEntityType(entityType, tenantId);

      if (refreshed) {
        lastRefreshTimes.put(entityType, System.currentTimeMillis());
        log.info("Pre-aggregation refresh triggered: entityType={}, tenant={}", entityType,
            tenantId);
      } else {
        log.debug("No pre-aggregation refresh needed for entityType={}", entityType);
      }

      ack.acknowledge();

    } catch (Exception e) {
      log.error("Failed to process entity mutation event: {}", event, e);
      throw e; // Trigger retry via @HighPriorityRetry
    }
  }

  /**
   * Check if entity type should be debounced.
   * 
   * @param entityType Entity type (e.g., "User", "Tenant")
   * @return true if last refresh was within debounce window
   */
  private boolean shouldDebounce(String entityType) {
    Long lastRefresh = lastRefreshTimes.get(entityType);
    if (lastRefresh == null) {
      return false;
    }

    long timeSinceLastRefresh = System.currentTimeMillis() - lastRefresh;
    return timeSinceLastRefresh < debounceMs;
  }

  /**
   * Handle DLT (Dead Letter Topic) messages.
   * 
   * <p>
   * These are messages that failed after all retry attempts.
   * 
   * <p>
   * S7 Phase 3: Will migrate to centralized DltManager
   */
  @KafkaListener(topics = "core.entities.lifecycle.mutated.dlt", groupId = "core-platform.reporting-preagg.dlq", containerFactory = "kafkaListenerContainerFactory")
  public void handleDlt(Map<String, Object> event, Acknowledgment ack) {
    log.error("Message sent to DLT after all retries exhausted: {}", event);
    // TODO (S7 Phase 3): Migrate to centralized DltManager
    ack.acknowledge();
  }

  /**
   * Get debounce statistics (for monitoring/health checks).
   */
  public Map<String, Long> getDebounceStats() {
    return Map.copyOf(lastRefreshTimes);
  }

  /**
   * Clear debounce cache (for testing/manual override).
   */
  public void clearDebounceCache() {
    lastRefreshTimes.clear();
    log.info("Debounce cache cleared");
  }
}
