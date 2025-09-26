package cz.muriel.core.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component("tenantIdCache") 
@Slf4j
public class TenantIdCache {

  private static final int TTL_MINUTES = 5;

  private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

  public UUID getTenantId(String tenantKey) {
    CacheEntry entry = cache.get(tenantKey);
    if (entry != null && !entry.isExpired()) {
      log.debug("Cache hit for tenant: {}", tenantKey);
      entry.refreshAccess(); // Refresh-on-read
      return entry.getTenantId();
    }

    log.debug("Cache miss for tenant: {}", tenantKey);
    return null;
  }

  public void putTenantId(String tenantKey, UUID tenantId) {
    cache.put(tenantKey, new CacheEntry(tenantId));
    log.debug("Cached tenant mapping: {} -> {}", tenantKey, tenantId);
  }

  public void evict(String tenantKey) {
    cache.remove(tenantKey);
    log.debug("Evicted tenant from cache: {}", tenantKey);
  }

  public void clear() {
    cache.clear();
    log.debug("Cleared tenant cache");
  }

  // Cleanup expired entries (can be called by scheduled task)
  public void cleanupExpired() {
    int removed = 0;
    var iterator = cache.entrySet().iterator();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      if (entry.getValue().isExpired()) {
        iterator.remove();
        removed++;
      }
    }
    if (removed > 0) {
      log.debug("Cleaned up {} expired cache entries", removed);
    }
  }

  private static class CacheEntry {
    private final UUID tenantId;
    private LocalDateTime lastAccess;

    public CacheEntry(UUID tenantId) {
      this.tenantId = tenantId;
      this.lastAccess = LocalDateTime.now();
    }

    public UUID getTenantId() {
      return tenantId;
    }

    public boolean isExpired() {
      return lastAccess.isBefore(LocalDateTime.now().minusMinutes(TTL_MINUTES));
    }

    public void refreshAccess() {
      this.lastAccess = LocalDateTime.now();
    }
  }
}
