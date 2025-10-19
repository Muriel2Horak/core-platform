/**
 * 🏢 Tenant Management API Helpers for E2E Tests
 * 
 * Provides utilities to create and delete tenants programmatically
 * during E2E test setup/teardown.
 */

import { APIRequestContext } from '@playwright/test';

export interface TenantCreateRequest {
  key: string;
  displayName: string;
}

export interface TenantInfo {
  id: string;
  key: string;
  displayName: string;
  realm: string;
  subdomain: string;
}

export interface CreateTenantResponse {
  success: boolean;
  message: string;
  tenant?: TenantInfo;
  error?: string;
}

/**
 * ✅ Creates a new tenant with complete infrastructure:
 * - Keycloak realm with roles
 * - Grafana organization + service account
 * - Database tenant registry
 */
export async function createTenant(
  request: APIRequestContext,
  tenantKey: string,
  displayName?: string
): Promise<CreateTenantResponse> {
  console.log(`🏗️  Creating tenant: ${tenantKey}...`);

  const response = await request.post('/api/admin/tenants', {
    data: {
      key: tenantKey,
      displayName: displayName || `Tenant ${tenantKey}`,
    },
  });

  const data = await response.json();

  if (data.success) {
    console.log(`✅ Tenant created: ${tenantKey} (id: ${data.tenant?.id})`);
  } else {
    console.error(`❌ Failed to create tenant: ${data.message}`);
  }

  return data;
}

/**
 * 🗑️ Deletes a tenant and all associated resources:
 * - Keycloak realm
 * - Grafana organization
 * - Database records
 */
export async function deleteTenant(
  request: APIRequestContext,
  tenantKey: string
): Promise<{ success: boolean; message: string }> {
  console.log(`🗑️  Deleting tenant: ${tenantKey}...`);

  const response = await request.delete(`/api/admin/tenants/${tenantKey}`);
  const data = await response.json();

  if (data.success) {
    console.log(`✅ Tenant deleted: ${tenantKey}`);
  } else {
    console.error(`⚠️  Failed to delete tenant: ${data.message}`);
  }

  return data;
}

/**
 * 🔍 Gets tenant status including:
 * - Keycloak realm existence
 * - Database registry entry
 * - Grafana organization binding
 */
export async function getTenantStatus(
  request: APIRequestContext,
  tenantKey: string
): Promise<any> {
  const response = await request.get(`/api/admin/tenants/${tenantKey}/status`);
  return await response.json();
}

/**
 * 🧪 Verifies tenant provisioning completeness
 * 
 * Checks:
 * 1. Keycloak realm exists
 * 2. Tenant roles exist (ROLE_USER, ROLE_ADMIN)
 * 3. Grafana org + service account created
 * 4. Database binding exists
 */
export async function verifyTenantProvisioning(
  request: APIRequestContext,
  tenantKey: string
): Promise<{
  keycloakRealm: boolean;
  tenantRoles: boolean;
  grafanaBinding: boolean;
  details: any;
}> {
  console.log(`🔍 Verifying tenant provisioning: ${tenantKey}...`);

  // Get tenant status
  const status = await getTenantStatus(request, tenantKey);

  // Check Grafana binding
  let grafanaBindingExists = false;
  try {
    const bindingResponse = await request.get(`/api/monitoring/tenant-bindings/${tenantKey}`);
    grafanaBindingExists = bindingResponse.ok();
  } catch (e) {
    console.warn(`⚠️  Could not verify Grafana binding: ${e}`);
  }

  const result = {
    keycloakRealm: status.realmExists === true,
    tenantRoles: status.rolesExist === true,
    grafanaBinding: grafanaBindingExists,
    details: status,
  };

  console.log(`📊 Provisioning status:`, result);

  return result;
}
