package cz.muriel.core.streaming;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🧪 Integration Tests for Kafka Streaming Infrastructure
 * 
 * Tests: - Topic configuration (cleanup.policy, retention, segment) - Message
 * ordering with keying ({entity}#{entityId}) - Event ordering guarantee for
 * same entity
 */
@SpringBootTest @Testcontainers
public class KafkaStreamingIT {

  @Container
  @SuppressWarnings("resource") // Managed by Testcontainers @Container lifecycle
  static ConfluentKafkaContainer kafka = 
      new ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.0");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    registry.add("streaming.enabled", () -> "true");
  }

  private AdminClient adminClient;
  private KafkaProducer<String, String> producer;
  private KafkaConsumer<String, String> consumer;

  @BeforeEach
  void setUp() {
    Properties adminProps = new Properties();
    adminProps.put("bootstrap.servers", kafka.getBootstrapServers());
    adminClient = AdminClient.create(adminProps);

    Properties producerProps = new Properties();
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        StringSerializer.class.getName());
    producer = new KafkaProducer<>(producerProps);

    Properties consumerProps = new Properties();
    consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class.getName());
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class.getName());
    consumer = new KafkaConsumer<>(consumerProps);
  }

  @AfterEach
  void tearDown() {
    if (producer != null)
      producer.close();
    if (consumer != null)
      consumer.close();
    if (adminClient != null)
      adminClient.close();
  }

  @Test
  void testTopicConfigurationFromMetamodel() throws ExecutionException, InterruptedException {
    // Given: Topic config based on metamodel
    String topicName = "core.user.events";
    Map<String, String> configs = new HashMap<>();
    configs.put("cleanup.policy", "compact,delete");
    configs.put("retention.ms", String.valueOf(Duration.ofDays(7).toMillis()));
    configs.put("segment.ms", String.valueOf(Duration.ofHours(1).toMillis()));
    configs.put("min.compaction.lag.ms", String.valueOf(Duration.ofMinutes(5).toMillis()));

    NewTopic topic = new NewTopic(topicName, 3, (short) 1);
    topic.configs(configs);

    // When: Create topic
    adminClient.createTopics(Collections.singleton(topic)).all().get();

    // Then: Topic exists with correct config
    var topicDescription = adminClient.describeTopics(Collections.singleton(topicName))
        .allTopicNames().get();
    assertThat(topicDescription).containsKey(topicName);
    assertThat(topicDescription.get(topicName).partitions()).hasSize(3);

    var configResource = new org.apache.kafka.common.config.ConfigResource(
        org.apache.kafka.common.config.ConfigResource.Type.TOPIC, topicName);
    var topicConfigs = adminClient.describeConfigs(Collections.singleton(configResource)).all()
        .get();

    assertThat(topicConfigs.get(configResource).get("cleanup.policy").value())
        .isEqualTo("compact,delete");
    assertThat(topicConfigs.get(configResource).get("retention.ms").value())
        .isEqualTo(String.valueOf(Duration.ofDays(7).toMillis()));
  }

  @Test
  void testMessageOrderingWithEntityKeying() throws Exception {
    // Given: Topic for testing
    String topicName = "test.ordering";
    adminClient.createTopics(Collections.singleton(new NewTopic(topicName, 1, (short) 1))).all()
        .get();

    // Given: Two entities
    String entity1Id = UUID.randomUUID().toString();
    String entity2Id = UUID.randomUUID().toString();

    // When: Publish multiple events for each entity with entity keying
    String key1 = "User#" + entity1Id;
    String key2 = "User#" + entity2Id;

    producer.send(new ProducerRecord<>(topicName, key1, "{\"event\": \"created\", \"version\": 1}"))
        .get();
    producer.send(new ProducerRecord<>(topicName, key2, "{\"event\": \"created\", \"version\": 1}"))
        .get();
    producer.send(new ProducerRecord<>(topicName, key1, "{\"event\": \"updated\", \"version\": 2}"))
        .get();
    producer.send(new ProducerRecord<>(topicName, key2, "{\"event\": \"updated\", \"version\": 2}"))
        .get();
    producer.send(new ProducerRecord<>(topicName, key1, "{\"event\": \"deleted\", \"version\": 3}"))
        .get();

    producer.flush();

    // Then: Consume and verify ordering
    consumer.subscribe(Collections.singleton(topicName));

    List<String> entity1Events = new ArrayList<>();
    List<String> entity2Events = new ArrayList<>();

    long endTime = System.currentTimeMillis() + 10000; // 10s timeout
    while (System.currentTimeMillis() < endTime
        && (entity1Events.size() < 3 || entity2Events.size() < 2)) {
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
      records.forEach(record -> {
        if (record.key().equals(key1)) {
          entity1Events.add(record.value());
        } else if (record.key().equals(key2)) {
          entity2Events.add(record.value());
        }
      });
    }

    // Verify entity1 events in order
    assertThat(entity1Events).hasSize(3);
    assertThat(entity1Events.get(0)).contains("\"event\": \"created\"");
    assertThat(entity1Events.get(1)).contains("\"event\": \"updated\"");
    assertThat(entity1Events.get(2)).contains("\"event\": \"deleted\"");

    // Verify entity2 events in order
    assertThat(entity2Events).hasSize(2);
    assertThat(entity2Events.get(0)).contains("\"event\": \"created\"");
    assertThat(entity2Events.get(1)).contains("\"event\": \"updated\"");
  }

  @Test
  void testPartitionAssignmentConsistency() throws Exception {
    // Given: Topic with multiple partitions
    String topicName = "test.partitioning";
    adminClient.createTopics(Collections.singleton(new NewTopic(topicName, 3, (short) 1))).all()
        .get();

    String entityId = UUID.randomUUID().toString();
    String key = "User#" + entityId;

    // When: Send multiple messages with same key
    Set<Integer> partitions = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      var metadata = producer.send(new ProducerRecord<>(topicName, key, "{\"version\": " + i + "}"))
          .get();
      partitions.add(metadata.partition());
    }

    // Then: All messages go to same partition (ordering guarantee)
    assertThat(partitions).hasSize(1);
  }
}
