# Metamodel – Fáze 2: Realtime Features & Performance

Implementace pokročilých funkcí pro metamodel-driven platformu.

## 📋 Obsah Fáze 2

### 2.1 WebSocket Presence & Editing Indicators

**Realtime presence tracking** - sledování online uživatelů a editačních indikátorů.

#### Backend API

**WebSocket Endpoint:**
```
ws://localhost:8080/ws/presence
```

**Autentizace:**
- JWT token v handshake (Security Context)
- Automatická extrakce `userId`, `username`, `tenantId`

**Messages (Client → Server):**

```json
// Join entity
{
  "action": "join",
  "entityType": "UserProfile",
  "entityId": "123"
}

// Leave entity
{
  "action": "leave",
  "entityType": "UserProfile",
  "entityId": "123"
}

// Heartbeat (každých 10s)
{
  "action": "heartbeat",
  "entityType": "UserProfile",
  "entityId": "123"
}

// Start editing field
{
  "action": "editStart",
  "entityType": "UserProfile",
  "entityId": "123",
  "fieldId": "email"
}

// Stop editing field
{
  "action": "editStop",
  "entityType": "UserProfile",
  "entityId": "123",
  "fieldId": "email"
}
```

**REST Fallback API:**

```http
# Get online users
GET /api/presence/{entityType}/{entityId}

# Get editing indicators
GET /api/presence/{entityType}/{entityId}/editing
```

**Redis Keys:**
```
presence:<tenantId>:<entityType>:<entityId>:<userId>  (TTL 30s)
presence_members:<tenantId>:<entityType>:<entityId>   (Set)
editing:<tenantId>:<entityType>:<entityId>:<fieldId>:<userId>  (TTL 30s)
```

**Rate Limiting:**
- Max 10 events per 5 seconds per user
- Token bucket algorithm

#### Frontend Usage

```typescript
// Hook pro presence
const { onlineUsers, subscribe, unsubscribe } = usePresence({
  entityType: 'UserProfile',
  entityId: '123'
});

// Hook pro editing indicators
const { editors, startEditing, stopEditing } = useEditingIndicator({
  entityType: 'UserProfile',
  entityId: '123',
  fieldId: 'email'
});

// Auto stop po 2s inaktivity
const debouncedStop = useDe bounce(() => stopEditing(), 2000);
```

---

### 2.2 Workflow, Stavy & SLA

**State machine** pro entity s guardy, SLA tracking a audit logem.

#### Databázové tabulky

```sql
-- Current state
entity_state (entity_type, entity_id, tenant_id, state_code, since)

-- Transition definitions
state_transition (entity_type, from_code, to_code, code, guard, sla_minutes)

-- Audit log
entity_state_log (id, entity_type, entity_id, from/to_code, changed_by, changed_at)
```

#### Metamodel YAML

```yaml
states:
  - code: draft
    label: Draft
  - code: active
    label: Active

transitions:
  - code: ACTIVATE
    from: draft
    to: active
    label: Activate
    guard:
      expression: "hasRole('CORE_ROLE_ADMIN')"
    slaMinutes: 60
```

#### Backend API

```http
# Get current state + SLA status
GET /api/entities/{type}/{id}/state
Response:
{
  "stateCode": "draft",
  "since": "2025-01-08T10:00:00Z",
  "slaStatus": "OK"  // OK | WARN | BREACH | NONE
}

# Get allowed transitions (filtered by guards)
GET /api/entities/{type}/{id}/transitions
Response: [
  {
    "code": "ACTIVATE",
    "fromCode": "draft",
    "toCode": "active",
    "slaMinutes": 60
  }
]

# Apply transition
POST /api/entities/{type}/{id}/transition/{code}
Response:
{
  "success": true,
  "newState": { "stateCode": "active", "since": "..." },
  "slaStatus": "OK"
}
```

**SLA Thresholds:**
- **OK**: < 80% elapsed
- **WARN**: 80-100% elapsed  
- **BREACH**: > 100% elapsed

**Guards:**
- Simple: `hasRole('ROLE_NAME')`
- Future: Complex SpEL expressions via PolicyEngine

---

### 2.3 Dokumenty & Fulltext Search

**MinIO storage** + **Apache Tika** text extraction + **PostgreSQL FTS**.

#### MinIO Configuration

```properties
minio.endpoint=http://minio:9000
minio.bucket-prefix=tenant
minio.versioning-enabled=true
```

**Bucket structure:**
```
tenant/<tenantId>/<entityType>/<entityId>/<filename>
```

#### Backend API

```http
# Upload document
POST /api/documents
Content-Type: multipart/form-data
{
  "file": <binary>,
  "entityType": "UserProfile",
  "entityId": "123"
}
Response:
{
  "documentId": "uuid",
  "downloadUrl": "/api/documents/{id}/download"
}

# Download document (presigned URL)
GET /api/documents/{id}/download
→ 302 Redirect to MinIO presigned URL (expires in 1h)

# Fulltext search
GET /api/search?q=john&types=UserProfile,Document&limit=20
Response: {
  "results": [
    {
      "type": "UserProfile",
      "id": "123",
      "highlights": ["full_name: <b>John</b> Doe"],
      "score": 0.95
    },
    {
      "type": "Document",
      "id": "456",
      "filename": "report.pdf",
      "highlights": ["...contains <b>john</b>..."],
      "score": 0.87
    }
  ],
  "total": 2
}
```

#### Metamodel Fulltext Fields

```yaml
fulltext:
  - full_name
  - email
  - bio
```

**Automatická indexace:**
- Entity: PostgreSQL trigger na INSERT/UPDATE → `tsvector` v dedicated column
- Dokumenty: Tika extrakce po uploadu → `document_index.content_tsv`

**GIN Index:**
```sql
CREATE INDEX idx_fts ON user_profile USING GIN(fts_tsv);
CREATE INDEX idx_document_fts ON document_index USING GIN(content_tsv);
```

---

### 2.4 Dotazování & Výkon

#### jOOQ DSL (typesafe SQL)

**Migrace z concat SQL na jOOQ:**
```java
// Before (unsafe)
String sql = "SELECT * FROM " + table + " WHERE " + filter;

// After (typesafe)
DSLContext dsl = ...;
dsl.selectFrom(table(tableName))
   .where(field("tenant_id").eq(tenantId))
   .and(parseFilter(filterExpr))
   .orderBy(field(sortKey).asc())
   .limit(pageSize)
   .fetch();
```

**Filter Parser:**
- Syntax: `field eq 'value'`, `age gt 18`, `status in ['active','draft']`
- Operators: `eq`, `ne`, `lt`, `lte`, `gt`, `gte`, `like`, `in`, `notIn`
- Závorky: `(status eq 'active') and (age gt 18)`
- Dot-notation (1 JOIN max): `department.name eq 'IT'`

#### Keyset Pagination

**Cursor-based** místo offset-based (výkon na velkých datasetech).

```http
# First page
GET /api/entities/UserProfile?limit=20&sort=email,asc

Response:
{
  "data": [...],
  "cursors": {
    "next": "email:john@doe.com,id:123",
    "prev": null
  }
}

# Next page
GET /api/entities/UserProfile?limit=20&sort=email,asc&cursorNext=email:john@doe.com,id:123

# Previous page
GET /api/entities/UserProfile?limit=20&sort=email,desc&cursorPrev=email:alice@example.com,id:99
```

**SQL:**
```sql
-- Next page
SELECT * FROM user_profile
WHERE (email, id) > ('john@doe.com', '123')
ORDER BY email ASC, id ASC
LIMIT 20;

-- Prev page
SELECT * FROM user_profile
WHERE (email, id) < ('alice@example.com', '99')
ORDER BY email DESC, id DESC
LIMIT 20;
```

**Total count** (optional, `?withTotal=true`):
- Separátní COUNT query (expensive)
- Disabled by default

#### Redis Cache

**Read-through pattern:**
```
entity:{type}:{id}:{version} → JSON
```

**Invalidation:**
- Listen na `change_events` queue (Postgres NOTIFY)
- Invaliduj konkrétní klíč při UPDATE/DELETE

**TTL:** 10 minut

**Metrics:**
- `cache.hit.rate` (Micrometer)

#### HTTP Caching

**If-Modified-Since / Last-Modified:**
```http
# First request
GET /api/entities/UserProfile
Response:
  Last-Modified: Wed, 08 Jan 2025 10:00:00 GMT
  [...data...]

# Subsequent request
GET /api/entities/UserProfile
If-Modified-Since: Wed, 08 Jan 2025 10:00:00 GMT

Response:
  304 Not Modified  (if no changes)
  -- OR --
  200 OK + new data (if changed)
```

**ETag:**
```http
GET /api/entities/UserProfile/123
Response:
  ETag: "v42"
  {...}

# Conditional update
PUT /api/entities/UserProfile/123
If-Match: "v42"
{...}

Response:
  200 OK (if match)
  412 Precondition Failed (if stale)
```

---

## 📊 Observabilita

### Metriky (Prometheus)

```
# WebSocket
websocket_connections_active
websocket_heartbeat_drops_total
websocket_rate_limit_exceeded_total

# Workflow
workflow_transitions_total{entity_type,from,to}
workflow_sla_breaches_total{entity_type}

# Documents
document_uploads_total
document_tika_extraction_duration_seconds

# CRUD
crud_read_duration_seconds{entity_type} (p50, p95, p99)
crud_list_duration_seconds (p95)
fulltext_search_duration_seconds

# Cache
cache_hit_rate{cache="redis"}
cache_evictions_total
```

### Logy (Loki)

```
# Presence
[tenant=acme] WebSocket connection established: userId=123
[tenant=acme] User joined: channel=presence:acme:UserProfile:456

# Workflow
[tenant=acme] State transition applied: entity=UserProfile/123, ACTIVATE, draft→active, by=admin

# Fulltext
[tenant=acme] Fulltext search: q="john", types=UserProfile,Document, results=5, took=45ms
```

---

## 🔒 Bezpečnost

### Rate Limiting

- **WebSocket:** 10 events / 5s per user (token bucket)
- **REST API:** Redis-based limiter (future)

### Audit

```sql
-- entity_state_log
INSERT INTO entity_state_log (entity_type, entity_id, changed_by, from_code, to_code)
VALUES ('UserProfile', '123', 'admin@acme.com', 'draft', 'active');

-- presence_activity (optional analytics)
INSERT INTO presence_activity (user_id, entity_type, entity_id, action)
VALUES ('user-123', 'UserProfile', '456', 'join');
```

### Tenant Isolation

- **WebSocket channels:** `presence:<tenantId>:...`
- **Redis keys:** tenant prefix
- **MinIO buckets:** `tenant/<tenantId>/`
- **Postgres RLS:** automatic via `TenantContextFilter`

---

## 🧪 Testy

### Integration Tests (Testcontainers)

```java
@Testcontainers
class Phase2IntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = ...;
  
  @Container
  static GenericContainer<?> redis = ...;
  
  @Container
  static GenericContainer<?> minio = ...;
  
  @Test
  void presenceTracking_multipleUsers() {
    // 2 users join same entity
    // verify Redis presence keys
    // verify heartbeat keeps alive
    // verify leave removes key
  }
  
  @Test
  void workflow_transitionWithGuard() {
    // user without role → transition fails
    // user with role → transition succeeds
    // verify state_log entry
  }
  
  @Test
  void fulltext_indexDocumentAfterUpload() {
    // upload PDF
    // wait for Tika extraction
    // search for keyword → found
  }
  
  @Test
  void pagination_keysetPerformance() {
    // insert 10k records
    // keyset pagination → fast
    // offset pagination → slow (comparison)
  }
}
```

### E2E Tests (Playwright)

```typescript
test('presence and edit indicators', async ({ browser }) => {
  const [page1, page2] = await Promise.all([
    browser.newPage(),
    browser.newPage()
  ]);
  
  // User A opens entity
  await page1.goto('/profiles/123');
  await expect(page1.locator('[data-testid="online-users"]')).toContainText('You');
  
  // User B opens same entity
  await page2.goto('/profiles/123');
  await expect(page2.locator('[data-testid="online-users"]')).toContainText('2 online');
  
  // User A starts editing field
  await page1.locator('#email').focus();
  await page2.waitForSelector('[data-testid="edit-banner"]');
  await expect(page2.locator('[data-testid="edit-banner"]')).toContainText('User A is editing email');
});

test('workflow state transitions', async ({ page }) => {
  await page.goto('/profiles/123');
  
  // Current state
  await expect(page.locator('[data-testid="state-badge"]')).toContainText('Draft');
  
  // Available transitions
  const activateBtn = page.locator('button:has-text("Activate")');
  await expect(activateBtn).toBeEnabled();
  
  // Apply transition
  await activateBtn.click();
  await expect(page.locator('[data-testid="state-badge"]')).toContainText('Active');
  
  // Check SLA indicator
  await expect(page.locator('[data-testid="sla-status"]')).toHaveClass(/sla-ok/);
});
```

---

## 🚀 Deployment

### Docker Compose

```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  
  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data
  
  backend:
    environment:
      REDIS_HOST: redis
      MINIO_ENDPOINT: http://minio:9000
```

### Environment Variables

```bash
# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# MinIO
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# WebSocket
WEBSOCKET_ALLOWED_ORIGINS=https://*.core-platform.local

# Documents
DOCUMENTS_MAX_UPLOAD_SIZE_MB=50
```

---

## 📝 Poznámky

### Limity MVP

- **Dot-notation:** Max 1 JOIN (např. `department.name`)
- **WS broadcast:** Single instance (Redis Pub/Sub pro multi-instance v production)
- **Filter parser:** Závorky podporovány, ale ne plný SpEL
- **Tika:** Synchronní extrakce (async queue v budoucnu)

### Budoucí Rozšíření

- **Collaborative editing:** OT/CRDT algoritmy
- **Advanced SLA:** Eskalace, notifikace
- **Document previews:** Thumbnails, PDF viewer
- **Fulltext ranking:** Custom scoring, facets
- **Distributed cache:** Redis Cluster

---

## 📚 Reference

- [jOOQ Manual](https://www.jooq.org/doc/latest/manual/)
- [MinIO Java Client](https://min.io/docs/minio/linux/developers/java/minio-java.html)
- [Apache Tika](https://tika.apache.org/)
- [PostgreSQL FTS](https://www.postgresql.org/docs/current/textsearch.html)
- [Redis Pub/Sub](https://redis.io/docs/manual/pubsub/)

---

**Autor:** Core Platform Team  
**Verze:** 2.0  
**Datum:** 8. ledna 2025
