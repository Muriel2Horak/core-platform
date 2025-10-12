# E2E Testy proti Lokálnímu Prostředí

## Přehled

E2E testy běží **proti existujícímu prod-like lokálnímu prostředí** na `https://core-platform.local`.

**Žádná orchestrace služeb** - všechny služby musí již běžet před spuštěním testů.

## Architektura

### Konfigurace

Testy čtou konfiguraci z existujících souborů v projektu:

1. **`.env`** (root projektu)
   - `DOMAIN` (default: `core-platform.local`)
   - `OIDC_ISSUER_URI` (Keycloak issuer URL)
   - `TEST_USER_PASSWORD` (heslo pro test uživatele)
   - `KEYCLOAK_TARGET_REALM` (default realm)

2. **`backend/src/main/resources/application.properties`**
   - Spring Security OAuth2 konfigurace
   - Keycloak client settings

3. **Environment Variables Override**
   - `E2E_BASE_URL` - override base URL (default: `https://core-platform.local`)
   - `E2E_IGNORE_TLS` - vypnout TLS validaci (default: `false`)
   - `E2E_REALM` - override Keycloak realm (default: `admin`)
   - `E2E_CLIENT_ID` - override client ID (default: `web`)
   - `E2E_USER` - override test username (default: `testuser`)
   - `E2E_PASS` - override test password (z `.env`: `TEST_USER_PASSWORD`)

### Složky

```
e2e/
├── config/
│   └── read-config.ts      # Čte konfiguraci z .env a application.properties
├── helpers/
│   └── login.ts            # Login helper přes Keycloak UI
├── specs/
│   ├── gui-smoke.spec.ts   # Smoke testy (login, menu, grid, detail)
│   └── workflow-execute.spec.ts  # Workflow testy (execute, timeline)
├── .auth/
│   └── state.json          # Uložená session (gitignore)
├── playwright.config.ts    # Playwright konfigurace
└── tsconfig.json           # TypeScript config pro e2e
```

## Příprava Prostředí

### 1. Spuštění Lokálního Prostředí

```bash
# Z rootu projektu
make clean
```

To zajistí:
- Docker services běží (Keycloak, PostgreSQL, backend, frontend)
- HTTPS proxy na `https://core-platform.local`
- SSL certifikáty jsou trusted (pokud ne, použij `E2E_IGNORE_TLS=true`)
- DNS resolve (`core-platform.local` → `127.0.0.1`)

### 2. Ověření Prostředí

Otevři prohlížeč a zkontroluj:
- ✅ `https://core-platform.local` - aplikace načte
- ✅ Login funguje přes Keycloak
- ✅ Žádné SSL varování (nebo nastav `E2E_IGNORE_TLS=true`)

### 3. Instalace Playwright

```bash
cd frontend
pnpm playwright:install
# nebo
npx playwright install --with-deps
```

## Spuštění Testů

### Lokálně

```bash
# Z frontend/ složky
pnpm test:e2e
```

### S přepsaným Base URL

```bash
E2E_BASE_URL=https://my-custom.local pnpm test:e2e
```

### Vypnutí TLS Validace

Pokud systémová CA nedůvěřuje self-signed certům:

```bash
E2E_IGNORE_TLS=true pnpm test:e2e
```

### S Vlastním Test Userem

```bash
E2E_USER=admin E2E_PASS=Admin.1234 pnpm test:e2e
```

### Debug Režim

```bash
# S viditelným prohlížečem
pnpm test:e2e:headed

# Playwright UI (interaktivní)
pnpm test:e2e:ui
```

### Spuštění Konkrétního Testu

```bash
pnpm test:e2e gui-smoke
pnpm test:e2e workflow-execute
```

## Testy

### 1. GUI Smoke Tests (`gui-smoke.spec.ts`)

**Co testuje:**
- ✅ Login flow přes Keycloak
- ✅ Zobrazení dashboardu po přihlášení
- ✅ Menu položky podle RBAC
- ✅ Grid/table zobrazení entity (např. Customers)
- ✅ Otevření detailu/popupu

**Příklad:**
```typescript
test('should display menu items based on RBAC', async ({ page }) => {
  // Login + ověření menu
});
```

### 2. Workflow Tests (`workflow-execute.spec.ts`)

**Co testuje:**
- ✅ Workflow panel v detailu entity
- ✅ Aktuální stav (highlighted)
- ✅ Dostupné přechody
- ✅ Execute dialog (RUNNING → SUCCESS/FAILED)
- ✅ Execution steps a durations
- ✅ Timeline update (ENTER_STATE/EXIT_STATE)
- ✅ UI unlock po update (stale→fresh)

**Příklad:**
```typescript
test('should execute workflow and show ExecutionDialog', async ({ page }) => {
  // Execute → ověř status změny
});
```

## Login Helper

### Základní Použití

```typescript
import { loginViaKeycloak } from '../helpers/login';

test('my test', async ({ page }) => {
  await loginViaKeycloak(page);
  // Teď jsi přihlášen
});
```

### Storage State (Zrychlení)

Login helper podporuje uložení session:

```typescript
import { loginAndSaveState, getStorageStatePath } from '../helpers/login';

// Jednou ulož session
test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage();
  await loginAndSaveState(page);
  await page.close();
});

// Pak použij ve všech testech
test.use({ storageState: getStorageStatePath() });
```

## Výsledky Testů

### HTML Report

Po spuštění testů:

```bash
# Otevře HTML report
pnpm dlx playwright show-report frontend/playwright-report
```

### Trace Viewer

Pokud test failne a má trace:

```bash
pnpm dlx playwright show-trace frontend/test-results/[test-name]/trace.zip
```

Trace obsahuje:
- Screenshots každé akce
- Network requesty
- Console logy
- DOM snapshots

## CI/CD

### GitHub Actions (Self-Hosted Runner)

Pokud máš self-hosted runner s běžícím prostředím:

```yaml
# .github/workflows/e2e.yml
name: E2E Tests

on:
  push:
    branches: [main]
  pull_request:

jobs:
  e2e:
    runs-on: self-hosted  # Musí mít běžící https://core-platform.local
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: '20'
      
      - name: Install pnpm
        run: npm install -g pnpm
      
      - name: Install dependencies
        working-directory: frontend
        run: pnpm install
      
      - name: Install Playwright
        working-directory: frontend
        run: pnpm playwright:install
      
      - name: Run E2E tests
        working-directory: frontend
        run: pnpm test:e2e
        env:
          E2E_IGNORE_TLS: 'true'  # Pokud runner nemá trusted cert
      
      - name: Upload report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-report
          path: frontend/playwright-report/
      
      - name: Upload traces
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-traces
          path: frontend/test-results/
```

**DŮLEŽITÉ:** Runner MUSÍ mít:
- Běžící `https://core-platform.local` (přes `make clean`)
- DNS resolve pro `core-platform.local`
- Trusted SSL cert NEBO nastavený `E2E_IGNORE_TLS=true`

## Troubleshooting

### SSL Certificate Error

**Problém:** `SSL certificate problem: self signed certificate`

**Řešení:**
```bash
E2E_IGNORE_TLS=true pnpm test:e2e
```

Nebo přidej cert do systémové CA:
```bash
# macOS
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ssl/core-platform.crt
```

### Keycloak Login Timeout

**Problém:** Test failuje na login flow

**Řešení:**
1. Ověř že Keycloak běží: `https://admin.core-platform.local`
2. Zkontroluj credentials v `.env`: `TEST_USER_PASSWORD`
3. Spusť test v headed mode: `pnpm test:e2e:headed`

### DNS Resolution Failed

**Problém:** `net::ERR_NAME_NOT_RESOLVED` pro `core-platform.local`

**Řešení:**
```bash
# Zkontroluj /etc/hosts
grep core-platform /etc/hosts

# Mělo by být:
# 127.0.0.1 core-platform.local admin.core-platform.local
```

### Tests Fail with "No Element Found"

**Problém:** Selektory nenacházejí elementy

**Řešení:**
1. Spusť v UI mode: `pnpm test:e2e:ui`
2. Ověř že UI skutečně zobrazuje daný element
3. Uprav selektory v testech podle skutečné DOM struktury
4. Použij `data-testid` atributy v komponentách pro stabilnější selektory

## Vývojářské Workflow

### Přidání Nového Testu

1. Vytvoř `e2e/specs/my-feature.spec.ts`
2. Importuj login helper:
   ```typescript
   import { test, expect } from '@playwright/test';
   import { loginViaKeycloak } from '../helpers/login';
   ```
3. Napsání testu:
   ```typescript
   test('my test', async ({ page }) => {
     await loginViaKeycloak(page);
     // tvůj test kód
   });
   ```
4. Spusť: `pnpm test:e2e my-feature`

### Best Practices

- ✅ Používej `data-testid` atributy pro selektory
- ✅ Nevytvářej závislosti mezi testy (každý test samostatný)
- ✅ Používej `expect` s timeouty pro async operace
- ✅ Loguj důležité kroky pomocí `console.log`
- ✅ Při failure kontroluj trace (`show-trace`)
- ❌ Nespoléhej na konkrétní pořadí testů
- ❌ Nepoužívej `test.only` v commitu
- ❌ Nespouštěj testy bez běžícího prostředí

## Odkazy

- [Playwright Documentation](https://playwright.dev/)
- [Playwright Test API](https://playwright.dev/docs/api/class-test)
- [Selectors](https://playwright.dev/docs/selectors)
- [Trace Viewer](https://playwright.dev/docs/trace-viewer)
