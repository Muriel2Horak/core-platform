#!/usr/bin/env node
/**
 * E2E Test Scaffold Script
 * 
 * Creates ephemeral test data for POST-DEPLOY E2E tests:
 * - Test tenant
 * - Test user with roles
 * - Test entity (PersonTest_${rand})
 * - Test workflow
 * 
 * Usage:
 *   node e2e/scripts/scaffold.ts --env POST
 *   POST_BASE_URL=https://staging.example.com node e2e/scripts/scaffold.ts
 */

import { writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { 
  createApiContext, 
  getAuthToken,
  createTenant,
  createTestUser,
  createTestEntity,
  publishUISpec,
} from '../helpers/api.js';

interface ScaffoldResult {
  tenantId: string;
  tenantName: string;
  user: {
    id: string;
    username: string;
    password: string;
  };
  entity: {
    id: string;
    name: string;
    version: string;
  };
  workflow?: {
    id: string;
    name: string;
  };
  baseUrl: string;
  timestamp: string;
}

async function scaffold(): Promise<ScaffoldResult> {
  const baseUrl = process.env.POST_BASE_URL || process.env.E2E_BASE_URL || 'https://core-platform.local';
  const rand = Date.now().toString(36);
  
  console.log('🚀 Starting E2E test scaffold...');
  console.log(`   Base URL: ${baseUrl}`);
  console.log(`   Random ID: ${rand}`);
  
  // Get admin token
  console.log('\n1️⃣  Getting admin auth token...');
  const adminToken = await getAuthToken(
    process.env.E2E_ADMIN_USER || 'test_admin',
    process.env.E2E_ADMIN_PASS || 'Test.1234'
  );
  
  const api = await createApiContext({ baseUrl, token: adminToken });
  
  // Create ephemeral tenant
  console.log('\n2️⃣  Creating ephemeral tenant...');
  const tenantName = `test_tenant_${rand}`;
  const tenant = await createTenant(api, tenantName, {
    description: 'E2E test tenant - AUTO CLEANUP',
    ephemeral: true,
  });
  console.log(`   ✅ Tenant created: ${tenant.id} (${tenant.name})`);
  
  // Create test user
  console.log('\n3️⃣  Creating test user...');
  const username = `testuser_${rand}`;
  const password = `Test.${rand}`;
  const user = await createTestUser(api, tenant.id, username, ['USER', 'ADMIN']);
  console.log(`   ✅ User created: ${user.id} (${username})`);
  console.log(`      Password: ${password}`);
  
  // Create test entity
  console.log('\n4️⃣  Creating test entity...');
  const entityName = `PersonTest_${rand}`;
  const entity = await createTestEntity(api, tenant.id, entityName, [
    { name: 'firstName', type: 'STRING', required: true },
    { name: 'lastName', type: 'STRING', required: true },
    { name: 'email', type: 'STRING', required: false },
    { name: 'age', type: 'INTEGER', required: false },
  ]);
  console.log(`   ✅ Entity created: ${entity.id} (${entity.name})`);
  
  // Publish UI Spec
  console.log('\n5️⃣  Publishing UI Spec...');
  const uiSpec = {
    entity: entityName,
    menu: {
      label: 'Test Persons',
      icon: 'users',
      order: 100,
    },
    grid: {
      columns: [
        { field: 'firstName', header: 'First Name', sortable: true },
        { field: 'lastName', header: 'Last Name', sortable: true },
        { field: 'email', header: 'Email', sortable: false },
      ],
    },
    form: {
      fields: [
        { name: 'firstName', label: 'First Name', type: 'text', required: true },
        { name: 'lastName', label: 'Last Name', type: 'text', required: true },
        { name: 'email', label: 'Email', type: 'email', required: false },
        { name: 'age', label: 'Age', type: 'number', required: false },
      ],
    },
  };
  
  const specVersion = await publishUISpec(api, entity.id, uiSpec);
  console.log(`   ✅ UI Spec published: v${specVersion.version}`);
  
  // Workflow creation skipped - workflows are created via Studio UI
  console.log('\n6️⃣  Workflow creation skipped (use Studio UI for complex workflows)');
  
  // Cleanup
  await api.dispose();
  
  // Build result
  const result: ScaffoldResult = {
    tenantId: tenant.id,
    tenantName: tenant.name,
    user: {
      id: user.id,
      username,
      password,
    },
    entity: {
      id: entity.id,
      name: entity.name,
      version: specVersion.version,
    },
    baseUrl,
    timestamp: new Date().toISOString(),
  };
  
  // Save to file
  const outputPath = resolve('e2e/.auth/scaffold-result.json');
  writeFileSync(outputPath, JSON.stringify(result, null, 2));
  console.log(`\n✅ Scaffold complete! Result saved to: ${outputPath}`);
  console.log('\n📋 Summary:');
  console.log(JSON.stringify(result, null, 2));
  
  return result;
}

// Run if called directly
if (import.meta.url === `file://${process.argv[1]}`) {
  scaffold()
    .then(() => {
      console.log('\n✅ Scaffold successful');
      process.exit(0);
    })
    .catch((error) => {
      console.error('\n❌ Scaffold failed:', error);
      process.exit(1);
    });
}

export { scaffold };
