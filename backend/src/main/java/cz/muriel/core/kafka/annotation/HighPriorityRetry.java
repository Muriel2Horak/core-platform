package cz.muriel.core.kafka.annotation;

import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ⚡ High Priority Retry Policy Meta-Annotation
 * 
 * For high-priority events: Notifications, analytics, reporting
 * 
 * Retry strategy: - 4 attempts - 2s initial delay - 2.0x exponential backoff -
 * 30s max delay - Total max duration: ~30s (2s + 4s + 8s + 16s)
 * 
 * DLT: Routes to core.platform.dlq.all
 */
@Target({
    ElementType.METHOD }) @Retention(RetentionPolicy.RUNTIME) @RetryableTopic(attempts = "${app.kafka.retry.high.attempts:4}", backoff = @Backoff(delayExpression = "${app.kafka.retry.high.delay-ms:2000}", multiplierExpression = "${app.kafka.retry.high.multiplier:2.0}", maxDelayExpression = "${app.kafka.retry.high.max-delay-ms:30000}"), kafkaTemplate = "kafkaTemplate", dltTopicSuffix = ".dlt", dltStrategy = DltStrategy.FAIL_ON_ERROR, include = Exception.class)
public @interface HighPriorityRetry {
}
