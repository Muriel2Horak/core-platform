import { test, expect } from '@playwright/test';

/**
 * S10-A: E2E test for Metamodel Studio RBAC & Route Access
 * 
 * Tests:
 * - Admin with CORE_ROLE_STUDIO can access /core-admin/studio
 * - Non-admin user gets 403 or redirected
 */

const API_BASE = process.env.API_BASE || 'https://core-platform.local';
const STUDIO_ADMIN = {
  username: 'studio-admin@muriel.cz',
  password: 'StudioAdmin123!',
  roles: ['CORE_ROLE_STUDIO'],
};
const REGULAR_USER = {
  username: 'regular-user@muriel.cz',
  password: 'RegularUser123!',
  roles: ['CORE_ROLE_USER'],
};

test.describe('S10-A: Metamodel Studio RBAC', () => {
  test('should allow admin with CORE_ROLE_STUDIO to access Studio', async ({
    page,
  }) => {
    // 1. Login as studio admin
    await page.goto(`${API_BASE}/login`);
    await page.fill('input[name="username"]', STUDIO_ADMIN.username);
    await page.fill('input[name="password"]', STUDIO_ADMIN.password);
    await page.click('button[type="submit"]');

    // Wait for redirect to dashboard
    await page.waitForURL(/dashboard/);

    // 2. Navigate to Studio
    await page.goto(`${API_BASE}/core-admin/studio`);

    // 3. Check Studio is loaded
    await expect(page.locator('text=🎨 Metamodel Studio')).toBeVisible();
    await expect(
      page.locator('text=Admin GUI pro správu metamodelu')
    ).toBeVisible();

    // 4. Check 3-column layout
    await expect(page.locator('text=📂 Model Tree')).toBeVisible();
    await expect(page.locator('text=✏️ Editor')).toBeVisible();
    await expect(page.locator('text=🔍 Diff & Validation')).toBeVisible();

    // 5. Check navigation tabs
    await expect(page.locator('text=📦 Entities')).toBeVisible();
    await expect(page.locator('text=🔗 Relations')).toBeVisible();
    await expect(page.locator('text=✓ Validations')).toBeVisible();
    await expect(page.locator('text=⚡ Workflow Steps')).toBeVisible();
  });

  test('should deny access for user without CORE_ROLE_STUDIO', async ({
    page,
  }) => {
    // 1. Login as regular user
    await page.goto(`${API_BASE}/login`);
    await page.fill('input[name="username"]', REGULAR_USER.username);
    await page.fill('input[name="password"]', REGULAR_USER.password);
    await page.click('button[type="submit"]');

    // Wait for redirect to dashboard
    await page.waitForURL(/dashboard/);

    // 2. Try to navigate to Studio
    await page.goto(`${API_BASE}/core-admin/studio`);

    // 3. Check access denied message
    await expect(page.locator('text=Přístup odepřen')).toBeVisible();
    await expect(page.locator('text=CORE_ROLE_STUDIO')).toBeVisible();

    // 4. Studio layout should NOT be visible
    await expect(page.locator('text=📂 Model Tree')).not.toBeVisible();
    await expect(page.locator('text=✏️ Editor')).not.toBeVisible();
  });
});
