# E2E16: Environment & Smoke Alignment

**Status:** 🔵 **TODO**  
**Priority:** P0 (MUST HAVE - Phase 1 Foundation)  
**Effort:** ~6 hodin  
**LOC:** ~400 řádků

---

## 🎯 Cíl Story

**Jasně definované smoke scénáře** pro rychlou validaci kritických cest v core-platform.local prostředí.

**As a** developer  
**I want** jasně definované smoke scénáře  
**So that** můžu rychle validovat kritické cesty (5-7 min)

---

## 📋 Acceptance Criteria

### 1. Environment Documentation
✅ Jasný popis core-platform.local prostředí:
- Docker Compose setup (služby, porty, dependencies)
- SSL certifikáty (self-signed wildcard)
- Keycloak realm konfigurace
- Test users a jejich role
- Network topology (nginx → backend → db, keycloak, loki)

### 2. Smoke Test Scenarios (4 kritické scénáře)

✅ **SMOKE-1: Login Flow**
```typescript
test('SMOKE-1: Login via Keycloak SSO @SMOKE @CRITICAL', async ({ page }) => {
  // 1. Navigate to app
  await page.goto('https://core-platform.local');
  
  // 2. Redirect to Keycloak
  await expect(page).toHaveURL(/admin.core-platform.local.*realms\/admin/);
  
  // 3. Login
  await page.getByLabel('Username').fill('test_admin');
  await page.getByLabel('Password').fill('Test.1234');
  await page.getByRole('button', { name: 'Sign In' }).click();
  
  // 4. Verify redirect to /admin
  await expect(page).toHaveURL(/core-platform.local\/admin/);
  
  // 5. Verify user menu visible
  await expect(page.getByText('test_admin')).toBeVisible();
});
```

✅ **SMOKE-2: Entity CRUD (Metamodel)**
```typescript
test('SMOKE-2: Create and view entity @SMOKE @CRITICAL', async ({ page }) => {
  await loginAsTestAdmin(page);
  
  // 1. Navigate to Metamodel Studio
  await page.goto('/admin/metamodel');
  
  // 2. Create new entity
  await page.getByRole('button', { name: 'New Entity' }).click();
  await page.getByLabel('Entity Name').fill('TestEntity');
  await page.getByLabel('Description').fill('Smoke test entity');
  await page.getByRole('button', { name: 'Save' }).click();
  
  // 3. Verify entity in list
  await expect(page.getByText('TestEntity')).toBeVisible();
  
  // 4. Open entity detail
  await page.getByText('TestEntity').click();
  await expect(page.getByText('Smoke test entity')).toBeVisible();
  
  // 5. Cleanup
  await deleteEntity(page, 'TestEntity');
});
```

✅ **SMOKE-3: Workflow Step Execution**
```typescript
test('SMOKE-3: Execute basic workflow step @SMOKE @CRITICAL', async ({ page }) => {
  await loginAsTestAdmin(page);
  
  // 1. Navigate to Workflow
  await page.goto('/admin/workflow');
  
  // 2. Start new workflow instance
  await page.getByRole('button', { name: 'New Instance' }).click();
  await page.getByLabel('Workflow Type').selectOption('approval');
  await page.getByRole('button', { name: 'Start' }).click();
  
  // 3. Execute transition
  await page.getByRole('button', { name: 'Approve' }).click();
  
  // 4. Verify state change
  await expect(page.getByText('Status: Approved')).toBeVisible();
});
```

✅ **SMOKE-4: Loki Log Viewer**
```typescript
test('SMOKE-4: View logs in Loki Log Viewer @SMOKE @CRITICAL', async ({ page }) => {
  await loginAsTestAdmin(page);
  
  // 1. Navigate to Log Viewer
  await page.goto('/admin/logs');
  
  // 2. Search logs
  await page.getByPlaceholder('Search logs...').fill('INFO');
  await page.getByRole('button', { name: 'Search' }).click();
  
  // 3. Verify log entries visible
  await expect(page.getByRole('table')).toBeVisible();
  await expect(page.locator('tbody tr')).toHaveCount({ min: 1 });
  
  // 4. Filter by time range
  await page.getByLabel('Time Range').selectOption('last-1h');
  await page.getByRole('button', { name: 'Apply' }).click();
  
  // 5. Verify results updated
  await expect(page.locator('tbody tr')).toHaveCount({ min: 1 });
});
```

### 3. Health Check Script

✅ Shell script pro endpoint validation:

```bash
#!/bin/bash
# scripts/smoke-health-check.sh

set -e

DOMAIN="${DOMAIN:-core-platform.local}"
TIMEOUT=5

echo "🔍 Core Platform Health Check"
echo "================================"

# Backend Health
echo -n "Backend Actuator... "
if curl -f -k -s -m $TIMEOUT "https://${DOMAIN}/api/actuator/health" > /dev/null; then
  echo "✅ OK"
else
  echo "❌ FAIL"
  exit 1
fi

# Loki BFF Health
echo -n "Loki BFF... "
if curl -f -k -s -m $TIMEOUT "https://${DOMAIN}/api/loki/health" > /dev/null; then
  echo "✅ OK"
else
  echo "❌ FAIL"
  exit 1
fi

# Keycloak Health
echo -n "Keycloak... "
if curl -f -k -s -m $TIMEOUT "https://admin.${DOMAIN}/health" > /dev/null; then
  echo "✅ OK"
else
  echo "❌ FAIL"
  exit 1
fi

# Frontend (Nginx)
echo -n "Frontend... "
if curl -f -k -s -m $TIMEOUT "https://${DOMAIN}/" > /dev/null; then
  echo "✅ OK"
else
  echo "❌ FAIL"
  exit 1
fi

echo "================================"
echo "✅ All services healthy"
```

### 4. NPM Scripts

✅ Package.json scripts pro smoke testy:

```json
{
  "scripts": {
    "test:smoke": "playwright test --grep @SMOKE",
    "test:smoke:headed": "playwright test --grep @SMOKE --headed",
    "test:smoke:debug": "playwright test --grep @SMOKE --debug",
    "health:check": "bash ../../scripts/smoke-health-check.sh"
  }
}
```

### 5. Makefile Integration

✅ Makefile target:

```makefile
.PHONY: test-smoke
test-smoke: ## Run smoke E2E tests (5-7 min)
	@echo "🧪 Running smoke tests..."
	@bash scripts/smoke-health-check.sh
	@cd e2e && npm run test:smoke
```

---

## 📂 Implementace

### File Structure

```
e2e/
├── specs/
│   └── smoke/
│       ├── login.spec.ts              (SMOKE-1)
│       ├── metamodel-crud.spec.ts     (SMOKE-2)
│       ├── workflow-execution.spec.ts (SMOKE-3)
│       └── loki-logs.spec.ts          (SMOKE-4)
├── helpers/
│   ├── auth.ts                        (login helpers)
│   └── cleanup.ts                     (test data cleanup)
├── fixtures/
│   └── test-users.ts                  (test user constants)
└── docs/
    └── environment.md                 (env documentation)

scripts/
└── smoke-health-check.sh              (health check script)

docs/
└── testing/
    └── smoke-tests.md                 (smoke test documentation)
```

### Environment Documentation

**File:** `e2e/docs/environment.md`

```markdown
# Core Platform Test Environment

## Services

| Service | URL | Port | Purpose |
|---------|-----|------|---------|
| Frontend | https://core-platform.local | 443 | Nginx + React SPA |
| Backend | https://core-platform.local/api | 8080 (internal) | Spring Boot REST API |
| Keycloak | https://admin.core-platform.local | 8443 (internal) | SSO Auth |
| Loki | http://loki:3100 | 3100 (internal) | Log aggregation |
| PostgreSQL | core-db:5432 | 5432 (internal) | Main database |

## Test Users

| Username | Password | Role | Tenant | Purpose |
|----------|----------|------|--------|---------|
| `test_admin` | Test.1234 | ADMIN | admin | Full access smoke tests |
| `test_user` | Test.1234 | USER | admin | Limited access tests |
| `e2e_tenant_a` | Test.1234 | ADMIN | tenant_a | Tenant isolation tests |
| `e2e_tenant_b` | Test.1234 | ADMIN | tenant_b | Tenant isolation tests |

## SSL Certificates

- **Type:** Self-signed wildcard
- **Domain:** `*.core-platform.local`
- **Location:** `docker/ssl/`
- **Valid:** 365 days from generation

## Docker Compose

```yaml
services:
  nginx:
    ports: ["443:443"]
  backend:
    ports: ["8080"] # Internal only
  keycloak:
    ports: ["8443"] # Internal only
  loki:
    ports: ["3100"] # Internal only
  postgres:
    ports: ["5432"] # Internal only
```

## Network Topology

```
Browser (https://core-platform.local)
  ↓
Nginx (SSL termination)
  ↓
Backend (http://backend:8080)
  ↓
├── PostgreSQL (core-db:5432)
├── Keycloak (http://keycloak:8443)
├── Loki (http://loki:3100)
└── Redis (redis:6379)
```

## Environment Variables

```bash
DOMAIN=core-platform.local
KEYCLOAK_BASE_URL=https://admin.core-platform.local
OIDC_ISSUER_URI=https://admin.core-platform.local/realms/admin
DATABASE_URL=jdbc:postgresql://core-db:5432/core
```
```

---

## ✅ Definition of Done

- [ ] 4 smoke test scénáře implementovány (login, CRUD, workflow, logs)
- [ ] Health check shell script vytvořen
- [ ] NPM scripts pro smoke testy (`test:smoke`)
- [ ] Makefile target `test-smoke`
- [ ] Environment dokumentace (`e2e/docs/environment.md`)
- [ ] Test users dokumentovány
- [ ] Všechny smoke testy tagged `@SMOKE @CRITICAL`
- [ ] Smoke suite běží < 7 min
- [ ] CI integrace ready (GitHub Actions workflow)

---

## 📊 Success Metrics

- **Execution Time:** < 7 min (target: 5 min)
- **Reliability:** < 2% flaky rate
- **Coverage:** 4 kritické cesty pokryty
- **Adoption:** Všichni devs spouští smoke testy před PR

---

## 🔗 Dependencies

- **E2E1:** Playwright framework setup
- **E2E2:** Page Object Model (LoginPage, MetamodelStudioPage, WorkflowPage, LokiLogViewerPage)
- **E2E14:** Test data management (test users, cleanup)
- Docker Compose environment running
- core-platform.local DNS configured

---

## 📝 Implementation Notes

### Smoke Test Design Principles

1. **Fast Feedback:** < 7 min total execution
2. **Critical Paths Only:** Login, basic CRUD, workflow, logs
3. **Independent:** Každý test může běžet samostatně
4. **Idempotent:** Cleanup po každém testu
5. **Realistic:** Používá reálný stack (ne mocks)

### Test Data Strategy

- **Setup:** Používá existující `test_admin` user (z E2E14 seeders)
- **Cleanup:** Maže vytvořené entity/workflow instances po testu
- **Isolation:** Každý test má unique data (timestamps, UUIDs)

### Health Check Strategy

- **Pre-test:** Spustit health check před smoke suite
- **Fast Fail:** Pokud health check failuje, skip smoke tests
- **Timeout:** 5s per endpoint (prevent hangs)

---

**Effort:** 6 hodin  
**Priority:** P0 (Phase 1 MUST HAVE)  
**Value:** Rychlá validace kritických cest, foundation pro CI/CD smoke tests

**Created:** 9. listopadu 2025
