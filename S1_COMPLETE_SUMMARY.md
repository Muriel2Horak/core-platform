# S1: Test Recovery - Complete Summary

## ✅ Final Status: 100% Complete (17/17 tests passing)

### Test Results Breakdown

#### CubeQueryServiceIT - Circuit Breaker Tests (5/5 ✅)
**Runtime**: ~60s  
**Status**: All passing

| Test | Description | Validates |
|------|-------------|-----------|
| `shouldExecuteQueryInClosedState` | Basic operation in CLOSED state | WebClient + WireMock integration, CB stays CLOSED on success |
| `shouldOpenCircuitBreakerOnFailures` | Failure threshold triggers OPEN | 3 failures → 100% failure rate → OPEN state |
| `opensOnFailuresThenRecoverToClosedState` | Recovery path OPEN → CLOSED | OPEN → wait 250ms → HALF_OPEN → 2 successes → CLOSED |
| `remainsOpenOnContinuousFailure` | Circuit stays open on failures | OPEN → HALF_OPEN → 2 failures → back to OPEN |
| `shouldIsolateCircuitBreakerPerTenant` | Per-tenant isolation | CB state independent per tenant |

**CircuitBreaker Config** (per-test registry):
```java
CircuitBreakerConfig.custom()
    .slidingWindowSize(4)
    .minimumNumberOfCalls(3)  // ⭐ Key fix - allows CB to open after 3 calls
    .failureRateThreshold(50.0f)
    .waitDurationInOpenState(Duration.ofMillis(250))  // Fast for tests
    .permittedNumberOfCallsInHalfOpenState(2)
    .recordExceptions(Exception.class)
    .build();
```

**Technical Approach**:
- **WireMock** for Cube.js API stubbing (deterministic 5xx/200 responses)
- **Per-test CircuitBreakerRegistry** for state isolation
- **WebClient with `.defaultStatusHandler()`** to convert 5xx → RuntimeException
- **@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)** for clean slate

---

#### PresenceNrtIT - Presence Tracking Tests (10/10 ✅)
**Runtime**: ~51s (down from 172s - **70% faster!**)  
**Status**: All passing

| Test | Description | Runtime | Validates |
|------|-------------|---------|-----------|
| `shouldTrackPresenceInRedis` | Basic presence tracking | <1s | SET operations, Redis key format |
| `shouldRemovePresenceOnUnsubscribe` | Cleanup on unsubscribe | <1s | SREM operation |
| `shouldAcquireEditLockInRedis` | Field-level locking | <1s | SET NX with TTL |
| `shouldPreventDuplicateLockAcquisition` | Lock contention | <1s | NX semantics |
| `shouldReleaseLockInRedis` | Lock release | <1s | DEL operation |
| `shouldExpirePresenceAfterTTL` | TTL expiration | ~2s | Awaitility, userTtlMs=1000 |
| `shouldRefreshTTLOnHeartbeat` | TTL refresh on heartbeat | ~2s | Heartbeat at 600ms, verify at 1300ms |
| `shouldIsolatePresenceByTenant` | Tenant isolation | <1s | Key prefixing |
| `shouldHandleMultipleUsersOnSameEntity` | Concurrent users | <1s | SET membership |
| `shouldHandleConcurrentLockAttempts` | Lock contention | <1s | Only 1 succeeds |

**Performance Improvements**:
- **TTL tests**: 65s → 2s (97% faster) via configurable `userTtlMs=1000`
- **Heartbeat tests**: 70s → 2s (97% faster) via configurable TTL
- **Total runtime**: 172s → 51s (**70% reduction**)

---

#### PresenceServiceIntegrationTest (2/2 ✅)
**Runtime**: ~10s  
**Status**: All passing (from S1/tests-recovery branch)

| Test | Description |
|------|-------------|
| `shouldTrackUserPresence` | Basic subscription/unsubscription |
| `shouldAcquireAndReleaseLock` | Lock lifecycle |

---

## 📊 Overall Progress

| Component | Tests | Status | Notes |
|-----------|-------|--------|-------|
| PresenceServiceIntegrationTest | 2/2 | ✅ | From S1/tests-recovery |
| PresenceNrtIT | 10/10 | ✅ | S1/tests-recovery-2 |
| CubeQueryServiceIT | 5/5 | ✅ | S1/tests-recovery-2 |
| **TOTAL** | **17/17** | **✅ 100%** | **All passing!** |

---

## 🔧 Technical Changes

### 1. CircuitBreaker Test Infrastructure

**File**: `backend/src/test/java/cz/muriel/core/reporting/service/CubeQueryServiceIT.java`

**Before**:
- Used Mockito for WebClient mocking
- Shared CircuitBreakerRegistry across tests
- CircuitBreaker state leaked between tests
- 2/4 tests failing due to state management

**After**:
```java
@BeforeEach
void setUp() {
  // WireMock for deterministic HTTP responses
  wireMockServer = new WireMockServer(0);
  wireMockServer.start();

  // WebClient with 5xx error handling
  cubeWebClient = WebClient.builder()
      .baseUrl("http://localhost:" + wireMockServer.port())
      .defaultStatusHandler(
          status -> status.is5xxServerError(),
          clientResponse -> clientResponse.createException()
              .map(ex -> new RuntimeException("Cube.js service error: " + ex.getMessage(), ex))
      )
      .build();

  // Per-test CB registry with fast config
  CircuitBreakerConfig config = CircuitBreakerConfig.custom()
      .slidingWindowSize(4)
      .minimumNumberOfCalls(3)  // ⭐ Critical for test determinism
      .failureRateThreshold(50.0f)
      .waitDurationInOpenState(Duration.ofMillis(250))
      .permittedNumberOfCallsInHalfOpenState(2)
      .build();

  circuitBreakerRegistry = CircuitBreakerRegistry.of(config);
}
```

**Key Insight**: CircuitBreaker needs `minimumNumberOfCalls` to calculate failure rate. Without it, failure rate is `-1.0` and CB never opens!

---

### 2. PresenceService TTL Configurability

**File**: `backend/src/main/java/cz/muriel/core/presence/PresenceService.java`

**Before**:
```java
private static final long USER_TTL_SECONDS = 60;  // Hardcoded!
private static final long LOCK_TTL_SECONDS = 120;
```

**After**:
```java
@Value("${app.presence.userTtlMs:60000}")
private long userTtlMs;

@Value("${app.presence.lockTtlMs:120000}")
private long lockTtlMs;

// Usage:
redisTemplate.expire(key, Duration.ofMillis(userTtlMs));
stringRedisTemplate.expire(lockKey, lockTtlMs, TimeUnit.MILLISECONDS);
```

**Benefits**:
- **Test speed**: 1000ms TTL instead of 60000ms
- **Production flexibility**: Can tune TTL via config
- **Backward compatible**: Defaults to original values

---

### 3. Test Configuration

**File**: `backend/src/test/resources/application-test.yml`

```yaml
app:
  presence:
    userTtlMs: 1000          # 1 second (was hardcoded 60000)
    lockTtlMs: 200           # 200ms (was hardcoded 120000)
    heartbeatIntervalMs: 50  # 50ms (informational, not used in tests)
```

---

### 4. Presence NRT Tests with Awaitility

**File**: `backend/src/test/java/cz/muriel/core/presence/PresenceNrtIT.java`

**Before** (Thread.sleep):
```java
Thread.sleep(50_000);  // 50 seconds!
presenceService.heartbeat(...);
Thread.sleep(20_000);  // Another 20 seconds!
```

**After** (Awaitility):
```java
await().pollDelay(600, TimeUnit.MILLISECONDS)
    .atMost(700, TimeUnit.MILLISECONDS)
    .untilAsserted(() -> {
      presenceService.heartbeat(...);
      assertThat(members).contains(USER_ID);
    });
```

**Benefits**:
- **Faster**: 2s instead of 70s
- **Resilient**: Polls until condition met
- **Readable**: Declarative intent

---

## 🎯 Lessons Learned

### 1. CircuitBreaker Configuration
- **`minimumNumberOfCalls`** is critical - CB won't transition without it
- Default is often `slidingWindowSize`, but not always
- Failure rate = `-1.0` means "not enough data"

### 2. WebClient Error Handling
- **`.retrieve()`** does NOT throw on 5xx by default!
- Must use `.defaultStatusHandler()` to convert errors
- Alternative: `.onStatus()` for per-request handling

### 3. Test Speed vs Realism
- **Prod values**: 60s TTL, 120s lock TTL
- **Test values**: 1s TTL, 200ms lock TTL
- **Benefit**: 97% faster tests, same coverage

### 4. WireMock vs Mockito
- **WireMock**: Better for HTTP stubbing, realistic responses
- **Mockito**: Better for unit-level mocking
- **Hybrid**: Use both where appropriate

---

## 📝 Git Branches

| Branch | Status | Tests | Notes |
|--------|--------|-------|-------|
| `S1/tests-recovery` | Merged | 13/13 ✅ | DI fixes, Redis Testcontainer |
| `S1/tests-recovery-2` | ✅ Ready | 17/17 ✅ | CB tests + TTL speed-up |

---

## 🚀 Next Steps

### Immediate
1. ✅ Merge `S1/tests-recovery-2` → `main`
2. Run full test suite: `mvn clean verify -Ptest`
3. Verify CI/CD pipeline

### Optional Enhancements
- Add Chaos Engineering tests (random CB state)
- Add metrics assertions (CB metrics, Redis ops)
- Add E2E tests for presence tracking via WebSocket

---

## 📦 Deliverables

### Code Changes
- ✅ `CubeQueryServiceIT.java` - 5 CB tests with WireMock
- ✅ `PresenceService.java` - Configurable TTL via @Value
- ✅ `PresenceNrtIT.java` - 10 tests with Awaitility
- ✅ `application-test.yml` - Fast TTL config

### Documentation
- ✅ This summary (S1_COMPLETE_SUMMARY.md)
- ✅ Inline comments explaining CB config
- ✅ Test method JavaDocs

### Test Results
```
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  ~02:20 min
```

---

**Generated**: 2025-10-12  
**Branch**: S1/tests-recovery-2  
**Author**: GitHub Copilot + Martin Horák  
**Session**: S1 Complete - CircuitBreaker + Presence NRT Tests
