import { test, expect } from '@playwright/test';

test('Debug Grafana SSO - Network Capture', async ({ page }) => {
  // Enable request logging
  page.on('request', request => {
    const url = request.url();
    if (url.includes('auth/grafana') || url.includes('monitoring') || url.includes('grafana')) {
      console.log('→ REQUEST:', request.method(), url);
      console.log('  Headers:', JSON.stringify(request.headers(), null, 2));
    }
  });

  page.on('response', async response => {
    const url = response.url();
    if (url.includes('auth/grafana') || url.includes('monitoring') || url.includes('grafana')) {
      console.log('← RESPONSE:', response.status(), url);
      console.log('  Headers:', JSON.stringify(response.headers(), null, 2));
      
      // Log response body for auth endpoint
      if (url.includes('auth/grafana')) {
        try {
          const body = await response.text();
          console.log('  Body:', body.substring(0, 500));
        } catch (e) {
          console.log('  Body: (binary or error)');
        }
      }
    }
  });

  console.log('\n🔐 Step 1: Navigate to login page');
  await page.goto('https://admin.core-platform.local/login');
  await page.waitForLoadState('networkidle');

  console.log('\n🔐 Step 2: Login as test_admin');
  await page.fill('input[name="username"]', 'test_admin');
  await page.fill('input[name="password"]', 'Test.1234');
  await page.click('button[type="submit"]');
  
  console.log('\n⏳ Waiting for redirect...');
  await page.waitForURL(/dashboard|admin/, { timeout: 10000 });
  console.log('✅ Logged in, current URL:', page.url());

  // Check cookies
  const cookies = await page.context().cookies();
  console.log('\n🍪 Cookies after login:');
  cookies.forEach(c => {
    if (c.name === 'at' || c.name === 'rt') {
      console.log(`  ${c.name}: ${c.value.substring(0, 30)}... (httpOnly: ${c.httpOnly})`);
    }
  });

  console.log('\n📊 Step 3: Navigate to Grafana monitoring page');
  await page.goto('https://admin.core-platform.local/core-admin/monitoring/');
  
  console.log('\n⏳ Waiting 5 seconds for page load...');
  await page.waitForTimeout(5000);

  console.log('\n📸 Taking screenshot...');
  await page.screenshot({ path: '/tmp/grafana-sso-debug.png', fullPage: true });

  // Check for login form
  const loginForm = await page.locator('input[name="user"]').count();
  const loginButton = await page.locator('button:has-text("Log in")').count();
  
  console.log('\n🔍 Page analysis:');
  console.log('  Login form present:', loginForm > 0);
  console.log('  Login button present:', loginButton > 0);
  console.log('  Current URL:', page.url());
  console.log('  Page title:', await page.title());

  // Get page content
  const bodyText = await page.locator('body').textContent();
  console.log('  Body text (first 200 chars):', bodyText?.substring(0, 200));

  if (loginForm > 0) {
    console.log('\n❌ FAIL: Grafana shows login form - SSO not working!');
  } else {
    console.log('\n✅ SUCCESS: No login form detected');
  }
});
