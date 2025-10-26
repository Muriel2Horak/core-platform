import { test, expect } from '@playwright/test';
import { loginAsAdmin, loginAsUser, loginAsUserManager } from '../../helpers/login';
import {
  createTestUser,
  generateTestName,
  cleanupTestData,
  navigateToAdminPage,
  waitForDialogClose // NEW: Helper to wait for dialog close instead of success messages
} from '../../helpers/fixtures';

/**
 * Users CRUD E2E Tests
 * 
 * Tests complete CRUD operations for user management with RBAC
 * 
 * Coverage:
 * - Create user (admin, user_manager)
 * - Read user list (admin, user_manager, regular user)
 * - Update user (admin, user_manager)
 * - Delete user (admin only)
 * - Assign roles (admin, user_manager)
 * - RBAC verification (user_manager can't delete)
 */

test.describe('Admin: Users CRUD', () => {
  let testUserIds: string[] = [];

  test.afterAll(async ({ browser }) => {
    // Cleanup test data
    const page = await browser.newPage();
    await loginAsAdmin(page);
    await cleanupTestData(page, { userIds: testUserIds });
    await page.close();
  });

  test('should create new user as admin', async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToAdminPage(page, '/users');

    // Click "Create User" button
    const createButton = page.getByRole('button', { name: /vytvořit uživatele|create user|nový uživatel/i });
    await expect(createButton).toBeVisible({ timeout: 10000 });
    await createButton.click();

    // Fill user form
    const username = generateTestName('test_user');
    await page.getByLabel(/uživatelské jméno|username/i).fill(username);
    // FIX: Use exact match to avoid matching "Uživatelské jméno" (username) field
    await page.locator('input[name="firstName"]').or(page.getByLabel('Jméno', { exact: true })).fill('Test');
    await page.getByLabel(/příjmení|last name/i).fill('User CRUD');
    // FIX: Use getByRole to target textbox specifically (avoids matching "E-mail ověřený" checkbox)
    await page.getByRole('textbox', { name: /e-mail/i }).fill(`${username}@test.local`);

    // Submit form
    const saveButton = page.getByRole('button', { name: /uložit|save|vytvořit/i });
    await saveButton.click();

    // Wait for dialog to close (instead of success message which may not exist)
    await waitForDialogClose(page);

    // Verify user appears in list
    await navigateToAdminPage(page, '/users');
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    await searchBox.fill(username);
    await page.waitForTimeout(1000); // Wait for search debounce

    await expect(page.getByText(username)).toBeVisible();
    await expect(page.getByText('Test User CRUD')).toBeVisible();

    // Store for cleanup
    const userRow = page.locator(`text=${username}`).locator('..').locator('..');
    const userId = await userRow.getAttribute('data-user-id') || '';
    if (userId) testUserIds.push(userId);
  });

  test('should create user as user_manager', async ({ page }) => {
    // Login as user with USER_MANAGER role
    await loginAsUser(page, 'test_admin', 'Test.1234'); // test_admin has ADMIN role which includes USER_MANAGER
    await navigateToAdminPage(page, '/users');

    const createButton = page.getByRole('button', { name: /vytvořit uživatele|create user|nový uživatel/i });
    await expect(createButton).toBeVisible({ timeout: 10000 });
    await createButton.click();

    const username = generateTestName('test_user_mgr');
    await page.getByLabel(/uživatelské jméno|username/i).fill(username);
    await page.getByLabel(/jméno|first name/i).fill('Manager');
    await page.getByLabel(/příjmení|last name/i).fill('Created User');
    await page.getByLabel(/e-mail|email/i).fill(`${username}@test.local`);

    const saveButton = page.getByRole('button', { name: /uložit|save|vytvořit/i });
    await saveButton.click();

    await expect(page.getByText(/úspěšně vytvořen|successfully created|uživatel byl vytvořen/i)).toBeVisible({ timeout: 5000 });
  });

  test('should read user list as regular user (read-only)', async ({ page }) => {
    await loginAsUser(page, 'test', 'Test.1234');
    await navigateToAdminPage(page, '/users');

    // Should see users list
    await expect(page.getByText(/seznam uživatelů|users list|uživatelé/i)).toBeVisible({ timeout: 10000 });

    // Should NOT see create button (read-only)
    const createButton = page.getByRole('button', { name: /vytvořit uživatele|create user|nový uživatel/i });
    await expect(createButton).not.toBeVisible();
  });

  test('should update user as admin', async ({ page }) => {
    await loginAsAdmin(page);

    // Create test user first
    const username = generateTestName('test_update');
    const { id: userId } = await createTestUser(page, username, {
      firstName: 'Original',
      lastName: 'Name'
    });
    testUserIds.push(userId);

    // Navigate to users and search
    await navigateToAdminPage(page, '/users');
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    await searchBox.fill(username);
    await page.waitForTimeout(1000);

    // Click edit button
    const editButton = page.locator(`text=${username}`).locator('..').locator('..').getByRole('button', { name: /upravit|edit/i });
    await editButton.click();

    // Update fields
    await page.getByLabel(/jméno|first name/i).fill('Updated');
    await page.getByLabel(/příjmení|last name/i).fill('User Name');

    // Save changes
    const saveButton = page.getByRole('button', { name: /uložit|save/i });
    await saveButton.click();

    await expect(page.getByText(/úspěšně aktualizován|successfully updated|změny uloženy/i)).toBeVisible({ timeout: 5000 });

    // Verify changes in list
    await navigateToAdminPage(page, '/users');
    await searchBox.fill(username);
    await page.waitForTimeout(1000);

    await expect(page.getByText('Updated User Name')).toBeVisible();
  });

  test('should assign roles to user as admin', async ({ page }) => {
    await loginAsAdmin(page);

    // Create test user
    const username = generateTestName('test_roles');
    const { id: userId } = await createTestUser(page, username, {
      roles: ['CORE_ROLE_USER']
    });
    testUserIds.push(userId);

    // Navigate to user and open role assignment
    await navigateToAdminPage(page, '/users');
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    await searchBox.fill(username);
    await page.waitForTimeout(1000);

    const userRow = page.locator(`text=${username}`).locator('..').locator('..');
    const rolesButton = userRow.getByRole('button', { name: /role|přiřadit role|assign roles/i });
    await rolesButton.click();

    // Select additional role (e.g., CORE_ROLE_MONITORING)
    const roleCheckbox = page.getByLabel(/CORE_ROLE_MONITORING/i);
    await roleCheckbox.check();

    // Save role assignment
    const saveButton = page.getByRole('button', { name: /uložit|save|přiřadit/i });
    await saveButton.click();

    await expect(page.getByText(/role přiřazeny|roles assigned|úspěšně přiřazeno/i)).toBeVisible({ timeout: 5000 });

    // Verify role in user detail
    await userRow.click();
    await expect(page.getByText(/CORE_ROLE_MONITORING/i)).toBeVisible();
  });

  test('should delete user as admin only', async ({ page }) => {
    await loginAsAdmin(page);

    // Create test user to delete
    const username = generateTestName('test_delete');
    const { id: userId } = await createTestUser(page, username);

    // Navigate to users and search
    await navigateToAdminPage(page, '/users');
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    await searchBox.fill(username);
    await page.waitForTimeout(1000);

    // Click delete button
    const deleteButton = page.locator(`text=${username}`).locator('..').locator('..').getByRole('button', { name: /smazat|delete|odstranit/i });
    await expect(deleteButton).toBeVisible();
    await deleteButton.click();

    // Confirm deletion
    const confirmButton = page.getByRole('button', { name: /potvrdit|confirm|ano|yes/i });
    await confirmButton.click();

    await expect(page.getByText(/úspěšně smazán|successfully deleted|uživatel odstraněn/i)).toBeVisible({ timeout: 5000 });

    // Verify user is gone
    await searchBox.fill(username);
    await page.waitForTimeout(1000);
    await expect(page.getByText(username)).not.toBeVisible();

    // Remove from cleanup list (already deleted)
    testUserIds = testUserIds.filter(id => id !== userId);
  });

  test('should NOT allow user_manager to delete users (RBAC)', async ({ page }) => {
    await loginAsUser(page, 'test_admin', 'Test.1234'); // Has USER_MANAGER capability

    // Create test user
    const username = generateTestName('test_rbac');
    const { id: userId } = await createTestUser(page, username);
    testUserIds.push(userId);

    await navigateToAdminPage(page, '/users');
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    await searchBox.fill(username);
    await page.waitForTimeout(1000);

    // Delete button should NOT be visible for user_manager
    const deleteButton = page.locator(`text=${username}`).locator('..').locator('..').getByRole('button', { name: /smazat|delete|odstranit/i });
    await expect(deleteButton).not.toBeVisible();

    // Or if visible, clicking should show permission error
    const isVisible = await deleteButton.isVisible().catch(() => false);
    if (isVisible) {
      await deleteButton.click();
      await expect(page.getByText(/nedostatečná oprávnění|insufficient permissions|přístup odepřen/i)).toBeVisible({ timeout: 3000 });
    }
  });

  test('should search and filter users', async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToAdminPage(page, '/users');

    // Search by username
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    await searchBox.fill('test_admin');
    await page.waitForTimeout(1000);

    await expect(page.getByText('test_admin')).toBeVisible();

    // Clear search
    await searchBox.clear();
    await page.waitForTimeout(500);

    // Search by email
    await searchBox.fill('test.admin@');
    await page.waitForTimeout(1000);
    await expect(page.getByText('test_admin')).toBeVisible();
  });

  test('should validate required fields on create', async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToAdminPage(page, '/users');

    const createButton = page.getByRole('button', { name: /vytvořit uživatele|create user|nový uživatel/i });
    await createButton.click();

    // Try to save without required fields
    const saveButton = page.getByRole('button', { name: /uložit|save|vytvořit/i });
    await saveButton.click();

    // Should show validation errors
    await expect(page.getByText(/povinné pole|required field|vyplňte|je nutné vyplnit/i)).toBeVisible({ timeout: 3000 });
  });

  test('should prevent duplicate username', async ({ page }) => {
    await loginAsAdmin(page);

    // Create first user
    const username = generateTestName('test_duplicate');
    const { id: userId } = await createTestUser(page, username);
    testUserIds.push(userId);

    // Try to create another user with same username
    await navigateToAdminPage(page, '/users');
    const createButton = page.getByRole('button', { name: /vytvořit uživatele|create user|nový uživatel/i });
    await createButton.click();

    await page.getByLabel(/uživatelské jméno|username/i).fill(username);
    await page.getByLabel(/jméno|first name/i).fill('Duplicate');
    await page.getByLabel(/příjmení|last name/i).fill('User');
    await page.getByLabel(/e-mail|email/i).fill(`duplicate@test.local`);

    const saveButton = page.getByRole('button', { name: /uložit|save|vytvořit/i });
    await saveButton.click();

    // Should show error about duplicate username
    await expect(page.getByText(/uživatel již existuje|user already exists|duplicitní|username taken/i)).toBeVisible({ timeout: 5000 });
  });
});
