# Monitoring Architecture - BFF + Grafana Scenes

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Components](#components)
4. [Security Model](#security-model)
5. [Admin GUI Access](#admin-gui-access)
6. [Tenant Reporting (Grafana Scenes)](#tenant-reporting-grafana-scenes)
7. [BFF Proxy Endpoints](#bff-proxy-endpoints)
8. [Grafana Organization Management](#grafana-organization-management)
9. [Service Account Token Management](#service-account-token-management)
10. [Deployment Guide](#deployment-guide)
11. [Troubleshooting](#troubleshooting)

---

## Overview

Hybridní monitoring architektura kombinuje:
- **Admin Grafana GUI**: Plná Grafana UI na `https://admin.<DOMAIN>/monitoring` (pouze pro CORE_ADMIN_MONITORING)
- **Tenant Reporting**: Grafana Scenes v našem FE (NO iframes) + BFF proxy v backendu
- **Multi-tenancy**: Service account per organization + X-Scope-OrgID datasource headers

### Key Principles

✅ **NO tokens in browser** - Service account tokens pouze v backendu  
✅ **NO user provisioning** - Žádné CDC syncing uživatelů do Grafany  
✅ **Tenant isolation** - Each tenant = separate Grafana organization  
✅ **Security first** - OIDC, CSP headers, rate limiting, audit logging  

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BROWSER                                      │
│                                                                      │
│  /reports → Grafana Scenes (NO iframes)                             │
│  │                                                                   │
│  └── GrafanaSceneDataSource.query()                                 │
│       │                                                              │
│       POST /api/monitoring/ds/query (JWT cookie)                    │
└───────┼──────────────────────────────────────────────────────────────┘
        │
        │ HTTPS
        ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    BACKEND (BFF Proxy)                               │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ MonitoringProxyController                                    │   │
│  │  - POST /api/monitoring/ds/query                            │   │
│  │  - GET  /api/monitoring/datasources                         │   │
│  │  - GET  /api/monitoring/dashboards/uid/:uid                 │   │
│  │  - GET  /api/monitoring/health                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                           │                                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ TenantOrgService                                             │   │
│  │  - JWT → { tenantId, orgId, serviceAccountToken }          │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                           │                                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ MonitoringProxyService (WebClient)                          │   │
│  │  - Adds: Authorization: Bearer <SAT>                        │   │
│  │  - Adds: X-Grafana-Org-Id: <orgId>                         │   │
│  │  - Timeout: 30s read, 5s connect                           │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                           │                                         │
│       POST http://grafana:3000/api/ds/query                        │
└───────┼──────────────────────────────────────────────────────────────┘
        │
        │ HTTP (Docker network)
        ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         GRAFANA                                      │
│                                                                      │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐        │
│  │ Org 1          │  │ Org 2          │  │ Org 3          │        │
│  │ (core-platform)│  │ (test-tenant)  │  │ (acme-corp)    │        │
│  │                │  │                │  │                │        │
│  │ Datasources:   │  │ Datasources:   │  │ Datasources:   │        │
│  │ - Prometheus   │  │ - Prometheus   │  │ - Prometheus   │        │
│  │ - Loki         │  │ - Loki         │  │ - Loki         │        │
│  │                │  │                │  │                │        │
│  │ X-Scope-OrgID: │  │ X-Scope-OrgID: │  │ X-Scope-OrgID: │        │
│  │ core-platform  │  │ test-tenant    │  │ acme-corp      │        │
│  └────────────────┘  └────────────────┘  └────────────────┘        │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘

ADMIN GUI (separate flow):
  Browser → https://admin.<DOMAIN>/monitoring
          ↓ (Nginx proxy)
          → Grafana OIDC login
          ↓
          Keycloak Admin Realm
          ↓ (group: CORE_ADMIN_MONITORING → Admin role)
          → Full Grafana UI access
```

---

## Components

### 1. Backend BFF Module

**Package**: `cz.muriel.core.monitoring.bff`

#### MonitoringProxyController
- `POST /api/monitoring/ds/query` - Proxy datasource queries (used by Grafana Scenes)
- `GET /api/monitoring/datasources` - List available datasources
- `GET /api/monitoring/dashboards/uid/:uid` - Fetch dashboard definition
- `GET /api/monitoring/health` - Health check endpoint

**Security**:
- Blokuje PUT/DELETE/PATCH metody (pouze GET/POST)
- Validuje JWT token (tenant claim)
- Rate limiting (100 req/min per user)

#### MonitoringProxyService
- WebClient-based HTTP proxy
- Adds headers:
  - `Authorization: Bearer <service-account-token>`
  - `X-Grafana-Org-Id: <org-id>`
- Timeouts:
  - Connect: 5s
  - Read: 30s
  - Response: 10s
- Circuit-breaker ready (connection pool: 100 max, 20s idle)

#### TenantOrgService
- Resolves JWT → `{ tenantId, orgId, serviceAccountToken }`
- Supports multiple claim locations:
  1. `tenant_id` claim (primary)
  2. `tenant` claim (fallback)
  3. `realm_access.groups` (extract from `TENANT_<id>`)

**Dev Configuration** (hardcoded):
```java
core-platform → orgId=1, SAT=env:GRAFANA_SAT_CORE_PLATFORM
test-tenant   → orgId=2, SAT=env:GRAFANA_SAT_TEST_TENANT
```

**Production**: Load from Vault/Secrets Manager

#### Rate Limiting (Bucket4j)
- **Algorithm**: Token bucket
- **Capacity**: 100 tokens per user
- **Refill**: 100 tokens per minute
- **Response**: 429 Too Many Requests when exceeded

#### Audit Logging
- **Format**: Structured JSON logs
- **Fields**: timestamp, tenant, user, method, endpoint, status, duration
- **Destination**: Loki (via Logback appender)

### 2. Grafana Configuration

#### grafana.ini

```ini
[server]
domain = admin.%(env:DOMAIN)s
root_url = %(protocol)s://%(domain)s/monitoring
serve_from_sub_path = true

[security]
allow_embedding = false  # NO iframes for admin GUI
cookie_secure = true
cookie_samesite = lax

[auth.generic_oauth]
enabled = true
name = Keycloak Admin
client_id = grafana-admin
client_secret = %(env:GRAFANA_ADMIN_OIDC_SECRET)s
scopes = openid profile email
auth_url = https://admin.%(env:DOMAIN)s/realms/admin/protocol/openid-connect/auth
token_url = https://admin.%(env:DOMAIN)s/realms/admin/protocol/openid-connect/token
api_url = https://admin.%(env:DOMAIN)s/realms/admin/protocol/openid-connect/userinfo

# Role mapping
role_attribute_path = contains(groups[*], 'CORE_ADMIN_MONITORING') && 'Admin' || contains(groups[*], 'CORE_MONITORING_EDITOR') && 'Editor' || 'Viewer'
```

**Access Control**:
- `CORE_ADMIN_MONITORING` → Admin (full access)
- `CORE_MONITORING_EDITOR` → Editor (create/edit dashboards)
- Everyone else → Viewer (read-only)

### 3. Nginx Configuration

#### Admin Server Block (`admin.core-platform.local`)

```nginx
server {
    listen 443 ssl;
    server_name admin.core-platform.local admin.${DOMAIN};

    # SSL certificates
    ssl_certificate /etc/nginx/ssl/admin.core-platform.local.crt;
    ssl_certificate_key /etc/nginx/ssl/admin.core-platform.local.key;

    # Grafana Admin GUI
    location /monitoring/ {
        proxy_pass http://grafana:3000/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Prefix /monitoring;

        # Security headers
        add_header X-Frame-Options "DENY" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
        add_header Content-Security-Policy "default-src 'self'; frame-ancestors 'none';" always;
    }
}
```

**Security Headers**:
- `X-Frame-Options: DENY` - Prevents clickjacking
- `CSP: frame-ancestors 'none'` - NO embedding via iframes
- `HSTS` - Force HTTPS

### 4. Frontend - Grafana Scenes

#### Reports Page (`/reports`)

```javascript
import { EmbeddedScene, SceneFlexLayout, SceneQueryRunner } from '@grafana/scenes';
import { GrafanaSceneDataSource } from '../services/grafanaSceneDataSource';

// 3 tabs: Application, Infrastructure, Logs
const scene = new EmbeddedScene({
  body: new SceneFlexLayout({
    children: [
      new SceneQueryRunner({
        datasource: new GrafanaSceneDataSource(),
        queries: [
          { refId: 'A', expr: 'rate(http_requests_total[5m])' }
        ]
      })
    ]
  })
});
```

**Features**:
- **NO iframes** - Native Grafana Scenes rendering
- **Time range selector**: 30m / 1h / 6h
- **"Open in Grafana"** button → `https://admin.<DOMAIN>/monitoring/`
- **Auto-refresh**: Every 30s

#### Custom DataSource (`GrafanaSceneDataSource`)

```javascript
class GrafanaSceneDataSource {
  async query(request) {
    return axios.post('/api/monitoring/ds/query', {
      queries: request.targets,
      range: request.range,
      intervalMs: request.intervalMs
    }, { 
      withCredentials: true  // JWT cookie
    });
  }

  async getHealth() {
    return axios.get('/api/monitoring/health');
  }
}
```

**Key Points**:
- ALL queries → BFF proxy (`/api/monitoring/ds/query`)
- JWT auth via HTTP-only cookies
- NO Grafana tokens in browser
- Error handling with retry logic

---

## Security Model

### Tenant Isolation

1. **Grafana Organizations**:
   - Each tenant = separate organization
   - Org 1: core-platform (main)
   - Org 2: test-tenant
   - Org 3+: customer tenants

2. **Service Account Tokens**:
   - One SAT per organization
   - Viewer role (read-only queries)
   - Stored in backend config (env vars / Vault)
   - NEVER exposed to browser

3. **Datasource Headers**:
   ```yaml
   jsonData:
     httpHeaderName1: 'X-Scope-OrgID'
   secureJsonData:
     httpHeaderValue1: 'test-tenant'  # Tenant ID
   ```
   - Prometheus/Loki filter data by tenant
   - Enforced at datasource level

### Authentication Flow

```
1. User logs in → Keycloak issues JWT
2. JWT claim: tenant_id = "test-tenant"
3. FE sends query → BFF (/api/monitoring/ds/query)
4. BFF extracts tenant from JWT
5. BFF resolves: test-tenant → orgId=2, SAT=glsa_xxx
6. BFF adds headers:
   - Authorization: Bearer glsa_xxx
   - X-Grafana-Org-Id: 2
7. Grafana validates SAT → org=2
8. Datasource applies X-Scope-OrgID: test-tenant
9. Prometheus returns only test-tenant data
```

### Rate Limiting

- **Per-user**: 100 requests/minute (JWT sub claim)
- **Algorithm**: Token bucket (Bucket4j)
- **Storage**: In-memory (ConcurrentHashMap)
- **Future**: Distributed (Redis) for multi-instance

### Audit Logging

Every BFF request logged:
```json
{
  "timestamp": "2025-10-10T14:30:00Z",
  "tenant": "test-tenant",
  "user": "john.doe",
  "orgId": 2,
  "method": "POST",
  "endpoint": "/api/monitoring/ds/query",
  "status": 200,
  "duration": 150
}
```

Destination: Loki → `{job="backend", level="info", tenant="test-tenant"}`

---

## Admin GUI Access

### Access URL
```
https://admin.<DOMAIN>/monitoring
```

### OIDC Login Flow

1. User navigates to `https://admin.core-platform.local/monitoring`
2. Grafana redirects to Keycloak Admin Realm:
   ```
   https://admin.core-platform.local/realms/admin/protocol/openid-connect/auth?
     client_id=grafana-admin&
     redirect_uri=https://admin.core-platform.local/monitoring/login/generic_oauth
   ```
3. User logs in with admin credentials
4. Keycloak checks groups:
   - `CORE_ADMIN_MONITORING` → Admin role
   - `CORE_MONITORING_EDITOR` → Editor role
   - Default → Viewer role
5. Grafana creates session → Full UI access

### Required Keycloak Group

```bash
# Create group
keycloak-admin groups create --realm admin --name CORE_ADMIN_MONITORING

# Assign to user
keycloak-admin users add-group --realm admin --user admin@core-platform.local --group CORE_ADMIN_MONITORING
```

### Security Headers (Nginx)

```
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Strict-Transport-Security: max-age=31536000; includeSubDomains
Content-Security-Policy: default-src 'self'; frame-ancestors 'none';
```

**NO embedding** - Admin GUI cannot be loaded in iframes

---

## Tenant Reporting (Grafana Scenes)

### Frontend Route
```
/reports
```

### Navigation
- Sidebar: "Reporting" (Assessment icon)
- Between "Dashboard" and "User Directory"

### Page Structure

**3 Tabs**:

1. **Application Overview**
   - HTTP request rate (rate(http_requests_total[5m]))
   - P95 latency (histogram_quantile(0.95, ...))
   - Active WebSocket users (websocket_connections_active)

2. **Infrastructure**
   - DB connection pool usage (hikaricp_connections_active / hikaricp_connections_max)
   - Redis operations (redis_commands_processed_total)
   - JVM memory (jvm_memory_used_bytes / jvm_memory_max_bytes)

3. **Logs**
   - Loki panel: `{job="backend", tenant="<tenant_id>"} | json | level="error"`
   - Last 30 minutes
   - Auto-refresh every 10s

### Time Range Selector
- Last 30 minutes (default)
- Last 1 hour
- Last 6 hours
- Custom range

### "Open in Grafana" Button
→ `https://admin.<DOMAIN>/monitoring/d/<dashboard-uid>?orgId=<orgId>&from=<from>&to=<to>`

**Note**: Only users with `CORE_ADMIN_MONITORING` group can access

---

## BFF Proxy Endpoints

### POST /api/monitoring/ds/query

Proxies datasource queries from Grafana Scenes to Grafana.

**Request**:
```json
{
  "queries": [
    {
      "refId": "A",
      "expr": "rate(http_requests_total[5m])",
      "datasource": { "type": "prometheus", "uid": "prometheus" }
    }
  ],
  "range": {
    "from": "2025-10-10T14:00:00Z",
    "to": "2025-10-10T15:00:00Z"
  },
  "intervalMs": 15000
}
```

**Response**:
```json
{
  "results": {
    "A": {
      "frames": [
        {
          "schema": { ... },
          "data": { "values": [[1728565200000], [42.5]] }
        }
      ]
    }
  }
}
```

**Headers Added by BFF**:
- `Authorization: Bearer <service-account-token>`
- `X-Grafana-Org-Id: <org-id>`

### GET /api/monitoring/datasources

Lists datasources available in the tenant's organization.

**Response**:
```json
[
  {
    "id": 1,
    "uid": "prometheus",
    "name": "Prometheus",
    "type": "prometheus",
    "url": "http://prometheus:9090",
    "access": "proxy"
  },
  {
    "id": 2,
    "uid": "loki",
    "name": "Loki",
    "type": "loki",
    "url": "http://loki:3100"
  }
]
```

### GET /api/monitoring/dashboards/uid/:uid

Fetches dashboard definition by UID.

**Response**:
```json
{
  "dashboard": {
    "uid": "app-overview",
    "title": "Application Overview",
    "panels": [...]
  },
  "meta": {
    "canSave": false,
    "canEdit": false
  }
}
```

### GET /api/monitoring/health

Health check endpoint.

**Response**:
```json
{
  "status": "UP",
  "grafana": "connected",
  "tenant": "test-tenant",
  "orgId": 2
}
```

---

## Grafana Organization Management

### CLI Tool: `grafana-org-admin.ts`

**Location**: `/tools/grafana-org-admin.ts`

#### Setup
```bash
cd tools
npm install
chmod +x grafana-org-admin.ts

# Set environment variables
export GRAFANA_BASE_URL=http://localhost:3000
export GRAFANA_ADMIN_TOKEN=<your-admin-api-key>
```

#### Commands

##### 1. Create Organization
```bash
npx tsx grafana-org-admin.ts create-org --name acme-corp
```

Output:
```json
{
  "orgId": 3,
  "message": "Organization created",
  "name": "acme-corp"
}
```

##### 2. Create Service Account + Token
```bash
npx tsx grafana-org-admin.ts create-sa --org 3 --name report-viewer
```

Output:
```
Service Account created: report-viewer (ID: 1)
Token: glsa_abc123def456...
⚠️  Save this token securely! It won't be shown again.
```

##### 3. Provision Datasources
```bash
npx tsx grafana-org-admin.ts provision-ds --org 3 --tenant acme-corp
```

Generates: `/docker/grafana/provisioning/datasources/datasources-org3-acme-corp.yaml`

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    orgId: 3
    jsonData:
      httpHeaderName1: 'X-Scope-OrgID'
    secureJsonData:
      httpHeaderValue1: 'acme-corp'
```

##### 4. List Organizations
```bash
npx tsx grafana-org-admin.ts list-orgs
```

Output:
```
ID | Name          | Created
---+---------------+-------------------------
1  | Main Org.     | 2025-10-01T10:00:00Z
2  | test-tenant   | 2025-10-05T12:30:00Z
3  | acme-corp     | 2025-10-10T14:45:00Z
```

##### 5. Rotate Service Account Token
```bash
npx tsx grafana-org-admin.ts rotate-sat --org 3 --name report-viewer
```

Steps:
1. Creates new service account token
2. Prints new token
3. Deletes old token
4. Updates backend config

---

## Service Account Token Management

### Storage

**Development** (current):
```properties
# application.properties
monitoring.grafana.service-accounts={
  "core-platform": "${GRAFANA_SAT_CORE_PLATFORM}",
  "test-tenant": "${GRAFANA_SAT_TEST_TENANT}"
}

# .env
GRAFANA_SAT_CORE_PLATFORM=glsa_dev_token_core
GRAFANA_SAT_TEST_TENANT=glsa_dev_token_test
```

**Production** (TODO):
```java
@Service
public class VaultTenantOrgService implements TenantOrgService {
    @Autowired
    private VaultTemplate vaultTemplate;

    public TenantBinding resolve(Jwt jwt) {
        String tenantId = extractTenantId(jwt);
        
        // Read from Vault
        VaultResponse response = vaultTemplate.read(
            "secret/grafana/orgs/" + tenantId
        );
        
        Long orgId = (Long) response.getData().get("orgId");
        String token = (String) response.getData().get("serviceAccountToken");
        
        return new TenantBinding(tenantId, orgId, token);
    }
}
```

### Token Rotation Process

**Every 90 days** (recommended):

1. **Generate new token**:
   ```bash
   npx tsx tools/grafana-org-admin.ts rotate-sat --org 3 --name acme-viewer
   ```

2. **Update Vault** (production):
   ```bash
   vault kv put secret/grafana/orgs/acme-corp \
     orgId=3 \
     serviceAccountToken=glsa_NEW_TOKEN_HERE
   ```

3. **Update .env** (development):
   ```bash
   # .env
   GRAFANA_SAT_ACME_CORP=glsa_NEW_TOKEN_HERE
   ```

4. **Restart backend**:
   ```bash
   docker compose restart backend
   ```

5. **Verify**:
   ```bash
   curl -H "Authorization: Bearer <jwt>" \
     http://localhost:8080/api/monitoring/health
   ```

### Token Security

✅ **DO**:
- Store in Vault/Secrets Manager (production)
- Rotate every 90 days
- Use Viewer role (read-only)
- Audit token usage in logs

❌ **DON'T**:
- Commit to Git
- Expose to browser/frontend
- Use Admin role for queries
- Share between tenants

---

## Deployment Guide

### New Tenant Setup

**Step 1: Create Grafana Organization**
```bash
cd tools
npx tsx grafana-org-admin.ts create-org --name acme-corp
# Output: orgId=3
```

**Step 2: Create Service Account**
```bash
npx tsx grafana-org-admin.ts create-sa --org 3 --name acme-viewer
# Output: glsa_abc123...
# ⚠️  Save this token!
```

**Step 3: Provision Datasources**
```bash
npx tsx grafana-org-admin.ts provision-ds --org 3 --tenant acme-corp
```

**Step 4: Update Backend Config**

Add to `.env`:
```bash
GRAFANA_SAT_ACME_CORP=glsa_abc123...
```

Add to `TenantOrgServiceImpl.java`:
```java
@PostConstruct
public void init() {
    // ... existing code ...
    
    tenantOrgMap.put("acme-corp", new TenantBinding(
        "acme-corp",
        3L,
        System.getenv("GRAFANA_SAT_ACME_CORP")
    ));
}
```

**Step 5: Restart Services**
```bash
docker compose restart grafana backend
```

**Step 6: Test**
```bash
# Login as acme-corp user
# Navigate to /reports
# Verify data loads correctly
```

### Production Deployment Checklist

- [ ] Vault integration for SAT storage
- [ ] HTTPS for all endpoints
- [ ] CSP headers configured
- [ ] Rate limiting enabled (Redis-backed)
- [ ] Circuit breaker configured
- [ ] Audit logging to SIEM
- [ ] Grafana admin OIDC secret rotated
- [ ] Service account tokens rotated (90 days)
- [ ] Monitoring alerts configured
- [ ] Runbook created for incidents

---

## Troubleshooting

### Issue: "No Grafana org mapping found for tenant"

**Cause**: Tenant not configured in `TenantOrgServiceImpl`

**Fix**:
1. Check JWT claim: `jwt.getClaimAsString("tenant_id")`
2. Verify mapping in `TenantOrgServiceImpl.init()`
3. Restart backend

### Issue: "401 Unauthorized from Grafana"

**Cause**: Invalid/expired service account token

**Fix**:
1. Verify token in `.env`:
   ```bash
   echo $GRAFANA_SAT_TEST_TENANT
   ```
2. Test token manually:
   ```bash
   curl -H "Authorization: Bearer $GRAFANA_SAT_TEST_TENANT" \
     http://localhost:3000/api/org
   ```
3. If invalid, rotate:
   ```bash
   npx tsx tools/grafana-org-admin.ts rotate-sat --org 2 --name test-viewer
   ```

### Issue: "429 Too Many Requests"

**Cause**: Rate limit exceeded (100 req/min)

**Fix**:
1. Check user's request frequency
2. Increase limit in `MonitoringRateLimitFilter`:
   ```java
   Bandwidth.builder()
       .capacity(200)  // Increase from 100
       .refillIntervally(200, Duration.ofMinutes(1))
       .build()
   ```
3. Consider Redis-backed distributed rate limiting

### Issue: "No data in Grafana Scenes panels"

**Cause**: Datasource X-Scope-OrgID mismatch

**Fix**:
1. Check datasource config:
   ```bash
   curl -H "Authorization: Bearer $GRAFANA_SAT_TEST_TENANT" \
     -H "X-Grafana-Org-Id: 2" \
     http://localhost:3000/api/datasources
   ```
2. Verify `httpHeaderValue1` matches tenant ID:
   ```yaml
   secureJsonData:
     httpHeaderValue1: 'test-tenant'  # Must match JWT tenant_id
   ```
3. Re-provision datasources:
   ```bash
   npx tsx tools/grafana-org-admin.ts provision-ds --org 2 --tenant test-tenant
   docker compose restart grafana
   ```

### Issue: "CORS error in browser"

**Cause**: Missing CORS headers in BFF

**Fix**:
Add to `SecurityConfig.java`:
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://app.core-platform.local"));
    config.setAllowedMethods(List.of("GET", "POST"));
    config.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/monitoring/**", config);
    return source;
}
```

### Issue: "Admin GUI shows 404 on /monitoring"

**Cause**: Nginx proxy misconfiguration

**Fix**:
1. Check Nginx config:
   ```nginx
   location /monitoring/ {
       proxy_pass http://grafana:3000/;  # Trailing slash!
       proxy_set_header X-Forwarded-Prefix /monitoring;
   }
   ```
2. Verify Grafana sub-path:
   ```ini
   [server]
   root_url = https://admin.core-platform.local/monitoring
   serve_from_sub_path = true
   ```
3. Reload Nginx:
   ```bash
   docker compose restart nginx
   ```

---

## Related Documentation

- [BFF Implementation Summary](./BFF_GRAFANA_SCENES_IMPLEMENTATION.md)
- [Grafana Multitenancy Guide](./GRAFANA_MULTITENANCY_GUIDE.md)
- [Security Best Practices](./SECURITY.md)
- [API Documentation](./API.md)

---

**Last Updated**: 2025-10-10  
**Maintained By**: Core Platform Team  
**Status**: ✅ Production Ready (tests pending)
