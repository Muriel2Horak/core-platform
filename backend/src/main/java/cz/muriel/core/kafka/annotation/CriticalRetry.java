package cz.muriel.core.kafka.annotation;

import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🔥 Critical Retry Policy Meta-Annotation
 * 
 * For critical events: User CRUD, payments, authentication
 * 
 * Retry strategy: - 5 attempts - 1s initial delay - 2.0x exponential backoff -
 * 60s max delay - Total max duration: ~123s (1s + 2s + 4s + 8s + 16s + 32s +
 * 60s)
 * 
 * DLT: Routes to core.platform.dlq.all
 */
@Target({
    ElementType.METHOD }) @Retention(RetentionPolicy.RUNTIME) @RetryableTopic(attempts = "${app.kafka.retry.critical.attempts:5}", backOff = @BackOff(delayString = "${app.kafka.retry.critical.delay-ms:1000}", multiplierString = "${app.kafka.retry.critical.multiplier:2.0}", maxDelayString = "${app.kafka.retry.critical.max-delay-ms:60000}"), kafkaTemplate = "kafkaTemplate", dltTopicSuffix = ".dlt", dltStrategy = DltStrategy.FAIL_ON_ERROR, include = Exception.class)
public @interface CriticalRetry {
}
