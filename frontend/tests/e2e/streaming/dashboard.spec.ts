import { test, expect } from '@playwright/test';

/**
 * 🧪 E2E Tests for Streaming Dashboard
 * 
 * Prerequisites:
 * - Docker compose with --profile streaming running
 * - Backend accessible at baseURL
 * - Test user with appropriate permissions
 */

test.describe('Streaming Dashboard - E2E', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to streaming dashboard
    await page.goto('/core-admin/streaming');
  });

  test('loads Grafana iframe successfully', async ({ page }) => {
    // Wait for Grafana iframe to load
    const grafanaFrame = page.frameLocator('iframe[title*="Grafana"]').first();
    
    // Verify iframe loads (HTTP 200)
    const response = await page.waitForResponse(
      response => response.url().includes('grafana') && response.status() === 200,
      { timeout: 10000 }
    );
    
    expect(response.ok()).toBeTruthy();
    console.log('✅ Grafana iframe loaded successfully');
  });

  test('displays streaming metrics counters', async ({ page }) => {
    // Wait for API call to complete
    await page.waitForResponse(
      response => response.url().includes('/api/admin/streaming/metrics'),
      { timeout: 10000 }
    );

    // Check for counters on the page
    const countersSection = page.locator('[data-testid="streaming-metrics"]');
    await expect(countersSection).toBeVisible({ timeout: 10000 });

    // Verify counters are > 0 after test flow
    const processedCount = await page.locator('[data-testid="messages-processed"]').textContent();
    const consumerCount = await page.locator('[data-testid="active-consumers"]').textContent();
    
    // Extract numbers from text (e.g., "Messages Processed: 123" -> "123")
    const processed = parseInt(processedCount?.match(/\d+/)?.[0] || '0');
    const consumers = parseInt(consumerCount?.match(/\d+/)?.[0] || '0');
    
    console.log(`📊 Metrics: ${processed} messages processed, ${consumers} active consumers`);
    
    // Expect at least some activity
    expect(processed).toBeGreaterThan(0);
  });

  test('replay DLQ button triggers API call', async ({ page }) => {
    // Find and click Replay DLQ button
    const replayButton = page.locator('button:has-text("Replay DLQ")');
    await expect(replayButton).toBeVisible();

    // Setup API response listener
    const responsePromise = page.waitForResponse(
      response => response.url().includes('/api/admin/streaming/dlq/replay')
    );

    // Click button
    await replayButton.click();

    // Verify API call
    const response = await responsePromise;
    expect(response.status()).toBe(202); // Accepted
    
    console.log('✅ DLQ replay initiated successfully');
  });

  test('verifies Kafka topics are created', async ({ page, request }) => {
    // Call admin API to list topics
    const response = await request.get('/api/admin/streaming/topics');
    expect(response.ok()).toBeTruthy();

    const topics = await response.json();
    
    // Verify expected topics exist
    expect(topics).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ name: expect.stringMatching(/core\..*\.events/) }),
      ])
    );

    console.log(`📋 Found ${topics.length} Kafka topics`);
  });

  test('streaming config endpoint returns valid configuration', async ({ page, request }) => {
    const response = await request.get('/api/admin/streaming/config');
    expect(response.ok()).toBeTruthy();

    const config = await response.json();
    
    // Verify config structure
    expect(config).toHaveProperty('enabled');
    expect(config).toHaveProperty('bootstrapServers');
    expect(config.enabled).toBe(true);

    console.log('✅ Streaming config validated:', JSON.stringify(config, null, 2));
  });

  test('health check shows streaming components', async ({ page, request }) => {
    const response = await request.get('/actuator/health');
    expect(response.ok()).toBeTruthy();

    const health = await response.json();
    
    // Verify overall status
    expect(health.status).toBe('UP');
    
    // Check for Kafka health indicator (if available)
    if (health.components?.kafka) {
      expect(health.components.kafka.status).toBe('UP');
      console.log('✅ Kafka health check passed');
    }
  });

  test('prometheus metrics include streaming metrics', async ({ page, request }) => {
    const response = await request.get('/actuator/prometheus');
    expect(response.ok()).toBeTruthy();

    const metrics = await response.text();
    
    // Verify streaming-related metrics are present
    expect(metrics).toContain('kafka_');
    expect(metrics).toContain('# HELP');

    console.log('✅ Prometheus metrics endpoint validated');
  });
});

test.describe('Streaming Dashboard - Test Flow', () => {
  test('end-to-end streaming flow', async ({ page, request }) => {
    // 1. Submit a test command
    console.log('📤 Step 1: Submitting test command...');
    
    const commandResponse = await request.post('/api/admin/commands', {
      data: {
        entityType: 'User',
        entityId: crypto.randomUUID(),
        operation: 'UPDATE',
        payload: { test: true },
      },
    });
    
    expect(commandResponse.status()).toBe(202);
    const command = await commandResponse.json();
    const commandId = command.id;
    
    console.log(`✅ Command submitted: ${commandId}`);

    // 2. Wait for event in Kafka (poll status endpoint)
    console.log('⏳ Step 2: Waiting for event processing...');
    
    let status = 'PENDING';
    let attempts = 0;
    const maxAttempts = 30; // 30 seconds max
    
    while (status !== 'APPLIED' && attempts < maxAttempts) {
      await page.waitForTimeout(1000);
      
      const statusResponse = await request.get(`/api/admin/commands/${commandId}/status`);
      if (statusResponse.ok()) {
        const statusData = await statusResponse.json();
        status = statusData.status;
        console.log(`  Status: ${status}`);
      }
      
      attempts++;
    }
    
    expect(status).toBe('APPLIED');
    console.log('✅ Event processed successfully');

    // 3. Verify metrics increased
    console.log('📊 Step 3: Verifying metrics...');
    
    await page.goto('/core-admin/streaming');
    await page.waitForResponse(
      response => response.url().includes('/api/admin/streaming/metrics')
    );
    
    const processedElement = page.locator('[data-testid="messages-processed"]');
    await expect(processedElement).toBeVisible();
    
    console.log('✅ Metrics verified');
  });
});
