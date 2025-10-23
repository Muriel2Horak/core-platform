/**
 * 🧪 Quick test: Verify JWKS HTTPS proxy fix resolved 502 error
 * 
 * Tests:
 * 1. Dashboard loads without 502 error
 * 2. JWKS endpoint returns valid JSON via HTTPS
 * 3. Grafana iframe loads successfully
 */

import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login';

test.describe('JWKS HTTPS Proxy - 502 Fix Verification', () => {
  
  test('should load dashboard without 502 error', async ({ page }) => {
    console.log('\n🧪 Testing JWKS HTTPS proxy fix...\n');
    
    // Track 502 errors
    let has502Error = false;
    page.on('response', response => {
      if (response.status() === 502) {
        has502Error = true;
        console.log(`❌ 502 Error: ${response.url()}`);
      }
    });

    // Login
    console.log('1️⃣ Logging in...');
    await login(page);
    console.log('✅ Login successful\n');

    // Navigate to Monitoring
    console.log('2️⃣ Navigating to monitoring...');
    await page.goto('/core-admin/monitoring');
    await page.waitForLoadState('networkidle', { timeout: 10000 });
    console.log('✅ Page loaded\n');

    // Check for 502 errors
    console.log('3️⃣ Checking for 502 errors...');
    expect(has502Error).toBe(false);
    console.log('✅ No 502 errors!\n');

    // Check if Grafana iframe is present
    console.log('4️⃣ Checking for Grafana embed...');
    const grafanaFrame = page.frameLocator('iframe[src*="/core-admin/monitoring"]');
    await expect(grafanaFrame.locator('body')).toBeVisible({ timeout: 10000 });
    console.log('✅ Grafana iframe loaded successfully!\n');

    console.log('🎉 JWKS HTTPS proxy fix verified - 502 error resolved!\n');
  });

  test('should verify JWKS endpoint returns valid JSON via HTTPS', async ({ request }) => {
    console.log('\n🧪 Testing JWKS endpoint...\n');
    
    // Test JWKS endpoint
    const response = await request.get('https://admin.core-platform.local/.well-known/jwks.json', {
      ignoreHTTPSErrors: true,
    });

    expect(response.ok()).toBeTruthy();
    const jwks = await response.json();
    
    // Verify JWKS structure
    expect(jwks).toHaveProperty('keys');
    expect(Array.isArray(jwks.keys)).toBe(true);
    expect(jwks.keys.length).toBeGreaterThan(0);
    
    const key = jwks.keys[0];
    expect(key).toHaveProperty('kty', 'RSA');
    expect(key).toHaveProperty('kid', 'grafana-bff-key-1');
    expect(key).toHaveProperty('n');
    expect(key).toHaveProperty('e', 'AQAB');
    
    console.log('✅ JWKS endpoint returns valid RSA key via HTTPS');
    console.log(`   kid: ${key.kid}`);
    console.log(`   kty: ${key.kty}`);
    console.log(`   e: ${key.e}\n`);
  });
});
