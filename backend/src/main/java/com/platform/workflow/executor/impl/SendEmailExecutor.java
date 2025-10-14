package com.platform.workflow.executor.impl;

import com.platform.workflow.executor.WorkflowExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 🔄 W7: Send Email Executor
 * 
 * Sends email notifications on workflow transitions.
 * Example: Order approved → Send confirmation email to customer
 * 
 * @since 2025-01-14
 */
@Component
public class SendEmailExecutor implements WorkflowExecutor {

    private static final Logger log = LoggerFactory.getLogger(SendEmailExecutor.class);

    @Override
    public String getName() {
        return "send-email";
    }

    @Override
    public CompletableFuture<Map<String, Object>> execute(
        String entityType,
        String entityId,
        String actionCode,
        Map<String, Object> context
    ) {
        return CompletableFuture.supplyAsync(() -> {
            String template = (String) context.getOrDefault("emailTemplate", "default");
            String recipient = (String) context.getOrDefault("recipient", "customer@example.com");
            String subject = (String) context.getOrDefault("subject", "Workflow Update");
            
            log.info("Sending email: template={}, recipient={}, subject={}", template, recipient, subject);
            
            // Simulate email sending
            try {
                Thread.sleep(500); // Simulate API call
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Email sending interrupted", e);
            }
            
            log.info("Email sent successfully to {} for {}/{}", recipient, entityType, entityId);
            
            return Map.of(
                "emailSent", true,
                "messageId", "msg-" + System.currentTimeMillis(),
                "recipient", recipient
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
            String messageId = (String) originalContext.get("messageId");
            log.warn("Compensating email send: messageId={} (sending cancellation notice)", messageId);
            
            // In real implementation: Send "cancelled" email or mark as void
        });
    }

    @Override
    public boolean supports(String actionCode) {
        // This executor supports any action with "email" in the code
        return actionCode != null && actionCode.contains("email");
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        // Email sending should retry aggressively (external API may be flaky)
        return RetryPolicy.AGGRESSIVE;
    }
}
