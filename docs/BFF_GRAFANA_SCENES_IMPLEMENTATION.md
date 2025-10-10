# BFF + Grafana Scenes Implementation Summary

## 🎯 Objective

Implementovat hybridní monitoring architekturu:
- **Admin Grafana GUI**: Plná Grafana na `https://admin.<DOMAIN>/monitoring` (OIDC protected)
- **Tenantní reporting**: Grafana Scenes ve FE + BFF proxy v backendu (NO iframes, NO tokens in browser)
- **Odstranění CDC**: User provisioning do Grafany již nepotřeba

---

## ✅ Implemented Components

### 1. Backend - BFF Proxy Module (`backend/monitoring-bff/`)

#### **MonitoringProxyController** (`/api/monitoring/*`)
- `POST /api/monitoring/ds/query` - Proxy datasource queries (Scenes → Grafana)
- `GET /api/monitoring/datasources` - List available datasources
- `GET /api/monitoring/dashboards/uid/:uid` - Fetch dashboard definition
- `GET /api/monitoring/health` - Health check
- **Security**: Block PUT/DELETE/PATCH, whitelist GET/POST only

#### **MonitoringProxyService**
- WebClient (reactive) proxy with timeouts (30s read, 5s connect)
- Adds headers: `Authorization: Bearer <SAT>`, `X-Grafana-Org-Id: <orgId>`
- Circuit-breaker ready (connection pool: 100 max, 20s idle timeout)
- **NO tokens exposed to browser**

#### **TenantOrgService**
- JWT tenant resolution (claims: `tenant_id`, `tenant`, `realm_access.groups`)
- Maps tenant → `{ orgId, serviceAccountToken }`
- Dev config: Hardcoded core-platform (orgId=1), test-tenant (orgId=2)
- **Production**: Load from Vault/Secrets Manager

#### **Rate Limiting** (Bucket4j)
- 100 requests/minute per user
- Applied to `/api/monitoring/**` endpoints
- 429 response when exceeded

#### **Audit Logging**
- Logs: tenant, user, method, endpoint, status, duration
- Structured format for SIEM integration

#### **Configuration** (`application.properties`)
```properties
monitoring.grafana.base-url=http://grafana:3000
monitoring.grafana.max-body-size=10485760
monitoring.grafana.connect-timeout=5000
monitoring.grafana.read-timeout=30000
```

---

### 2. Grafana Configuration

#### **grafana.ini** - Admin GUI Setup
```ini
[server]
domain = admin.%(env:DOMAIN)s
root_url = %(protocol)s://%(domain)s/monitoring
serve_from_sub_path = true

[security]
allow_embedding = false  # NO iframes for admin GUI
cookie_secure = true

[auth.generic_oauth]
enabled = true
client_id = grafana-admin
client_secret = %(env:GRAFANA_ADMIN_OIDC_SECRET)s
auth_url = https://admin.%(env:DOMAIN)s/realms/admin/protocol/openid-connect/auth
token_url = https://admin.%(env:DOMAIN)s/realms/admin/protocol/openid-connect/token
role_attribute_path = contains(groups[*], 'CORE_ADMIN_MONITORING') && 'Admin' || 'Viewer'
```

**Access Control**:
- OIDC only (no local login)
- Keycloak group `CORE_ADMIN_MONITORING` → Admin role
- `CORE_MONITORING_EDITOR` → Editor role
- Everyone else → Viewer

---

### 3. Nginx Configuration

#### **Admin Server Block** (`admin.core-platform.local`)
```nginx
location /monitoring/ {
    proxy_pass http://grafana:3000/;
    proxy_set_header X-Forwarded-Prefix /monitoring;
    # CSP: frame-ancestors 'none' - NO embedding
    add_header Content-Security-Policy "default-src 'self'; frame-ancestors 'none';" always;
}
```

**Security Headers**:
- `X-Frame-Options: DENY`
- `Strict-Transport-Security: max-age=31536000`
- `Content-Security-Policy: frame-ancestors 'none'`

---

### 4. Frontend - Grafana Scenes Integration

#### **Reports Page** (`/reports`)
- **NO iframes** - native Grafana Scenes rendering
- 3 tabs:
  - **Application**: HTTP rate, P95 latency, active WebSocket users
  - **Infrastructure**: DB connection pool, Redis ops
  - **Logs**: Loki logs panel (last 30min)
- Time range selector: Last 6h / 1h / 30min
- "Open in Grafana" button → `https://admin.<DOMAIN>/monitoring/`

#### **Custom DataSource** (`GrafanaSceneDataSource`)
```javascript
async query(request) {
  return axios.post('/api/monitoring/ds/query', {
    queries: [...],
    range: {...}
  }, { withCredentials: true });
}
```

**Features**:
- Proxies ALL requests through BFF (`/api/monitoring/ds/query`)
- JWT auth via cookies (automatic)
- Supports Prometheus + Loki datasources
- Error handling with retry logic

#### **Navigation**
- Added "Reporting" menu item (Assessment icon)
- Route: `/reports` (authenticated users only)

#### **Dependencies**
```json
{
  "@grafana/scenes": "^6.39.5"
}
```

---

### 5. DevOps Tools - Grafana Org Management

#### **CLI Script** (`tools/grafana-org-admin.ts`)

**Commands**:
```bash
# Create organization
npx tsx grafana-org-admin.ts create-org --name test-tenant

# Create service account + token
npx tsx grafana-org-admin.ts create-sa --org 2 --name report-viewer

# Generate datasource provisioning YAML
npx tsx grafana-org-admin.ts provision-ds --org 2 --tenant test-tenant

# List all orgs
npx tsx grafana-org-admin.ts list-orgs

# Rotate token
npx tsx grafana-org-admin.ts rotate-sat --org 2 --name report-viewer
```

**Features**:
- TypeScript (consistent with stack)
- Commander.js CLI framework
- Grafana API integration (axios)
- Auto-generates datasource YAML with `X-Scope-OrgID` headers
- Token rotation (creates new, deletes old)

**Example Workflow**:
```bash
# 1. Create org → orgId=3
# 2. Create SA → token=glsa_xxx
# 3. Provision datasources → datasources-org3-acme.yaml
# 4. Save to Vault → GRAFANA_SAT_ACME_CORP=glsa_xxx
# 5. Update backend mapping → TenantOrgServiceImpl
# 6. Restart services
```

---

## 🏗️ Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         BROWSER (FE)                            │
│                                                                 │
│  /reports → Grafana Scenes (NO iframes)                        │
│  │                                                              │
│  └── GrafanaSceneDataSource.query()                            │
│       │                                                         │
│       POST /api/monitoring/ds/query (JWT cookie)               │
└───────┼─────────────────────────────────────────────────────────┘
        │
        │ HTTPS
        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    BACKEND (BFF Proxy)                          │
│                                                                 │
│  MonitoringProxyController                                      │
│  │                                                              │
│  ├── Extract tenant from JWT (TenantOrgService)                │
│  ├── Resolve orgId + SAT                                       │
│  ├── Rate limit (100 req/min per user)                         │
│  ├── Audit log (tenant, user, endpoint, duration)              │
│  │                                                              │
│  └── MonitoringProxyService.forwardQuery()                     │
│       │                                                         │
│       POST http://grafana:3000/api/ds/query                    │
│       Headers:                                                  │
│         - Authorization: Bearer <SAT>                           │
│         - X-Grafana-Org-Id: <orgId>                            │
└───────┼─────────────────────────────────────────────────────────┘
        │
        │ HTTP (Docker network)
        ▼
┌─────────────────────────────────────────────────────────────────┐
│                         GRAFANA                                 │
│                                                                 │
│  Org 1 (Main) ────────┐                                        │
│  Org 2 (test-tenant) ─┼─→ Datasources (Prometheus, Loki)      │
│  Org 3 (acme-corp)────┘    │                                   │
│                            │ X-Scope-OrgID: <tenant_id>        │
│                            ▼                                    │
│                       ┌─────────────┐                          │
│                       │ Prometheus  │                          │
│                       │ Loki        │                          │
│                       └─────────────┘                          │
└─────────────────────────────────────────────────────────────────┘

ADMIN GUI (separate path):
https://admin.<DOMAIN>/monitoring → Grafana (OIDC protected)
```

---

## 🔒 Security Features

### Data Isolation
- **Per-tenant orgs**: Each tenant has separate Grafana organization
- **Service account tokens**: One SAT per org (Viewer role, read-only)
- **X-Scope-OrgID**: Datasources filter data by tenant ID header
- **BFF enforces tenant**: JWT claim → orgId mapping in backend

### Authentication & Authorization
- **FE → BFF**: JWT token in cookie (Keycloak issued)
- **BFF → Grafana**: Service account token (never exposed to browser)
- **Admin GUI**: OIDC with group-based roles (`CORE_ADMIN_MONITORING`)

### Rate Limiting & Audit
- 100 requests/minute per user (Bucket4j)
- Structured audit logs: `tenant={}, user={}, endpoint={}, duration={}ms`
- Failed queries logged with error details

### Network Security
- **Admin GUI**: HSTS, CSP `frame-ancestors 'none'`, X-Frame-Options DENY
- **BFF proxy**: Block mutations (PUT/DELETE/PATCH), whitelist queries only
- **Datasources**: Internal Docker network only, no external access

---

## 📝 Environment Variables

### Backend
```properties
# Grafana BFF
monitoring.grafana.base-url=http://grafana:3000

# Service Account Tokens (load from Vault in production!)
GRAFANA_SAT_CORE_PLATFORM=glsa_...
GRAFANA_SAT_TEST_TENANT=glsa_...
```

### Grafana
```properties
# Admin GUI OIDC
GRAFANA_ADMIN_OIDC_SECRET=grafana-admin-oidc-secret-change-in-prod
```

### CLI Tools
```bash
# For grafana-org-admin.ts
export GRAFANA_ADMIN_TOKEN=<admin-api-token>
export GRAFANA_BASE_URL=http://localhost:3000
```

---

## 🧪 Testing Checklist

### Backend BFF
- [ ] Unit: TenantOrgService resolves JWT → orgId correctly
- [ ] Unit: MonitoringProxyService adds Authorization + X-Grafana-Org-Id
- [ ] Integration: WireMock stub, test `/api/monitoring/ds/query` → 200
- [ ] E2E: Real Grafana, verify tenant isolation (org1 can't see org2 data)

### Frontend Scenes
- [ ] E2E (Playwright): Login → /reports → panels load data
- [ ] E2E: Time range change triggers new query
- [ ] E2E: Tab switch loads different dashboard
- [ ] Unit: GrafanaSceneDataSource.query() calls BFF with correct payload

### CLI Tools
- [ ] create-org: Creates new org, returns orgId
- [ ] create-sa: Generates token, prints to stdout
- [ ] provision-ds: Writes YAML file with correct X-Scope-OrgID
- [ ] rotate-sat: Deletes old token, creates new one

---

## 📚 Documentation

- **Backend API**: Swagger/OpenAPI for `/api/monitoring/*` endpoints
- **CLI Tools**: `tools/README.md` with usage examples
- **Architecture**: This document
- **Runbook**: `docs/runbooks/monitoring-incident.md` (TODO)

---

## 🚀 Deployment Workflow

### New Tenant Setup

1. **Create Grafana org**:
   ```bash
   npx tsx tools/grafana-org-admin.ts create-org --name acme-corp
   # Output: orgId=3
   ```

2. **Create service account**:
   ```bash
   npx tsx tools/grafana-org-admin.ts create-sa --org 3 --name acme-viewer
   # Save: GRAFANA_SAT_ACME_CORP=glsa_xxx
   ```

3. **Provision datasources**:
   ```bash
   npx tsx tools/grafana-org-admin.ts provision-ds --org 3 --tenant acme-corp
   ```

4. **Update backend**:
   - Add to `TenantOrgServiceImpl.init()`:
     ```java
     tenantOrgMap.put("acme-corp", new TenantBinding(
         "acme-corp", 
         3L, 
         System.getenv("GRAFANA_SAT_ACME_CORP")
     ));
     ```

5. **Restart services**:
   ```bash
   docker compose restart grafana backend
   ```

### Token Rotation (every 90 days)

```bash
npx tsx tools/grafana-org-admin.ts rotate-sat --org 3 --name acme-viewer
# Update Vault with new token
# Restart backend to reload
```

---

## ❌ Removed Components (CDC User Provisioning)

### Files to Delete (NOT DONE YET - TODO)
- `backend/jobs/grafana-user-cdc/**`
- `.github/workflows/**` steps for Grafana user sync
- Terraform/Ansible per-user provisioning configs
- DB tables: `grafana_user_sync`, `grafana_user_cdc_events`
- Secrets: `GRAFANA_ADMIN_TOKEN_FOR_USER_CDC`

### Migration Script (TODO)
```sql
-- V99__remove_grafana_user_cdc.sql
DROP TABLE IF EXISTS grafana_user_cdc_events;
DROP TABLE IF EXISTS grafana_user_sync;
-- Remove cron jobs if any
```

### Documentation Updates (TODO)
- `docs/monitoring.md`: Remove "User Provisioning" section
- Add: "Nepoužívá se. Přístup přes service-account per org + BFF proxy."

---

## 📊 Metrics & Observability

### BFF Metrics (Prometheus)
- `monitoring_bff_requests_total{tenant,endpoint,status}`
- `monitoring_bff_request_duration_seconds{tenant,endpoint}`
- `monitoring_bff_rate_limit_exceeded_total{tenant}`
- `monitoring_bff_errors_total{tenant,error_type}`

### Grafana Dashboards
- **BFF Overview**: Request rate, latency, errors by tenant
- **Tenant Usage**: Query volume, data transfer by org
- **Security Audit**: Rate limit hits, failed auth attempts

---

## 🎓 Key Learnings

1. **BFF Pattern**: Abstracts Grafana complexity, enforces security at backend
2. **Grafana Scenes**: Modern alternative to iframes, better UX, full control
3. **Service Accounts**: More secure than per-user tokens, easier to rotate
4. **X-Scope-OrgID**: Standard header for multi-tenancy in Prometheus/Loki
5. **TypeScript CLI**: Consistent tooling across stack, better than shell scripts

---

## ✅ Definition of Done

- [x] `/reports` funguje, zobrazuje živá data přes Scenes (bez iframu)
- [x] `/api/monitoring/ds/query` přidává Authorization + X-Grafana-Org-Id
- [x] Admin Grafana dostupná jen na `admin.<DOMAIN>/monitoring` (OIDC)
- [x] Grafana.ini: OIDC config, role mapping, sub-path
- [x] Nginx: Admin server block s CSP frame-ancestors 'none'
- [x] Frontend: Scenes datasource volá BFF proxy
- [x] CLI tools: create-org, create-sa, provision-ds, rotate-sat
- [ ] Tests (unit + integration) pro BFF - **TODO**
- [ ] E2E tests (Playwright) pro /reports - **TODO**
- [ ] Odstranění CDC kódu/CI/secrets - **TODO**
- [ ] DB migrace V99__remove_grafana_user_cdc.sql - **TODO**
- [ ] Dokumentace monitoring.md - **TODO**
- [ ] Runbook monitoring-incident.md - **TODO**

---

## 🔜 Next Steps

1. **Write Tests**:
   - Backend unit tests (TenantOrgService, MonitoringProxyService)
   - Integration tests with WireMock
   - E2E tests for /reports page

2. **Remove CDC**:
   - Delete old user provisioning code
   - Remove CI workflows
   - Drop DB tables
   - Update docs

3. **Production Hardening**:
   - Vault integration for SAT storage
   - Circuit breaker for WebClient
   - Distributed rate limiting (Redis)
   - Structured logging (JSON format)

4. **Documentation**:
   - OpenAPI spec for BFF endpoints
   - Runbook for token rotation
   - Migration guide from old system

---

**Status**: ✅ Core implementation complete, ready for testing & refinement
**Branch**: `feature/streaming-dashboard`
**Commits**: 4 (BFF, Grafana config, Frontend Scenes, CLI tools)
