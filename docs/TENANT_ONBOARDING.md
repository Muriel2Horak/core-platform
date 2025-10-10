# Tenant Onboarding Guide - Monitoring System

## Overview

This guide walks you through adding a new tenant to the monitoring system. Each tenant gets their own isolated Grafana organization with pre-configured datasources.

## Prerequisites

- Admin access to Grafana (https://admin.core-platform.local/monitoring)
- `CORE_ADMIN_MONITORING` role in Keycloak admin realm
- CLI tools installed: Node.js 18+, npx
- Tenant already exists in Core Platform database

## Step-by-Step Guide

### 1. Create Grafana Organization

Create a dedicated organization for the new tenant:

```bash
cd /Users/martinhorak/Projects/core-platform/docker

npx tsx tools/grafana-org-admin.ts create-org --name "acme-corp"
```

**Expected Output**:
```json
{
  "message": "Organization 'acme-corp' created",
  "orgId": 4
}
```

**Note the `orgId`** - you'll need it for the next steps.

### 2. Create Service Account

Create a service account with Viewer role for BFF proxy:

```bash
npx tsx tools/grafana-org-admin.ts create-sa \
  --org 4 \
  --name "acme-corp-viewer" \
  --role "Viewer"
```

**Expected Output**:
```json
{
  "id": 7,
  "name": "acme-corp-viewer",
  "login": "sa-acme-corp-viewer",
  "orgId": 4,
  "role": "Viewer",
  "tokens": []
}
```

**Note the service account `id`** for token creation.

### 3. Generate Service Account Token

Generate a token for the service account:

```bash
npx tsx tools/grafana-org-admin.ts create-sa \
  --org 4 \
  --name "acme-corp-viewer" \
  --role "Viewer" \
  --create-token
```

**Expected Output**:
```json
{
  "id": 7,
  "name": "acme-corp-viewer",
  "login": "sa-acme-corp-viewer",
  "orgId": 4,
  "role": "Viewer",
  "token": "glsa_xxxxxxxxxxxxxxxxxxxxxxxxxxx"
}
```

**⚠️ IMPORTANT**: Store the token securely. It won't be shown again.

### 4. Provision Datasources

Provision Prometheus and Loki datasources for the org:

```bash
npx tsx tools/grafana-org-admin.ts provision-ds \
  --org 4 \
  --prometheus-url "http://prometheus:9090" \
  --loki-url "http://loki:3100"
```

**Expected Output**:
```json
{
  "message": "Datasources provisioned for org 4",
  "datasources": [
    {
      "id": 12,
      "uid": "prometheus",
      "name": "Prometheus",
      "type": "prometheus",
      "url": "http://prometheus:9090"
    },
    {
      "id": 13,
      "uid": "loki",
      "name": "Loki",
      "type": "loki",
      "url": "http://loki:3100"
    }
  ]
}
```

### 5. Store Token in Backend Configuration

#### Option A: Environment Variables (Development)

Add to `.env`:

```bash
# Tenant: acme-corp (orgId: 4)
GRAFANA_ORG_ACME_CORP_ID=4
GRAFANA_ORG_ACME_CORP_TOKEN=glsa_xxxxxxxxxxxxxxxxxxxxxxxxxxx
```

#### Option B: Kubernetes Secrets (Production)

Create secret:

```bash
kubectl create secret generic grafana-sat-acme-corp \
  --namespace=core-platform \
  --from-literal=orgId=4 \
  --from-literal=token=glsa_xxxxxxxxxxxxxxxxxxxxxxxxxxx
```

Update backend deployment to mount secret:

```yaml
# k8s/backend/deployment.yaml
spec:
  containers:
    - name: backend
      env:
        - name: GRAFANA_ORG_ACME_CORP_ID
          valueFrom:
            secretKeyRef:
              name: grafana-sat-acme-corp
              key: orgId
        - name: GRAFANA_ORG_ACME_CORP_TOKEN
          valueFrom:
            secretKeyRef:
              name: grafana-sat-acme-corp
              key: token
```

#### Option C: HashiCorp Vault (Production - Recommended)

Store token in Vault:

```bash
vault kv put secret/grafana/orgs/acme-corp \
  orgId=4 \
  serviceAccountToken=glsa_xxxxxxxxxxxxxxxxxxxxxxxxxxx
```

Backend will automatically fetch from Vault using `VaultTenantOrgService`.

### 6. Update Tenant Mapping

Add tenant-to-org mapping in backend:

**File**: `backend/src/main/java/cz/muriel/core/monitoring/bff/service/TenantOrgServiceImpl.java`

```java
@Override
public TenantBinding resolve(Jwt jwt) {
  String tenantId = extractTenantId(jwt);
  
  return switch (tenantId) {
    case "core-platform" -> new TenantBinding(tenantId, 1, getToken("CORE_PLATFORM"));
    case "test-tenant" -> new TenantBinding(tenantId, 2, getToken("TEST_TENANT"));
    case "acme-corp" -> new TenantBinding(tenantId, 4, getToken("ACME_CORP")); // NEW
    default -> throw new IllegalArgumentException("Unknown tenant: " + tenantId);
  };
}

private String getToken(String tenant) {
  return environment.getProperty("GRAFANA_ORG_" + tenant + "_TOKEN");
}
```

**Restart backend** to load new configuration.

### 7. Verify Setup

#### Test BFF Health Check

```bash
curl https://app.core-platform.local/api/monitoring/health
```

**Expected**: `{"database":"ok"}`

#### Test BFF Query (as Tenant User)

```bash
# Login as acme-corp user and get JWT
export JWT="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X POST https://app.core-platform.local/api/monitoring/ds/query \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "queries": [{
      "refId": "A",
      "expr": "up",
      "range": true
    }],
    "from": "now-1h",
    "to": "now"
  }'
```

**Expected**: JSON response with query results.

#### Verify Grafana Org

Login to Grafana admin UI:

1. Go to https://admin.core-platform.local/monitoring
2. Login with admin credentials
3. Switch org: **Configuration → Organizations** → Select "acme-corp"
4. Verify datasources: **Configuration → Data sources**
   - Should see Prometheus and Loki

### 8. Create Default Dashboards (Optional)

Import default dashboards for the tenant:

```bash
# Export dashboard JSON from another org
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://admin.core-platform.local/monitoring/api/dashboards/uid/tenant-app-metrics \
  > dashboard.json

# Import into new org (switch to orgId 4 first)
curl -X POST \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Grafana-Org-Id: 4" \
  -d @dashboard.json \
  https://admin.core-platform.local/monitoring/api/dashboards/db
```

## Automated Token Rotation

The system automatically rotates tokens monthly via k8s CronJob:

**File**: `k8s/monitoring/cronjob-rotate-grafana-tokens.yaml`

### Add Tenant to Rotation ConfigMap

```yaml
# k8s/monitoring/configmap-grafana-tenants.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-tenants
  namespace: core-platform
data:
  tenants: |
    core-platform:1
    test-tenant:2
    acme-corp:4  # NEW
```

Apply ConfigMap:

```bash
kubectl apply -f k8s/monitoring/configmap-grafana-tenants.yaml
```

The CronJob will:
1. Read tenants from ConfigMap
2. Rotate tokens on 1st of each month (00:00 UTC)
3. Update k8s Secrets
4. Restart backend deployment
5. Send Slack notification

## Monitoring & Alerts

### Prometheus Alerts

Tenant-specific alerts are automatically enabled:

- **BFFHighErrorRate**: >5% error rate for tenant
- **BFFRateLimitExceeded**: Tenant hitting rate limit (100 req/min)
- **BFFSlowQueries**: P95 latency >5s for tenant queries

View alerts: https://admin.core-platform.local/monitoring/alerting

### Grafana Dashboard

Monitor BFF health per tenant:

**Dashboard**: Monitoring BFF Health  
**URL**: https://admin.core-platform.local/monitoring/d/monitoring-bff-health

**Panels**:
- Request rate by tenant
- Error rate by tenant
- P95/P99 latency by tenant
- Rate limit hits by tenant

### Logs (Loki)

Query tenant logs:

```logql
{service="monitoring-bff", tenant="acme-corp"} | json
```

**Explore**: https://admin.core-platform.local/monitoring/explore

## Troubleshooting

### "Unknown tenant" Error

**Symptom**: `{"error":"Unknown tenant: acme-corp"}`

**Cause**: Tenant not mapped in `TenantOrgServiceImpl`.

**Solution**: Add mapping and restart backend (see Step 6).

### "Unauthorized" Error (401)

**Symptom**: BFF returns 401 when querying.

**Cause**: Invalid or expired service account token.

**Solution**: Regenerate token:

```bash
npx tsx tools/grafana-org-admin.ts rotate-sat \
  --org 4 \
  --name "acme-corp-viewer"
```

Update secret/env var and restart backend.

### Rate Limit (429)

**Symptom**: `{"error":"Rate limit exceeded"}`

**Cause**: Tenant exceeded 100 req/min.

**Solution**: 
1. Check frontend refresh interval (increase from 5s to 30s)
2. Review number of panels (reduce if >10)
3. Consider increasing rate limit in `MonitoringRateLimitFilter`

### Circuit Breaker Open (503)

**Symptom**: `{"error":"Grafana service unavailable"}`

**Cause**: Grafana is down or unhealthy.

**Solution**:
1. Check Grafana health: `curl http://grafana:3000/api/health`
2. Check Grafana logs: `docker logs core-grafana`
3. Circuit breaker auto-recovers in 30s (half-open state)

## Offboarding (Remove Tenant)

### 1. Delete Grafana Organization

```bash
# List orgs to find ID
npx tsx tools/grafana-org-admin.ts list-orgs

# Delete org
curl -X DELETE \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://admin.core-platform.local/monitoring/api/orgs/4
```

### 2. Remove from Backend

Remove tenant mapping from `TenantOrgServiceImpl.java` and restart backend.

### 3. Delete Secrets

```bash
# Development
# Remove from .env

# Production
kubectl delete secret grafana-sat-acme-corp -n core-platform
```

### 4. Remove from CronJob ConfigMap

Remove tenant from `k8s/monitoring/configmap-grafana-tenants.yaml` and reapply.

## CLI Reference

### List All Organizations

```bash
npx tsx tools/grafana-org-admin.ts list-orgs
```

### Rotate Service Account Token

```bash
npx tsx tools/grafana-org-admin.ts rotate-sat \
  --org 4 \
  --name "acme-corp-viewer"
```

### Get Current Token (for debugging)

```bash
# Login to Grafana admin UI
# Go to Administration → Service accounts → acme-corp-viewer → Tokens
```

## Best Practices

### 1. Use Vault in Production

Never store tokens in environment variables or ConfigMaps in production. Always use HashiCorp Vault or equivalent secret management.

### 2. Rotate Tokens Regularly

The CronJob rotates tokens monthly. For high-security environments, consider weekly rotation:

```yaml
# k8s/monitoring/cronjob-rotate-grafana-tokens.yaml
spec:
  schedule: "0 0 * * 0"  # Weekly (Sunday midnight)
```

### 3. Monitor Token Usage

Check Prometheus metrics for token usage:

```promql
monitoring:bff:request_rate:5m{tenant="acme-corp"}
```

### 4. Set Up Alerts

Enable PagerDuty integration for critical tenant alerts:

```yaml
# docker/prometheus/alertmanager/alertmanager.yml
receivers:
  - name: 'pagerduty-acme-corp'
    pagerduty_configs:
      - service_key: <acme-corp-key>
```

### 5. Test Before Production

Always test new tenant setup in development environment first:

```bash
# Test query with dev JWT
curl -X POST http://localhost:8080/api/monitoring/ds/query \
  -H "Authorization: Bearer $DEV_JWT" \
  -d '{"queries":[{"refId":"A","expr":"up"}]}'
```

## References

- [Monitoring BFF Architecture](../MONITORING_BFF_ARCHITECTURE.md)
- [Grafana Scenes Usage Guide](../frontend/GRAFANA_SCENES_GUIDE.md)
- [Incident Runbook](../runbooks/MONITORING_INCIDENT_RUNBOOK.md)
- [API Documentation](https://app.core-platform.local/swagger-ui.html)
