/**
 * POST-DEPLOY: Cleanup Visibility Test
 * 
 * Verifies that teardown properly removes test entities from UI.
 */

import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login.js';
import { teardown } from '../../scripts/teardown.js';
import { readFileSync, existsSync } from 'node:fs';

test.describe('Cleanup Visibility E2E', () => {
  test('should remove test entity from menu after teardown', async ({ page }) => {
    // Check if scaffold result exists
    if (!existsSync('e2e/.auth/scaffold-result.json')) {
      test.skip();
      return;
    }
    
    const result = JSON.parse(readFileSync('e2e/.auth/scaffold-result.json', 'utf-8'));
    const entityName = result.entity.name;
    
    // Login
    await login(page);
    
    // Verify entity is visible before teardown
    await page.goto('/');
    const menuItem = page.locator(`nav a:has-text("${entityName}")`).first();
    
    if (await menuItem.count() > 0) {
      await expect(menuItem).toBeVisible();
    }
    
    // Run teardown
    await teardown();
    
    // Refresh and verify entity is gone
    await page.reload();
    await page.waitForLoadState('networkidle');
    
    const menuItemAfter = page.locator(`nav a:has-text("${entityName}")`).first();
    expect(await menuItemAfter.count()).toBe(0);
  });
});
