package cz.muriel.core.kafka.annotation;

import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 📋 Normal Retry Policy Meta-Annotation
 * 
 * For normal-priority events: Logging, auditing, non-critical
 * 
 * Retry strategy: - 3 attempts - 5s initial delay - 2.0x exponential backoff -
 * 30s max delay - Total max duration: ~35s (5s + 10s + 20s)
 * 
 * DLT: Routes to core.platform.dlq.all
 */
@Target({ ElementType.METHOD }) @Retention(RetentionPolicy.RUNTIME) @RetryableTopic(attempts = "${app.kafka.retry.normal.attempts:3}", backoff = @Backoff(delayExpression = "${app.kafka.retry.normal.delay-ms:5000}", multiplierExpression = "${app.kafka.retry.normal.multiplier:2.0}", maxDelayExpression = "${app.kafka.retry.normal.max-delay-ms:30000}"), kafkaTemplate = "kafkaTemplate", dltTopicSuffix = ".dlt", dltStrategy = DltStrategy.FAIL_ON_ERROR, include = Exception.class)
public @interface NormalRetry {
}
