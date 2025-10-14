package com.platform.workflow.executor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 🔄 W7: Workflow Executor Interface
 * 
 * Executes automatic workflow actions (e.g., SendEmail, NotifySlack, UpdateInventory).
 * 
 * Features:
 * - Async execution with CompletableFuture
 * - Rollback/compensation on failure
 * - Metadata input/output for context passing
 * 
 * @since 2025-01-14
 */
public interface WorkflowExecutor {

    /**
     * Unique executor name (e.g., "send-email", "notify-slack")
     */
    String getName();

    /**
     * Execute the automatic action asynchronously.
     * 
     * @param entityType The type of entity (e.g., "order", "invoice")
     * @param entityId The entity identifier
     * @param actionCode The workflow action being executed
     * @param context Input metadata (e.g., email template, recipient, params)
     * @return CompletableFuture with execution result metadata
     */
    CompletableFuture<Map<String, Object>> execute(
        String entityType,
        String entityId,
        String actionCode,
        Map<String, Object> context
    );

    /**
     * Compensate/rollback the action if downstream steps fail.
     * 
     * @param entityType The type of entity
     * @param entityId The entity identifier
     * @param actionCode The workflow action
     * @param originalContext The context from successful execution
     * @return CompletableFuture with compensation result
     */
    CompletableFuture<Void> compensate(
        String entityType,
        String entityId,
        String actionCode,
        Map<String, Object> originalContext
    );

    /**
     * Check if this executor supports the given action.
     * 
     * @param actionCode The workflow action code
     * @return true if this executor handles this action
     */
    boolean supports(String actionCode);

    /**
     * Get retry policy configuration.
     * 
     * @return RetryPolicy for this executor
     */
    default RetryPolicy getRetryPolicy() {
        return RetryPolicy.DEFAULT;
    }

    /**
     * Retry policy configuration
     */
    record RetryPolicy(
        int maxAttempts,
        long initialDelayMs,
        long maxDelayMs,
        double backoffMultiplier
    ) {
        public static final RetryPolicy DEFAULT = new RetryPolicy(3, 1000, 30000, 2.0);
        public static final RetryPolicy NO_RETRY = new RetryPolicy(1, 0, 0, 1.0);
        public static final RetryPolicy AGGRESSIVE = new RetryPolicy(5, 500, 60000, 1.5);
    }
}
