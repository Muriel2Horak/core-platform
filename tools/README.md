# Grafana Organization & Service Account Management

CLI tool for managing Grafana organizations and service account tokens for tenant-based reporting.

## Setup

```bash
cd tools
npm install
```

## Usage

### 1. Create Organization

Create a new Grafana organization for a tenant:

```bash
export GRAFANA_ADMIN_TOKEN=<your-admin-token>
npx tsx grafana-org-admin.ts create-org --name test-tenant
```

Output:
```
✅ Organization created: test-tenant (ID: 2)
📝 Next: Create service account with:
   npx tsx tools/grafana-org-admin.ts create-sa --org 2 --name report-viewer
```

### 2. Create Service Account Token

Create a service account and generate an access token:

```bash
npx tsx grafana-org-admin.ts create-sa --org 2 --name report-viewer
```

Output:
```
✅ Service account created: report-viewer (ID: 1)
🔐 Token created: report-viewer-token
📋 Token (save securely): glsa_xxx...

⚠️  IMPORTANT: Save this token securely:
   GRAFANA_SAT_REPORT_VIEWER=glsa_xxx...
```

**Save the token to your secrets manager (Vault, AWS Secrets Manager, etc.)**

### 3. Generate Datasource Provisioning

Generate a datasource provisioning YAML file with X-Scope-OrgID headers:

```bash
npx tsx grafana-org-admin.ts provision-ds --org 2 --tenant test-tenant
```

Output:
```
✅ Datasource config written to: docker/grafana/provisioning/datasources/datasources-org2-test-tenant.yaml

📝 Next steps:
   1. Restart Grafana to load new datasources
   2. Add SAT to vault/secrets manager
   3. Update backend TENANT_ORG_MAP configuration
```

### 4. List All Organizations

```bash
npx tsx grafana-org-admin.ts list-orgs
```

Output:
```
📋 Grafana Organizations (3):
   1. Main Org.
   2. test-tenant
   3. acme-corp
```

### 5. Rotate Service Account Token

When a token is compromised or expiring:

```bash
npx tsx grafana-org-admin.ts rotate-sat --org 2 --name report-viewer
```

Output:
```
✅ New token created: report-viewer-token-1728567890
🗑️  Deleted old token: report-viewer-token
📋 New token (save securely): glsa_yyy...

⚠️  IMPORTANT: Update your secrets with the new token:
   GRAFANA_SAT_REPORT_VIEWER=glsa_yyy...
```

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GRAFANA_ADMIN_TOKEN` | Yes | Grafana admin API token |
| `GRAFANA_BASE_URL` | No | Grafana URL (default: http://localhost:3000) |

## Complete Workflow

Full setup for a new tenant:

```bash
# 1. Create org
npx tsx grafana-org-admin.ts create-org --name acme-corp
# Output: orgId = 3

# 2. Create service account
npx tsx grafana-org-admin.ts create-sa --org 3 --name acme-viewer
# Save token: GRAFANA_SAT_ACME_VIEWER=glsa_...

# 3. Generate datasources
npx tsx grafana-org-admin.ts provision-ds --org 3 --tenant acme-corp

# 4. Update .env
echo "GRAFANA_SAT_ACME_CORP=glsa_..." >> .env

# 5. Update backend TenantOrgServiceImpl
# Add mapping: "acme-corp" -> { orgId: 3, token: env.GRAFANA_SAT_ACME_CORP }

# 6. Restart services
docker compose restart grafana backend
```

## Security Notes

- **NEVER** commit service account tokens to Git
- Store tokens in Vault, AWS Secrets Manager, or similar
- Rotate tokens regularly (every 90 days recommended)
- Use read-only Viewer role for reporting
- Each tenant gets its own org and token for data isolation

## Troubleshooting

### Error: "GRAFANA_ADMIN_TOKEN environment variable is required"

Set the admin token:
```bash
export GRAFANA_ADMIN_TOKEN=$(docker exec grafana grafana-cli admin reset-admin-password --password-from-stdin <<< "admin123" | grep -oP 'glsa_\w+')
```

### Error: "Organization already exists"

The script handles this gracefully and returns the existing org ID.

### Datasources not showing up

1. Check file was created: `ls docker/grafana/provisioning/datasources/`
2. Restart Grafana: `docker compose restart grafana`
3. Check Grafana logs: `docker logs grafana`
