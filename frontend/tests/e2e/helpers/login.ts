/**
 * Login Helper pro Keycloak Authentication
 * 
 * Provádí login flow přes Keycloak UI.
 * Podporuje storage state pro session persistence.
 */

import { Page, expect } from '@playwright/test';
import { readE2EConfig } from '../../../../e2e/config/read-config.js';
import { readFileSync, existsSync, mkdirSync, unlinkSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const STORAGE_STATE_PATH = join(__dirname, '../.auth/state.json');

/**
 * Perform login via Keycloak UI
 */
export async function loginViaKeycloak(page: Page): Promise<void> {
  const e2eConfig = readE2EConfig();
  const { baseUrl, testUser } = e2eConfig;
  
  console.log(`🔐 Logging in as ${testUser.username} to ${baseUrl}`);
  
  // Navigace na hlavní stránku
  await page.goto(baseUrl);
  
  // Čekáme na přesměrování na Keycloak nebo na tlačítko Login
  // (záleží na tom, jestli UI má landing page nebo automatický redirect)
  try {
    // Pokus 1: přímý redirect na Keycloak
    await page.waitForURL('**/realms/**/protocol/openid-connect/auth**', { timeout: 5000 });
  } catch {
    // Pokus 2: kliknout na Login button
    const loginButton = page.getByRole('button', { name: /login|přihlásit/i });
    if (await loginButton.isVisible({ timeout: 2000 })) {
      await loginButton.click();
      await page.waitForURL('**/realms/**/protocol/openid-connect/auth**');
    }
  }
  
  // Fill Keycloak login form
  await page.fill('input[name="username"], input#username', testUser.username);
  await page.fill('input[name="password"], input#password', testUser.password);
  
  // Submit
  await page.click('input[type="submit"], button[type="submit"]');
  
  // Čekáme na redirect zpět do aplikace
  await page.waitForURL(`${baseUrl}/**`, { timeout: 15000 });
  
  // Ověříme, že jsme přihlášení (např. kontrola přítomnosti user menu)
  // Toto je generické - konkrétní testy mohou ověřit specifické prvky
  await expect(page).not.toHaveURL(/\/realms\//);
  
  console.log('✅ Login successful');
}

/**
 * Login a uložení session do storage state
 */
export async function loginAndSaveState(page: Page): Promise<void> {
  await loginViaKeycloak(page);
  
  // Uložíme storage state
  const storageDir = dirname(STORAGE_STATE_PATH);
  if (!existsSync(storageDir)) {
    mkdirSync(storageDir, { recursive: true });
  }
  
  await page.context().storageState({ path: STORAGE_STATE_PATH });
  console.log(`💾 Session saved to ${STORAGE_STATE_PATH}`);
}

/**
 * Zkontroluje, zda existuje uložený storage state
 */
export function hasStoredSession(): boolean {
  return existsSync(STORAGE_STATE_PATH);
}

/**
 * Získá cestu k uloženému storage state
 */
export function getStorageStatePath(): string | undefined {
  return hasStoredSession() ? STORAGE_STATE_PATH : undefined;
}

/**
 * Smaže uložený storage state
 */
export function clearStoredSession(): void {
  if (existsSync(STORAGE_STATE_PATH)) {
    unlinkSync(STORAGE_STATE_PATH);
    console.log('🗑️  Stored session cleared');
  }
}
