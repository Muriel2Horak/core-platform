# Fáze 3 - Reporting & Analytics - Souhrn implementace

## ✅ Hotové (2 commity)

### Commit 1: `4fe112f` - Foundation and DSL
**Fáze 3.0 - Příprava a konfigurace (KOMPLETNÍ)**
- ✅ Struktura reporting modulu (8 balíčků)
- ✅ Konfigurace: `application-reporting.yml`, `ReportingProperties`, `ReportingConfiguration`
- ✅ Feature toggles: `ReportingFeatureToggle`
- ✅ Cache: Redis (primární) + Caffeine (fallback)
- ✅ RestClient pro Cube.js s Bearer autentizací
- ✅ Závislosti: Caffeine 3.x, Bucket4j 8.10.1

**Fáze 3.1 - DSL pro dotazy (ČÁSTEČNĚ)**
- ✅ `QueryRequest` DTO (dimensions, measures, filters, timeRange, pagination)
- ✅ `QueryResponse` DTO (data, metadata, cacheHit, executionTime)
- ✅ `@ValidQuery` constraint + `QueryRequestValidator`
- ✅ Guardrails: max rows (50k), max interval (92 days), required time range
- ✅ `QueryFingerprint` - SHA-256 deterministický hash
- ✅ `MetamodelSpecService` - integrace s `MetamodelRegistry`
- ✅ `EntitySpec` - allowed dimensions/measures/filters per entity
- ✅ Unit testy: `ReportingPropertiesTest`, `ReportingFeatureToggleTest` (100% coverage)

### Commit 2: `91592b0` - Implementation Plan
- ✅ Komplexní plán všech 11 subfází
- ✅ SQL schémata pro `report_view`, `reporting_job`, `audit_change`
- ✅ API specifikace (endpoints, request/response)
- ✅ Bezpečnostní požadavky (RLS, RBAC, sanitizace)
- ✅ Metriky a observabilita (Micrometer, structured logging)
- ✅ Runbook šablony pro incidenty

---

## 📂 Struktura kódu

```
backend/src/main/java/cz/muriel/core/reporting/
├── api/                    # REST kontrolery (TODO)
│   ├── ReportQueryController.java
│   ├── ReportMetadataController.java
│   ├── ReportViewController.java
│   ├── BulkUpdateController.java
│   └── ReportingExceptionHandler.java
├── app/                    # Konfigurace ✅
│   ├── ReportingConfiguration.java
│   ├── ReportingProperties.java
│   └── ReportingFeatureToggle.java
├── cube/                   # Cube.js integrace (TODO)
│   ├── CubeClient.java
│   ├── CubeMapper.java
│   └── CubeSecurityContext.java
├── dsl/                    # Query DSL ✅
│   ├── QueryRequest.java
│   ├── QueryResponse.java
│   ├── QueryRequestValidator.java
│   └── ValidQuery.java
├── jobs/                   # Bulk operations (TODO)
│   ├── BulkUpdateWorker.java
│   ├── BulkUpdateService.java
│   └── JobStatusTracker.java
├── model/                  # JPA entity (TODO)
│   ├── ReportView.java
│   ├── ReportingJob.java
│   ├── ReportingJobEvent.java
│   └── AuditChange.java
├── repo/                   # Repository (TODO)
│   ├── ReportViewRepository.java
│   ├── ReportingJobRepository.java
│   └── AuditChangeRepository.java
├── security/               # Security & RLS (TODO)
│   ├── RateLimitFilter.java
│   ├── TenantContextFilter.java
│   └── QuerySanitizer.java
└── support/                # Pomocné třídy ✅
    ├── EntitySpec.java
    ├── MetamodelSpecService.java
    └── QueryFingerprint.java
```

---

## 🎯 Klíčové vlastnosti (hotové části)

### 1. Configuration-driven
```yaml
reporting:
  enabled: true
  max-rows: 50000
  max-interval-days: 92
  cache:
    provider: redis  # nebo caffeine
  rate-limit:
    per-tenant-per-min: 120
  cube:
    base-url: http://cube:4000
    api-token: ${CUBE_API_TOKEN}
  bulk:
    chunk-size: 1000
    max-affect-rows: 500000
```

### 2. Validace s guardrails
```java
@PostMapping("/api/reports/query")
public QueryResponse query(@Valid @ValidQuery @RequestBody QueryRequest req) {
    // Automatická validace:
    // - limit <= 50000
    // - timeRange interval <= 92 days
    // - required timeRange (kromě lookup tables)
    // - allowed dimensions/measures/filters (z metamodelu)
}
```

### 3. Cache fingerprinting
```java
String fingerprint = queryFingerprint.generate(tenantId, query, specVersion);
// SHA-256 hash z:
// - tenant, entity, specVersion
// - sorted dimensions, measures, filters
// - timeRange, limit, offset, orderBy
// Výsledek: "a3f2e1d... " (64 chars hex)
```

### 4. Metamodel integrace
```java
EntitySpec spec = metamodelSpecService.getEntitySpec("User");
// Vrací:
// - allowedDimensions: ["id", "email", "status", "role", ...]
// - allowedMeasures: ["login_count", "session_duration", ...]
// - allowedFilters: ["email", "status", "created_at", ...]
// - allowedAggregations: ["count", "sum", "avg", "min", "max", "countDistinct"]
// - fields: [FieldSpec{name, type, editable, filterable, sortable, allowedOperators}]
```

---

## 🔄 Workflow (plánovaný)

### Read Query Flow
```
User → POST /api/reports/query
  ↓
1. JWT auth → TenantContext (tenantId, roles, groups)
2. Validace DSL (@ValidQuery)
3. MetamodelSpecService.validateQuery() 
4. QueryFingerprint.generate()
5. Cache lookup (Redis)
   ├─ HIT → Response + X-Cache: HIT
   └─ MISS ↓
6. CubeMapper.toeCubeQuery()
7. CubeClient.execute() → Cube.js API
8. Response → Cache store (TTL=60s)
9. Response + X-Cache: MISS + X-Query-Time-Ms
```

### Bulk Update Flow (TODO)
```
User → POST /api/entities/User/bulk-update
  ↓
1. JWT auth + RBAC check
2. Dry-run? → COUNT(*) + sample IDs → Response
3. Real run:
   a. Create ReportingJob (status=PENDING, idempotency_key)
   b. Worker picks job (status=RUNNING)
   c. Process chunks (1000 rows/tx)
      - Validate editable fields (EntitySpec)
      - Optimistic locking (version++)
      - Audit log (before/after)
   d. Complete (status=SUCCESS/FAILED)
4. Response: { jobId, status }

User → GET /api/entities/User/bulk-update/{jobId}
  → { status, totalRows, affectedRows, progress, events }

User → DELETE /api/entities/User/bulk-update/{jobId}
  → Cancel if PENDING/RUNNING
```

---

## 📊 Metriky (plánované)

```java
// Automatické (@Timed, @Counted):
report_query_latency_seconds{tenant, entity, cache_hit, quantile}
report_query_total{tenant, entity, status}
report_query_rows{tenant, entity}

// Custom:
bulk_jobs_running{tenant}
bulk_rows_changed_total{tenant, entity}
cube_api_latency_seconds{status}
cache_hit_ratio{cache_type} // redis vs caffeine
```

---

## 🔒 Security (plánované)

### RLS (Row-Level Security)
- **Čtení:** Cube.js schémata s `context.tenantId`
- **Zápis:** SQL vždy `WHERE tenant_id = :tenant` (BE-side)

### RBAC (Role-Based Access Control)
- **Query API:** autorizovaní uživatelé tenanta
- **Report Views:**
  - `private`: jen owner
  - `group`: members skupiny
  - `tenant`: role `report-view:tenant:read`
  - `global`: admin only
- **Bulk Update:** role `entity:{entity}:bulk-update`

### Sanitizace
- DSL → jOOQ nebo prepared statements (NO raw SQL)
- Whitelist polí z `EntitySpec`
- Validace operátorů a hodnot

---

## 📈 Výkon (cíle)

| Metrika | Cíl | Současný stav |
|---------|-----|---------------|
| Query latency (p95) | < 500ms | N/A (TODO) |
| Query latency (p99) | < 1s | N/A (TODO) |
| Cache HIT rate | > 60% | N/A (TODO) |
| Max concurrent queries | 1000/sec | N/A (TODO) |
| Bulk update throughput | 10k rows/sec | N/A (TODO) |

---

## 🧪 Testy (plánované)

### Unit testy (částečně hotové)
- ✅ `ReportingPropertiesTest` - konfigurace
- ✅ `ReportingFeatureToggleTest` - toggles
- ⏳ `QueryFingerprintTest` - determinismus, collision resistance
- ⏳ `QueryRequestValidatorTest` - všechny validation rules
- ⏳ `MetamodelSpecServiceTest` - mock MetamodelRegistry
- ⏳ `CubeMapperTest` - DSL → Cube query mapping

### Integrační testy (TODO)
- `ReportQueryControllerIT` - e2e query flow + cache
- `RateLimitFilterIT` - bucket4j rate limiting
- `BulkUpdateWorkerIT` - chunking, audit, rollback
- `ReportViewControllerIT` - CRUD + RBAC

### Load testy (TODO)
- Gatling/JMeter scénáře
- Top 10 queries (realistic data)
- Sledování p95/p99 pod zátěží

---

## 📝 Dokumentace

| Dokument | Status |
|----------|--------|
| `PHASE_3_0_REPORTING_CONFIG.md` | ✅ Hotovo |
| `PHASE_3_IMPLEMENTATION_PLAN.md` | ✅ Hotovo |
| `MONITORING_REPORTING.md` | ⏳ TODO |
| `runbooks/REPORTING_INCIDENTS.md` | ⏳ TODO |
| API docs (OpenAPI/Swagger) | ⏳ TODO |

---

## 🚀 Next Steps

### Priorita 1: Cube.js integrace (Fáze 3.2)
```bash
# Vytvořit:
backend/src/main/java/cz/muriel/core/reporting/cube/CubeClient.java
backend/src/main/java/cz/muriel/core/reporting/cube/CubeMapper.java
backend/src/test/java/cz/muriel/core/reporting/cube/CubeClientTest.java

# Přidat Resilience4j:
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>

# Test s WireMock:
@AutoConfigureWireMock
class CubeClientTest { ... }
```

### Priorita 2: REST API (Fáze 3.3)
```bash
# Vytvořit:
backend/src/main/java/cz/muriel/core/reporting/api/ReportQueryController.java
backend/src/main/java/cz/muriel/core/reporting/api/ReportingExceptionHandler.java
backend/src/main/java/cz/muriel/core/reporting/security/RateLimitFilter.java

# Integrační test:
backend/src/test/java/cz/muriel/core/reporting/api/ReportQueryControllerIT.java
```

### Priorita 3: DB + Bulk (Fáze 3.4-3.5)
```bash
# Migrace:
backend/src/main/resources/db/migration/V202510091400__create_report_views.sql
backend/src/main/resources/db/migration/V202510091500__create_bulk_jobs.sql

# Modely:
backend/src/main/java/cz/muriel/core/reporting/model/ReportView.java
backend/src/main/java/cz/muriel/core/reporting/model/ReportingJob.java
```

---

## 🎓 Lessons Learned

1. **Metamodel integrace:** Použití `MetamodelRegistry` + `EntitySchema` místo custom definicí
2. **FieldSchema:** `type` je `String`, ne enum → lowercase stringy ("string", "uuid", "timestamp")
3. **Feature toggles:** `@ConditionalOnProperty` pro gradual rollout
4. **Cache abstrakce:** Redis primární, Caffeine fallback (graceful degradation)
5. **Fingerprinting:** Deterministické hashování pro cache klíče (sorted collections)
6. **Guardrails:** Validace na více úrovních (Bean Validation + custom validators + metamodel check)

---

## 🏁 Acceptance (celková DoD)

- [ ] `/api/reports/query` funguje s cache, rate-limit, metrikami
- [ ] `/api/report-views` CRUD + RBAC
- [ ] `/api/entities/{entity}/bulk-update` dry-run + async job
- [ ] Redis cache + Caffeine fallback
- [ ] Bucket4j rate-limit (120/min/tenant)
- [ ] Micrometer metriky do Prometheus
- [ ] RLS v čtení (Cube) i zápisu (SQL)
- [ ] CDC provisioning do Grafany odstraněn
- [ ] Unit + integrační testy zelené
- [ ] Load testy s p95 < 500ms
- [ ] Dokumentace + runbooky

**Současný progress: ~20% ✅**

---

## 📞 Kontakt

Pro pokračování v implementaci konkrétní subfáze:
1. Specifikuj číslo fáze (3.2 - 3.11)
2. Požadované komponenty (např. "CubeClient s retry logikou")
3. Případně priority (security first, performance first, apod.)

Doporučuji **TDD přístup**: Test → Implement → Refactor → Commit → Next
