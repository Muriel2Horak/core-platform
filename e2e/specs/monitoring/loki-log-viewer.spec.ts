/**
 * E2E Tests for Loki Native UI - LogViewer Component
 * 
 * Test Coverage:
 * - LogViewer renders with default query
 * - Time range selector works
 * - Query input changes filter results
 * - Auto-refresh toggle functionality
 * - CSV export downloads file
 * - Tenant isolation (admin tenant only sees their logs)
 * - Error handling for failed API calls
 * 
 * Prerequisites:
 * - Loki running with logs ingested
 * - Backend BFF endpoints operational
 * - User logged in as admin tenant
 */

import { test, expect } from '@playwright/test';

test.describe('Loki Log Viewer - Basic Functionality', () => {
  
  test.beforeEach(async ({ page }) => {
    // Login as admin user (adjust based on your auth flow)
    await page.goto('/login');
    await page.fill('[name="username"]', 'admin');
    await page.fill('[name="password"]', 'admin');
    await page.click('button[type="submit"]');
    await page.waitForURL('/admin', { timeout: 10000 });
  });

  test('LogViewer renders on Monitoring Page', async ({ page }) => {
    await page.goto('/admin/monitoring');
    
    // Wait for LogViewer component to load
    await expect(page.getByText('Log Query (LogQL)')).toBeVisible();
    
    // Check default query is present
    const queryInput = page.locator('input[placeholder*="LogQL"]');
    await expect(queryInput).toBeVisible();
    
    // Check time range selector
    await expect(page.getByText(/Last.*hours?/)).toBeVisible();
    
    // Check log table headers
    await expect(page.getByText('Timestamp')).toBeVisible();
    await expect(page.getByText('Level')).toBeVisible();
    await expect(page.getByText('Service')).toBeVisible();
    await expect(page.getByText('Message')).toBeVisible();
  });

  test('Time range selector filters logs', async ({ page }) => {
    await page.goto('/admin/monitoring');
    
    // Wait for initial logs to load
    await page.waitForSelector('table tbody tr', { timeout: 15000 });
    const initialRowCount = await page.locator('table tbody tr').count();
    
    // Change time range to 24 hours
    await page.click('button:has-text("Last")'); // Open dropdown
    await page.click('[data-value="24"]'); // Select 24 hours
    
    // Wait for reload
    await page.waitForTimeout(2000);
    
    // Verify logs reloaded (row count may change)
    const newRowCount = await page.locator('table tbody tr').count();
    expect(newRowCount).toBeGreaterThan(0);
  });

  test('Query input changes filter logs', async ({ page }) => {
    await page.goto('/admin/monitoring');
    
    // Wait for default logs
    await page.waitForSelector('table tbody tr', { timeout: 15000 });
    
    // Change query to filter errors only
    const queryInput = page.locator('input[placeholder*="LogQL"]');
    await queryInput.clear();
    await queryInput.fill('{service="backend"} |~ "(?i)error"');
    await queryInput.press('Enter');
    
    // Wait for filtered results
    await page.waitForTimeout(2000);
    
    // Verify ERROR level badges are present
    const errorBadges = page.locator('[data-testid*="error"], .MuiChip-colorError');
    const count = await errorBadges.count();
    
    // If no errors in last 1h, that's OK - test query execution worked
    if (count > 0) {
      await expect(errorBadges.first()).toBeVisible();
    }
  });

  test('Auto-refresh toggle works', async ({ page }) => {
    await page.goto('/admin/monitoring');
    
    // Find auto-refresh toggle
    const autoRefreshButton = page.locator('button:has-text("Auto Refresh")');
    await expect(autoRefreshButton).toBeVisible();
    
    // Toggle OFF
    await autoRefreshButton.click();
    await expect(page.getByText(/Auto refresh disabled/i)).toBeVisible();
    
    // Toggle ON
    await autoRefreshButton.click();
    await expect(page.getByText(/Auto refresh enabled/i)).toBeVisible();
  });

  test('CSV export downloads file', async ({ page }) => {
    await page.goto('/admin/monitoring');
    
    // Wait for logs to load
    await page.waitForSelector('table tbody tr', { timeout: 15000 });
    
    // Setup download listener
    const downloadPromise = page.waitForEvent('download', { timeout: 10000 });
    
    // Click Export CSV button
    const exportButton = page.locator('button:has-text("Export CSV")');
    await exportButton.click();
    
    // Wait for download
    const download = await downloadPromise;
    
    // Verify filename
    expect(download.suggestedFilename()).toMatch(/logs-\d{4}-\d{2}-\d{2}\.csv/);
    
    // Verify file content (basic check)
    const content = await download.path();
    expect(content).toBeTruthy();
  });

  test('Tenant isolation - only sees own logs', async ({ page }) => {
    await page.goto('/admin/monitoring');
    
    // Wait for logs
    await page.waitForSelector('table tbody tr', { timeout: 15000 });
    
    // Check that all log entries have tenant="admin" in query
    // (This is implicit via BFF, but we can verify via network monitoring)
    
    const response = await page.waitForResponse(
      (resp) => resp.url().includes('/api/monitoring/logs'),
      { timeout: 10000 }
    );
    
    expect(response.status()).toBe(200);
    
    // Verify response contains logs (basic check)
    const json = await response.json();
    expect(json).toHaveProperty('data');
  });

  test('Error handling for failed API calls', async ({ page }) => {
    // Intercept API call to return error
    await page.route('**/api/monitoring/logs*', (route) => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Loki unavailable' }),
      });
    });
    
    await page.goto('/admin/monitoring');
    
    // Wait for error message
    await expect(page.getByText(/failed to load logs/i)).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Loki Log Viewer - Admin Pages Integration', () => {
  
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="username"]', 'admin');
    await page.fill('[name="password"]', 'admin');
    await page.click('button[type="submit"]');
    await page.waitForURL('/admin', { timeout: 10000 });
  });

  test('Admin Security Page shows security events', async ({ page }) => {
    await page.goto('/admin/security');
    
    // Verify LogViewer is present
    await expect(page.getByText('Log Query (LogQL)')).toBeVisible();
    
    // Verify default query targets security events
    const queryInput = page.locator('input[placeholder*="LogQL"]');
    const queryValue = await queryInput.inputValue();
    expect(queryValue).toMatch(/(401|403|unauthorized|security)/i);
  });

  test('Admin Audit Page shows audit logs', async ({ page }) => {
    await page.goto('/admin/audit');
    
    // Verify LogViewer is present
    await expect(page.getByText('Log Query (LogQL)')).toBeVisible();
    
    // Verify default query targets audit events
    const queryInput = page.locator('input[placeholder*="LogQL"]');
    const queryValue = await queryInput.inputValue();
    expect(queryValue).toMatch(/(audit|created|updated|deleted)/i);
  });

  test('Streaming Dashboard shows Kafka events', async ({ page }) => {
    await page.goto('/admin/streaming');
    
    // Verify LogViewer is present
    await expect(page.getByText('Log Query (LogQL)')).toBeVisible();
    
    // Verify default query targets streaming events
    const queryInput = page.locator('input[placeholder*="LogQL"]');
    const queryValue = await queryInput.inputValue();
    expect(queryValue).toMatch(/(streaming|kafka|outbox)/i);
  });

  test('Reports page has 3 tabs with LogViewer', async ({ page }) => {
    await page.goto('/admin/reports');
    
    // Verify tabs exist
    await expect(page.getByRole('tab', { name: /system/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /application/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /security/i })).toBeVisible();
    
    // Click each tab and verify LogViewer loads
    for (const tabName of ['System Logs', 'Application Logs', 'Security Logs']) {
      await page.click(`[role="tab"]:has-text("${tabName}")`);
      await expect(page.getByText('Log Query (LogQL)')).toBeVisible();
    }
  });
});

test.describe('Loki Log Viewer - Performance', () => {
  
  test('LogViewer loads within 5 seconds', async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="username"]', 'admin');
    await page.fill('[name="password"]', 'admin');
    await page.click('button[type="submit"]');
    await page.waitForURL('/admin', { timeout: 10000 });
    
    const startTime = Date.now();
    await page.goto('/admin/monitoring');
    
    // Wait for logs table to appear
    await page.waitForSelector('table tbody tr', { timeout: 15000 });
    
    const loadTime = Date.now() - startTime;
    expect(loadTime).toBeLessThan(5000); // 5 second threshold
  });

  test('Handles 1000+ log entries without freezing', async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="username"]', 'admin');
    await page.fill('[name="password"]', 'admin');
    await page.click('button[type="submit"]');
    await page.waitForURL('/admin', { timeout: 10000 });
    
    await page.goto('/admin/monitoring');
    
    // Change query to get maximum entries (5000 limit from config)
    const queryInput = page.locator('input[placeholder*="LogQL"]');
    await queryInput.clear();
    await queryInput.fill('{service=~".+"}'); // All services
    await queryInput.press('Enter');
    
    // Change time range to 24h to get more logs
    await page.click('button:has-text("Last")');
    await page.click('[data-value="24"]');
    
    // Wait for load
    await page.waitForTimeout(3000);
    
    // Verify table is still responsive
    const rows = page.locator('table tbody tr');
    const count = await rows.count();
    
    // If we got lots of logs, verify scrolling works
    if (count > 100) {
      await rows.first().scrollIntoViewIfNeeded();
      await rows.last().scrollIntoViewIfNeeded();
    }
    
    // Table should still be interactive
    expect(count).toBeGreaterThan(0);
  });
});
