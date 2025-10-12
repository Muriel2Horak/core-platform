package cz.muriel.core.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 🔄 Kafka Retry Policy Configuration
 * 
 * Defines standardized retry policies for Kafka consumers based on severity:
 * 
 * - CRITICAL: User CRUD, payments, auth (5 attempts, 1s-60s) - HIGH: Notifications,
 * analytics, reporting (4 attempts, 2s-30s) - NORMAL: Logging, auditing (3
 * attempts, 5s-30s) - BULK: Batch operations, imports (2 attempts, 10s-60s)
 */
@Configuration @ConfigurationProperties(prefix = "app.kafka.retry") @Data
public class KafkaRetryPolicyConfig {

  private RetryPolicy critical = new RetryPolicy(5, 1000L, 2.0, 60000L);
  private RetryPolicy high = new RetryPolicy(4, 2000L, 2.0, 30000L);
  private RetryPolicy normal = new RetryPolicy(3, 5000L, 2.0, 30000L);
  private RetryPolicy bulk = new RetryPolicy(2, 10000L, 1.5, 60000L);

  @Data
  public static class RetryPolicy {
    private int attempts;
    private long delayMs;
    private double multiplier;
    private long maxDelayMs;

    public RetryPolicy() {
    }

    public RetryPolicy(int attempts, long delayMs, double multiplier, long maxDelayMs) {
      this.attempts = attempts;
      this.delayMs = delayMs;
      this.multiplier = multiplier;
      this.maxDelayMs = maxDelayMs;
    }

    /**
     * Calculate total max retry duration (for monitoring/alerting)
     */
    public long getTotalMaxDurationMs() {
      long total = 0;
      long currentDelay = delayMs;
      for (int i = 0; i < attempts; i++) {
        total += Math.min(currentDelay, maxDelayMs);
        currentDelay = (long) (currentDelay * multiplier);
      }
      return total;
    }
  }
}
