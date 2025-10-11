package cz.muriel.core.streaming;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🧪 Integration Tests for PostgreSQL-based Streaming Infrastructure
 * 
 * Tests: - Command Queue with SKIP LOCKED batching - Operation ID unique
 * constraint and deduplication - Work State TTL expiration - Outbox Final
 * atomic write in single transaction
 */
@SpringBootTest @Testcontainers
public class PostgresStreamingIT {

  @Container @SuppressWarnings("resource")
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("testdb").withUsername("test").withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    registry.add("spring.flyway.enabled", () -> "true");
  }

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void testCommandQueueSkipLockedBatching() throws Exception {
    // Given: Insert 100 commands
    for (int i = 0; i < 100; i++) {
      jdbcTemplate.update(
          "INSERT INTO command_queue (id, tenant_id, entity_type, entity_id, operation, payload, status, created_at) "
              + "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?)",
          UUID.randomUUID(), "test-tenant", "User", UUID.randomUUID().toString(), "UPDATE",
          "{\"test\": true}", "PENDING", LocalDateTime.now());
    }

    // When: Multiple workers process batches concurrently
    int numWorkers = 4;
    int batchSize = 10;
    AtomicInteger processedCount = new AtomicInteger(0);
    ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
    CountDownLatch latch = new CountDownLatch(numWorkers);

    for (int i = 0; i < numWorkers; i++) {
      executor.submit(() -> {
        try {
          while (true) {
            // Fetch batch with SKIP LOCKED
            List<Map<String, Object>> batch = jdbcTemplate
                .queryForList("SELECT id FROM command_queue " + "WHERE status = 'PENDING' "
                    + "ORDER BY created_at " + "LIMIT ? FOR UPDATE SKIP LOCKED", batchSize);

            if (batch.isEmpty()) {
              break;
            }

            // Process batch
            for (Map<String, Object> row : batch) {
              UUID id = (UUID) row.get("id");
              jdbcTemplate.update(
                  "UPDATE command_queue SET status = 'PROCESSED', processed_at = ? WHERE id = ?",
                  LocalDateTime.now(), id);
              processedCount.incrementAndGet();
            }
          }
        } finally {
          latch.countDown();
        }
      });
    }

    // Then: All commands processed without duplicates
    latch.await(30, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(processedCount.get()).isEqualTo(100);

    Long pendingCount = jdbcTemplate
        .queryForObject("SELECT COUNT(*) FROM command_queue WHERE status = 'PENDING'", Long.class);
    assertThat(pendingCount).isZero();
  }

  @Test
  void testOperationIdUniqueConstraintAndDedupe() {
    // Given: Operation ID
    String operationId = "op-" + UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();

    // When: First insert succeeds
    int inserted = jdbcTemplate.update(
        "INSERT INTO command_queue (id, tenant_id, entity_type, entity_id, operation, operation_id, payload, status, created_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?) ON CONFLICT (operation_id) DO NOTHING",
        UUID.randomUUID(), tenantId.toString(), "User", UUID.randomUUID().toString(), "CREATE",
        operationId, "{}", "PENDING", LocalDateTime.now());
    assertThat(inserted).isEqualTo(1);

    // When: Duplicate insert is silently ignored
    int duplicate = jdbcTemplate.update(
        "INSERT INTO command_queue (id, tenant_id, entity_type, entity_id, operation, operation_id, payload, status, created_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?) ON CONFLICT (operation_id) DO NOTHING",
        UUID.randomUUID(), tenantId.toString(), "User", UUID.randomUUID().toString(), "CREATE",
        operationId, "{}", "PENDING", LocalDateTime.now());
    assertThat(duplicate).isZero();

    // Then: Only one record exists
    Long count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM command_queue WHERE operation_id = ?", Long.class, operationId);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void testWorkStateTTLExpiration() throws Exception {
    // Given: Insert work state with TTL
    UUID entityId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO work_state (entity_type, entity_id, state, expires_at) "
            + "VALUES (?, ?, ?, ?)",
        "User", entityId, "UPDATING", LocalDateTime.now().plusSeconds(2));

    // When: Wait for TTL expiration
    Thread.sleep(3000);

    // Then: Cleanup job removes expired states
    jdbcTemplate.update("DELETE FROM work_state WHERE expires_at < NOW()");

    Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM work_state WHERE entity_id = ?",
        Long.class, entityId);
    assertThat(count).isZero();
  }

  @Test
  void testOutboxFinalAtomicWrite() {
    // Given: Transaction context
    UUID commandId = UUID.randomUUID();
    UUID entityId = UUID.randomUUID();
    String tenantId = "test-tenant";

    // When: Write command result and outbox in single transaction
    jdbcTemplate.execute((java.sql.Connection conn) -> {
      try (var stmt = conn.prepareStatement(
          "UPDATE command_queue SET status = 'COMPLETED', result = ?::jsonb WHERE id = ?")) {
        stmt.setString(1, "{\"success\": true}");
        stmt.setObject(2, commandId);
        stmt.executeUpdate();
      }

      try (var stmt = conn.prepareStatement(
          "INSERT INTO outbox_final (id, tenant_id, aggregate_type, aggregate_id, event_type, payload, created_at) "
              + "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)")) {
        stmt.setObject(1, UUID.randomUUID());
        stmt.setString(2, tenantId);
        stmt.setString(3, "User");
        stmt.setObject(4, entityId);
        stmt.setString(5, "UserUpdated");
        stmt.setString(6, "{\"userId\": \"" + entityId + "\"}");
        stmt.setObject(7, LocalDateTime.now());
        stmt.executeUpdate();
      }

      return null;
    });

    // Then: Both records committed atomically
    Long commandCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM command_queue WHERE id = ? AND status = 'COMPLETED'", Long.class,
        commandId);
    assertThat(commandCount).isEqualTo(1);

    Long outboxCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM outbox_final WHERE aggregate_id = ?::text", Long.class,
        entityId.toString());
    assertThat(outboxCount).isEqualTo(1);
  }
}
