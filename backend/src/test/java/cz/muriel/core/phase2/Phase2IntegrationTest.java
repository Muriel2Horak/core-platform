package cz.muriel.core.phase2;

import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 🧪 Phase 2 Integration Tests
 * 
 * Tests for WebSocket, Workflow, Documents and Search functionality
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) @Testcontainers
public class Phase2IntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  @Autowired
  private MinioClient minioClient;

  // Testcontainers - auto-cleanup handled by JUnit lifecycle, warnings are false
  // positives
  @SuppressWarnings("resource") @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("testdb").withUsername("test").withPassword("test").withReuse(false);

  @SuppressWarnings("resource") @Container
  static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379)
      .withReuse(false);

  @SuppressWarnings("resource") @Container
  static GenericContainer<?> minio = new GenericContainer<>("minio/minio:latest")
      .withExposedPorts(9000, 9001).withEnv("MINIO_ROOT_USER", "minioadmin")
      .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
      .withCommand("server /data --console-address :9001").withReuse(false);

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // Postgres
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    // Redis
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", redis::getFirstMappedPort);

    // MinIO
    registry.add("minio.endpoint",
        () -> String.format("http://%s:%d", minio.getHost(), minio.getFirstMappedPort()));
  }

  @BeforeAll
  static void setUp() {
    // Wait for containers to be ready
    assertTrue(postgres.isRunning());
    assertTrue(redis.isRunning());
    assertTrue(minio.isRunning());
  }

  @Test
  void contextLoads() {
    // Basic smoke test
    assertTrue(port > 0);
  }

  @Test
  void testDatabaseMigration() {
    // Verify Phase 2 tables exist (V5 migration)
    var tables = this.jdbcTemplate.queryForList(
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('entity_state', 'state_transition', 'document', 'document_index')",
        String.class);
    assertThat(tables).hasSize(4);
    assertThat(tables).contains("entity_state", "state_transition", "document", "document_index");
  }

  @Test
  void testRedisConnection() {
    // Test Redis connectivity
    this.redisTemplate.opsForValue().set("test:key", "test-value");
    String value = this.redisTemplate.opsForValue().get("test:key");
    assertThat(value).isEqualTo("test-value");
  }

  @Test
  void testMinioConnection() throws Exception {
    // Test MinIO connectivity
    boolean bucketExists = this.minioClient
        .bucketExists(io.minio.BucketExistsArgs.builder().bucket("test-bucket").build());
    assertThat(bucketExists).isFalse(); // Bucket shouldn't exist yet
  }

  /**
   * Future integration test scenarios:
   * @see https://github.com/Muriel2Horak/core-platform/issues/GH-P2.5
   * 
   * - Workflow: State transitions with guards and SLA calculations
   * - Documents: Upload, download, text extraction, versioning
   * - Search: Fulltext search across entities and documents
   * - WebSocket: Presence tracking and editing indicators
   * - Cache: Redis invalidation on entity updates
   * - jOOQ: Type-safe queries and filter parser
   * - Pagination: Keyset pagination with cursors
   */
}
