# Fáze 3 - Reporting & Analytics - Implementační plán

## ✅ HOTOVO - Fáze 3.0 a částečně 3.1

### Fáze 3.0 - Příprava a konfigurace (KOMPLETNÍ)
- [x] Struktura reporting modulu (`api/`, `app/`, `cube/`, `dsl/`, `jobs/`, `model/`, `repo/`, `security/`, `support/`)
- [x] `application-reporting.yml` s konfigurací
- [x] `ReportingProperties` + `ReportingConfiguration` + `ReportingFeatureToggle`
- [x] Redis + Caffeine cache konfigurace
- [x] RestClient pro Cube.js s Bearer auth
- [x] Závislosti: Caffeine, Bucket4j
- [x] Unit testy (Properties, FeatureToggle)
- [x] Dokumentace: `PHASE_3_0_REPORTING_CONFIG.md`

### Fáze 3.1 - DSL pro dotazy a guardrails (ČÁSTEČNĚ)
- [x] `QueryRequest` DTO (entity, dimensions, measures, filters, orderBy, limit, offset, timeRange)
- [x] `QueryResponse` DTO
- [x] `@ValidQuery` + `QueryRequestValidator` (max rows, max interval, required time range)
- [x] `QueryFingerprint` - deterministický SHA-256 hash
- [x] `EntitySpec` + `MetamodelSpecService` - integrace s metamodelem
- [ ] **TODO:** Unit testy pro validátory a fingerprint
- [ ] **TODO:** Integrační testy DSL validace

---

## 🚧 TODO - Zbývající subfáze

### Fáze 3.2 - Integrace Cube (Semantická vrstva)

**Komponenty k vytvoření:**

1. **`CubeClient` service** (`/reporting/cube/`)
   - Volání `/cubejs-api/v1/load` nebo `/cubejs-api/v1/sql`
   - `Authorization: Bearer ${CUBE_API_TOKEN}`
   - Timeout handling (connectTimeout, readTimeout)
   - Retry logic pro network chyby (exponential backoff)
   - Circuit breaker pattern (Resilience4j)

2. **`CubeMapper`** (`/reporting/cube/`)
   - Převod `QueryRequest` → Cube.js query JSON:
     ```json
     {
       "measures": ["Users.count"],
       "dimensions": ["Users.status", "Users.role"],
       "timeDimensions": [{
         "dimension": "Users.createdAt",
         "dateRange": ["2025-01-01", "2025-10-09"]
       }],
       "filters": [{"member": "Users.tenantId", "operator": "equals", "values": ["tenant-123"]}],
       "order": [["Users.createdAt", "desc"]],
       "limit": 1000,
       "offset": 0
     }
     ```

3. **`CubeSecurityContext`** (`/reporting/security/`)
   - Předávání `tenantId` a `roles` do Cube.js security context
   - Nebo RLS přímo ve schématech Cube (preferováno)

4. **Testy:**
   - WireMock stub pro Cube API
   - Test chování na 4xx/5xx
   - Micrometer metrika: `cube_api_latency_seconds{status}`

**Závislosti:**
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
```

---

### Fáze 3.3 - Reporting Query API (Cache, Rate Limit, Errors)

**API Endpoints:**

1. **`POST /api/reports/query`** (`/reporting/api/ReportQueryController.java`)
   ```java
   @PostMapping("/query")
   public ResponseEntity<QueryResponse> executeQuery(
       @Valid @ValidQuery @RequestBody QueryRequest request,
       Authentication auth
   ) {
       // 1. Validace DSL + guardrails
       // 2. Extract tenantId from JWT
       // 3. Cache lookup (fingerprint)
       // 4. MISS -> CubeClient -> cache store
       // 5. Response s X-Cache: HIT/MISS
   }
   ```

2. **`GET /api/reports/metadata/{entity}`** (`/reporting/api/ReportMetadataController.java`)
   ```java
   @GetMapping("/metadata/{entity}")
   public EntitySpec getMetadata(@PathVariable String entity) {
       return metamodelSpecService.getEntitySpec(entity);
   }
   ```

**Rate Limiting Filter:**
- `RateLimitFilter` extends `OncePerRequestFilter`
- Bucket4j per tenant: `120 req/min`
- Headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`
- 429 Too Many Requests s `Retry-After`

**Error Handling:**
- `@RestControllerAdvice` + `ProblemDetail` (RFC 7807)
- Typy chyb:
  - `INVALID_QUERY` → 400
  - `QUERY_TOO_LARGE` → 400
  - `INTERVAL_TOO_WIDE` → 400
  - `FORBIDDEN_FIELD` → 403
  - `RATE_LIMITED` → 429
  - `UPSTREAM_TIMEOUT` → 504
  - `CUBE_ERROR` → 502

**Cache:**
```java
@Cacheable(value = "reportQueries", key = "#fingerprint")
public QueryResponse executeQuery(String fingerprint, QueryRequest req, String tenant) {
    // Cube API call
}
```

**Testy:**
- Integrační test HIT/MISS
- Rate limit test (burst requests)
- HTTP status codes + Problem+JSON payloads

---

### Fáze 3.4 - Sdílené pohledy (JIRA-style)

**DB Migrace:** `V{timestamp}__create_report_views.sql`
```sql
CREATE TABLE report_view (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('private', 'group', 'tenant', 'global')),
    owner_id UUID NOT NULL REFERENCES "user"(id),
    group_id UUID REFERENCES "group"(id),
    tenant_id UUID NOT NULL,
    definition JSONB NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_report_view_entity ON report_view(entity);
CREATE INDEX idx_report_view_scope ON report_view(scope);
CREATE INDEX idx_report_view_owner ON report_view(owner_id);
CREATE INDEX idx_report_view_tenant ON report_view(tenant_id);
```

**API Endpoints:**
```java
// GET /api/report-views?entity=User&scope=private
// POST /api/report-views
// PUT /api/report-views/{id}
// DELETE /api/report-views/{id}
```

**RBAC:**
- Private: jen `owner_id = currentUser`
- Group: `group_id IN (userGroups)`
- Tenant: role s oprávněním `report-view:tenant:read`
- Global: admin pouze

**Validace `definition`:**
- Kontrola proti `EntitySpec` (povolené sloupce, filtry, aggregace)

---

### Fáze 3.5 - Bulk Update (Async Jobs)

**DB Migrace:** `V{timestamp}__create_bulk_jobs.sql`
```sql
CREATE TABLE reporting_job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP DEFAULT NOW(),
    created_by UUID NOT NULL,
    tenant_id UUID NOT NULL,
    entity VARCHAR(100) NOT NULL,
    where_json JSONB NOT NULL,
    patch_json JSONB NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED')),
    dry_run BOOLEAN DEFAULT FALSE,
    total_rows BIGINT,
    affected_rows BIGINT,
    message TEXT,
    idempotency_key VARCHAR(255) UNIQUE,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE TABLE reporting_job_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES reporting_job(id),
    ts TIMESTAMP DEFAULT NOW(),
    level VARCHAR(20) NOT NULL,
    message TEXT
);

CREATE TABLE audit_change (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ts TIMESTAMP DEFAULT NOW(),
    tenant_id UUID NOT NULL,
    actor UUID,
    entity VARCHAR(100),
    entity_id UUID,
    op VARCHAR(20), -- UPDATE, DELETE
    before JSONB,
    after JSONB,
    job_id UUID REFERENCES reporting_job(id)
);

CREATE INDEX idx_reporting_job_tenant ON reporting_job(tenant_id);
CREATE INDEX idx_reporting_job_status ON reporting_job(status);
CREATE INDEX idx_audit_change_job ON audit_change(job_id);
```

**API:**
```java
// POST /api/entities/{entity}/bulk-update
{
  "where": { /* DSL filters */ },
  "patch": { "status": "archived" },
  "dryRun": true,
  "idempotencyKey": "unique-key-123"
}

// Response (dry-run):
{
  "estimatedRows": 1523,
  "sampleIds": ["uuid1", "uuid2", ...],
  "jobId": null
}

// Response (real run):
{
  "jobId": "job-uuid",
  "status": "PENDING"
}

// GET /api/entities/{entity}/bulk-update/{jobId}
// DELETE /api/entities/{entity}/bulk-update/{jobId} (cancel)
```

**Worker:**
- `@Scheduled` nebo Spring Batch
- Status: `PENDING` → pick job → `RUNNING`
- Chunky: `chunkSize=1000` rows per transaction
- Optimistic locking (`version++`)
- Audit zápisy do `audit_change`
- Limity: `maxAffectRows=500000`

**Security:**
- `PATCH` jen editable fields (dle `EntitySpec`)
- `WHERE` vždy doplň `tenant_id = :tenant` na BE (RLS)

---

### Fáze 3.6 - Metriky a Observabilita

**Micrometer metrics:**
```java
@Timed(value = "report.query.latency", percentiles = {0.95, 0.99}, histogram = true)
@Counted(value = "report.query.total")
public QueryResponse executeQuery(...) { ... }

// Custom metrics:
- report_query_latency_seconds{tenant, entity, cache_hit} (timer)
- report_query_rows{tenant, entity} (distribution summary)
- bulk_jobs_running{tenant} (gauge)
- bulk_rows_changed_total (counter)
```

**Structured logging:**
```java
MDC.put("tenant", tenantId);
MDC.put("entity", entity);
MDC.put("fingerprint", fingerprint);
MDC.put("jobId", jobId);

log.info("Query executed: fingerprint={}, cacheHit={}, rows={}, latency={}ms", ...);
```

**Actuator:**
- `/actuator/prometheus` export

---

### Fáze 3.7 - Security, RLS, Guardrails

**JWT → TenantContext:**
```java
@Component
public class TenantContextFilter extends OncePerRequestFilter {
    protected void doFilterInternal(...) {
        Jwt jwt = ((JwtAuthenticationToken) auth).getToken();
        String tenantId = jwt.getClaim("tenant_id");
        List<String> roles = jwt.getClaim("roles");
        List<String> groups = jwt.getClaim("groups");
        
        TenantContext.set(tenantId, roles, groups);
    }
}
```

**RLS:**
- Čtení: Cube.js schémata s `context.tenantId`
- Zápis: SQL vždy `WHERE tenant_id = :tenant`

**Guardrails:**
- Odmítni dotazy bez filtru/časového okna (lookup tables excepted)
- Strop: `dimensions.size() <= 10`, `measures.size() <= 20`
- Heuristika: vysoká kardinalita `groupBy` → suggest aggregation
- Upstream timeouts: `readTimeout=30s`

**Sanitizace:**
- DSL → bezpečný SQL builder (jOOQ nebo prepared statements)
- Whitelist povolených polí z `EntitySpec`

**Testy:**
- 401 bez JWT
- 403 cizí tenant
- 400/403 nepovolená pole

---

### Fáze 3.8 - Cache a výkon

**Redis cache:**
- Key: `rpt:{tenant}:{entity}:{fingerprint}`
- TTL: `defaultTtlSeconds=60`

**Invalidace:**
- Admin endpoint: `POST /api/reports/cache/invalidate`
  ```json
  {
    "entity": "User",
    "reason": "Schema updated"
  }
  ```
- Audit záznam invalidace

**Limity:**
- Response size: max 10MB
- Při překročení: 413 s návrhem zúžit filtr nebo agregovat

**DoD:**
- p95/p99 latency monitoring
- HIT rate > 60% pro typické dotazy
- Test TTL a invalidace

---

### Fáze 3.9 - Odstranění CDC provisioningu do Grafany

**Akce:**
1. **Smazat kód:**
   - `GrafanaUserSyncService`, `GrafanaCDCWorker`, atd.
   - Schedulery a CRONy

2. **Smazat CI:**
   - `.github/workflows/*-grafana-sync.yml`

3. **Smazat secrets:**
   - `GRAFANA_API_KEY`, `GRAFANA_ORG_ID`

4. **DB migrace:**
   - `DROP TABLE grafana_user_map;` (pokud existuje)

5. **Dokumentace:**
   - Update: Admin přístup přes OIDC (realm admin)
   - Tenanti nechodí do Grafany

---

### Fáze 3.10 - Hardening a záťěžové testy

**Rate-limit:**
- Per tenant per route: `120/min`
- 429 s `Retry-After`

**Idempotency:**
- `Idempotency-Key` header pro bulk operations
- Double-submit protection

**Backpressure:**
- Max paralelní joby: `queueConcurrency=2`
- Health check workeru: `/actuator/health/bulkWorker`

**Load testy (Gatling/JMeter):**
```scala
scenario("Top Views")
  .exec(http("Query Users")
    .post("/api/reports/query")
    .body(StringBody("""{ ... }"""))
    .check(status.is(200)))
  .pause(1)
```

**DoD:**
- Load test report (p95/p99 < 500ms)
- Navržené úpravy limitů

---

### Fáze 3.11 - Dokumentace a Runbooky

**Dokumentace:** `docs/MONITORING_REPORTING.md`
- Architektura (DSL → Cube → BFF)
- Cache strategie
- Rate-limit pravidla
- RLS mechanismy
- Bulk workflow

**Runbooky:** `docs/runbooks/REPORTING_INCIDENTS.md`
- Expirace `CUBE_API_TOKEN` → rotace
- Výpadek Redis → fallback Caffeine
- Selhání workeru → restart, check logs
- Rollback bulk jobu → cancel + audit cleanup

---

## 📋 Acceptance Criteria - Celková DoD

- [ ] `/api/reports/query`: validace, cache, rate-limit, metriky, Problem+JSON
- [ ] `/api/report-views`: CRUD se sdílením (private/group/tenant/global)
- [ ] `/api/entities/{entity}/bulk-update`: dry-run, async job, audit, idempotence, cancel
- [ ] Redis cache (fallback Caffeine)
- [ ] Bucket4j rate-limit (120/min/tenant)
- [ ] Micrometer metriky exportované do Prometheus
- [ ] RLS v čtení (Cube) i zápisu (SQL)
- [ ] Guardrails aktivní (max rows, max interval, forbidden fields)
- [ ] CDC provisioning do Grafany odstraněn (kód, CI, DB, docs)
- [ ] Testy zelené (unit + integration)
- [ ] Zatěžové testy proběhly, limity nastavené
- [ ] Dokumentace + runbooky kompletní

---

## 🚀 Doporučený postup

### Krok 1: Cube.js integrace (Fáze 3.2)
```bash
# Vytvořit komponenty:
touch backend/src/main/java/cz/muriel/core/reporting/cube/CubeClient.java
touch backend/src/main/java/cz/muriel/core/reporting/cube/CubeMapper.java
touch backend/src/main/java/cz/muriel/core/reporting/security/CubeSecurityContext.java
touch backend/src/test/java/cz/muriel/core/reporting/cube/CubeClientTest.java
```

### Krok 2: REST API (Fáze 3.3)
```bash
touch backend/src/main/java/cz/muriel/core/reporting/api/ReportQueryController.java
touch backend/src/main/java/cz/muriel/core/reporting/api/ReportMetadataController.java
touch backend/src/main/java/cz/muriel/core/reporting/security/RateLimitFilter.java
touch backend/src/main/java/cz/muriel/core/reporting/api/ReportingExceptionHandler.java
```

### Krok 3: DB Migrace (Fáze 3.4-3.5)
```bash
touch backend/src/main/resources/db/migration/V202510091400__create_report_views.sql
touch backend/src/main/resources/db/migration/V202510091500__create_bulk_jobs.sql
touch backend/src/main/java/cz/muriel/core/reporting/model/ReportView.java
touch backend/src/main/java/cz/muriel/core/reporting/model/ReportingJob.java
```

### Krok 4: Testy
```bash
# Jednotkové testy
backend/src/test/java/cz/muriel/core/reporting/dsl/QueryFingerprintTest.java
backend/src/test/java/cz/muriel/core/reporting/dsl/QueryRequestValidatorTest.java

# Integrační testy
backend/src/test/java/cz/muriel/core/reporting/api/ReportQueryControllerIT.java
backend/src/test/java/cz/muriel/core/reporting/jobs/BulkUpdateWorkerIT.java
```

### Krok 5: Dokumentace
```bash
touch docs/MONITORING_REPORTING.md
touch docs/runbooks/REPORTING_INCIDENTS.md
```

---

## 📊 Současný stav

- **Větev:** `feature/be-reporting-phase3`
- **Commit:** `4fe112f` - "feat(reporting): Phase 3.0-3.1 - Reporting foundation and DSL"
- **Hotovo:** ~20% celkové fáze
- **Zbývá:** Cube integrace, API, Bulk jobs, Dokumentace, Testy, Cleanup

---

## 💡 Tipy pro pokračování

1. **Postupně:** Implementuj subfázi po subfázi, vždy s testy a commitem
2. **TDD:** Nejprve test, pak implementace
3. **Integration tests:** Testcontainers pro PostgreSQL, Redis, WireMock pro Cube
4. **Metriky:** Micrometer `@Timed` a `@Counted` od začátku
5. **Security first:** RLS a validace vždy před implementací funkce
6. **Dokumentuj průběžně:** Readme v každé subfázi

Potřebuješ-li pokračovat v implementaci konkrétní subfáze, dej vědět!
