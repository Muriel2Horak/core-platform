import { test, expect, chromium, Browser, BrowserContext, Page } from '@playwright/test';

/**
 * E2E tests for real-time presence tracking
 * 
 * Tests concurrent user scenarios with 2 browsers:
 * - User A and User B view same entity
 * - User A sees User B in presence indicator
 * - User A acquires lock, User B sees lock indicator
 * - Write pipeline sets stale flag, both users see warning
 */

test.describe('Real-Time Presence Tracking', () => {
  let browserA: Browser;
  let browserB: Browser;
  let contextA: BrowserContext;
  let contextB: BrowserContext;
  let pageA: Page;
  let pageB: Page;

  const ORDER_ID = '123';
  const USER_A = 'user-a@test.com';
  const USER_B = 'user-b@test.com';
  const TENANT_ID = 'test-tenant';

  test.beforeAll(async () => {
    // Launch 2 separate browsers
    browserA = await chromium.launch();
    browserB = await chromium.launch();
  });

  test.afterAll(async () => {
    await browserA.close();
    await browserB.close();
  });

  test.beforeEach(async () => {
    // Create contexts for each user
    contextA = await browserA.newContext();
    contextB = await browserB.newContext();

    pageA = await contextA.newPage();
    pageB = await contextB.newPage();

    // Login as different users (assuming login flow exists)
    await pageA.goto('/login');
    await pageA.fill('[data-testid="email"]', USER_A);
    await pageA.fill('[data-testid="password"]', 'password');
    await pageA.click('[data-testid="login-button"]');
    await pageA.waitForURL('/dashboard'); // Wait for redirect

    await pageB.goto('/login');
    await pageB.fill('[data-testid="email"]', USER_B);
    await pageB.fill('[data-testid="password"]', 'password');
    await pageB.click('[data-testid="login-button"]');
    await pageB.waitForURL('/dashboard');
  });

  test.afterEach(async () => {
    await contextA.close();
    await contextB.close();
  });

  test('should show presence indicator when multiple users view same entity', async () => {
    // User A navigates to order edit page
    await pageA.goto(`/orders/${ORDER_ID}/edit`);
    await expect(pageA.locator('[data-testid="presence-indicator"]')).toBeVisible();
    await expect(pageA.locator('[data-testid="presence-users"]')).toContainText('1'); // Only user A

    // User B navigates to same order
    await pageB.goto(`/orders/${ORDER_ID}/edit`);
    await expect(pageB.locator('[data-testid="presence-indicator"]')).toBeVisible();

    // User A should now see 2 users
    await expect(pageA.locator('[data-testid="presence-users"]')).toContainText('2', { timeout: 5000 });

    // User B should see 2 users
    await expect(pageB.locator('[data-testid="presence-users"]')).toContainText('2');
  });

  test('should show avatars of other users', async () => {
    await pageA.goto(`/orders/${ORDER_ID}/edit`);
    await pageB.goto(`/orders/${ORDER_ID}/edit`);

    // Wait for presence update
    await pageA.waitForTimeout(1000);

    // User A should see User B avatar
    const avatarA = pageA.locator(`[data-testid="presence-avatar-${USER_B}"]`);
    await expect(avatarA).toBeVisible();

    // User B should see User A avatar
    const avatarB = pageB.locator(`[data-testid="presence-avatar-${USER_A}"]`);
    await expect(avatarB).toBeVisible();
  });

  test('should show field lock indicator when user acquires lock', async () => {
    await pageA.goto(`/orders/${ORDER_ID}/edit`);
    await pageB.goto(`/orders/${ORDER_ID}/edit`);

    // User A focuses on field (acquires lock)
    await pageA.click('[data-testid="field-totalAmount"]');

    // User A should see green lock (owns lock)
    const lockA = pageA.locator('[data-testid="lock-totalAmount"]');
    await expect(lockA).toHaveClass(/text-green-600/);

    // User B should see red lock (locked by other)
    const lockB = pageB.locator('[data-testid="lock-totalAmount"]');
    await expect(lockB).toBeVisible({ timeout: 3000 });
    await expect(lockB).toHaveClass(/text-red-600/);

    // User B field should be disabled
    const fieldB = pageB.locator('[data-testid="field-totalAmount"]');
    await expect(fieldB).toBeDisabled();
  });

  test('should release lock when user blurs field', async () => {
    await pageA.goto(`/orders/${ORDER_ID}/edit`);
    await pageB.goto(`/orders/${ORDER_ID}/edit`);

    // User A acquires lock
    await pageA.click('[data-testid="field-totalAmount"]');

    // Wait for lock to appear
    await pageB.locator('[data-testid="lock-totalAmount"]').waitFor({ state: 'visible', timeout: 3000 });

    // User A releases lock (blur field)
    await pageA.click('body'); // Click outside to blur

    // User B should see lock removed
    await expect(pageB.locator('[data-testid="lock-totalAmount"]')).not.toHaveClass(/text-red-600/, { timeout: 3000 });

    // User B field should be enabled
    const fieldB = pageB.locator('[data-testid="field-totalAmount"]');
    await expect(fieldB).toBeEnabled();
  });

  test('should show stale badge when entity is being modified', async () => {
    await pageA.goto(`/orders/${ORDER_ID}/edit`);
    await pageB.goto(`/orders/${ORDER_ID}/edit`);

    // Simulate write pipeline (via API call)
    await pageA.request.post(`/api/test/kafka/publish-mutating`, {
      data: {
        tenantId: TENANT_ID,
        entity: 'Order',
        id: ORDER_ID,
        userId: USER_A,
      },
    });

    // Both users should see stale badge
    await expect(pageA.locator('[data-testid="stale-badge"]')).toBeVisible({ timeout: 5000 });
    await expect(pageB.locator('[data-testid="stale-badge"]')).toBeVisible({ timeout: 5000 });

    // Both users should see "Editing by User A"
    await expect(pageA.locator('[data-testid="stale-badge"]')).toContainText('Editing');
    await expect(pageB.locator('[data-testid="stale-badge"]')).toContainText('Editing');

    // User B should see field disabled
    const fieldB = pageB.locator('[data-testid="field-totalAmount"]');
    await expect(fieldB).toBeDisabled();
  });

  test('should clear stale badge and increment version when modification completes', async () => {
    await pageA.goto(`/orders/${ORDER_ID}/edit`);
    await pageB.goto(`/orders/${ORDER_ID}/edit`);

    // Get initial version
    const initialVersion = await pageA.locator('[data-testid="entity-version"]').textContent();

    // Set stale
    await pageA.request.post(`/api/test/kafka/publish-mutating`, {
      data: { tenantId: TENANT_ID, entity: 'Order', id: ORDER_ID, userId: USER_A },
    });

    await expect(pageA.locator('[data-testid="stale-badge"]')).toBeVisible({ timeout: 5000 });

    // Complete modification
    await pageA.request.post(`/api/test/kafka/publish-mutated`, {
      data: { tenantId: TENANT_ID, entity: 'Order', id: ORDER_ID, userId: USER_A, version: 6 },
    });

    // Stale badge should disappear
    await expect(pageA.locator('[data-testid="stale-badge"]')).not.toBeVisible({ timeout: 5000 });
    await expect(pageB.locator('[data-testid="stale-badge"]')).not.toBeVisible({ timeout: 5000 });

    // Version should increment
    const newVersionA = await pageA.locator('[data-testid="entity-version"]').textContent();
    const newVersionB = await pageB.locator('[data-testid="entity-version"]').textContent();

    expect(parseInt(newVersionA!)).toBeGreaterThan(parseInt(initialVersion!));
    expect(newVersionA).toBe(newVersionB);
  });

  test('should auto-reconnect WebSocket after disconnect', async () => {
    await pageA.goto(`/orders/${ORDER_ID}/edit`);

    // Check connected status
    await expect(pageA.locator('[data-testid="connection-status"]')).toContainText('Live');

    // Simulate network disconnect (close WebSocket via browser console)
    await pageA.evaluate(() => {
      // @ts-ignore - accessing global WebSocket
      const ws = window.__presenceClient?.ws;
      if (ws) ws.close();
    });

    // Connection status should show "Offline"
    await expect(pageA.locator('[data-testid="connection-status"]')).toContainText('Offline', { timeout: 2000 });

    // Should auto-reconnect within 5s
    await expect(pageA.locator('[data-testid="connection-status"]')).toContainText('Live', { timeout: 10000 });
  });

  test('should maintain presence across page navigation', async () => {
    await pageA.goto(`/orders/${ORDER_ID}/edit`);
    await pageB.goto(`/orders/${ORDER_ID}/edit`);

    // User A navigates away
    await pageA.goto('/dashboard');

    // User B should see User A removed from presence
    await expect(pageB.locator('[data-testid="presence-users"]')).toContainText('1', { timeout: 5000 });

    // User A navigates back
    await pageA.goto(`/orders/${ORDER_ID}/edit`);

    // User B should see User A back in presence
    await expect(pageB.locator('[data-testid="presence-users"]')).toContainText('2', { timeout: 5000 });
  });

  test('should handle heartbeat timeout gracefully', async () => {
    await pageA.goto(`/orders/${ORDER_ID}/edit`);
    await pageB.goto(`/orders/${ORDER_ID}/edit`);

    // Verify both users visible
    await expect(pageA.locator('[data-testid="presence-users"]')).toContainText('2');

    // Pause User A heartbeat (simulate tab frozen)
    await pageA.evaluate(() => {
      // @ts-ignore
      if (window.__presenceClient?.heartbeatInterval) {
        // @ts-ignore
        clearInterval(window.__presenceClient.heartbeatInterval);
      }
    });

    // Wait for TTL expiration (60s + buffer)
    await pageB.waitForTimeout(62000);

    // User B should see User A removed
    await expect(pageB.locator('[data-testid="presence-users"]')).toContainText('1');
  });
});
