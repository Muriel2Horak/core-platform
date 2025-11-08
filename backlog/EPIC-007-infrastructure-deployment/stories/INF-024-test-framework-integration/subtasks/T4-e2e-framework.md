# T4: E2E Test Framework Setup (Playwright)

**Parent Story:** INF-024 Test Framework Integration  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 4 hours  
**Owner:** QA/Frontend

---

## 🎯 Objective

Configure Playwright E2E tests integrated with Makefile (`test-e2e-pre`, `test-e2e-post`).

---

## 📋 Tasks

### 1. Install Playwright (Already Done ✅)

```bash
cd e2e
npm install --save-dev @playwright/test
npx playwright install chromium
```

### 2. Create Playwright Configuration

**File:** `e2e/playwright.config.ts`

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './specs',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['json', { outputFile: 'test-results.json' }],
    ['junit', { outputFile: 'test-results.xml' }],
    ['list']
  ],
  use: {
    baseURL: process.env.BASE_URL || 'https://admin.core-platform.local',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] }
    }
  ],
  webServer: process.env.CI ? undefined : {
    command: 'echo "Assuming services already running"',
    url: 'https://admin.core-platform.local/api/actuator/health',
    reuseExistingServer: true,
    ignoreHTTPSErrors: true,
    timeout: 30000
  }
});
```

### 3. Create E2E Test Helpers

**File:** `e2e/helpers/auth.ts`

```typescript
import { Page } from '@playwright/test';

export async function loginAsAdmin(page: Page) {
  await page.goto('/');
  
  // Keycloak login
  await page.fill('input[name="username"]', process.env.TEST_ADMIN_USER || 'admin');
  await page.fill('input[name="password"]', process.env.TEST_ADMIN_PASSWORD || 'admin');
  await page.click('input[type="submit"]');
  
  // Wait for redirect to dashboard
  await page.waitForURL('/dashboard');
}

export async function logout(page: Page) {
  await page.click('[data-testid="user-menu"]');
  await page.click('[data-testid="logout-button"]');
  await page.waitForURL('/');
}
```

### 4. Create Smoke Test Suite

**File:** `e2e/specs/smoke/health-check.spec.ts`

```typescript
import { test, expect } from '@playwright/test';

test.describe('Pre-Deploy Smoke Tests', () => {
  test('API health check', async ({ request }) => {
    const response = await request.get('/api/actuator/health');
    expect(response.ok()).toBeTruthy();
    
    const data = await response.json();
    expect(data.status).toBe('UP');
  });

  test('Frontend loads', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/Core Platform/);
  });

  test('Login page accessible', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('input[name="username"]')).toBeVisible();
  });
});
```

### 5. Create Full E2E Test Suite

**File:** `e2e/specs/users/user-crud.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { loginAsAdmin, logout } from '../../helpers/auth';

test.describe('User Management E2E', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test.afterEach(async ({ page }) => {
    await logout(page);
  });

  test('Create new user', async ({ page }) => {
    // Navigate to users page
    await page.click('[data-testid="nav-users"]');
    await page.waitForURL('/users');

    // Click create button
    await page.click('[data-testid="create-user-button"]');
    
    // Fill form
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="name"]', 'Test User');
    await page.click('button[type="submit"]');

    // Verify success
    await expect(page.locator('.toast-success')).toContainText('User created');
    await expect(page.locator('[data-testid="user-table"]')).toContainText('test@example.com');
  });

  test('Edit user', async ({ page }) => {
    await page.goto('/users');
    
    // Find user row and click edit
    const row = page.locator('[data-testid="user-row"]').filter({ hasText: 'test@example.com' });
    await row.locator('[data-testid="edit-button"]').click();

    // Update name
    await page.fill('input[name="name"]', 'Updated User');
    await page.click('button[type="submit"]');

    // Verify
    await expect(page.locator('.toast-success')).toContainText('User updated');
  });

  test('Delete user', async ({ page }) => {
    await page.goto('/users');
    
    const row = page.locator('[data-testid="user-row"]').filter({ hasText: 'test@example.com' });
    await row.locator('[data-testid="delete-button"]').click();

    // Confirm deletion
    await page.click('[data-testid="confirm-delete"]');

    // Verify
    await expect(page.locator('.toast-success')).toContainText('User deleted');
    await expect(page.locator('[data-testid="user-table"]')).not.toContainText('test@example.com');
  });
});
```

### 6. Integrate with Makefile (Already Done ✅)

**File:** `Makefile`

```makefile
.PHONY: test-e2e-pre test-e2e-post test-e2e

test-e2e-pre:
	@echo "🧪 Running E2E smoke tests (pre-deploy)"
	cd e2e && npx playwright test specs/smoke --project=chromium

test-e2e-post:
	@echo "🧪 Running full E2E test suite (post-deploy)"
	cd e2e && npx playwright test --project=chromium,firefox

test-e2e: test-e2e-pre test-e2e-post
```

### 7. Test E2E Framework

```bash
# Ensure services running
make up
make verify

# Run smoke tests (fast, ~2-3 min)
make test-e2e-pre

# Run full suite (slow, ~15-20 min)
make test-e2e-post

# View HTML report
cd e2e
npx playwright show-report
```

---

## ✅ Acceptance Criteria

- [ ] Playwright installed and configured
- [ ] Smoke test suite runs in < 5 minutes
- [ ] Full E2E suite covers CRUD operations
- [ ] Tests run in CI/CD pipeline
- [ ] HTML report generated with screenshots
- [ ] Failed tests capture video recordings
- [ ] Makefile targets `test-e2e-pre` and `test-e2e-post` work
- [ ] Tests pass consistently (< 1% flakiness)

---

## 🔗 Dependencies

- **REQUIRES:** T3 (Coverage Thresholds)
- Services must be running (`make up`)
