import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login.js';
import { TestLogger } from '../../helpers/test-logger.js';

/**
 * 🔍 DEBUG TEST: Analyzuje 403 error při /api/auth/session
 */
test.describe('Auth 403 Debug', () => {
  test('Login and capture /api/auth/session request details', async ({ page }) => {
    test.setTimeout(90000);
    
    const requests: any[] = [];
    const responses: any[] = [];
    const failedRequests: any[] = [];

    TestLogger.testStart('403 Debug - Network Analysis', 1, 1);

    // 📡 Zachytávání network requestů
    page.on('request', request => {
      const url = request.url();
      if (url.includes('/api/')) {
        requests.push({
          url: url,
          method: request.method(),
          headers: request.headers(),
          postData: request.postData(),
          timestamp: new Date().toISOString()
        });
      }
    });

    page.on('response', async response => {
      const url = response.url();
      if (url.includes('/api/')) {
        let body = null;
        try {
          body = await response.text();
        } catch (e) {
          body = '<failed to read>';
        }

        const responseData = {
          url: url,
          status: response.status(),
          statusText: response.statusText(),
          headers: response.headers(),
          body: body,
          timestamp: new Date().toISOString()
        };
        
        responses.push(responseData);
        
        if (response.status() >= 400) {
          failedRequests.push(responseData);
        }
      }
    });

    // 🔐 Provést login
    TestLogger.step('Performing Keycloak authentication...', 1);
    await login(page, { waitForDashboard: false }); // Skip dashboard wait
    TestLogger.success('Login completed');

    // ⏳ Počkat na async operace
    TestLogger.step('Waiting for async operations...', 2);
    await page.waitForTimeout(5000);

    // �� Vypsat API requesty
    console.log('\n' + '='.repeat(80));
    console.log(`📤 API REQUESTS (${requests.length} total):`);
    console.log('='.repeat(80));
    requests.forEach((req, i) => {
      const path = req.url.split('admin.core-platform.local')[1] || req.url;
      console.log(`[${i + 1}] ${req.method} ${path} @ ${req.timestamp}`);
    });

    // 📊 Vypsat failed requesty
    if (failedRequests.length > 0) {
      console.log('\n' + '='.repeat(80));
      console.log(`🚨 FAILED REQUESTS (${failedRequests.length}):`);
      console.log('='.repeat(80));
      failedRequests.forEach((res, i) => {
        const path = res.url.split('admin.core-platform.local')[1] || res.url;
        console.log(`\n[${i + 1}] ${res.status} - ${path}`);
        console.log(`Body: ${res.body}`);
        console.log(`Headers:`, res.headers);
      });
    }

    // 🔍 Analyzovat /api/auth/session
    const sessionReq = requests.find(r => r.url.includes('/api/auth/session'));
    const sessionRes = responses.find(r => r.url.includes('/api/auth/session'));

    console.log('\n' + '='.repeat(80));
    console.log('🎯 /api/auth/session ANALYSIS:');
    console.log('='.repeat(80));

    if (sessionReq) {
      console.log('\n✅ REQUEST FOUND:');
      console.log('Method:', sessionReq.method);
      console.log('Headers:', JSON.stringify(sessionReq.headers, null, 2));
      if (sessionReq.postData) console.log('Body:', sessionReq.postData);
    } else {
      console.log('\n❌ NO REQUEST FOUND - Frontend never called /api/auth/session!');
    }

    if (sessionRes) {
      console.log('\n📥 RESPONSE:');
      console.log('Status:', sessionRes.status);
      console.log('Headers:', JSON.stringify(sessionRes.headers, null, 2));
      console.log('Body:', sessionRes.body);
      
      if (sessionRes.status === 403) {
        console.log('\n🔥 403 FORBIDDEN ERROR DETECTED! 🔥');
      }
    } else {
      console.log('\n❌ NO RESPONSE - Request never reached server or timed out!');
    }

    // 📸 Screenshot
    await page.screenshot({ path: 'e2e/screenshots/auth-403-debug.png', fullPage: true });

    TestLogger.testEnd();

    // Assertions
    if (!sessionReq) throw new Error('Frontend never called /api/auth/session!');
    if (!sessionRes) throw new Error('No response received for /api/auth/session!');
    if (sessionRes.status === 403) throw new Error(`403 Forbidden: ${sessionRes.body}`);

    expect(sessionRes.status).toBe(200);
  });
});
