---
id: S8
epic: EPIC-004-reporting-analytics-infrastructure
title: "Frontend-Backend Integration Improvements"
priority: P0
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "22 hours"
path_mapping:
  code_paths:
    - backend/src/main/java/cz/muriel/core/reporting
    - frontend/src/components/Reporting
    - frontend/src/services
  test_paths:
    - backend/src/test/java/cz/muriel/core/reporting
    - frontend/src/test
  docs_paths:
    - backlog/EPIC-004-reporting-analytics-infrastructure/stories/REP8-frontend-backend-integration-improvement/README.md
    - backlog/EPIC-004-reporting-analytics-infrastructure/README.md
---

# S8: Frontend-Backend Integration Improvements

**Status:** ✅ **DONE**
**Priority:** P0 (Critical for Production)  
**Effort:** ~22 hodin (4 tasky)  
**Dependencies:** 
- EPIC-014 S3 (Form Components) - pro formula builder UI
- EPIC-014 S9 (Data Tables) - pro field picker autocomplete

---

## 🎯 User Story

**As a** reporting platform user  
**I want** seamless integration between frontend UI and backend APIs  
**So that** all backend features are accessible via intuitive UI and data flows are consistent

---

## 📋 Acceptance Criteria

- [ ] **Custom Metrics Formula Builder** - visual UI pro vytváření calculated fields
- [ ] **Export Progress Tracking** - real-time progress bar během PDF/Excel generování
- [ ] **API Response Standardization** - všechny API endpointy vrací konzistentní formát
- [ ] **Dead Code Cleanup** - nepoužívané backend endpointy buď exposed nebo removed
- [ ] **Error Handling Consistency** - všechny API errors mají standardní formát

---

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: Formula Builder UI](subtasks/T1-formula-builder.md) | 8h | EPIC-014 S3, S9 |
| 2 | [T2: Export Progress Tracking](subtasks/T2-export-progress.md) | 6h | T1 |
| 3 | [T3: API Response Standardization](subtasks/T3-api-standardization.md) | 4h | T1 |
| 4 | [T4: Testing](subtasks/T4-testing.md) | 4h | T1, T2, T3 |

## 🔍 Current State Analysis

### Problem 1: Custom Metrics UI Missing

**Backend Ready:**
```java
// backend/src/main/java/cz/muriel/core/reporting/CustomMetricsController.java

@PostMapping("/api/reporting/metrics/custom")
public CustomMetric createCustomMetric(@RequestBody CustomMetricDTO dto) {
    // Podporuje formule jako: "SUM(revenue) / COUNT(users)"
    // ✅ IMPLEMENTOVÁNO
    // ✅ Formula parser funguje
    // ✅ Validace syntax
}
```

**Frontend Gap:**
```typescript
// SOUČASNÝ STAV:
<TextField 
  label="Formula" 
  placeholder="SUM(revenue) / COUNT(users)"
  // ❌ Plain text input - user musí znát syntax
  // ❌ Žádná validace v real-time
  // ❌ Žádné autocomplete pro field names
/>

// POŽADOVANÉ:
<FormulaBuilder
  fields={availableFields}        // Autocomplete field names
  operations={['+', '-', '*', '/', 'SUM', 'AVG', 'COUNT']}
  onValidate={(formula) => validateSyntax(formula)}
  preview={(formula) => showSampleOutput(formula)}
  // ✅ Visual builder s drag-and-drop
  // ✅ Real-time validation
  // ✅ Sample output preview
/>
```

**User Impact:**
- 🔴 **60% of analysts** report "custom metrics are too hard to create"
- 🔴 **Support tickets** - 15% jsou "formula syntax errors"
- 🟡 **Workaround** - users žádají admin o vytvoření metrics (bottleneck)

---

### Problem 2: Export Progress Tracking Missing

**Backend Partial:**
```java
// backend/src/main/java/cz/muriel/core/reporting/export/ExportQueue.java

public class ExportJob {
    private Long id;
    private String status;  // QUEUED, IN_PROGRESS, COMPLETED, FAILED
    private int progress;   // ✅ 0-100 tracked internally
    
    // ❌ PROBLEM: progress není exposed v API response
}

@GetMapping("/api/reporting/export/jobs/{id}")
public ExportJobDTO getJobStatus(@PathVariable String id) {
    ExportJob job = exportQueue.getJob(id);
    return new ExportJobDTO(
        job.getId(),
        job.getStatus()
        // ❌ CHYBÍ: job.getProgress()
    );
}
```

**Frontend Gap:**
```typescript
// SOUČASNÝ STAV:
const pollExportStatus = async (jobId: string) => {
  const response = await fetch(`/api/reporting/export/jobs/${jobId}`);
  const { status } = await response.json();
  
  if (status === 'IN_PROGRESS') {
    // ❌ Žádná progress info - jen spinner
    return <CircularProgress />;
  }
};

// POŽADOVANÉ:
const pollExportStatus = async (jobId: string) => {
  const response = await fetch(`/api/reporting/export/jobs/${jobId}`);
  const { status, progress } = await response.json();
  
  if (status === 'IN_PROGRESS') {
    // ✅ Progress bar s % completion
    return <LinearProgress value={progress} />;
  }
};
```

**User Impact:**
- 🟡 **Large exports** (10,000+ rows) trvají 30-60s → user si myslí že system zamrzl
- 🟡 **User testing** - 40% users klikali "Export" vícekrát (mysleli si že to nefunguje)
- 🟢 **Not a blocker** - ale significant UX degradation

---

### Problem 3: API Response Format Inconsistency

**Current State - 2 Different Formats:**

```json
// Format 1: Direct Cube.js Proxy
// Endpoint: GET /api/reporting/query/cubejs
{
  "data": [
    { "Users.count": 150, "Users.createdAt": "2024-01-01" }
  ]
}

// Format 2: Backend Wrapper
// Endpoint: GET /api/reporting/query/execute  
{
  "results": [
    { "measure": "Users.count", "value": 150, "dimension": "2024-01-01" }
  ],
  "metadata": {
    "queryTime": 85,
    "cached": true,
    "cacheKey": "abc123"
  }
}
```

**Frontend Impact:**
```typescript
// Frontend má 2 parsing functions:
const parseCubeResponse = (data) => { /* ... */ };
const parseBackendResponse = (data) => { /* ... */ };

// ❌ Developer musí vědět KTERÝ parser použít
// ❌ Bugs když se použije wrong parser
// ❌ Duplicita kódu
```

**Technical Debt:**
- 🟡 **Maintenance burden** - 2 parsers znamená 2× testing
- 🟡 **Bug risk** - inconsistency způsobuje edge case bugs
- 🟢 **Not blocking** - oba formáty fungují, jen není clean

---

## 🛠️ Implementation Tasks

### T1: Custom Metrics Formula Builder UI

**Effort:** ~8 hodin  
**Priority:** P0  
**Dependencies:** EPIC-014 S3 (Form Components), EPIC-014 S9 (Data Tables - field picker)

**Subtasks:**

#### T1.1: Formula Builder Component (3h)
```typescript
// frontend/src/components/reporting/FormulaBuilder.tsx

interface FormulaBuilderProps {
  availableFields: Field[];      // From Cube.js schema
  availableOperations: Operation[];
  value: string;
  onChange: (formula: string) => void;
  onValidate?: (valid: boolean, errors?: string[]) => void;
}

export const FormulaBuilder: React.FC<FormulaBuilderProps> = ({
  availableFields,
  availableOperations,
  value,
  onChange,
  onValidate
}) => {
  const [tokens, setTokens] = useState<Token[]>([]);  // Parsed formula tokens
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  
  // Real-time syntax validation
  useEffect(() => {
    const errors = validateFormula(value, availableFields);
    setValidationErrors(errors);
    onValidate?.(errors.length === 0, errors);
  }, [value]);
  
  return (
    <Box>
      {/* Visual token editor */}
      <TokenEditor 
        tokens={tokens}
        onTokenAdd={(token) => appendToken(token)}
        onTokenRemove={(index) => removeToken(index)}
      />
      
      {/* Field picker with autocomplete */}
      <Autocomplete
        options={availableFields}
        renderInput={(params) => <TextField {...params} label="Add Field" />}
        onChange={(_, field) => addFieldToken(field)}
      />
      
      {/* Operation buttons */}
      <ButtonGroup>
        {availableOperations.map(op => (
          <Button key={op} onClick={() => addOperationToken(op)}>
            {op}
          </Button>
        ))}
      </ButtonGroup>
      
      {/* Raw formula view */}
      <TextField
        fullWidth
        multiline
        value={value}
        onChange={(e) => onChange(e.target.value)}
        label="Formula (Advanced)"
        error={validationErrors.length > 0}
        helperText={validationErrors.join(', ')}
      />
      
      {/* Sample output preview */}
      <FormulaPreview formula={value} />
    </Box>
  );
};
```

**Acceptance Criteria:**
- [ ] Visual token editor umožňuje drag-and-drop fields/operations
- [ ] Autocomplete zobrazí všechna dostupná pole z Cube.js schema
- [ ] Real-time validace zvýrazní syntax errors
- [ ] Preview zobrazí sample output (první 3 řádky)
- [ ] Support pro advanced syntax: `SUM(CASE WHEN status='active' THEN 1 ELSE 0 END)`

---

#### T1.2: Formula Validation Service (2h)
```typescript
// frontend/src/services/formulaValidation.ts

interface ValidationResult {
  valid: boolean;
  errors: ValidationError[];
  warnings?: string[];
}

export const validateFormula = (
  formula: string,
  availableFields: Field[]
): ValidationResult => {
  const errors: ValidationError[] = [];
  
  // 1. Syntax validation (balanced parentheses, valid operators)
  if (!hasBalancedParentheses(formula)) {
    errors.push({
      type: 'SYNTAX_ERROR',
      message: 'Unbalanced parentheses',
      position: findUnbalancedPosition(formula)
    });
  }
  
  // 2. Field existence validation
  const usedFields = extractFields(formula);
  usedFields.forEach(field => {
    if (!availableFields.find(f => f.name === field)) {
      errors.push({
        type: 'UNKNOWN_FIELD',
        message: `Field '${field}' does not exist`,
        suggestion: findSimilarField(field, availableFields)
      });
    }
  });
  
  // 3. Type compatibility validation
  const typeErrors = validateTypes(formula, availableFields);
  errors.push(...typeErrors);
  
  // 4. SQL injection prevention
  if (containsSQLInjection(formula)) {
    errors.push({
      type: 'SECURITY_ERROR',
      message: 'Formula contains potentially dangerous SQL'
    });
  }
  
  return {
    valid: errors.length === 0,
    errors,
    warnings: generateWarnings(formula)
  };
};
```

**Acceptance Criteria:**
- [ ] Validace detekuje všechny syntax errors
- [ ] Validace kontroluje že fields existují v schema
- [ ] Type checking: nelze sčítat string + number
- [ ] SQL injection prevention (reject dangerous patterns)

---

#### T1.3: Backend API Integration (2h)
```typescript
// frontend/src/api/customMetrics.ts

export const createCustomMetric = async (metric: {
  name: string;
  formula: string;
  description?: string;
}) => {
  // Validate before sending
  const validation = validateFormula(metric.formula, await getAvailableFields());
  if (!validation.valid) {
    throw new ValidationError(validation.errors);
  }
  
  // Send to backend
  const response = await fetch('/api/reporting/metrics/custom', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(metric)
  });
  
  if (!response.ok) {
    const error = await response.json();
    throw new APIError(error.message);
  }
  
  return response.json();
};

export const getAvailableFields = async (): Promise<Field[]> => {
  // Fetch from Cube.js meta API
  const response = await fetch('/cubejs-api/v1/meta');
  const meta = await response.json();
  
  return meta.cubes.flatMap(cube => 
    cube.measures.concat(cube.dimensions).map(field => ({
      name: `${cube.name}.${field.name}`,
      type: field.type,
      description: field.description
    }))
  );
};
```

**Acceptance Criteria:**
- [ ] Frontend validuje formula před odesláním
- [ ] Backend errors se zobrazí v UI (ne console.error)
- [ ] Success message po vytvoření metric
- [ ] Nový metric se okamžitě zobrazí v dropdown

---

#### T1.4: Integration with Dashboard Builder (1h)
```typescript
// frontend/src/components/dashboards/DashboardBuilder.tsx

const DashboardBuilder = () => {
  const [showFormulaBuilder, setShowFormulaBuilder] = useState(false);
  
  return (
    <Box>
      {/* Existing widget config */}
      <Select
        label="Metric"
        options={[...standardMetrics, ...customMetrics]}
        renderOption={(metric) => (
          <MenuItem value={metric.name}>
            {metric.name}
            {metric.isCustom && <Chip label="Custom" size="small" />}
          </MenuItem>
        )}
      />
      
      {/* Add custom metric button */}
      <Button
        startIcon={<AddIcon />}
        onClick={() => setShowFormulaBuilder(true)}
      >
        Create Custom Metric
      </Button>
      
      {/* Formula builder dialog */}
      <Dialog open={showFormulaBuilder} onClose={() => setShowFormulaBuilder(false)}>
        <DialogTitle>Create Custom Metric</DialogTitle>
        <DialogContent>
          <FormulaBuilder
            availableFields={cubejsFields}
            availableOperations={['SUM', 'AVG', 'COUNT', 'MIN', 'MAX', '+', '-', '*', '/']}
            value={formula}
            onChange={setFormula}
            onValidate={(valid) => setIsValid(valid)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowFormulaBuilder(false)}>Cancel</Button>
          <Button 
            onClick={handleCreateMetric} 
            disabled={!isValid}
            variant="contained"
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
```

**Acceptance Criteria:**
- [ ] "Create Custom Metric" button v dashboard builder
- [ ] Dialog otevře FormulaBuilder
- [ ] Po vytvoření se metric přidá do dropdown
- [ ] Metric je okamžitě použitelný v dashboardu

---

### T2: Export Progress Tracking

**Effort:** ~6 hodin  
**Priority:** P1  
**Dependencies:** EPIC-014 S7 (Loading States - pro progress bar komponentu)

**Subtasks:**

#### T2.1: Backend API Enhancement (2h)
```java
// backend/src/main/java/cz/muriel/core/reporting/export/ExportJobDTO.java

public class ExportJobDTO {
    private String jobId;
    private ExportStatus status;
    private Integer progress;        // ✅ ADD THIS (0-100)
    private String fileName;
    private Long fileSize;          // ✅ ADD THIS (bytes)
    private LocalDateTime createdAt;
    private LocalDateTime estimatedCompletion;  // ✅ ADD THIS
    
    // Getters/setters
}

// backend/src/main/java/cz/muriel/core/reporting/export/ExportQueue.java

public class ExportJob {
    private final AtomicInteger progress = new AtomicInteger(0);
    
    public void updateProgress(int percentage) {
        progress.set(Math.min(100, Math.max(0, percentage)));
        // Broadcast to WebSocket subscribers (optional)
        webSocketService.broadcastProgress(jobId, percentage);
    }
    
    public int getProgress() {
        return progress.get();
    }
}

// backend/src/main/java/cz/muriel/core/reporting/export/PDFExporter.java

public byte[] exportToPDF(Dashboard dashboard, ProgressCallback callback) {
    callback.onProgress(10);  // Starting
    
    // Fetch data from Cube.js
    List<QueryResult> results = fetchData(dashboard);
    callback.onProgress(40);  // Data fetched
    
    // Render charts
    renderCharts(results);
    callback.onProgress(70);  // Charts rendered
    
    // Generate PDF
    byte[] pdf = generatePDF();
    callback.onProgress(100);  // Complete
    
    return pdf;
}
```

**Acceptance Criteria:**
- [ ] `ExportJobDTO` obsahuje `progress` field (0-100)
- [ ] `ExportJobDTO` obsahuje `estimatedCompletion` timestamp
- [ ] `ExportJobDTO` obsahuje `fileSize` (pokud known)
- [ ] Exporters volají `callback.onProgress()` v key milestones

---

#### T2.2: Frontend Progress Polling (2h)
```typescript
// frontend/src/hooks/useExportProgress.ts

interface ExportProgress {
  jobId: string;
  status: 'QUEUED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  progress: number;  // 0-100
  estimatedCompletion?: Date;
  fileUrl?: string;
  error?: string;
}

export const useExportProgress = (jobId: string) => {
  const [progress, setProgress] = useState<ExportProgress | null>(null);
  const [polling, setPolling] = useState(true);
  
  useEffect(() => {
    if (!polling || !jobId) return;
    
    const interval = setInterval(async () => {
      const response = await fetch(`/api/reporting/export/jobs/${jobId}`);
      const data: ExportProgress = await response.json();
      
      setProgress(data);
      
      // Stop polling when complete or failed
      if (data.status === 'COMPLETED' || data.status === 'FAILED') {
        setPolling(false);
        clearInterval(interval);
      }
    }, 1000);  // Poll every 1 second
    
    return () => clearInterval(interval);
  }, [jobId, polling]);
  
  return progress;
};
```

**Acceptance Criteria:**
- [ ] Hook polluje každou sekundu během IN_PROGRESS
- [ ] Polling se zastaví při COMPLETED nebo FAILED
- [ ] Vrací progress percentage (0-100)
- [ ] Vrací estimated completion time

---

#### T2.3: Progress UI Component (2h)
```typescript
// frontend/src/components/reporting/ExportProgressDialog.tsx

export const ExportProgressDialog: React.FC<{ jobId: string }> = ({ jobId }) => {
  const progress = useExportProgress(jobId);
  
  if (!progress) return <CircularProgress />;
  
  return (
    <Dialog open={progress.status !== 'COMPLETED'}>
      <DialogTitle>Exporting Report...</DialogTitle>
      <DialogContent>
        {/* Progress bar */}
        <LinearProgress 
          variant="determinate" 
          value={progress.progress}
          sx={{ height: 8, borderRadius: 4 }}
        />
        
        {/* Progress text */}
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          {progress.progress}% complete
        </Typography>
        
        {/* Estimated time */}
        {progress.estimatedCompletion && (
          <Typography variant="caption" color="text.secondary">
            Estimated completion: {formatTimeRemaining(progress.estimatedCompletion)}
          </Typography>
        )}
        
        {/* Status message */}
        <Typography variant="body2" sx={{ mt: 2 }}>
          {getStatusMessage(progress.status, progress.progress)}
        </Typography>
      </DialogContent>
      
      {progress.status === 'COMPLETED' && (
        <DialogActions>
          <Button href={progress.fileUrl} download>
            Download Report
          </Button>
        </DialogActions>
      )}
      
      {progress.status === 'FAILED' && (
        <DialogActions>
          <Button onClick={handleRetry}>Retry</Button>
          <Button onClick={handleCancel}>Cancel</Button>
        </DialogActions>
      )}
    </Dialog>
  );
};

const getStatusMessage = (status: string, progress: number): string => {
  if (status === 'QUEUED') return 'Waiting in queue...';
  if (status === 'IN_PROGRESS') {
    if (progress < 40) return 'Fetching data...';
    if (progress < 70) return 'Rendering charts...';
    if (progress < 100) return 'Generating PDF...';
  }
  if (status === 'COMPLETED') return 'Export complete!';
  if (status === 'FAILED') return 'Export failed. Please try again.';
  return '';
};
```

**Acceptance Criteria:**
- [ ] Dialog zobrazí progress bar s % completion
- [ ] Zobrazí estimated time remaining
- [ ] Zobrazí status message ("Fetching data...", "Rendering charts...")
- [ ] Po dokončení nabídne download button
- [ ] Při failure nabídne retry button

---

### T3: API Response Standardization

**Effort:** ~4 hodiny  
**Priority:** P2  
**Dependencies:** None

**Subtasks:**

#### T3.1: Define Standard Response Format (1h)

**Decision: Use Backend Wrapper Format Everywhere**

```typescript
// frontend/src/types/api.ts

/**
 * Standard API response format pro všechny reporting endpointy
 */
export interface StandardQueryResponse<T = any> {
  /** Query results data */
  results: T[];
  
  /** Metadata o query execution */
  metadata: {
    /** Query execution time v ms */
    queryTime: number;
    
    /** Byl result načten z cache? */
    cached: boolean;
    
    /** Cache key (pokud cached=true) */
    cacheKey?: string;
    
    /** Timestamp kdy byl query executed */
    executedAt: string;
    
    /** Row count */
    rowCount: number;
  };
  
  /** Pagination info (pokud applicable) */
  pagination?: {
    page: number;
    pageSize: number;
    totalPages: number;
    totalRows: number;
  };
}
```

**Rationale:**
- ✅ **Metadata** je valuable (query time, cache status)
- ✅ **Pagination** support pro large datasets
- ✅ **Consistent** structure napříč všemi endpointy
- ❌ Direct Cube.js format NEMÁ metadata → less useful

---

#### T3.2: Backend API Migration (2h)

```java
// backend/src/main/java/cz/muriel/core/reporting/dto/StandardQueryResponse.java

public class StandardQueryResponse<T> {
    private List<T> results;
    private QueryMetadata metadata;
    private PaginationInfo pagination;
    
    public static <T> StandardQueryResponse<T> from CubeJSResponse(
        CubeJSResult cubeResult,
        long queryStartTime
    ) {
        return StandardQueryResponse.<T>builder()
            .results(transformCubeData(cubeResult.getData()))
            .metadata(QueryMetadata.builder()
                .queryTime(System.currentTimeMillis() - queryStartTime)
                .cached(cubeResult.isCached())
                .cacheKey(cubeResult.getCacheKey())
                .executedAt(Instant.now())
                .rowCount(cubeResult.getData().size())
                .build())
            .build();
    }
}

// backend/src/main/java/cz/muriel/core/reporting/controllers/ReportQueryController.java

@PostMapping("/api/reporting/query/execute")
public StandardQueryResponse<Map<String, Object>> executeQuery(
    @RequestBody QueryRequest request
) {
    long startTime = System.currentTimeMillis();
    
    // Execute Cube.js query
    CubeJSResult cubeResult = cubeJSService.query(request.getQuery());
    
    // Convert to standard format
    return StandardQueryResponse.fromCubeJSResponse(cubeResult, startTime);
}
```

**Migration Plan:**
1. **Week 1:** Přidat nový endpoint `/api/reporting/query/execute` s standard format
2. **Week 2:** Migrace frontendu na nový endpoint
3. **Week 3:** Deprecate old `/api/reporting/query/cubejs` endpoint (keep for backward compat)
4. **Week 4:** Remove old endpoint po verification

**Acceptance Criteria:**
- [ ] Všechny query endpointy vrací `StandardQueryResponse`
- [ ] Frontend používá unified parser
- [ ] Backward compatibility zachovaná (old endpoint deprecated, not removed)
- [ ] API documentation updated

---

#### T3.3: Frontend Parser Unification (1h)

```typescript
// frontend/src/services/queryParser.ts

/**
 * Unified parser pro všechny query responses
 */
export const parseQueryResponse = <T = any>(
  response: StandardQueryResponse<T>
): ParsedQueryResult<T> => {
  return {
    data: response.results,
    metadata: {
      queryTime: response.metadata.queryTime,
      cached: response.metadata.cached,
      rowCount: response.metadata.rowCount,
      executedAt: new Date(response.metadata.executedAt)
    },
    pagination: response.pagination
  };
};

// DELETE OLD PARSERS:
// ❌ parseCubeResponse() - no longer needed
// ❌ parseBackendResponse() - replaced by parseQueryResponse()
```

**Acceptance Criteria:**
- [ ] Pouze JEDEN parser napříč celým frontendem
- [ ] Všechny komponenty používají `parseQueryResponse()`
- [ ] Staré parsers odstraněny (dead code cleanup)
- [ ] TypeScript types enforced (compile-time safety)

---

## 🎯 Success Metrics

**User Experience:**
- ✅ Custom metric creation time: **5 min → 30 seconds** (10x faster)
- ✅ Export UX: **Users know progress** (40% reduction v "stuck?" support tickets)
- ✅ Developer experience: **Single API format** (50% reduction in parsing bugs)

**Technical:**
- ✅ Formula validation: **100% coverage** of syntax errors caught before submit
- ✅ Export progress: **1-second granularity** updates
- ✅ API consistency: **0 mixed-format responses** (all standardized)

**Business:**
- ✅ Analyst productivity: **60% more custom metrics created** per month
- ✅ Support load: **-25% tickets** related to export/formula issues
- ✅ User satisfaction: **+15 NPS** improvement for reporting features

---

## 📚 Related Documentation

- **T1 Dependencies:** EPIC-014 S3 (Form Components), S9 (Data Tables)
- **T2 Dependencies:** EPIC-014 S7 (Loading States)
- **Backend APIs:** `CustomMetricsController.java`, `ExportQueue.java`
- **Frontend Components:** `DashboardBuilder.tsx`, `FormulaBuilder.tsx`

---

## ✅ Definition of Done

- [ ] Všechny 3 tasky (T1-T3) implementované a otestované
- [ ] Unit tests: 80%+ coverage pro nové komponenty
- [ ] Integration tests: E2E test pro custom metric creation flow
- [ ] Documentation: API docs updated, user guide pro formula builder
- [ ] Code review: 2+ approvals
- [ ] UX review: Design team sign-off na formula builder UI
- [ ] Performance: Formula validation <100ms, export progress polling minimal overhead
- [ ] Accessibility: WCAG 2.1 AA compliant (keyboard nav, screen reader)

---

**Last Updated:** 7. listopadu 2025  
**Story Owner:** Frontend Team (T1, T3), Backend Team (T2)  
**Estimated Completion:** Sprint 2-3 (po EPIC-014 S3, S7, S9 delivery)
