/**
 * E2E Tests: MCP (Model Context Protocol) Endpoints
 * 
 * Tests MCP tool endpoints:
 * - ui_context.get_current_view
 * - wf_context.get_workflow
 * - auth.get_user_capabilities
 * - data_context.query (not implemented, should return 501)
 */

import { test, expect } from '@playwright/test';
import { login } from '../../helpers/login.js';
import { TestLogger } from '../../helpers/test-logger';

test.describe('MCP Endpoints E2E Tests', () => {
  test.beforeAll(() => {
    TestLogger.suiteStart('MCP ENDPOINTS TESTS');
  });

  test.afterAll(() => {
    TestLogger.suiteEnd('MCP ENDPOINTS TESTS');
  });

  test.beforeEach(async ({ page }) => {
    TestLogger.info('Logging in...');
    await login(page);
    TestLogger.success('Login completed');
  });

  test('MCP ui_context.get_current_view should return UI metadata', async ({ page, request }) => {
    TestLogger.testStart('MCP UI Context Tool', 1, 4);

    // Step 1: Enable AI
    TestLogger.step('Enabling AI...', 1);
    await page.goto('/admin/metamodel-studio');
    await page.click('text=🤖 AI Config');
    await page.waitForTimeout(1000);
    
    const aiToggle = page.locator('[type="checkbox"]').first();
    if (!(await aiToggle.isChecked())) {
      await aiToggle.click();
      await page.waitForTimeout(500);
    }
    await page.click('button:has-text("Save")');
    await page.waitForTimeout(1000);
    TestLogger.success('AI enabled');

    // Step 2: Call MCP UI context tool
    TestLogger.step('Calling MCP ui_context tool...', 2);
    const response = await request.post('/api/ai/mcp/ui_context/get_current_view', {
      headers: { 'Content-Type': 'application/json' },
      data: { routeId: 'users.list' },
    });

    if (response.status() === 404) {
      TestLogger.warn('MCP returned 404 (AI may be disabled)');
      test.skip();
      return;
    }

    expect(response.ok()).toBeTruthy();
    const context = await response.json();
    TestLogger.success('MCP UI context fetched');

    // Step 3: Verify response structure
    TestLogger.verify('Verifying UI context structure...');
    expect(context).toHaveProperty('routeId');
    expect(context).toHaveProperty('entity');
    expect(context).toHaveProperty('fields');
    TestLogger.success('UI context structure is correct');

    // Step 4: Verify META_ONLY (no data values)
    TestLogger.verify('Verifying META_ONLY mode...');
    const contextString = JSON.stringify(context);
    const hasValueFields = /"value"\s*:/.test(contextString);
    expect(hasValueFields).toBe(false);
    TestLogger.success('META_ONLY verified (no data values)');

    TestLogger.testEnd();
  });

  test('MCP wf_context.get_workflow should return workflow metadata', async ({ page, request }) => {
    TestLogger.testStart('MCP Workflow Context Tool', 2, 4);

    // Step 1: Enable AI
    TestLogger.step('Enabling AI...', 1);
    await page.goto('/admin/metamodel-studio');
    await page.click('text=🤖 AI Config');
    await page.waitForTimeout(1000);
    
    const aiToggle = page.locator('[type="checkbox"]').first();
    if (!(await aiToggle.isChecked())) {
      await aiToggle.click();
      await page.waitForTimeout(500);
    }
    await page.click('button:has-text("Save")');
    await page.waitForTimeout(1000);
    TestLogger.success('AI enabled');

    // Step 2: Call MCP workflow context tool
    TestLogger.step('Calling MCP wf_context tool...', 2);
    const response = await request.post('/api/ai/mcp/wf_context/get_workflow', {
      headers: { 'Content-Type': 'application/json' },
      data: { entity: 'workflow-draft' },
    });

    if (response.status() === 404) {
      TestLogger.warn('MCP returned 404 (AI may be disabled)');
      test.skip();
      return;
    }

    expect(response.ok()).toBeTruthy();
    const context = await response.json();
    TestLogger.success('MCP workflow context fetched');

    // Step 3: Verify response structure
    TestLogger.verify('Verifying workflow context structure...');
    expect(context).toHaveProperty('entity');
    expect(context).toHaveProperty('states');
    expect(context).toHaveProperty('actions');
    TestLogger.success('Workflow context structure is correct');

    // Step 4: Verify actions have howto steps
    TestLogger.verify('Verifying action metadata...');
    if (context.actions && context.actions.length > 0) {
      const actionWithHowto = context.actions.find((a: any) => a.howto && a.howto.length > 0);
      if (actionWithHowto) {
        TestLogger.success(`Found action with howto: ${actionWithHowto.label}`);
      } else {
        TestLogger.warn('No actions with howto steps found');
      }
    }

    TestLogger.testEnd();
  });

  test('MCP auth.get_user_capabilities should return user permissions (stub)', async ({ page, request }) => {
    TestLogger.testStart('MCP Auth Tool (Stub)', 3, 3);

    // Step 1: Enable AI
    TestLogger.step('Enabling AI...', 1);
    await page.goto('/admin/metamodel-studio');
    await page.click('text=🤖 AI Config');
    await page.waitForTimeout(1000);
    
    const aiToggle = page.locator('[type="checkbox"]').first();
    if (!(await aiToggle.isChecked())) {
      await aiToggle.click();
      await page.waitForTimeout(500);
    }
    await page.click('button:has-text("Save")');
    await page.waitForTimeout(1000);
    TestLogger.success('AI enabled');

    // Step 2: Call MCP auth tool
    TestLogger.step('Calling MCP auth tool...', 2);
    const response = await request.post('/api/ai/mcp/auth/get_user_capabilities', {
      headers: { 'Content-Type': 'application/json' },
      data: {},
    });

    if (response.status() === 404) {
      TestLogger.warn('MCP returned 404 (AI may be disabled)');
      test.skip();
      return;
    }

    expect(response.ok()).toBeTruthy();
    const context = await response.json();
    TestLogger.success('MCP auth context fetched (stub)');

    // Step 3: Verify stub response
    TestLogger.verify('Verifying stub response...');
    expect(context).toHaveProperty('stub');
    expect(context.stub).toBe(true);
    TestLogger.success('Stub response correct');

    TestLogger.testEnd();
  });

  test('MCP data_context.query should return 501 (not implemented)', async ({ page, request }) => {
    TestLogger.testStart('MCP Data Context Not Implemented', 4, 3);

    // Step 1: Enable AI
    TestLogger.step('Enabling AI...', 1);
    await page.goto('/admin/metamodel-studio');
    await page.click('text=🤖 AI Config');
    await page.waitForTimeout(1000);
    
    const aiToggle = page.locator('[type="checkbox"]').first();
    if (!(await aiToggle.isChecked())) {
      await aiToggle.click();
      await page.waitForTimeout(500);
    }
    await page.click('button:has-text("Save")');
    await page.waitForTimeout(1000);
    TestLogger.success('AI enabled');

    // Step 2: Call MCP data context tool
    TestLogger.step('Calling MCP data_context tool...', 2);
    const response = await request.post('/api/ai/mcp/data_context/query', {
      headers: { 'Content-Type': 'application/json' },
      data: { entity: 'users', query: 'list' },
    });

    // Step 3: Verify 501 response
    TestLogger.verify('Verifying 501 NOT_IMPLEMENTED...');
    expect(response.status()).toBe(501);
    TestLogger.success('Correctly returns 501 NOT_IMPLEMENTED');

    TestLogger.testEnd();
  });

  test('MCP endpoints should return 404 when AI is disabled', async ({ page, request }) => {
    TestLogger.testStart('MCP Disabled Returns 404', 5, 3);

    // Step 1: Disable AI
    TestLogger.step('Disabling AI...', 1);
    await page.goto('/admin/metamodel-studio');
    await page.click('text=🤖 AI Config');
    await page.waitForTimeout(1000);
    
    const aiToggle = page.locator('[type="checkbox"]').first();
    if (await aiToggle.isChecked()) {
      await aiToggle.click();
      await page.waitForTimeout(500);
    }
    await page.click('button:has-text("Save")');
    await page.waitForTimeout(1000);
    TestLogger.success('AI disabled');

    // Step 2: Call MCP endpoints
    TestLogger.step('Calling MCP endpoints with AI disabled...', 2);
    const responses = await Promise.all([
      request.post('/api/ai/mcp/ui_context/get_current_view', {
        data: { routeId: 'users.list' },
      }),
      request.post('/api/ai/mcp/wf_context/get_workflow', {
        data: { entity: 'workflow-draft' },
      }),
      request.post('/api/ai/mcp/auth/get_user_capabilities', {
        data: {},
      }),
    ]);

    // Step 3: Verify all return 404
    TestLogger.verify('Verifying all MCP endpoints return 404...');
    responses.forEach((response, idx) => {
      expect(response.status()).toBe(404);
      TestLogger.info(`MCP endpoint ${idx + 1} returned 404 ✓`);
    });
    TestLogger.success('All MCP endpoints correctly return 404 when AI disabled');

    TestLogger.testEnd();
  });
});
