# 📋 TODO AUDIT - Reporting Module

> **Generováno:** 11. října 2025  
> **Audit SHA:** f79f51341fb253c0e64b3b09baf5bdce22e3389c  
> **Status:** 65% implementováno, 35% zbývá

---

## 🔴 KRITICKÉ (P0) - Blokující production deploy

### TODO-R01: Cube.js Infrastructure Setup
- **Priorita:** 🔴 P0
- **Effort:** 8h
- **Vlastník:** DevOps + BE
- **Blocking:** Celý reporting modul

**Kroky:**
1. Přidat Cube.js service do `docker/docker-compose.yml`:
   ```yaml
   cube:
     image: cubejs/cube:latest
     container_name: core-cube
     ports: ["4000:4000"]
     environment:
       CUBEJS_DB_TYPE: postgres
       CUBEJS_DB_HOST: db
       CUBEJS_DB_NAME: ${DB_INTERNAL_NAME}
       CUBEJS_API_SECRET: ${CUBE_API_TOKEN}
     volumes: ["./cube/schema:/cube/conf/schema"]
     networks: [core-net]
   ```

2. Vytvořit `cube/schema/` adresář

3. Vytvořit základní Cube.js schémata:
   - `cube/schema/Users.js`
   - `cube/schema/Tenants.js`
   - `cube/schema/Groups.js`

4. Implementovat RLS v Cube schématech:
   ```javascript
   sql: `SELECT * FROM users_directory 
         WHERE tenant_id = ${SECURITY_CONTEXT.tenantId.filter('tenant_id')}`
   ```

5. Definovat pre-aggregations:
   ```javascript
   preAggregations: {
     statusRollup: {
       measures: [count],
       dimensions: [status],
       timeDimension: createdAt,
       granularity: 'day',
       refreshKey: { every: '1 hour' }
     }
   }
   ```

6. Přidat `CUBE_API_TOKEN` do `.env`

7. Testovat connection: `curl http://localhost:4000/cubejs-api/v1/meta`

**DoD:**
- [ ] Cube.js service běží v Docker
- [ ] Min. 3 entity schémata vytvořena
- [ ] RLS funguje (tenant isolation)
- [ ] Pre-aggregace definovány
- [ ] Dokumentace v `docs/CUBE_SETUP.md`

---

### TODO-R02: Metamodel UI Spec Generator
- **Priorita:** 🔴 P0
- **Effort:** 6h
- **Vlastník:** BE
- **Blocking:** Frontend dynamické renderování

**Kroky:**
1. Vytvořit `backend/src/main/java/cz/muriel/core/reporting/support/MetamodelSpecExporter.java`:
   ```java
   @Component
   public class MetamodelSpecExporter {
     public EntitySpecDTO exportEntitySpec(String entity) {
       EntitySchema schema = registry.getEntitySchema(entity);
       return EntitySpecDTO.builder()
         .entity(entity)
         .specVersion(computeChecksum(schema))
         .dimensions(extractDimensions(schema))
         .measures(extractMeasures(schema))
         .relations(extractRelations(schema))
         .editableFields(extractEditableFields(schema))
         .validations(extractValidations(schema))
         .defaultView(extractDefaultView(schema))
         .drilldowns(extractDrilldowns(schema))
         .enums(extractEnums(schema))
         .build();
     }
   }
   ```

2. Implementovat helper metody:
   - `extractDimensions()` - všechna pole kromě JSONB/BLOB
   - `extractMeasures()` - numeric fieldy s count/sum/avg/min/max
   - `extractRelations()` - manyToOne/oneToMany
   - `extractEditableFields()` - !pk && !generated && !readOnly
   - `extractValidations()` - required, pattern, min/max
   - `extractEnums()` - z metamodel enum definic

3. Přidat checksum versioning:
   ```java
   private String computeChecksum(EntitySchema schema) {
     String json = objectMapper.writeValueAsString(schema);
     return DigestUtils.sha256Hex(json);
   }
   ```

4. Vytvořit endpoint v `ReportQueryController`:
   ```java
   @GetMapping("/api/reports/metadata/{entity}/spec")
   public ResponseEntity<EntitySpecDTO> getFullSpec(
       @PathVariable String entity) {
     return ResponseEntity.ok(specExporter.exportEntitySpec(entity));
   }
   ```

5. Unit testy:
   ```java
   @Test
   void shouldExportCompleteSpec() {
     EntitySpecDTO spec = exporter.exportEntitySpec("User");
     assertThat(spec.getDimensions()).contains("email", "status");
     assertThat(spec.getMeasures()).containsKey("count");
     assertThat(spec.getEditableFields()).doesNotContain("id", "created_at");
   }
   ```

**DoD:**
- [ ] `MetamodelSpecExporter` implementován
- [ ] Endpoint `/api/reports/metadata/{entity}/spec` funguje
- [ ] Spec obsahuje dimensions, measures, relations, validations, enums
- [ ] Checksum versioning funkční
- [ ] Unit testy 100% coverage
- [ ] Dokumentace v JavaDoc

---

### TODO-R03: Frontend Reporting Components
- **Priorita:** 🔴 P0
- **Effort:** 32h (4 MD)
- **Vlastník:** FE
- **Blocking:** Reporting UI

**Kroky:**

#### 3.1. Dependencies (2h)
```bash
cd frontend
npm install --save \
  @finos/perspective@^2.9.0 \
  @finos/perspective-viewer@^2.9.0 \
  @finos/perspective-viewer-datagrid@^2.9.0 \
  apache-echarts@^5.4.3 \
  @mui/x-data-grid-pro@^6.18.0
```

#### 3.2. ExplorerGrid Component (12h)
Vytvořit `frontend/src/components/reporting/ExplorerGrid.tsx`:

```typescript
import { DataGridPro, GridColDef, GridRowsProp } from '@mui/x-data-grid-pro';
import { useState, useEffect } from 'react';
import { reportingApi } from '@/services/api';

interface ExplorerGridProps {
  entity: string;
  filters?: Record<string, any>;
  onRowClick?: (row: any) => void;
}

export function ExplorerGrid({ entity, filters, onRowClick }: ExplorerGridProps) {
  const [rows, setRows] = useState<GridRowsProp>([]);
  const [columns, setColumns] = useState<GridColDef[]>([]);
  const [loading, setLoading] = useState(false);
  const [paginationModel, setPaginationModel] = useState({ page: 0, pageSize: 25 });
  const [rowCount, setRowCount] = useState(0);
  const [selectedRows, setSelectedRows] = useState<string[]>([]);

  // Fetch data
  useEffect(() => {
    async function loadData() {
      setLoading(true);
      try {
        const response = await reportingApi.query({
          entity,
          filters,
          limit: paginationModel.pageSize,
          offset: paginationModel.page * paginationModel.pageSize
        });
        setRows(response.data);
        setRowCount(response.totalCount);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [entity, filters, paginationModel]);

  // Fetch columns from spec
  useEffect(() => {
    async function loadSpec() {
      const spec = await reportingApi.getEntitySpec(entity);
      const cols: GridColDef[] = spec.dimensions.map(dim => ({
        field: dim.name,
        headerName: dim.label,
        editable: spec.editableFields.includes(dim.name),
        type: dim.type === 'timestamp' ? 'dateTime' : 'string'
      }));
      setColumns(cols);
    }
    loadSpec();
  }, [entity]);

  // Handle cell edit
  const handleCellEdit = async (params: any) => {
    try {
      await reportingApi.patchEntity(entity, params.id, {
        [params.field]: params.value
      }, params.row.version);
      
      // Optimistic update
      setRows(prev => prev.map(row => 
        row.id === params.id 
          ? { ...row, [params.field]: params.value, version: params.row.version + 1 }
          : row
      ));
      
      toast.success('Updated successfully');
    } catch (err: any) {
      if (err.status === 409) {
        toast.error('Conflict: record was updated by another user');
      } else {
        toast.error('Update failed');
      }
      // Revert optimistic update
      throw err;
    }
  };

  // Handle bulk action
  const handleBulkAction = async (action: string) => {
    if (selectedRows.length === 0) {
      toast.warning('No rows selected');
      return;
    }

    const confirmed = await confirm(
      `Apply ${action} to ${selectedRows.length} rows?`
    );
    if (!confirmed) return;

    try {
      const jobId = await reportingApi.bulkUpdate(entity, {
        where: { id: { in: selectedRows } },
        patch: { status: action },
        idempotencyKey: uuid()
      });
      
      toast.success(`Bulk job ${jobId} started`);
    } catch (err) {
      toast.error('Bulk update failed');
    }
  };

  return (
    <Box>
      {/* Bulk action toolbar */}
      {selectedRows.length > 0 && (
        <Toolbar>
          <Typography>{selectedRows.length} selected</Typography>
          <Button onClick={() => handleBulkAction('ACTIVE')}>Activate</Button>
          <Button onClick={() => handleBulkAction('INACTIVE')}>Deactivate</Button>
        </Toolbar>
      )}

      {/* Data grid */}
      <DataGridPro
        rows={rows}
        columns={columns}
        loading={loading}
        checkboxSelection
        rowSelectionModel={selectedRows}
        onRowSelectionModelChange={setSelectedRows}
        paginationMode="server"
        paginationModel={paginationModel}
        onPaginationModelChange={setPaginationModel}
        rowCount={rowCount}
        onCellEditCommit={handleCellEdit}
        onRowClick={(params) => onRowClick?.(params.row)}
      />
    </Box>
  );
}
```

#### 3.3. PivotViewer Component (8h)
Vytvořit `frontend/src/components/reporting/PivotViewer.tsx`:

```typescript
import { useEffect, useRef } from 'react';
import perspective from '@finos/perspective';
import '@finos/perspective-viewer';
import '@finos/perspective-viewer-datagrid';

interface PivotViewerProps {
  entity: string;
  data: any[];
}

export function PivotViewer({ entity, data }: PivotViewerProps) {
  const viewerRef = useRef<any>();

  useEffect(() => {
    async function loadPerspective() {
      const worker = perspective.worker();
      const table = await worker.table(data);
      await viewerRef.current.load(table);
      
      // Default config
      await viewerRef.current.restore({
        plugin: 'Datagrid',
        group_by: ['status'],
        aggregates: { id: 'count' }
      });
    }
    
    if (data.length > 0) {
      loadPerspective();
    }
  }, [data]);

  const handleExport = async (format: 'csv' | 'xlsx') => {
    const csv = await viewerRef.current.copy();
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${entity}_export.${format}`;
    a.click();
  };

  return (
    <Box>
      <Toolbar>
        <Button onClick={() => handleExport('csv')}>Export CSV</Button>
        <Button onClick={() => handleExport('xlsx')}>Export XLSX</Button>
      </Toolbar>
      <perspective-viewer ref={viewerRef} style={{ height: 600 }} />
    </Box>
  );
}
```

#### 3.4. ChartPanel Component (8h)
Vytvořit `frontend/src/components/reporting/ChartPanel.tsx`:

```typescript
import { useEffect, useRef } from 'react';
import * as echarts from 'echarts';

interface ChartPanelProps {
  type: 'bar' | 'line' | 'pie';
  data: any[];
  xField: string;
  yField: string;
  onDrillDown?: (value: any) => void;
}

export function ChartPanel({ type, data, xField, yField, onDrillDown }: ChartPanelProps) {
  const chartRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!chartRef.current) return;

    const chart = echarts.init(chartRef.current);

    const option = {
      title: { text: `${yField} by ${xField}` },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: data.map(d => d[xField])
      },
      yAxis: { type: 'value' },
      series: [{
        name: yField,
        type,
        data: data.map(d => d[yField])
      }]
    };

    chart.setOption(option);

    chart.on('click', (params) => {
      onDrillDown?.({
        [xField]: params.name,
        [yField]: params.value
      });
    });

    return () => chart.dispose();
  }, [type, data, xField, yField]);

  return <div ref={chartRef} style={{ width: '100%', height: 400 }} />;
}
```

#### 3.5. Integration (2h)
Vytvořit `frontend/src/pages/Reporting.tsx`:

```typescript
export function ReportingPage() {
  const [view, setView] = useState<'table' | 'pivot' | 'chart'>('table');
  const [entity, setEntity] = useState('User');

  return (
    <Box>
      <Tabs value={view} onChange={(e, v) => setView(v)}>
        <Tab value="table" label="Table" />
        <Tab value="pivot" label="Pivot" />
        <Tab value="chart" label="Chart" />
      </Tabs>

      {view === 'table' && <ExplorerGrid entity={entity} />}
      {view === 'pivot' && <PivotViewer entity={entity} />}
      {view === 'chart' && <ChartPanel entity={entity} />}
    </Box>
  );
}
```

**DoD:**
- [ ] ExplorerGrid s server-side pagination/sort/filter
- [ ] Inline edit s optimistic locking
- [ ] Bulk selection a actions
- [ ] PivotViewer s FINOS Perspective
- [ ] Export CSV/XLSX
- [ ] ChartPanel s ECharts
- [ ] Drill-down navigace
- [ ] E2E testy pro všechny komponenty
- [ ] Storybook stories

---

### TODO-R04: Inline Edit & Bulk Operations API
- **Priorita:** 🔴 P0
- **Effort:** 16h (2 MD)
- **Vlastník:** BE
- **Blocking:** Zápisy do reportů

**Kroky:**

#### 4.1. Entity CRUD Controller (6h)
Vytvořit `backend/src/main/java/cz/muriel/core/reporting/api/EntityCrudController.java`:

```java
@RestController
@RequestMapping("/api/entities/{entity}")
@RequiredArgsConstructor
public class EntityCrudController {
  
  private final DSLContext jooq;
  private final MetamodelRegistry registry;
  private final CubeSecurityContext securityContext;
  private final AuditService auditService;

  @PatchMapping("/{id}")
  public ResponseEntity<?> patchEntity(
      @PathVariable String entity,
      @PathVariable UUID id,
      @RequestBody Map<String, Object> patch,
      @RequestHeader("If-Match") Integer version,
      Authentication auth) {
    
    // 1. Load spec
    EntitySchema schema = registry.getEntitySchema(entity);
    
    // 2. Validate editable fields
    patch.keySet().forEach(field -> {
      FieldDef fieldDef = schema.getFields().stream()
        .filter(f -> f.getName().equals(field))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown field: " + field));
      
      if (Boolean.TRUE.equals(fieldDef.getPk()) || 
          Boolean.TRUE.equals(fieldDef.getGenerated()) ||
          Boolean.TRUE.equals(fieldDef.getReadOnly())) {
        throw new ForbiddenException("Field not editable: " + field);
      }
    });
    
    // 3. Apply RLS
    String tenantId = securityContext.extractTenantId(auth);
    
    // 4. Update with optimistic lock
    UpdateSetMoreStep<?> update = jooq.update(DSL.table(schema.getTable()));
    patch.forEach((k, v) -> update.set(DSL.field(k), v));
    
    int affected = update
      .where(DSL.field("id").eq(id))
      .and(DSL.field("tenant_id").eq(UUID.fromString(tenantId)))
      .and(DSL.field("version").eq(version))
      .execute();
    
    if (affected == 0) {
      return ResponseEntity.status(409)
        .body(ProblemDetail.forStatusAndDetail(
          HttpStatus.CONFLICT,
          "Record was updated by another user (version mismatch)"
        ));
    }
    
    // 5. Audit log
    auditService.logChange(
      UUID.fromString(tenantId),
      UUID.fromString(securityContext.extractUserId(auth)),
      entity,
      id,
      "UPDATE",
      objectMapper.valueToTree(patch)
    );
    
    // 6. Invalidate cache
    cacheManager.getCache("reportQueryCache").evict(entity);
    
    return ResponseEntity.ok().build();
  }
}
```

#### 4.2. Bulk Update Controller (4h)
Přidat do stejného souboru:

```java
@PostMapping("/{entity}/bulk-update")
public ResponseEntity<BulkJobResponse> bulkUpdate(
    @PathVariable String entity,
    @Valid @RequestBody BulkUpdateRequest request,
    Authentication auth) {
  
  String tenantId = securityContext.extractTenantId(auth);
  String userId = securityContext.extractUserId(auth);
  
  // Validate patch fields
  EntitySchema schema = registry.getEntitySchema(entity);
  validateEditableFields(schema, request.getPatch());
  
  // Dry run?
  if (Boolean.TRUE.equals(request.getDryRun())) {
    Condition where = filterParser.parse(request.getWhere())
      .and(DSL.field("tenant_id").eq(UUID.fromString(tenantId)));
    
    int count = jooq.selectCount()
      .from(DSL.table(schema.getTable()))
      .where(where)
      .fetchOne(0, int.class);
    
    List<UUID> sampleIds = jooq.select(DSL.field("id", UUID.class))
      .from(DSL.table(schema.getTable()))
      .where(where)
      .limit(10)
      .fetch(0, UUID.class);
    
    return ResponseEntity.ok(BulkJobResponse.builder()
      .dryRun(true)
      .estimatedRows(count)
      .sampleIds(sampleIds)
      .build());
  }
  
  // Check idempotency
  if (request.getIdempotencyKey() != null) {
    Optional<ReportingJob> existing = jobRepository
      .findByIdempotencyKey(request.getIdempotencyKey());
    if (existing.isPresent()) {
      return ResponseEntity.ok(BulkJobResponse.fromJob(existing.get()));
    }
  }
  
  // Create job
  ReportingJob job = ReportingJob.builder()
    .tenantId(UUID.fromString(tenantId))
    .createdBy(UUID.fromString(userId))
    .entity(entity)
    .whereJson(objectMapper.valueToTree(request.getWhere()))
    .patchJson(objectMapper.valueToTree(request.getPatch()))
    .status(JobStatus.PENDING)
    .idempotencyKey(request.getIdempotencyKey())
    .build();
  
  jobRepository.save(job);
  
  return ResponseEntity.accepted()
    .header("Location", "/api/bulk-jobs/" + job.getId())
    .body(BulkJobResponse.fromJob(job));
}
```

#### 4.3. Bulk Worker (4h)
Vytvořit `backend/src/main/java/cz/muriel/core/reporting/jobs/BulkUpdateWorker.java`:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class BulkUpdateWorker {
  
  private final ReportingJobRepository jobRepository;
  private final ReportingJobEventRepository eventRepository;
  private final DSLContext jooq;
  private final MetamodelRegistry registry;
  private final AuditService auditService;
  private final ReportingProperties properties;

  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void processJobs() {
    List<ReportingJob> pending = jobRepository
      .findByStatus(JobStatus.PENDING)
      .stream()
      .limit(properties.getBulk().getQueueConcurrency())
      .collect(toList());
    
    for (ReportingJob job : pending) {
      processJob(job);
    }
  }

  private void processJob(ReportingJob job) {
    try {
      job.setStatus(JobStatus.RUNNING);
      job.setStartedAt(Instant.now());
      jobRepository.save(job);
      
      logEvent(job, "INFO", "Job started");
      
      EntitySchema schema = registry.getEntitySchema(job.getEntity());
      Map<String, Object> patch = objectMapper.convertValue(
        job.getPatchJson(), new TypeReference<>() {}
      );
      
      Condition where = filterParser.parse(
        objectMapper.convertValue(job.getWhereJson(), new TypeReference<>() {})
      ).and(DSL.field("tenant_id").eq(job.getTenantId()));
      
      // Count total
      int total = jooq.selectCount()
        .from(DSL.table(schema.getTable()))
        .where(where)
        .fetchOne(0, int.class);
      
      if (total > properties.getBulk().getMaxAffectRows()) {
        throw new IllegalArgumentException(
          "Too many rows affected: " + total + 
          " (max: " + properties.getBulk().getMaxAffectRows() + ")"
        );
      }
      
      job.setTotalRows(total);
      logEvent(job, "INFO", "Processing " + total + " rows");
      
      // Process in chunks
      int chunkSize = properties.getBulk().getChunkSize();
      int affected = 0;
      
      for (int offset = 0; offset < total; offset += chunkSize) {
        List<UUID> ids = jooq.select(DSL.field("id", UUID.class))
          .from(DSL.table(schema.getTable()))
          .where(where)
          .limit(chunkSize)
          .offset(offset)
          .fetch(0, UUID.class);
        
        UpdateSetMoreStep<?> update = jooq.update(DSL.table(schema.getTable()));
        patch.forEach((k, v) -> update.set(DSL.field(k), v));
        
        int chunkAffected = update
          .where(DSL.field("id").in(ids))
          .execute();
        
        affected += chunkAffected;
        
        // Audit log
        ids.forEach(id -> auditService.logChange(
          job.getTenantId(),
          job.getCreatedBy(),
          job.getEntity(),
          id,
          "UPDATE",
          job.getPatchJson()
        ));
        
        logEvent(job, "INFO", 
          String.format("Processed chunk %d/%d (%d rows)", 
            offset / chunkSize + 1, 
            (total + chunkSize - 1) / chunkSize,
            chunkAffected
          )
        );
      }
      
      job.setStatus(JobStatus.SUCCESS);
      job.setAffectedRows(affected);
      job.setCompletedAt(Instant.now());
      
      logEvent(job, "INFO", "Job completed successfully");
      
    } catch (Exception e) {
      log.error("Bulk job failed: {}", job.getId(), e);
      job.setStatus(JobStatus.FAILED);
      job.setMessage(e.getMessage());
      job.setCompletedAt(Instant.now());
      logEvent(job, "ERROR", "Job failed: " + e.getMessage());
    } finally {
      jobRepository.save(job);
    }
  }

  private void logEvent(ReportingJob job, String level, String message) {
    ReportingJobEvent event = ReportingJobEvent.builder()
      .jobId(job.getId())
      .level(level)
      .message(message)
      .build();
    eventRepository.save(event);
  }
}
```

#### 4.4. Job Status Endpoint (2h)
```java
@GetMapping("/api/bulk-jobs/{id}")
public ResponseEntity<BulkJobResponse> getJobStatus(@PathVariable UUID id) {
  ReportingJob job = jobRepository.findById(id)
    .orElseThrow(() -> new NotFoundException("Job not found"));
  
  List<ReportingJobEvent> events = eventRepository.findByJobId(id);
  
  return ResponseEntity.ok(BulkJobResponse.builder()
    .jobId(job.getId())
    .status(job.getStatus())
    .totalRows(job.getTotalRows())
    .affectedRows(job.getAffectedRows())
    .progress(calculateProgress(job))
    .events(events)
    .build());
}

@PostMapping("/api/bulk-jobs/{id}/cancel")
public ResponseEntity<?> cancelJob(@PathVariable UUID id) {
  ReportingJob job = jobRepository.findById(id)
    .orElseThrow(() -> new NotFoundException("Job not found"));
  
  if (job.getStatus() == JobStatus.RUNNING) {
    job.setStatus(JobStatus.CANCELLED);
    jobRepository.save(job);
  }
  
  return ResponseEntity.ok().build();
}
```

**DoD:**
- [ ] PATCH /api/entities/{entity}/{id} implementováno
- [ ] Optimistic locking (If-Match header)
- [ ] POST /api/entities/{entity}/bulk-update implementováno
- [ ] Dry-run mode funguje
- [ ] Async worker zpracovává joby
- [ ] Chunkování (1000 rows/chunk)
- [ ] Idempotence (idempotencyKey)
- [ ] GET /api/bulk-jobs/{id} status endpoint
- [ ] POST /api/bulk-jobs/{id}/cancel
- [ ] Audit log pro všechny změny
- [ ] Unit + integration testy
- [ ] Dokumentace v OpenAPI

---

## 🟡 VYSOKÉ (P1) - Stabilita a kvalita

### TODO-R05: Circuit Breaker & Rate Limit Filter
- **Priorita:** 🟡 P1
- **Effort:** 4h
- **Vlastník:** BE

**Kroky:**
1. Přidat Resilience4j circuit breaker do `ReportingConfiguration`:
   ```java
   @Bean
   public CircuitBreakerRegistry reportingCircuitBreakerRegistry() {
     CircuitBreakerConfig config = CircuitBreakerConfig.custom()
       .failureRateThreshold(50)
       .waitDurationInOpenState(Duration.ofSeconds(30))
       .slidingWindowSize(10)
       .build();
     return CircuitBreakerRegistry.of(config);
   }
   
   @Bean
   public CircuitBreaker cubeCircuitBreaker(CircuitBreakerRegistry registry) {
     return registry.circuitBreaker("cube");
   }
   ```

2. Upravit `CubeClient.executeQuery()`:
   ```java
   public List<Map<String, Object>> executeQuery(CubeQueryRequest request) {
     return CircuitBreaker.decorateSupplier(cubeCircuitBreaker, () -> {
       // existing implementation
     }).get();
   }
   ```

3. Vytvořit `RateLimitFilter.java`:
   ```java
   @Component
   public class RateLimitFilter extends OncePerRequestFilter {
     private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
     private final ReportingProperties properties;
     
     @Override
     protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
         FilterChain chain) throws ServletException, IOException {
       
       if (!req.getRequestURI().startsWith("/api/reports/")) {
         chain.doFilter(req, res);
         return;
       }
       
       String tenantId = extractTenantId(req);
       Bucket bucket = buckets.computeIfAbsent(tenantId, this::createBucket);
       
       if (bucket.tryConsume(1)) {
         chain.doFilter(req, res);
       } else {
         res.setStatus(429);
         res.setHeader("Retry-After", "60");
         res.setContentType("application/json");
         res.getWriter().write("""
           {
             "error": "Too Many Requests",
             "message": "Rate limit exceeded. Max %d requests per minute per tenant.",
             "retryAfter": 60
           }
           """.formatted(properties.getRateLimit().getPerTenantPerMin()));
       }
     }
     
     private Bucket createBucket(String tenantId) {
       Bandwidth limit = Bandwidth.builder()
         .capacity(properties.getRateLimit().getPerTenantPerMin())
         .refillIntervally(properties.getRateLimit().getPerTenantPerMin(), 
                          Duration.ofMinutes(1))
         .build();
       return Bucket.builder().addLimit(limit).build();
     }
   }
   ```

4. Testy:
   ```java
   @Test
   void shouldOpenCircuitBreakerAfter5Failures() {
     // Mock 5 consecutive failures
     for (int i = 0; i < 5; i++) {
       assertThrows(Exception.class, () -> cubeClient.executeQuery(request));
     }
     
     // Circuit should be open
     assertThat(cubeCircuitBreaker.getState()).isEqualTo(State.OPEN);
   }
   
   @Test
   void shouldReturn429OnRateLimitExceeded() throws Exception {
     for (int i = 0; i < 120; i++) {
       mockMvc.perform(post("/api/reports/query")...);
     }
     
     mockMvc.perform(post("/api/reports/query")...)
       .andExpect(status().isTooManyRequests())
       .andExpect(header().string("Retry-After", "60"));
   }
   ```

**DoD:**
- [ ] Circuit breaker implementován
- [ ] CB se otevře po 50% failure rate
- [ ] Half-open state po 30s
- [ ] Rate limit filter implementován
- [ ] 429 s Retry-After header
- [ ] Metriky pro CB state
- [ ] Unit + integration testy
- [ ] Dokumentace v runbooku

---

### TODO-R06: Security Hardening
- **Priorita:** 🟡 P1
- **Effort:** 4h
- **Vlastník:** BE

**Kroky:**
1. Vytvořit `ReportingSecurityConfig.java`:
   ```java
   @Configuration
   public class ReportingSecurityConfig {
     
     @Bean
     public FilterRegistrationBean<ContentTypeFilter> contentTypeFilter() {
       FilterRegistrationBean<ContentTypeFilter> reg = new FilterRegistrationBean<>();
       reg.setFilter(new ContentTypeFilter());
       reg.addUrlPatterns("/api/reports/*", "/api/entities/*");
       reg.setOrder(1);
       return reg;
     }
     
     @Component
     public static class ContentTypeFilter extends OncePerRequestFilter {
       @Override
       protected void doFilterInternal(HttpServletRequest req, 
           HttpServletResponse res, FilterChain chain) 
           throws ServletException, IOException {
         
         // Enforce JSON for POST/PUT/PATCH
         if (List.of("POST", "PUT", "PATCH").contains(req.getMethod())) {
           String contentType = req.getContentType();
           if (contentType == null || !contentType.startsWith("application/json")) {
             res.setStatus(415);
             res.setContentType("application/problem+json");
             res.getWriter().write("""
               {
                 "type": "about:blank",
                 "title": "Unsupported Media Type",
                 "status": 415,
                 "detail": "Content-Type must be application/json"
               }
               """);
             return;
           }
         }
         
         // Security headers
         res.setHeader("X-Content-Type-Options", "nosniff");
         res.setHeader("X-Frame-Options", "DENY");
         res.setHeader("X-XSS-Protection", "1; mode=block");
         
         // Remove sensitive headers from client
         if (req.getHeader("X-Cube-API-Token") != null) {
           // Log security violation
           log.warn("Client sent X-Cube-API-Token header - blocked");
         }
         
         chain.doFilter(req, res);
       }
     }
   }
   ```

2. Zpřísnit field whitelist validaci v `MetamodelSpecService`:
   ```java
   public void validateFieldAccess(String entity, String field, 
       Authentication auth) {
     EntitySchema schema = registry.getEntitySchema(entity);
     FieldDef fieldDef = schema.getFields().stream()
       .filter(f -> f.getName().equals(field))
       .findFirst()
       .orElseThrow(() -> new IllegalArgumentException("Unknown field: " + field));
     
     // Check if field is admin-only
     if (Boolean.TRUE.equals(fieldDef.getAdminOnly()) && 
         !hasRole(auth, "CORE_ROLE_ADMIN")) {
       throw new ForbiddenException("Field requires admin role: " + field);
     }
     
     // Check if field is sensitive
     if (Boolean.TRUE.equals(fieldDef.getSensitive()) && 
         !hasPermission(auth, entity, "read_sensitive")) {
       throw new ForbiddenException("Field is sensitive: " + field);
     }
   }
   ```

3. Log redaction v `LoggingContextFilter`:
   ```java
   private static final Pattern SECRET_PATTERN = Pattern.compile(
     "(api[_-]?key|token|secret|password)\\s*[=:]\\s*(['\"]?)([^'\"\\s]+)\\2",
     Pattern.CASE_INSENSITIVE
   );
   
   private String redactSecrets(String message) {
     return SECRET_PATTERN.matcher(message)
       .replaceAll("$1=$2***REDACTED***$2");
   }
   ```

**DoD:**
- [ ] Content-Type enforcement
- [ ] Security headers (X-Frame-Options, X-Content-Type-Options)
- [ ] Client header blocking (X-Cube-API-Token, etc.)
- [ ] Field-level RBAC (adminOnly, sensitive)
- [ ] Log redaction (secrets, tokens)
- [ ] Security tests (header injection, CSRF)
- [ ] Penetration test report
- [ ] Security audit sign-off

---

### TODO-R07: Reporting Integration Tests & CI
- **Priorita:** 🟡 P1
- **Effort:** 12h
- **Vlastník:** BE (8h) + FE (4h)

**Backend IT testy (8h):**

Vytvořit `backend/src/test/java/cz/muriel/core/reporting/api/ReportQueryControllerIT.java`:

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ReportQueryControllerIT {
  
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
  
  @Container
  static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
    .withExposedPorts(6379);
  
  @BeforeEach
  void setup() {
    // Seed test data
  }
  
  @Test
  void shouldExecuteValidQueryAndReturnData() throws Exception {
    QueryRequest request = QueryRequest.builder()
      .entity("User")
      .dimensions(List.of("status"))
      .measures(List.of(new Measure("id", "count")))
      .timeRange(new TimeRange(
        Instant.now().minus(7, ChronoUnit.DAYS),
        Instant.now()
      ))
      .limit(100)
      .build();
    
    mockMvc.perform(post("/api/reports/query")
        .header("Authorization", "Bearer " + mockJwtToken())
        .contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data").isArray())
      .andExpect(jsonPath("$.metadata.rowCount").isNumber())
      .andExpect(header().string("X-Cache", anyOf(is("HIT"), is("MISS"))));
  }
  
  @Test
  void shouldCacheQueryResults() throws Exception {
    // First request - MISS
    MvcResult result1 = mockMvc.perform(post("/api/reports/query")...)
      .andExpect(header().string("X-Cache", "MISS"))
      .andReturn();
    
    String fingerprint1 = result1.getResponse().getHeader("X-Query-Fingerprint");
    
    // Second request - HIT
    MvcResult result2 = mockMvc.perform(post("/api/reports/query")...)
      .andExpect(header().string("X-Cache", "HIT"))
      .andReturn();
    
    String fingerprint2 = result2.getResponse().getHeader("X-Query-Fingerprint");
    
    assertThat(fingerprint1).isEqualTo(fingerprint2);
  }
  
  @Test
  void shouldReturn429OnRateLimitExceeded() throws Exception {
    // Send 121 requests (limit is 120/min)
    for (int i = 0; i < 121; i++) {
      MvcResult result = mockMvc.perform(post("/api/reports/query")...)
        .andReturn();
      
      if (i < 120) {
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
      } else {
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(result.getResponse().getHeader("Retry-After")).isEqualTo("60");
      }
    }
  }
  
  @Test
  void shouldEnforceRLS() throws Exception {
    // Create user in tenant A
    String tenantAToken = createMockJwt("tenant-a", "user-1");
    
    // Query with tenant A token - should see only tenant A data
    mockMvc.perform(post("/api/reports/query")
        .header("Authorization", "Bearer " + tenantAToken)
        .content(...))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data[*].tenant_id", everyItem(is("tenant-a"))));
    
    // Query with tenant B token - should see only tenant B data
    String tenantBToken = createMockJwt("tenant-b", "user-2");
    
    mockMvc.perform(post("/api/reports/query")
        .header("Authorization", "Bearer " + tenantBToken)
        .content(...))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data[*].tenant_id", everyItem(is("tenant-b"))));
  }
  
  @Test
  void shouldRejectInvalidQuery() throws Exception {
    QueryRequest invalid = QueryRequest.builder()
      .entity("User")
      .limit(100000) // Exceeds max 50000
      .build();
    
    mockMvc.perform(post("/api/reports/query")
        .content(objectMapper.writeValueAsString(invalid)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.title").value("Constraint Violation"))
      .andExpect(jsonPath("$.violations[0].field").value("limit"));
  }
  
  @Test
  void shouldReturn503OnCubeTimeout() throws Exception {
    // Mock Cube.js timeout
    wireMockServer.stubFor(post("/cubejs-api/v1/load")
      .willReturn(aResponse().withFixedDelay(35000))); // > 30s timeout
    
    mockMvc.perform(post("/api/reports/query")...)
      .andExpect(status().isServiceUnavailable())
      .andExpect(jsonPath("$.title").value("Service Unavailable"));
  }
  
  @Test
  void shouldReturn502OnCubeError() throws Exception {
    wireMockServer.stubFor(post("/cubejs-api/v1/load")
      .willReturn(aResponse().withStatus(500)));
    
    mockMvc.perform(post("/api/reports/query")...)
      .andExpect(status().isBadGateway());
  }
}
```

**Frontend E2E testy (4h):**

Vytvořit `frontend/tests/e2e/reporting.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';

test.describe('Reporting Module', () => {
  
  test('should render ExplorerGrid with data', async ({ page }) => {
    await page.goto('/reporting/users');
    
    // Wait for grid to load
    await expect(page.locator('[data-testid="explorer-grid"]')).toBeVisible();
    
    // Should have rows
    await expect(page.locator('.MuiDataGrid-row')).toHaveCount.greaterThan(0);
    
    // Should have columns
    await expect(page.locator('.MuiDataGrid-columnHeader')).toContainText(['Email', 'Status']);
  });
  
  test('should perform inline edit', async ({ page }) => {
    await page.goto('/reporting/users');
    
    // Click on cell
    await page.click('[data-row-id="1"] [data-field="status"]');
    
    // Edit value
    await page.fill('.MuiDataGrid-cell--editing input', 'INACTIVE');
    await page.press('.MuiDataGrid-cell--editing input', 'Enter');
    
    // Should show success toast
    await expect(page.locator('.toast-success')).toBeVisible();
    await expect(page.locator('.toast-success')).toContainText('Updated successfully');
  });
  
  test('should handle edit conflict (409)', async ({ page }) => {
    // Mock 409 response
    await page.route('**/api/entities/User/*', (route) => {
      route.fulfill({ status: 409, body: '{"error":"Conflict"}' });
    });
    
    await page.goto('/reporting/users');
    await page.click('[data-row-id="1"] [data-field="status"]');
    await page.fill('.MuiDataGrid-cell--editing input', 'ACTIVE');
    await page.press('.MuiDataGrid-cell--editing input', 'Enter');
    
    // Should show error toast
    await expect(page.locator('.toast-error')).toContainText('updated by another user');
  });
  
  test('should select rows and perform bulk action', async ({ page }) => {
    await page.goto('/reporting/users');
    
    // Select 3 rows
    await page.click('[data-row-id="1"] .MuiCheckbox-root');
    await page.click('[data-row-id="2"] .MuiCheckbox-root');
    await page.click('[data-row-id="3"] .MuiCheckbox-root');
    
    // Should show toolbar
    await expect(page.locator('[data-testid="bulk-toolbar"]')).toBeVisible();
    await expect(page.locator('[data-testid="bulk-toolbar"]')).toContainText('3 selected');
    
    // Click bulk action
    await page.click('button:has-text("Activate")');
    
    // Confirm dialog
    await page.click('button:has-text("Confirm")');
    
    // Should show job started toast
    await expect(page.locator('.toast-success')).toContainText('Bulk job');
  });
  
  test('should render PivotViewer', async ({ page }) => {
    await page.goto('/reporting/users/pivot');
    
    // Wait for Perspective viewer
    await expect(page.locator('perspective-viewer')).toBeVisible();
    
    // Should have controls
    await expect(page.locator('perspective-viewer')).toContainText('Group By');
  });
  
  test('should export from PivotViewer', async ({ page }) => {
    await page.goto('/reporting/users/pivot');
    
    // Wait for load
    await page.waitForLoadState('networkidle');
    
    // Click export button
    const downloadPromise = page.waitForEvent('download');
    await page.click('button:has-text("Export CSV")');
    const download = await downloadPromise;
    
    // Verify download
    expect(download.suggestedFilename()).toMatch(/\.csv$/);
  });
  
  test('should render ChartPanel and drill down', async ({ page }) => {
    await page.goto('/reporting/users/chart');
    
    // Wait for chart
    await expect(page.locator('[data-testid="chart-container"]')).toBeVisible();
    
    // Click on bar
    await page.click('canvas', { position: { x: 100, y: 200 } });
    
    // Should navigate to table view with filter
    await expect(page).toHaveURL(/\/reporting\/users\?status=ACTIVE/);
  });
  
  test('should handle rate limit (429)', async ({ page }) => {
    // Mock 429 response
    await page.route('**/api/reports/query', (route) => {
      route.fulfill({
        status: 429,
        headers: { 'Retry-After': '60' },
        body: '{"error":"Rate limit exceeded"}'
      });
    });
    
    await page.goto('/reporting/users');
    
    // Should show rate limit toast
    await expect(page.locator('.toast-warning')).toContainText('Rate limit');
  });
});
```

**CI Workflow:**

Upravit `.github/workflows/ci.yml`:

```yaml
jobs:
  backend-reporting-tests:
    name: Backend Reporting Tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      
      - name: Start test dependencies
        run: docker-compose up -d db redis
      
      - name: Run reporting integration tests
        run: |
          cd backend
          ./mvnw verify -Dit.test=**/reporting/**/*IT.java
      
      - uses: actions/upload-artifact@v3
        if: always()
        with:
          name: backend-test-results
          path: backend/target/failsafe-reports/

  frontend-reporting-e2e:
    name: Frontend Reporting E2E
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '24'
      
      - name: Install dependencies
        run: |
          cd frontend
          npm ci
      
      - name: Install Playwright
        run: npx playwright install --with-deps
      
      - name: Run E2E tests
        run: |
          cd frontend
          npm run test:e2e -- --grep "Reporting"
      
      - uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report
          path: frontend/playwright-report/
```

**DoD:**
- [ ] Backend IT testy pokrývají: valid query, cache, rate-limit, RLS, errors
- [ ] Frontend E2E testy pokrývají: ExplorerGrid, PivotViewer, ChartPanel
- [ ] CI workflow job pro backend reporting testy
- [ ] CI workflow job pro frontend reporting E2E
- [ ] Test artifacts (JUnit XML, screenshots, videos)
- [ ] Test coverage >80%
- [ ] Dokumentace test strategií

---

### TODO-R08: Performance Optimizations
- **Priorita:** 🟡 P1
- **Effort:** 8h
- **Vlastník:** BE + DevOps

**Kroky:**

1. **Single-flight deduplication** (3h):
   ```java
   @Component
   public class QueryDeduplicator {
     private final Map<String, CompletableFuture<QueryResponse>> inflight 
       = new ConcurrentHashMap<>();
     
     public CompletableFuture<QueryResponse> deduplicate(
         String fingerprint,
         Supplier<QueryResponse> supplier) {
       
       return inflight.computeIfAbsent(fingerprint, k -> 
         CompletableFuture.supplyAsync(supplier, executor)
           .whenComplete((res, ex) -> {
             inflight.remove(k);
             if (ex != null) {
               log.error("Query failed: {}", fingerprint, ex);
             }
           })
       );
     }
   }
   
   // V ReportQueryService
   public QueryResponse executeQuery(QueryRequest request, Authentication auth) {
     String fingerprint = queryFingerprint.generate(tenantId, request, specVersion);
     
     return queryDeduplicator.deduplicate(fingerprint, () -> {
       // existing query execution logic
     }).join();
   }
   ```

2. **Cube.js pre-aggregations** (3h):
   ```javascript
   // cube/schema/Users.js
   preAggregations: {
     // Daily active users by status
     dailyStatusCounts: {
       measures: [count, activeCount],
       dimensions: [status],
       timeDimension: createdAt,
       granularity: 'day',
       refreshKey: {
         every: '1 hour',
         incremental: true,
         updateWindow: '7 day'
       },
       partitionGranularity: 'month'
     },
     
     // Weekly user registration trend
     weeklySignups: {
       measures: [count],
       dimensions: [tenantId],
       timeDimension: createdAt,
       granularity: 'week',
       refreshKey: {
         every: '6 hours'
       }
     }
   }
   ```

3. **Grafana dashboard** (2h):
   Vytvořit `docker/grafana/provisioning/dashboards/reporting-bff.json`:
   ```json
   {
     "dashboard": {
       "title": "Reporting BFF Metrics",
       "panels": [
         {
           "title": "Query Latency (p50/p95/p99)",
           "targets": [{
             "expr": "histogram_quantile(0.95, rate(reporting_query_latency_seconds_bucket[5m]))",
             "legendFormat": "p95"
           }]
         },
         {
           "title": "Cache Hit Rate",
           "targets": [{
             "expr": "rate(reporting_query_total{cache_hit=\"true\"}[5m]) / rate(reporting_query_total[5m]) * 100"
           }]
         },
         {
           "title": "Queries per Minute",
           "targets": [{
             "expr": "rate(reporting_query_total[1m]) * 60"
           }]
         },
         {
           "title": "Error Rate",
           "targets": [{
             "expr": "rate(reporting_query_total{status=~\"4..|5..\"}[5m]) / rate(reporting_query_total[5m]) * 100"
           }]
         },
         {
           "title": "Circuit Breaker State",
           "targets": [{
             "expr": "resilience4j_circuitbreaker_state{name=\"cube\"}"
           }]
         },
         {
           "title": "Bulk Jobs Queue",
           "targets": [{
             "expr": "reporting_bulk_jobs_total{status=\"PENDING\"}"
           }]
         }
       ]
     }
   }
   ```

**DoD:**
- [ ] Single-flight deduplication implementováno
- [ ] Paralelní identické dotazy sdílí result
- [ ] Pre-aggregations v Cube.js pro top 5 queries
- [ ] Refresh strategie (hourly/daily)
- [ ] Grafana dashboard s p95/p99 latency
- [ ] Cache hit rate monitoring
- [ ] Circuit breaker state alert
- [ ] Load test: p95 < 500ms @ 100 req/s
- [ ] Dokumentace performance tuningu

---

## 🟢 STŘEDNÍ (P2) - Nice-to-have

### TODO-R09: View Definition Validation
- **Priorita:** 🟢 P2
- **Effort:** 2h
- **Vlastník:** BE

Viz **PR #6** v audit reportu - validace definition JSON proti metamodel spec.

---

### TODO-R10: Documentation Updates
- **Priorita:** 🟢 P2
- **Effort:** 2h
- **Vlastník:** Tech Writer / BE Lead

Viz **PR #10** v audit reportu - aktualizace README, rozšíření runbooku.

---

## 📊 Effort Summary

| Priorita | Počet TODO | Effort | Deadline |
|----------|-----------|--------|----------|
| 🔴 P0 | 4 | 62h (8 MD) | Sprint 1-3 |
| 🟡 P1 | 4 | 28h (3.5 MD) | Sprint 4-5 |
| 🟢 P2 | 2 | 4h (0.5 MD) | Sprint 6 |
| **TOTAL** | **10** | **94h (12 MD)** | **6 sprintů** |

---

## 🎯 Recommended Sprint Plan

### Sprint 1 (2 týdny): Infrastructure
- TODO-R01: Cube.js Setup (8h)
- TODO-R02: UI Spec Generator (6h)
- TODO-R05: Circuit Breaker (4h)

### Sprint 2-3 (4 týdny): Frontend
- TODO-R03: Frontend Components (32h)

### Sprint 4 (2 týdny): Writes & Security
- TODO-R04: Inline/Bulk Edit (16h)
- TODO-R06: Security Hardening (4h)

### Sprint 5 (2 týdny): Quality
- TODO-R07: Tests & CI (12h)
- TODO-R08: Performance (8h)

### Sprint 6 (1 týden): Polish
- TODO-R09: Validation (2h)
- TODO-R10: Docs (2h)

---

## ✅ Definition of Done (Global)

Každý TODO je hotový, když:

- [ ] Kód implementován dle specifikace
- [ ] Unit testy napsány (min. 80% coverage)
- [ ] Integration/E2E testy (kde relevantní)
- [ ] Code review completed (2+ approvals)
- [ ] Dokumentace aktualizována (JavaDoc, TSDoc, README)
- [ ] CI pipeline zelená
- [ ] Manuální smoke test passed
- [ ] Security review (pro P0/P1)
- [ ] Performance baseline verified (pro API změny)
- [ ] Deployed to staging
- [ ] Product Owner acceptance

---

**Konec TODO dokumentu** - Vygenerováno 11. října 2025
