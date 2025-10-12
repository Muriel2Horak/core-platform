package cz.muriel.core.streaming.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.streaming.metrics.StreamingMetrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 📥 Event Consumer
 * 
 * Consumes events from entity.events.* topics. Provides basic event processing
 * with JSON parsing and metrics. Extend this for specific business logic: -
 * Real-time notifications/webhooks - Analytics/reporting pipelines - Search
 * index updates - Cache invalidation
 */
@Slf4j @Component @ConditionalOnProperty(name = "streaming.enabled", havingValue = "true")
public class EventConsumer {

  private final ObjectMapper objectMapper;
  private final StreamingMetrics metrics;

  @Autowired
  public EventConsumer(ObjectMapper objectMapper, StreamingMetrics metrics) {
    this.objectMapper = objectMapper;
    this.metrics = metrics;
  }

  /**
   * Consume User events from entity.events.user topic
   * 
   * Processes final committed events after outbox dispatch. Parses JSON payload
   * and extracts metadata for downstream processing.
   */
  @KafkaListener(topics = "#{@streamingConfig.getTopic().getPrefix() + '.entity.events.user'}", groupId = "core-platform-events", concurrency = "2", autoStartup = "${streaming.consumer.auto-start:false}")
  public void consumeUserEvents(ConsumerRecord<String, String> record, Acknowledgment ack) {
    long startTime = System.currentTimeMillis();

    try {
      log.info("📩 Received User event: key={}, partition={}, offset={}", record.key(),
          record.partition(), record.offset());

      // ✅ Parse JSON payload
      JsonNode payload = objectMapper.readTree(record.value());

      String eventType = payload.path("eventType").asText("UNKNOWN");
      String entityId = payload.path("entityId").asText();
      String operation = payload.path("operation").asText("UNKNOWN");

      log.debug("Event details: type={}, entity={}, operation={}", eventType, entityId, operation);
      log.debug("Payload: {}", record.value());

      // ✅ Record metrics
      metrics.recordWorkerSuccess("User", "normal");

      // ✅ Apply business logic extensions
      applyBusinessLogic(eventType, entityId, operation, record.value());

      // Acknowledge message
      ack.acknowledge();

      long duration = System.currentTimeMillis() - startTime;
      log.debug("✅ User event processed in {}ms", duration);

    } catch (Exception e) {
      log.error("Failed to process User event at offset {}: {}", record.offset(), e.getMessage(),
          e);
      metrics.recordWorkerError("User", "normal", "processing_error");

      // Don't acknowledge - will be retried
      throw new RuntimeException("Failed to process User event", e);
    }
  }

  /**
   * Consume Inflight Command events from entity.events.inflight topic
   * 
   * Tracks in-progress commands for monitoring dashboards. Parses JSON to extract
   * command status and timing information.
   */
  @KafkaListener(topics = "#{@streamingConfig.getTopic().getPrefix() + '.entity.events.inflight'}", groupId = "core-platform-events", concurrency = "2", autoStartup = "${streaming.consumer.auto-start:false}")
  public void consumeInflightEvents(ConsumerRecord<String, String> record, Acknowledgment ack) {
    long startTime = System.currentTimeMillis();

    try {
      log.info("📩 Received Inflight event: key={}, partition={}, offset={}", record.key(),
          record.partition(), record.offset());

      // ✅ Parse JSON payload
      JsonNode payload = objectMapper.readTree(record.value());

      String commandId = payload.path("commandId").asText();
      String status = payload.path("status").asText("UNKNOWN");
      String operation = payload.path("operation").asText();

      log.debug("Inflight command: id={}, status={}, operation={}", commandId, status, operation);
      log.debug("Payload: {}", record.value());

      // ✅ Record metrics
      metrics.recordWorkerSuccess("Inflight", "normal");

      // ✅ Apply monitoring logic extensions
      applyMonitoringLogic(commandId, status, operation, record.value());

      // Acknowledge message
      ack.acknowledge();

      long duration = System.currentTimeMillis() - startTime;
      log.debug("✅ Inflight event processed in {}ms", duration);

    } catch (Exception e) {
      log.error("Failed to process Inflight event at offset {}: {}", record.offset(),
          e.getMessage(), e);
      metrics.recordWorkerError("Inflight", "normal", "processing_error");

      // Don't acknowledge - will be retried
      throw new RuntimeException("Failed to process Inflight event", e);
    }
  }

  /**
   * Consume DLQ (Dead Letter Queue) events from entity.events.dlq topic
   * 
   * Processes failed commands that exceeded retry limits. Parses JSON to extract
   * failure metadata for alerting and manual intervention.
   */
  @KafkaListener(topics = "#{@streamingConfig.getTopic().getPrefix() + '.entity.events.dlq'}", groupId = "core-platform-events-dlq", concurrency = "1", autoStartup = "${streaming.consumer.auto-start:false}")
  public void consumeDLQEvents(ConsumerRecord<String, String> record, Acknowledgment ack) {
    long startTime = System.currentTimeMillis();

    try {
      log.warn("⚠️ Received DLQ event: key={}, partition={}, offset={}", record.key(),
          record.partition(), record.offset());

      // ✅ Parse JSON payload
      JsonNode payload = objectMapper.readTree(record.value());

      String commandId = payload.path("commandId").asText();
      String errorMessage = payload.path("error").asText("Unknown error");
      int retryCount = payload.path("retryCount").asInt(0);
      String entityType = payload.path("entityType").asText("UNKNOWN");

      log.error("DLQ command failed: id={}, retries={}, type={}, error={}", commandId, retryCount,
          entityType, errorMessage);
      log.error("Failed payload: {}", record.value());

      // ✅ Record metrics
      metrics.recordWorkerError(entityType, "dlq", "max_retries_exceeded");

      // Note: DLT storage/alerting is handled by centralized DltManager

      // Acknowledge message even for DLQ (prevent infinite loops)
      ack.acknowledge();

      long duration = System.currentTimeMillis() - startTime;
      log.warn("⚠️ DLQ event logged in {}ms - manual intervention may be required", duration);

    } catch (Exception e) {
      log.error("Failed to process DLQ event at offset {}: {}", record.offset(), e.getMessage(), e);
      metrics.recordWorkerError("DLQ", "dlq", "dlq_processing_error");

      // Acknowledge even on error to prevent infinite DLQ loops
      ack.acknowledge();
    }
  }

  /**
   * Extension point for business logic processing. Override this method to add
   * custom processing (search indexing, webhooks, notifications, etc.)
   * 
   * @param eventType Entity type (e.g., "User", "Order")
   * @param entityId Entity ID
   * @param operation CRUD operation (create, update, delete)
   * @param payload Raw JSON payload
   */
  protected void applyBusinessLogic(String eventType, String entityId, String operation,
      String payload) {
    // Default: No-op (extension point for subclasses)
    // Future implementations:
    // - Update search indexes (Elasticsearch/OpenSearch)
    // - Trigger webhooks to external systems
    // - Send notifications (email/SMS/push)
    // - Invalidate caches (Redis)
    // - Update analytics databases
    log.trace("Business logic extension point - eventType={}, entityId={}, operation={}", eventType,
        entityId, operation);
  }

  /**
   * Extension point for monitoring/observability logic. Override this method to
   * add custom monitoring (Grafana dashboards, custom metrics, etc.)
   * 
   * @param commandId Command ID
   * @param status Command status (pending, success, failed)
   * @param operation Operation name
   * @param payload Raw JSON payload
   */
  protected void applyMonitoringLogic(String commandId, String status, String operation,
      String payload) {
    // Default: No-op (extension point for subclasses)
    // Future implementations:
    // - Update Grafana dashboards with command status
    // - Track command duration/latency in custom metrics
    // - Alert on stuck commands (>threshold time)
    // - Update command registry/state machine
    log.trace("Monitoring logic extension point - commandId={}, status={}, operation={}", commandId,
        status, operation);
  }
}
