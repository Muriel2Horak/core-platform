# 🔄 Workflow + n8n - GAP Analýza (2025)

> ⚠️ **OBSOLETE:** Tento dokument byl nahrazen [`WORKFLOW_UNIFIED_ARCHITECTURE.md`](./WORKFLOW_UNIFIED_ARCHITECTURE.md)  
> ✅ **Použij:** Unified Architecture pro aktuální stav a roadmap  
> 📖 **Historie:** Tento audit byl proveden 8. listopadu 2025 a vedl k vytvoření sjednocené 2-vrstvé architektury

**Datum:** 8. listopadu 2025  
**Status:** 🔴 **ARCHIVED** (nahrazeno unified architecture)  
**Účel:** [ARCHIVED] Původní gap analýza která vedla ke sjednocení EPIC-006 + EPIC-011

---

## 🎯 EXECUTIVE SUMMARY

### Původní Vize (2 systémy)

1. **Core Workflow Engine** (EPIC-006):
   - Metamodel-integrated state machine
   - Deterministický, auditovatelný runtime
   - Typizované kroky: APPROVAL, SERVICE_REST_SYNC, KAFKA_COMMAND, EXTERNAL_TASK, TIMER/DELAY
   - Workflow Studio (React Flow editor)
   - Business procesy v platformě

2. **n8n Integration** (EPIC-011):
   - Externí orchestrátor pro integrace
   - AI pipelines, ETL, batch jobs
   - Komunikace přes Kafka/webhooks/REST
   - Keycloak SSO, multi-tenant aware

### Aktuální Stav (implementováno)

| Komponenta | Status | Implementace | Gap |
|------------|--------|--------------|-----|
| **W1-W12 Core Engine** | ✅ 100% | 18,000 LOC, 119 tests | **Chybí typizované kroky** |
| **Workflow Steps Editor** | ✅ 100% | S10-E complete | **Mock executors** |
| **n8n Deployment** | ⚠️ 0% | Není nasazeno | **Celý EPIC-011** |
| **Metamodel Integration** | ⚠️ Partial | Schema exists | **Runtime vazba chybí** |

### Kritické Nálezy

🔴 **CRITICAL GAPS**:
1. **Typizované kroky NEIMPLEMENTOVÁNY** - APPROVAL, REST_SYNC, KAFKA_COMMAND neexistují v runtime
2. **Workflow.steps[] je MOCK** - Dry-run pouze template substituci, žádná exekuce
3. **n8n NENÍ NASAZENÝ** - Žádný Docker service, Keycloak integrace, BFF API
4. **Workflow ↔ n8n integrace chybí** - EXTERNAL_TASK executor pattern neimplementován

🟡 **MEDIUM GAPS**:
5. **Workflow Studio nedokončený** - Editor existuje, ale není napojen na runtime execution
6. **SLA tracking partial** - W8 implementuje timers, ale ne SLA escalations
7. **AI pipeline orchestrace chybí** - Žádná integrace MCP → n8n → AI

✅ **GOOD NEWS**:
8. **Konektory NEPOTŘEBUJEME** - n8n má 400+ built-in nodes (Jira, Confluence, Trello, HTTP, atd.)

---

## 📊 EPIC-006: Workflow Engine - Detailní Audit

### Co MÁME (W1-W12 Complete)

#### ✅ W1: JSON Workflow Model
**Soubory:**
- `WorkflowDefinition.java` - Core model
- `WorkflowState.java`, `WorkflowTransition.java`
- `workflow-definition.yaml` - Schema examples

**Capabilities:**
- States: start, state, decision, loop, wait, end
- Transitions: from → to + guards + actions
- Guards: CEL expressions (partial support)
- Actions: String identifiers (no execution!)

**GAP:** Actions jsou jen stringy, není runtime executor!

---

#### ✅ W2: Persistence Layer
**Databáze:**
```sql
-- V1__init.sql
CREATE TABLE entity_state (
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  tenant_id TEXT NOT NULL,
  state_code TEXT NOT NULL,
  since TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE state_transition (
  entity_type TEXT NOT NULL,
  from_code TEXT,
  to_code TEXT NOT NULL,
  code TEXT NOT NULL,
  guard JSONB,
  sla_minutes INTEGER
);

CREATE TABLE entity_state_log (
  id BIGSERIAL PRIMARY KEY,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  from_code TEXT,
  to_code TEXT NOT NULL,
  transition_code TEXT NOT NULL,
  changed_by TEXT NOT NULL,
  changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Plus W9 Versioning:**
```sql
CREATE TABLE workflow_versions (
  id BIGSERIAL PRIMARY KEY,
  entity_type VARCHAR(100) NOT NULL,
  version INTEGER NOT NULL,
  schema_definition JSONB NOT NULL,
  created_by VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  is_active BOOLEAN NOT NULL DEFAULT false,
  migration_notes TEXT,
  CONSTRAINT uq_workflow_version UNIQUE (entity_type, version)
);

CREATE TABLE workflow_instance_versions (
  instance_id VARCHAR(100) NOT NULL,
  workflow_version_id BIGINT NOT NULL REFERENCES workflow_versions(id),
  migrated_at TIMESTAMP DEFAULT NOW(),
  migrated_by VARCHAR(100)
);
```

**Capabilities:**
- State storage per entity (entity_type + entity_id + tenant_id)
- Transition definitions with guards & SLA
- Audit log per transition
- Version tracking (W9)
- Migration history

**GAP:** Žádná tabulka pro workflow_instances (runtime execution context)!

---

#### ✅ W3-W4: React Flow Designer
**Frontend:**
- `WorkflowDesigner.tsx` - Visual editor
- `WorkflowGraph.tsx` - State machine visualization
- `WorkflowTimeline.tsx` - History view
- Node types: State, Decision, Loop, Wait, Start, End

**Capabilities:**
- Drag-and-drop nodes
- Edge configuration (guards, labels)
- JSON export/import
- Live preview

**GAP:** Editor je standalone, není napojený na execution runtime!

---

#### ✅ W5: Runtime Foundation
**Backend:**
- `WorkflowService.java` - State machine transitions
- `WorkflowEventPublisher.java` - Kafka events
- `WorkflowMetricsService.java` - Prometheus metrics
- `WorkflowModels.java` - EntityState, StateTransition, TransitionResult

**REST API:**
```java
GET /api/entities/{entityType}/{entityId}/state
GET /api/entities/{entityType}/{entityId}/transitions
POST /api/entities/{entityType}/{entityId}/transition/{transitionCode}
```

**Capabilities:**
- Current state retrieval
- Allowed transitions (with guard evaluation)
- Apply transition with validation
- SLA tracking (NONE, OK, WARN, BREACH)
- Event publishing (ENTER_STATE, EXIT_STATE, ACTION_APPLIED)
- Metrics tracking (duration, errors, SLA warnings)

**GAP:**
- Guards pouze simple role checks: `hasRole('ROLE_NAME')` - CEL expressions not supported!
- Actions not executed - pouze logged!
- Žádné automatické kroky (APPROVAL, REST call, atd.)

---

#### ✅ W6: Frontend UX
**Components:**
- `WorkflowGraph.tsx` - Real-time state visualization
- `WorkflowTimeline.tsx` - History timeline
- `WorkflowStatePanel.tsx` - Current state + allowed actions

**Features:**
- Visual state highlighting
- Transition history
- Allowed actions buttons
- SLA status indicators

**GAP:** UI volá pouze basic transitions, neumí trigger automatické kroky

---

#### ✅ W7: Node Executors (PARTIAL!)
**Soubory:**
- `WorkflowExecutor.java` - Interface (prázdný!)
- `HttpExecutor.java` - Placeholder (not implemented)
- `ScriptExecutor.java` - Placeholder (not implemented)
- `HumanExecutor.java` - Placeholder (not implemented)

**Co CHYBÍ:**
```java
// ❌ NEEXISTUJE IMPLEMENTACE!
public interface WorkflowExecutor {
  ExecutionResult execute(WorkflowContext context);
}

// ❌ APPROVAL executor
class ApprovalExecutor implements WorkflowExecutor {
  // single / allOf / anyOf / quorum
  // role-based, SLA, eskalace
}

// ❌ SERVICE_REST_SYNC executor
class RestSyncExecutor implements WorkflowExecutor {
  // OpenAPI client generation
  // retry, timeout, circuit breaker, idempotence
}

// ❌ KAFKA_COMMAND executor
class KafkaCommandExecutor implements WorkflowExecutor {
  // publish event, wait for response/callback
  // timeout + DLQ
}

// ❌ EXTERNAL_TASK executor
class ExternalTaskExecutor implements WorkflowExecutor {
  // poll & complete pattern
  // worker registration
}

// ❌ TIMER/DELAY executor
class TimerExecutor implements WorkflowExecutor {
  // schedule delayed actions
  // reminders, deadlines
}
```

**CRITICAL GAP:** W7 má pouze placeholdery, žádná reálná implementace!

---

#### ✅ W8: Timers & SLA (PARTIAL)
**Implementace:**
- `WorkflowTimerService.java` - Periodic timer check (každou minutu)
- `workflow_timers` table (pending/fired/cancelled)

**Capabilities:**
- SLA tracking per state (sla_minutes)
- Timer firing (SLA_WARNING, SLA_BREACH)
- Metrics on SLA violations

**GAP:**
- DELAY/TIMER kroky nejsou implementovány jako part workflow steps!
- Escalations not implemented
- Reminders not implemented

---

#### ✅ W9: Versioning
**Implementace:**
- `workflow_versions` table
- `workflow_instance_versions` table
- Migration strategies (IMMEDIATE, LAZY, MANUAL)
- REST API for version management

**Capabilities:**
- Version storage (JSONB schema)
- Activation/deactivation
- Instance migration tracking
- Migration history

**GAP:** Migration logic není provázaná s execution runtime

---

#### ✅ W10: Workflow Studio
**Implementace:**
- `MetamodelStudioPage.tsx` - Main UI
- `WorkflowStepsEditor.tsx` - Steps editor (S10-E)
- Diff/Propose/Approve workflow (S10-D)

**Capabilities:**
- Visual node editor
- Steps configuration (type, inputMap, retry, timeout)
- Validation (unique IDs, valid references)
- Dry-run testing (mock context)
- Proposal workflow (draft → review → approve)

**GAP:**
- Steps editor je UI-only, backend má jen mock validation/dry-run
- Žádná vazba na execution runtime
- OpenAPI/AsyncAPI reference parsing not implemented

---

#### ✅ W11: Testing & Simulation
**Implementace:**
- `WorkflowTestingService.java` - Dry-run mode

**Capabilities:**
- Transition simulation (no DB writes)
- Guard evaluation (simple rules)
- Mock data generation
- Test scenario playback

**GAP:** Dry-run pouze state transitions, ne workflow steps execution

---

#### ✅ W12: Monitoring (PARTIAL)
**Implementace:**
- `WorkflowMetricsService.java` - Prometheus metrics
- Grafana dashboards (planned, not created)

**Capabilities:**
- Transition counters
- State duration tracking
- SLA warning/breach counts
- Error rates

**GAP:**
- Grafana dashboards neexistují!
- Žádný real-time workflow instance dashboard
- Žádné alerting na stuck workflows

---

### ❌ Co CHYBÍ (GAPs v EPIC-006)

#### 1. Typizované Kroky (Executors)

**Požadavky z vize:**
- APPROVAL: single / allOf / anyOf / quorum, role-based, SLA, eskalace
- SERVICE_REST_SYNC: OpenAPI client, retry, timeout, circuit breaker, idempotence
- KAFKA_COMMAND: publish + wait for callback, timeout, DLQ
- EXTERNAL_TASK: poll & complete, worker registration
- TIMER/DELAY: scheduled actions, reminders, deadlines
- SCRIPT/EXPRESSION: simple CEL expressions (opatrně)

**Současný stav:** ŽÁDNÝ executor implementovaný! Pouze placeholders.

**Nové User Stories:**
- **WF12: APPROVAL Executor** (~800 LOC)
  - Single approver, allOf (unanimous), anyOf (first), quorum (threshold)
  - Role-based (Keycloak roles)
  - SLA tracking with escalations
  - Email notifications
  - Approval UI widget

- **WF13: REST_SYNC Executor** (~1,200 LOC)
  - OpenAPI spec parsing
  - HTTP client generation
  - Retry with exponential backoff
  - Circuit breaker integration
  - Timeout handling
  - Idempotent requests (correlation ID)
  - Error mapping

- **WF14: KAFKA_COMMAND Executor** (~600 LOC)
  - Kafka producer integration
  - Command/reply pattern (correlation ID)
  - Timeout with DLQ fallback
  - Event schema validation (AsyncAPI)

- **WF15: EXTERNAL_TASK Executor** (~800 LOC)
  - Worker registration endpoint
  - Poll tasks API
  - Complete/fail task API
  - Heartbeat mechanism
  - Worker timeout detection

- **WF16: TIMER/DELAY Executor** (~400 LOC)
  - Schedule delayed transitions
  - Reminder notifications
  - Deadline enforcement
  - Integration s WorkflowTimerService

---

#### 2. Workflow Runtime Instance Management

**Chybí:**
```sql
CREATE TABLE workflow_instances (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  entity_type VARCHAR(100) NOT NULL,
  entity_id VARCHAR(100) NOT NULL,
  tenant_id VARCHAR(100) NOT NULL,
  workflow_version_id BIGINT REFERENCES workflow_versions(id),
  status VARCHAR(50) NOT NULL, -- RUNNING, COMPLETED, FAILED, CANCELLED
  started_at TIMESTAMP NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMP,
  current_step_id VARCHAR(100),
  context JSONB, -- runtime variables
  error_message TEXT
);

CREATE TABLE workflow_step_executions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workflow_instance_id UUID REFERENCES workflow_instances(id),
  step_id VARCHAR(100) NOT NULL,
  step_type VARCHAR(50) NOT NULL, -- APPROVAL, REST_SYNC, etc.
  status VARCHAR(50) NOT NULL, -- PENDING, RUNNING, SUCCESS, FAILED, SKIPPED
  started_at TIMESTAMP,
  completed_at TIMESTAMP,
  input JSONB,
  output JSONB,
  error_message TEXT,
  retry_count INTEGER DEFAULT 0
);
```

**Nová US:**
- **WF17: Workflow Instance Runtime** (~1,500 LOC)
  - Create instance on workflow start
  - Track current step
  - Store execution context (variables)
  - Step execution log
  - Error handling & retry
  - Completion detection

---

#### 3. Workflow Steps Deklarativní Definice v Metamodelu

**Současný stav:**
- `WorkflowStepsEditor.tsx` má UI pro workflow.steps[]
- Backend má mock validation/dry-run
- **Ale:** Žádná storage, žádná execution!

**Chybí:**
```json
// V metamodel entity definition:
{
  "entityName": "Order",
  "workflow": {
    "enabled": true,
    "states": [...],
    "transitions": [...],
    "steps": [ // ❌ TOTO CHYBÍ V RUNTIME!
      {
        "id": "step-1",
        "type": "APPROVAL",
        "config": {
          "approvalType": "allOf",
          "roles": ["ROLE_MANAGER"],
          "slaMinutes": 60
        },
        "onSuccess": "step-2",
        "onError": "step-error"
      },
      {
        "id": "step-2",
        "type": "REST_SYNC",
        "config": {
          "openapiRef": "/api/inventory#POST_allocate",
          "inputMap": {
            "orderId": "${entityId}",
            "items": "${items}"
          },
          "retry": {
            "maxAttempts": 3,
            "backoffMultiplier": 2.0
          }
        }
      }
    ]
  }
}
```

**Nová US:**
- **WF18: Workflow Steps Schema v Metamodel** (~600 LOC)
  - Rozšířit entity schema o workflow.steps[]
  - Validace step types (APPROVAL, REST_SYNC, atd.)
  - Validace references (onSuccess, onError)
  - Storage do workflow_versions JSONB

---

#### 4. Konektory - ✅ **DELEGOVÁNO NA n8n!**

**Požadavky z vize:**
- Generic REST from OpenAPI (codegen + templating)
- Jira connector (create issue, update, comment)
- Confluence connector (create page, update)
- Trello connector (create card, move)
- Secrets management (vault integration)
- RBAC per connector
- Audit log

**✅ ŘEŠENÍ: n8n má built-in 400+ konektorů!**

n8n poskytuje out-of-the-box:
- **Jira Node:** Create/update issues, transitions, comments, attachments
- **Confluence Node:** Create/update pages, add comments
- **Trello Node:** Create/move cards, add members, labels
- **Generic HTTP Request Node:** Volání libovolného REST API
- **Webhook Node:** Příjem webhooků z externích systémů
- **400+ dalších konektorů:** Slack, Gmail, Google Sheets, Airtable, Notion, atd.

**Co NEPOTŘEBUJEME implementovat:**
- ❌ ~~WF19: Generic REST Connector~~ → n8n HTTP Request node
- ❌ ~~WF20: Jira Connector~~ → n8n Jira node
- ❌ ~~WF21: Confluence Connector~~ → n8n Confluence node
- ❌ ~~WF22: Trello Connector~~ → n8n Trello node

**Co POTŘEBUJEME (workflow integrace):**
- ✅ **WF15: EXTERNAL_TASK Executor** - Core workflow deleguje na n8n
- ✅ **N8N6: Backend BFF API** - Core volá n8n workflows via REST
- ✅ **N8N4: Workflow Templates** - Pre-built n8n flows pro Jira/Confluence/Trello

**Výhoda:**
- 🚀 Žádný custom kód pro konektory
- 🔄 n8n má aktualizace konektorů (nové API features)
- 🎨 Visual workflow builder pro business users
- 🔌 Plug & play integrace

---

#### 5. Observability & Monitoring Dashboards

**Současný stav:**
- Metrics se publikují (Prometheus)
- **Ale:** Žádné Grafana dashboards!

**Nová US:**
- **WF23: Workflow Grafana Dashboards** (~300 LOC)
  - Dashboard "Workflow Overview": instance counts, success/fail rates
  - Dashboard "Workflow Details": per-workflow metrics, step durations
  - Dashboard "SLA Monitoring": warnings, breaches, escalations
  - Alerting rules: stuck workflows (>24h), high error rate (>10%)

---

## 📊 EPIC-011: n8n Integration - Detailní Audit

### Co MÁME

**Aktuální stav:** ❌ **NIČEHO implementováno!**

EPIC-011 má pouze backlog stories (N8N1-N8N6), žádná implementace.

---

### ❌ Co CHYBÍ (Celý EPIC-011)

#### Story Overview

| ID | Story | Status | LOC Estimate | Effort |
|----|-------|--------|--------------|--------|
| N8N1 | n8n Platform Deployment | ❌ TODO | ~400 | 4h |
| N8N2 | Keycloak SSO Integration | ❌ TODO | ~300 | 4h |
| N8N3 | Nginx Reverse Proxy | ❌ TODO | ~200 | 2h |
| N8N4 | Workflow Templates & Docs | ❌ TODO | ~500 | 8h |
| N8N5 | Monitoring & Alerting | ❌ TODO | ~400 | 4h |
| N8N6 | Backend BFF API | ❌ TODO | ~800 | 12h |

**Total Effort:** ~34 hours (~5 developer days)

---

#### N8N1: n8n Platform Deployment (~400 LOC, 4h)

**Goal:** Deploy n8n Community Edition with PostgreSQL backend

**Deliverables:**
- Docker Compose service definition:
  ```yaml
  n8n:
    image: n8nio/n8n:latest
    environment:
      - N8N_BASIC_AUTH_ACTIVE=false  # SSO via Keycloak
      - N8N_EXTERNAL_HOOK_FILES=/hooks/keycloak-sso.js
      - WEBHOOK_URL=https://admin.core-platform.local/n8n/webhook
      - DB_TYPE=postgresdb
      - DB_POSTGRESDB_HOST=core-db
      - DB_POSTGRESDB_PORT=5432
      - DB_POSTGRESDB_DATABASE=n8n
      - DB_POSTGRESDB_USER=n8n_user
      - DB_POSTGRESDB_PASSWORD=${N8N_DB_PASSWORD}
    volumes:
      - n8n_data:/home/node/.n8n
      - ./docker/n8n/hooks:/hooks
    ports:
      - "5678:5678"  # Internal only
    depends_on:
      - core-db
  ```

- PostgreSQL database creation:
  ```sql
  CREATE USER n8n_user WITH PASSWORD '${N8N_DB_PASSWORD}';
  CREATE DATABASE n8n OWNER n8n_user;
  GRANT ALL PRIVILEGES ON DATABASE n8n TO n8n_user;
  ```

- Environment variables:
  ```bash
  # .env.template
  N8N_DB_PASSWORD=<strong-random-password>
  N8N_WEBHOOK_URL=https://admin.core-platform.local/n8n/webhook
  ```

**Acceptance Criteria:**
- ✅ n8n accessible at http://n8n:5678 (internal network)
- ✅ PostgreSQL stores workflow definitions and execution history
- ✅ Webhooks functional for external integrations

---

#### N8N2: Keycloak SSO Integration (~300 LOC, 4h)

**Goal:** Configure Keycloak client for n8n SSO authentication

**Deliverables:**
- Keycloak client creation (realm-admin.template.json):
  ```json
  {
    "clientId": "n8n-client",
    "name": "n8n Workflow Automation",
    "protocol": "openid-connect",
    "publicClient": false,
    "secret": "${N8N_CLIENT_SECRET}",
    "redirectUris": [
      "https://admin.${DOMAIN}/n8n/*",
      "https://admin.${DOMAIN}/n8n/callback"
    ],
    "webOrigins": [
      "https://admin.${DOMAIN}"
    ],
    "defaultClientScopes": ["email", "profile", "roles"]
  }
  ```

- Client roles:
  ```json
  {
    "roles": {
      "client": {
        "n8n-client": [
          { "name": "n8n-users", "description": "Can view and execute workflows" },
          { "name": "n8n-admins", "description": "Full workflow management" }
        ]
      }
    }
  }
  ```

- n8n SSO hook (`docker/n8n/hooks/keycloak-sso.js`):
  ```javascript
  module.exports = {
    async authenticate(req, res) {
      // Redirect to Keycloak login
      const keycloakUrl = process.env.KEYCLOAK_BASE_URL;
      const clientId = process.env.N8N_CLIENT_ID;
      const redirectUri = process.env.N8N_REDIRECT_URI;
      
      res.redirect(`${keycloakUrl}/realms/admin/protocol/openid-connect/auth?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code`);
    },
    
    async callback(req, res) {
      // Exchange code for JWT token
      // Validate token with Keycloak
      // Set n8n session cookie
    }
  };
  ```

**Acceptance Criteria:**
- ✅ Users redirected to Keycloak login when accessing /n8n
- ✅ Successful login grants access to n8n UI
- ✅ Logout from n8n also logs out from Keycloak

---

#### N8N3: Nginx Reverse Proxy Configuration (~200 LOC, 2h)

**Goal:** Configure Nginx to proxy /n8n/* to n8n service

**Deliverables:**
- Nginx configuration (`docker/nginx/nginx-ssl.conf.template`):
  ```nginx
  # n8n Workflow Automation
  location /n8n/ {
      proxy_pass http://n8n:5678/;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
      
      # WebSocket support for n8n editor
      proxy_http_version 1.1;
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection "upgrade";
      
      # Timeouts for long-running workflows
      proxy_read_timeout 300s;
      proxy_send_timeout 300s;
  }
  
  # n8n webhooks (separate path for external triggers)
  location /n8n/webhook/ {
      proxy_pass http://n8n:5678/webhook/;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
  }
  ```

**Acceptance Criteria:**
- ✅ n8n UI accessible at https://admin.core-platform.local/n8n/
- ✅ WebSocket connections work (live workflow editor updates)
- ✅ Webhook endpoints functional at /n8n/webhook/{workflowId}

---

#### N8N4: Workflow Templates & Documentation (~500 LOC, 8h)

**Goal:** Create reusable workflow templates and integration documentation

**Deliverables:**
- Workflow templates (JSON exports):
  1. **Jira Integration Template**:
     - Trigger: Webhook from core-platform (order created)
     - Action: Create Jira ticket
     - Action: Add comment with order details
  
  2. **Slack Notification Template**:
     - Trigger: Kafka event (user registered)
     - Action: Send Slack message to #onboarding channel
  
  3. **AI Pipeline Template**:
     - Trigger: HTTP webhook
     - Action: Call OpenAI API
     - Action: Store result in core-platform DB (via REST)
  
  4. **ETL Template**:
     - Trigger: Cron (daily at 2 AM)
     - Action: Fetch data from external API
     - Action: Transform JSON
     - Action: POST to core-platform BFF

- Documentation (`docs/n8n/INTEGRATION_GUIDE.md`):
  ```markdown
  # n8n Integration Guide
  
  ## Authentication
  - n8n uses Keycloak SSO (admin realm)
  - Roles: n8n-users (read/execute), n8n-admins (full access)
  
  ## Triggering n8n Workflows from Core Platform
  
  ### Option 1: Kafka Events
  - n8n listens to Kafka topics: `core.orders`, `core.users`
  - Configure Kafka trigger node with bootstrap servers
  
  ### Option 2: Webhooks
  - Workflow webhook URL: `/n8n/webhook/{workflowId}`
  - POST JSON payload from backend
  
  ### Option 3: REST API
  - n8n REST API: `/n8n/api/v1/workflows/{id}/execute`
  - Requires JWT token from Keycloak
  
  ## Calling Core Platform from n8n
  
  ### Backend BFF API
  - Use HTTP Request node
  - URL: `https://admin.core-platform.local/api/n8n/...`
  - Auth: JWT Bearer token (from workflow context)
  
  ## Multi-Tenancy
  - Pass `tenantId` in workflow payload
  - BFF validates tenant access
  - n8n workflows run in shared instance (tenant isolation via data, not infra)
  ```

**Acceptance Criteria:**
- ✅ 4+ workflow templates importable into n8n
- ✅ Integration guide covers Kafka, webhooks, REST API
- ✅ Multi-tenant patterns documented

---

#### N8N5: Monitoring & Alerting Integration (~400 LOC, 4h)

**Goal:** Monitor n8n health and workflow execution metrics

**Deliverables:**
- Prometheus exporter for n8n:
  - Metrics: workflow_executions_total, workflow_errors_total, workflow_duration_seconds
  - Endpoint: `/n8n/metrics` (if supported) or custom exporter

- Grafana dashboard (`monitoring/grafana/dashboards/n8n-dashboard.json`):
  - Panel: Workflow Executions (last 24h)
  - Panel: Error Rate (%)
  - Panel: Average Execution Duration
  - Panel: Active Workflows Count

- Alerting rules (`monitoring/prometheus/alerts/n8n-alerts.yml`):
  ```yaml
  groups:
    - name: n8n_alerts
      rules:
        - alert: N8nHighErrorRate
          expr: rate(workflow_errors_total[5m]) > 0.1
          for: 5m
          annotations:
            summary: "n8n workflow error rate above 10%"
        
        - alert: N8nServiceDown
          expr: up{job="n8n"} == 0
          for: 2m
          annotations:
            summary: "n8n service is down"
  ```

**Acceptance Criteria:**
- ✅ n8n metrics scraped by Prometheus
- ✅ Grafana dashboard shows workflow execution stats
- ✅ Alerts fire on high error rate or service down

---

#### N8N6: Backend BFF API (~800 LOC, 12h)

**Goal:** Spring Boot BFF proxy for n8n REST API with JWT validation and caching

**Deliverables:**
- REST controller (`N8nBffController.java`):
  ```java
  @RestController
  @RequestMapping("/api/n8n")
  @RequiredArgsConstructor
  @Slf4j
  public class N8nBffController {
    
    private final WebClient n8nClient;
    private final CacheManager cacheManager;
    
    /**
     * List workflows (cached 5 minutes)
     */
    @GetMapping("/workflows")
    @Cacheable("n8n-workflows")
    @PreAuthorize("hasAuthority('n8n-users')")
    public ResponseEntity<List<Map<String, Object>>> listWorkflows(Authentication auth) {
      String tenantId = extractTenantId(auth);
      
      List<Map<String, Object>> workflows = n8nClient.get()
        .uri("/api/v1/workflows")
        .header("Authorization", "Bearer " + getN8nAdminToken())
        .retrieve()
        .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
        .collectList()
        .block();
      
      // Filter workflows by tenant (if workflow has tenantId tag)
      return ResponseEntity.ok(
        workflows.stream()
          .filter(w -> matchesTenant(w, tenantId))
          .collect(Collectors.toList())
      );
    }
    
    /**
     * Execute workflow
     */
    @PostMapping("/workflows/{workflowId}/execute")
    @PreAuthorize("hasAuthority('n8n-users')")
    public ResponseEntity<Map<String, Object>> executeWorkflow(
      @PathVariable String workflowId,
      @RequestBody Map<String, Object> payload,
      Authentication auth
    ) {
      String tenantId = extractTenantId(auth);
      String userId = extractUserId(auth);
      
      // Inject tenant context
      payload.put("tenantId", tenantId);
      payload.put("userId", userId);
      
      Map<String, Object> result = n8nClient.post()
        .uri("/api/v1/workflows/{id}/execute", workflowId)
        .header("Authorization", "Bearer " + getN8nAdminToken())
        .bodyValue(payload)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
        .block();
      
      // Audit log
      log.info("n8n workflow executed: workflowId={}, tenantId={}, userId={}", 
               workflowId, tenantId, userId);
      
      return ResponseEntity.ok(result);
    }
    
    /**
     * Get workflow execution status
     */
    @GetMapping("/executions/{executionId}")
    @PreAuthorize("hasAuthority('n8n-users')")
    public ResponseEntity<Map<String, Object>> getExecution(
      @PathVariable String executionId,
      Authentication auth
    ) {
      // Similar pattern - proxy to n8n API with tenant validation
    }
  }
  ```

- WebClient configuration (`N8nWebClientConfig.java`):
  ```java
  @Configuration
  public class N8nWebClientConfig {
    
    @Value("${n8n.base-url:http://n8n:5678}")
    private String n8nBaseUrl;
    
    @Bean("n8nClient")
    public WebClient n8nWebClient() {
      return WebClient.builder()
        .baseUrl(n8nBaseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
    }
  }
  ```

- Cache configuration (`application.yml`):
  ```yaml
  spring:
    cache:
      cache-names:
        - n8n-workflows
      caffeine:
        spec: maximumSize=100,expireAfterWrite=5m
  ```

**Acceptance Criteria:**
- ✅ GET /api/n8n/workflows returns tenant-filtered workflows
- ✅ POST /api/n8n/workflows/{id}/execute triggers workflow with tenant context
- ✅ Workflows cached for 5 minutes
- ✅ JWT validation via Keycloak
- ✅ Audit logging per workflow execution

---

## 🔗 Integrace Workflow Engine + n8n

### Vzorový Use Case: Order Approval with External Notifications

**Scénář:**
1. User submits Order (core-platform)
2. Core workflow engine přejde do stavu `PENDING_APPROVAL`
3. **Krok APPROVAL** vyvolá n8n workflow:
   - n8n pošle Slack notifikaci managerovi
   - n8n vytvoří Jira ticket
   - n8n čeká na odpověď (webhook callback)
4. Manager schválí v Jira → webhook → n8n → core-platform
5. Core workflow přejde do `APPROVED`
6. **Krok REST_SYNC** zavolá inventory service (alokace zásob)
7. **Krok KAFKA_COMMAND** publikuje `order.approved` event
8. Workflow končí v `COMPLETED`

**Implementace:**

```json
// Order workflow definition (core-platform metamodel)
{
  "entityName": "Order",
  "workflow": {
    "states": [
      { "code": "draft", "label": "Draft" },
      { "code": "pending_approval", "label": "Pending Approval" },
      { "code": "approved", "label": "Approved" },
      { "code": "completed", "label": "Completed" }
    ],
    "transitions": [
      {
        "from": "draft",
        "to": "pending_approval",
        "code": "SUBMIT",
        "guard": { "expression": "hasRole('ROLE_USER')" }
      },
      {
        "from": "pending_approval",
        "to": "approved",
        "code": "APPROVE",
        "guard": { "expression": "hasRole('ROLE_MANAGER')" }
      }
    ],
    "steps": [
      {
        "id": "step-approval-notify",
        "type": "EXTERNAL_TASK",
        "config": {
          "taskType": "n8n-approval-flow",
          "endpoint": "https://admin.core-platform.local/api/n8n/workflows/approval-notify/execute",
          "inputMap": {
            "orderId": "${entityId}",
            "amount": "${amount}",
            "requester": "${createdBy}"
          },
          "timeoutMinutes": 60
        },
        "onSuccess": "step-allocate",
        "onError": "step-error"
      },
      {
        "id": "step-allocate",
        "type": "REST_SYNC",
        "config": {
          "openapiRef": "/api/inventory#POST_allocate",
          "inputMap": {
            "orderId": "${entityId}",
            "items": "${items}"
          },
          "retry": {
            "maxAttempts": 3,
            "backoffMultiplier": 2.0
          }
        },
        "onSuccess": "step-publish-event"
      },
      {
        "id": "step-publish-event",
        "type": "KAFKA_COMMAND",
        "config": {
          "topic": "core.orders",
          "eventType": "order.approved",
          "payload": {
            "orderId": "${entityId}",
            "tenantId": "${tenantId}",
            "approvedAt": "${now()}"
          }
        }
      }
    ]
  }
}
```

**n8n Workflow (approval-notify):**
```json
{
  "name": "Order Approval Notification",
  "nodes": [
    {
      "type": "n8n-nodes-base.webhook",
      "name": "Webhook Trigger",
      "parameters": {
        "path": "approval-notify"
      }
    },
    {
      "type": "n8n-nodes-base.slack",
      "name": "Send Slack Message",
      "parameters": {
        "channel": "#approvals",
        "text": "Order {{$json.orderId}} needs approval (amount: {{$json.amount}})"
      }
    },
    {
      "type": "n8n-nodes-base.jira",
      "name": "Create Jira Ticket",
      "parameters": {
        "project": "OPS",
        "issueType": "Task",
        "summary": "Approve Order {{$json.orderId}}",
        "description": "Requester: {{$json.requester}}\nAmount: {{$json.amount}}"
      }
    },
    {
      "type": "n8n-nodes-base.wait",
      "name": "Wait for Approval",
      "parameters": {
        "resume": "webhook",
        "webhookPath": "approval-callback/{{$json.orderId}}"
      }
    },
    {
      "type": "n8n-nodes-base.httpRequest",
      "name": "Callback to Core Platform",
      "parameters": {
        "url": "https://admin.core-platform.local/api/entities/Order/{{$json.orderId}}/transition/APPROVE",
        "method": "POST"
      }
    }
  ]
}
```

---

## 📝 Nové User Stories - Kompletní Seznam

### EPIC-006: Workflow Engine - Doplnění

| ID | Story | Estimate | Priority | Dependencies |
|----|-------|----------|----------|--------------|
| **WF12** | APPROVAL Executor | 800 LOC, 3 days | 🔴 HIGH | W5, W7 |
| **WF13** | REST_SYNC Executor | 1,200 LOC, 5 days | 🔴 HIGH | W5, W7 |
| **WF14** | KAFKA_COMMAND Executor | 600 LOC, 2 days | 🔴 HIGH | W5, W7, Kafka |
| **WF15** | EXTERNAL_TASK Executor | 800 LOC, 3 days | 🟡 MEDIUM | W5, W7 |
| **WF16** | TIMER/DELAY Executor | 400 LOC, 2 days | 🟡 MEDIUM | W5, W8 |
| **WF17** | Workflow Instance Runtime | 1,500 LOC, 6 days | 🔴 HIGH | W5, WF12-16 |
| **WF18** | Workflow Steps Schema v Metamodel | 600 LOC, 3 days | 🔴 HIGH | META, W10 |
| **WF19** | Generic REST Connector | 1,500 LOC, 6 days | 🟡 MEDIUM | WF13 |
| **WF20** | Jira Connector | 800 LOC, 3 days | 🟢 LOW | WF19 |
| **WF21** | Confluence Connector | 600 LOC, 2 days | 🟢 LOW | WF19 |
| **WF22** | Trello Connector | 600 LOC, 2 days | 🟢 LOW | WF19 |
| **WF23** | Workflow Grafana Dashboards | 300 LOC, 1 day | 🟡 MEDIUM | W12, Grafana |

**Total Estimate:** ~9,700 LOC, ~38 developer days

---

### EPIC-011: n8n Integration - Kompletní Implementace

| ID | Story | Estimate | Priority | Dependencies |
|----|-------|----------|----------|--------------|
| **N8N1** | n8n Platform Deployment | 400 LOC, 1 day | 🔴 HIGH | PostgreSQL |
| **N8N2** | Keycloak SSO Integration | 300 LOC, 1 day | 🔴 HIGH | Keycloak, N8N1 |
| **N8N3** | Nginx Reverse Proxy | 200 LOC, 0.5 day | 🔴 HIGH | Nginx, N8N1 |
| **N8N4** | Workflow Templates & Docs | 500 LOC, 2 days | 🟡 MEDIUM | N8N1-3 |
| **N8N5** | Monitoring & Alerting | 400 LOC, 1 day | 🟡 MEDIUM | Prometheus, Grafana |
| **N8N6** | Backend BFF API | 800 LOC, 3 days | 🔴 HIGH | N8N1-3 |

**Total Estimate:** ~2,600 LOC, ~8.5 developer days

---

## 🎯 Implementační Roadmap

### Phase 1: Workflow Executors (Priority 1) - 3 weeks
**WF12-WF16:** Implementovat typizované kroky

**Výstup:**
- ✅ APPROVAL executor (single, allOf, anyOf, quorum)
- ✅ REST_SYNC executor (OpenAPI, retry, circuit breaker)
- ✅ KAFKA_COMMAND executor (publish + callback)
- ✅ EXTERNAL_TASK executor (poll & complete)
- ✅ TIMER/DELAY executor

**Testy:**
- Unit tests per executor type
- Integration tests s real Kafka, HTTP server
- E2E test: Order approval workflow

---

### Phase 2: Workflow Runtime (Priority 1) - 2 weeks
**WF17:** Workflow Instance Management

**Výstup:**
- ✅ workflow_instances table
- ✅ workflow_step_executions table
- ✅ Runtime orchestration (step-by-step execution)
- ✅ Error handling & retry
- ✅ Completion detection

**Testy:**
- Integration tests s workflow instance lifecycle
- E2E test: Multi-step workflow execution

---

### Phase 3: n8n Deployment (Priority 1) - 1 week
**N8N1-N8N3:** Základní nasazení

**Výstup:**
- ✅ n8n Docker service
- ✅ PostgreSQL database
- ✅ Keycloak SSO
- ✅ Nginx proxy
- ✅ Přístup na https://admin.core-platform.local/n8n/

**Testy:**
- Smoke test: n8n login via Keycloak
- Smoke test: Create simple workflow in n8n UI

---

### Phase 4: n8n BFF API (Priority 1) - 1 week
**N8N6:** Backend integrace

**Výstup:**
- ✅ Spring Boot BFF controller
- ✅ WebClient to n8n API
- ✅ JWT validation
- ✅ Tenant filtering
- ✅ Audit logging

**Testy:**
- Unit tests: BFF endpoints
- Integration test: Trigger n8n workflow from backend

---

### Phase 5: Workflow ↔ n8n Integration (Priority 2) - 2 weeks
**WF18 + Integration Pattern:**

**Výstup:**
- ✅ Workflow steps schema v metamodel
- ✅ EXTERNAL_TASK executor calls n8n workflows
- ✅ n8n callback webhook to core-platform
- ✅ End-to-end Order approval use case

**Testy:**
- E2E test: Order submission → n8n notification → approval → completion

---

### Phase 6: Connectors (Priority 3) - 4 weeks
**WF19-WF22:** Generic REST + Jira/Confluence/Trello

**Výstup:**
- ✅ OpenAPI client codegen
- ✅ Jira REST API connector
- ✅ Confluence REST API connector
- ✅ Trello REST API connector
- ✅ Secrets management (per-tenant credentials)

**Testy:**
- Integration tests with real Jira/Confluence/Trello (sandbox accounts)

---

### Phase 7: Observability (Priority 3) - 1 week
**WF23 + N8N5:** Monitoring & Dashboards

**Výstup:**
- ✅ Grafana dashboards (Workflow + n8n)
- ✅ Alerting rules
- ✅ Prometheus metrics scraping

**Testy:**
- Manual: Verify dashboards show real data
- Alert test: Trigger high error rate → verify alert fires

---

## 📋 Acceptance Criteria - Kompletní Systém

### Workflow Engine (Core)

**State Management:**
- ✅ Entity má aktuální state (entity_state table)
- ✅ Transitions s guards & SLA (state_transition table)
- ✅ Audit log všech změn (entity_state_log)

**Typed Steps Execution:**
- ✅ APPROVAL krok: Manager schválí v UI, workflow pokračuje
- ✅ REST_SYNC krok: HTTP POST na inventory API, retry on failure
- ✅ KAFKA_COMMAND krok: Publish event, wait for callback
- ✅ EXTERNAL_TASK krok: Delegace na n8n, timeout detection
- ✅ TIMER/DELAY krok: Scheduled action za X minut

**Workflow Studio:**
- ✅ Visual editor (React Flow) pro workflow states
- ✅ Steps editor pro workflow.steps[]
- ✅ Validation: Unique IDs, valid references, reasonable retry values
- ✅ Dry-run: Test workflow s mock data
- ✅ Diff/Propose/Approve: Changes go through review

**Runtime:**
- ✅ Workflow instance tracking (workflow_instances)
- ✅ Step execution log (workflow_step_executions)
- ✅ Error handling: Retry s exponential backoff, DLQ fallback
- ✅ Completion detection: All steps SUCCESS → mark instance COMPLETED

**Observability:**
- ✅ Grafana dashboard: Workflow overview (instance counts, success/fail rates)
- ✅ Grafana dashboard: Workflow details (per-workflow metrics, step durations)
- ✅ Alerting: Stuck workflows (>24h), high error rate (>10%)

---

### n8n Integration

**Deployment:**
- ✅ n8n accessible at https://admin.core-platform.local/n8n/
- ✅ Keycloak SSO: Login → redirect to Keycloak → return to n8n
- ✅ PostgreSQL backend: Workflows persisted

**Backend BFF:**
- ✅ GET /api/n8n/workflows: List workflows (tenant-filtered, cached 5min)
- ✅ POST /api/n8n/workflows/{id}/execute: Trigger workflow with tenant context
- ✅ GET /api/n8n/executions/{id}: Get execution status
- ✅ JWT validation: Only authenticated users with n8n-users role
- ✅ Audit log: Every workflow execution logged

**Workflow Templates:**
- ✅ Jira Integration: Order created → create Jira ticket
- ✅ Slack Notification: User registered → send Slack message
- ✅ AI Pipeline: HTTP webhook → OpenAI API → store result
- ✅ ETL: Daily cron → fetch external data → POST to core-platform

**Monitoring:**
- ✅ Grafana dashboard: n8n executions, error rate, duration
- ✅ Alerting: n8n service down, high error rate

---

### Integration (Workflow ↔ n8n)

**Use Case: Order Approval**
- ✅ User submits Order → workflow přejde `PENDING_APPROVAL`
- ✅ EXTERNAL_TASK krok triggers n8n workflow (approval-notify)
- ✅ n8n pošle Slack message + creates Jira ticket
- ✅ Manager schválí v Jira → webhook → n8n → callback to core-platform
- ✅ Workflow přejde `APPROVED` → REST_SYNC allocates inventory
- ✅ KAFKA_COMMAND publishes `order.approved` event
- ✅ Workflow instance marked COMPLETED

---

## 📌 Doporučení

### Priority 1 (MUST HAVE - Q1 2026)
1. **WF12-WF16:** Typed executors (bez nich workflow engine je nefunkční)
2. **WF17:** Workflow instance runtime (bez toho nejde orchestrace)
3. **N8N1-N8N3:** n8n deployment (základní integrace)
4. **N8N6:** BFF API (backend integrace)

**Estimate:** ~7 týdnů (1 senior dev full-time)

---

### Priority 2 (SHOULD HAVE - Q2 2026)
5. **WF18:** Workflow steps schema v metamodel (deklarativní definice)
6. **N8N4:** Workflow templates (reusable patterns)
7. **Phase 5:** Workflow ↔ n8n integration (end-to-end use case)
8. **WF23:** Grafana dashboards (observability)

**Estimate:** ~4 týdny

---

### Priority 3 (NICE TO HAVE - Q3 2026)
9. **WF19-WF22:** Connectors (Jira, Confluence, Trello)
10. **N8N5:** n8n monitoring
11. **Advanced:** AI pipeline orchestrace (MCP → n8n → AI)

**Estimate:** ~6 týdnů

---

## 🎓 Lessons Learned

### Co fungovalo dobře ✅
- **Workflow State Machine (W1-W5):** Solidní základ, čistá separace concerns
- **Versioning (W9):** Předvídavé, umožní evoluci workflow bez breaking changes
- **React Flow Designer (W3-W4):** UX je skvělá, drag-and-drop intuitivní
- **Incremental approach:** 12 fází (W1-W12) umožnilo iterativní delivery

### Co nefungovalo ❌
- **Executors (W7):** Podcenili jsme komplexitu - vznikly jen placeholdery
- **Steps editor (S10-E):** UI hotové, ale backend jen mock - chybí storage & execution
- **n8n (EPIC-011):** Kompletně ignorováno - zřejmě priorita jinde
- **Integration testing:** Unit tests OK, ale chybí E2E workflow tests

### Co bychom příště udělali jinak 🔄
1. **Start with executors:** W7 mělo být priority 1, ne až W7
2. **E2E test-driven:** Definovat end-to-end use case PŘED implementací
3. **n8n earlier:** Integrovat n8n do plánu od začátku, ne jako separate EPIC
4. **Mock less:** Dry-run a validation je fajn, ale nesmí nahradit real execution

---

**END OF AUDIT**
