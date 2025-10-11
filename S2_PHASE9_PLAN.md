# S2: Phase 9 - Backend Integration Tests (4h)

**Cíl:** Spustit a ověřit všechny backend integration testy s Testcontainers  
**Očekávaný čas:** 4h  
**Prerekvizity:** Phase 8 complete (all code implemented)

---

## 1️⃣ Backend Test Suite Overview

### 1.1 PresenceServiceIntegrationTest.java
**Location:** `backend/src/test/java/cz/horak/platform/presence/PresenceServiceIntegrationTest.java`

**Test Coverage (18 test cases):**
- ✅ Subscribe to entity presence
- ✅ Heartbeat updates TTL
- ✅ Acquire field lock
- ✅ Release field lock
- ✅ Stale mode activation
- ✅ Stale mode deactivation
- ✅ Version tracking
- ✅ Multi-user scenarios
- ✅ Redis TTL expiration
- ✅ Kafka event publishing

**Testcontainers:**
- Redis (latest)
- Kafka (KRaft mode)

---

## 2️⃣ Test Execution Plan

### Task 1: Verify Test Files Exist (15min)
- [ ] Locate PresenceServiceIntegrationTest.java
- [ ] Check test configuration
- [ ] Verify Testcontainers dependencies in pom.xml

### Task 2: Run Integration Tests (2h)
- [ ] Start Testcontainers (Redis + Kafka)
- [ ] Run all 18 test cases
- [ ] Capture test results
- [ ] Fix any failures

### Task 3: Verify Test Coverage (1h)
- [ ] Check all presence operations tested
- [ ] Verify Redis interactions (set, expire, delete)
- [ ] Verify Kafka events published
- [ ] Check edge cases (concurrent users, TTL expiration)

### Task 4: Documentation (1h)
- [ ] Document test results
- [ ] Update S2_PROGRESS.md
- [ ] Create test report summary

---

## 3️⃣ Expected Test Results

### Success Criteria:
- ✅ All 18 tests pass
- ✅ Redis operations verified (TTL, expiration)
- ✅ Kafka events published correctly
- ✅ No memory leaks in Testcontainers
- ✅ Test execution < 5 minutes

### Failure Scenarios:
- ❌ Redis connection timeout → Check Testcontainers logs
- ❌ Kafka not ready → Increase startup timeout
- ❌ Test flakiness → Add retry logic
- ❌ TTL not working → Check Redis commands

---

## 4️⃣ Commands

### Run All Tests:
```bash
cd backend
./mvnw test -Dtest=PresenceServiceIntegrationTest
```

### Run Single Test:
```bash
./mvnw test -Dtest=PresenceServiceIntegrationTest#testSubscribeToPresence
```

### Run with Verbose Logging:
```bash
./mvnw test -Dtest=PresenceServiceIntegrationTest -X
```

---

## 🎯 Phase 9 Deliverables

- [ ] All 18 integration tests pass
- [ ] Test coverage report
- [ ] Performance metrics (execution time)
- [ ] Bug fixes (if any failures)
- [ ] Updated documentation

---

## ⏱️ Time Tracking

| Task | Estimate | Actual | Notes |
|------|----------|--------|-------|
| Verify test files | 15min | TBD | Check test exists |
| Run integration tests | 2h | TBD | 18 test cases |
| Verify coverage | 1h | TBD | Check all scenarios |
| Documentation | 1h | TBD | Test report |
| **Total** | **4h** | **TBD** | |

---

**Progress:** 0% (Ready to start)  
**Next:** Locate PresenceServiceIntegrationTest.java
