package cz.muriel.core.reporting.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker configuration for Cube.js client
 * 
 * Protects backend from Cube.js failures: - Opens circuit after 50% failure
 * rate in 10 requests - Half-open after 30 seconds - Fallback: Return cached
 * data or error response
 * 
 * Monitoring: - Metrics exposed via
 * /actuator/metrics/resilience4j.circuitbreaker - Grafana dashboard for CB
 * state changes
 */
@Slf4j @Configuration
public class CircuitBreakerConfiguration {

  /**
   * Circuit Breaker for Cube.js query client
   */
  @Bean
  public CircuitBreaker cubeQueryCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        // Failure rate threshold: open circuit if 50% of calls fail
        .failureRateThreshold(50)

        // Slow call threshold: consider calls >5s as slow
        .slowCallDurationThreshold(Duration.ofSeconds(5)).slowCallRateThreshold(50) // Open if 50%
                                                                                    // of calls are
                                                                                    // slow

        // Sliding window: last 10 calls
        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED).slidingWindowSize(10)
        .minimumNumberOfCalls(5) // Need at least 5 calls before calculating failure rate

        // Wait 30 seconds in OPEN state before transitioning to HALF_OPEN
        .waitDurationInOpenState(Duration.ofSeconds(30))

        // In HALF_OPEN, allow 5 test calls
        .permittedNumberOfCallsInHalfOpenState(5)

        // Automatically transition from OPEN to HALF_OPEN
        .automaticTransitionFromOpenToHalfOpenEnabled(true)

        // Ignore specific exceptions (don't count as failures)
        .ignoreExceptions(IllegalArgumentException.class)

        // Record exceptions (count as failures)
        .recordExceptions(java.net.SocketTimeoutException.class, java.net.ConnectException.class,
            java.io.IOException.class)

        .build();

    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("cubeQuery", config);

    // Event listeners for monitoring
    circuitBreaker.getEventPublisher()
        .onStateTransition(event -> log.warn("Cube Circuit Breaker state changed: {} -> {}",
            event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
        .onError(event -> log.error("Cube query failed: {}", event.getThrowable().getMessage()))
        .onSuccess(event -> log.debug("Cube query succeeded in {}ms",
            event.getElapsedDuration().toMillis()))
        .onCallNotPermitted(event -> log.warn("Cube query rejected: Circuit Breaker is OPEN"));

    return circuitBreaker;
  }

  /**
   * Circuit Breaker for Cube.js load endpoint (metadata queries)
   */
  @Bean
  public CircuitBreaker cubeMetaCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
    // More lenient config for metadata queries (less critical)
    CircuitBreakerConfig config = CircuitBreakerConfig.custom().failureRateThreshold(70) // 70%
                                                                                         // failure
                                                                                         // rate
        .slowCallDurationThreshold(Duration.ofSeconds(10)).slidingWindowSize(20)
        .minimumNumberOfCalls(10).waitDurationInOpenState(Duration.ofSeconds(60)).build();

    return circuitBreakerRegistry.circuitBreaker("cubeMeta", config);
  }
}
