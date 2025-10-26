/**
 * API Helper
 * 
 * Thin wrappers around admin and public APIs.
 * NO DIRECT DB ACCESS - only via public/admin endpoints.
 */

import { APIRequestContext, request } from '@playwright/test';
import { readE2EConfig } from '../config/read-config.js';

export interface ApiOptions {
  baseUrl?: string;
  token?: string;
}

/**
 * Create API request context
 */
export async function createApiContext(options: ApiOptions = {}): Promise<APIRequestContext> {
  const config = readE2EConfig();
  
  return await request.newContext({
    baseURL: options.baseUrl || config.baseUrl,
    ignoreHTTPSErrors: config.ignoreTLS,
    extraHTTPHeaders: options.token ? {
      'Authorization': `Bearer ${options.token}`,
    } : {},
  });
}

/**
 * Get auth token for API calls
 * Uses Keycloak direct grant (password flow) for tests
 */
export async function getAuthToken(
  username?: string,
  password?: string
): Promise<string> {
  const config = readE2EConfig();
  
  const user = username || config.testUser.username;
  const pass = password || config.testUser.password;
  
  const api = await createApiContext();
  
  const response = await api.post(`${config.keycloak.authServerUrl}/realms/${config.keycloak.realm}/protocol/openid-connect/token`, {
    form: {
      grant_type: 'password',
      client_id: config.keycloak.clientId,
      username: user,
      password: pass,
    },
  });
  
  if (!response.ok()) {
    throw new Error(`Failed to get auth token: ${response.status()} ${await response.text()}`);
  }
  
  const data = await response.json();
  await api.dispose();
  
  return data.access_token;
}

/**
 * Get admin auth token (test_admin user)
 */
export async function getAdminAuthToken(): Promise<string> {
  return getAuthToken('test_admin', 'Test.1234');
}

/**
 * Get test user auth token (test user)
 */
export async function getTestUserAuthToken(): Promise<string> {
  return getAuthToken('test', 'Test.1234');
}

// ============================================================================
// ADMIN APIs
// ============================================================================

/**
 * Create ephemeral tenant via Admin API
 */
export async function createTenant(
  api: APIRequestContext,
  name: string,
  config?: Record<string, unknown>
): Promise<{ id: string; name: string }> {
  const response = await api.post('/api/admin/tenants', {
    data: {
      name,
      config: config || {},
      isTest: true, // Flag for cleanup
    },
  });
  
  if (!response.ok()) {
    throw new Error(`Failed to create tenant: ${response.status()} ${await response.text()}`);
  }
  
  return await response.json();
}

/**
 * Delete tenant via Admin API
 */
export async function deleteTenant(
  api: APIRequestContext,
  tenantId: string
): Promise<void> {
  const response = await api.delete(`/api/admin/tenants/${tenantId}`);
  
  if (!response.ok() && response.status() !== 404) {
    throw new Error(`Failed to delete tenant: ${response.status()} ${await response.text()}`);
  }
}

/**
 * Create test user via Admin API
 */
export async function createTestUser(
  api: APIRequestContext,
  tenantId: string,
  username: string,
  roles: string[] = []
): Promise<{ id: string; username: string }> {
  const response = await api.post(`/api/admin/tenants/${tenantId}/users`, {
    data: {
      username,
      email: `${username}@test.local`,
      firstName: 'Test',
      lastName: 'User',
      roles,
      isTest: true,
    },
  });
  
  if (!response.ok()) {
    throw new Error(`Failed to create user: ${response.status()} ${await response.text()}`);
  }
  
  return await response.json();
}

/**
 * Delete test user via Admin API
 */
export async function deleteTestUser(
  api: APIRequestContext,
  userId: string
): Promise<void> {
  const response = await api.delete(`/api/admin/users/${userId}`);
  
  if (!response.ok() && response.status() !== 404) {
    throw new Error(`Failed to delete user: ${response.status()} ${await response.text()}`);
  }
}

/**
 * Create test entity definition via Admin API
 */
export async function createTestEntity(
  api: APIRequestContext,
  tenantId: string,
  entityName: string,
  fields: Array<{ name: string; type: string; required?: boolean }>
): Promise<{ id: string; name: string; version: string }> {
  const response = await api.post(`/api/admin/tenants/${tenantId}/entities`, {
    data: {
      name: entityName,
      fields,
      isTest: true,
    },
  });
  
  if (!response.ok()) {
    throw new Error(`Failed to create entity: ${response.status()} ${await response.text()}`);
  }
  
  return await response.json();
}

/**
 * Delete test entity via Admin API
 */
export async function deleteTestEntity(
  api: APIRequestContext,
  entityId: string
): Promise<void> {
  const response = await api.delete(`/api/admin/entities/${entityId}`);
  
  if (!response.ok() && response.status() !== 404) {
    throw new Error(`Failed to delete entity: ${response.status()} ${await response.text()}`);
  }
}

/**
 * Publish UI Spec via Admin API
 */
export async function publishUISpec(
  api: APIRequestContext,
  entityId: string,
  spec: Record<string, unknown>
): Promise<{ version: string }> {
  const response = await api.post(`/api/admin/entities/${entityId}/ui-spec/publish`, {
    data: spec,
  });
  
  if (!response.ok()) {
    throw new Error(`Failed to publish UI spec: ${response.status()} ${await response.text()}`);
  }
  
  return await response.json();
}

// ============================================================================
// PUBLIC APIs (same as GUI uses)
// ============================================================================

/**
 * Get current user profile
 */
export async function getCurrentUser(api: APIRequestContext): Promise<any> {
  const response = await api.get('/api/me');
  
  if (!response.ok()) {
    throw new Error(`Failed to get current user: ${response.status()}`);
  }
  
  return await response.json();
}

/**
 * Update user profile
 */
export async function updateUserProfile(
  api: APIRequestContext,
  updates: Record<string, unknown>
): Promise<any> {
  const response = await api.patch('/api/me', {
    data: updates,
  });
  
  if (!response.ok()) {
    throw new Error(`Failed to update profile: ${response.status()} ${await response.text()}`);
  }
  
  return await response.json();
}

/**
 * Search users in directory (public API)
 */
export async function searchUsers(
  api: APIRequestContext,
  query: string
): Promise<any[]> {
  const response = await api.get(`/api/user-directories?q=${encodeURIComponent(query)}`);
  
  if (!response.ok()) {
    throw new Error(`Failed to search users: ${response.status()}`);
  }
  
  return await response.json();
}

/**
 * Check user changes via CDC (Change Data Capture) endpoint
 * @param api - API request context
 * @param since - Optional timestamp to check for changes since
 * @returns CDC response with timestamp and hasChanges flag
 */
export async function checkUserChanges(
  api: APIRequestContext,
  since?: number
): Promise<{ timestamp: number; hasChanges: boolean; username: string; lastModified: number }> {
  const url = since ? `/api/me/changes?since=${since}` : '/api/me/changes';
  const response = await api.get(url);
  
  if (!response.ok()) {
    throw new Error(`Failed to check user changes: ${response.status()}`);
  }
  
  return await response.json();
}

/**
 * Get UI Spec for entity
 */
export async function getUISpec(
  api: APIRequestContext,
  entityName: string
): Promise<any> {
  const response = await api.get(`/api/ui-specs/${entityName}`);
  
  if (!response.ok()) {
    throw new Error(`Failed to get UI spec: ${response.status()}`);
  }
  
  return await response.json();
}

/**
 * Get entity records (grid data)
 */
export async function getEntityRecords(
  api: APIRequestContext,
  entityName: string,
  params?: { page?: number; size?: number; filter?: string }
): Promise<any> {
  const query = new URLSearchParams({
    page: String(params?.page || 0),
    size: String(params?.size || 20),
    ...(params?.filter && { filter: params.filter }),
  });
  
  const response = await api.get(`/api/entities/${entityName}?${query}`);
  
  if (!response.ok()) {
    throw new Error(`Failed to get entity records: ${response.status()}`);
  }
  
  return await response.json();
}

/**
 * Create entity record
 */
export async function createEntityRecord(
  api: APIRequestContext,
  entityName: string,
  data: Record<string, unknown>
): Promise<any> {
  const response = await api.post(`/api/entities/${entityName}`, {
    data,
  });
  
  if (!response.ok()) {
    throw new Error(`Failed to create record: ${response.status()} ${await response.text()}`);
  }
  
  return await response.json();
}

/**
 * Get workflow state for entity record
 */
export async function getWorkflowState(
  api: APIRequestContext,
  entityName: string,
  recordId: string
): Promise<any> {
  const response = await api.get(`/api/entities/${entityName}/${recordId}/workflow`);
  
  if (!response.ok()) {
    throw new Error(`Failed to get workflow state: ${response.status()}`);
  }
  
  return await response.json();
}

/**
 * Execute workflow transition
 */
export async function executeWorkflowTransition(
  api: APIRequestContext,
  entityName: string,
  recordId: string,
  transitionId: string,
  data?: Record<string, unknown>
): Promise<any> {
  const response = await api.post(`/api/entities/${entityName}/${recordId}/workflow/execute`, {
    data: {
      transitionId,
      ...data,
    },
  });
  
  if (!response.ok()) {
    throw new Error(`Failed to execute transition: ${response.status()} ${await response.text()}`);
  }
  
  return await response.json();
}

// ============================================================================
// KEYCLOAK SYNC APIs (Admin only)
// ============================================================================

/**
 * Trigger bulk sync of users from Keycloak for a tenant
 * @returns Sync job ID for status tracking
 */
export async function syncUsersFromKeycloak(
  api: APIRequestContext,
  tenantKey: string
): Promise<{ status: string; syncId: string; message: string }> {
  const response = await api.post(`/api/admin/keycloak-sync/users/${tenantKey}`);
  
  if (!response.ok() && response.status() !== 202) {
    throw new Error(`Failed to sync users: ${response.status()} ${await response.text()}`);
  }
  
  return await response.json();
}

/**
 * Trigger bulk sync of roles from Keycloak for a tenant
 * @returns Sync job ID for status tracking
 */
export async function syncRolesFromKeycloak(
  api: APIRequestContext,
  tenantKey: string
): Promise<{ status: string; syncId: string; message: string }> {
  const response = await api.post(`/api/admin/keycloak-sync/roles/${tenantKey}`);
  
  if (!response.ok() && response.status() !== 202) {
    throw new Error(`Failed to sync roles: ${response.status()} ${await response.text()}`);
  }
  
  return await response.json();
}

/**
 * Trigger bulk sync of groups from Keycloak for a tenant
 * @returns Sync job ID for status tracking
 */
export async function syncGroupsFromKeycloak(
  api: APIRequestContext,
  tenantKey: string
): Promise<{ status: string; syncId: string; message: string }> {
  const response = await api.post(`/api/admin/keycloak-sync/groups/${tenantKey}`);
  
  if (!response.ok() && response.status() !== 202) {
    throw new Error(`Failed to sync groups: ${response.status()} ${await response.text()}`);
  }
  
  return await response.json();
}

/**
 * Trigger full sync (users + roles + groups) from Keycloak for a tenant
 * @returns Sync job ID for status tracking
 */
export async function syncAllFromKeycloak(
  api: APIRequestContext,
  tenantKey: string
): Promise<{ status: string; syncId: string; message: string }> {
  const response = await api.post(`/api/admin/keycloak-sync/all/${tenantKey}`);
  
  if (!response.ok() && response.status() !== 202) {
    throw new Error(`Failed to sync all: ${response.status()} ${await response.text()}`);
  }
  
  return await response.json();
}

/**
 * Get sync job status
 * @returns Sync job status with progress and results
 */
export async function getSyncStatus(
  api: APIRequestContext,
  syncId: string
): Promise<{
  syncId: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  progress?: number;
  results?: {
    usersProcessed?: number;
    rolesProcessed?: number;
    groupsProcessed?: number;
    errors?: string[];
  };
}> {
  const response = await api.get(`/api/admin/keycloak-sync/status/${syncId}`);
  
  if (!response.ok()) {
    throw new Error(`Failed to get sync status: ${response.status()} ${await response.text()}`);
  }
  
  return await response.json();
}
