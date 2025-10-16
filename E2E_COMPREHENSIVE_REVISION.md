# 🧪 E2E Tests - Comprehensive Revision

## 📊 Současný stav (2025-01-XX)

### Výsledky posledního běhu
- **Duration**: 168.25s (~2.8 min)
- **Passed**: 4/21 tests (19% success rate)
- **Failed**: 17/21 tests (81% failure rate)
- **Kritický problém**: Většina testů testuje funkce, které neexistují

---

## 🎯 Zjištěné problémy

### 1. Testy vs. Realita - Mapování funkčnosti

#### ✅ **Co EXISTUJE v aplikaci:**
```typescript
// Skutečné routy z App.jsx (lines 447-500)
/dashboard                     // Dashboard s widgety
/reports                       // Grafana Scenes (monitoring)
/reporting                     // Reporting Explorer (ag-grid + charts)
/user-directory                // Veřejný adresář uživatelů
/profile                       // Profil uživatele

/core-admin/*                  // Admin sekce (hierarchy)
├── /monitoring                // Prometheus + Grafana
├── /users                     // User management
├── /users/:id/edit            // Edit user
├── /roles                     // Role management
├── /roles/:id/edit            // Edit role
├── /groups                    // Groups
├── /tenants                   // Tenant management
├── /tenants/:id/edit          // Edit tenant
├── /security                  // Security settings
├── /audit                     // Audit logs
├── /keycloak-sync             // Keycloak sync
├── /sync-history              // Sync history
├── /streaming                 // Streaming dashboard
├── /workflows                 // Workflow designer
└── /studio                    // Metamodel studio

/tenant-admin/*                // Tenant Admin sekce
├── /                          // Tenant dashboard
├── /users                     // Tenant-scoped users
├── /roles                     // Tenant-scoped roles
├── /groups                    // Tenant-scoped groups
└── /keycloak-sync             // Tenant-scoped sync

/examples/*                    // Příklady
├── /data-table                // DataTable demo
└── /kanban                    // Kanban demo
```

#### ❌ **Co NEEXISTUJE (ale testy to testují):**
```typescript
/entities/*                    // ❌ Žádná dynamická entita routovací
/entities/Customers            // ❌ Test 03_entity_grid_form_smoke.spec.ts
/entities/Orders               // ❌ Test 05_workflow_runtime_smoke.spec.ts
/workflow/*                    // ❌ Workflow není jako samostatná route
```

### 2. Pre-deploy vs. Post-deploy - Nesmyslné rozdělení

**Problém identifikovaný uživatelem:**
> "nedává mi smysl mít prebuild a postbuild E2E, když se ději oboje na sestavením prostředí ne?"

**Realita:**
- Oba běží proti stejnému lokálnímu prostředí
- Žádný deployment mezi nimi
- Rozdělení je legacy z cloudového mindset (pre-deploy smoke + post-deploy integration)
- V lokálním vývojovém prostředí to nedává smysl

**Doporučení**: Sloučit do jedné logické struktury:
- `smoke/` - Rychlé základní testy (login, menu, navigation)
- `features/` - Funkční testy jednotlivých feature (reporting, monitoring, admin)
- `integration/` - End-to-end scénáře (create user → assign role → verify)

### 3. Login Helper - Session caching

**Problém:**
```typescript
// e2e/helpers/login.ts line 47-60
if (isKeycloakPage) {
  // Tento kód NIKDY neběží, protože Keycloak si pamatuje session
  await page.fill('input[name="username"]', username);
  await page.fill('input[name="password"]', password);
  await submitButton.first().click();
}
```

**Root cause:**
- První test úspěšně přihlásí přes Keycloak
- Keycloak nastaví session cookie
- Další testy volají `login()`, ale už nejsou na Keycloak stránce
- Timeout čekání na username field (15s)
- Test failne: `TimeoutError: page.fill: Timeout 15000ms exceeded`

**Fix:**
```typescript
export async function login(page: Page, options: LoginOptions = {}): Promise<void> {
  const config = readE2EConfig();
  
  // Nejdřív zkontroluj, jestli už jsme přihlášení
  if (await isLoggedIn(page)) {
    console.log('Already logged in, skipping Keycloak flow');
    return;
  }
  
  const username = options.username || config.testUser.username;
  const password = options.password || config.testUser.password;

  // Navigate to app (should redirect to Keycloak IF not logged in)
  await page.goto('/');

  // Pokud je stále Keycloak, vyplň login
  const isKeycloakPage = page.url().includes(config.keycloak.authServerUrl);
  
  if (isKeycloakPage) {
    await page.fill('input[name="username"]', username);
    await page.fill('input[name="password"]', password);
    
    const submitButton = page.locator('button[type="submit"], input[type="submit"]');
    await submitButton.first().click();
    
    // Wait for redirect
    await page.waitForURL(url => !url.toString().includes('keycloak'), { timeout: 15000 });
  }
  
  // Počkej až se naloaduje app
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});
}
```

### 4. isLoggedIn() Helper - False negatives

**Problém:**
```typescript
// e2e/helpers/login.ts line 73-85
export async function isLoggedIn(page: Page): Promise<boolean> {
  // Check if on Keycloak login page
  if (page.url().includes(config.keycloak.authServerUrl)) {
    return false;
  }
  
  // Check for auth token or user indicator in DOM
  const hasUserMenu = await page.locator('[data-testid="user-menu"]').count() > 0;
  return hasUserMenu;
}
```

**Proč failuje:**
- Test volá `isLoggedIn()` okamžitě po `login()`
- React aplikace se ještě nemusela plně vyrenderovat
- `data-testid="user-menu"` ještě není v DOMu
- Vrátí `false`, i když je uživatel přihlášený

**Fix:**
```typescript
export async function isLoggedIn(page: Page): Promise<boolean> {
  const config = readE2EConfig();
  
  // Pokud jsme na Keycloak login page, určitě nejsme přihlášení
  if (page.url().includes(config.keycloak.authServerUrl)) {
    return false;
  }
  
  // Počkej až se vykreslí user menu (s timeoutem)
  try {
    await page.waitForSelector('[data-testid="user-menu"]', { timeout: 5000 });
    return true;
  } catch {
    return false;
  }
}
```

### 5. Chybějící testovací data

**Problém identifikovaný uživatelem:**
> "všiml jsem si, že nám chybí založené a přidělení role pro Studia pro test_admin"

**Test user `test_admin` nemá:**
- ❌ Práva pro Metamodel Studio (`CORE_ROLE_STUDIO_ADMIN`?)
- ❌ Workflow designer role
- ❌ Tenant admin role pro testování tenant-admin sekce

**Potřebné akce:**
1. Zjistit jaké role existují v systému
2. Přiřadit `test_admin` potřebné role pro testování všech feature
3. Vytvořit `test_tenant_admin` uživatele pro tenant-admin testy
4. Dokumentovat test users v `e2e/config/test-users.md`

### 6. Grafana Scenes - Nekonečné načítání

**Problém:**
> "a taky grafana scenes se jenom točí"

**Možné příčiny:**
1. Backend BFF proxy (`/api/monitoring/*`) nefunguje správně
2. Grafana není dostupná nebo nemá data
3. Frontend chybí error handling pro Grafana fetch failures
4. CORS problém mezi frontend a BFF

**Debug kroky:**
```bash
# Zkontroluj Grafana health
curl http://localhost:3000/api/health

# Zkontroluj BFF proxy
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/monitoring/query

# Zkontroluj browser console v /reports route
# Hledej network errors, CORS issues
```

---

## 📋 Detailní analýza testů

### PRE-DEPLOY Tests (e2e/specs/pre/)

#### ✅ **01_login_smoke.spec.ts** (2/3 passed)
**Status**: Mostly working, needs minor fixes

**Testy:**
1. ✅ Login and redirect to dashboard - **WORKS**
2. ✅ Show login form on initial visit - **WORKS**
3. ❌ Reject invalid credentials - **FAILS** (Keycloak session caching)

**Issues:**
- Test 3 předpokládá čistý stav, ale Keycloak má session z testu 1
- Navigace na `/` nezobrazí login form, okamžitě redirectne

**Fix:**
```typescript
test('should reject invalid credentials', async ({ page, context }) => {
  // Vymaž všechny cookies před testem
  await context.clearCookies();
  
  await page.goto('/');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  
  // Try invalid login
  await page.fill('input[name="username"]', 'invalid_user');
  await page.fill('input[name="password"]', 'wrong_password');
  await page.click('button[type="submit"]');
  
  // Should show error message
  await expect(page.locator('text=/invalid|error|incorrect/i').first())
    .toBeVisible({ timeout: 5000 });
});
```

#### ⚠️ **02_menu_rbac_smoke.spec.ts** (1/2 passed)
**Status**: Partially working, needs route updates

**Testy:**
1. ❌ Show menu items matching user roles - **FAILS** (testuje Customers entitu která neexistuje)
2. ✅ Hide admin menu for non-admin - **WORKS**
3. ✅ Show user profile menu - **WORKS**

**Issues:**
- Test 1 používá `testEntity = 'Customers'` který neexistuje
- Zkouší fetchnout `/api/ui-specs/Customers` - 404
- Měl by testovat skutečné menu items (Dashboard, Reports, Reporting, User Directory)

**Fix:**
```typescript
test('should show menu items based on user roles', async ({ page }) => {
  await login(page);
  
  // Základní položky které by měl vidět každý přihlášený user
  await expect(page.locator('nav a:has-text("Dashboard")')).toBeVisible();
  await expect(page.locator('nav a:has-text("User Directory")')).toBeVisible();
  
  // Reports/Reporting jsou viditelné pro všechny
  const reportsLink = page.locator('nav a[href="/reports"]');
  if (await reportsLink.count() > 0) {
    await expect(reportsLink).toBeVisible();
  }
});

test('should show admin menu for admin users', async ({ page }) => {
  await login(page, {
    username: 'test_admin',
    password: 'Test.1234',
  });
  
  // Admin sekce by měla být viditelná
  await expect(page.locator('nav a:has-text("Core Admin")')).toBeVisible();
  
  // Zkontroluj submenu items
  const adminLink = page.locator('nav a:has-text("Core Admin")');
  await adminLink.click();
  
  await expect(page.locator('nav a:has-text("Users")')).toBeVisible();
  await expect(page.locator('nav a:has-text("Tenants")')).toBeVisible();
  await expect(page.locator('nav a:has-text("Roles")')).toBeVisible();
});
```

#### ❌ **03_entity_grid_form_smoke.spec.ts** (0 passed - SKIPPED)
**Status**: SKIP correct - entita neexistuje

**Akce**: SMAZAT CELÝ TEST

**Důvod**: `/entities/*` route neexistuje, dynamické entity nejsou implementované

#### ❌ **04_workflow_panel_smoke.spec.ts** (0 passed)
**Status**: Invalid - testuje neexistující feature

**Issues:**
- Zkouší navigovat na `/entities/Customers`
- Očekává workflow panel v detailu entity
- Entity detail neexistuje, workflow panel taky ne

**Akce**: SMAZAT nebo PŘEPSAT na testování skutečného workflow designeru

**Alternativa** - Test workflow designer v admin sekci:
```typescript
test('should display workflow designer in admin section', async ({ page }) => {
  await login(page, {
    username: 'test_admin',
    password: 'Test.1234',
  });
  
  await page.goto('/core-admin/workflows');
  
  // Měl by se zobrazit workflow designer nebo seznam workflows
  await expect(page.locator('h1, h2').filter({ hasText: /workflow/i })).toBeVisible();
});
```

#### ❌ **05_workflow_runtime_smoke.spec.ts** (0 passed)
**Status**: Invalid - testuje neexistující feature

**Issues:**
- Zkouší navigovat na `/entities/Orders`
- Očekává timeline panel, forecast panel, SLA badges
- Nic z toho neexistuje

**Akce**: SMAZAT CELÝ TEST

---

### POST-DEPLOY Tests (e2e/specs/post/)

#### ❌ **10_auth_profile_update.spec.ts** (0 passed)
**Status**: Partially valid concept, needs implementation check

**Issues:**
- Test předpokládá funkční profile update flow
- Navigace přes user menu → profile
- Update displayName a verifikace

**Potřeba ověřit:**
1. Funguje profile update v UI?
2. Existuje `/profile` route?
3. Funguje `updateUserProfile` API?

**Akce**: PŘEPSAT na skutečný profile update flow

#### ❌ **20_admin_create_entity_and_ui.spec.ts** (0 passed)
**Status**: Invalid - Studio UI není hotové

**Issues:**
- Očekává GUI pro vytváření entit v Studio
- Dynamické entity nejsou implementované
- Scaffold result JSON (`e2e/.auth/scaffold-result.json`) neexistuje

**Akce**: SMAZAT nebo nahradit testem pro Studio metadata browsing

**Alternativa:**
```typescript
test('should browse metamodel in Studio', async ({ page }) => {
  await login(page, {
    username: 'test_admin',
    password: 'Test.1234',
  });
  
  await page.goto('/core-admin/studio');
  
  // Měl by se zobrazit metamodel browser nebo nějaké studio UI
  await expect(page.locator('h1, h2').filter({ hasText: /studio|metamodel/i })).toBeVisible();
});
```

#### ❌ **30_workflow_create_and_run.spec.ts** (0 passed)
**Status**: Not implemented (placeholder)

**Issues:**
- Test je prázdný placeholder
- Komentář: "COPILOT_HINT: Workflow creation via Studio GUI not yet implemented."

**Akce**: SMAZAT nebo implementovat až bude workflow designer hotový

#### ❌ **40_directory_consistency.spec.ts** (0 passed)
**Status**: Valid concept, probably minor issues

**Issues:**
- Test hledá user directory search
- Měl by fungovat, pokud `/directory/users` existuje (nebo `/user-directory`)

**Možná chyba v route:**
```typescript
// Test používá:
await page.goto('/directory/users');

// Ale skutečná routa je:
// /user-directory (z App.jsx line 460)
```

**Fix:**
```typescript
test('should search user in directory', async ({ page }) => {
  await login(page);
  
  await page.goto('/user-directory'); // OPRAVA route
  
  const searchInput = page.locator('input[type="search"], input[placeholder*="search" i]').first();
  await searchInput.fill('test_admin');
  
  await expect(page.locator('text="test_admin"')).toBeVisible({ timeout: 10000 });
});
```

#### ❌ **50_cleanup_visibility.spec.ts** (0 passed)
**Status**: Invalid - depends on non-existent scaffold

**Issues:**
- Očekává `e2e/.auth/scaffold-result.json`
- Teardown script který smaže test entity
- Entity management neexistuje

**Akce**: SMAZAT CELÝ TEST

---

### MONITORING Tests (e2e/specs/monitoring/)

#### ❓ **grafana-scenes-integration.spec.ts** (status unknown)
**Status**: Complex integration test, needs investigation

**Co testuje:**
1. Vytvoření tenanta s Grafana provisioningem
2. Service account creation
3. Dashboard load s Grafana Scenes
4. Multi-tenant data isolation
5. Error handling

**Issues:**
- Uživatel hlásil: "grafana scenes se jenom točí"
- Může být problém s BFF proxy nebo Grafana dostupností

**Akce**: 
1. Zkontrolovat Grafana health (`curl localhost:3000/api/health`)
2. Zkontrolovat BFF proxy (`/api/monitoring/*`)
3. Přidat error handling do Reports.jsx
4. Vytvořit jednodušší smoke test před tímto komplexním testem

**Nový smoke test:**
```typescript
test('should load Reports page without errors', async ({ page }) => {
  await login(page);
  
  await page.goto('/reports');
  
  // Měl by se zobrazit Reports page header
  await expect(page.locator('h1, h4').filter({ hasText: /reporting|analytics/i })).toBeVisible();
  
  // Neměl by být error alert
  const errorAlert = page.locator('[role="alert"], .MuiAlert-standardError');
  expect(await errorAlert.count()).toBe(0);
});
```

---

## 🔨 Navrhovaná nová struktura

### Nová organizace testů

```
e2e/specs/
├── smoke/                              # Rychlé základní testy (< 1 min)
│   ├── 01_authentication.spec.ts       # Login, logout, session
│   ├── 02_navigation.spec.ts           # Menu, routing, 404 handling
│   └── 03_health.spec.ts               # All pages load without errors
│
├── features/                           # Funkční testy jednotlivých feature
│   ├── dashboard/
│   │   ├── widgets.spec.ts             # Dashboard widgets
│   │   └── stats.spec.ts               # Stats cards
│   │
│   ├── user-directory/
│   │   ├── search.spec.ts              # Search users
│   │   └── display.spec.ts             # User cards display
│   │
│   ├── reporting/
│   │   ├── explorer.spec.ts            # Reporting Explorer (ag-grid)
│   │   ├── charts.spec.ts              # Chart rendering (echarts)
│   │   └── export.spec.ts              # Export functionality
│   │
│   ├── reports/
│   │   ├── grafana-scenes.spec.ts      # Grafana Scenes integration
│   │   └── tabs.spec.ts                # Application/Infrastructure/Logs tabs
│   │
│   └── admin/
│       ├── users.spec.ts               # User management
│       ├── roles.spec.ts               # Role management
│       ├── tenants.spec.ts             # Tenant management
│       ├── monitoring.spec.ts          # Monitoring page
│       ├── audit.spec.ts               # Audit logs
│       └── keycloak-sync.spec.ts       # Keycloak sync
│
├── integration/                        # End-to-end scénáře (multiple features)
│   ├── user-lifecycle.spec.ts          # Create → Edit → Assign role → Delete
│   ├── tenant-lifecycle.spec.ts        # Create tenant → Provision → Delete
│   └── rbac-flow.spec.ts               # Login as different roles, verify access
│
└── ai/                                 # AI-related tests (pokud existují)
    └── ...
```

### Priorita implementace

#### PHASE 1: Smoke Tests (kritické, < 1 den)
1. ✅ **01_authentication.spec.ts**
   - Login with valid credentials
   - Login with invalid credentials (clear cookies)
   - Logout
   - Session persistence across page reloads

2. ✅ **02_navigation.spec.ts**
   - All main routes accessible (dashboard, reports, reporting, user-directory)
   - Menu items visible based on roles
   - 404 handling
   - Deep linking works

3. ✅ **03_health.spec.ts**
   - Dashboard loads without errors
   - Reports loads without errors
   - Reporting loads without errors
   - User Directory loads without errors
   - Admin pages load without errors (for admin user)

#### PHASE 2: Feature Tests (high priority, 2-3 dny)
1. **Dashboard**
   - Stats cards display correct counts
   - Widgets render
   - RBAC - cards show/hide based on permissions

2. **User Directory**
   - Search functionality
   - User cards display
   - Pagination (if exists)

3. **Reporting Explorer**
   - Entity selector works
   - Table view renders data
   - Charts render
   - Tab switching works

4. **Reports (Grafana Scenes)**
   - Page loads without infinite spinner
   - Tabs work (Application/Infrastructure/Logs)
   - "Open in Grafana" link works
   - Error handling when Grafana unavailable

5. **Admin Features (for test_admin)**
   - Users page lists users
   - Create user (if UI exists)
   - Edit user
   - Roles page lists roles
   - Tenants page lists tenants
   - Monitoring page loads

#### PHASE 3: Integration Tests (lower priority, 3-5 dní)
- Complete user lifecycle
- Complete tenant lifecycle
- RBAC verification across multiple features
- Grafana provisioning on tenant creation

---

## 🛠️ Okamžité akce

### 1. Fix login helper (CRITICAL)
```bash
# Edit e2e/helpers/login.ts
code e2e/helpers/login.ts
```

**Změny:**
- Přidat `isLoggedIn()` check na začátku `login()`
- Nevolat Keycloak flow pokud už jsme přihlášení
- Přidat `waitForSelector` do `isLoggedIn()` místo synchronního `count()`

### 2. Smazat neplatné testy (IMMEDIATE)
```bash
rm e2e/specs/pre/03_entity_grid_form_smoke.spec.ts
rm e2e/specs/pre/04_workflow_panel_smoke.spec.ts
rm e2e/specs/pre/05_workflow_runtime_smoke.spec.ts
rm e2e/specs/post/20_admin_create_entity_and_ui.spec.ts
rm e2e/specs/post/30_workflow_create_and_run.spec.ts
rm e2e/specs/post/50_cleanup_visibility.spec.ts
```

### 3. Opravit directory test route (QUICK FIX)
```typescript
// e2e/specs/post/40_directory_consistency.spec.ts
- await page.goto('/directory/users');
+ await page.goto('/user-directory');
```

### 4. Přidat Grafana health smoke test (QUICK WIN)
```bash
# Nový soubor: e2e/specs/monitoring/reports-smoke.spec.ts
```

### 5. Zkontrolovat test_admin role (BLOCKING)
```bash
# Zjistit jaké role má test_admin
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/me

# Zjistit jaké role existují
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/roles

# Přiřadit chybějící role
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"roleId": "STUDIO_ADMIN_ROLE_ID"}' \
  http://localhost:8080/api/admin/users/{test_admin_id}/roles
```

### 6. Zjednodušit 01_login_smoke.spec.ts (IMPROVE)
```typescript
// Test 3: Clear cookies před testem invalid credentials
test('should reject invalid credentials', async ({ page, context }) => {
  await context.clearCookies();
  // ... rest of test
});
```

### 7. Frontend build (REQUIRED for data-testid)
```bash
# Aktuálně frontend nebyl rebuild po přidání data-testid="user-menu"
make frontend-rebuild
# nebo
cd frontend && npm run build
```

---

## 📐 Implementační plán

### Week 1: Foundation (Smoke Tests)
**Cíl:** Všechny smoke testy zelené

- [ ] Fix `login()` helper - session caching
- [ ] Fix `isLoggedIn()` helper - waitForSelector
- [ ] Smazat neplatné testy (6 souborů)
- [ ] Opravit directory route v 40_directory_consistency
- [ ] Zjednodušit 01_login_smoke (clear cookies)
- [ ] Rebuild frontend s data-testid
- [ ] Vytvořit `smoke/01_authentication.spec.ts`
- [ ] Vytvořit `smoke/02_navigation.spec.ts`
- [ ] Vytvořit `smoke/03_health.spec.ts`
- [ ] Všechny smoke testy projdou

### Week 2: Core Features
**Cíl:** Dashboard, User Directory, Reports fungují

- [ ] Zkontrolovat Grafana health
- [ ] Fix Reports.jsx error handling
- [ ] Vytvořit `features/reports/smoke.spec.ts`
- [ ] Vytvořit `features/dashboard/widgets.spec.ts`
- [ ] Vytvořit `features/user-directory/search.spec.ts`
- [ ] Vytvořit `features/reporting/explorer.spec.ts`

### Week 3: Admin Features
**Cíl:** Admin sekce testovatelná

- [ ] Přiřadit test_admin všechny potřebné role
- [ ] Vytvořit test_tenant_admin uživatele
- [ ] Dokumentovat test users v `e2e/config/test-users.md`
- [ ] Vytvořit `features/admin/users.spec.ts`
- [ ] Vytvořit `features/admin/roles.spec.ts`
- [ ] Vytvořit `features/admin/tenants.spec.ts`
- [ ] Vytvořit `features/admin/monitoring.spec.ts`

### Week 4: Integration Tests
**Cíl:** End-to-end scénáře

- [ ] Vytvořit `integration/user-lifecycle.spec.ts`
- [ ] Vytvořit `integration/tenant-lifecycle.spec.ts`
- [ ] Vytvořit `integration/rbac-flow.spec.ts`
- [ ] Grafana provisioning test (monitoring)

---

## 🎯 Success Metrics

### Current State (2025-01-XX)
- ❌ 4/21 tests passing (19%)
- ❌ 17/21 tests failing (81%)
- ❌ Tests test non-existent features
- ❌ Login helper breaks on session caching
- ❌ Pre/post structure is meaningless

### Target State (Week 1 - Smoke Tests)
- ✅ 10/10 smoke tests passing (100%)
- ✅ All smoke tests complete in < 1 minute
- ✅ No false negatives (session caching fixed)
- ✅ Simplified structure (smoke/features/integration)

### Target State (Week 4 - Full Suite)
- ✅ 30+ tests covering real features
- ✅ 80%+ test success rate
- ✅ All core features tested (dashboard, reporting, admin)
- ✅ Integration tests verify complete flows
- ✅ Test users with proper roles documented

---

## 📝 Next Steps

1. **Get approval** from uživatele na tento plán
2. **Start with fixes** - login helper, delete invalid tests
3. **Week 1 implementation** - smoke tests foundation
4. **Iterate** - add feature tests incrementally
5. **Monitor** - test success rate, flakiness, execution time

---

## 🤔 Questions for User

1. **Test user roles**: Které role má mít `test_admin`? Potřebuje Studio admin, Workflow designer?
2. **Grafana**: Chceš debugovat proč Grafana Scenes se "jenom točí"? Nebo to můžeme odložit?
3. **Priority**: Souhlasíš s tímto plánem (smoke → features → integration)?
4. **Timeline**: Je OK implementovat během 4 týdnů, nebo je to urgentní?
5. **Frontend rebuild**: Mám spustit `make frontend-rebuild` teď, nebo máš nějaké uncommitted změny?

