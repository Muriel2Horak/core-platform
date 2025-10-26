import { test, expect } from '@playwright/test';
import { readFileSync } from 'fs';

/**
 * E2E TEST: Loki Monitoring CSV Export
 * 
 * Validates:
 * 1. User can login to Monitoring UI
 * 2. Time filter works (15m preset)
 * 3. Level filter works (level=ERROR)
 * 4. CSV export produces valid file
 * 5. CSV contains headers and data
 * 
 * Prerequisites:
 * - Services running (make up / make dev-up)
 * - Test data exists (run smoke test first)
 * - Environment variables set (see .env.e2e.example)
 */

const BASE_URL = process.env.E2E_BASE_URL ?? 'https://admin.core-platform.local';
const MONITORING_PATH = process.env.E2E_MONITORING_PATH ?? '/monitoring';
const USERNAME = process.env.E2E_USERNAME ?? 'test_admin';
const PASSWORD = process.env.E2E_PASSWORD ?? 'admin123';

test.describe('Loki Monitoring - CSV Export', () => {
  test.setTimeout(90000); // 90s for slow local SSL

  test('Login → Set 15m filter → level=ERROR → Export CSV → Verify content', async ({ page }) => {
    // === STEP 1: Navigate to Monitoring (may redirect to Keycloak login) ===
    await test.step('Navigate to Monitoring page', async () => {
      await page.goto(`${BASE_URL}${MONITORING_PATH}`, { 
        waitUntil: 'domcontentloaded',
        timeout: 30000 
      });
    });

    // === STEP 2: Handle Keycloak Login if redirected ===
    await test.step('Login via Keycloak if needed', async () => {
      const usernameInput = page.locator('input[name="username"]').first();
      
      // Check if we're on Keycloak login page (wait max 3s)
      const isLoginPage = await usernameInput.isVisible({ timeout: 3000 }).catch(() => false);
      
      if (isLoginPage) {
        await usernameInput.fill(USERNAME);
        await page.fill('input[name="password"]', PASSWORD);
        await page.click('button[type="submit"]');
        
        // Wait for redirect back to app
        await page.waitForURL(new RegExp(`${MONITORING_PATH}`), { timeout: 15000 });
      }
    });

    // === STEP 3: Verify Monitoring UI loaded ===
    await test.step('Verify Monitoring UI components visible', async () => {
      // Wait for log viewer to be ready
      const logViewer = page.locator('[data-testid="log-viewer"]');
      await expect(logViewer).toBeVisible({ timeout: 10000 });
    });

    // === STEP 4: Set 15m time filter ===
    await test.step('Set time filter to last 15 minutes', async () => {
      const quick15m = page.locator('[data-testid="time-quick-15m"]');
      
      // If quick preset exists, use it
      if (await quick15m.isVisible({ timeout: 2000 }).catch(() => false)) {
        await quick15m.click();
      } else {
        // Fallback: use custom time picker
        await page.locator('[data-testid="time-range-picker"]').click();
        await page.locator('[data-testid="time-range-custom"]').click();
        await page.locator('[data-testid="time-from"]').fill('15');
        await page.locator('[data-testid="time-unit"]').selectOption('minutes');
        await page.locator('[data-testid="time-apply"]').click();
      }
    });

    // === STEP 5: Apply level=ERROR filter ===
    await test.step('Apply level=ERROR filter', async () => {
      const filterInput = page.locator('[data-testid="log-filter"]');
      await filterInput.fill('level="error"');
      
      const applyButton = page.locator('[data-testid="apply-filter"]');
      await applyButton.click();
      
      // Wait for results to load (spinner disappears)
      await page.waitForSelector('[data-testid="loading-spinner"]', { 
        state: 'hidden', 
        timeout: 10000 
      }).catch(() => {
        // If no spinner, that's fine
      });
    });

    // === STEP 6: Verify log results appeared ===
    await test.step('Verify log results loaded', async () => {
      const logRow = page.locator('[data-testid="log-row"]').first();
      
      // Either we have results, or we have "empty state" message
      const hasResults = await logRow.isVisible({ timeout: 5000 }).catch(() => false);
      const hasEmptyState = await page.locator('[data-testid="empty-state"]').isVisible({ timeout: 1000 }).catch(() => false);
      
      if (!hasResults && !hasEmptyState) {
        throw new Error('Expected either log results or empty state, got neither');
      }
      
      if (hasEmptyState) {
        test.skip(true, 'No error logs in last 15m - skipping CSV export test');
      }
    });

    // === STEP 7: Export CSV ===
    let csvContent: string = '';
    await test.step('Export CSV file', async () => {
      const exportButton = page.locator('[data-testid="export-csv"]');
      await expect(exportButton).toBeVisible();
      
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 15000 }),
        exportButton.click(),
      ]);
      
      // Verify download happened
      const path = await download.path();
      expect(path).toBeTruthy();
      
      // Read CSV content from downloaded file
      csvContent = readFileSync(path!, 'utf-8');
      expect(csvContent.length).toBeGreaterThan(0);
    });

    // === STEP 8: Verify CSV structure ===
    await test.step('Verify CSV headers and content', async () => {
      // Check for expected headers
      const headerLine = csvContent.split('\n')[0];
      expect(headerLine.toLowerCase()).toMatch(/timestamp/);
      expect(headerLine.toLowerCase()).toMatch(/level/);
      expect(headerLine.toLowerCase()).toMatch(/message/);
      
      // Check we have at least 2 lines (header + 1 data row)
      const lines = csvContent.trim().split('\n');
      expect(lines.length).toBeGreaterThanOrEqual(2);
      
      // Verify data row contains actual data (not empty)
      if (lines.length > 1) {
        const dataRow = lines[1];
        expect(dataRow.length).toBeGreaterThan(0);
        expect(dataRow).not.toBe(headerLine); // Data != header
      }
    });
  });

  test('Verify tenant isolation - different realm sees different data', async ({ page, context }) => {
    test.skip(!process.env.E2E_USERNAME_REALM2, 'Set E2E_USERNAME_REALM2 and E2E_PASSWORD_REALM2 to test tenant isolation');
    
    const realm2Username = process.env.E2E_USERNAME_REALM2!;
    const realm2Password = process.env.E2E_PASSWORD_REALM2!;
    
    // === Get data from Realm 1 (admin) ===
    await test.step('Login as Realm 1 user and get log count', async () => {
      await page.goto(`${BASE_URL}${MONITORING_PATH}`);
      
      // Login if needed
      const usernameInput = page.locator('input[name="username"]').first();
      if (await usernameInput.isVisible({ timeout: 3000 }).catch(() => false)) {
        await usernameInput.fill(USERNAME);
        await page.fill('input[name="password"]', PASSWORD);
        await page.click('button[type="submit"]');
        await page.waitForURL(new RegExp(`${MONITORING_PATH}`), { timeout: 15000 });
      }
      
      await expect(page.locator('[data-testid="log-viewer"]')).toBeVisible({ timeout: 10000 });
      
      // Get total log count
      const realm1Count = await page.locator('[data-testid="total-logs-count"]').textContent();
      expect(realm1Count).toBeTruthy();
      
      // Store in test context
      (test as any).realm1Count = realm1Count;
    });
    
    // === Logout and login as Realm 2 user ===
    await test.step('Logout and login as Realm 2 user', async () => {
      // Logout
      await page.locator('[data-testid="user-menu"]').click();
      await page.locator('[data-testid="logout"]').click();
      
      // Clear cookies to force re-login
      await context.clearCookies();
      
      // Navigate to Monitoring again
      await page.goto(`${BASE_URL}${MONITORING_PATH}`);
      
      // Should redirect to Keycloak login
      await page.waitForSelector('input[name="username"]', { timeout: 10000 });
      await page.fill('input[name="username"]', realm2Username);
      await page.fill('input[name="password"]', realm2Password);
      await page.click('button[type="submit"]');
      
      await page.waitForURL(new RegExp(`${MONITORING_PATH}`), { timeout: 15000 });
    });
    
    // === Verify Realm 2 sees different data ===
    await test.step('Verify Realm 2 user sees different log count', async () => {
      await expect(page.locator('[data-testid="log-viewer"]')).toBeVisible({ timeout: 10000 });
      
      const realm2Count = await page.locator('[data-testid="total-logs-count"]').textContent();
      expect(realm2Count).toBeTruthy();
      
      // Counts should be different (unless both are 0)
      const realm1 = (test as any).realm1Count;
      if (realm1 !== '0' && realm2Count !== '0') {
        expect(realm2Count).not.toBe(realm1);
      }
    });
  });
});
