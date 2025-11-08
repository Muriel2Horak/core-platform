# EPIC-011: n8n External Orchestration Layer

> ⚠️ **MERGED WITH EPIC-006:** Tento EPIC je nyní **Phase 3** unified Workflow Orchestration architektury.  
> 📖 **See:** [`WORKFLOW_UNIFIED_ARCHITECTURE.md`](../WORKFLOW_UNIFIED_ARCHITECTURE.md) pro kompletní design.

**Status:** 🔴 **0% IMPLEMENTED** (planned for Week 4-5)  
**Dependencies:** EPIC-006 Phase 2 (WF15 EXTERNAL_TASK executor)  
**LOC:** ~2,600  
**Roadmap:** Week 4 of unified implementation

---

## 🎯 Epic Goal

Deploy **n8n Community Edition** jako external orchestration hub pro:
- 🔌 **Integrace třetích stran** (Jira, Confluence, Trello, M365, Google, Slack)
- 🤖 **AI/ML pipelines** (Langchain, OpenAI, local LLMs)
- 📊 **ETL/batch jobs** (CSV export, data transformation)
- 🚀 **Visual workflow builder** (400+ built-in nodes)

**Integration:** n8n workflows voláné z Core Platform via **EXTERNAL_TASK executor (WF15)**.

---

## 🏗️ Architecture (Layer 2 of Unified Workflow)

```text
┌─────────────────────────────────────────────────────────────┐
│  LAYER 1: INTERNAL WORKFLOW ENGINE (EPIC-006)               │
│  - Core business procesy (Order, Invoice, Contract)         │
│  - Typed executors: APPROVAL, REST_SYNC, KAFKA_COMMAND      │
└────────────────────┬────────────────────────────────────────┘
                     │ WF15: EXTERNAL_TASK executor
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  LAYER 2: EXTERNAL n8n ORCHESTRATION (EPIC-011)             │
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ n8n Platform │    │ Backend BFF  │    │ Nginx Proxy  │ │
│  │ (N8N1)       │◄───│ API (N8N6)   │◄───│ (N8N3)       │ │
│  │ - Workflows  │    │ - JWT valid  │    │ - /n8n/*     │ │
│  │ - 400+ nodes │    │ - Tenant     │    │ - Keycloak   │ │
│  │ - PostgreSQL │    │ - Rate limit │    │   SSO (N8N2) │ │
│  └──────┬───────┘    └──────────────┘    └──────────────┘ │
│         │                                                   │
│         ▼                                                   │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ Built-in Nodes (No Custom Code Needed!)              │ │
│  │ - Jira, Confluence, Trello                           │ │
│  │ - Slack, Gmail, Google Sheets                        │ │
│  │ - HTTP Request, Webhook                              │ │
│  │ - OpenAI, Langchain                                  │ │
│  └──────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```


## 📊 Component Overview

| Component | Purpose | Port | Tech Stack | Status |
|-----------|---------|------|------------|--------|
| **n8n** | Workflow automation engine | 5678 | Node.js, PostgreSQL | ⏳ TODO (N8N1) |
| **Nginx** | Reverse proxy, /n8n/* routing | 443 | Nginx 1.25+ | ⏳ TODO (N8N3) |
| **Backend BFF** | n8n API proxy, monitoring | 8080 | Spring Boot, WebClient | ⏳ TODO (N8N6) |
| **Keycloak** | SSO identity provider | 8443 | Java, PostgreSQL | ✅ EXISTS |
| **Templates** | Pre-built n8n workflows | - | JSON exports | ⏳ TODO (N8N4) |
| **Monitoring** | Grafana dashboards | 3000 | Grafana | ⏳ TODO (N8N5) |

---

## 🎯 Success Metrics

- **Security**: 100% n8n access requires Keycloak SSO login
- **Availability**: 99.9% uptime (n8n + BFF)
- **Performance**: <200ms BFF API latency, <2s n8n UI load
- **Adoption**: 50+ workflows created within first month
- **Integration**: 10+ external systems connected (Jira, Confluence, Trello, M365, Google)

---

## 📋 Stories

### N8N1: Platform Deployment (~400 LOC, 1 day)

**Goal**: Deploy n8n Community Edition s PostgreSQL backend

**Deliverables**:
- Docker Compose service definition
- PostgreSQL database `n8n` (separate from `core`)
- DB user: `n8n_app` (separate credentials)
- Environment config (webhook URL, protocol, host)
- Execution retention: 30 days
- Volume mounts: `/root/.n8n`

**Docker Compose:**
```yaml
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
  ports:
    - "5678"  # Internal only, NOT exposed to host
  depends_on:
    - core-db
```

**Acceptance Criteria**:
- ✅ n8n accessible at http://n8n:5678 (internal network)
- ✅ PostgreSQL stores workflow definitions + execution history
- ✅ Webhooks functional for external integrations

**Effort**: ~4 hours | **Details**: [stories/N8N1.md](./stories/N8N1.md)

---

### N8N2: Keycloak SSO Integration (~300 LOC, 1 day)

**Goal**: Configure Keycloak client for n8n SSO authentication

**Deliverables**:
- Keycloak client: `n8n-client`
- Redirect URIs: `https://admin.core-platform.local/n8n/*`
- Client roles: `n8n-users`, `n8n-admins`
- User group mapping
- JWT token configuration
- n8n OAuth2 config (disable basic auth)

**Acceptance Criteria**:
- ✅ Users redirected to Keycloak login when accessing /n8n
- ✅ Successful login grants access to n8n UI
- ✅ Roles n8n-users and n8n-admins enforced

**Effort**: ~1 day | **Details**: [stories/N8N2.md](./stories/N8N2.md)

---

### N8N3: Nginx Reverse Proxy (~200 LOC, 0.5 day)

**Goal**: Configure Nginx to proxy /n8n/* to n8n with SSO enforcement

**Deliverables**:
- Nginx location block:
```nginx
location /n8n/ {
  auth_request /auth;  # Keycloak validation
  proxy_pass http://n8n:5678/;
  proxy_set_header X-Forwarded-Proto https;
  proxy_set_header Upgrade $http_upgrade;  # WebSocket support
  proxy_set_header Connection "upgrade";
}
```
- SSL termination
- WebSocket support (n8n editor)

**Acceptance Criteria**:
- ✅ n8n accessible at https://admin.core-platform.local/n8n/
- ✅ Keycloak SSO enforcement
- ✅ WebSocket connections work (editor UX)

**Effort**: ~0.5 day | **Details**: [stories/N8N3.md](./stories/N8N3.md)

---

### N8N4: Workflow Templates (~500 LOC, 2 days)

**Goal**: Pre-built n8n workflows pro common use cases

**Deliverables**:
- n8n workflow JSON exports:
  1. **jira-create-issue.json**
     - Trigger: Webhook (from Core via WF15)
     - Node 1: Jira create issue
     - Node 2: HTTP callback → `/external-tasks/{id}/complete`
  2. **confluence-sync.json**
     - Trigger: Webhook
     - Node 1: Confluence update page
     - Node 2: Callback
  3. **trello-automation.json**
     - Trigger: Webhook
     - Node 1: Trello create card
     - Node 2: Callback
  4. **ai-summarization.json**
     - Trigger: Webhook
     - Node 1: OpenAI API (text summarization)
     - Node 2: Callback

- Import script:
```bash
n8n import:workflow --input=templates/jira-create-issue.json
```

- Documentation (README per template):
  - Input schema
  - Output schema
  - Configuration steps
  - Screenshots

**Acceptance Criteria**:
- ✅ All 4 templates imported and functional
- ✅ Documentation complete
- ✅ End-to-end test: Core workflow → n8n template → external system

**Effort**: ~2 days | **Details**: [stories/N8N4.md](./stories/N8N4.md)

---

### N8N5: Monitoring & Alerting (~400 LOC, 1 day)

**Goal**: Grafana dashboards pro n8n execution monitoring

**Deliverables**:
- Grafana dashboard: `n8n-monitoring.json`
  - Panels:
    - Active workflows count
    - Executions/hour (by workflow)
    - Success rate (%)
    - Avg execution duration
    - Failed executions (last 24h)
  - Data source: n8n PostgreSQL DB
  - Queries:
    ```sql
    -- Active workflows
    SELECT COUNT(*) FROM workflows WHERE active = true;
    
    -- Executions/hour
    SELECT COUNT(*) FROM executions 
    WHERE finished_at > NOW() - INTERVAL '1 hour';
    
    -- Success rate
    SELECT 
      SUM(CASE WHEN finished = true AND success = true THEN 1 ELSE 0 END) * 100.0 / COUNT(*) as success_rate
    FROM executions
    WHERE started_at > NOW() - INTERVAL '24 hours';
    ```

- Alerting rules:
  - n8n workflow execution failure rate > 10%
  - n8n API latency > 5s

**Acceptance Criteria**:
- ✅ Grafana dashboard displays real-time n8n metrics
- ✅ Alerts triggered on thresholds

**Effort**: ~1 day | **Details**: [stories/N8N5.md](./stories/N8N5.md)

---

### N8N6: Backend BFF API ⚡ **KLÍČOVÝ PRO INTEGRACI** (~800 LOC, 3 days)

**Goal**: Spring Boot proxy pro n8n REST API s JWT validation, tenant filtering, rate limiting

**Deliverables**:
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
    String userId = jwt.getSubject();
    
    // 2. Rate limit check (Redis-based)
    if (!rateLimiter.allowRequest(tenantId, userId)) {
      throw new RateLimitExceededException();
    }
    
    // 3. Audit log
    auditLog.log("n8n_workflow_execute", workflowId, input, userId);
    
    // 4. Call n8n REST API
    return webClient.post()
      .uri("http://n8n:5678/api/v1/workflows/{id}/execute", workflowId)
      .header("Authorization", "Bearer " + n8nApiToken)
      .bodyValue(input)
      .retrieve()
      .toEntity(ExecutionResult.class)
      .block();
  }
  
  @GetMapping("/workflows")
  @Cacheable(value = "n8n-workflows", ttl = 300)  // 5 min cache
  public List<WorkflowSummary> listWorkflows(@AuthenticationPrincipal Jwt jwt) {
    String tenantId = jwt.getClaim("tenant_id");
    
    // Filter workflows by tenant (if multi-tenant support added)
    return webClient.get()
      .uri("http://n8n:5678/api/v1/workflows")
      .retrieve()
      .bodyToFlux(WorkflowSummary.class)
      .collectList()
      .block();
  }
  
  @GetMapping("/executions/{executionId}")
  public ExecutionDetail getExecution(
    @PathVariable String executionId,
    @AuthenticationPrincipal Jwt jwt
  ) {
    return webClient.get()
      .uri("http://n8n:5678/api/v1/executions/{id}", executionId)
      .retrieve()
      .bodyToMono(ExecutionDetail.class)
      .block();
  }
}
```

- Features:
  - JWT validation (Keycloak token)
  - Tenant extraction from token
  - Rate limiting (100 requests/minute per user)
  - Cache (workflow definitions, 5 min TTL)
  - Audit logging (kdo volal n8n API, kdy, s jakými daty)

**API Endpoints:**
- `POST /api/n8n/workflows/{id}/execute` - Execute n8n workflow
- `GET /api/n8n/workflows` - List all workflows (cached)
- `GET /api/n8n/executions/{id}` - Get execution details

**Acceptance Criteria**:
- ✅ JWT validation funguje (reject unauthorized)
- ✅ Rate limiting enforced (429 Too Many Requests)
- ✅ Audit log records all API calls
- ✅ Cache reduces n8n API load

**Effort**: ~3 days | **Details**: [stories/N8N6.md](./stories/N8N6.md)

---

## 🔗 Integration with Core Workflow (EPIC-006)

### EXTERNAL_TASK Executor Pattern

**Flow:**
```
Core Workflow (WF17 Orchestrator)
  ↓
WF15: ExternalTaskExecutor.execute()
  ↓ creates external_task record (status=PENDING)
  ↓
BFF API (N8N6): POST /api/n8n/workflows/{id}/execute
  ↓ JWT validation, rate limit, audit
  ↓
n8n Workflow Execution (N8N1)
  ↓ Webhook trigger → Jira node → HTTP callback node
  ↓
Callback: POST /api/workflows/external-tasks/{taskId}/complete
  ↓ update external_task (status=COMPLETED, output={jiraKey: "PROJ-123"})
  ↓
WF17: Orchestrator continues to next step
  ↓ uses ${step-2.output.jiraKey} in step-3
```

**Example Workflow Step (v metamodelu):**
```json
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
}
```

---

## 📊 Overall Impact

### Metrics
- **Integration Coverage**: 400+ external systems available (Jira, Confluence, Trello, M365, Google, Slack, atd.)
- **Development Speed**: 0 LOC custom connectors (leverage n8n nodes)
- **Maintenance**: 0 hours (n8n updates connectors)
- **Business Agility**: Visual builder → business users can create integrations

### Business Value
- **Cost Savings**: No custom connector development ($50k+ saved)
- **Faster Integration**: Hours (vs. weeks for custom code)
- **Plug & Play**: 400+ nodes ready to use
- **Visual Builder**: Business users empowered (no dev needed)

---

## 🎯 Roadmap (Week 4-5 of Unified Implementation)

### Week 4: n8n Deployment
- **Day 1**: N8N1 Platform Deployment
- **Day 2**: N8N2 Keycloak SSO + N8N3 Nginx Proxy
- **Day 3-5**: N8N6 Backend BFF API
- **Day 6-7**: N8N4 Workflow Templates
- **Day 8**: N8N5 Monitoring

### Week 5: Integration Testing
- E2E test: Order approval → EXTERNAL_TASK → n8n Jira flow → callback → ERP sync
- Timeout scenarios: n8n nereaguje → onError routing
- Load testing: 100 concurrent n8n executions

---

## 🚀 Next Steps

1. ✅ **Complete WF15** (EXTERNAL_TASK executor) - prerequisite pro integraci
2. ⏳ **Deploy n8n** (N8N1) - Docker + PostgreSQL
3. ⏳ **Setup SSO** (N8N2) + Proxy (N8N3)
4. ⏳ **Implement BFF** (N8N6) - Spring Boot proxy
5. ⏳ **Create Templates** (N8N4) - Jira, Confluence, Trello, AI
6. ⏳ **E2E Test** - Core → n8n → Jira → callback

---

**For complete architecture, see:**
- [`WORKFLOW_UNIFIED_ARCHITECTURE.md`](../WORKFLOW_UNIFIED_ARCHITECTURE.md) - 2-layer design
- [EPIC-006 README](../EPIC-006-workflow-engine/README.md) - Internal workflow engine (Layer 1)
- Proxy pass to http://n8n:5678
- Keycloak auth_request integration (via oauth2-proxy or native)
- Webhook bypass (public access for /n8n/webhook/*)
- SSL termination

**Acceptance Criteria**:
- ✅ https://admin.core-platform.local/n8n redirects to Keycloak
- ✅ Authenticated users access n8n UI
- ✅ Webhooks accessible without authentication

**Effort**: ~3 hours | **Details**: [S3.md](./stories/S3.md)

---

### S4: Workflow Templates & Documentation (~300 LOC)

**Goal**: Provide starter workflow templates and user documentation

**Deliverables**:
- Workflow templates (HTTP request, database query, email notification)
- User guide (creating workflows, managing executions)
- Best practices documentation
- Troubleshooting guide

**Acceptance Criteria**:
- ✅ 3+ starter workflow templates available
- ✅ User guide published (Markdown in docs/)
- ✅ Best practices documented

**Effort**: ~2 hours | **Details**: [S4.md](./stories/S4.md)

---

### S5: Monitoring & Alerting Integration (~300 LOC)

**Goal**: Integrate n8n metrics with Grafana monitoring

**Deliverables**:
- n8n Prometheus metrics exporter configuration
- Grafana dashboard (workflow executions, success/failure rate)
- Alerting rules (execution failures >10% in 5min)
- Loki log aggregation for n8n logs

**Acceptance Criteria**:
- ✅ n8n metrics visible in Grafana
- ✅ Dashboard shows workflow execution stats
- ✅ Alerts trigger on failure threshold

**Effort**: ~4 hours | **Details**: [S5.md](./stories/S5.md)

---

---

### S6: Backend n8n Integration (BFF Pattern) (~1,200 LOC)

**Goal**: Enable workflow monitoring in React frontend via backend BFF API

**Deliverables**:
- n8n REST API client (WebClient, timeout 10s, API key auth)
- Domain models (N8nWorkflow, N8nExecution, sanitized nodes)
- BFF proxy controller with role-based access (@PreAuthorize)
- DTOs (WorkflowSummaryDTO, ExecutionDTO, ExecutionDetailDTO)
- Cache configuration (Caffeine, 5 min TTL, n8n-workflows)
- Frontend React dashboard (WorkflowDashboard.tsx)
- TypeScript types (WorkflowSummary, Execution, ExecutionStatus)
- Real-time monitoring (5s polling, auto-refresh executions)
- Role-based UI (users: read-only, admins: activate/deactivate + n8n link)
- Integration tests (@SpringBootTest, MockMvc, WireMock)
- E2E tests (Playwright, workflow-dashboard.spec.ts)

**Acceptance Criteria**:
- ✅ GET /api/n8n/workflows returns workflow list (JWT authenticated)
- ✅ Frontend dashboard shows real-time workflow status
- ✅ Admin users can activate/deactivate workflows
- ✅ "Open in n8n" link visible only for admins
- ✅ Auto-refresh every 5 seconds
- ✅ Cache hit rate >80%

**Effort**: ~23 hours | **Details**: [S6.md](./stories/S6.md)

---

## 🔐 Security Features

- **SSO**: Keycloak OIDC integration (existing identity provider)
- **JWT Authentication**: All backend API calls require valid JWT token
- **Role-Based Access**: n8n-users (read-only), n8n-admins (full access)
- **Credential Sanitization**: BFF strips sensitive data from n8n API responses
- **Audit Logging**: All admin actions logged (activate/deactivate workflows)
- **Network Isolation**: n8n internal-only, accessible via Nginx proxy or BFF API
- **Webhook Security**: Public webhooks for integrations (no auth required)

## 🚀 Implementation Plan

### Phase 1: Foundation (Week 1)

- ✅ S1: Deploy n8n + PostgreSQL
- ✅ S2: Configure Keycloak SSO client
- ✅ S3: Nginx reverse proxy setup

**DoD**: n8n accessible via SSO at https://admin.core-platform.local/n8n

### Phase 2: Templates & Monitoring (Week 2)

- ✅ S4: Workflow templates + documentation
- ✅ S5: Grafana monitoring integration

**DoD**: Starter templates available, metrics in Grafana

### Phase 3: Backend Integration (Week 3)

- ✅ S6: Backend BFF API + React dashboard

**DoD**: Workflow monitoring dashboard operational, real-time updates

## 📚 Documentation

- **N8N_SETUP_GUIDE.md**: Installation and configuration
- **N8N_WORKFLOW_TEMPLATES.md**: Starter workflow examples
- **N8N_USER_GUIDE.md**: End-user workflow creation guide
- **N8N_API_DOCUMENTATION.md**: Backend BFF API reference

## 🎓 Dependencies

- **External**: Keycloak (existing EPIC-003 Monitoring & Observability)
- **Infrastructure**: Nginx, Docker, PostgreSQL
- **Backend**: Spring Boot, WebClient, Spring Security
- **Frontend**: React, TypeScript, Axios
- **Skills**: n8n workflow development, REST API integration, OAuth2/OIDC

## 🏁 Definition of Done

- [ ] All 6 stories implemented with acceptance criteria met
- [ ] n8n running in Docker Compose with PostgreSQL backend
- [ ] 100% UI access requires Keycloak SSO login
- [ ] Webhooks publicly accessible (no auth) at /n8n/webhook/*
- [ ] Keycloak client configured with n8n-users and n8n-admins roles
- [ ] Nginx proxy routes /n8n/* to n8n service
- [ ] Backend BFF API operational (GET /api/n8n/workflows, POST /api/n8n/workflows/:id/activate)
- [ ] Frontend dashboard showing workflow status (active, executions, success rate)
- [ ] Real-time execution monitoring (5s polling)
- [ ] Role-based access enforced (users: read-only, admins: activate/deactivate)
- [ ] Grafana dashboard displays n8n metrics (executions, success rate)
- [ ] Alerting configured (execution failures >10% threshold)
- [ ] Workflow templates available (3+ starter examples)
- [ ] Documentation complete (setup guide, user guide, API docs)
- [ ] E2E tests passing (n8n login, workflow monitoring dashboard)

---

**Epic Owner**: Platform Team  
**Priority**: Medium  
**Target**: Q1 2026  
**Estimated Effort**: ~40 hours (~1 week, 1 engineer)  
**Status**: 📝 Documentation complete, awaiting implementation

**Last Updated**: 2025-11-07
