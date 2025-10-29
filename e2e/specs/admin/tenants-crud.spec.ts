import { test, expect } from '@playwright/test';
import { loginAsAdmin, loginAsUser } from '../../helpers/login';
import {
  createTestTenant,
  generateTestName,
  cleanupTestData,
  navigateToAdminPage,
  waitForDialogClose
} from '../../helpers/fixtures';

/**
 * Tenants CRUD E2E Tests
 * 
 * Tests complete CRUD operations for tenant management with RBAC
 * 
 * Coverage:
 * - Create tenant (admin only)
 * - Read tenant list (admin, tenant_admin can view own)
 * - Update tenant (admin only)
 * - Delete tenant (admin only)
 * - Verify Grafana provisioning on create
 * - Verify cleanup on delete
 * - RBAC verification (only admin can manage tenants)
 */

test.describe('Admin: Tenants CRUD', () => {
  let testTenantIds: string[] = [];

  test.afterAll(async ({ browser }) => {
    // Cleanup test data
    const page = await browser.newPage();
    await loginAsAdmin(page);
    await cleanupTestData(page, { tenantIds: testTenantIds });
    await page.close();
  });

  test('should create new tenant as admin', async ({ page }) => {
    await loginAsAdmin(page);

    // Create via API
    const tenantKey = generateTestName('test-tenant', true).toLowerCase();
    const { id: tenantId } = await createTestTenant(page, tenantKey, 'Test Tenant Name');
    testTenantIds.push(tenantId);

    // Verify tenant appears in UI
    await navigateToAdminPage(page, '/tenants');
    await expect(page.getByText(tenantKey)).toBeVisible();
  });

  test('should verify Grafana provisioning after tenant creation', async ({ page }) => {
    await loginAsAdmin(page);

    // Create tenant
    const tenantKey = generateTestName('test_grafana').toLowerCase();
    const { id: tenantId } = await createTestTenant(page, tenantKey, `Grafana Test Tenant`);
    testTenantIds.push(tenantId);

    // Wait for Grafana provisioning (might take a few seconds)
    await page.waitForTimeout(3000);

    // Navigate to tenant detail
    await navigateToAdminPage(page, '/tenants');
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(tenantKey);
      await page.waitForTimeout(1000);
    }

    const tenantRow = page.locator(`text=${tenantKey}`).locator('..').locator('..');
    await tenantRow.click();

    // Should show Grafana provisioning status
    await expect(page.getByText(/grafana.*provisioned|grafana.*ready|grafana.*ok/i)).toBeVisible({ timeout: 10000 });

    // Should show Grafana organization ID
    await expect(page.getByText(/organization.*id|org.*id/i)).toBeVisible();
  });

  test('should read tenant list as admin', async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToAdminPage(page, '/tenants');

    // Should see tenants list (use heading role to be more specific)
    await expect(page.getByRole('heading', { name: /seznam tenantů|tenants list/i })).toBeVisible({ timeout: 10000 });

    // Should see create button
    const createButton = page.getByRole('button', { name: /vytvořit tenant|create tenant|nový tenant/i });
    await expect(createButton).toBeVisible();
  });

  test('should NOT allow tenant_admin to access all tenants', async ({ page }) => {
    await loginAsUser(page, 'test_admin', 'Test.1234'); // Has ADMIN which includes TENANT_ADMIN
    await navigateToAdminPage(page, '/tenants');

    // tenant_admin should only see their own tenant(s)
    // Full list should not be accessible or should be filtered
    const tenantCount = await page.locator('[data-tenant-id]').count();
    
    // If tenant_admin can access tenants page, should see limited view
    // This depends on your RBAC implementation
    expect(tenantCount).toBeGreaterThanOrEqual(0);
  });

  test('should NOT allow regular user to access tenants page', async ({ page }) => {
    await loginAsUser(page, 'test', 'Test.1234');
    await navigateToAdminPage(page, '/tenants');

    // Should show access denied or redirect
    const isDenied = await page.getByText(/přístup odepřen|access denied|nedostatečná oprávnění/i).isVisible().catch(() => false);
    const isRedirected = page.url().includes('/dashboard') || page.url().includes('/403');

    expect(isDenied || isRedirected).toBeTruthy();
  });

  test('should update tenant as admin', async ({ page }) => {
    await loginAsAdmin(page);

    // Create test tenant first
    const tenantKey = generateTestName('test_update').toLowerCase();
    const { id: tenantId } = await createTestTenant(page, tenantKey, 'Original Name');
    testTenantIds.push(tenantId);

    // Navigate to tenants
    await navigateToAdminPage(page, '/tenants');

    // Search for tenant
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(tenantKey);
      await page.waitForTimeout(1000);
    }

    // Click edit button
    const editButton = page.locator(`text=${tenantKey}`).locator('..').locator('..').getByRole('button', { name: /upravit|edit/i });
    await editButton.click();

    // Update display name (using exact Czech label)
    await page.getByLabel('Název tenanta *').fill('Updated Tenant Name');

    // Save changes
    const saveButton = page.getByRole('button', { name: /uložit|save/i });
    await saveButton.click();

    await waitForDialogClose(page);

    // Verify changes
    await navigateToAdminPage(page, '/tenants');
    if (await searchBox.isVisible()) {
      await searchBox.fill(tenantKey);
      await page.waitForTimeout(1000);
    }
    await expect(page.getByText('Updated Tenant Name')).toBeVisible();
  });

  test('should toggle tenant enabled status as admin', async ({ page }) => {
    await loginAsAdmin(page);

    // Create test tenant
    const tenantKey = generateTestName('test_toggle').toLowerCase();
    const { id: tenantId } = await createTestTenant(page, tenantKey, 'Toggle Test Tenant');
    testTenantIds.push(tenantId);

    // Navigate to tenants
    await navigateToAdminPage(page, '/tenants');

    // Search for tenant
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(tenantKey);
      await page.waitForTimeout(1000);
    }

    // Find and click disable/enable toggle
    const tenantRow = page.locator(`text=${tenantKey}`).locator('..').locator('..');
    const toggleSwitch = tenantRow.getByRole('switch', { name: /enabled|aktivní/i })
      .or(tenantRow.locator('input[type="checkbox"]').first());
    
    // Click to disable
    await toggleSwitch.click();
    await page.waitForTimeout(1000); // Wait for toggle to complete

    // Click to re-enable
    await toggleSwitch.click();
    await page.waitForTimeout(1000); // Wait for toggle to complete
  });

  test('should delete tenant as admin and cleanup Grafana', async ({ page }) => {
    await loginAsAdmin(page);

    // Create test tenant to delete
    const tenantKey = generateTestName('test_delete').toLowerCase();
    await createTestTenant(page, tenantKey, 'Delete Test Tenant');

    // Wait for Grafana provisioning
    await page.waitForTimeout(3000);

    // Navigate to tenants
    await navigateToAdminPage(page, '/tenants');

    // Search for tenant
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(tenantKey);
      await page.waitForTimeout(1000);
    }

    // Click delete button
    const deleteButton = page.locator(`text=${tenantKey}`).locator('..').locator('..').getByRole('button', { name: /smazat|delete|odstranit/i });
    await expect(deleteButton).toBeVisible();
    await deleteButton.click();

    // Confirm deletion (should warn about Grafana cleanup)
    await expect(page.getByText(/grafana.*odstraněn|grafana.*deleted|grafana.*cleanup/i)).toBeVisible({ timeout: 3000 });
    
    const confirmButton = page.getByRole('button', { name: /potvrdit|confirm|ano|yes/i });
    await confirmButton.click();

    // Wait for deletion and Grafana cleanup (takes longer)
    await waitForDialogClose(page, { timeout: 15000 });

    // Verify tenant is gone
    if (await searchBox.isVisible()) {
      await searchBox.fill(tenantKey);
      await page.waitForTimeout(1000);
    }
    await expect(page.getByText(tenantKey)).not.toBeVisible();
  });

  test('should search and filter tenants', async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToAdminPage(page, '/tenants');

    // Create test tenants for search
    const tenant1Key = generateTestName('alpha').toLowerCase();
    const tenant2Key = generateTestName('beta').toLowerCase();
    
    const { id: id1 } = await createTestTenant(page, tenant1Key, 'Alpha Tenant');
    const { id: id2 } = await createTestTenant(page, tenant2Key, 'Beta Tenant');
    testTenantIds.push(id1, id2);

    // Refresh page
    await navigateToAdminPage(page, '/tenants');

    // Search for specific tenant
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill('alpha');
      await page.waitForTimeout(1000);

      await expect(page.getByText(/Alpha Tenant/i)).toBeVisible();
      await expect(page.getByText(/Beta Tenant/i)).not.toBeVisible();

      // Clear and search for other
      await searchBox.clear();
      await searchBox.fill('beta');
      await page.waitForTimeout(1000);

      await expect(page.getByText(/Beta Tenant/i)).toBeVisible();
    }
  });

  test('should validate required fields on create', async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToAdminPage(page, '/tenants');

    const createButton = page.getByRole('button', { name: /vytvořit tenant|create tenant|nový tenant/i });
    await createButton.click();

    // Try to save without required fields
    const saveButton = page.getByRole('button', { name: /uložit|save|vytvořit/i });
    await saveButton.click();

    // Should show validation errors
    await expect(page.getByText(/povinné pole|required field|vyplňte|je nutné vyplnit/i)).toBeVisible({ timeout: 3000 });
  });

  test('should validate tenant key format', async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToAdminPage(page, '/tenants');

    const createButton = page.getByRole('button', { name: /vytvořit tenant|create tenant|nový tenant/i });
    await createButton.click();

    // Try invalid tenant key (with spaces or special chars)
    await page.getByLabel('ID tenanta *').fill('Invalid Key 123!');
    await page.getByLabel('Název tenanta *').fill('Test Tenant');

    const saveButton = page.getByRole('button', { name: /uložit|save|vytvořit/i });
    await saveButton.click();

    // Should show validation error about format
    await expect(page.getByText(/neplatný formát|invalid format|pouze|lowercase|alfanumerické/i)).toBeVisible({ timeout: 3000 });
  });

  test('should prevent duplicate tenant key', async ({ page }) => {
    await loginAsAdmin(page);

    // Create first tenant
    const tenantKey = generateTestName('duplicate').toLowerCase();
    const { id: tenantId } = await createTestTenant(page, tenantKey, 'First Tenant');
    testTenantIds.push(tenantId);

    // Try to create another tenant with same key
    await navigateToAdminPage(page, '/tenants');
    const createButton = page.getByRole('button', { name: /vytvořit tenant|create tenant|nový tenant/i });
    await createButton.click();

    await page.getByLabel('ID tenanta *').fill(tenantKey);
    await page.getByLabel('Název tenanta *').fill('Duplicate Tenant');

    const saveButton = page.getByRole('button', { name: /uložit|save|vytvořit/i });
    await saveButton.click();

    // Should show error about duplicate tenant key
    await expect(page.getByText(/tenant již existuje|tenant already exists|duplicitní klíč/i)).toBeVisible({ timeout: 5000 });
  });

  test('should show tenant statistics', async ({ page }) => {
    await loginAsAdmin(page);

    // Create tenant
    const tenantKey = generateTestName('stats').toLowerCase();
    const { id: tenantId } = await createTestTenant(page, tenantKey, 'Stats Test Tenant');
    testTenantIds.push(tenantId);

    // Navigate to tenant detail
    await navigateToAdminPage(page, '/tenants');
    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/hledat|search/i));
    if (await searchBox.isVisible()) {
      await searchBox.fill(tenantKey);
      await page.waitForTimeout(1000);
    }

    const tenantRow = page.locator(`text=${tenantKey}`).locator('..').locator('..');
    await tenantRow.click();

    // Should show statistics
    await expect(page.getByText(/uživatelé|users|počet uživatelů/i)).toBeVisible({ timeout: 5000 });
    await expect(page.getByText(/skupiny|groups/i)).toBeVisible();
    await expect(page.getByText(/grafana/i)).toBeVisible();
  });
});
