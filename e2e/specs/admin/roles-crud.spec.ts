import { test, expect } from '@playwright/test';
import { loginAsAdmin, loginAsUser } from '../../helpers/login';
import {
  createTestRole,
  generateTestName,
  cleanupTestData,
  navigateToAdminPage
} from '../../helpers/fixtures';

/**
 * Roles CRUD E2E Tests
 * 
 * Tests complete CRUD operations for role management with RBAC
 * 
 * Coverage:
 * - Create role (admin only)
 * - Read role list (admin, user_manager can view)
 * - Update role (admin only)
 * - Delete role (admin only)
 * - RBAC verification (user_manager is read-only)
 */

test.describe('Admin: Roles CRUD', () => {
  let testRoleNames: string[] = [];

  test.afterAll(async ({ browser }) => {
    // Cleanup test data
    const page = await browser.newPage();
    await loginAsAdmin(page);
    await cleanupTestData(page, { roleNames: testRoleNames });
    await page.close();
  });

  test('should create new role as admin', async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToAdminPage(page, '/roles');

    // Click "Create Role" button
    const createButton = page.getByRole('button', { name: /vytvořit roli|create role|nová role/i });
    await expect(createButton).toBeVisible({ timeout: 10000 });
    await createButton.click();

    // Fill role form
    const roleName = generateTestName('TEST_ROLE');
    await page.getByLabel(/název role|role name|name/i).fill(roleName);
    await page.getByLabel(/popis|description/i).fill('Test role created by E2E');

    // Submit form
    const saveButton = page.getByRole('button', { name: /uložit|save|vytvořit/i });
    await saveButton.click();

    // Wait for success message
    await expect(page.getByText(/úspěšně vytvořena|successfully created|role byla vytvořena/i)).toBeVisible({ timeout: 5000 });

    // Verify role appears in list
    await navigateToAdminPage(page, '/roles');
    await expect(page.getByText(roleName)).toBeVisible();

    // Store for cleanup
    testRoleNames.push(roleName);
  });

  test('should read role list as user_manager (read-only)', async ({ page }) => {
    await loginAsUser(page, 'test_admin', 'Test.1234'); // Has USER_MANAGER capability
    await navigateToAdminPage(page, '/roles');

    // Should see roles list
    await expect(page.getByText(/seznam rolí|roles list|role/i)).toBeVisible({ timeout: 10000 });

    // Should see system roles
    await expect(page.getByText(/CORE_ROLE_USER/i)).toBeVisible();
    await expect(page.getByText(/CORE_ROLE_ADMIN/i)).toBeVisible();

    // Should NOT see create button (read-only)
    const createButton = page.getByRole('button', { name: /vytvořit roli|create role|nová role/i });
    await expect(createButton).not.toBeVisible();
  });

  test('should NOT allow regular user to access roles page', async ({ page }) => {
    await loginAsUser(page, 'test', 'Test.1234');
    await navigateToAdminPage(page, '/roles');

    // Should show access denied or redirect
    const isDenied = await page.getByText(/přístup odepřen|access denied|nedostatečná oprávnění/i).isVisible().catch(() => false);
    const isRedirected = page.url().includes('/dashboard') || page.url().includes('/403');

    expect(isDenied || isRedirected).toBeTruthy();
  });

  test('should update role as admin', async ({ page }) => {
    await loginAsAdmin(page);

    // Create test role first
    const roleName = generateTestName('TEST_UPDATE_ROLE');
    await createTestRole(page, roleName, 'Original description');
    testRoleNames.push(roleName);

    // Navigate to roles
    await navigateToAdminPage(page, '/roles');

    // Search for role
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(roleName);
      await page.waitForTimeout(1000);
    }

    // Click edit button
    const editButton = page.locator(`text=${roleName}`).locator('..').locator('..').getByRole('button', { name: /upravit|edit/i });
    await editButton.click();

    // Update description
    await page.getByLabel(/popis|description/i).fill('Updated role description');

    // Save changes
    const saveButton = page.getByRole('button', { name: /uložit|save/i });
    await saveButton.click();

    await expect(page.getByText(/úspěšně aktualizována|successfully updated|změny uloženy/i)).toBeVisible({ timeout: 5000 });

    // Verify changes
    await navigateToAdminPage(page, '/roles');
    if (await searchBox.isVisible()) {
      await searchBox.fill(roleName);
      await page.waitForTimeout(1000);
    }
    await expect(page.getByText('Updated role description')).toBeVisible();
  });

  test('should delete role as admin', async ({ page }) => {
    await loginAsAdmin(page);

    // Create test role to delete
    const roleName = generateTestName('TEST_DELETE_ROLE');
    await createTestRole(page, roleName);

    // Navigate to roles
    await navigateToAdminPage(page, '/roles');

    // Search for role
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(roleName);
      await page.waitForTimeout(1000);
    }

    // Click delete button
    const deleteButton = page.locator(`text=${roleName}`).locator('..').locator('..').getByRole('button', { name: /smazat|delete|odstranit/i });
    await expect(deleteButton).toBeVisible();
    await deleteButton.click();

    // Confirm deletion
    const confirmButton = page.getByRole('button', { name: /potvrdit|confirm|ano|yes/i });
    await confirmButton.click();

    await expect(page.getByText(/úspěšně smazána|successfully deleted|role odstraněna/i)).toBeVisible({ timeout: 5000 });

    // Verify role is gone
    if (await searchBox.isVisible()) {
      await searchBox.fill(roleName);
      await page.waitForTimeout(1000);
    }
    await expect(page.getByText(roleName)).not.toBeVisible();

    // Remove from cleanup list (already deleted)
    testRoleNames = testRoleNames.filter(name => name !== roleName);
  });

  test('should NOT allow user_manager to create roles (RBAC)', async ({ page }) => {
    await loginAsUser(page, 'test_admin', 'Test.1234');
    await navigateToAdminPage(page, '/roles');

    // Create button should not be visible
    const createButton = page.getByRole('button', { name: /vytvořit roli|create role|nová role/i });
    await expect(createButton).not.toBeVisible();
  });

  test('should NOT allow user_manager to delete roles (RBAC)', async ({ page }) => {
    await loginAsUser(page, 'test_admin', 'Test.1234');

    // Create test role as admin first
    await loginAsAdmin(page);
    const roleName = generateTestName('TEST_RBAC_ROLE');
    await createTestRole(page, roleName);
    testRoleNames.push(roleName);

    // Switch to user_manager
    await loginAsUser(page, 'test_admin', 'Test.1234');
    await navigateToAdminPage(page, '/roles');

    // Search for role
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(roleName);
      await page.waitForTimeout(1000);
    }

    // Delete button should NOT be visible
    const deleteButton = page.locator(`text=${roleName}`).locator('..').locator('..').getByRole('button', { name: /smazat|delete|odstranit/i });
    await expect(deleteButton).not.toBeVisible();
  });

  test('should search and filter roles', async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToAdminPage(page, '/roles');

    // Search for system role
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill('CORE_ROLE_USER');
      await page.waitForTimeout(1000);

      await expect(page.getByText(/CORE_ROLE_USER/i)).toBeVisible();

      // Clear search
      await searchBox.clear();
      await page.waitForTimeout(500);
    }

    // Should see multiple roles
    await expect(page.getByText(/CORE_ROLE_ADMIN/i)).toBeVisible();
    await expect(page.getByText(/CORE_ROLE_USER/i)).toBeVisible();
  });

  test('should validate required fields on create', async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToAdminPage(page, '/roles');

    const createButton = page.getByRole('button', { name: /vytvořit roli|create role|nová role/i });
    await createButton.click();

    // Try to save without required fields
    const saveButton = page.getByRole('button', { name: /uložit|save|vytvořit/i });
    await saveButton.click();

    // Should show validation errors
    await expect(page.getByText(/povinné pole|required field|vyplňte|je nutné vyplnit/i)).toBeVisible({ timeout: 3000 });
  });

  test('should prevent duplicate role name', async ({ page }) => {
    await loginAsAdmin(page);

    // Create first role
    const roleName = generateTestName('TEST_DUPLICATE_ROLE');
    await createTestRole(page, roleName);
    testRoleNames.push(roleName);

    // Try to create another role with same name
    await navigateToAdminPage(page, '/roles');
    const createButton = page.getByRole('button', { name: /vytvořit roli|create role|nová role/i });
    await createButton.click();

    await page.getByLabel(/název role|role name|name/i).fill(roleName);
    await page.getByLabel(/popis|description/i).fill('Duplicate role');

    const saveButton = page.getByRole('button', { name: /uložit|save|vytvořit/i });
    await saveButton.click();

    // Should show error about duplicate role
    await expect(page.getByText(/role již existuje|role already exists|duplicitní/i)).toBeVisible({ timeout: 5000 });
  });

  test('should show role permissions/capabilities', async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToAdminPage(page, '/roles');

    // Click on CORE_ROLE_ADMIN to see details
    const adminRole = page.getByText(/CORE_ROLE_ADMIN/i).first();
    await adminRole.click();

    // Should show role details with permissions
    await expect(page.getByText(/oprávnění|permissions|capabilities/i)).toBeVisible({ timeout: 5000 });

    // Should show that it's a composite role
    await expect(page.getByText(/composite|složená role/i)).toBeVisible();

    // Should list sub-roles
    await expect(page.getByText(/CORE_ROLE_USER_MANAGER/i)).toBeVisible();
    await expect(page.getByText(/CORE_ROLE_TENANT_ADMIN/i)).toBeVisible();
  });
});
