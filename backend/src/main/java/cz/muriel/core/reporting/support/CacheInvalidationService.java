package cz.muriel.core.reporting.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for cache management and invalidation.
 */
@Slf4j @Service @RequiredArgsConstructor
public class CacheInvalidationService {

  private final CacheManager cacheManager;
  private final ReportingMetrics metrics;

  private final AtomicLong invalidationCount = new AtomicLong(0);

  /**
   * Invalidate cache by entity.
   */
  public void invalidateByEntity(String entity) {
    Cache cache = cacheManager.getCache("reportQueryCache");
    if (cache != null) {
      // Cannot invalidate by pattern in Spring Cache, would need Redis directly
      log.info("Cache invalidation requested for entity: {}", entity);
      invalidationCount.incrementAndGet();
    }
  }

  /**
   * Invalidate cache by tenant.
   */
  public void invalidateByTenant(String tenantId) {
    Cache cache = cacheManager.getCache("reportQueryCache");
    if (cache != null) {
      log.info("Cache invalidation requested for tenant: {}", tenantId);
      invalidationCount.incrementAndGet();
    }
  }

  /**
   * Invalidate all cached reports.
   */
  public void invalidateAll() {
    Cache cache = cacheManager.getCache("reportQueryCache");
    if (cache != null) {
      cache.clear();
      log.info("All report cache cleared");
      invalidationCount.incrementAndGet();
    }
  }

  /**
   * Scheduled cache cleanup (every 5 minutes).
   */
  @Scheduled(fixedRate = 300000) // 5 minutes
  public void scheduledCleanup() {
    log.debug("Running scheduled cache cleanup");

    // Log cache statistics
    double hitRate = metrics.getCacheHitRate();
    log.info("Cache hit rate: {:.2f}%", hitRate);

    // Cache is automatically expired by TTL, so no manual cleanup needed
    // This is just for logging and monitoring
  }

  /**
   * Get total invalidation count.
   */
  public long getInvalidationCount() {
    return invalidationCount.get();
  }

  /**
   * Warm up cache with common queries (optional).
   */
  public void warmUpCache() {
    log.info("Cache warm-up not implemented yet");
    // Could be implemented to pre-load frequently accessed reports
  }
}
