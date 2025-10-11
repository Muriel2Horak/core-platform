package cz.muriel.core.reporting.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import cz.muriel.core.reporting.service.QueryDeduplicator;

/**
 * Prometheus metrics for reporting module.
 * Exposes metrics for Grafana dashboard.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ReportingMetricsConfiguration {

    private final MeterRegistry meterRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final QueryDeduplicator queryDeduplicator;

    /**
     * Register Prometheus gauges for reporting metrics.
     */
    @Scheduled(fixedRate = 5000)
    public void registerMetrics() {
        // Query deduplication metrics
        Gauge.builder("reporting.query.inflight", queryDeduplicator, QueryDeduplicator::getInflightCount)
            .description("Number of in-flight deduplicated queries")
            .register(meterRegistry);

        // Circuit Breaker state metrics
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            String cbName = circuitBreaker.getName();

            Gauge.builder("reporting.circuit_breaker.state", circuitBreaker, cb -> {
                    switch (cb.getState()) {
                        case CLOSED: return 0.0;
                        case OPEN: return 1.0;
                        case HALF_OPEN: return 0.5;
                        default: return -1.0;
                    }
                })
                .tag("circuit_breaker", cbName)
                .description("Circuit Breaker state (0=CLOSED, 0.5=HALF_OPEN, 1=OPEN)")
                .register(meterRegistry);

            Gauge.builder("reporting.circuit_breaker.failure_rate", circuitBreaker, cb ->
                    cb.getMetrics().getFailureRate())
                .tag("circuit_breaker", cbName)
                .description("Circuit Breaker failure rate (%)")
                .register(meterRegistry);

            Gauge.builder("reporting.circuit_breaker.slow_call_rate", circuitBreaker, cb ->
                    cb.getMetrics().getSlowCallRate())
                .tag("circuit_breaker", cbName)
                .description("Circuit Breaker slow call rate (%)")
                .register(meterRegistry);
        });
    }
}
