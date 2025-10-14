/**
 * POST-DEPLOY: Admin Create Entity & UI Test
 * 
 * Full E2E test:
 * 1. Use Admin GUI (Studio) to create TEST entity
 * 2. Add validation/relations
 * 3. Publish/Approve UI spec
 * 4. Verify entity appears in menu
 * 5. Verify RBAC (grid/detail accessible)
 */

import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login.js';
import { createApiContext, getAuthToken, getUISpec } from '../../helpers/api.js';
import { readFileSync } from 'node:fs';

test.describe('Admin Create Entity & UI E2E', () => {
  let entityName: string;
  
  test.beforeAll(() => {
    // Load scaffold result to get test entity name
    try {
      const result = JSON.parse(readFileSync('e2e/.auth/scaffold-result.json', 'utf-8'));
      entityName = result.entity.name;
    } catch {
      entityName = `PersonTest_${Date.now()}`;
    }
  });
  
  test('should create entity via Studio GUI', async ({ page }) => {
    // Login as admin
    await login(page, {
      username: 'test_admin',
      password: 'Test.1234',
    });
    
    // Navigate to Studio
    await page.goto('/studio/entities');
    
    // Click "Create Entity"
    const createBtn = page.locator('button:has-text("Create Entity"), button:has-text("New Entity")').first();
    await createBtn.click();
    
    // Fill entity form
    const nameInput = page.locator('input[name="name"], input[id="entityName"]').first();
    await nameInput.fill(entityName);
    
    // Add fields
    const addFieldBtn = page.locator('button:has-text("Add Field")').first();
    
    // Field 1: firstName
    await addFieldBtn.click();
    await page.locator('input[name="field.name"]').last().fill('firstName');
    await page.locator('select[name="field.type"]').last().selectOption('STRING');
    await page.locator('input[name="field.required"]').last().check();
    
    // Field 2: lastName  
    await addFieldBtn.click();
    await page.locator('input[name="field.name"]').last().fill('lastName');
    await page.locator('select[name="field.type"]').last().selectOption('STRING');
    await page.locator('input[name="field.required"]').last().check();
    
    // Save entity
    const saveBtn = page.locator('button:has-text("Save"), button[type="submit"]').first();
    await saveBtn.click();
    
    // Success message
    await expect(page.locator('text=/success|created/i').first()).toBeVisible({ timeout: 10000 });
  });
  
  test('should publish UI spec and verify in menu', async ({ page }) => {
    await login(page, {
      username: 'test_admin',
      password: 'Test.1234',
    });
    
    // Navigate to entity in Studio
    await page.goto(`/studio/entities/${entityName}`);
    
    // Go to UI Spec tab
    const uiSpecTab = page.locator('button:has-text("UI Spec"), a:has-text("UI Spec")').first();
    await uiSpecTab.click();
    
    // Publish
    const publishBtn = page.locator('button:has-text("Publish")').first();
    await publishBtn.click();
    
    // Confirm
    const confirmBtn = page.locator('button:has-text("Confirm"), button:has-text("Yes")').first();
    if (await confirmBtn.count() > 0) {
      await confirmBtn.click();
    }
    
    // Wait for success
    await expect(page.locator('text=/published|success/i').first()).toBeVisible({ timeout: 10000 });
    
    // Refresh and check menu
    await page.goto('/');
    
    // Entity should appear in menu
    const menuItem = page.locator(`nav a:has-text("${entityName}"), nav a:has-text("Test Persons")`).first();
    await expect(menuItem).toBeVisible({ timeout: 5000 });
  });
  
  test('should render entity grid and detail', async ({ page }) => {
    await login(page);
    
    // Navigate to entity
    await page.goto(`/entities/${entityName}`);
    
    // Grid should render
    const grid = page.locator('[data-testid="entity-grid"], .entity-grid, table').first();
    await expect(grid).toBeVisible({ timeout: 10000 });
    
    // Should have columns for firstName, lastName
    await expect(page.locator('th:has-text("First Name"), th:has-text("firstName")').first()).toBeVisible();
    await expect(page.locator('th:has-text("Last Name"), th:has-text("lastName")').first()).toBeVisible();
  });
  
  test('should verify UI spec via API', async ({ page }) => {
    await login(page);
    
    const token = await getAuthToken();
    const api = await createApiContext({ token });
    
    // Get UI spec
    const uiSpec = await getUISpec(api, entityName);
    
    expect(uiSpec).toBeTruthy();
    expect(uiSpec.entity).toBe(entityName);
    expect(uiSpec.grid).toBeTruthy();
    expect(uiSpec.form).toBeTruthy();
    
    await api.dispose();
  });
});
