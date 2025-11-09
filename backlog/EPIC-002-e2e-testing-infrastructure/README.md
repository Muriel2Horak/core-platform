# EPIC-002: E2E Testing Infrastructure

**Status:** 🔵 **IN PROGRESS**  
**Priority:** P0 (Critical Foundation)  
**Effort:** ~80 hodin  
**LOC:** ~6,000 řádků

---

## 🎯 Cíle EPICU

**Stabilní, pragmatická a dlouhodobě udržitelná E2E/testovací infrastruktura** pro core-platform, která pokryje klíčové scénáře bez přílišné komplexity.

### Hlavní Cíle

1. **Stabilní E2E Framework (Playwright)**
   - Testy běží nad reálným prostředím `core-platform.local`
   - Konzistentní struktura: Page Object Model (POM), sdílené helpery
   - Jasné tagování testů pro organizaci a filtrování

2. **Spolehlivé Smoke/E2E Scénáře**
   - **Login** přes Keycloak SSO
   - **Základní práce s entitami** a Metamodel UI
   - **Workflow** (vytvoření instance, přechod stavů)
   - **Monitoring** (Loki Log Viewer UI)

3. **API Contract Testing**
   - Základní contract testy pro kritické BFF API
   - Detekce breaking changes v API
   - OpenAPI/JSON schema validation

4. **Mock Services & Test Data**
   - Mock pro externí závislosti (deterministické testy)
   - Automatické vytváření a cleanup test dat
   - Izolace od produkčních dat

5. **CI Pipeline**
   - **PR checks:** Unit + Integration + Smoke E2E (mandatory)
   - **Full/Regression E2E:** Volitelná, manuálně spouštěná
   - Rozumné quality gates (ne "vše nebo nic")

6. **Testing Guidelines**
   - Jasná dokumentace jak psát testy
   - Konvence pro tagging, struktu, helpers
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
│   ├── Smoke Tests (5-7 min) - Kritické cesty (login, základní CRUD)
│   ├── Full E2E (20-30 min) - Kompletní scénáře (workflow, monitoring)
│   ├── Security Tests - Tenant isolation, role-based access
│   └── Test Tags: @SMOKE, @CRITICAL, @REGRESSION, @CORE-XXX
│
├── Page Object Model (POM)
│   ├── LoginPage, MainLayoutPage
│   ├── MetamodelStudioPage
│   ├── WorkflowPage
│   └── LokiLogViewerPage
│
├── API Contract Tests
│   ├── Metamodel BFF API
│   ├── Workflow BFF API
│   ├── Loki BFF API
│   └── Auth/Tenant API
│
├── Mock Services
│   ├── Keycloak Mock (pro některé scénáře)
│   ├── Externí API Mocks
│   └── Loki je real (není mockovaný)
│
└── Test Data Management
    ├── Seed Scripts (users, tenants, roles)
    ├── Clean Scripts (cleanup po testech)
    ├── Test Tenant (izolované prostředí)
    └── Opakovatelnost (deterministické testy)
```

### Test Types

| Type | Framework | Purpose | Run Time | Trigger |
|------|-----------|---------|----------|---------|
| **Smoke E2E** | Playwright | Rychlá validace kritických cest | 5-7 min | PR mandatory |
| **Full E2E** | Playwright | Kompletní flow (workflow, monitoring) | 20-30 min | Manual/nightly |
| **Security E2E** | Playwright | Tenant isolation, RBAC | 5-10 min | PR/nightly |
| **API Contract** | OpenAPI/JSON Schema | Detekce breaking changes | 3-5 min | PR mandatory |
| **Unit (BE)** | JUnit | Service layer, business logic | 5-10 min | PR mandatory |
| **Unit (FE)** | Vitest | React components, hooks | 2-5 min | PR mandatory |
| **Integration (BE)** | Testcontainers | DB, Kafka, Redis | 10-15 min | PR mandatory |

---

## 📊 Fázování

### Phase 1 – Foundation (MUST HAVE)

**Cíl:** Základní funkční E2E infrastruktura pro klíčové scénáře

**Stories:**
- E2E1: Playwright Test Framework Setup
- E2E2: Page Object Model (POM)
- E2E9: Test Tagging System (@SMOKE, @CRITICAL, @CORE-XXX)
- E2E12: Testing Standards Guide
- E2E13: Mock Services Integration
- E2E14: Test Data Management
- E2E15: GitHub Actions CI/CD Workflows
- **E2E16: Environment & Smoke Alignment** (nová)
- **E2E17: Security & Negative E2E Scenarios** (nová)

**Výstup:**
- ✅ Funkční Playwright setup s POM
- ✅ Smoke scénáře (login, CRUD, workflow základy)
- ✅ Test data s production safety
- ✅ Mock pro kritické závislosti
- ✅ CI pipeline (smoke E2E v PR)

### Phase 2 – Stabilita & Kvalita

**Cíl:** Rozšíření coverage, API contract testy, rozumné quality gates

**Stories:**
- E2E6: API Contract Testing (upraveno - focus na klíčové BFF)
- E2E11: CI/CD Quality Gates (upraveno - rozumné thresholdy)
- E2E5: Accessibility Testing (upraveno - incremental, best effort)

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
| [E2E14](#e2e14-test-data-management) | Test Data Management | 1 | ✅ DONE | ~800 | 12h | Test data + safety |
| [E2E15](#e2e15-github-actions-cicd-workflows) | GitHub Actions CI/CD Workflows | 1 | ✅ DONE | ~800 | 4h | CI/CD dokumentace |
| [E2E16](#e2e16-environment--smoke-alignment) | **Environment & Smoke Alignment** | 1 | 🔵 TODO | ~400 | 6h | **Smoke testy** |
| [E2E17](#e2e17-security--negative-e2e-scenarios) | **Security & Negative E2E Scenarios** | 1 | 🔵 TODO | ~500 | 8h | **Security** |
| [E2E6](#e2e6-api-contract-testing) | API Contract Testing | 2 | 🔵 TODO | ~400 | 6h | Breaking changes |
| [E2E11](#e2e11-cicd-quality-gates) | CI/CD Quality Gates | 2 | 🔵 TODO | ~300 | 5h | Automatická validace |
| [E2E5](#e2e5-accessibility-a11y-testing) | Accessibility (a11y) Testing | 2 | ✅ DONE | ~300 | 6h | WCAG checks |
| [E2E4](#e2e4-visual-regression-testing) | Visual Regression Testing | 3 | 🔵 TODO | ~400 | 8h | **OPTIONAL** |
| [E2E7](#e2e7-performance-testing) | Performance Testing | 3 | 🔵 TODO | ~300 | 6h | **OPTIONAL** |
| [E2E8](#e2e8-test-reporting--overview) | Test Reporting & Overview | 3 | 🔵 TODO | ~300 | 5h | **OPTIONAL** |
| [E2E10](#e2e10-coverage-dashboard) | Coverage Dashboard | 3 | 🔵 TODO | ~300 | 4h | **OPTIONAL** |
| **TOTAL** | | | **5/16** | **~7,200** | **~106h** | **Pragmatic E2E infrastructure** |

**Poznámky:**
- **Phase 1 (MUST HAVE):** 9 stories, ~52h - Základní funkční infrastruktura (5 done, 4 todo)
- **Phase 2 (Stabilita):** 3 stories, ~17h - Rozšíření coverage a quality (1 done, 2 todo)
- **Phase 3 (NICE TO HAVE):** 4 stories, ~23h - Volitelné nadstavby (0 done, 4 todo)

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

### Phase 2: Stabilita & Kvalita

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
