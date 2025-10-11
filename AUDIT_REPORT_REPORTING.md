# 📊 AUDIT REPORT: Reporting Subsystém

## 0️⃣ Metainfo

| Položka | Hodnota |
|---------|---------|
| **Commit SHA** | `f79f51341fb253c0e64b3b09baf5bdce22e3389c` |
| **Datum auditu** | 11. října 2025 |
| **Java/Maven** | Java 21.0.8 (Eclipse Adoptium) / Maven 3.9.11 |
| **Node.js/npm** | Node.js v24.3.0 / npm 11.4.2 |
| **Auditor** | Senior Auditor - Reporting Module Review |

---

## 📋 Executive Summary

### Celkové hodnocení: ⚠️ **PARTIAL (65%)**

**Implementováno:**
- ✅ BFF Reporting API (DSL, Cube mapper, cache, rate-limit)
- ✅ DB schémata (report_view, reporting_job, audit_change)
- ✅ Konfigurace a properties
- ✅ Základní testy (unit)

**Chybí:**
- ❌ Cube.js instalace a konfigurace (samostatný service)
- ❌ Cube.js schémata pro entity
- ❌ UI komponenty (ExplorerGrid, PivotViewer, ChartPanel)
- ❌ Bulk update implementace (async worker)
- ❌ Inline edit API
- ❌ Generátor UI spec z metamodelu
- ❌ FINOS Perspective integrace
- ❌ Circuit breaker pro Cube.js
- ❌ E2E testy reporting funkcí
- ❌ Pre-aggregace a refresh strategie

---

## 1️⃣ Semantická vrstva (Cube.js)

### STATUS: ❌ **FAIL**

| Požadavek | Evidence | Náprava |
|-----------|----------|---------|
| **Instalace Cube.js** | ❌ Chybí v docker-compose.yml | Přidat Cube.js service do docker-compose.yml |
| **Konfigurace** | ✅ `application-reporting.yml:13-17` (base-url, token) | - |
| **Schémata** | ❌ Chybí cube/ adresář | Vytvořit cube/ s schématy pro klíčové entity |
| **RLS (securityContext)** | ✅ `CubeMapper.java:97-99` (tenantId filter) | - |
| **Generátor schémat** | ❌ Chybí skript | Vytvořit skript generující Cube.js schémata z metamodelu |
| **Dokumentace** | ☑️ Částečná - `REPORTING_README.md` | Rozšířit o Cube.js model, pre-aggs, refresh |

### EVIDENCE:
```yaml
# application-reporting.yml:13-17
cube:
  base-url: ${CUBE_BASE_URL:http://cube:4000}
  api-token: ${CUBE_API_TOKEN:}
  connect-timeout-ms: 5000
  read-timeout-ms: 30000
```

```java
// CubeMapper.java:97-99 - RLS tenant filter
cubeFilters.add(
  CubeQueryRequest.Filter.builder()
    .member(toCubeDimension(query.getEntity(), "tenant_id"))
    .operator("equals").values(List.of(tenantId)).build()
);
```

### NÁPRAVA:

**PR #1: Cube.js Infrastructure & Schemas** (⏱ 8h, vlastník: DevOps/BE)

```diff
# docker/docker-compose.yml
+  # Cube.js Semantic Layer
+  cube:
+    image: cubejs/cube:latest
+    container_name: core-cube
+    ports:
+      - "4000:4000"
+    environment:
+      - CUBEJS_DB_TYPE=postgres
+      - CUBEJS_DB_HOST=db
+      - CUBEJS_DB_PORT=5432
+      - CUBEJS_DB_NAME=${DB_INTERNAL_NAME}
+      - CUBEJS_DB_USER=${DB_INTERNAL_USERNAME}
+      - CUBEJS_DB_PASS=${DB_INTERNAL_PASSWORD}
+      - CUBEJS_API_SECRET=${CUBE_API_TOKEN}
+      - CUBEJS_DEV_MODE=true
+    volumes:
+      - ./cube/schema:/cube/conf/schema
+    depends_on:
+      - db
+    networks:
+      - core-net

# Vytvořit cube/schema/Users.js
+cube(`Users`, {
+  sql: `SELECT * FROM users_directory WHERE tenant_id = \${SECURITY_CONTEXT.tenantId.filter('tenant_id')}`,
+  
+  measures: {
+    count: { sql: `id`, type: `count` },
+    activeCount: {
+      sql: `id`,
+      type: `count`,
+      filters: [{ sql: `\${CUBE}.status = 'ACTIVE'` }]
+    }
+  },
+  
+  dimensions: {
+    id: { sql: `id`, type: `string`, primaryKey: true },
+    email: { sql: `email`, type: `string` },
+    status: { sql: `status`, type: `string` },
+    createdAt: { sql: `created_at`, type: `time` }
+  },
+  
+  preAggregations: {
+    statusRollup: {
+      measures: [count],
+      dimensions: [status],
+      timeDimension: createdAt,
+      granularity: `day`,
+      refreshKey: { every: `1 hour` }
+    }
+  }
+});
```

**Priorita:** 🔴 **P0** (blocking)

---

## 2️⃣ Metamodel → UI Spec

### STATUS: ❌ **FAIL**

| Požadavek | Evidence | Náprava |
|-----------|----------|---------|
| **Spec JSON generátor** | ❌ Chybí script/tool | Implementovat MetamodelSpecExporter |
| **Spec obsah** | ☑️ `EntitySpec.java` (partial) | Rozšířit o drilldowns, validations, enums |
| **Verzování** | ❌ Chybí specVersion/checksum | Přidat version tracking |

### EVIDENCE:
```java
// MetamodelSpecService.java:24-48 - Partial spec support
public EntitySpec getEntitySpec(String entityName) {
  EntitySchema schema = registry.getEntitySchema(entityName);
  // Returns dimensions, measures, basic metadata
}
```

### NÁPRAVA:

**PR #2: Metamodel Spec Generator** (⏱ 6h, vlastník: BE)

```diff
# Vytvořit backend/src/main/java/cz/muriel/core/reporting/support/MetamodelSpecExporter.java
+@Component
+public class MetamodelSpecExporter {
+  
+  public EntitySpecDTO exportEntitySpec(String entity) {
+    EntitySchema schema = registry.getEntitySchema(entity);
+    
+    return EntitySpecDTO.builder()
+      .entity(entity)
+      .specVersion(computeChecksum(schema))
+      .dimensions(extractDimensions(schema))
+      .measures(extractMeasures(schema))
+      .relations(extractRelations(schema))
+      .editableFields(extractEditableFields(schema))
+      .validations(extractValidations(schema))
+      .defaultView(extractDefaultView(schema))
+      .drilldowns(extractDrilldowns(schema))
+      .build();
+  }
+  
+  private String computeChecksum(EntitySchema schema) {
+    return DigestUtils.sha256Hex(objectMapper.writeValueAsString(schema));
+  }
+}

# Přidat endpoint
+@GetMapping("/api/reports/metadata/{entity}/spec")
+public ResponseEntity<EntitySpecDTO> getFullSpec(@PathVariable String entity) {
+  return ResponseEntity.ok(specExporter.exportEntitySpec(entity));
+}
```

**Priorita:** 🔴 **P0** (blocking pro FE)

---

## 3️⃣ BFF Reporting API (DSL → Cube)

### STATUS: ✅ **PASS (90%)**

| Požadavek | Evidence | Náprava |
|-----------|----------|---------|
| **DSL DTO** | ✅ `QueryRequest.java:11-50` | - |
| **Mapper (DSL→Cube)** | ✅ `CubeMapper.java:29-101` | - |
| **POST /api/reports/query** | ✅ `ReportQueryController.java:28-50` | - |
| **Cache** | ✅ Redis (application-reporting.yml:8) | - |
| **Rate-limit** | ✅ Bucket4j (pom.xml:252-261) | ⚠️ Chybí filter implementace |
| **Circuit breaker** | ❌ Chybí pro Cube.js | Přidat Resilience4j CB |
| **Guardrails** | ✅ `QueryRequestValidator.java:31-68` | - |
| **Problem+JSON** | ☑️ `ReportingExceptionHandler.java` | Rozšířit handling |

### EVIDENCE:
```java
// QueryRequestValidator.java:31-37 - Guardrails
if (query.getLimit() != null && query.getLimit() > properties.getMaxRows()) {
  context.buildConstraintViolationWithTemplate(
    "Limit exceeds maximum allowed rows: " + properties.getMaxRows())
    .addPropertyNode("limit").addConstraintViolation();
  isValid = false;
}

// application-reporting.yml:2-6 - Limits
max-rows: 50000
max-interval-days: 92
default-ttl-seconds: 60
```

### NÁPRAVA:

**PR #3: Circuit Breaker & Rate Limit Filter** (⏱ 4h, vlastník: BE)

```diff
# Vytvořit backend/src/main/java/cz/muriel/core/reporting/config/ReportingConfiguration.java
+@Bean
+public CircuitBreakerRegistry reportingCircuitBreakerRegistry() {
+  CircuitBreakerConfig config = CircuitBreakerConfig.custom()
+    .failureRateThreshold(50)
+    .waitDurationInOpenState(Duration.ofSeconds(30))
+    .permittedNumberOfCallsInHalfOpenState(5)
+    .slidingWindowSize(10)
+    .recordExceptions(HttpServerErrorException.class, TimeoutException.class)
+    .build();
+  return CircuitBreakerRegistry.of(config);
+}

+@Bean
+public CircuitBreaker cubeCircuitBreaker(CircuitBreakerRegistry registry) {
+  return registry.circuitBreaker("cube");
+}

# CubeClient.java - použít CB
+@Autowired private CircuitBreaker cubeCircuitBreaker;
+
+public List<Map<String, Object>> executeQuery(CubeQueryRequest request) {
+  return CircuitBreaker.decorateSupplier(cubeCircuitBreaker, () -> {
+    // existing implementation
+  }).get();
+}

# Vytvořit RateLimitFilter.java
+@Component
+public class RateLimitFilter extends OncePerRequestFilter {
+  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
+  
+  @Override
+  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, 
+      FilterChain chain) throws ServletException, IOException {
+    String tenantId = extractTenantId(req);
+    Bucket bucket = buckets.computeIfAbsent(tenantId, this::createBucket);
+    
+    if (bucket.tryConsume(1)) {
+      chain.doFilter(req, res);
+    } else {
+      res.setStatus(429);
+      res.setHeader("Retry-After", "60");
+      res.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
+    }
+  }
+}
```

**Priorita:** 🟡 **P1** (high)

---

## 4️⃣ Guardrails & Security

### STATUS: ☑️ **PARTIAL (70%)**

| Požadavek | Evidence | Náprava |
|-----------|----------|---------|
| **Povinné časové okno** | ✅ `QueryRequestValidator.java:64-68` | - |
| **maxRows** | ✅ `application-reporting.yml:3` (50000) | - |
| **maxIntervalDays** | ✅ `application-reporting.yml:4` (92) | - |
| **Whitelist polí** | ☑️ `MetamodelSpecService.java` | Zpřísnit validaci |
| **RLS v čtení** | ✅ `CubeMapper.java:70-74` (tenant filter) | - |
| **RLS v zápisu** | ❌ Chybí (bulk/inline edit neimplementováno) | Implementovat |
| **Header hardening** | ❌ Chybí security headers | Přidat SecurityConfig |
| **Log redaction** | ☑️ Částečně (LoggingContextFilter) | Rozšířit |

### EVIDENCE:
```java
// QueryRequestValidator.java:64-68 - Required time range
if (query.getTimeRange() == null && !isEntityExemptFromTimeRange(query.getEntity())) {
  context.buildConstraintViolationWithTemplate("Time range is required for this entity")
    .addPropertyNode("timeRange").addConstraintViolation();
  isValid = false;
}
```

### NÁPRAVA:

**PR #4: Security Hardening** (⏱ 4h, vlastník: BE)

```diff
# Vytvořit ReportingSecurityConfig.java
+@Configuration
+public class ReportingSecurityConfig {
+  
+  @Bean
+  public FilterRegistrationBean<ContentTypeFilter> contentTypeFilter() {
+    FilterRegistrationBean<ContentTypeFilter> reg = new FilterRegistrationBean<>();
+    reg.setFilter(new ContentTypeFilter());
+    reg.addUrlPatterns("/api/reports/*");
+    return reg;
+  }
+  
+  @Component
+  public static class ContentTypeFilter extends OncePerRequestFilter {
+    @Override
+    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
+        FilterChain chain) throws ServletException, IOException {
+      
+      // Enforce JSON content-type for POST/PUT
+      if (List.of("POST", "PUT", "PATCH").contains(req.getMethod())) {
+        String contentType = req.getContentType();
+        if (contentType == null || !contentType.startsWith("application/json")) {
+          res.setStatus(415);
+          res.getWriter().write("{\"error\":\"Unsupported Media Type\"}");
+          return;
+        }
+      }
+      
+      // Security headers
+      res.setHeader("X-Content-Type-Options", "nosniff");
+      res.setHeader("X-Frame-Options", "DENY");
+      
+      chain.doFilter(req, res);
+    }
+  }
+}
```

**Priorita:** 🟡 **P1** (high)

---

## 5️⃣ UI pro reporting (FE)

### STATUS: ❌ **FAIL (0%)**

| Požadavek | Evidence | Náprava |
|-----------|----------|---------|
| **ExplorerGrid** | ❌ Neexistuje | Implementovat DataGrid komponenty |
| **PivotViewer** | ❌ Neexistuje (FINOS Perspective) | Integrovat @finos/perspective |
| **ChartPanel** | ❌ Neexistuje (ECharts) | Implementovat s Apache ECharts |
| **Inline edit** | ❌ Chybí | Implementovat cell/row editors |
| **Bulk selection** | ❌ Chybí | Přidat checkbox selection |
| **Drill-down** | ❌ Chybí | Implementovat navigaci |

### EVIDENCE:
```typescript
// frontend/tests/e2e/reports.spec.ts:1-50 - Test existuje ale pro Grafana Scenes
// ❌ Žádné komponenty pro ExplorerGrid/Pivot/Charts
```

### NÁPRAVA:

**PR #5: Frontend Reporting Components** (⏱ 32h, vlastník: FE)

```diff
# 1. Instalace závislostí
+{
+  "dependencies": {
+    "@finos/perspective": "^2.9.0",
+    "@finos/perspective-viewer": "^2.9.0",
+    "@finos/perspective-viewer-datagrid": "^2.9.0",
+    "apache-echarts": "^5.4.3",
+    "@mui/x-data-grid-pro": "^6.18.0"
+  }
+}

# 2. Vytvořit frontend/src/components/reporting/ExplorerGrid.tsx
+import { DataGridPro } from '@mui/x-data-grid-pro';
+
+export function ExplorerGrid({ entity, filters }) {
+  const [rows, setRows] = useState([]);
+  const [loading, setLoading] = useState(false);
+  
+  const handleCellEdit = async (params) => {
+    try {
+      await api.patch(`/api/entities/${entity}/${params.id}`, {
+        [params.field]: params.value,
+        version: params.row.version
+      });
+      // Optimistic update
+    } catch (err) {
+      if (err.status === 409) {
+        toast.error('Conflict: record was updated by another user');
+      }
+    }
+  };
+  
+  return (
+    <DataGridPro
+      rows={rows}
+      loading={loading}
+      checkboxSelection
+      onCellEditCommit={handleCellEdit}
+      pagination
+      paginationMode="server"
+    />
+  );
+}

# 3. Vytvořit frontend/src/components/reporting/PivotViewer.tsx
+import perspective from '@finos/perspective';
+import '@finos/perspective-viewer';
+
+export function PivotViewer({ data }) {
+  const viewerRef = useRef();
+  
+  useEffect(() => {
+    const worker = perspective.worker();
+    const table = worker.table(data);
+    viewerRef.current.load(table);
+  }, [data]);
+  
+  return <perspective-viewer ref={viewerRef} />;
+}

# 4. Vytvořit frontend/src/components/reporting/ChartPanel.tsx
+import * as echarts from 'echarts';
+
+export function ChartPanel({ type, data, onDrillDown }) {
+  const chartRef = useRef();
+  
+  useEffect(() => {
+    const chart = echarts.init(chartRef.current);
+    chart.setOption(getChartOption(type, data));
+    chart.on('click', (params) => {
+      onDrillDown(params.name, params.value);
+    });
+  }, [type, data]);
+  
+  return <div ref={chartRef} style={{ height: 400 }} />;
+}
```

**Priorita:** 🔴 **P0** (blocking)

---

## 6️⃣ Sdílené pohledy (BE)

### STATUS: ✅ **PASS (95%)**

| Požadavek | Evidence | Náprava |
|-----------|----------|---------|
| **DB entita** | ✅ `V1__init.sql:650-673` | - |
| **CRUD API** | ✅ `ReportViewController.java:26-130` | - |
| **RBAC** | ✅ `@PreAuthorize` annotations | - |
| **Validace** | ☑️ Základní | Přidat metamodel validation |

### EVIDENCE:
```sql
-- V1__init.sql:650-673
CREATE TABLE report_view (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL,
  entity VARCHAR(255) NOT NULL,
  scope VARCHAR(20) NOT NULL CHECK (scope IN ('private', 'group', 'tenant', 'global')),
  definition JSONB NOT NULL,
  is_default BOOLEAN DEFAULT false,
  ...
);
```

```java
// ReportViewController.java:75-79 - RBAC
@PostMapping @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'USER')")
public ResponseEntity<ReportView> createView(
  @Valid @RequestBody ReportView view,
  Authentication authentication) { ... }
```

### NÁPRAVA:

**PR #6: View Definition Validation** (⏱ 2h, vlastník: BE)

```diff
# ReportViewController.java
+@Autowired private MetamodelSpecService specService;
+
+private void validateDefinition(String entity, JsonNode definition) {
+  EntitySpec spec = specService.getEntitySpec(entity);
+  
+  // Validate dimensions exist
+  definition.get("dimensions").forEach(dim -> {
+    if (!spec.getDimensions().contains(dim.asText())) {
+      throw new IllegalArgumentException("Invalid dimension: " + dim);
+    }
+  });
+  
+  // Validate measures
+  definition.get("measures").forEach(measure -> {
+    if (!spec.getMeasures().containsKey(measure.get("field").asText())) {
+      throw new IllegalArgumentException("Invalid measure: " + measure);
+    }
+  });
+}
```

**Priorita:** 🟢 **P2** (medium)

---

## 7️⃣ Zápisy: inline edit & bulk

### STATUS: ❌ **FAIL (10%)**

| Požadavek | Evidence | Náprava |
|-----------|----------|---------|
| **Inline edit API** | ❌ Neexistuje | Implementovat PATCH /api/entities/{entity}/{id} |
| **Optimistic locking** | ✅ `report_view.version` column | Rozšířit na entity |
| **Bulk update API** | ❌ Neexistuje | Implementovat POST /api/entities/{entity}/bulk-update |
| **Async job worker** | ❌ Neexistuje | Implementovat @Scheduled worker |
| **DB tabulky** | ✅ `reporting_job`, `reporting_job_event` | - |
| **Audit log** | ✅ `audit_change` table | - |

### EVIDENCE:
```sql
-- V1__init.sql:676-720
CREATE TABLE reporting_job (
  id UUID PRIMARY KEY,
  entity VARCHAR(255) NOT NULL,
  where_json JSONB NOT NULL,
  patch_json JSONB NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  idempotency_key VARCHAR(255),
  ...
);

CREATE TABLE audit_change (
  id UUID PRIMARY KEY,
  tenant_id UUID NOT NULL,
  actor UUID NOT NULL,
  entity VARCHAR(255) NOT NULL,
  entity_id UUID NOT NULL,
  op VARCHAR(10) NOT NULL CHECK (op IN ('INSERT', 'UPDATE', 'DELETE')),
  ...
);
```

### NÁPRAVA:

**PR #7: Inline Edit & Bulk Operations** (⏱ 16h, vlastník: BE)

```diff
# 1. Vytvořit EntityCrudController.java
+@RestController
+@RequestMapping("/api/entities/{entity}")
+public class EntityCrudController {
+  
+  @PatchMapping("/{id}")
+  public ResponseEntity<?> patchEntity(
+      @PathVariable String entity,
+      @PathVariable UUID id,
+      @RequestBody Map<String, Object> patch,
+      @RequestHeader("If-Match") Integer version,
+      Authentication auth) {
+    
+    // 1. Validate editable fields
+    EntitySpec spec = specService.getEntitySpec(entity);
+    patch.keySet().forEach(field -> {
+      if (!spec.getEditableFields().contains(field)) {
+        throw new ForbiddenException("Field not editable: " + field);
+      }
+    });
+    
+    // 2. Apply RLS
+    String tenantId = extractTenantId(auth);
+    
+    // 3. Update with optimistic lock
+    int updated = jooq.update(DSL.table(spec.getTable()))
+      .set(patch)
+      .where(DSL.field("id").eq(id))
+      .and(DSL.field("tenant_id").eq(tenantId))
+      .and(DSL.field("version").eq(version))
+      .execute();
+    
+    if (updated == 0) {
+      throw new ConflictException("Record was updated by another user");
+    }
+    
+    // 4. Audit log
+    auditService.logChange(tenantId, auth.getName(), entity, id, "UPDATE", patch);
+    
+    return ResponseEntity.ok().build();
+  }
+}

# 2. Vytvořit BulkUpdateController.java
+@PostMapping("/{entity}/bulk-update")
+public ResponseEntity<BulkJobResponse> bulkUpdate(
+    @PathVariable String entity,
+    @RequestBody BulkUpdateRequest request,
+    Authentication auth) {
+  
+  // Validate
+  if (request.isDryRun()) {
+    int count = jooq.selectCount()
+      .from(DSL.table(entity))
+      .where(filterParser.parse(request.getWhere()))
+      .fetchOne(0, int.class);
+    return ResponseEntity.ok(BulkJobResponse.dryRun(count));
+  }
+  
+  // Create job
+  ReportingJob job = ReportingJob.builder()
+    .entity(entity)
+    .whereJson(request.getWhere())
+    .patchJson(request.getPatch())
+    .idempotencyKey(request.getIdempotencyKey())
+    .status("PENDING")
+    .build();
+  jobRepository.save(job);
+  
+  return ResponseEntity.accepted()
+    .header("Location", "/api/bulk-jobs/" + job.getId())
+    .body(BulkJobResponse.accepted(job.getId()));
+}

# 3. Vytvořit BulkUpdateWorker.java
+@Scheduled(fixedDelay = 5000)
+@Transactional
+public void processJobs() {
+  List<ReportingJob> pending = jobRepository.findByStatus("PENDING")
+    .stream().limit(5).collect(toList());
+  
+  for (ReportingJob job : pending) {
+    job.setStatus("RUNNING");
+    jobRepository.save(job);
+    
+    try {
+      int affected = processJob(job);
+      job.setStatus("SUCCESS");
+      job.setAffectedRows(affected);
+    } catch (Exception e) {
+      job.setStatus("FAILED");
+      job.setMessage(e.getMessage());
+    }
+    
+    jobRepository.save(job);
+  }
+}
```

**Priorita:** 🔴 **P0** (blocking)

---

## 8️⃣ Testy & CI

### STATUS: ☑️ **PARTIAL (40%)**

| Požadavek | Evidence | Náprava |
|-----------|----------|---------|
| **Profil test** | ✅ `application-test.yml:78` | - |
| **Unit testy** | ✅ 2 testy (ReportingPropertiesTest, FeatureToggleTest) | Rozšířit pokrytí |
| **Integrační testy** | ❌ Chybí | Implementovat IT testy |
| **E2E testy** | ☑️ `reports.spec.ts` (ale jen Grafana) | Přidat reporting E2E |
| **CI workflow** | ❌ Žádný separátní job pro reporting | Přidat do ci.yml |

### EVIDENCE:
```yaml
# backend/src/test/resources/application-test.yml:78
cube:
  base-url: http://localhost:4000
```

```java
// backend/src/test/java/cz/muriel/core/reporting/app/
// - ReportingPropertiesTest.java ✅
// - ReportingFeatureToggleTest.java ✅
```

### NÁPRAVA:

**PR #8: Reporting Tests & CI** (⏱ 12h, vlastník: BE/FE)

```diff
# 1. Vytvořit ReportQueryControllerIT.java
+@SpringBootTest
+@AutoConfigureMockMvc
+@ActiveProfiles("test")
+class ReportQueryControllerIT {
+  
+  @Test
+  void shouldExecuteValidQuery() throws Exception {
+    mockMvc.perform(post("/api/reports/query")
+        .header("Authorization", "Bearer " + mockJwt)
+        .contentType(APPLICATION_JSON)
+        .content("""
+          {
+            "entity": "User",
+            "dimensions": ["status"],
+            "measures": [{"field": "id", "aggregation": "count"}],
+            "limit": 100
+          }
+        """))
+      .andExpect(status().isOk())
+      .andExpect(jsonPath("$.data").isArray());
+  }
+  
+  @Test
+  void shouldReturn429OnRateLimit() throws Exception {
+    // Send 121 requests
+    for (int i = 0; i < 121; i++) {
+      mockMvc.perform(post("/api/reports/query")...);
+    }
+    mockMvc.perform(post("/api/reports/query")...)
+      .andExpect(status().isTooManyRequests())
+      .andExpect(header().exists("Retry-After"));
+  }
+  
+  @Test
+  void shouldEnforceRLS() throws Exception {
+    // Query with tenant A token should not see tenant B data
+  }
+}

# 2. Vytvořit frontend/tests/e2e/reporting.spec.ts
+test.describe('Reporting Module', () => {
+  test('should render ExplorerGrid', async ({ page }) => {
+    await page.goto('/reports/users');
+    await expect(page.locator('[data-testid="explorer-grid"]')).toBeVisible();
+  });
+  
+  test('should perform inline edit', async ({ page }) => {
+    await page.click('[data-row-id="1"] [data-field="status"]');
+    await page.fill('input', 'INACTIVE');
+    await page.press('input', 'Enter');
+    await expect(page.locator('.toast-success')).toBeVisible();
+  });
+  
+  test('should render PivotViewer', async ({ page }) => {
+    await page.goto('/reports/users/pivot');
+    await expect(page.locator('perspective-viewer')).toBeVisible();
+  });
+});

# 3. Rozšířit .github/workflows/ci.yml
+  backend-reporting-tests:
+    name: Backend Reporting Tests
+    runs-on: ubuntu-latest
+    steps:
+      - uses: actions/checkout@v3
+      - uses: actions/setup-java@v3
+        with:
+          java-version: '21'
+      - name: Run reporting tests
+        run: |
+          cd backend
+          ./mvnw test -Dtest=**/reporting/**/*Test.java
+      - uses: actions/upload-artifact@v3
+        with:
+          name: test-results
+          path: backend/target/surefire-reports/
```

**Priorita:** 🟡 **P1** (high)

---

## 9️⃣ Výkon & Provoz

### STATUS: ☑️ **PARTIAL (50%)**

| Požadavek | Evidence | Náprava |
|-----------|----------|---------|
| **Cache (Redis)** | ✅ `application-reporting.yml:8-10` | - |
| **Cache TTL** | ✅ 60s (`application-reporting.yml:5`) | - |
| **Pre-aggregace** | ❌ Chybí (Cube.js neinstalován) | Implementovat v Cube schématech |
| **Metriky** | ☑️ `ReportingMetrics.java` (partial) | Přidat dashboard |
| **Single-flight dedup** | ❌ Chybí | Implementovat |

### EVIDENCE:
```java
// ReportingMetrics.java:30-50 - Partial metrics
public void recordQueryLatency(String tenantId, String entity, long ms, boolean cacheHit) {
  Timer.builder("reporting.query.latency")
    .tag("tenant", tenantId)
    .tag("entity", entity)
    .tag("cache_hit", String.valueOf(cacheHit))
    .register(registry)
    .record(ms, TimeUnit.MILLISECONDS);
}
```

### NÁPRAVA:

**PR #9: Performance Optimizations** (⏱ 8h, vlastník: BE/DevOps)

```diff
# 1. Single-flight deduplication
+@Component
+public class QueryDeduplicator {
+  private final Map<String, CompletableFuture<QueryResponse>> inflight = new ConcurrentHashMap<>();
+  
+  public CompletableFuture<QueryResponse> deduplicate(String fingerprint, 
+      Supplier<QueryResponse> supplier) {
+    return inflight.computeIfAbsent(fingerprint, k -> 
+      CompletableFuture.supplyAsync(supplier)
+        .whenComplete((res, ex) -> inflight.remove(k))
+    );
+  }
+}

# 2. Přidat pre-aggregace do Cube schémat
+// cube/schema/Users.js
+preAggregations: {
+  dailyStatusCounts: {
+    measures: [count],
+    dimensions: [status],
+    timeDimension: createdAt,
+    granularity: 'day',
+    refreshKey: {
+      every: '1 hour',
+      incremental: true,
+      updateWindow: '7 day'
+    }
+  }
+}

# 3. Grafana dashboard (docker/grafana/provisioning/dashboards/reporting.json)
+{
+  "dashboard": {
+    "title": "Reporting BFF Metrics",
+    "panels": [
+      {
+        "title": "Query Latency p95",
+        "targets": [{
+          "expr": "histogram_quantile(0.95, rate(reporting_query_latency_seconds_bucket[5m]))"
+        }]
+      },
+      {
+        "title": "Cache Hit Rate",
+        "targets": [{
+          "expr": "rate(reporting_query_total{cache_hit=\"true\"}[5m]) / rate(reporting_query_total[5m])"
+        }]
+      }
+    ]
+  }
+}
```

**Priorita:** 🟡 **P1** (high)

---

## 🔟 Dokumentace & Runbooky

### STATUS: ✅ **PASS (85%)**

| Požadavek | Evidence | Náprava |
|-----------|----------|---------|
| **REPORTING_README.md** | ✅ Existuje | Aktualizovat stav implementace |
| **REPORTING_OPERATIONS_RUNBOOK.md** | ✅ Existuje (552 řádků) | - |
| **Architektura** | ✅ Diagramy v README | - |
| **Příklady** | ✅ cURL příklady v runbooku | - |
| **Incidenty** | ☑️ Částečně | Rozšířit troubleshooting |

### EVIDENCE:
```markdown
# docs/REPORTING_README.md - 200 lines
# docs/REPORTING_OPERATIONS_RUNBOOK.md - 552 lines
```

### NÁPRAVA:

**PR #10: Documentation Updates** (⏱ 2h, vlastník: Tech Writer/BE Lead)

```diff
# docs/REPORTING_README.md
+## ✅ Hotové (aktualizováno)
+
+### Fáze 3.0-3.7 - KOMPLETNÍ
+- Reporting modul (BFF API)
+- DB schémata
+- Cache + rate-limit
+- Sdílené pohledy
+
+### TODO
+- [ ] Cube.js instalace (PR #1)
+- [ ] UI komponenty (PR #5)
+- [ ] Bulk operations (PR #7)
+- [ ] E2E testy (PR #8)

# docs/REPORTING_OPERATIONS_RUNBOOK.md - rozšířit sekci Troubleshooting
+### Cube.js Down
+
+**Symptoms:** 502 errors from /api/reports/query
+
+**Steps:**
+1. Check circuit breaker state:
+   ```bash
+   curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
+   ```
+2. Check Cube.js logs:
+   ```bash
+   docker logs core-cube
+   ```
+3. Restart Cube.js:
+   ```bash
+   docker restart core-cube
+   ```
+4. Wait for circuit breaker to close (30s)
+
+### Cache Warmup
+
+**When:** After Cube.js schema changes
+
+**Steps:**
+1. Invalidate cache:
+   ```bash
+   curl -X POST http://localhost:8080/api/reports/cache/invalidate
+   ```
+2. Pre-warm common queries:
+   ```bash
+   bash scripts/reporting/cache-warmup.sh
+   ```
```

**Priorita:** 🟢 **P2** (medium)

---

## 📊 Souhrnná tabulka

| Oblast | Status | Priorita | Effort | Vlastník |
|--------|--------|----------|--------|----------|
| 1. Cube.js instalace | ❌ FAIL | 🔴 P0 | 8h | DevOps/BE |
| 2. UI Spec generátor | ❌ FAIL | 🔴 P0 | 6h | BE |
| 3. Circuit breaker | ☑️ PARTIAL | 🟡 P1 | 4h | BE |
| 4. Security hardening | ☑️ PARTIAL | 🟡 P1 | 4h | BE |
| 5. FE komponenty | ❌ FAIL | 🔴 P0 | 32h | FE |
| 6. View validation | ✅ PASS | 🟢 P2 | 2h | BE |
| 7. Inline/Bulk edit | ❌ FAIL | 🔴 P0 | 16h | BE |
| 8. Testy & CI | ☑️ PARTIAL | 🟡 P1 | 12h | BE/FE |
| 9. Performance opt. | ☑️ PARTIAL | 🟡 P1 | 8h | BE/DevOps |
| 10. Dokumentace | ✅ PASS | 🟢 P2 | 2h | Tech Writer |

**Celkem:** 94h (12 MD)

---

## ✅ Checklist

### 1. Semantická vrstva (Cube.js)
- [ ] Cube.js service v docker-compose.yml
- [ ] Cube.js schémata pro klíčové entity (Users, Orders, Tickets)
- [ ] Measures, dimensions, joins definovány
- [ ] Pre-aggregations s refresh key
- [ ] RLS security context v schématech
- [ ] Generator schémat z metamodelu
- [ ] Dokumentace modelu

### 2. Metamodel → UI Spec
- [ ] EntitySpecDTO s dimensions, measures, relations
- [ ] Generátor spec z metamodelu (checksum versioning)
- [ ] GET /api/reports/metadata/{entity}/spec endpoint
- [ ] Validační pravidla v spec
- [ ] Enum values v spec

### 3. BFF Reporting API
- [x] DSL DTO (QueryRequest/Response)
- [x] Mapper DSL → Cube query
- [x] POST /api/reports/query endpoint
- [x] GET /api/reports/metadata/{entity} endpoint
- [x] Redis cache (primary)
- [x] Caffeine cache (fallback)
- [ ] Rate-limit filter (Bucket4j)
- [ ] Circuit breaker (Resilience4j)
- [x] Guardrails (maxRows, maxInterval, timeRange)
- [x] Problem+JSON error handling

### 4. Guardrails & Security
- [x] Povinné časové okno
- [x] maxRows limit (50000)
- [x] maxIntervalDays (92)
- [ ] Whitelist polí (zpřísnit)
- [x] RLS v čtení (tenant filter)
- [ ] RLS v zápisu
- [ ] Header hardening (Content-Type, X-Frame-Options)
- [ ] Log redaction (secrets)

### 5. UI pro reporting
- [ ] ExplorerGrid (DataGrid Pro)
- [ ] Server-side pagination/sort/filter
- [ ] Inline edit (cell/row)
- [ ] Bulk selection (checkboxes)
- [ ] PivotViewer (FINOS Perspective)
- [ ] Export (CSV/XLSX)
- [ ] ChartPanel (ECharts)
- [ ] Drill-down (chart → table → detail)

### 6. Sdílené pohledy
- [x] DB entita report_view
- [x] CRUD API (/api/reports/views)
- [x] RBAC (private/group/tenant/global)
- [ ] Definition validation (metamodel)

### 7. Zápisy
- [ ] PATCH /api/entities/{entity}/{id}
- [ ] Optimistic locking (version)
- [ ] POST /api/entities/{entity}/bulk-update
- [ ] Dry-run mode
- [ ] Async worker (@Scheduled)
- [ ] Job status endpoint
- [x] Audit log (audit_change)

### 8. Testy & CI
- [x] Profil test (application-test.yml)
- [ ] Integrační testy (DSL, cache, rate-limit, RLS)
- [ ] Bulk testy (dry-run, idempotence, rollback)
- [ ] FE E2E (ExplorerGrid, PivotViewer, ChartPanel)
- [ ] CI workflow job pro reporting

### 9. Výkon & Provoz
- [x] Cache TTL (60s)
- [ ] Pre-aggregace v Cube
- [ ] Single-flight deduplication
- [ ] Metriky (latency p95/p99, cache hit rate)
- [ ] Grafana dashboard (reporting BFF)

### 10. Dokumentace
- [x] REPORTING_README.md
- [x] RUNBOOK (architektura, troubleshooting)
- [ ] Aktualizace stavu implementace
- [ ] Rozšířená sekce incidentů

---

## 🎯 Kritické závěry

### ❌ **FAIL** - Blokující položky (P0):
1. **Cube.js instalace** - Bez semantic layer není reporting funkční
2. **UI komponenty** - Bez FE není možné reporty zobrazovat
3. **Inline/Bulk edit** - Zápisy nejsou implementovány
4. **UI Spec generátor** - FE nemá metadata pro dynamické renderování

### ☑️ **PARTIAL** - Vyžaduje dokončení (P1):
5. **Circuit breaker** - Bez CB je systém zranitelný vůči Cube.js výpadkům
6. **Rate limit filter** - Konfigurace existuje, chybí filter
7. **Testy** - Pokrytí je nedostatečné (40%)
8. **Security hardening** - Chybí header validation

### ✅ **PASS** - Funkční komponenty:
- BFF DSL API (QueryRequest/Response)
- DB schémata (report_view, reporting_job, audit_change)
- Cache mechanizmus (Redis + Caffeine)
- Sdílené pohledy (CRUD + RBAC)
- Dokumentace (README + RUNBOOK)

---

## 📝 Doporučené priority PRs

### Fáze 1 (Sprint 1): Infrastruktura ⏱ 2 týdny
1. **PR #1**: Cube.js Infrastructure (8h)
2. **PR #2**: UI Spec Generator (6h)
3. **PR #3**: Circuit Breaker & Rate Limit (4h)

### Fáze 2 (Sprint 2-3): Uživatelské rozhraní ⏱ 3 týdny
4. **PR #5**: Frontend Reporting Components (32h)
5. **PR #8** (část 1): E2E testy pro FE (6h)

### Fáze 3 (Sprint 4): Zápisy & Audit ⏱ 2 týdny
6. **PR #7**: Inline Edit & Bulk Operations (16h)
7. **PR #4**: Security Hardening (4h)

### Fáze 4 (Sprint 5): Kvalita & Optimalizace ⏱ 1 týden
8. **PR #8** (část 2): BE integrační testy (6h)
9. **PR #9**: Performance Optimizations (8h)
10. **PR #6**: View Validation (2h)
11. **PR #10**: Documentation Updates (2h)

---

## 📧 Kontakty pro eskalaci

- **Backend Lead**: (bulk operations, circuit breaker)
- **Frontend Lead**: (UI komponenty)
- **DevOps**: (Cube.js deployment)
- **Security**: (hardening, RLS audit)

---

**Konec auditu** - Dokument vygenerován 11. října 2025
