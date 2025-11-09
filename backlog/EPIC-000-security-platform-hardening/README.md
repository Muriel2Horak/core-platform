# EPIC-000: Security & Access Control Platform Hardening

**Status:** 🔴 **MASTER REFERENCE** (Continuous)  
**Priority:** P0 (SECURITY CRITICAL)  
**Owner:** Security + DevOps + Platform Team  
**Created:** 9. listopadu 2025

> **MASTER SECURITY EPIC**: Tento dokument je **jediný zdroj pravdy** pro všechna bezpečnostní pravidla v core-platform. Všechny ostatní EPICy se na něj musí odkazovat a dodržovat jeho požadavky.

---

## 🎯 Účel

**EPIC-000 sjednocuje všechny security požadavky napříč platformou a definuje závazná pravidla pro:**
- EPIC-002 (E2E Testing) - security test scenarios
- EPIC-003 (Monitoring) - audit & alerting
- EPIC-007 (Infrastructure) - config & secrets management
- EPIC-011 (n8n Workflow) - integration security
- EPIC-012 (Vault) - secrets backend
- EPIC-016 (AI/MCP) - data protection & AI safety
- EPIC-017 (Modular Architecture) - module isolation

**Není to teoretický "security bla bla" – každé pravidlo je implementovatelné a testovatelné.**

---

## 📋 Scope

Tento EPIC pokrývá:

1. **Identity & Access Management** - Keycloak jako jediný IdP
2. **RBAC & Tenant Isolation** - Multi-tenant access control
3. **API & Network Security** - HTTPS, rate limiting, CORS
4. **Secrets & Certificates** - Vault integration, rotation
5. **Logging & Audit** - Structured logs, audit trails
6. **External Integrations** - n8n, AI, third-party APIs
7. **Build & Supply Chain** - Dependency scanning, secret scanning
8. **Security Testing** - E2E security tests, compliance

---

## 🔐 1. Identity & Access Management (Keycloak)

### Pravidla

**Keycloak je jediný zdroj identity:**
- ✅ Každý člověk = uživatelský účet v Keycloaku
- ✅ Každý backend service / n8n / integrace = service account v Keycloaku
- ✅ Žádné long-lived tokeny v localStorage pro sensitive operace
- ✅ Všechny důležité API calls: `access_token` + `audience` / `scope` kontrola

### Core Role Definitions

| Role | Scope | Permissions | Use Case |
|------|-------|-------------|----------|
| `CORE_ADMIN` | Global | Full platform access, user mgmt, system config | DevOps, platform admin |
| `TENANT_ADMIN` | Tenant-scoped | Tenant config, user mgmt (own tenant), billing | Organization admin |
| `INTEGRATION_ADMIN` | Tenant-scoped | n8n workflows, API keys, webhooks | Integration specialist |
| `METAMODEL_DESIGNER` | Tenant-scoped | Metamodel design, workflow design | Business analyst |
| `USER` | Tenant-scoped | Read/write data (own tenant), execute workflows | End user |
| `AUDITOR` | Global (read-only) | Audit logs, compliance reports | Compliance officer |
| `SERVICE_ACCOUNT` | Service-scoped | API access (specific service) | Backend, n8n, AI |

### Keycloak → Application Mapping

**JWT Claims Structure:**
```json
{
  "sub": "user-uuid",
  "preferred_username": "john.doe@tenant.com",
  "realm": "admin",
  "tenant_id": "tenant-123",
  "roles": ["TENANT_ADMIN", "METAMODEL_DESIGNER"],
  "scope": "openid profile email",
  "audience": ["backend-api", "n8n-api"]
}
```

**Backend Permission Check:**
```java
@PreAuthorize("hasRole('TENANT_ADMIN') and @tenantService.canAccess(#tenantId)")
public void updateTenantConfig(String tenantId, ConfigDTO config) {
    // Implementation
}
```

**Multi-Tenant Mapping:**
- `tenant_id` claim v JWT (povinný pro všechny user requesty)
- Subdomain → tenant mapping: `tenant-123.core-platform.local` → `tenant_id=tenant-123`
- Realm per environment: `admin` (dev/staging/prod mají stejnou strukturu)

### Security Requirements

**Frontend:**
- ❌ NIKDY: `localStorage.setItem('api_key', ...)` nebo long-lived credentials
- ✅ VŽDY: Authorization Code Flow s PKCE
- ✅ Token refresh: automatický (silent iframe nebo refresh token)
- ✅ Logout: clear session + Keycloak logout endpoint

**Backend:**
- ✅ JWT validation: signature, expiry, issuer, audience
- ✅ Role extraction: z Keycloak JWT claims
- ✅ Service accounts: client credentials flow (ne user password!)

**Návaznost na EPICy:**
- **EPIC-007**: Keycloak deployment, SSL, realm config
- **EPIC-011**: n8n používá service account s limited scope
- **EPIC-016**: AI assistant má service account, ne user credentials

---

## 🔒 2. RBAC & Tenant Isolation

### Global Rule: Žádný Short-Cut!

**Každý request do backendu MUSÍ:**
1. Extrahovat `tenant_id` z JWT / host / realm
2. Ověřit že uživatel má přístup KE SVÉMU tenantu
3. Žádný přístup napříč tenanty (ani přes n8n, ani přes AI, ani přes DMS)

### Tenant Isolation Matrix

| Feature | Tenant Check | Implementation |
|---------|--------------|----------------|
| **Metamodel** | ✅ Required | `@TenantScoped` annotation, JPA filter |
| **Workflow Engine** | ✅ Required | Workflow instance tagged with `tenant_id` |
| **DMS (Documents)** | ✅ Required | S3 bucket prefix: `tenant-123/documents/` |
| **Loki UI** | ✅ Required | LogQL filter: `{tenant="tenant-123"}` |
| **Monitoring Dashboards** | ✅ Required | Grafana data source variable: `$tenant_id` |
| **n8n Workflows** | ✅ Required | Workflow tagged with `tenant_id`, execution context isolated |
| **Modular Plugins** | ✅ Required | Module registration per tenant, shared code isolated |

### Implementation Examples

**JPA Tenant Filter (Backend):**
```java
@Entity
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = "string"))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class WorkflowDefinition {
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    // ...
}

@Component
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    public static void setTenant(String tenantId) {
        currentTenant.set(tenantId);
    }
    
    public static String getTenant() {
        return currentTenant.get();
    }
}
```

**n8n Workflow Execution (Isolated):**
```typescript
// n8n custom node: TenantAwareHttpRequest
async execute(this: IExecuteFunctions): Promise<INodeExecutionData[][]> {
  const tenantId = this.getNodeParameter('tenantId', 0) as string;
  
  // Verify workflow's tenant matches execution context
  if (tenantId !== this.getWorkflow().settings.tenantId) {
    throw new Error('Tenant mismatch: workflow cannot access other tenants');
  }
  
  const url = `https://backend/api/tenants/${tenantId}/data`;
  // ... fetch with tenant-scoped token
}
```

**Loki Query (UI Filter):**
```logql
{app="backend", tenant="tenant-123"} |= "ERROR"
```

### Security Requirements

**Kontroly pro všechny features:**
- ✅ Metamodel API: `GET /api/metamodels?tenant={tenant_id}` - jen "svoje" modely
- ✅ Workflow API: `POST /api/workflows/{id}/execute` - kontrola že workflow patří danému tenantu
- ✅ DMS API: `GET /api/documents/{id}` - kontrola tenant_id v metadatech dokumentu
- ✅ Monitoring: Grafana dashboard variables musí filtrovat podle `$tenant_id`
- ✅ n8n: Workflow nemůže číst/psát data jiného tenanta

**Testování:**
- ✅ E2E test: User z `tenant-A` nesmí vidět data z `tenant-B`
- ✅ Integration test: API vrací 403 Forbidden při pokusu o cross-tenant access
- ✅ Audit: Každý cross-tenant attempt logován do Loki

**Návaznost na EPICy:**
- **EPIC-002**: E2E testy zahrnují tenant isolation scenarios
- **EPIC-005**: Metamodel Studio respektuje tenant scope
- **EPIC-006**: Workflow Engine izoluje execution context
- **EPIC-008**: DMS používá tenant-scoped S3 prefixes
- **EPIC-011**: n8n workflows tagged with tenant_id
- **EPIC-017**: Modular plugins registered per tenant

---

## 🔑 3. Secrets & Vault Integration

### Pravidla

**Všechna hesla, client secrets, API klíče, DB přístupy:**
- ❌ NIKDY v Gitu (`.env` v `.gitignore`)
- ❌ NIKDY hardcoded v kódu, Dockerfile, YAML
- ❌ NIKDY plaintext v n8n workflows
- ✅ V secrets manageru (Vault nebo kompatibilní backend)
- ✅ Přístup přes role-based policies
- ✅ Rotace definovaná (DB, JWT signing keys, API keys)

### Secret Categories

| Category | Examples | Rotation Period | Owner |
|----------|----------|-----------------|-------|
| **Database** | `POSTGRES_PASSWORD`, `REDIS_PASSWORD` | 90 dní | DevOps |
| **Keycloak** | `KEYCLOAK_ADMIN_PASSWORD`, `OIDC_CLIENT_SECRET` | 180 dní | Security team |
| **SMTP** | `SMTP_PASSWORD` | 180 dní | DevOps |
| **External APIs** | `OPENAI_API_KEY`, `STRIPE_SECRET_KEY` | On compromise | Integration admin |
| **n8n** | `N8N_ENCRYPTION_KEY`, webhook secrets | 90 dní | Integration admin |
| **SSL/TLS** | Private keys, CA certs | 365 dní (auto Let's Encrypt) | DevOps |
| **JWT Signing** | `JWT_SECRET` | 180 dní | Security team |

### Implementation Flow

```
Application Startup
  ↓
1. Read .env (VAULT_ADDR, VAULT_TOKEN, VAULT_ROLE)
  ↓
2. Authenticate to Vault (AppRole or Kubernetes auth)
  ↓
3. Fetch secrets from Vault paths:
   - secret/data/database (DB credentials)
   - secret/data/keycloak (OAuth2 secrets)
   - secret/data/integrations (API keys)
  ↓
4. Inject into application (Spring Boot properties, env vars)
  ↓
5. Runtime: Never log secrets, never return in API responses
```

**Spring Boot Integration:**
```yaml
# application.yml
spring:
  cloud:
    vault:
      uri: ${VAULT_ADDR:http://vault:8200}
      authentication: APPROLE
      app-role:
        role-id: ${VAULT_ROLE_ID}
        secret-id: ${VAULT_SECRET_ID}
      database:
        enabled: true
        role: backend-db-role
        backend: database
```

**n8n Credential Loading:**
```typescript
// n8n custom credential type: VaultBackedCredential
import { ICredentialType, INodeProperties } from 'n8n-workflow';

export class VaultBackedOpenAI implements ICredentialType {
  name = 'vaultBackedOpenAI';
  displayName = 'OpenAI (Vault)';
  
  properties: INodeProperties[] = [
    {
      displayName: 'Vault Path',
      name: 'vaultPath',
      type: 'string',
      default: 'secret/data/openai',
      description: 'Path to secret in Vault',
    },
  ];
  
  async authenticate(credentials: any): Promise<string> {
    const vaultClient = getVaultClient();
    const secret = await vaultClient.read(credentials.vaultPath);
    return secret.data.api_key;
  }
}
```

### Security Requirements

**Načítání secrets:**
- ✅ Backend: Spring Cloud Vault nebo Vault Java SDK
- ✅ n8n: Custom credential types s Vault backend
- ✅ Frontend: NIKDY! Secrets jen v backendu
- ✅ CI/CD: GitHub Actions secrets → injektované přes env vars

**Rotace:**
- ✅ Automated rotation: Vault dynamic secrets (DB credentials)
- ✅ Manual rotation: API keys (documented procedure)
- ✅ Notification: Slack alert 7 days before expiry

**Audit:**
- ✅ Každý Vault access logován (kdo, kdy, jaký secret)
- ✅ Logy v Loki: `{app="vault-audit"}`

**Návaznost na EPICy:**
- **EPIC-007**: `.env` management, gitignore rules
- **EPIC-012**: Vault deployment, policy management, rotation automation
- **EPIC-011**: n8n Vault-backed credentials

---

## 🌐 4. API & Network Security

### Pravidla

**HTTPS Everywhere:**
- ✅ Nginx jako vstupní brána (SSL termination)
- ✅ Backend internal HTTP OK (Docker network izolovaný)
- ✅ External APIs: POUZE HTTPS (Let's Encrypt certs)

**Network Segmentation:**
- ✅ Public tier: Nginx (port 443)
- ✅ Application tier: Backend, Frontend static (internal)
- ✅ Data tier: PostgreSQL, Redis, Kafka (internal, ne exposed)
- ✅ Monitoring tier: Loki, Prometheus, Grafana (internal + auth)

### Nginx Configuration

**Rate Limiting:**
```nginx
# /etc/nginx/conf.d/rate-limit.conf
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=login:10m rate=5r/m;

location /api/ {
    limit_req zone=api burst=20 nodelay;
    # ...
}

location /auth/realms/admin/protocol/openid-connect/token {
    limit_req zone=login burst=3 nodelay;
    # ...
}
```

**WAF Rules (Basic):**
```nginx
# Block SQL injection attempts
if ($args ~* "(\<|%3C).*script.*(\>|%3E)") {
    return 403;
}

if ($query_string ~* "union.*select.*\(") {
    return 403;
}

# Block common attack patterns
location ~ /\. {
    deny all;
}
```

**CORS Policy:**
```nginx
# Strict CORS - NO wildcards
add_header 'Access-Control-Allow-Origin' 'https://admin.core-platform.local' always;
add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type' always;
add_header 'Access-Control-Max-Age' '3600' always;
```

### Security Headers

```nginx
# Security headers (all responses)
add_header X-Frame-Options "DENY" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' https://admin.core-platform.local;" always;
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;
```

### API Endpoint Classification

| Type | Examples | Access Control | Exposure |
|------|----------|----------------|----------|
| **Public** | Health checks, static assets | None | Internet |
| **Authenticated** | `/api/metamodels`, `/api/workflows` | JWT required | Authenticated users |
| **Admin** | `/api/admin/users`, `/api/admin/tenants` | `CORE_ADMIN` role | Platform admins only |
| **Internal** | Actuator endpoints, metrics | IP whitelist or mTLS | Internal network only |
| **Service-to-Service** | n8n → Backend, Backend → Keycloak | Service account + mTLS | Docker network |

### Multi-Tenant Subdomains

**Subdomain Routing:**
```nginx
server {
    server_name ~^(?<tenant>[^.]+)\.core-platform\.local$;
    
    location / {
        # Inject tenant ID into headers
        proxy_set_header X-Tenant-ID $tenant;
        proxy_pass http://backend:8080;
    }
}
```

**Backend Tenant Extraction:**
```java
@Component
public class TenantInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        String tenantId = request.getHeader("X-Tenant-ID");
        if (tenantId == null) {
            // Fallback: extract from JWT claim
            tenantId = extractTenantFromJWT(request);
        }
        TenantContext.setTenant(tenantId);
        return true;
    }
}
```

### Security Requirements

**Endpoint Protection:**
- ✅ Žádný přímý přístup na Loki, DB, Keycloak admin (pouze přes Nginx)
- ✅ Frontend NIKDY nevolá interní služby přímo (pouze přes backend API)
- ✅ Rate limiting na všech public endpoints
- ✅ CORS: strict allow-list (ne `*`)

**Testování:**
- ✅ E2E test: Rate limit enforcement (429 Too Many Requests)
- ✅ E2E test: CORS headers correct
- ✅ Security scan: No exposed internal ports

**Návaznost na EPICy:**
- **EPIC-007**: Nginx SSL config, reverse proxy setup
- **EPIC-003**: Monitoring WAF metrics (blocked requests)

---

## 📊 5. Logging, Audit & Monitoring

### Pravidla

**Structured Logging:**
- ✅ JSON format (Loki-friendly)
- ✅ Citlivá data maskovaná (hesla, API klíče, PII)
- ✅ Context propagation (tenant_id, user_id, request_id)

**Audit Trail:**
- ✅ Každá změna metamodelu → audit event
- ✅ Každá změna workflow definice → audit event
- ✅ Každé spuštění n8n workflow → audit event
- ✅ Každý upload/delete dokumentu (DMS) → audit event
- ✅ Každá změna uživatelských oprávnění → audit event

**Monitoring:**
- ✅ Backend, Frontend, Loki, Kafka, DB, Keycloak → metrics exportované
- ✅ Alerty: error rate, latence, anomálie, security events

### Log Structure

**Backend (Spring Boot):**
```json
{
  "timestamp": "2025-11-09T10:30:45.123Z",
  "level": "INFO",
  "logger": "cz.muriel.core.metamodel.MetamodelService",
  "message": "Metamodel updated",
  "tenant_id": "tenant-123",
  "user_id": "user-456",
  "request_id": "req-789",
  "metamodel_id": "model-abc",
  "operation": "UPDATE",
  "changes": {
    "fields_added": 3,
    "fields_removed": 1
  }
}
```

**Sensitive Data Masking:**
```java
@Component
public class LogMaskingConverter extends ClassicConverter {
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("password\":\"[^\"]+\"");
    
    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        return PASSWORD_PATTERN.matcher(message).replaceAll("password\":\"***MASKED***\"");
    }
}
```

### Audit Events

**Event Types:**
```typescript
enum AuditEventType {
  METAMODEL_CREATED = 'metamodel.created',
  METAMODEL_UPDATED = 'metamodel.updated',
  METAMODEL_DELETED = 'metamodel.deleted',
  WORKFLOW_CREATED = 'workflow.created',
  WORKFLOW_EXECUTED = 'workflow.executed',
  WORKFLOW_FAILED = 'workflow.failed',
  DOCUMENT_UPLOADED = 'document.uploaded',
  DOCUMENT_DELETED = 'document.deleted',
  USER_ROLE_CHANGED = 'user.role_changed',
  TENANT_CONFIG_UPDATED = 'tenant.config_updated',
  N8N_WORKFLOW_TRIGGERED = 'n8n.workflow_triggered',
  AI_PROMPT_EXECUTED = 'ai.prompt_executed',
}
```

**Audit Event Storage:**
```sql
CREATE TABLE audit_events (
  id UUID PRIMARY KEY,
  timestamp TIMESTAMPTZ NOT NULL,
  tenant_id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255),
  event_type VARCHAR(100) NOT NULL,
  resource_type VARCHAR(100),
  resource_id VARCHAR(255),
  changes JSONB,
  metadata JSONB,
  ip_address INET,
  user_agent TEXT
);

CREATE INDEX idx_audit_tenant_time ON audit_events (tenant_id, timestamp DESC);
CREATE INDEX idx_audit_event_type ON audit_events (event_type, timestamp DESC);
```

**Loki Labels:**
```logql
{
  app="backend",
  tenant="tenant-123",
  level="ERROR",
  audit="true"
}
```

### Monitoring Metrics

**Backend Actuator (Prometheus):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: core-platform-backend
      tenant: ${TENANT_ID:default}
```

**Custom Metrics:**
```java
@Component
public class SecurityMetrics {
    private final Counter authFailures;
    private final Counter tenantIsolationViolations;
    
    public SecurityMetrics(MeterRegistry registry) {
        this.authFailures = Counter.builder("security.auth.failures")
            .description("Authentication failures")
            .tag("type", "jwt_validation")
            .register(registry);
            
        this.tenantIsolationViolations = Counter.builder("security.tenant.violations")
            .description("Tenant isolation violations detected")
            .register(registry);
    }
}
```

**Grafana Alerts:**
```yaml
# alert-rules.yml
groups:
  - name: security
    interval: 1m
    rules:
      - alert: HighAuthFailureRate
        expr: rate(security_auth_failures_total[5m]) > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High authentication failure rate"
          
      - alert: TenantIsolationViolation
        expr: increase(security_tenant_violations_total[5m]) > 0
        for: 0m
        labels:
          severity: critical
        annotations:
          summary: "Tenant isolation violation detected!"
```

### Security Requirements

**Minimální audit trail:**
- ✅ Metamodel změny: kdo, kdy, co změnil
- ✅ Workflow execution: kdo spustil, vstup, výstup, chyby
- ✅ n8n workflows: trigger source, execution context, API calls
- ✅ DMS dokumenty: upload, download, delete actions
- ✅ User management: role změny, přidání/odebrání uživatelů

**Retention:**
- ✅ Application logs: 30 dní (Loki)
- ✅ Audit events: 1 rok (PostgreSQL)
- ✅ Metrics: 90 dní (Prometheus)

**Compliance:**
- ✅ GDPR: Audit logs obsahují consent events
- ✅ SOC 2: Immutable audit trail, tamper-evident

**Návaznost na EPICy:**
- **EPIC-003**: Monitoring & Observability - Loki deployment, Grafana dashboards
- **EPIC-005**: Metamodel Studio - audit events při změnách
- **EPIC-006**: Workflow Engine - execution audit trail
- **EPIC-011**: n8n - workflow execution logging

---

## 🔗 6. External Integrations (n8n, AI, APIs)

### Pravidla

**n8n jako Internal Orchestrator:**
- ✅ n8n běží jako service account (`n8n-service@core-platform.local`)
- ✅ Všechny API volání přes oficiální backend API (ne přímý DB access)
- ✅ Secrets POUZE z Vaultu (ne hardcoded v workflow JSON)
- ✅ Workflow execution auditovaná (kdo spustil, vstup, výstup)
- ✅ Workflow s AI / externími službami = konfigurovatelné, vypnutelné

### n8n Security Architecture

```
User/Trigger
  ↓
n8n Workflow Execution
  ↓
1. Authenticate as service account (Keycloak)
  ↓
2. Fetch secrets from Vault (API keys, credentials)
  ↓
3. Call Backend API (tenant-scoped)
  ↓
4. Log execution to audit trail
  ↓
5. Return result (no secrets in response)
```

**n8n Service Account:**
```json
{
  "clientId": "n8n-service",
  "clientSecret": "${VAULT:secret/n8n/client-secret}",
  "serviceAccountsEnabled": true,
  "roles": ["INTEGRATION_SERVICE"],
  "scope": ["backend-api:read", "backend-api:write"]
}
```

**Workflow Security Context:**
```typescript
interface WorkflowSecurityContext {
  tenantId: string;           // Workflow belongs to tenant
  executedBy: string;         // User or trigger source
  allowedApis: string[];      // Whitelist of callable APIs
  secretsScope: string[];     // Vault paths accessible
  maxExecutionTime: number;   // Timeout (prevent runaway)
}
```

### AI & LLM Integration

**Data Protection Rules:**
- ❌ NIKDY neposílat PII do AI bez policy checku
- ❌ NIKDY celý DB dump do LLM
- ✅ Anonymizace kde to dává smysl (hash user IDs, mask emails)
- ✅ Allow-list pro AI endpoints (OpenAI, Anthropic, Azure OpenAI)
- ✅ Rate limiting pro AI calls (cost control)

**AI Safety Policy:**
```yaml
# config/ai-safety-policy.yml
ai:
  providers:
    openai:
      enabled: true
      models: ['gpt-4', 'gpt-3.5-turbo']
      max_tokens: 4000
      rate_limit: 100/hour/tenant
    anthropic:
      enabled: false  # Disabled for now
      
  data_protection:
    pii_detection: true
    anonymization: true
    allowed_fields:
      - metamodel.name
      - metamodel.description
    blocked_fields:
      - user.email
      - user.phone
      - document.content  # Unless explicitly allowed
      
  audit:
    log_prompts: true
    log_responses: true
    retention_days: 90
```

**Metamodel + AI Integration:**
```typescript
// MCP Server: Metamodel Tools
async function getMetamodelSchema(tenantId: string, modelId: string): Promise<Schema> {
  // 1. Verify tenant access
  if (!canAccessTenant(tenantId)) {
    throw new Error('Unauthorized');
  }
  
  // 2. Fetch schema (structure only, no data)
  const schema = await fetchMetamodel(tenantId, modelId);
  
  // 3. Anonymize sensitive fields
  return anonymizeSchema(schema, ['createdBy', 'modifiedBy']);
}
```

### Third-Party API Security

**API Key Management:**
```typescript
// n8n custom node: SecureApiCall
export class SecureApiCall implements INodeType {
  async execute(this: IExecuteFunctions): Promise<INodeExecutionData[][]> {
    const apiKeyPath = this.getNodeParameter('vaultPath', 0) as string;
    
    // 1. Fetch API key from Vault
    const apiKey = await this.helpers.getCredential('vaultBackedApiKey', apiKeyPath);
    
    // 2. Validate allow-list
    const url = this.getNodeParameter('url', 0) as string;
    if (!isAllowedDomain(url)) {
      throw new Error(`Domain ${url} not in allow-list`);
    }
    
    // 3. Make request with audit logging
    const response = await this.helpers.request({
      url,
      headers: { 'Authorization': `Bearer ${apiKey}` },
    });
    
    // 4. Audit log
    await logAuditEvent({
      type: 'EXTERNAL_API_CALL',
      url,
      tenantId: this.getWorkflow().settings.tenantId,
    });
    
    return response;
  }
}
```

**Domain Allow-List:**
```typescript
const ALLOWED_EXTERNAL_DOMAINS = [
  'api.openai.com',
  'api.anthropic.com',
  'api.stripe.com',
  'hooks.slack.com',
  // Tenant-specific (from config)
  ...getTenantAllowedDomains(tenantId)
];

function isAllowedDomain(url: string): boolean {
  const domain = new URL(url).hostname;
  return ALLOWED_EXTERNAL_DOMAINS.includes(domain);
}
```

### Security Requirements

**n8n:**
- ✅ Service account autentizace (Keycloak)
- ✅ Secrets z Vaultu (ne hardcoded)
- ✅ Tenant isolation (workflow execution context)
- ✅ Audit trail (každé spuštění logováno)
- ✅ Rate limiting (prevent abuse)

**AI:**
- ✅ PII detection before sending to LLM
- ✅ Anonymization kde možno
- ✅ Allow-list AI providers
- ✅ Cost tracking (token usage per tenant)
- ✅ Prompt + response logging (compliance)

**Testování:**
- ✅ E2E test: n8n workflow nemůže volat nepovolené API
- ✅ E2E test: AI call s PII je blokován
- ✅ Integration test: Secrets loaded z Vaultu (ne env vars)

**Návaznost na EPICy:**
- **EPIC-011**: n8n deployment, custom nodes, Vault integration
- **EPIC-016**: AI/MCP server, prompt safety, data protection
- **EPIC-012**: Vault secret management for API keys

---

## 🛡️ 7. Build & Supply Chain Security

### Pravidla

**Dependency Scanning:**
- ✅ OWASP Dependency Check (Java backend)
- ✅ npm audit (Frontend)
- ✅ Trivy (Docker image scanning)
- ✅ CI pipeline blokuje CRITICAL CVEs

**Secret Scanning:**
- ✅ GitLeaks pre-commit hook
- ✅ GitHub Secret Scanning (repo level)
- ✅ CI check: fail pokud secret leak

**Image Provenance:**
- ✅ Signed Docker images (cosign nebo Docker Content Trust)
- ✅ SBOM (Software Bill of Materials) generovaný
- ✅ Image tags: semantic versioning (ne `latest`)

### CI/CD Security Pipeline

```yaml
# .github/workflows/security.yml
name: Security Checks

on: [push, pull_request]

jobs:
  secret-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: GitLeaks Scan
        uses: gitleaks/gitleaks-action@v2
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          
  dependency-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: OWASP Dependency Check
        run: |
          cd backend
          ./mvnw dependency-check:check
      - name: Upload Report
        uses: actions/upload-artifact@v3
        with:
          name: dependency-check-report
          path: backend/target/dependency-check-report.html
          
  image-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build Image
        run: docker build -t core-platform/backend:${{ github.sha }} .
      - name: Trivy Scan
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: core-platform/backend:${{ github.sha }}
          severity: CRITICAL,HIGH
          exit-code: 1  # Fail on findings
          
  sbom-generation:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Generate SBOM
        run: |
          syft core-platform/backend:${{ github.sha }} -o spdx-json > sbom.json
      - name: Upload SBOM
        uses: actions/upload-artifact@v3
        with:
          name: sbom
          path: sbom.json
```

### Dependency Management

**Backend (Maven):**
```xml
<!-- pom.xml -->
<build>
  <plugins>
    <plugin>
      <groupId>org.owasp</groupId>
      <artifactId>dependency-check-maven</artifactId>
      <version>8.4.0</version>
      <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>  <!-- Fail on HIGH severity -->
        <suppressionFiles>
          <suppressionFile>owasp-suppressions.xml</suppressionFile>
        </suppressionFiles>
      </configuration>
    </plugin>
  </plugins>
</build>
```

**Frontend (npm):**
```json
{
  "scripts": {
    "audit": "npm audit --audit-level=high",
    "audit:fix": "npm audit fix"
  }
}
```

**Pre-Commit Hooks (Lefthook):**
```yaml
# lefthook.yml
pre-commit:
  commands:
    gitleaks:
      run: gitleaks protect --staged --verbose
    dependency-check:
      glob: "{pom.xml,package.json}"
      run: make security-check
```

### Docker Image Hardening

**Multi-Stage Build:**
```dockerfile
# Backend Dockerfile (security-hardened)
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy only necessary artifacts
COPY --chown=appuser:appgroup target/backend.jar /app/backend.jar

# Drop privileges
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/backend.jar"]
```

**Image Signing (Cosign):**
```bash
# Sign image after build
cosign sign --key cosign.key core-platform/backend:v1.2.3

# Verify before deployment
cosign verify --key cosign.pub core-platform/backend:v1.2.3
```

### Security Requirements

**CI/CD Gates:**
- ✅ Secret scan PASS (no leaks)
- ✅ Dependency scan PASS (no CRITICAL CVEs)
- ✅ Image scan PASS (no HIGH/CRITICAL vulnerabilities)
- ✅ SBOM generated and attached to release

**Build Artifacts:**
- ✅ Docker images: semantic versioning (`v1.2.3`, ne `latest`)
- ✅ Signed images (verifiable provenance)
- ✅ SBOM (CycloneDX nebo SPDX format)

**Monitoring:**
- ✅ Dependabot alerts (GitHub)
- ✅ Weekly dependency review
- ✅ Incident response plan for zero-day CVEs

**Návaznost na EPICy:**
- **EPIC-002**: E2E tests zahrnují security scans
- **EPIC-007**: Dockerfile hardening, image build process

---

## 🧪 8. Security Testing & Definition of Done

### Minimální DoD pro Feature / EPIC

Každý feature merge MUSÍ splňovat:

**1. Žádný Secret Leak**
- ✅ GitLeaks pre-commit check PASS
- ✅ No hardcoded API keys, passwords, tokens v diffu
- ✅ `.env` changes reviewed (no real secrets)

**2. RBAC & Tenant Isolation Tested**
- ✅ Unit test: User z `tenant-A` nesmí vidět data z `tenant-B`
- ✅ Integration test: API vrací 403 Forbidden při cross-tenant access
- ✅ E2E test: UI zakáže cross-tenant actions

**3. Logy Neobsahují Citlivá Data**
- ✅ Manual review: Logy nemaskují PII (emails, phones, API keys)
- ✅ Automated check: Regex scan pro common secrets patterns

**4. Externí Integrace = Auditované & Limited**
- ✅ n8n workflow: secrets z Vaultu (ne hardcoded)
- ✅ AI call: PII detection enabled
- ✅ API call: domain allow-list enforced
- ✅ Audit event logován do Loki

**5. E2E Security Tests PASS**
- ✅ Authentication flow (login, token refresh, logout)
- ✅ Authorization (role-based access control)
- ✅ Tenant isolation (cross-tenant blocked)
- ✅ Rate limiting (429 after threshold)
- ✅ CORS (correct headers)

### E2E Security Test Suite

**Test Categories:**

| Category | Test Count | Examples |
|----------|------------|----------|
| **Authentication** | 5 | Login success, invalid credentials, token expiry, refresh flow, logout |
| **Authorization** | 8 | Role-based access, tenant isolation, cross-tenant blocked, admin-only endpoints |
| **API Security** | 6 | Rate limiting, CORS, SQL injection, XSS, CSRF |
| **Secrets** | 4 | No secrets in response, Vault integration, rotation trigger |
| **Audit** | 3 | Audit event created, audit log query, retention |

**Example E2E Test (Playwright):**
```typescript
// e2e/specs/security/tenant-isolation.spec.ts
test('User cannot access other tenant data', async ({ page, request }) => {
  // Login as user from tenant-A
  await loginAsUser(page, 'user-tenant-a@example.com', 'Test.1234');
  
  // Get tenant-A metamodel (should succeed)
  const tenantAModel = await request.get('/api/metamodels/model-123', {
    headers: { 'X-Tenant-ID': 'tenant-a' }
  });
  expect(tenantAModel.status()).toBe(200);
  
  // Try to access tenant-B metamodel (should fail)
  const tenantBModel = await request.get('/api/metamodels/model-456', {
    headers: { 'X-Tenant-ID': 'tenant-b' }
  });
  expect(tenantBModel.status()).toBe(403);
  
  // Verify audit event logged
  const auditLogs = await request.get('/api/admin/audit', {
    params: { event_type: 'TENANT_ISOLATION_VIOLATION' }
  });
  expect(auditLogs.json()).toHaveLength(1);
});
```

**Example Security Test (Rate Limiting):**
```typescript
test('API enforces rate limiting', async ({ request }) => {
  const requests = [];
  
  // Send 15 requests rapidly (limit is 10/s)
  for (let i = 0; i < 15; i++) {
    requests.push(request.get('/api/health'));
  }
  
  const responses = await Promise.all(requests);
  
  // First 10 should succeed
  expect(responses.slice(0, 10).every(r => r.status() === 200)).toBe(true);
  
  // Remaining should be rate-limited
  expect(responses.slice(10).some(r => r.status() === 429)).toBe(true);
});
```

### Compliance Checklist

**GDPR:**
- ✅ User consent tracking (audit trail)
- ✅ Right to deletion (cascade delete user data)
- ✅ Data export (API endpoint pro user data)
- ✅ PII minimization (only collect necessary data)

**SOC 2:**
- ✅ Immutable audit logs (write-only, no delete)
- ✅ Access controls tested (RBAC verified)
- ✅ Encryption in transit (HTTPS everywhere)
- ✅ Encryption at rest (DB, S3 buckets)

**ISO 27001:**
- ✅ Security policy documented (tento EPIC)
- ✅ Risk assessment (vulnerability scanning)
- ✅ Incident response plan (runbook)

### Security Requirements

**Každý PR musí:**
- ✅ Pass GitLeaks (no secrets)
- ✅ Pass dependency scan (no CRITICAL CVEs)
- ✅ Include security test coverage (pokud mění auth/RBAC/API)

**Každý EPIC musí:**
- ✅ Mít "Security Alignment" sekci v README
- ✅ Reference na EPIC-000 pravidla
- ✅ DoD zahrnuje security checks

**Návaznost na EPICy:**
- **EPIC-002**: E2E Testing - security test scenarios
- **EPIC-007**: Infrastructure - hardening checklist

---

## 📚 Security Alignment v Ostatních EPICech

Každý EPIC MUSÍ mít sekci **"Security Alignment"**, která říká:
- Jak tento EPIC respektuje EPIC-000
- Jaké konkrétní security pravidlo přebírá

### Template pro Security Alignment

```markdown
## 🔒 Security Alignment (EPIC-000)

**Tento EPIC dodržuje následující pravidla z [EPIC-000](../EPIC-000-security-platform-hardening/README.md):**

### Identity & Access Management
- ✅ Používá Keycloak jako jediný IdP
- ✅ Service accounts pro integrace
- ✅ Žádné long-lived tokeny v kódu

### RBAC & Tenant Isolation
- ✅ Všechny API requesty: tenant_id kontrola
- ✅ Data scoped per tenant (JPA filter, S3 prefix, atd.)

### Secrets Management
- ✅ Secrets načítány z Vaultu (ne .env hardcoded)
- ✅ Rotace definovaná (90-day cycle pro DB credentials)

### Logging & Audit
- ✅ Strukturované logy (JSON, Loki)
- ✅ Audit trail: všechny změny logované

### Testing
- ✅ E2E security tests: [odkaz na test specs]
- ✅ DoD zahrnuje security checklist
```

---

## 🔗 EPIC Dependencies

**EPIC-000 poskytuje pravidla pro:**

| EPIC | Security Requirement | Implementation |
|------|----------------------|----------------|
| **EPIC-002** | E2E security tests | Authentication, RBAC, tenant isolation test scenarios |
| **EPIC-003** | Monitoring & alerts | Security metrics (auth failures, tenant violations), alerting |
| **EPIC-007** | Infrastructure hardening | .env management, Nginx WAF, SSL config |
| **EPIC-011** | n8n security | Service account auth, Vault secrets, audit logging |
| **EPIC-012** | Vault integration | Secret storage, rotation, policy management |
| **EPIC-016** | AI safety | PII detection, anonymization, allow-list |
| **EPIC-017** | Module isolation | Plugin tenant scoping, module permissions |

**EPIC-000 závisí na:**
- **EPIC-007**: Keycloak deployment, Nginx config, SSL setup
- **EPIC-012**: Vault backend pro secrets management

---

## 📝 Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- ✅ Keycloak role model defined (core roles documented)
- ✅ Tenant isolation JPA filters (backend)
- ✅ Security headers (Nginx config)
- ✅ GitLeaks pre-commit hook

### Phase 2: Secrets & Vault (Week 3-4)
- 🔵 Vault integration (EPIC-012)
- 🔵 Secret rotation automation (DB credentials)
- 🔵 n8n Vault-backed credentials

### Phase 3: Monitoring & Audit (Week 5-6)
- 🔵 Audit event schema (PostgreSQL table)
- 🔵 Structured logging (backend + n8n)
- 🔵 Security metrics (Prometheus + Grafana)

### Phase 4: Testing & Compliance (Week 7-8)
- 🔵 E2E security test suite (Playwright)
- 🔵 CI/CD security gates (dependency scan, image scan)
- 🔵 Compliance documentation (GDPR, SOC 2)

---

## 🎯 Success Criteria

**EPIC-000 je úspěšný pokud:**

1. ✅ **Všechny EPICy mají "Security Alignment" sekci**
2. ✅ **Zero secrets v Git** (GitLeaks clean history)
3. ✅ **Tenant isolation 100%** (E2E testy pass)
4. ✅ **Vault integration** (všechny secrets z Vault, ne .env)
5. ✅ **Audit trail complete** (metamodel, workflow, DMS, users)
6. ✅ **CI/CD security gates** (dependency scan, secret scan, image scan)
7. ✅ **E2E security tests** (auth, RBAC, rate limit, CORS)
8. ✅ **Compliance-ready** (GDPR, SOC 2 documentation)

---

## 📖 References

- **EPIC-002**: [E2E Testing Infrastructure](../EPIC-002-e2e-testing-infrastructure/README.md)
- **EPIC-003**: [Monitoring & Observability](../EPIC-003-monitoring-observability/README.md)
- **EPIC-007**: [Infrastructure & Deployment](../EPIC-007-infrastructure-deployment/README.md)
- **EPIC-011**: [n8n Workflow Automation](../EPIC-011-n8n-workflow-automation/README.md)
- **EPIC-012**: [Vault Integration](../EPIC-012-vault-integration/README.md)
- **EPIC-016**: [AI/MCP Collaboration](../EPIC-016-ai-metamodel-collaboration/README.md)
- **EPIC-017**: [Modular Architecture](../EPIC-017-modular-architecture/README.md)

**External:**
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [HashiCorp Vault Best Practices](https://developer.hashicorp.com/vault/tutorials/recommended-patterns)
- [GDPR Compliance Guide](https://gdpr.eu/)

---

**Last Updated:** 9. listopadu 2025  
**Epic Owner:** Security Team + Martin Horak (@Muriel2Horak)  
**Status:** 🔴 MASTER REFERENCE (Continuous)
