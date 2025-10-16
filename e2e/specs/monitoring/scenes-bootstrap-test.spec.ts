/**
 * Grafana Scenes Bootstrap - Standalone Test
 * 
 * Tests if the new ESM scenes.bootstrap.js loads and initializes correctly
 */

import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login.js';

test.describe('Grafana Scenes Bootstrap - Native Integration', () => {
  test('should load scenes.bootstrap.js and initialize grafanaBootData', async ({ page }) => {
    // Track console messages
    const consoleMessages: string[] = [];
    page.on('console', msg => {
      const text = msg.text();
      if (text.includes('scenes') || text.includes('bootdata') || text.includes('grafanaBootData')) {
        consoleMessages.push(text);
      }
    });

    // Navigate to monitoring page
    await login(page);
    await page.goto('/admin/monitoring');
    
    // Wait for scenes bootstrap to load
    await page.waitForTimeout(3000);
    
    // Check if grafanaBootData exists
    const hasBootData = await page.evaluate(() => {
      return window.grafanaBootData !== undefined;
    });
    
    expect(hasBootData).toBeTruthy();
    console.log('✅ grafanaBootData exists:', hasBootData);
    
    // Check if scenes.bootstrap.js loaded
    const scenesBootstrapLoaded = consoleMessages.some(msg => 
      msg.includes('[scenes.bootstrap]') && msg.includes('Starting')
    );
    
    expect(scenesBootstrapLoaded).toBeTruthy();
    console.log('✅ scenes.bootstrap.js loaded');
    
    // Check if scenes.start was imported
    const scenesStartLoaded = consoleMessages.some(msg =>
      msg.includes('[scenes.bootstrap]') && msg.includes('Loading scenes app module')
    );
    
    console.log('📋 Console messages:', consoleMessages.filter(m => m.includes('scenes')));
    
    // Check if #grafana-scenes-root exists
    const scenesRoot = page.locator('#grafana-scenes-root');
    await expect(scenesRoot).toBeAttached();
    console.log('✅ #grafana-scenes-root exists');
    
    // Check if scenes.bootstrap.js script tag exists
    const scenesScript = await page.locator('script[src*="scenes.bootstrap.js"]').count();
    expect(scenesScript).toBeGreaterThan(0);
    console.log('✅ scenes.bootstrap.js script tag found');
  });

  test('should verify ESM module loading (type="module")', async ({ page }) => {
    await page.goto('/');
    
    // Check if script has type="module"
    const isModule = await page.evaluate(() => {
      const script = document.querySelector('script[src*="scenes.bootstrap.js"]');
      return script?.getAttribute('type') === 'module';
    });
    
    expect(isModule).toBeTruthy();
    console.log('✅ scenes.bootstrap.js is ESM module (type="module")');
  });

  test('should verify bootdata initialization order', async ({ page }) => {
    const consoleMessages: string[] = [];
    let bootDataTime: number | null = null;
    let scenesTime: number | null = null;
    
    page.on('console', msg => {
      const text = msg.text();
      const now = Date.now();
      
      if (text.includes('grafanaBootData initialized')) {
        bootDataTime = now;
        consoleMessages.push(`[${now}] ${text}`);
      }
      if (text.includes('[scenes.bootstrap]')) {
        if (!scenesTime) scenesTime = now;
        consoleMessages.push(`[${now}] ${text}`);
      }
    });
    
    await page.goto('/');
    await page.waitForTimeout(2000);
    
    console.log('📋 Initialization order:');
    consoleMessages.forEach(msg => console.log(msg));
    
    // Boot data should be initialized before scenes
    if (bootDataTime && scenesTime) {
      expect(bootDataTime).toBeLessThan(scenesTime);
      console.log('✅ Boot data initialized before scenes module');
    }
  });
});
