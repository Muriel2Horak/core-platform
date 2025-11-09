# EPIC-000: Security & Access Control Platform Hardening

**Status:** 🔴 **MASTER REFERENCE** (Continuous)  
**Priority:** P0 (SECURITY CRITICAL)  
**Owner:** Security + DevOps + Platform Team  
**Created:** 9. listopadu 2025  
**Updated:** 9. listopadu 2025 (Konsolidace + alignment)

---

## 🎯 Purpose

**EPIC-000 je jediný závazný "Security Master Contract" pro celou core-platform.** Definuje bezpečnostní principy, výstupy a kontrolní mechanismy, které VŠECHNY ostatní EPICy musí dodržovat. Nejde o implementační EPIC – konkrétní řešení jsou v navazujících EPICech. Tohle je **rámec a baseline**, proti kterému se všechno měří.

---

## 📋 Scope & Návaznosti

EPIC-000 definuje **co** musí platforma splňovat v oblasti bezpečnosti. **Jak** to implementovat řeší:

- **EPIC-002** (E2E Testing) - security test scenarios, regresní testy auth/RBAC/multi-tenant
- **EPIC-003** (Monitoring & Observability) - security metriky, alerty, audit dashboards
- **EPIC-007** (Infrastructure & Deployment) - Keycloak deployment, SSL, secrets management, network izolace
- **EPIC-011** (n8n Workflow Automation) - service account auth, integration security, Vault pro credentials
- **EPIC-012** (Vault Integration) - secrets storage, rotace, policies (implementuje požadavky EPIC-000 Pillar 3)
- **EPIC-014** (UX/UI Design System) - security UI komponenty (login, consent, error states)
- **EPIC-016** (AI/MCP Collaboration) - AI safety, data protection, PII anonymizace
- **EPIC-017** (Modular Architecture) - module isolation, tenant-scoped plugin registry

**Tento dokument NEŘEŠÍ:**
- Konkrétní vendor volby (Vault vs. AWS Secrets Manager, konkrétní WAF)
- UI/UX design detaily (barvy, layouty, user journeys)
- Detailní implementační plány (ty jsou v jednotlivých story README)

---

## 🔒 Security Pillars

### 1. Identity & Access Management

**Princip:** Keycloak je **jediný IdP** pro celou platformu. Žádný přímý přístup na interní služby bez tokenu od Keycloaku.

#### Keycloak jako Sole IdP
- **Realms:**
  - `admin` realm - globální administrace + všechny tenanty (centralizovaný provoz)
  - Každý tenant = vlastní subdoména, ale **ne** vlastní realm (izolace přes claims, ne realms)
  
- **Role Model:**

  | Role Type | Role Name | Scope | Permissions | Use Case |
  |-----------|-----------|-------|-------------|----------|
  | **Platform** | `CORE_ADMIN` | Global | Full platform access, systém config, user mgmt across tenants | DevOps, platform admin |
  | **Platform** | `CORE_AUDITOR` | Global (read-only) | Audit logs, compliance reports, cross-tenant monitoring | Compliance officer |
  | **Platform** | `CORE_SUPPORT` | Global (limited) | Read user data (any tenant), no write, no config | Customer support |
  | **Platform** | `INTEGRATION_ADMIN` | Global | Manage n8n workflows, API keys, service accounts | Integration specialist |
  | **Tenant** | `TENANT_ADMIN` | Tenant-scoped | Tenant config, user mgmt (own tenant only), billing | Organization admin |
  | **Tenant** | `TENANT_USER` | Tenant-scoped | Read/write data (own tenant), execute workflows, upload docs | End user |
  | **Service** | `SERVICE_ACCOUNT` | Service-scoped | API access (specific service, limited scope) | Backend, n8n, AI/MCP |

- **SSO Across Components:**
  - ✅ Frontend (React) → Authorization Code Flow + PKCE
  - ✅ Backend BFF (Spring Boot) → Resource Server (JWT validation)
  - ✅ Admin FE → Same flow as frontend, role check `CORE_ADMIN`
  - ✅ n8n → Reverse proxy autentizace přes Keycloak (OAuth2 Proxy nebo custom middleware) + dedicated client
  - ✅ Grafana (optional) → OAuth2 plugin, ale **BEZ tight-coupling** (standalone fallback možný)

- **Požadavek:** Žádný direct access na DB, Kafka, Redis, Loki, MinIO bez validního Keycloak tokenu. Service accounts pro backend-to-backend komunikaci (Client Credentials flow).

#### Implementace (odkazy na EPICy)
- **EPIC-007:** Keycloak deployment s SSL, realm config, service account setup
- **EPIC-011:** n8n používá service account, OAuth2 proxy konfigurace
- **EPIC-016:** AI/MCP má service account, ne user credentials

#### Outcomes
- [ ] Keycloak realm `admin` nakonfigurován s definovanými rolemi (platform + tenant)
- [ ] Všechny frontend aplikace používají Authorization Code Flow + PKCE
- [ ] Backend validuje JWT, extrahuje role, no direct DB access bez tokenu
- [ ] n8n je za reverse proxy s Keycloak auth
- [ ] Service accounts pro všechny backend-to-backend integrace

---

### 2. Multitenancy & Isolation

**Princip:** Tenant = subdoména + `tenant_id` claim v JWT + data prefix/namespace. **Zero cross-tenant data leak.**

#### Tenant Architecture
- **Identifikace tenanta:**
  - Subdoména: `tenant-123.core-platform.local` → `tenant_id=tenant-123`
  - JWT claim: `tenant_id` (povinný pro všechny user requesty, ne service accounts)
  - Data namespace: DB tabulky s `tenant_id` sloupcem, S3 prefix `tenant-123/`, Loki label `{tenant="tenant-123"}`

- **Tenant Guard (Centralizovaný Filter/Interceptor):**
  - Kontrola `tenant_id` na **všech BE boundaries:**
    - REST API endpoints
    - Kafka message consumption
    - Loki query execution
    - n8n workflow callbacks
  - Implementace: Spring Boot `@Component` + `@ControllerAdvice` nebo servlet filter
  - Testováno: E2E test "User z tenant-A nesmí vidět data tenant-B"

#### Mandatory Tenant Checks

| Feature | Tenant Check Required | Implementation |
|---------|----------------------|----------------|
| **Metamodel API** | ✅ | JPA filter `@FilterDef`, `WHERE tenant_id = :tenantId` |
| **Workflow Execution** | ✅ | Workflow instance tagged `tenant_id`, execution context izolovaný |
| **DMS (Documents)** | ✅ | S3 bucket prefix `tenant-123/documents/{file_id}` |
| **Loki Logs** | ✅ | UI filtruje `{tenant="tenant-123"}`, API vrací jen own tenant logs |
| **Grafana Dashboards** | ✅ | Data source variable `$tenant_id`, query filtered |
| **n8n Workflows** | ✅ | Workflow tagged `tenant_id`, nodes validate tenant ownership |
| **Modular Plugins** | ✅ | Plugin registry per tenant, shared code bez cross-tenant side effects |

#### Separace Logů, Auditů, Reportingu
- **Loki:** Label `tenant={tenant_id}` na všech logách, UI query vždy s tenant filter
- **Audit:** Audit events tagged s `tenant_id` + `user_id`, no global audit across tenants
- **Reporting:** Cube.js queries s `tenant_id` filter, dashboards scoped per tenant

#### Implementace (odkazy na EPICy)
- **EPIC-002:** E2E testy zahrnují tenant isolation scenarios (negative tests)
- **EPIC-005:** Metamodel Studio respektuje tenant scope v UI i API
- **EPIC-006:** Workflow Engine izoluje execution context per tenant
- **EPIC-008:** DMS používá tenant-scoped S3 prefixes + metadata
- **EPIC-011:** n8n workflows tagged `tenant_id`, nodes validate ownership
- **EPIC-017:** Modular plugins registered per tenant

#### Outcomes
- [ ] Centralizovaný Tenant Guard implementován a aktivní (Spring filter/interceptor)
- [ ] Všechny DB entity mají `tenant_id` sloupec + JPA filter
- [ ] S3 buckets používají tenant prefixes
- [ ] Loki logs tagged `tenant={tenant_id}`
- [ ] E2E test: cross-tenant isolation verified (403 Forbidden při pokusu o access)
- [ ] Audit: každý cross-tenant attempt logován do Loki

---

### 3. Secrets & Certificates

**Princip:** Všechny credentials (DB passwords, API keys, JWT signing keys, M365/Google/n8n connectors) **mimo GIT, načítané z env/secret store, rotované, auditované**. TLS všude.

#### Requirements (EPIC-000 definuje, EPIC-012 implementuje)

**EPIC-000 říká "co musí být":**
- ❌ NIKDY v Gitu: `.env`, SSL private keys, API keys, DB passwords
- ❌ NIKDY hardcoded v kódu: `password="admin123"`, `apiKey="sk-..."`
- ✅ VŽDY z env vars nebo secret store (Vault, AWS Secrets Manager, atd.)
- ✅ Rotace definovaná: DB passwords (90 dní), JWT signing keys (180 dní), API keys (on compromise)
- ✅ Audit: kdo kdy přistoupil k jakému secretu (Vault audit log)

**EPIC-012 dodává řešení:**
- Vault deployment (dev/staging/prod)
- AppRole auth pro backend
- Secret paths: `secret/data/database`, `secret/data/keycloak`, `secret/data/integrations`
- Rotation policies + automated rotation (kde možné)

#### Certificates & TLS
- **Development/Local:**
  - Self-signed certifikáty OK (generované pomocí `docker/ssl/generate-ssl.sh`)
  - Wildcard cert `*.core-platform.local`
  
- **Production:**
  - ✅ Důvěryhodná CA (Let's Encrypt nebo podniková CA)
  - ✅ Automatizovaná obnova (Certbot, cert-manager)
  - ✅ HTTPS all the way: Nginx front door, backend-to-Keycloak, backend-to-Kafka (optional SASL_SSL)

- **TLS Enforcement:**
  - Nginx: `ssl_protocols TLSv1.2 TLSv1.3;`
  - Backend: Spring Boot SSL bundle pro Keycloak komunikaci
  - Kafka: SASL_SSL pro produkci (dev může být PLAINTEXT)

#### Secret Categories & Rotation

| Category | Examples | Rotation Period | Owner |
|----------|----------|-----------------|-------|
| **Database** | `POSTGRES_PASSWORD`, `REDIS_PASSWORD` | 90 dní | DevOps |
| **Keycloak** | `KEYCLOAK_ADMIN_PASSWORD`, `OIDC_CLIENT_SECRET` | 180 dní | Security team |
| **External APIs** | `OPENAI_API_KEY`, `M365_CLIENT_SECRET`, `STRIPE_SECRET_KEY` | On compromise | Integration admin |
| **n8n** | `N8N_ENCRYPTION_KEY`, webhook secrets | 90 dní | Integration admin |
| **JWT Signing** | `JWT_SECRET` | 180 dní | Security team |

#### Implementace (odkazy na EPICy)
- **EPIC-007:** `.env` v `.gitignore`, SSL certifikáty generované, no hardcoded DB URLs
- **EPIC-012:** Vault deployment, secret paths, rotation policies, audit logging

#### Outcomes
- [ ] `.env` v `.gitignore`, `.env.example` jako template (bez secrets)
- [ ] Žádné plaintext secrets v Git history (TruffleHog check v CI)
- [ ] Vault nakonfigurován s AppRole auth (nebo ekvivalent)
- [ ] Backend načítá secrets z Vault při startupu
- [ ] n8n credentials uložené ve Vaultu (ne plaintext v workflows)
- [ ] SSL certifikáty: self-signed pro dev, Let's Encrypt (nebo CA) pro prod
- [ ] Rotace secrets documented + automated kde možné

---

### 4. Network & Boundary Protection

**Princip:** Nginx/API Gateway jako **jediný vstupní bod**. Všechno ostatní internal network only. Rate limiting, security headers, IP allow/deny pro admin.

#### Nginx jako Front Door
- **Všechny requesty jdou přes Nginx:**
  - Frontend static files (React build)
  - Backend API (`/api/*`)
  - Keycloak (`/realms/*`, `/admin/*`)
  - Grafana (`/grafana/*`)
  - n8n webhook endpoints (optional, pokud exposed)

- **Security Headers (Nginx config):**
  ```nginx
  add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';" always;
  add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
  add_header X-Frame-Options "DENY" always;
  add_header X-Content-Type-Options "nosniff" always;
  add_header Referrer-Policy "strict-origin-when-cross-origin" always;
  ```

- **Rate Limiting:**
  - Public endpoints (login, signup): 10 req/min per IP
  - API endpoints: 100 req/min per user (token-based)
  - Admin endpoints: 50 req/min + IP allowlist

- **IP Allow/Deny:**
  - Admin console (`/admin/*`): allowlist (VPN IPs, office IPs)
  - Public frontend: no IP restrictions

#### Internal Network Isolation
- ❌ **ZAKÁZÁNO zvenku (direct access):**
  - PostgreSQL (port 5432) - pouze Docker internal network
  - Redis (port 6379) - pouze internal
  - Kafka (port 9092) - pouze internal
  - MinIO (port 9000) - pouze internal, S3 API přes backend proxy
  - Loki (port 3100) - pouze internal, UI přes Grafana

- ✅ **Povoleno pouze přes Nginx/Backend:**
  - Frontend → Nginx → Backend API
  - Backend → DB/Redis/Kafka (internal network)
  - Loki UI → Grafana datasource (internal HTTP)

#### Implementace (odkazy na EPICy)
- **EPIC-007:** Nginx config, SSL termination, upstream definitions, security headers
- **EPIC-003:** Grafana datasource pro Loki (internal URL)

#### Outcomes
- [ ] Nginx jediný exposed port (80/443)
- [ ] DB, Redis, Kafka, MinIO, Loki ne exposed na host (docker-compose `expose` ne `ports`)
- [ ] Security headers v Nginx config
- [ ] Rate limiting active na public endpoints
- [ ] IP allowlist pro admin console
- [ ] E2E test: direct DB access z venku fails (connection refused)

---

### 5. Logging, Auditing & Observability

**Princip:** Všechny kritické akce **auditovatelné**. Strukturované logy → Loki. Security events filtrovatelné per tenant + user.

#### Audit Requirements (Co Musí Být Logováno)
- **User Actions:**
  - Login (success, failed attempts, lockout)
  - Role changes (add/remove role, permission change)
  - Tenant management (create tenant, suspend, delete)
  
- **Workflow Actions:**
  - Workflow execution (start, stop, error)
  - Metamodel changes (create, update, delete schema)
  
- **DMS Operations:**
  - File upload, download, delete
  - Share/unshare, permission change
  
- **Configuration Changes:**
  - System config update (admin)
  - Integration config (API keys, webhooks)
  
- **n8n Integrations:**
  - Workflow create/update/delete
  - External API calls (M365, Google, Jira)

#### Structured Logs → Loki
- **Format:** JSON (Logback, Winston)
- **Mandatory Fields:**
  ```json
  {
    "timestamp": "2025-11-09T14:23:45Z",
    "level": "INFO",
    "service": "backend",
    "tenant_id": "tenant-123",
    "user_id": "user-456",
    "action": "workflow_executed",
    "workflow_id": "wf-789",
    "result": "success",
    "duration_ms": 234
  }
  ```

- **Loki Labels:**
  - `{app="backend", environment="prod", tenant="tenant-123"}`
  - Umožňuje query: `{app="backend", tenant="tenant-123"} |= "ERROR"`

#### Security Events (Filtrovatelné)
- **Per Tenant:** `{tenant="tenant-123", action="login_failed"}`
- **Per User:** `{user_id="user-456", action="role_changed"}`
- **Cross-Tenant Attempts:** `{action="cross_tenant_access_denied"}` → trigger alert

#### Monitoring & Alerts (Odkaz na EPIC-003)
- **EPIC-000 říká:** Security events musí být monitorovatelné a alertovatelné
- **EPIC-003 implementuje:** Grafana dashboards, Prometheus alerts, PagerDuty integrace

**Příklad alert:** "10+ failed login attempts za 5 minut → notify security team"

#### Implementace (odkazy na EPICy)
- **EPIC-003:** Loki deployment, Grafana datasource, alert rules, dashboards
- **EPIC-007:** Logback config (backend), Winston config (frontend), Loki appender

#### Outcomes
- [ ] Všechny kritické akce logované do Loki (JSON format)
- [ ] Loki labels obsahují `tenant`, `user_id`, `action`
- [ ] Grafana dashboard pro security events (login fails, cross-tenant attempts, role changes)
- [ ] Alert rule: >10 failed logins za 5 min → notify
- [ ] E2E test: failed login je vidět v Loki s `action="login_failed"`

---

### 6. Secure Development & CI/CD

**Princip:** Security checks **v CI pipeline, blokující**, ne optional. No high/critical CVEs, no plaintext secrets v repo.

#### Mandatory CI Checks

| Check Type | Tool | Blocker | What It Catches |
|------------|------|---------|-----------------|
| **SAST** | CodeQL (GitHub) nebo SpotBugs | ✅ | SQL injection, XSS, unsafe deserialization |
| **Dependency Scanning** | OWASP Dependency-Check, `npm audit`, `osv-scanner` | ✅ | Known CVEs v dependencies |
| **Container Scanning** | Trivy, Grype | ✅ | Vulnerable base images, OS packages |
| **Secrets Scanning** | TruffleHog, GitLeaks | ✅ | API keys, passwords, private keys v Git |
| **Lint + Format** | ESLint, Prettier (FE), Checkstyle (BE) | ⚠️ | Code style violations (warning, ne blocker) |

#### Blocking Conditions
- ❌ **CI FAIL pokud:**
  - High nebo Critical CVE v dependency
  - Plaintext secret detected v Git
  - SAST najde SQL injection, XSS, nebo unsafe deserializace
  - Container image má critical vulnerability
  
- ⚠️ **CI WARNING (ne fail) pokud:**
  - Medium CVE (review required, ale ne auto-block)
  - Code style violation (ESLint)

#### E2E Security Tests (Odkaz na EPIC-002)
- **EPIC-000 požaduje:** Bezpečnostní scénáře v E2E testech
- **EPIC-002 implementuje:** Story E2E17-security-negative-scenarios
  - Auth bypass attempts (401, 403 expected)
  - RBAC violations (user bez role nesmí access admin endpoint)
  - Multi-tenant isolation (user z tenant-A nesmí vidět data tenant-B)
  - CSRF protection (missing token → 403)

#### Implementace (odkazy na EPICy)
- **EPIC-002:** E2E security scenarios (E2E17 story)
- **EPIC-007:** CI pipeline definice (GitHub Actions workflow)

#### Outcomes
- [ ] GitHub Actions workflow s SAST, dependency scan, container scan, secrets scan
- [ ] CI fails pokud high/critical CVE detected
- [ ] TruffleHog pre-commit hook (optional, recommended)
- [ ] E2E security tests (E2E17) v rámci post-deploy testu
- [ ] Security scan report v každém PR (GitHub Security tab)

---

### 7. Integration Security (n8n, AI/MCP, External Connectors)

**Princip:** n8n za reverse proxy, auth přes Keycloak. AI/MCP bez production secrets/PII bez anonymizace. External connectors přes service accounts, secrets ve Vaultu.

#### n8n Security Architecture

**n8n NIKDY ne direct exposed:**
- ✅ Vždy za reverse proxy (Nginx nebo OAuth2 Proxy)
- ✅ Autentizace přes Keycloak/OpenID
  - OAuth2 Proxy (`oauth2-proxy`) před n8n UI
  - n8n API calls: `Authorization: Bearer {keycloak_token}`
  
- ✅ Limited internal API access:
  - n8n může volat backend API jen přes BFF (ne direct DB access)
  - Backend ověří token, extrahuje `tenant_id`, aplikuje tenant guard
  
- ✅ Service account pro n8n:
  - Keycloak client `n8n-service-account` (Client Credentials flow)
  - Scope: `workflow:execute`, `api:read`, `api:write` (limited, ne admin)
  
- ❌ Zakázáno:
  - n8n direct access na PostgreSQL
  - n8n execute arbitrary SQL
  - n8n workflows bez tenant tagging

#### AI/MCP Security

**AI Gateway / Policy Layer (Princip):**
- **EPIC-000 říká:** Všechny AI requesty musí jít přes "AI Gateway" nebo policy layer
- **EPIC-016 implementuje:** Konkrétní implementace (proxy, rate limiting, PII detection)

**Requirements:**
- ❌ NIKDY production secrets do AI bez explicitní anonymizace:
  - API keys, DB passwords, user emails → anonymizované nebo redacted
  - PII (jména, adresy, čísla karet) → detekované a blokovány
  
- ✅ AI Gateway kontroluje:
  - Rate limiting (10 req/min per user)
  - Allow-list pro AI integrations (OpenAI, Claude, local LLM)
  - PII detection (regex, NER model)
  - Audit log: kdo kdy volal AI s jakým promptem
  
- ✅ Service account pro AI/MCP:
  - Keycloak client `ai-mcp-service-account`
  - Scope: `ai:query`, `mcp:read` (limited)

#### External Connectors (M365, Google, Jira, Stripe, ...)

**Všechny external integrace přes service accounts:**
- ✅ M365: Azure AD service principal, client secret ve Vaultu
- ✅ Google Workspace: Service account JSON key ve Vaultu
- ✅ Jira: API token ve Vaultu
- ✅ Stripe: Secret key ve Vaultu

**Audit všech volání:**
- Loki log: `{action="external_api_call", service="m365", user_id="..."}`
- Obsahuje: endpoint, method, response status, duration

#### Implementace (odkazy na EPICy)
- **EPIC-011:** n8n deployment za OAuth2 Proxy, service account config, Vault credentials
- **EPIC-012:** Vault paths pro external connector secrets
- **EPIC-016:** AI Gateway implementace, PII detection, rate limiting

#### Outcomes
- [ ] n8n za OAuth2 Proxy, autentizace přes Keycloak
- [ ] n8n service account v Keycloaku (Client Credentials flow)
- [ ] n8n credentials (M365, Google, Jira) uložené ve Vaultu (ne plaintext)
- [ ] AI Gateway/policy layer implementován (rate limiting, PII detection)
- [ ] AI service account v Keycloaku
- [ ] Audit log: všechny external API calls logované do Loki
- [ ] E2E test: n8n workflow s Vault credential úspěšně vykoná external API call

---

### 8. Threat Model & OWASP Alignment

**Princip:** Cíl je **OWASP Top 10, CIS Benchmarks, Zero Trust foundation**. Explicitní coverage hlavních attack vectors.

#### OWASP Top 10 Coverage

| OWASP Risk | Protection Mechanism | Implementation |
|------------|---------------------|----------------|
| **A01: Broken Access Control** | Keycloak RBAC, tenant guard, `@PreAuthorize` | EPIC-000 Pillar 1, 2 |
| **A02: Cryptographic Failures** | TLS everywhere, Vault pro secrets, bcrypt passwords | EPIC-000 Pillar 3, 4 |
| **A03: Injection** | Prepared statements (JPA), no string SQL, input validation | Backend coding standards |
| **A04: Insecure Design** | Threat modeling, security reviews v PR | EPIC-000 + EPIC-002 |
| **A05: Security Misconfiguration** | `.env` v `.gitignore`, no default passwords, automated config checks | EPIC-007, EPIC-012 |
| **A06: Vulnerable Components** | Dependency scanning (OWASP Dep-Check, osv), CI blocks high CVEs | EPIC-000 Pillar 6 |
| **A07: Identification & Auth Failures** | Keycloak, MFA (optional), account lockout, no weak passwords | EPIC-000 Pillar 1 |
| **A08: Software & Data Integrity** | Container signing (optional), Git commit signing (optional) | Future EPIC |
| **A09: Logging & Monitoring Failures** | Structured logs → Loki, audit trail, alerts | EPIC-000 Pillar 5, EPIC-003 |
| **A10: SSRF** | URL allowlist, no user-controlled URLs v backend fetch | Backend coding standards |

#### Specific Attack Vector Guards

**Injection Guard:**
- ✅ Backend: JPA/Hibernate (prepared statements), no `EntityManager.createNativeQuery(userInput)`
- ✅ Frontend: No `eval()`, no `dangerouslySetInnerHTML` s user input
- ✅ SQL: Všechny queries přes JPA Criteria API nebo `@Query` s parametry

**XSS Guard:**
- ✅ CSP header: `script-src 'self'` (no inline scripts kromě whitelistu)
- ✅ React: automatický escaping (default behavior)
- ✅ No `innerHTML` s user input

**CSRF Guard:**
- ✅ Same-Site cookies: `SameSite=Strict` pro session cookies
- ✅ CSRF tokens v forms (Spring Security default)
- ✅ Double-submit cookie pattern (optional)

**Secure File Upload (DMS):**
- ✅ Content-Type validation (ne jen extension check)
- ✅ File size limit (např. 100MB)
- ✅ Antivirus scan (ClamAV nebo cloud service) před uložením do S3
- ✅ S3 bucket public access DISABLED

**Rate Limiting + Brute-Force Protection:**
- ✅ Login endpoint: max 5 failed attempts → account lockout 15 min
- ✅ Nginx rate limiting: 10 req/min na `/auth/login`
- ✅ API rate limiting: 100 req/min per user token

#### CIS Benchmarks & Zero Trust

**CIS Docker Benchmark:**
- ✅ Non-root users v containers
- ✅ Read-only root filesystems (kde možné)
- ✅ Minimal base images (alpine, distroless)
- ✅ No privileged containers

**Zero Trust Principles:**
- ✅ "Never trust, always verify" - každý request autentizovaný + autorizovaný
- ✅ Least privilege - service accounts mají jen nutné scope
- ✅ Micro-segmentation - internal network isolation (DB, Kafka ne exposed)

#### Implementace (odkazy na EPICy)
- **EPIC-002:** Security negative tests (injection, XSS, CSRF attempts)
- **EPIC-007:** CIS Docker benchmarks v Dockerfile
- **EPIC-008:** DMS antivirus scan, file type validation

#### Outcomes
- [ ] OWASP Top 10 coverage documented v README
- [ ] Prepared statements všude (no string SQL from UI)
- [ ] CSP header active, no inline scripts
- [ ] CSRF tokens v forms
- [ ] DMS: antivirus scan active, content-type validation
- [ ] Rate limiting na login endpoint (max 5 attempts)
- [ ] CIS Docker benchmark compliance (non-root users, minimal images)
- [ ] E2E test: injection attempt → 400 Bad Request, XSS attempt → escaped output

---

## � Deliverables / Definition of Done

**EPIC-000 je splněný, když:**

1. **Keycloak Configuration + Role Model**
   - [ ] Keycloak realm `admin` nakonfigurován
   - [ ] Role model documented: platform roles (CORE_ADMIN, CORE_AUDITOR, CORE_SUPPORT) + tenant roles (TENANT_ADMIN, TENANT_USER)
   - [ ] Service accounts vytvořené: `backend-service-account`, `n8n-service-account`, `ai-mcp-service-account`

2. **Security Policies Documented**
   - [ ] Tento README.md je single source of truth pro security baseline
   - [ ] Coding standards zahrnují security best practices (prepared statements, no eval, input validation)
   - [ ] PR checklist obsahuje security review položky

3. **CI Pipeline s Security Checks**
   - [ ] GitHub Actions workflow s SAST, dependency scan, container scan, secrets scan
   - [ ] CI blokuje PR při high/critical CVE nebo detected secret
   - [ ] Security scan report v každém PR (GitHub Security tab nebo Markdown report)

4. **Audit Logs v Klíčových Bodech**
   - [ ] Login (success, failed), role changes, tenant management logované do Loki
   - [ ] Workflow execution, DMS operations, config changes logované
   - [ ] Cross-tenant access attempts logované + alert triggered

5. **Multi-Tenant Boundary Tests Verified**
   - [ ] E2E test: user z tenant-A nesmí vidět data tenant-B (403 Forbidden)
   - [ ] E2E test: API s `tenant_id=tenant-B` v JWT vrací 403 pokud request z tenant-A
   - [ ] Tenant Guard filter active a testovaný

6. **Secrets Management Operational**
   - [ ] Vault deployed (dev/staging/prod) nebo ekvivalent
   - [ ] Backend načítá DB credentials, Keycloak secrets, external API keys z Vaultu
   - [ ] `.env` v `.gitignore`, žádné plaintext secrets v Git history

7. **Network Security Active**
   - [ ] Nginx jediný exposed port, DB/Redis/Kafka/Loki internal only
   - [ ] Security headers v Nginx config (CSP, HSTS, X-Frame-Options)
   - [ ] Rate limiting active na public endpoints

8. **Integration Security Enforced**
   - [ ] n8n za OAuth2 Proxy, autentizace přes Keycloak
   - [ ] AI Gateway/policy layer implementován (rate limiting, PII detection)
   - [ ] External connectors (M365, Google, Jira) používají service accounts + Vault credentials

9. **OWASP Top 10 Coverage**
   - [ ] Injection guard (prepared statements), XSS guard (CSP), CSRF guard (tokens)
   - [ ] Secure file upload (antivirus, content-type validation)
   - [ ] Brute-force protection (rate limiting, lockout)

---

## 🚫 Non-Goals

**EPIC-000 NEŘEŠÍ:**

1. **Konkrétní Vendor Volby:**
   - Ne: "Musíme použít HashiCorp Vault" (může být AWS Secrets Manager, Azure Key Vault, atd.)
   - Ano: "Musíme mít secrets storage s rotací a audit logem"

2. **UI/UX Design Detaily:**
   - Ne: "Login form má mít modrou tlačítko 48px high"
   - Ano: "Login form musí podporovat Keycloak Authorization Code Flow + PKCE"

3. **Implementační Plány Jednotlivých EPICů:**
   - Ne: "EPIC-012 Vault má 15 stories, tady jsou všechny"
   - Ano: "EPIC-012 implementuje secrets storage requirements z EPIC-000 Pillar 3"

4. **Produkční Vendor-Specific Konfigurace:**
   - Ne: "Let's Encrypt certbot config pro produkční cluster"
   - Ano: "Produkce musí mít důvěryhodnou CA, self-signed jen pro dev"

5. **Detailní Compliance Audity:**
   - Ne: "SOC 2 Type II audit report template"
   - Ano: "Audit logs musí být dostupné pro compliance review"

---

## 📚 References

- **OWASP Top 10 (2021):** https://owasp.org/Top10/
- **CIS Docker Benchmark:** https://www.cisecurity.org/benchmark/docker
- **Keycloak Documentation:** https://www.keycloak.org/documentation
- **NIST Cybersecurity Framework:** https://www.nist.gov/cyberframework
- **Zero Trust Architecture (NIST SP 800-207):** https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-207.pdf

---

**Last Updated:** 9. listopadu 2025  
**Owned by:** Security + DevOps + Platform Team  
**Review Cycle:** Quarterly (každé 3 měsíce security review + update)
