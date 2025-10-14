package com.platform.workflow.executor.impl;

import com.platform.workflow.executor.WorkflowExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 🔄 W7: Update Inventory Executor
 * 
 * Updates inventory levels on order workflow transitions. Example: Order
 * confirmed → Reserve inventory
 * 
 * Demonstrates: - Critical business logic execution - Compensation with
 * inventory rollback - Custom retry policy (no retry for idempotent updates)
 * 
 * @since 2025-01-14
 */
@Component
public class UpdateInventoryExecutor implements WorkflowExecutor {

  private static final Logger log = LoggerFactory.getLogger(UpdateInventoryExecutor.class);

  @Override
  public String getName() {
    return "update-inventory";
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(String entityType, String entityId,
      String actionCode, Map<String, Object> context) {
    return CompletableFuture.supplyAsync(() -> {
      String productId = (String) context.getOrDefault("productId", "unknown");
      Integer quantity = (Integer) context.getOrDefault("quantity", 1);
      String operation = (String) context.getOrDefault("operation", "reserve");

      log.info("Updating inventory: product={}, quantity={}, operation={}", productId, quantity,
          operation);

      // Simulate inventory update
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Inventory update interrupted", e);
      }

      // Simulate success
      int newStock = 100 - quantity;
      log.info("Inventory updated: product={}, newStock={}", productId, newStock);

      return Map.of("inventoryUpdated", true, "productId", productId, "previousStock", 100,
          "newStock", newStock, "operation", operation);
    });
  }

  @Override
  public CompletableFuture<Void> compensate(String entityType, String entityId, String actionCode,
      Map<String, Object> originalContext) {
    return CompletableFuture.runAsync(() -> {
      String productId = (String) originalContext.get("productId");
      Integer newStock = (Integer) originalContext.get("newStock");
      Integer previousStock = (Integer) originalContext.get("previousStock");

      log.warn("Compensating inventory update: product={}, rollback {}→{}", productId, newStock,
          previousStock);

      // Rollback inventory change
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      log.info("Inventory rollback complete for product={}", productId);
    });
  }

  @Override
  public boolean supports(String actionCode) {
    return actionCode != null && actionCode.contains("inventory");
  }

  @Override
  public RetryPolicy getRetryPolicy() {
    // Inventory updates are idempotent in real systems, no retry needed
    return RetryPolicy.NO_RETRY;
  }
}
