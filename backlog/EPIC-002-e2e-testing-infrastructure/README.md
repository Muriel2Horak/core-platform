# EPIC-002: E2E Testing Infrastructure

**Status:** 🔵 **IN PROGRESS**  
**Priority:** P0 (Critical Foundation)  
**Effort:** ~80 hodin  
**LOC:** ~6,000 řádků

---

## 🎯 Cíle EPICU

**Stabilní, pragmatická a dlouhodobě udržitelná E2E/testovací infrastruktura** pro core-platform jako **plnohodnotná součást kvality**, ne jen Playwright setup.

### Hlavní Cíle

1. **Stabilní E2E Framework (Playwright)**
   - Testy běží nad reálným prostředím `core-platform.local`
   - Konzistentní struktura: Page Object Model (POM), sdílené helpery
   - Jasné tagování testů pro organizaci a filtrování

2. **Test Data & Tenant Lifecycle Management**
   - Každý E2E scénář používá dedikovaný test tenant (create → use → cleanup)
   - Helper funkce: `createTenantForTest()`, `createUserWithRole()`, `seedData()`, `cleanupTenant()`
   - Žádná PII - pouze syntetická data
   - Deterministické testy (repeatable, stejný vstup = stejný výsledek)

3. **Spolehlivé Smoke/E2E Scénáře**
   - **Tenant & Identity Lifecycle**: vytvoření tenanta, rolí, uživatelů, RBAC validace
   - **Login** přes Keycloak SSO (multi-realm)
   - **Core Application Flows**: entity CRUD, workflow transitions, document upload, DMS linkage
   - **Monitoring**: Loki Log Viewer UI, fulltext search
   - **n8n Orchestration**: základní workflow execution (když ready)

4. **Performance & SLO-aware E2E**
   - Měření času u klíčových scénářů (login, CRUD, workflow, search)
   - Definované performance thresholdy (KPI/SLI):
     - Login (OIDC flow): p95 < 2s
     - Dashboard load: p95 < 1.5s
     - Entity CRUD: p95 < 500ms
     - Workflow transition: p95 < 1s
     - Log search: p95 < 2s
   - Test FAIL/WARN pokud threshold překročen

5. **Metrics & Observability Integration**
   - E2E runner publikuje metriky do Prometheus:
     - `e2e_tests_total`, `e2e_tests_failed`
     - `e2e_scenario_duration_seconds`
     - `e2e_slo_violation_total`
   - Vizualizace v admin-only dashboardu:
     - Grafana (admin realm, `CORE_PLATFORM_ADMIN` role)
     - Nebo vlastní Monitoring UI (EPIC-003)
   - Hodnocení trendů: pass rate, flakiness, duration over time
   - **Source of Truth**: CI status (HTML/JUnit reports), metriky = náhled

6. **Production-Safe Non-Invasive Checks**
   - Malý subset read-only E2E testů pro PROD (post-deploy)
   - Pouze: login (test user), health endpoints, klíčové view dostupnost
   - **Nikdy** nemodifikuje data, žádné create/update/delete
   - Běží automatizovaně v CI po deploy

7. **API Contract Testing**
   - Základní contract testy pro kritické BFF API
   - Detekce breaking changes v API
   - OpenAPI/JSON schema validation

8. **CI Pipeline & Quality Gates**
   - **PR checks:** Unit + Integration + Smoke E2E (mandatory)
   - **Full/Regression E2E:** Nightly nebo on-demand
   - Rozumné quality gates (ne "vše nebo nic")

9. **Testing Guidelines**
   - Jasná dokumentace jak psát testy
   - Konvence pro tagging, strukturu, helpers
   - Best practices pro náš tým

### Principy

```
✅ Trunk-based workflow: Vše v main, malé inkrementy, feature flagy
✅ Pragmatismus: Pokrýt klíčové scénáře, ne všechno možné
✅ Udržitelnost: Žádné enterprise QA platformy, co tým neutáhne
✅ Incremental: Nejdřív stabilní malý set, potom rozšiřovat
```

---

## 🏗️ Architektura

```
E2E Testing Infrastructure
│
├── E2E Tests (Playwright)
│   ├── Smoke Tests (5-7 min) - Kritické cesty (login, CRUD, health)
│   ├── Full E2E (20-30 min) - Kompletní scénáře (tenant lifecycle, workflow, n8n)
│   ├── Performance E2E - SLO-aware tests (měření času, KPI assertions)
│   ├── Security Tests - Tenant isolation, RBAC, cross-tenant protection
│   ├── Prod-Safe Checks - Read-only post-deploy validation
│   └── Test Tags: @SMOKE, @CRITICAL, @REGRESSION, @PERFORMANCE, @PROD-SAFE
│
├── Test Data & Tenant Lifecycle
│   ├── createTenantForTest() - Dedikovaný test tenant per suite
│   ├── createUserWithRole() - Users, roles, groups via Keycloak admin API
│   ├── seedSampleData() - Entities, workflows, documents (deterministické)
│   ├── cleanupTenant() - Teardown nebo test-only namespace cleanup
│   └── NO PII - Pouze syntetická data, anonymizovaná fixtures
│
├── Page Object Model (POM)
│   ├── LoginPage, MainLayoutPage
│   ├── TenantManagementPage (tenant lifecycle)
│   ├── MetamodelStudioPage
│   ├── WorkflowPage
│   ├── LokiLogViewerPage
│   └── n8nIntegrationPage (když ready)
│
├── Performance & KPI Tracking
│   ├── Scenario Timers (login, CRUD, workflow, search)
│   ├── SLI Thresholds (p95 < X seconds)
│   ├── Assertions (fail if threshold exceeded)
│   └── Metrics Export (Prometheus format)
│
├── Observability Integration
│   ├── Prometheus Metrics:
│   │   ├── e2e_tests_total (counter)
│   │   ├── e2e_tests_failed (counter)
│   │   ├── e2e_scenario_duration_seconds (histogram)
│   │   └── e2e_slo_violation_total (counter)
│   ├── Dashboards:
│   │   ├── Grafana (admin-only, CORE_PLATFORM_ADMIN role)
│   │   └── Custom Monitoring UI (EPIC-003 Loki/Prometheus frontend)
│   └── Trend Analysis: pass rate, flakiness, duration over time
│
├── API Contract Tests
│   ├── Metamodel BFF API (OpenAPI schema validation)
│   ├── Workflow BFF API
│   ├── Loki BFF API
│   ├── Tenant Management API
│   └── Auth/Identity API (Keycloak admin)
│
├── Mock Services
│   ├── External API Mocks (deterministic responses)
│   ├── Keycloak Mock (pro některé scénáře)
│   └── Real Services: Loki, Prometheus, n8n (NOT mocked)
│
└── CI/CD Integration
    ├── PR Checks: Unit + IT + Smoke E2E (mandatory)
    ├── Nightly: Full E2E + Performance tests
    ├── Post-Deploy: Prod-safe read-only checks
    ├── Metrics Push: E2E results → Prometheus
    └── Reports: HTML (JUnit/Playwright) + GitHub Actions artifacts
```

### Test Types & Metrics

| Type | Framework | Purpose | Run Time | KPI/SLI | Trigger |
|------|-----------|---------|----------|---------|---------|
| **Smoke E2E** | Playwright | Rychlá validace kritických cest | 5-7 min | Login <2s, Dashboard <1.5s | PR mandatory |
| **Full E2E** | Playwright | Kompletní flow (tenant, workflow, n8n) | 20-30 min | CRUD <500ms, Workflow <1s | Nightly |
| **Performance E2E** | Playwright + metrics | SLO validation, threshold assertions | 10-15 min | Search <2s, Tenant create <20s | Nightly |
| **Security E2E** | Playwright | Tenant isolation, RBAC, cross-tenant | 5-10 min | N/A | PR/nightly |
| **Prod-Safe Checks** | Playwright | Read-only post-deploy validation | 3-5 min | Health <500ms | Post-deploy |
| **API Contract** | OpenAPI/JSON Schema | Detekce breaking changes | 3-5 min | N/A | PR mandatory |
| **Unit (BE)** | JUnit | Service layer, business logic | 5-10 min | N/A | PR mandatory |
| **Unit (FE)** | Vitest | React components, hooks | 2-5 min | N/A | PR mandatory |
| **Integration (BE)** | Testcontainers | DB, Kafka, Redis | 10-15 min | N/A | PR mandatory |

---

## 📊 Fázování

### Phase 1 – Foundation (MUST HAVE)

**Cíl:** Základní funkční E2E infrastruktura s test data lifecycle a tenant management

**Stories:**
- E2E1: Playwright Test Framework Setup ✅ DONE
- E2E2: Page Object Model (POM) ✅ DONE
- E2E9: Test Tagging System (@SMOKE, @CRITICAL, @PERFORMANCE, @PROD-SAFE) ✅ DONE
- **E2E14: Test Data & Tenant Lifecycle** (rozšířeno) ✅ DONE
- E2E13: Mock Services Integration
- E2E15: GitHub Actions CI/CD Workflows ✅ DONE
- **E2E16: Environment & Smoke Alignment** ✅ DONE
- **E2E17: Security & Negative E2E Scenarios** (nová)
- **E2E18: Tenant & Identity Lifecycle E2E** (nová)

**Výstup:**
- ✅ Funkční Playwright setup s POM
- ✅ Test data lifecycle (createTenantForTest, createUserWithRole, seedData, cleanup)
- ✅ Smoke scénáře (login, tenant creation, CRUD, workflow basics)
- ✅ Tenant & identity E2E (multi-realm, RBAC validation)
- ✅ Mock pro kritické závislosti
- ✅ CI pipeline (smoke E2E v PR)

### Phase 2 – Performance, Metrics & Quality

**Cíl:** SLO-aware testy, observability integrace, production checks

**Stories:**
- **E2E19: Performance & SLO-aware E2E** (nová - měření času, KPI thresholdy)
- **E2E20: Observability & Metrics Integration** (nová - Prometheus metrics, Grafana dashboards)
- **E2E21: Production-Safe Non-Invasive Checks** (nová - read-only post-deploy tests)
- E2E6: API Contract Testing (upraveno - focus na klíčové BFF)
- E2E11: CI/CD Quality Gates (upraveno - rozumné thresholdy + metrics)
- E2E5: Accessibility Testing ✅ DONE

**Výstup:**
- ✅ Performance E2E s KPI assertions (login <2s, CRUD <500ms, workflow <1s)
- ✅ Prometheus metrics z E2E testů (duration, pass rate, SLO violations)
- ✅ Admin-only dashboard (Grafana nebo custom Monitoring UI)
- ✅ Prod-safe checks (read-only, post-deploy validation)
- ✅ Contract testy pro Metamodel, Workflow, Loki BFF
- ✅ Quality gates s metrics-based thresholdy

**Výstup:**
- ✅ Contract testy pro Metamodel, Workflow, Loki BFF
- ✅ Quality gates (unit + IT + smoke mandatory, full E2E optional)
- ✅ Základní a11y checks na klíčových stránkách

### Phase 3 – Nadstavba (NICE TO HAVE)

**Cíl:** Volitelné rozšíření pro vizuální regrese, performance, reporting

**Stories:**
- E2E4: Visual Regression Testing (volitelné, pár kritických obrazovek)
- E2E7: Performance Testing (volitelné, 2-3 scénáře)
- E2E8: Test Reporting & Overview (zjednodušeno - script z JUnit/Playwright reportů, bez vlastní DB)
- E2E10: Coverage Dashboard (zjednodušeno - JaCoCo/Playwright HTML reports, GitHub Pages)

**Výstup:**
- ⚠️ Volitelné vizuální regrese (Percy/Chromatic)
- ⚠️ Volitelné perf testy (login, search, workflow)
- ⚠️ Jednoduchý overview report (tagy + coverage)
- ⚠️ HTML coverage dashboard (bez Grafany)

---

## 📋 Stories Overview

| ID | Story | Phase | Status | LOC | Effort | Value |
|----|-------|-------|--------|-----|--------|-------|
| [E2E1](#e2e1-playwright-test-framework-setup) | Playwright Test Framework Setup | 1 | ✅ DONE | ~1,200 | 6h | Foundation |
| [E2E2](#e2e2-page-object-model-pom) | Page Object Model (POM) | 1 | ✅ DONE | ~800 | 8h | Struktura testů |
| [E2E9](#e2e9-test-tagging-system) | Test Tagging System | 1 | ✅ DONE | ~300 | 4h | Organizace |
| [E2E12](#e2e12-testing-standards-guide) | Testing Standards & Guide | 1 | 🔵 TODO | ~600 | 8h | Dokumentace |
| [E2E13](#e2e13-mock-services) | Mock Services Integration | 1 | 🔵 TODO | ~600 | 10h | Deterministické testy |
| [E2E14](#e2e14-test-data-management) | Test Data Management | 1 | ✅ DONE | ~800 | 12h | Test data + safety (rozšířeno) |
| [E2E15](#e2e15-github-actions-cicd-workflows) | GitHub Actions CI/CD Workflows | 1 | ✅ DONE | ~800 | 4h | CI/CD dokumentace |
| [E2E16](#e2e16-environment--smoke-alignment) | **Environment & Smoke Alignment** | 1 | 🔵 TODO | ~400 | 6h | **Smoke testy** |
| [E2E17](#e2e17-security--negative-e2e-scenarios) | **Security & Negative E2E Scenarios** | 1 | 🔵 TODO | ~500 | 8h | **Security** |
| **[E2E18](#e2e18-tenant--identity-lifecycle-e2e)** | **Tenant & Identity Lifecycle E2E** | 1 | 🔵 TODO | **~800** | **12h** | **NEW: Multi-tenant, RBAC, isolation** |
| **[E2E19](#e2e19-performance--slo-aware-e2e)** | **Performance & SLO-aware E2E** | 2 | 🔵 TODO | **~400** | **8h** | **NEW: KPI thresholds, timers** |
| **[E2E20](#e2e20-observability--metrics-integration)** | **Observability & Metrics Integration** | 2 | 🔵 TODO | **~500** | **10h** | **NEW: Prometheus, Grafana** |
| **[E2E21](#e2e21-production-safe-non-invasive-checks)** | **Production-Safe Non-Invasive Checks** | 2 | 🔵 TODO | **~300** | **6h** | **NEW: Read-only prod tests** |
| [E2E6](#e2e6-api-contract-testing) | API Contract Testing | 2 | 🔵 TODO | ~400 | 6h | Breaking changes |
| [E2E11](#e2e11-cicd-quality-gates) | CI/CD Quality Gates | 2 | 🔵 TODO | ~300 | 5h | Automatická validace |
| [E2E5](#e2e5-accessibility-a11y-testing) | Accessibility (a11y) Testing | 2 | ✅ DONE | ~300 | 6h | WCAG checks |
| [E2E4](#e2e4-visual-regression-testing) | Visual Regression Testing | 3 | 🔵 TODO | ~400 | 8h | **OPTIONAL** |
| [E2E7](#e2e7-performance-testing) | Performance Testing | 3 | 🔵 TODO | ~300 | 6h | **OPTIONAL** (merged → E2E19) |
| [E2E8](#e2e8-test-reporting--overview) | Test Reporting & Overview | 3 | 🔵 TODO | ~300 | 5h | **OPTIONAL** |
| [E2E10](#e2e10-coverage-dashboard) | Coverage Dashboard | 3 | 🔵 TODO | ~300 | 4h | **OPTIONAL** |
| **TOTAL** | | | **6/20** | **~9,200** | **~140h** | **Comprehensive E2E quality framework** |

**Poznámky:**
- **Phase 1 (Foundation):** 10 stories, ~64h - Základní infrastruktura + tenant lifecycle + security (6 done, 4 todo)
- **Phase 2 (Performance & Quality):** 6 stories, ~42h - Metrics, observability, prod checks, contract tests (1 done, 5 todo)
- **Phase 3 (NICE TO HAVE):** 4 stories, ~23h - Volitelné nadstavby (0 done, 4 todo)
- **NEW Stories (E2E18-E2E21):** 4 stories, ~36h - Multi-tenant E2E, performance SLO, metrics, prod-safe
- **Source of Truth:** CI status (HTML/JUnit reports, GitHub Actions) - Grafana pouze metrics visualization

---

## 📖 Detailed Stories

### Phase 1: Foundation (MUST HAVE)

#### E2E1: Playwright Test Framework Setup

> **Foundation:** Základní Playwright setup pro core-platform.local prostředí

**As a** developer  
**I want** funkční Playwright framework  
**So that** můžu psát E2E testy nad reálným prostředím

**Acceptance Criteria:**

✅ Playwright nainstalován a nakonfigurován  
✅ Konfigurace pro `core-platform.local` (SSL, Nginx, Keycloak, Loki)  
✅ Základní login helper (Keycloak SSO flow)  
✅ Environment configuration (`.env` pro test prostředí)  
✅ První smoke test (login → redirect na /admin)

**Scope:**
- Instalace Playwright (`e2e/package.json`)
- Config `playwright.config.ts` (baseURL: https://core-platform.local)
- Login helper (`e2e/helpers/auth.ts`)
- První test (`e2e/specs/smoke/login.spec.ts`)

**Status:** ✅ **DONE** (Wave 1, červenec 2024)

**Details:** [E2E1 Story](./stories/E2E1-playwright-test-framework-setup/README.md)

---

#### E2E2: Page Object Model (POM)

> **Structure:** Konzistentní struktura testů pomocí Page Objects

**As a** developer  
**I want** Page Object Model konvenci  
**So that** testy jsou čitelné a maintainable

**Acceptance Criteria:**

✅ Page objekty pro klíčové stránky:
- `LoginPage` (Keycloak login)
- `MainLayoutPage` (top bar, sidebar navigation)
- `MetamodelStudioPage` (entity editor, schema designer)
- `WorkflowPage` (workflow designer, instance viewer)
- `LokiLogViewerPage` (log search, filters)

✅ Jednotná konvence (getters pro elementy, actions, assertions)  
✅ Sdílené base page (`BasePage` s common utilities)

**Scope:**
- Struktura `e2e/pages/`
- Page objects pro 5 klíčových stránek
- Helper metody (waitForLoad, navigateTo)
- Příklady použití v testech

**Status:** ✅ **DONE** (Wave 1, červenec 2024)

**Details:** [E2E2 Story](./stories/E2E2-page-object-model-pom-pattern/README.md)

---

#### E2E9: Test Tagging System

> **Organization:** Systém tagů pro filtrování a organizaci testů

**As a** developer  
**I want** standardní tagging konvenci  
**So that** můžu spouštět jen relevantní testy (smoke, critical, regression)

**Acceptance Criteria:**

✅ Definované tagy:
- `@SMOKE` - Rychlé smoke testy (5-7 min)
- `@CRITICAL` - Kritické cesty (login, workflow základy)
- `@REGRESSION` - Full regression suite
- `@TENANT(admin)` - Tenant-specific testy
- `@CORE-XXX` - Mapování na User Story (volitelné)

✅ Filtrovací skripty (`npm run test:smoke`, `npm run test:critical`)  
✅ CI integrace (smoke v PR, regression manuálně)

**Scope:**
- Dokumentace tagů (`docs/testing-tagging.md`)
- Playwright tagging (test.describe decorators)
- NPM skripty pro filtrování
- CI konfigurace (GitHub Actions)

**Status:** ✅ **DONE** (Wave 1, červenec 2024)

**Details:** [E2E9 Story](./stories/E2E9-test-tagging-system-implementation-tasks/README.md)

---

#### E2E12: Testing Standards Guide

> **Documentation:** Kompletní guide pro psaní testů

**As a** developer  
**I want** jasnou dokumentaci testing standardů  
**So that** vím jak psát testy konzistentně

**Acceptance Criteria:**

✅ Dokumentace pokrývá:
- Kdy psát E2E vs Unit vs Integration testy
- Playwright + POM konvence
- Tagging (@SMOKE, @CRITICAL)
- Test data management
- Mock services usage
- Debugging tips

✅ Konkrétní příklady pro každý typ testu  
✅ Reflektuje reálný stack (Keycloak, Loki, core-platform.local)

**Scope:**
- `docs/testing-guide.md` (~600 LOC)
- Příklady testů (smoke, full E2E, API contract)
- Troubleshooting sekce
- Best practices

**Status:** 🔵 **TODO** - Potřeba aktualizovat pro pragmatický přístup

**Details:** [E2E12 Story](./stories/E2E12-testing-standards-guide-implementation-t/README.md)

---

#### E2E13: Mock Services Integration

> **Deterministické testy:** Mock pro externí služby (Keycloak, externí API)

**As a** developer  
**I want** mock servery pro externí závislosti  
**So that** integration testy jsou rychlé, spolehlivé a nezávislé na external services

**Acceptance Criteria:**

✅ WireMock setup pro integration testy  
✅ Mock pro Keycloak (token, user API) - pouze pro některé scénáře  
✅ Mock pro externí API (pokud existují)  
✅ Loki zůstává real (není mockovaný, pokud to jde jednoduše)  
✅ Integration testy používají mocks

**Scope:**
- WireMock Testcontainer setup
- Keycloak mock stubs
- Externí API mocks (pokud potřeba)
- Helper utility pro mocking

**Status:** 🔵 **TODO** - Mock jen kde nutné, Loki real

**Details:** [E2E13 Story](./stories/E2E13-mock-services-implementation-tasks/README.md)

---

#### E2E14: Test Data Management

> **Opakovatelnost:** Automatické vytváření/mazání test dat + production safety

**As a** developer  
**I want** automatický systém pro test data  
**So that** testy mají konzistentní data a NIKDY se nedostanou do produkce

**Acceptance Criteria:**

✅ Seed skripty (users, tenants, roles) s `test_` / `e2e_` prefixem  
✅ Automatic cleanup po testech  
✅ Production safety guards (@Profile, startup check, DB triggers)  
✅ Test tenant pro izolaci  
✅ Opakovatelnost (deterministické test data)

**Scope:**
- TestDataSeeder (@Profile("!production"))
- ProductionSafetyConfig (startup check)
- TestDataManager (cleanup utilities)
- Database triggers (prevent test_ in production)
- Playwright test data helpers

**Status:** ✅ **DONE** (Wave 1, červenec 2024)

**Details:** [E2E14 Story](./stories/E2E14-test-data-management-implementation-task/README.md)

---

#### E2E15: GitHub Actions CI/CD Workflows

> **Dokumentace:** Kompletní guide pro všechny GitHub Actions workflows

**Status:** ✅ **DONE**

**As a** developer  
**I want** jasnou dokumentaci CI/CD pipeline  
**So that** rozumím jak funguje automatické testování a deployment

**Acceptance Criteria:**

✅ Dokumentace všech 13 workflows (ci, pre-deploy, post-deploy, e2e, code-quality, security-scan, atd.)  
✅ Trigger conditions (push, PR, schedule, manual)  
✅ Enable/disable procedures  
✅ Troubleshooting guide  
✅ Best practices (caching, matrix, conditional execution)

**Current State:**
- Workflows DISABLED (v `.github/workflows-disabled/`)
- Důvod: EPIC-017 development, save CI minutes
- Re-enable: Po implementaci modular architecture

**Status:** ✅ **DONE** (Wave 1, červenec 2024)

**Details:** [E2E15 Story](./stories/E2E15-github-actions-workflows/README.md)

---

#### E2E16: Environment & Smoke Alignment

> **NEW!** Smoke testy:** Definice smoke scénářů pro core-platform.local prostředí

**As a** developer  
**I want** jasně definované smoke scénáře  
**So that** můžu rychle validovat kritické cesty (5-7 min)

**Acceptance Criteria:**

✅ Jasný popis core-platform.local prostředí (Docker setup, služby, SSL)  
✅ 4 smoke scénáře: Login, CRUD entity, Workflow krok, Log Viewer  
✅ Shell script na health checks (backend, Loki BFF, Keycloak)  
✅ Environment dokumentace (services, test users)  
✅ Makefile integrace (`make test-smoke`)

**Scope:**
- 4 smoke Playwright testy (@SMOKE tag)
- Shell script pro endpoint validation
- Environment config documentation
- NPM script `test:smoke`

**Status:** 🔵 **TODO** - Nová story (Phase 1 MUST HAVE)

**Details:** [E2E16 Story](./stories/E2E16-environment-smoke-alignment/README.md) - **TODO: Vytvořit**

---

#### E2E17: Security & Negative E2E Scenarios

> **NEW! Security:** Ověření tenant isolation, RBAC, authentication

**As a** developer  
**I want** security E2E scénáře  
**So that** vím že tenant isolation a RBAC funguje správně

**Acceptance Criteria:**

✅ 3-5 security E2E scénářů:
- Nepřihlášený uživatel → redirect na login
- Tenant A nevidí data tenant B (isolation)
- User bez admin role nemá přístup do admin sekce
- Expirovaný token → redirect na login
- CSRF token validation

✅ Negative scénáře (unauthorized access, invalid input)  
✅ @SECURITY tag pro filtrování

**Scope:**
- 5 Playwright security testů
- Tenant isolation validation
- RBAC checks
- Authentication edge cases

**Status:** 🔵 **TODO** - Nová story (Phase 1-2 HIGH VALUE)

**Details:** [E2E17 Story](./stories/E2E17-security-negative-scenarios/README.md) - **TODO: Vytvořit**

---

#### E2E18: Tenant & Identity Lifecycle E2E

> **NEW! Multi-tenant:** End-to-end testy pro tenant a identity lifecycle (creation, RBAC, isolation)

**As a** product owner  
**I want** E2E validaci tenant lifecycle  
**So that** vím že tenant creation, multi-realm routing a identity funguje správně

**Acceptance Criteria:**

✅ **Tenant Creation Flow:**
- Vytvoření tenant via API (Tenant Management) → realm v Keycloak + DB schema
- Subdomain routing (https://TENANT.core-platform.local → správný realm)
- Keycloak admin API: ověření realm existence, clients, roles

✅ **Identity Management:**
- Vytvoření admin user, groups, roles (via Keycloak admin API nebo BFF)
- Login jako tenant admin → ověření access do admin sekce
- Vytvoření regular user → ověření RBAC (nemá přístup do admin features)

✅ **Tenant Isolation:**
- Tenant A nevidí data tenant B (entities, workflows, documents, logs)
- Ověření query isolation na úrovni DB nebo API

✅ **Multi-realm Routing:**
- https://admin.core-platform.local → admin realm (Master admin, platform operations)
- https://tenant1.core-platform.local → tenant1 realm (Tenant-specific users)
- https://tenant2.core-platform.local → tenant2 realm
- Invalid tenant → 404 nebo fallback

**Test Scenarios:**

```typescript
test('@TENANT @CRITICAL Tenant creation and admin user setup', async ({ page }) => {
  // 1. Create tenant via API
  const tenant = await createTenantForTest('acme-corp');
  
  // 2. Verify Keycloak realm created
  const realm = await keycloakAdminApi.getRealm('acme-corp');
  expect(realm).toBeDefined();
  
  // 3. Create admin user
  const adminUser = await createUserWithRole(tenant, 'admin', 'TENANT_ADMIN');
  
  // 4. Login as admin → verify admin features visible
  await page.goto('https://acme-corp.core-platform.local');
  await loginAs(page, adminUser);
  await expect(page.locator('[data-testid="admin-menu"]')).toBeVisible();
});

test('@TENANT @CRITICAL Tenant isolation validation', async ({ page }) => {
  // 1. Create two tenants with sample data
  const tenantA = await createTenantForTest('tenant-a');
  await seedSampleData(tenantA, { projects: 5, tasks: 20 });
  
  const tenantB = await createTenantForTest('tenant-b');
  await seedSampleData(tenantB, { projects: 3, tasks: 15 });
  
  // 2. Login to tenant A → verify only A data visible
  await loginAsTenant(page, tenantA);
  const projectsA = await api.get('/api/v1/projects');
  expect(projectsA.length).toBe(5);
  
  // 3. Login to tenant B → verify only B data visible
  await loginAsTenant(page, tenantB);
  const projectsB = await api.get('/api/v1/projects');
  expect(projectsB.length).toBe(3);
  
  // 4. Cleanup
  await cleanupTenant(tenantA);
  await cleanupTenant(tenantB);
});

test('@TENANT @RBAC User cannot access admin features', async ({ page }) => {
  const tenant = await createTenantForTest('rbac-test');
  const regularUser = await createUserWithRole(tenant, 'user', 'USER');
  
  await page.goto(`https://rbac-test.core-platform.local`);
  await loginAs(page, regularUser);
  
  // Admin menu should NOT be visible
  await expect(page.locator('[data-testid="admin-menu"]')).not.toBeVisible();
  
  // Direct navigation to admin route → redirect or 403
  await page.goto(`https://rbac-test.core-platform.local/admin`);
  await expect(page.locator('text=Unauthorized')).toBeVisible();
});
```

**Helpers Needed:**

```typescript
// e2e/helpers/tenant.ts
export async function createTenantForTest(slug: string, options?: TenantOptions): Promise<Tenant> {
  // Call Tenant Management API → create realm + DB schema
  const response = await fetch('/api/v1/admin/tenants', {
    method: 'POST',
    body: JSON.stringify({ slug, name: options?.name || slug, ...options })
  });
  return response.json();
}

export async function createUserWithRole(tenant: Tenant, username: string, role: string): Promise<User> {
  // Keycloak admin API → create user in tenant realm
  const keycloakAdmin = getKeycloakAdminClient();
  const user = await keycloakAdmin.users.create({
    realm: tenant.keycloakRealm,
    username,
    email: `${username}@${tenant.slug}.test`,
    enabled: true,
    credentials: [{ type: 'password', value: 'Test.1234' }]
  });
  
  // Assign role
  await keycloakAdmin.users.addRealmRoleMappings({
    realm: tenant.keycloakRealm,
    id: user.id,
    roles: [{ name: role }]
  });
  
  return { id: user.id, username, tenant, role };
}

export async function seedSampleData(tenant: Tenant, data: { projects?: number; tasks?: number }): Promise<void> {
  // Create deterministic sample data for testing
  const api = getApiClient(tenant);
  
  if (data.projects) {
    for (let i = 1; i <= data.projects; i++) {
      await api.post('/api/v1/projects', {
        name: `Project ${i}`,
        description: `Test project ${i} for ${tenant.slug}`
      });
    }
  }
  
  // ... similar for tasks, documents, workflows
}

export async function cleanupTenant(tenant: Tenant): Promise<void> {
  // Delete tenant → cascades to Keycloak realm + DB schema
  await fetch(`/api/v1/admin/tenants/${tenant.id}`, { method: 'DELETE' });
}
```

**LOC:** ~800 (helpers: ~400, tests: ~400)  
**Effort:** ~12h  
**Priority:** HIGH (Phase 1 - MUST HAVE)  
**Status:** 🔵 **TODO**

**Details:** [E2E18 Story](./stories/E2E18-tenant-identity-lifecycle/README.md) - **TODO: Vytvořit**

---

### Phase 2: Performance, Metrics & Quality

#### E2E19: Performance & SLO-aware E2E

> **NEW! Performance:** E2E testy s měřením času a KPI thresholdy (fail if exceeded)

**As a** product owner  
**I want** E2E testy které měří performance  
**So that** detekuji regresi a vím že SLO jsou splněné

**Acceptance Criteria:**

✅ **KPI/SLI Definitions (p95 thresholds):**
- Login (OIDC flow): **p95 < 2s**
- Dashboard load (initial): **p95 < 1.5s**
- Tenant creation (Keycloak + DB): **p95 < 10-20s**
- Entity CRUD (create/update): **p95 < 500ms**
- Workflow transition: **p95 < 1s**
- Log search/list view: **p95 < 2s**

✅ **Measurement:**
- Každý performance scénář měří čas (start → end)
- Playwright trace timing API nebo custom timers
- Ukládá do Prometheus metrics (histogram: `e2e_scenario_duration_seconds`)

✅ **Assertion Logic:**
- Test FAILS pokud threshold překročen
- Optional: WARN pokud threshold blízko (90% of limit)
- Metrics export → Grafana dashboard zobrazí trend

✅ **Test Tagging:**
- `@PERFORMANCE` tag pro filtrování
- Běží nightly (ne v každém PR - long-running)

**Test Scenarios:**

```typescript
test('@PERFORMANCE Login OIDC flow p95 < 2s', async ({ page }) => {
  const startTime = Date.now();
  
  await page.goto('https://admin.core-platform.local');
  await loginAs(page, testUser);
  await expect(page.locator('[data-testid="dashboard"]')).toBeVisible();
  
  const duration = (Date.now() - startTime) / 1000; // seconds
  
  // Publish metric
  await publishMetric('e2e_scenario_duration_seconds', duration, {
    scenario: 'login_oidc',
    p95_threshold: 2
  });
  
  // Assert threshold
  expect(duration).toBeLessThan(2); // FAIL if > 2s
  
  // Optional: warn if close to threshold
  if (duration > 1.8) {
    console.warn(`⚠️ Login close to threshold: ${duration}s (limit: 2s)`);
  }
});

test('@PERFORMANCE Entity CRUD p95 < 500ms', async ({ page }) => {
  await loginAs(page, testUser);
  
  const startTime = Date.now();
  
  // Create entity
  await page.click('[data-testid="new-entity-btn"]');
  await page.fill('[name="entityName"]', 'Test Entity');
  await page.click('[data-testid="save-btn"]');
  await expect(page.locator('text=Entity created')).toBeVisible();
  
  const duration = (Date.now() - startTime) / 1000;
  
  await publishMetric('e2e_scenario_duration_seconds', duration, {
    scenario: 'entity_create',
    p95_threshold: 0.5
  });
  
  expect(duration).toBeLessThan(0.5); // FAIL if > 500ms
});

test('@PERFORMANCE Workflow transition p95 < 1s', async ({ page }) => {
  await loginAs(page, testUser);
  
  // Setup: Create workflow instance
  const workflow = await createWorkflowInstance();
  await page.goto(`/workflows/${workflow.id}`);
  
  const startTime = Date.now();
  
  // Trigger transition
  await page.click('[data-testid="transition-approve"]');
  await expect(page.locator('text=Approved')).toBeVisible();
  
  const duration = (Date.now() - startTime) / 1000;
  
  await publishMetric('e2e_scenario_duration_seconds', duration, {
    scenario: 'workflow_transition',
    p95_threshold: 1
  });
  
  expect(duration).toBeLessThan(1);
});
```

**Metrics Helper:**

```typescript
// e2e/helpers/metrics.ts
import { Gauge, Histogram, Counter, Registry } from 'prom-client';

const registry = new Registry();

const scenarioDuration = new Histogram({
  name: 'e2e_scenario_duration_seconds',
  help: 'E2E scenario execution time in seconds',
  labelNames: ['scenario', 'p95_threshold'],
  buckets: [0.1, 0.5, 1, 2, 5, 10, 20]
});

const sloViolations = new Counter({
  name: 'e2e_slo_violation_total',
  help: 'Total E2E scenarios that exceeded SLO threshold',
  labelNames: ['scenario', 'threshold']
});

registry.registerMetric(scenarioDuration);
registry.registerMetric(sloViolations);

export async function publishMetric(name: string, value: number, labels: Record<string, any>) {
  scenarioDuration.observe(labels, value);
  
  // Count SLO violations
  if (value > labels.p95_threshold) {
    sloViolations.inc({ scenario: labels.scenario, threshold: labels.p95_threshold });
  }
}

export async function exportMetrics(): Promise<string> {
  return registry.metrics();
}
```

**CI Integration:**

```yaml
# .github/workflows/e2e-performance.yml
name: E2E Performance Tests

on:
  schedule:
    - cron: '0 2 * * *' # Nightly at 2 AM
  workflow_dispatch:

jobs:
  performance:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run performance E2E
        run: npm run test:performance
      
      - name: Export metrics to Prometheus
        run: |
          curl -X POST http://prometheus:9090/api/v1/write \
            -H "Content-Type: application/x-protobuf" \
            --data-binary @e2e/metrics.bin
      
      - name: Check SLO violations
        run: |
          violations=$(grep 'e2e_slo_violation_total' metrics.txt | wc -l)
          if [ $violations -gt 0 ]; then
            echo "❌ $violations SLO violations detected!"
            exit 1
          fi
```

**LOC:** ~400 (helpers: ~100, tests: ~300)  
**Effort:** ~8h  
**Priority:** HIGH (Phase 2)  
**Status:** 🔵 **TODO**

**Details:** [E2E19 Story](./stories/E2E19-performance-slo-aware/README.md) - **TODO: Vytvořit**

---

#### E2E20: Observability & Metrics Integration

> **NEW! Observability:** Export E2E metrics do Prometheus, Grafana dashboards, trend analysis

**As a** platform engineer  
**I want** E2E metrics v Grafana dashboards  
**So that** vidím trend (pass rate, flakiness, duration) a můžu detekovat regresi

**Acceptance Criteria:**

✅ **Prometheus Metrics Export:**
- `e2e_tests_total` (counter) - Celkový počet testů
- `e2e_tests_failed` (counter) - Počet failed testů
- `e2e_scenario_duration_seconds` (histogram) - Časy scénářů (login, CRUD, workflow, search)
- `e2e_slo_violation_total` (counter) - Počet SLO porušení

✅ **Grafana Dashboard (Admin-Only):**
- Panel: E2E Pass Rate (last 7 days, last 30 days)
- Panel: Scenario Duration Trends (p95, p50 over time)
- Panel: SLO Violations (by scenario)
- Panel: Flakiness Rate (retries, intermittent failures)
- Access: CORE_PLATFORM_ADMIN role, admin realm (OIDC)

✅ **Custom Monitoring UI Integration (EPIC-003):**
- Link to E2E metrics from custom Monitoring UI
- Admin-only section "E2E Test Health"
- Show latest run status, duration, failures

✅ **Trend Analysis:**
- Historical data retention (90 days minimum)
- Alerting: Slack/email pokud pass rate < 80%

**Metrics Exporter:**

```typescript
// e2e/helpers/metrics-exporter.ts
import { Registry, collectDefaultMetrics } from 'prom-client';
import fs from 'fs';

const registry = new Registry();
collectDefaultMetrics({ register: registry });

// ... register custom metrics (from E2E19)

export async function exportToPrometheus(outputPath: string) {
  const metrics = await registry.metrics();
  fs.writeFileSync(outputPath, metrics);
  console.log(`✅ Metrics exported to ${outputPath}`);
}

// In test global teardown:
export async function globalTeardown() {
  await exportToPrometheus('./e2e/metrics.txt');
  
  // Push to Prometheus pushgateway (if configured)
  if (process.env.PROMETHEUS_PUSHGATEWAY) {
    const gateway = new Pushgateway(process.env.PROMETHEUS_PUSHGATEWAY);
    await gateway.pushAdd({ jobName: 'e2e-tests' });
  }
}
```

**Grafana Dashboard JSON:**

```json
{
  "dashboard": {
    "title": "E2E Test Health (Admin Only)",
    "panels": [
      {
        "title": "E2E Pass Rate (Last 7 Days)",
        "targets": [
          {
            "expr": "(1 - (sum(rate(e2e_tests_failed[7d])) / sum(rate(e2e_tests_total[7d])))) * 100"
          }
        ],
        "type": "stat",
        "thresholds": [
          { "value": 0, "color": "red" },
          { "value": 80, "color": "yellow" },
          { "value": 95, "color": "green" }
        ]
      },
      {
        "title": "Scenario Duration p95 (Login, CRUD, Workflow)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(e2e_scenario_duration_seconds_bucket[1h])) by (scenario, le))",
            "legendFormat": "{{scenario}}"
          }
        ],
        "type": "graph"
      },
      {
        "title": "SLO Violations (Last 24h)",
        "targets": [
          {
            "expr": "sum(increase(e2e_slo_violation_total[24h])) by (scenario)"
          }
        ],
        "type": "table"
      }
    ],
    "access": {
      "mode": "Admin Only",
      "role": "CORE_PLATFORM_ADMIN",
      "realm": "admin"
    }
  }
}
```

**CI Integration:**

```yaml
# .github/workflows/e2e-post-deploy.yml
- name: Export E2E metrics to Prometheus
  if: always()
  run: |
    npm run test:export-metrics
    
    # Push to Prometheus pushgateway
    curl -X POST http://prometheus-pushgateway:9091/metrics/job/e2e-tests \
      --data-binary @e2e/metrics.txt

- name: Notify if pass rate < 80%
  run: |
    pass_rate=$(curl -s http://prometheus:9090/api/v1/query?query='...' | jq '.data.result[0].value[1]')
    if (( $(echo "$pass_rate < 80" | bc -l) )); then
      echo "❌ E2E pass rate: $pass_rate% (threshold: 80%)"
      # Send Slack notification
    fi
```

**LOC:** ~500 (metrics exporter: ~150, dashboard JSON: ~200, integration: ~150)  
**Effort:** ~10h  
**Priority:** MEDIUM (Phase 2)  
**Status:** 🔵 **TODO**

**Details:** [E2E20 Story](./stories/E2E20-observability-metrics-integration/README.md) - **TODO: Vytvořit**

---

#### E2E21: Production-Safe Non-Invasive Checks

> **NEW! Prod Validation:** Read-only E2E testy pro production environment (post-deploy validation)

**As a** platform engineer  
**I want** bezpečné read-only E2E testy pro PROD  
**So that** můžu validovat deployment bez modifikace dat

**Acceptance Criteria:**

✅ **Read-Only Operations Only:**
- Login (test user account)
- GET operations (health, status, list views)
- Navigation validation (routes accessible)
- NO create/update/delete operations
- NO data modifications

✅ **Test Scenarios:**
- Login flow (test user, OIDC)
- Health endpoints (/api/actuator/health, /api/status)
- Dashboard load (verify no errors)
- Key views accessible (Metamodel Studio, Workflow, Logs)
- API response validation (200 OK, no 500 errors)

✅ **Dedicated Test Accounts:**
- `prod-readonly@core-platform.test` (role: READ_ONLY)
- NO admin privileges
- Isolated from production users

✅ **Post-Deploy CI Trigger:**
- Runs automatically after deploy to PROD
- Fail deployment if critical checks fail
- Report to Slack/email

**Test Scenarios:**

```typescript
test('@PROD-SAFE @CRITICAL Login and health check', async ({ page }) => {
  const startTime = Date.now();
  
  // Login as read-only test user
  await page.goto('https://core-platform.prod');
  await loginAs(page, prodReadOnlyUser);
  
  // Verify dashboard loads
  await expect(page.locator('[data-testid="dashboard"]')).toBeVisible();
  
  const duration = (Date.now() - startTime) / 1000;
  expect(duration).toBeLessThan(3); // Health check threshold
  
  // Publish metric
  await publishMetric('e2e_scenario_duration_seconds', duration, {
    scenario: 'prod_login_health',
    environment: 'production'
  });
});

test('@PROD-SAFE API health endpoints', async ({ request }) => {
  // Backend health
  const backendHealth = await request.get('/api/actuator/health');
  expect(backendHealth.status()).toBe(200);
  expect(await backendHealth.json()).toMatchObject({ status: 'UP' });
  
  // Loki BFF health
  const lokiHealth = await request.get('/api/loki/health');
  expect(lokiHealth.status()).toBe(200);
  
  // Metamodel API
  const metamodelHealth = await request.get('/api/v1/metamodel/health');
  expect(metamodelHealth.status()).toBe(200);
});

test('@PROD-SAFE Key views accessible', async ({ page }) => {
  await loginAs(page, prodReadOnlyUser);
  
  // Metamodel Studio
  await page.goto('/metamodel-studio');
  await expect(page.locator('h1:has-text("Metamodel Studio")')).toBeVisible();
  
  // Workflow Dashboard
  await page.goto('/workflows');
  await expect(page.locator('[data-testid="workflow-list"]')).toBeVisible();
  
  // Log Viewer
  await page.goto('/logs');
  await expect(page.locator('[data-testid="log-search"]')).toBeVisible();
  
  // NO errors in console
  const errors = page.locator('.error-message');
  await expect(errors).toHaveCount(0);
});

test('@PROD-SAFE NO data modifications allowed', async ({ page }) => {
  await loginAs(page, prodReadOnlyUser);
  
  // Verify create/edit/delete buttons NOT visible
  await page.goto('/metamodel-studio');
  await expect(page.locator('[data-testid="new-entity-btn"]')).not.toBeVisible();
  
  // API write operations → 403 Forbidden
  const createResponse = await page.request.post('/api/v1/entities', {
    data: { name: 'Test Entity' }
  });
  expect(createResponse.status()).toBe(403);
});
```

**CI Integration:**

```yaml
# .github/workflows/prod-safe-checks.yml
name: Production Safe Checks

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Environment to test'
        required: true
        default: 'production'
        type: choice
        options:
          - staging
          - production

jobs:
  prod-safe:
    runs-on: ubuntu-latest
    environment: ${{ github.event.inputs.environment }}
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Run prod-safe E2E checks
        run: npm run test:prod-safe
        env:
          BASE_URL: ${{ secrets.PROD_BASE_URL }}
          TEST_USER: ${{ secrets.PROD_READONLY_USER }}
          TEST_PASSWORD: ${{ secrets.PROD_READONLY_PASSWORD }}
      
      - name: Publish metrics
        if: always()
        run: npm run test:export-metrics
      
      - name: Notify on failure
        if: failure()
        run: |
          curl -X POST ${{ secrets.SLACK_WEBHOOK }} \
            -H 'Content-Type: application/json' \
            -d '{"text":"❌ Prod-safe checks FAILED on ${{ github.event.inputs.environment }}"}'
```

**Security:**
- Test user credentials → GitHub Secrets (ne hardcoded)
- Read-only role enforcement (Keycloak RBAC)
- Rate limiting (max 10 requests/minute)

**LOC:** ~300 (tests: ~200, CI config: ~100)  
**Effort:** ~6h  
**Priority:** MEDIUM (Phase 2)  
**Status:** 🔵 **TODO**

**Details:** [E2E21 Story](./stories/E2E21-prod-safe-non-invasive-checks/README.md) - **TODO: Vytvořit**

---

### Phase 2: Stabilita & Kvalita (continued)

#### E2E6: API Contract Testing

> **Breaking changes:** Detekce změn v API pomocí OpenAPI/JSON schema

**As a** developer  
**I want** contract testy pro BFF API  
**So that** detekuji breaking changes před deployem

**Acceptance Criteria:**

✅ Contract testy pro klíčové BFF API:
- Metamodel BFF (entity schema, CRUD)
- Workflow BFF (instance, transitions)
- Loki BFF (log query, filters)
- Auth/Tenant API (login, tenant info)

✅ OpenAPI/JSON schema validation  
✅ CI integrace (fail build na breaking change)

**Scope:**
- OpenAPI specs pro BFF
- Contract test runner (Pact/Portman)
- JSON schema assertions
- CI workflow

**Status:** 🔵 **TODO** (Phase 2 - Stabilita, focus na klíčové BFF)

**Poznámka:** Cíl je detect breaking changes, ne API management platform.

**Details:** [E2E6 Story](./stories/E2E6-api-contract-testing/README.md)

---

#### E2E11: CI/CD Quality Gates

> **Rozumné gates:** PR checks (unit + IT + smoke), optional full E2E

**As a** DevOps engineer  
**I want** rozumné quality gates  
**So that** PR checks jsou rychlé a mandatory E2E nepřetěžuje CI

**Acceptance Criteria:**

✅ **PR Pipeline (mandatory):**
- Unit testy (BE + FE)
- Integration testy (BE)
- Smoke E2E (5-7 min)
- Coverage: 70-80% BE, 60% FE

✅ **Full/Regression E2E (optional):**
- Manual trigger nebo nightly
- Nepřetěžuje PR reviews

✅ Žádné hard gates na experimentální testy

**Scope:**
- PR quality gates workflow
- Full E2E workflow (manual/nightly)
- Coverage thresholds
- Fail fast strategy

**Status:** 🔵 **TODO** (Phase 2 - Stabilita, rozumné gates bez přílišných omezení)

**Details:** [E2E11 Story](./stories/E2E11-ci-cd-quality-gates-implementation-tasks/README.md)

---

#### E2E5: Accessibility (a11y) Testing

> **Incremental:** Základní a11y checks, best effort

**As a** developer  
**I want** základní accessibility checks  
**So that** klíčové stránky splňují WCAG základy

**Acceptance Criteria:**

✅ Axe/pa11y check na 3-5 klíčových stránkách (Login, Dashboard, Metamodel, Workflow)  
✅ WCAG 2.1 Level A checks  
✅ Incremental/best effort (ne hard gate)

**Scope:**
- Axe-core integrace
- A11y checks na vybrané stránky
- Reportování (ne auto-fail)
- Optional CI check

**Poznámka:** Označeno jako "incremental / best effort", ne mandatory blocker.

**Status:** ✅ **DONE** (Wave 1, červenec 2024) - Best effort, non-blocking

**Details:** [E2E5 Story](./stories/E2E5-accessibility-a11y-testing/README.md)

---

### Phase 3: Nadstavba (NICE TO HAVE)

#### E2E4: Visual Regression Testing

> **OPTIONAL:** Vizuální regrese pro pár kritických obrazovek

**As a** developer  
**I want** visual regression checks  
**So that** detekovány nechtěné UI změny

**Acceptance Criteria:**

✅ Vizuální regrese na 2-3 stránkách (Login, Dashboard, Metamodel Studio)  
✅ Percy/Chromatic/Playwright screenshots  
✅ Explicitně OPTIONAL (není blokátor)

**Scope:**
- Percy nebo Chromatic setup
- Screenshots pro 2-3 stránky
- Optional CI

**Status:** 🔵 **TODO** (Phase 3 - OPTIONAL, low priority)

**Poznámka:** Phase 3 - volitelné, low priority.

**Details:** [E2E4 Story](./stories/E2E4-visual-regression-testing/README.md)

---

#### E2E7: Performance Testing

> **OPTIONAL:** Performance baseline pro 2-3 scénáře

**As a** developer  
**I want** performance baseline  
**So that** detekovány výkonnostní regrese

**Acceptance Criteria:**

✅ Performance testy (Login < 2s, Search < 1s, Workflow step < 500ms)  
✅ K6/Lighthouse/Playwright performance API  
✅ Neblokující (samostatně spouštěné)

**Scope:**
- K6 nebo Playwright perf API
- 2-3 testy
- Baseline measurements
- Optional CI (nightly)

**Status:** 🔵 **TODO** (Phase 3 - OPTIONAL, non-blocking)

**Poznámka:** Phase 3 - volitelné, není v PR.

**Details:** [E2E7 Story](./stories/E2E7-performance-testing/README.md)

---

#### E2E8: Test Reporting & Overview

> **OPTIONAL, zjednodušeno:** Script z JUnit/Playwright reportů, žádná DB

**As a** developer  
**I want** jednoduchý report overview  
**So that** vidím které testy prošly/selhaly

**Acceptance Criteria:**

✅ Script parsující JUnit/Playwright XML/JSON reporty  
✅ Generuje HTML/Markdown overview (test ID, tags, pass/fail, duration)  
✅ ŽÁDNÁ vlastní databáze  
✅ ŽÁDNÉ API

**Scope:**
- Node.js/Python parser script
- HTML output
- GitHub Pages (optional)
- Žádná DB, žádné API

**Status:** 🔵 **TODO** (Phase 3 - OPTIONAL, zjednodušeno oproti S8)

**Poznámka:** Phase 3 - zjednodušené oproti původní S8 (Test Registry s DB).

**Details:** [E2E8 Story](./stories/E2E8-s8-implementation-tasks/README.md)

---

#### E2E10: Coverage Dashboard

> **OPTIONAL, zjednodušeno:** JaCoCo/Playwright HTML reports, žádná Grafana

**As a** developer  
**I want** přehled coverage metrik  
**So that** vidím pokrytí kódu

**Acceptance Criteria:**

✅ Standardní coverage reports (JaCoCo, Playwright, Vitest) → HTML  
✅ GitHub Pages publikace (optional)  
✅ ŽÁDNÁ Grafana závislost  
✅ ŽÁDNÁ vlastní dashboard app

**Scope:**
- JaCoCo/Istanbul coverage config
- HTML report generation
- GitHub Pages deploy
- Žádná custom app

**Status:** 🔵 **TODO** (Phase 3 - OPTIONAL, bez Grafany)

**Poznámka:** Phase 3 - zjednodušené, bez Grafany.

**Details:** [E2E10 Story](./stories/E2E10-coverage-dashboard-implementation-tasks/README.md)

---

## 🎯 Definition of Done (Phase 1)

- [ ] Playwright framework setup (E2E1)
- [ ] Page Object Model pro 5 klíčových stránek (E2E2)
- [ ] Test tagging system (@SMOKE, @CRITICAL, @CORE-XXX) (E2E9)
- [ ] Testing standards guide dokumentace (E2E12)
- [ ] Mock services (WireMock pro Keycloak, externí API) (E2E13)
- [ ] Test data management (seeders, cleanup, production safety) (E2E14)
- [ ] GitHub Actions CI/CD dokumentace (E2E15) ✅ DONE
- [ ] Smoke tests (4 scénáře: login, CRUD, workflow, logs) (E2E16)
- [ ] Security E2E tests (tenant isolation, RBAC, auth) (E2E17)
- [ ] PR pipeline (unit + IT + smoke E2E)
- [ ] Production safety checks (no test_ data in prod)

---

## 📈 Success Metrics

- **Smoke Testy:** < 7 min execution time (kritické cesty covered)
- **PR Pipeline:** < 20 min total (unit + IT + smoke)
- **Test Data Safety:** 0 test users/tenants v produkci (automated guards)
- **CI Reliability:** < 5% failed builds kvůli flaky testům
- **Adoption:** Všichni devs píší smoke testy pro nové features
- **Coverage:** 70-80% line coverage (BE), 60% (FE)

---

## 🔗 Dependencies

- **EPIC-001**: Backlog system (User Stories pro mapování)
- **EPIC-003**: CI/CD pipeline (GitHub Actions workflows)
- Playwright 1.42+
- JUnit 5
- WireMock 2.35+
- Testcontainers
- PostgreSQL (pro integration testy)
- core-platform.local environment (Docker setup)

---

## 📅 Implementation Roadmap

### Phase 1: Foundation (8-10 týdnů, MUST HAVE)

**Week 1-2: Playwright Setup & POM**
- E2E1: Playwright framework setup
- E2E2: Page Object Model (5 page objects)
- První smoke test (login)

**Week 3: Smoke Tests & Environment**
- E2E16: Environment & Smoke Alignment (4 smoke scénáře)
- Health check script
- Environment dokumentace

**Week 4-5: Test Data & Mocking**
- E2E14: Test Data Management (seeders, cleanup, production safety)
- E2E13: Mock Services (WireMock, Keycloak mocks)

**Week 6: Security Tests**
- E2E17: Security & Negative E2E Scenarios (5 security testů)

**Week 7: Tagging & Standards**
- E2E9: Test Tagging System (@SMOKE, @CRITICAL)
- E2E12: Testing Standards Guide

**Week 8-10: CI/CD Integration**
- E2E11: CI/CD Quality Gates (PR pipeline)
- E2E15: GitHub Actions documentation ✅ (already done)
- Integration všech komponent

### Phase 2: Stabilita (4-5 týdnů)

**Week 11-12: API Contract Testing**
- E2E6: API Contract Testing (Metamodel, Workflow, Loki BFF)

**Week 13: Accessibility**
- E2E5: Accessibility Testing (3-5 klíčových stránek)

**Week 14-15: Quality Gates Tuning**
- Optimalizace PR pipeline
- Rozšíření coverage

### Phase 3: Nadstavba (4-6 týdnů, OPTIONAL)

**Week 16-17: Visual Regression (optional)**
- E2E4: Visual Regression Testing (2-3 stránky)

**Week 18-19: Performance (optional)**
- E2E7: Performance Testing (baseline measurements)

**Week 20-21: Reporting (optional)**
- E2E8: Test Reporting & Overview (script z reportů)
- E2E10: Coverage Dashboard (HTML reports, GitHub Pages)

---

## 🔄 Aktuální Stav (Status Tracking)

| Story | Status | Progress | Notes |
|-------|--------|----------|-------|
| E2E1 | 🔵 TODO | 0% | Playwright setup |
| E2E2 | 🔵 TODO | 0% | POM - 5 page objects |
| E2E5 | 🔵 TODO | 0% | Phase 2 - Accessibility (incremental) |
| E2E6 | 🔵 TODO | 0% | Phase 2 - API Contract Testing |
| E2E9 | 🔵 TODO | 0% | Test tagging (@SMOKE, @CRITICAL) |
| E2E11 | 🔵 TODO | 0% | Phase 2 - Quality gates |
| E2E12 | 🔵 TODO | 0% | Testing guide |
| E2E13 | 🔵 TODO | 0% | Mock services (Keycloak, ext API) |
| E2E14 | 🔵 TODO | 0% | Test data + production safety |
| E2E15 | ✅ DONE | 100% | CI/CD workflows dokumentace |
| E2E16 | 🔵 TODO | 0% | **NEW!** Smoke tests environment |
| E2E17 | 🔵 TODO | 0% | **NEW!** Security & negative tests |
| E2E4 | 🔵 TODO | 0% | Phase 3 - Visual regression (OPTIONAL) |
| E2E7 | 🔵 TODO | 0% | Phase 3 - Performance (OPTIONAL) |
| E2E8 | 🔵 TODO | 0% | Phase 3 - Test reporting (OPTIONAL) |
| E2E10 | 🔵 TODO | 0% | Phase 3 - Coverage dashboard (OPTIONAL) |

### Current Focus
🎯 **Phase 1 Foundation** - Preparing for implementation  
✅ E2E15 dokumentace kompletní  
🔜 Next up: E2E1 (Playwright setup)

### Blockers
- Žádné aktuální blokátory
- GitHub Actions workflows disabled během EPIC-017 (expected)

---

**Total Effort:** ~106 hours (~13 týdnů)  
**Priority:** P0 (Foundation for quality assurance)  
**Value:** Stabilní E2E infrastruktura + pragmatický přístup + udržitelnost

**Last Updated:** 9. listopadu 2025
