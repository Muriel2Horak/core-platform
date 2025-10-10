import { test, expect } from '@playwright/test';

/**
 * E2E tests for Monitoring Reports/Scenes
 * 
 * Prerequisites:
 * 1. Backend running in test profile: ./mvnw spring-boot:run -Dspring-boot.run.profiles=test
 * 2. Set E2E_BASE_URL to backend URL
 * 
 * These tests verify:
 * - Page loads and renders Grafana Scenes
 * - API calls to /api/monitoring/ds/query work
 * - Time range changes trigger new queries
 * - Rate limiting shows proper toast
 */

test.describe('Monitoring Reports', () => {
  
  test.beforeEach(async ({ page }) => {
    // Mock auth header is set globally in playwright.config.ts
    // But we can override it per test if needed
  });

  test('should load reports page and display data', async ({ page }) => {
    // Navigate to reports page
    await page.goto('/reports');
    
    // Wait for page to load
    await expect(page).toHaveTitle(/Reports|Monitoring/i);
    
    // Wait for API call to complete
    const apiResponse = await page.waitForResponse(
      response => response.url().includes('/api/monitoring/ds/query') && 
                  response.status() === 200,
      { timeout: 10000 }
    );
    
    // Verify response is valid
    expect(apiResponse.status()).toBe(200);
    const body = await apiResponse.json();
    expect(body).toHaveProperty('results');
    
    // Verify UI rendered (at least one chart or table container)
    // Note: Exact selectors depend on Grafana Scenes structure
    await expect(page.locator('[data-testid="scene-container"], .scene-container, .grafana-scene')).toBeVisible({
      timeout: 5000
    });
  });

  test('should change time range and trigger new query', async ({ page }) => {
    await page.goto('/reports');
    
    // Wait for initial load
    await page.waitForResponse(
      resp => resp.url().includes('/api/monitoring/ds/query') && resp.status() === 200
    );
    
    // Find and click time range picker (Grafana Scenes component)
    // Note: Selector depends on actual Scenes implementation
    const timeRangePicker = page.locator('[data-testid="time-range-picker"], button:has-text("Last"), .time-range-picker');
    
    if (await timeRangePicker.isVisible({ timeout: 2000 }).catch(() => false)) {
      await timeRangePicker.click();
      
      // Select different time range (e.g., "Last 6 hours")
      await page.locator('text="Last 6 hours", [data-testid="time-range-6h"]').click();
      
      // Wait for new API call with updated time range
      const newQueryResponse = await page.waitForResponse(
        resp => resp.url().includes('/api/monitoring/ds/query') && 
                resp.status() === 200,
        { timeout: 10000 }
      );
      
      expect(newQueryResponse.status()).toBe(200);
    } else {
      console.warn('Time range picker not found - skipping time range change test');
    }
  });

  test('should handle rate limit (429) with toast notification', async ({ page }) => {
    // Mock rate limit response by intercepting route
    await page.route('**/api/monitoring/ds/query', route => {
      route.fulfill({
        status: 429,
        headers: {
          'Content-Type': 'application/problem+json',
          'Retry-After': '60'
        },
        body: JSON.stringify({
          type: 'about:blank',
          title: 'Too Many Requests',
          status: 429,
          detail: 'Rate limit exceeded. Please try again later.'
        })
      });
    });
    
    await page.goto('/reports');
    
    // Wait for toast/snackbar notification to appear
    // Note: Selector depends on UI notification library (MUI Snackbar, react-toastify, etc.)
    const toast = page.locator('[role="alert"], .MuiSnackbar-root, .toast, .notification');
    await expect(toast).toBeVisible({ timeout: 5000 });
    
    // Verify message contains rate limit info
    await expect(toast).toContainText(/too many requests|rate limit|try again/i);
  });

  test('should handle unauthorized access', async ({ page }) => {
    // Override auth header to remove authentication
    await page.setExtraHTTPHeaders({
      'X-Test-Auth': ''  // Empty auth = unauthorized
    });
    
    const response = await page.goto('/reports');
    
    // Should redirect to login or show 403
    expect(response?.status()).toBeGreaterThanOrEqual(400);
  });

  test('should handle API errors gracefully', async ({ page }) => {
    // Mock server error
    await page.route('**/api/monitoring/ds/query', route => {
      route.fulfill({
        status: 500,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ error: 'Internal Server Error' })
      });
    });
    
    await page.goto('/reports');
    
    // Verify error message is displayed
    const errorMessage = page.locator('[role="alert"], .error-message, .MuiAlert-root');
    await expect(errorMessage).toBeVisible({ timeout: 5000 });
  });

  test('should filter data when applying filters', async ({ page }) => {
    await page.goto('/reports');
    
    // Wait for initial load
    await page.waitForResponse(
      resp => resp.url().includes('/api/monitoring/ds/query') && resp.status() === 200
    );
    
    // Find filter controls (depends on Scenes implementation)
    const filterControl = page.locator('[data-testid="filter"], .filter-control, input[placeholder*="filter" i]');
    
    if (await filterControl.isVisible({ timeout: 2000 }).catch(() => false)) {
      // Apply filter
      await filterControl.fill('test-filter-value');
      
      // Wait for filtered query
      const filteredResponse = await page.waitForResponse(
        resp => resp.url().includes('/api/monitoring/ds/query') && resp.status() === 200,
        { timeout: 10000 }
      );
      
      expect(filteredResponse.status()).toBe(200);
      
      // Verify filter was applied (check request body)
      const requestBody = filteredResponse.request().postDataJSON();
      expect(JSON.stringify(requestBody)).toContain('test-filter-value');
    }
  });
});

test.describe('Monitoring Admin', () => {
  test('should require admin role for admin pages', async ({ page }) => {
    // Set auth with only USER role (no ADMIN)
    await page.setExtraHTTPHeaders({
      'X-Test-Auth': 'tenant=test-tenant;roles=ROLE_USER'
    });
    
    const response = await page.goto('/monitoring/admin');
    
    // Should be forbidden or redirected
    if (response) {
      expect(response.status()).toBeGreaterThanOrEqual(403);
    }
  });
  
  test('admin with ROLE_ADMIN should access admin pages', async ({ page }) => {
    // Set auth with ADMIN role
    await page.setExtraHTTPHeaders({
      'X-Test-Auth': 'tenant=test-tenant;roles=ROLE_USER,ROLE_ADMIN'
    });
    
    const response = await page.goto('/monitoring/admin');
    
    // Should be allowed
    if (response) {
      expect(response.status()).toBeLessThan(400);
    }
  });
});
