# E2E Testing - Two-Tier Strategy

This directory contains the E2E testing infrastructure for Core Platform with a **two-tier approach**:

1. **PRE-DEPLOY** (Fast Smoke Tests): Gate before deployment
2. **POST-DEPLOY** (Full E2E): Validation after deployment

## 🎯 Goals

- **PRE-DEPLOY**: Fast smoke tests (≤5-7 min) against `https://core-platform.local`
- **POST-DEPLOY**: Full E2E tests against deployed environments (`staging`, `production`)
- **No DB access**: All tests use GUI + public/admin APIs only
- **Ephemeral data**: POST tests create and clean up test data automatically
- **Copilot hints**: Failures include actionable suggestions for fixes

## 📁 Structure

```
e2e/
├── config/
│   └── read-config.ts          # Config reader (env + YAML)
├── helpers/
│   ├── login.ts                # Keycloak GUI login helper
│   └── api.ts                  # API wrappers (no DB access)
├── scripts/
│   ├── scaffold.ts             # Create ephemeral test data
│   └── teardown.ts             # Cleanup test data
├── specs/
│   ├── pre/                    # PRE-DEPLOY smoke tests
│   │   ├── 01_login_smoke.spec.ts
│   │   ├── 02_menu_rbac_smoke.spec.ts
│   │   ├── 03_entity_grid_form_smoke.spec.ts
│   │   └── 04_workflow_panel_smoke.spec.ts
│   └── post/                   # POST-DEPLOY full E2E
│       ├── 10_auth_profile_update.spec.ts
│       ├── 20_admin_create_entity_and_ui.spec.ts
│       ├── 30_workflow_create_and_run.spec.ts
│       ├── 40_directory_consistency.spec.ts
│       └── 50_cleanup_visibility.spec.ts
├── .auth/                      # Session storage (gitignored)
├── playwright.config.ts        # Playwright configuration
└── package.json                # Dependencies & scripts
```

## 🚀 Quick Start

### 1. Install Dependencies

```bash
cd e2e
npm install
npx playwright install --with-deps chromium
```

### 2. Run PRE-DEPLOY Tests (Local)

```bash
npm run test:pre
```

Environment variables:
```bash
PRE_BASE_URL=https://core-platform.local
E2E_IGNORE_TLS=true
E2E_USER=test
E2E_PASS=Test.1234
```

### 3. Run POST-DEPLOY Tests (Deployed Environment)

```bash
# Create ephemeral test data
npm run scaffold

# Run tests
POST_BASE_URL=https://staging.core-platform.company npm run test:post

# Cleanup
npm run teardown
```

Environment variables:
```bash
POST_BASE_URL=https://staging.core-platform.company
E2E_ADMIN_USER=test_admin
E2E_ADMIN_PASS=Test.1234
E2E_IGNORE_TLS=false
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PRE_BASE_URL` | Pre-deploy environment URL | `https://core-platform.local` |
| `POST_BASE_URL` | Post-deploy environment URL | (required) |
| `E2E_IGNORE_TLS` | Ignore TLS certificate errors | `false` |
| `E2E_USER` | Test user username | `test` |
| `E2E_PASS` | Test user password | `Test.1234` |
| `E2E_ADMIN_USER` | Admin user for scaffold/teardown | `test_admin` |
| `E2E_ADMIN_PASS` | Admin password | `Test.1234` |

### Config Sources

1. `.env` file (root directory)
2. `backend/src/main/resources/application.properties`
3. Environment variable overrides

## 🧪 Test Suites

### PRE-DEPLOY (Smoke Tests)

Fast tests that verify core functionality:

- ✅ **Login**: Keycloak authentication flow
- ✅ **Menu RBAC**: Menu items match user roles
- ✅ **Entity Grid/Form**: Basic CRUD operations
- ✅ **Workflow Panel**: State highlighting, transitions, guard tooltips

**Run against**: `https://core-platform.local` (local dev environment)  
**Duration**: ≤5-7 minutes  
**Trigger**: Every push to `main`, PR

### POST-DEPLOY (Full E2E)

Complete end-to-end scenarios:

- ✅ **Auth & Profile**: Login → update profile → verify in directory
- ✅ **Admin Entity Creation**: Studio → create entity → publish UI → verify menu
- ✅ **Workflow Execution**: Create workflow → execute → verify timeline
- ✅ **Directory Consistency**: Search users → verify metadata sync
- ✅ **Cleanup Verification**: Teardown → verify entity removed

**Run against**: Deployed environments (staging, production)  
**Duration**: ≤20-30 minutes  
**Trigger**: After successful deployment

## 🔄 CI/CD Integration

### GitHub Actions

#### Pre-Deploy Workflow (`.github/workflows/pre-deploy.yml`)

```yaml
on:
  push:
    branches: [main]
  pull_request:
```

- Runs PRE-DEPLOY smoke tests
- Gates deployment (must pass)
- Uploads HTML report + traces
- Comments on PR with results

#### Post-Deploy Workflow (`.github/workflows/post-deploy.yml`)

```yaml
on:
  workflow_run:
    workflows: ["Deploy"]
    types: [completed]
```

- Runs on self-hosted runner
- Executes scaffold → tests → teardown
- Generates COPILOT hints on failure
- Uploads comprehensive artifacts

### Required Secrets

```bash
# GitHub Repository Secrets
PRE_BASE_URL=https://core-platform.local
E2E_IGNORE_TLS=true

# Test users are hardcoded:
# - Regular user: test / Test.1234
# - Admin user: test_admin / Test.1234

# Environment-specific (staging, production)
staging_BASE_URL=https://staging.core-platform.company
production_BASE_URL=https://prod.core-platform.company
```

## 🛠️ Development

### Writing Tests

```typescript
import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login.js';

test('my test', async ({ page }) => {
  await login(page);
  
  // Your test logic
  await page.goto('/entities/MyEntity');
  await expect(page.locator('.entity-grid')).toBeVisible();
});
```

### Using API Helpers

```typescript
import { createApiContext, getAuthToken, getUISpec } from '../../helpers/api.js';

const token = await getAuthToken();
const api = await createApiContext({ token });

const uiSpec = await getUISpec(api, 'MyEntity');
expect(uiSpec.entity).toBe('MyEntity');

await api.dispose();
```

### Debug Mode

```bash
# Run with headed browser
npx playwright test --project=pre --headed

# Run specific test file
npx playwright test specs/pre/01_login_smoke.spec.ts

# Debug mode
npx playwright test --debug
```

## 📊 Artifacts & Reporting

### Test Reports

- **HTML Report**: `playwright-report/index.html`
- **JSON Report**: `playwright-report/results.json`
- **Traces**: `test-results/**/*.zip`

View report:
```bash
npm run report
```

### COPILOT Hints

On failure, workflows generate JSON with:
- Failed test names
- Suspected root causes
- Recommended fixes with file paths and steps

Example:
```json
{
  "suite": "post-deploy",
  "env": "https://staging.core-platform.company",
  "suspectedCauses": ["Keycloak redirect mismatch"],
  "recommendedFixes": [{
    "title": "Update Keycloak redirect URIs",
    "files": ["keycloak/realm-export.json"],
    "steps": ["Add staging URL to valid redirects", "Redeploy"]
  }]
}
```

## ✅ Definition of Done

- [x] PRE-DEPLOY tests pass against `https://core-platform.local`
- [x] POST-DEPLOY tests pass against deployed environments
- [x] All tests use GUI/API only (no DB access)
- [x] Ephemeral data cleanup works correctly
- [x] COPILOT hints generated on failures
- [x] GitHub workflows integrated
- [x] Trace/report artifacts uploaded

## 🐛 Troubleshooting

### Common Issues

**TLS Certificate Errors**
```bash
E2E_IGNORE_TLS=true npm run test:pre
```

**Keycloak Login Fails**
- Check `E2E_USER` and `E2E_PASS` match Keycloak realm
- Verify redirect URIs include test base URL
- Check Keycloak is accessible

**Scaffold Fails**
- Verify admin credentials (`E2E_ADMIN_USER`, `E2E_ADMIN_PASS`)
- Check admin API endpoints are accessible
- Review scaffold logs for specific error

**Tests Timeout**
- Increase timeout in `playwright.config.ts`
- Check network latency to environment
- Verify services are healthy (Loki, Kafka, MinIO)

## 📚 Resources

- [Playwright Documentation](https://playwright.dev)
- [E2E Implementation Summary](../E2E_IMPLEMENTATION_SUMMARY.md)
- [Two-Tier Gap Analysis](../E2E_TWO_TIER_GAP_ANALYSIS.md)

- **Helpers** jsou také ve `frontend/tests/e2e/helpers/` ze stejného důvodu
- **Config** zůstává v `e2e/config/` protože nemá závislosti na Playwright
- Vše čte z existujících konfiguračních souborů projektu (trunk-based, žádné duplikace)
