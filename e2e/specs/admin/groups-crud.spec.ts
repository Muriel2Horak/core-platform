import { test, expect } from '@playwright/test';
import { loginAsAdmin, loginAsUser, loginAsUserManager } from '../../helpers/login';
import {
  createTestGroup,
  createTestUser,
  generateTestName,
  cleanupTestData,
  navigateToAdminPage,
  waitForDialogClose
} from '../../helpers/fixtures';

/**
 * Groups CRUD E2E Tests
 * 
 * Tests complete CRUD operations for group management with RBAC
 * 
 * Coverage:
 * - Create group (admin, user_manager)
 * - Read group list (admin, user_manager)
 * - Update group (admin, user_manager)
 * - Delete group (admin only)
 * - Add/Remove members (admin, user_manager)
 * - RBAC verification
 */

test.describe('Admin: Groups CRUD', () => {
  let testGroupIds: string[] = [];
  let testUserIds: string[] = [];

  test.afterAll(async ({ browser }) => {
    // Cleanup test data
    const page = await browser.newPage();
    await loginAsAdmin(page);
    await cleanupTestData(page, { groupIds: testGroupIds, userIds: testUserIds });
    await page.close();
  });

  test('should create new group as admin', async ({ page }) => {
    await loginAsAdmin(page);

    // Create via API
    const groupName = generateTestName('test-group');
    const { id: groupId } = await createTestGroup(page, groupName);
    testGroupIds.push(groupId);

    // Verify group appears in UI
    await navigateToAdminPage(page, '/groups');
    await expect(page.getByText(groupName)).toBeVisible();
  });

  test('should read group list as user_manager', async ({ page }) => {
    await loginAsUser(page, 'test_admin', 'Test.1234');
    await navigateToAdminPage(page, '/groups');

    // Should see groups list (use heading to avoid matching navigation menu)
    await expect(page.getByRole('heading', { name: /seznam skupin|groups list/i })).toBeVisible({ timeout: 10000 });

    // Should see create button (user_manager can manage groups)
    const createButton = page.getByRole('button', { name: /vytvořit skupinu|create group|nová skupina/i });
    await expect(createButton).toBeVisible();
  });

  test('should NOT allow regular user to access groups page', async ({ page }) => {
    await loginAsUser(page, 'test', 'Test.1234');
    await navigateToAdminPage(page, '/groups');

    // Should show access denied or redirect
    const isDenied = await page.getByText(/přístup odepřen|access denied|nedostatečná oprávnění/i).isVisible().catch(() => false);
    const isRedirected = page.url().includes('/dashboard') || page.url().includes('/403');

    expect(isDenied || isRedirected).toBeTruthy();
  });

  test('should update group name as admin', async ({ page }) => {
    await loginAsAdmin(page);

    // Create test group first
    const groupName = generateTestName('Test Update Group');
    const { id: groupId } = await createTestGroup(page, groupName);
    testGroupIds.push(groupId);

    // Navigate to groups
    await navigateToAdminPage(page, '/groups');

    // Search for group
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(groupName);
      await page.waitForTimeout(1000);
    }

    // Click edit button
    const editButton = page.locator(`text=${groupName}`).locator('..').locator('..').getByRole('button', { name: /upravit|edit/i });
    await editButton.click();

    // Update name
    const updatedName = `${groupName} - Updated`;
    await page.getByLabel(/název skupiny|group name|name/i).fill(updatedName);

    // Save changes
    const saveButton = page.getByRole('button', { name: /uložit|save/i });
    await saveButton.click();

    await waitForDialogClose(page);

    // Verify changes
    await navigateToAdminPage(page, '/groups');
    await expect(page.getByText(updatedName)).toBeVisible();
  });

  test('should add member to group as admin', async ({ page }) => {
    await loginAsAdmin(page);

    // Create test user and group
    const username = generateTestName('test_user');
    const { id: userId } = await createTestUser(page, username, {
      firstName: 'Test',
      lastName: 'Member'
    });
    testUserIds.push(userId);

    const groupName = generateTestName('Test Member Group');
    const { id: groupId } = await createTestGroup(page, groupName);
    testGroupIds.push(groupId);

    // Navigate to group detail
    await navigateToAdminPage(page, '/groups');
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(groupName);
      await page.waitForTimeout(1000);
    }

    const groupRow = page.locator(`text=${groupName}`).locator('..').locator('..');
    await groupRow.click();

    // Click "Add Member" button
    const addMemberButton = page.getByRole('button', { name: /přidat člena|add member/i });
    await addMemberButton.click();

    // Search for user
    const userSearchBox = page.getByPlaceholder(/hledat uživatele|search user/i);
    await userSearchBox.fill(username);
    await page.waitForTimeout(1000);

    // Select user
    const userCheckbox = page.getByLabel(username).or(page.locator(`text=${username}`).locator('..').locator('input[type="checkbox"]'));
    await userCheckbox.check();

    // Confirm addition
    const confirmButton = page.getByRole('button', { name: /přidat|add|potvrdit/i });
    await confirmButton.click();

    await waitForDialogClose(page);

    // Verify member appears in group
    await expect(page.getByText(username)).toBeVisible();
  });

  test('should remove member from group as admin', async ({ page }) => {
    await loginAsAdmin(page);

    // Create test user and group with member
    const username = generateTestName('test_remove_user');
    const { id: userId } = await createTestUser(page, username);
    testUserIds.push(userId);

    const groupName = generateTestName('Test Remove Group');
    const { id: groupId } = await createTestGroup(page, groupName, {
      members: [userId]
    });
    testGroupIds.push(groupId);

    // Navigate to group detail
    await navigateToAdminPage(page, '/groups');
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(groupName);
      await page.waitForTimeout(1000);
    }

    const groupRow = page.locator(`text=${groupName}`).locator('..').locator('..');
    await groupRow.click();

    // Find remove button for member
    const removeButton = page.locator(`text=${username}`).locator('..').locator('..').getByRole('button', { name: /odebrat|remove/i });
    await removeButton.click();

    // Confirm removal
    const confirmButton = page.getByRole('button', { name: /potvrdit|confirm|ano|yes/i });
    await confirmButton.click();

    await waitForDialogClose(page);

    // Verify member is gone
    await expect(page.getByText(username)).not.toBeVisible();
  });

  test('should delete group as admin', async ({ page }) => {
    await loginAsAdmin(page);

    // Create test group to delete
    const groupName = generateTestName('Test Delete Group');
    await createTestGroup(page, groupName);

    // Navigate to groups
    await navigateToAdminPage(page, '/groups');

    // Search for group
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(groupName);
      await page.waitForTimeout(1000);
    }

    // Click delete button
    const deleteButton = page.locator(`text=${groupName}`).locator('..').locator('..').getByRole('button', { name: /smazat|delete|odstranit/i });
    await expect(deleteButton).toBeVisible();
    await deleteButton.click();

    // Confirm deletion
    const confirmButton = page.getByRole('button', { name: /potvrdit|confirm|ano|yes/i });
    await confirmButton.click();

    await waitForDialogClose(page);

    // Verify group is gone
    if (await searchBox.isVisible()) {
      await searchBox.fill(groupName);
      await page.waitForTimeout(1000);
    }
    await expect(page.getByText(groupName)).not.toBeVisible();
  });

  test('should NOT allow user_manager to delete groups (RBAC)', async ({ page }) => {
    await loginAsAdmin(page);

    // Create test group
    const groupName = generateTestName('Test RBAC Group');
    const { id: groupId } = await createTestGroup(page, groupName);
    testGroupIds.push(groupId);

    // Switch to user_manager
    await loginAsUser(page, 'test_admin', 'Test.1234');
    await navigateToAdminPage(page, '/groups');

    // Search for group
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(groupName);
      await page.waitForTimeout(1000);
    }

    // Delete button should NOT be visible
    const deleteButton = page.locator(`text=${groupName}`).locator('..').locator('..').getByRole('button', { name: /smazat|delete|odstranit/i });
    await expect(deleteButton).not.toBeVisible();
  });

  test('should search and filter groups', async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToAdminPage(page, '/groups');

    // Create test groups for search
    const group1 = generateTestName('Alpha Group');
    const group2 = generateTestName('Beta Group');
    
    const { id: id1 } = await createTestGroup(page, group1);
    const { id: id2 } = await createTestGroup(page, group2);
    testGroupIds.push(id1, id2);

    // Refresh page
    await navigateToAdminPage(page, '/groups');

    // Search for specific group
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill('Alpha');
      await page.waitForTimeout(1000);

      await expect(page.getByText(group1)).toBeVisible();
      await expect(page.getByText(group2)).not.toBeVisible();

      // Clear and search for other
      await searchBox.clear();
      await searchBox.fill('Beta');
      await page.waitForTimeout(1000);

      await expect(page.getByText(group2)).toBeVisible();
    }
  });

  test('should validate required fields on create', async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToAdminPage(page, '/groups');

    const createButton = page.getByRole('button', { name: /vytvořit skupinu|create group|nová skupina/i });
    await createButton.click();

    // Try to save without required fields
    const saveButton = page.getByRole('button', { name: /uložit|save|vytvořit/i });
    await saveButton.click();

    // Should show validation errors
    await expect(page.getByText(/povinné pole|required field|vyplňte|je nutné vyplnit/i)).toBeVisible({ timeout: 3000 });
  });

  test('should show group member count', async ({ page }) => {
    await loginAsAdmin(page);

    // Create group with members
    const username1 = generateTestName('member1');
    const username2 = generateTestName('member2');
    
    const { id: userId1 } = await createTestUser(page, username1);
    const { id: userId2 } = await createTestUser(page, username2);
    testUserIds.push(userId1, userId2);

    const groupName = generateTestName('Members Count Group');
    const { id: groupId } = await createTestGroup(page, groupName, {
      members: [userId1, userId2]
    });
    testGroupIds.push(groupId);

    // Navigate to groups
    await navigateToAdminPage(page, '/groups');

    // Search for group
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(groupName);
      await page.waitForTimeout(1000);
    }

    // Should show member count
    await expect(page.getByText(/2.*členové|2.*members|členů: 2/i)).toBeVisible();
  });
});
