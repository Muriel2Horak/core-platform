package cz.muriel.core.locks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Edit lock service for preventing concurrent modifications
 */
@Slf4j @Service @RequiredArgsConstructor
public class EditLockService {

  private final EditLockRepository repository;

  /**
   * Acquire or renew lock
   * 
   * @return Acquired lock
   * @throws LockConflictException if lock is held by another user
   */
  @Transactional
  public EditLock acquireLock(String tenantId, String entityType, String entityId, String userId,
      int ttlSeconds) {

    Optional<EditLock> existing = repository.findByTenantIdAndEntityTypeAndEntityId(tenantId,
        entityType, entityId);

    if (existing.isPresent()) {
      EditLock lock = existing.get();

      // Check if expired
      if (lock.getExpiresAt().isBefore(Instant.now())) {
        // Expired, take over
        log.info("Taking over expired lock: {}/{}/{}", tenantId, entityType, entityId);
        lock.setUserId(userId);
        lock.setAcquiredAt(Instant.now());
        lock.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        return repository.save(lock);
      }

      // Check if same user (renew)
      if (lock.getUserId().equals(userId)) {
        log.debug("Renewing lock: {}/{}/{}", tenantId, entityType, entityId);
        lock.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        return repository.save(lock);
      }

      // Lock held by another user
      throw new LockConflictException("Entity is locked by another user: " + lock.getUserId(),
          lock);
    }

    // Create new lock
    EditLock lock = EditLock.builder().tenantId(tenantId).entityType(entityType).entityId(entityId)
        .userId(userId).lockType("soft").acquiredAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(ttlSeconds)).build();

    log.info("Acquired lock: {}/{}/{} by {}", tenantId, entityType, entityId, userId);
    return repository.save(lock);
  }

  /**
   * Release lock
   * 
   * @return true if released, false if not found or not owner
   */
  @Transactional
  public boolean releaseLock(String tenantId, String entityType, String entityId, String userId,
      boolean isAdmin) {

    Optional<EditLock> existing = repository.findByTenantIdAndEntityTypeAndEntityId(tenantId,
        entityType, entityId);

    if (existing.isEmpty()) {
      return false;
    }

    EditLock lock = existing.get();

    // Only owner or admin can release
    if (!lock.getUserId().equals(userId) && !isAdmin) {
      throw new LockConflictException("Cannot release lock owned by another user", lock);
    }

    repository.delete(lock);
    log.info("Released lock: {}/{}/{}", tenantId, entityType, entityId);
    return true;
  }

  /**
   * Check if entity is locked
   */
  public Optional<EditLock> getLock(String tenantId, String entityType, String entityId) {
    return repository.findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId)
        .filter(lock -> lock.getExpiresAt().isAfter(Instant.now()));
  }

  /**
   * Cleanup expired locks (runs every 15 seconds)
   */
  @Scheduled(fixedDelay = 15000) @Transactional
  public void cleanupExpiredLocks() {
    int deleted = repository.deleteExpired(Instant.now());
    if (deleted > 0) {
      log.info("Cleaned up {} expired locks", deleted);
    }
  }
}
