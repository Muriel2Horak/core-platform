import { test, expect } from '@playwright/test';

/**
 * E2E tests for Reporting Explorer
 * 
 * These tests validate the complete reporting workflow:
 * - Loading entity data
 * - Pagination controls
 * - Inline editing
 * - Bulk operations
 * - Export functionality
 */

test.describe('Reporting Explorer', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to reporting page
    await page.goto('/reporting');
  });

  test('should load reporting page with default entity', async ({ page }) => {
    // Check page title
    await expect(page.locator('h1')).toContainText('Reporting Explorer');
    
    // Check entity selector exists
    await expect(page.locator('#entity-selector')).toBeVisible();
    
    // Check tabs are visible
    await expect(page.locator('button[role="tab"]').first()).toContainText('Table View');
    await expect(page.locator('button[role="tab"]').nth(1)).toContainText('Charts');
  });

  test('should display data grid with users', async ({ page }) => {
    // Wait for grid to load
    await page.waitForSelector('.ag-root');
    
    // Check column headers
    await expect(page.locator('.ag-header-cell-text').first()).toBeVisible();
    
    // Check data rows exist
    const rowCount = await page.locator('.ag-row').count();
    expect(rowCount).toBeGreaterThan(0);
  });

  test('should switch between table and chart views', async ({ page }) => {
    // Start on table view
    await expect(page.locator('.ag-root')).toBeVisible();
    
    // Click Charts tab
    await page.click('button[role="tab"]:has-text("Charts")');
    
    // Check charts are visible
    await page.waitForTimeout(1000); // Wait for chart render
    await expect(page.locator('canvas')).toBeVisible(); // ECharts uses canvas
  });

  test('should change entity in selector', async ({ page }) => {
    // Open entity selector
    await page.click('#entity-selector');
    
    // Select Tenants
    await page.click('li:has-text("Tenants")');
    
    // Wait for grid to reload
    await page.waitForSelector('.ag-root');
    
    // Verify URL or state change (if applicable)
    const selectedValue = await page.locator('#entity-selector').inputValue();
    expect(selectedValue).toBe('tenants_registry');
  });

  test('should paginate through data', async ({ page }) => {
    // Wait for grid
    await page.waitForSelector('.ag-root');
    
    // Get first row data
    const firstRowBefore = await page.locator('.ag-row').first().textContent();
    
    // Click next page button
    await page.click('button[aria-label="Next Page"]');
    
    // Wait for data to load
    await page.waitForTimeout(500);
    
    // Get first row data after pagination
    const firstRowAfter = await page.locator('.ag-row').first().textContent();
    
    // Rows should be different
    expect(firstRowBefore).not.toBe(firstRowAfter);
  });

  test('should sort by column', async ({ page }) => {
    // Wait for grid
    await page.waitForSelector('.ag-root');
    
    // Click on a column header to sort
    await page.click('.ag-header-cell-text:has-text("Status")');
    
    // Wait for sort to apply
    await page.waitForTimeout(500);
    
    // Check sort indicator appears
    await expect(page.locator('.ag-sort-ascending-icon, .ag-sort-descending-icon')).toBeVisible();
  });

  test('should select multiple rows for bulk action', async ({ page }) => {
    // Wait for grid
    await page.waitForSelector('.ag-root');
    
    // Click row checkboxes
    await page.click('.ag-selection-checkbox >> nth=0');
    await page.click('.ag-selection-checkbox >> nth=1');
    
    // Check bulk action toolbar appears
    await expect(page.locator('text=/2 rows selected/i')).toBeVisible();
    
    // Check bulk action buttons
    await expect(page.locator('button:has-text("Activate")')).toBeVisible();
    await expect(page.locator('button:has-text("Deactivate")')).toBeVisible();
  });

  test('should export data to CSV', async ({ page }) => {
    // Wait for grid
    await page.waitForSelector('.ag-root');
    
    // Set up download listener
    const downloadPromise = page.waitForEvent('download');
    
    // Click export button
    await page.click('button[aria-label="Export to CSV"]');
    
    // Wait for download
    const download = await downloadPromise;
    
    // Check filename
    expect(download.suggestedFilename()).toMatch(/users_directory.*\.csv/);
  });

  test('should handle inline cell editing', async ({ page }) => {
    // Wait for grid
    await page.waitForSelector('.ag-root');
    
    // Double-click on an editable cell
    const cell = page.locator('.ag-cell').filter({ hasText: /ACTIVE|INACTIVE/ }).first();
    await cell.dblclick();
    
    // Check cell is in edit mode
    await expect(page.locator('.ag-cell-inline-editing')).toBeVisible();
    
    // Type new value (if input field)
    const input = page.locator('.ag-cell-inline-editing input');
    if (await input.isVisible()) {
      await input.fill('NEW_VALUE');
      await input.press('Enter');
      
      // Check for success notification
      await expect(page.locator('text=/saved|updated/i')).toBeVisible({ timeout: 5000 });
    }
  });

  test('should show error on concurrent edit conflict', async ({ page }) => {
    // This test simulates a 409 Conflict response
    // In real scenario, another user would edit the same record
    
    // Mock API to return 409 on next PATCH
    await page.route('**/api/entities/**', async (route) => {
      if (route.request().method() === 'PATCH') {
        await route.fulfill({
          status: 409,
          body: JSON.stringify({
            message: 'Concurrent modification detected'
          })
        });
      } else {
        await route.continue();
      }
    });
    
    // Wait for grid
    await page.waitForSelector('.ag-root');
    
    // Try to edit a cell
    const cell = page.locator('.ag-cell').first();
    await cell.dblclick();
    
    const input = page.locator('.ag-cell-inline-editing input');
    if (await input.isVisible()) {
      await input.fill('TEST');
      await input.press('Enter');
      
      // Check error notification
      await expect(page.locator('text=/conflict|modified/i')).toBeVisible({ timeout: 5000 });
    }
  });
});

test.describe('Chart Panel', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/reporting');
    await page.click('button[role="tab"]:has-text("Charts")');
  });

  test('should render chart with data', async ({ page }) => {
    // Wait for chart to render
    await page.waitForSelector('canvas', { timeout: 5000 });
    
    // Check canvas element exists (ECharts)
    const canvas = page.locator('canvas');
    await expect(canvas).toBeVisible();
  });

  test('should switch between chart types', async ({ page }) => {
    // Wait for chart controls
    await page.waitForSelector('text=Chart Type');
    
    // Click chart type selector
    await page.click('label:has-text("Chart Type") + div');
    
    // Select Pie chart
    await page.click('li:has-text("Pie")');
    
    // Wait for chart to re-render
    await page.waitForTimeout(1000);
    
    // Chart should still be visible
    await expect(page.locator('canvas')).toBeVisible();
  });
});
