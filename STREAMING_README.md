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
- **Grafana**: 3 dashboardy (Overview, Entities, Operations) - dostupné na `/monitoring/`
  - **streaming-overview**: Celkový přehled - queue depth, outbox, success rate, latency, DLQ
  - **streaming-entities**: Per-entity metriky - throughput, latency a chyby pro user/group/role/permission
  - **streaming-ops**: Operační monitoring - work state, locky, DLQ breakdown, priority queues, error logs
- **Alerts**: Queue threshold, DLQ, P95 latency SLO (30s)
- **Real-time Dashboard**: Admin UI na `/admin/streaming` s live metrikami

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

## 🧪 Testing & CI

### Pre-Deploy Testing Stack

Kompletní testovací infrastruktura zajišťuje kvalitu před nasazením do produkce.

#### Backend Tests

**1. Static Analysis** (Maven profiles)
```bash
# Maven Enforcer - duplicate classes, dependency convergence
cd backend && ./mvnw validate -P enforce-rules

# OWASP Dependency-Check - CVE scan (fail on CVSS >= 7)
./mvnw verify -P security -DskipTests

# JaCoCo Coverage - 70% minimum line coverage
./mvnw test jacoco:check -P unit-tests
```

**2. Unit Tests** (Surefire)
```bash
cd backend && ./mvnw test -P unit-tests
```

**3. Integration Tests** (Failsafe + Testcontainers)
```bash
cd backend && ./mvnw verify -P integration-tests

# Test suites:
# - PostgresStreamingIT: SKIP LOCKED batching, deduplication, TTL
# - KafkaStreamingIT: topic config, entity keying, partition consistency
# - PriorityAndPoliciesIT: priority lanes, strict reads, PII redaction
# - OpenApiContractIT: API contract validation, exports openapi.json
```

#### Frontend Tests

**1. Unit Tests** (Vitest + React Testing Library)
```bash
cd frontend

# Verify React version (no duplicates)
npm run verify:react

# Run unit tests with coverage
npm run test:unit
```

**2. E2E Tests** (Playwright)
```bash
# Start compose stack
./scripts/e2e-setup.sh

# Run E2E tests
cd frontend && npm run test:e2e

# Teardown
./scripts/e2e-teardown.sh
```

#### Infrastructure Tests

**Compose Stack Smoke Tests**
```bash
# Start streaming profile
docker compose --profile streaming up -d

# Run smoke tests
./scripts/infra-smoke-test.sh

# Tests:
# - /actuator/health, /actuator/prometheus
# - Kafka topics via kafka-topics.sh
# - Topic policies: cleanup.policy=compact, retention.ms
# - Grafana provisioning: /api/search?query=Streaming
# - Prometheus targets health
# - Mini flow: POST command → status==APPLIED
```

**Alert Validation**
```bash
# Validate Prometheus alert rules
./scripts/validate-alerts.sh

# Checks:
# - YAML syntax (yamllint)
# - PromQL syntax via Prometheus API
# - Expected alert names (9 rules)
# - Dry-run evaluation against metrics
# - Annotations (severity, summary, description)
```

### CI Pipeline (GitHub Actions)

Workflow: `.github/workflows/streaming-tests.yml`

**Jobs:**
1. **backend-static**: Maven Enforcer + OWASP
2. **backend-unit**: Unit tests + JaCoCo 70%
3. **backend-it**: Testcontainers IT + OpenAPI export
4. **frontend-unit**: Vitest + React dedupe check
5. **compose-smoke**: Docker Compose + infra-smoke-test.sh
6. **frontend-e2e**: Playwright with Chromium
7. **image-scan**: Trivy CRITICAL/HIGH vulnerabilities

**Artifacts:**
- OWASP reports (HTML/JSON)
- JaCoCo coverage reports
- OpenAPI spec (`openapi.json`)
- Playwright HTML report
- Docker logs (on failure)

**Branch Protection:**
- All jobs must pass
- 70% code coverage minimum
- No CRITICAL/HIGH CVEs

### Test Reports

**After CI run:**
- Coverage: `backend/target/site/jacoco/index.html`
- OWASP: `backend/target/dependency-check-report.html`
- Playwright: `frontend/playwright-report/index.html`
- OpenAPI spec: `backend/target/openapi/openapi.json`

## 🔐 Security

- Admin UI: Role `PlatformAdmin` nebo `Ops`
- DLQ API: `@PreAuthorize("hasAnyRole('PlatformAdmin', 'Ops')")`
- Kafka: SASL/PLAIN (produkce), PLAINTEXT (dev)
- Grafana: OAuth2 s Keycloak (produkce)

## 🤖 AI Hooks (META_ONLY)

**Since:** 2025-10-14

Streamingová infrastruktura je integrována s AI hooks pro in-app agenty:

### AI Context Export

```bash
# Get workflow context for streaming entities
curl http://localhost:8080/api/ai/mcp/wf_context/get_workflow \
  -X POST -H "Content-Type: application/json" \
  -d '{"entity": "WorkflowDraft"}'
```

Returns:
- Workflow states (draft, pending, approved, etc.)
- Actions with `howto` steps
- Streaming priority annotations (`CRITICAL`, `HIGH`, `NORMAL`, `BULK`)

### Streaming-Specific Metadata

Entity schemas contain streaming config:

```yaml
# workflow-draft.yaml
streaming:
  enabled: true
  priority: normal
  strictReads: true
  
transitions:
  - code: submit
    streamingPriority: HIGH  # Affects queue priority
    howto:
      - "Validate draft completeness"
      - "Click Submit button"
      - "Command queued with HIGH priority"
```

### Strict Reads Integration

AI context respects `strictReads`:

```bash
# Strict mode: returns 423 if entity is UPDATING
curl "http://localhost:8080/api/ai/context?routeId=workflow-draft.edit&strict=true"

# Non-strict: returns 200 + state.updating=true
curl "http://localhost:8080/api/ai/context?routeId=workflow-draft.edit&strict=false"
```

### Telemetry

AI metrics for streaming actions:

```promql
# AI requests for streaming entities
ai_requests_total{route=~"workflow-.*"}

# Help requests for streaming workflows
ai_help_requests_total{route=~"workflow-.*"}
```

**See:** `docs/AI_GUIDE.md` for complete AI documentation

## 📚 Reference

- **Outbox Pattern**: https://microservices.io/patterns/data/transactional-outbox.html
- **Kafka Idempotence**: https://kafka.apache.org/documentation/#producerconfigs_enable.idempotence
- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus
- **Kafka UI**: http://localhost:8090
- **Grafana**: http://localhost:3001
- **AI Guide**: `docs/AI_GUIDE.md`

## 🐛 Known Issues

- Backend KeycloakAdminService má chyby v UserUpdateRequest - není součástí streaming featury
- Frontend StreamingDashboardPage zatím nemá real-time metrics endpoint - placeholder

## 🚧 Roadmap

- [ ] Kafka Schema Registry pro Avro/Protobuf
- [ ] Dead Letter Queue UI s filtering a bulk retry
- [ ] Distributed tracing (Jaeger) pro command flow
- [ ] Multi-DC replication (Kafka MirrorMaker)
- [ ] Event versioning a schema evolution
