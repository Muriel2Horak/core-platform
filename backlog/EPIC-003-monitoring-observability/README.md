# EPIC-003: Monitoring & Observability Stack

**Status:** 🟡 **70% COMPLETE** (Core stack done, frontend dashboards TODO)  
**Implementováno:** Září - Říjen 2024 (Loki + Prometheus + Native UI)  
**LOC:** ~13,500 řádků (~8,000 done + ~5,500 TODO)  
**Dokumentace:** `MONITORING_COMPLETE.md`, `LOKI_MIGRATION_COMPLETE.md`, `EPIC_COMPLETE_LOKI_UI.md`

---

## 🎯 Cíle EPICu

### Primární Cíle

**Vytvořit enterprise-grade observability stack** s centralizovaným logováním, metrikami a nativním frontend UI, který poskytuje real-time přehled o zdraví platformy a umožňuje rychlou diagnostiku problémů **bez závislosti na externích vizualizačních nástrojích**.

### Klíčové Principy

1. **Native-First Approach**: 
   - Primární UX = **vlastní React komponenty** v našem frontendu
   - Loki UI + Prometheus metriky renderované nativně
   - **Žádné embedování třetích stran** do tenant UI

2. **Tenant Isolation**:
   - Každý tenant vidí **pouze své logy a metriky**
   - Izolace na úrovni Keycloak realm + tenant_id labels
   - BFF API kontroluje tenant claims před Loki/Prometheus queries

3. **Security by Design**:
   - **Všechno přes HTTPS** (TLS terminace v Nginx)
   - **Žádný přímý přístup** z browseru na Prometheus/Loki
   - Rate limiting, CSP headers, redakce citlivých dat

4. **Grafana jako Optional Tool**:
   - **NENÍ součást core UX** pro běžné uživatele
   - Slouží **pouze pro SRE/ops týmy** (admin realm)
   - Běží za Nginx reverse proxy na `https://admin.<domain>/grafana`
   - OIDC/SSO pouze vůči **admin realm** (žádný per-tenant org cirkus)

### Business Goals
- **Proaktivní monitoring**: Detekovat problémy před eskalací (SLO: MTTR < 15 minut)
- **Multi-tenant visibility**: Každý tenant vidí své metriky (zero cross-tenant leak)
- **Compliance**: Audit logs pro regulatorní požadavky (GDPR, SOC2)
- **Self-Service**: Uživatelé mohou debugovat bez ops týmu
- **Cost Efficiency**: Vlastní UI = žádné Grafana licence per tenant

---

## 🏗️ Architektura Observability Stacku

### Core Stack (POVINNÉ komponenty)

```
┌─────────────────────────────────────────────────────────────────┐
│                      TENANT UI (React)                         │
│  ┌──────────────────┐  ┌──────────────────┐                    │
│  │  📊 Monitoring   │  │  📝 Log Viewer   │                    │
│  │  Dashboard       │  │  (Loki UI)       │                    │
│  │  - Metrics       │  │  - Real-time     │                    │
│  │  - Health Cards  │  │  - Filters       │                    │
│  │  - SLO Tracking  │  │  - Search        │                    │
│  └────────┬─────────┘  └────────┬─────────┘                    │
└───────────┼─────────────────────┼──────────────────────────────┘
            │                     │
            │ HTTPS (JWT)         │ HTTPS (JWT)
            ▼                     ▼
┌───────────────────────────────────────────────────────────────┐
│                   BACKEND BFF (Spring Boot)                   │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  Tenant Guard: Extract realm from JWT                   │  │
│  │  → Inject {tenant="<realm>"} filter into all queries    │  │
│  └─────────────────────────────────────────────────────────┘  │
│                     │                       │                  │
│       /api/monitoring/metrics    /api/monitoring/logs         │
│                     ▼                       ▼                  │
│          ┌─────────────────┐     ┌──────────────────┐         │
│          │  Prometheus     │     │  Loki Client     │         │
│          │  Java Client    │     │  (LogQL)         │         │
│          └────────┬────────┘     └────────┬─────────┘         │
└───────────────────┼──────────────────────┼────────────────────┘
                    │                      │
         Internal   │                      │  Internal
         Network    │                      │  Network
                    ▼                      ▼
         ┌────────────────┐     ┌───────────────────┐
         │  Prometheus    │     │  Loki             │
         │  :9090         │     │  :3100            │
         │  (metrics DB)  │     │  (log aggregator) │
         └────────────────┘     └───────────────────┘
                ▲                          ▲
                │                          │
                │ Pull metrics             │ Push logs
                │                          │
        ┌───────┴──────┐          ┌────────┴────────┐
        │  Exporters   │          │  Promtail       │
        │  (JVM, DB,   │          │  (log shipper)  │
        │   Redis,     │          │                 │
        │   Kafka)     │          │                 │
        └──────────────┘          └─────────────────┘
```

### Optional Stack (Pro SRE/Ops týmy)

```
┌────────────────────────────────────────────────────────────┐
│                 ADMIN REALM ONLY                           │
│         https://admin.<domain>/grafana                     │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Grafana (Optional - Pro SRE/Ops)                    │  │
│  │  - Advanced dashboards                               │  │
│  │  - Cross-tenant view (admin only)                    │  │
│  │  - Alerting & notifications                          │  │
│  │  - OIDC/SSO → admin realm                           │  │
│  └──────────────────────────────────────────────────────┘  │
│                           ▲                                 │
│                           │ Reverse Proxy                   │
│                           │ /grafana → :3000                │
└───────────────────────────┼─────────────────────────────────┘
                            │
                    ┌───────┴────────┐
                    │  Nginx          │
                    │  (TLS, CSP,     │
                    │   Rate Limit)   │
                    └─────────────────┘
```

---

## 📊 Native Monitoring UI (Primary Interface)

### 1. Loki UI - Log Viewer

**Status:** ✅ **DONE** (Říjen 2024)  
**Implementace:** `frontend/src/pages/Monitoring/LogViewer.tsx`

#### Features
- **Real-time log streaming** (WebSocket connection)
- **LogQL query builder** (GUI + raw mode)
- **Tenant-scoped filtering** (automatic {realm="<tenant>"} injection)
- **Time range selection** (last 5m, 1h, 24h, custom)
- **Log level filtering** (ERROR, WARN, INFO, DEBUG, TRACE)
- **Full-text search** (regex support)
- **Context view** (show surrounding logs)
- **Export** (JSON, CSV)

#### Backend BFF API
```java
// backend/src/main/java/cz/muriel/core/monitoring/MonitoringController.java
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
  
  @GetMapping("/logs")
  public ResponseEntity<LogQueryResponse> queryLogs(
    @RequestParam String query,
    @RequestParam(required = false) Long start,
    @RequestParam(required = false) Long end,
    @RequestParam(defaultValue = "100") int limit,
    @AuthenticationPrincipal Jwt jwt
  ) {
    // 1. Extract tenant from JWT
    String realm = extractRealm(jwt);
    
    // 2. Inject tenant filter into LogQL query
    String scopedQuery = injectTenantFilter(query, realm);
    
    // 3. Execute query against Loki
    LogQueryResponse response = lokiClient.queryRange(
      scopedQuery, 
      start != null ? start : System.currentTimeMillis() - 3600000,
      end != null ? end : System.currentTimeMillis(),
      limit
    );
    
    return ResponseEntity.ok(response);
  }
  
  private String injectTenantFilter(String query, String realm) {
    // {service="backend"} → {service="backend",realm="tenant-a"}
    if (query.contains("{")) {
      return query.replaceFirst("\\{", "{realm=\"" + realm + "\",");
    } else {
      // service → {realm="tenant-a",service}
      return "{realm=\"" + realm + "\"} " + query;
    }
  }
}
```

#### Frontend Implementation
```typescript
// frontend/src/pages/Monitoring/LogViewer.tsx
export const LogViewer: React.FC = () => {
  const [query, setQuery] = useState<string>('{service="backend"}');
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [loading, setLoading] = useState(false);
  
  const fetchLogs = async () => {
    setLoading(true);
    try {
      const response = await api.get('/api/monitoring/logs', {
        params: {
          query,
          start: Date.now() - 3600000, // Last hour
          limit: 100
        }
      });
      setLogs(response.data.data.result);
    } catch (error) {
      console.error('Failed to fetch logs:', error);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <Box>
      <Typography variant="h4">Log Viewer</Typography>
      
      {/* Query Builder */}
      <LogQLQueryBuilder
        value={query}
        onChange={setQuery}
        onExecute={fetchLogs}
      />
      
      {/* Log Table */}
      <LogTable
        logs={logs}
        loading={loading}
        onRefresh={fetchLogs}
      />
    </Box>
  );
};
```

#### Tenant Isolation Example
```bash
# User v realmu "tenant-a" zadá query:
{service="backend"} | json | level="ERROR"

# Backend BFF automaticky injectuje:
{realm="tenant-a",service="backend"} | json | level="ERROR"

# Loki vrátí POUZE logy z realm="tenant-a"
# → Zero cross-tenant data leak ✅
```

---

### 2. Prometheus Metrics - Business Dashboards

**Status:** 🔵 **TODO** (Q1 2025)  
**Implementace:** `frontend/src/pages/Monitoring/MetricsDashboard.tsx`

#### Planned Features
- **System Health Cards**:
  - Backend uptime, request rate, error rate
  - Database connections, query latency
  - Redis hit rate, memory usage
  - Kafka consumer lag, topic size
  
- **Business Metrics**:
  - Active users (per tenant)
  - Workflow executions (success/failed/pending)
  - DMS operations (upload/download/OCR)
  - API usage (per endpoint)

- **SLO Tracking**:
  - API P95 latency < 500ms
  - Error rate < 1%
  - Database P99 < 100ms

#### Backend BFF API (Planned)
```java
@GetMapping("/metrics/{metricName}")
public ResponseEntity<MetricQueryResponse> queryMetric(
  @PathVariable String metricName,
  @RequestParam(required = false) String aggregation,
  @RequestParam(required = false) Long start,
  @RequestParam(required = false) Long end,
  @AuthenticationPrincipal Jwt jwt
) {
  String realm = extractRealm(jwt);
  
  // PromQL query s tenant filter
  String query = String.format(
    "%s{tenant=\"%s\"}",
    metricName,
    realm
  );
  
  if (aggregation != null) {
    query = String.format("%s(%s)", aggregation, query);
  }
  
  MetricQueryResponse response = prometheusClient.queryRange(
    query,
    start != null ? start : System.currentTimeMillis() - 3600000,
    end != null ? end : System.currentTimeMillis(),
    "15s" // Step
  );
  
  return ResponseEntity.ok(response);
}
```

#### Frontend Implementation (Planned)
```typescript
// frontend/src/pages/Monitoring/MetricsDashboard.tsx
export const MetricsDashboard: React.FC = () => {
  return (
    <Grid container spacing={3}>
      {/* Health Cards */}
      <Grid item xs={12} md={3}>
        <HealthCard
          title="API Health"
          metric="http_server_requests_seconds_count"
          threshold={{ warning: 100, critical: 500 }}
        />
      </Grid>
      
      {/* Time Series Charts */}
      <Grid item xs={12}>
        <LineChart
          title="Request Rate (req/s)"
          query="rate(http_server_requests_seconds_count[5m])"
          yAxisLabel="Requests/sec"
        />
      </Grid>
      
      {/* Business Metrics */}
      <Grid item xs={12} md={6}>
        <MetricCard
          title="Active Workflows"
          query='sum(workflow_execution_status{status="running"})'
          format="number"
        />
      </Grid>
    </Grid>
  );
};
```

---

## 🔧 Optional Grafana (Pro SRE/Ops)

### Pozice v Architektuře

**Grafana NENÍ povinná součást core UX.** Slouží jako **pokročilý nástroj pro provozní týmy**.

#### Kdy používat Grafana:
- ✅ **SRE/DevOps debugging**: Potřebuješ cross-tenant view, pokročilé queries
- ✅ **Alerting management**: Nastavování alert rules, notification channels
- ✅ **Dashboard prototyping**: Rychlý vývoj nových dashboardů před portováním do FE
- ✅ **Ad-hoc analysis**: Složité PromQL/LogQL queries, které nejsou v native UI

#### Kdy NEPOUŽÍVAT Grafana:
- ❌ **Běžní tenant uživatelé**: Používají Native Monitoring UI
- ❌ **Embed do tenant stránek**: Žádné iframe/Scenes embedded
- ❌ **Per-tenant provisioning**: Žádné auto-vytváření Grafana orgs per tenant

### Konfigurace

#### Nginx Reverse Proxy
```nginx
# docker/nginx/nginx-ssl.conf
location /grafana/ {
    # Admin realm only!
    auth_request /auth/validate-admin;
    
    proxy_pass http://grafana:3000/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto https;
    
    # Rate limiting (bruteforce protection)
    limit_req zone=admin burst=20 nodelay;
    
    # Security headers
    add_header Content-Security-Policy "frame-ancestors 'self'" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
}

location /auth/validate-admin {
    internal;
    proxy_pass http://backend:8080/api/auth/validate-role;
    proxy_set_header X-Required-Role "CORE_PLATFORM_ADMIN,OBSERVABILITY_ADMIN";
}
```

#### Grafana OIDC Configuration
```ini
# docker/grafana/grafana.ini
[server]
root_url = https://admin.core-platform.local/grafana
serve_from_sub_path = true

[auth.generic_oauth]
enabled = true
name = Keycloak
allow_sign_up = true
client_id = grafana-admin-client
client_secret = ${GRAFANA_OIDC_SECRET}
scopes = openid email profile
auth_url = https://admin.core-platform.local/realms/admin/protocol/openid-connect/auth
token_url = https://admin.core-platform.local/realms/admin/protocol/openid-connect/token
api_url = https://admin.core-platform.local/realms/admin/protocol/openid-connect/userinfo
role_attribute_path = contains(realm_access.roles[*], 'CORE_PLATFORM_ADMIN') && 'Admin' || 'Viewer'

# POUZE admin realm!
# Žádný per-tenant org provisioning
# Žádný multi-tenant SSO cirkus
```

#### Security Constraints
- **TLS Only**: Vždy přes HTTPS (port 443)
- **Admin Realm Only**: SSO pouze proti `admin` realm v Keycloaku
- **RBAC**: Přístup pouze pro role `CORE_PLATFORM_ADMIN`, `OBSERVABILITY_ADMIN`
- **CSP**: `frame-ancestors 'self'` - žádné embedování do cizích domén
- **Rate Limiting**: Max 20 req/s na /grafana endpoint (bruteforce ochrana)

### Recommended Dashboards (Pro SRE)

Grafana dashboardy (v `docker/grafana/dashboards/`):

1. **System Overview** (`system-overview.json`):
   - CPU, Memory, Disk usage (všechny služby)
   - Network I/O
   - Container restarts

2. **Application Runtime** (`app-runtime.json`):
   - JVM heap/non-heap memory
   - GC pauses
   - Thread count
   - HTTP request rate & latency

3. **Database Performance** (`database-perf.json`):
   - Connection pool usage
   - Query latency (P50, P95, P99)
   - Slow query count
   - Transaction rate

4. **Kafka Monitoring** (`kafka-monitoring.json`):
   - Topic size, partition count
   - Consumer lag per group
   - Producer throughput
   - DLQ message count

5. **Security Audit** (`security-audit.json`):
   - Failed login attempts
   - Role changes
   - Admin actions
   - Suspicious patterns

**POZNÁMKA:** Tyto dashboardy jsou **pouze pro admin realm**. Běžní tenant uživatelé používají **Native Monitoring UI** s subset těchto metrik.

---

## 🔒 Security & Compliance (EPIC-000 Integration)

### TLS & Network Security

**VŠECHEN monitoring provoz běží pouze přes HTTPS** terminovaný v Nginx.

#### Konfigurace (odkaz na EPIC-000)
```nginx
# SSL/TLS Configuration (z EPIC-000)
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers HIGH:!aNULL:!MD5;
ssl_certificate /etc/nginx/ssl/server.crt.pem;
ssl_certificate_key /etc/nginx/ssl/server.key.pem;

# Security headers (z EPIC-000)
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "SAMEORIGIN" always;
add_header Content-Security-Policy "frame-ancestors 'self'" always;
```

#### Endpoint Security

| Endpoint | Exposed | Security | Notes |
|----------|---------|----------|-------|
| **Loki `:3100`** | ❌ Internal only | BFF API kontroluje JWT | Žádný přímý přístup z browseru |
| **Prometheus `:9090`** | ❌ Internal only | BFF API kontroluje JWT | Žádný přímý přístup z browseru |
| **Grafana `/grafana`** | ✅ Via Nginx | OIDC/SSO + admin realm | Rate limited, CSP protected |
| **BFF `/api/monitoring/*`** | ✅ Via Nginx | JWT validation + Tenant Guard | TLS, rate limited |

### Data Sensitivity & Redaction

**Logy mohou obsahovat citlivá data** (PII, credentials, business data).

#### Povinné opatření (z EPIC-000):

1. **Redakce vybraných polí**:
   ```java
   // backend/src/main/resources/logback-spring.xml
   <encoder class="net.logstash.logback.encoder.LogstashEncoder">
     <fieldNames>
       <message>message</message>
       <levelValue>[ignore]</levelValue>
     </fieldNames>
     <jsonGeneratorDecorator class="cz.muriel.core.logging.PiiRedactingDecorator"/>
   </encoder>
   ```
   
   ```java
   // PII Redaction
   public class PiiRedactingDecorator implements JsonGeneratorDecorator {
     private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
     private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d{1,3}[\\s-]?\\(?\\d{2,4}\\)?[\\s-]?\\d{3,4}[\\s-]?\\d{3,4}");
     
     @Override
     public JsonGenerator decorate(JsonGenerator generator) {
       return new RedactingGenerator(generator, List.of(EMAIL_PATTERN, PHONE_PATTERN));
     }
   }
   ```

2. **Retenční politika**:
   ```yaml
   # loki/loki-config.yml
   limits_config:
     retention_period: 2160h  # 90 days default
     
   # Audit logs (compliance)
   retention_stream:
     - selector: '{level="AUDIT"}'
       priority: 1
       period: 8760h  # 365 days
   ```

3. **Audit přístupů**:
   ```java
   @GetMapping("/logs")
   @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CORE_PLATFORM_ADMIN')")
   public ResponseEntity<LogQueryResponse> queryLogs(...) {
     // Audit log access
     auditService.logAccess(
       AuditEvent.builder()
         .user(jwt.getSubject())
         .action("QUERY_LOGS")
         .resource("monitoring.logs")
         .query(query)
         .timestamp(Instant.now())
         .build()
     );
     
     // ... execute query
   }
   ```

### Compliance Requirements

Monitoring stack musí splňovat (z EPIC-000):

- ✅ **GDPR**: PII redakce, data retention, right to be forgotten
- ✅ **SOC2**: Audit trail, access control, encryption at rest/transit
- ✅ **ISO 27001**: Change logging, incident detection, security monitoring

---

## 🎯 Use Cases & Dashboardy

### Use Case 1: System Health (Admin Realm)

**WHO:** Platform admin (CORE_PLATFORM_ADMIN role v admin realmu)  
**WHAT:** Přehled o celkovém zdraví platformy  
**WHERE:** Native Monitoring UI nebo Grafana

#### Prometheus Metrics
```promql
# Backend health
up{job="backend"} == 1

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / 
rate(http_server_requests_seconds_count[5m]) * 100

# P95 latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Database connections
hikaricp_connections_active / hikaricp_connections_max * 100

# Redis memory
redis_memory_used_bytes / redis_memory_max_bytes * 100

# Kafka consumer lag
sum(kafka_consumer_lag) by (topic, consumer_group)

# Pod/container restarts
kube_pod_container_status_restarts_total
```

#### Loki Labels (Must Have)
```logql
{service="backend", level="ERROR"}
{service="postgres", level="WARN"}
{service="redis"}
{service="kafka"}
{service="nginx", level="ERROR"}
```

#### Doporučené zobrazení
- **Native FE**: Health Cards + Line Charts (React + MUI Charts)
- **Grafana** (optional): System Overview dashboard (cross-tenant view)

---

### Use Case 2: Application Health Per Tenant

**WHO:** Tenant admin (TENANT_ADMIN role v konkrétním tenant realm-u)  
**WHAT:** Monitoring aplikace pro svůj tenant  
**WHERE:** Native Monitoring UI (Log Viewer + Metrics Dashboard)

#### Prometheus Metrics (Tenant-scoped)
```promql
# Request rate (pouze můj tenant)
rate(http_server_requests_seconds_count{tenant="tenant-a"}[5m])

# Error rate
rate(http_server_requests_seconds_count{tenant="tenant-a", status=~"5.."}[5m])

# Active users
user_sessions_active{tenant="tenant-a"}

# Business metrics
workflow_execution_total{tenant="tenant-a", status="success"}
dms_operation_total{tenant="tenant-a", operation="upload"}
```

#### Loki Labels (Must Have)
```logql
# Application logs (auto-filtered by BFF)
{tenant="tenant-a", service="backend"}

# User actions
{tenant="tenant-a", level="AUDIT", action=~"user.*"}

# Errors
{tenant="tenant-a", level="ERROR"}
```

#### Doporučené zobrazení
- **Native FE ONLY**: Tenant users nemají přístup do Grafany
- **BFF automaticky injectuje** `{tenant="<realm>"}` do všech queries

---

### Use Case 3: Streaming & Workflow Observability

**WHO:** Tenant admin, workflow developer  
**WHAT:** Monitoring Kafka topics a workflow executions  
**WHERE:** Native Monitoring UI

#### Prometheus Metrics
```promql
# Kafka consumer lag (per tenant)
kafka_consumer_lag{tenant="tenant-a", topic=~"workflow.*"}

# DLQ message count
sum(kafka_topic_size{topic=~".*dlq"}) by (topic)

# Workflow execution states
workflow_execution_status{tenant="tenant-a", status="running"}
workflow_execution_status{tenant="tenant-a", status="failed"}
workflow_execution_status{tenant="tenant-a", status="completed"}

# SLA breaches
workflow_execution_duration_seconds{tenant="tenant-a"} > 300  # >5min

# DMS operations
dms_operation_total{tenant="tenant-a", operation="ocr", status="success"}
dms_operation_total{tenant="tenant-a", operation="upload", status="failed"}
```

#### Loki Labels (Must Have)
```logql
# Workflow execution logs
{tenant="tenant-a", service="workflow-engine", workflow_id=~".+"}

# Kafka errors
{tenant="tenant-a", service="kafka-consumer", level="ERROR"}

# DMS operation logs
{tenant="tenant-a", service="dms", operation=~"upload|download|ocr"}
```

#### Doporučené zobrazení
- **Native FE**: Workflow Dashboard s real-time status cards
- **Grafana** (SRE only): Kafka Lag dashboard (cross-tenant troubleshooting)

---

### Use Case 4: Security & Audit

**WHO:** Security officer, compliance auditor  
**WHAT:** Přehled security events a audit trail  
**WHERE:** Native Monitoring UI (Log Viewer) + Grafana (admin realm)

#### Prometheus Metrics
```promql
# Failed login attempts (all tenants)
sum(keycloak_login_attempts{status="failed"}) by (tenant, realm)

# Role changes
sum(increase(keycloak_role_changes_total[1h])) by (tenant)

# Administrative actions
sum(increase(admin_action_total{action=~"tenant_create|connector_create"}[1h]))
```

#### Loki Labels (Must Have)
```logql
# Failed logins
{service="keycloak", level="WARN", event="LOGIN_ERROR"}

# Role changes
{service="backend", level="AUDIT", action="role_change"}

# Tenant management
{service="backend", level="AUDIT", action=~"tenant_.*"}

# User tracking (GDPR-compliant)
{tenant="tenant-a", level="AUDIT", user_id=~".+"}

# Trace ID correlation
{tenant="tenant-a", trace_id="abc123"}
```

#### Doporučené zobrazení
- **Native FE**: Audit Log Viewer (filtered by tenant)
- **Grafana** (admin realm): Security Audit dashboard (cross-tenant alerts)

---

## 🧪 Testing & Quality Gates (EPIC-002 Integration)

### Smoke Tests (Pre-Deploy)

**Cíl:** Ověřit že Prometheus a Loki běží a vrací data.

```typescript
// e2e/specs/monitoring/smoke.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Monitoring Stack Smoke Tests', () => {
  
  test('Prometheus is healthy', async ({ request }) => {
    const response = await request.get('http://prometheus:9090/-/healthy');
    expect(response.status()).toBe(200);
  });
  
  test('Prometheus has targets up', async ({ request }) => {
    const response = await request.get('http://prometheus:9090/api/v1/targets');
    const data = await response.json();
    
    const activeTargets = data.data.activeTargets;
    expect(activeTargets.length).toBeGreaterThan(0);
    
    const upTargets = activeTargets.filter(t => t.health === 'up');
    expect(upTargets.length).toBeGreaterThan(0);
  });
  
  test('Loki is healthy', async ({ request }) => {
    const response = await request.get('http://loki:3100/ready');
    expect(response.status()).toBe(200);
  });
  
  test('Loki returns logs', async ({ request }) => {
    const query = encodeURIComponent('{service="backend"}');
    const response = await request.get(
      `http://loki:3100/loki/api/v1/query_range?query=${query}&limit=10`
    );
    const data = await response.json();
    
    expect(data.status).toBe('success');
    expect(data.data.result.length).toBeGreaterThan(0);
  });
  
  test('BFF monitoring endpoints exist', async ({ request }) => {
    // Requires valid JWT
    const response = await request.get('/api/monitoring/health');
    expect(response.status()).toBe(200);
    
    const health = await response.json();
    expect(health.prometheus).toBe('UP');
    expect(health.loki).toBe('UP');
  });
});
```

### E2E Tests (Post-Deploy)

**Cíl:** Ověřit tenant isolation a funkčnost native UI.

```typescript
// e2e/specs/monitoring/log-viewer.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Log Viewer - Tenant Isolation', () => {
  
  test('User A sees only own logs', async ({ page }) => {
    // Login as tenant-a user
    await page.goto('/login');
    await page.fill('[name="username"]', 'admin@tenant-a');
    await page.fill('[name="password"]', 'Test.1234');
    await page.click('button[type="submit"]');
    
    // Navigate to Log Viewer
    await page.goto('/monitoring/logs');
    
    // Execute query
    await page.fill('[data-testid="logql-input"]', '{service="backend"}');
    await page.click('[data-testid="execute-query"]');
    
    // Wait for results
    await page.waitForSelector('[data-testid="log-table"]');
    
    // Verify ALL logs have tenant="tenant-a" label
    const logs = await page.$$eval(
      '[data-testid="log-entry"]',
      entries => entries.map(e => e.getAttribute('data-tenant'))
    );
    
    expect(logs.every(tenant => tenant === 'tenant-a')).toBe(true);
  });
  
  test('User A cannot see User B logs', async ({ page }) => {
    // Login as tenant-a user
    await loginAsTenantA(page);
    
    // Navigate to Log Viewer
    await page.goto('/monitoring/logs');
    
    // Try to query tenant-b logs (should be blocked by BFF)
    await page.fill('[data-testid="logql-input"]', '{tenant="tenant-b"}');
    await page.click('[data-testid="execute-query"]');
    
    // Should return 0 results (or 403 Forbidden)
    const errorMessage = await page.textContent('[data-testid="error-message"]');
    expect(errorMessage).toContain('Access denied');
  });
  
  test('Metrics Dashboard shows tenant-scoped data', async ({ page }) => {
    await loginAsTenantA(page);
    
    await page.goto('/monitoring/metrics');
    
    // Wait for metrics to load
    await page.waitForSelector('[data-testid="metric-card"]');
    
    // Verify request rate metric
    const requestRate = await page.textContent('[data-testid="metric-request-rate"]');
    expect(requestRate).toMatch(/\d+\.\d+ req\/s/);
    
    // Verify tenant label is present in query
    const queryText = await page.getAttribute('[data-testid="metric-request-rate"]', 'data-query');
    expect(queryText).toContain('tenant="tenant-a"');
  });
});
```

### Grafana Smoke Test (Optional, Manual)

**Cíl:** Ověřit že Grafana je dostupná pro admin realm.

```bash
# Manual check
curl -k https://admin.core-platform.local/grafana/login
# Očekáváno: 200 OK + Keycloak redirect

# Nebo jednoduchý Playwright test:
test('Grafana is accessible for admin', async ({ page }) => {
  await page.goto('https://admin.core-platform.local/grafana');
  
  // Should redirect to Keycloak login
  await expect(page).toHaveURL(/keycloak.*\/realms\/admin\/protocol\/openid-connect\/auth/);
});
```

**POZNÁMKA:** Komplexní E2E testy pro Grafana SSO NEJSOU vyžadovány (admin-only tool, manuální verifikace stačí).

---

## 🚫 Out of Scope (CO NEDĚLÁME)

### 1. Per-Tenant Grafana Organizations

❌ **ZAHODIT:**
- Automatické vytváření Grafana org per tenant
- `GrafanaMonitoringProvisioningService.provisionMonitoringForTenant()`
- Multi-tenant SSO bridge přes BFF

**DŮVOD:**
- Komplexní správa (create/delete tenant → sync Grafana orgs)
- Licence náklady (Grafana Enterprise pro multi-org)
- Tenant users mají **Native Monitoring UI** - nepotřebují Grafana

**MÍSTO TOHO:**
- Grafana pouze pro **admin realm** (SRE/ops týmy)
- Jediná Grafana instance, bez per-tenant provisioningu

---

### 2. Grafana Scenes Embedded v Tenant UI

❌ **ZAHODIT:**
- Embedování Grafana dashboardů do našich tenant stránek
- `<iframe src="/grafana/d/<dashboard-uid>?orgId=<tenant-org-id>" />`
- Grafana Scenes React komponenty

**DŮVOD:**
- Security riziko (CSP bypass, clickjacking)
- Závislost na externím nástroji (vendor lock-in)
- Performance (loading Grafana iframe je pomalé)

**MÍSTO TOHO:**
- **Native React komponenty** pro metrics/logs
- MUI Charts, Recharts, nebo vlastní D3.js grafy
- Data přímo z BFF API (Prometheus/Loki queries)

---

### 3. Složitý JWT SSO Bridge pro Multi-Org

❌ **ZAHODIT:**
- JWT token exchange mezi našim backendem a Grafanou
- Custom auth proxy s org-id routing
- Per-tenant service accounts v Grafaně

**DŮVOD:**
- Zbytečná komplexita (Grafana není pro běžné uživatele)
- Security surface area (token leaks, impersonation)
- Maintenance overhead (sync users/orgs/permissions)

**MÍSTO TOHO:**
- Grafana **POUZE OIDC/SSO proti admin realm**
- Žádný JWT bridge, žádný token exchange
- Admin users login přímo přes Keycloak

---

### 4. Grafana Alerting per Tenant

❌ **NEIMPLEMENTOVAT:**
- Per-tenant alert rules v Grafaně
- Per-tenant notification channels (Slack, email per org)

**DŮVOD:**
- Tenant-specific alerty můžeme řešit v **Prometheus Alertmanager**
- Nebo v **budoucí Native Alerting UI** (custom React)

**MÍSTO TOHO:**
- Grafana alerting pouze pro **platform-wide alerty** (admin realm)
- Tenant alerting přes Prometheus rules + Alertmanager routing

---

## 📚 Related EPICs & Documentation

### EPIC-000: Security & Access Control
- **TLS/HTTPS**: Všechen monitoring provoz pouze přes HTTPS
- **JWT Validation**: BFF kontroluje JWT před každým query
- **Tenant Guard**: Automatická injekce tenant filters
- **PII Redaction**: Citlivá data v logách musí být redakována
- **Audit Logging**: Access k monitoring API musí být auditován
- **Rate Limiting**: Ochrana proti bruteforce (admin endpoints)

### EPIC-002: E2E Infrastructure
- **Smoke Tests**: Pre-deploy validace (Prometheus/Loki health)
- **E2E Tests**: Post-deploy tenant isolation checks
- **Test Data**: Generování test logů a metrik
- **CI/CD Integration**: Automated testing v GitHub Actions

### EPIC-007: Workflow Engine
- **Workflow Metrics**: `workflow_execution_*` metriky
- **Kafka Monitoring**: Consumer lag, DLQ tracking
- **SLA Tracking**: Workflow duration SLOs

### EPIC-012: DMS (Document Management)
- **DMS Metrics**: `dms_operation_*` metriky
- **OCR Monitoring**: Success rate, latency
- **Storage Metrics**: S3 usage per tenant

---

## 📊 Stories Overview (Updated)

| ID | Story | Status | LOC | Components | Value |
|----|-------|--------|-----|------------|-------|
| **MON-001** | Loki Log Aggregation | ✅ DONE | ~1,000 | Loki 3.0 + Promtail | Centralized logs |
| **MON-002** | Prometheus Metrics | ✅ DONE | ~1,500 | Prometheus 2.x | Time-series metrics |
| **MON-003** | ~~Grafana Dashboards~~ | ⚠️ DEPRECATED | ~~2,000~~ | Moved to optional | ~~Visualization~~ |
| **MON-004** | ~~Tenant Auto-Provisioning~~ | ⚠️ REMOVED | ~~800~~ | N/A | ~~Per-tenant orgs~~ |
| **MON-005** | Recording Rules & Alerting | ✅ DONE | ~1,300 | Prometheus rules | Aggregated metrics + alerts |
| **MON-007** | Native Loki UI | ✅ DONE | ~1,400 | BFF API + React | De-Grafana logs |
| **MON-008** | Native Metrics Dashboard | 🔵 TODO | ~2,500 | React + MUI Charts | Integrated UI |
| **MON-009** | Real-Time Widgets | 🔵 TODO | ~1,200 | WebSocket + Live | Live metrics |
| **MON-010** | Optional Grafana (Admin) | 🔵 TODO | ~500 | Nginx proxy + OIDC | SRE tool |
| **TOTAL** | | **4/9** | **~11,400** | **Native-first stack** | **Full observability** |

---

## 🎯 Next Steps (Q1 2025)

### Priority 1: Native Metrics Dashboard (MON-008)
- [ ] Design mockupy (Figma) - Health Cards, Line Charts, SLO tracking
- [ ] Backend BFF API (`/api/monitoring/metrics/{metricName}`)
- [ ] Frontend React komponenty (MUI Charts integration)
- [ ] E2E testy (tenant isolation)

### Priority 2: Real-Time Widgets (MON-009)
- [ ] WebSocket endpoint pro live metrics (`/ws/monitoring/live`)
- [ ] React hooks pro real-time updates
- [ ] Live log streaming
- [ ] Auto-refresh dashboards

### Priority 3: Optional Grafana (MON-010)
- [ ] Nginx reverse proxy konfigurace (`/grafana` path)
- [ ] Keycloak OIDC client pro admin realm
- [ ] Admin-only dashboards (System, Kafka, Security)
- [ ] Dokumentace pro SRE týmy

---

## ✅ Definition of Done

### Pro každou story:
- [x] **Implementace**: Kód napsán, unit testy passed
- [x] **E2E testy**: Playwright specs pro klíčové flows
- [x] **Dokumentace**: README updated, API docs
- [x] **Security review**: EPIC-000 compliance (JWT, TLS, tenant isolation)
- [ ] **Performance**: Query latency < 1s (P95)
- [ ] **Accessibility**: WCAG 2.1 AA (pro Native UI)

### Pro celý EPIC:
- [x] **Loki stack**: Centralized logging s multi-tenant labels
- [x] **Prometheus stack**: Metrics collection s tenant filters
- [x] **Native Loki UI**: Log viewer v React (de-Grafana)
- [ ] **Native Metrics UI**: Business dashboards v React
- [ ] **Optional Grafana**: Admin-only tool za Nginx proxy
- [ ] **E2E coverage**: ≥80% critical paths
- [ ] **Load testing**: 1000 concurrent users, < 2s response time

---

**Last Updated:** 9. listopadu 2025  
**Owner:** Platform Team + DevOps  
**Review Cycle:** Bi-weekly (sprint reviews)

---

## 📊 Stories Implementation Details (Preserve from Original)

### MON-001: Loki Log Aggregation

**Status:** ✅ **DONE** (Září 2024)  
**LOC:** ~1,000

#### Loki Stack Configuration

```yaml
# docker/loki/loki-config.yml
auth_enabled: false

server:
  http_listen_port: 3100

common:
  path_prefix: /loki
  storage:
    filesystem:
      chunks_directory: /loki/chunks
      rules_directory: /loki/rules
  replication_factor: 1
  ring:
    kvstore:
      store: inmemory

schema_config:
  configs:
    - from: 2024-01-01
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h

storage_config:
  boltdb_shipper:
    active_index_directory: /loki/boltdb-shipper-active
    cache_location: /loki/boltdb-shipper-cache
  filesystem:
    directory: /loki/chunks

limits_config:
  retention_period: 2160h  # 90 days
  ingestion_rate_mb: 100
  ingestion_burst_size_mb: 200
```

#### Promtail Labels
```yaml
# promtail/promtail-config.yml
scrape_configs:
  - job_name: docker
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
    relabel_configs:
      - source_labels: ['__meta_docker_container_label_com_docker_compose_service']
        target_label: 'service'
      - source_labels: ['__meta_docker_container_label_tenant']
        target_label: 'tenant'
      - source_labels: ['__meta_docker_container_name']
        regex: '/(.*)'
        target_label: 'container'
```

#### LogQL Queries
```logql
# Všechny ERROR logy za poslední hodinu
{service="backend"} |= "ERROR" | json

# Logy konkrétního tenantu
{tenant="tenant-a"} | json

# Slow query detection (>1s)
{service="backend"} | json | duration > 1000ms

# Audit logs s user tracking
{service="backend", level="AUDIT"} | json | line_format "{{.user}}: {{.action}}"
```

---

### MON-002: Prometheus Metrics Collection

**Status:** ✅ **DONE** (Září 2024)  
**LOC:** ~1,500

#### Metric Categories

**1. JVM Metrics (Micrometer):**
```yaml
# backend/src/main/resources/application.yml
management:
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:dev}
```

Metrics:
- `jvm_memory_used_bytes{area="heap"}` - Heap memory
- `jvm_gc_pause_seconds` - GC duration
- `jvm_threads_live` - Active threads
- `process_cpu_usage` - CPU utilization

**2. HTTP Metrics:**
```promql
http_server_requests_seconds_count{method="GET", uri="/api/tenants", status="200"}
http_server_requests_seconds_sum{method="POST", uri="/api/users", status="201"}
```

**3. Database Metrics (HikariCP):**
```promql
hikaricp_connections_active{pool="core-db"}
hikaricp_connections_pending{pool="core-db"}
hikaricp_connections_timeout_total{pool="core-db"}
```

**4. Custom Business Metrics:**
```java
// backend/src/main/java/cz/muriel/core/metrics/BusinessMetrics.java
@Component
public class BusinessMetrics {
  private final MeterRegistry registry;
  
  @PostConstruct
  public void registerMetrics() {
    Gauge.builder("tenants.total", tenantService, TenantService::count)
      .description("Total number of tenants")
      .register(registry);
    
    Gauge.builder("users.active.count", userService::countActive)
      .description("Active users in last 30 days")
      .register(registry);
  }
}
```

#### Prometheus Configuration
```yaml
# prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'backend'
    static_configs:
      - targets: ['backend:8080']
    metrics_path: '/actuator/prometheus'
  
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
  
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
  
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']
```

---

### MON-005: Recording Rules & Alerting

**Status:** ✅ **DONE** (Září 2024)  
**LOC:** ~1,300

#### Recording Rules
```yaml
# prometheus/rules/recording_rules.yml
groups:
  - name: http_aggregations
    interval: 30s
    rules:
      - record: job:http_requests_total:rate5m
        expr: sum(rate(http_server_requests_seconds_count[5m])) by (job)
      
      - record: job:http_request_duration_seconds:p95
        expr: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (job, le))
      
      - record: job:http_errors:rate5m
        expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)
  
  - name: jvm_aggregations
    interval: 60s
    rules:
      - record: job:jvm_memory_used_ratio:heap
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

#### Alert Rules
```yaml
# prometheus/alerts/alerts.yml
groups:
  - name: slo_alerts
    interval: 30s
    rules:
      - alert: HighErrorRate
        expr: job:http_errors:rate5m / job:http_requests_total:rate5m > 0.01
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High HTTP error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }}"
      
      - alert: HighLatency
        expr: job:http_request_duration_seconds:p95 > 1.0
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High API latency"
          description: "P95 latency is {{ $value }}s"
  
  - name: resource_alerts
    interval: 60s
    rules:
      - alert: HighMemoryUsage
        expr: job:jvm_memory_used_ratio:heap > 0.90
        for: 15m
        labels:
          severity: warning
        annotations:
          summary: "High JVM heap usage"
          description: "Heap usage is {{ $value | humanizePercentage }}"
```

#### Alertmanager Configuration
```yaml
# alertmanager/alertmanager.yml
global:
  resolve_timeout: 5m
  slack_api_url: ${SLACK_WEBHOOK_URL}

route:
  receiver: 'default'
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 5m
  repeat_interval: 4h
  routes:
    - match:
        severity: critical
      receiver: 'pagerduty'
    - match:
        severity: warning
      receiver: 'slack'

receivers:
  - name: 'default'
    slack_configs:
      - channel: '#alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
  
  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: ${PAGERDUTY_SERVICE_KEY}
  
  - name: 'slack'
    slack_configs:
      - channel: '#ops-alerts'
```

---

### MON-006: Alert Routing & Notifications

**Status:** ✅ **DONE** (Září 2024)  
**LOC:** ~700

#### Alert Categories

**1. SLO Alerts:**
- P95 latency > 500ms
- Error rate > 1%
- Availability < 99.9%

**2. Resource Alerts:**
- CPU > 80%
- Memory > 90%
- Disk > 85%

**3. Business Alerts:**
- Failed workflow > 10/hour
- Kafka consumer lag > 1000
- Database connection pool exhausted

**4. Security Alerts:**
- Failed login attempts > 10/minute
- Unauthorized access attempts
- SSL certificate expiration (< 30 days)

---

### MON-007: Native Loki UI (De-Grafana Implementation)

**Status:** ✅ **DONE** (Říjen 2024)  
**LOC:** ~1,400  
**Dokumentace:** `EPIC_COMPLETE_LOKI_UI.md`

#### Backend BFF API

```java
// backend/src/main/java/cz/muriel/core/monitoring/MonitoringController.java
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
  
  private final LokiClient lokiClient;
  
  @GetMapping("/logs")
  @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CORE_PLATFORM_ADMIN')")
  public ResponseEntity<LogQueryResponse> queryLogs(
    @RequestParam String query,
    @RequestParam(required = false) Long start,
    @RequestParam(required = false) Long end,
    @RequestParam(defaultValue = "100") int limit,
    @AuthenticationPrincipal Jwt jwt
  ) {
    // Extract tenant from JWT
    String realm = extractRealm(jwt);
    
    // Inject tenant filter
    String scopedQuery = injectTenantFilter(query, realm);
    
    // Execute Loki query
    LogQueryResponse response = lokiClient.queryRange(
      scopedQuery,
      start != null ? start : System.currentTimeMillis() - 3600000,
      end != null ? end : System.currentTimeMillis(),
      limit
    );
    
    // Audit log access
    auditService.logAccess(
      AuditEvent.builder()
        .user(jwt.getSubject())
        .action("QUERY_LOGS")
        .query(scopedQuery)
        .timestamp(Instant.now())
        .build()
    );
    
    return ResponseEntity.ok(response);
  }
  
  @GetMapping("/logs/labels")
  public ResponseEntity<List<String>> getLabels(@AuthenticationPrincipal Jwt jwt) {
    String realm = extractRealm(jwt);
    List<String> labels = lokiClient.getLabels(realm);
    return ResponseEntity.ok(labels);
  }
  
  @GetMapping("/logs/label/{name}/values")
  public ResponseEntity<List<String>> getLabelValues(
    @PathVariable String name,
    @AuthenticationPrincipal Jwt jwt
  ) {
    String realm = extractRealm(jwt);
    List<String> values = lokiClient.getLabelValues(name, realm);
    return ResponseEntity.ok(values);
  }
  
  private String injectTenantFilter(String query, String realm) {
    if (query.contains("{")) {
      return query.replaceFirst("\\{", "{realm=\"" + realm + "\",");
    } else {
      return "{realm=\"" + realm + "\"} " + query;
    }
  }
  
  private String extractRealm(Jwt jwt) {
    return jwt.getClaimAsString("iss").split("/realms/")[1];
  }
}
```

#### Loki Client Implementation

```java
// backend/src/main/java/cz/muriel/core/monitoring/LokiClient.java
@Service
public class LokiClient {
  
  @Value("${loki.url:http://loki:3100}")
  private String lokiUrl;
  
  private final WebClient webClient;
  
  public LokiClient(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder
      .baseUrl(lokiUrl)
      .build();
  }
  
  public LogQueryResponse queryRange(String query, long start, long end, int limit) {
    return webClient.get()
      .uri(uriBuilder -> uriBuilder
        .path("/loki/api/v1/query_range")
        .queryParam("query", query)
        .queryParam("start", start * 1_000_000) // Convert to nanoseconds
        .queryParam("end", end * 1_000_000)
        .queryParam("limit", limit)
        .build())
      .retrieve()
      .bodyToMono(LogQueryResponse.class)
      .block();
  }
  
  public List<String> getLabels(String realm) {
    Map<String, Object> response = webClient.get()
      .uri("/loki/api/v1/labels")
      .retrieve()
      .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
      .block();
    
    return (List<String>) response.get("data");
  }
  
  public List<String> getLabelValues(String label, String realm) {
    Map<String, Object> response = webClient.get()
      .uri("/loki/api/v1/label/" + label + "/values")
      .retrieve()
      .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
      .block();
    
    return (List<String>) response.get("data");
  }
}
```

#### Frontend React Components

```typescript
// frontend/src/pages/Monitoring/LogViewer.tsx
import React, { useState, useEffect } from 'react';
import { Box, Typography, TextField, Button, Select, MenuItem } from '@mui/material';
import { LogTable } from './LogTable';
import { LogQLQueryBuilder } from './LogQLQueryBuilder';
import { api } from '../../services/api';

export const LogViewer: React.FC = () => {
  const [query, setQuery] = useState<string>('{service="backend"}');
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [timeRange, setTimeRange] = useState<'5m' | '1h' | '24h' | 'custom'>('1h');
  
  const fetchLogs = async () => {
    setLoading(true);
    try {
      const now = Date.now();
      const start = timeRange === '5m' ? now - 300000
                 : timeRange === '1h' ? now - 3600000
                 : now - 86400000;
      
      const response = await api.get('/api/monitoring/logs', {
        params: { query, start, end: now, limit: 100 }
      });
      
      setLogs(response.data.data.result);
    } catch (error) {
      console.error('Failed to fetch logs:', error);
    } finally {
      setLoading(false);
    }
  };
  
  useEffect(() => {
    fetchLogs();
    const interval = setInterval(fetchLogs, 10000); // Auto-refresh každých 10s
    return () => clearInterval(interval);
  }, [query, timeRange]);
  
  return (
    <Box>
      <Typography variant="h4">Log Viewer</Typography>
      
      {/* Query Builder */}
      <LogQLQueryBuilder
        value={query}
        onChange={setQuery}
        onExecute={fetchLogs}
      />
      
      {/* Time Range Selector */}
      <Select value={timeRange} onChange={(e) => setTimeRange(e.target.value as any)}>
        <MenuItem value="5m">Last 5 minutes</MenuItem>
        <MenuItem value="1h">Last hour</MenuItem>
        <MenuItem value="24h">Last 24 hours</MenuItem>
      </Select>
      
      {/* Log Table */}
      <LogTable
        logs={logs}
        loading={loading}
        onRefresh={fetchLogs}
      />
    </Box>
  );
};
```

```typescript
// frontend/src/pages/Monitoring/LogTable.tsx
import React from 'react';
import { 
  Table, TableBody, TableCell, TableHead, TableRow, 
  Chip, Box, CircularProgress 
} from '@mui/material';
import { format } from 'date-fns';

interface LogEntry {
  timestamp: number;
  line: string;
  labels: Record<string, string>;
}

interface LogTableProps {
  logs: LogEntry[];
  loading: boolean;
  onRefresh: () => void;
}

export const LogTable: React.FC<LogTableProps> = ({ logs, loading, onRefresh }) => {
  const getLevelColor = (level: string) => {
    switch (level) {
      case 'ERROR': return 'error';
      case 'WARN': return 'warning';
      case 'INFO': return 'info';
      default: return 'default';
    }
  };
  
  if (loading) return <CircularProgress />;
  
  return (
    <Table data-testid="log-table">
      <TableHead>
        <TableRow>
          <TableCell>Timestamp</TableCell>
          <TableCell>Level</TableCell>
          <TableCell>Service</TableCell>
          <TableCell>Message</TableCell>
          <TableCell>Tenant</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {logs.map((log, idx) => (
          <TableRow 
            key={idx} 
            data-testid="log-entry"
            data-tenant={log.labels.tenant}
          >
            <TableCell>
              {format(new Date(log.timestamp / 1000000), 'yyyy-MM-dd HH:mm:ss')}
            </TableCell>
            <TableCell>
              <Chip 
                label={log.labels.level || 'INFO'} 
                color={getLevelColor(log.labels.level)}
                size="small"
              />
            </TableCell>
            <TableCell>{log.labels.service}</TableCell>
            <TableCell>{log.line}</TableCell>
            <TableCell>{log.labels.tenant || 'N/A'}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
};
```

#### Features Implemented
- ✅ Real-time log streaming (auto-refresh každých 10s)
- ✅ LogQL query builder (GUI + raw mode)
- ✅ Tenant-scoped filtering (automatic injection)
- ✅ Time range selection (5m, 1h, 24h, custom)
- ✅ Log level filtering (ERROR, WARN, INFO, DEBUG)
- ✅ Full-text search (regex support)
- ✅ Export (JSON, CSV)

#### Tenant Isolation Example
```bash
# User v realmu "tenant-a" zadá query:
{service="backend"} | json | level="ERROR"

# Backend BFF automaticky injectuje:
{realm="tenant-a",service="backend"} | json | level="ERROR"

# Loki vrátí POUZE logy z realm="tenant-a"
# → Zero cross-tenant data leak ✅
```

---

