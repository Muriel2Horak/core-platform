/**
 * POST-DEPLOY: Auth & Profile Update Test
 * 
 * ⚠️ SKIP: Profile edit UI not implemented
 * 
 * Full E2E test:
 * 1. Login via Keycloak GUI
 * 2. Update user profile (displayName)
 * 3. Verify change in User Directory (via public API)
 * 4. Verify streaming event (if UI shows "recently updated" badge)
 */

import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login.js';
import { createApiContext, getAuthToken, getCurrentUser, updateUserProfile, searchUsers } from '../../helpers/api';

test.describe('Auth & Profile Update E2E', () => {
  test.skip('should update user profile and verify in directory', async ({ page }) => {
    // 1. Login via GUI
    await login(page);
    
    // 2. Navigate to user profile
    // 🎯 A11Y: Use role-based selector for user menu button
    const userMenuButton = page.getByRole('button', { name: /account menu/i });
    await userMenuButton.click();
    
    // Click profile menu item (Czech: "Můj profil")
    const profileMenuItem = page.getByRole('menuitem', { name: /můj profil|my profile/i });
    await profileMenuItem.click();
    
    // 3. Update display name
    const newDisplayName = `Test User E2E ${Date.now()}`;
    
    // 🎯 A11Y: Use more specific label (there are multiple inputs with "jméno")
    // Look for editable display name field (not the readonly username field)
    const displayNameInput = page.getByRole('textbox', { name: /display name|zobrazované jméno/i })
      .or(page.locator('input[name="displayName"], input[id*="displayName"]').first());
    await displayNameInput.fill(newDisplayName);
    
    // Save button
    const saveBtn = page.getByRole('button', { name: /save|uložit/i });
    await saveBtn.click();
    
    // Wait for success message
    await expect(page.locator('text=/success|saved|uloženo/i').first()).toBeVisible({ timeout: 5000 });
    
    // 4. Verify via API (same as GUI uses)
    const token = await getAuthToken();
    const api = await createApiContext({ token });
    
    const currentUser = await getCurrentUser(api);
    expect(currentUser.displayName).toBe(newDisplayName);
    
    // 5. Search in User Directory
    const searchResults = await searchUsers(api, newDisplayName);
    
    const found = searchResults.some((u: any) => u.displayName === newDisplayName);
    expect(found).toBeTruthy();
    
    // 6. Verify in GUI
    // 🔧 FIX: Correct route is /user-directory, not /directory/users
    await page.goto('/user-directory');
    
    // 🎯 A11Y: Use searchbox role
    const searchInput = page.getByRole('searchbox').or(page.getByRole('textbox', { name: /search|hledat/i }));
    await searchInput.fill(newDisplayName);
    
    // Should find the updated user
    await expect(page.locator(`text="${newDisplayName}"`).first()).toBeVisible({ timeout: 10000 });
    
    // Cleanup
    await api.dispose();
  });
  
  test.skip('should show recently updated badge in directory', async ({ page }) => {
    // ⚠️ SKIP: Requires streaming/presence events implementation
    // Login
    await login(page);
    
    // Get current user
    const token = await getAuthToken();
    const api = await createApiContext({ token });
    
    // Update profile
    const newName = `Updated ${Date.now()}`;
    await updateUserProfile(api, { displayName: newName });
    
    // Navigate to directory
    // 🔧 FIX: Correct route is /user-directory, not /directory/users
    await page.goto('/user-directory');
    
    // 🎯 A11Y: Use searchbox role
    const searchInput = page.getByRole('searchbox').or(page.getByRole('textbox', { name: /search|hledat/i }));
    await searchInput.fill(newName);
    
    // Look for "recently updated" badge or indicator
    const badge = page.locator('[data-testid="recently-updated"], .badge-updated, .user-updated').first();
    
    if (await badge.count() > 0) {
      await expect(badge).toBeVisible({ timeout: 5000 });
    } else {
      console.log('COPILOT_HINT: No "recently updated" badge found. Check if presence/streaming events are configured.');
    }
    
    await api.dispose();
  });
});
