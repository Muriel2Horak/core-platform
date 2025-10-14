/**
 * PRE-DEPLOY: Entity Grid & Form Smoke Test
 * 
 * Verifies that entity grid renders and basic CRUD operations work.
 * Tests against existing entity (e.g., Customers).
 */

import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login.js';

test.describe('Entity Grid & Form Smoke Test', () => {
  const testEntity = 'Customers'; // Replace with actual entity name
  
  test.beforeEach(async ({ page }) => {
    await login(page);
  });
  
  test('should render entity grid', async ({ page }) => {
    // Navigate to entity
    await page.goto(`/entities/${testEntity}`);
    
    // Grid should be visible
    const grid = page.locator('[data-testid="entity-grid"], .entity-grid, table').first();
    await expect(grid).toBeVisible({ timeout: 10000 });
    
    // Should have columns
    const headers = page.locator('th, [role="columnheader"]');
    const headerCount = await headers.count();
    expect(headerCount).toBeGreaterThan(0);
  });
  
  test('should open detail/popup on row click', async ({ page }) => {
    await page.goto(`/entities/${testEntity}`);
    
    // Wait for grid
    await page.waitForSelector('[data-testid="entity-grid"], .entity-grid, table', { timeout: 10000 });
    
    // Click first row (if exists)
    const firstRow = page.locator('tbody tr, [role="row"]').first();
    const rowCount = await firstRow.count();
    
    if (rowCount > 0) {
      await firstRow.click();
      
      // Detail/popup should open
      const detail = page.locator('[data-testid="entity-detail"], .entity-detail, .modal, .dialog').first();
      await expect(detail).toBeVisible({ timeout: 5000 });
    } else {
      console.log('No rows in grid, skipping detail test');
    }
  });
  
  test('should show create button if user has permission', async ({ page }) => {
    await page.goto(`/entities/${testEntity}`);
    
    // Create button should be visible (if user has CREATE permission)
    const createBtn = page.locator('button:has-text("Create"), button:has-text("Add"), button:has-text("New")').first();
    
    const count = await createBtn.count();
    if (count > 0) {
      await expect(createBtn).toBeVisible();
      
      // Click to open form
      await createBtn.click();
      
      // Form should appear
      const form = page.locator('form, [data-testid="entity-form"]').first();
      await expect(form).toBeVisible({ timeout: 5000 });
    } else {
      console.log('No create button (user may not have CREATE permission)');
    }
  });
  
  test('should validate required fields in form', async ({ page }) => {
    await page.goto(`/entities/${testEntity}`);
    
    // Open create form
    const createBtn = page.locator('button:has-text("Create"), button:has-text("Add"), button:has-text("New")').first();
    
    const count = await createBtn.count();
    if (count === 0) {
      console.log('No create button, skipping validation test');
      return;
    }
    
    await createBtn.click();
    await page.waitForSelector('form, [data-testid="entity-form"]', { timeout: 5000 });
    
    // Try to submit without filling required fields
    const submitBtn = page.locator('button[type="submit"], button:has-text("Save"), button:has-text("Submit")').first();
    await submitBtn.click();
    
    // Should show validation errors
    const hasErrors = await page.locator('.error, .invalid, [aria-invalid="true"]').count() > 0;
    expect(hasErrors).toBeTruthy();
  });
});
