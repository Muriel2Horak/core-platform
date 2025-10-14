package com.platform.workflow.executor.impl;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.*;

/**
 * 🧪 W7: SendEmailExecutor Unit Tests
 * 
 * @since 2025-01-14
 */
class SendEmailExecutorTest {

  private final SendEmailExecutor executor = new SendEmailExecutor();

  @Test
  void shouldHaveCorrectName() {
    assertThat(executor.getName()).isEqualTo("send-email");
  }

  @Test
  void shouldSupportEmailActions() {
    assertThat(executor.supports("send-email")).isTrue();
    assertThat(executor.supports("notify-email")).isTrue();
    assertThat(executor.supports("email-confirmation")).isTrue();
    assertThat(executor.supports("send-sms")).isFalse();
  }

  @Test
  void shouldUseAggressiveRetryPolicy() {
    var policy = executor.getRetryPolicy();

    assertThat(policy.maxAttempts()).isEqualTo(5);
    assertThat(policy.initialDelayMs()).isEqualTo(500);
  }

  @Test
  void shouldExecuteEmailSending() throws ExecutionException, InterruptedException {
    Map<String, Object> context = Map.of("emailTemplate", "order-confirmation", "recipient",
        "customer@example.com", "subject", "Order Confirmed");

    var result = executor.execute("order", "123", "send-email", context).get();

    assertThat(result.get("emailSent")).isEqualTo(true);
    assertThat(result.get("recipient")).isEqualTo("customer@example.com");
    assertThat(result).containsKey("messageId");
  }

  @Test
  void shouldUseDefaultsWhenContextEmpty() throws ExecutionException, InterruptedException {
    var result = executor.execute("order", "123", "send-email", Map.of()).get();

    assertThat(result.get("emailSent")).isEqualTo(true);
    assertThat(result.get("recipient")).isEqualTo("customer@example.com");
  }

  @Test
  void shouldCompensateEmailSend() throws ExecutionException, InterruptedException {
    Map<String, Object> context = Map.of("messageId", "msg-12345");

    // Should not throw
    executor.compensate("order", "123", "send-email", context).get();
  }
}
