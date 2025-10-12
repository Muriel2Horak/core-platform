package cz.muriel.core.kafka;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for KafkaTopicNamingConvention
 */
class KafkaTopicNamingConventionTest {

  @Test
  void shouldGenerateEntityLifecycleTopic() {
    assertEquals("core.entities.user.mutating",
        KafkaTopicNamingConvention.entityLifecycleTopic("user", "mutating"));
    assertEquals("core.entities.user.mutated",
        KafkaTopicNamingConvention.entityLifecycleTopic("user", "mutated"));
    assertEquals("core.entities.group.created",
        KafkaTopicNamingConvention.entityLifecycleTopic("group", "created"));
    assertEquals("core.entities.order.deleted",
        KafkaTopicNamingConvention.entityLifecycleTopic("order", "deleted"));
  }

  @Test
  void shouldGenerateReportingTopic() {
    assertEquals("core.reporting.preagg.refresh-requested",
        KafkaTopicNamingConvention.reportingTopic("preagg", "refresh-requested"));
    assertEquals("core.reporting.analytics.completed",
        KafkaTopicNamingConvention.reportingTopic("analytics", "completed"));
  }

  @Test
  void shouldGeneratePlatformTopic() {
    assertEquals("core.platform.dlq.all", KafkaTopicNamingConvention.platformTopic("dlq", "all"));
    assertEquals("core.platform.audit.critical",
        KafkaTopicNamingConvention.platformTopic("audit", "critical"));
    assertEquals("core.platform.metrics.system",
        KafkaTopicNamingConvention.platformTopic("metrics", "system"));
  }

  @Test
  void shouldValidateTopicNames() {
    // Valid
    assertTrue(KafkaTopicNamingConvention.isValidTopicName("core.entities.user.mutated"));
    assertTrue(
        KafkaTopicNamingConvention.isValidTopicName("core.reporting.preagg.refresh-requested"));
    assertTrue(KafkaTopicNamingConvention.isValidTopicName("core.platform.dlq.all"));

    // Invalid
    assertFalse(KafkaTopicNamingConvention.isValidTopicName("UserMutated")); // Wrong format
    assertFalse(KafkaTopicNamingConvention.isValidTopicName("core-entities-user")); // Hyphens
                                                                                    // instead of
                                                                                    // dots
    assertFalse(KafkaTopicNamingConvention.isValidTopicName("CORE.ENTITIES.USER")); // Uppercase
    assertFalse(KafkaTopicNamingConvention.isValidTopicName("core.entities.user")); // Only 3 parts
    assertFalse(KafkaTopicNamingConvention.isValidTopicName("core.entities")); // Only 2 parts
    assertFalse(KafkaTopicNamingConvention.isValidTopicName("")); // Empty
    assertFalse(KafkaTopicNamingConvention.isValidTopicName(null)); // Null
  }

  @Test
  void shouldExtractEntityFromTopic() {
    assertEquals("user", KafkaTopicNamingConvention.extractEntity("core.entities.user.mutated"));
    assertEquals("group", KafkaTopicNamingConvention.extractEntity("core.entities.group.created"));
    assertEquals("preagg",
        KafkaTopicNamingConvention.extractEntity("core.reporting.preagg.refresh-requested"));
    assertNull(KafkaTopicNamingConvention.extractEntity("invalid-topic"));
    assertNull(KafkaTopicNamingConvention.extractEntity(""));
  }

  @Test
  void shouldExtractEventTypeFromTopic() {
    assertEquals("mutated",
        KafkaTopicNamingConvention.extractEventType("core.entities.user.mutated"));
    assertEquals("created",
        KafkaTopicNamingConvention.extractEventType("core.entities.group.created"));
    assertEquals("refresh-requested",
        KafkaTopicNamingConvention.extractEventType("core.reporting.preagg.refresh-requested"));
    assertNull(KafkaTopicNamingConvention.extractEventType("invalid-topic"));
    assertNull(KafkaTopicNamingConvention.extractEventType(null));
  }

  @Test
  void shouldRejectInvalidInput() {
    assertThrows(IllegalArgumentException.class,
        () -> KafkaTopicNamingConvention.entityLifecycleTopic(null, "mutated"));
    assertThrows(IllegalArgumentException.class,
        () -> KafkaTopicNamingConvention.entityLifecycleTopic("", "mutated"));
    assertThrows(IllegalArgumentException.class,
        () -> KafkaTopicNamingConvention.entityLifecycleTopic("User", "mutated")); // Uppercase
    assertThrows(IllegalArgumentException.class,
        () -> KafkaTopicNamingConvention.entityLifecycleTopic("user_profile", "mutated")); // Underscore
  }

  @Test
  void shouldGenerateLegacyTopicForBackwardCompat() {
    assertEquals("core.entities.lifecycle.mutating",
        KafkaTopicNamingConvention.legacyLifecycleTopic("mutating"));
    assertEquals("core.entities.lifecycle.mutated",
        KafkaTopicNamingConvention.legacyLifecycleTopic("mutated"));
  }

  @Test
  void shouldGenerateDlqTopic() {
    assertEquals("core.platform.dlq.all", KafkaTopicNamingConvention.dlqTopic());
  }
}
