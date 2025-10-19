import { test, expect } from '@playwright/test';
import { createTenant, deleteTenant, verifyTenantProvisioning } from '../../helpers/tenant-api';
import { login } from '../../helpers/login';

/**
 * 🧪 E2E Tests for Complete Tenant Provisioning
 * 
 * Verifies that creating a tenant automatically provisions:
 * 1. ✅ Keycloak realm with tenant-specific configuration
 * 2. ✅ Tenant roles (ROLE_USER, ROLE_ADMIN, etc.)
 * 3. ✅ Grafana organization with unique ID
 * 4. ✅ Grafana service account with API token
 * 5. ✅ Database tenant_bindings record
 * 6. ✅ No 409 Conflict errors (idempotent provisioning)
 * 
 * This test replaces the need for V1.1__seed_demo.sql migration!
 */

test.describe('🏢 Complete Tenant Provisioning - E2E', () => {
  const TEST_TENANT_KEY = 'e2e-test-tenant';
  const TEST_TENANT_DISPLAY = 'E2E Test Tenant';

  test.beforeAll(async () => {
    console.log('\n🏢 ═══════════════════════════════════════════════════');
    console.log('🏢  COMPLETE TENANT PROVISIONING E2E TEST SUITE');
    console.log('🏢 ═══════════════════════════════════════════════════\n');
  });

  test.afterEach(async ({ request }) => {
    // Cleanup: Delete test tenant after each test
    console.log('\n🧹 Cleaning up test tenant...');
    try {
      await deleteTenant(request, TEST_TENANT_KEY);
    } catch (e) {
      console.warn('⚠️  Cleanup failed (tenant may not exist):', e);
    }
  });

  test('✅ Should create tenant with complete infrastructure provisioning', async ({ request }) => {
    console.log('📋 Test 1: Creating tenant and verifying all components...\n');

    // 1. Create tenant via API
    const createResponse = await createTenant(request, TEST_TENANT_KEY, TEST_TENANT_DISPLAY);

    expect(createResponse.success).toBeTruthy();
    expect(createResponse.tenant).toBeDefined();
    expect(createResponse.tenant?.key).toBe(TEST_TENANT_KEY);
    expect(createResponse.tenant?.displayName).toBe(TEST_TENANT_DISPLAY);
    expect(createResponse.tenant?.id).toBeDefined();

    console.log('✅ Step 1/4: Tenant created via API\n');

    // 2. Wait for provisioning to complete (Keycloak + Grafana are async)
    await new Promise(resolve => setTimeout(resolve, 2000));

    // 3. Verify complete provisioning
    const provisioningStatus = await verifyTenantProvisioning(request, TEST_TENANT_KEY);

    expect(provisioningStatus.keycloakRealm).toBeTruthy();
    console.log('✅ Step 2/4: Keycloak realm exists\n');

    expect(provisioningStatus.tenantRoles).toBeTruthy();
    console.log('✅ Step 3/4: Tenant roles provisioned\n');

    expect(provisioningStatus.grafanaBinding).toBeTruthy();
    console.log('✅ Step 4/4: Grafana org + service account provisioned\n');

    console.log('🎉 All provisioning steps completed successfully!\n');
  });

  test('✅ Should have Grafana service account with valid API token', async ({ request }) => {
    console.log('📋 Test 2: Verifying Grafana service account token...\n');

    // 1. Create tenant
    await createTenant(request, TEST_TENANT_KEY, TEST_TENANT_DISPLAY);
    await new Promise(resolve => setTimeout(resolve, 2000));

    // 2. Get Grafana binding
    const bindingResponse = await request.get(`/api/monitoring/tenant-bindings/${TEST_TENANT_KEY}`);
    expect(bindingResponse.ok()).toBeTruthy();

    const binding = await bindingResponse.json();
    
    expect(binding.tenantId).toBe(TEST_TENANT_KEY);
    expect(binding.grafanaOrgId).toBeDefined();
    expect(binding.grafanaOrgId).toBeGreaterThan(0);
    expect(binding.serviceAccountId).toBeDefined();
    expect(binding.serviceAccountToken).toBeDefined();
    expect(binding.serviceAccountToken).toMatch(/^glsa_/); // Grafana token format

    console.log(`✅ Grafana org ID: ${binding.grafanaOrgId}`);
    console.log(`✅ Service account ID: ${binding.serviceAccountId}`);
    console.log(`✅ Token format valid: ${binding.serviceAccountToken.substring(0, 20)}...`);
    console.log('\n🎉 Grafana provisioning verified!\n');
  });

  test('✅ Should have tenant-specific Keycloak roles', async ({ request }) => {
    console.log('📋 Test 3: Verifying Keycloak tenant roles...\n');

    // 1. Create tenant
    await createTenant(request, TEST_TENANT_KEY, TEST_TENANT_DISPLAY);
    await new Promise(resolve => setTimeout(resolve, 2000));

    // 2. Get tenant status (includes roles check)
    const status = await request.get(`/api/admin/tenants/${TEST_TENANT_KEY}/status`);
    const statusData = await status.json();

    expect(statusData.realmExists).toBeTruthy();
    expect(statusData.rolesExist).toBeTruthy();

    console.log('✅ Keycloak realm exists');
    console.log('✅ Tenant roles provisioned');
    console.log('\n🎉 Keycloak configuration verified!\n');
  });

  test('✅ Should NOT cause 409 Conflict on monitoring dashboard access', async ({ page, request }) => {
    console.log('📋 Test 4: Verifying no 409 conflicts in monitoring...\n');

    // 1. Create tenant
    await createTenant(request, TEST_TENANT_KEY, TEST_TENANT_DISPLAY);
    await new Promise(resolve => setTimeout(resolve, 2000));

    // 2. Login as admin (to access cross-tenant monitoring)
    await login(page, { username: 'admin', password: 'admin' });

    // 3. Navigate to monitoring dashboard
    await page.goto('/core-admin/monitoring');
    await page.waitForLoadState('networkidle');

    // 4. Monitor console for 409 errors
    const consoleErrors: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    // 5. Wait for data to load
    await page.waitForTimeout(3000);

    // 6. Check for 409 Conflict errors
    const has409Error = consoleErrors.some(err => 
      err.includes('409') || 
      err.includes('Conflict') ||
      err.includes('Organization name taken')
    );

    expect(has409Error).toBeFalsy();

    if (has409Error) {
      console.error('❌ Found 409 Conflict errors:', consoleErrors.filter(e => e.includes('409')));
    } else {
      console.log('✅ No 409 Conflict errors detected');
    }

    console.log('\n🎉 Monitoring dashboard works without conflicts!\n');
  });

  test('✅ Should delete tenant and clean up all resources', async ({ request }) => {
    console.log('📋 Test 5: Verifying complete tenant deletion...\n');

    // 1. Create tenant
    await createTenant(request, TEST_TENANT_KEY, TEST_TENANT_DISPLAY);
    await new Promise(resolve => setTimeout(resolve, 2000));

    // 2. Verify it exists
    const statusBefore = await request.get(`/api/admin/tenants/${TEST_TENANT_KEY}/status`);
    expect(statusBefore.ok()).toBeTruthy();
    console.log('✅ Tenant exists before deletion\n');

    // 3. Delete tenant
    const deleteResponse = await deleteTenant(request, TEST_TENANT_KEY);
    expect(deleteResponse.success).toBeTruthy();

    // 4. Wait for cleanup
    await new Promise(resolve => setTimeout(resolve, 2000));

    // 5. Verify it's gone
    const statusAfter = await request.get(`/api/admin/tenants/${TEST_TENANT_KEY}/status`);
    const statusData = await statusAfter.json();
    
    expect(statusData.realmExists).toBeFalsy();
    console.log('✅ Keycloak realm deleted');

    const bindingAfter = await request.get(`/api/monitoring/tenant-bindings/${TEST_TENANT_KEY}`);
    expect(bindingAfter.ok()).toBeFalsy();
    console.log('✅ Grafana binding deleted');

    console.log('\n🎉 Complete cleanup verified!\n');
  });
});
