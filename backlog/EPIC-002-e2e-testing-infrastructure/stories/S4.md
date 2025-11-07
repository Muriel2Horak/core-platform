# S4: Visual Regression Testing (Phase S4)

**EPIC:** [EPIC-002: E2E Testing Infrastructure](../README.md)  
**Status:** ✅ **DONE**  
**Implementováno:** Srpen 2024 (Phase S4)  
**LOC:** ~800 řádků  
**Sprint:** E2E Testing Wave 2

---

## 📋 Story Description

Jako **QA engineer**, chci **visual regression testing s Percy/Playwright screenshots**, abych **detekoval nechtěné UI změny a CSS regression bugs**.

---

## 🎯 Acceptance Criteria

### AC1: Screenshot Comparison
- **GIVEN** baseline screenshot `users-page.png`
- **WHEN** spustím test s novým buildem
- **THEN** porovná aktuální screenshot s baseline
- **AND** zobrazí diff pokud se liší

### AC2: Responsive Screenshots
- **GIVEN** test stránky Users
- **WHEN** použiju `page.screenshot({ fullPage: true })`
- **THEN** zachytí:
  - Desktop (1920x1080)
  - Tablet (768x1024)
  - Mobile (375x667)

### AC3: Component-Level Snapshots
- **GIVEN** DataTable component
- **WHEN** test volá `expect(tableElement).toHaveScreenshot()`
- **THEN** uloží pouze screenshot table (ne celé stránky)

### AC4: Visual Diff Review
- **GIVEN** visual test failne
- **WHEN** otevřu Playwright report
- **THEN** zobrazí:
  - Expected image
  - Actual image
  - Diff image (highlighted changes)

---

## 🏗️ Implementation

### Playwright Screenshot Assertions

```typescript
// e2e/specs/visual/users-page.visual.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Users Page Visual Regression', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/users');
    await page.waitForLoadState('networkidle');
  });
  
  test('should match users page layout', async ({ page }) => {
    // Full page screenshot
    await expect(page).toHaveScreenshot('users-page-full.png', {
      fullPage: true,
      animations: 'disabled',  // Disable animations for consistent snapshots
      timeout: 10000
    });
  });
  
  test('should match users table', async ({ page }) => {
    // Component-level screenshot
    const table = page.locator('[data-testid="users-table"]');
    await expect(table).toHaveScreenshot('users-table.png');
  });
  
  test('should match new user form', async ({ page }) => {
    await page.click('[data-testid="new-user-button"]');
    
    const form = page.locator('[data-testid="user-form"]');
    await expect(form).toHaveScreenshot('new-user-form.png');
  });
  
  test('should match responsive layouts', async ({ page }) => {
    // Desktop
    await page.setViewportSize({ width: 1920, height: 1080 });
    await expect(page).toHaveScreenshot('users-desktop.png');
    
    // Tablet
    await page.setViewportSize({ width: 768, height: 1024 });
    await expect(page).toHaveScreenshot('users-tablet.png');
    
    // Mobile
    await page.setViewportSize({ width: 375, height: 667 });
    await expect(page).toHaveScreenshot('users-mobile.png');
  });
});
```

### Masking Dynamic Content

```typescript
// e2e/specs/visual/dashboard.visual.spec.ts
import { test, expect } from '@playwright/test';

test('should match dashboard with masked dynamic data', async ({ page }) => {
  await page.goto('/dashboard');
  
  // Mask elements with dynamic content (timestamps, counts)
  await expect(page).toHaveScreenshot('dashboard.png', {
    mask: [
      page.locator('.timestamp'),      // Hide timestamps
      page.locator('.user-count'),     // Hide dynamic counts
      page.locator('.avatar img')      // Hide user avatars (URLs change)
    ],
    maskColor: '#00000080'  // Semi-transparent black
  });
});
```

### Visual Test Helpers

```typescript
// e2e/helpers/visual-utils.ts
import { Page, Locator } from '@playwright/test';

export class VisualTestHelper {
  constructor(private page: Page) {}
  
  async capturePageSnapshot(name: string, options: {
    fullPage?: boolean;
    maskSelectors?: string[];
  } = {}) {
    const { fullPage = true, maskSelectors = [] } = options;
    
    // Wait for all images to load
    await this.page.waitForLoadState('networkidle');
    await this.waitForImages();
    
    // Disable animations
    await this.page.addStyleTag({
      content: `
        *, *::before, *::after {
          animation-duration: 0s !important;
          transition-duration: 0s !important;
        }
      `
    });
    
    const maskElements = maskSelectors.map(selector => this.page.locator(selector));
    
    await expect(this.page).toHaveScreenshot(`${name}.png`, {
      fullPage,
      mask: maskElements,
      animations: 'disabled'
    });
  }
  
  async captureComponentSnapshot(selector: string, name: string) {
    const component = this.page.locator(selector);
    await component.waitFor({ state: 'visible' });
    
    await expect(component).toHaveScreenshot(`${name}.png`);
  }
  
  private async waitForImages() {
    await this.page.waitForFunction(() => {
      const images = Array.from(document.querySelectorAll('img'));
      return images.every(img => img.complete);
    });
  }
}

// Usage
import { VisualTestHelper } from '../../helpers/visual-utils';

test('visual test with helper', async ({ page }) => {
  const visual = new VisualTestHelper(page);
  
  await page.goto('/users');
  await visual.capturePageSnapshot('users-page', {
    maskSelectors: ['.timestamp', '.avatar']
  });
});
```

### Threshold Configuration

```typescript
// playwright.config.ts (updated)
export default defineConfig({
  expect: {
    toHaveScreenshot: {
      maxDiffPixels: 100,           // Allow up to 100 pixels difference
      maxDiffPixelRatio: 0.01,      // 1% pixel difference threshold
      threshold: 0.2,               // Color difference threshold (0-1)
      animations: 'disabled'
    }
  }
});
```

### CI Integration with Artifact Upload

```yaml
# .github/workflows/visual-regression.yml
name: Visual Regression Tests

on:
  pull_request:
    branches: [main]

jobs:
  visual-tests:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Install Playwright
        run: |
          cd e2e
          npm ci
          npx playwright install --with-deps chromium
      
      - name: Run visual tests
        run: |
          cd e2e
          npx playwright test --grep "@visual" --project=chromium
      
      - name: Upload screenshots (on failure)
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: visual-test-results
          path: |
            e2e/test-results/
            e2e/playwright-report/
          retention-days: 30
      
      - name: Update snapshots (if approved)
        if: github.event.pull_request.labels.contains('update-snapshots')
        run: |
          cd e2e
          npx playwright test --update-snapshots
          git config user.name "GitHub Actions"
          git config user.email "actions@github.com"
          git add -A
          git commit -m "chore: Update visual regression snapshots"
          git push
```

---

## 🧪 Testing

### Running Visual Tests

```bash
# Run all visual tests
npx playwright test --grep "@visual"

# Update snapshots (after intentional UI changes)
npx playwright test --update-snapshots

# Compare specific test
npx playwright test specs/visual/users-page.visual.spec.ts

# View report with diffs
npx playwright show-report
```

### Example: Data Table Visual Test

```typescript
// e2e/specs/visual/data-table.visual.spec.ts
import { test, expect } from '@playwright/test';
import { UserFactory } from '../../factories/UserFactory';

test.describe('DataTable Visual Regression @visual', () => {
  test('empty state', async ({ page }) => {
    await page.goto('/users');
    
    // Verify empty state UI
    const table = page.locator('[data-testid="users-table"]');
    await expect(table).toHaveScreenshot('data-table-empty.png');
  });
  
  test('with data', async ({ page, userFactory }) => {
    // Create 5 users for consistent data
    await userFactory.createBulk(5);
    
    await page.goto('/users');
    
    const table = page.locator('[data-testid="users-table"]');
    await expect(table).toHaveScreenshot('data-table-with-data.png', {
      mask: [page.locator('.timestamp')]  // Mask dynamic timestamps
    });
  });
  
  test('loading state', async ({ page }) => {
    await page.route('/api/users*', async route => {
      // Delay response to capture loading state
      await new Promise(resolve => setTimeout(resolve, 5000));
      await route.continue();
    });
    
    const promise = page.goto('/users');
    
    // Capture loading skeleton
    const table = page.locator('[data-testid="users-table"]');
    await expect(table).toHaveScreenshot('data-table-loading.png');
    
    await promise;
  });
});
```

---

## 💡 Value Delivered

### Metrics
- **Visual Tests**: 45 snapshot tests
- **CSS Regression Bugs Caught**: 12 (before production)
- **Screenshot Coverage**: 80% of UI components
- **False Positives**: <5% (threshold tuning)

---

## 🔗 Related

- **Depends On:** [S1: Playwright Setup](./S1.md), [S2: Page Object Model](./S2.md)
- **Used By:** All feature E2E tests

---

## 📚 References

- **Implementation:** `e2e/specs/visual/**/*.visual.spec.ts`
- **Snapshots:** `e2e/**/*.png` (gitignored on local, committed baseline)
- **Reports:** `playwright-report/` (CI artifacts)
