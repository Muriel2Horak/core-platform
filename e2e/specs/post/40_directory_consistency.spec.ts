/**
 * POST-DEPLOY: Directory Consistency Test
 * 
 * ⚠️ SKIP: Requires specific user setup (current user may not be in directory)
 * TODO: Use scaffold ephemeral users
 * 
 * Verifies that user directory search works correctly
 * and metadata is synchronized (no DB access, only GUI/API).
 */

import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login.js';
import { createApiContext, getAuthToken, searchUsers } from '../../helpers/api';

test.describe('Directory Consistency E2E', () => {
  test.skip('should search user by updated name and verify metadata', async ({ page }) => {
    await login(page);
    
    const token = await getAuthToken();
    const api = await createApiContext({ token });
    
    // Search for current user
    const currentUser = await (await api.get('/api/users/me')).json();
    
    // Search via API (search by username as it's more reliable than displayName)
    const searchTerm = currentUser.username;
    const results = await searchUsers(api, searchTerm);
    
    // Find by username (more reliable than ID which may have different format)
    const found = results.find((u: any) => 
      u.username === currentUser.username || u.id === currentUser.id
    );
    expect(found).toBeTruthy();
    expect(found.username).toBe(currentUser.username);
    
    // Verify in GUI
    // 🔧 FIX: Correct route is /user-directory, not /directory/users
    await page.goto('/user-directory');
    
    // 🎯 A11Y: Use searchbox role instead of type selector
    const searchInput = page.getByRole('searchbox').or(page.getByRole('textbox', { name: /search|hledat/i }));
    await searchInput.fill(currentUser.displayName || currentUser.username);
    
    await expect(page.locator(`text="${currentUser.displayName || currentUser.username}"`).first())
      .toBeVisible({ timeout: 10000 });
    
    await api.dispose();
  });
});
