/**
 * Keycloak Login Helper
 * 
 * Handles GUI-based Keycloak login flow for E2E tests.
 */

import { Page } from '@playwright/test';
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
  
  // 🔧 FIX: Zkontroluj jestli už jsme přihlášení (Keycloak session caching)
  const alreadyLoggedIn = await isLoggedIn(page);
  if (alreadyLoggedIn) {
    console.log('✓ Already logged in, skipping Keycloak flow');
    return;
  }
  
  const username = options.username || config.testUser.username;
  const password = options.password || config.testUser.password;
  const waitForDashboard = options.waitForDashboard ?? true;

  console.log(`🔐 Logging in as ${username}...`);
  
  // Navigate to app (should redirect to Keycloak if not logged in)
  await page.goto('/');

  // Wait for Keycloak login page
  const isKeycloakPage = page.url().includes(config.keycloak.authServerUrl);
  
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
    // Wait for navigation away from Keycloak
    await page.waitForURL(url => {
      const urlString = url.toString();
      return !urlString.includes('keycloak') && !urlString.includes(':8081');
    }, { timeout: 15000 });
    
    console.log('✓ Redirected back to app');
    
    // Wait for app to be fully loaded (either dashboard or root that will redirect)
    await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {
      // Ignore timeout, continue
    });
    
    // If we're on root, wait a bit for React Router redirect
    const currentURL = page.url();
    if (currentURL.endsWith('/') && !currentURL.includes('/dashboard')) {
      await page.waitForTimeout(2000); // Give React Router time to redirect
    }
    
    console.log('✓ Login complete');
  }
}

/**
 * Checks if user is already logged in
 * 
 * 🔧 FIX: Wait for user menu to appear instead of synchronous count check
 */
export async function isLoggedIn(page: Page): Promise<boolean> {
  const config = readE2EConfig();
  
  // Check if on Keycloak login page
  if (page.url().includes(config.keycloak.authServerUrl)) {
    return false;
  }
  
  // Wait for user menu to appear (with timeout)
  try {
    await page.waitForSelector('[data-testid="user-menu"], .user-profile, #user-dropdown', { 
      timeout: 5000,
      state: 'visible'
    });
    return true;
  } catch {
    // User menu not found within timeout
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
 * Login as regular test user (test)
 */
export async function loginAsTestUser(page: Page): Promise<void> {
  await login(page, {
    username: 'test',
    password: 'Test.1234',
  });
}
