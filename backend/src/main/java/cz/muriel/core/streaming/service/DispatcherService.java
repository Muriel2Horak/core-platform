package cz.muriel.core.streaming.service;

import cz.muriel.core.config.StreamingConfig;
import cz.muriel.core.metamodel.MetamodelLoader;
import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.streaming.entity.OutboxFinal;
import cz.muriel.core.streaming.metrics.StreamingMetrics;
import cz.muriel.core.streaming.repository.OutboxFinalRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 📤 Dispatcher Service
 * 
 * Polls outbox_final and publishes events to Kafka. Uses transactional outbox
 * pattern for guaranteed delivery.
 */
@Slf4j @Service @ConditionalOnProperty(name = "streaming.enabled", havingValue = "true")
public class DispatcherService {

  private final OutboxFinalRepository outboxFinalRepository;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final StreamingConfig streamingConfig;
  private final MetamodelLoader metamodelLoader;
  private final StreamingMetrics metrics;

  private GlobalMetamodelConfig globalConfig;

  public DispatcherService(OutboxFinalRepository outboxFinalRepository,
      KafkaTemplate<String, String> kafkaTemplate, StreamingConfig streamingConfig,
      MetamodelLoader metamodelLoader, StreamingMetrics metrics) {
    this.outboxFinalRepository = outboxFinalRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.streamingConfig = streamingConfig;
    this.metamodelLoader = metamodelLoader;
    this.metrics = metrics;

    // Load global config
    this.globalConfig = metamodelLoader.loadGlobalConfig();
  }

  /**
   * Main dispatcher loop - polls and publishes messages
   */
  @Scheduled(fixedDelayString = "${streaming.dispatcher.poll-interval-ms:100}") @Transactional
  public void dispatchMessages() {
    if (!globalConfig.getStreaming().isEnabled()) {
      return;
    }

    try {
      int batchSize = 100; // TODO: Make configurable
      List<OutboxFinal> messages = outboxFinalRepository.fetchUnsentMessages(batchSize);

      if (messages.isEmpty()) {
        return;
      }

      log.debug("Dispatcher processing {} messages", messages.size());

      for (OutboxFinal message : messages) {
        publishMessage(message);
      }

    } catch (Exception e) {
      log.error("Dispatcher failed to process messages", e);
    }
  }

  /**
   * Publish a single message to Kafka
   */
  private void publishMessage(OutboxFinal message) {
    try {
      String topicName = buildTopicName(message);
      String key = buildPartitionKey(message);
      String payload = buildPayload(message);

      // Create producer record with headers
      ProducerRecord<String, String> record = new ProducerRecord<>(topicName, key, payload);

      // Add headers from message
      if (message.getHeadersJson() != null) {
        // TODO: Parse headers and add to record
      }

      // Send to Kafka (async with callback)
      CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);

      future.whenComplete((result, ex) -> {
        if (ex == null) {
          // Success - mark as sent
          markAsSent(message, result.getRecordMetadata());
          metrics.recordDispatchSuccess(message.getEntity());
        } else {
          // Failure - handle error
          metrics.recordDispatchError(message.getEntity(), ex.getClass().getSimpleName());
          handlePublishError(message, ex);
        }
      });

    } catch (Exception e) {
      handlePublishError(message, e);
    }
  }

  /**
   * Build Kafka topic name
   */
  private String buildTopicName(OutboxFinal message) {
    String prefix = streamingConfig.getTopic().getPrefix();
    String entityName = message.getEntity().toLowerCase();
    return String.format("%s.entity.events.%s", prefix, entityName);
  }

  /**
   * Build partition key for ordered delivery per entity
   */
  private String buildPartitionKey(OutboxFinal message) {
    return message.getEntity() + "#" + message.getEntityId();
  }

  /**
   * Build message payload
   */
  private String buildPayload(OutboxFinal message) {
    // TODO: Build proper JSON payload with diff/snapshot based on config
    // For now, return diff_json as-is
    return message.getDiffJson() != null ? message.getDiffJson() : "{}";
  }

  /**
   * Mark message as sent
   */
  @Transactional
  public void markAsSent(OutboxFinal message, RecordMetadata metadata) {
    try {
      message.setSentAt(Instant.now());
      outboxFinalRepository.save(message);

      log.info("✅ Published event {} to topic {} partition {} offset {}", message.getId(),
          metadata.topic(), metadata.partition(), metadata.offset());

    } catch (Exception e) {
      log.error("Failed to mark message {} as sent", message.getId(), e);
    }
  }

  /**
   * Handle publish error with retry logic
   */
  private void handlePublishError(OutboxFinal message, Throwable error) {
    log.error("Failed to publish message {}: {}", message.getId(), error.getMessage(), error);

    message.setRetryCount(message.getRetryCount() + 1);
    message.setErrorMessage(error.getMessage());

    int maxRetries = 3; // TODO: Make configurable
    if (message.getRetryCount() >= maxRetries) {
      // Move to DLQ
      metrics.recordDLQ(message.getEntity(), "dispatcher");
      publishToDLQ(message);
    }

    outboxFinalRepository.save(message);
  }

  /**
   * Publish to DLQ topic
   */
  private void publishToDLQ(OutboxFinal message) {
    try {
      String dlqTopic = streamingConfig.getTopic().getPrefix() + "."
          + globalConfig.getStreaming().getOutboxDlqTopic();

      String payload = String.format(
          "{\"original_message\":%s,\"error\":\"%s\",\"retry_count\":%d}", message.getDiffJson(),
          message.getErrorMessage(), message.getRetryCount());

      kafkaTemplate.send(dlqTopic, message.getId().toString(), payload);

      log.warn("Message {} moved to DLQ after {} retries", message.getId(),
          message.getRetryCount());

    } catch (Exception e) {
      log.error("Failed to publish to DLQ", e);
    }
  }
}
