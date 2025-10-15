package cz.muriel.core.presence;

import cz.muriel.core.test.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for PresenceService with real Redis
 * 
 * Uses Redis container from AbstractIntegrationTest
 * 
 * NOTE: Uses SAME_THREAD execution mode because tests verify Redis lock atomicity
 * and race conditions. Parallel execution interferes with timing-sensitive assertions.
 */
@SpringBootTest
@Execution(ExecutionMode.SAME_THREAD)
class PresenceServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private PresenceService presenceService;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  // Use dynamic tenant ID for parallel test isolation
  private String tenantId;

  private static final String ENTITY = "Order";
  private static final String ID = "123";
  private static final String USER_1 = "user1";
  private static final String USER_2 = "user2";

  @BeforeEach
  void setUp() {
    // Use unique tenant ID per test (inherited topicSuffix from AbstractIntegrationTest)
    tenantId = topicSuffix;
  }

  @AfterEach
  void tearDown() {
    // Ensure cleanup
    presenceService.unsubscribe(USER_1, tenantId, ENTITY, ID);
    presenceService.unsubscribe(USER_2, tenantId, ENTITY, ID);
  }

  @Test
  void shouldTrackUserPresence() {
    // Subscribe user 1
    presenceService.subscribe(USER_1, tenantId, ENTITY, ID);

    // Get presence
    Set<Object> users = presenceService.getPresence(tenantId, ENTITY, ID);

    // Verify
    assertThat(users).contains(USER_1);
  }

  @Test
  void shouldTrackMultipleUsers() {
    // Subscribe both users
    presenceService.subscribe(USER_1, tenantId, ENTITY, ID);
    presenceService.subscribe(USER_2, tenantId, ENTITY, ID);

    // Get presence
    Set<Object> users = presenceService.getPresence(tenantId, ENTITY, ID);

    // Verify both users
    assertThat(users).containsExactlyInAnyOrder(USER_1, USER_2);
  }

  @Test
  void shouldRemoveUserOnUnsubscribe() {
    // Subscribe and unsubscribe
    presenceService.subscribe(USER_1, tenantId, ENTITY, ID);
    presenceService.unsubscribe(USER_1, tenantId, ENTITY, ID);

    // Get presence
    Set<Object> users = presenceService.getPresence(tenantId, ENTITY, ID);

    // Verify user removed
    assertThat(users).doesNotContain(USER_1);
  }

  /**
   * ⏱️ SLOW TEST: Waits 62 seconds for presence TTL expiration
   */
  @Test @Disabled("Slow test - waits 62s for TTL expiration. Enable for manual testing.")
  void shouldExpirePresenceAfterTTL() {
    // Subscribe user
    presenceService.subscribe(USER_1, tenantId, ENTITY, ID);

    // Wait for TTL expiration (60s + 1s buffer)
    await().atMost(java.time.Duration.ofSeconds(62)).untilAsserted(() -> {
      Set<Object> users = presenceService.getPresence(tenantId, ENTITY, ID);
      assertThat(users).isEmpty();
    });
  }

  /**
   * ⏱️ SLOW TEST: Waits 81 seconds with heartbeat refresh
   */
  @Test @Disabled("Slow test - waits 81s testing heartbeat refresh. Enable for manual testing.")
  void shouldRefreshTTLOnHeartbeat() {
    // Subscribe user
    presenceService.subscribe(USER_1, tenantId, ENTITY, ID);

    // Wait 40s and send heartbeat
    await().pollDelay(java.time.Duration.ofSeconds(40)).atMost(java.time.Duration.ofSeconds(41))
        .untilAsserted(() -> {
          presenceService.heartbeat(USER_1, tenantId, ENTITY, ID);
          Set<Object> users = presenceService.getPresence(tenantId, ENTITY, ID);
          assertThat(users).contains(USER_1); // Still present after 40s
        });

    // Wait another 40s (80s total, would expire without heartbeat)
    await().pollDelay(java.time.Duration.ofSeconds(40)).atMost(java.time.Duration.ofSeconds(41))
        .untilAsserted(() -> {
          Set<Object> users = presenceService.getPresence(tenantId, ENTITY, ID);
          assertThat(users).contains(USER_1); // Still present due to heartbeat
        });
  }

  @Test
  void shouldAcquireLockAtomically() {
    String field = "totalAmount";

    // User 1 acquires lock
    boolean acquired1 = presenceService.acquireLock(USER_1, tenantId, ENTITY, ID, field);
    assertThat(acquired1).isTrue();

    // User 2 tries to acquire same lock
    boolean acquired2 = presenceService.acquireLock(USER_2, tenantId, ENTITY, ID, field);
    assertThat(acquired2).isFalse();

    // Verify lock owner
    String owner = presenceService.getLockOwner(tenantId, ENTITY, ID, field);
    assertThat(owner).isEqualTo(USER_1);
  }

  @Test
  void shouldReleaseLockOnlyByOwner() {
    String field = "totalAmount";

    // User 1 acquires lock
    presenceService.acquireLock(USER_1, tenantId, ENTITY, ID, field);

    // User 2 tries to release (should fail)
    presenceService.releaseLock(USER_2, tenantId, ENTITY, ID, field);
    String owner = presenceService.getLockOwner(tenantId, ENTITY, ID, field);
    assertThat(owner).isEqualTo(USER_1); // Still locked by user 1

    // User 1 releases
    presenceService.releaseLock(USER_1, tenantId, ENTITY, ID, field);
    owner = presenceService.getLockOwner(tenantId, ENTITY, ID, field);
    assertThat(owner).isNull(); // Lock released
  }

  /**
   * ⏱️ SLOW TEST: Waits 122 seconds for lock TTL expiration
   */
  @Test @Disabled("Slow test - waits 122s for TTL expiration. Enable for manual testing.")
  void shouldExpireLockAfterTTL() {
    String field = "totalAmount";

    // Acquire lock
    presenceService.acquireLock(USER_1, tenantId, ENTITY, ID, field);

    // Wait for TTL expiration (120s + 1s buffer)
    await().atMost(java.time.Duration.ofSeconds(122)).untilAsserted(() -> {
      String owner = presenceService.getLockOwner(tenantId, ENTITY, ID, field);
      assertThat(owner).isNull();
    });
  }

  /**
   * ⏱️ SLOW TEST: Waits 81 seconds then refreshes lock
   */
  @Test @Disabled("Slow test - waits 81s before refresh. Enable for manual testing.")
  void shouldRefreshLockTTL() {
    String field = "totalAmount";

    // Acquire lock
    presenceService.acquireLock(USER_1, tenantId, ENTITY, ID, field);

    // Wait 80s and refresh
    await().pollDelay(java.time.Duration.ofSeconds(80)).atMost(java.time.Duration.ofSeconds(81))
        .untilAsserted(() -> {
          presenceService.refreshLock(USER_1, tenantId, ENTITY, ID, field);
          String owner = presenceService.getLockOwner(tenantId, ENTITY, ID, field);
          assertThat(owner).isEqualTo(USER_1); // Still locked
        });
  }

  @Test
  void shouldSetStaleFlag() {
    // Mark as stale
    presenceService.setStale(tenantId, ENTITY, ID, true, USER_1);

    // Verify stale
    boolean isStale = presenceService.isStale(tenantId, ENTITY, ID);
    assertThat(isStale).isTrue();

    String busyBy = presenceService.getBusyBy(tenantId, ENTITY, ID);
    assertThat(busyBy).isEqualTo(USER_1);
  }

  @Test
  void shouldClearStaleFlag() {
    // Mark as stale
    presenceService.setStale(tenantId, ENTITY, ID, true, USER_1);

    // Clear stale
    presenceService.setStale(tenantId, ENTITY, ID, false, null);

    // Verify not stale
    boolean isStale = presenceService.isStale(tenantId, ENTITY, ID);
    assertThat(isStale).isFalse();

    String busyBy = presenceService.getBusyBy(tenantId, ENTITY, ID);
    assertThat(busyBy).isNull();
  }

  @Test
  void shouldIncrementVersion() {
    // Increment version 3 times
    Long v1 = presenceService.incrementVersion(tenantId, ENTITY, ID);
    Long v2 = presenceService.incrementVersion(tenantId, ENTITY, ID);
    Long v3 = presenceService.incrementVersion(tenantId, ENTITY, ID);

    // Verify versions
    assertThat(v1).isEqualTo(1);
    assertThat(v2).isEqualTo(2);
    assertThat(v3).isEqualTo(3);

    // Verify getVersion
    Long current = presenceService.getVersion(tenantId, ENTITY, ID);
    assertThat(current).isEqualTo(3);
  }

  @Test
  void shouldIsolateTenantsAndEntities() {
    // Subscribe user1 to different entities
    presenceService.subscribe(USER_1, tenantId, "Order", "1");
    presenceService.subscribe(USER_1, tenantId, "Order", "2");
    presenceService.subscribe(USER_1, tenantId, "Product", "1");

    // Verify isolation
    assertThat(presenceService.getPresence(tenantId, "Order", "1")).contains(USER_1);
    assertThat(presenceService.getPresence(tenantId, "Order", "2")).contains(USER_1);
    assertThat(presenceService.getPresence(tenantId, "Product", "1")).contains(USER_1);
    assertThat(presenceService.getPresence(tenantId, "Order", "999")).isEmpty();
  }
}
