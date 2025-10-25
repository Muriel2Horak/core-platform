/**
 * DEBUG TEST - Grafana SSO Flow
 * 
 * Test naviguje na /core-admin/monitoring/ a zachytává:
 * - Všechny HTTP requesty/responses
 * - Cookies
 * - Backend provisioning logy
 * - Screenshot výsledku
 */

import { test } from '@playwright/test';
import { login } from '../../helpers/login';

test('Debug Grafana SSO - Full Flow', async ({ page }) => {
  console.log('\n🔍 GRAFANA SSO DEBUG TEST');
  console.log('═══════════════════════════════════════════════════\n');
  
  // Zachytávání requestů
  const requests: any[] = [];
  page.on('request', request => {
    const url = request.url();
    if (url.includes('auth') || url.includes('monitoring') || url.includes('grafana') || url.includes('jwks')) {
      requests.push({
        type: 'REQUEST',
        method: request.method(),
        url: url,
        headers: request.headers()
      });
      console.log(`→ ${request.method()} ${url}`);
    }
  });

  page.on('response', async response => {
    const url = response.url();
    if (url.includes('auth') || url.includes('monitoring') || url.includes('grafana') || url.includes('jwks')) {
      const headers = response.headers();
      console.log(`← ${response.status()} ${url}`);
      
      // Speciální log pro /_auth/grafana endpoint
      if (url.includes('/_auth/grafana')) {
        console.log('  📋 Auth Response Headers:', JSON.stringify(headers, null, 2));
        
        try {
          const body = await response.text();
          console.log('  📄 Auth Response Body:', body.substring(0, 300));
        } catch (e) {
          console.log('  📄 Auth Response Body: (empty or binary)');
        }
      }
      
      requests.push({
        type: 'RESPONSE',
        status: response.status(),
        url: url,
        headers: headers
      });
    }
  });

  // STEP 1: Login
  console.log('🔐 STEP 1: Login as test_admin');
  await login(page, { username: 'test_admin', password: 'Test.1234', waitForDashboard: true });
  console.log('✅ Login complete\n');

  // Zkontroluj cookies
  const cookies = await page.context().cookies();
  console.log('🍪 Cookies after login:');
  cookies.forEach(c => {
    if (c.name === 'at' || c.name === 'rt') {
      console.log(`  ${c.name}: ${c.value.substring(0, 40)}...`);
      console.log(`    httpOnly: ${c.httpOnly}, secure: ${c.secure}, domain: ${c.domain}`);
    }
  });
  console.log('');

  // STEP 2: Navigate to Grafana
  console.log('📊 STEP 2: Navigate to /core-admin/monitoring/');
  await page.goto('https://admin.core-platform.local/core-admin/monitoring/');
  
  console.log('⏳ Waiting 5 seconds for page load...');
  await page.waitForTimeout(5000);

  // STEP 3: Analyze page
  console.log('\n🔍 STEP 3: Page Analysis');
  const pageUrl = page.url();
  const pageTitle = await page.title();
  console.log(`  Current URL: ${pageUrl}`);
  console.log(`  Page Title: ${pageTitle}`);

  // Check for Grafana login form
  const loginFormCount = await page.locator('input[name="user"]').count();
  const loginButtonCount = await page.locator('button:has-text("Log in")').count();
  const grafanaLogoCount = await page.locator('[aria-label="Grafana"]').count();
  
  console.log(`  Login form (input[name="user"]): ${loginFormCount}`);
  console.log(`  Login button: ${loginButtonCount}`);
  console.log(`  Grafana logo: ${grafanaLogoCount}`);

  if (loginFormCount > 0) {
    console.log('\n❌ PROBLEM: Grafana shows LOGIN FORM - SSO FAILED!');
  } else {
    console.log('\n✅ SUCCESS: No login form detected');
  }

  // STEP 4: Screenshot
  console.log('\n📸 STEP 4: Taking screenshot');
  await page.screenshot({ path: '/tmp/grafana-sso-debug.png', fullPage: true });
  console.log('  Screenshot saved: /tmp/grafana-sso-debug.png');

  // STEP 5: Summary
  console.log('\n📋 STEP 5: Request Summary');
  const authRequests = requests.filter(r => r.url.includes('/_auth/grafana'));
  console.log(`  Total auth requests: ${authRequests.length}`);
  
  authRequests.forEach((r, i) => {
    console.log(`\n  Auth Request #${i + 1}:`);
    console.log(`    Type: ${r.type}`);
    console.log(`    Method: ${r.method || 'N/A'}`);
    console.log(`    Status: ${r.status || 'N/A'}`);
    if (r.headers && r.headers['grafana-jwt']) {
      console.log(`    ✅ Has Grafana-JWT header`);
    }
    if (r.headers && r.headers['x-grafana-org-id']) {
      console.log(`    ✅ Has X-Grafana-Org-Id: ${r.headers['x-grafana-org-id']}`);
    }
  });

  console.log('\n═══════════════════════════════════════════════════');
  console.log('🏁 DEBUG TEST COMPLETE');
  console.log('═══════════════════════════════════════════════════\n');
});
