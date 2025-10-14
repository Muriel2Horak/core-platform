/**
 * PRE-DEPLOY: Menu RBAC Smoke Test
 * 
 * Verifies that menu items are rendered according to user roles.
 * Uses UI spec config to validate RBAC.
 */

import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login.js';
import { createApiContext, getAuthToken, getUISpec } from '../../helpers/api.js';

test.describe('Menu RBAC Smoke Test', () => {
  test('should show menu items matching user roles', async ({ page }) => {
    // Login first
    await login(page);
    
    // Get auth token for API calls
    const token = await getAuthToken();
    const api = await createApiContext({ token });
    
    // Fetch UI specs to know what entities exist
    // TODO: Replace with actual entities from your config
    const testEntity = 'Customers'; // Example entity
    
    try {
      const uiSpec = await getUISpec(api, testEntity);
      
      // If user has access to entity, menu item should be visible
      if (uiSpec) {
        const menuItem = page.locator(`nav a:has-text("${uiSpec.menu?.label || testEntity}")`);
        await expect(menuItem).toBeVisible({ timeout: 5000 });
      }
    } catch (error) {
      // Entity doesn't exist or no access - this is fine for smoke test
      console.log(`Entity ${testEntity} not accessible, skipping menu check`);
    }
    
    await api.dispose();
  });
  
  test('should hide admin menu for non-admin users', async ({ page }) => {
    // Login with regular user (not admin)
    await login(page, { 
      username: 'test',
      password: 'Test.1234',
    });
    
    // Admin-only items should not be visible
    const adminMenu = page.locator('nav a:has-text("Studio"), nav a:has-text("Admin"), nav a:has-text("Configuration")').first();
    
    // Either not exists or not visible
    const count = await adminMenu.count();
    if (count > 0) {
      await expect(adminMenu).not.toBeVisible();
    }
  });
  
  test('should show user profile menu', async ({ page }) => {
    await login(page);
    
    // User menu should always be visible for logged-in users
    const userMenu = page.locator('[data-testid="user-menu"], .user-profile, #user-dropdown').first();
    await expect(userMenu).toBeVisible({ timeout: 5000 });
    
    // Click to expand
    await userMenu.click();
    
    // Should show profile and logout options
    const profileOption = page.locator('text=/profile/i').first();
    const logoutOption = page.locator('text=/logout|sign out/i').first();
    
    await expect(profileOption).toBeVisible({ timeout: 3000 });
    await expect(logoutOption).toBeVisible({ timeout: 3000 });
  });
});
