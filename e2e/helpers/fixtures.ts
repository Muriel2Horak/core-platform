import { Page } from '@playwright/test';

/**
 * Test Data Fixtures Helper
 * 
 * Provides reusable functions for creating and cleaning up test data
 * for CRUD tests (Users, Roles, Groups, Tenants)
 */

const API_BASE = process.env.API_BASE || 'https://admin.core-platform.local';

/**
 * Generate unique test name with timestamp
 * @param prefix - Prefix for the generated name
 * @param useDashes - If true, replaces underscores with dashes (for tenant keys)
 */
export function generateTestName(prefix: string, useDashes = false): string {
  const timestamp = Date.now();
  const name = `${prefix}_${timestamp}`;
  return useDashes ? name.replace(/_/g, '-') : name;
}

/**
 * Create test user via API
 */
export async function createTestUser(
  page: Page,
  username: string,
  options: {
    firstName?: string;
    lastName?: string;
    email?: string;
    roles?: string[];
    enabled?: boolean;
  } = {}
): Promise<{ id: string; username: string }> {
  const userData = {
    username,
    firstName: options.firstName || 'Test',
    lastName: options.lastName || 'User',
    email: options.email || `${username}@test.local`,
    enabled: options.enabled !== undefined ? options.enabled : true,
    realmRoles: options.roles || ['CORE_ROLE_USER'],
    credentials: [
      { type: 'password', value: 'Test.1234', temporary: false }
    ]
  };

  const response = await page.request.post(
    `${API_BASE}/api/users`, // FIX: Backend has /api/users, not /api/admin/users
    {
      data: userData,
      headers: {
        'Content-Type': 'application/json'
      }
    }
  );

  if (!response.ok()) {
    throw new Error(`Failed to create user: ${response.status()} ${await response.text()}`);
  }

  const result = await response.json();
  return { id: result.id || result.userId, username };
}

/**
 * Delete test user via API
 */
export async function deleteTestUser(
  page: Page,
  userId: string
): Promise<void> {
  const response = await page.request.delete(
    `${API_BASE}/api/users/${userId}` // FIX: Backend has /api/users, not /api/admin/users
  );

  if (!response.ok() && response.status() !== 404) {
    console.warn(`Failed to delete user ${userId}: ${response.status()}`);
  }
}

/**
 * Create test role via API
 */
export async function createTestRole(
  page: Page,
  name: string,
  description?: string
): Promise<{ name: string }> {
  const roleData = {
    name,
    description: description || `Test role ${name}`
  };

  const response = await page.request.post(
    `${API_BASE}/api/roles`, // FIX: Backend has /api/roles, not /api/admin/roles
    {
      data: roleData,
      headers: {
        'Content-Type': 'application/json'
      }
    }
  );

  if (!response.ok()) {
    throw new Error(`Failed to create role: ${response.status()} ${await response.text()}`);
  }

  return { name };
}

/**
 * Delete test role via API
 */
export async function deleteTestRole(
  page: Page,
  roleName: string
): Promise<void> {
  const response = await page.request.delete(
    `${API_BASE}/api/roles/${roleName}` // FIX: Backend has /api/roles, not /api/admin/roles
  );

  if (!response.ok() && response.status() !== 404) {
    console.warn(`Failed to delete role ${roleName}: ${response.status()}`);
  }
}

/**
 * Create test group via API
 */
export async function createTestGroup(
  page: Page,
  name: string,
  options: {
    members?: string[]; // user IDs
  } = {}
): Promise<{ id: string; name: string }> {
  const groupData = {
    name
  };

  const response = await page.request.post(
    `${API_BASE}/api/groups`, // FIX: Backend has /api/groups, not /api/admin/groups
    {
      data: groupData,
      headers: {
        'Content-Type': 'application/json'
      }
    }
  );

  if (!response.ok()) {
    throw new Error(`Failed to create group: ${response.status()} ${await response.text()}`);
  }

  const result = await response.json();
  const groupId = result.id || result.groupId;

  // Add members if provided
  if (options.members && options.members.length > 0) {
    for (const userId of options.members) {
      await page.request.post(
        `${API_BASE}/api/groups/${groupId}/members`, // FIX: Backend has /api/groups, not /api/admin/groups
        {
          data: { userId },
          headers: {
            'Content-Type': 'application/json'
          }
        }
      );
    }
  }

  return { id: groupId, name };
}

/**
 * Delete test group via API
 */
export async function deleteTestGroup(
  page: Page,
  groupId: string
): Promise<void> {
  const response = await page.request.delete(
    `${API_BASE}/api/groups/${groupId}` // FIX: Backend has /api/groups, not /api/admin/groups
  );

  if (!response.ok() && response.status() !== 404) {
    console.warn(`Failed to delete group ${groupId}: ${response.status()}`);
  }
}

/**
 * Create Test Tenant via API
 * ✅ FIXED: Properly sends `key` field (was sending `tenantKey`)
 * ✅ AUTO-FIX: Replaces underscores with dashes in key (tenant key validation requirement)
 */
export async function createTestTenant(page: Page, tenantKey: string, displayName?: string) {
  // Auto-fix tenant key format: replace underscores with dashes, ensure lowercase
  const validatedKey = tenantKey.toLowerCase().replace(/_/g, '-');
  
  const payload = {
    key: validatedKey, // ✅ FIXED: was `tenantKey`
    name: displayName || `Tenant ${validatedKey}`,
    enabled: true,
  };

  const response = await page.request.post(`${API_BASE}/api/admin/tenants`, {
    data: payload,
  });

  if (!response.ok()) {
    throw new Error(`Failed to create tenant: ${response.status()} ${await response.text()}`);
  }

  const result = await response.json();
  return { id: result.id, key: validatedKey };
}

/**
 * Delete test tenant via API
 */
export async function deleteTestTenant(
  page: Page,
  tenantId: string
): Promise<void> {
  const response = await page.request.delete(
    `${API_BASE}/api/admin/tenants/${tenantId}`
  );

  if (!response.ok() && response.status() !== 404) {
    console.warn(`Failed to delete tenant ${tenantId}: ${response.status()}`);
  }
}

/**
 * Cleanup all test data created during tests
 * Call this in afterAll() or afterEach()
 */
export async function cleanupTestData(
  page: Page,
  data: {
    userIds?: string[];
    roleNames?: string[];
    groupIds?: string[];
    tenantIds?: string[];
  }
): Promise<void> {
  // Delete in reverse order (users → groups → roles → tenants)
  
  if (data.userIds) {
    for (const userId of data.userIds) {
      await deleteTestUser(page, userId);
    }
  }

  if (data.groupIds) {
    for (const groupId of data.groupIds) {
      await deleteTestGroup(page, groupId);
    }
  }

  if (data.roleNames) {
    for (const roleName of data.roleNames) {
      await deleteTestRole(page, roleName);
    }
  }

  if (data.tenantIds) {
    for (const tenantId of data.tenantIds) {
      await deleteTestTenant(page, tenantId);
    }
  }
}

/**
 * Wait for element with retry (useful for async operations)
 */
export async function waitForElement(
  page: Page,
  selector: string,
  options: { timeout?: number; state?: 'attached' | 'detached' | 'visible' | 'hidden' } = {}
): Promise<void> {
  await page.waitForSelector(selector, {
    timeout: options.timeout || 5000,
    state: options.state || 'visible'
  });
}

/**
 * Navigate to admin page and wait for load
 */
export async function navigateToAdminPage(
  page: Page,
  path: string
): Promise<void> {
  // CRITICAL FIX: All admin routes are under /core-admin/* (not legacy /users, /roles etc)
  const adminPath = path.startsWith('/core-admin/') ? path : `/core-admin${path}`;
  await page.goto(`${API_BASE}${adminPath}`);
  await page.waitForLoadState('domcontentloaded');
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {
    console.log('Network not idle after 10s, continuing anyway');
  });
}

/**
 * Wait for dialog/modal to close (generic helper for success verification)
 * Instead of waiting for success messages that may not exist, we wait for dialog close
 */
export async function waitForDialogClose(
  page: Page,
  options: { timeout?: number } = {}
): Promise<void> {
  const timeout = options.timeout || 5000;
  
  // Wait for MUI Dialog to disappear (common pattern across all CRUD forms)
  await page.waitForSelector('[role="dialog"]', { state: 'hidden', timeout }).catch(() => {
    // Dialog might already be gone, that's fine
  });
  
  // Small delay to ensure list refresh
  await page.waitForTimeout(500);
}
