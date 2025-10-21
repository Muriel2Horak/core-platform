# 🔍 Komplexní analýza logování a monitoringu
**Datum**: 21. října 2025  
**Účel**: Audit logování, identifikace problémů s `tenant=unknown`, revize dashboardů a návrh separace Security/Audit stránek

---

## 📋 Executive Summary

### Klíčové nálezy:
1. ✅ **Backend logování funguje správně** - tenant ID se nastavuje z JWT přes `TenantFilter`
2. ⚠️ **Frontend logy DO logují do Loki** přes `/api/frontend-logs` → Backend → Loki
3. 🔴 **KRITICKÝ PROBLÉM**: Tenant ID v frontend logách je `"unknown"` kvůli chybějící inicializaci
4. 🔴 **KRITICKÝ PROBLÉM**: Mnoho requestů nemá tenant kontext (public endpointy, health checks)
5. ⚠️ **Architektura**: Security a Audit dashboardy by měly být separátní stránky s vlastním ACL

---

## 🏗️ Architektura logování

### 1. Backend Logování (✅ Funguje správně)

#### Technický stack:
```
JWT Token → TenantFilter → MDC.put("tenant", tenantKey) → Logback → Loki
```

**Soubory:**
- `TenantFilter.java` - Extrahuje tenant z JWT a nastavuje MDC
- `logback-spring.xml` - Konfigurace Loki appenderů s MDC tags
- `application.properties` - Nastavení log levels

#### Flow:
```java
// TenantFilter.java (Order=2, After Security)
1. SecurityContextHolder.getContext().getAuthentication()
2. tenantResolver.resolveTenantKey() // Z JWT claim
3. TenantContext.setTenantKey(tenantKey)
4. MDC.put("tenant", tenantKey) // ⭐ Tohle funguje!
5. filterChain.doFilter(request, response)
6. finally: TenantContext.clear() + MDC.remove("tenant")
```

**Loki Labels (logback-spring.xml):**
```xml
<!-- Standard backend logs -->
<pattern>
  container=core-backend,
  service=backend,
  source=backend,
  level=%level,
  environment=${ENVIRONMENT:-development},
  tenant=${mdc:tenant:-unknown}  <!-- ⭐ Bere z MDC -->
</pattern>

<!-- Monitoring BFF logs -->
<pattern>
  container=core-backend,
  service=monitoring-bff,
  source=backend,
  level=%level,
  environment=${ENVIRONMENT:-development},
  tenant=${mdc:tenant:-unknown},
  orgId=${mdc:orgId:-unknown}
</pattern>

<!-- Audit logs -->
<pattern>
  container=core-backend,
  service=backend,
  source=backend,
  level=AUDIT,
  event_type=audit,
  environment=${ENVIRONMENT:-development},
  tenant=${mdc:tenant:-unknown}
</pattern>
```

#### Log Levels (application.properties):
```properties
# Root level
logging.level.root=INFO

# Spring Security (DEBUG pro troubleshooting)
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.security.oauth2=DEBUG
logging.level.org.springframework.security.oauth2.server.resource=DEBUG
logging.level.org.springframework.security.web.access.intercept=DEBUG

# Core Platform (DEBUG)
logging.level.cz.muriel.core=DEBUG
logging.level.cz.muriel.core.controller.UserProfileController=DEBUG
logging.level.cz.muriel.core.tenant=DEBUG
logging.level.cz.muriel.core.auth.config.DynamicJwtDecoder=DEBUG

# Flyway (DEBUG)
logging.level.org.flywaydb=DEBUG

# Hibernate (INFO - production ready)
logging.level.org.hibernate.stat=INFO
logging.level.org.hibernate.SQL=INFO
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=INFO

# Environment variables pro override:
# LOG_LEVEL - root level (default: INFO)
# AUDIT_LOG_LEVEL - audit logger (default: INFO)
# LOGGING_LEVEL_CZ_MURIEL_CORE - core platform (default: DEBUG)
```

**⚠️ POZOR**: DEBUG level na production je **nebezpečný**:
- Loguje se všechno včetně citlivých dat
- Obrovský objem logů → náklady na Loki storage
- Performance overhead
- **DOPORUČENÍ**: Přepnout na INFO/WARN pro production

---

### 2. Frontend Logování (⚠️ Částečně funguje)

#### Technický stack:
```
logger.js → /api/frontend-logs → FrontendLogsController.java → Loki HTTP API
```

**Soubory:**
- `frontend/src/services/logger.js` - Singleton logger s queue
- `backend/.../FrontendLogsController.java` - Proxy endpoint do Loki
- Frontend logy mají label `container=core-frontend`

#### Flow:
```javascript
// logger.js
1. logger.info("message", { context }) // Console + queue
2. Queue batch: 10 items nebo 5s interval
3. POST /api/frontend-logs + Bearer token
4. Backend controller → Loki HTTP API
5. Loki label: container=core-frontend, tenant=${tenant}
```

**Frontend Logger API:**
```javascript
// Singleton instance
import logger from './services/logger.js';

// Set context (po přihlášení)
logger.setTenantContext(tenant, username);
logger.setAuthenticated(true);

// Log methods
logger.debug("message", { component: "xyz" });
logger.info("message", { operation: "load" });
logger.warn("message", { category: "validation" });
logger.error("message", { stack: error.stack });

// Speciální metody
logger.auth("Login successful", { method: "oauth2" });
logger.pageView("/dashboard", { referrer: "/" });
logger.userAction("button-click", { button: "save" });

// Manual flush (critical logs)
await logger.flushImmediate();
```

---

## 🔴 KRITICKÝ PROBLÉM: Frontend tenant=unknown

### Root Cause Analysis:

**Problém**: Frontend logger má hardcoded `tenant: 'unknown'` při inicializaci:
```javascript
// logger.js constructor
this.tenant = 'unknown';  // ⚠️ PROBLÉM
this.username = 'anonymous';
```

**Kdy se nastavuje správně:**
```javascript
logger.setTenantContext(tenant, username);
```

**KDE SE VOLÁ?** 🔍
```bash
# Hledání v kódu
grep -r "setTenantContext" frontend/src/
```

**NALEZENO:**
- ❌ **NIKDE** - `setTenantContext()` se nevolá!
- Frontend logger se nikdy neinicializuje s tenant contextem
- Všechny frontend logy mají `tenant=unknown`

### Důsledky:
1. ❌ Loki query `{tenant="xyz"}` nevrací frontend logy
2. ❌ Dashboardy nevidí frontend chyby pro konkrétní tenant
3. ❌ Audit trail nekompletní (chybí frontend akce)
4. ❌ Troubleshooting složitý (nelze filtrovat tenant)

---

## 🔴 DRUHÝ PROBLÉM: Public endpointy bez tenant kontextu

### Zdroje `tenant=unknown` v backend logách:

#### 1. **Unauthenticated endpointy**
```java
// TenantFilter.java - pouze pro authenticated
if (auth != null && auth.isAuthenticated() && !isAnonymous(auth)) {
  // Set tenant context
} else {
  // ⚠️ Tenant zůstane "unknown"
}
```

**Které endpointy:**
- `/actuator/health`
- `/actuator/prometheus`
- `/api/public/**`
- Static resources
- Error pages (401, 403, 404, 500)

#### 2. **Health checks & monitoring**
```java
// MonitoringAuditFilter.java
String tenantId = "unknown"; // ⚠️ Default pro monitoring BFF

// Pokud není JWT v requestu:
if (jwtToken == null || jwtToken.isEmpty()) {
  tenantId = "unknown"; // Health checks, Prometheus scrape
}
```

#### 3. **Fallback hodnoty**
```java
// Různé controllery
String tenant = jwt.getClaim("tenant");
return tenant != null ? tenant : "unknown"; // ⚠️ Fallback

// AiMetricsCollector.java
.tags(Tags.of("tenant", tenantId != null ? tenantId : "unknown"))
```

### Řešení:
1. ✅ Health checks: Použít speciální label `tenant=system`
2. ✅ Public API: Label `tenant=public`
3. ✅ Frontend init: Zavolat `setTenantContext()` po login
4. ⚠️ Error handling: Zkontrolovat zda tenant context existuje před logem

---

## 📊 Dashboard analýza

### Aktuální stav (po konsolidaci):
**AxiomMonitoringPage** - 8 tabů:
1. System Overview (Axiom SLO)
2. Advanced (Runtime/DB/Redis)
3. Streaming (Kafka lag)
4. Security & Compliance
5. Audit & Governance
6. Performance (RED)
7. Platform Health (SLI/SLO)
8. Logs (Loki)

### 🚨 Problémy s daty:

#### A) Prázdné dashboardy
**Důvody:**
1. **Tenant filter nefunguje** - Grafana org není správně namapována na tenant
2. **Metriky chybí** - Backend neexportuje některé metriky
3. **LogQL query špatně** - Filtr na `tenant=unknown` nic nenajde

**Příklad špatného query:**
```logql
# ❌ ŠPATNĚ - všechny logy mají tenant=unknown
{container="core-frontend", tenant="tenant1"}

# ✅ SPRÁVNĚ - filtruj až po načtení
{container="core-frontend"} | json | tenant="tenant1"
```

#### B) Tenant=unknown v Loki
**Fixnutí:**
1. Frontend: Zavolat `logger.setTenantContext()` po login
2. Backend: Přidat `tenant=system` pro health checks
3. Dashboards: Upravit LogQL queries pro správné filtrování

#### C) Chybějící metriky
**Zkontrolovat:**
```bash
# Prometheus metrics endpoint
curl http://localhost:8080/actuator/prometheus | grep -i "tenant\|http\|kafka"

# Dostupné metriky:
- http_server_requests_seconds_* (✅)
- kafka_consumer_lag_* (❓)
- jvm_memory_* (✅)
- hikaricp_connections_* (✅)
```

---

## 🔐 Doporučení: Separace Security & Audit

### Současný stav:
- Security tab je v AxiomMonitoringPage (tab 4)
- Audit tab je v AxiomMonitoringPage (tab 5)
- ACL: `requiredRoles: ['CORE_ROLE_ADMIN']`

### 🎯 Navrhované změny:

#### 1. Vytvořit separátní stránky:

```
/core-admin/monitoring        → AxiomMonitoringPage (6 tabů)
  ├─ System Overview
  ├─ Advanced (Runtime/DB/Redis)
  ├─ Streaming (Kafka)
  ├─ Performance (RED)
  ├─ Platform Health (SLI/SLO)
  └─ Logs (Loki)

/core-admin/security          → SecurityMonitoringPage (dedikovaná)
  ├─ Security Overview
  ├─ Failed Logins
  ├─ 401/403/429 Anomalies
  ├─ JWT Errors
  ├─ TLS Certificates
  └─ Threat Detection

/core-admin/audit             → AuditLogPage (dedikovaná)
  ├─ Audit Overview
  ├─ CRUD Events
  ├─ Workflow Transitions
  ├─ Bulk Operations
  ├─ Grafana Access
  └─ Frontend Events
```

#### 2. ACL / Role Based Access:

```typescript
// SidebarNav.tsx
{
  id: 'axiom-monitoring',
  label: 'System Monitoring',
  href: '/core-admin/monitoring',
  requiredRoles: ['CORE_ROLE_ADMIN'], // Tech admins
},
{
  id: 'security-monitoring',
  label: 'Security Dashboard',
  href: '/core-admin/security',
  requiredRoles: ['CORE_ROLE_SECURITY', 'CORE_ROLE_ADMIN'], // Security team
  badge: 'SEC',
  badgeColor: 'error',
},
{
  id: 'audit-log',
  label: 'Audit Log',
  href: '/core-admin/audit',
  requiredRoles: ['CORE_ROLE_AUDITOR', 'CORE_ROLE_ADMIN'], // Auditors
  badge: 'AUDIT',
  badgeColor: 'warning',
},
```

#### 3. Důvody pro separaci:

**Bezpečnost:**
- 🔐 Security dashboard má citlivé info (attack patterns, IPs)
- 📋 Audit log má compliance požadavky (GDPR, ISO 27001)
- 👥 Různé role: SysAdmin ≠ SecurityAnalyst ≠ Auditor

**UX:**
- 🎯 Lepší focus - každý dashboard má jasný účel
- 🚀 Rychlejší načítání - menší stránky
- 📱 Mobile friendly - méně tabů

**Compliance:**
- ✅ Auditovatelnost - kdo se díval do logů?
- ✅ Separace zodpovědností (SoD)
- ✅ RBAC - granular permissions

---

## 🔧 Akční plán

### PHASE 1: Fix Frontend Tenant Context (🔴 KRITICKÉ)

**1.1 Inicializace loggeru po přihlášení:**
```typescript
// App.jsx - po Keycloak init
useEffect(() => {
  if (authenticated && keycloak.tokenParsed) {
    const tenant = keycloak.tokenParsed.tenant || 'unknown';
    const username = keycloak.tokenParsed.preferred_username || 'anonymous';
    
    logger.setTenantContext(tenant, username);
    logger.auth('User authenticated', { 
      method: 'keycloak',
      tenant,
      username 
    });
  }
}, [authenticated, keycloak]);
```

**1.2 Testing:**
```bash
# 1. Login to app
# 2. Open console
# 3. Check logs:
logger.info("test", { component: "test" });

# 4. Check Loki:
curl -G http://localhost:3100/loki/api/v1/query \
  --data-urlencode 'query={container="core-frontend"}' \
  | jq '.data.result[0].stream.tenant'
# Expected: actual tenant name (not "unknown")
```

### PHASE 2: Fix Backend Tenant Labels (⚠️ VYSOKÁ)

**2.1 Health checks → `tenant=system`:**
```java
// TenantFilter.java
String requestUri = request.getRequestURI();
if (requestUri.startsWith("/actuator/")) {
  MDC.put("tenant", "system");
  TenantContext.setTenantKey("system");
}
```

**2.2 Public API → `tenant=public`:**
```java
if (requestUri.startsWith("/api/public/")) {
  MDC.put("tenant", "public");
  TenantContext.setTenantKey("public");
}
```

**2.3 MonitoringAuditFilter → fix:**
```java
// MonitoringAuditFilter.java
if (jwtToken == null || jwtToken.isEmpty()) {
  tenantId = requestUri.contains("/actuator/") ? "system" : "public";
}
```

### PHASE 3: Dashboard Query Fixes (⚠️ STŘEDNÍ)

**3.1 Loki queries - oprava:**
```logql
# ❌ BEFORE
{container="core-frontend", tenant="tenant1"}

# ✅ AFTER
{container="core-frontend"} | json | line_format "{{.tenant}}" | tenant="tenant1"

# OR better:
{container="core-frontend"} | json | tenant =~ ".+"  # Exclude "unknown"
```

**3.2 Prometheus queries - přidat tenant tag:**
```promql
# ❌ BEFORE
rate(http_server_requests_seconds_count[5m])

# ✅ AFTER
rate(http_server_requests_seconds_count{tenant!="system",tenant!="public"}[5m])
```

### PHASE 4: Separace Security & Audit (🔵 NÍZKÁ PRIORITA)

**4.1 Vytvořit nové stránky:**
```
frontend/src/pages/Admin/
  ├─ SecurityMonitoringPage.tsx    (NEW)
  ├─ AuditLogPage.tsx               (NEW)
  └─ AxiomMonitoringPage.tsx        (MODIFY - remove Security/Audit tabs)
```

**4.2 Routing:**
```jsx
// App.jsx
<Route path="/core-admin">
  <Route path="monitoring" element={<AxiomMonitoringPage />} />
  <Route path="security" element={<SecurityMonitoringPage />} />
  <Route path="audit" element={<AuditLogPage />} />
</Route>
```

**4.3 ACL setup:**
```java
// SecurityConfig.java
.requestMatchers("/core-admin/monitoring").hasRole("ADMIN")
.requestMatchers("/core-admin/security").hasAnyRole("ADMIN", "SECURITY")
.requestMatchers("/core-admin/audit").hasAnyRole("ADMIN", "AUDITOR")
```

### PHASE 5: Log Level Cleanup (🔵 NÍZKÁ PRIORITA)

**5.1 Production-ready log levels:**
```properties
# application-production.properties
logging.level.root=WARN
logging.level.cz.muriel.core=INFO
logging.level.org.springframework.security=WARN
logging.level.org.hibernate=WARN
logging.level.AUDIT=INFO  # Always keep audit logs
```

**5.2 Environment-based:**
```yaml
# docker-compose.yml
environment:
  - LOG_LEVEL=INFO  # Override per environment
  - AUDIT_LOG_LEVEL=INFO
  - LOGGING_LEVEL_CZ_MURIEL_CORE=INFO
```

---

## 📝 Checklist

### Immediate Actions (Dnes):
- [ ] Fix frontend `logger.setTenantContext()` call
- [ ] Test frontend logs v Loki s real tenant
- [ ] Commit: "fix(logging): Initialize frontend logger with tenant context"

### This Week:
- [ ] Fix backend tenant labels (system/public)
- [ ] Update dashboard LogQL queries
- [ ] Test všechny dashboardy s real data
- [ ] Commit: "fix(monitoring): Improve tenant labeling in logs"

### Next Sprint:
- [ ] Separate Security dashboard → `/core-admin/security`
- [ ] Separate Audit dashboard → `/core-admin/audit`
- [ ] Setup RBAC for monitoring pages
- [ ] Commit: "feat(monitoring): Separate Security and Audit dashboards with RBAC"

### Future:
- [ ] Production log levels (INFO/WARN)
- [ ] Log retention policy (Loki)
- [ ] Dashboard variables pro tenant selection
- [ ] Alerting rules v Grafana

---

## 🎯 Expected Outcomes

Po implementaci všech fixes:

1. ✅ **Frontend logy s real tenant:**
   ```
   {container="core-frontend", tenant="acme-corp"} → 1,523 lines
   {container="core-frontend", tenant="demo-tenant"} → 892 lines
   ```

2. ✅ **Backend logy clean:**
   ```
   {tenant="system"} → Health checks, Prometheus
   {tenant="public"} → Public API
   {tenant="acme-corp"} → Business requests
   ```

3. ✅ **Dashboardy s daty:**
   - Security: Failed logins by tenant
   - Audit: CRUD events by user
   - Performance: Request rate by tenant

4. ✅ **RBAC:**
   - Admins → Full monitoring
   - Security team → Security dashboard only
   - Auditors → Audit logs only

---

## 📚 Reference

### Důležité soubory:
```
Backend Logging:
- backend/src/main/java/cz/muriel/core/tenant/TenantFilter.java
- backend/src/main/resources/logback-spring.xml
- backend/src/main/resources/application.properties
- backend/src/main/java/cz/muriel/core/controller/FrontendLogsController.java

Frontend Logging:
- frontend/src/services/logger.js
- frontend/src/services/api.js
- frontend/src/App.jsx (Keycloak init)

Monitoring:
- frontend/src/pages/Admin/AxiomMonitoringPage.tsx
- frontend/src/shared/ui/SidebarNav.tsx
- docker/grafana/provisioning/dashboards/
```

### External docs:
- [Loki LogQL](https://grafana.com/docs/loki/latest/query/)
- [Logback MDC](https://logback.qos.ch/manual/mdc.html)
- [Spring Boot Logging](https://docs.spring.io/spring-boot/reference/features/logging.html)

---

**Author**: GitHub Copilot  
**Reviewed by**: Martin Horak  
**Status**: 🔴 ACTION REQUIRED - Frontend tenant context missing
