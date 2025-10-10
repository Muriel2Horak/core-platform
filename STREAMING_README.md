# Streaming Infrastructure

Streamingová infrastruktura pro core-platform využívající Kafka, PostgreSQL fronty a Outbox pattern.

## 🏗️ Architektura

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐      ┌──────────┐
│   Client    │─────>│ REST API     │─────>│ CommandQueue│─────>│  Worker  │
│  (REST/UI)  │      │ Controller   │      │  (Postgres) │      │  Service │
└─────────────┘      └──────────────┘      └─────────────┘      └──────────┘
                                                                       │
                                                                       ▼
                                            ┌─────────────┐      ┌──────────┐
                                            │  WorkState  │<─────│ Process  │
                                            │  (Locking)  │      │ Command  │
                                            └─────────────┘      └──────────┘
                                                                       │
                                                                       ▼
                                            ┌─────────────┐      ┌──────────┐
                                            │   Outbox    │<─────│  Write   │
                                            │   Final     │      │  Event   │
                                            └─────────────┘      └──────────┘
                                                  │
                                                  ▼
                                            ┌──────────────┐     ┌──────────┐
                                            │  Dispatcher  │────>│  Kafka   │
                                            │   Service    │     │  Topics  │
                                            └──────────────┘     └──────────┘
```

## 📊 Komponenty

### 1. **CommandQueue** - Fronta příkazů
- Asynchronní fronty pro CREATE, UPDATE, DELETE, BULK operace
- Priority: `critical` → `high` → `normal` → `bulk`
- Status flow: `pending` → `processing` → `completed` / `failed` / `dlq`
- Retry logic s exponenciálním backoffem

### 2. **WorkState** - Stavová synchronizace
- Pesimistické zámky na úrovni entity (FOR UPDATE SKIP LOCKED)
- Single-writer semantika - jen jeden worker může aktualizovat entitu najednou
- TTL-based expiry (5 min) pro automatické uvolnění zamrzlých locků
- Stavy: `idle` ↔ `updating`

### 3. **OutboxFinal** - Transakční outbox
- Garantuje at-least-once delivery do Kafky
- Idempotentní publisher (acks=all, transakcional ID)
- Partition key: `{entity}#{entityId}` pro ordering per entity

### 4. **Kafka Topics**
- **Commands**: `{entity}-commands` (24h retention, compacted)
- **Events**: `{entity}-events` (7d retention, compacted)
- **Inflight**: `{entity}-inflight` (30min retention) - pre-event notifikace

### 5. **Metrics & Monitoring**
- **Prometheus**: 15+ metrik (queue depth, latency, errors, DLQ)
- **Grafana**: 3 dashboardy (Overview, Entities, Operations)
- **Alerts**: Queue threshold, DLQ, P95 latency SLO (30s)

## 🚀 Quickstart

### 1. Povolit streaming v metamodelu

**Globální config** (`backend/src/main/resources/metamodel/global-config.yaml`):
```yaml
streaming:
  enabled: true
  defaultPartitions: 3
  defaultReplicationFactor: 1
  defaultRetentionHours: 168
```

**Entity config** (`backend/src/main/resources/metamodel/user.yaml`):
```yaml
entity: user
table: users
streaming:
  enabled: true
  partitions: 6
  eventPayloadMode: diff  # full | diff | minimal
  strictReads: true       # return 423 when entity updating
  workerBatchSize: 100
  maxRetries: 3
```

### 2. Spustit Kafka stack

```bash
# Start Kafka + Prometheus + Grafana
docker compose --profile streaming up -d

# Kontrola topicců
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### 3. Povolit feature flag

**.env**:
```bash
STREAMING_ENABLED=true
```

### 4. Navštívit Admin UI

- **Streaming Dashboard**: http://localhost/core-admin/streaming
- **Kafka UI**: http://localhost:8090
- **Grafana**: http://localhost:3001

## 📈 Použití

### REST API - Zápis s priority

```bash
# Normální operace
POST /api/users
{
  "email": "test@example.com",
  "firstName": "John",
  "priority": "normal"
}

# Kritická operace (přednostní)
POST /api/users
{
  "email": "vip@example.com",
  "priority": "critical"
}

# Bulk import (nízká priorita)
POST /api/users/bulk
{
  "users": [...],
  "priority": "bulk"
}
```

### Strict Reads - 423 Locked

Když je `strictReads: true`, GET při update vrátí 423:

```bash
GET /api/users/123
HTTP/1.1 423 Locked
{
  "error": "Entity is currently being updated",
  "retryAfter": 5
}
```

### DLQ Management API

```bash
# Seznam DLQ zpráv
GET /api/admin/streaming/dlq?entity=user&errorType=ValidationException

# Retry jedné zprávy
POST /api/admin/streaming/dlq/{id}/retry

# Retry všech DLQ pro entitu
POST /api/admin/streaming/dlq/retry-all?entity=user

# Smazat DLQ zprávu
DELETE /api/admin/streaming/dlq/{id}
```

## 🔍 Monitoring

### Grafana Dashboards

1. **Streaming Overview**
   - Celková hloubka fronty
   - Unsent outbox zprávy
   - Worker success rate
   - DLQ count

2. **Streaming Entities**
   - Per-entity metriky
   - Success rate per entity
   - P95 latency histogram
   - Error breakdown by type

3. **Streaming Operations**
   - Work state transitions
   - Lock expiry rate
   - DLQ by entity table
   - Queue depth by priority

### Prometheus Alerts

- **StreamingQueueDepthHigh**: queue > 1000 (5min)
- **StreamingQueueDepthCritical**: queue > 5000 (2min)
- **StreamingOutboxUnsentGrowing**: roste 10min
- **StreamingLatencyP95High**: P95 > 30s (5min)
- **StreamingDLQMessagesDetected**: nové DLQ zprávy
- **StreamingLocksExpiring**: >10 expired locků za 5min

## 🧪 Testování

### Unit testy

```bash
cd backend
./mvnw test -Dtest=WorkerServiceTest
./mvnw test -Dtest=DispatcherServiceTest
```

### Integration test - AD Sync

```bash
# Simulace AD sync 5000 uživatelů
POST /api/admin/test/ad-sync
{
  "userCount": 5000,
  "priority": "high"
}

# Kontrola metrik
curl localhost:9090/api/v1/query?query=core_stream_worker_success_total
```

### Load test

```bash
# Apache Bench - 1000 requests, 50 concurrent
ab -n 1000 -c 50 -p user.json -T application/json \
   http://localhost/api/users

# Sledovat Grafana: Streaming Overview
```

### Chaos test

```bash
# Zabít worker a sledovat queue backlog
docker compose kill backend
sleep 30
docker compose up -d backend

# Kontrola DLQ a recovery
```

## 🛠️ Troubleshooting

### Queue backlog

```sql
-- Zjistit hloubku fronty
SELECT status, priority, COUNT(*) 
FROM command_queue 
GROUP BY status, priority;

-- Najít nejstarší pending
SELECT * FROM command_queue 
WHERE status = 'pending' 
ORDER BY created_at 
LIMIT 10;
```

### Zamrzlé locky

```sql
-- Najít staré locky
SELECT * FROM work_state 
WHERE state = 'updating' 
  AND locked_at < NOW() - INTERVAL '10 minutes';

-- Ručně uvolnit
DELETE FROM work_state 
WHERE state = 'updating' 
  AND locked_at < NOW() - INTERVAL '15 minutes';
```

### DLQ replay

```bash
# Retry všech DLQ pro ValidationException
POST /api/admin/streaming/dlq/retry-all?errorType=ValidationException

# Retry pouze user entity
POST /api/admin/streaming/dlq/retry-all?entity=user
```

### Kafka lag

```bash
# Check consumer lag
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group core-platform-events
```

## 📝 Přidání nové entity do streamingu

1. **Metamodel** - přidat streaming config:
```yaml
entity: order
streaming:
  enabled: true
  partitions: 3
  eventPayloadMode: full
```

2. **TopicEnsurer** - automaticky vytvoří topics při startu

3. **EventConsumer** - přidat listener:
```java
@KafkaListener(topics = "order-events", groupId = "core-platform-events")
public void handleOrderEvent(String message) {
    // Process event
}
```

4. **Controller** - použít CommandQueue pro zápisy:
```java
@PostMapping("/orders")
public ResponseEntity<Order> create(@RequestBody OrderRequest req) {
    Order order = orderService.create(req);
    commandQueueRepository.save(CommandQueue.builder()
        .entity("order")
        .entityId(order.getId())
        .operation("CREATE")
        .priority(req.getPriority())
        .payload(mapper.writeValueAsString(order))
        .build());
    return ok(order);
}
```

## 🔐 Security

- Admin UI: Role `PlatformAdmin` nebo `Ops`
- DLQ API: `@PreAuthorize("hasAnyRole('PlatformAdmin', 'Ops')")`
- Kafka: SASL/PLAIN (produkce), PLAINTEXT (dev)
- Grafana: OAuth2 s Keycloak (produkce)

## 📚 Reference

- **Outbox Pattern**: https://microservices.io/patterns/data/transactional-outbox.html
- **Kafka Idempotence**: https://kafka.apache.org/documentation/#producerconfigs_enable.idempotence
- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus
- **Kafka UI**: http://localhost:8090
- **Grafana**: http://localhost:3001

## 🐛 Known Issues

- Backend KeycloakAdminService má chyby v UserUpdateRequest - není součástí streaming featury
- Frontend StreamingDashboardPage zatím nemá real-time metrics endpoint - placeholder

## 🚧 Roadmap

- [ ] Kafka Schema Registry pro Avro/Protobuf
- [ ] Dead Letter Queue UI s filtering a bulk retry
- [ ] Distributed tracing (Jaeger) pro command flow
- [ ] Multi-DC replication (Kafka MirrorMaker)
- [ ] Event versioning a schema evolution
