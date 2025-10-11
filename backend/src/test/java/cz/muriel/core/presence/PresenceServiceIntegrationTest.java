package cz.muriel.core.presence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for PresenceService with real Redis
 * 
 * Uses Testcontainers to spin up Redis instance
 */
@SpringBootTest @Testcontainers
class PresenceServiceIntegrationTest {

  @Container
  static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
      .withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("app.redis.enabled", () -> "true");
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", redis::getFirstMappedPort);
  }

  @Autowired
  private PresenceService presenceService;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  private static final String TENANT_ID = "test-tenant";
  private static final String ENTITY = "Order";
  private static final String ID = "123";
  private static final String USER_1 = "user1";
  private static final String USER_2 = "user2";

  @BeforeEach
  void setUp() {
    // Clean up Redis before each test
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  @AfterEach
  void tearDown() {
    // Ensure cleanup
    presenceService.unsubscribe(USER_1, TENANT_ID, ENTITY, ID);
    presenceService.unsubscribe(USER_2, TENANT_ID, ENTITY, ID);
  }

  @Test
  void shouldTrackUserPresence() {
    // Subscribe user 1
    presenceService.subscribe(USER_1, TENANT_ID, ENTITY, ID);

    // Get presence
    Set<Object> users = presenceService.getPresence(TENANT_ID, ENTITY, ID);

    // Verify
    assertThat(users).contains(USER_1);
  }

  @Test
  void shouldTrackMultipleUsers() {
    // Subscribe both users
    presenceService.subscribe(USER_1, TENANT_ID, ENTITY, ID);
    presenceService.subscribe(USER_2, TENANT_ID, ENTITY, ID);

    // Get presence
    Set<Object> users = presenceService.getPresence(TENANT_ID, ENTITY, ID);

    // Verify both users
    assertThat(users).containsExactlyInAnyOrder(USER_1, USER_2);
  }

  @Test
  void shouldRemoveUserOnUnsubscribe() {
    // Subscribe and unsubscribe
    presenceService.subscribe(USER_1, TENANT_ID, ENTITY, ID);
    presenceService.unsubscribe(USER_1, TENANT_ID, ENTITY, ID);

    // Get presence
    Set<Object> users = presenceService.getPresence(TENANT_ID, ENTITY, ID);

    // Verify user removed
    assertThat(users).doesNotContain(USER_1);
  }

  @Test
  void shouldExpirePresenceAfterTTL() {
    // Subscribe user
    presenceService.subscribe(USER_1, TENANT_ID, ENTITY, ID);

    // Wait for TTL expiration (60s + 1s buffer)
    await().atMost(java.time.Duration.ofSeconds(62)).untilAsserted(() -> {
      Set<Object> users = presenceService.getPresence(TENANT_ID, ENTITY, ID);
      assertThat(users).isEmpty();
    });
  }

  @Test
  void shouldRefreshTTLOnHeartbeat() {
    // Subscribe user
    presenceService.subscribe(USER_1, TENANT_ID, ENTITY, ID);

    // Wait 40s and send heartbeat
    await().pollDelay(java.time.Duration.ofSeconds(40)).atMost(java.time.Duration.ofSeconds(41))
        .untilAsserted(() -> {
          presenceService.heartbeat(USER_1, TENANT_ID, ENTITY, ID);
          Set<Object> users = presenceService.getPresence(TENANT_ID, ENTITY, ID);
          assertThat(users).contains(USER_1); // Still present after 40s
        });

    // Wait another 40s (80s total, would expire without heartbeat)
    await().pollDelay(java.time.Duration.ofSeconds(40)).atMost(java.time.Duration.ofSeconds(41))
        .untilAsserted(() -> {
          Set<Object> users = presenceService.getPresence(TENANT_ID, ENTITY, ID);
          assertThat(users).contains(USER_1); // Still present due to heartbeat
        });
  }

  @Test
  void shouldAcquireLockAtomically() {
    String field = "totalAmount";

    // User 1 acquires lock
    boolean acquired1 = presenceService.acquireLock(USER_1, TENANT_ID, ENTITY, ID, field);
    assertThat(acquired1).isTrue();

    // User 2 tries to acquire same lock
    boolean acquired2 = presenceService.acquireLock(USER_2, TENANT_ID, ENTITY, ID, field);
    assertThat(acquired2).isFalse();

    // Verify lock owner
    String owner = presenceService.getLockOwner(TENANT_ID, ENTITY, ID, field);
    assertThat(owner).isEqualTo(USER_1);
  }

  @Test
  void shouldReleaseLockOnlyByOwner() {
    String field = "totalAmount";

    // User 1 acquires lock
    presenceService.acquireLock(USER_1, TENANT_ID, ENTITY, ID, field);

    // User 2 tries to release (should fail)
    presenceService.releaseLock(USER_2, TENANT_ID, ENTITY, ID, field);
    String owner = presenceService.getLockOwner(TENANT_ID, ENTITY, ID, field);
    assertThat(owner).isEqualTo(USER_1); // Still locked by user 1

    // User 1 releases
    presenceService.releaseLock(USER_1, TENANT_ID, ENTITY, ID, field);
    owner = presenceService.getLockOwner(TENANT_ID, ENTITY, ID, field);
    assertThat(owner).isNull(); // Lock released
  }

  @Test
  void shouldExpireLockAfterTTL() {
    String field = "totalAmount";

    // Acquire lock
    presenceService.acquireLock(USER_1, TENANT_ID, ENTITY, ID, field);

    // Wait for TTL expiration (120s + 1s buffer)
    await().atMost(java.time.Duration.ofSeconds(122)).untilAsserted(() -> {
      String owner = presenceService.getLockOwner(TENANT_ID, ENTITY, ID, field);
      assertThat(owner).isNull();
    });
  }

  @Test
  void shouldRefreshLockTTL() {
    String field = "totalAmount";

    // Acquire lock
    presenceService.acquireLock(USER_1, TENANT_ID, ENTITY, ID, field);

    // Wait 80s and refresh
    await().pollDelay(java.time.Duration.ofSeconds(80)).atMost(java.time.Duration.ofSeconds(81))
        .untilAsserted(() -> {
          presenceService.refreshLock(USER_1, TENANT_ID, ENTITY, ID, field);
          String owner = presenceService.getLockOwner(TENANT_ID, ENTITY, ID, field);
          assertThat(owner).isEqualTo(USER_1); // Still locked
        });
  }

  @Test
  void shouldSetStaleFlag() {
    // Mark as stale
    presenceService.setStale(TENANT_ID, ENTITY, ID, true, USER_1);

    // Verify stale
    boolean isStale = presenceService.isStale(TENANT_ID, ENTITY, ID);
    assertThat(isStale).isTrue();

    String busyBy = presenceService.getBusyBy(TENANT_ID, ENTITY, ID);
    assertThat(busyBy).isEqualTo(USER_1);
  }

  @Test
  void shouldClearStaleFlag() {
    // Mark as stale
    presenceService.setStale(TENANT_ID, ENTITY, ID, true, USER_1);

    // Clear stale
    presenceService.setStale(TENANT_ID, ENTITY, ID, false, null);

    // Verify not stale
    boolean isStale = presenceService.isStale(TENANT_ID, ENTITY, ID);
    assertThat(isStale).isFalse();

    String busyBy = presenceService.getBusyBy(TENANT_ID, ENTITY, ID);
    assertThat(busyBy).isNull();
  }

  @Test
  void shouldIncrementVersion() {
    // Increment version 3 times
    Long v1 = presenceService.incrementVersion(TENANT_ID, ENTITY, ID);
    Long v2 = presenceService.incrementVersion(TENANT_ID, ENTITY, ID);
    Long v3 = presenceService.incrementVersion(TENANT_ID, ENTITY, ID);

    // Verify versions
    assertThat(v1).isEqualTo(1);
    assertThat(v2).isEqualTo(2);
    assertThat(v3).isEqualTo(3);

    // Verify getVersion
    Long current = presenceService.getVersion(TENANT_ID, ENTITY, ID);
    assertThat(current).isEqualTo(3);
  }

  @Test
  void shouldIsolateTenantsAndEntities() {
    // Subscribe user1 to different entities
    presenceService.subscribe(USER_1, TENANT_ID, "Order", "1");
    presenceService.subscribe(USER_1, TENANT_ID, "Order", "2");
    presenceService.subscribe(USER_1, TENANT_ID, "Product", "1");

    // Verify isolation
    assertThat(presenceService.getPresence(TENANT_ID, "Order", "1")).contains(USER_1);
    assertThat(presenceService.getPresence(TENANT_ID, "Order", "2")).contains(USER_1);
    assertThat(presenceService.getPresence(TENANT_ID, "Product", "1")).contains(USER_1);
    assertThat(presenceService.getPresence(TENANT_ID, "Order", "999")).isEmpty();
  }
}
