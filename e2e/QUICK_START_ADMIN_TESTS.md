# 🚀 Quick Start: E2E Admin Tests

Po každém buildu spusť admin testy pro validaci:

```bash
# 1. Build (pokud jsi změnil backend .java soubory)
make clean-fast

# 2. Ověř, že services běží
make verify

# 3. Spusť admin CRUD testy (55 testů, 3-5 min)
make test-e2e-admin
```

## Výstup, který uvidíš:

```
╔════════════════════════════════════════════════════════════════╗
║  👥 ADMIN CRUD E2E TESTS (55 tests)                            ║
╚════════════════════════════════════════════════════════════════╝

⚠️  Requires: Running environment (make dev-up or make up)
📋 Tests: Users, Roles, Groups, Tenants, Keycloak Sync
⏱️  Duration: ~3-5 minutes

▶️  Running admin CRUD tests...

Running 55 tests using 1 worker

  ✅ [chromium] › admin/users-crud.spec.ts:10:3 › Users CRUD › should create new user
  ✅ [chromium] › admin/users-crud.spec.ts:25:3 › Users CRUD › should read user list
  ✅ [chromium] › admin/roles-crud.spec.ts:10:3 › Roles CRUD › should create new role
  ...
  ✅ [chromium] › admin/keycloak-sync.spec.ts:245:3 › Keycloak Sync › should validate statistics

55 ✅ passed (3.2m)

✅ Admin CRUD E2E tests completed!
📊 Report: e2e/playwright-report/index.html
```

## Pokud test failne:

```bash
# 1. Zkontroluj logy
make logs-backend

# 2. Zkontroluj health
make verify

# 3. Re-run s debug
cd e2e
npx playwright test specs/admin/users-crud.spec.ts --debug
```

## Jen Keycloak Sync testy:

```bash
make test-e2e-sync
```

---

**Dokumentace**: Viz [E2E_TEST_SUITE.md](../E2E_TEST_SUITE.md) pro detaily
