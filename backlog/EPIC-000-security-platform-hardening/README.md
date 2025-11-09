# EPIC-000: Security & Access Control Platform Hardening

**Status:** 🔴 **MASTER REFERENCE** (Continuous)  
**Priority:** P0 (SECURITY CRITICAL)  
**Owner:** Security + DevOps + Platform Team  
**Created:** 9. listopadu 2025  
**Updated:** 9. listopadu 2025 (Tenant Model Clarification - realm-per-tenant DEFINITIVELY)

---

## 🎯 Purpose

**EPIC-000 je jediný závazný "Security Master Contract" pro celou core-platform.** Definuje bezpečnostní principy, výstupy a kontrolní mechanismy, které VŠECHNY ostatní EPICy musí dodržovat. Nejde o implementační EPIC – konkrétní řešení jsou v navazujících EPICech. Tohle je **rámec a baseline**, proti kterému se všechno měří.

### Účel EPIC-000

EPIC-000 je **zastřešující bezpečnostní epic pro celou platformu Virelio/Core Platform.**

Definuje minimální bezpečnostní standardy, které musí splňovat:
- ✅ Všechny ostatní EPICy (002-017)
- ✅ Všechny moduly (core i zákaznické)
- ✅ Všechny integrační body (FE, BE, Keycloak, n8n, AI, DMS, monitoring)
- ✅ Produkční i předprodukční prostředí

**Bez splnění tohoto EPICu není povoleno:**
- ❌ Označit release jako "production-ready"
- ❌ Připojit nové integrační systémy
- ❌ Zpřístupnit službu externím tenantům

---

## ⚠️ ZÁVAZNÝ MODEL MULTITENANCY

**Core-platform závazně používá model `tenant = subdoména = Keycloak realm`.**

Dřívější úvahy o "single-realm + tenant_id claims" jako bezpečnostní boundary jsou **opuštěny a nejsou podporovány**. Tento princip je **neměnný** - jakákoliv změna vyžaduje zásadní revizi celé architektury.

---

## 🏛️ NÁŠ TENANT MODEL (NEMĚNIT)

**Core Platform používá konzistentní model:**

```
tenant = subdoména = Keycloak realm
```

### Příklady

- `admin.core-platform.local` → realm: `admin` (platform / superadmin)
- `tenant-a.core-platform.local` → realm: `tenant-a`
- `acme.core-platform.local` → realm: `acme`

### Izolace Tenantů

Izolace tenantů je zajištěna na **třech úrovních:**

1. **Keycloak realmy (PRIMÁRNÍ BEZPEČNOSTNÍ BOUNDARY):**
   - Každý tenant má vlastní realm
   - **Realm = bezpečnostní hranice** - žádné sdílené identity mezi tenanty
   - Žádné cross-realm login sessions
   - Model "všichni v jednom realm + tenant_id claim" NENÍ podporován

2. **Backend vrstva (aplikační vynucení):**
   - BFF (Backend-For-Frontend) kontroluje realm z JWT issuer
   - `tenant_id` slouží jako **stabilní identifikátor** (ne security boundary)
   - RLS (Row-Level Security) na DB úrovni s `tenant_id` sloupcem
   - Metamodel & data-scoping per realm
   - Tenant Guard (centralizovaný filter) validuje subdoména ↔ realm ↔ tenant_id konzistenci

3. **Síťová vrstva (doménová izolace):**
   - Subdoména určuje realm
   - Přihlášení FE/BE vždy proti realm-u odvozenému ze subdomény
   - Všechny komponenty ověřují konzistenci (subdoména + realm + tenant_id)

### Závaznost Modelu

**Tento model je závazný a neměnný.**

**Všechny nové komponenty MUSÍ tento model respektovat:**
- ✅ Workflow Engine - izolace per realm
- ✅ DMS (Document Management) - data per realm, žádné cross-realm přístupy
- ✅ Loki/Loki UI - logy tagované realm label, filtrování per realm
- ✅ n8n Proxy - workflow execution context vázaný na realm
- ✅ Metamodel Studio - definice entit per realm
- ✅ Monitoring (Grafana/Prometheus) - metriky per realm, dashboardy per realm
- ✅ E2E Infrastructure - test data per realm, žádné míchání

**❌ ZAKÁZÁNO:**
- Míchání dat napříč realmy bez explicitního multi-realm scope (pouze pro admin realm)
- Sdílené login session mezi tenanty
- Jakékoliv alternativní modely izolace (tenant model je POUZE realm-per-tenant)

---

## 🚨 Production Readiness Gate (Minimální Bezpečnostní Baseline)

Následující body tvoří **"hard" baseline**. Musí být splněny **PŘED nasazením do produkce:**

### 1. Identita & SSO
- ✅ Keycloak je jediný IdP pro FE, BE, admin nástroje (+ pokud je to rozumné: Grafana, n8n)
- ✅ Všechna rozhraní (GUI, API) ověřují uživatele přes JWT z Keycloaku
- ✅ Všechna JWT jsou ověřována proti správnému issuer + audience + podpisu (JWK/JWKS)
- ❌ Žádné `alg=none`, žádné weak klíče

### 2. TLS / HTTPS
- ✅ Všechen provoz zvenku běží přes HTTPS
- ✅ TLS terminace na ingress / reverse proxy
- ✅ Interní komunikace mezi kontejnery buď:
  - Běží jen v privátní síti bez přímého přístupu zvenku, nebo
  - Je chráněna (mTLS / network policy), podle prostředí
- ❌ Žádné login/API endpointy přes plain HTTP dostupné zvenku

### 3. Secrety a Konfigurace
- ❌ **ŽÁDNÉ secrety v Gitu** (`.env`, certy, klíče, hesla)
- ✅ `.env`, privátní klíče a podobné soubory jsou v `.gitignore`
- ✅ Konfigurace používá environment variables nebo secret manager (Vault/KMS)
- ✅ Připravená integrace na Vault (EPIC-012) – aby šlo secrety postupně přemigrovat

### 4. Tenant Isolation
- ✅ Každý request je jednoznačně mapovaný na tenant (z JWT / subdomény / contextu)
- ✅ Všechny BFF/API vrsty aplikují tenant filter server-side
- ❌ Není možné dotazovat nebo měnit data jiného tenantu pouze změnou ID v URL
- ✅ Audit logy obsahují tenant + uživatele + akci

### 5. Autorizace & RBAC
- ✅ Role, permissiony a scopes spravované centrálně (Keycloak / metamodel)
- ✅ Admin funkce (studio, workflow designer, DMS nastavení, integrace, monitoring) pouze pro dedikované role:
  - `CORE_PLATFORM_ADMIN`, `CORE_SECURITY_ADMIN`, `CORE_TENANT_ADMIN`
- ❌ Žádná "hardcoded" privilegia v kódu mimo definovaný model

### 6. Logging, Audit, Observabilita
- ✅ Aplikace loguje strukturovaně (JSON), včetně:
  - `correlation-id`, `tenant`, `user` (pokud dává smysl), typ operace
- ✅ Bezpečnostní a administrativní operace jsou auditovány:
  - Změny konfigurace
  - Změny rolí
  - Přístupy k citlivým datům
  - Workflow & DMS klíčové akce
- ✅ Logy směrovány do centrálního úložiště (Loki/ELK), s řízeným přístupem
- ✅ Monitoring (Prometheus/Grafana/Loki UI) má alerty pro:
  - Zvýšenou chybovost
  - Podezřelé patterny (brute force, opakované 401/403)
  - Výpadky klíčových komponent

### 7. CI/CD & Dependency Security
- ✅ Povinný dependency scanning (SCA) na všech repozitářích
- ✅ Povinný secret scanning
- ❌ Build failuje při kritických CVE nebo nalezených secretech
- ✅ Infrastructure-as-code (Docker, K8s, GitHub Actions) prochází lintem a základním security scanem
- ✅ Release pipeline má quality gates (testy, coverage, security checks)

### 8. Perimetr & Integrace
- ✅ Všechna admin a interní rozhraní chráněná (IP range, VPN, SSO, role)
- ✅ Externí integrace (n8n, webhooky, AI, konektory) běží přes bezpečné proxy / BFF
- ❌ Žádné přímé přístupy z integračních nástrojů do databází bez kontrolní vrstvy

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

### Detailní Vazby na Ostatní EPICy

**EPIC-003: Monitoring & Observability**
- Musí používat bezpečné logování, korektní práci s PII
- Řízený přístup k logům a dashboardům (tenant isolation)
- Audit trail pro security events (login fails, role changes, cross-tenant attempts)

**EPIC-005: Metamodel & Studio**
- Metamodel musí umožnit definici:
  - Datové klasifikace (PII, citlivá data, veřejná data)
  - Přístupových pravidel (role-based, tenant-scoped)
  - Auditovatelných změn modelu (kdo kdy změnil schema)
- Vše v souladu s tímto security framework

**EPIC-007: Infrastructure & Deployment**
- Deployment musí podporovat:
  - TLS terminaci (Nginx, Ingress)
  - Bezpečné nakládání se secrety (Vault, env vars)
  - Síťovou segmentaci (DB/Redis/Kafka internal only)
  - Readiness/liveness/health pro bezpečné rollouty

**EPIC-011: n8n Workflow Automation**
- n8n (nebo jiný orchestrátor) **NESMÍ:**
  - Obcházet RBAC (každý workflow má tenant + role context)
  - Přistupovat přímo k DB (pouze přes BFF/API)
  - Posílat citlivá data mimo bezpečné boundary (audit required)
- Přístup jen přes BFF/proxy s jasnými scopes

**EPIC-012: Vault Integration**
- Implementuje konkrétní mechanismus pro správu:
  - Klíčů (DB passwords, API keys, JWT signing keys)
  - Hesel (Keycloak admin, SMTP, external services)
  - Certifikátů (SSL/TLS, CA certs)
  - Rotaci (automated kde možné)
- V souladu s požadavky EPIC-000 Pillar 3

**EPIC-014: UX/UI Design System**
- UI musí respektovat bezpečnostní stavy:
  - Locky (read-only režimy)
  - Session expiry (automatický logout)
  - Error states (bez internal stack traces)
- Neukazovat interní IDs a citlivá data tam, kde to není nutné

**EPIC-017: Modular Architecture**
- Každý modul (core i zákaznický) **MUSÍ:**
  - Používat centrální autentizaci/autorizaci (Keycloak)
  - Respektovat tenant izolaci (tenant guard)
  - Respektovat audit logging (structured logs)
- **NESMÍ:**
  - Zavádět vlastní "login" mechanismus
  - Obcházet RBAC přes direct DB access
  - Sdílet data mezi tenanty bez explicit kontroly

#### Zpřísněná Pravidla pro Moduly a Rozšíření (EPIC-017)

**Žádný modul (projekt, plugin, rozšíření) NESMÍ:**
- ❌ Zavádět vlastní login / autentizační mechanismus (pouze Keycloak)
- ❌ Obcházet Keycloak / centrální RBAC (všechny role přes Keycloak)
- ❌ Obcházet Tenant Guard (musí respektovat `tenant_id` z tokenu)
- ❌ Přistupovat přímo na DB jiného modulu nebo systémové tabulky (pouze přes API/BFF)
- ❌ Ukládat svoje secrety "po svém" (musí použít stejný secret management model - Vault/EPIC-012)

**Moduly SMĚJÍ přinést pouze:**
- ✅ Vlastní obrazovky (FE komponenty v rámci Design System)
- ✅ Workflow definice (v rámci Workflow Engine)
- ✅ Integrační kroky (n8n nodes, API connectors)
- ✅ Entitní typy (metamodel extensions)
- ✅ Konektory (external API integrations)
- ✅ **VŠE v souladu s centrálním security modelem** (Keycloak auth, tenant guard, audit logging)

**Tento dokument NEŘEŠÍ:**
- Konkrétní vendor volby (Vault vs. AWS Secrets Manager, konkrétní WAF)
- UI/UX design detaily (barvy, layouty, user journeys)
- Detailní implementační plány (ty jsou v jednotlivých story README)

---

## 🔒 Security Pillars

### 1. Identity & Access Management

**Princip:** Keycloak je **jediný IdP** pro celou platformu. Žádný přímý přístup na interní služby bez tokenu od Keycloaku.

#### Keycloak jako Sole IdP

**Architektura Keycloak:**

- **`admin` realm** – slouží **pouze** pro správu platformy, globální administrátory, support a systémové integrace (`CORE_ADMIN_*`, `CORE_SUPPORT_*`, `CORE_AUDITOR` apod.)

- **Každý tenant = vlastní realm.** Platí **invariant:**
  - **subdoména tenanta = název / identifikátor jeho realm-u**
  - Příklad: `tenant-a.core-platform.local` → realm `tenant-a`
  - Příklad: `acme.core-platform.local` → realm `acme`
  
- **Frontend i backend vždy určují cílový realm z host/subdomény.** Login **nikdy nemíchá tenanty** do jednoho realm-u.

- **Model "všichni tenantí v jednom realm-u + tenant_id claim" není podporovaný jako bezpečnostní boundary.**
  - `tenant_id` claim lze používat **jen jako stabilní identifikátor** napříč systémy (pro reporting, integrace, tracking)
  - **NE jako náhradu** realm izolace
  - Primární bezpečnostní boundary zůstává **realm-per-tenant**
  
- **Role Model:**

  **Role jsou vždy definované per realm.**

  - **Nemusíme přidávat `tenant_id` do názvu role**, protože realm už je tenant boundary.
  - **Příklady:**
    - V realmu `tenant-a`: `TENANT_ADMIN`, `USER`, `FINANCE_MANAGER`
    - V realmu `acme`: `TENANT_ADMIN`, `USER`, `PROJECT_LEAD`
    - V realmu `admin`: `CORE_PLATFORM_ADMIN`, `CORE_AUDITOR`, `CORE_SUPPORT`
  - **Žádné sdílené globální role napříč realmy** (mimo admin realm s explicitními cross-realm pravomocemi)

  | Role Type | Role Name | Scope | Permissions | Use Case |
  |-----------|-----------|-------|-------------|----------|
  | **Platform** | `CORE_PLATFORM_ADMIN` | Cross-realm (pouze admin) | Full platform access, systém config, user mgmt across realms | DevOps, platform admin |
  | **Platform** | `CORE_AUDITOR` | Cross-realm read-only | Audit logs, compliance reports, cross-realm monitoring | Compliance officer |
  | **Platform** | `CORE_SUPPORT` | Cross-realm limited | Read user data (any realm), no write, no config | Customer support |
  | **Platform** | `INTEGRATION_ADMIN` | Cross-realm | Manage n8n workflows, API keys, service accounts | Integration specialist |
  | **Tenant** | `TENANT_ADMIN` | Realm-scoped | Tenant config, user mgmt (own realm only), billing | Organization admin |
  | **Tenant** | `USER` | Realm-scoped | Read/write data (own realm), execute workflows, upload docs | End user |
  | **Service** | `SERVICE_ACCOUNT` | Service-scoped | API access (specific service, limited scope) | Backend, n8n, AI/MCP |

  **Provisioning uživatelů:**
  - Probíhá **vždy do konkrétního realmu** (tenanta)
  - Automatizační skripty / n8n / onboarding flow **NESMÍ míchat realmy**
  - Každý nový tenant = nový Keycloak realm + subdoména + base roles (TENANT_ADMIN, USER)

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

**Princip multitenance:**

- **Tenant = subdoména = Keycloak realm**
- **Každý request je mapován na realm podle host/subdomény**
- **Všechny backendové služby používají `tenant_id`** (z realm + claims) **pro izolace v databázi, message brokerech, úložištích a logování**
- **`tenant_id` je druhá vrstva izolace a trasovatelnosti** (DB schema/column, Loki label, MinIO prefix), **ne hlavní bezpečnostní hranice** – tou zůstává **realm-per-tenant**

**Zero cross-tenant data leak.**

#### Tenant Architecture

**Identifikace tenanta (3 kontrolní body):**

1. **Subdoména (doménová vrstva):**
   - `tenant-a.core-platform.local` → tenant: `tenant-a`
   - `acme.core-platform.local` → tenant: `acme`

2. **Keycloak Realm (identitní vrstva - PRIMÁRNÍ BOUNDARY):**
   - JWT obsahuje `iss` (issuer): `https://tenant-a.core-platform.local/realms/tenant-a`
   - Každý tenant má vlastní realm
   - **Realm JE bezpečnostní hranice** - žádné sdílené identity mezi realmy

3. **Data Namespace (aplikační vrstva):**
   - `tenant_id` / `realm` sloupec v DB tabulkách
     - **Slouží jako stabilní identifikátor** (např. pro integrační systémy, reporting)
     - **NENÍ primární bezpečnostní boundary** (ta je realm!)
     - Redundantní kontrola: backend může validovat `tenant_id` ↔ realm konzistenci
   - S3 prefix: `tenant-a/documents/`
   - Loki label: `{realm="tenant-a"}`
   - Metamodel scoping per realm

**Backend ověřuje konzistenci:**
- Subdoména z `Host` header
- Realm z JWT `iss` claim
- Pokud subdoména ≠ realm → **request odmítnut (401/403)**

**Tenant Guard (Centralizovaný Filter/Interceptor):**
- Kontrola realm/tenant na **všech BE boundaries:**
  - REST API endpoints
  - Kafka message consumption
  - Loki query execution
  - n8n workflow callbacks
- Implementace: Spring Boot `@Component` + `@ControllerAdvice` nebo servlet filter
- Testováno: E2E test "User z realm tenant-a nesmí vidět data realm acme"

#### Mandatory Tenant Checks

| Feature | Tenant Check Required | Implementation |
|---------|----------------------|----------------|
| **Metamodel API** | ✅ | JPA filter `@FilterDef`, `WHERE realm = :realm` nebo RLS policies |
| **Workflow Execution** | ✅ | Workflow instance tagged `realm`, execution context izolovaný |
| **DMS (Documents)** | ✅ | S3 bucket prefix `tenant-a/documents/{file_id}` |
| **Loki Logs** | ✅ | UI filtruje `{realm="tenant-a"}`, API vrací jen own realm logs |
| **Grafana Dashboards** | ✅ | Data source variable `$realm`, query filtered |
| **n8n Workflows** | ✅ | Workflow tagged `realm`, nodes validate realm ownership |
| **Modular Plugins** | ✅ | Plugin registry per realm, shared code bez cross-realm side effects |

#### Separace Logů, Auditů, Reportingu
- **Loki:** Label `realm={realm_name}` na všech logách, UI query vždy s realm filter
- **Audit:** Audit events tagged s `realm` + `user_id`, no global audit across realms (pouze admin realm)
- **Reporting:** Cube.js queries s `realm` filter, dashboards scoped per realm

#### Cross-Tenant Přístup (Platformní Funkce)

**NENÍ povolen implicitně. Pouze explicitně pro platformní administraci:**

- **admin realm** má speciální role:
  - `CORE_PLATFORM_ADMIN` - může číst/zapisovat cross-realm (debugging, support)
  - `CORE_AUDITOR` - může číst cross-realm (compliance, security audit)
  - `CORE_SUPPORT` - může číst cross-realm (customer support, limited scope)

**Všechny cross-realm operace:**
- ✅ Musí být explicitně auditovány (kdo, kdy, který realm, proč)
- ✅ Musí projít přes dedikovaný API endpoint (ne implicitní bypass)
- ✅ Jsou logované do Loki s `cross_realm_access=true` flag

#### Implementace (odkazy na EPICy)
- **EPIC-002:** E2E testy zahrnují realm isolation scenarios (negative tests)
- **EPIC-005:** Metamodel Studio respektuje realm scope v UI i API
- **EPIC-006:** Workflow Engine izoluje execution context per realm
- **EPIC-007:** Automatizace pro zakládání nových realmů + subdomain routing
- **EPIC-008:** DMS používá realm-scoped S3 prefixes + metadata
- **EPIC-011:** n8n workflows tagged `realm`, nodes validate ownership
- **EPIC-017:** Modular plugins registered per realm

#### Outcomes
- [ ] Centralizovaný Tenant Guard implementován a aktivní (Spring filter/interceptor)
- [ ] Všechny requesty ověřují konzistenci: subdoména ↔ realm ↔ JWT issuer
- [ ] DB entities používají `realm` sloupec nebo RLS policies
- [ ] S3 buckets používají realm prefixes
- [ ] Loki logs tagged `realm={realm_name}`
- [ ] E2E test: cross-realm isolation verified (403 Forbidden při pokusu o access)
- [ ] Audit: každý cross-realm attempt logován do Loki
- [ ] Každý nový realm má vlastní Keycloak realm, subdoménu, base roles

---

### 2a. Tenant Validace (Security Guardrails)

**Každý request do backendu musí splnit:**

#### 1. Tenant Odvození

**TenantId je odvozen z:**
- ✅ **Subdoména** (z `Host` header)
  - Příklad: `tenant-a.core-platform.local` → tenant: `tenant-a`
- ✅ **Realm / Issuer v JWT**
  - `iss` claim: `https://tenant-a.core-platform.local/realms/tenant-a`
- ✅ **Explicitní claim** (pokud je přítomen)
  - `realm` nebo `tenant` claim v JWT (doplňkový mechanismus)

#### 2. Backend Konzistence Check

**Backend musí ověřit, že všechny 3 zdroje sedí:**

```
IF subdoména ≠ realm FROM JWT issuer:
  → REJECT request (401 Unauthorized nebo 403 Forbidden)
  → LOG security event: "Realm mismatch: subdomain=tenant-a, jwt.iss=acme"

IF explicitní claim `realm` ≠ subdoména:
  → REJECT request
  → LOG security event: "Claim/subdomain mismatch"
```

**Implementace:**
- Spring Boot filter (nejvyšší priorita)
- Validuje **před** application logic
- Loguje **každý** mismatch attempt (Loki s `security_violation=true`)

#### 3. Cross-Realm Přístup - ZAKÁZÁNO Implicitně

**NENÍ povolen nikdy implicitně.**

**Pokud existují "platformní" funkce:**
- Jsou v dedikovaném realmu **`admin`**
- Pod speciálními rolemi:
  - `CORE_PLATFORM_ADMIN` - plný cross-realm přístup (debugging, emergency operations)
  - `CORE_AUDITOR` - read-only cross-realm (compliance, security audit)
  - `CORE_SUPPORT` - limited read-only cross-realm (customer support)

**Každý cross-realm přístup:**
- ✅ Musí být **explicitně auditován** (kdo, kdy, který realm, akce, důvod)
- ✅ Musí projít **dedikovaným API endpointem** (ne implicitní bypass)
- ✅ Je logován do Loki s flagy: `cross_realm_access=true`, `target_realm={realm}`, `reason={reason}`

#### 4. Služby Musí Vynucovat Tenant Context

**Všechny služby (REST, streaming, Loki, DMS, workflow, n8n proxy, metamodel API) MUSÍ:**

- ✅ **Dostat tenant kontext z JWT + subdomény**
  - Parsovat `iss` claim nebo `realm` claim
  - Ověřit proti `Host` header
- ✅ **Logovat tenant jednoznačně** (pro audit)
  - Všechny logy tagged `realm={realm_name}`
  - Structured logging s `tenant`, `user_id`, `correlation_id`
- ✅ **Nikdy nezpracovat data jiného realmu pod cizím tenantem**
  - DB queries filtrované `WHERE realm = :realm`
  - S3 access omezen na `{realm}/` prefix
  - Loki queries filtrované `{realm="{realm}"}`

**Příklad validace:**

```java
// Spring Boot Filter
public class TenantGuard implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        String subdomain = extractSubdomain(req.getServerName());
        String jwtRealm = extractRealmFromJWT(req.getHeader("Authorization"));
        
        if (!subdomain.equals(jwtRealm)) {
            log.warn("Realm mismatch: subdomain={}, jwt.realm={}", subdomain, jwtRealm);
            ((HttpServletResponse) res).sendError(403, "Realm mismatch");
            return;
        }
        
        // Nastavit tenant context pro downstream services
        TenantContext.setCurrentRealm(jwtRealm);
        chain.doFilter(req, res);
    }
}
```

#### Implementace (odkazy na EPICy)
- **EPIC-002:** E2E testy zahrnují realm mismatch scenarios (401/403 expected)
- **EPIC-007:** Nginx/Ingress routing podle subdomény, validace na edge
- **Backend (všechny API):** TenantGuard filter aktivní, konzistence check

#### Outcomes
- [ ] TenantGuard filter implementován a aktivní (nejvyšší priorita v Spring chain)
- [ ] Každý request validuje: subdoména ↔ JWT realm ↔ explicitní claim
- [ ] Mismatche logovány do Loki (`security_violation=true`)
- [ ] Cross-realm přístupy pouze přes admin realm + audit log
- [ ] E2E test: mismatch realm → 403 Forbidden
- [ ] E2E test: cross-realm attempt bez CORE_PLATFORM_ADMIN → 403 Forbidden

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

**Environment-Specific Requirements:**

**DEV (Development/Local):**
- ✅ Tolerováno použití `.env` souborů, **ALE:**
  - `.env` **MUSÍ být** v `.gitignore`
  - Vzor je pouze `.env.example` **bez skutečných secretů**
  - Lokální `.env` pouze pro lokální vývojové prostředí, NIKDY ne commitnuté

**STAGE/PROD (Staging/Production):**
- ✅ **POVINNÉ:**
  - Kubernetes secrets / managed secret store / Vault (EPIC-012)
  - DB hesla, Keycloak client secrets, integrační klíče, SMTP, externí API keys atd. se **NESMÍ psát do manifestů ani do image**
  - JWT signing keys, šifrovací klíče, privátní klíče certifikátů jsou **verzované a rotačně spravované** přes secret manager
- ❌ **ZAKÁZÁNO:**
  - Plaintext secrets v Kubernetes YAML
  - Secrets v Docker image layers
  - Hardcoded credentials v application.properties/yml

**EPIC-012 Vault Integration definuje závazný způsob správy secretů pro produkční prostředí; tento EPIC stanovuje principy, EPIC-012 jejich implementaci.**

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

#### AI, LLM & Data Protection (Detailní Pravidla)

**Jakákoliv integrace AI/LLM** (MCP, asistenti, generování workflow/modelů, analýza logů) **NESMÍ** bez výslovné konfigurace a kontroly posílat:
- ❌ Produkční PII (jména, emaily, telefonní čísla, osobní identifikátory)
- ❌ Citlivá obchodní data (finance, strategie, interní know-how)
- ❌ Tenant-specific tajemství (API keys, přístupové údaje, proprietary algoritmy)
- ❌ Jakákoliv data mimo kontrolovaný boundary

**MUSÍ používat:**
- ✅ **Předzpracování:**
  - Maskování (replace PII s placeholders: `USER_123`, `EMAIL_456`)
  - Anonymizace (agregace, generalizace, k-anonymita)
  - Redaction (odstranění celých bloků citlivých dat)
  
- ✅ **Bezpečné připojení:**
  - HTTPS only (TLS 1.2+)
  - Authentication tokens (API keys v Vaultu, ne hardcoded)
  - Timeout & retry policies (avoid hanging connections)
  
- ✅ **Logování a audit:**
  - Kdo volal AI (user_id, tenant_id)
  - Co poslal (prompt hash, ne full prompt pokud citlivý)
  - Kdy a s jakým výsledkem (timestamp, status code, token count)
  
- ✅ **Centrální konfigurace:**
  - Feature flags (které AI features jsou povolené per tenant)
  - Povolené scénáře (code generation OK, document analysis requires review)
  - Schválené nástroje (OpenAI API, Claude, local Llama, ne arbitrary endpoints)

**Příklady zakázaných scénářů:**
- ❌ "Pošli celý audit log do ChatGPT pro analýzu" (obsahuje PII + citlivá data)
- ❌ "AI vygeneruj SQL query na základě user inputu" (injection risk)
- ❌ "Nech AI přistupovat k production DB pro 'lepší kontext'" (data leak)

**Příklady povolených scénářů (s kontrolou):**
- ✅ "AI vygeneruj workflow template" (žádná citlivá data v promptu)
- ✅ "AI analyzuj anonymizované metrics" (PII odstraněno před odesláním)
- ✅ "AI asistent pro metamodel design" (pracuje s schema, ne s daty)

#### AI & LLM Security - Specifické Požadavky (EPIC-016)

**Všechna AI volání (ChatGPT, interní LLM, MCP tools) MUSÍ:**
- ✅ Jít přes **bezpečnou backend vrstvu** (ne přímo z prohlížeče)
- ✅ Používat **stejné RBAC a tenant omezení** jako lidský uživatel
- ✅ Mít **auditovatelný log** (kdo/co/na základě čeho změnil)

**Do LLM se NESMÍ posílat:**
- ❌ Cross-tenant data (žádné "vezmi data z tenant-A a použij je v tenant-B")
- ❌ Plné osobní údaje bez anonymizace / pseudonymizace
- ❌ Secrety, tokeny, interní klíče, konfigurace (API keys, DB passwords, JWT secrets)

**Jakýkoliv "AI agent" pracující s metadaty/metamodelem:**
- ✅ Používá **stejné RBAC a tenant omezení** jako lidský uživatel (ne bypass přes service account s admin právy)
- ✅ Má **auditovatelný log** (kdo/co/na základě čeho změnil schema/workflow/konfiguraci)
- ✅ Změny konfigurace musí **projít člověkem** (Propose/Approve workflow, ne direct apply)
- ❌ NESMÍ autonomně měnit produkční schema bez human approval

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

## ✅ Security DoD Checklist (Production-Ready)

Před nasazením do produkce **MUSÍ být splněny** všechny následující body:

### Autentizace & Autorizace
- [ ] Všechny služby ověřují JWT (issuer, audience, expirace, signature, `alg != none`)
- [ ] Tenant izolace je vynucená: subdoména → tenant → claim → backend guard (není možné cross-tenant čtení ani zápis)
- [ ] Žádný endpoint neakceptuje `tenantId` nebo `orgId` pouze z query/body bez nezávislého ověření z tokenu
- [ ] Všechny admin / internal endpointy jsou chráněné rolí (`CORE_ADMIN_*`/`SYSTEM`) a nejsou veřejně dostupné

### Secrets Management
- [ ] Žádné secrety, hesla, `client_secret`, API keys ani privátní klíče nejsou v Gitu (`.env`, YAML, JSON, shell, Dockerfile)
- [ ] V produkci jsou všechny secrety spravované přes secret manager / Vault (EPIC-012), ne přes lokální `.env`

### Network & TLS
- [ ] HTTPS je povinné pro FE, API gateway, Keycloak, n8n, AI gateway i externí integrace
- [ ] Konfigurované CORS je restriktivní (jen povolené originy, žádné `*` pro credentials)
- [ ] Security hlavičky (CSP, HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy) jsou nastavené na ingress / Nginx úrovni

### Logging & Audit
- [ ] Logy a audity neobsahují citlivá data (PII, klíče, tokeny); technické detaily jsou logované strukturovaně do Loki
- [ ] Všechny integrace (n8n, AI, konektory, moduly) komunikují výhradně přes BFF/API gateway nebo dedikovanou proxy, ne přímo na DB/Kafku/Loki

### Testing & CI/CD
- [ ] CI/CD pipeline spouští SCA (dependency scan), secret scan, lint, testy a failuje na HIGH/CRITICAL issues
- [ ] E2E testy pokrývají: login, RBAC, multitenant izolaci, základní happy-path pro klíčové moduly

### Incident Response
- [ ] Existuje SECURITY_RUNBOOK / incident response postup, jak řešit únik, podezřelé chování, kompromitaci klíčů
- [ ] Všechny změny security modelu procházejí code review a jsou reflektované v tomto EPICu

---

## 🔐 Governance & Ownership

**Tento EPIC je "single source of truth" pro security model celé platformy.**

**Jakákoliv změna, která ovlivňuje:**
- Autentizaci (Keycloak, JWT, SSO)
- Autorizaci (RBAC, role, permissions, scopes)
- Správu secretů (Vault, env vars, rotation)
- Multitenancy (tenant guard, izolace, cross-tenant policies)
- Přístup k infrastruktuře (Loki, Kafka, DB, n8n, AI)

**MUSÍ:**
- ✅ Projít **security review** (code review s focus na security)
- ✅ Být **zapsaná do tohoto dokumentu** (update EPIC-000 README)
- ✅ Být **promítnutá do souvisejících EPICů** (007, 011, 012, 016, 017)

**Ownership:**
- **Primary Owner:** Security + DevOps + Platform Team
- **Reviewers:** Tech Lead + Security Officer (pokud existuje)
- **Approval Required:** Změny EPIC-000 vyžadují approval minimálně 2 members (Security + DevOps/Platform Lead)

**Review Cycle:**
- ✅ Quarterly review (každé 3 měsíce) - update dle nových threat vectors, compliance requirements
- ✅ Ad-hoc review při security incidents, major architectural changes, new integrations

---

## � Vztah k Dalším EPICům

**Tento EPIC definuje principy. Implementaci řeší tyto EPICy:**

### EPIC-003: Monitoring & Observability
**Tenant-aware monitoring:**
- ✅ Loki, Prometheus, tracing **vždy tagovat `realm={realm_name}`**
- ✅ UI dashboardy filtrované podle realmu (uživatel vidí jen svůj tenant)
- ✅ Cross-realm monitoring pouze pro `admin` realm s rolemi CORE_AUDITOR/CORE_PLATFORM_ADMIN
- ✅ Alerty per realm (ne globální alerty bez tenant context)

### EPIC-005: Metamodel & Studio
**Realm-scoped metamodel:**
- ✅ Definice entit, relací a přístupových pravidel **vždy vázaná na realm**
- ✅ Metamodel Studio API ověřuje realm z JWT a nezobrazuje cross-realm definice
- ✅ Auditovatelné změny modelu: kdo kdy v jakém realmu změnil schema
- ✅ Žádné sdílené globální entity mimo explicitní platform-level typy (např. `AuditLog`, `SystemConfig`)

### EPIC-007: Infrastructure & Deployment
**Automatizace pro nové tenanty:**
- ✅ Zakládání nového tenanta = **vytvoření subdomény + Keycloak realmu + base roles**
- ✅ Automatizační skripty (Terraform, Ansible, n8n) nesmějí míchat realmy
- ✅ Každý nový realm má:
  - Vlastní subdoménu (DNS routing)
  - Vlastní Keycloak realm (identity isolation)
  - Base role set (TENANT_ADMIN, USER)
  - Namespace v DB/S3/Loki (realm prefix/label)

### EPIC-011: n8n Integration
**Realm-scoped workflow automation:**
- ✅ n8n přístup **vždy omezen na konkrétního tenanta/realm**
- ✅ Workflow execution context obsahuje `realm` a nesmí cross-realm operace bez explicit scope
- ✅ **Žádný "god-mode" workflow** bez jasných guardrails (TenantGuard validace)
- ✅ Service account pro n8n má scope limitovaný na specific realms (ne wildcard cross-realm)

### EPIC-017: Modular Architecture
**Realm-per-module activation:**
- ✅ Moduly (projekty, pluginy, rozšíření) se **aktivují per realm**
- ✅ Konfigurace modulu je **per realm** (ne globální shared config)
- ✅ **Žádné míchání dat mezi realmy** v modulech (respektovat TenantGuard)
- ✅ Plugin registry per realm (tenant A má jiné moduly než tenant B)

---

## ⚠️ ZÁVĚREČNÝ PRINCIP (NEMĚNIT)

**Core Platform trvale používá princip:**

```
tenant = subdoména = Keycloak realm
```

**Jakékoliv změny tohoto principu jsou breaking change architektury** a musí být vedeny jako:
- ✅ Samostatný **architektonický návrh (ADR)** s review process
- ✅ Samostatný **migrační EPIC** s rollback plánem
- ❌ **NE jako nenápadná úprava konfigurace** nebo "quick fix"

**Důvod:**
- Tento model je zakódován v Keycloak realm struktuře, subdomain routing, JWT issuer validation, DB/S3/Loki namespacing
- Změna vyžaduje migraci všech existujících tenantů, rewrite TenantGuard, update všech služeb
- Není to "switch config flag" - je to fundamental architectural shift

---

## �📚 References

- **OWASP Top 10 (2021):** https://owasp.org/Top10/
- **CIS Docker Benchmark:** https://www.cisecurity.org/benchmark/docker
- **Keycloak Documentation:** https://www.keycloak.org/documentation
- **NIST Cybersecurity Framework:** https://www.nist.gov/cyberframework
- **Zero Trust Architecture (NIST SP 800-207):** https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-207.pdf

---

**Last Updated:** 9. listopadu 2025  
**Owned by:** Security + DevOps + Platform Team  
**Review Cycle:** Quarterly (každé 3 měsíce security review + update)
