# EPIC-011: n8n Integration & Orchestration Hub

> 🏛️ **DESIGN DECISION:** n8n is a **mandatory core component** of Virelio/Core Platform.  
> 📖 **Architecture:** See [`EPIC-007-infrastructure-deployment`](../EPIC-007-infrastructure-deployment/README.md) for infrastructure setup.

**Status:** 🔴 **0% IMPLEMENTED** (Week 4-5)  
**Dependencies:** EPIC-007 (n8n platform deployment), EPIC-006 (optional: WF15 EXTERNAL_TASK executor)  
**LOC:** ~2,600  
**Roadmap:** Week 4 of core platform rollout

---

## 🏛️ Design Decision: n8n jako Core Platform Component

**n8n is a mandatory core component of Virelio/Core Platform.**

### Rationale

1. **Primary Integration & Orchestration Engine**
   - 400+ built-in nodes pro external systems (Jira, M365, Slack, Trello, Google Workspace)
   - Visual workflow builder (no-code integrations)
   - AI orchestration via MCP/LLM gateway (EPIC-016)
   - ETL/batch processing (CSV export, data transformation)

2. **Security & Governance**
   - **Always behind Keycloak SSO** (admin realm only)
   - **Always behind Nginx reverse proxy** (SSL termination, rate limiting)
   - **Observed via Loki/Prometheus** (logs + metrics)
   - RBAC: `CORE_PLATFORM_ADMIN`, `INTEGRATION_ADMIN` roles

3. **Architectural Principles**
   - n8n is NOT for internal Core business processes (that's EPIC-006 Workflow Engine)
   - n8n IS for: external integrations, AI workflows, ETL/batch jobs
   - n8n běží POUZE v admin realm/admin tenantovi
   - n8n přistupuje k Core POUZE přes API/eventy (NO direct DB access)

4. **Deployment Requirements**
   - Docker/K8s deployment (same tier as Backend, Keycloak, PostgreSQL)
   - PostgreSQL database `n8n` (separate from `core`)
   - Health checks, volume persistence
   - Loki log shipping, Prometheus metrics scraping

### Non-Goals

- ❌ **No public n8n endpoints** (always behind Nginx + Keycloak)
- ❌ **No per-tenant n8n instances** (single admin realm instance)
- ❌ **No direct database access** from n8n (API/events only)
- ❌ **No replacement of EPIC-006 Workflow Engine** (different scopes)

---

## 🎯 Epic Goal

Configure **n8n Community Edition** jako mandatory integration hub pro:
- 🔌 **External System Integrations** (Jira, Confluence, Trello, M365, Google Workspace, Slack)
- 🤖 **AI Workflow Orchestration** (MCP/LLM gateway, document classification, enrichment)
- 📊 **ETL/Batch Processing** (CSV export, data transformation, scheduled reports)
- 🚀 **Visual No-Code Automation** (400+ built-in nodes, JSON workflow templates)
- 🔐 **Secure Multi-Tenant Access** (Keycloak SSO, RBAC, audit logging)

**Access:** n8n běží na `https://admin.${DOMAIN}/n8n` (admin realm only).  
**Integration:** n8n může volat Core Platform API, poslouchat Core eventy, orchestrovat AI/MCP calls.

---

## 🏗️ Architecture: Core Integration & Orchestration Layer

```text
┌──────────────────────────────────────────────────────────────────────┐
│  ADMIN REALM / ADMIN TENANT (core-platform.local)                    │
│                                                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ USER                                                            │ │
│  │  └─→ https://admin.core-platform.local/n8n (Keycloak login)   │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                               │                                       │
│                               ▼                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ NGINX REVERSE PROXY (SSL Termination, Rate Limiting)           │ │
│  │  Route: /n8n/* → http://n8n:5678/                              │ │
│  │  Auth: Keycloak SSO required (auth_request /auth)              │ │
│  │  CSP: frame-ancestors 'self'                                   │ │
│  │  Rate Limit: 50 req/s per IP                                   │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                               │                                       │
│                               ▼                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ n8n COMMUNITY EDITION (Workflow Automation Engine)             │ │
│  │  Port: 5678 (internal only)                                    │ │
│  │  Database: PostgreSQL (database: n8n, user: n8n_app)           │ │
│  │  Features: 400+ nodes, visual builder, webhook support         │ │
│  └────────┬──────────────┬──────────────┬────────────────────────┘ │
│           │              │              │                            │
│           ▼              ▼              ▼                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────┐ │
│  │ Core API    │  │ Core Events │  │ External Systems            │ │
│  │ (REST)      │  │ (Kafka/CDC) │  │ - Jira, Confluence, Trello  │ │
│  │ - GET /api/ │  │ - Tenant    │  │ - M365, Google Workspace    │ │
│  │   tenants   │  │   created   │  │ - Slack, Gmail              │ │
│  │ - POST /api/│  │ - User      │  │ - OpenAI, MCP/LLM gateway   │ │
│  │   workflows │  │   onboard   │  └─────────────────────────────┘ │
│  └─────────────┘  └─────────────┘                                   │
│                                                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ OBSERVABILITY                                                   │ │
│  │  - Loki: n8n logs {service="n8n", tenant="admin"}              │ │
│  │  - Prometheus: n8n metrics (if available)                      │ │
│  │  - Grafana: n8n dashboard (workflow executions, error rates)   │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

### Integration Patterns

**Pattern 1: External System Sync**
```
Core Platform Event (Tenant Created)
  → Kafka → n8n Webhook Trigger
  → n8n Workflow:
      1. Create Jira Project
      2. Create Confluence Space
      3. Send Slack notification
      4. Call Core API: POST /api/integrations/sync-status
```

**Pattern 2: AI Workflow Orchestration**
```
User uploads document → Core API
  → n8n Workflow (via Core API call):
      1. Call MCP/LLM gateway: classify document
      2. Extract metadata (OpenAI API)
      3. Store results: POST /api/documents/{id}/metadata
      4. Trigger follow-up workflow (email notification)
```

**Pattern 3: Scheduled ETL/Batch**
```
n8n Cron Trigger (every day 2am)
  → n8n Workflow:
      1. GET /api/reports/daily-export
      2. Transform to CSV
      3. Upload to S3/MinIO
      4. Send email with download link
```

### Security & Compliance

1. **Authentication & Authorization**
   - SSO přes Keycloak admin realm
   - Roles: `CORE_PLATFORM_ADMIN`, `INTEGRATION_ADMIN`
   - n8n user management DISABLED (Keycloak only)

2. **Access Control**
   - n8n UI dostupné POUZE admin realm users
   - n8n může volat Core API POUZE přes backend (ne přímo na DB)
   - Webhooky chráněny Nginx rate limiting

3. **Audit & Logging**
   - Všechny workflow executions logovány do Loki
   - Error tracking: Loki {level="error", service="n8n"}
   - Metrics: Prometheus scraping (pokud n8n expose /metrics)

4. **Data Governance**
   - n8n workflow data stored v PostgreSQL `n8n` database
   - Retention policy: 30 days execution history
   - NO direct DB access from n8n (API/events only)

---


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

### N8N4: Reference Workflows & Templates (~500 LOC, 2 days)

**Goal**: Pre-built n8n workflows pro common integration patterns

**Deliverables**:

**1. External System Sync Templates:**
- **`jira-sync-on-tenant-created.json`**
  - Trigger: Webhook (Kafka CDC event: tenant.created)
  - Actions:
    1. Jira: Create Project (key=tenant.subdomain)
    2. Jira: Create default issue types
    3. Core API: POST /api/integrations/jira/sync-status
  - Security: Webhook URL protected by Nginx rate limiting

- **`confluence-space-onboarding.json`**
  - Trigger: Webhook (tenant.created event)
  - Actions:
    1. Confluence: Create Space (key=tenant.subdomain)
    2. Confluence: Apply default templates
    3. Core API: POST /api/integrations/confluence/sync-status

- **`slack-notification-workflow.json`**
  - Trigger: Webhook (user.onboarded event)
  - Actions:
    1. Slack: Post message to #onboarding channel
    2. Slack: Send DM to user
    3. Core API: POST /api/notifications/sent

**2. AI Orchestration Templates:**
- **`ai-document-classification.json`**
  - Trigger: HTTP Request (from Core API: POST /workflows/classify-document)
  - Actions:
    1. MCP/LLM Gateway: Classify document (invoice, contract, receipt)
    2. Extract metadata (date, amount, parties)
    3. Core API: POST /api/documents/{id}/metadata
  - Security: API key validation

- **`ai-email-enrichment.json`**
  - Trigger: Webhook (email.received event)
  - Actions:
    1. OpenAI: Summarize email body
    2. OpenAI: Extract action items
    3. Core API: POST /api/emails/{id}/enrichment

**3. ETL/Batch Processing Templates:**
- **`csv-export-daily.json`**
  - Trigger: Cron (every day 2am)
  - Actions:
    1. Core API: GET /api/reports/daily-export
    2. Transform to CSV
    3. MinIO: Upload to S3 bucket
    4. Email: Send download link to admins

**Import Script:**
```bash
#!/bin/bash
# scripts/n8n/import-templates.sh

templates=(
  "jira-sync-on-tenant-created.json"
  "confluence-space-onboarding.json"
  "slack-notification-workflow.json"
  "ai-document-classification.json"
  "ai-email-enrichment.json"
  "csv-export-daily.json"
)

for template in "${templates[@]}"; do
  echo "Importing $template..."
  docker exec core-n8n n8n import:workflow --input=/templates/$template
done
```

**Documentation (per template):**
- Input schema (webhook payload/API request)
- Output schema (API responses)
- Configuration steps (API keys, credentials)
- Screenshots (workflow canvas)

**Acceptance Criteria**:
- ✅ All 6 templates imported and functional
- ✅ Documentation complete
- ✅ End-to-end test: Core event → n8n workflow → external system → Core API callback

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

### N8N6: Testing & Quality Gates (~600 LOC, 2 days)

**Goal**: Ověřit security, compliance a funkčnost n8n integrace

**Deliverables**:

**1. Security Tests:**
- **Test Scenario 1: Unauthorized Access**
  ```bash
  # Test: n8n UI vyžaduje Keycloak SSO
  curl -I https://admin.core-platform.local/n8n
  # Expected: 302 redirect to Keycloak login
  # Expected: NO 200 response without auth
  ```

- **Test Scenario 2: Direct DB Access Prevention**
  ```java
  @Test
  void n8nCannotAccessCoreDatabase() {
    // Verify n8n DB user 'n8n_app' nemá GRANT na 'core' schema
    assertThrows(SQLException.class, () -> {
      Connection conn = DriverManager.getConnection(
        "jdbc:postgresql://core-db:5432/core",
        "n8n_app",
        System.getenv("N8N_DB_PASSWORD")
      );
    });
  }
  ```

- **Test Scenario 3: API-Only Access**
  ```typescript
  // Verify n8n workflows can ONLY call Core API
  // NOT direct SQL queries
  
  test('n8n workflow uses Core API not direct DB', async () => {
    const workflow = await loadWorkflow('jira-sync-on-tenant-created.json');
    const nodes = workflow.nodes;
    
    // Assert: NO PostgreSQL nodes in workflow
    const dbNodes = nodes.filter(n => n.type === 'n8n-nodes-base.postgres');
    expect(dbNodes).toHaveLength(0);
    
    // Assert: HTTP Request nodes target Core API
    const httpNodes = nodes.filter(n => n.type === 'n8n-nodes-base.httpRequest');
    httpNodes.forEach(node => {
      expect(node.parameters.url).toMatch(/^https:\/\/admin\.core-platform\.local\/api\//);
    });
  });
  ```

**2. Compliance Tests:**
- **Test Scenario 4: Audit Logging**
  ```bash
  # Test: All n8n workflow executions logged to Loki
  
  # 1. Execute workflow
  workflow_id=$(curl -X POST https://admin.core-platform.local/api/n8n/workflows/jira-sync/execute \
    -H "Authorization: Bearer $KEYCLOAK_TOKEN" \
    -d '{"tenantId": "acme"}')
  
  # 2. Query Loki for execution log
  logs=$(curl -G "http://loki:3100/loki/api/v1/query" \
    --data-urlencode 'query={service="n8n", workflow_id="jira-sync"}')
  
  # 3. Assert: Log entry exists
  echo "$logs" | jq -e '.data.result | length > 0'
  ```

- **Test Scenario 5: RBAC Enforcement**
  ```java
  @Test
  void onlyAdminCanAccessN8nUI() {
    // User without CORE_PLATFORM_ADMIN role
    String regularUserToken = keycloak.getToken("user@acme.com", "password");
    
    // Try to access n8n UI
    Response response = RestAssured.given()
      .header("Authorization", "Bearer " + regularUserToken)
      .get("https://admin.core-platform.local/n8n");
    
    // Expected: 403 Forbidden (not authorized)
    assertEquals(403, response.getStatusCode());
  }
  ```

**3. Functional Tests:**
- **Test Scenario 6: End-to-End Workflow Execution**
  ```typescript
  test('Jira sync workflow executes successfully', async () => {
    // 1. Trigger workflow via Core API
    const response = await fetch('https://admin.core-platform.local/api/n8n/workflows/jira-sync/execute', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${keycloakToken}` },
      body: JSON.stringify({ tenantId: 'acme', action: 'create-project' })
    });
    
    // 2. Verify workflow executed
    expect(response.status).toBe(200);
    const result = await response.json();
    expect(result.status).toBe('success');
    
    // 3. Verify Jira project created
    const jiraProject = await jiraClient.getProject('ACME');
    expect(jiraProject).toBeDefined();
    
    // 4. Verify Core API callback received
    const syncStatus = await coreAPI.get(`/api/integrations/jira/sync-status/acme`);
    expect(syncStatus.status).toBe('synced');
  });
  ```

**Playwright E2E Tests:**
```typescript
// e2e/specs/n8n/n8n-sso.spec.ts

test('n8n UI requires Keycloak login', async ({ page }) => {
  // 1. Navigate to n8n UI
  await page.goto('https://admin.core-platform.local/n8n');
  
  // 2. Should redirect to Keycloak
  await expect(page).toHaveURL(/.*auth\/realms\/admin\/protocol\/openid-connect\/auth.*/);
  
  // 3. Login as admin
  await page.fill('input[name=username]', 'admin');
  await page.fill('input[name=password]', 'admin');
  await page.click('input[type=submit]');
  
  // 4. Should redirect back to n8n
  await expect(page).toHaveURL('https://admin.core-platform.local/n8n/');
  
  // 5. n8n UI should be visible
  await expect(page.locator('.n8n-canvas')).toBeVisible();
});
```

**Acceptance Criteria**:
- ✅ Security tests pass (unauthorized access blocked, direct DB blocked, API-only)
- ✅ Compliance tests pass (audit logging, RBAC)
- ✅ Functional tests pass (end-to-end workflow execution)
- ✅ Playwright E2E tests pass (SSO login flow)

**Effort**: ~2 days | **Details**: [stories/N8N6.md](./stories/N8N6.md)

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
