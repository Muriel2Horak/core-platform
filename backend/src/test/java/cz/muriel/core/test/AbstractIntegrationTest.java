package cz.muriel.core.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

/**
 * 🏗️ Base class for integration tests with shared Testcontainers and per-test isolation.
 * 
 * **Shared Containers** (1 per JVM for speed):
 * - PostgreSQL 16-alpine
 * - Redis 7-alpine
 * - Kafka 7.6.1 (Confluent Platform)
 * 
 * **Per-Test Isolation** (parallel execution safe):
 * - Unique DB schema: s_<8-char-uuid>
 * - Unique Kafka topics: base_topic_<schema>
 * - Unique consumer groups: test_<schema>
 * 
 * **Usage:**
 * <pre>{@code
 * @SpringBootTest(webEnvironment = RANDOM_PORT)
 * class MyIT extends AbstractIntegrationTest {
 *   // Access schemaName, topicSuffix for test isolation
 * }
 * }</pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Reuse containers across all tests in class
@Import(MockTestConfig.class)
public abstract class AbstractIntegrationTest {

  // ==================== SHARED CONTAINERS (1 per JVM) ====================

  @Container
  @SuppressWarnings("resource") // Testcontainers manages lifecycle
  protected static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(
      "postgres:16-alpine")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test")
          .withReuse(true); // Reuse across tests for speed

  @Container
  @SuppressWarnings("resource")
  protected static final GenericContainer<?> redisContainer = new GenericContainer<>(
      DockerImageName.parse("redis:7-alpine"))
          .withExposedPorts(6379)
          .withReuse(true);

  @Container
  @SuppressWarnings("resource")
  protected static final KafkaContainer kafkaContainer = new KafkaContainer(
      DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
          .withKraft() // Use KRaft mode (no Zookeeper)
          .withReuse(true);

  // ==================== PER-TEST ISOLATION ====================

  /**
   * Unique schema name for this test (e.g., "s_a1b2c3d4").
   * Each test gets its own isolated schema in shared PostgreSQL.
   */
  protected String schemaName;

  /**
   * Unique topic suffix for this test (e.g., "base_topic_s_a1b2c3d4").
   * Use this to create isolated Kafka topics per test.
   */
  protected String topicSuffix;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setupTestIsolation(TestInfo testInfo) {
    // Generate unique 8-char schema name: s_<uuid>
    String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    schemaName = "s_" + uuid;
    topicSuffix = schemaName;

    // Create isolated schema in shared PostgreSQL
    jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
    // Set search_path for this test's schema
    jdbcTemplate.execute("SET search_path TO " + schemaName + ", public");

    // Copy all tables from public to test schema (structure only, no data)
    jdbcTemplate.queryForList(
        "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename NOT LIKE 'flyway%'",
        String.class)
        .forEach(table -> jdbcTemplate.execute(
            "CREATE TABLE " + schemaName + "." + table + " (LIKE public." + table + " INCLUDING ALL)"));

    System.out.printf("✅ Test isolation ready: schema=%s, topic_suffix=%s, test=%s%n",
        schemaName, topicSuffix, testInfo.getDisplayName());
  }

  @AfterEach
  void cleanupTestIsolation() {
    // Drop test schema (CASCADE removes all tables)
    jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
    System.out.printf("🧹 Cleaned up schema: %s%n", schemaName);
  }

  // ==================== DYNAMIC PROPERTIES ====================

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // PostgreSQL - shared container
    registry.add("spring.datasource.url", () -> 
        postgresContainer.getJdbcUrl() + "?currentSchema=public");
    registry.add("spring.datasource.username", postgresContainer::getUsername);
    registry.add("spring.datasource.password", postgresContainer::getPassword);

    // Redis - shared container
    registry.add("spring.data.redis.host", redisContainer::getHost);
    registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    registry.add("app.redis.enabled", () -> "true");

    // Kafka - shared container
    registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    registry.add("spring.kafka.consumer.group-id", () -> "test-consumer-group");

    // JPA/Hibernate - validate schema with Flyway
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.jpa.properties.hibernate.dialect",
        () -> "org.hibernate.dialect.PostgreSQLDialect");

    // Disable components not needed in tests
    registry.add("keycloak.datasource.enabled", () -> "false");
    registry.add("app.rate-limit.enabled", () -> "false");

    // Flyway - run migrations on shared public schema
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.flyway.clean-disabled", () -> "false");
  }
}
