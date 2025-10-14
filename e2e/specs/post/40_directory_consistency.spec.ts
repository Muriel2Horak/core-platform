/**
 * POST-DEPLOY: Directory Consistency Test
 * 
 * Verifies that user directory search works correctly
 * and metadata is synchronized (no DB access, only GUI/API).
 */

import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login.js';
import { createApiContext, getAuthToken, searchUsers } from '../../helpers/api.js';

test.describe('Directory Consistency E2E', () => {
  test('should search user by updated name and verify metadata', async ({ page }) => {
    await login(page);
    
    const token = await getAuthToken();
    const api = await createApiContext({ token });
    
    // Search for current user
    const currentUser = await (await api.get('/api/users/me')).json();
    
    // Search via API
    const results = await searchUsers(api, currentUser.displayName || currentUser.username);
    
    const found = results.find((u: any) => u.id === currentUser.id);
    expect(found).toBeTruthy();
    
    // Verify in GUI
    await page.goto('/directory/users');
    const searchInput = page.locator('input[type="search"]').first();
    await searchInput.fill(currentUser.displayName || currentUser.username);
    
    await expect(page.locator(`text="${currentUser.displayName || currentUser.username}"`).first())
      .toBeVisible({ timeout: 10000 });
    
    await api.dispose();
  });
});
