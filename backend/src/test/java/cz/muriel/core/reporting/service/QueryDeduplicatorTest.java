package cz.muriel.core.reporting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QueryDeduplicator. Tests Single-Flight pattern for duplicate
 * query detection.
 */
class QueryDeduplicatorTest {

  private QueryDeduplicator queryDeduplicator;
  private AtomicInteger executionCount;

  @BeforeEach
  void setUp() {
    queryDeduplicator = new QueryDeduplicator();
    executionCount = new AtomicInteger(0);
  }

  @Test
  void shouldExecuteQueryOnlyOnceForDuplicates() throws Exception {
    // Arrange
    Map<String, Object> query = Map.of("dimensions", List.of("User.id"));
    Map<String, Object> expectedResult = Map.of("data", List.of());

    // Act - Execute same query concurrently 3 times
    CompletableFuture<Map<String, Object>> future1 = CompletableFuture
        .supplyAsync(() -> queryDeduplicator.executeWithDeduplication(query, "tenant-1", () -> {
          executionCount.incrementAndGet();
          try {
            Thread.sleep(200); // Ensure other requests arrive during execution
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return expectedResult;
        }));

    // Small delay to ensure future1 starts first
    Thread.sleep(50);

    CompletableFuture<Map<String, Object>> future2 = CompletableFuture
        .supplyAsync(() -> queryDeduplicator.executeWithDeduplication(query, "tenant-1", () -> {
          executionCount.incrementAndGet();
          return expectedResult;
        }));

    CompletableFuture<Map<String, Object>> future3 = CompletableFuture
        .supplyAsync(() -> queryDeduplicator.executeWithDeduplication(query, "tenant-1", () -> {
          executionCount.incrementAndGet();
          return expectedResult;
        }));

    // Wait for all
    CompletableFuture.allOf(future1, future2, future3).join();

    // Assert: Query executed only ONCE (deduplication worked)
    assertEquals(1, executionCount.get(),
        "Query should be executed only once despite 3 concurrent requests");
    assertEquals(expectedResult, future1.get());
    assertEquals(expectedResult, future2.get());
    assertEquals(expectedResult, future3.get());
  }

  @Test
  void shouldExecuteSeparatelyForDifferentQueries() {
    // Arrange
    Map<String, Object> query1 = Map.of("dimensions", List.of("User.id"));
    Map<String, Object> query2 = Map.of("dimensions", List.of("Company.name"));

    // Act
    queryDeduplicator.executeWithDeduplication(query1, "tenant-1", () -> {
      executionCount.incrementAndGet();
      return Map.of("data1", List.of());
    });

    queryDeduplicator.executeWithDeduplication(query2, "tenant-1", () -> {
      executionCount.incrementAndGet();
      return Map.of("data2", List.of());
    });

    // Assert: Different queries → 2 executions
    assertEquals(2, executionCount.get());
  }

  @Test
  void shouldIsolateDifferentTenants() {
    // Arrange
    Map<String, Object> query = Map.of("dimensions", List.of("User.id"));

    // Act: Same query, different tenants
    queryDeduplicator.executeWithDeduplication(query, "tenant-1", () -> {
      executionCount.incrementAndGet();
      return Map.of("data", List.of());
    });

    queryDeduplicator.executeWithDeduplication(query, "tenant-2", () -> {
      executionCount.incrementAndGet();
      return Map.of("data", List.of());
    });

    // Assert: Different tenants → 2 executions
    assertEquals(2, executionCount.get());
  }

  @Test
  void shouldRemoveFromInflightAfterCompletion() throws Exception {
    // Arrange
    Map<String, Object> query = Map.of("dimensions", List.of("User.id"));

    // Act: Execute query
    CompletableFuture<Map<String, Object>> future = CompletableFuture
        .supplyAsync(() -> queryDeduplicator.executeWithDeduplication(query, "tenant-1", () -> {
          executionCount.incrementAndGet();
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return Map.of("data", List.of());
        }));

    // During execution, inflight count should be > 0
    Thread.sleep(50);
    assertTrue(queryDeduplicator.getInflightCount() > 0,
        "Should have in-flight query during execution");

    // Wait for completion
    future.join();

    // After completion, inflight should be 0
    assertEquals(0, queryDeduplicator.getInflightCount(),
        "Inflight count should be 0 after completion");
  }

  @Test
  void shouldHandleExceptionsInExecutor() {
    // Arrange
    Map<String, Object> query = Map.of("dimensions", List.of("User.id"));

    // Act & Assert
    assertThrows(RuntimeException.class,
        () -> queryDeduplicator.executeWithDeduplication(query, "tenant-1", () -> {
          throw new RuntimeException("Cube.js error");
        }));

    // Inflight should be cleared after exception
    assertEquals(0, queryDeduplicator.getInflightCount());
  }

  @Test
  void shouldGenerateConsistentFingerprints() {
    // Arrange - Use SAME query object to ensure identical fingerprints
    Map<String, Object> query = Map.of("dimensions", List.of("User.id"), "filters", List.of());

    List<String> fingerprints = Collections.synchronizedList(new ArrayList<>());

    // Act: Execute same query in parallel to test deduplication
    CountDownLatch latch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    new Thread(() -> {
      try {
        startLatch.await();
        queryDeduplicator.executeWithDeduplication(query, "tenant-1", () -> {
          fingerprints.add("exec1");
          try {
            Thread.sleep(100); // Simulate slow query
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return Map.of();
        });
      } catch (Exception e) {
        // ignore
      } finally {
        latch.countDown();
      }
    }).start();

    new Thread(() -> {
      try {
        startLatch.await();
        queryDeduplicator.executeWithDeduplication(query, "tenant-1", () -> {
          fingerprints.add("exec2");
          return Map.of();
        });
      } catch (Exception e) {
        // ignore
      } finally {
        latch.countDown();
      }
    }).start();

    startLatch.countDown(); // Start both threads
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Assert: Same fingerprint → only 1 execution (deduplication worked)
    assertEquals(1, fingerprints.size(), "Identical queries should generate same fingerprint");
  }

  @Test
  void shouldClearInflightQueries() throws Exception {
    // Arrange
    Map<String, Object> query = Map.of("dimensions", List.of("User.id"));

    CompletableFuture
        .supplyAsync(() -> queryDeduplicator.executeWithDeduplication(query, "tenant-1", () -> {
          try {
            Thread.sleep(200);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return Map.of();
        }));

    Thread.sleep(50);
    assertTrue(queryDeduplicator.getInflightCount() > 0);

    // Act: Clear
    queryDeduplicator.clear();

    // Assert
    assertEquals(0, queryDeduplicator.getInflightCount());
  }
}
