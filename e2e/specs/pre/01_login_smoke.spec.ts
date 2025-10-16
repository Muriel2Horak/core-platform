/**
 * PRE-DEPLOY: Login Smoke Test
 * 
 * Fast test that verifies basic Keycloak authentication flow.
 * Should complete in < 30 seconds.
 */

import { test, expect } from '@playwright/test';
import { login, isLoggedIn } from '../../helpers/login.js';
import { TestLogger } from '../../helpers/test-logger';

test.describe('Login Smoke Test', () => {
  test.beforeAll(() => {
    TestLogger.suiteStart('LOGIN SMOKE TESTS');
  });

  test.afterAll(() => {
    TestLogger.suiteEnd('LOGIN SMOKE TESTS');
  });

  test('should login via Keycloak GUI and redirect to dashboard', async ({ page }) => {
    TestLogger.testStart('Keycloak Login & Dashboard Redirect', 1, 3);
    
    // Perform login
    TestLogger.step('Performing Keycloak authentication...', 1);
    await login(page);
    TestLogger.success('Login completed');
    
    // Verify we're on dashboard/home
    TestLogger.verify('Verifying redirect to dashboard...');
    await expect(page).toHaveURL(/\/(dashboard|home)/i);
    TestLogger.success('Redirected to dashboard/home');
    
    // Verify logged in state
    TestLogger.step('Checking logged-in state...', 2);
    const loggedIn = await isLoggedIn(page);
    expect(loggedIn).toBe(true);
    TestLogger.success('User is logged in');
    
    // Verify user menu is visible
    TestLogger.step('Verifying UI elements...', 3);
    const userMenu = page.locator('[data-testid="user-menu"], .user-profile, #user-dropdown').first();
    await expect(userMenu).toBeVisible({ timeout: 5000 });
    TestLogger.success('User menu visible');
    
    TestLogger.testEnd();
  });
  
  test('should show login form on initial visit', async ({ page }) => {
    TestLogger.testStart('Initial Visit Shows Login Form', 2, 3);
    
    TestLogger.step('Navigating to root URL...', 1);
    await page.goto('/');
    TestLogger.success('Page loaded');
    
    // Should redirect to Keycloak or show login form
    TestLogger.verify('Checking for login form elements...');
    const hasLoginForm = await page.locator('input[name="username"], input[type="text"]').count() > 0;
    const hasPasswordField = await page.locator('input[name="password"], input[type="password"]').count() > 0;
    
    expect(hasLoginForm || hasPasswordField).toBeTruthy();
    TestLogger.success('Login form displayed');
    
    TestLogger.testEnd();
  });
  
  test('should reject invalid credentials', async ({ page, context }) => {
    TestLogger.testStart('Invalid Credentials Rejection', 3, 3);
    
    // 🔧 FIX: Clear cookies before test to ensure Keycloak login form appears
    TestLogger.step('Clearing cookies to reset session...', 1);
    await context.clearCookies();
    TestLogger.success('Cookies cleared');
    
    TestLogger.step('Navigating to login page...', 2);
    await page.goto('/');
    TestLogger.success('Login page loaded');
    
    // Wait for login form (should appear now since session is cleared)
    TestLogger.step('Waiting for login form...', 3);
    await page.waitForSelector('input[name="username"]', { timeout: 10000 });
    TestLogger.success('Login form ready');
    
    // Try invalid login
    TestLogger.step('Attempting login with invalid credentials...', 4);
    await page.fill('input[name="username"]', 'invalid-user');
    await page.fill('input[name="password"]', 'wrong-password');
    
    const submitButton = page.locator('button[type="submit"], input[type="submit"]').first();
    await submitButton.click();
    TestLogger.success('Login form submitted');
    
    // Should show error message
    TestLogger.verify('Verifying error message displayed...');
    const errorVisible = await page.locator('text=/invalid|incorrect|error/i').isVisible({ timeout: 5000 }).catch(() => false);
    expect(errorVisible).toBeTruthy();
    TestLogger.success('Error message displayed correctly');
    
    TestLogger.testEnd();
  });
});
