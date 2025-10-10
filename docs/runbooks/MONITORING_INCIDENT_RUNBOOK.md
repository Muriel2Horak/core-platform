# Monitoring System Incident Runbook

## 🚨 Quick Reference

| Alert | Severity | Response Time | Escalation |
|-------|----------|---------------|------------|
| BFF Down | P1 | 5 minutes | Platform Team Lead |
| Grafana Down | P1 | 5 minutes | Platform Team Lead |
| SAT Expired | P2 | 30 minutes | On-call Engineer |
| Rate Limit Exceeded | P3 | 1 hour | Team Slack |
| Slow Queries | P3 | 2 hours | Team Slack |

---

## 📋 Incident Response Process

### 1. Initial Assessment (0-5 min)

```bash
# Check service health
curl -H "Authorization: Bearer <jwt>" \
  https://app.core-platform.local/api/monitoring/health

# Check Grafana status
curl https://admin.core-platform.local/monitoring/api/health

# Check BFF logs
docker compose logs backend | grep "monitoring.bff" | tail -50
```

### 2. Identify Root Cause (5-15 min)

- [ ] BFF service down? → [BFF Down](#bff-down-p1)
- [ ] Grafana down? → [Grafana Down](#grafana-down-p1)
- [ ] 401 errors? → [SAT Issues](#service-account-token-expired-p2)
- [ ] 429 errors? → [Rate Limiting](#rate-limit-exceeded-p3)
- [ ] Slow responses? → [Performance Issues](#slow-queries-p3)
- [ ] No data in panels? → [Datasource Issues](#no-data-in-panels-p3)

### 3. Communication (Throughout)

```markdown
**Incident Template** (Slack #incidents):

🚨 **INCIDENT**: Monitoring BFF/Grafana Issue
**Severity**: P1/P2/P3
**Impact**: <describe user impact>
**Status**: Investigating / Identified / Resolving / Resolved
**ETA**: <estimated resolution time>
**Owner**: @engineer-name

**Updates**:
- [HH:MM] Initial detection, investigating...
- [HH:MM] Root cause identified: ...
- [HH:MM] Fix applied, monitoring...
- [HH:MM] RESOLVED
```

---

## 🔥 P1 Incidents (Critical)

### BFF Down (P1)

**Symptoms**:
- `/api/monitoring/*` returns 502/503
- Grafana Scenes panels show "Network Error"
- Backend logs: `WebClient connection refused`

**Diagnosis**:
```bash
# 1. Check backend container
docker compose ps backend
docker compose logs backend --tail 100

# 2. Check WebClient health
curl http://localhost:8080/actuator/health | jq '.components.webClient'

# 3. Check Grafana connectivity from backend
docker compose exec backend curl -v http://grafana:3000/api/health
```

**Resolution**:

**Option 1: Restart Backend**
```bash
docker compose restart backend

# Wait 30s, verify
curl -H "Authorization: Bearer <jwt>" \
  https://app.core-platform.local/api/monitoring/health
```

**Option 2: Network Issue**
```bash
# Check Docker network
docker network inspect core-platform_default

# Recreate network
docker compose down
docker compose up -d
```

**Option 3: Config Issue**
```bash
# Check application.properties
docker compose exec backend cat /app/application.properties | grep monitoring.grafana

# Verify env vars
docker compose exec backend env | grep GRAFANA
```

**Escalation**:
- If not resolved in 15 min → Platform Team Lead
- If infrastructure issue → DevOps Team

---

### Grafana Down (P1)

**Symptoms**:
- Admin GUI (`https://admin.<DOMAIN>/monitoring`) returns 502
- BFF logs: `Connection refused to grafana:3000`
- All tenant dashboards blank

**Diagnosis**:
```bash
# 1. Check Grafana container
docker compose ps grafana
docker compose logs grafana --tail 100

# 2. Check Grafana process
docker compose exec grafana ps aux | grep grafana

# 3. Check Grafana config
docker compose exec grafana cat /etc/grafana/grafana.ini | grep -A5 "\[server\]"
```

**Resolution**:

**Option 1: Restart Grafana**
```bash
docker compose restart grafana

# Wait for startup (30-60s)
docker compose logs -f grafana
# Look for: "HTTP Server Listen"

# Verify
curl http://localhost:3000/api/health
```

**Option 2: Database Issue**
```bash
# Check Grafana DB
docker compose exec postgres psql -U admin -d core_platform -c \
  "SELECT count(*) FROM grafana.dashboard;"

# If corrupted, restore from backup
docker compose exec postgres pg_restore -U admin -d core_platform \
  /backups/grafana_$(date +%Y%m%d).sql
```

**Option 3: OIDC Issue**
```bash
# Check Keycloak connectivity
curl https://admin.core-platform.local/realms/admin/.well-known/openid-configuration

# Verify client secret
docker compose exec grafana cat /etc/grafana/grafana.ini | grep client_secret

# Compare with Keycloak
keycloak-admin clients get-secret --realm admin --client grafana-admin
```

**Escalation**:
- If not resolved in 15 min → Platform Team Lead
- If Keycloak issue → Auth Team

---

## ⚠️ P2 Incidents (High)

### Service Account Token Expired (P2)

**Symptoms**:
- BFF returns 401 from Grafana
- Logs: `Grafana API returned 401 Unauthorized`
- Specific tenant affected

**Diagnosis**:
```bash
# 1. Identify affected tenant
grep "401 Unauthorized" docker-logs/backend.log | grep "tenant=" | tail -10

# 2. Check token validity
export TENANT_ID=test-tenant
export SAT=$(docker compose exec backend env | grep GRAFANA_SAT_${TENANT_ID^^} | cut -d= -f2)

curl -H "Authorization: Bearer $SAT" \
  -H "X-Grafana-Org-Id: 2" \
  http://localhost:3000/api/org
# Expected: 200 OK
# Actual: 401 → Token expired/invalid
```

**Resolution**:

**Step 1: Rotate Token**
```bash
cd tools

# Get orgId from logs/config
export ORG_ID=2

# Rotate (creates new, deletes old)
npx tsx grafana-org-admin.ts rotate-sat \
  --org $ORG_ID \
  --name ${TENANT_ID}-viewer

# Output:
# New token: glsa_NEW_TOKEN_HERE
# Old token deleted
```

**Step 2: Update Config**

**Development**:
```bash
# Update .env
echo "GRAFANA_SAT_${TENANT_ID^^}=glsa_NEW_TOKEN_HERE" >> .env

# Restart backend
docker compose restart backend
```

**Production** (Vault):
```bash
# Update Vault
vault kv put secret/grafana/orgs/${TENANT_ID} \
  orgId=$ORG_ID \
  serviceAccountToken=glsa_NEW_TOKEN_HERE

# Restart backend (picks up new token from Vault)
kubectl rollout restart deployment/backend -n core-platform
```

**Step 3: Verify**
```bash
# Test BFF endpoint
curl -H "Authorization: Bearer <jwt-for-tenant>" \
  https://app.core-platform.local/api/monitoring/health

# Expected: {"status":"UP","tenant":"test-tenant",...}
```

**Prevention**:
- Set calendar reminder: Rotate tokens every 90 days
- Implement automated rotation (TODO)

---

### Datasource Configuration Mismatch (P2)

**Symptoms**:
- Panels show "No data"
- Prometheus/Loki return data but Grafana filters it out
- BFF logs: Query succeeded but empty results

**Diagnosis**:
```bash
# 1. Check datasource config
curl -H "Authorization: Bearer $SAT" \
  -H "X-Grafana-Org-Id: $ORG_ID" \
  http://localhost:3000/api/datasources | jq '.[] | select(.type=="prometheus")'

# 2. Verify X-Scope-OrgID header
# Should match tenant ID exactly

# 3. Test direct Prometheus query
curl 'http://localhost:9090/api/v1/query?query=up' \
  -H 'X-Scope-OrgID: test-tenant'
```

**Resolution**:

**Step 1: Re-provision Datasources**
```bash
cd tools

npx tsx grafana-org-admin.ts provision-ds \
  --org $ORG_ID \
  --tenant $TENANT_ID

# Output: /docker/grafana/provisioning/datasources/datasources-org${ORG_ID}-${TENANT_ID}.yaml
```

**Step 2: Verify YAML**
```bash
cat docker/grafana/provisioning/datasources/datasources-org${ORG_ID}-${TENANT_ID}.yaml

# Check:
# - orgId matches
# - httpHeaderValue1 matches tenant ID
# - URL is correct
```

**Step 3: Restart Grafana**
```bash
docker compose restart grafana

# Wait for provisioning (10s)
docker compose logs grafana | grep "provisioning"
```

**Step 4: Verify in Grafana UI**
1. Login to admin GUI
2. Switch to org: `https://admin.<DOMAIN>/monitoring/?orgId=$ORG_ID`
3. Settings → Data sources
4. Check Prometheus → Additional settings → Custom HTTP Headers
5. Verify: `X-Scope-OrgID = test-tenant`

---

## ⏰ P3 Incidents (Medium)

### Rate Limit Exceeded (P3)

**Symptoms**:
- Some users get 429 Too Many Requests
- Logs: `Rate limit exceeded for user: john.doe`
- Affects specific heavy users

**Diagnosis**:
```bash
# 1. Find affected users
grep "Rate limit exceeded" docker-logs/backend.log | \
  awk '{print $NF}' | sort | uniq -c | sort -rn

# 2. Check request pattern
grep "user:john.doe" docker-logs/backend.log | \
  grep "/api/monitoring" | \
  awk '{print $1}' | \
  uniq -c
# If >100 requests in 1 minute → legitimate rate limit
```

**Resolution**:

**Option 1: Temporary Bypass (Emergency)**
```java
// MonitoringRateLimitFilter.java
private static final Set<String> BYPASS_USERS = Set.of(
    "admin@core-platform.local",
    "john.doe@emergency.com"
);

@Override
public void doFilter(ServletRequest request, ...) {
    String username = extractUsername(request);
    if (BYPASS_USERS.contains(username)) {
        chain.doFilter(request, response);
        return;
    }
    // ... rate limit logic
}
```

**Option 2: Increase Limit for User**
```java
// MonitoringRateLimitFilter.java
private Bandwidth getLimit(String username) {
    // VIP users: 500 req/min
    if (isVipUser(username)) {
        return Bandwidth.builder()
            .capacity(500)
            .refillIntervally(500, Duration.ofMinutes(1))
            .build();
    }
    
    // Default: 100 req/min
    return Bandwidth.builder()
        .capacity(100)
        .refillIntervally(100, Duration.ofMinutes(1))
        .build();
}
```

**Option 3: Increase Global Limit**
```java
// For all users
Bandwidth.builder()
    .capacity(200)  // Was 100
    .refillIntervally(200, Duration.ofMinutes(1))
    .build()
```

**Deploy**:
```bash
# Rebuild
cd backend && ./mvnw clean package -DskipTests

# Restart
docker compose restart backend
```

---

### Slow Queries (P3)

**Symptoms**:
- Panels load slowly (>5s)
- BFF logs: `Query duration: 8000ms`
- Users complain about laggy dashboards

**Diagnosis**:
```bash
# 1. Check slow queries in logs
grep "Query duration" docker-logs/backend.log | \
  awk -F'duration: ' '{print $2}' | \
  awk '{print $1}' | \
  sort -rn | head -20

# 2. Check Prometheus query performance
curl 'http://localhost:9090/api/v1/query?query=<slow-query>' \
  -H 'X-Scope-OrgID: test-tenant' \
  -w "\nTime: %{time_total}s\n"

# 3. Check Grafana query inspector
# Admin GUI → Dashboard → Panel → Inspect → Query
```

**Resolution**:

**Option 1: Optimize PromQL Query**
```promql
# Bad (slow):
sum(rate(http_requests_total[5m])) by (endpoint, method, status)

# Good (fast):
sum(rate(http_requests_total[5m])) by (endpoint)

# Use recording rules for complex queries
```

**Option 2: Reduce Time Range**
```javascript
// Frontend - Reports.jsx
const defaultTimeRange = {
  from: 'now-30m',  // Was 'now-6h'
  to: 'now'
};
```

**Option 3: Increase BFF Timeout**
```java
// MonitoringBffConfig.java
@Bean
public WebClient grafanaClient(...) {
    return WebClient.builder()
        .baseUrl(grafanaBaseUrl)
        .clientConnector(new ReactorClientHttpConnector(
            HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60))  // Was 30s
        ))
        .build();
}
```

**Option 4: Add Caching**
```java
@Cacheable(value = "grafana-queries", key = "#jwt.subject + #body.hashCode()")
public ResponseEntity<String> forwardQuery(Jwt jwt, Map<String, Object> body) {
    // ... existing logic
}

// TTL: 30 seconds (don't cache longer for real-time data)
```

---

### No Data in Panels (P3)

**Symptoms**:
- Grafana Scenes panels empty
- BFF returns 200 but empty results
- Prometheus has data but Grafana doesn't show it

**Diagnosis**:
```bash
# 1. Check if Prometheus has data
curl 'http://localhost:9090/api/v1/query?query=up' \
  -H 'X-Scope-OrgID: test-tenant'

# 2. Check Grafana datasource query
curl -H "Authorization: Bearer $SAT" \
  -H "X-Grafana-Org-Id: $ORG_ID" \
  http://localhost:3000/api/ds/query \
  -d '{
    "queries": [{"refId":"A","expr":"up","datasource":{"type":"prometheus"}}],
    "from": "now-1h",
    "to": "now"
  }'

# 3. Check X-Scope-OrgID header
docker compose logs prometheus | grep "X-Scope-OrgID"
```

**Resolution**:

**Option 1: Verify Tenant ID**
```bash
# Check JWT claim
echo $JWT | base64 -d | jq '.tenant_id'

# Check datasource header
curl -H "Authorization: Bearer $SAT" \
  http://localhost:3000/api/datasources/1 | \
  jq '.secureJsonFields.httpHeaderValue1'

# MUST MATCH!
```

**Option 2: Re-provision Datasource**
```bash
cd tools
npx tsx grafana-org-admin.ts provision-ds --org 2 --tenant test-tenant
docker compose restart grafana
```

**Option 3: Check Prometheus Scraping**
```bash
# Verify metrics have tenant label
curl http://localhost:9090/api/v1/query?query=up | \
  jq '.data.result[] | select(.metric.tenant=="test-tenant")'

# If empty, check metric_relabel_configs in prometheus.yml
```

---

## 🔧 Maintenance Tasks

### Monthly: Token Rotation

```bash
# Script: rotate-all-tokens.sh
#!/bin/bash

TENANTS=("core-platform" "test-tenant" "acme-corp")
ORG_IDS=(1 2 3)

for i in "${!TENANTS[@]}"; do
    TENANT=${TENANTS[$i]}
    ORG_ID=${ORG_IDS[$i]}
    
    echo "Rotating token for $TENANT (org $ORG_ID)..."
    
    npx tsx tools/grafana-org-admin.ts rotate-sat \
        --org $ORG_ID \
        --name ${TENANT}-viewer
    
    # Update Vault
    vault kv put secret/grafana/orgs/$TENANT \
        orgId=$ORG_ID \
        serviceAccountToken=$NEW_TOKEN
done

# Restart backend
kubectl rollout restart deployment/backend -n core-platform
```

### Weekly: Health Check

```bash
# Script: weekly-health-check.sh
#!/bin/bash

# 1. Check all BFF endpoints
for ENDPOINT in /ds/query /datasources /health; do
    curl -f -H "Authorization: Bearer $JWT" \
        https://app.core-platform.local/api/monitoring$ENDPOINT \
        || echo "❌ $ENDPOINT failed"
done

# 2. Check Grafana orgs
for ORG_ID in 1 2 3; do
    curl -f -H "Authorization: Bearer $SAT" \
        -H "X-Grafana-Org-Id: $ORG_ID" \
        http://localhost:3000/api/org \
        || echo "❌ Org $ORG_ID failed"
done

# 3. Check datasources
for ORG_ID in 1 2 3; do
    DS_COUNT=$(curl -s -H "Authorization: Bearer $SAT" \
        -H "X-Grafana-Org-Id: $ORG_ID" \
        http://localhost:3000/api/datasources | jq 'length')
    
    if [ "$DS_COUNT" -lt 2 ]; then
        echo "❌ Org $ORG_ID has only $DS_COUNT datasources (expected 2+)"
    fi
done
```

---

## 📊 Monitoring Dashboards

### BFF Health Dashboard (Grafana)

**Metrics**:
```promql
# Request rate by endpoint
sum(rate(http_server_requests_seconds_count{uri=~"/api/monitoring.*"}[5m])) by (uri)

# Error rate
sum(rate(http_server_requests_seconds_count{uri=~"/api/monitoring.*",status=~"5.."}[5m])) 
  / 
sum(rate(http_server_requests_seconds_count{uri=~"/api/monitoring.*"}[5m]))

# P95 latency
histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket{uri=~"/api/monitoring.*"}[5m])) by (le, uri)
)

# Rate limit hits
sum(rate(monitoring_bff_rate_limit_exceeded_total[5m])) by (tenant)

# Grafana API errors
sum(rate(monitoring_bff_grafana_errors_total[5m])) by (error_type)
```

**Alerts**:
```yaml
# alerts/monitoring-bff.yml
groups:
  - name: monitoring_bff
    interval: 30s
    rules:
      - alert: BFFHighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{uri=~"/api/monitoring.*",status=~"5.."}[5m])) 
          / sum(rate(http_server_requests_seconds_count{uri=~"/api/monitoring.*"}[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "BFF error rate > 5%"
          
      - alert: BFFSlowQueries
        expr: |
          histogram_quantile(0.95, 
            sum(rate(http_server_requests_seconds_bucket{uri="/api/monitoring/ds/query"}[5m])) by (le)
          ) > 5
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "BFF P95 latency > 5s"
```

---

## 📞 Escalation Contacts

| Role | Contact | Availability |
|------|---------|--------------|
| On-call Engineer | Slack: @oncall, Phone: +420 XXX | 24/7 |
| Platform Team Lead | Slack: @platform-lead | Mon-Fri 9-18 |
| DevOps Team | Slack: #devops-incidents | 24/7 |
| Auth Team (Keycloak) | Slack: #auth-team | Mon-Fri 9-18 |

**Escalation Path**:
1. On-call Engineer (0-15 min)
2. Platform Team Lead (15-30 min)
3. CTO (30+ min, P1 only)

---

## 📚 Related Documentation

- [Monitoring Architecture](./MONITORING_BFF_ARCHITECTURE.md)
- [BFF Implementation](./BFF_GRAFANA_SCENES_IMPLEMENTATION.md)
- [API Documentation](./API.md)

---

**Last Updated**: 2025-10-10  
**Maintained By**: Platform Team  
**On-call Rotation**: https://pagerduty.core-platform.local
