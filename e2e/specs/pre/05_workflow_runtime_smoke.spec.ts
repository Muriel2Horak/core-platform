import { test, expect } from '@playwright/test';

/**
 * 🧪 PRE-DEPLOY: Workflow Runtime Smoke Test
 * 
 * ⚠️ SKIP: Workflow API not implemented yet
 * 
 * Verifies core workflow runtime functionality:
 * - Create instance
 * - Transition to next state
 * - Query instance state
 * - Event publishing
 * 
 * Runs: On push to main (before deploy)
 */

const API_BASE = process.env.API_URL || 'http://localhost:8080';
const TENANT_ID = 'T1';

test.describe.skip('Workflow Runtime Smoke Tests', () => {
  let instanceId: number;

  test('should create workflow instance', async ({ request }) => {
    const response = await request.post(`${API_BASE}/api/v1/workflows/instances`, {
      headers: {
        'Content-Type': 'application/json',
        'X-Tenant-Id': TENANT_ID,
      },
      data: {
        entityType: 'ORDER',
        entityId: `TEST-${Date.now()}`,
        initialState: 'DRAFT',
        metadata: { test: true },
      },
    });

    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.id).toBeDefined();
    expect(data.currentState).toBe('DRAFT');
    
    instanceId = data.id;
  });

  test('should transition to next state', async ({ request }) => {
    expect(instanceId).toBeDefined();

    const response = await request.put(
      `${API_BASE}/api/v1/workflows/instances/${instanceId}/transition`,
      {
        headers: {
          'Content-Type': 'application/json',
          'X-Tenant-Id': TENANT_ID,
        },
        data: {
          event: 'submit',
          metadata: { submittedBy: 'test-user' },
        },
      }
    );

    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.currentState).toBe('SUBMITTED');
  });

  test('should query instance state', async ({ request }) => {
    expect(instanceId).toBeDefined();

    const response = await request.get(
      `${API_BASE}/api/v1/workflows/instances/${instanceId}`,
      {
        headers: {
          'X-Tenant-Id': TENANT_ID,
        },
      }
    );

    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.id).toBe(instanceId);
    expect(data.currentState).toBe('SUBMITTED');
  });

  test('should retrieve event history', async ({ request }) => {
    expect(instanceId).toBeDefined();

    const response = await request.get(
      `${API_BASE}/api/v1/workflows/instances/${instanceId}/events`,
      {
        headers: {
          'X-Tenant-Id': TENANT_ID,
        },
      }
    );

    expect(response.ok()).toBeTruthy();
    const events = await response.json();
    expect(Array.isArray(events)).toBeTruthy();
    expect(events.length).toBeGreaterThanOrEqual(2); // create + submit
  });

  test('should search instances', async ({ request }) => {
    const response = await request.get(
      `${API_BASE}/api/v1/workflows/instances/search?entityType=ORDER&currentState=SUBMITTED`,
      {
        headers: {
          'X-Tenant-Id': TENANT_ID,
        },
      }
    );

    expect(response.ok()).toBeTruthy();
    const results = await response.json();
    expect(Array.isArray(results)).toBeTruthy();
    expect(results.some((r: any) => r.id === instanceId)).toBeTruthy();
  });
});
