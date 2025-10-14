/**
 * PRE-DEPLOY: Login Smoke Test
 * 
 * Fast test that verifies basic Keycloak authentication flow.
 * Should complete in < 30 seconds.
 */

import { test, expect } from '@playwright/test';
import { login, isLoggedIn } from '../../helpers/login.js';

test.describe('Login Smoke Test', () => {
  test('should login via Keycloak GUI and redirect to dashboard', async ({ page }) => {
    // Perform login
    await login(page);
    
    // Verify we're on dashboard/home
    await expect(page).toHaveURL(/\/(dashboard|home)/i);
    
    // Verify logged in state
    const loggedIn = await isLoggedIn(page);
    expect(loggedIn).toBe(true);
    
    // Verify user menu is visible
    const userMenu = page.locator('[data-testid="user-menu"], .user-profile, #user-dropdown').first();
    await expect(userMenu).toBeVisible({ timeout: 5000 });
  });
  
  test('should show login form on initial visit', async ({ page }) => {
    await page.goto('/');
    
    // Should redirect to Keycloak or show login form
    const hasLoginForm = await page.locator('input[name="username"], input[type="text"]').count() > 0;
    const hasPasswordField = await page.locator('input[name="password"], input[type="password"]').count() > 0;
    
    expect(hasLoginForm || hasPasswordField).toBeTruthy();
  });
  
  test('should reject invalid credentials', async ({ page }) => {
    await page.goto('/');
    
    // Wait for login form
    await page.waitForSelector('input[name="username"]', { timeout: 10000 });
    
    // Try invalid login
    await page.fill('input[name="username"]', 'invalid-user');
    await page.fill('input[name="password"]', 'wrong-password');
    await page.click('input[type="submit"]');
    
    // Should show error message
    const errorVisible = await page.locator('text=/invalid|incorrect|error/i').count() > 0;
    expect(errorVisible).toBeTruthy();
  });
});
