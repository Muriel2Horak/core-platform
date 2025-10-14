package com.platform.workflow.executor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 🔄 W7: Workflow Execution Service
 * 
 * Orchestrates executor invocation with: - Async execution - Retry logic with
 * exponential backoff - Compensation on failure - Metrics tracking
 * 
 * @since 2025-01-14
 */
@Service
public class WorkflowExecutionService {

  private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionService.class);

  private final WorkflowExecutorRegistry registry;
  private final Timer executionTimer;
  private final Counter successCounter;
  private final Counter failureCounter;
  private final Counter retryCounter;
  private final Counter compensationCounter;

  public WorkflowExecutionService(WorkflowExecutorRegistry registry, MeterRegistry meterRegistry) {
    this.registry = registry;
    this.executionTimer = Timer.builder("workflow.executor.duration")
        .description("Executor execution duration").tag("type", "execution")
        .register(meterRegistry);
    this.successCounter = Counter.builder("workflow.executor.success")
        .description("Successful executor executions").register(meterRegistry);
    this.failureCounter = Counter.builder("workflow.executor.failure")
        .description("Failed executor executions").register(meterRegistry);
    this.retryCounter = Counter.builder("workflow.executor.retry")
        .description("Executor retry attempts").register(meterRegistry);
    this.compensationCounter = Counter.builder("workflow.executor.compensation")
        .description("Executor compensations").register(meterRegistry);
  }

  /**
   * Execute workflow action with registered executors
   */
  public CompletableFuture<Map<String, Object>> executeAction(String entityType, String entityId,
      String actionCode, Map<String, Object> context) {
    var executors = registry.findExecutorsForAction(actionCode);

    if (executors.isEmpty()) {
      log.debug("No executors found for action: {}", actionCode);
      return CompletableFuture.completedFuture(Map.of());
    }

    // Execute all matching executors in parallel
    var futures = executors.stream()
        .map(executor -> executeWithRetry(executor, entityType, entityId, actionCode, context))
        .toArray(CompletableFuture[]::new);

    return CompletableFuture.allOf(futures).thenApply(v -> {
      // Merge results from all executors
      Map<String, Object> mergedResults = new java.util.HashMap<>();
      for (var future : futures) {
        @SuppressWarnings("unchecked")
        var result = (CompletableFuture<Map<String, Object>>) future;
        mergedResults.putAll(result.join());
      }
      return mergedResults;
    });
  }

  /**
   * Execute with retry logic
   */
  private CompletableFuture<Map<String, Object>> executeWithRetry(WorkflowExecutor executor,
      String entityType, String entityId, String actionCode, Map<String, Object> context) {
    var policy = executor.getRetryPolicy();

    return executeAttempt(executor, entityType, entityId, actionCode, context, 1, policy)
        .exceptionally(ex -> {
          log.error("Executor {} failed after {} attempts: {}", executor.getName(),
              policy.maxAttempts(), ex.getMessage());
          failureCounter.increment();
          return Map.of("error", ex.getMessage());
        });
  }

  /**
   * Single execution attempt with exponential backoff
   */
  private CompletableFuture<Map<String, Object>> executeAttempt(WorkflowExecutor executor,
      String entityType, String entityId, String actionCode, Map<String, Object> context,
      int attempt, WorkflowExecutor.RetryPolicy policy) {
    var startTime = System.currentTimeMillis();

    return executor.execute(entityType, entityId, actionCode, context)
        .whenComplete((result, ex) -> {
          var duration = System.currentTimeMillis() - startTime;
          executionTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);

          if (ex == null) {
            successCounter.increment();
            log.info("Executor {} completed for {}/{} in {}ms", executor.getName(), entityType,
                entityId, duration);
          }
        }).exceptionally(ex -> {
          if (attempt < policy.maxAttempts()) {
            retryCounter.increment();
            long delay = calculateBackoff(attempt, policy);

            log.warn("Executor {} attempt {}/{} failed, retrying in {}ms: {}", executor.getName(),
                attempt, policy.maxAttempts(), delay, ex.getMessage());

            // Schedule retry
            try {
              Thread.sleep(delay);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              throw new RuntimeException(e);
            }

            return executeAttempt(executor, entityType, entityId, actionCode, context, attempt + 1,
                policy).join();
          }
          throw new RuntimeException(ex);
        });
  }

  /**
   * Compensate action execution
   */
  public CompletableFuture<Void> compensateAction(String entityType, String entityId,
      String actionCode, Map<String, Object> originalContext) {
    var executors = registry.findExecutorsForAction(actionCode);

    if (executors.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    compensationCounter.increment();

    var futures = executors.stream().map(executor -> executor
        .compensate(entityType, entityId, actionCode, originalContext).exceptionally(ex -> {
          log.error("Compensation failed for executor {}: {}", executor.getName(), ex.getMessage());
          return null;
        })).toArray(CompletableFuture[]::new);

    return CompletableFuture.allOf(futures);
  }

  /**
   * Calculate exponential backoff delay
   */
  private long calculateBackoff(int attempt, WorkflowExecutor.RetryPolicy policy) {
    long delay = (long) (policy.initialDelayMs()
        * Math.pow(policy.backoffMultiplier(), attempt - 1));
    return Math.min(delay, policy.maxDelayMs());
  }
}
