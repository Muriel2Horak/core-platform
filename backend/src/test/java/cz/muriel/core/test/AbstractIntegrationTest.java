package cz.muriel.core.test;

import cz.muriel.core.config.TestKafkaConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

/**
 * 🏗️ Base class for integration tests with shared Testcontainers and per-test
 * isolation.
 * 
 * **Shared Containers** (1 per JVM for speed): - PostgreSQL 16-alpine - Redis
 * 7-alpine - Kafka 7.6.1 (Confluent Platform)
 * 
 * **Per-Test Isolation** (parallel execution safe): - Unique Kafka topics:
 * base_topic_<8-char-uuid> - Unique Redis key prefix: test_<8-char-uuid>: - DB
 * cleanup via DELETE (not TRUNCATE - avoids deadlocks)
 * 
 * **Usage:**
 * 
 * <pre>{@code
 * @SpringBootTest(webEnvironment = RANDOM_PORT)
 * class MyIT extends AbstractIntegrationTest {
 *   // Access topicSuffix for Kafka isolation
 *   // Use redisKeyPrefix() for Redis keys
 *   // Add @Transactional to test class if you want auto-rollback
 * }
 * }</pre>
 */
@SpringBootTest @ActiveProfiles("test") @Testcontainers @Import(TestKafkaConfig.class)
public abstract class AbstractIntegrationTest {

  // ==================== SHARED CONTAINERS (1 per JVM) ====================

  @Container @SuppressWarnings("resource") // Testcontainers manages lifecycle
  protected static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(
      "postgres:16-alpine").withDatabaseName("testdb").withUsername("test").withPassword("test")
          .withReuse(true); // Reuse across tests for speed

  @Container @SuppressWarnings("resource")
  protected static final GenericContainer<?> redisContainer = new GenericContainer<>(
      DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379).withReuse(true);

  @Container @SuppressWarnings("resource")
  protected static final KafkaContainer kafkaContainer = new KafkaContainer(
      DockerImageName.parse("confluentinc/cp-kafka:7.6.1")).withKraft() // Use KRaft mode (no
                                                                        // Zookeeper)
          .withStartupAttempts(3);

  // ==================== PER-TEST ISOLATION ====================

  /**
   * Unique topic suffix for this test (e.g., "test_a1b2c3d4"). Use this to create
   * isolated Kafka topics per test.
   */
  protected String topicSuffix;

  @Autowired(required = false)
  private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

  @Autowired(required = false)
  private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setupTestIsolation(TestInfo testInfo) {
    // Generate unique 8-char ID for Kafka topic isolation
    String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    topicSuffix = "test_" + uuid;

    System.out.printf("✅ Test starting: %s (topic_suffix=%s)%n", testInfo.getDisplayName(),
        topicSuffix);
  }

  @AfterEach
  void cleanupTestIsolation() {
    // DB cleanup: DELETE instead of TRUNCATE (lighter locks, no deadlocks in
    // parallel)
    // Skip if no JdbcTemplate (some tests don't use DB)
    if (jdbcTemplate != null) {
      try {
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename NOT LIKE 'flyway%'",
            String.class);

        // Use DELETE instead of TRUNCATE (avoids AccessExclusiveLock)
        for (String table : tables) {
          try {
            jdbcTemplate.execute("DELETE FROM " + table);
          } catch (Exception e) {
            // Some tables might not exist or have FK constraints - ignore
          }
        }
      } catch (Exception e) {
        // DB might not be available - ignore
      }
    }

    // Redis cleanup: delete keys with our prefix only
    if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
      try {
        String pattern = redisKeyPrefix() + "*";
        var connection = redisTemplate.getConnectionFactory().getConnection();
        var keys = connection.keyCommands().keys(pattern.getBytes());
        if (keys != null && !keys.isEmpty()) {
          connection.keyCommands().del(keys.toArray(new byte[0][]));
          System.out.printf("🧹 Deleted %d Redis keys with prefix: %s%n", keys.size(),
              redisKeyPrefix());
        }
      } catch (Exception e) {
        // Ignore Redis cleanup failures (non-critical)
        System.err.printf("⚠️  Redis cleanup failed: %s%n", e.getMessage());
      }
    }
  }

  /**
   * Returns unique Redis key prefix for this test. Use this to avoid key
   * collisions in parallel tests.
   * 
   * Example:
   * {@code redisTemplate.opsForValue().set(redisKeyPrefix() + "mykey", value)}
   */
  protected String redisKeyPrefix() {
    return topicSuffix + ":";
  }

  // ==================== DYNAMIC PROPERTIES ====================

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // PostgreSQL - shared container
    registry.add("spring.datasource.url",
        () -> postgresContainer.getJdbcUrl() + "?currentSchema=public");
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
