#!/usr/bin/env node
/**
 * E2E Test Teardown Script
 * 
 * Cleans up ephemeral test data created by scaffold:
 * - Delete test entity and UI spec
 * - Delete test user
 * - Delete test tenant
 * - Clean up topics/artifacts (Kafka, MinIO)
 * 
 * Usage:
 *   node e2e/scripts/teardown.ts --env POST
 *   POST_BASE_URL=https://staging.example.com node e2e/scripts/teardown.ts
 */

import { readFileSync, existsSync, unlinkSync } from 'node:fs';
import { resolve } from 'node:path';
import { 
  createApiContext, 
  getAuthToken,
  deleteTenant,
  deleteTestUser,
  deleteTestEntity,
} from '../helpers/api.js';

interface ScaffoldResult {
  tenantId: string;
  tenantName: string;
  user: {
    id: string;
    username: string;
  };
  entity: {
    id: string;
    name: string;
  };
  workflow?: {
    id: string;
  };
  baseUrl: string;
  timestamp?: string;
}

async function teardown(): Promise<void> {
  const baseUrl = process.env.POST_BASE_URL || process.env.E2E_BASE_URL || 'https://core-platform.local';
  
  console.log('🧹 Starting E2E test teardown...');
  console.log(`   Base URL: ${baseUrl}`);
  
  // Load scaffold result
  const resultPath = resolve('e2e/.auth/scaffold-result.json');
  
  if (!existsSync(resultPath)) {
    console.warn(`⚠️  No scaffold result found at ${resultPath}`);
    console.log('   Nothing to clean up.');
    return;
  }
  
  const result: ScaffoldResult = JSON.parse(readFileSync(resultPath, 'utf-8'));
  console.log(`\n📋 Cleaning up scaffold from ${result.timestamp}:`);
  console.log(`   Tenant: ${result.tenantName}`);
  console.log(`   User: ${result.user.username}`);
  console.log(`   Entity: ${result.entity.name}`);
  
  // Get admin token
  console.log('\n1️⃣  Getting admin auth token...');
  const adminToken = await getAuthToken(
    process.env.E2E_ADMIN_USER || 'test_admin',
    process.env.E2E_ADMIN_PASS || 'Test.1234'
  );
  
  const api = await createApiContext({ baseUrl, token: adminToken });
  
  // Delete entity (includes UI spec)
  console.log('\n2️⃣  Deleting test entity...');
  try {
    await deleteTestEntity(api, result.entity.id);
    console.log(`   ✅ Entity deleted: ${result.entity.name}`);
  } catch (error: any) {
    console.warn(`   ⚠️  Failed to delete entity: ${error.message}`);
  }
  
  // Delete workflow (if exists)
  if (result.workflow) {
    console.log('\n3️⃣  Deleting test workflow...');
    try {
      // TODO: Implement workflow deletion when API is ready
      console.log(`   ⏭️  Workflow deletion not implemented yet`);
    } catch (error: any) {
      console.warn(`   ⚠️  Failed to delete workflow: ${error.message}`);
    }
  }
  
  // Delete user
  console.log('\n4️⃣  Deleting test user...');
  try {
    await deleteTestUser(api, result.user.id);
    console.log(`   ✅ User deleted: ${result.user.username}`);
  } catch (error: any) {
    console.warn(`   ⚠️  Failed to delete user: ${error.message}`);
  }
  
  // Delete tenant (cascades to related data)
  console.log('\n5️⃣  Deleting test tenant...');
  try {
    await deleteTenant(api, result.tenantId);
    console.log(`   ✅ Tenant deleted: ${result.tenantName}`);
  } catch (error: any) {
    console.warn(`   ⚠️  Failed to delete tenant: ${error.message}`);
  }
  
  // Clean up Kafka topics (via admin API)
  console.log('\n6️⃣  Cleaning up Kafka topics...');
  try {
    // TODO: Implement topic cleanup when admin API supports it
    console.log(`   ⏭️  Topic cleanup not implemented yet`);
  } catch (error: any) {
    console.warn(`   ⚠️  Failed to clean topics: ${error.message}`);
  }
  
  // Clean up MinIO artifacts (via admin API)
  console.log('\n7️⃣  Cleaning up MinIO artifacts...');
  try {
    // TODO: Implement MinIO cleanup when admin API supports it
    console.log(`   ⏭️  MinIO cleanup not implemented yet`);
  } catch (error: any) {
    console.warn(`   ⚠️  Failed to clean MinIO: ${error.message}`);
  }
  
  // Cleanup
  await api.dispose();
  
  // Remove scaffold result file
  unlinkSync(resultPath);
  console.log(`\n✅ Teardown complete! Removed ${resultPath}`);
}

// Run if called directly
if (import.meta.url === `file://${process.argv[1]}`) {
  teardown()
    .then(() => {
      console.log('\n✅ Teardown successful');
      process.exit(0);
    })
    .catch((error) => {
      console.error('\n❌ Teardown failed:', error);
      process.exit(1);
    });
}

export { teardown };
