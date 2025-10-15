package com.platform.workflow.executor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * 🧪 W7: WorkflowExecutionService Unit Tests
 * 
 * Tests: - Async execution - Retry logic with exponential backoff -
 * Compensation on failure - Metrics tracking
 * 
 * @since 2025-01-14
 */
class WorkflowExecutionServiceTest {

  private WorkflowExecutionService service;
  private WorkflowExecutorRegistry registry;
  private MeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    registry = new WorkflowExecutorRegistry();
    meterRegistry = new SimpleMeterRegistry();
    service = new WorkflowExecutionService(registry, meterRegistry);
  }

  @Test
  void shouldExecuteSuccessfully() throws ExecutionException, InterruptedException {
    var executor = new SuccessExecutor();
    registry.register(executor);

    var result = service.executeAction("order", "123", "test-action", Map.of()).get();

    assertThat(result).containsEntry("status", "success");
    assertThat(meterRegistry.counter("workflow.executor.success").count()).isEqualTo(1.0);
  }

  @Test
  void shouldRetryOnFailure() throws ExecutionException, InterruptedException {
    var executor = new RetryExecutor(2); // Fail twice, succeed on 3rd attempt
    registry.register(executor);

    var result = service.executeAction("order", "123", "retry-action", Map.of()).get();

    assertThat(result).containsEntry("status", "success");
    assertThat(executor.attemptCount.get()).isEqualTo(3);
    assertThat(meterRegistry.counter("workflow.executor.retry").count()).isEqualTo(2.0);
    assertThat(meterRegistry.counter("workflow.executor.success").count()).isEqualTo(1.0);
  }

  @Test
  void shouldFailAfterMaxRetries() throws ExecutionException, InterruptedException {
    var executor = new AlwaysFailExecutor();
    registry.register(executor);

    var result = service.executeAction("order", "123", "fail-action", Map.of()).get();

    // Service catches exception and returns error map instead of throwing
    assertThat(result).containsKey("error");
    assertThat(meterRegistry.counter("workflow.executor.failure").count()).isEqualTo(1.0);
  }

  @Test
  void shouldExecuteMultipleExecutorsInParallel() throws ExecutionException, InterruptedException {
    var executor1 = new SuccessExecutor("exec-1", "parallel-action");
    var executor2 = new SuccessExecutor("exec-2", "parallel-action");
    registry.register(executor1);
    registry.register(executor2);

    var result = service.executeAction("order", "123", "parallel-action", Map.of()).get();

    // Both executors return {status: "success"}, results are merged
    // Since both return the same key "status", the result will have size 1
    assertThat(result).containsEntry("status", "success");
    assertThat(result).isNotEmpty();
  }

  @Test
  void shouldCompensateAction() throws ExecutionException, InterruptedException {
    var executor = new CompensatableExecutor();
    registry.register(executor);

    service.compensateAction("order", "123", "compensate-action", Map.of("originalData", "value"))
        .get();

    assertThat(executor.compensated).isTrue();
    assertThat(meterRegistry.counter("workflow.executor.compensation").count()).isEqualTo(1.0);
  }

  @Test
  void shouldHandleNoExecutorsGracefully() throws ExecutionException, InterruptedException {
    var result = service.executeAction("order", "123", "unknown-action", Map.of()).get();

    assertThat(result).isEmpty();
  }

  @Test
  void shouldCalculateExponentialBackoff() {
    var executor = new RetryExecutor(3);
    registry.register(executor);

    var startTime = System.currentTimeMillis();
    try {
      service.executeAction("order", "123", "retry-action", Map.of()).get();
    } catch (Exception e) {
      // Expected
    }
    var duration = System.currentTimeMillis() - startTime;

    // With backoff: 1000ms + 2000ms + 4000ms = 7000ms minimum
    // Allow some tolerance for execution overhead
    assertThat(duration).isGreaterThanOrEqualTo(6000);
  }

  // Test executors
  static class SuccessExecutor implements WorkflowExecutor {
    private final String name;
    private final String action;

    SuccessExecutor() {
      this("success-executor", "test-action");
    }

    SuccessExecutor(String name, String action) {
      this.name = name;
      this.action = action;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public CompletableFuture<Map<String, Object>> execute(String entityType, String entityId,
        String actionCode, Map<String, Object> context) {
      return CompletableFuture.completedFuture(Map.of("status", "success"));
    }

    @Override
    public CompletableFuture<Void> compensate(String entityType, String entityId, String actionCode,
        Map<String, Object> originalContext) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean supports(String actionCode) {
      return action.equals(actionCode);
    }
  }

  static class RetryExecutor implements WorkflowExecutor {
    private final int failuresBeforeSuccess;
    final AtomicInteger attemptCount = new AtomicInteger(0);

    RetryExecutor(int failuresBeforeSuccess) {
      this.failuresBeforeSuccess = failuresBeforeSuccess;
    }

    @Override
    public String getName() {
      return "retry-executor";
    }

    @Override
    public CompletableFuture<Map<String, Object>> execute(String entityType, String entityId,
        String actionCode, Map<String, Object> context) {
      int attempt = attemptCount.incrementAndGet();
      if (attempt <= failuresBeforeSuccess) {
        return CompletableFuture.failedFuture(new RuntimeException("Simulated failure " + attempt));
      }
      return CompletableFuture.completedFuture(Map.of("status", "success"));
    }

    @Override
    public CompletableFuture<Void> compensate(String entityType, String entityId, String actionCode,
        Map<String, Object> originalContext) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean supports(String actionCode) {
      return "retry-action".equals(actionCode);
    }

    @Override
    public RetryPolicy getRetryPolicy() {
      return new RetryPolicy(5, 1000, 30000, 2.0);
    }
  }

  static class AlwaysFailExecutor implements WorkflowExecutor {
    @Override
    public String getName() {
      return "fail-executor";
    }

    @Override
    public CompletableFuture<Map<String, Object>> execute(String entityType, String entityId,
        String actionCode, Map<String, Object> context) {
      return CompletableFuture.failedFuture(new RuntimeException("Always fails"));
    }

    @Override
    public CompletableFuture<Void> compensate(String entityType, String entityId, String actionCode,
        Map<String, Object> originalContext) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean supports(String actionCode) {
      return "fail-action".equals(actionCode);
    }

    @Override
    public RetryPolicy getRetryPolicy() {
      return new RetryPolicy(3, 100, 1000, 2.0);
    }
  }

  static class CompensatableExecutor implements WorkflowExecutor {
    boolean compensated = false;

    @Override
    public String getName() {
      return "compensate-executor";
    }

    @Override
    public CompletableFuture<Map<String, Object>> execute(String entityType, String entityId,
        String actionCode, Map<String, Object> context) {
      return CompletableFuture.completedFuture(Map.of("executed", true));
    }

    @Override
    public CompletableFuture<Void> compensate(String entityType, String entityId, String actionCode,
        Map<String, Object> originalContext) {
      compensated = true;
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean supports(String actionCode) {
      return "compensate-action".equals(actionCode);
    }
  }
}
