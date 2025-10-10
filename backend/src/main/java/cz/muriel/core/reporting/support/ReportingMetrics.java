package cz.muriel.core.reporting.support;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Metrics collector for Reporting module using Micrometer.
 */
@Slf4j @Component
public class ReportingMetrics {

  private final MeterRegistry registry;
  private final Counter queryRequestCounter;
  private final Counter queryCacheHitCounter;
  private final Counter queryCacheMissCounter;

  public ReportingMetrics(MeterRegistry registry) {
    this.registry = registry;

    // Query request counter
    this.queryRequestCounter = Counter.builder("reporting.query.requests")
        .description("Total number of report queries").tag("module", "reporting")
        .register(registry);

    // Cache metrics
    this.queryCacheHitCounter = Counter.builder("reporting.cache.hits")
        .description("Number of cache hits").tag("module", "reporting").register(registry);

    this.queryCacheMissCounter = Counter.builder("reporting.cache.misses")
        .description("Number of cache misses").tag("module", "reporting").register(registry);
  }

  /**
   * Record query request.
   */
  public void recordQueryRequest() {
    queryRequestCounter.increment();
  }

  /**
   * Record cache hit.
   */
  public void recordCacheHit() {
    queryCacheHitCounter.increment();
  }

  /**
   * Record cache miss.
   */
  public void recordCacheMiss() {
    queryCacheMissCounter.increment();
  }

  /**
   * Record query error.
   */
  public void recordQueryError(String errorType) {
    Counter.builder("reporting.query.errors").tag("module", "reporting")
        .tag("error_type", errorType).register(registry).increment();
  }

  /**
   * Record rate limit exceeded.
   */
  public void recordRateLimitExceeded(String tenantId) {
    Counter.builder("reporting.ratelimit.exceeded").tag("module", "reporting")
        .tag("tenant_id", tenantId).register(registry).increment();
  }

  /**
   * Record query execution time.
   */
  public void recordQueryExecution(long durationMs, String entity, boolean cacheHit) {
    Timer.builder("reporting.query.duration").tag("module", "reporting").tag("entity", entity)
        .tag("cache_hit", String.valueOf(cacheHit)).register(registry)
        .record(durationMs, TimeUnit.MILLISECONDS);
  }

  /**
   * Record Cube.js API call duration.
   */
  public void recordCubeApiCall(long durationMs, boolean success) {
    Timer.builder("reporting.cube.api.duration").tag("module", "reporting")
        .tag("success", String.valueOf(success)).register(registry)
        .record(durationMs, TimeUnit.MILLISECONDS);
  }

  /**
   * Get cache hit rate.
   */
  public double getCacheHitRate() {
    double hits = queryCacheHitCounter.count();
    double misses = queryCacheMissCounter.count();
    double total = hits + misses;
    return total > 0 ? (hits / total) * 100 : 0;
  }
}
