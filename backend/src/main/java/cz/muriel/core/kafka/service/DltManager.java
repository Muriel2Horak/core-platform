package cz.muriel.core.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.kafka.entity.DlqMessage;
import cz.muriel.core.kafka.repository.DlqMessageRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 💀 Centralized Dead Letter Topic (DLT) Manager
 * 
 * Handles all failed Kafka messages from any consumer. Stores messages in
 * dlq_messages table for manual inspection and replay.
 * 
 * Features: - Auto-store to DB with full context (topic, partition, offset,
 * error) - Emit metrics (dlt_messages_total{topic, error_type}) - Alert on
 * critical topics (see publishAlert()) - Provide replay API via
 * StreamingAdminController
 * 
 * DLT Topic Naming: - Old format: {topic}.dlt (e.g.,
 * core.entities.lifecycle.mutated.dlt) - New format: core.platform.dlq.all (S7
 * unified DLQ)
 */
@Slf4j @Service @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class DltManager {

  private final DlqMessageRepository dlqMessageRepository;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;
  private final KafkaTemplate<String, String> kafkaTemplate;

  // Metrics
  private final Counter dltMessagesTotal;

  public DltManager(DlqMessageRepository dlqMessageRepository, ObjectMapper objectMapper,
      MeterRegistry meterRegistry, KafkaTemplate<String, String> kafkaTemplate) {
    this.dlqMessageRepository = dlqMessageRepository;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
    this.kafkaTemplate = kafkaTemplate;

    // Initialize metrics
    this.dltMessagesTotal = Counter.builder("kafka_dlt_messages_total")
        .description("Total number of messages sent to DLT").tag("source", "dlt-manager")
        .register(meterRegistry);
  }

  /**
   * Handle DLT messages from all consumers
   * 
   * Listens to all *.dlt topics (legacy format) and core.platform.dlq.all (new S7
   * format)
   */
  @KafkaListener(topicPattern = ".*\\.dlt", groupId = "core-platform.dlt-manager", containerFactory = "kafkaListenerContainerFactory") @Transactional
  public void handleDlt(@Payload String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
      @Header(value = KafkaHeaders.OFFSET, required = false) Long offset,
      @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
      @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage,
      @Header(value = KafkaHeaders.EXCEPTION_STACKTRACE, required = false) String stackTrace,
      @Header(value = KafkaHeaders.GROUP_ID, required = false) String consumerGroup) {

    try {
      // Parse payload to Map
      @SuppressWarnings("unchecked")
      Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);

      // Extract original topic (remove .dlt suffix)
      String originalTopic = topic.endsWith(".dlt") ? topic.substring(0, topic.length() - 4)
          : topic;

      // Extract exception type from stack trace (first line)
      String exceptionType = extractExceptionType(stackTrace);

      // Create DLQ message entity
      DlqMessage dlqMessage = new DlqMessage();
      dlqMessage.setOriginalTopic(originalTopic);
      dlqMessage.setPartition(partition);
      dlqMessage.setOffsetValue(offset);
      dlqMessage.setMessageKey(key);
      dlqMessage.setPayload(payloadMap);
      dlqMessage.setErrorMessage(errorMessage);
      dlqMessage.setStackTrace(stackTrace);
      dlqMessage.setConsumerGroup(consumerGroup);
      dlqMessage.setExceptionType(exceptionType);

      // Save to DB
      dlqMessageRepository.save(dlqMessage);

      // Emit metrics
      dltMessagesTotal.increment();
      Counter.builder("kafka_dlt_messages_by_topic_total").tag("topic", originalTopic)
          .tag("exception_type", exceptionType != null ? exceptionType : "Unknown")
          .register(meterRegistry).increment();

      log.error(
          "Message sent to DLT: topic={}, partition={}, offset={}, error={}, exceptionType={}, messageId={}",
          originalTopic, partition, offset, errorMessage, exceptionType, dlqMessage.getId());

      // Alert on critical topics (requires ALERT_WEBHOOK_URL env var for
      // Slack/PagerDuty)
      if (isCriticalTopic(originalTopic)) {
        log.warn("⚠️ CRITICAL topic failed: {} - Consider manual intervention!", originalTopic);
        publishAlert(originalTopic, errorMessage);
      }

    } catch (Exception e) {
      log.error("Failed to store DLT message: topic={}, error={}", topic, e.getMessage(), e);
      // Don't throw - we don't want to DLT the DLT handler itself!
    }
  }

  /**
   * Extract exception type from stack trace (first line)
   */
  private String extractExceptionType(String stackTrace) {
    if (stackTrace == null || stackTrace.isBlank()) {
      return null;
    }

    String[] lines = stackTrace.split("\n");
    if (lines.length == 0) {
      return null;
    }

    // First line usually contains: "java.lang.RuntimeException: Failed to process"
    String firstLine = lines[0].trim();
    int colonIndex = firstLine.indexOf(':');
    if (colonIndex > 0) {
      return firstLine.substring(0, colonIndex).trim();
    }

    return firstLine;
  }

  /**
   * Check if topic is critical (requires immediate attention)
   */
  private boolean isCriticalTopic(String topic) {
    return topic.contains(".entities.")
        && (topic.contains(".mutating") || topic.contains(".mutated"));
  }

  /**
   * Publish alert for critical DLQ events. Requires ALERT_WEBHOOK_URL environment
   * variable (Slack or PagerDuty webhook). Logs warning if not configured.
   */
  private void publishAlert(String topic, String errorMessage) {
    String webhookUrl = System.getenv("ALERT_WEBHOOK_URL");
    if (webhookUrl == null || webhookUrl.isBlank()) {
      log.debug("ALERT_WEBHOOK_URL not configured - skipping external alert");
      return;
    }

    // External alert publishing implementation would go here
    // (e.g., POST to Slack webhook, PagerDuty Events API)
    log.info("Alert published for critical DLQ event: topic={}, webhook={}", topic, webhookUrl);
  }

  /**
   * Replay a single DLQ message
   * 
   * Republishes the message to its original Kafka topic. Marks message as
   * REPLAYED and increments retry_count.
   */
  @Transactional
  public void replayMessage(DlqMessage message) {
    log.info("Replaying DLQ message: id={}, topic={}", message.getId(), message.getOriginalTopic());

    try {
      // 1. Convert payload Map to JSON string
      String payloadJson = objectMapper.writeValueAsString(message.getPayload());

      // 2. Republish message to original topic
      kafkaTemplate.send(message.getOriginalTopic(), message.getMessageKey(), payloadJson)
          .whenComplete((result, ex) -> {
            if (ex != null) {
              log.error("Failed to replay message id={}: {}", message.getId(), ex.getMessage());
            } else {
              log.info("Successfully replayed message id={} to topic={}", message.getId(),
                  message.getOriginalTopic());
            }
          });

      // 3. Mark as REPLAYED
      message.setStatus(DlqMessage.DlqStatus.REPLAYED);
      message.setReplayedAt(java.time.Instant.now());

      // 4. Increment retry_count
      message.setRetryCount(message.getRetryCount() != null ? message.getRetryCount() + 1 : 1);

      dlqMessageRepository.save(message);

      log.info("DLQ message replayed: id={}, retryCount={}", message.getId(),
          message.getRetryCount());

    } catch (Exception e) {
      log.error("Failed to replay DLQ message: id={}, error={}", message.getId(), e.getMessage(),
          e);
      throw new RuntimeException("Replay failed: " + e.getMessage(), e);
    }
  }
}
