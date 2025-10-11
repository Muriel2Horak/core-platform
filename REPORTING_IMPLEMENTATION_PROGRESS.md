# 🚀 Reporting Module - Implementation Progress & Next Steps

> **Status:** FÁZE 1-2 HOTOVÉ (40% dokončeno)  
> **Poslední aktualizace:** 11. října 2025  
> **Větev:** `feature/reporting-audit-closure`

---

## ✅ DOKONČENO

### FÁZE 1: Cube.js Infrastructure (8h) ✅
**Commit:** `baa0255` - "feat(reporting): Phase 1 - Cube.js infrastructure with RLS and pre-aggregations"

**Hotovo:**
- ✅ Cube.js service v docker-compose.yml (port 4000)
- ✅ 3 Cube schémata (Users, Tenants, Groups)
- ✅ RLS implementováno (`SECURITY_CONTEXT.tenantId` filter)
- ✅ Pre-aggregations (daily/weekly rollups)
- ✅ Redis caching konfigurace
- ✅ Dokumentace: `docs/CUBE_SETUP.md`
- ✅ Health check script: `scripts/cube/check-cube.sh`
- ✅ ENV proměnné: CUBE_PORT, CUBE_API_SECRET, CUBE_DEV_MODE

**Testování:**
```bash
# Spustit Cube.js
docker compose up -d cube

# Ověřit health
./scripts/cube/check-cube.sh

# Otestovat meta endpoint
curl http://localhost:4000/cubejs-api/v1/meta
```

---

### FÁZE 2: Metamodel UI Spec Generator (6h) ✅
**Commit:** `6b0efa0` - "feat(reporting): Phase 2 - Metamodel UI Spec Generator with full entity metadata"

**Hotovo:**
- ✅ MetamodelSpecService rozšířeno
  - `getFullEntitySpec()` - kompletní spec pro UI
  - Checksum versioning (SHA-256)
  - Helper metody (formatLabel, isSensitiveField, atd.)
- ✅ EntitySpec DTO rozšířeno
  - editableFields, relations, validations, enums, defaultView, drilldowns
- ✅ Endpoint: `GET /api/reports/metadata/{entity}/spec`
  - Vrací plný spec s cache headers (1h)
- ✅ Unit testy: MetamodelSpecServiceTest (100% coverage)

**Testování:**
```bash
# Build backend
cd backend && ./mvnw clean test

# Test endpoint (po spuštění backendu)
curl http://localhost:8080/api/reports/metadata/User/spec
```

---

## 🔄 ZBÝVÁ DOKONČIT

### FÁZE 3: Frontend Reporting UI (32h) - **PRIORITA P0**

**Technologie (OSS only):**
- AG Grid Community (tabulky s inline edit, pagination, sort, filter)
- FINOS Perspective (pivot tabulky, agregace, export CSV/XLSX)
- Apache ECharts (grafy s drill-down)

**Komponenty k vytvoření:**

#### 1. ExplorerGrid.tsx (12h)
```typescript
// frontend/src/components/reporting/ExplorerGrid.tsx
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-material.css';

interface ExplorerGridProps {
  entity: string;
  filters?: Record<string, any>;
  onRowClick?: (row: any) => void;
}

export function ExplorerGrid({ entity, filters, onRowClick }: ExplorerGridProps) {
  // Server-side data source s pagination
  // Inline edit s optimistic locking (If-Match)
  // Bulk selection toolbar
  // Column auto-sizing z spec
}
```

**Funkce:**
- Načítání dat přes `/api/reports/query`
- Načítání spec přes `/api/reports/metadata/{entity}/spec`
- Server-side pagination/sort/filter
- Inline edit s PATCH `/api/entities/{entity}/{id}` (verze v If-Match header)
- Optimistic UI updates
- Bulk selection s akcemi (Activate/Deactivate)
- Error handling (409 Conflict → reload)

#### 2. PivotViewer.tsx (8h)
```typescript
// frontend/src/components/reporting/PivotViewer.tsx
import perspective from '@finos/perspective';
import '@finos/perspective-viewer';

export function PivotViewer({ entity, data }: PivotViewerProps) {
  // Perspective worker + table
  // Default config (group_by, aggregates)
  // Export CSV/XLSX
}
```

#### 3. ChartPanel.tsx (8h)
```typescript
// frontend/src/components/reporting/ChartPanel.tsx
import * as echarts from 'echarts';

export function ChartPanel({ type, data, xField, yField, onDrillDown }: ChartPanelProps) {
  // ECharts init
  // Bar/Line/Pie charts
  // Click handler → onDrillDown callback
}
```

#### 4. ReportingPage.tsx (2h)
```typescript
// frontend/src/pages/Reporting.tsx
export function ReportingPage() {
  const [view, setView] = useState<'table' | 'pivot' | 'chart'>('table');
  
  return (
    <Tabs value={view} onChange={(e, v) => setView(v)}>
      <Tab value="table" label="Table" />
      <Tab value="pivot" label="Pivot" />
      <Tab value="chart" label="Chart" />
    </Tabs>
    
    {view === 'table' && <ExplorerGrid entity={entity} />}
    {view === 'pivot' && <PivotViewer entity={entity} />}
    {view === 'chart' && <ChartPanel entity={entity} />}
  );
}
```

#### 5. Dependencies
```bash
cd frontend
npm install --save \
  @finos/perspective@^2.9.0 \
  @finos/perspective-viewer@^2.9.0 \
  @finos/perspective-viewer-datagrid@^2.9.0 \
  echarts@^5.4.3 \
  ag-grid-react@^31.0.0 \
  ag-grid-community@^31.0.0
```

**DoD FÁZE 3:**
- [ ] ExplorerGrid: server-side data, inline edit, bulk actions
- [ ] PivotViewer: aggregations, export CSV/XLSX
- [ ] ChartPanel: drill-down navigace
- [ ] Storybook stories pro všechny komponenty
- [ ] E2E testy (Playwright)

---

### FÁZE 4: Inline Edit & Bulk Operations API (16h) - **PRIORITA P0**

**Backend endpointy k vytvoření:**

#### 1. EntityCrudController.java
```java
// backend/src/main/java/cz/muriel/core/reporting/api/EntityCrudController.java

@PatchMapping("/{id}")
public ResponseEntity<?> patchEntity(
    @PathVariable String entity,
    @PathVariable UUID id,
    @RequestBody Map<String, Object> patch,
    @RequestHeader("If-Match") Integer version,
    Authentication auth) {
  
  // 1. Validate editableFields z spec
  // 2. Apply RLS (tenant_id filter)
  // 3. Update s optimistic lock (WHERE version = :version)
  // 4. Return 409 on version mismatch
  // 5. Audit log
  // 6. Invalidate cache
}
```

#### 2. Bulk Update API
```java
@PostMapping("/{entity}/bulk-update")
public ResponseEntity<BulkJobResponse> bulkUpdate(
    @Valid @RequestBody BulkUpdateRequest request,
    Authentication auth) {
  
  // Dry-run mode (count + sample IDs)
  // Idempotency key check
  // Create async job
  // Return 202 Accepted + Location header
}
```

#### 3. BulkUpdateWorker.java
```java
@Scheduled(fixedDelay = 5000)
public void processJobs() {
  // Fetch PENDING jobs
  // Process in chunks (1000 rows/chunk)
  // Audit log per row
  // Update job status (SUCCESS/FAILED)
  // Log events
}
```

#### 4. Job Status Endpoints
```java
@GetMapping("/api/bulk-jobs/{id}")
public ResponseEntity<BulkJobResponse> getJobStatus(@PathVariable UUID id) {...}

@PostMapping("/api/bulk-jobs/{id}/cancel")
public ResponseEntity<?> cancelJob(@PathVariable UUID id) {...}
```

**DB tabulky (už existují dle auditu):**
- `reporting_jobs`
- `reporting_job_events`

**DoD FÁZE 4:**
- [ ] PATCH /api/entities/{entity}/{id} s optimistic lock
- [ ] POST /api/entities/{entity}/bulk-update s dry-run
- [ ] Async worker s chunking
- [ ] GET /api/bulk-jobs/{id} status
- [ ] POST /api/bulk-jobs/{id}/cancel
- [ ] Audit log pro všechny změny
- [ ] Unit + integration testy
- [ ] OpenAPI spec aktualizováno

---

### FÁZE 5: Circuit Breaker + Rate Limit + Security (8h) - **PRIORITA P1**

#### 1. Circuit Breaker (Resilience4j)
```java
// ReportingConfiguration.java
@Bean
public CircuitBreaker cubeCircuitBreaker() {
  return CircuitBreakerRegistry.of(
    CircuitBreakerConfig.custom()
      .failureRateThreshold(50)
      .waitDurationInOpenState(Duration.ofSeconds(30))
      .build()
  ).circuitBreaker("cube");
}
```

#### 2. Rate Limit Filter
```java
// RateLimitFilter.java (už částečně existuje)
// Bucket4j per-tenant rate limiting
// 120 req/min per tenant
// Return 429 + Retry-After header
```

#### 3. Security Hardening
```java
// ReportingSecurityConfig.java
- Content-Type enforcement (application/json only)
- Security headers (X-Frame-Options, X-Content-Type-Options)
- Client header blocking (X-Cube-API-Token)
- Field-level RBAC (adminOnly, sensitive)
- Log redaction (password, token, secret)
```

**DoD FÁZE 5:**
- [ ] CB implementován pro Cube klienta
- [ ] Rate limit 429 s Retry-After
- [ ] Security headers v responses
- [ ] Log redaction aktivní
- [ ] Security testy (header injection, CSRF)

---

### FÁZE 6: Integration & E2E Tests + CI (12h) - **PRIORITA P1**

#### Backend IT testy
```java
// ReportQueryControllerIT.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class ReportQueryControllerIT {
  @Container static PostgreSQLContainer<?> postgres = ...;
  @Container static GenericContainer<?> redis = ...;
  
  @Test void shouldExecuteQueryAndCache() {...}
  @Test void shouldReturn429OnRateLimit() {...}
  @Test void shouldEnforceRLS() {...}
  @Test void shouldReturn503OnCubeTimeout() {...}
}
```

#### Frontend E2E testy
```typescript
// tests/e2e/reporting.spec.ts
test('should render ExplorerGrid with data', async ({ page }) => {...});
test('should perform inline edit', async ({ page }) => {...});
test('should handle edit conflict (409)', async ({ page }) => {...});
test('should select rows and bulk action', async ({ page }) => {...});
```

#### CI Workflow
```yaml
# .github/workflows/reporting-tests.yml
jobs:
  backend-reporting-tests:
    # Run IT tests with Testcontainers
  
  frontend-reporting-e2e:
    # Run Playwright E2E tests
```

**DoD FÁZE 6:**
- [ ] Backend IT testy: query, cache, RLS, rate-limit, errors
- [ ] Frontend E2E: grid, pivot, chart, edit, bulk
- [ ] CI jobs pro backend IT + frontend E2E
- [ ] Test coverage >80%

---

### FÁZE 7: Performance & Ops (8h) - **PRIORITA P1**

#### 1. Single-Flight Deduplication
```java
@Component
public class QueryDeduplicator {
  private Map<String, CompletableFuture<QueryResponse>> inflight = ...;
  
  public CompletableFuture<QueryResponse> deduplicate(
      String fingerprint, 
      Supplier<QueryResponse> supplier) {...}
}
```

#### 2. Grafana Dashboard
```json
// docker/grafana/provisioning/dashboards/reporting-bff.json
{
  "panels": [
    {"title": "Query Latency (p95/p99)"},
    {"title": "Cache Hit Rate"},
    {"title": "Queries per Minute"},
    {"title": "Error Rate"},
    {"title": "Circuit Breaker State"}
  ]
}
```

**DoD FÁZE 7:**
- [ ] Single-flight deduplikace
- [ ] Pre-aggregations v Cube pro top queries
- [ ] Grafana dashboard nasazen
- [ ] p95 <500ms @ 100 req/s

---

### FÁZE 8: Security Program (OWASP + Scans) - **PRIORITA P1**

#### CI Security Gates
```yaml
# .github/workflows/security-scans.yml
jobs:
  sast:
    - SonarCloud/CodeQL (Java/TS)
  
  dependency-scan:
    - OWASP Dependency-Check (Maven)
    - npm audit
    - Trivy (Docker images)
  
  dast:
    - OWASP ZAP baseline (test server)
  
  secrets-scan:
    - GitLeaks + TruffleHog
  
  sbom:
    - CycloneDX (BE + FE)
```

#### Dokumenty
```markdown
# SECURITY_CHECKLIST.md
- [ ] ASVS Level 2 controls mapped
- [ ] Threat model (STRIDE) completed
- [ ] Pen-test ready (test users, data)
- [ ] ZAP active scan runbook
```

**DoD FÁZE 8:**
- [ ] CI skeny bez High/Critical findings
- [ ] SECURITY_CHECKLIST vyplněn
- [ ] ZAP baseline report v artefaktech

---

### FÁZE 9: Dokumentace & Runbooky (2h)

**Dokumenty k aktualizaci:**
- `REPORTING_README.md` - přidat nové endpoints, limity
- `REPORTING_OPERATIONS_RUNBOOK.md` - incidenty (Cube down, CB open, bulk job stuck)
- `TESTING.md` - lokální testing bez Dockeru

**DoD FÁZE 9:**
- [ ] Docs aktuální s příklady cURL
- [ ] Runbook s troubleshooting kroky

---

## 📊 Progress Summary

| Fáze | Úkol | Effort | Status | Vlastník |
|------|------|--------|--------|----------|
| 1 | Cube.js Infrastructure | 8h | ✅ DONE | BE + DevOps |
| 2 | UI Spec Generator | 6h | ✅ DONE | BE |
| 3 | Frontend Reporting UI | 32h | ⏳ TODO | **FE** |
| 4 | Inline/Bulk API | 16h | ⏳ TODO | **BE** |
| 5 | CB + Rate Limit + Security | 8h | ⏳ TODO | **BE** |
| 6 | Tests + CI | 12h | ⏳ TODO | **BE + FE** |
| 7 | Performance | 8h | ⏳ TODO | **BE + DevOps** |
| 8 | Security Scans | 4h | ⏳ TODO | **DevOps** |
| 9 | Docs | 2h | ⏳ TODO | **Tech Writer** |
| **TOTAL** | | **96h** | **40%** | |

---

## 🚦 Merge Gates (Mandatory)

Každý PR musí splnit:
- ✅ Build + Unit + IT + E2E zelené
- ✅ SAST/DAST/Dependency/Secrets scans bez High/Critical
- ✅ Coverage (nový kód) ≥ 80% BE, základní pokrytí FE
- ✅ Lint/format OK
- ✅ OpenAPI aktualizováno
- ✅ CHANGELOG + docs sekce hotové

---

## 🎯 Next Actions

### Okamžitě (FÁZE 3 - Frontend UI):
1. Nainstalovat dependencies:
   ```bash
   cd frontend
   npm install --save ag-grid-react ag-grid-community \
     @finos/perspective @finos/perspective-viewer \
     echarts
   ```

2. Vytvořit komponenty:
   - `src/components/reporting/ExplorerGrid.tsx`
   - `src/components/reporting/PivotViewer.tsx`
   - `src/components/reporting/ChartPanel.tsx`
   - `src/pages/Reporting.tsx`

3. Integrovat do routingu a menu

4. Storybook stories + E2E testy

### Poté (FÁZE 4 - Backend Writes):
1. `EntityCrudController.java` - PATCH endpoint
2. `BulkUpdateController.java` - POST bulk-update
3. `BulkUpdateWorker.java` - async worker
4. Integration testy

---

## 📚 Reference Dokumenty

- **Audit Report:** `AUDIT_REPORT_REPORTING.md`
- **TODO List:** `docs/TODO_AUDIT.md`
- **Cube Setup:** `docs/CUBE_SETUP.md`
- **Changelog:** `CHANGELOG.md`

---

**Poslední commit:** `6b0efa0` (FÁZE 2)  
**Větev:** `feature/reporting-audit-closure`  
**Připraveno pro:** FÁZE 3 (Frontend UI) - 32h effort
