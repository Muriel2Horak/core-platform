package cz.muriel.core.cache;

import cz.muriel.core.entity.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache pro tenant data s TTL (Time To Live) mechanismem. Optimalizuje výkon
 * při častých dotazech na tenant informace.
 */
@Component @Slf4j
public class TenantCache {

  private static final int DEFAULT_TTL_MINUTES = 5;

  private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
  private final int ttlMinutes;

  public TenantCache() {
    this.ttlMinutes = DEFAULT_TTL_MINUTES;
  }

  /**
   * Získá tenant z cache nebo prázdný Optional pokud není nalezen nebo je
   * expired.
   */
  public Optional<Tenant> get(String tenantKey) {
    CacheEntry entry = cache.get(tenantKey);

    if (entry == null) {
      log.debug("Cache miss for tenant: {}", tenantKey);
      return Optional.empty();
    }

    if (entry.isExpired()) {
      cache.remove(tenantKey);
      log.debug("Cache entry expired for tenant: {}", tenantKey);
      return Optional.empty();
    }

    log.debug("Cache hit for tenant: {}", tenantKey);
    return Optional.of(entry.tenant);
  }

  /**
   * Uloží tenant do cache s TTL.
   */
  public void put(String tenantKey, Tenant tenant) {
    cache.put(tenantKey, new CacheEntry(tenant, LocalDateTime.now().plusMinutes(ttlMinutes)));
    log.debug("Cached tenant: {} (TTL: {} minutes)", tenantKey, ttlMinutes);
  }

  /**
   * Odstraní tenant z cache.
   */
  public void evict(String tenantKey) {
    cache.remove(tenantKey);
    log.debug("Evicted tenant from cache: {}", tenantKey);
  }

  /**
   * Vyčistí celou cache.
   */
  public void clear() {
    int size = cache.size();
    cache.clear();
    log.debug("Cleared cache ({} entries)", size);
  }

  /**
   * Vrátí aktuální velikost cache.
   */
  public int size() {
    return cache.size();
  }

  /**
   * Vyčistí expirované záznamy z cache.
   */
  public void cleanupExpired() {
    LocalDateTime now = LocalDateTime.now();
    cache.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
    log.debug("Cleaned up expired cache entries");
  }

  /**
   * Vnitřní třída pro cache entry s TTL.
   */
  private static class CacheEntry {
    final Tenant tenant;
    final LocalDateTime expiresAt;

    CacheEntry(Tenant tenant, LocalDateTime expiresAt) {
      this.tenant = tenant;
      this.expiresAt = expiresAt;
    }

    boolean isExpired() {
      return LocalDateTime.now().isAfter(expiresAt);
    }
  }
}
