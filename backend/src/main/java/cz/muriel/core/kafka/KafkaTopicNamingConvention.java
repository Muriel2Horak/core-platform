package cz.muriel.core.kafka;

import java.util.regex.Pattern;

/**
 * 🏷️ Kafka Topic Naming Convention Helper
 * 
 * Enforces standardized topic naming: core.<domain>.<entity>.<event-type>
 * 
 * Examples: - core.entities.user.mutating - core.entities.user.mutated -
 * core.entities.group.created - core.reporting.preagg.refresh-requested -
 * core.platform.dlq.all
 */
public class KafkaTopicNamingConvention {

  private static final String NAMESPACE = "core";
  private static final String DOMAIN_ENTITIES = "entities";
  private static final String DOMAIN_REPORTING = "reporting";
  private static final String DOMAIN_PLATFORM = "platform";

  // Topic name pattern: namespace.domain.entity.event-type (all lowercase, dots
  // and hyphens only)
  private static final Pattern VALID_TOPIC_PATTERN = Pattern
      .compile("^[a-z]+\\.[a-z]+\\.[a-z0-9-]+\\.[a-z0-9-]+$");

  /**
   * Generate entity lifecycle topic name
   * 
   * @param entity Entity name (e.g., "user", "group")
   * @param eventType Event type (e.g., "mutating", "mutated", "created",
   * "deleted")
   * @return Topic name: core.entities.{entity}.{eventType}
   */
  public static String entityLifecycleTopic(String entity, String eventType) {
    validate(entity, "entity");
    validate(eventType, "eventType");
    return String.format("%s.%s.%s.%s", NAMESPACE, DOMAIN_ENTITIES, entity.toLowerCase(),
        eventType.toLowerCase());
  }

  /**
   * Generate reporting topic name
   * 
   * @param entity Entity name (e.g., "user", "preagg")
   * @param eventType Event type (e.g., "refresh-requested", "completed")
   * @return Topic name: core.reporting.{entity}.{eventType}
   */
  public static String reportingTopic(String entity, String eventType) {
    validate(entity, "entity");
    validate(eventType, "eventType");
    return String.format("%s.%s.%s.%s", NAMESPACE, DOMAIN_REPORTING, entity.toLowerCase(),
        eventType.toLowerCase());
  }

  /**
   * Generate platform-level topic name (DLQ, system events)
   * 
   * @param category Category (e.g., "dlq", "audit", "metrics")
   * @param subType Sub-type (e.g., "all", "critical")
   * @return Topic name: core.platform.{category}.{subType}
   */
  public static String platformTopic(String category, String subType) {
    validate(category, "category");
    validate(subType, "subType");
    return String.format("%s.%s.%s.%s", NAMESPACE, DOMAIN_PLATFORM, category.toLowerCase(),
        subType.toLowerCase());
  }

  /**
   * Generate DLQ topic name (unified dead letter queue)
   * 
   * @return Topic name: core.platform.dlq.all
   */
  public static String dlqTopic() {
    return platformTopic("dlq", "all");
  }

  /**
   * Validate topic name against naming convention
   * 
   * @param topicName Topic name to validate
   * @return true if valid, false otherwise
   */
  public static boolean isValidTopicName(String topicName) {
    if (topicName == null || topicName.isBlank()) {
      return false;
    }
    return VALID_TOPIC_PATTERN.matcher(topicName).matches();
  }

  /**
   * Extract entity name from topic
   * 
   * @param topicName Topic name (e.g., "core.entities.user.mutated")
   * @return Entity name (e.g., "user"), or null if invalid
   */
  public static String extractEntity(String topicName) {
    if (!isValidTopicName(topicName)) {
      return null;
    }
    String[] parts = topicName.split("\\.");
    if (parts.length != 4) {
      return null;
    }
    return parts[2]; // namespace.domain.entity.eventType
  }

  /**
   * Extract event type from topic
   * 
   * @param topicName Topic name (e.g., "core.entities.user.mutated")
   * @return Event type (e.g., "mutated"), or null if invalid
   */
  public static String extractEventType(String topicName) {
    if (!isValidTopicName(topicName)) {
      return null;
    }
    String[] parts = topicName.split("\\.");
    if (parts.length != 4) {
      return null;
    }
    return parts[3]; // namespace.domain.entity.eventType
  }

  /**
   * Get backward-compatible legacy topic name (for migration phase)
   * 
   * @param entity Entity name
   * @param eventType Event type (mutating, mutated)
   * @return Legacy topic: core.entities.lifecycle.{eventType}
   */
  public static String legacyLifecycleTopic(String eventType) {
    return String.format("core.entities.lifecycle.%s", eventType.toLowerCase());
  }

  // Private validation helper
  private static void validate(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " cannot be null or blank");
    }
    if (!value.matches("^[a-z0-9-]+$")) {
      throw new IllegalArgumentException(
          fieldName + " must contain only lowercase letters, numbers, and hyphens: " + value);
    }
  }
}
