package cz.muriel.core.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that require Kafka messaging.
 * 
 * Extends AbstractIntegrationTest and adds: - Kafka container for message
 * streaming - Automatic Kafka bootstrap server configuration - Container reuse
 * for fast test execution
 * 
 * Usage:
 * 
 * <pre>
 * {
 *   &#64;code @SpringBootTest
 *   class MyKafkaTest extends AbstractKafkaIntegrationTest {
 *     &#64;Autowired
 *     private KafkaTemplate<String, String> kafkaTemplate;
 * 
 *     @Test
 *     void shouldPublishMessage() {
 *       kafkaTemplate.send("test-topic", "key", "value");
 *       // assertions
 *     }
 *   }
 * }
 * </pre>
 * 
 * Provides: - PostgreSQL container (from AbstractIntegrationTest) - Redis
 * container (from AbstractIntegrationTest) - Kafka container (cp-kafka 7.6.0) -
 * All test profile configurations - Mock beans for testing
 * 
 * @see AbstractIntegrationTest
 */
@SpringBootTest
public abstract class AbstractKafkaIntegrationTest extends AbstractIntegrationTest {

  @Container @SuppressWarnings("resource") // Testcontainers manages lifecycle automatically
  protected static final ConfluentKafkaContainer kafkaContainer = new ConfluentKafkaContainer(
      DockerImageName.parse("confluentinc/cp-kafka:7.6.0")).withReuse(true); // Reuse container
                                                                             // for speed

  @DynamicPropertySource
  static void configureKafkaProperties(DynamicPropertyRegistry registry) {
    // Kafka bootstrap servers
    registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);

    // Enable streaming features in tests
    registry.add("streaming.enabled", () -> "true");
    registry.add("workflow.kafka.enabled", () -> "true");

    // Kafka consumer configuration for tests
    registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    registry.add("spring.kafka.consumer.enable-auto-commit", () -> "false");

    // Kafka producer configuration for tests
    registry.add("spring.kafka.producer.acks", () -> "all");
    registry.add("spring.kafka.producer.retries", () -> "3");
  }
}
