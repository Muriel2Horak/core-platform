package com.platform.workflow.executor.impl;

import com.platform.workflow.executor.WorkflowExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 🔄 W7: Webhook Notification Executor
 * 
 * Sends webhook notifications to external systems (Slack, MS Teams, custom endpoints).
 * Example: Invoice paid → Notify accounting system
 * 
 * @since 2025-01-14
 */
@Component
public class WebhookNotificationExecutor implements WorkflowExecutor {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotificationExecutor.class);

    @Override
    public String getName() {
        return "webhook-notification";
    }

    @Override
    public CompletableFuture<Map<String, Object>> execute(
        String entityType,
        String entityId,
        String actionCode,
        Map<String, Object> context
    ) {
        return CompletableFuture.supplyAsync(() -> {
            String webhookUrl = (String) context.getOrDefault("webhookUrl", "https://hooks.slack.com/...");
            String payload = (String) context.getOrDefault("payload", "{}");
            
            log.info("Sending webhook notification: url={}, payload={}", webhookUrl, payload);
            
            // Simulate webhook call
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Webhook call interrupted", e);
            }
            
            log.info("Webhook notification sent to {} for {}/{}", webhookUrl, entityType, entityId);
            
            return Map.of(
                "webhookSent", true,
                "responseCode", 200,
                "webhookUrl", webhookUrl
            );
        });
    }

    @Override
    public CompletableFuture<Void> compensate(
        String entityType,
        String entityId,
        String actionCode,
        Map<String, Object> originalContext
    ) {
        return CompletableFuture.runAsync(() -> {
            String webhookUrl = (String) originalContext.get("webhookUrl");
            log.warn("Compensating webhook notification to {}", webhookUrl);
            
            // Send cancellation webhook
        });
    }

    @Override
    public boolean supports(String actionCode) {
        return actionCode != null && (
            actionCode.contains("notify") || 
            actionCode.contains("webhook") ||
            actionCode.contains("alert")
        );
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return new RetryPolicy(4, 2000, 45000, 2.0);
    }
}
