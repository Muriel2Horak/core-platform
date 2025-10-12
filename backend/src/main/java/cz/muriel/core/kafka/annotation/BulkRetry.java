package cz.muriel.core.kafka.annotation;

import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 📦 Bulk Retry Policy Meta-Annotation
 * 
 * For bulk operations: Batch imports, background jobs
 * 
 * Retry strategy: - 2 attempts - 10s initial delay - 1.5x exponential backoff -
 * 60s max delay - Total max duration: ~25s (10s + 15s)
 * 
 * DLT: Routes to core.platform.dlq.all
 */
@Target({
    ElementType.METHOD }) @Retention(RetentionPolicy.RUNTIME) @RetryableTopic(attempts = "${app.kafka.retry.bulk.attempts:2}", backoff = @Backoff(delayExpression = "${app.kafka.retry.bulk.delay-ms:10000}", multiplierExpression = "${app.kafka.retry.bulk.multiplier:1.5}", maxDelayExpression = "${app.kafka.retry.bulk.max-delay-ms:60000}"), kafkaTemplate = "kafkaTemplate", dltTopicSuffix = ".dlt", dltStrategy = DltStrategy.FAIL_ON_ERROR, include = Exception.class)
public @interface BulkRetry {
}
