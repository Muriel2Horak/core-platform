package cz.muriel.core.presence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing real-time presence tracking and field-level locks
 * 
 * Redis Keys Schema: - presence:{tenant}:{entity}:{id}:users → SET of userId
 * (TTL configurable via app.presence.userTtlMs) - presence:{tenant}:{entity}:{id}:lock:{field} →
 * STRING userId (TTL configurable via app.presence.lockTtlMs, NX) - presence:{tenant}:{entity}:{id}:stale →
 * BOOLEAN (set by Kafka consumer) - presence:{tenant}:{entity}:{id}:version →
 * INT (incremented on MUTATED) - presence:{tenant}:{entity}:{id}:busyBy →
 * STRING userId (during MUTATING)
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = false)
public class PresenceService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final StringRedisTemplate stringRedisTemplate;
  
  @Value("${app.presence.userTtlMs:60000}")  // Default 60 seconds
  private long userTtlMs;
  
  @Value("${app.presence.lockTtlMs:120000}")  // Default 120 seconds
  private long lockTtlMs;

  public PresenceService(RedisTemplate<String, Object> redisTemplate,
      StringRedisTemplate stringRedisTemplate) {
    this.redisTemplate = redisTemplate;
    this.stringRedisTemplate = stringRedisTemplate;
  }

  /**
   * Subscribe user to entity presence
   * 
   * @param userId User ID
   * @param tenantId Tenant ID
   * @param entity Entity name (e.g., "Order")
   * @param id Entity ID
   */
  public void subscribe(String userId, String tenantId, String entity, String id) {
    String key = buildUsersKey(tenantId, entity, id);

    // Add user to set with TTL
    redisTemplate.opsForSet().add(key, userId);
    redisTemplate.expire(key, Duration.ofMillis(userTtlMs));

    log.debug("User {} subscribed to {}:{} (tenant: {})", userId, entity, id, tenantId);
  }

  /**
   * Unsubscribe user from entity presence
   */
  public void unsubscribe(String userId, String tenantId, String entity, String id) {
    String key = buildUsersKey(tenantId, entity, id);
    redisTemplate.opsForSet().remove(key, userId);

    log.debug("User {} unsubscribed from {}:{} (tenant: {})", userId, entity, id, tenantId);
  }

  /**
   * Send heartbeat to keep user presence alive
   */
  public void heartbeat(String userId, String tenantId, String entity, String id) {
    String key = buildUsersKey(tenantId, entity, id);

    // Check if user is in set
    Boolean isMember = redisTemplate.opsForSet().isMember(key, userId);
    if (Boolean.TRUE.equals(isMember)) {
      // Refresh TTL
      redisTemplate.expire(key, Duration.ofMillis(userTtlMs));
      log.trace("Heartbeat from user {} for {}:{}", userId, entity, id);
    } else {
      log.warn("Heartbeat from unsubscribed user {}, re-subscribing", userId);
      subscribe(userId, tenantId, entity, id);
    }
  }

  /**
   * Get all users currently viewing this entity
   */
  public Set<Object> getPresence(String tenantId, String entity, String id) {
    String key = buildUsersKey(tenantId, entity, id);
    return redisTemplate.opsForSet().members(key);
  }

  /**
   * Try to acquire lock on a specific field
   * 
   * @return true if lock acquired, false if already locked by someone else
   */
  public boolean acquireLock(String userId, String tenantId, String entity, String id,
      String field) {
    String key = buildLockKey(tenantId, entity, id, field);

    // SET NX PX - atomic operation
    Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, userId,
        Duration.ofMillis(lockTtlMs));

    if (Boolean.TRUE.equals(acquired)) {
      log.info("Lock acquired: user={}, field={}, entity={}:{}", userId, field, entity, id);
      return true;
    } else {
      String owner = stringRedisTemplate.opsForValue().get(key);
      log.warn("Lock failed: field={} already locked by {}", field, owner);
      return false;
    }
  }

  /**
   * Release lock on a specific field
   * 
   * @param userId User ID (only owner can release, or TTL expiry)
   */
  public void releaseLock(String userId, String tenantId, String entity, String id, String field) {
    String key = buildLockKey(tenantId, entity, id, field);
    String owner = stringRedisTemplate.opsForValue().get(key);

    if (owner == null) {
      log.debug("Lock release: no lock exists for field={}", field);
      return;
    }

    if (owner.equals(userId)) {
      stringRedisTemplate.delete(key);
      log.info("Lock released: user={}, field={}, entity={}:{}", userId, field, entity, id);
    } else {
      log.warn("Lock release denied: user={} tried to release lock owned by {}", userId, owner);
    }
  }

  /**
   * Get current lock owner for a field (null if not locked)
   */
  public String getLockOwner(String tenantId, String entity, String id, String field) {
    String key = buildLockKey(tenantId, entity, id, field);
    return stringRedisTemplate.opsForValue().get(key);
  }

  /**
   * Refresh lock TTL (extend expiration)
   */
  public void refreshLock(String userId, String tenantId, String entity, String id, String field) {
    String key = buildLockKey(tenantId, entity, id, field);
    String owner = stringRedisTemplate.opsForValue().get(key);

    if (userId.equals(owner)) {
      stringRedisTemplate.expire(key, lockTtlMs, TimeUnit.MILLISECONDS);
      log.trace("Lock refreshed: user={}, field={}", userId, field);
    }
  }

  /**
   * Check if entity is in "stale" mode (being modified by write pipeline)
   */
  public boolean isStale(String tenantId, String entity, String id) {
    String key = buildStaleKey(tenantId, entity, id);
    String value = stringRedisTemplate.opsForValue().get(key);
    return "true".equals(value);
  }

  /**
   * Set entity stale flag (called by Kafka consumer on MUTATING event)
   */
  public void setStale(String tenantId, String entity, String id, boolean stale, String busyBy) {
    String staleKey = buildStaleKey(tenantId, entity, id);
    String busyByKey = buildBusyByKey(tenantId, entity, id);

    if (stale) {
      stringRedisTemplate.opsForValue().set(staleKey, "true", Duration.ofMinutes(5));
      if (busyBy != null) {
        stringRedisTemplate.opsForValue().set(busyByKey, busyBy, Duration.ofMinutes(5));
      }
      log.info("Entity {}:{} marked as STALE (busyBy: {})", entity, id, busyBy);
    } else {
      stringRedisTemplate.delete(staleKey);
      stringRedisTemplate.delete(busyByKey);
      log.info("Entity {}:{} marked as NOT STALE", entity, id);
    }
  }

  /**
   * Get user ID who is currently modifying the entity (during MUTATING)
   */
  public String getBusyBy(String tenantId, String entity, String id) {
    String key = buildBusyByKey(tenantId, entity, id);
    return stringRedisTemplate.opsForValue().get(key);
  }

  /**
   * Get current version of entity
   */
  public Long getVersion(String tenantId, String entity, String id) {
    String key = buildVersionKey(tenantId, entity, id);
    String value = stringRedisTemplate.opsForValue().get(key);
    return value != null ? Long.parseLong(value) : null;
  }

  /**
   * Increment version (called by Kafka consumer on MUTATED event)
   */
  public Long incrementVersion(String tenantId, String entity, String id) {
    String key = buildVersionKey(tenantId, entity, id);
    Long version = stringRedisTemplate.opsForValue().increment(key);
    stringRedisTemplate.expire(key, Duration.ofHours(24)); // Keep version for 24h
    log.info("Entity {}:{} version incremented to {}", entity, id, version);
    return version;
  }

  // ========== Key Builders ==========

  private String buildUsersKey(String tenantId, String entity, String id) {
    return String.format("presence:%s:%s:%s:users", tenantId, entity, id);
  }

  private String buildLockKey(String tenantId, String entity, String id, String field) {
    return String.format("presence:%s:%s:%s:lock:%s", tenantId, entity, id, field);
  }

  private String buildStaleKey(String tenantId, String entity, String id) {
    return String.format("presence:%s:%s:%s:stale", tenantId, entity, id);
  }

  private String buildBusyByKey(String tenantId, String entity, String id) {
    return String.format("presence:%s:%s:%s:busyBy", tenantId, entity, id);
  }

  private String buildVersionKey(String tenantId, String entity, String id) {
    return String.format("presence:%s:%s:%s:version", tenantId, entity, id);
  }
}
