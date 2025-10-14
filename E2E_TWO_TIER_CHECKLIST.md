# E2E Two-Tier Testing - Implementation Checklist

**Status:** 📊 Planning Phase  
**Cíl:** PRE-DEPLOY gate (≤5min) + POST-DEPLOY validation (≤15min)

---

## 🔴 PHASE 1: PRE-DEPLOY Foundation (CRITICAL - 4-6h)

### Helpers (1-2h)
- [ ] `e2e/helpers/login.ts`
  - [ ] `loginViaKeycloak(page, username, password)` - Keycloak UI login flow
  - [ ] `ensureAuthenticated(page)` - check /api/users/me
  - [ ] Handle redirect to dashboard

- [ ] `e2e/helpers/api.ts`
  - [ ] `getUserProfile(baseURL)` - GET /api/users/me
  - [ ] `getTenantInfo(baseURL)` - GET /api/tenants/me
  - [ ] `getUISpecs(baseURL, entity?)` - GET /api/ui-specs
  - [ ] `getMenuConfig(baseURL)` - derive from UI specs + RBAC

- [ ] `e2e/helpers/await-until.ts`
  - [ ] `awaitUntil(selector, options)` - exponential backoff
  - [ ] `waitForStaleToFresh(page, entityId)` - poll presence state
  - [ ] `waitForWorkflowState(page, wfId, state)` - poll WF timeline

### PRE Smoke Tests (2-3h)
- [ ] `e2e/specs/pre/01_login_smoke.spec.ts`
  - [ ] Test: Login as `test` user → redirect to dashboard
  - [ ] Test: Login as `test_admin` → redirect to admin dashboard
  - [ ] Assert: No 401/403 errors in console

- [ ] `e2e/specs/pre/02_menu_rbac_smoke.spec.ts`
  - [ ] Load UISpec config for `test` user
  - [ ] Assert: Menu items match user roles (no forbidden items visible)
  - [ ] Assert: Admin menu hidden for non-admin

- [ ] `e2e/specs/pre/03_entity_grid_form_smoke.spec.ts`
  - [ ] Navigate to existing entity (e.g., "Users Directory")
  - [ ] Assert: Grid renders with rows
  - [ ] Open detail popup → assert fields render
  - [ ] If CREATE permission: create record → update → delete
  - [ ] If READ-ONLY: verify no edit buttons visible

- [ ] `e2e/specs/pre/04_workflow_panel_smoke.spec.ts`
  - [ ] Open entity detail with WF panel
  - [ ] Assert: Current state highlighted
  - [ ] Assert: Available transitions visible
  - [ ] Hover disabled edge → assert tooltip shows guard reason
  - [ ] Assert: Timeline shows ENTER/EXIT events

### Playwright Config (30min)
- [ ] Add **project "pre"** to `e2e/playwright.config.ts`:
  ```ts
  {
    name: 'pre',
    testDir: './specs/pre',
    timeout: 30 * 1000,
    use: {
      baseURL: process.env.PRE_BASE_URL || 'https://core-platform.local',
      trace: 'on-first-retry',
    }
  }
  ```
- [ ] Update `package.json` scripts:
  ```json
  "test:e2e:pre": "playwright test --project=pre"
  ```

### CI Workflow (1h)
- [ ] Create `.github/workflows/pre-deploy.yml`:
  - [ ] Trigger: `on: push (branches: [main])`
  - [ ] Jobs:
    - [ ] checkout → setup-node → npm ci
    - [ ] `npx playwright install --with-deps chromium`
    - [ ] `npm run test:e2e:pre`
  - [ ] Artifacts: `playwright-report`, `test-results`
  - [ ] On failure: Output COPILOT_HINT with suspected fixes

- [ ] Add COPILOT_HINT reporter:
  - [ ] Custom Playwright reporter class
  - [ ] Parse failures → suggest fixes (Keycloak redirect, RBAC config)
  - [ ] Print `COPILOT_HINT: [file] [fix]` to console

---

## 🟡 PHASE 2: POST-DEPLOY Foundation (HIGH - 9-11h)

### Scaffolding Scripts (3-4h)
- [ ] `e2e/scripts/scaffold.ts`
  - [ ] CLI arg: `--env [staging|prod]`
  - [ ] Load $POST_BASE_URL from env
  - [ ] Admin API calls:
    - [ ] Create ephemeral tenant `test-e2e-${timestamp}`
    - [ ] Create test user with roles (USER, ADMIN)
    - [ ] Create TEST entity `PersonTest_${rand}` via `/api/admin/entities`
    - [ ] Publish UI-spec for entity
    - [ ] Create TEST workflow (DRAFT→APPROVED, steps: APPROVAL + REST)
  - [ ] If WireMock available: Setup stub for `/api/mock-ok`
  - [ ] Output JSON:
    ```json
    {
      "tenantId": "uuid",
      "user": {"username": "test-e2e-user", "password": "..."},
      "entityName": "PersonTest_abc123",
      "workflowId": "uuid",
      "urls": {
        "base": "https://staging...",
        "entity": "/entities/PersonTest_abc123",
        "workflow": "/workflows/..."
      }
    }
    ```
  - [ ] Save to `e2e/.scaffold-data.json`

- [ ] `e2e/scripts/teardown.ts`
  - [ ] CLI arg: `--env [staging|prod]`
  - [ ] Load `.scaffold-data.json`
  - [ ] Admin API calls:
    - [ ] Delete TEST entity + UI-spec
    - [ ] Delete TEST workflow
    - [ ] Delete test user
    - [ ] Delete tenant (if ephemeral)
  - [ ] Kafka: Drop topics `test-e2e-*` (if admin API available)
  - [ ] MinIO: Delete test namespace objects
  - [ ] Cleanup `.scaffold-data.json`

### POST Full E2E Tests (4-5h)
- [ ] `e2e/specs/post/10_auth_profile_update.spec.ts`
  - [ ] Login via Keycloak
  - [ ] Navigate to "User Profile"
  - [ ] Update `displayName` → "Test User E2E"
  - [ ] Assert: Change visible in "User Directory" (via API /api/users/search)
  - [ ] Assert: Presence event fired (check streaming dashboard badge)

- [ ] `e2e/specs/post/20_admin_create_entity_and_ui.spec.ts`
  - [ ] Login as admin
  - [ ] Navigate to Studio
  - [ ] Create entity `PersonTest_${rand}` via GUI
  - [ ] Add fields: firstName, lastName, validations
  - [ ] Publish + Approve
  - [ ] Assert: UI-spec endpoint returns new entity
  - [ ] Assert: Menu shows new entity
  - [ ] Assert: Grid renders for entity

- [ ] `e2e/specs/post/30_workflow_create_and_run.spec.ts`
  - [ ] Login as admin
  - [ ] Navigate to Studio → Workflows
  - [ ] Create WF: DRAFT→APPROVED
    - [ ] Step A: APPROVAL (single approver)
    - [ ] Step B: SERVICE_REST_SYNC → `/api/mock-ok`
  - [ ] Publish + Approve
  - [ ] Create `PersonTest` record
  - [ ] Open detail → WF panel → "Execute"
  - [ ] APPROVAL step:
    - [ ] Approve as current user
    - [ ] Assert: Timeline shows ENTER→EXIT with duration
  - [ ] REST step:
    - [ ] Assert: WireMock hit (check `/api/wiremock/requests`)
    - [ ] Assert: State RUNNING→SUCCESS
    - [ ] Assert: UI unlocks (stale→fresh)
  - [ ] Assert: Forecast shows next steps
  - [ ] Assert: Disabled edges show guard tooltips

- [ ] `e2e/specs/post/40_directory_consistency.spec.ts`
  - [ ] Search user by updated name (from test 10)
  - [ ] Open user detail
  - [ ] Assert: Metadata synchronized (no DB access, same API as GUI)
  - [ ] Assert: Groups/roles match Keycloak

- [ ] `e2e/specs/post/50_cleanup_visibility.spec.ts`
  - [ ] Run teardown script
  - [ ] Assert: TEST entity removed from menu (non-admin role)
  - [ ] Assert: UI-spec endpoint returns original version
  - [ ] Assert: Grid no longer accessible

### Playwright Config (30min)
- [ ] Add **project "post"** to `e2e/playwright.config.ts`:
  ```ts
  {
    name: 'post',
    testDir: './specs/post',
    timeout: 120 * 1000,
    use: {
      baseURL: process.env.POST_BASE_URL || 'https://staging.core-platform.company',
      trace: 'on',
    }
  }
  ```
- [ ] Update `package.json` scripts:
  ```json
  "test:e2e:post": "playwright test --project=post"
  ```

### CI Workflow (2h)
- [ ] Create `.github/workflows/post-deploy.yml`:
  - [ ] Trigger: `on: workflow_run` (after deploy workflow)
  - [ ] runs-on: **self-hosted**
  - [ ] Jobs:
    - [ ] checkout → setup-node → npm ci
    - [ ] `npx playwright install --with-deps chromium`
    - [ ] `node e2e/scripts/scaffold.ts --env $DEPLOY_ENV`
    - [ ] `npm run test:e2e:post`
    - [ ] `node e2e/scripts/teardown.ts --env $DEPLOY_ENV` (always run)
  - [ ] Artifacts:
    - [ ] `playwright-report`
    - [ ] `test-results`
    - [ ] `e2e-copilot-summary.json`
  - [ ] On failure: 
    - [ ] Fetch last 300 lines server logs (Loki API)
    - [ ] Generate COPILOT JSON + HINTS

- [ ] Add COPILOT JSON reporter:
  - [ ] Custom reporter outputs:
    ```
    ##[COPILOT_START_JSON]
    {
      "suite": "post-deploy",
      "env": "$POST_BASE_URL",
      "failedTests": [...],
      "suspectedCauses": ["exact messages"],
      "recommendedFixes": [
        {
          "title": "Keycloak redirect mismatch",
          "files": ["keycloak/realm.json"],
          "steps": ["Update redirectUris to include $POST_BASE_URL/callback"]
        }
      ]
    }
    ##[COPILOT_END_JSON]
    ```
  - [ ] Max 5x `COPILOT_HINT:` lines with diffs

---

## 🟢 PHASE 3: Polish & Observability (MEDIUM - 3h)

### Error Handling (2h)
- [ ] Playwright custom reporter for COPILOT output
  - [ ] Parse test failures
  - [ ] Match error patterns to known fixes
  - [ ] Generate JSON + HINT output

- [ ] Server log integration
  - [ ] Helper: `fetchLokiLogs(baseURL, since, filter)`
  - [ ] On test failure: Fetch last 5min of logs
  - [ ] Append to test artifacts

- [ ] Trace enhancements
  - [ ] Auto-capture on failure: screenshot + HAR + trace
  - [ ] Video recording for full POST suite

### Documentation (1h)
- [ ] `e2e/README.md`
  - [ ] Architecture overview (PRE vs POST)
  - [ ] How to run locally
  - [ ] How to debug failures
  - [ ] COPILOT hints interpretation

- [ ] Update `TEST_DEPLOYMENT_FLOW.md`
  - [ ] Add PRE-DEPLOY gate section
  - [ ] Add POST-DEPLOY validation section
  - [ ] Workflow diagrams

---

## 📊 PROGRESS TRACKING

| Phase | Tasks | Completed | Progress | ETA |
|-------|-------|-----------|----------|-----|
| Phase 1: PRE-DEPLOY | 14 | 0 | ░░░░░░░░░░ 0% | 4-6h |
| Phase 2: POST-DEPLOY | 22 | 0 | ░░░░░░░░░░ 0% | 9-11h |
| Phase 3: Polish | 6 | 0 | ░░░░░░░░░░ 0% | 3h |
| **TOTAL** | **42** | **0** | **░░░░░░░░░░ 0%** | **16-20h** |

---

## 🎯 IMMEDIATE NEXT STEPS

1. **Start Phase 1** (PRE-DEPLOY Foundation):
   - [ ] Create `e2e/helpers/login.ts` - Keycloak login helper
   - [ ] Create `e2e/helpers/api.ts` - API wrappers
   - [ ] Create first smoke test: `01_login_smoke.spec.ts`

2. **Validate Current Setup**:
   - [ ] Run existing E2E tests: `pnpm test:e2e`
   - [ ] Check Keycloak redirect URIs include both domains
   - [ ] Verify test users `test` and `test_admin` work

3. **Prepare Environment**:
   - [ ] Ensure https://core-platform.local is running
   - [ ] Verify admin API endpoints accessible
   - [ ] Check WireMock integration works

---

## ✅ DEFINITION OF DONE

### PRE-DEPLOY
- [x] Struktura: `e2e/helpers/`, `e2e/specs/pre/`
- [ ] 4 smoke testy zelené (≤5min runtime)
- [ ] CI workflow `.github/workflows/pre-deploy.yml`
- [ ] COPILOT_HINT při selhání
- [ ] Artifacts: HTML report + traces

### POST-DEPLOY
- [ ] Struktura: `e2e/scripts/`, `e2e/specs/post/`
- [ ] Scaffold + Teardown scripty fungují
- [ ] 5 full E2E testů zelené (≤15min runtime)
- [ ] CI workflow `.github/workflows/post-deploy.yml`
- [ ] COPILOT JSON + HINTS při selhání
- [ ] Artifacts: report + traces + summary

### Overall
- [ ] Trunk-based: push(main) → PRE gate
- [ ] Pouze GUI + API testy (NO DB)
- [ ] Dokumentace kompletní
- [ ] All tests green ✅
