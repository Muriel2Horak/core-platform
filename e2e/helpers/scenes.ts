/**
 * Grafana Scenes E2E Helper
 * 
 * Utilities for waiting on native Grafana Scenes integration
 */

import { Page, expect } from '@playwright/test';

/**
 * Waits for Grafana Scenes container to be visible and ready
 * 
 * @param page - Playwright page instance
 * @param timeout - Maximum wait time in milliseconds (default: 15000)
 */
export async function waitForScenesReady(page: Page, timeout: number = 15000): Promise<void> {
  console.log('⏳ Waiting for Grafana Scenes to be ready...');
  
  // Wait for scenes root container to be visible
  const scenesRoot = page.locator('#grafana-scenes-root');
  await expect(scenesRoot).toBeVisible({ timeout });
  console.log('✓ Scenes root container visible');
  
  // Wait for scene content to appear (look for common Grafana UI elements)
  // Try multiple possible indicators
  const sceneContent = page.locator('#grafana-scenes-root [class*="scene"]').first();
  try {
    await expect(sceneContent).toBeVisible({ timeout: 5000 });
    console.log('✓ Scene content rendered');
  } catch {
    console.warn('⚠️  Scene content not detected, but container is visible');
  }
  
  console.log('✅ Grafana Scenes ready');
}

/**
 * Waits for a specific scene panel or text to be visible
 * 
 * @param page - Playwright page instance
 * @param textOrSelector - Text content or CSS selector to wait for
 * @param timeout - Maximum wait time in milliseconds (default: 15000)
 */
export async function waitForSceneContent(
  page: Page, 
  textOrSelector: string, 
  timeout: number = 15000
): Promise<void> {
  console.log(`⏳ Waiting for scene content: ${textOrSelector}...`);
  
  // First ensure scenes root is visible
  await waitForScenesReady(page, timeout);
  
  // Then wait for specific content
  const contentLocator = textOrSelector.startsWith('.') || textOrSelector.startsWith('#')
    ? page.locator(textOrSelector)
    : page.getByText(new RegExp(textOrSelector, 'i'));
  
  await expect(contentLocator).toBeVisible({ timeout });
  console.log(`✓ Scene content visible: ${textOrSelector}`);
}

/**
 * Checks console for Grafana boot errors
 * 
 * @param page - Playwright page instance
 * @returns Array of boot-related error messages
 */
export async function checkBootErrors(page: Page): Promise<string[]> {
  const errors: string[] = [];
  
  page.on('console', (msg) => {
    if (msg.type() === 'error' && msg.text().includes('grafanaBootData')) {
      errors.push(msg.text());
    }
  });
  
  return errors;
}
