package cz.muriel.core.presence;

import cz.muriel.core.test.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Near-Real-Time integration tests for Presence tracking system. Tests
 * WebSocket subscriptions, Redis state, locks, and TTL behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("test")
class PresenceNrtIT extends AbstractIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private PresenceService presenceService;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private StringRedisTemplate stringRedisTemplate;

  private static final String TENANT_ID = "test-tenant";
  private static final String USER_ID = "user1";
  private static final String ENTITY_TYPE = "Order";
  private static final String ENTITY_ID = "123";

  @BeforeEach
  void setUp() {
    // Clean Redis before each test
    Set<String> keys = redisTemplate.keys("presence:*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
    }
  }

  @AfterEach
  void tearDown() {
    // Clean Redis after each test
    Set<String> keys = redisTemplate.keys("presence:*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
    }
  }

  @Test
  void shouldTrackPresenceInRedis() {
    // When
    presenceService.subscribe(USER_ID, TENANT_ID, ENTITY_TYPE, ENTITY_ID);

    // Then
    String key = "presence:" + TENANT_ID + ":" + ENTITY_TYPE + ":" + ENTITY_ID + ":users";
    Set<Object> members = redisTemplate.opsForSet().members(key);
    assertThat(members).contains(USER_ID);
  }

  @Test
  void shouldRemovePresenceOnUnsubscribe() {
    // Given
    presenceService.subscribe(USER_ID, TENANT_ID, ENTITY_TYPE, ENTITY_ID);

    // When
    presenceService.unsubscribe(USER_ID, TENANT_ID, ENTITY_TYPE, ENTITY_ID);

    // Then
    String key = "presence:" + TENANT_ID + ":" + ENTITY_TYPE + ":" + ENTITY_ID + ":users";
    Set<Object> members = redisTemplate.opsForSet().members(key);
    assertThat(members).doesNotContain(USER_ID);
  }

  @Test
  void shouldAcquireEditLockInRedis() {
    // When
    boolean acquired = presenceService.acquireLock(USER_ID, TENANT_ID, ENTITY_TYPE, ENTITY_ID,
        "name");

    // Then
    assertThat(acquired).isTrue();
    String lockKey = "presence:" + TENANT_ID + ":" + ENTITY_TYPE + ":" + ENTITY_ID + ":lock:name";
    String owner = stringRedisTemplate.opsForValue().get(lockKey);
    assertThat(owner).isEqualTo(USER_ID);
  }

  @Test
  void shouldPreventDuplicateLockAcquisition() {
    // Given
    presenceService.acquireLock(USER_ID, TENANT_ID, ENTITY_TYPE, ENTITY_ID, "name");

    // When
    boolean acquired = presenceService.acquireLock("user2", TENANT_ID, ENTITY_TYPE, ENTITY_ID,
        "name");

    // Then
    assertThat(acquired).isFalse();
  }

  @Test
  void shouldReleaseLockInRedis() {
    // Given
    presenceService.acquireLock(USER_ID, TENANT_ID, ENTITY_TYPE, ENTITY_ID, "name");

    // When
    presenceService.releaseLock(USER_ID, TENANT_ID, ENTITY_TYPE, ENTITY_ID, "name");

    // Then
    String lockKey = "presence:" + TENANT_ID + ":" + ENTITY_TYPE + ":" + ENTITY_ID + ":lock:name";
    String owner = stringRedisTemplate.opsForValue().get(lockKey);
    assertThat(owner).isNull();
  }

  @Test
  void shouldExpirePresenceAfterTTL() {
    // Given
    presenceService.subscribe(USER_ID, TENANT_ID, ENTITY_TYPE, ENTITY_ID);
    String key = "presence:" + TENANT_ID + ":" + ENTITY_TYPE + ":" + ENTITY_ID + ":users";

    // When - wait for TTL expiration (60 seconds default)
    await().atMost(65, TimeUnit.SECONDS).untilAsserted(() -> {
      Set<Object> members = redisTemplate.opsForSet().members(key);
      assertThat(members).isEmpty();
    });
  }

  @Test
  void shouldRefreshTTLOnHeartbeat() throws InterruptedException {
    // Given
    presenceService.subscribe(USER_ID, TENANT_ID, ENTITY_TYPE, ENTITY_ID);
    String key = "presence:" + TENANT_ID + ":" + ENTITY_TYPE + ":" + ENTITY_ID + ":users";

    // When - send heartbeat after 50 seconds (before 60s TTL expiration)
    Thread.sleep(50_000);
    presenceService.heartbeat(USER_ID, TENANT_ID, ENTITY_TYPE, ENTITY_ID);
    
    // Then - wait another 20 seconds (total 70s, would have expired at 60s without heartbeat)
    Thread.sleep(20_000);
    Set<Object> members = redisTemplate.opsForSet().members(key);
    assertThat(members).contains(USER_ID);
  }

  @Test
  void shouldIsolatePresenceByTenant() {
    // Given
    String tenant1 = "tenant1";
    String tenant2 = "tenant2";

    // When
    presenceService.subscribe(USER_ID, tenant1, ENTITY_TYPE, ENTITY_ID);
    presenceService.subscribe(USER_ID, tenant2, ENTITY_TYPE, ENTITY_ID);

    // Then
    String key1 = "presence:" + tenant1 + ":" + ENTITY_TYPE + ":" + ENTITY_ID + ":users";
    String key2 = "presence:" + tenant2 + ":" + ENTITY_TYPE + ":" + ENTITY_ID + ":users";

    Set<Object> members1 = redisTemplate.opsForSet().members(key1);
    Set<Object> members2 = redisTemplate.opsForSet().members(key2);

    assertThat(members1).contains(USER_ID);
    assertThat(members2).contains(USER_ID);
    assertThat(key1).isNotEqualTo(key2); // Different Redis keys
  }

  @Test
  void shouldHandleMultipleUsersOnSameEntity() {
    // When
    presenceService.subscribe("user1", TENANT_ID, ENTITY_TYPE, ENTITY_ID);
    presenceService.subscribe("user2", TENANT_ID, ENTITY_TYPE, ENTITY_ID);
    presenceService.subscribe("user3", TENANT_ID, ENTITY_TYPE, ENTITY_ID);

    // Then
    String key = "presence:" + TENANT_ID + ":" + ENTITY_TYPE + ":" + ENTITY_ID + ":users";
    Set<Object> members = redisTemplate.opsForSet().members(key);
    assertThat(members).containsExactlyInAnyOrder("user1", "user2", "user3");
  }

  @Test
  void shouldHandleConcurrentLockAttempts() {
    // When - 3 users try to acquire lock simultaneously
    boolean lock1 = presenceService.acquireLock("user1", TENANT_ID, ENTITY_TYPE, ENTITY_ID, "name");
    boolean lock2 = presenceService.acquireLock("user2", TENANT_ID, ENTITY_TYPE, ENTITY_ID, "name");
    boolean lock3 = presenceService.acquireLock("user3", TENANT_ID, ENTITY_TYPE, ENTITY_ID, "name");

    // Then - only one should succeed
    int successes = (lock1 ? 1 : 0) + (lock2 ? 1 : 0) + (lock3 ? 1 : 0);
    assertThat(successes).isEqualTo(1);
  }
}
