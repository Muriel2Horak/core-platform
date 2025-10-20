# 📊 Axiom Monitoring Package - Deployment Guide

## 🎯 Overview

Complete monitoring solution automatically deployed with every tenant creation. Includes:
- **7 Production Dashboards**: System, Runtime, DB, Redis, Kafka, Security, Audit
- **40+ Recording Rules**: Pre-aggregated SLO, latency, Kafka lag metrics
- **30+ Alert Rules**: Comprehensive coverage with runbook URLs
- **Auto-Provisioning**: Dashboards created automatically for each new tenant
- **Frontend Integration**: 5-tab monitoring interface at `/core-admin/axiom-monitoring`

---

## 📦 What's Included

### Prometheus Recording Rules (7 files)
```bash
docker/prometheus/rules/
├── axiom_slo.yml       # SLO tracking (error rate, burn rate, availability)
├── axiom_http.yml      # HTTP latency (p50/p95/p99) + status rates
├── axiom_kafka.yml     # Consumer lag, DLQ, partition skew
├── axiom_db.yml        # Connection pool saturation, slow queries
├── axiom_redis.yml     # Hit rate, evictions, memory utilization
├── axiom_security.yml  # Failed logins, 403/401 rates, TLS cert expiry
└── axiom_jvm.yml       # Heap utilization, GC pause, threads
```

### Prometheus Alert Rules (7 files, 30+ alerts)
```bash
docker/prometheus/alerts/
├── axiom_slo_alerts.yml       # SLO breaches, error budget exhaustion
├── axiom_http_alerts.yml      # High latency, error rate spikes
├── axiom_kafka_alerts.yml     # Consumer lag, DLQ growth, rebalances
├── axiom_db_alerts.yml        # Pool saturation, connection timeouts
├── axiom_redis_alerts.yml     # Low hit rate, memory pressure
├── axiom_security_alerts.yml  # Failed login spikes, cert expiry
└── axiom_jvm_alerts.yml       # Heap pressure, GC pause duration
```

### Grafana Dashboards (7 dashboards)
```bash
docker/grafana/provisioning/dashboards/
├── system/axiom_sys_overview.json      # 🏆 Flagship: SLO, errors, latency, deps
├── advanced/axiom_adv_runtime.json     # JVM heap, GC, threads
├── advanced/axiom_adv_db.json          # HikariCP pool, slow queries
├── advanced/axiom_adv_redis.json       # Cache hit rate, memory, evictions
├── streaming/axiom_kafka_lag.json      # Consumer lag per topic/partition
├── security/axiom_security.json        # Auth failures, 403/401, Loki drilldown
└── audit/axiom_audit.json              # CRUD events, operations, entity types
```

### Frontend Integration
```typescript
frontend/src/pages/Admin/AxiomMonitoringPage.tsx
// 5 tabs with iframe embeds:
// - Tab 1: System Overview
// - Tab 2: Advanced (Runtime/DB/Redis sub-dashboards)
// - Tab 3: Streaming (Kafka lag)
// - Tab 4: Security
// - Tab 5: Audit
```

---

## 🚀 Deployment Steps

### 1. Initial Setup (One-Time)

```bash
# 1.1 Ensure scripts are executable
chmod +x scripts/test-monitoring-deploy.sh
chmod +x scripts/test-monitoring-runtime.sh

# 1.2 Install promtool for validation (optional but recommended)
# macOS:
brew install prometheus

# Ubuntu/Debian:
sudo apt-get install prometheus

# 1.3 Verify configuration files exist
ls -la docker/prometheus/rules/axiom_*.yml
ls -la docker/prometheus/alerts/axiom_*.yml
ls -la docker/grafana/provisioning/dashboards/*/axiom_*.json
```

### 2. Pre-Deploy Testing (REQUIRED)

Run **before** every deployment to catch config errors:

```bash
# Run all pre-deploy tests (2-3 min)
make test-monitoring-deploy

# Tests perform:
# ✅ Prometheus recording rules validation (promtool)
# ✅ Prometheus alert rules validation (promtool)
# ✅ Grafana dashboard JSON syntax (jq)
# ✅ Provisioning config YAML structure
# ✅ Frontend integration files exist
```

**Expected Output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 Test Summary
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Total Tests:  45
Passed:       45
Failed:       0

✅ ALL PRE-DEPLOY TESTS PASSED
Monitoring package is ready for deployment!
```

### 3. Build & Deploy

```bash
# 3.1 Clean rebuild (rebuilds all containers)
make rebuild

# 3.2 OR deploy to existing environment
docker compose up -d prometheus grafana

# 3.3 Verify services started
docker compose ps | grep -E "(prometheus|grafana)"
```

### 4. Post-Deploy Verification (RECOMMENDED)

Run **after** deployment to validate runtime behavior:

```bash
# Run all post-deploy tests (5-10 min)
make test-monitoring-runtime

# Tests perform:
# ✅ Recording rules loaded in Prometheus
# ✅ Alert rules loaded in Prometheus
# ✅ Dashboards accessible via Grafana API
# ✅ Metrics flowing (sample queries)
# ✅ Error spike simulation (SLO tracking)
# ✅ Dashboard rendering test
```

**Expected Output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 Test Summary
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Total Tests:  25
Passed:       22
Skipped:      3
Failed:       0

✅ POST-DEPLOY TESTS COMPLETED
Monitoring package is fully functional!
```

---

## 🏢 Tenant Auto-Provisioning

### How It Works

When a **new tenant is created** via `/api/admin/tenants`, the system automatically:

1. **Creates Keycloak realm** (via `KeycloakRealmManagementService`)
2. **Registers tenant in DB** (via `TenantService.createTenantRegistry`)
3. **🆕 Auto-provisions Grafana monitoring** (via `GrafanaMonitoringProvisioningService`):
   - Creates Grafana organization: `Tenant: {tenantKey}`
   - Imports all 7 Axiom dashboards to tenant org
   - Sets default tenant variable to `{tenantKey}`
   - Tenant admin gets immediate access to monitoring

### Implementation Details

```java
// TenantManagementController.java
@PostMapping
public ResponseEntity<Map<String, Object>> createTenant(
    @Valid @RequestBody CreateTenantRequest request) {
  
  // 1. Create Keycloak realm
  keycloakRealmManagementService.createTenant(
      request.getKey(), 
      request.getDisplayName()
  );
  
  // 2. Register tenant in DB
  Optional<Tenant> tenant = tenantService.findTenantByKey(request.getKey());
  
  // 3. 📊 AUTO-PROVISION: Grafana monitoring dashboards
  grafanaMonitoringProvisioningService.provisionMonitoringForTenant(
      request.getKey(),
      request.getDisplayName()
  );
  
  return ResponseEntity.status(CREATED).body(response);
}
```

### Testing Tenant Provisioning

```bash
# 1. Create test tenant via UI
# Navigate to: Admin → Tenants → Create Tenant
# Key: test-company
# Display Name: Test Company Inc.

# 2. OR via API
curl -X POST http://localhost:8080/api/admin/tenants \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "key": "test-company",
    "displayName": "Test Company Inc."
  }'

# 3. Verify Grafana org created
curl -u admin:admin http://localhost:3000/api/orgs/name/Tenant:%20test-company

# Expected response:
{
  "id": 4,
  "name": "Tenant: test-company",
  "address": {}
}

# 4. Verify dashboards imported
curl -u admin:admin \
  -H "X-Grafana-Org-Id: 4" \
  http://localhost:3000/api/search?type=dash-db | jq '.[].uid'

# Expected: 7 dashboard UIDs
"axiom_sys_overview"
"axiom_adv_runtime"
"axiom_adv_db"
"axiom_adv_redis"
"axiom_kafka_lag"
"axiom_security"
"axiom_audit"
```

### Rollback Tenant Provisioning (if needed)

```bash
# If tenant creation fails, manually cleanup Grafana org:

# 1. Find org ID
ORG_ID=$(curl -s -u admin:admin \
  "http://localhost:3000/api/orgs/name/Tenant:%20test-company" | jq '.id')

# 2. Delete org (cascades to dashboards)
curl -X DELETE -u admin:admin \
  "http://localhost:3000/api/orgs/$ORG_ID"
```

---

## 🧪 CI/CD Integration

### GitHub Actions Example

```yaml
name: Deploy with Monitoring Tests

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      # PRE-DEPLOY: Validate monitoring config
      - name: Pre-Deploy Monitoring Tests
        run: |
          chmod +x scripts/test-monitoring-deploy.sh
          make test-monitoring-deploy
      
      # BUILD & DEPLOY
      - name: Build and Deploy
        run: |
          make rebuild
          docker compose up -d
      
      # POST-DEPLOY: Verify monitoring runtime
      - name: Post-Deploy Monitoring Tests
        run: |
          sleep 60  # Wait for services to stabilize
          make test-monitoring-runtime
```

### Makefile Targets

```bash
# Available Make targets:

make test-monitoring-deploy   # Pre-deploy config validation (fast, 2-3 min)
make test-monitoring-runtime  # Post-deploy runtime tests (5-10 min)
make test-monitoring          # Full suite (deploy + runtime)

# Integration with existing CI:
make clean                    # Already includes test-monitoring-deploy
make rebuild                  # Add RUN_MONITORING_TESTS=true for post-deploy
```

---

## 📊 Accessing Monitoring Dashboards

### For Core Admins

1. **Frontend UI** (recommended):
   ```
   https://admin.core-platform.local/core-admin/axiom-monitoring
   ```
   - Tab 1: System Overview (SLO, errors, latency)
   - Tab 2: Advanced (Runtime/JVM, Database, Redis)
   - Tab 3: Streaming (Kafka lag per topic/partition)
   - Tab 4: Security (Failed logins, 403/401 events)
   - Tab 5: Audit (CRUD operations, entity changes)

2. **Direct Grafana** (for customization):
   ```
   https://grafana.core-platform.local
   Login: admin / {GRAFANA_PASSWORD}
   ```

### For Tenant Admins

1. **Frontend UI** (tenant org context):
   ```
   https://{tenant-key}.core-platform.local/core-admin/axiom-monitoring
   ```
   - Auto-filtered to tenant data via `tenant` variable
   - Same 5 tabs as core admin
   - Data isolation via Grafana org ID

2. **Direct Grafana** (tenant org):
   ```
   https://grafana.core-platform.local
   Login: {tenant-admin-email} / {password}
   Org: Tenant: {tenant-key}
   ```

---

## 🔍 Troubleshooting

### Issue: Recording rules not loading

**Symptoms:**
```bash
# Prometheus shows no Axiom rules
curl localhost:9090/api/v1/rules | jq '.data.groups[].name' | grep axiom
# (empty)
```

**Solution:**
```bash
# 1. Check Prometheus config
docker exec core-prometheus cat /etc/prometheus/prometheus.yml | grep rule_files -A 5

# Should contain:
rule_files:
  - "/etc/prometheus/rules/axiom_*.yml"
  - "/etc/prometheus/alerts/axiom_*.yml"

# 2. Verify files mounted correctly
docker exec core-prometheus ls -la /etc/prometheus/rules/
docker exec core-prometheus ls -la /etc/prometheus/alerts/

# 3. Validate YAML syntax
make test-monitoring-deploy

# 4. Restart Prometheus
docker compose restart prometheus
```

### Issue: Dashboards not appearing in Grafana

**Symptoms:**
```bash
# No Axiom dashboards in search
curl -u admin:admin localhost:3000/api/search?type=dash-db | jq '.[].uid' | grep axiom
# (empty)
```

**Solution:**
```bash
# 1. Check provisioning config
docker exec core-grafana cat /etc/grafana/provisioning/dashboards/dashboards.yml

# Should contain 5 providers:
# - Axiom Monitoring - System
# - Axiom Monitoring - Advanced
# - Axiom Monitoring - Streaming
# - Axiom Monitoring - Security
# - Axiom Monitoring - Audit

# 2. Verify dashboard JSON files mounted
docker exec core-grafana ls -la /etc/grafana/provisioning/dashboards/system/
docker exec core-grafana ls -la /etc/grafana/provisioning/dashboards/advanced/
docker exec core-grafana ls -la /etc/grafana/provisioning/dashboards/streaming/
docker exec core-grafana ls -la /etc/grafana/provisioning/dashboards/security/
docker exec core-grafana ls -la /etc/grafana/provisioning/dashboards/audit/

# 3. Check JSON syntax
make test-monitoring-deploy

# 4. Restart Grafana
docker compose restart grafana

# 5. Force re-provisioning
docker compose down grafana
docker volume rm core-platform_grafana_data
docker compose up -d grafana
```

### Issue: Tenant provisioning fails silently

**Symptoms:**
```bash
# Tenant created but no Grafana org
curl -u admin:admin localhost:3000/api/orgs/name/Tenant:%20test-company
# 404 Not Found
```

**Solution:**
```bash
# 1. Check backend logs for provisioning errors
docker compose logs backend | grep "GrafanaMonitoringProvisioningService"

# Look for:
# "📊 Starting Grafana monitoring provisioning for tenant: test-company"
# "✅ Grafana org created/found: orgId=4 for tenant=test-company"
# "🎉 Monitoring provisioning completed for tenant: test-company"

# 2. Verify Grafana API reachable from backend
docker exec core-backend curl -u admin:admin http://grafana:3000/api/health

# 3. Check Grafana admin credentials
docker exec core-backend printenv | grep GRAFANA

# 4. Manually trigger provisioning (via API)
curl -X POST http://localhost:8080/api/admin/tenants/{tenantKey}/provision-monitoring \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Issue: Alerts not firing

**Symptoms:**
```bash
# Prometheus shows alerts but none are firing
curl localhost:9090/api/v1/alerts | jq '.data.alerts[] | select(.state == "firing")'
# (empty)
```

**Solution:**
```bash
# 1. Check if recording rules have data
curl 'localhost:9090/api/v1/query?query=app:http_requests:error_rate5m' | jq '.data.result[].value'

# 2. Verify alert thresholds are realistic
# e.g., SLOBurnRateCritical triggers on >14.4x burn rate
# May need traffic to generate errors

# 3. Simulate error spike to test alerts
bash scripts/test-monitoring-runtime.sh

# 4. Check Prometheus logs
docker compose logs prometheus | grep -i alert
```

### Issue: Frontend monitoring page 404

**Symptoms:**
```
https://admin.core-platform.local/core-admin/axiom-monitoring
→ 404 Not Found
```

**Solution:**
```bash
# 1. Verify route exists in App.jsx
grep -n "axiom-monitoring" frontend/src/App.jsx

# Should contain:
<Route path="axiom-monitoring" element={<AxiomMonitoringPage />} />

# 2. Check component exported
grep -n "AxiomMonitoringPage" frontend/src/pages/Admin/index.ts

# Should contain:
export { AxiomMonitoringPage } from './AxiomMonitoringPage';

# 3. Rebuild frontend
docker compose build frontend
docker compose up -d frontend nginx

# 4. Clear browser cache
# Shift+Reload or Cmd+Shift+R
```

---

## 📈 Performance Tuning

### Recording Rule Intervals

Default: `interval: 30s` for all recording rules

```yaml
# To reduce Prometheus load, increase interval:
groups:
  - name: axiom_slo
    interval: 60s  # Change from 30s to 60s
    rules:
      - record: app:slo:availability5m
        expr: ...
```

**Trade-offs:**
- ⬆️ Higher interval = Lower Prometheus CPU/memory
- ⬇️ Lower interval = More granular metrics, faster alert response

### Dashboard Refresh Rates

Default:
- System Overview: `refresh: 30s`
- Kafka Lag: `refresh: 10s` (real-time)
- Others: `refresh: 1m`

```json
// To reduce Grafana load:
{
  "refresh": "5m",  // Change from 30s to 5m
  "time": {
    "from": "now-6h",
    "to": "now"
  }
}
```

### Alert Evaluation

Default: Prometheus evaluates alerts every `evaluation_interval: 15s`

```yaml
# prometheus.yml
global:
  evaluation_interval: 30s  # Change from 15s to 30s
```

---

## 🎓 Best Practices

### 1. Always Run Pre-Deploy Tests

```bash
# REQUIRED before every deployment
make test-monitoring-deploy

# Catches:
# - YAML syntax errors
# - Invalid PromQL expressions
# - Missing dashboard files
# - Provisioning config issues
```

### 2. Monitor the Monitors

```bash
# Set up alerts for monitoring infrastructure itself:

# Prometheus down
- alert: PrometheusDown
  expr: up{job="prometheus"} == 0
  for: 5m

# Grafana down
- alert: GrafanaDown
  expr: up{job="grafana"} == 0
  for: 5m

# Recording rules lagging
- alert: RecordingRulesLagging
  expr: prometheus_rule_group_last_duration_seconds > 60
  for: 10m
```

### 3. Tenant Isolation Verification

```bash
# After creating new tenant, verify data isolation:

# 1. Login as tenant admin
# 2. Navigate to System Overview dashboard
# 3. Check tenant variable default = {tenant-key}
# 4. Query metrics:
curl 'localhost:9090/api/v1/query?query=http_server_requests_seconds_count{tenant="test-company"}'

# Should only return tenant-specific data
```

### 4. Regular Backups

```bash
# Backup Grafana dashboards (automated via provisioning)
tar -czf grafana-dashboards-$(date +%F).tar.gz \
  docker/grafana/provisioning/dashboards/

# Backup Prometheus rules
tar -czf prometheus-rules-$(date +%F).tar.gz \
  docker/prometheus/rules/ \
  docker/prometheus/alerts/

# Backup Prometheus data (TSDB)
docker exec core-prometheus tar -czf /tmp/prometheus-data.tar.gz /prometheus
docker cp core-prometheus:/tmp/prometheus-data.tar.gz ./
```

---

## 📚 Additional Resources

- **Design Documentation**: `docs/MONITORING_PACKAGE.md`
- **Recording Rules**: `docker/prometheus/rules/axiom_*.yml`
- **Alert Rules**: `docker/prometheus/alerts/axiom_*.yml`
- **Dashboards**: `docker/grafana/provisioning/dashboards/*/axiom_*.json`
- **Frontend Component**: `frontend/src/pages/Admin/AxiomMonitoringPage.tsx`

---

## 🆘 Support

For issues or questions:

1. Check **Troubleshooting** section above
2. Run diagnostic tests: `make test-monitoring`
3. Review logs: `docker compose logs prometheus grafana`
4. Open GitHub issue with test output + logs

---

**Last Updated**: 2025-01-20  
**Version**: 1.0.0 (Production Ready)
