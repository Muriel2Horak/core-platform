# E2E Test Suite Documentation

> **Kompletní E2E testovací pokrytí Core Platform**  
> Poslední aktualizace: 26. října 2025

## 📋 Obsah

- [Přehled testů](#přehled-testů)
- [Struktura testů](#struktura-testů)
- [Spouštění testů](#spouštění-testů)
- [Pokrytí funkcionalit](#pokrytí-funkcionalit)
- [Continuous Integration](#continuous-integration)

---

## 🎯 Přehled testů

### Statistiky
- **Celkem testů**: 55
- **Test souborů**: 5 (admin CRUD) + ostatní
- **Celkové pokrytí**: ~1500+ řádků test kódu
- **Průměrná doba běhu**: ~3-5 minut (všechny testy)

### Kategorie testů

| Kategorie | Počet testů | Soubor | Status |
|-----------|-------------|--------|--------|
| **Users CRUD** | 10 | `admin/users-crud.spec.ts` | ✅ |
| **Roles CRUD** | 11 | `admin/roles-crud.spec.ts` | ✅ |
| **Groups CRUD** | 11 | `admin/groups-crud.spec.ts` | ✅ |
| **Tenants CRUD** | 13 | `admin/tenants-crud.spec.ts` | ✅ |
| **Keycloak Sync** | 10 | `admin/keycloak-sync.spec.ts` | ✅ |
| **CDC Polling** | 3 | `post/15_cdc_polling.spec.ts` | ✅ |
| **Auth & Profile** | - | `post/10_auth_profile_update.spec.ts` | ✅ |
| **Monitoring** | - | `monitoring/*.spec.ts` | ✅ |
| **Workflows** | - | `pre/05_workflow_*.spec.ts` | ✅ |

---

## 📂 Struktura testů

```
e2e/
├── specs/
│   ├── admin/              # Admin CRUD operace (55 testů)
│   │   ├── users-crud.spec.ts       # 10 testů - správa uživatelů
│   │   ├── roles-crud.spec.ts       # 11 testů - správa rolí
│   │   ├── groups-crud.spec.ts      # 11 testů - správa skupin
│   │   ├── tenants-crud.spec.ts     # 13 testů - správa tenantů
│   │   └── keycloak-sync.spec.ts    # 10 testů - Keycloak bulk sync
│   ├── post/               # Post-login funkcionality
│   │   ├── 15_cdc_polling.spec.ts   # CDC endpoint testy
│   │   ├── 10_auth_profile_update.spec.ts
│   │   └── 40_directory_consistency.spec.ts
│   ├── pre/                # Pre-login a smoke testy
│   │   ├── 01_login_smoke.spec.ts
│   │   ├── 02_menu_rbac_smoke.spec.ts
│   │   ├── 05_workflow_runtime_smoke.spec.ts
│   │   ├── 06_workflow_ux.spec.ts
│   │   └── 08_studio_rbac.spec.ts
│   ├── monitoring/         # Loki, Grafana monitoring
│   │   ├── loki-log-viewer.spec.ts
│   │   ├── loki-csv-export.spec.ts
│   │   └── logs-export-e2e.spec.ts
│   ├── ai/                 # AI features
│   │   ├── ai-help-widget.spec.ts
│   │   └── mcp-endpoints.spec.ts
│   └── tenant/             # Tenant provisioning
│       └── complete-provisioning.spec.ts
├── helpers/
│   ├── api.ts              # API helper funkce (incl. Keycloak Sync)
│   ├── login.ts            # Login helpers
│   ├── fixtures.ts         # Test fixtures
│   └── scenes.ts           # Grafana Scenes helpers
└── config/
    └── read-config.js      # E2E konfigurace
```

---

## 🚀 Spouštění testů

### Základní příkazy

```bash
# Všechny testy
npx playwright test

# Pouze admin CRUD testy
npx playwright test specs/admin/

# Konkrétní test suite
npx playwright test specs/admin/users-crud.spec.ts
npx playwright test specs/admin/keycloak-sync.spec.ts

# S UI (headed mode)
npx playwright test --headed

# Debug mode
npx playwright test --debug

# S reportem
npx playwright test --reporter=html
npx playwright show-report
```

### Rychlé smoke testy (po každém buildu)

```bash
# Pre-login testy (rychlé, <1 min)
npx playwright test specs/pre/

# Admin CRUD základní funkcionalita (2-3 min)
npx playwright test specs/admin/ --grep "@smoke"

# Full admin suite (všech 55 testů, ~5 min)
npx playwright test specs/admin/
```

### Testování konkrétních funkcionalit

```bash
# Users CRUD
npx playwright test specs/admin/users-crud.spec.ts

# Keycloak Sync
npx playwright test specs/admin/keycloak-sync.spec.ts

# CDC Polling
npx playwright test specs/post/15_cdc_polling.spec.ts

# Monitoring
npx playwright test specs/monitoring/
```

---

## 🎯 Pokrytí funkcionalit

### 1. **Users CRUD** (10 testů) ✅

**Co testujeme**:
- ✅ Create user (admin + user_manager)
- ✅ Read user list (admin + user_manager + regular user)
- ✅ Update user (admin + user_manager)
- ✅ Delete user (admin only)
- ✅ Assign roles to user
- ✅ RBAC verification (user_manager can't delete)
- ✅ Search & filter users
- ✅ Validation (required fields, duplicates)

**Endpoints**:
```
POST   /api/admin/users
GET    /api/admin/users
GET    /api/admin/users/{id}
PUT    /api/admin/users/{id}
DELETE /api/admin/users/{id}
POST   /api/admin/users/{id}/roles
```

---

### 2. **Roles CRUD** (11 testů) ✅

**Co testujeme**:
- ✅ Create role (admin only)
- ✅ Read role list (admin + user_manager read-only)
- ✅ Update role (admin only)
- ✅ Delete role (admin only)
- ✅ RBAC verification (user_manager read-only)
- ✅ Search & filter roles
- ✅ Validation (required fields, duplicates)
- ✅ Role permissions/capabilities display

**Endpoints**:
```
POST   /api/admin/roles
GET    /api/admin/roles
GET    /api/admin/roles/{id}
PUT    /api/admin/roles/{id}
DELETE /api/admin/roles/{id}
```

---

### 3. **Groups CRUD** (11 testů) ✅

**Co testujeme**:
- ✅ Create group (admin + user_manager)
- ✅ Read group list (admin + user_manager)
- ✅ Update group (admin + user_manager)
- ✅ Delete group (admin only)
- ✅ Add/Remove members
- ✅ RBAC verification
- ✅ Search & filter groups
- ✅ Validation
- ✅ Member count display

**Endpoints**:
```
POST   /api/admin/groups
GET    /api/admin/groups
GET    /api/admin/groups/{id}
PUT    /api/admin/groups/{id}
DELETE /api/admin/groups/{id}
POST   /api/admin/groups/{id}/members
DELETE /api/admin/groups/{id}/members/{userId}
```

---

### 4. **Tenants CRUD** (13 testů) ✅

**Co testujeme**:
- ✅ Create tenant (admin only)
- ✅ Read tenant list (admin, tenant_admin sees own)
- ✅ Update tenant (admin only)
- ✅ Delete tenant (admin only)
- ✅ **Grafana provisioning verification** (create + cleanup)
- ✅ Toggle enabled status
- ✅ RBAC verification
- ✅ Search & filter tenants
- ✅ Validation (required, format, duplicates)
- ✅ Tenant statistics

**Endpoints**:
```
POST   /api/admin/tenants
GET    /api/admin/tenants
GET    /api/admin/tenants/{id}
PUT    /api/admin/tenants/{id}
DELETE /api/admin/tenants/{id}
PATCH  /api/admin/tenants/{id}/toggle-enabled
```

**Speciální testy**:
- Grafana datasource auto-provisioning
- Grafana cleanup při smazání tenantu

---

### 5. **Keycloak Bulk Sync** (10 testů) ✅ **NOVÉ!**

**Co testujeme**:
- ✅ Sync users from Keycloak (async job)
- ✅ Sync roles from Keycloak (async job)
- ✅ Sync groups from Keycloak (async job)
- ✅ Full sync (users + roles + groups)
- ✅ Sync status tracking (polling async job)
- ✅ Idempotence (repeated sync is safe)
- ✅ RBAC verification (admin only)
- ✅ Error handling (invalid tenant)
- ✅ Tenant isolation
- ✅ Sync statistics (counts, errors)

**Endpoints**:
```
POST /api/admin/keycloak-sync/users/{tenantKey}
POST /api/admin/keycloak-sync/roles/{tenantKey}
POST /api/admin/keycloak-sync/groups/{tenantKey}
POST /api/admin/keycloak-sync/all/{tenantKey}
GET  /api/admin/keycloak-sync/status/{syncId}
```

**API Helper funkce** (v `helpers/api.ts`):
```typescript
syncUsersFromKeycloak(api, tenantKey)
syncRolesFromKeycloak(api, tenantKey)
syncGroupsFromKeycloak(api, tenantKey)
syncAllFromKeycloak(api, tenantKey)
getSyncStatus(api, syncId)
```

---

### 6. **CDC (Change Data Capture)** (3 testy) ✅

**Co testujeme**:
- ✅ Detect user changes via CDC endpoint
- ✅ CDC timestamp without 'since' parameter
- ✅ Multiple CDC polls (timestamp diff tracking)

**Endpoints**:
```
GET /api/me/changes?since={timestamp}
```

**API Helper funkce**:
```typescript
checkUserChanges(api, since?)
```

---

## 🔄 Continuous Integration

### GitHub Actions / CI Pipeline

**Doporučený workflow**:

```yaml
# .github/workflows/e2e-tests.yml
name: E2E Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  e2e-tests:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      # 1. Build backend + frontend
      - name: Build services
        run: make clean-fast
      
      # 2. Wait for healthy
      - name: Wait for backend
        run: |
          timeout 120 sh -c 'until docker inspect core-backend --format="{{.State.Health.Status}}" | grep -q healthy; do sleep 2; done'
      
      # 3. Run smoke tests (quick)
      - name: Smoke tests
        run: |
          cd e2e
          npx playwright test specs/pre/
      
      # 4. Run admin CRUD tests
      - name: Admin CRUD tests
        run: |
          cd e2e
          npx playwright test specs/admin/
      
      # 5. Run CDC tests
      - name: CDC tests
        run: |
          cd e2e
          npx playwright test specs/post/15_cdc_polling.spec.ts
      
      # 6. Upload report on failure
      - uses: actions/upload-artifact@v3
        if: failure()
        with:
          name: playwright-report
          path: e2e/playwright-report/
```

### Makefile integrace

**Přidat do `Makefile`**:

```makefile
# E2E testy
.PHONY: test-e2e test-e2e-admin test-e2e-smoke

test-e2e: ## Spustí všechny E2E testy
	@echo "🧪 Running all E2E tests..."
	cd e2e && npx playwright test

test-e2e-admin: ## Spustí admin CRUD testy (55 testů)
	@echo "🧪 Running admin CRUD E2E tests..."
	cd e2e && npx playwright test specs/admin/

test-e2e-smoke: ## Spustí rychlé smoke testy
	@echo "🧪 Running smoke E2E tests..."
	cd e2e && npx playwright test specs/pre/

test-e2e-sync: ## Spustí Keycloak Sync testy
	@echo "🧪 Running Keycloak Sync E2E tests..."
	cd e2e && npx playwright test specs/admin/keycloak-sync.spec.ts

# Full pipeline (build + tests)
test-full: clean-fast test-e2e-smoke test-e2e-admin ## Build + smoke + admin testy
	@echo "✅ Full test pipeline completed"
```

---

## 📊 Test Maintenance

### Přidání nového testu

1. **Vytvořit test soubor**:
   ```bash
   touch e2e/specs/admin/new-feature.spec.ts
   ```

2. **Použít template**:
   ```typescript
   import { test, expect } from '@playwright/test';
   import { loginAsAdmin } from '../../helpers/login';
   
   test.describe('Feature Name', () => {
     test('should do something', async ({ page }) => {
       await loginAsAdmin(page);
       // test logic
     });
   });
   ```

3. **Přidat helper funkce do `api.ts`** (pokud potřeba)

4. **Spustit test**:
   ```bash
   npx playwright test specs/admin/new-feature.spec.ts
   ```

5. **Aktualizovat dokumentaci** (tento soubor)

---

## 🐛 Debugging

### Když test failuje

```bash
# 1. Spustit s UI
npx playwright test specs/admin/users-crud.spec.ts --headed

# 2. Debug mode (step-through)
npx playwright test specs/admin/users-crud.spec.ts --debug

# 3. Zkontrolovat logy
make logs-backend
make logs-frontend

# 4. Trace viewer (zachytí vše)
npx playwright test --trace on
npx playwright show-trace trace.zip
```

### Běžné problémy

1. **Backend není healthy**:
   ```bash
   make verify
   docker inspect core-backend --format='{{.State.Health.Status}}'
   ```

2. **Test timeout**:
   - Zvýšit timeout v `playwright.config.ts`
   - Zkontrolovat zda backend nepadá (logy)

3. **RBAC fails**:
   - Zkontrolovat Keycloak roles
   - Verify test user má správné permissions

---

## 📈 Coverage Goals

| Oblast | Aktuální | Cíl |
|--------|----------|-----|
| **Admin CRUD** | 55 testů | ✅ Kompletní |
| **Keycloak Sync** | 10 testů | ✅ Kompletní |
| **CDC** | 3 testy | ✅ Základní |
| **Workflows** | Částečné | 🔄 Rozšířit |
| **Monitoring** | Částečné | 🔄 Rozšířit |
| **AI Features** | Částečné | 🔄 Rozšířit |

---

## 📝 Changelog

### 2025-10-26
- ✅ Přidány Keycloak Sync testy (10 testů)
- ✅ Rozšířeny API helpers (sync funkce)
- ✅ CDC testy opraveny (correct endpoints)
- ✅ Dokumentace konsolidována

### 2025-10-25
- ✅ CDC Polling testy vytvořeny (3 testy)
- ✅ User profile update testy

---

## 🤝 Contributing

1. Každý nový feature musí mít E2E testy
2. Testy musí být atomické (nezávislé)
3. Používat existující helper funkce z `api.ts`
4. Aktualizovat dokumentaci
5. Spustit `make test-e2e-admin` před commitem

---

**Kontakt**: Tým Core Platform  
**Poslední revize**: 26. října 2025
