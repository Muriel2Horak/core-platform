# 🔄 Workflow + n8n - Nové User Stories (2025)

> ⚠️ **OBSOLETE:** Tento dokument byl nahrazen integrací do EPICů  
> ✅ **Použij:**  
>   - [EPIC-006 README](./EPIC-006-workflow-engine/README.md) - Workflow Orchestration (unified)  
>   - [EPIC-011 README](./EPIC-011-n8n-workflow-automation/README.md) - n8n External Layer  
>   - [`WORKFLOW_UNIFIED_ARCHITECTURE.md`](./WORKFLOW_UNIFIED_ARCHITECTURE.md) - Kompletní architektura  
> 📖 **Historie:** Původní story definice ze dne 8. listopadu 2025

**Datum:** 8. listopadu 2025  
**Status:** 🔴 **ARCHIVED** (stories přesunuty do EPIC READMEs)  
**Účel:** [ARCHIVED] Dodefinování chybějících stories (nyní v EPIC-006 Phase 2 & EPIC-011)

---

## 📋 EPIC-006: Workflow Engine - Nové Stories (WF12-WF23)

### ✅ Hotové Stories (W1-W11)

| ID | Story | Status | LOC |
|----|-------|--------|-----|
| WF1 | JSON Workflow Model | ✅ DONE | ~1,200 |
| WF2 | Workflow Execution Engine | ✅ DONE | ~800 |
| WF3 | React Flow Visual Designer | ✅ DONE | ~1,200 |
| WF4 | Enhanced Drag-and-Drop UX | ✅ DONE | ~1,300 |
| WF5 | Runtime Foundation | ✅ DONE | ~3,000 |
| WF6 | Frontend UX Enhancements | ✅ DONE | ~2,000 |
| WF7 | Node Executors (HTTP/Script/Human) | ⚠️ PARTIAL | ~500 (placeholders!) |
| WF8 | Timer Nodes & SLA Tracking | ✅ DONE | ~600 |
| WF9 | Workflow Versioning | ✅ DONE | ~1,800 |
| WF10 | Workflow Studio UI Integration | ✅ DONE | ~2,200 |
| WF11 | Workflow Testing & Simulation | ✅ DONE | ~900 |

**Total:** ~15,500 LOC

---

### 🆕 Nové Stories (WF12-WF23)

| ID | Story | Estimate | Priority | Dependencies | Status |
|----|-------|----------|----------|--------------|--------|
| [WF12](#wf12-approval-executor) | APPROVAL Executor | 800 LOC, 3d | 🔴 HIGH | W5, W7 | ✅ **READY** (detail hotový) |
| [WF13](#wf13-rest-sync-executor) | REST_SYNC Executor | 1,200 LOC, 5d | 🔴 HIGH | W5, W7 | ⏳ TODO |
| [WF14](#wf14-kafka-command-executor) | KAFKA_COMMAND Executor | 600 LOC, 2d | 🔴 HIGH | W5, W7, Kafka | ⏳ TODO |
| [WF15](#wf15-external-task-executor) | EXTERNAL_TASK Executor | 800 LOC, 3d | � HIGH | W5, W7 | ⏳ TODO |
| [WF16](#wf16-timer-delay-executor) | TIMER/DELAY Executor | 400 LOC, 2d | 🟡 MEDIUM | W5, W8 | ⏳ TODO |
| [WF17](#wf17-workflow-instance-runtime) | Workflow Instance Runtime | 1,500 LOC, 6d | 🔴 HIGH | W5, WF12-16 | ⏳ TODO |
| [WF18](#wf18-workflow-steps-schema) | Workflow Steps Schema v Metamodel | 600 LOC, 3d | 🔴 HIGH | META, W10 | ⏳ TODO |
| ~~WF19~~ | ~~Generic REST Connector~~ | ~~❌ CANCELLED~~ | - | **n8n HTTP Request node** | ✅ DELEGATED |
| ~~WF20~~ | ~~Jira Connector~~ | ~~❌ CANCELLED~~ | - | **n8n Jira node** | ✅ DELEGATED |
| ~~WF21~~ | ~~Confluence Connector~~ | ~~❌ CANCELLED~~ | - | **n8n Confluence node** | ✅ DELEGATED |
| ~~WF22~~ | ~~Trello Connector~~ | ~~❌ CANCELLED~~ | - | **n8n Trello node** | ✅ DELEGATED |
| [WF19](#wf19-workflow-grafana-dashboards) | Workflow Grafana Dashboards | 300 LOC, 1d | 🟡 MEDIUM | W12, Grafana | ⏳ TODO |

**Total Estimate:** ~5,600 LOC, ~25 developer days *(was ~9,700 LOC - saved 4,100 LOC díky n8n!)*

---

## 📊 EPIC-011: n8n Integration - Stories (N8N1-N8N6)

| ID | Story | Estimate | Priority | Dependencies | Status |
|----|-------|----------|----------|--------------|--------|
| [N8N1](#n8n1-n8n-platform-deployment) | n8n Platform Deployment | 400 LOC, 1d | 🔴 HIGH | PostgreSQL | ⏳ TODO |
| [N8N2](#n8n2-keycloak-sso-integration) | Keycloak SSO Integration | 300 LOC, 1d | 🔴 HIGH | Keycloak, N8N1 | ⏳ TODO |
| [N8N3](#n8n3-nginx-reverse-proxy) | Nginx Reverse Proxy | 200 LOC, 0.5d | 🔴 HIGH | Nginx, N8N1 | ⏳ TODO |
| [N8N4](#n8n4-workflow-templates-docs) | Workflow Templates & Docs | 500 LOC, 2d | 🟡 MEDIUM | N8N1-3 | ⏳ TODO |
| [N8N5](#n8n5-monitoring-alerting) | Monitoring & Alerting | 400 LOC, 1d | 🟡 MEDIUM | Prometheus, Grafana | ⏳ TODO |
| [N8N6](#n8n6-backend-bff-api) | Backend BFF API | 800 LOC, 3d | 🔴 HIGH | N8N1-3 | ⏳ TODO |

**Total Estimate:** ~2,600 LOC, ~8.5 developer days

---

## 🎯 Implementační Roadmap

### Phase 1: Workflow Executors (Priority 1) - 3 týdny
**Stories:** WF12, WF13, WF14, WF15, WF16

**Výstup:**
- ✅ APPROVAL executor (single, allOf, anyOf, quorum)
- ✅ REST_SYNC executor (OpenAPI, retry, circuit breaker)
- ✅ KAFKA_COMMAND executor (publish + callback)
- ✅ EXTERNAL_TASK executor (poll & complete)
- ✅ TIMER/DELAY executor

---

### Phase 2: Workflow Runtime (Priority 1) - 2 týdny
**Stories:** WF17

**Výstup:**
- ✅ workflow_instances table
- ✅ workflow_step_executions table
- ✅ Runtime orchestration (step-by-step execution)

---

### Phase 3: n8n Deployment (Priority 1) - 1 týden
**Stories:** N8N1, N8N2, N8N3

**Výstup:**
- ✅ n8n Docker service
- ✅ Keycloak SSO
- ✅ Nginx proxy

---

### Phase 4: n8n BFF API (Priority 1) - 1 týden
**Stories:** N8N6

**Výstup:**
- ✅ Spring Boot BFF controller
- ✅ JWT validation
- ✅ Tenant filtering

---

### Phase 5: Integration (Priority 2) - 2 týdny
**Stories:** WF18 + Integration Pattern

**Výstup:**
- ✅ Workflow steps schema v metamodel
- ✅ End-to-end Order approval use case
- ✅ Core → n8n integration via EXTERNAL_TASK

---

### ~~Phase 6: Connectors~~ ❌ **CANCELLED - n8n má built-in**
~~Stories: WF19, WF20, WF21, WF22~~

**Nahrazeno:** n8n 400+ built-in nodes (Jira, Confluence, Trello, HTTP, atd.)

**Nový Phase 6: n8n Templates** - 1 týden
**Stories:** N8N4

**Výstup:**
- ✅ Pre-built n8n workflows (Jira approval, Confluence sync, Trello automation)
- ✅ Dokumentace + screenshots
- ✅ Import templates do n8n

---

### Phase 7: Observability (Priority 3) - 1 týden
**Stories:** WF19 (renumbered from WF23), N8N5

**Výstup:**
- ✅ Grafana dashboards (Workflow + n8n)
- ✅ Alerting rules

---

## 📖 Story Details

### WF12: APPROVAL Executor

**Detail:** [stories/WF12-approval-executor/README.md](./EPIC-006-workflow-engine/stories/WF12-approval-executor/README.md)

**TL;DR:**
- Approval types: SINGLE, ALL_OF, ANY_OF, QUORUM
- Role-based + concrete users
- SLA tracking with escalations
- Email/Slack notifications
- Frontend ApprovalWidget.tsx

---

### WF13: REST_SYNC Executor

**Goal:** HTTP REST calls s retry, circuit breaker, timeout

**Features:**
- OpenAPI spec parsing
- HTTP client generation (OkHttp/WebClient)
- Retry with exponential backoff
- Circuit breaker integration (Resilience4j)
- Idempotent requests (correlation ID)
- Response mapping (JSON → variables)

**Config:**
```json
{
  "type": "REST_SYNC",
  "config": {
    "openapiRef": "/api/inventory#POST_allocate",
    "inputMap": {
      "orderId": "${entityId}",
      "items": "${items}"
    },
    "retry": {
      "maxAttempts": 3,
      "initialDelayMs": 1000,
      "maxDelayMs": 30000,
      "backoffMultiplier": 2.0
    },
    "timeout": 10000,
    "circuitBreaker": {
      "failureThreshold": 5,
      "resetTimeoutMs": 60000
    }
  }
}
```

**Implementation:**
- `RestSyncExecutor.java` - Executor impl
- OpenAPI parser integration
- OkHttp client with retry interceptor
- Resilience4j circuit breaker
- Variable substitution: `${varName}` → context values

**Tests:**
- Mock HTTP server (WireMock)
- Retry scenarios (503 → 503 → 200)
- Circuit breaker (5x 500 → OPEN → wait → HALF_OPEN)
- Timeout scenarios

**Effort:** 1,200 LOC, 5 days

---

### WF14: KAFKA_COMMAND Executor

**Goal:** Kafka publish s correlation-based callback

**Features:**
- Kafka producer integration
- Command/reply pattern (request-response via topics)
- Correlation ID tracking
- Timeout with DLQ fallback
- AsyncAPI schema validation

**Config:**
```json
{
  "type": "KAFKA_COMMAND",
  "config": {
    "topic": "core.commands.inventory",
    "eventType": "allocate-stock",
    "payload": {
      "orderId": "${entityId}",
      "items": "${items}"
    },
    "replyTopic": "core.replies.inventory",
    "correlationIdField": "correlationId",
    "timeoutMs": 30000,
    "dlqTopic": "core.dlq.inventory"
  }
}
```

**Implementation:**
- `KafkaCommandExecutor.java`
- Kafka producer (Spring Kafka)
- Correlation ID generation (UUID)
- Reply consumer with timeout
- DLQ fallback on timeout

**Tests:**
- Embedded Kafka (Testcontainers)
- Happy path: publish → receive reply
- Timeout scenario: publish → no reply → DLQ
- Multiple concurrent commands (correlation ID isolation)

**Effort:** 600 LOC, 2 days

---

### WF15: EXTERNAL_TASK Executor

**Goal:** Poll & complete pattern pro external workers

**Features:**
- Worker registration endpoint
- Poll tasks API
- Complete/fail task API
- Heartbeat mechanism
- Worker timeout detection

**Config:**
```json
{
  "type": "EXTERNAL_TASK",
  "config": {
    "taskType": "n8n-approval-flow",
    "endpoint": "https://admin.core-platform.local/api/n8n/workflows/approval/execute",
    "inputMap": {
      "orderId": "${entityId}"
    },
    "timeoutMinutes": 60,
    "pollIntervalMs": 5000
  }
}
```

**REST API:**
```java
// Worker polls for tasks
GET /api/workflows/external-tasks/poll?workerType={type}&limit=10

// Worker completes task
POST /api/workflows/external-tasks/{taskId}/complete
Body: { "output": {...} }

// Worker fails task
POST /api/workflows/external-tasks/{taskId}/fail
Body: { "error": "..." }

// Worker heartbeat
POST /api/workflows/external-tasks/{taskId}/heartbeat
```

**Implementation:**
- `ExternalTaskExecutor.java`
- `external_tasks` table (task_id, worker_type, status, input, output, timeout)
- Poll endpoint (long-polling support)
- Complete/fail handlers
- Heartbeat tracking
- Timeout checker (scheduled job)

**Tests:**
- Worker polls → receives task
- Worker completes → workflow continues
- Worker fails → workflow errors
- Timeout → task marked failed
- Heartbeat → extends timeout

**Effort:** 800 LOC, 3 days

---

### WF16: TIMER/DELAY Executor

**Goal:** Scheduled delayed actions

**Features:**
- Schedule action za X minut
- Reminder notifications
- Deadline enforcement
- Integration s WorkflowTimerService

**Config:**
```json
{
  "type": "TIMER",
  "config": {
    "delayMinutes": 60,
    "action": "remind",
    "notifyChannels": ["email", "slack"]
  }
}
```

**Implementation:**
- `TimerExecutor.java`
- Integration s `WorkflowTimerService` (W8)
- Schedule timer in `workflow_timers` table
- Timer fired → continue workflow

**Effort:** 400 LOC, 2 days

---

### WF17: Workflow Instance Runtime

**Goal:** Runtime orchestration pro workflow execution

**Database:**
```sql
CREATE TABLE workflow_instances (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  entity_type VARCHAR(100) NOT NULL,
  entity_id VARCHAR(100) NOT NULL,
  tenant_id VARCHAR(100) NOT NULL,
  workflow_version_id BIGINT REFERENCES workflow_versions(id),
  status VARCHAR(50) NOT NULL,  -- RUNNING, COMPLETED, FAILED, CANCELLED
  started_at TIMESTAMP NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMP,
  current_step_id VARCHAR(100),
  context JSONB,  -- runtime variables
  error_message TEXT
);

CREATE TABLE workflow_step_executions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workflow_instance_id UUID REFERENCES workflow_instances(id),
  step_id VARCHAR(100) NOT NULL,
  step_type VARCHAR(50) NOT NULL,
  status VARCHAR(50) NOT NULL,  -- PENDING, RUNNING, SUCCESS, FAILED, SKIPPED
  started_at TIMESTAMP,
  completed_at TIMESTAMP,
  input JSONB,
  output JSONB,
  error_message TEXT,
  retry_count INTEGER DEFAULT 0
);
```

**Java Service:**
```java
@Service
public class WorkflowInstanceService {
  
  // Start workflow
  WorkflowInstance startWorkflow(String entityType, String entityId, String tenantId);
  
  // Execute next step
  StepExecutionResult executeStep(UUID instanceId, String stepId);
  
  // Handle step completion
  void onStepComplete(UUID instanceId, String stepId, Map<String, Object> output);
  
  // Handle step failure
  void onStepFailure(UUID instanceId, String stepId, String error);
  
  // Check if workflow complete
  boolean isWorkflowComplete(UUID instanceId);
  
  // Cancel workflow
  void cancelWorkflow(UUID instanceId, String reason);
}
```

**Features:**
- Step-by-step execution orchestration
- Variable context management
- Error handling & retry
- Completion detection
- Cancellation support

**Effort:** 1,500 LOC, 6 days

---

### WF18: Workflow Steps Schema v Metamodel

**Goal:** Deklarativní workflow steps v entity schema

**Schema Extension:**
```json
{
  "entityName": "Order",
  "workflow": {
    "enabled": true,
    "states": [...],
    "transitions": [...],
    "steps": [  // ← NEW!
      {
        "id": "step-1",
        "type": "APPROVAL",
        "config": {...}
      },
      {
        "id": "step-2",
        "type": "REST_SYNC",
        "config": {...}
      }
    ]
  }
}
```

**Implementation:**
- Rozšířit `EntityDefinition.java` o `workflowSteps` field
- Validace step types (APPROVAL, REST_SYNC, atd.)
- Validace references (onSuccess, onError)
- Storage do `workflow_versions` JSONB

**Effort:** 600 LOC, 3 days

---

### ~~WF19-WF22: Connectors~~ ❌ **CANCELLED - DELEGATED TO n8n**

**Důvod zrušení:**
n8n poskytuje 400+ built-in nodes včetně všech plánovaných konektorů:

| Story | n8n Built-in Node | Capabilities |
|-------|-------------------|--------------|
| ~~WF19: Generic REST~~ | **HTTP Request node** | GET/POST/PUT/DELETE, headers, auth, ${} variables |
| ~~WF20: Jira~~ | **Jira node** | Create/update issues, transitions, comments, JQL queries |
| ~~WF21: Confluence~~ | **Confluence node** | Create/update pages, add comments, attach files |
| ~~WF22: Trello~~ | **Trello node** | Create/move cards, add members, labels, attachments |

**Co NEPOTŘEBUJEME:**
- ❌ Custom OpenAPI parser (n8n HTTP Request node má built-in)
- ❌ Jira API client (n8n Jira node je maintained)
- ❌ Confluence/Trello API wrappers (n8n má native support)
- ❌ Webhook listeners (n8n má Webhook Trigger node)

**Co MÍSTO TOHO IMPLEMENTUJEME:**
- ✅ **WF15: EXTERNAL_TASK Executor** - Core workflow deleguje úkoly na n8n workflows
- ✅ **N8N6: Backend BFF API** - Core volá n8n REST API pro spuštění workflows
- ✅ **N8N4: Workflow Templates** - Pre-built n8n flows (Jira approval, Confluence sync, atd.)

**Úspora:**
- **4,100 LOC** custom connector code eliminováno
- **13 developer days** saved
- **0 maintenance** (n8n updates connectors)
- **Visual builder** pro business users

---

### WF19: Workflow Grafana Dashboards

**Goal:** Observability dashboards pro workflow execution

**Dashboards:**
1. **Workflow Overview:**
   - Instance counts (running, completed, failed)
   - Success/fail rates
   - Average duration

2. **Workflow Details:**
   - Per-workflow metrics
   - Step durations
   - Retry rates

3. **SLA Monitoring:**
   - SLA warnings
   - SLA breaches
   - Escalations

**Alerting:**
- Stuck workflows (>24h)
- High error rate (>10%)

**Effort:** 300 LOC, 1 day

---

### N8N1: n8n Platform Deployment

**Detail:** Viz [WORKFLOW_N8N_AUDIT_2025.md](../WORKFLOW_N8N_AUDIT_2025.md#n8n1-n8n-platform-deployment-400-loc-4h)

**TL;DR:**
- Docker Compose service definition
- PostgreSQL database (`n8n` DB)
- Environment: `N8N_BASIC_AUTH_ACTIVE=false` (SSO via Keycloak)

**Effort:** 400 LOC, 1 day

---

### N8N2: Keycloak SSO Integration

**Goal:** n8n login přes Keycloak OIDC

**Implementation:**
- Keycloak client: `n8n-client`
- Redirect URIs: `/n8n/*`, `/n8n/callback`
- Client roles: `n8n-users`, `n8n-admins`
- n8n SSO hook (`docker/n8n/hooks/keycloak-sso.js`)

**Effort:** 300 LOC, 1 day

---

### N8N3: Nginx Reverse Proxy

**Goal:** Proxy `/n8n/*` → n8n service

**Config:**
```nginx
location /n8n/ {
  proxy_pass http://n8n:5678/;
  proxy_http_version 1.1;
  proxy_set_header Upgrade $http_upgrade;
  proxy_set_header Connection "upgrade";
}
```

**Effort:** 200 LOC, 0.5 day

---

### N8N4: Workflow Templates & Docs

**Goal:** Reusable workflow templates

**Templates:**
1. Jira Integration (webhook → create ticket)
2. Slack Notification (Kafka → send message)
3. AI Pipeline (HTTP → OpenAI → store result)
4. ETL (cron → fetch data → POST to BFF)

**Docs:**
- Integration guide (Kafka, webhooks, REST API)
- Multi-tenancy patterns

**Effort:** 500 LOC, 2 days

---

### N8N5: Monitoring & Alerting

**Goal:** Prometheus + Grafana for n8n

**Metrics:**
- `workflow_executions_total`
- `workflow_errors_total`
- `workflow_duration_seconds`

**Dashboard:**
- Workflow executions (last 24h)
- Error rate (%)
- Average execution duration

**Alerts:**
- `N8nHighErrorRate` (>10%)
- `N8nServiceDown`

**Effort:** 400 LOC, 1 day

---

### N8N6: Backend BFF API

**Goal:** Spring Boot proxy pro n8n REST API

**Endpoints:**
```java
GET /api/n8n/workflows              // List workflows (cached 5min)
POST /api/n8n/workflows/{id}/execute // Execute workflow
GET /api/n8n/executions/{id}        // Get execution status
```

**Features:**
- JWT validation
- Tenant filtering
- Cache (5 minutes)
- Audit logging

**Effort:** 800 LOC, 3 days

---

## 📝 Next Steps

1. **Review & Prioritize:** Product team schválí priority
2. **Create Subtasks:** Pro každou story vytvořit tasky
3. **Assign:** Přiřadit stories do sprintů
4. **Implement:** Start with Phase 1 (WF12-WF16)

---

**Vytvořeno:** 8. listopadu 2025  
**Autor:** GitHub Copilot (Assistant)  
**Zdroj:** [WORKFLOW_N8N_AUDIT_2025.md](../WORKFLOW_N8N_AUDIT_2025.md)
