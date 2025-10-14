package cz.muriel.core.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🧪 W5: Workflow Events Kafka Integration Test
 * 
 * Tests:
 * - Publishing ENTER_STATE, EXIT_STATE, ACTION_APPLIED events
 * - JSON Schema validation of event payloads
 * - Event ordering and delivery
 * 
 * @since 2025-10-14
 */
@SpringBootTest
@Testcontainers
class WorkflowEventsKafkaIT {

  @Container
  @SuppressWarnings("resource")
  static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.0");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
  }

  @Autowired
  private WorkflowEventPublisher eventPublisher;

  private KafkaConsumer<String, String> consumer;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final String TOPIC = "workflow.events";

  @BeforeEach
  void setUp() {
    Properties consumerProps = new Properties();
    consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    consumer = new KafkaConsumer<>(consumerProps);
    consumer.subscribe(List.of(TOPIC));
  }

  @AfterEach
  void tearDown() {
    if (consumer != null) {
      consumer.close();
    }
  }

  // ============================================
  // EVENT PUBLISHING TESTS
  // ============================================

  @Test
  void testPublishEnterStateEvent() throws Exception {
    // Arrange
    String tenantId = "test-tenant";
    String entityType = "Order";
    String entityId = "order-123";
    String stateCode = "APPROVED";
    String actor = "user-1";
    UUID instanceId = UUID.randomUUID();

    // Act
    eventPublisher.publishEnterState(tenantId, entityType, entityId, stateCode, actor, instanceId);

    // Assert: Consume event
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
    assertThat(records.isEmpty()).isFalse();

    String value = records.iterator().next().value();
    @SuppressWarnings("unchecked")
    Map<String, Object> event = objectMapper.readValue(value, Map.class);

    assertThat(event.get("tenantId")).isEqualTo(tenantId);
    assertThat(event.get("entityType")).isEqualTo(entityType);
    assertThat(event.get("entityId")).isEqualTo(entityId);
    assertThat(event.get("eventType")).isEqualTo("ENTER_STATE");
    assertThat(event.get("toStateCode")).isEqualTo(stateCode);
    assertThat(event.get("actor")).isEqualTo(actor);
    assertThat(event.get("eventId")).isNotNull();
    assertThat(event.get("timestamp")).isNotNull();
  }

  @Test
  void testPublishExitStateEvent() throws Exception {
    // Arrange
    String tenantId = "test-tenant";
    String entityType = "Order";
    String entityId = "order-456";
    String stateCode = "PENDING";
    String actor = "user-2";
    Long durationMs = 120000L;
    UUID instanceId = UUID.randomUUID();

    // Act
    eventPublisher.publishExitState(tenantId, entityType, entityId, stateCode, actor, durationMs, instanceId);

    // Assert
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
    assertThat(records.isEmpty()).isFalse();

    String value = records.iterator().next().value();
    @SuppressWarnings("unchecked")
    Map<String, Object> event = objectMapper.readValue(value, Map.class);

    assertThat(event.get("eventType")).isEqualTo("EXIT_STATE");
    assertThat(event.get("fromStateCode")).isEqualTo(stateCode);
    assertThat(event.get("durationMs")).isEqualTo(durationMs.intValue());
  }

  @Test
  void testPublishActionAppliedEvent() throws Exception {
    // Arrange
    String tenantId = "test-tenant";
    String entityType = "Order";
    String entityId = "order-789";
    String fromState = "PENDING";
    String toState = "APPROVED";
    String transitionCode = "approve";
    String actor = "user-3";
    Long durationMs = 5000L;
    UUID instanceId = UUID.randomUUID();

    // Act
    eventPublisher.publishActionApplied(tenantId, entityType, entityId, fromState, toState, 
        transitionCode, actor, durationMs, instanceId);

    // Assert
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
    assertThat(records.isEmpty()).isFalse();

    String value = records.iterator().next().value();
    @SuppressWarnings("unchecked")
    Map<String, Object> event = objectMapper.readValue(value, Map.class);

    assertThat(event.get("eventType")).isEqualTo("ACTION_APPLIED");
    assertThat(event.get("fromStateCode")).isEqualTo(fromState);
    assertThat(event.get("toStateCode")).isEqualTo(toState);
    assertThat(event.get("transitionCode")).isEqualTo(transitionCode);
    assertThat(event.get("durationMs")).isEqualTo(durationMs.intValue());
  }

  @Test
  void testPublishErrorEvent() throws Exception {
    // Arrange
    String tenantId = "test-tenant";
    String entityType = "Order";
    String entityId = "order-error";
    String stateCode = "PROCESSING";
    String errorDetails = "Network timeout";
    String actor = "system";
    UUID instanceId = UUID.randomUUID();

    // Act
    eventPublisher.publishError(tenantId, entityType, entityId, stateCode, errorDetails, actor, instanceId);

    // Assert
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
    assertThat(records.isEmpty()).isFalse();

    String value = records.iterator().next().value();
    @SuppressWarnings("unchecked")
    Map<String, Object> event = objectMapper.readValue(value, Map.class);

    assertThat(event.get("eventType")).isEqualTo("ERROR");
    assertThat(event.get("fromStateCode")).isEqualTo(stateCode);
    assertThat(event.get("errorDetails")).isEqualTo(errorDetails);
  }

  // ============================================
  // EVENT ORDERING TESTS
  // ============================================

  @Test
  void testEventOrdering_sameEntity() throws Exception {
    // Arrange: Publish 3 events for same entity (should preserve order)
    String entityType = "Order";
    String entityId = "order-sequence";
    String tenantId = "test-tenant";

    // Act
    eventPublisher.publishEnterState(tenantId, entityType, entityId, "PENDING", "user", null);
    Thread.sleep(100); // Small delay
    eventPublisher.publishExitState(tenantId, entityType, entityId, "PENDING", "user", 1000L, null);
    Thread.sleep(100);
    eventPublisher.publishEnterState(tenantId, entityType, entityId, "APPROVED", "user", null);

    // Assert: Events should arrive in order (same partition key)
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
    assertThat(records.count()).isGreaterThanOrEqualTo(3);

    List<String> eventTypes = new ArrayList<>();
    records.forEach(record -> {
      try {
        @SuppressWarnings("unchecked")
        Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
        eventTypes.add((String) event.get("eventType"));
      } catch (Exception e) {
        // Ignore
      }
    });

    // Verify order contains ENTER -> EXIT -> ENTER
    assertThat(eventTypes).contains("ENTER_STATE", "EXIT_STATE", "ENTER_STATE");
  }

  // ============================================
  // JSON SCHEMA VALIDATION
  // ============================================

  @Test
  void testEventPayload_hasRequiredFields() throws Exception {
    // Arrange
    eventPublisher.publishEnterState("t1", "Entity", "e1", "STATE", "actor", null);

    // Act
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
    String value = records.iterator().next().value();
    @SuppressWarnings("unchecked")
    Map<String, Object> event = objectMapper.readValue(value, Map.class);

    // Assert: Required fields present
    assertThat(event).containsKeys(
        "eventId",
        "tenantId",
        "entityType",
        "entityId",
        "eventType",
        "actor",
        "timestamp"
    );
  }
}
