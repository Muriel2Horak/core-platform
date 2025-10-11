package cz.muriel.core.presence.store;

import java.util.Set;

/**
 * Interface for presence storage operations Allows swapping between Redis
 * (prod) and in-memory (test) implementations
 */
public interface PresenceStore {

  // User presence
  void addUser(String key, String userId, long ttlSeconds);

  void removeUser(String key, String userId);

  Set<String> getUsers(String key);

  void refreshUserTtl(String key, long ttlSeconds);

  // Field locks
  boolean acquireLock(String key, String userId, long ttlSeconds);

  void releaseLock(String key);

  String getLockOwner(String key);

  // Stale flag
  void setStale(String key, boolean stale);

  Boolean isStale(String key);

  // Version
  void incrementVersion(String key);

  Long getVersion(String key);

  // Busy flag
  void setBusyBy(String key, String userId, long ttlSeconds);

  void clearBusyBy(String key);

  String getBusyBy(String key);

  // Cleanup
  void delete(String key);

  boolean exists(String key);
}
