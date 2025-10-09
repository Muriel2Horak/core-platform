package cz.muriel.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 🔒 CDC Lock Service
 * 
 * Provides entity-level locking to prevent concurrent CDC event processing for
 * the same entity. This prevents version conflicts when multiple CDC events for
 * the same user/group/role arrive simultaneously.
 * 
 * Key features: - Per-entity locks (not global) - Automatic lock cleanup - Fair
 * locking (FIFO order) - Timeout support
 */
@Slf4j @Service @RequiredArgsConstructor
public class CdcLockService {

  // Entity type + entity ID → Lock
  private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

  /**
   * 🔒 Acquire lock for entity
   * 
   * @param entityType - e.g. "User", "Group", "Role"
   * @param entityId - entity UUID or Keycloak ID
   * @param timeoutSeconds - max time to wait for lock
   * @return true if lock acquired, false if timeout
   */
  public boolean acquireLock(String entityType, String entityId, long timeoutSeconds) {
    String lockKey = buildLockKey(entityType, entityId);
    ReentrantLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantLock(true)); // fair=true

    try {
      boolean acquired = lock.tryLock(timeoutSeconds, TimeUnit.SECONDS);
      if (acquired) {
        log.debug("🔒 Lock acquired: {}", lockKey);
      } else {
        log.warn("⏱️ Lock timeout: {} (waited {}s)", lockKey, timeoutSeconds);
      }
      return acquired;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("❌ Lock acquisition interrupted: {}", lockKey, e);
      return false;
    }
  }

  /**
   * 🔓 Release lock for entity
   */
  public void releaseLock(String entityType, String entityId) {
    String lockKey = buildLockKey(entityType, entityId);
    ReentrantLock lock = locks.get(lockKey);

    if (lock != null && lock.isHeldByCurrentThread()) {
      lock.unlock();
      log.debug("🔓 Lock released: {}", lockKey);

      // Cleanup if no threads waiting
      if (!lock.hasQueuedThreads()) {
        locks.remove(lockKey);
        log.trace("🗑️ Lock removed (no waiters): {}", lockKey);
      }
    }
  }

  /**
   * 🔄 Execute code with entity lock
   */
  public <T> T withLock(String entityType, String entityId, long timeoutSeconds,
      LockCallback<T> callback) {
    boolean acquired = acquireLock(entityType, entityId, timeoutSeconds);
    if (!acquired) {
      throw new RuntimeException("Failed to acquire lock for " + entityType + ":" + entityId
          + " after " + timeoutSeconds + "s");
    }

    try {
      return callback.execute();
    } finally {
      releaseLock(entityType, entityId);
    }
  }

  /**
   * 🔄 Execute code with entity lock (void return)
   */
  public void withLockVoid(String entityType, String entityId, long timeoutSeconds,
      VoidLockCallback callback) {
    boolean acquired = acquireLock(entityType, entityId, timeoutSeconds);
    if (!acquired) {
      log.warn("⚠️ Skipping CDC event processing - lock timeout for {}:{}", entityType, entityId);
      return; // Skip instead of throwing exception
    }

    try {
      callback.execute();
    } finally {
      releaseLock(entityType, entityId);
    }
  }

  private String buildLockKey(String entityType, String entityId) {
    return entityType + ":" + entityId;
  }

  /**
   * 📊 Get lock statistics
   */
  public int getActiveLockCount() {
    return locks.size();
  }

  @FunctionalInterface
  public interface LockCallback<T> {
    T execute();
  }

  @FunctionalInterface
  public interface VoidLockCallback {
    void execute();
  }
}
