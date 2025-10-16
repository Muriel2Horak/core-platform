/**
 * Keycloak Login Helper
 * 
 * Handles GUI-based Keycloak login flow for E2E tests.
 */

import { Page, expect } from '@playwright/test';
import { readE2EConfig } from '../config/read-config.js';

export interface LoginOptions {
  username?: string;
  password?: string;
  waitForDashboard?: boolean;
}

/**
 * Performs Keycloak login via GUI
 * 
 * @param page - Playwright page instance
 * @param options - Login options (defaults to config)
 */
export async function login(page: Page, options: LoginOptions = {}): Promise<void> {
  const config = readE2EConfig();
  
  const username = options.username || config.testUser.username;
  const password = options.password || config.testUser.password;
  const waitForDashboard = options.waitForDashboard ?? true;

  console.log(`🔐 Logging in as ${username}...`);
  
  // Navigate to app (should redirect to Keycloak if not logged in)
  await page.goto('/');
  
  // 🔧 FIX: Check login status AFTER navigation
  const alreadyLoggedIn = await isLoggedIn(page);
  if (alreadyLoggedIn) {
    console.log('✓ Already logged in, skipping Keycloak flow');
    return;
  }

  // Wait for Keycloak login page - improved detection
  const currentUrl = page.url();
  const isKeycloakPage = currentUrl.includes('/realms/') && currentUrl.includes('/protocol/openid-connect');
  
  if (isKeycloakPage) {
    // Fill login form
    await page.fill('input[name="username"]', username);
    await page.fill('input[name="password"]', password);
    
    // Submit - Try button first (newer Keycloak), fallback to input
    const submitButton = page.locator('button[type="submit"], input[type="submit"], button:has-text("Sign In")');
    await submitButton.first().click();
    
    console.log('✓ Keycloak credentials submitted');
  }

  // Wait for redirect back to app
  if (waitForDashboard) {
    // 🎯 Navigation hardening: Wait for redirect to dashboard/home/admin
    await page.waitForURL(/(dashboard|home|core-admin)/, { timeout: 15000 });
    console.log('✓ Redirected back to app');
    
    // Wait for network to be idle (all bundles loaded)
    await page.waitForLoadState('networkidle', { timeout: 15000 });
    console.log('✓ Network idle');
    
    console.log('✓ Login complete');
  }
}

/**
 * Checks if user is already logged in
 * 
 * Uses accessible role-based selector instead of data-testid
 * to work reliably even when test attributes are stripped in production builds.
 */
export async function isLoggedIn(page: Page): Promise<boolean> {
  // Check if on Keycloak login page
  const currentUrl = page.url();
  if (currentUrl.includes('/realms/') && currentUrl.includes('/protocol/openid-connect')) {
    console.log('🔐 Still on Keycloak page, not logged in');
    return false;
  }
  
  // Wait for page to be fully loaded
  await page.waitForLoadState('domcontentloaded');
  
  // 🎯 A11Y-FIRST: Use role-based selector (works in production builds)
  // Quick check (3s timeout) - if we're logged in, menu should be visible immediately
  const userMenuButton = page.getByRole('button', { name: /account menu/i });
  
  try {
    await expect(userMenuButton).toBeVisible({ timeout: 3000 });
    console.log('✅ User menu visible - user is logged in');
    return true;
  } catch {
    // Menu not visible after 3s - probably not logged in
    return false;
  }
}

/**
 * Logout via GUI
 */
export async function logout(page: Page): Promise<void> {
  // Click user menu
  const userMenu = page.locator('[data-testid="user-menu"], .user-profile, #user-dropdown').first();
  await userMenu.click();
  
  // Click logout
  const logoutBtn = page.locator('text=/logout/i, [data-testid="logout-btn"]').first();
  await logoutBtn.click();
  
  // Wait for Keycloak or login page
  await page.waitForURL(/\/auth|\/login/i, { timeout: 10000 });
}

/**
 * Perform login with session reuse (faster for multiple tests)
 * 
 * Saves auth state to .auth/${username}.json
 */
export async function loginWithStorage(page: Page, options: LoginOptions = {}): Promise<void> {
  const config = readE2EConfig();
  const username = options.username || config.testUser.username;
  
  // Try to load stored state
  const storageStatePath = `e2e/.auth/${username}.json`;
  
  try {
    await page.context().storageState({ path: storageStatePath });
    
    // Verify still logged in
    await page.goto('/');
    if (await isLoggedIn(page)) {
      return; // Session is valid
    }
  } catch {
    // No stored state or invalid
  }
  
  // Perform fresh login
  await login(page, options);
  
  // Save session
  await page.context().storageState({ path: storageStatePath });
}

/**
 * Login as admin user (test_admin)
 */
export async function loginAsAdmin(page: Page): Promise<void> {
  await login(page, {
    username: 'test_admin',
    password: 'Test.1234',
  });
}

/**
 * Login as any user with username and password
 */
export async function loginAsUser(page: Page, username: string, password: string): Promise<void> {
  await login(page, {
    username,
    password,
  });
}

/**
 * Login as regular test user (test)
 */
export async function loginAsTestUser(page: Page): Promise<void> {
  await login(page, {
    username: 'test',
    password: 'Test.1234',
  });
}
