# 🔄 Workflow Engine - Unified Architecture (EPIC-006 + EPIC-011)

**Datum:** 8. listopadu 2025  
**Autor:** Systémový architekt  
**Účel:** Sjednocení 2 EPICů do koherentní 2-vrstvé architektury

---

## 🎯 Executive Summary

**Problém:**  
Máme **2 oddělené EPICy** (EPIC-006 Internal Workflow, EPIC-011 n8n External), které **jdou proti sobě** - stories nejsou koordinované, chybí jasná hranice zodpovědnosti, není definovaný integration pattern.

**Řešení:**  
**JEDEN EPIC "Workflow Orchestration" s 2 vrstvami:**

```
┌─────────────────────────────────────────────────────────────────────┐
│  VRSTVA 1: INTERNAL WORKFLOW ENGINE (metamodel-driven)              │
│  - Core state machine (states, transitions, guards, SLA)            │
│  - Typed executors: APPROVAL, REST_SYNC, KAFKA_COMMAND, TIMER       │
│  - Workflow steps orchestration (sequential execution)              │
│  - Audit trail, versioning, monitoring                              │
│  - Scope: CORE business procesy (Order approval, Invoice lifecycle) │
└─────────────────────────────────────────────────────────────────────┘
                               │
                               │ EXTERNAL_TASK executor
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  VRSTVA 2: EXTERNAL n8n LAYER (visual orchestration hub)            │
│  - Integration třetích stran (Jira, Confluence, Trello, M365)      │
│  - AI/ML pipelines (Langchain, OpenAI, local models)               │
│  - ETL/batch jobs mimo core doménu                                  │
│  - Visual workflow builder (400+ built-in nodes)                    │
│  - Scope: EXTERNAL integrace + heavy data processing                │
└─────────────────────────────────────────────────────────────────────┘
```

**Klíčové principy:**
1. **Separation of Concerns**: Core workflow engine ≠ External orchestration
2. **Single Integration Point**: `EXTERNAL_TASK` executor = most jediný
3. **Built-in vs. Custom**: Leverage n8n 400+ nodes, nebuildit custom connectors
4. **Metadata-driven**: Workflow steps v metamodelu, ne hardcoded v kódu

---

## 📊 Co MÁME dnes (Reality Check)

### ✅ EPIC-006: Internal Workflow Engine (W1-W12)

**Status:** 🟢 **100% COMPLETE** (reported)  
**LOC:** ~18,000  
**Tests:** 119 unit + 15 integration

| ID | Component | Status | Reality |
|----|-----------|--------|---------|
| **W1** | JSON Workflow Model | ✅ DONE | State machine definition (states, transitions, guards, actions) |
| **W2** | Persistence Layer | ✅ DONE | `entity_state`, `state_transition`, `entity_state_log` tables |
| **W3-W4** | Visual Designer | ✅ DONE | React Flow drag-and-drop, edge validation, guard editor |
| **W5** | Runtime Foundation | ✅ DONE | `WorkflowService` - state transitions, SLA tracking, Kafka events |
| **W6** | Frontend UX | ✅ DONE | `WorkflowGraph`, `TimelinePanel`, `ActionsBar` |
| **W7** | Executors | ⚠️ **PARTIAL!** | Interface + orchestrator OK, **typed executors CHYBÍ** |
| **W8** | Timers & SLA | ✅ DONE | `WorkflowTimerService`, `workflow_timers` table, periodic checks |
| **W9** | Versioning | ✅ DONE | `workflow_versions`, `workflow_instance_versions`, migration strategies |
| **W10** | Studio UI | ⚠️ **UI ONLY** | `WorkflowStepsEditor` - **backend mock, no storage/execution** |
| **W11** | Testing | ✅ DONE | `WorkflowTestingService` - dry-run simulation |
| **W12** | Monitoring | ⚠️ **METRICS ONLY** | Prometheus metrics, **Grafana dashboards CHYBÍ** |

#### 🔍 W7 Executors - Detailed Gap Analysis

**Co existuje:**
```java
// ✅ Executor framework je hotový
WorkflowExecutor interface               // ✅ OK (async, retry, compensate)
WorkflowExecutorRegistry                 // ✅ OK (executor lookup)
WorkflowExecutorOrchestrator             // ✅ OK (retry + compensation logic)

// ✅ Základní implementace (proof-of-concept)
WebhookNotificationExecutor              // ✅ OK (HTTP POST webhook)
EmailNotificationExecutor                // ✅ OK (SendGrid/SMTP)
```

**Co CHYBÍ (kritické):**
```java
// ❌ Typed executors podle původní vize
ApprovalExecutor                         // ❌ MISSING (SINGLE, ALL_OF, ANY_OF, QUORUM)
RestSyncExecutor                         // ❌ MISSING (OpenAPI, circuit breaker, retry)
KafkaCommandExecutor                     // ❌ MISSING (request-reply pattern, correlation ID)
ExternalTaskExecutor                     // ❌ MISSING (poll & complete for n8n)
TimerDelayExecutor                       // ❌ MISSING (scheduled actions, reminders)
```

**Důsledek:**  
- Workflow může měnit stavy (W5), ale **nemůže orchestrovat komplexní procesy**
- Chybí sequenční exekuce kroků (approval → REST call → Kafka event → wait)
- Chybí runtime instance management (`workflow_instances`, `workflow_step_executions` tables)

#### 🔍 W10 Workflow Steps - Detailed Gap Analysis

**Co existuje:**
```typescript
// ✅ Frontend komponenta kompletní
WorkflowStepsEditor.tsx                  // ✅ OK (add/edit/remove steps)
  - Step types: rest, kafka, email, custom
  - InputMap editor (${variable} substitution)
  - Retry policy configuration
  - onSuccess/onError routing
  - Validation + dry-run API calls

// ✅ Mock backend endpoints
POST /api/admin/workflows/{entity}/validate-steps  // ✅ OK (schema validation)
POST /api/admin/workflows/{entity}/dry-run         // ✅ OK (mock execution)
```

**Co CHYBÍ:**
```java
// ❌ Persistence layer
workflow_steps table                     // ❌ MISSING (no storage!)
GET/POST/PUT/DELETE /api/workflow-steps  // ❌ MISSING (CRUD endpoints)

// ❌ Runtime execution
WorkflowStepOrchestrator                 // ❌ MISSING (sequential execution)
workflow_instances table                 // ❌ MISSING (runtime state)
workflow_step_executions table           // ❌ MISSING (step results)

// ❌ Schema integration do metamodelu
EntityDefinition.workflowSteps field     // ❌ MISSING (steps[] v entity schema)
```

**Důsledek:**  
- UI lze editovat kroky, ale **ukládá se NIKAM**
- Dry-run je mock, **nepouští real executors**
- Workflow steps **nejsou součástí metamodelu** → nelze je použít v runtime

---

### ❌ EPIC-011: n8n External Layer

**Status:** 🔴 **0% IMPLEMENTED**  
**LOC:** 0  
**Tests:** 0

| ID | Component | Status | Gap |
|----|-----------|--------|-----|
| **N8N1** | Platform Deployment | ❌ TODO | Žádný Docker service, žádná PostgreSQL DB pro n8n |
| **N8N2** | Keycloak SSO | ❌ TODO | Žádný Keycloak client, žádná konfigurace |
| **N8N3** | Nginx Proxy | ❌ TODO | Žádná `/n8n/*` route v nginx.conf |
| **N8N4** | Workflow Templates | ❌ TODO | Žádné pre-built n8n flows |
| **N8N5** | Monitoring | ❌ TODO | Žádné Grafana dashboards pro n8n |
| **N8N6** | Backend BFF API | ❌ TODO | Žádný Spring Boot proxy pro n8n REST API |

**Důsledek:**  
- n8n **není dostupný vůbec**
- Core workflow **nemůže delegovat úkoly** na external orchestrator
- **Žádné integrace** s Jira/Confluence/Trello/M365/Google

---

## 🏗️ Koherentní Architektura (2-Layer Design)

### Layer 1: Internal Workflow Engine

**Zodpovědnost:**
- ✅ Core business procesy (Order approval, Invoice lifecycle, Contract signing)
- ✅ State machine s guardy (hasRole, amount > 1000, status == "PENDING")
- ✅ SLA tracking & escalations
- ✅ Audit trail (kdo, kdy, proč změnil stav)
- ✅ Workflow versioning (migrace schémat)

**Komponenty:**
```
┌─────────────────────────────────────────────────────────────┐
│  METAMODEL LAYER                                            │
│  - EntityDefinition.workflow: { states, transitions }       │
│  - EntityDefinition.workflowSteps: [ APPROVAL, REST, ... ]  │  ← NEW!
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  WORKFLOW RUNTIME ENGINE                                     │
│  - WorkflowService: executeTransition()                     │  ← EXISTS
│  - WorkflowStepOrchestrator: executeSteps()                 │  ← NEW!
│  - WorkflowTimerService: checkSla()                         │  ← EXISTS
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  TYPED EXECUTORS (W7 Extension)                             │
│  - ApprovalExecutor (SINGLE, ALL_OF, ANY_OF, QUORUM)        │  ← NEW!
│  - RestSyncExecutor (OpenAPI, retry, circuit breaker)       │  ← NEW!
│  - KafkaCommandExecutor (publish + correlate reply)         │  ← NEW!
│  - TimerDelayExecutor (schedule action)                     │  ← NEW!
│  - ExternalTaskExecutor (delegate to n8n)                   │  ← NEW! (most)
└─────────────────────────────────────────────────────────────┘
```

**Database Schema:**
```sql
-- ✅ Existing (W2, W5, W8, W9)
entity_state (entity_type, entity_id, state_code, tenant_id, since)
state_transition (from_code, to_code, code, guard JSONB, sla_minutes)
entity_state_log (audit trail)
workflow_timers (scheduled_at, action, status)
workflow_versions (schema_definition JSONB)

-- ❌ NEW (WF17 Runtime)
workflow_instances (
  id UUID PRIMARY KEY,
  entity_type VARCHAR(100),
  entity_id VARCHAR(100),
  workflow_version_id BIGINT,
  status VARCHAR(50),  -- RUNNING, COMPLETED, FAILED, CANCELLED
  current_step_id VARCHAR(100),
  context JSONB,  -- runtime variables ${orderId}, ${amount}
  created_at TIMESTAMP,
  completed_at TIMESTAMP
);

workflow_step_executions (
  id UUID PRIMARY KEY,
  workflow_instance_id UUID REFERENCES workflow_instances(id),
  step_id VARCHAR(100),
  step_type VARCHAR(50),  -- APPROVAL, REST_SYNC, KAFKA_COMMAND, atd.
  status VARCHAR(50),  -- PENDING, RUNNING, SUCCESS, FAILED
  input JSONB,
  output JSONB,
  error TEXT,
  retry_count INTEGER,
  started_at TIMESTAMP,
  completed_at TIMESTAMP
);

-- ❌ NEW (WF12 APPROVAL)
workflow_approval_requests (
  id UUID PRIMARY KEY,
  workflow_instance_id UUID,
  approval_type VARCHAR(50),  -- SINGLE, ALL_OF, ANY_OF, QUORUM
  required_approvals INTEGER,
  approvers TEXT[],  -- List of user IDs or roles
  due_at TIMESTAMP,
  escalated BOOLEAN,
  status VARCHAR(50)
);

workflow_approval_responses (
  id UUID PRIMARY KEY,
  approval_request_id UUID,
  user_id VARCHAR(100),
  response VARCHAR(50),  -- APPROVE, REJECT
  comment TEXT,
  responded_at TIMESTAMP,
  UNIQUE (approval_request_id, user_id)
);

-- ❌ NEW (WF15 EXTERNAL_TASK)
workflow_external_tasks (
  id UUID PRIMARY KEY,
  workflow_instance_id UUID,
  task_type VARCHAR(100),  -- "n8n-approval-flow", "n8n-jira-sync"
  worker_id VARCHAR(100),  -- n8n worker registration
  status VARCHAR(50),  -- PENDING, CLAIMED, COMPLETED, FAILED, TIMEOUT
  input JSONB,
  output JSONB,
  timeout_at TIMESTAMP,
  last_heartbeat_at TIMESTAMP,
  claimed_at TIMESTAMP,
  completed_at TIMESTAMP
);
```

---

### Layer 2: External n8n Orchestration Hub

**Zodpovědnost:**
- ✅ Integrace třetích stran (Jira, Confluence, Trello, Slack, M365, Google)
- ✅ AI/ML pipelines (Langchain, OpenAI, local LLMs)
- ✅ ETL/batch jobs (CSV export, data transformation)
- ✅ Heavy data processing (mimo core transakce)
- ✅ Visual workflow builder pro business users

**Komponenty:**
```
┌─────────────────────────────────────────────────────────────┐
│  n8n COMMUNITY EDITION                                       │
│  - Visual workflow builder (web UI)                         │
│  - 400+ built-in nodes (Jira, Confluence, HTTP, Webhook)    │
│  - PostgreSQL persistence (workflows + executions)          │
│  - REST API (create/execute/monitor workflows)              │
│  - Webhook triggers (external events → n8n workflows)       │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Backend BFF Proxy (N8N6)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  BACKEND BFF API (Spring Boot)                              │
│  - JWT validation (Keycloak token)                          │
│  - Tenant filtering (multi-tenant aware)                    │
│  - Rate limiting (prevent abuse)                            │
│  - Audit logging (kdo volal n8n API)                        │
│  - Cache (workflow definitions, 5 min TTL)                  │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Reverse Proxy (N8N3)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  NGINX PROXY                                                 │
│  - /n8n/* → n8n:5678                                        │
│  - Keycloak SSO (N8N2) - require auth before access         │
│  - SSL termination                                          │
└─────────────────────────────────────────────────────────────┘
```

**Docker Compose:**
```yaml
# ❌ NEW (N8N1)
n8n:
  image: n8nio/n8n:latest
  environment:
    - N8N_BASIC_AUTH_ACTIVE=false  # SSO via Keycloak
    - DB_TYPE=postgresdb
    - DB_POSTGRESDB_DATABASE=n8n
    - DB_POSTGRESDB_HOST=core-db
    - DB_POSTGRESDB_USER=n8n_app
    - DB_POSTGRESDB_PASSWORD=${N8N_DB_PASSWORD}
    - WEBHOOK_URL=https://admin.core-platform.local/n8n/webhook
    - N8N_HOST=admin.core-platform.local
    - N8N_PROTOCOL=https
    - N8N_PORT=443
  ports:
    - "5678"  # Internal only, NOT exposed to host
  depends_on:
    - core-db
```

---

## 🔗 Integration Pattern: Internal ↔ External

### Workflow Step: EXTERNAL_TASK Type

**Use Case:**  
Core workflow potřebuje **delegovat úkol** na n8n (např. "synchronizuj s Jira", "pošli AI summarization", "export do Google Sheets").

**Workflow Step Definition (v metamodelu):**
```json
{
  "entityType": "Order",
  "workflow": {
    "states": [...],
    "transitions": [...]
  },
  "workflowSteps": [
    {
      "id": "step-1",
      "type": "APPROVAL",
      "label": "Manager Approval",
      "config": {
        "approvalType": "SINGLE",
        "roles": ["ORDER_APPROVER"],
        "slaMinutes": 60
      },
      "onSuccess": "step-2",
      "onError": "step-error"
    },
    {
      "id": "step-2",
      "type": "EXTERNAL_TASK",
      "label": "Create Jira Ticket",
      "config": {
        "taskType": "n8n-jira-create-issue",
        "endpoint": "https://admin.core-platform.local/api/n8n/workflows/jira-create/execute",
        "inputMap": {
          "orderId": "${entityId}",
          "orderAmount": "${context.amount}",
          "customerName": "${context.customer.name}"
        },
        "timeoutMinutes": 10
      },
      "onSuccess": "step-3",
      "onError": "step-error"
    },
    {
      "id": "step-3",
      "type": "REST_SYNC",
      "label": "Update ERP System",
      "config": {
        "method": "POST",
        "url": "https://erp.example.com/api/orders/${entityId}/sync",
        "headers": {
          "Authorization": "Bearer ${secrets.ERP_API_TOKEN}"
        },
        "body": {
          "status": "APPROVED",
          "jiraTicket": "${step-2.output.jiraKey}"
        }
      },
      "onSuccess": "step-end"
    }
  ]
}
```

**Execution Flow:**

```
┌────────────────────────────────────────────────────────────────┐
│  1. Core Workflow Runtime (WorkflowStepOrchestrator)           │
│     - Load workflow instance                                   │
│     - Execute step-1 (APPROVAL) → ApprovalExecutor             │
│     - Wait for approval responses                              │
│     - Approval granted → transition to step-2                  │
└────────────────────┬───────────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────────┐
│  2. ExternalTaskExecutor (WF15)                                │
│     - Create external_task record:                             │
│       * task_type = "n8n-jira-create-issue"                    │
│       * input = { orderId, orderAmount, customerName }         │
│       * status = PENDING                                       │
│       * timeout_at = now() + 10 minutes                        │
│     - Mark workflow_step_execution as RUNNING                  │
└────────────────────┬───────────────────────────────────────────┘
                     │
                     │ HTTP POST /api/n8n/workflows/jira-create/execute
                     │ Body: { orderId, orderAmount, customerName }
                     ▼
┌────────────────────────────────────────────────────────────────┐
│  3. Backend BFF API (N8N6) - Spring Boot                       │
│     - Validate JWT token (Keycloak)                            │
│     - Extract tenant_id from token                             │
│     - Check rate limits                                        │
│     - Call n8n REST API:                                       │
│       POST n8n:5678/api/v1/workflows/jira-create/execute       │
│     - Audit log: "user X triggered n8n workflow Y"             │
└────────────────────┬───────────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────────┐
│  4. n8n Workflow Execution                                     │
│     - Trigger: Webhook (from BFF API)                          │
│     - Node 1: HTTP Request → Jira API (create issue)           │
│       * Uses n8n built-in "Jira" node                          │
│       * Input: project, summary, description                   │
│       * Output: jiraKey = "PROJ-123"                           │
│     - Node 2: HTTP Request → Core Platform callback            │
│       * POST /api/workflows/external-tasks/{taskId}/complete   │
│       * Body: { output: { jiraKey: "PROJ-123" } }              │
└────────────────────┬───────────────────────────────────────────┘
                     │
                     │ Callback
                     ▼
┌────────────────────────────────────────────────────────────────┐
│  5. Core Platform - Complete Task Endpoint                     │
│     - Update external_task:                                    │
│       * status = COMPLETED                                     │
│       * output = { jiraKey: "PROJ-123" }                       │
│     - Update workflow_step_execution:                          │
│       * status = SUCCESS                                       │
│       * output = { jiraKey: "PROJ-123" }                       │
│     - WorkflowStepOrchestrator continues to step-3             │
└────────────────────┬───────────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────────┐
│  6. Core Workflow Runtime (step-3: REST_SYNC)                  │
│     - RestSyncExecutor executes                                │
│     - POST https://erp.example.com/api/orders/123/sync         │
│     - Body includes: jiraKey = "PROJ-123" (from step-2 output) │
│     - ERP sync complete → workflow COMPLETED                   │
└────────────────────────────────────────────────────────────────┘
```

**Timeout Handling:**
```java
// ExternalTaskExecutor checks timeout via scheduled job
@Scheduled(fixedRate = 30000) // Every 30 seconds
public void checkTimeouts() {
  List<ExternalTask> timedOutTasks = externalTaskRepo.findByStatusAndTimeoutBefore(
    "PENDING", Instant.now()
  );
  
  for (ExternalTask task : timedOutTasks) {
    // Mark as TIMEOUT
    task.setStatus("TIMEOUT");
    task.setError("External worker did not complete within " + task.getTimeoutMinutes() + " minutes");
    
    // Update workflow step execution
    WorkflowStepExecution stepExec = stepExecRepo.findById(task.getWorkflowStepExecutionId());
    stepExec.setStatus("FAILED");
    stepExec.setError("External task timeout");
    
    // Trigger onError step (if configured)
    workflowOrchestrator.continueWorkflow(task.getWorkflowInstanceId(), "step-error");
  }
}
```

---

## 📋 Sjednocené User Stories (WF12-WF18 + N8N1-N8N6)

### 🔴 Priority 1: Foundation (Weeks 1-3)

#### WF12: APPROVAL Executor *(3 days, 800 LOC)*
**Goal:** APPROVAL step type s 4 approval modes

**Deliverables:**
- `ApprovalExecutor.java` implements WorkflowExecutor
- Approval types: SINGLE, ALL_OF, ANY_OF, QUORUM
- `workflow_approval_requests`, `workflow_approval_responses` tables
- REST API: `POST /api/workflows/approvals/{requestId}/respond`
- Email/Slack notifications
- SLA escalations (scheduled job)
- Metrics: approval_requests_created, approval_response_time, approvals_escalated

**Dependencies:** W5 (WorkflowService), W7 (executor framework)

---

#### WF13: REST_SYNC Executor *(5 days, 1,200 LOC)*
**Goal:** REST_SYNC step type s OpenAPI support

**Deliverables:**
- `RestSyncExecutor.java` implements WorkflowExecutor
- OpenAPI spec parsing (extract endpoint, method, schema)
- HTTP client (Spring WebClient)
- Retry logic (exponential backoff)
- Circuit breaker (Resilience4j)
- Timeout handling
- `${variable}` substitution v URL, headers, body
- Idempotence via correlation ID
- Metrics: rest_calls_total, rest_call_duration, rest_circuit_breaker_state

**Dependencies:** W5, W7

---

#### WF14: KAFKA_COMMAND Executor *(2 days, 600 LOC)*
**Goal:** KAFKA_COMMAND step type s request-reply pattern

**Deliverables:**
- `KafkaCommandExecutor.java` implements WorkflowExecutor
- Kafka producer (Spring Kafka)
- Correlation ID generation (UUID)
- Reply consumer (correlate via correlation ID)
- Timeout → DLQ fallback
- AsyncAPI schema validation
- Metrics: kafka_commands_sent, kafka_replies_received, kafka_timeouts

**Dependencies:** W5, W7, Kafka

---

#### WF15: EXTERNAL_TASK Executor *(3 days, 800 LOC)* **← MOST KRITICKÝ**
**Goal:** EXTERNAL_TASK step type pro n8n integraci

**Deliverables:**
- `ExternalTaskExecutor.java` implements WorkflowExecutor
- `workflow_external_tasks` table
- REST API:
  - `POST /api/workflows/external-tasks/{taskId}/complete` (callback from n8n)
  - `POST /api/workflows/external-tasks/{taskId}/fail`
  - `POST /api/workflows/external-tasks/{taskId}/heartbeat`
- Timeout detection (scheduled job)
- Worker registration (optional)
- Metrics: external_tasks_created, external_tasks_completed, external_tasks_timeout

**Dependencies:** W5, W7, N8N6 (BFF API)

---

#### WF16: TIMER/DELAY Executor *(2 days, 400 LOC)*
**Goal:** TIMER step type pro delayed actions

**Deliverables:**
- `TimerDelayExecutor.java` implements WorkflowExecutor
- Integration s `WorkflowTimerService` (W8)
- Schedule timer in `workflow_timers` table
- Timer fired → continue workflow
- Reminder notifications (email/Slack)
- Metrics: timers_scheduled, timers_fired

**Dependencies:** W5, W8

---

#### WF17: Workflow Instance Runtime *(6 days, 1,500 LOC)* **← KRITICKÝ**
**Goal:** Runtime orchestration pro step-by-step execution

**Deliverables:**
- `WorkflowStepOrchestrator.java`
- `workflow_instances`, `workflow_step_executions` tables
- Sequential execution: step 1 → wait → step 2 → wait → step 3
- Runtime context (variables `${entityId}`, `${step-1.output.jiraKey}`)
- Error handling: onError routing
- Compensation: rollback on failure
- REST API:
  - `POST /api/workflows/instances/{entityId}/start`
  - `GET /api/workflows/instances/{instanceId}`
  - `POST /api/workflows/instances/{instanceId}/cancel`
- Metrics: workflow_instances_created, workflow_instances_completed, workflow_execution_duration

**Dependencies:** W5, WF12-WF16 (all executors)

---

#### WF18: Workflow Steps Schema v Metamodel *(3 days, 600 LOC)*
**Goal:** Workflow steps jako součást entity definition

**Deliverables:**
- Extend `EntityDefinition.java`:
  ```java
  @JsonProperty("workflowSteps")
  private List<WorkflowStep> workflowSteps;
  ```
- `WorkflowStep` Java record:
  ```java
  record WorkflowStep(
    String id,
    StepType type,  // APPROVAL, REST_SYNC, KAFKA_COMMAND, EXTERNAL_TASK, TIMER
    String label,
    Map<String, Object> config,
    String onSuccess,
    String onError,
    RetryPolicy retry
  ) {}
  ```
- Storage v `workflow_versions` JSONB column
- Validation: step IDs unique, onSuccess/onError references valid
- Migration: existing workflows → add empty `workflowSteps: []`
- `WorkflowStepsEditor` UI → save to metamodel API

**Dependencies:** META (metamodel CRUD), W10 (UI editor)

---

### 🔴 Priority 2: n8n Deployment (Week 4)

#### N8N1: Platform Deployment *(1 day, 400 LOC)*
**Goal:** n8n Docker service s PostgreSQL backend

**Deliverables:**
- `docker-compose.yml` n8n service
- PostgreSQL database `n8n` (separate from `core`)
- DB user: `n8n_app` (separate credentials)
- Environment config: webhook URL, protocol, host
- Execution retention: 30 days
- Volume mounts: `/root/.n8n` (persistent storage)

**Dependencies:** PostgreSQL

---

#### N8N2: Keycloak SSO Integration *(1 day, 300 LOC)*
**Goal:** n8n authentication via Keycloak

**Deliverables:**
- Keycloak client: `n8n-client`
- Redirect URIs: `https://admin.core-platform.local/n8n/*`
- Client roles: `n8n-users`, `n8n-admins`
- Realm role mapping
- JWT token configuration
- n8n OAuth2 config (disable basic auth)

**Dependencies:** Keycloak, N8N1

---

#### N8N3: Nginx Reverse Proxy *(0.5 day, 200 LOC)*
**Goal:** `/n8n/*` routing s SSL

**Deliverables:**
- `nginx.conf` location block:
  ```nginx
  location /n8n/ {
    auth_request /auth;  # Keycloak validation
    proxy_pass http://n8n:5678/;
    proxy_set_header X-Forwarded-Proto https;
  }
  ```
- SSL termination
- WebSocket support (n8n editor)

**Dependencies:** Nginx, N8N1, N8N2

---

#### N8N6: Backend BFF API *(3 days, 800 LOC)* **← MOST PRO INTEGRACI**
**Goal:** Spring Boot proxy pro n8n REST API

**Deliverables:**
- `N8nBffController.java`:
  ```java
  @RestController
  @RequestMapping("/api/n8n")
  public class N8nBffController {
    @PostMapping("/workflows/{workflowId}/execute")
    public ResponseEntity<ExecutionResult> executeWorkflow(
      @PathVariable String workflowId,
      @RequestBody Map<String, Object> input,
      @AuthenticationPrincipal Jwt jwt
    ) {
      // 1. Validate JWT
      String tenantId = jwt.getClaim("tenant_id");
      
      // 2. Rate limit check
      rateLimiter.checkLimit(tenantId);
      
      // 3. Audit log
      auditLog.log("n8n_workflow_execute", workflowId, input, jwt.getSubject());
      
      // 4. Call n8n REST API
      return webClient.post()
        .uri("http://n8n:5678/api/v1/workflows/{id}/execute", workflowId)
        .bodyValue(input)
        .retrieve()
        .toEntity(ExecutionResult.class);
    }
  }
  ```
- JWT validation (Keycloak token)
- Tenant filtering (multi-tenant aware)
- Rate limiting (Redis-based)
- Cache (workflow definitions, 5 min TTL)
- REST endpoints:
  - `POST /api/n8n/workflows/{id}/execute`
  - `GET /api/n8n/workflows`
  - `GET /api/n8n/executions/{id}`

**Dependencies:** N8N1-N8N3, Spring Security, Redis

---

#### N8N4: Workflow Templates *(2 days, 500 LOC)*
**Goal:** Pre-built n8n workflows pro common use cases

**Deliverables:**
- n8n workflow JSONs:
  1. `jira-create-issue.json` (Webhook trigger → Jira create issue → HTTP callback)
  2. `confluence-sync.json` (Webhook → Confluence update page)
  3. `trello-automation.json` (Webhook → Trello create card)
  4. `ai-summarization.json` (Webhook → OpenAI API → callback)
- Import script: `n8n import:workflow --input=templates/jira-create-issue.json`
- Documentation: screenshots + setup guide
- README per template (input schema, output schema, configuration)

**Dependencies:** N8N1-N8N3

---

#### N8N5: Monitoring *(1 day, 400 LOC)*
**Goal:** Grafana dashboards pro n8n

**Deliverables:**
- Grafana dashboard: `n8n-monitoring.json`
  - Panels: active workflows, executions/hour, success rate, avg duration
  - Data source: n8n PostgreSQL DB
  - Queries: `SELECT COUNT(*) FROM executions WHERE finished = true`
- Alerting rules:
  - n8n workflow execution failure rate > 10%
  - n8n API latency > 5s

**Dependencies:** Grafana, N8N1

---

### 🟡 Priority 3: Observability (Week 5)

#### WF19: Workflow Grafana Dashboards *(1 day, 300 LOC)*
**Goal:** Grafana dashboards pro internal workflow engine

**Deliverables:**
- Grafana dashboard: `workflow-monitoring.json`
  - Panels:
    - Active workflow instances (by status)
    - Step executions/hour (by type)
    - Approval pending time (avg, p95, p99)
    - External task timeout rate
    - Executor retry rate
  - Data source: Prometheus (metrics from WF12-WF17)
- Alerting rules:
  - Approval SLA breach > 5/hour
  - External task timeout rate > 20%

**Dependencies:** W12 (metrics), Grafana

---

## 🎯 Implementační Roadmap (5 týdnů)

### Week 1: Core Executors (WF12-WF14)
- **Day 1-3:** WF12 APPROVAL Executor
- **Day 4-5:** WF14 KAFKA_COMMAND Executor
- **Total:** 3 executors hotové, ~2,600 LOC

---

### Week 2: Integration Executors (WF13, WF15, WF16)
- **Day 1-5:** WF13 REST_SYNC Executor
- **Day 6-8:** WF15 EXTERNAL_TASK Executor ← **Klíčový pro n8n**
- **Day 9-10:** WF16 TIMER/DELAY Executor
- **Total:** 3 executors hotové, ~2,400 LOC

---

### Week 3: Workflow Runtime (WF17, WF18)
- **Day 1-6:** WF17 Workflow Instance Runtime (orchestrator)
- **Day 7-9:** WF18 Workflow Steps Schema v Metamodel
- **Total:** Runtime engine hotový, ~2,100 LOC

---

### Week 4: n8n Deployment (N8N1-N8N6)
- **Day 1:** N8N1 Platform Deployment
- **Day 2:** N8N2 Keycloak SSO + N8N3 Nginx Proxy
- **Day 3-5:** N8N6 Backend BFF API
- **Day 6-7:** N8N4 Workflow Templates
- **Day 8:** N8N5 Monitoring
- **Total:** n8n fully operational, ~2,200 LOC

---

### Week 5: Observability + Integration Testing (WF19 + E2E)
- **Day 1:** WF19 Workflow Grafana Dashboards
- **Day 2-5:** End-to-end integration tests:
  1. Order approval (APPROVAL → REST_SYNC → Kafka event)
  2. Jira ticket creation (APPROVAL → EXTERNAL_TASK → n8n → Jira API)
  3. Timeout scenarios (EXTERNAL_TASK timeout → onError routing)
  4. Compensation (REST_SYNC fails → rollback approval)
- **Total:** Observability + testing complete, ~300 LOC + tests

---

## ✅ Definition of Done

### Internal Workflow Engine (EPIC-006 Extension)
- [x] W1-W12 existující komponenty fungují
- [ ] WF12-WF16 Typed executors implementované a otestované
- [ ] WF17 Runtime orchestration funguje (sequential step execution)
- [ ] WF18 Workflow steps v metamodelu (save/load/validate)
- [ ] WF19 Grafana dashboards (workflow metrics visualized)
- [ ] E2E test: Order approval workflow (4 steps, approval → REST → Kafka → complete)

### External n8n Layer (EPIC-011)
- [ ] N8N1 n8n deployed a přístupný
- [ ] N8N2 Keycloak SSO funguje (users can login)
- [ ] N8N3 Nginx proxy routes `/n8n/*` correctly
- [ ] N8N4 Templates imported (Jira, Confluence, Trello, AI)
- [ ] N8N5 Grafana dashboards (n8n metrics visualized)
- [ ] N8N6 BFF API funguje (JWT validation, rate limiting, audit)
- [ ] E2E test: Core workflow → n8n workflow → Jira ticket created

### Integration (Internal ↔ External)
- [ ] EXTERNAL_TASK executor deleguje na n8n
- [ ] n8n workflow volá callback `/external-tasks/{id}/complete`
- [ ] Timeout handling funguje (n8n nereaguje → onError routing)
- [ ] E2E test: Order approval → EXTERNAL_TASK → n8n Jira flow → ERP sync

---

## 📐 Architectural Decisions

### AD-1: Proč 2 vrstvy místo 1 unified systému?

**Decision:** Oddělit Internal (metamodel-driven) od External (n8n visual).

**Reasons:**
1. **Separation of Concerns**: Core business logic (approval, state machine) ≠ External integrations (Jira API)
2. **Technology Fit**: Metamodel pro typed workflows (compile-time validation) vs. n8n pro visual ad-hoc workflows
3. **User Personas**: Developers (metamodel YAML) vs. Business users (n8n drag-and-drop)
4. **Failure Isolation**: n8n down → core workflows stále fungují (approval, state transitions)

**Consequences:**
- ✅ Clear responsibility boundaries
- ✅ Each layer optimized for its use case
- ❌ Integration overhead (EXTERNAL_TASK executor + BFF API)

---

### AD-2: Proč EXTERNAL_TASK místo direct n8n calls?

**Decision:** Core workflow engine NESMÍ přímo volat n8n REST API.

**Reasons:**
1. **Decoupling**: Core workflow nezná n8n implementaci
2. **Testability**: ExternalTaskExecutor lze mocknout
3. **Flexibility**: EXTERNAL_TASK může delegovat na jiný systém (Temporal, Camunda) v budoucnu
4. **Observability**: External task má vlastní lifecycle (pending → claimed → completed → timeout)

**Implementation:**
```java
// ❌ ŠPATNĚ - tight coupling
RestTemplate.postForEntity("http://n8n:5678/workflows/123/execute", input);

// ✅ SPRÁVNĚ - loose coupling via EXTERNAL_TASK
ExternalTask task = externalTaskExecutor.createTask("n8n-jira-sync", input);
// n8n worker polls task, executes, calls callback
```

---

### AD-3: Proč workflow steps v metamodelu místo separátní tabulky?

**Decision:** `workflowSteps` jako součást `EntityDefinition.workflow` v JSONB.

**Reasons:**
1. **Versioning**: Workflow steps jsou součást workflow verze (atomická změna)
2. **Co-location**: States + transitions + steps v jednom dokumentu
3. **Schema Evolution**: JSONB umožňuje flexibilitu (přidat nový step type bez migrace)
4. **Metamodel Consistency**: Entity definice je single source of truth

**Consequences:**
- ✅ Atomické verze (steps změněné → celý workflow nová verze)
- ✅ Jednodušší backup/restore
- ❌ JSONB queries složitější než SQL JOIN

---

## 🚀 Next Steps

1. **Review architektury** (stakeholders)
2. **Approval roadmapu** (product owner)
3. **Implementace WF12** (první executor jako proof-of-concept)
4. **n8n POC** (N8N1 deployment + simple workflow)
5. **Integration test** (EXTERNAL_TASK → n8n → callback)

---

**Author:** Systémový Architekt  
**Date:** 8. listopadu 2025  
**Version:** 1.0  
**Status:** ✅ Architecture Proposal - Ready for Review
