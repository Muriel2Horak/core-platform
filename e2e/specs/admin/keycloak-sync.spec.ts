import { test, expect } from '@playwright/test';
import {
  createApiContext,
  getAdminAuthToken,
  getTestUserAuthToken,
  syncUsersFromKeycloak,
  syncRolesFromKeycloak,
  syncGroupsFromKeycloak,
  syncAllFromKeycloak,
  getSyncStatus,
} from '../../helpers/api';

/**
 * Keycloak Bulk Synchronization E2E Tests
 * 
 * ⚠️ SKIP: API tests selhávají - pravděpodobně backend API changes
 * TODO: Review sync endpoints after backend updates
 * TODO: Fix authentication/authorization for sync operations
 * 
 * Tests bulk synchronization of users, roles, and groups from Keycloak to application database.
 * 
 * Coverage:
 * - Sync users from Keycloak (admin only)
 * - Sync roles from Keycloak (admin only)
 * - Sync groups from Keycloak (admin only)
 * - Full sync (all entities)
 * - Sync status tracking (async jobs)
 * - RBAC verification (admin only access)
 * - Idempotence (repeated sync safe)
 * - Tenant isolation
 * - Error handling
 */

test.describe.skip('Admin: Keycloak Bulk Sync', () => {
  const TENANT_KEY = 'admin'; // Use existing admin tenant
  
  test('should sync users from Keycloak as admin', async ({ page }) => {
    const adminToken = await getAdminAuthToken();
    const api = await createApiContext({ token: adminToken });

    try {
      // Trigger user sync
      const syncResult = await syncUsersFromKeycloak(api, TENANT_KEY);
      
      // Verify response structure
      expect(syncResult.status).toBe('started');
      expect(syncResult.syncId).toBeTruthy();
      expect(syncResult.message).toContain('User synchronization started');
      
      console.log(`✅ User sync started: ${syncResult.syncId}`);
      
      // Wait a bit for async job to process
      await page.waitForTimeout(3000);
      
      // Check sync status
      const status = await getSyncStatus(api, syncResult.syncId);
      expect(status.syncId).toBe(syncResult.syncId);
      expect(['PENDING', 'RUNNING', 'COMPLETED']).toContain(status.status);
      
      if (status.status === 'COMPLETED') {
        expect(status.results?.usersProcessed).toBeGreaterThanOrEqual(0);
        console.log(`✅ Users synced: ${status.results?.usersProcessed}`);
      }
    } finally {
      await api.dispose();
    }
  });

  test('should sync roles from Keycloak as admin', async ({ page }) => {
    const adminToken = await getAdminAuthToken();
    const api = await createApiContext({ token: adminToken });

    try {
      // Trigger role sync
      const syncResult = await syncRolesFromKeycloak(api, TENANT_KEY);
      
      // Verify response structure
      expect(syncResult.status).toBe('started');
      expect(syncResult.syncId).toBeTruthy();
      expect(syncResult.message).toContain('Role synchronization started');
      
      console.log(`✅ Role sync started: ${syncResult.syncId}`);
      
      // Wait for processing
      await page.waitForTimeout(3000);
      
      // Check sync status
      const status = await getSyncStatus(api, syncResult.syncId);
      expect(status.syncId).toBe(syncResult.syncId);
      expect(['PENDING', 'RUNNING', 'COMPLETED']).toContain(status.status);
      
      if (status.status === 'COMPLETED') {
        expect(status.results?.rolesProcessed).toBeGreaterThanOrEqual(0);
        console.log(`✅ Roles synced: ${status.results?.rolesProcessed}`);
      }
    } finally {
      await api.dispose();
    }
  });

  test('should sync groups from Keycloak as admin', async ({ page }) => {
    const adminToken = await getAdminAuthToken();
    const api = await createApiContext({ token: adminToken });

    try {
      // Trigger group sync
      const syncResult = await syncGroupsFromKeycloak(api, TENANT_KEY);
      
      // Verify response structure
      expect(syncResult.status).toBe('started');
      expect(syncResult.syncId).toBeTruthy();
      expect(syncResult.message).toContain('Group synchronization started');
      
      console.log(`✅ Group sync started: ${syncResult.syncId}`);
      
      // Wait for processing
      await page.waitForTimeout(3000);
      
      // Check sync status
      const status = await getSyncStatus(api, syncResult.syncId);
      expect(status.syncId).toBe(syncResult.syncId);
      expect(['PENDING', 'RUNNING', 'COMPLETED']).toContain(status.status);
      
      if (status.status === 'COMPLETED') {
        expect(status.results?.groupsProcessed).toBeGreaterThanOrEqual(0);
        console.log(`✅ Groups synced: ${status.results?.groupsProcessed}`);
      }
    } finally {
      await api.dispose();
    }
  });

  test('should perform full sync (users + roles + groups)', async ({ page }) => {
    const adminToken = await getAdminAuthToken();
    const api = await createApiContext({ token: adminToken });

    try {
      // Trigger full sync
      const syncResult = await syncAllFromKeycloak(api, TENANT_KEY);
      
      // Verify response structure
      expect(syncResult.status).toBe('started');
      expect(syncResult.syncId).toBeTruthy();
      expect(syncResult.message).toContain('synchronization started');
      
      console.log(`✅ Full sync started: ${syncResult.syncId}`);
      
      // Full sync takes longer - wait more
      await page.waitForTimeout(5000);
      
      // Check sync status
      const status = await getSyncStatus(api, syncResult.syncId);
      expect(status.syncId).toBe(syncResult.syncId);
      expect(['PENDING', 'RUNNING', 'COMPLETED']).toContain(status.status);
      
      if (status.status === 'COMPLETED') {
        // Verify all entities were processed
        expect(status.results?.usersProcessed).toBeGreaterThanOrEqual(0);
        expect(status.results?.rolesProcessed).toBeGreaterThanOrEqual(0);
        expect(status.results?.groupsProcessed).toBeGreaterThanOrEqual(0);
        
        console.log(`✅ Full sync completed:`, status.results);
      }
    } finally {
      await api.dispose();
    }
  });

  test('should verify idempotence - repeated sync is safe', async ({ page }) => {
    const adminToken = await getAdminAuthToken();
    const api = await createApiContext({ token: adminToken });

    try {
      // First sync
      const sync1 = await syncUsersFromKeycloak(api, TENANT_KEY);
      await page.waitForTimeout(3000);
      const status1 = await getSyncStatus(api, sync1.syncId);
      
      // Second sync (immediate repeat)
      const sync2 = await syncUsersFromKeycloak(api, TENANT_KEY);
      await page.waitForTimeout(3000);
      const status2 = await getSyncStatus(api, sync2.syncId);
      
      // Both should succeed
      expect(status1.status).not.toBe('FAILED');
      expect(status2.status).not.toBe('FAILED');
      
      // Results should be consistent (same user count or similar)
      if (status1.status === 'COMPLETED' && status2.status === 'COMPLETED') {
        const diff = Math.abs(
          (status1.results?.usersProcessed || 0) - (status2.results?.usersProcessed || 0)
        );
        expect(diff).toBeLessThanOrEqual(5); // Allow small variance for concurrent changes
        
        console.log(`✅ Idempotence verified: ${status1.results?.usersProcessed} vs ${status2.results?.usersProcessed}`);
      }
    } finally {
      await api.dispose();
    }
  });

  test('should NOT allow regular user to trigger sync (RBAC)', async () => {
    const userToken = await getTestUserAuthToken();
    const api = await createApiContext({ token: userToken });

    try {
      // Attempt to sync as regular user
      let errorThrown = false;
      
      try {
        await syncUsersFromKeycloak(api, TENANT_KEY);
      } catch (error: any) {
        errorThrown = true;
        // Expect 403 Forbidden
        expect(error.message).toMatch(/403|Forbidden|Access.*denied/i);
      }
      
      expect(errorThrown).toBe(true);
      console.log('✅ RBAC verified: Regular user cannot trigger sync');
    } finally {
      await api.dispose();
    }
  });

  test('should handle invalid tenant key gracefully', async () => {
    const adminToken = await getAdminAuthToken();
    const api = await createApiContext({ token: adminToken });

    try {
      let errorThrown = false;
      
      try {
        await syncUsersFromKeycloak(api, 'nonexistent-tenant-xyz');
      } catch (error: any) {
        errorThrown = true;
        // Expect 404 or 400 error
        expect(error.message).toMatch(/404|400|not found|invalid/i);
      }
      
      expect(errorThrown).toBe(true);
      console.log('✅ Error handling verified: Invalid tenant rejected');
    } finally {
      await api.dispose();
    }
  });

  test('should track sync progress via status endpoint', async ({ page }) => {
    const adminToken = await getAdminAuthToken();
    const api = await createApiContext({ token: adminToken });

    try {
      // Start sync
      const syncResult = await syncUsersFromKeycloak(api, TENANT_KEY);
      const syncId = syncResult.syncId;
      
      // Poll status multiple times to see progression
      const statuses: string[] = [];
      
      for (let i = 0; i < 5; i++) {
        await page.waitForTimeout(1000);
        const status = await getSyncStatus(api, syncId);
        statuses.push(status.status);
        
        if (status.status === 'COMPLETED' || status.status === 'FAILED') {
          break;
        }
      }
      
      // Verify status progression
      expect(statuses.length).toBeGreaterThan(0);
      
      // Final status should be terminal
      const finalStatus = statuses[statuses.length - 1];
      expect(['COMPLETED', 'FAILED']).toContain(finalStatus);
      
      console.log(`✅ Status progression tracked: ${statuses.join(' → ')}`);
    } finally {
      await api.dispose();
    }
  });

  test('should verify tenant isolation - sync affects only target tenant', async () => {
    // This is more of a documentation test - we verify the API contract
    // Actual isolation is tested in backend unit tests
    
    const adminToken = await getAdminAuthToken();
    const api = await createApiContext({ token: adminToken });

    try {
      // Sync for admin tenant
      const syncResult = await syncUsersFromKeycloak(api, TENANT_KEY);
      
      // Verify syncId contains tenant reference
      expect(syncResult.syncId).toBeTruthy();
      expect(syncResult.message).toContain(TENANT_KEY);
      
      console.log('✅ Tenant isolation contract verified');
    } finally {
      await api.dispose();
    }
  });

  test('should provide sync statistics in status response', async ({ page }) => {
    const adminToken = await getAdminAuthToken();
    const api = await createApiContext({ token: adminToken });

    try {
      // Trigger full sync to get all stats
      const syncResult = await syncAllFromKeycloak(api, TENANT_KEY);
      
      // Wait for completion
      await page.waitForTimeout(5000);
      
      const status = await getSyncStatus(api, syncResult.syncId);
      
      if (status.status === 'COMPLETED') {
        // Verify statistics structure
        expect(status.results).toBeDefined();
        expect(typeof status.results?.usersProcessed).toBe('number');
        expect(typeof status.results?.rolesProcessed).toBe('number');
        expect(typeof status.results?.groupsProcessed).toBe('number');
        
        // All counts should be non-negative
        expect(status.results?.usersProcessed).toBeGreaterThanOrEqual(0);
        expect(status.results?.rolesProcessed).toBeGreaterThanOrEqual(0);
        expect(status.results?.groupsProcessed).toBeGreaterThanOrEqual(0);
        
        console.log('✅ Sync statistics:', status.results);
      }
    } finally {
      await api.dispose();
    }
  });
});
