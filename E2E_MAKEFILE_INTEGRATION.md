# E2E Tests Integration with Makefile

## 🎯 Overview

E2E testy jsou plně integrovány do Makefile s podporou **two-tier** strategie:

1. **PRE-DEPLOY** (Fast Gate): Smoke testy před deploymentem
2. **POST-DEPLOY** (Full E2E): Komplexní testy po deploymentu

## 🚀 Quick Start

### Setup (jednou)
```bash
make e2e-setup
```

### Spuštění testů

```bash
# PRE-DEPLOY smoke tests (5-7 min)
make test-e2e-pre

# POST-DEPLOY full E2E (20-30 min)
make test-e2e-post

# Všechny E2E testy
make test-e2e
```

## 📋 Make Targets

### Základní E2E targets

| Target | Popis | Trvání | Kdy použít |
|--------|-------|--------|------------|
| `make e2e-setup` | Instalace dependencies + Playwright | 2-3 min | Jednou na začátku |
| `make test-e2e-pre` | PRE-DEPLOY smoke testy | 5-7 min | Před každým deploymentem |
| `make test-e2e-post` | POST-DEPLOY full E2E | 20-30 min | Po deploymentu |
| `make test-e2e` | Všechny E2E (pre + post) | 25-35 min | Manuální validace |
| `make e2e-report` | Otevři HTML report | Okamžitě | Po testech |

### Helper targets

| Target | Popis | Použití |
|--------|-------|---------|
| `make e2e-scaffold` | Vytvoř testovací data | Debugging POST testů |
| `make e2e-teardown` | Smaž testovací data | Cleanup po selhání |

### CI/CD targets

| Target | Popis | Kdy použít |
|--------|-------|------------|
| `make ci-test-pipeline` | Plný CI pipeline (unit + E2E gate) | GitHub Actions |
| `make ci-post-deploy` | Post-deployment validace | Po deploy workflow |
| `make test-comprehensive` | Kompletní test suite | Before merge |

## 🔧 Integration Examples

### 1. Local Development Workflow

```bash
# 1. Spusť dev prostředí
make dev-up

# 2. Počkej než naběhne
make dev-check

# 3. Spusť PRE-DEPLOY testy
make test-e2e-pre

# 4. Zobraz report
make e2e-report
```

### 2. CI/CD Pipeline (GitHub Actions)

#### Pre-Deploy Gate
```bash
# Spustí se automaticky při push/PR
make ci-test-pipeline

# Co dělá:
# 1. Unit testy (backend + frontend)
# 2. Spustí prostředí
# 3. PRE-DEPLOY E2E smoke testy
# 4. Blokuje deployment při selhání
```

#### Post-Deploy Validation
```bash
# Spustí se po úspěšném deploymentu
make ci-post-deploy

# Co dělá:
# 1. Scaffold (vytvoří testovací data)
# 2. POST-DEPLOY E2E testy
# 3. Teardown (smaže testovací data)
```

### 3. Rebuild s E2E Gate

```bash
# Rebuild s automatickým E2E gate
RUN_E2E_PRE=true make rebuild

# Kroky:
# 1. Unit testy
# 2. Build images
# 3. Spuštění prostředí
# 4. PRE-DEPLOY E2E testy (pokud RUN_E2E_PRE=true)
```

### 4. Manual Testing Flow

```bash
# Pro manuální testování nových features

# 1. Setup (jednou)
make e2e-setup

# 2. Spusť prostředí
make dev-up

# 3. Vývoj features...
# (hot reload funguje automaticky)

# 4. Test změn
make test-e2e-pre

# 5. Zobraz výsledky
make e2e-report

# 6. Fix issues, opakuj 4-5
```

## 🎭 Test Coverage

### PRE-DEPLOY Tests (Smoke)

**Target**: `make test-e2e-pre`

Testy:
- ✅ `01_login_smoke.spec.ts` - Keycloak login flow
- ✅ `02_menu_rbac_smoke.spec.ts` - Menu RBAC
- ✅ `03_entity_grid_form_smoke.spec.ts` - CRUD operations
- ✅ `04_workflow_panel_smoke.spec.ts` - Workflow UI

**Kdy spustit**:
- Před každým deploymentem
- Po každé změně v auth/RBAC
- Po změnách v UI komponentách
- V GitHub Actions (automaticky)

**Trvání**: 5-7 minut

### POST-DEPLOY Tests (Full E2E)

**Target**: `make test-e2e-post`

Testy:
- ✅ `10_auth_profile_update.spec.ts` - Profile + directory
- ✅ `20_admin_create_entity_and_ui.spec.ts` - Entity creation
- ✅ `30_workflow_create_and_run.spec.ts` - Workflow execution
- ✅ `40_directory_consistency.spec.ts` - Data consistency
- ✅ `50_cleanup_visibility.spec.ts` - Cleanup verification

**Kdy spustit**:
- Po deploymentu do staging
- Po deploymentu do production
- Pro validaci celého systému
- V GitHub Actions (automaticky po deploy)

**Trvání**: 20-30 minut

## 🔄 Environment Variables

### PRE-DEPLOY
```bash
# Default: https://core-platform.local
PRE_BASE_URL=https://core-platform.local

# Ignorovat TLS chyby
E2E_IGNORE_TLS=true
```

### POST-DEPLOY
```bash
# URL prostředí pro testování
# Local development (výchozí):
POST_BASE_URL=https://core-platform.local

# Staging/Production deployment:
POST_BASE_URL=https://staging.your-domain.com

# Admin credentials (výchozí: test_admin/Test.1234)
E2E_ADMIN_USER=test_admin
E2E_ADMIN_PASS=Test.1234
```

## 📊 Reporting

### View Reports

```bash
# Otevře HTML report v browseru
make e2e-report
```

Nebo manuálně:
```bash
open e2e/playwright-report/index.html
```

### Report Files

- **HTML Report**: `e2e/playwright-report/index.html`
- **JSON Report**: `e2e/playwright-report/results.json`
- **Traces**: `e2e/test-results/**/*.zip`
- **Screenshots**: `e2e/test-results/**/screenshots/`

## 🐛 Troubleshooting

### Problem: E2E dependencies missing

```bash
# Solution
make e2e-setup
```

### Problem: Tests timeout

```bash
# Check if environment is running
make dev-check

# Check logs
make logs-backend
make logs-frontend

# Increase timeout in playwright.config.ts
```

### Problem: Login fails

```bash
# Verify test users exist in Keycloak
# Regular: test/Test.1234
# Admin: test_admin/Test.1234

# Check Keycloak logs
make logs-keycloak
```

### Problem: Scaffold fails in POST tests

```bash
# Manually run scaffold to see error
make e2e-scaffold

# Check admin credentials
# Default: test_admin/Test.1234

# Cleanup if needed
make e2e-teardown
```

### Problem: Tests pass locally but fail in CI

```bash
# Check CI logs for:
# 1. Services not fully started (wait-healthy timeout)
# 2. TLS certificate issues (set E2E_IGNORE_TLS=true)
# 3. Network issues (baseURL incorrect)
# 4. Test users not created in Keycloak
```

## 🔗 Integration Points

### 1. Makefile Targets

```makefile
# E2E tests jsou integrovány v:
- help                  # Základní help
- help-advanced         # Advanced help
- rebuild              # Optional E2E gate (RUN_E2E_PRE=true)
- ci-test-pipeline     # CI/CD pipeline
- ci-post-deploy       # Post-deploy validation
- test-comprehensive   # Kompletní test suite
```

### 2. GitHub Workflows

```yaml
# .github/workflows/pre-deploy.yml
- Trigger: push to main, PR
- Command: make ci-test-pipeline
- Gate: Must pass before merge

# .github/workflows/post-deploy.yml
- Trigger: After deploy workflow
- Command: make ci-post-deploy
- Validation: Full E2E on deployed env
```

### 3. VS Code Tasks

Můžete přidat do `.vscode/tasks.json`:

```json
{
  "label": "E2E: PRE-DEPLOY",
  "type": "shell",
  "command": "make test-e2e-pre",
  "group": "test"
}
```

## 📝 Best Practices

### 1. Run PRE tests frequently
```bash
# Po každé feature změně
make test-e2e-pre
```

### 2. Run POST tests on deploy
```bash
# Po každém deploymentu do staging/prod
make ci-post-deploy
```

### 3. Use E2E gate in CI
```bash
# V GitHub Actions
RUN_E2E_PRE=true make rebuild
```

### 4. Keep tests fast
- PRE testy: ≤7 minut (smoke only)
- POST testy: ≤30 minut (full scenarios)

### 5. Clean up on failure
```bash
# Pokud POST testy selžou a zanechají data
make e2e-teardown
```

## 🎉 Summary

### ✅ Co je hotovo

- [x] Plná integrace do Makefile
- [x] PRE-DEPLOY smoke testy
- [x] POST-DEPLOY full E2E
- [x] CI/CD pipeline targets
- [x] Scaffold/teardown helpers
- [x] HTML reporting
- [x] Environment variable support
- [x] GitHub Actions workflows

### 🚀 Použití

```bash
# Development (local vybuildované prostředí)
make dev-up && make test-e2e-pre

# CI/CD Pre-Deploy
make ci-test-pipeline

# CI/CD Post-Deploy (local)
make ci-post-deploy

# CI/CD Post-Deploy (staging/prod)
POST_BASE_URL=https://staging.your-domain.com make ci-post-deploy

# Manual Full Suite
make test-e2e
```

**E2E tests jsou 100% integrovány a připraveny k použití!** 🎊
