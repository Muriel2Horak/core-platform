# EPIC-011: n8n Integration & Orchestration Hub

> 🏛️ **DESIGN DECISION:** n8n is a **mandatory core component** of Virelio/Core Platform with **multi-tenant support**.  
> 📖 **Infrastructure prerequisite:** This EPIC builds on n8n infrastructure deployed in [EPIC-007 Infrastructure & Deployment](../EPIC-007-infrastructure-deployment/README.md).

**Status:** 🔴 **0% IMPLEMENTED** (Week 4-5)  
**Dependencies:** 
- **EPIC-007** (n8n platform deployment: Docker, Nginx, Keycloak SSO, PostgreSQL, Loki/Prometheus) ← **REQUIRED**
- EPIC-006 (optional: WF15 EXTERNAL_TASK executor for n8n → Camunda integration)

**LOC:** ~5,900 (base ~2,600 + multi-tenant ~1,800 + governance/security ~1,500)  
**Stories:** 13 (N8N1-N8N13: deployment, SSO, proxy, templates, monitoring, BFF, provisioning, routing, isolation, connector, registry sync, workflow governance, webhook security)  
**Roadmap:** Week 4-5 of core platform rollout

**Backend implementation:** Java/Spring Boot (default). No Python refactor planned.

---

## Canonical Story Directories

- N8N1: `stories/N8N1-n8n-platform-deployment/README.md`
- N8N2: `stories/N8N2-keycloak-sso-integration/README.md`
- N8N3: `stories/N8N3-nginx-reverse-proxy-configuration/README.md`
- N8N4: `stories/N8N4-workflow-templates-documentation/README.md`
- N8N5: `stories/N8N5-monitoring-alerting-integration/README.md`
- N8N6: `stories/N8N6-backend-n8n-integration-bff-pattern/README.md`
- N8N7: `stories/N8N7-n8n-provisioning-service/README.md`
- N8N8: `stories/N8N8-multi-tenant-sso-routing/README.md`
- N8N9: `stories/N8N9-tenant-isolation-audit/README.md`
- N8N10: `stories/N8N10-core-connector-node/README.md`
- N8N11: `stories/N8N11-workflow-registry-governance/README.md`
- N8N12: `stories/N8N12-webhook-security/README.md`
- N8N13: `stories/N8N13-workflow-governance/README.md`

Deprecated duplicates are marked in their README files and kept for reference.

---

## 🔗 EPIC-007 Infrastructure Prerequisite

**EPIC-011 assumes n8n infrastructure is deployed and functional per EPIC-007:**

✅ **What EPIC-007 provides (infrastructure layer):**
- n8n Docker service running (`n8n:5678` internal network)
- PostgreSQL database `n8n` with user management **ENABLED** (`N8N_USER_MANAGEMENT_DISABLED=false`)
- Keycloak OIDC clients configured (admin realm + tenant realms)
- Nginx reverse proxy with SSL termination, per-tenant routing, audit headers
- Loki log shipping (`{service="n8n"}`)
- Prometheus metrics scraping
- Smoke test validation (`make smoke-test-env`)

✅ **How EPIC-007 maps Keycloak realms to n8n:**
- **Nginx routing**: `https://{tenant}.${DOMAIN}/n8n` → Backend BFF (with X-Core-Tenant header)
- **Audit headers injected by Nginx**:
  - `X-Core-Tenant: acme` (z JWT realm claim)
  - `X-Core-User: designer@acme.com` (z JWT sub)
  - `X-Core-N8N-Account: tenant-acme` (derived)
- **Multi-realm SSO**: Keycloak client `n8n-admin-client` (admin realm) + WF_* roles in tenant/admin realms
- **Governance roles**: WF_ADMIN/WF_EDITOR/WF_READER for n8n access checks (see N8N13)

❌ **What EPIC-007 does NOT provide (business logic in EPIC-011):**
- Backend BFF proxy implementation (`N8nProxyController`)
- n8n account provisioning service (auto-create `tenant-{realm}@n8n.local`)
- Core Connector custom node (tenant-scoped API calls)
- Workflow templates (Jira sync, AI orchestration, ETL jobs)
- Integration testing (Playwright E2E for n8n workflows)

**Single sign-on flow (EPIC-007 infra + EPIC-011 BFF logic):**

```
1. User (tenant 'acme') navigates: https://acme.core-platform.local/n8n
   ↓
2. Nginx: SSL termination, auth_request /auth (Keycloak validation)
   ↓ (if not authenticated)
3. Keycloak: Redirect to login (realm 'acme'), user logs in
   ↓
4. Keycloak: Issues JWT (claims: realm=acme, sub=designer@acme.com, roles include WF_EDITOR)
   ↓
5. Nginx: Extracts JWT claims, injects headers:
   - X-Core-Tenant: acme
   - X-Core-User: designer@acme.com
   - X-Core-N8N-Account: tenant-acme
   ↓
6. Nginx: Proxies to Backend BFF: http://backend:8080/bff/n8n/proxy
   ↓
7. **Backend BFF (EPIC-011)**: N8nProxyController
   - Validates X-Core-Tenant matches JWT realm (security)
   - Checks if n8n account 'tenant-acme@n8n.local' exists
   - If NOT exists → auto-creates via n8n REST API (provisioning)
   - Obtains n8n session token (impersonate account)
   - Proxies request to n8n:5678 with session cookie
   - Logs action to Loki: {tenant: "acme", user: "designer@acme.com", action: "view_workflows"}
   ↓
8. n8n: Returns workflow editor UI (authenticated as tenant-acme account)
   ↓
9. Frontend: User sees n8n workflows owned by tenant 'acme'
```

**Audit behavior via proxy/logs (NOT n8n fork):**
- **Per-user audit**: BFF logs every action to Loki with X-Core-User header
  - Multiple designers (designer1@acme.com, designer2@acme.com) share account `tenant-acme`, but Loki distinguishes their actions
- **Tenant isolation**: BFF validates X-Core-Tenant header matches JWT realm (403 if mismatch)
- **n8n remains vanilla**: No custom audit code in n8n itself - all tracking via BFF proxy + Nginx access logs

---

## 🏛️ Design Decision: n8n jako Multi-Tenant Integration Designer

**n8n is a mandatory core component of Virelio/Core Platform** with **multi-tenant workflow design support**.

### Rationale

1. **Multi-Tenant Integration & Orchestration Engine**
   - 400+ built-in nodes pro external systems (Jira, M365, Slack, Trello, Google Workspace)
   - Visual workflow builder (no-code integrations)
   - **Per-tenant workflow designers** (každý tenant má svůj n8n account)
   - AI orchestration via MCP/LLM gateway (EPIC-016)
   - ETL/batch processing (CSV export, data transformation)

2. **Security & Governance**
   - **Always behind Keycloak SSO** (per-tenant + admin realm)
   - **Always behind Nginx reverse proxy** (SSL termination, rate limiting, audit headers)
   - **Observed via Loki/Prometheus** (logs + metrics + audit trail)
   - RBAC: `WF_READER/WF_EDITOR/WF_ADMIN`

3. **Multi-Tenant Architecture**
   - **1x n8n instance** (shared infrastructure)
   - **N x n8n user accounts** (1 account per tenant: `tenant-{subdomain}`)
   - **Access URLs**:
     - Admin realm: `https://admin.${DOMAIN}/n8n` (Core admins)
     - Tenant realms: `https://{tenant}.${DOMAIN}/n8n` (tenant designers)
   - **Auto-provisioning**: n8nProvisioningService vytváří n8n accounts při prvním přístupu
   - **Tenant isolation**: Workflows sdílené mezi více adminy stejného tenanta (shared account)

4. **Architectural Principles**
   - n8n is NOT for internal Core business processes (that's EPIC-006 Workflow Engine)
   - n8n IS for: external integrations, AI workflows, ETL/batch jobs, **per-tenant custom workflows**
   - n8n přistupuje k Core POUZE přes API/eventy (NO direct DB access)
   - **Core Connector node**: Custom n8n node pro tenant-scoped API calls (X-Core-Tenant header injection)

5. **Deployment Requirements**
   - Docker/K8s deployment (same tier as Backend, Keycloak, PostgreSQL)
   - PostgreSQL database `n8n` (separate from `core`)
   - Health checks, volume persistence
   - Loki log shipping, Prometheus metrics scraping
   - **User management ENABLED** (`N8N_USER_MANAGEMENT_DISABLED=false`)

### Non-Goals

- ❌ **No public n8n endpoints** (always behind Nginx + Keycloak)
- ❌ **No per-tenant n8n instances** (shared instance, per-tenant accounts)
- ❌ **No direct database access** from n8n (API/events only)
- ❌ **No replacement of EPIC-006 Workflow Engine** (different scopes)
- ❌ **No fork of n8n** (use n8n Community Edition as-is, custom node for Core Connector)

---

## 🎯 Epic Goal

Configure **n8n Community Edition** jako mandatory **multi-tenant integration hub** pro:
- 🔌 **External System Integrations** (Jira, Confluence, Trello, M365, Google Workspace, Slack)
- 🤖 **AI Workflow Orchestration** (MCP/LLM gateway, document classification, enrichment)
- 📊 **ETL/Batch Processing** (CSV export, data transformation, scheduled reports)
- 🚀 **Visual No-Code Automation** (400+ built-in nodes, JSON workflow templates)
- 🏢 **Per-Tenant Workflow Design** (každý tenant může designovat vlastní integrace)
- 🔐 **Secure Multi-Tenant Access** (Keycloak SSO, auto-provisioning, audit logging)

**Access:**
- **Admin realm**: `https://admin.${DOMAIN}/n8n` (Core Platform admins)
- **Tenant realms**: `https://{tenant}.${DOMAIN}/n8n` (tenant users with `WF_READER/WF_EDITOR/WF_ADMIN` roles)

**Integration:** n8n může volat Core Platform API, poslouchat Core eventy, orchestrovat AI/MCP calls. Každý workflow má tenant context (X-Core-Tenant header).

---

## 🏗️ Architecture: Multi-Tenant Integration & Orchestration Layer

```text
┌──────────────────────────────────────────────────────────────────────┐
│  MULTI-TENANT ARCHITECTURE                                            │
│                                                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ USERS (per tenant/realm)                                        │ │
│  │  Admin realm: admin@core.local → https://admin.${DOMAIN}/n8n   │ │
│  │  Tenant 'acme': designer@acme.com → https://acme.${DOMAIN}/n8n │ │
│  │  Tenant 'beta': designer@beta.com → https://beta.${DOMAIN}/n8n │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                               │                                       │
│                               ▼                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ KEYCLOAK SSO (Multi-Realm)                                      │ │
│  │  Realms: admin, acme, beta, ...                                │ │
│  │  Role: WF_EDITOR (per tenant)                                   │ │
│  │  JWT contains: realm (=tenant), user, roles                    │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                               │                                       │
│                               ▼                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ NGINX REVERSE PROXY (SSL + Audit Headers)                      │ │
│  │  Route: /{tenant}/n8n/* → http://backend:8080/bff/n8n/proxy    │ │
│  │  Auth: Keycloak SSO required                                   │ │
│  │  Headers Injection:                                            │ │
│  │    X-Core-Tenant: acme (from JWT realm claim)                  │ │
│  │    X-Core-User: designer@acme.com (from JWT sub claim)         │ │
│  │    X-Core-N8N-Account: tenant-acme (computed)                  │ │
│  │  Logging: Promtail → Loki (audit trail)                        │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                               │                                       │
│                               ▼                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ BACKEND BFF (n8nProvisioningService)                           │ │
│  │  1. Validate JWT, extract tenant + user                        │ │
│  │  2. Check n8n account exists: GET /api/users (tenant-{realm})  │ │
│  │  3. If NOT exists → auto-create:                               │ │
│  │       POST /api/users { email: tenant-{realm}@n8n.local }      │ │
│  │       POST /api/auth/magic-link (or OIDC if supported)         │ │
│  │  4. Proxy request to n8n with n8n session (impersonate tenant) │ │
│  │  5. Log action to Loki: {tenant, user, action, n8n_account}    │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                               │                                       │
│                               ▼                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ n8n COMMUNITY EDITION (Single Instance, Multi-Account)         │ │
│  │  Port: 5678 (internal only)                                    │ │
│  │  Database: PostgreSQL (database: n8n, user: n8n_app)           │ │
│  │  Features: 400+ nodes, visual builder, webhook support         │ │
│  │  User Management: ENABLED (N8N_USER_MANAGEMENT_DISABLED=false) │ │
│  │                                                                 │ │
│  │  Accounts (examples):                                          │ │
│  │    - tenant-acme@n8n.local (workflows for 'acme' tenant)       │ │
│  │    - tenant-beta@n8n.local (workflows for 'beta' tenant)       │ │
│  │    - admin-instance-owner@n8n.local (Core Platform admin)      │ │
│  │                                                                 │ │
│  │  Workflows per tenant:                                         │ │
│  │    - tenant-acme: Jira sync, Slack notifications, AI classify  │ │
│  │    - tenant-beta: Confluence sync, Email enrichment, CSV export│ │
│  └────────┬──────────────┬──────────────┬────────────────────────┘ │
│           │              │              │                            │
│           ▼              ▼              ▼                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────┐ │
│  │ Core API    │  │ Core Events │  │ External Systems            │ │
│  │ (REST)      │  │ (Kafka/CDC) │  │ - Jira, Confluence, Trello  │ │
│  │ Headers:    │  │ - Tenant    │  │ - M365, Google Workspace    │ │
│  │ X-Core-     │  │   created   │  │ - Slack, Gmail              │ │
│  │ Tenant: acme│  │ - User      │  │ - OpenAI, MCP/LLM gateway   │ │
│  │ (from n8n   │  │   onboard   │  └─────────────────────────────┘ │
│  │ workflow)   │  └─────────────┘                                   │
│  └─────────────┘                                                     │
│                                                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ OBSERVABILITY & AUDIT                                           │ │
│  │  - Loki: n8n logs {service="n8n", tenant="acme"}               │ │
│  │  - Loki: Nginx audit logs {X-Core-Tenant, X-Core-User}         │ │
│  │  - Prometheus: n8n metrics (per tenant if possible)            │ │
│  │  - Grafana: n8n dashboard (workflow executions, error rates)   │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

### Integration Patterns

**Pattern 1: Per-Tenant External System Sync**
```
Core Platform Event (Tenant 'acme' Created)
  → Kafka → n8n Webhook Trigger (tenant-acme account)
  → n8n Workflow (tenant-acme owner):
      1. Create Jira Project (key=ACME, tenant-specific credentials)
      2. Create Confluence Space (key=acme, tenant-specific credentials)
      3. Send Slack notification (tenant-specific channel)
      4. Call Core API: POST /api/integrations/sync-status
         Headers: X-Core-Tenant: acme
```

**Pattern 2: Multi-Tenant AI Workflow Orchestration**
```
User uploads document (tenant 'beta') → Core API
  → n8n Workflow (tenant-beta account, via Core API call):
      1. Call MCP/LLM gateway: classify document
      2. Extract metadata (OpenAI API, tenant-specific API key)
      3. Store results: POST /api/documents/{id}/metadata
         Headers: X-Core-Tenant: beta
      4. Trigger follow-up workflow (email notification, tenant-specific template)
```

**Pattern 3: Per-Tenant Scheduled ETL/Batch**
```
n8n Cron Trigger (tenant-acme account, every day 2am)
  → n8n Workflow:
      1. GET /api/reports/daily-export
         Headers: X-Core-Tenant: acme
      2. Transform to CSV (tenant-specific columns)
      3. Upload to S3/MinIO (tenant-specific bucket)
      4. Send email with download link (tenant-specific recipients)
```

**Pattern 4: Core Connector Node (Tenant-Scoped API Calls)**
```typescript
// Custom n8n node: @core-platform/n8n-node-core-connector
// Automatically injects X-Core-Tenant header based on n8n account

const coreConnector = {
  name: 'Core API',
  type: 'n8n-nodes-base.httpRequest',
  config: {
    url: 'https://admin.core-platform.local/api/tenants',
    authentication: 'predefinedCredentialType',
    headers: {
      'X-Core-Tenant': '{{$workflow.tenantId}}',  // Auto-injected by BFF
      'X-Core-User': '{{$user.email}}',
      'X-Core-N8N-Account': 'tenant-{{$workflow.tenantId}}'
    }
  }
};
```

### Security & Compliance

**🔒 KRITICKÉ: Všechen přístup do n8n jde POUZE přes Backend BFF**

**Žádný direct access** z frontendu nebo webhooků přímo na n8n service:
- ✅ Frontend → Nginx → **Backend BFF** → n8n
- ✅ Webhooky → Nginx → **Backend BFF** → n8n  
- ❌ ~~Frontend → Nginx → n8n~~ (ZAKÁZÁNO)

**BFF Proxy Flow:**
1. **Request přijde**: `https://acme.${DOMAIN}/n8n/workflows`
2. **Nginx** ověří SSL, rate limit, směřuje na Backend BFF
3. **Backend BFF (`N8nProxyController`):**
   - Ověří JWT token (Keycloak)
   - Extrahuje tenant z JWT realm (`acme`)
   - Najde/vytvoří n8n account (`tenant-acme@n8n.local`)
   - Získá n8n session token (impersonation)
   - **Proxy request** na n8n s session cookie
4. **n8n** vrátí response (workflows, UI, webhook execution)
5. **BFF** loguje do Loki: `{tenant: "acme", user: "designer@acme.com", action: "view_workflows"}`

**Důvod:** BFF kontroluje autorizaci, tenant isolation, audit logging. n8n service běží POUZE na interním Dockeru networku (port 5678 není exposed).

---

1. **Authentication & Authorization**
   - SSO přes Keycloak (multi-realm support)
   - **Admin realm**: `WF_ADMIN` role → full n8n access
   - **Tenant realms**: `WF_READER/WF_EDITOR/WF_ADMIN` roles → per-tenant n8n account access
   - **Auto-provisioning**: n8nProvisioningService (backend BFF)
     - First access → creates n8n account `tenant-{realm}@n8n.local`
     - Subsequent accesses → reuse existing account
     - **Poznámka**: Konkrétní provisioning mechanismus (REST API credentials, magic link, atd.) bude navržen v N8N1/N8N2 podle aktuálních možností n8n CE.

2. **Access Control**
   - n8n UI dostupné PER TENANT přes BFF proxy:
     - `https://acme.${DOMAIN}/n8n` → Backend BFF → n8n (account: tenant-acme)
     - `https://beta.${DOMAIN}/n8n` → Backend BFF → n8n (account: tenant-beta)
   - n8n může volat Core API POUZE s X-Core-Tenant header (validated by backend)
   - Webhooky chráněny Nginx rate limiting

3. **Tenant Isolation**
   - **n8n accounts**: 1 account per tenant (`tenant-{realm}`)
   - **Workflow ownership**: Všechny workflows patří tenant accountu
   - **Shared access**: Více adminů stejného tenanta sdílí 1 n8n account
     - n8n Community Edition podporuje multiple concurrent sessions pro stejný účet
     - Všichni tenant admins vidí stejné workflows, execution history, credentials (tenant-shared workspace)
     - **Per-user audit trail**: BFF proxy předává X-Core-User header → Loki loguje kdo (Core user) dělal co v n8n UI
     - Příklad: designer1@acme.com a designer2@acme.com sdílí account `tenant-acme`, ale Loki rozlišuje jejich akce
   - **Cross-tenant protection**: n8n workflows NEMOHOU pristupovat k jiným tenant datům
     - Backend validuje X-Core-Tenant header (must match JWT realm)
     - API calls bez správného headeru → 403 Forbidden

4. **Audit & Logging**
   - Všechny workflow executions logovány do Loki:
     ```json
     {
       "service": "n8n",
       "tenant": "acme",
       "user": "designer@acme.com",
       "n8n_account": "tenant-acme",
       "workflow_id": "jira-sync",
       "execution_id": "12345",
       "status": "success"
     }
     ```
   - Nginx access log obsahuje audit headers:
     ```
     X-Core-Tenant: acme
     X-Core-User: designer@acme.com
     X-Core-N8N-Account: tenant-acme
     ```
   - Error tracking: Loki `{level="error", service="n8n", tenant="acme"}`
   - Metrics: Prometheus scraping (pokud n8n expose /metrics)

5. **Data Governance**
   - n8n workflow data stored v PostgreSQL `n8n` database
   - Retention policy: 30 days execution history
   - NO direct DB access from n8n (API/events only, X-Core-Tenant validated)
   - Tenant-specific credentials stored v n8n (encrypted at rest)
   - **MANDATORY: Core Connector node pro všechna volání do Core Platform API**
     - ❌ Generic HTTP Request node **NENÍ povolen** pro Core API (chybějící guardrails)
     - ✅ Core Connector node **MUSÍ být použit** → auto-inject X-Core-Tenant, X-Core-User
     - ✅ Whitelisted routes (`/api/tenants`, `/api/ai/*`, atd.) → prevence arbitrary URLs
     - ✅ Pre-configured SSL trust (self-signed CA v dev)
     - Příklad špatného workflow: `HTTP Request → https://admin.${DOMAIN}/api/tenants` (chybí X-Core-Tenant!)
     - Příklad správného workflow: `Core Connector → Tenants → Get` (header auto-injected)

6. **AI/MCP Workflow Governance**
   - **AI/MCP calls MUST go through Core Platform AI Gateway** (ne přímo OpenAI/external LLMs)
   - Důvod: Centralizovaný audit, rate limiting, cost tracking, prompt injection protection
   - n8n workflows používají Core API endpoint: `POST /api/ai/classify`, `/api/ai/summarize`
   - Backend AI gateway loguje všechny AI volání do Loki s tenant context
   - Tenant nemůže použít vlastní OpenAI API key v n8n (bezpečnostní riziko, non-compliant)

7. **Integration with Core Platform Security**
   - **OIDC/SSO**: Sladěno s [EPIC-000 Security Hardening](../EPIC-000-security-hardening/README.md)
     - Multi-realm Keycloak, JWT validation, role-based access
   - **Infrastructure deployment**: Řízeno přes [EPIC-007 Infrastructure Deployment](../EPIC-007-infrastructure-deployment/README.md)
     - Nginx reverse proxy, SSL termination, Docker orchestration
   - **Audit logging**: Integrace s [EPIC-003 Monitoring & Observability](../EPIC-003-monitoring-observability/README.md)
     - Loki centralizovaný logging, Prometheus metrics, Grafana dashboards

---


## 📊 Component Overview

| Component | Purpose | Port | Tech Stack | Status |
|-----------|---------|------|------------|--------|
| **n8n** | Multi-tenant workflow automation engine | 5678 | Node.js, PostgreSQL | ⏳ TODO (N8N1) |
| **Nginx** | Reverse proxy, per-tenant routing, audit headers | 443 | Nginx 1.25+ | ⏳ TODO (N8N3, N8N8) |
| **Backend BFF** | n8n provisioning, proxy, tenant validation | 8080 | Spring Boot, WebClient | ⏳ TODO (N8N6, N8N7, N8N8, N8N11, N8N13) |
| **Keycloak** | Multi-realm SSO identity provider | 8443 | Java, PostgreSQL | ✅ EXISTS |
| **Templates** | Pre-built n8n workflows | - | JSON exports | ⏳ TODO (N8N4) |
| **Monitoring** | Grafana dashboards + Loki audit | 3000 | Grafana, Loki | ⏳ TODO (N8N5, N8N9) |
| **Workflow Registry** | Metadata sync from n8n | 8080 | Spring Boot, Postgres | ⏳ TODO (N8N11) |
| **Workflow Governance** | Role-based access and audit | 8080 | Spring Boot, Keycloak | ⏳ TODO (N8N13) |
| **Webhook Security** | HMAC verification + secrets | 8080 | Spring Boot | ⏳ TODO (N8N12) |
| **Core Connector** | Custom n8n node (tenant-scoped API calls) | - | TypeScript, n8n SDK | ⏳ TODO (N8N10) |

---

## 🎯 Success Metrics

- **Security**: 100% n8n access requires Keycloak SSO login per tenant
- **Multi-Tenancy**: N tenants → N n8n accounts (1:1 mapping)
- **Tenant Isolation**: 0 cross-tenant data leaks (validated via tests)
- **Audit Trail**: 100% workflow executions logged to Loki with tenant context
- **Availability**: 99.9% uptime (n8n + BFF)
- **Performance**: <200ms BFF API latency, <2s n8n UI load
- **Adoption**: 50+ workflows created within first month (across all tenants)
- **Integration**: 10+ external systems connected per tenant (Jira, Confluence, Trello, M365, Google)

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

**Effort**: ~4 hours | **Details**: [stories/N8N1-n8n-platform-deployment/README.md](./stories/N8N1-n8n-platform-deployment/README.md)

---

### N8N2: Keycloak SSO Integration (~300 LOC, 1 day)

**Goal**: Configure Keycloak client for n8n SSO authentication + navrhnout provisioning mechanismus

**Deliverables**:
- Keycloak client: `n8n-client`
- Redirect URIs: `https://admin.core-platform.local/n8n/*`, `https://{tenant}.${DOMAIN}/n8n/*`
- Realm roles: `WF_READER`, `WF_EDITOR`, `WF_ADMIN`
- User group mapping (realm-specific roles)
- JWT token configuration (realm claim, sub, email)
- **Provisioning mechanismus design:**
  - Prozkoumat n8n CE REST API capabilities (`POST /api/v1/users`, `/api/v1/auth/*`)
  - Navrhnout credential flow: password-based, magic link, nebo OIDC (podle dostupných n8n API)
  - Definovat account naming convention: `tenant-{realm}@n8n.local`, `admin-instance-owner@n8n.local`
  - Implementační poznámka: Nekomplikovat EPICem "magic-link/OIDC" pokud n8n CE to nepodporuje přímo – použít co funguje (REST API credentials)

**Acceptance Criteria**:
- ✅ Users redirected to Keycloak login when accessing /n8n
- ✅ Successful login grants access to n8n UI
- ✅ Multi-realm support: admin realm + tenant realms
- ✅ Provisioning strategy documented (REST API / credentials management)

**Effort**: ~1 day | **Details**: [stories/N8N2-keycloak-sso-integration/README.md](./stories/N8N2-keycloak-sso-integration/README.md)

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

**Effort**: ~0.5 day | **Details**: [stories/N8N3-nginx-reverse-proxy-configuration/README.md](./stories/N8N3-nginx-reverse-proxy-configuration/README.md)

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

**Effort**: ~2 days | **Details**: [stories/N8N4-workflow-templates-documentation/README.md](./stories/N8N4-workflow-templates-documentation/README.md)

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

**Effort**: ~1 day | **Details**: [stories/N8N5-monitoring-alerting-integration/README.md](./stories/N8N5-monitoring-alerting-integration/README.md)

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
    // User without WF_EDITOR/WF_ADMIN role
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

**Effort**: ~2 days | **Details**: [stories/N8N6-backend-n8n-integration-bff-pattern/README.md](./stories/N8N6-backend-n8n-integration-bff-pattern/README.md)

---

### N8N7: n8n Provisioning Service (~600 LOC, 2 days)

**Goal**: Auto-create n8n user accounts per tenant při prvním přístupu

**Deliverables**:
- Backend service: `n8nProvisioningService`
- n8n REST API client (n8n management API)

**Explicitní provisioning flow (krok za krokem):**

```java
// 1. User přistupuje s WF_READER/WF_EDITOR/WF_ADMIN rolí
GET https://acme.${DOMAIN}/n8n
  → Nginx routes to: http://backend:8080/bff/n8n/proxy

// 2. Backend BFF (N8nProxyController.ensureProvisioningAndProxy)
@GetMapping("/bff/n8n/proxy/**")
public ResponseEntity<String> proxyToN8n(
  @RequestHeader("X-Core-Tenant") String tenant,  // acme (z JWT realm)
  @RequestHeader("X-Core-User") String user,      // designer@acme.com (z JWT sub)
  HttpServletRequest request
) {
  // Krok 2.1: Validuj že tenant v headeru = JWT realm
  String jwtRealm = jwtService.extractRealm(request);
  if (!tenant.equals(jwtRealm)) {
    throw new ForbiddenException("Tenant mismatch");
  }
  
  // Krok 2.2: Zkontroluj jestli n8n account existuje
  String n8nAccountEmail = "tenant-" + tenant + "@n8n.local";
  Optional<N8nUser> existingAccount = n8nClient.getUserByEmail(n8nAccountEmail);
  
  // Krok 2.3: Pokud NE → vytvoř nový account
  if (existingAccount.isEmpty()) {
    N8nUser newAccount = n8nClient.createUser(N8nUserRequest.builder()
      .email(n8nAccountEmail)
      .firstName("Tenant")
      .lastName(tenant.toUpperCase())
      .password(passwordGenerator.generateSecure(32))  // nebo magic link
      .build()
    );
    
    // Krok 2.4: Loguj vytvoření do Loki
    log.info("n8n account created", Map.of(
      "action", "n8n_account_created",
      "tenant", tenant,
      "user", user,
      "n8n_account", n8nAccountEmail,
      "timestamp", Instant.now()
    ));
  }
  
  // Krok 2.5: Získej n8n session token (impersonate account)
  String n8nSessionToken = n8nAuthService.getSessionToken(n8nAccountEmail);
  
  // Krok 2.6: Proxy request to n8n s session cookie
  return webClient.get()
    .uri("http://n8n:5678" + extractN8nPath(request))
    .header("Cookie", "n8n-auth=" + n8nSessionToken)
    .retrieve()
    .toEntity(String.class)
    .block();
}
```

**Account naming convention:**
  - Admin realm: `admin-instance-owner@n8n.local`
  - Tenant realms: `tenant-{subdomain}@n8n.local` (e.g., `tenant-acme@n8n.local`)

**Environment config:**
  ```yaml
  n8n:
    environment:
      - N8N_USER_MANAGEMENT_DISABLED=false  # ENABLE user management
      - N8N_USER_MANAGEMENT_JWT_SECRET=${N8N_JWT_SECRET}
  ```

**Acceptance Criteria**:
- ✅ First access to n8n creates tenant account automatically
- ✅ Subsequent accesses reuse existing account
- ✅ Multiple admins of same tenant share 1 n8n account
- ✅ Account creation logged to Loki with audit trail
- ✅ Test: 100 tenants created → 100 n8n accounts

**Effort**: ~2 days | **Details**: [stories/N8N7-n8n-provisioning-service/README.md](./stories/N8N7-n8n-provisioning-service/README.md)

---

### N8N8: Multi-Tenant SSO & Routing (~500 LOC, 2 days)

**Goal**: Configure Nginx + BFF routing pro per-tenant n8n access

**Deliverables**:

**1. Nginx Configuration:**
```nginx
# Per-tenant n8n routing
location ~ ^/([a-z0-9-]+)/n8n/ {
  set $tenant $1;
  
  # Validate tenant exists (optional: call backend API)
  # auth_request /api/tenants/$tenant/validate;
  
  # Keycloak SSO enforcement
  auth_request /auth;
  auth_request_set $auth_user $upstream_http_x_auth_request_user;
  auth_request_set $auth_realm $upstream_http_x_auth_request_realm;
  
  # Inject audit headers
  proxy_set_header X-Core-Tenant $auth_realm;
  proxy_set_header X-Core-User $auth_user;
  proxy_set_header X-Core-N8N-Account tenant-$auth_realm;
  
  # Proxy to backend BFF (NOT directly to n8n!)
  proxy_pass http://backend:8080/bff/n8n/proxy;
  proxy_set_header X-Original-URI $request_uri;
  
  # WebSocket support
  proxy_set_header Upgrade $http_upgrade;
  proxy_set_header Connection "upgrade";
}

# Admin realm n8n routing
location /n8n/ {
  auth_request /auth;
  auth_request_set $auth_user $upstream_http_x_auth_request_user;
  
  # Inject audit headers (admin realm)
  proxy_set_header X-Core-Tenant admin;
  proxy_set_header X-Core-User $auth_user;
  proxy_set_header X-Core-N8N-Account admin-instance-owner;
  
  proxy_pass http://backend:8080/bff/n8n/proxy;
  proxy_set_header Upgrade $http_upgrade;
  proxy_set_header Connection "upgrade";
}
```

**2. Backend BFF Proxy:**
```java
@RestController
@RequestMapping("/bff/n8n/proxy")
public class N8nProxyController {
  
  @GetMapping("/**")
  public ResponseEntity<String> proxyGet(
    @RequestHeader("X-Core-Tenant") String tenant,
    @RequestHeader("X-Core-User") String user,
    HttpServletRequest request
  ) {
    // 1. Validate tenant matches JWT realm
    String jwtRealm = extractRealmFromJWT(request);
    if (!tenant.equals(jwtRealm)) {
      throw new ForbiddenException("Tenant mismatch");
    }
    
    // 2. Ensure n8n account exists (provision if needed)
    String n8nAccount = n8nProvisioningService.ensureAccountExists(tenant);
    
    // 3. Impersonate n8n account (get session token)
    String n8nSession = n8nAuthService.getSessionToken(n8nAccount);
    
    // 4. Proxy request to n8n with session
    return webClient.get()
      .uri("http://n8n:5678" + request.getRequestURI().replace("/bff/n8n/proxy", ""))
      .header("Cookie", "n8n-auth=" + n8nSession)
      .retrieve()
      .toEntity(String.class)
      .block();
  }
  
  // Similar for POST, PUT, DELETE
}
```

**3. Keycloak Realm Mapping:**
- Each tenant subdomain = Keycloak realm (`acme.${DOMAIN}` = realm `acme`)
- Role `WF_READER/WF_EDITOR/WF_ADMIN` required for n8n access
- JWT contains `realm` claim (used for tenant identification)

**Acceptance Criteria**:
- ✅ Tenant 'acme' users access n8n at `https://acme.${DOMAIN}/n8n`
- ✅ Tenant 'beta' users access n8n at `https://beta.${DOMAIN}/n8n`
- ✅ Admin realm users access n8n at `https://admin.${DOMAIN}/n8n`
- ✅ Nginx injects X-Core-* headers correctly
- ✅ BFF validates tenant matches JWT realm
- ✅ Session impersonation works (n8n shows correct account)

**Effort**: ~2 days | **Details**: [stories/N8N8-multi-tenant-sso-routing/README.md](./stories/N8N8-multi-tenant-sso-routing/README.md)

---

### N8N9: Tenant Isolation & Audit Headers (~400 LOC, 1.5 days)

**Goal**: Enforce tenant isolation a audit trail pro n8n actions

**Deliverables**:

**1. Tenant Isolation Tests:**
```java
@Test
void tenantCannotAccessOtherTenantsWorkflows() {
  // Tenant 'acme' user
  String acmeToken = keycloak.getToken("designer@acme.com", "password");
  
  // Try to access tenant 'beta' workflows
  Response response = RestAssured.given()
    .header("Authorization", "Bearer " + acmeToken)
    .get("https://beta.core-platform.local/n8n/workflows");
  
  // Expected: 403 Forbidden (tenant mismatch)
  assertEquals(403, response.getStatusCode());
}

@Test
void n8nWorkflowsCannotCrossAccessTenantData() {
  // Create workflow in tenant 'acme' account
  Workflow workflow = createN8nWorkflow("tenant-acme@n8n.local", "jira-sync");
  
  // Add HTTP node calling Core API
  HttpNode httpNode = workflow.addNode("HttpRequest", {
    url: "https://admin.core-platform.local/api/tenants"
  });
  
  // Execute workflow
  ExecutionResult result = n8nClient.executeWorkflow(workflow.id);
  
  // Assert: Core API received X-Core-Tenant header
  var apiCall = mockServer.verify("/api/tenants");
  assertEquals("acme", apiCall.getHeader("X-Core-Tenant"));
  
  // Assert: Backend validated tenant and returned ONLY acme data
  assertFalse(result.output.contains("beta"));  // No cross-tenant data leak
}
```

**2. Audit Trail Configuration:**
```yaml
# Promtail config: Scrape Nginx access logs
scrape_configs:
  - job_name: nginx-n8n-audit
    static_configs:
      - targets:
          - localhost
        labels:
          job: nginx-n8n-audit
          __path__: /var/log/nginx/access.log
    pipeline_stages:
      - regex:
          expression: '.*X-Core-Tenant: (?P<tenant>[^ ]+).*X-Core-User: (?P<user>[^ ]+).*X-Core-N8N-Account: (?P<n8n_account>[^ ]+).*'
      - labels:
          tenant:
          user:
          n8n_account:
```

**3. Loki Queries (Audit Trail):**
```logql
# All n8n actions by tenant 'acme'
{job="nginx-n8n-audit", tenant="acme"}

# All n8n actions by user
{job="nginx-n8n-audit", user="designer@acme.com"}

# All n8n workflow executions (from n8n container logs)
{service="n8n", tenant="acme"} |= "workflow execution"

# Failed n8n workflows per tenant
{service="n8n", level="error"} | json | tenant="acme"
```

**4. Core API Validation:**
```java
@PostMapping("/api/tenants")
public ResponseEntity<TenantDTO> createTenant(
  @RequestHeader("X-Core-Tenant") String tenant,
  @RequestBody TenantRequest request,
  @AuthenticationPrincipal Jwt jwt
) {
  // Validate tenant header matches JWT realm
  String jwtRealm = jwt.getClaimAsString("realm");
  if (!tenant.equals(jwtRealm)) {
    log.warn("Tenant mismatch: header={}, jwt={}", tenant, jwtRealm);
    throw new ForbiddenException("Tenant header does not match JWT realm");
  }
  
  // Proceed with tenant-scoped operation
  return ResponseEntity.ok(tenantService.createTenant(tenant, request));
}
```

**Acceptance Criteria**:
- ✅ Tenant 'acme' workflows CANNOT access tenant 'beta' data
- ✅ Core API validates X-Core-Tenant matches JWT realm (403 if mismatch)
- ✅ Nginx access logs contain X-Core-* headers
- ✅ Loki audit trail shows who (user) did what (action) in which tenant
- ✅ Test: 100 workflows executed → 100 audit log entries with correct tenant

**Effort**: ~1.5 days | **Details**: [stories/N8N9-tenant-isolation-audit/README.md](./stories/N8N9-tenant-isolation-audit/README.md)

---

### N8N10: Core API Connector Node (~300 LOC, 1 day)

**Goal**: Custom n8n node pro safe tenant-scoped Core API calls

**Proč custom node a ne generic HTTP Request?**
- ✅ **Auto-inject X-Core-Tenant header** (odvozený z n8n account name)
  - User nemůže zapomenout header → prevence cross-tenant leaku
  - User nemůže manuálně změnit tenant → bezpečnost
- ✅ **Pouze Core Platform API endpoints** (ne arbitrary URLs)
  - Whitelisted routes: `/api/tenants`, `/api/ai/classify`, atd.
  - Prevence phishing/data exfiltration
- ✅ **Automatické SSL trust** (self-signed CA v dev)
- ✅ **User-friendly resource picker** (Tenants, Users, AI Services)

**Deliverables**:

**1. Custom n8n Node Package:**
```typescript
// packages/@core-platform/n8n-node-core-connector/src/CoreConnector.node.ts

import { INodeType, INodeTypeDescription, IExecuteFunctions } from 'n8n-workflow';

export class CoreConnector implements INodeType {
  description: INodeTypeDescription = {
    displayName: 'Core Platform API',
    name: 'corePlatformApi',
    group: ['transform'],
    version: 1,
    description: 'Call Core Platform API with automatic tenant context',
    icon: 'file:core-platform.svg',
    defaults: {
      name: 'Core API',
    },
    inputs: ['main'],
    outputs: ['main'],
    properties: [
      {
        displayName: 'Resource',
        name: 'resource',
        type: 'options',
        options: [
          { name: 'Tenants', value: 'tenants' },
          { name: 'Users', value: 'users' },
          { name: 'AI Services', value: 'ai' },
        ],
        default: 'tenants',
      },
      {
        displayName: 'Operation',
        name: 'operation',
        type: 'options',
        displayOptions: {
          show: { resource: ['tenants'] },
        },
        options: [
          { name: 'Get', value: 'get' },
          { name: 'Create', value: 'create' },
          { name: 'Update', value: 'update' },
        ],
        default: 'get',
      },
      // ... další properties
    ],
  };

  async execute(this: IExecuteFunctions) {
    const items = this.getInputData();
    const returnData = [];

    // KRITICKÉ: Extrahuj tenant z n8n account credentials
    const credentials = await this.getCredentials('corePlatformApi');
    const n8nAccount = credentials.accountName as string;  // "tenant-acme@n8n.local"
    const tenant = extractTenantFromAccount(n8nAccount);    // "acme"

    for (let i = 0; i < items.length; i++) {
      const resource = this.getNodeParameter('resource', i) as string;
      const operation = this.getNodeParameter('operation', i) as string;

      let endpoint = '';
      let method = 'GET';
      let body = {};

      // Build endpoint based on resource/operation
      if (resource === 'tenants' && operation === 'get') {
        endpoint = '/api/tenants';
        method = 'GET';
      } else if (resource === 'ai' && operation === 'classify') {
        endpoint = '/api/ai/classify';
        method = 'POST';
        body = { text: this.getNodeParameter('text', i) };
      }

      // ✅ AUTOMATIC TENANT HEADER INJECTION
      const response = await this.helpers.request({
        method,
        url: `https://admin.${process.env.DOMAIN}${endpoint}`,  // ← ALWAYS admin.${DOMAIN}
        headers: {
          'X-Core-Tenant': tenant,  // ← AUTO-INJECTED (user nemůže override!)
          'Content-Type': 'application/json',
        },
        body,
        json: true,
        rejectUnauthorized: false,  // Dev: trust self-signed cert
      });

      returnData.push({ json: response });
    }

    return [returnData];
  }
}

function extractTenantFromAccount(accountName: string): string {
  // "tenant-acme@n8n.local" → "acme"
  const match = accountName.match(/^tenant-([a-z0-9-]+)@n8n\.local$/);
  if (!match) {
    throw new Error('Invalid n8n account format (expected: tenant-{realm}@n8n.local)');
  }
  return match[1];
}
```

**2. Node Installation (v n8n Docker image):**
```dockerfile
# docker/n8n/Dockerfile
FROM n8nio/n8n:1.66.0

# Install Core Connector node
WORKDIR /usr/local/lib/node_modules
COPY packages/@core-platform/n8n-node-core-connector ./n8n-node-core-connector
RUN cd n8n-node-core-connector && npm install && npm run build

# Node discovery
ENV N8N_CUSTOM_EXTENSIONS="/usr/local/lib/node_modules"
```

**3. Použití v n8n workflow (UI):**
```
1. Drag & drop "Core Platform API" node
2. Select Resource: "Tenants"
3. Select Operation: "Get"
4. Execute

→ Node automaticky volá: GET https://admin.core-platform.local/api/tenants
→ Header: X-Core-Tenant: acme (extracted from tenant-acme@n8n.local)
→ Backend BFF ověří tenant → vrátí POUZE acme data
```

**Security Benefits:**
- ❌ User **NEMŮŽE** zadat arbitrary URL (jen whitelisted Core API routes)
- ❌ User **NEMŮŽE** změnit X-Core-Tenant header (auto-derived z account)
- ❌ User **NEMŮŽE** volat cross-tenant data (backend validuje header vs JWT)
- ✅ User **MÁ** user-friendly UI (resource picker místo manuálního URL)

**Acceptance Criteria**:
          { name: 'Tenants', value: 'tenants' },
          { name: 'Users', value: 'users' },
          { name: 'Documents', value: 'documents' },
          { name: 'Workflows', value: 'workflows' },
        ],
        default: 'tenants',
      },
      {
        displayName: 'Operation',
        name: 'operation',
        type: 'options',
        options: [
          { name: 'Get', value: 'get' },
          { name: 'List', value: 'list' },
          { name: 'Create', value: 'create' },
          { name: 'Update', value: 'update' },
        ],
        default: 'list',
      },
    ],
  };

  async execute(this: IExecuteFunctions): Promise<INodeExecutionData[][]> {
    const items = this.getInputData();
    const returnData: INodeExecutionData[] = [];

    // Get tenant from n8n account email (tenant-acme@n8n.local → acme)
    const currentUser = this.getWorkflow().settings.callerIds?.userId;
    const tenantMatch = currentUser?.match(/^tenant-([a-z0-9-]+)@/);
    const tenant = tenantMatch ? tenantMatch[1] : 'admin';

    for (let i = 0; i < items.length; i++) {
      const resource = this.getNodeParameter('resource', i) as string;
      const operation = this.getNodeParameter('operation', i) as string;

      // Build API URL
      const url = `https://admin.core-platform.local/api/${resource}`;

      // Call Core API with tenant header
      const response = await this.helpers.request({
        method: operation === 'create' ? 'POST' : 'GET',
        url,
        headers: {
          'X-Core-Tenant': tenant,  // Auto-injected!
          'X-Core-N8N-Account': `tenant-${tenant}`,
          'Authorization': `Bearer ${credentials.apiKey}`,  // Service account token
        },
        body: operation === 'create' ? items[i].json : undefined,
        json: true,
      });

      returnData.push({ json: response });
    }

    return [returnData];
  }
}
```

**2. Node Credentials:**
```typescript
// packages/@core-platform/n8n-node-core-connector/credentials/CorePlatformApi.credentials.ts

import { ICredentialType, INodeProperties } from 'n8n-workflow';

export class CorePlatformApi implements ICredentialType {
  name = 'corePlatformApi';
  displayName = 'Core Platform API';
  properties: INodeProperties[] = [
    {
      displayName: 'API Key',
      name: 'apiKey',
      type: 'string',
      default: '',
      description: 'Service account API key (per tenant)',
    },
  ];
}
```

**3. Installation & Usage:**
```bash
# Build custom node package
cd packages/@core-platform/n8n-node-core-connector
npm run build

# Install in n8n (Docker volume mount)
docker cp dist/. core-n8n:/usr/local/lib/node_modules/@core-platform/n8n-node-core-connector/

# Restart n8n to load custom node
docker restart core-n8n
```

**4. Example Workflow (using Core Connector):**
```json
{
  "nodes": [
    {
      "name": "Get Tenant Info",
      "type": "@core-platform/corePlatformApi",
      "parameters": {
        "resource": "tenants",
        "operation": "get"
      },
      "credentials": {
        "corePlatformApi": "tenant-acme-service-account"
      }
    },
    {
      "name": "Create Document",
      "type": "@core-platform/corePlatformApi",
      "parameters": {
        "resource": "documents",
        "operation": "create",
        "body": {
          "title": "{{$node['Get Tenant Info'].json.name}} Report",
          "content": "..."
        }
      }
    }
  ]
}
```

**Acceptance Criteria**:
- ✅ Custom node available in n8n UI (Core Platform API)
- ✅ Node automatically injects X-Core-Tenant header (from n8n account email)
- ✅ Workflows using Core Connector CANNOT manually override tenant header
- ✅ Test: Workflow in tenant 'acme' calls Core API → backend receives X-Core-Tenant: acme
- ✅ Documentation: How to use Core Connector node (screenshots, examples)

**Effort**: ~1 day | **Details**: [stories/N8N10-core-connector-node/README.md](./stories/N8N10-core-connector-node/README.md)

---

### N8N11: Workflow Registry Sync (~500 LOC, 1.5 days)

**Goal**: Sync n8n workflow metadata into Core for visibility and audit.

**Deliverables**:
- Workflow registry tables (metadata, categories, tags)
- API endpoints for listing and manual sync
- Service account integration (Keycloak client credentials) for sync
- Metrics for sync success/failure + last sync age

**Acceptance Criteria**:
- ✅ Registry sync populates workflows from n8n
- ✅ List endpoint returns consistent metadata (id, name, category, active)
- ✅ Sync can be triggered manually and scheduled
- ✅ Service account token is used for sync calls

**Effort**: ~1.5 days | **Details**: [stories/N8N11-workflow-registry-governance/README.md](./stories/N8N11-workflow-registry-governance/README.md)

---

### N8N13: Workflow Governance and RBAC (~500 LOC, 2 days)

**Goal**: Enforce role-based governance for workflow access across tenants.

**Deliverables**:
- Keycloak role model (WF_ADMIN, WF_EDITOR, WF_READER, WF_PROXY_SERVICE)
- Governance checks for view/edit/run decisions
- Proxy/BFF enforcement using role headers
- Audit log for governance decisions
- Cache for short-lived access decisions

**Acceptance Criteria**:
- ✅ Roles are mapped from Keycloak JWT claims
- ✅ Governance checks return allow/deny + reason
- ✅ Proxy/BFF blocks unauthorized workflow actions
- ✅ Audit entries include workflow_id, category, roles, decision

**Effort**: ~2 days | **Details**: [stories/N8N13-workflow-governance/README.md](./stories/N8N13-workflow-governance/README.md)

---

### N8N12: Webhook Security and Signature Verification (~350 LOC, 1.5 days)

**Goal**: Secure public n8n webhooks with HMAC signature validation.

**Deliverables**:
- Signature verification utility (sha256=... header)
- Per-hook secret mapping with Vault-backed config
- Reference workflow template for signature verification

**Acceptance Criteria**:
- ✅ Missing/invalid signature returns 401
- ✅ Per-hook secrets supported (env/json file)
- ✅ Audit logs include trace IDs for webhook calls

**Effort**: ~1.5 days | **Details**: [stories/N8N12-webhook-security/README.md](./stories/N8N12-webhook-security/README.md)

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

### N8N4: Workflow Templates & Documentation (~300 LOC)

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

**Effort**: ~2 hours | **Details**: [stories/N8N4-workflow-templates-documentation/README.md](./stories/N8N4-workflow-templates-documentation/README.md)

---

### N8N5: Monitoring & Alerting Integration (~300 LOC)

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

**Effort**: ~4 hours | **Details**: [stories/N8N5-monitoring-alerting-integration/README.md](./stories/N8N5-monitoring-alerting-integration/README.md)

---

---

### N8N6: Backend n8n Integration (BFF Pattern) (~1,200 LOC)

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

**Effort**: ~23 hours | **Details**: [stories/N8N6-backend-n8n-integration-bff-pattern/README.md](./stories/N8N6-backend-n8n-integration-bff-pattern/README.md)

---

## 🔐 Security Features

- **SSO**: Keycloak OIDC integration (existing identity provider)
- **JWT Authentication**: All backend API calls require valid JWT token
- **Role-Based Access**: WF_READER (read-only), WF_EDITOR (edit/run), WF_ADMIN (governance)
- **Credential Sanitization**: BFF strips sensitive data from n8n API responses
- **Audit Logging**: All admin actions logged (activate/deactivate workflows)
- **Network Isolation**: n8n internal-only, accessible via Nginx proxy or BFF API
- **Webhook Security**: HMAC signature verification for public webhooks (N8N12)

## 🚀 Implementation Plan (Multi-Tenant)

### Phase 1: Foundation (Week 4)

- [ ] N8N1: Deploy n8n + PostgreSQL (1 day)
- [ ] N8N2: Configure Keycloak SSO client (1 day)
- [ ] N8N3: Nginx reverse proxy setup (0.5 day)

**DoD**: n8n accessible via SSO at https://admin.core-platform.local/n8n

### Phase 2: Multi-Tenant Infrastructure (Week 4-5)

- [ ] N8N7: n8n Provisioning Service (2 days)
- [ ] N8N8: Multi-Tenant SSO & Routing (2 days)
- [ ] N8N9: Tenant Isolation & Audit Headers (1.5 days)

**DoD**: Per-tenant n8n access functional (`https://acme.${DOMAIN}/n8n`), auto-provisioning works, audit trail in Loki

### Phase 3: Workflows & Monitoring (Week 5)

- [ ] N8N4: Workflow templates + documentation (2 days)
- [ ] N8N5: Grafana monitoring integration (1 day)
- [ ] N8N10: Core API Connector Node (1 day)

**DoD**: Starter templates available, metrics in Grafana, Core Connector node operational

### Phase 4: Testing & Quality (Week 5)

- [ ] N8N6: Testing & Quality Gates (2 days)

**DoD**: Security tests pass, tenant isolation validated, E2E tests pass

### Phase 5: Governance & Webhook Security (Week 5)

- [ ] N8N11: Workflow Registry Sync (1.5 days)
- [ ] N8N13: Workflow Governance and RBAC (2 days)
- [ ] N8N12: Webhook Security and Signature Verification (1.5 days)

**DoD**: Registry sync + governance enforced and signed webhooks validated

## 📚 Documentation

- **N8N_MULTI_TENANT_SETUP_GUIDE.md**: Installation and multi-tenant configuration
- **N8N_WORKFLOW_TEMPLATES.md**: Starter workflow examples (per-tenant)
- **N8N_USER_GUIDE.md**: End-user workflow creation guide (tenant designers)
- **N8N_API_DOCUMENTATION.md**: Backend BFF API reference (provisioning, proxy)
- **N8N_CORE_CONNECTOR_GUIDE.md**: Custom Core Connector node usage
- **N8N_WORKFLOW_REGISTRY.md**: Registry sync and governance rules
- **N8N_WEBHOOK_SECURITY.md**: Signature verification and secrets management

## 🎓 Dependencies

- **External**: Keycloak (multi-realm support, EPIC-003 Monitoring & Observability)
- **Infrastructure**: Nginx, Docker, PostgreSQL
- **Backend**: Spring Boot, WebClient, Spring Security, n8nProvisioningService
- **Frontend**: React, TypeScript, Axios (optional: n8n UI link)
- **Skills**: n8n workflow development, multi-tenant architecture, REST API integration, OAuth2/OIDC

## 🏁 Definition of Done

- [ ] All 13 stories implemented with acceptance criteria met (N8N1-N8N13)
- [ ] n8n running in Docker Compose with PostgreSQL backend
- [ ] n8n user management ENABLED (`N8N_USER_MANAGEMENT_DISABLED=false`)
- [ ] 100% UI access requires Keycloak SSO login (per tenant)
- [ ] Per-tenant n8n access functional:
  - [ ] Admin realm: `https://admin.${DOMAIN}/n8n`
  - [ ] Tenant realms: `https://{tenant}.${DOMAIN}/n8n`
- [ ] Auto-provisioning: First access creates n8n account (`tenant-{realm}@n8n.local`)
- [ ] Keycloak roles `WF_READER/WF_EDITOR/WF_ADMIN` configured per tenant
- [ ] Nginx proxy routes per-tenant + injects audit headers (X-Core-Tenant, X-Core-User, X-Core-N8N-Account)
- [ ] Backend BFF validates tenant matches JWT realm (403 if mismatch)
- [ ] Tenant isolation validated:
  - [ ] Tenant 'acme' workflows CANNOT access tenant 'beta' data
  - [ ] Core API validates X-Core-Tenant header
- [ ] Audit trail in Loki:
  - [ ] Nginx access logs with X-Core-* headers
  - [ ] n8n workflow executions with tenant context
- [ ] Workflow registry sync available (N8N11)
- [ ] Workflow governance enforced with Keycloak roles (N8N13)
- [ ] Webhook signature verification enabled for /api/v1/webhooks/n8n/* (N8N12)
- [ ] Core Connector node operational:
  - [ ] Custom node available in n8n UI
  - [ ] Auto-injects X-Core-Tenant header
- [ ] Grafana dashboard displays n8n metrics (per tenant if possible)
- [ ] Alerting configured (execution failures >10% threshold)
- [ ] Workflow templates available (6+ examples: Jira, Confluence, Slack, AI, ETL)
- [ ] Documentation complete (multi-tenant setup guide, user guide, API docs, Core Connector guide)
- [ ] E2E tests passing:
  - [ ] Multi-tenant SSO login flow
  - [ ] Tenant isolation tests
  - [ ] Workflow execution with audit headers

---

**Epic Owner**: Platform Team  
**Priority**: High (mandatory core component)  
**Target**: Q1 2026  
**Estimated Effort**: ~21 days (13 stories: N8N1-N8N13)  
**LOC:** ~5,900 (base ~2,600 + multi-tenant ~1,800 + governance/security ~1,500)  
**Status**: 📝 Documentation complete, multi-tenant architecture redesigned, awaiting implementation

**Last Updated**: 2025-11-07
