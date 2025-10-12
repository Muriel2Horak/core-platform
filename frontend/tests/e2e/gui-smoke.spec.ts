/**
 * GUI Smoke Test
 * 
 * Ověřuje základní funkčnost UI:
 * - Login flow
 * - Menu rendering podle RBAC
 * - Grid zobrazení entity
 * - Otevření detailu/popupu
 */

import { test, expect } from '@playwright/test';
import { loginViaKeycloak } from './helpers/login.js';
import { readE2EConfig } from '../../../e2e/config/read-config.js';

const e2eConfig = readE2EConfig();

test.describe('GUI Smoke Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Login před každým testem
    await loginViaKeycloak(page);
  });

  test('should display dashboard after login', async ({ page }) => {
    // Ověříme, že jsme na dashboardu
    await expect(page).toHaveURL(new RegExp(`${e2eConfig.baseUrl}(?:/dashboard)?`));
    
    // Ověříme základní UI prvky
    const navigation = page.locator('nav, [role="navigation"]');
    await expect(navigation).toBeVisible();
  });

  test('should display menu items based on RBAC', async ({ page }) => {
    // Počkáme na načtení menu
    const navigation = page.locator('nav, [role="navigation"]');
    await expect(navigation).toBeVisible();
    
    // Ověříme přítomnost základních menu položek
    // (Konkrétní položky závisí na roli test usera)
    const menuItems = page.locator('nav a, nav button, [role="navigation"] a, [role="navigation"] button');
    const count = await menuItems.count();
    
    expect(count).toBeGreaterThan(0);
    console.log(`✅ Found ${count} menu items`);
    
    // Logujeme nalezené menu items pro debug
    for (let i = 0; i < Math.min(count, 10); i++) {
      const text = await menuItems.nth(i).textContent();
      console.log(`  - Menu item ${i + 1}: ${text?.trim()}`);
    }
  });

  test('should open entity grid (Customers)', async ({ page }) => {
    // Pokusíme se najít a otevřít entitu "Customers" nebo jiný běžný typ
    // Toto je generické - přizpůsobte podle vaší UI struktury
    
    // Možnost 1: přímá navigace na /customers (nebo podobnou cestu)
    await page.goto(`${e2eConfig.baseUrl}/customers`);
    
    // Možnost 2: Kliknout na menu položku
    // const customersLink = page.getByRole('link', { name: /customers|zákazníci/i });
    // if (await customersLink.isVisible({ timeout: 2000 })) {
    //   await customersLink.click();
    // }
    
    // Ověříme, že se zobrazil grid/table
    const gridContainer = page.locator('[role="grid"], table, .ag-grid, .MuiDataGrid-root');
    
    // Počkáme až se grid načte (může trvat i několik sekund kvůli API)
    await expect(gridContainer).toBeVisible({ timeout: 10000 });
    
    console.log('✅ Grid/table rendered');
  });

  test('should open entity detail/popup', async ({ page }) => {
    // Navigace na grid
    await page.goto(`${e2eConfig.baseUrl}/customers`);
    
    // Počkáme na grid
    const gridContainer = page.locator('[role="grid"], table, .ag-grid, .MuiDataGrid-root');
    await expect(gridContainer).toBeVisible({ timeout: 10000 });
    
    // Klikneme na první řádek nebo tlačítko "Detail"
    // Přizpůsobte selektory podle vaší UI implementace
    const firstRow = page.locator('[role="row"]:not([role="row"]:has-text("Column"))').first();
    
    if (await firstRow.isVisible({ timeout: 2000 })) {
      await firstRow.click();
      
      // Ověříme, že se otevřel dialog/detail
      const dialog = page.locator('[role="dialog"], .MuiDialog-root, .modal');
      await expect(dialog).toBeVisible({ timeout: 5000 });
      
      console.log('✅ Detail/popup opened');
    } else {
      console.warn('⚠️  No rows found in grid, skipping detail test');
    }
  });
});
