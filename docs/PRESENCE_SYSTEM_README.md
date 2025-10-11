# Real-Time Presence Tracking System

## Přehled

Systém pro real-time sledování, kdo právě prohlíží/edituje entity v aplikaci.

**Základní vlastnosti:**
- WebSocket komunikace s backendem
- Redis pro distribuované sledování presence
- Kafka lifecycle eventy (MUTATING/MUTATED)
- Field-level locks pro zabránění concurrent editů
- Auto-reconnect a heartbeat (30s interval)

## Architektura

```
Frontend (React)                Backend (Spring)              Infrastructure
┌─────────────────┐            ┌──────────────────┐          ┌──────────────┐
│ usePresence()   │◄──WebSocket──►PresenceHandler │◄────────►│ Redis        │
│ hook            │            │                  │          │ (presence    │
└─────────────────┘            └──────────────────┘          │  + locks)    │
       │                               │                      └──────────────┘
       │                               │
       ▼                               ▼                      ┌──────────────┐
┌─────────────────┐            ┌──────────────────┐          │ Kafka        │
│ PresenceClient  │            │LifecycleConsumer│◄────────►│ (MUTATING/   │
│ (WebSocket)     │            │LifecycleProducer│          │  MUTATED)    │
└─────────────────┘            └──────────────────┘          └──────────────┘
       │
       ▼
┌─────────────────┐
│PresenceIndicator│
│FieldLockInd.    │
└─────────────────┘
```

## Backend Komponenty

### 1. PresenceService
- **Lokace:** `backend/src/main/java/cz/muriel/core/presence/PresenceService.java`
- **Funkce:**
  - `subscribe(userId, tenantId, entity, id)` - Přidá usera do Redis SET
  - `heartbeat(userId, ...)` - Obnoví TTL (60s)
  - `acquireLock(userId, ..., field)` - Atomic lock přes `SET NX PX 120000`
  - `setStale(tenantId, ..., stale, busyBy)` - Nastaví "stale" flag
  - `incrementVersion(tenantId, ...)` - Zvýší verzi entity

### 2. WebSocketConfig + PresenceWebSocketHandler
- **Endpoint:** `ws://localhost:8080/ws/presence`
- **Protokol:**
  ```json
  // SUB (subscribe)
  {"type":"SUB", "userId":"u1", "tenantId":"t1", "entity":"Order", "id":"123"}
  
  // Server response
  {"type":"PRESENCE", "users":["u1","u2"], "stale":false, "version":5}
  
  // HB (heartbeat) každých 30s
  {"type":"HB"}
  
  // LOCK (acquire field lock)
  {"type":"LOCK", "field":"totalAmount"}
  {"type":"LOCK_ACK", "field":"totalAmount", "success":true}
  ```

### 3. EntityLifecycleProducer + Consumer
- **Topics:**
  - `core.entities.lifecycle.mutating` - Entity se začíná editovat
  - `core.entities.lifecycle.mutated` - Edit dokončen
- **Retry:** 3× exponential backoff (1s, 3s, 9s) + DLQ

## Frontend Komponenty

### 1. PresenceClient (TypeScript)
- **Lokace:** `frontend/src/lib/presence/PresenceClient.ts`
- **Funkce:**
  - `connect()` / `disconnect()`
  - `acquireLock(field)` / `releaseLock(field)`
  - Auto-reconnect s exponential backoff (max 5 pokusů)
  - Heartbeat každých 30s

### 2. usePresence() Hook
- **Lokace:** `frontend/src/lib/presence/usePresence.ts`
- **Použití:**
  ```tsx
  const { presence, acquireLock, releaseLock, error } = usePresence({
    entity: 'Order',
    id: orderId,
    tenantId: currentTenant,
    userId: currentUser.id,
  });
  
  // presence.users → string[]
  // presence.stale → boolean
  // presence.busyBy → string | null
  // presence.version → number | null
  // presence.connected → boolean
  ```

### 3. UI Components
- **PresenceIndicator:** Zobrazí avatary aktivních uživatelů + stale badge
- **FieldLockIndicator:** Zobrazí zámek na fieldu s možností acquire/release

## Použití v Aplikaci

### Příklad: Order Edit Page

```tsx
import { usePresence } from '@/lib/presence';
import { PresenceIndicator } from '@/components/presence/PresenceIndicator';

function OrderEditPage({ orderId }: { orderId: string }) {
  const { currentTenant, currentUser } = useAuth();
  
  const { presence, acquireLock, releaseLock, error } = usePresence({
    entity: 'Order',
    id: orderId,
    tenantId: currentTenant.id,
    userId: currentUser.id,
  });

  // Acquire lock when focusing field
  const handleFieldFocus = (field: string) => {
    acquireLock(field);
  };

  // Release lock when blurring field
  const handleFieldBlur = (field: string) => {
    releaseLock(field);
  };

  return (
    <div>
      {/* Header with presence indicator */}
      <div className="flex justify-between items-center mb-4">
        <h1>Edit Order #{orderId}</h1>
        <PresenceIndicator 
          presence={presence}
          currentUserId={currentUser.id}
          getUserDisplayName={(userId) => users[userId]?.name}
        />
      </div>

      {/* Stale warning */}
      {presence.stale && presence.busyBy && (
        <Alert variant="warning">
          ⚠️ This record is currently being modified by {presence.busyBy}
        </Alert>
      )}

      {/* Form fields with lock indicators */}
      <Input
        label="Total Amount"
        onFocus={() => handleFieldFocus('totalAmount')}
        onBlur={() => handleFieldBlur('totalAmount')}
        disabled={presence.stale}
      />
    </div>
  );
}
```

## Redis Keys Schema

```
presence:{tenant}:{entity}:{id}:users         → SET (userId, ...) | TTL 60s
presence:{tenant}:{entity}:{id}:lock:{field}  → STRING userId     | TTL 120s
presence:{tenant}:{entity}:{id}:stale         → STRING "true"     | TTL 5min
presence:{tenant}:{entity}:{id}:busyBy        → STRING userId     | TTL 5min
presence:{tenant}:{entity}:{id}:version       → INT               | TTL 24h
```

## Konfigurace

### Backend (application.properties)
```properties
# Existing Redis config already OK
app.redis.enabled=true
spring.data.redis.host=redis
spring.data.redis.port=6379

# Kafka config (already exists)
app.kafka.enabled=true
spring.kafka.bootstrap-servers=kafka:9092
```

### Frontend (env)
```env
VITE_WS_URL=ws://localhost:8080/ws/presence
```

## Testování

### 1. Unit Tests (Backend)
```java
@SpringBootTest
@Testcontainers
class PresenceServiceTest {
  @Container
  static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
      .withExposedPorts(6379);
      
  @Test
  void shouldTrackUserPresence() {
    presenceService.subscribe("user1", "t1", "Order", "123");
    Set<Object> users = presenceService.getPresence("t1", "Order", "123");
    assertThat(users).contains("user1");
  }
}
```

### 2. Integration Tests (E2E)
```typescript
// tests/e2e/presence.spec.ts
test('should show presence indicator', async ({ page }) => {
  await page.goto('/orders/123/edit');
  
  // Check presence indicator exists
  await expect(page.locator('[data-testid="presence-indicator"]')).toBeVisible();
  
  // Should show current user
  await expect(page.locator('[data-testid="presence-users"]')).toContainText('1');
});
```

## Monitoring

### Redis Monitoring (Grafana)
```promql
# Active presence connections
redis_presence_active_users{entity="Order"}

# Lock acquisitions
redis_presence_locks_acquired_total

# Stale entities
redis_presence_stale_entities
```

### Kafka Monitoring
```promql
# Lifecycle events throughput
kafka_consumer_records_consumed_total{topic="core.entities.lifecycle.mutating"}

# DLQ size (should be 0)
kafka_consumer_records_consumed_total{topic="core.entities.lifecycle.mutating.dlt"}
```

## Limity a SLA

- **Heartbeat interval:** 30s (client) → 60s TTL (server)
- **Lock TTL:** 120s (auto-release pokud client zmizí)
- **Stale TTL:** 5min (safety timeout pokud MUTATED event nedorazí)
- **Max reconnect attempts:** 5× s exponential backoff
- **Max WebSocket connections:** Limitováno Redis/Kafka kapacitou (tisíce simultánních uživatelů)

## Troubleshooting

### Problem: WebSocket connection fails
- **Check:** `app.redis.enabled=true` v application.properties
- **Check:** Redis běží na `localhost:6379`
- **Check:** CORS policy v WebSocketConfig

### Problem: Presence data nezmizí
- **Reason:** Heartbeat stále běží i když uživatel zavřel tab
- **Fix:** Browser posílá `beforeunload` event → disconnect()

### Problem: Lock zůstal locked napořád
- **Reason:** Client crash bez odpojení
- **Fix:** TTL 120s vyčistí starý lock automaticky

## Next Steps

Po implementaci kompletního systému:
1. **Phase 9:** Backend testy (IT s Testcontainers)
2. **Phase 10:** E2E testy (Playwright s 2 browsers)
3. **Docs:** Migration guide pro integraci do existujících pages
4. **Monitoring:** Grafana dashboards pro presence metrics
