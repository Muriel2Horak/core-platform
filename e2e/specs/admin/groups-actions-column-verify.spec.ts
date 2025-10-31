import { test, expect } from '@playwright/test';
import { loginAsAdmin } from '../../helpers/login';

/**
 * QUICK TEST: Verify Groups Actions Column Deployment
 * 
 * Purpose: Verify that conditional actions column from commit 2f500ef is deployed
 * This is a PURE UI test - no fixtures, no setup, just verify UI rendering
 */

test.describe('Groups: Actions Column Verification', () => {
  
  test('should show actions column with MoreVertIcon for admin', async ({ page }) => {
    // Login as admin
    await loginAsAdmin(page);
    
    // Navigate to groups page
    await page.goto('/core-admin/groups');
    
    // Wait for page to load
    await page.waitForSelector('table', { timeout: 15000 });
    
    // Create a test group if table is empty
    const noDataMessage = await page.locator('text=Žádná data k zobrazení').isVisible().catch(() => false);
    if (noDataMessage) {
      console.log('📝 Creating test group for verification...');
      await page.click('button:has-text("Vytvořit skupinu")');
      await page.fill('input[name="name"]', 'test-verify-group');
      await page.click('button:has-text("Vytvořit")');
      await page.waitForTimeout(2000); // Wait for creation
    }
    
    // Wait for data to load (table rows)
    await page.waitForTimeout(1000); // API refresh
    
    // 🎯 KEY VERIFICATION: Find ANY group row with MoreVertIcon button
    // (conditional actions column should render for users with canManageGroups)
    const actionButton = page.locator('button:has(svg[data-testid="MoreVertIcon"])').first();
    
    // This will FAIL if frontend has old code (no conditional actions column)
    // This will PASS if frontend has new code from commit 2f500ef
    await expect(actionButton).toBeVisible({ timeout: 10000 });
    
    console.log('✅ Actions column with MoreVertIcon is visible - new code deployed!');
  });
  
  test('should open menu when clicking MoreVertIcon', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/core-admin/groups');
    
    // Wait for table
    await page.waitForSelector('table', { timeout: 15000 });
    await page.waitForTimeout(3000);
    
    // Find and click menu button
    const menuButton = page.locator('button:has(svg[data-testid="MoreVertIcon"])').first();
    await menuButton.waitFor({ state: 'visible', timeout: 10000 });
    await menuButton.click();
    
    // Verify menu opens with expected options
    await expect(page.getByRole('menuitem', { name: /upravit|edit/i })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: /smazat|delete/i })).toBeVisible();
    
    console.log('✅ Menu opens correctly with Edit and Delete options');
  });
  
});
