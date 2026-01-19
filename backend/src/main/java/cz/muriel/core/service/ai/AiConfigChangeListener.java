package cz.muriel.core.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.dto.ConfigChangeEvent;
import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * Kafka listener for AI config change events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "streaming.enabled", havingValue = "true", matchIfMissing = false)
public class AiConfigChangeListener {

  private final ObjectMapper objectMapper;
  private final GlobalMetamodelConfig globalConfig;
  private final MetamodelRegistry metamodelRegistry;

  @KafkaListener(topics = "platform.config.changes", groupId = "core-platform.ai-config",
      containerFactory = "kafkaListenerContainerFactory")
  public void onConfigChange(String payload, Acknowledgment ack) {
    try {
      ConfigChangeEvent event = objectMapper.readValue(payload, ConfigChangeEvent.class);
      if (!"AI_CONFIG_CHANGED".equals(event.eventType())) {
        return;
      }

      if (event.config() == null) {
        log.warn("AI config change event missing config payload");
        return;
      }

      globalConfig.setAi(event.config());
      metamodelRegistry.reload();
      log.info("✅ Applied AI config change event: {}", event.eventId());
    } catch (Exception e) {
      log.error("❌ Failed to process AI config change event", e);
    } finally {
      if (ack != null) {
        ack.acknowledge();
      }
    }
  }
}
