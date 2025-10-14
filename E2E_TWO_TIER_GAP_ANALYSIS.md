# E2E Two-Tier Testing - Gap Analysis & Implementation Plan

**Datum:** 14. října 2025  
**Goal:** Dvouvrstvé automatizované testování: PRE-DEPLOY gate (fast) + POST-DEPLOY validation (full E2E)

---

## ✅ Co JIŽ MÁME

### Infrastruktura
- ✅ `e2e/config/read-config.ts` - čte env + YAML (Keycloak, baseURL, users)
- ✅ `e2e/playwright.config.ts` - baseURL, ignoreHTTPSErrors, trace:on-first-retry
- ✅ `.github/workflows/e2e.yml` - základní E2E workflow (self-hosted runner)
- ✅ Existující E2E testy v `frontend/tests/e2e/`:
  - `gui-smoke.spec.ts` (4 testy)
  - `reports.spec.ts` (8 testů)
  - `streaming/dashboard.spec.ts` (9 testů)
  - `workflow-execute.spec.ts` (4 testy)

### Konfigurace
- ✅ ENV overrides: `E2E_BASE_URL`, `E2E_IGNORE_TLS`, `E2E_USER`, `E2E_PASS`
- ✅ Keycloak client "web" s redirect URIs pro oba domény
- ✅ Test users: `test` (user), `test_admin` (admin) s heslem `Test.1234`

---

## ❌ Co CHYBÍ pro PRE-DEPLOY (fast gate)

### 1. Struktura složek
```
e2e/
├── helpers/          ❌ PRÁZDNÉ - chybí login.ts, api.ts
├── scripts/          ❌ NEEXISTUJE - chybí scaffold.ts, teardown.ts
└── specs/
    ├── pre/          ❌ NEEXISTUJE - rychlé smoke testy
    └── post/         ❌ NEEXISTUJE - plné E2E scénáře
```

### 2. Helper moduly
- ❌ `e2e/helpers/login.ts` - Keycloak UI login helper
- ❌ `e2e/helpers/api.ts` - thin wrappers pro admin/public APIs (bez DB)
- ❌ `e2e/helpers/await-until.ts` - exponenciální backoff helper

### 3. PRE-DEPLOY testy (e2e/specs/pre/)
- ❌ `01_login_smoke.spec.ts` - login přes GUI, redirect na dashboard
- ❌ `02_menu_rbac_smoke.spec.ts` - menu odpovídá rolím test uživatele
- ❌ `03_entity_grid_form_smoke.spec.ts` - grid render, popup detail, CRUD
- ❌ `04_workflow_panel_smoke.spec.ts` - WF panel, stav, přechody, guards

### 4. Playwright konfigurace pro PRE
- ❌ **Project "pre"** v `playwright.config.ts`:
  ```ts
  {
    name: 'pre',
    testDir: './specs/pre',
    timeout: 30 * 1000, // Kratší timeout pro smoke
    use: {
      baseURL: process.env.PRE_BASE_URL || 'https://core-platform.local',
    }
  }
  ```

### 5. CI workflow PRE-DEPLOY
- ❌ `.github/workflows/pre-deploy.yml`:
  - Trigger: `push (main)`
  - Steps: checkout → node setup → playwright install → **pnpm test:e2e --project=pre**
  - Artifacts: HTML report + traces
  - **COPILOT_HINT** výstup při selhání

---

## ❌ Co CHYBÍ pro POST-DEPLOY (full E2E)

### 1. Scaffolding & Teardown scripty
- ❌ `e2e/scripts/scaffold.ts`:
  - Založí ephemeral tenant + test uživatele (admin API)
  - Vytvoří TEST entitu `PersonTest_${rand}` s UI-spec
  - Vytvoří TEST workflow (DRAFT→APPROVED)
  - WireMock stub pro REST kroky
  - Vrátí JSON: `{ tenantId, user, entityName, wfId, urls }`

- ❌ `e2e/scripts/teardown.ts`:
  - Purge entitu & specVersion
  - Smaže test uživatele/role
  - Drop topics/artefakty
  - Smaže MinIO soubory (test namespace)

### 2. POST-DEPLOY testy (e2e/specs/post/)
- ❌ `10_auth_profile_update.spec.ts` - login → změň displayName → ověř v Directory
- ❌ `20_admin_create_entity_and_ui.spec.ts` - vytvoř entitu přes Studio → verify menu
- ❌ `30_workflow_create_and_run.spec.ts` - definuj WF → execute → timeline/forecast
- ❌ `40_directory_consistency.spec.ts` - vyhledej user → verify metadata sync
- ❌ `50_cleanup_visibility.spec.ts` - teardown → verify TEST entita zmizela

### 3. Playwright konfigurace pro POST
- ❌ **Project "post"** v `playwright.config.ts`:
  ```ts
  {
    name: 'post',
    testDir: './specs/post',
    timeout: 120 * 1000, // Delší timeout pro full flows
    use: {
      baseURL: process.env.POST_BASE_URL || 'https://staging.core-platform.company',
    }
  }
  ```

### 4. CI workflow POST-DEPLOY
- ❌ `.github/workflows/post-deploy.yml`:
  - Trigger: `workflow_run` po deploy workflow
  - runs-on: **self-hosted**
  - Steps:
    1. checkout → node setup → playwright install
    2. **node e2e/scripts/scaffold.ts --env POST**
    3. **pnpm test:e2e --project=post**
    4. **node e2e/scripts/teardown.ts --env POST**
  - Artifacts: report + traces + **e2e-json summaries**
  - **COPILOT_HINT + JSON** výstup při selhání

### 5. COPILOT výstup formát
- ❌ JSON blok s `##[COPILOT_START_JSON]` / `##[COPILOT_END_JSON]`:
  ```json
  {
    "suite": "post-deploy",
    "env": "$POST_BASE_URL",
    "failedTests": [...],
    "suspectedCauses": ["exact messages"],
    "recommendedFixes": [
      {
        "title": "Keycloak redirect mismatch",
        "files": ["keycloak.json"],
        "steps": ["update redirect URIs"]
      }
    ]
  }
  ```
- ❌ Max 5x `COPILOT_HINT: ...` s konkrétním diffem/konfigurákem

---

## 📋 IMPLEMENTAČNÍ PLÁN (Priority)

### PHASE 1: PRE-DEPLOY Foundation (Essential)
**Cíl:** Funkční fast gate ≤5–7 min proti https://core-platform.local

1. **Helpers (1-2h)**
   - [ ] `e2e/helpers/login.ts` - Keycloak UI login
   - [ ] `e2e/helpers/api.ts` - API wrappers (GET /api/users/me, /api/tenants/me, /api/ui-specs)
   - [ ] `e2e/helpers/await-until.ts` - exponenciální backoff

2. **PRE Smoke Tests (2-3h)**
   - [ ] `e2e/specs/pre/01_login_smoke.spec.ts`
   - [ ] `e2e/specs/pre/02_menu_rbac_smoke.spec.ts`
   - [ ] `e2e/specs/pre/03_entity_grid_form_smoke.spec.ts`
   - [ ] `e2e/specs/pre/04_workflow_panel_smoke.spec.ts`

3. **Playwright Config PRE (30min)**
   - [ ] Přidat project "pre" do `playwright.config.ts`
   - [ ] `PRE_BASE_URL` env override

4. **CI Workflow PRE (1h)**
   - [ ] `.github/workflows/pre-deploy.yml` - push(main) trigger
   - [ ] COPILOT_HINT při selhání

### PHASE 2: POST-DEPLOY Foundation (Complex)
**Cíl:** Plné E2E s ephemeral data proti $POST_BASE_URL

5. **Scaffolding (3-4h)**
   - [ ] `e2e/scripts/scaffold.ts` - tenant, user, entity, WF, WireMock stub
   - [ ] `e2e/scripts/teardown.ts` - cleanup vše

6. **POST Full E2E Tests (4-5h)**
   - [ ] `e2e/specs/post/10_auth_profile_update.spec.ts`
   - [ ] `e2e/specs/post/20_admin_create_entity_and_ui.spec.ts`
   - [ ] `e2e/specs/post/30_workflow_create_and_run.spec.ts`
   - [ ] `e2e/specs/post/40_directory_consistency.spec.ts`
   - [ ] `e2e/specs/post/50_cleanup_visibility.spec.ts`

7. **Playwright Config POST (30min)**
   - [ ] Přidat project "post" do `playwright.config.ts`
   - [ ] `POST_BASE_URL` env override

8. **CI Workflow POST (2h)**
   - [ ] `.github/workflows/post-deploy.yml` - workflow_run trigger
   - [ ] Scaffold → Test → Teardown orchestrace
   - [ ] COPILOT JSON + HINT výstup

### PHASE 3: Robustness & Observability (Polish)

9. **Error Handling & Copilot (2h)**
   - [ ] Playwright reporter plugin pro COPILOT_HINT
   - [ ] JSON summary generátor
   - [ ] Server log fetch (Loki API) při selhání

10. **Documentation (1h)**
    - [ ] `e2e/README.md` - jak spustit PRE vs POST
    - [ ] Update `TEST_DEPLOYMENT_FLOW.md`

---

## 📊 ESTIMACE

| Phase | Effort | Priority | Dependencies |
|-------|--------|----------|--------------|
| Phase 1: PRE-DEPLOY | **4-6h** | 🔴 Critical | Žádné |
| Phase 2: POST-DEPLOY | **9-11h** | 🟡 High | Admin API, WireMock |
| Phase 3: Polish | **3h** | 🟢 Medium | Phase 1+2 |
| **TOTAL** | **16-20h** | | |

---

## 🎯 DEFINITION OF DONE

### PRE-DEPLOY (Gate)
- ✅ 4 smoke testy zelené proti https://core-platform.local (≤5min)
- ✅ CI workflow `.github/workflows/pre-deploy.yml` push(main) trigger
- ✅ Artifacts: HTML report + traces
- ✅ COPILOT_HINT při selhání

### POST-DEPLOY (Validation)
- ✅ 5 full E2E testů zelené proti $POST_BASE_URL (≤15min)
- ✅ Ephemeral data scaffolding + teardown
- ✅ CI workflow `.github/workflows/post-deploy.yml` po deploy
- ✅ Artifacts: report + traces + COPILOT JSON
- ✅ Cleanup vždy smaže test data

### Overall
- ✅ Trunk-based: commit to main spustí PRE gate
- ✅ Pouze GUI + public API testy (NO DB poking)
- ✅ Trace + COPILOT hints pro každý fail

---

## 🚀 QUICK START (po dokončení)

### PRE-DEPLOY (lokální)
```bash
# Rychlé smoke testy proti lokálu
pnpm test:e2e --project=pre

# S custom URL
PRE_BASE_URL=https://dev.core-platform.local pnpm test:e2e --project=pre
```

### POST-DEPLOY (staging/prod)
```bash
# Scaffold → Test → Teardown
node e2e/scripts/scaffold.ts --env staging
POST_BASE_URL=https://staging.core-platform.company pnpm test:e2e --project=post
node e2e/scripts/teardown.ts --env staging
```

---

## 📝 NOTES

- **Aktuální E2E testy** v `frontend/tests/e2e/` ZŮSTANOU (25 testů, zaměřené na monitoring/reporting)
- **Nové testy** v `e2e/specs/` budou **ODDĚLENENÉ** (trunk-based gate + full validation)
- **WireMock** už máme integrovaný v testech (`c.m.c.t.wiremock.WireMockExtension`)
- **Admin API** endpoints už existují (`/api/admin/...`, `/api/ui-specs/...`)
