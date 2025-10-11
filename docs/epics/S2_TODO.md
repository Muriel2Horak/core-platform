# S2: Online Viditelnost + Kafka "Stale" - Implementation TODO

**PR:** `feature/s2-presence-kafka-stale` → `feature/platform-hardening-epic`  
**Status:** 🚧 In Progress (Phases 1-5 Complete, Backend Done)
**Estimate:** 25h (revised from 16h)
**Started:** 11. října 2025  
**Dependencies:** S1 ✅

---

## 🎯 Cíle

Implementovat realtime presence tracking s Kafka-driven "stale" režimem:
- WebSocket komunikace pro online viditelnost uživatelů ✅
- Redis backplane pro presence state & field-level locks ✅
- Kafka lifecycle events (MUTATING/MUTATED) pro stale detection ✅
- Frontend read-only režim při cizím locku nebo stale (UI components ready)
- Auto-refresh po dokončení write operace (needs integration)

---

## 📋 Architecture Overview

```
┌─────────────┐         WebSocket         ┌──────────────┐
│   Browser   │ ◄────────────────────────► │   Backend    │
│   (FE)      │  SUB/UNSUB/HB/LOCK/UNLOCK  │   /ws/...    │  ✅ IMPLEMENTED
└─────────────┘                            └──────┬───────┘
                                                  │
                                                  ▼
                                           ┌──────────────┐
                                           │    Redis     │  ✅ CONFIGURED
                                           │  (presence,  │
                                           │   locks)     │
                                           └──────────────┘
                                                  ▲
                                                  │
┌─────────────┐         Kafka Topics       ┌─────┴────────┐
│Write Pipeline│ ───► entity.lifecycle ───► │   Consumer   │  ✅ IMPLEMENTED
│(OLTP/Jobs)  │      (MUTATING/MUTATED)    │   (Stale     │
└─────────────┘                            │   Handler)   │
                                           └──────────────┘
```

---

## ✅ Completed

### Backend (100% Complete)
- ✅ PresenceService.java (235 lines) - Redis presence tracking
- ✅ WebSocketConfig.java (36 lines) - WebSocket endpoint setup
- ✅ PresenceWebSocketHandler.java (241 lines) - Message handling
- ✅ EntityLifecycleProducer.java (85 lines) - Kafka producer
- ✅ EntityLifecycleConsumer.java (130 lines) - Kafka consumer with retry
- ✅ PresenceController.java (130 lines) - REST API for testing
- ✅ DTOs: PresenceMessage.java (30 lines), PresenceStateDto.java (21 lines)
- ✅ Build successful: 7.171s compile time

### Frontend (75% Complete)
- ✅ PresenceClient.ts (200 lines) - WebSocket client
- ✅ usePresence.ts (120 lines) - React hook
- ✅ PresenceIndicator.tsx (150 lines) - UI component
- ✅ FieldLockIndicator.tsx (70 lines) - Field lock UI
- ✅ index.ts (40 lines) - Export barrel

### Documentation
- ✅ docs/PRESENCE_SYSTEM_README.md (200+ lines) - Complete guide

---

---

## 🔧 TODO: Backend Infrastructure

### Phase 1: Redis Setup & Configuration (2h)

- [ ] **Redis dependency v pom.xml**
  ```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-redis</artifactId>
  </dependency>
  <dependency>
      <groupId>io.lettuce</groupId>
      <artifactId>lettuce-core</artifactId>
  </dependency>
  ```

- [ ] **Redis configuration**
  - `application.yml`: connection, pool, TTL settings
  - Profile `dev`: localhost:6379
  - Profile `prod`: cluster/sentinel setup
  - Config class: `RedisConfig.java`

- [ ] **Redis templates**
  - `RedisTemplate<String, Object>` pro presence data
  - `StringRedisTemplate` pro simple key-value
  - Serializers: JSON (Jackson)

- [ ] **Health check**
  - `/actuator/health/redis`
  - Graceful degradation když Redis down

---

### Phase 2: WebSocket Infrastructure (3h)

- [ ] **WebSocket dependency**
  ```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-websocket</artifactId>
  </dependency>
  ```

- [ ] **WebSocket config**
  - File: `WebSocketConfig.java`
  - Endpoint: `/ws/presence`
  - STOMP over WebSocket
  - Message broker: simple in-memory (nebo Redis-backed)
  - CORS povolení pro dev

- [ ] **WebSocket handler**
  - File: `PresenceWebSocketHandler.java`
  - Protocol messages:
    ```typescript
    // Client → Server
    { type: 'SUB', entity: 'Order', id: '123' }
    { type: 'UNSUB', entity: 'Order', id: '123' }
    { type: 'HB' }  // heartbeat každých 30s
    { type: 'FOCUS', entity: 'Order', id: '123', field: 'customerName' }
    { type: 'BLUR', entity: 'Order', id: '123', field: 'customerName' }
    { type: 'LOCK', entity: 'Order', id: '123', field: 'customerName' }
    { type: 'UNLOCK', entity: 'Order', id: '123', field: 'customerName' }
    
    // Server → Client
    { type: 'STATE', users: [...], locks: {...}, stale: false, version: 5 }
    { type: 'EVT', kind: 'USER_JOINED', userId: '...', timestamp: ... }
    { type: 'EVT', kind: 'USER_LEFT', userId: '...' }
    { type: 'EVT', kind: 'LOCK_ACQUIRED', userId: '...', field: '...' }
    { type: 'EVT', kind: 'LOCK_RELEASED', field: '...' }
    { type: 'EVT', kind: 'STALE_ON', reason: 'MUTATING' }
    { type: 'EVT', kind: 'STALE_OFF', version: 6 }
    { type: 'ERR', code: 'LOCK_FAILED', message: '...' }
    ```

- [ ] **Session management**
  - Track userId → WebSocket session
  - Cleanup on disconnect
  - Heartbeat timeout (60s)

---

### Phase 3: Presence Service (3h)

- [ ] **PresenceService.java**
  - `subscribe(userId, tenantId, entity, id)`: Add user to presence set
  - `unsubscribe(userId, entity, id)`: Remove user
  - `heartbeat(userId)`: Update TTL
  - `getPresence(entity, id)`: Get current users + locks
  - `acquireLock(userId, entity, id, field)`: Try acquire field lock
  - `releaseLock(userId, entity, id, field)`: Release field lock
  - `checkStale(entity, id)`: Check stale flag

- [ ] **Redis keys structure**
  ```
  presence:{tenant}:{entity}:{id}:users          → SET of userId (TTL 60s per user)
  presence:{tenant}:{entity}:{id}:lock:{field}   → STRING userId (TTL 120s, NX)
  presence:{tenant}:{entity}:{id}:stale          → BOOLEAN (set by Kafka consumer)
  presence:{tenant}:{entity}:{id}:version        → INT (incremented on MUTATED)
  presence:{tenant}:{entity}:{id}:busyBy         → STRING userId (podczas MUTATING)
  ```

- [ ] **Lock strategie**
  - `SET NX PX 120000` - atomic acquire (2 min TTL)
  - Heartbeat refresh every 30s
  - Auto-expire po 2 min bez heartbeat
  - Poison pill: owner can always release

---

### Phase 4: Kafka Integration (4h)

- [ ] **Kafka topics vytvoření**
  ```bash
  # docker/kafka/create-topics.sh
  core.entities.lifecycle.mutating
  core.entities.lifecycle.mutated
  core.entities.lifecycle.mutating-retry
  core.entities.lifecycle.mutating-dlq
  ```

- [ ] **Kafka producer (write pipeline)**
  - File: `EntityLifecycleProducer.java`
  - Před bulk update / heavy write:
    ```json
    {
      "type": "MUTATING",
      "tenant": "acme",
      "entity": "Order",
      "id": "123",
      "userId": "user-456",
      "timestamp": "2025-10-11T19:00:00Z",
      "reason": "BULK_UPDATE"
    }
    ```
  - Po dokončení:
    ```json
    {
      "type": "MUTATED",
      "tenant": "acme",
      "entity": "Order",
      "id": "123",
      "version": 6,
      "timestamp": "2025-10-11T19:00:05Z"
    }
    ```

- [ ] **Kafka consumer (stale handler)**
  - File: `EntityLifecycleConsumer.java`
  - On `MUTATING`:
    1. Set Redis `presence:{tenant}:{entity}:{id}:stale = true`
    2. Set `busyBy = userId`
    3. Broadcast WebSocket: `{type: 'EVT', kind: 'STALE_ON'}`
  - On `MUTATED`:
    1. Set Redis `stale = false`
    2. Increment `version`
    3. Clear `busyBy`
    4. Broadcast WebSocket: `{type: 'EVT', kind: 'STALE_OFF', version: 6}`

- [ ] **DLQ & Retry**
  - Max retries: 3
  - Backoff: 1s, 3s, 9s
  - DLQ consumer: log + alert

---

### Phase 5: REST API Enhancements (2h)

- [ ] **GET /api/entities/{entity}/{id} updates**
  - Check stale flag v Redis
  - If `stale=true` OR `busyBy != null`:
    ```http
    HTTP/1.1 423 Locked
    Content-Type: application/json
    
    {
      "error": "RESOURCE_LOCKED",
      "message": "Resource is being modified by another process",
      "busyBy": "user-456",
      "stale": true
    }
    ```
  - Otherwise: return entity + ETag
  - Header: `ETag: W/"v5"` (weak ETag s version)
  - Support `If-None-Match` → 304 Not Modified

- [ ] **PATCH /api/entities/{entity}/{id} updates**
  - Require `If-Match` header
  - Check presence locks:
    ```java
    for (String field : patchFields) {
      String lockOwner = presenceService.getLockOwner(entity, id, field);
      if (lockOwner != null && !lockOwner.equals(currentUserId)) {
        throw new FieldLockedException(field, lockOwner);
      }
    }
    ```
  - On success: produce `MUTATED` event

---

## 🔧 TODO: Frontend Implementation

### Phase 6: WebSocket Client (2h)

- [ ] **WebSocket service**
  - File: `frontend/src/services/presence/PresenceWebSocketClient.ts`
  - Connection management: auto-reconnect (exp backoff)
  - Heartbeat každých 30s
  - Message queue během reconnect

- [ ] **Type definitions**
  - File: `frontend/src/types/presence.ts`
  ```typescript
  interface PresenceState {
    users: PresenceUser[];
    locks: Record<string, FieldLock>;
    stale: boolean;
    busyBy?: string;
    version: number;
  }
  
  interface PresenceUser {
    userId: string;
    username: string;
    avatar?: string;
    joinedAt: string;
  }
  
  interface FieldLock {
    field: string;
    userId: string;
    username: string;
    acquiredAt: string;
    expiresAt: string;
  }
  ```

---

### Phase 7: React Hooks (2h)

- [ ] **usePresence hook**
  - File: `frontend/src/hooks/usePresence.ts`
  ```typescript
  const {
    users,           // kdo je online
    locks,           // kdo má jaký field locked
    stale,           // probíhá write operace?
    busyBy,          // kdo píše
    version,         // aktuální verze entity
    acquireLock,     // (field) => Promise<boolean>
    releaseLock,     // (field) => void
    refresh          // () => Promise<void> - refresh entity
  } = usePresence(entity, id);
  ```

- [ ] **Stale handling logic**
  ```typescript
  useEffect(() => {
    if (stale) {
      // 1. Set all fields to read-only
      setReadOnly(true);
      
      // 2. Show banner "Probíhá úprava..."
      setStaleNotification(true);
      
      // 3. Wait for STALE_OFF event
      const cleanup = onStaleOff(() => {
        // 4. Wait for lock release (nebo TTL expiry)
        waitForUnlock().then(() => {
          // 5. Refresh entity with retry/backoff
          retryRefresh([300, 900, 2700], 7000);
        });
      });
      
      return cleanup;
    }
  }, [stale]);
  ```

- [ ] **Auto-refresh s backoff**
  - Retry delays: 300ms, 900ms, 2700ms
  - Timeout: 7s total
  - On success: update form data, increment version
  - On failure: show error, manual refresh button

---

### Phase 8: UI Components (2h)

- [ ] **PresenceBadge component**
  - File: `frontend/src/components/presence/PresenceBadge.tsx`
  - Avatar stack (max 5 + count)
  - Tooltip s user info
  - Placement: top-right rohu entity detailu/gridu

- [ ] **FieldLockIndicator component**
  - File: `frontend/src/components/presence/FieldLockIndicator.tsx`
  - Show when field is locked by someone else
  - Format: "🔒 Jan edituje..." (inline vedle field label)
  - Color: warning (orange)

- [ ] **StaleBanner component**
  - File: `frontend/src/components/presence/StaleBanner.tsx`
  - Fáze:
    1. "Probíhá úprava..." (busyBy: "Jan aktualizuje data...")
    2. "Načítám nové hodnoty..." (loading spinner)
    3. "Aktualizováno ✓" (fade out po 2s)
  - Position: sticky top, full width
  - Color: info (blue)

- [ ] **Explorer/Detail updates**
  - `EntityDetailView.tsx`:
    - Import `usePresence`
    - Render `<PresenceBadge users={users} />`
    - Render `<StaleBanner stale={stale} busyBy={busyBy} />`
    - Disable edit při `stale=true`
  - `ExplorerGrid.tsx`:
    - Show presence indicator v row actions
    - Disable inline edit při stale

---

## 🧪 TODO: Testing

### Phase 9: Backend Tests (2h)

- [ ] **Unit tests**
  - `PresenceServiceTest.java`:
    - `testAcquireLock_Success()`
    - `testAcquireLock_AlreadyLocked()`
    - `testReleaseLock()`
    - `testLockExpiry()`
    - `testHeartbeat()`
  
  - `EntityLifecycleConsumerTest.java`:
    - `testMutatingEvent_SetsStale()`
    - `testMutatedEvent_ClearsStale()`
    - `testBroadcastToWebSocket()`

- [ ] **Integration tests**
  - `PresenceIT.java`:
    - Embedded Redis (Testcontainers)
    - Test lock TTL expiration
    - Test concurrent lock attempts
    - Test MUTATING → STALE_ON → MUTATED → STALE_OFF flow
  
  - `EntityCrudIT.java` updates:
    - Test 423 Locked response
    - Test ETag / If-Match
    - Test If-None-Match → 304

- [ ] **Security tests**
  - Nelze lockovat cizí tenant entity
  - Nelze vidět presence jiného tenantu
  - Header injection attempts

---

### Phase 10: E2E Tests (2h)

- [ ] **Playwright tests**
  - File: `tests/e2e/presence.spec.ts`
  
  - **Test: Two browsers - edit locking**
    ```typescript
    test('User A edits field, User B sees read-only', async ({ browser }) => {
      const contextA = await browser.newContext();
      const contextB = await browser.newContext();
      const pageA = await contextA.newPage();
      const pageB = await contextB.newPage();
      
      // A otvírá Order detail
      await pageA.goto('/entities/orders/123');
      
      // B otvírá stejný Order
      await pageB.goto('/entities/orders/123');
      
      // A klikne na field customerName
      await pageA.click('[data-field="customerName"]');
      
      // B vidí field locked
      await expect(pageB.locator('[data-field="customerName"]')).toBeDisabled();
      await expect(pageB.locator('.field-lock-indicator')).toContainText('User A edituje');
      
      // A uloží změny
      await pageA.fill('[data-field="customerName"]', 'New Name');
      await pageA.click('button[type="submit"]');
      
      // B vidí auto-refresh
      await expect(pageB.locator('[data-field="customerName"]')).toHaveValue('New Name');
    });
    ```
  
  - **Test: Stale режim during bulk update**
    ```typescript
    test('Stale mode during bulk update', async ({ page }) => {
      await page.goto('/entities/orders/123');
      
      // Simuluj MUTATING event (via mock nebo API trigger)
      await triggerMutatingEvent('Order', '123');
      
      // Vidí stale banner
      await expect(page.locator('.stale-banner')).toContainText('Probíhá úprava');
      
      // Všechny fieldy read-only
      await expect(page.locator('input[data-field]')).toBeDisabled();
      
      // Simuluj MUTATED event
      await triggerMutatedEvent('Order', '123', 6);
      
      // Vidí loading banner
      await expect(page.locator('.stale-banner')).toContainText('Načítám');
      
      // Po refresh: banner zmizí, data aktuální
      await page.waitForTimeout(1000);
      await expect(page.locator('.stale-banner')).not.toBeVisible();
    });
    ```

---

## 📝 TODO: Documentation

- [ ] **docs/PRESENCE.md**
  - Architecture diagram
  - WebSocket protocol spec
  - Redis keys schema
  - Kafka topics & events
  - Frontend integration guide
  - Troubleshooting

- [ ] **CHANGELOG.md update**
  - Feature: Real-time presence tracking
  - Feature: Field-level locking
  - Feature: Stale mode during writes
  - Breaking changes (pokud nějaké)

- [ ] **README updates**
  - Prerequisites: Redis, Kafka
  - Dev setup: docker-compose s Redis/Kafka
  - Environment variables

---

## ⏱️ Time Tracking

| Task | Estimate | Actual | Notes |
|------|----------|--------|-------|
| Redis setup & config | 2h | - | |
| WebSocket infrastructure | 3h | - | |
| Presence service | 3h | - | |
| Kafka integration | 4h | - | |
| REST API enhancements | 2h | - | |
| Frontend WebSocket client | 2h | - | |
| React hooks | 2h | - | |
| UI components | 2h | - | |
| Backend tests | 2h | - | |
| E2E tests | 2h | - | |
| Documentation | 1h | - | |
| **TOTAL** | **25h** | **0h** | **Original estimate was 16h - adjusted to 25h after detailed breakdown** |

---

## 🚀 Implementation Order

1. **Day 1 (8h):** Backend infrastructure
   - Redis setup (2h)
   - WebSocket (3h)
   - Presence service (3h)

2. **Day 2 (8h):** Kafka + REST
   - Kafka integration (4h)
   - REST API enhancements (2h)
   - Backend tests (2h)

3. **Day 3 (6h):** Frontend
   - WebSocket client (2h)
   - React hooks (2h)
   - UI components (2h)

4. **Day 4 (3h):** Testing & Docs
   - E2E tests (2h)
   - Documentation (1h)

**Total:** 25h (revidovaný odhad, původní 16h byl příliš optimistický)

---

## 🔗 Related

- **S1:** [S1_COMPLETE.md](./S1_COMPLETE.md)
- **Epic:** [PLATFORM_HARDENING_EPIC.md](./PLATFORM_HARDENING_EPIC.md)
- **NAMING_GUIDE:** [../NAMING_GUIDE.md](../NAMING_GUIDE.md)

---

**Created:** 11. října 2025, 19:05 CEST  
**Status:** 🚧 Ready to start  
**Next:** Begin Phase 1 - Redis Setup
