# 📅 EPIC: Admin & Lifecycle - Implementation Plan

**Datum:** 12. října 2025  
**Branch base:** `feature/admin-epic` (vše v main, žádné nové větve)  
**Style:** No-chatter – diffy, PR, krátké DoD, logy jako přílohy  
**Status:** Plán pro S10–S15

---

## 🎯 Cíle EPIC

Implementovat kompletní admin & lifecycle subsystém:
1. **Metamodel Studio** – GUI + BE API pro správu metamodelu s diff/preview/approval
2. **Admin Config GUI** – per-env a per-tenant parametry, secrets masking, audit
3. **Archivace & Obnova** – backup/restore jobs, S3/MinIO, DR runbook
4. **Workflow** – per-entity lifecycle, editor, akce v UI, Policy/Rules engine
5. **Admin Console** – unified dashboard pro jobs, health, streaming, CB
6. **RBAC** – 5 nových Keycloak rolí pro admin oblasti
7. **Hardening** – E2E testy, CI gates, security, 0 TODO/warnings

---

## 📦 Delivery Plan (S10–S15)

### **S10-0 – Audit & Gap Report** ✅ DONE

**Deliverables:**
- [x] `docs/EPIC_ADMIN_LIFECYCLE_AUDIT.md`
- [x] `docs/EPIC_ADMIN_LIFECYCLE_PLAN.md`

**DoD:**
- [x] PR merged po review
- [x] Žádné změny kódu

---

### **S10 – Metamodel Studio (GUI + BE, diff/preview/approval)**

**Odhad:** 43.5h  
**Priorita:** HIGH (blokující pro ostatní admin GUI)  
**Závislosti:** Žádné

#### Scope

**Frontend (`/admin/studio`):**
- Seznam entit (fetch z metamodelu)
- Detail editor:
  - Form fields (name, tableName, fields)
  - Monaco JSON editor (raw YAML/JSON)
  - Validation (JSON schema + naming-lint)
  - Diff view vůči current (side-by-side)
- Akce:
  - **Validate** (POST `/api/admin/studio/validate`)
  - **Preview modelgen** (POST `/api/admin/studio/preview` – dry-run)
  - **Create Proposal** (POST `/api/admin/studio/proposals`)

**Backend:**
```
GET    /api/admin/studio/entities           # List all entities
GET    /api/admin/studio/entities/{entity}  # Get entity detail
PUT    /api/admin/studio/entities/{entity}  # Update entity (draft)
POST   /api/admin/studio/validate           # Validate entity (schema + naming-lint)
POST   /api/admin/studio/preview            # Dry-run modelgen + cube-smoke /meta
POST   /api/admin/studio/proposals          # Create change request
GET    /api/admin/studio/proposals          # List CRs
POST   /api/admin/studio/proposals/{id}/approve  # Approve CR → regenerate schemas
```

**DB migrace:**
```sql
-- V202510121000__create_metamodel_versions.sql
CREATE TABLE metamodel_versions (
    id BIGSERIAL PRIMARY KEY,
    version TEXT NOT NULL UNIQUE,
    entities JSONB NOT NULL,      -- snapshot celého metamodelu
    checksum TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by TEXT
);

-- V202510121001__create_metamodel_change_requests.sql
CREATE TABLE metamodel_change_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_name TEXT NOT NULL,
    diff JSONB NOT NULL,           -- před/po
    status TEXT NOT NULL,          -- pending, approved, rejected
    author TEXT NOT NULL,
    approver TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    approved_at TIMESTAMPTZ,
    comments TEXT
);
```

**RBAC:**
- Keycloak role: `CORE_ADMIN_STUDIO`
- Backend: `@PreAuthorize("hasAuthority('CORE_ADMIN_STUDIO')")`
- Frontend: menu visibility check

**CI:**
```yaml
# .github/workflows/modelgen-validate.yml
name: Metamodel Validation
on:
  pull_request:
    paths:
      - 'backend/src/main/resources/metamodel/*.yaml'
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Validate metamodel
        run: ./scripts/validate-metamodel.sh
      - name: Preview modelgen dry-run
        run: ./scripts/modelgen-dry-run.sh
      - name: Cube smoke test
        run: curl http://localhost:4000/cubejs-api/v1/meta
```

#### DoD

- [ ] FE: `/admin/studio` komponenta hotová
- [ ] BE: 8 endpoints implementované
- [ ] DB: 2 migrace aplikované
- [ ] RBAC: role přidána do realm + @PreAuthorize
- [ ] CI: modelgen-validate job green
- [ ] E2E: edit → validate → preview → propose → approve → modelgen → cube smoke
- [ ] Docs: `docs/ADMIN_STUDIO.md` (flow, omezení, recovery)
- [ ] PR: tail z `mvn -Ptest verify` + `pnpm test:e2e`

---

### **S11 – Admin Config GUI (env/tenant parametry, flags, secrets)**

**Odhad:** 24.5h  
**Priorita:** MEDIUM  
**Závislosti:** Žádné (paralelně s S10)

#### Scope

**Frontend (`/admin/config`):**
- Tabulka env×tenant (rows: tenants, cols: envs)
- Editor hodnot (inline nebo dialog)
- Maskování secrets:
  - Input type=password
  - Zobrazení: `••••1234` (last 4 chars)
- Diff view (before/after)
- Export/import JSON
- Feature flags toggles (boolean switches)

**Backend:**
```
GET    /api/admin/config/{scope}  # scope=env|tenant|feature-flags
PUT    /api/admin/config/{scope}  # update with audit trail
POST   /api/admin/config/export   # JSON export
POST   /api/admin/config/import   # JSON import
```

**Maskování:**
```java
// ConfigMaskingInterceptor.java
@Component
public class ConfigMaskingInterceptor implements HandlerInterceptor {
    @Override
    public void postHandle(..., Object handler, ModelAndView modelAndView) {
        if (response instanceof ConfigResponse) {
            maskSecrets(response);
        }
    }
    
    private void maskSecrets(ConfigResponse response) {
        response.getValues().forEach((key, value) -> {
            if (key.contains("SECRET") || key.contains("PASSWORD")) {
                response.put(key, maskValue(value));
            }
        });
    }
    
    private String maskValue(String value) {
        if (value.length() <= 4) return "••••";
        return "••••" + value.substring(value.length() - 4);
    }
}
```

**DB migrace:**
```sql
-- V202510121100__create_config_audit.sql
CREATE TABLE config_audit (
    id BIGSERIAL PRIMARY KEY,
    scope TEXT NOT NULL,              -- env/tenant/feature-flags
    key TEXT NOT NULL,
    before_value TEXT,                -- masked if secret
    after_value TEXT,                 -- masked if secret
    changed_by TEXT NOT NULL,
    changed_at TIMESTAMPTZ DEFAULT NOW(),
    ip_address TEXT
);
CREATE INDEX idx_config_audit_scope ON config_audit(scope);
CREATE INDEX idx_config_audit_key ON config_audit(key);
CREATE INDEX idx_config_audit_changed_at ON config_audit(changed_at DESC);
```

**RBAC:**
- Keycloak role: `CORE_ADMIN_CONFIG`

#### DoD

- [ ] FE: `/admin/config` komponenta
- [ ] BE: 4 endpoints + masking interceptor
- [ ] DB: config_audit migrace
- [ ] E2E: edit with masking → audit → export/import
- [ ] Docs: `docs/ENV_CONFIG.md` (maskování, audit, rollout, canary)
- [ ] PR: tail logů

---

### **S12 – Archivace & Obnova (Backup/Restore)**

**Odhad:** 31.5h  
**Priorita:** MEDIUM  
**Závislosti:** Žádné (paralelně s S10/S11)

#### Scope

**DB:**
```sql
-- V202510121200__create_backup_restore_jobs.sql
CREATE TABLE backup_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope TEXT NOT NULL,              -- full, tenant, entity
    source TEXT NOT NULL,             -- tenant_id nebo entity_type
    storage_path TEXT NOT NULL,       -- s3://bucket/path
    checksum TEXT NOT NULL,           -- SHA-256
    status TEXT NOT NULL,             -- pending, running, completed, failed
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    size_bytes BIGINT,
    logs TEXT
);

CREATE TABLE restore_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    backup_id UUID REFERENCES backup_jobs(id),
    target_env TEXT NOT NULL,         -- sandbox/production
    dry_run BOOLEAN DEFAULT FALSE,
    status TEXT NOT NULL,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    logs TEXT
);
```

**Backend:**
```
POST   /api/admin/backup              # on-demand, scope (full/tenant/entity)
GET    /api/admin/backup/jobs         # list with filters
GET    /api/admin/backup/jobs/{id}    # detail + logs

POST   /api/admin/restore             # wizard: source/time/scope/dry-run
GET    /api/admin/restore/jobs        # list
GET    /api/admin/restore/jobs/{id}   # detail + logs
```

**MinIO integrace:**
```java
// BackupService.java
@Service
public class BackupService {
    @Autowired private MinioClient minioClient;
    @Autowired private BackupJobRepository backupJobRepository;
    
    public BackupJob createBackup(String scope, String source) {
        BackupJob job = new BackupJob();
        job.setScope(scope);
        job.setSource(source);
        job.setStatus("pending");
        backupJobRepository.save(job);
        
        CompletableFuture.runAsync(() -> executeBackup(job));
        return job;
    }
    
    private void executeBackup(BackupJob job) {
        try {
            job.setStatus("running");
            job.setStartedAt(Instant.now());
            
            // 1. Export data to temp file
            Path tempFile = exportData(job.getScope(), job.getSource());
            
            // 2. Compute checksum
            String checksum = computeSHA256(tempFile);
            
            // 3. Upload to MinIO (SSE-S3)
            String storagePath = String.format("backups/%s/%s.sql.gz", 
                job.getScope(), job.getId());
            minioClient.putObject(PutObjectArgs.builder()
                .bucket("backups")
                .object(storagePath)
                .stream(Files.newInputStream(tempFile), Files.size(tempFile), -1)
                .serverSideEncryption(ServerSideEncryption.atRest())
                .build());
            
            // 4. Update job
            job.setStoragePath(storagePath);
            job.setChecksum(checksum);
            job.setStatus("completed");
            job.setFinishedAt(Instant.now());
            backupJobRepository.save(job);
            
        } catch (Exception e) {
            job.setStatus("failed");
            job.setLogs(e.getMessage());
            backupJobRepository.save(job);
        }
    }
}
```

**Retention:**
```java
@Scheduled(cron = "0 0 2 * * *")  // Daily 2AM
public void applyRetentionPolicy() {
    Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
    List<BackupJob> oldBackups = backupJobRepository.findOlderThan(cutoff);
    
    oldBackups.forEach(backup -> {
        // Delete from MinIO
        minioClient.removeObject(RemoveObjectArgs.builder()
            .bucket("backups")
            .object(backup.getStoragePath())
            .build());
        
        // Mark as deleted
        backup.setStatus("deleted");
        backupJobRepository.save(backup);
    });
}
```

**Frontend (`/admin/backup`):**
- Tab: Create Backup (form: scope, source)
- Tab: Backup Jobs (list, detail, logs)
- Tab: Restore (wizard: select backup, target env, dry-run toggle)
- Tab: Restore Jobs (list, detail, logs)

**DR runbook:**
```bash
#!/bin/bash
# scripts/dr-restore.sh
# Disaster Recovery - Restore from backup

BACKUP_ID=$1
TARGET_ENV=${2:-sandbox}

if [ -z "$BACKUP_ID" ]; then
    echo "Usage: $0 <backup-id> [target-env]"
    exit 1
fi

# 1. Download from MinIO
mc cp minio/backups/full/${BACKUP_ID}.sql.gz /tmp/restore.sql.gz

# 2. Verify checksum
EXPECTED=$(curl http://localhost:8080/api/admin/backup/jobs/${BACKUP_ID} | jq -r '.checksum')
ACTUAL=$(sha256sum /tmp/restore.sql.gz | awk '{print $1}')
if [ "$EXPECTED" != "$ACTUAL" ]; then
    echo "Checksum mismatch!"
    exit 1
fi

# 3. Restore to target
if [ "$TARGET_ENV" == "sandbox" ]; then
    gunzip < /tmp/restore.sql.gz | psql -h sandbox-db -U core_user -d core_platform
else
    echo "Production restore requires manual approval!"
    exit 1
fi
```

**RBAC:**
- Keycloak role: `CORE_ADMIN_BACKUP`

#### DoD

- [ ] BE: 6 endpoints + BackupService + RestoreService
- [ ] DB: 2 migrace
- [ ] MinIO: SSE-S3, checksum, retention
- [ ] FE: `/admin/backup` komponenta (wizard, jobs)
- [ ] Script: `scripts/dr-restore.sh`
- [ ] E2E: create backup → dry-run restore → sandbox restore → verify
- [ ] Docs: `docs/BACKUP_RESTORE.md` (strategie, RPO/RTO, verifikace, DR drill)
- [ ] PR: tail logů

---

### **S13 – Workflow (definice, editor, runtime, akce v UI)**

**Odhad:** 51.5h  
**Priorita:** HIGH  
**Závislosti:** S10 (editor pattern)

#### Scope

**Definice (YAML/JSON per entita):**
```yaml
# backend/src/main/resources/workflows/user.yaml
states:
  - code: draft
    label: Draft
  - code: active
    label: Active
  - code: suspended
    label: Suspended

transitions:
  - code: ACTIVATE
    from: draft
    to: active
    label: Activate User
    guard:
      type: cel
      expression: "has(request.auth.claims.roles) && 'CORE_ROLE_ADMIN' in request.auth.claims.roles"
    slaMinutes: 60
    actions:
      - type: email
        template: user-activated
      - type: webhook
        url: ${WEBHOOK_URL}/user-activated
  
  - code: SUSPEND
    from: active
    to: suspended
    label: Suspend User
    guard:
      type: cel
      expression: "has(request.auth.claims.roles) && 'CORE_ROLE_ADMIN' in request.auth.claims.roles"
    slaMinutes: 30
```

**DB migrace:**
```sql
-- V202510121300__create_workflow_instances.sql
CREATE TABLE workflow_instances (
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    workflow_version TEXT NOT NULL,  -- verzování definic
    current_state TEXT NOT NULL,
    since TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (entity_type, entity_id, tenant_id)
);
CREATE INDEX idx_workflow_instances_state ON workflow_instances(current_state);
```

**Backend:**
```java
// WorkflowService.java (rozšíření existujícího)
@Service
public class WorkflowService {
    
    // Přidat timer support
    @Scheduled(fixedDelay = 60000)  // Check every minute
    public void processTimers() {
        List<WorkflowModels.EntityState> states = jdbcTemplate.query(
            "SELECT * FROM entity_state WHERE sla_minutes IS NOT NULL " +
            "AND since + (sla_minutes || ' minutes')::interval < NOW()",
            new EntityStateRowMapper()
        );
        
        states.forEach(state -> {
            // Emit Kafka event
            kafkaTemplate.send("core.workflow.events", WorkflowEvent.builder()
                .type("SLA_BREACH")
                .entityType(state.getEntityType())
                .entityId(state.getEntityId())
                .stateCode(state.getStateCode())
                .since(state.getSince())
                .build()
            );
        });
    }
    
    // Přidat CEL guard support
    @Autowired private PolicyEngine policyEngine;
    
    private boolean evaluateGuard(Authentication auth, Map<String, Object> guard) {
        if (guard == null || guard.isEmpty()) {
            return true;
        }
        
        String type = (String) guard.get("type");
        String expression = (String) guard.get("expression");
        
        if ("cel".equals(type)) {
            return policyEngine.evaluateCEL(expression, auth);
        }
        
        // Fallback: simple hasRole
        if (expression.startsWith("hasRole('")) {
            String role = expression.substring(9, expression.length() - 2);
            return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
        }
        
        return true;
    }
}

// PolicyEngine.java (rozšíření)
public interface PolicyEngine {
    boolean evaluateCEL(String expression, Authentication auth);
}

@Service
public class CELPolicyEngine implements PolicyEngine {
    @Override
    public boolean evaluateCEL(String expression, Authentication auth) {
        // Implementace CEL evaluation
        // https://github.com/google/cel-java
        CELEvaluator evaluator = new CELEvaluator();
        Map<String, Object> context = Map.of(
            "request", Map.of(
                "auth", Map.of(
                    "claims", extractClaims(auth)
                )
            )
        );
        return evaluator.evaluate(expression, context);
    }
}

// Kafka events
@Component
public class WorkflowEventProducer {
    @Autowired private KafkaTemplate<String, WorkflowEvent> kafkaTemplate;
    
    public void emit(WorkflowEvent event) {
        kafkaTemplate.send("core.workflow.events", event.getEntityId(), event);
    }
}
```

**Frontend (EntityView SDK extension):**
```tsx
// frontend/src/lib/entityview/WorkflowActions.tsx
interface WorkflowActionsProps {
  entityType: string;
  entityId: string;
  currentState: string;
}

export const WorkflowActions: React.FC<WorkflowActionsProps> = ({
  entityType,
  entityId,
  currentState
}) => {
  const [transitions, setTransitions] = useState<Transition[]>([]);
  const [loading, setLoading] = useState(false);
  const { presence, isStale } = usePresence({
    entity: entityType,
    id: entityId,
    ...
  });
  
  useEffect(() => {
    fetchTransitions();
  }, [currentState]);
  
  const fetchTransitions = async () => {
    const res = await axios.get(
      `/api/entities/${entityType}/${entityId}/transitions`
    );
    setTransitions(res.data);
  };
  
  const handleAction = async (transitionCode: string) => {
    if (isStale || presence.lock) {
      // Show warning
      return;
    }
    
    setLoading(true);
    try {
      await axios.post(
        `/api/entities/${entityType}/${entityId}/transition/${transitionCode}`
      );
      // Refresh
      fetchTransitions();
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <Box>
      <Typography variant="h6">Workflow Actions</Typography>
      {isStale && (
        <Alert severity="warning">
          Entity is being modified elsewhere. Actions disabled.
        </Alert>
      )}
      <Stack direction="row" spacing={2} mt={2}>
        {transitions.map(t => (
          <Button
            key={t.code}
            variant="contained"
            onClick={() => handleAction(t.code)}
            disabled={isStale || presence.lock || loading}
          >
            {t.code}
          </Button>
        ))}
      </Stack>
    </Box>
  );
};
```

**Frontend (`/admin/workflows` editor):**
- Seznam definic (fetch z `/workflows/*.yaml`)
- Monaco editor (YAML mode)
- Validation (JSON schema)
- Preview (dot graph rendering – Graphviz)
- Diff (side-by-side)
- Publish (commit to repo nebo uložit do DB)

**RBAC:**
- Keycloak role: `CORE_ADMIN_WORKFLOW`

#### DoD

- [ ] BE: Timer scheduler + CEL guards + Kafka events
- [ ] DB: workflow_instances migrace
- [ ] FE: EntityView SDK – Actions panel
- [ ] FE: `/admin/workflows` editor
- [ ] E2E: User spustí workflow akci → stav se změní → read-only při lock/stale
- [ ] Unit: Akce mění stav deterministicky, guards respektovány
- [ ] IT: Timers se spouští
- [ ] Docs: `docs/WORKFLOW.md` (definice, guards, actions, timers, testy)
- [ ] PR: tail logů

---

### **S14 – Admin Console (Jobs + Health + Streaming/CB/Grafana)**

**Odhad:** 27.5h  
**Priorita:** MEDIUM  
**Závislosti:** S12 (backup jobs), S13 (workflow instances), S10 (modelgen jobs)

#### Scope

**Frontend (`/admin/console`):**
- Tab: **Jobs**
  - Subtabs: Modelgen, Pre-Agg, Backup/Restore, Workflow Timers
  - List, detail, logs
  - Actions: retry, cancel
- Tab: **Health**
  - Micrometer/Actuator snapshot
  - Circuit breaker states
  - DB connection pool stats
  - Kafka lag
- Tab: **Streaming**
  - Refactor `StreamingDashboardPage.tsx` → tab
  - DLQ, queue depth, lag
- Tab: **Grafana**
  - Embed monitoring dashboards
- Tab: **Circuit Breakers**
  - List all CB (CubeClient, Kafka, DB)
  - State: CLOSED, OPEN, HALF_OPEN
  - Metrics: failure rate, slow call rate

**Backend:**
```
GET    /api/admin/jobs                # Agregovaný přehled
GET    /api/admin/jobs/{id}           # Detail + logs
POST   /api/admin/jobs/{id}/retry     # Retry failed job
POST   /api/admin/jobs/{id}/cancel    # Cancel running job

GET    /api/admin/health              # Custom agregace
```

**Implementace:**
```java
// AdminJobsController.java
@RestController
@RequestMapping("/api/admin/jobs")
@PreAuthorize("hasAuthority('CORE_ADMIN_CONSOLE')")
public class AdminJobsController {
    
    @Autowired private BackupJobRepository backupJobRepository;
    @Autowired private RestoreJobRepository restoreJobRepository;
    // TODO: Add PreAggJobRepository, ModelgenJobRepository
    
    @GetMapping
    public ResponseEntity<JobSummary> getJobs() {
        JobSummary summary = JobSummary.builder()
            .backup(backupJobRepository.findByStatusIn(List.of("pending", "running")))
            .restore(restoreJobRepository.findByStatusIn(List.of("pending", "running")))
            // .preAgg(preAggJobRepository.findByStatusIn(...))
            // .modelgen(modelgenJobRepository.findByStatusIn(...))
            .build();
        return ResponseEntity.ok(summary);
    }
}

// AdminHealthController.java
@RestController
@RequestMapping("/api/admin/health")
@PreAuthorize("hasAuthority('CORE_ADMIN_CONSOLE')")
public class AdminHealthController {
    
    @Autowired private HealthEndpoint healthEndpoint;
    @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @GetMapping
    public ResponseEntity<HealthSummary> getHealth() {
        HealthSummary summary = HealthSummary.builder()
            .actuator(healthEndpoint.health())
            .circuitBreakers(getCircuitBreakerStates())
            .kafkaLag(getKafkaLag())
            .build();
        return ResponseEntity.ok(summary);
    }
    
    private Map<String, String> getCircuitBreakerStates() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .collect(Collectors.toMap(
                CircuitBreaker::getName,
                cb -> cb.getState().name()
            ));
    }
}
```

**RBAC:**
- Keycloak role: `CORE_ADMIN_CONSOLE`

#### DoD

- [ ] FE: `/admin/console` dashboard (5 tabů)
- [ ] BE: 4 endpoints (jobs, health, retry, cancel)
- [ ] Refactor: `StreamingDashboardPage` → tab
- [ ] Dashboardy zobrazují aktuální stavy
- [ ] Odkazy na logy (Loki)
- [ ] Akce: retry/cancel fungují
- [ ] Docs: `docs/ADMIN_CONSOLE.md`
- [ ] PR: tail logů

---

### **S15 – Hardening, CI & Docs (celé EPIC)**

**Odhad:** 33.5h  
**Priorita:** CRITICAL  
**Závislosti:** S10, S11, S12, S13, S14

#### Scope

**E2E testy:**

1. **Studio:**
   ```typescript
   // frontend/src/test/e2e/studio.spec.ts
   test('Studio workflow', async ({ page }) => {
     // 1. Edit entity
     await page.goto('/admin/studio');
     await page.click('[data-testid="entity-user"]');
     await page.fill('[data-testid="field-name"]', 'updatedName');
     
     // 2. Validate
     await page.click('[data-testid="btn-validate"]');
     await expect(page.locator('[data-testid="validation-success"]')).toBeVisible();
     
     // 3. Preview
     await page.click('[data-testid="btn-preview"]');
     await expect(page.locator('[data-testid="preview-diff"]')).toBeVisible();
     
     // 4. Propose
     await page.click('[data-testid="btn-propose"]');
     await expect(page.locator('[data-testid="cr-created"]')).toBeVisible();
     
     // 5. Approve
     await page.click('[data-testid="btn-approve"]');
     await expect(page.locator('[data-testid="modelgen-success"]')).toBeVisible();
   });
   ```

2. **Config:**
   ```typescript
   test('Config edit with masking', async ({ page }) => {
     await page.goto('/admin/config');
     await page.fill('[data-testid="config-api-secret"]', 'new-secret-123');
     await page.click('[data-testid="btn-save"]');
     
     // Verify masked in audit
     await page.goto('/admin/config/audit');
     await expect(page.locator('[data-testid="audit-before"]')).toContainText('••••');
   });
   ```

3. **Backup/Restore:**
   ```typescript
   test('Backup and restore', async ({ page }) => {
     await page.goto('/admin/backup');
     await page.click('[data-testid="btn-create-backup"]');
     await page.selectOption('[data-testid="scope"]', 'tenant');
     await page.click('[data-testid="btn-confirm"]');
     
     // Wait for completion
     await page.waitForSelector('[data-testid="backup-completed"]', { timeout: 60000 });
     
     // Dry-run restore
     await page.click('[data-testid="btn-restore"]');
     await page.check('[data-testid="dry-run"]');
     await page.click('[data-testid="btn-confirm"]');
     await expect(page.locator('[data-testid="restore-dry-run-success"]')).toBeVisible();
   });
   ```

4. **Workflow:**
   ```typescript
   test('Workflow action with lock', async ({ page, context }) => {
     // User 1: Edit entity
     await page.goto('/entities/user/123');
     await page.click('[data-testid="btn-edit"]');
     
     // User 2: Try workflow action
     const page2 = await context.newPage();
     await page2.goto('/entities/user/123');
     await expect(page2.locator('[data-testid="workflow-actions"]')).toBeDisabled();
     await expect(page2.locator('[data-testid="stale-warning"]')).toBeVisible();
   });
   ```

**CI gates:**

```yaml
# .github/workflows/epic-admin-lifecycle.yml
name: EPIC Admin & Lifecycle
on:
  pull_request:
    branches: [main]
    paths:
      - 'backend/src/main/java/cz/muriel/core/admin/**'
      - 'frontend/src/pages/Admin/**'
      - 'docs/ADMIN_*.md'

jobs:
  todo-killer:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Kill TODOs in src
        run: |
          if grep -RIn "TODO:" backend/src frontend/src | grep -v "docs/" | grep -v "test/"; then
            echo "❌ Found TODOs in source code"
            exit 1
          fi

  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Frontend lint
        working-directory: frontend
        run: pnpm lint
      - name: Backend lint
        working-directory: backend
        run: ./mvnw -q -DskipTests verify

  e2e-admin:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Start services
        run: docker compose --profile admin up -d
      - name: Wait for services
        run: timeout 120 bash -c 'until curl -sf http://localhost:8080/actuator/health; do sleep 2; done'
      - name: Run E2E tests
        working-directory: frontend
        run: pnpm test:e2e --grep "admin|studio|config|backup|workflow"
      - name: Upload Playwright report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-report
          path: frontend/playwright-report/

  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: OWASP Dependency Check
        working-directory: backend
        run: ./mvnw verify -P security -DskipTests
      - name: ZAP Baseline Scan
        uses: zaproxy/action-baseline@v0.7.0
        with:
          target: 'http://localhost:8080'
      - name: Trivy vulnerability scan
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: 'backend/'
      - name: Secrets scan
        uses: trufflesecurity/trufflehog@main
        with:
          path: ./
          base: main
          head: HEAD

  coverage:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run tests with coverage
        working-directory: backend
        run: ./mvnw test jacoco:report
      - name: Check coverage (≥70%)
        working-directory: backend
        run: ./mvnw jacoco:check -Djacoco.haltOnFailure=true
```

**Docs aktualizace:**
- [ ] `docs/ADMIN_STUDIO.md`
- [ ] `docs/ENV_CONFIG.md`
- [ ] `docs/BACKUP_RESTORE.md`
- [ ] `docs/WORKFLOW.md`
- [ ] `docs/ADMIN_CONSOLE.md`
- [ ] `docs/OBSERVABILITY.md` (Micrometer/Grafana pro admin oblasti)
- [ ] `docs/AUDIT_TRAIL.md` (spec pro audit logging)
- [ ] Release notes

#### DoD

- [ ] 11 E2E scenarios passing
- [ ] CI gates: TODO killer, lint, E2E mandatory
- [ ] Security: 0 High/Critical z OWASP/ZAP/Trivy, secrets scan OK
- [ ] Coverage: ≥70% nový kód
- [ ] Docs: všechny 7 dokumentů aktualizované
- [ ] 0 TODO/warnings v `backend/src` + `frontend/src`
- [ ] PR: release notes + tail z CI jobů

---

## 📊 Celkový odhad

| PR | Oblast | Hodiny | Kumulativně |
|----|--------|--------|-------------|
| S10 | Metamodel Studio | 43.5h | 43.5h |
| S11 | Admin Config GUI | 24.5h | 68.0h |
| S12 | Archivace & Obnova | 31.5h | 99.5h |
| S13 | Workflow | 51.5h | 151.0h |
| S14 | Admin Console | 27.5h | 178.5h |
| S15 | Hardening & Tests | 33.5h | 212.0h |

**Total:** ~212 hodin (~5-6 týdnů při full-time práci)

---

## 🔗 Závislosti a doporučené pořadí

```
S10 (Studio) ──┐
               ├─→ S13 (Workflow - editor pattern)
S11 (Config) ──┘

S12 (Backup) ──→ S14 (Console - backup tab)

S13 (Workflow) ──→ S15 (E2E workflow tests)

S14 (Console) ──→ S15 (E2E console tests)

S10,S11,S12,S13,S14 ──→ S15 (Hardening všech)
```

**Doporučené pořadí:**
1. **S10** (Studio) – nastaví admin GUI patterns
2. **S11** (Config) + **S12** (Backup) – paralelně (bez závislostí)
3. **S13** (Workflow) – po S10 (použije editor pattern)
4. **S14** (Console) – po S12,S13 (agreguje joby)
5. **S15** (Hardening) – na konec (testuje vše)

---

## 🛡️ RBAC Summary

| Role | Scope | Endpoints |
|------|-------|-----------|
| `CORE_ADMIN_STUDIO` | Metamodel management | `/api/admin/studio/**` |
| `CORE_ADMIN_CONFIG` | Environment configuration | `/api/admin/config/**` |
| `CORE_ADMIN_BACKUP` | Backup/restore operations | `/api/admin/backup/**`, `/api/admin/restore/**` |
| `CORE_ADMIN_WORKFLOW` | Workflow definitions | `/api/admin/workflows/**` |
| `CORE_ADMIN_CONSOLE` | Ops monitoring | `/api/admin/jobs/**`, `/api/admin/health` |

**Keycloak realm update:**
- Přidat 5 rolí do `docker/keycloak/realm-admin.template.json`
- Export po konfiguraci

---

## 📚 Reference

**Existující kód k využití:**
- `MetamodelAdminController.java` – hot reload API
- `WorkflowService.java` – state management
- `StreamingDashboardPage.tsx` – dashboard template
- `MinIOProperties.java` – storage config
- `usePresence.ts` – lock pattern

**Dokumentace:**
- `METAMODEL_PHASE_2_3_COMPLETE.md` – hot reload
- `STREAMING_README.md` – monitoring patterns
- `REPORTING_OPERATIONS_RUNBOOK.md` – ops best practices
- `NAMING_GUIDE.md` – naming conventions
- `S7_COMPLETE.md` – Kafka retry patterns

**CI/CD vzory:**
- `.github/workflows/streaming-tests.yml` – testování
- `.github/workflows/naming-lint.yml` – lint gates

---

## ✅ Completion Criteria (EPIC)

- [ ] Studio/Config/Backup/Workflow/Admin Console hotové a otestované
- [ ] RBAC doplněné (5 rolí + realm export)
- [ ] CI zelené (TODO killer, lint, E2E, security, coverage)
- [ ] 0 TODO/warnings v source kódu
- [ ] Dokumentace aktuální (7 MD souborů)
- [ ] Release notes napsané
- [ ] Pro každý PR: krátký popis, seznam změn, DoD, odkazy na běhy CI

---

**Poznámka:** Tento plán je living document. Odhady se mohou měnit na základě complexity objevené během implementace.
