import { test, expect, Page } from '@playwright/test';

/**
 * 🧪 W6: Workflow UX E2E Test
 * 
 * ⚠️ SKIP: Workflow UI not implemented yet
 * 
 * Tests the complete workflow visualization and interaction flow:
 * - Graph rendering with layout toggle (elk/dagre)
 * - Current state highlighting
 * - Allowed/blocked edges with tooltips
 * - Timeline panel with SLA badges
 * - Actions bar with lock detection
 * - Read-only mode on lock signal
 * 
 * @since 2025-10-14
 */

test.describe.skip('W6 - Workflow Frontend UX', () => {
  let page: Page;
  
  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    
    // Setup: Create test workflow instance
    await page.request.post('/api/workflows/order/test-w6', {
      data: {
        initialState: 'draft',
      },
    });
    
    // Transition to submitted state
    await page.request.post('/api/workflows/order/test-w6/transition', {
      data: {
        action: 'submit',
      },
    });
  });

  test('renders workflow graph with current state highlighted', async () => {
    await page.goto('/workflows/order/test-w6');
    
    // Wait for graph to load
    await page.waitForSelector('[data-testid="react-flow"]');
    
    // Check current state is highlighted (blue border/background)
    const currentNode = page.locator('[data-testid="node-submitted"]');
    await expect(currentNode).toHaveAttribute('data-current', 'true');
    
    // Check styling (blue border)
    const nodeStyle = await currentNode.evaluate((el) => 
      window.getComputedStyle(el).borderColor
    );
    expect(nodeStyle).toContain('33, 150, 243'); // rgb(33, 150, 243) = blue
  });

  test('toggles layout algorithm between elk and dagre', async () => {
    await page.goto('/workflows/order/test-w6');
    
    // Check default layout is ELK
    const elkButton = page.getByRole('button', { name: /elk/i });
    await expect(elkButton).toHaveAttribute('aria-pressed', 'true');
    
    // Toggle to Dagre
    const dagreButton = page.getByRole('button', { name: /dagre/i });
    await dagreButton.click();
    
    await expect(dagreButton).toHaveAttribute('aria-pressed', 'true');
    await expect(elkButton).toHaveAttribute('aria-pressed', 'false');
    
    // Verify nodes repositioned (dagre uses different layout)
    const node = page.locator('[data-testid="node-submitted"]');
    const initialPos = await node.evaluate((el) => {
      const transform = el.style.transform;
      return transform;
    });
    
    // Switch back to ELK
    await elkButton.click();
    
    const newPos = await node.evaluate((el) => el.style.transform);
    expect(newPos).not.toBe(initialPos); // Position changed
  });

  test('shows allowed edge as green and animated', async () => {
    await page.goto('/workflows/order/test-w6');
    
    // Allowed edge: submitted → approved
    const allowedEdge = page.locator('[data-testid="edge-submitted-approved"]');
    
    await expect(allowedEdge).toHaveAttribute('data-animated', 'true');
    
    const edgeStyle = await allowedEdge.evaluate((el) => 
      window.getComputedStyle(el).stroke
    );
    expect(edgeStyle).toContain('#4caf50'); // green
  });

  test('shows blocked edge as gray with "why not" tooltip', async () => {
    await page.goto('/workflows/order/test-w6');
    
    // Blocked edge: submitted → draft (reject not allowed)
    const blockedEdge = page.locator('[data-testid="edge-submitted-draft"]');
    
    await expect(blockedEdge).toHaveAttribute('data-animated', 'false');
    
    const edgeStyle = await blockedEdge.evaluate((el) => 
      window.getComputedStyle(el).stroke
    );
    expect(edgeStyle).toContain('#9e9e9e'); // gray
    
    // Hover to show tooltip
    await blockedEdge.hover();
    
    await expect(page.locator('[role="tooltip"]')).toContainText(
      /Missing manager approval/i
    );
  });

  test('displays timeline with durations and SLA badges', async () => {
    await page.goto('/workflows/order/test-w6');
    
    // Check timeline header
    await expect(page.getByText('Workflow Timeline')).toBeVisible();
    
    // Check total duration
    await expect(page.getByText(/Total:/)).toBeVisible();
    
    // Check timeline entries
    const entries = page.locator('[data-testid="duration"]');
    await expect(entries.first()).toBeVisible();
    
    // Check SLA badges
    const slaBadges = page.locator('[data-testid="sla-badge"]');
    const firstBadge = slaBadges.first();
    
    await expect(firstBadge).toBeVisible();
    const badgeText = await firstBadge.textContent();
    expect(['OK', 'WARN', 'BREACH']).toContain(badgeText);
  });

  test('shows action buttons with current state', async () => {
    await page.goto('/workflows/order/test-w6');
    
    // Check current state chip
    await expect(page.getByTestId('current-state')).toContainText('Current: submitted');
    
    // Check unlock icon (not locked initially)
    await expect(page.getByTestId('unlock-icon')).toBeVisible();
    
    // Check action buttons
    const approveButton = page.getByTestId('action-approve');
    await expect(approveButton).toBeVisible();
    await expect(approveButton).toBeEnabled();
    await expect(approveButton).toContainText(/Approve → approved/);
  });

  test('disables actions when workflow is locked by another user', async () => {
    await page.goto('/workflows/order/test-w6');
    
    // Simulate lock signal via Kafka (published by another user)
    await page.request.post('/api/workflows/order/test-w6/lock', {
      data: {
        userId: 'other-user@example.com',
      },
    });
    
    // Wait for lock propagation (SSE or polling)
    await page.waitForTimeout(2000);
    
    // Check lock warning is visible
    await expect(page.getByTestId('lock-warning')).toBeVisible();
    await expect(page.getByText(/locked by other-user@example.com/i)).toBeVisible();
    
    // Check lock icon
    await expect(page.getByTestId('lock-icon')).toBeVisible();
    
    // Check all actions are disabled
    const approveButton = page.getByTestId('action-approve');
    await expect(approveButton).toBeDisabled();
    
    // Hover to see lock tooltip
    await approveButton.hover();
    await expect(page.locator('[role="tooltip"]')).toContainText(
      /locked by other-user/i
    );
  });

  test('re-enables actions when workflow is unlocked', async () => {
    await page.goto('/workflows/order/test-w6');
    
    // Lock first
    await page.request.post('/api/workflows/order/test-w6/lock', {
      data: {
        userId: 'temp-user@example.com',
      },
    });
    
    await page.waitForTimeout(1000);
    
    // Verify locked
    await expect(page.getByTestId('lock-icon')).toBeVisible();
    
    // Unlock
    await page.request.post('/api/workflows/order/test-w6/unlock', {
      data: {
        userId: 'temp-user@example.com',
      },
    });
    
    await page.waitForTimeout(1000);
    
    // Verify unlocked
    await expect(page.getByTestId('unlock-icon')).toBeVisible();
    
    // Actions should be enabled again
    const approveButton = page.getByTestId('action-approve');
    await expect(approveButton).toBeEnabled();
  });

  test('refreshes stale data before applying action', async () => {
    await page.goto('/workflows/order/test-w6');
    
    // Wait for stale warning (30s timeout in ActionsBar)
    // For E2E, we can mock time or just wait
    await page.waitForTimeout(31000);
    
    // Check stale warning
    await expect(page.getByTestId('stale-warning')).toBeVisible();
    await expect(page.getByText(/data may be stale/i)).toBeVisible();
    
    // Click action - should trigger refresh first
    const approveButton = page.getByTestId('action-approve');
    await approveButton.click();
    
    // Verify refresh happened (stale warning disappears)
    await expect(page.getByTestId('stale-warning')).not.toBeVisible();
  });

  test.afterAll(async () => {
    // Cleanup test workflow
    await page.request.delete('/api/workflows/order/test-w6');
    await page.close();
  });
});
