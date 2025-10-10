#!/usr/bin/env tsx
/**
 * Grafana Organization & Service Account Management CLI
 * 
 * Commands:
 *   create-org --name <TENANT>              Create new Grafana organization
 *   create-sa --org <orgId> --name <name>   Create service account token for org
 *   provision-ds --org <orgId> --tenant <ID> Generate datasource provisioning YAML
 *   list-orgs                               List all organizations
 *   rotate-sat --org <orgId>                Rotate service account token
 * 
 * Usage:
 *   npx tsx tools/grafana-org-admin.ts create-org --name test-tenant
 *   npx tsx tools/grafana-org-admin.ts create-sa --org 2 --name report-viewer
 */

import { Command } from 'commander';
import axios, { AxiosInstance } from 'axios';
import * as fs from 'fs/promises';
import * as path from 'path';
import * as yaml from 'js-yaml';

interface GrafanaConfig {
  baseUrl: string;
  adminToken: string;
}

interface Organization {
  id: number;
  name: string;
}

interface ServiceAccount {
  id: number;
  name: string;
  orgId: number;
}

interface ServiceAccountToken {
  id: number;
  name: string;
  key: string;
}

class GrafanaOrgAdmin {
  private client: AxiosInstance;

  constructor(config: GrafanaConfig) {
    this.client = axios.create({
      baseURL: config.baseUrl,
      headers: {
        'Authorization': `Bearer ${config.adminToken}`,
        'Content-Type': 'application/json',
      },
    });
  }

  async createOrg(name: string): Promise<Organization> {
    console.log(`📝 Creating organization: ${name}`);
    
    try {
      const response = await this.client.post('/api/orgs', { name });
      const org = response.data;
      
      console.log(`✅ Organization created: ${org.name} (ID: ${org.orgId})`);
      return { id: org.orgId, name: org.name };
    } catch (error: any) {
      if (error.response?.status === 409) {
        console.log(`⚠️  Organization already exists: ${name}`);
        const orgs = await this.listOrgs();
        const existing = orgs.find(o => o.name === name);
        return existing!;
      }
      throw error;
    }
  }

  async createServiceAccount(orgId: number, name: string): Promise<ServiceAccountToken> {
    console.log(`🔑 Creating service account: ${name} for org ${orgId}`);
    
    // Switch to org context
    await this.client.post(`/api/user/using/${orgId}`);
    
    // Create service account
    const saResponse = await this.client.post('/api/serviceaccounts', {
      name,
      role: 'Viewer', // Read-only for reporting
    });
    
    const serviceAccount = saResponse.data;
    console.log(`✅ Service account created: ${serviceAccount.name} (ID: ${serviceAccount.id})`);
    
    // Create token for service account
    const tokenResponse = await this.client.post(
      `/api/serviceaccounts/${serviceAccount.id}/tokens`,
      {
        name: `${name}-token`,
      }
    );
    
    const token = tokenResponse.data;
    console.log(`🔐 Token created: ${token.name}`);
    console.log(`📋 Token (save securely): ${token.key}`);
    
    return {
      id: serviceAccount.id,
      name: serviceAccount.name,
      key: token.key,
    };
  }

  async listOrgs(): Promise<Organization[]> {
    const response = await this.client.get('/api/orgs');
    return response.data;
  }

  async provisionDatasources(orgId: number, tenantId: string): Promise<void> {
    console.log(`📊 Generating datasource provisioning for org ${orgId}, tenant ${tenantId}`);
    
    const datasources = {
      apiVersion: 1,
      datasources: [
        {
          name: 'Prometheus',
          type: 'prometheus',
          uid: 'prometheus',
          access: 'proxy',
          url: 'http://prometheus:9090',
          orgId,
          isDefault: true,
          jsonData: {
            httpHeaderName1: 'X-Scope-OrgID',
          },
          secureJsonData: {
            httpHeaderValue1: tenantId,
          },
        },
        {
          name: 'Loki',
          type: 'loki',
          uid: 'loki',
          access: 'proxy',
          url: 'http://loki:3100',
          orgId,
          jsonData: {
            httpHeaderName1: 'X-Scope-OrgID',
          },
          secureJsonData: {
            httpHeaderValue1: tenantId,
          },
        },
      ],
    };
    
    const yamlContent = yaml.dump(datasources);
    const filename = `datasources-org${orgId}-${tenantId}.yaml`;
    const outputPath = path.join(process.cwd(), 'docker/grafana/provisioning/datasources', filename);
    
    await fs.writeFile(outputPath, yamlContent, 'utf-8');
    console.log(`✅ Datasource config written to: ${outputPath}`);
    console.log(`\n📝 Next steps:`);
    console.log(`   1. Restart Grafana to load new datasources`);
    console.log(`   2. Add SAT to vault/secrets manager`);
    console.log(`   3. Update backend TENANT_ORG_MAP configuration`);
  }

  async rotateToken(orgId: number, serviceAccountName: string): Promise<ServiceAccountToken> {
    console.log(`🔄 Rotating token for service account in org ${orgId}`);
    
    // Switch to org
    await this.client.post(`/api/user/using/${orgId}`);
    
    // List service accounts
    const saResponse = await this.client.get('/api/serviceaccounts/search');
    const serviceAccount = saResponse.data.serviceAccounts.find((sa: any) => sa.name === serviceAccountName);
    
    if (!serviceAccount) {
      throw new Error(`Service account ${serviceAccountName} not found in org ${orgId}`);
    }
    
    // List tokens
    const tokensResponse = await this.client.get(`/api/serviceaccounts/${serviceAccount.id}/tokens`);
    const oldTokens = tokensResponse.data;
    
    // Create new token
    const newTokenResponse = await this.client.post(
      `/api/serviceaccounts/${serviceAccount.id}/tokens`,
      {
        name: `${serviceAccountName}-token-${Date.now()}`,
      }
    );
    
    const newToken = newTokenResponse.data;
    console.log(`✅ New token created: ${newToken.name}`);
    console.log(`📋 New token (save securely): ${newToken.key}`);
    
    // Delete old tokens
    for (const token of oldTokens) {
      await this.client.delete(`/api/serviceaccounts/${serviceAccount.id}/tokens/${token.id}`);
      console.log(`🗑️  Deleted old token: ${token.name}`);
    }
    
    return {
      id: serviceAccount.id,
      name: serviceAccount.name,
      key: newToken.key,
    };
  }
}

// CLI Program
const program = new Command();

program
  .name('grafana-org-admin')
  .description('Grafana organization and service account management')
  .version('1.0.0');

program
  .command('create-org')
  .description('Create a new Grafana organization for a tenant')
  .requiredOption('--name <name>', 'Organization name (tenant ID)')
  .action(async (options) => {
    const config = getGrafanaConfig();
    const admin = new GrafanaOrgAdmin(config);
    
    try {
      const org = await admin.createOrg(options.name);
      console.log(`\n✨ Organization ready: ${org.name} (ID: ${org.id})`);
      console.log(`\n📝 Next: Create service account with:`);
      console.log(`   npx tsx tools/grafana-org-admin.ts create-sa --org ${org.id} --name report-viewer`);
    } catch (error: any) {
      console.error(`❌ Error: ${error.message}`);
      process.exit(1);
    }
  });

program
  .command('create-sa')
  .description('Create service account and token for an organization')
  .requiredOption('--org <orgId>', 'Organization ID', parseInt)
  .requiredOption('--name <name>', 'Service account name')
  .action(async (options) => {
    const config = getGrafanaConfig();
    const admin = new GrafanaOrgAdmin(config);
    
    try {
      const token = await admin.createServiceAccount(options.org, options.name);
      console.log(`\n✨ Service account ready!`);
      console.log(`\n⚠️  IMPORTANT: Save this token securely (it won't be shown again):`);
      console.log(`   GRAFANA_SAT_${options.name.toUpperCase().replace(/-/g, '_')}=${token.key}`);
      console.log(`\n📝 Next: Generate datasource provisioning with:`);
      console.log(`   npx tsx tools/grafana-org-admin.ts provision-ds --org ${options.org} --tenant <TENANT_ID>`);
    } catch (error: any) {
      console.error(`❌ Error: ${error.message}`);
      process.exit(1);
    }
  });

program
  .command('provision-ds')
  .description('Generate datasource provisioning YAML for organization')
  .requiredOption('--org <orgId>', 'Organization ID', parseInt)
  .requiredOption('--tenant <tenantId>', 'Tenant ID for X-Scope-OrgID header')
  .action(async (options) => {
    const config = getGrafanaConfig();
    const admin = new GrafanaOrgAdmin(config);
    
    try {
      await admin.provisionDatasources(options.org, options.tenant);
    } catch (error: any) {
      console.error(`❌ Error: ${error.message}`);
      process.exit(1);
    }
  });

program
  .command('list-orgs')
  .description('List all Grafana organizations')
  .action(async () => {
    const config = getGrafanaConfig();
    const admin = new GrafanaOrgAdmin(config);
    
    try {
      const orgs = await admin.listOrgs();
      console.log(`\n📋 Grafana Organizations (${orgs.length}):\n`);
      orgs.forEach(org => {
        console.log(`   ${org.id}. ${org.name}`);
      });
    } catch (error: any) {
      console.error(`❌ Error: ${error.message}`);
      process.exit(1);
    }
  });

program
  .command('rotate-sat')
  .description('Rotate service account token (creates new, deletes old)')
  .requiredOption('--org <orgId>', 'Organization ID', parseInt)
  .requiredOption('--name <saName>', 'Service account name')
  .action(async (options) => {
    const config = getGrafanaConfig();
    const admin = new GrafanaOrgAdmin(config);
    
    try {
      const token = await admin.rotateToken(options.org, options.name);
      console.log(`\n✨ Token rotated successfully!`);
      console.log(`\n⚠️  IMPORTANT: Update your secrets with the new token:`);
      console.log(`   GRAFANA_SAT_${options.name.toUpperCase().replace(/-/g, '_')}=${token.key}`);
    } catch (error: any) {
      console.error(`❌ Error: ${error.message}`);
      process.exit(1);
    }
  });

function getGrafanaConfig(): GrafanaConfig {
  const baseUrl = process.env.GRAFANA_BASE_URL || 'http://localhost:3000';
  const adminToken = process.env.GRAFANA_ADMIN_TOKEN;
  
  if (!adminToken) {
    console.error('❌ Error: GRAFANA_ADMIN_TOKEN environment variable is required');
    console.error('\nSet it with:');
    console.error('   export GRAFANA_ADMIN_TOKEN=<your-admin-token>');
    process.exit(1);
  }
  
  return { baseUrl, adminToken };
}

program.parse();
