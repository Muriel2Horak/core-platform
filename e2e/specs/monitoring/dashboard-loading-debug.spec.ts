/**
 * 🐛 DEBUG Test: Grafana Dashboard Loading Issues
 * 
 * Purpose: Investigate why dashboards show only loading spinner
 * 
 * Checks:
 * 1. Component mounting
 * 2. Scene initialization errors
 * 3. BFF API responses
 * 4. Console errors
 * 5. Network requests
 */

import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login';

test.describe('Dashboard Loading Debug', () => {
  
  test('should debug Monitoring page loading', async ({ page }) => {
    console.log('\n🐛 DEBUG: Investigating Monitoring page loading issue\n');
    
    // Track console errors
    const consoleErrors: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
        console.log('❌ Console Error:', msg.text());
      } else if (msg.type() === 'warning') {
        console.log('⚠️  Console Warning:', msg.text());
      }
    });

    // Track failed network requests
    const failedRequests: Array<{ url: string; status: number }> = [];
    page.on('response', response => {
      if (!response.ok()) {
        failedRequests.push({
          url: response.url(),
          status: response.status(),
        });
        console.log(`❌ Failed request: ${response.status()} ${response.url()}`);
      }
    });

    // Login
    console.log('1️⃣ Logging in...');
    await login(page);
    console.log('✅ Login successful\n');

    // Navigate to Monitoring
    console.log('2️⃣ Navigating to /admin/monitoring...');
    await page.goto('/admin/monitoring', { waitUntil: 'domcontentloaded' });
    console.log('✅ Page loaded\n');

    // Wait a bit for React to render
    await page.waitForTimeout(2000);

    // Check if loading spinner is present
    console.log('3️⃣ Checking for loading spinner...');
    const hasLoadingSpinner = await page.locator('[role="progressbar"], .MuiCircularProgress-root').isVisible();
    console.log(hasLoadingSpinner ? '⚠️  Loading spinner is visible' : '✅ No loading spinner\n');

    // Check if Scene container mounted
    console.log('4️⃣ Checking for Scene components...');
    const sceneContainer = page.locator('[ref], [data-testid*="scene"], div[class*="scene"]').first();
    const sceneExists = await sceneContainer.count() > 0;
    console.log(sceneExists ? '✅ Scene container found' : '❌ Scene container NOT found\n');

    // Check for error messages
    console.log('5️⃣ Checking for error messages...');
    const errorAlert = page.locator('.MuiAlert-standardError, [role="alert"]');
    const hasError = await errorAlert.count() > 0;
    if (hasError) {
      const errorText = await errorAlert.first().textContent();
      console.log(`❌ Error message found: ${errorText}\n`);
    } else {
      console.log('✅ No error alerts\n');
    }

    // Check if tabs are present
    console.log('6️⃣ Checking for tabs...');
    const tabs = page.locator('[role="tab"]');
    const tabCount = await tabs.count();
    console.log(`📊 Found ${tabCount} tabs\n`);

    // Get tab labels
    if (tabCount > 0) {
      for (let i = 0; i < tabCount; i++) {
        const tabText = await tabs.nth(i).textContent();
        console.log(`   Tab ${i + 1}: ${tabText}`);
      }
      console.log('');
    }

    // Check page title
    console.log('7️⃣ Checking page title...');
    const title = await page.locator('h4').first().textContent();
    console.log(`📄 Page title: ${title}\n`);

    // Wait for potential scene activation
    console.log('8️⃣ Waiting 5s for scene activation...');
    await page.waitForTimeout(5000);

    // Re-check loading spinner
    const stillLoading = await page.locator('[role="progressbar"], .MuiCircularProgress-root').isVisible();
    console.log(stillLoading ? '❌ STILL LOADING after 5s!' : '✅ Loading completed\n');

    // Check for BFF API calls
    console.log('9️⃣ Checking BFF API calls...');
    const bffCalls = failedRequests.filter(r => r.url.includes('/api/monitoring/'));
    if (bffCalls.length > 0) {
      console.log('❌ Failed BFF calls:');
      bffCalls.forEach(call => console.log(`   ${call.status} ${call.url}`));
    } else {
      console.log('✅ No failed BFF calls (or no BFF calls at all)\n');
    }

    // Take screenshot
    console.log('🔟 Taking debug screenshot...');
    await page.screenshot({ 
      path: 'e2e/test-results/monitoring-page-debug.png', 
      fullPage: true 
    });
    console.log('✅ Screenshot saved: test-results/monitoring-page-debug.png\n');

    // Summary
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📋 DEBUG SUMMARY');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`Scene container: ${sceneExists ? '✅' : '❌'}`);
    console.log(`Loading spinner: ${stillLoading ? '❌ STUCK' : '✅'}`);
    console.log(`Error messages: ${hasError ? '❌ YES' : '✅ NO'}`);
    console.log(`Console errors: ${consoleErrors.length}`);
    console.log(`Failed requests: ${failedRequests.length}`);
    console.log(`BFF failures: ${bffCalls.length}`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

    // Print console errors
    if (consoleErrors.length > 0) {
      console.log('❌ CONSOLE ERRORS:');
      consoleErrors.forEach((err, i) => console.log(`${i + 1}. ${err}`));
      console.log('');
    }

    // Print failed requests
    if (failedRequests.length > 0) {
      console.log('❌ FAILED REQUESTS:');
      failedRequests.forEach((req, i) => 
        console.log(`${i + 1}. ${req.status} ${req.url}`)
      );
      console.log('');
    }

    // Assertions (lenient - we're debugging)
    expect(tabCount).toBeGreaterThan(0); // At least tabs should render
    expect(title).toContain('Monitoring'); // Page title should be correct
  });

  test('should debug Security page loading', async ({ page }) => {
    console.log('\n🐛 DEBUG: Investigating Security page loading issue\n');
    
    const consoleErrors: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
        console.log('❌ Console Error:', msg.text());
      }
    });

    await login(page);
    await page.goto('/admin/security', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(3000);

    const stillLoading = await page.locator('[role="progressbar"], .MuiCircularProgress-root').isVisible();
    console.log(`Loading spinner: ${stillLoading ? '❌ VISIBLE' : '✅ HIDDEN'}`);
    console.log(`Console errors: ${consoleErrors.length}`);

    await page.screenshot({ 
      path: 'e2e/test-results/security-page-debug.png', 
      fullPage: true 
    });

    expect(consoleErrors.length).toBeLessThan(5); // Allow some minor errors
  });

  test('should debug Audit page loading', async ({ page }) => {
    console.log('\n🐛 DEBUG: Investigating Audit page loading issue\n');
    
    const consoleErrors: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
        console.log('❌ Console Error:', msg.text());
      }
    });

    await login(page);
    await page.goto('/admin/audit', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(3000);

    const stillLoading = await page.locator('[role="progressbar"], .MuiCircularProgress-root').isVisible();
    console.log(`Loading spinner: ${stillLoading ? '❌ VISIBLE' : '✅ HIDDEN'}`);
    console.log(`Console errors: ${consoleErrors.length}`);

    await page.screenshot({ 
      path: 'e2e/test-results/audit-page-debug.png', 
      fullPage: true 
    });

    expect(consoleErrors.length).toBeLessThan(5);
  });
});
