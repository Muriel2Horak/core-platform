# Monitoring Package - Production Grade Observability

## 🎯 Overview

Comprehensive monitoring solution for Core Platform with multi-tenant support, standardized dashboards, recording rules, and alerts.

**Philosophy**: Observable by default, actionable alerts, drilldown-first design.

---

## 📁 Folder Structure

```
Grafana Folders:
├── System Monitoring      → High-level KPIs, SLO tracking, error budget
├── Advanced Monitoring    → Deep-dive: JVM, DB, Redis, Nginx, Keycloak
├── Streaming Operations   → Kafka: brokers, topics, consumers, lag, DLQ
├── Security & Compliance  → Auth failures, anomalies, cert expiry, RBAC
└── Audit & Governance     → CRUD tracking, workflow, Grafana access, FE events
```

---

## 🆔 Dashboard UIDs & Naming Convention

**Pattern**: `axiom_<category>_<name>`

### System Monitoring
- `axiom_sys_overview` - System Overview (KPI cards, SLO, error budget)
- `axiom_sys_errors` - Error Analysis (5xx breakdown, top errors from Loki)
- `axiom_sys_latency` - Latency Analysis (p50/p95/p99 heatmaps)

### Advanced Monitoring
- `axiom_adv_runtime` - Runtime & JVM (GC, heap, threads, pools)
- `axiom_adv_http` - HTTP Server Deep Dive (endpoints, status codes, retries)
- `axiom_adv_db` - Database Operations (pool saturation, slow queries, locks)
- `axiom_adv_redis` - Redis Performance (hit rate, evictions, commands)
- `axiom_adv_nginx` - Nginx Upstream (upstream timing, errors, rate limiting)
- `axiom_adv_keycloak` - Keycloak Auth Metrics (login rate, errors, sessions)

### Streaming Operations
- `axiom_kafka_streaming` - Kafka Streaming Overview (broker health, topic metrics)
- `axiom_kafka_lag` - Consumer Lag Analysis (lag per group/topic/partition)
- `axiom_kafka_dlq` - Dead Letter Queue Monitoring (DLQ growth, age, replay)

### Security & Compliance
- `axiom_security` - Security Dashboard (failed logins, 403/429 spikes, anomalies)
- `axiom_security_certs` - Certificate Expiry (Nginx, Keycloak, Grafana TLS)

### Audit & Governance
- `axiom_audit` - Audit Dashboard (CRUD events, workflow, bulk operations)
- `axiom_audit_grafana` - Grafana Access Audit (who viewed what dashboard)

---

## 📊 Standard Variables

All dashboards MUST support these variables:

```yaml
Variables:
  - name: tenant
    type: query
    datasource: Prometheus
    query: label_values(http_server_requests_seconds_count, tenant)
    multi: true
    includeAll: true
    
  - name: service
    type: query
    datasource: Prometheus
    query: label_values(http_server_requests_seconds_count{tenant=~"$tenant"}, service)
    multi: true
    includeAll: true
    
  - name: namespace
    type: query
    datasource: Prometheus
    query: label_values(http_server_requests_seconds_count{tenant=~"$tenant",service=~"$service"}, namespace)
    multi: false
    includeAll: false
    
  - name: instance
    type: query
    datasource: Prometheus
    query: label_values(http_server_requests_seconds_count{tenant=~"$tenant",service=~"$service"}, instance)
    multi: true
    includeAll: true
```

**Additional variables** (dashboard-specific):
- `kafka_topic` - Streaming dashboards
- `consumer_group` - Kafka lag/DLQ dashboards
- `endpoint` - HTTP deep dive
- `entity_type` - Audit dashboards

---

## 🔧 Recording Rules

**File structure**: `docker/prometheus/rules/axiom_*.yml`

### axiom_slo.yml
```yaml
groups:
  - name: axiom_slo_recording
    interval: 30s
    rules:
      # Error rate per service (5m window)
      - record: app:http_requests:error_rate5m
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (tenant, service, namespace)
          /
          sum(rate(http_server_requests_seconds_count[5m])) by (tenant, service, namespace)
      
      # SLO burn rate (1h window, 99.9% SLO = 0.1% error budget)
      - record: app:slo:burn_rate1h
        expr: |
          app:http_requests:error_rate5m / 0.001
      
      # Error budget remaining (30d window)
      - record: app:slo:error_budget_remaining
        expr: |
          1 - (
            sum(increase(http_server_requests_seconds_count{status=~"5.."}[30d])) by (tenant, service)
            /
            sum(increase(http_server_requests_seconds_count[30d])) by (tenant, service)
          ) / 0.001
```

### axiom_http.yml
```yaml
groups:
  - name: axiom_http_recording
    interval: 30s
    rules:
      # Latency quantiles
      - record: app:http_requests:latency_p50
        expr: histogram_quantile(0.50, sum(rate(http_server_requests_seconds_bucket[5m])) by (tenant, service, endpoint, le))
      
      - record: app:http_requests:latency_p95
        expr: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (tenant, service, endpoint, le))
      
      - record: app:http_requests:latency_p99
        expr: histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (tenant, service, endpoint, le))
      
      # Request rate by status
      - record: app:http_requests:rate_2xx
        expr: sum(rate(http_server_requests_seconds_count{status=~"2.."}[5m])) by (tenant, service, namespace)
      
      - record: app:http_requests:rate_4xx
        expr: sum(rate(http_server_requests_seconds_count{status=~"4.."}[5m])) by (tenant, service, namespace)
      
      - record: app:http_requests:rate_5xx
        expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (tenant, service, namespace)
```

### axiom_kafka.yml
```yaml
groups:
  - name: axiom_kafka_recording
    interval: 30s
    rules:
      # Consumer lag aggregation
      - record: app:kafka:consumer_lag_total
        expr: sum(kafka_consumer_lag) by (tenant, consumer_group, topic)
      
      # Max lag per topic
      - record: app:kafka:consumer_lag_max
        expr: max(kafka_consumer_lag) by (tenant, consumer_group, topic)
      
      # Produce/consume rate
      - record: app:kafka:produce_rate
        expr: sum(rate(kafka_producer_record_send_total[5m])) by (tenant, topic)
      
      - record: app:kafka:consume_rate
        expr: sum(rate(kafka_consumer_records_consumed_total[5m])) by (tenant, consumer_group, topic)
```

### axiom_db.yml
```yaml
groups:
  - name: axiom_db_recording
    interval: 30s
    rules:
      # Connection pool saturation
      - record: app:db:pool_saturation
        expr: |
          hikaricp_connections_active / hikaricp_connections_max
      
      # Slow query rate (>1s)
      - record: app:db:slow_queries_rate
        expr: |
          sum(rate(http_server_requests_seconds_count{uri=~"/api/.*",http_server_requests_seconds_bucket{le="1.0"} == 0}[5m])) by (tenant, service)
```

### axiom_security.yml
```yaml
groups:
  - name: axiom_security_recording
    interval: 30s
    rules:
      # Failed login rate
      - record: app:security:failed_logins_rate
        expr: sum(rate(keycloak_failed_login_attempts_total[5m])) by (tenant, realm)
      
      # 403 rate (authorization failures)
      - record: app:security:forbidden_rate
        expr: sum(rate(http_server_requests_seconds_count{status="403"}[5m])) by (tenant, service, endpoint)
      
      # 429 rate (rate limit hits)
      - record: app:security:rate_limit_hits
        expr: sum(rate(http_server_requests_seconds_count{status="429"}[5m])) by (tenant, service)
```

---

## 🚨 Alert Rules

**File structure**: `docker/prometheus/alerts/axiom_*.yml`

**Runbook URL template**: `https://docs.core-platform.io/runbooks/<alert_name>`

### axiom_slo_alerts.yml
```yaml
groups:
  - name: axiom_slo_alerts
    rules:
      - alert: SLOBurnRateCritical
        expr: app:slo:burn_rate1h > 14.4
        for: 5m
        labels:
          severity: critical
          category: slo
        annotations:
          summary: "Critical SLO burn rate for {{ $labels.service }}"
          description: "Error rate is {{ $value }}x the error budget (99.9% SLO). Service: {{ $labels.service }}, Tenant: {{ $labels.tenant }}"
          runbook_url: "https://docs.core-platform.io/runbooks/slo-burn-rate-critical"
      
      - alert: ErrorBudgetExhausted
        expr: app:slo:error_budget_remaining < 0.1
        for: 15m
        labels:
          severity: warning
          category: slo
        annotations:
          summary: "Error budget near exhaustion for {{ $labels.service }}"
          description: "Only {{ $value | humanizePercentage }} error budget remaining (99.9% SLO). Service: {{ $labels.service }}"
          runbook_url: "https://docs.core-platform.io/runbooks/error-budget-exhausted"
```

### axiom_http_alerts.yml
```yaml
groups:
  - name: axiom_http_alerts
    rules:
      - alert: HighLatencyP95
        expr: app:http_requests:latency_p95 > 2
        for: 10m
        labels:
          severity: warning
          category: performance
        annotations:
          summary: "High p95 latency for {{ $labels.service }}"
          description: "p95 latency is {{ $value | humanizeDuration }}. Endpoint: {{ $labels.endpoint }}"
          runbook_url: "https://docs.core-platform.io/runbooks/high-latency-p95"
      
      - alert: HighErrorRate
        expr: app:http_requests:error_rate5m > 0.05
        for: 5m
        labels:
          severity: critical
          category: availability
        annotations:
          summary: "High 5xx error rate for {{ $labels.service }}"
          description: "5xx error rate is {{ $value | humanizePercentage }}. Service: {{ $labels.service }}"
          runbook_url: "https://docs.core-platform.io/runbooks/high-error-rate"
```

### axiom_kafka_alerts.yml
```yaml
groups:
  - name: axiom_kafka_alerts
    rules:
      - alert: KafkaConsumerLagHigh
        expr: app:kafka:consumer_lag_total > 10000
        for: 10m
        labels:
          severity: warning
          category: streaming
        annotations:
          summary: "High consumer lag for {{ $labels.consumer_group }}"
          description: "Consumer lag is {{ $value }} messages. Topic: {{ $labels.topic }}, Group: {{ $labels.consumer_group }}"
          runbook_url: "https://docs.core-platform.io/runbooks/kafka-consumer-lag-high"
      
      - alert: KafkaDLQGrowth
        expr: rate(kafka_dlq_messages_total[15m]) > 10
        for: 5m
        labels:
          severity: critical
          category: streaming
        annotations:
          summary: "DLQ growing for {{ $labels.topic }}"
          description: "DLQ receiving {{ $value }} messages/sec. Topic: {{ $labels.topic }}"
          runbook_url: "https://docs.core-platform.io/runbooks/kafka-dlq-growth"
```

### axiom_security_alerts.yml
```yaml
groups:
  - name: axiom_security_alerts
    rules:
      - alert: FailedLoginSpike
        expr: app:security:failed_logins_rate > 10
        for: 5m
        labels:
          severity: warning
          category: security
        annotations:
          summary: "Failed login spike detected"
          description: "{{ $value }} failed logins/sec. Tenant: {{ $labels.tenant }}, Realm: {{ $labels.realm }}"
          runbook_url: "https://docs.core-platform.io/runbooks/failed-login-spike"
      
      - alert: ForbiddenRateHigh
        expr: app:security:forbidden_rate > 5
        for: 5m
        labels:
          severity: warning
          category: security
        annotations:
          summary: "High 403 rate for {{ $labels.service }}"
          description: "{{ $value }} forbidden requests/sec. Endpoint: {{ $labels.endpoint }}"
          runbook_url: "https://docs.core-platform.io/runbooks/forbidden-rate-high"
      
      - alert: TLSCertificateExpiring
        expr: (probe_ssl_earliest_cert_expiry - time()) / 86400 < 30
        for: 1h
        labels:
          severity: warning
          category: security
        annotations:
          summary: "TLS certificate expiring soon"
          description: "Certificate expires in {{ $value }} days. Instance: {{ $labels.instance }}"
          runbook_url: "https://docs.core-platform.io/runbooks/tls-certificate-expiring"
```

---

## 🎨 Dashboard Design Guidelines

### Panel Structure
```yaml
Standard Layout:
  Row 1: KPI Cards (Stat panels)
    - Primary SLO metric
    - Error budget remaining
    - Current error rate
    - p95 latency
    
  Row 2: Time Series (main metrics)
    - Request rate (2xx/4xx/5xx stacked)
    - Latency quantiles (p50/p95/p99)
    
  Row 3: Deep Dive
    - Top N errors table (from Loki)
    - Heatmap (latency distribution)
    
  Row 4: Dependencies
    - DB/Redis/Kafka summary
    - Drilldown links
```

### Color Scheme
```yaml
Success (2xx): Green (#73BF69)
Client Error (4xx): Yellow (#FADE2A)
Server Error (5xx): Red (#F2495C)
Latency p50: Blue (#5794F2)
Latency p95: Orange (#FF9830)
Latency p99: Red (#F2495C)
```

### Drilldown Strategy
- All panels link to Loki with `requestId` preserved
- Time range synchronized across dashboard
- Variables passed in URL: `?var-tenant=$tenant&var-service=$service`

---

## 🔄 Grafana Provisioning (Multi-Tenant)

### Directory Structure
```
docker/grafana/provisioning/
├── datasources/
│   ├── prometheus.yml          # Per-org Prometheus with X-Scope-OrgID
│   └── loki.yml                # Per-org Loki with X-Scope-OrgID
├── dashboards/
│   ├── dashboards.yml          # Folder configuration
│   ├── system/
│   │   ├── axiom_sys_overview.json
│   │   ├── axiom_sys_errors.json
│   │   └── axiom_sys_latency.json
│   ├── advanced/
│   │   ├── axiom_adv_runtime.json
│   │   ├── axiom_adv_http.json
│   │   ├── axiom_adv_db.json
│   │   └── axiom_adv_redis.json
│   ├── streaming/
│   │   ├── axiom_kafka_streaming.json
│   │   ├── axiom_kafka_lag.json
│   │   └── axiom_kafka_dlq.json
│   ├── security/
│   │   ├── axiom_security.json
│   │   └── axiom_security_certs.json
│   └── audit/
│       ├── axiom_audit.json
│       └── axiom_audit_grafana.json
├── alerting/
│   └── rules.yml               # Alert rules provisioning
└── notifiers/
    └── alertmanager.yml        # Alertmanager datasource
```

### datasources/prometheus.yml
```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    jsonData:
      httpHeaderName1: 'X-Scope-OrgID'
    secureJsonData:
      httpHeaderValue1: '$__org.id'
    isDefault: true
    editable: false
```

### datasources/loki.yml
```yaml
apiVersion: 1
datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    jsonData:
      httpHeaderName1: 'X-Scope-OrgID'
      derivedFields:
        - name: requestId
          matcherRegex: 'requestId=([\\w-]+)'
          url: '/explore?left=["now-1h","now","Loki",{"expr":"{requestId=\"$${__value.raw}\"}"}]'
    secureJsonData:
      httpHeaderValue1: '$__org.id'
    editable: false
```

### dashboards/dashboards.yml
```yaml
apiVersion: 1
providers:
  - name: 'System Monitoring'
    orgId: 1
    folder: 'System Monitoring'
    type: file
    disableDeletion: true
    editable: false
    updateIntervalSeconds: 30
    options:
      path: /etc/grafana/provisioning/dashboards/system
      
  - name: 'Advanced Monitoring'
    orgId: 1
    folder: 'Advanced Monitoring'
    type: file
    disableDeletion: true
    editable: false
    updateIntervalSeconds: 30
    options:
      path: /etc/grafana/provisioning/dashboards/advanced
      
  - name: 'Streaming Operations'
    orgId: 1
    folder: 'Streaming Operations'
    type: file
    disableDeletion: true
    editable: false
    updateIntervalSeconds: 30
    options:
      path: /etc/grafana/provisioning/dashboards/streaming
      
  - name: 'Security & Compliance'
    orgId: 1
    folder: 'Security & Compliance'
    type: file
    disableDeletion: true
    editable: false
    updateIntervalSeconds: 30
    options:
      path: /etc/grafana/provisioning/dashboards/security
      
  - name: 'Audit & Governance'
    orgId: 1
    folder: 'Audit & Governance'
    type: file
    disableDeletion: true
    editable: false
    updateIntervalSeconds: 30
    options:
      path: /etc/grafana/provisioning/dashboards/audit
```

---

## 🌐 Frontend Integration

### Navigation Structure
```typescript
// frontend/src/routes/monitoring/+layout.svelte
<nav class="monitoring-tabs">
  <a href="/monitoring/system">System</a>
  <a href="/monitoring/advanced">Advanced</a>
  <a href="/monitoring/streaming">Streaming</a>
  <a href="/monitoring/security">Security</a>
  <a href="/monitoring/audit">Audit</a>
</nav>
```

### Dashboard Embed Template
```svelte
<!-- frontend/src/routes/monitoring/system/+page.svelte -->
<script lang="ts">
  import { page } from '$app/stores';
  
  $: tenant = $page.data.session.tenant;
  $: dashboardUrl = `/core-admin/monitoring/d/axiom_sys_overview?orgId=1&var-tenant=${tenant}&kiosk=tv`;
</script>

<div class="dashboard-container">
  <iframe 
    src={dashboardUrl}
    title="System Overview"
    sandbox="allow-same-origin allow-scripts"
  />
  
  <a href="/core-admin/monitoring/d/axiom_sys_overview" target="_blank">
    Open in Grafana
  </a>
</div>
```

---

## 📝 Backend Logging & Metrics Requirements

### MDC (Mapped Diagnostic Context)
```java
// Ensure all logs include:
MDC.put("tenant", tenant);
MDC.put("service", "core-backend");
MDC.put("requestId", UUID.randomUUID().toString());
MDC.put("userId", authentication.getName());
```

### Loki Log Parsing
```yaml
# docker/loki/loki.yml
pipeline_stages:
  - regex:
      expression: '.*tenant=(?P<tenant>[^\s]+).*service=(?P<service>[^\s]+).*requestId=(?P<requestId>[^\s]+).*'
  - labels:
      tenant:
      service:
      requestId:
```

### Micrometer Metrics
```java
// Existing metrics to verify:
- http.server.requests (uri, method, status, tenant, service)
- jvm.memory.used / jvm.memory.max
- jvm.gc.pause
- hikaricp.connections.active / hikaricp.connections.max
- redis.commands
- kafka.consumer.lag
- kafka.producer.record.send.total

// Business metrics to add:
- audit.events.total (entity_type, operation)
- workflow.transitions.total (from_state, to_state)
- bulk.operations.total (entity_type, count)
```

---

## ✅ Testing Strategy

### Pre-Deploy (CI)
```bash
# Validate provisioning
docker compose exec grafana grafana-cli admin data-source list
docker compose exec grafana grafana-cli admin folder list
docker compose exec grafana grafana-cli admin dashboard list

# Headless render test
for uid in axiom_sys_overview axiom_adv_runtime axiom_kafka_lag axiom_security axiom_audit; do
  curl -s "http://grafana:3000/render/d/$uid?orgId=1&width=1920&height=1080" > /tmp/$uid.png
  [ -f /tmp/$uid.png ] && echo "✅ $uid rendered" || echo "❌ $uid failed"
done

# Alert rules lint
docker compose exec prometheus promtool check rules /etc/prometheus/alerts/*.yml

# Loki query smoke test
curl -s "http://loki:3100/loki/api/v1/query?query={tenant=\"default\"}" | jq '.status'
```

### Post-Deploy (Staging)
```bash
# Simulate 5xx spike
hey -n 1000 -c 10 http://backend:8080/api/force-error

# Check alert fired
curl -s http://prometheus:9090/api/v1/alerts | jq '.data.alerts[] | select(.labels.alertname=="HighErrorRate")'

# Verify annotation created
curl -s http://grafana:3000/api/annotations?tags=ci-deployment

# Simulate Kafka lag
docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group test-group --describe

# Verify dashboard panel
curl -s "http://grafana:3000/api/dashboards/uid/axiom_kafka_lag" | jq '.dashboard.panels[] | select(.title=="Consumer Lag")'

# Audit CRUD event
curl -X POST http://backend:8080/api/entities -d '{"name":"test"}'

# Verify in Loki
curl -s "http://loki:3100/loki/api/v1/query?query={entity_type=\"entity\",operation=\"CREATE\"}" | jq '.data.result'
```

---

## 📚 Runbooks

### High-Level Runbooks (docs/runbooks/)

#### slo-burn-rate-critical.md
```markdown
# SLO Burn Rate Critical

**Severity**: Critical  
**Category**: SLO

## Symptoms
- Error rate is >14.4x the error budget (99.9% SLO)
- Multiple 5xx errors in short time window

## Impact
- Service availability degraded
- Error budget exhausted rapidly
- Potential customer impact

## Investigation
1. Check System Overview dashboard: `/d/axiom_sys_overview`
2. Identify service with high error rate
3. Drill into Error Analysis: `/d/axiom_sys_errors`
4. Check Loki for error patterns: `{service="<service>",status=~"5.."} | json`

## Mitigation
1. Check recent deployments (rollback if recent change)
2. Check DB/Redis/Kafka health
3. Increase instance count if CPU/memory saturated
4. Enable circuit breaker if dependency failing

## Prevention
- Review error logs for patterns
- Add regression tests for common errors
- Improve monitoring coverage for dependencies
```

#### kafka-consumer-lag-high.md
```markdown
# Kafka Consumer Lag High

**Severity**: Warning  
**Category**: Streaming

## Symptoms
- Consumer lag > 10,000 messages
- Processing rate < produce rate

## Impact
- Delayed message processing
- Potential data staleness
- Risk of partition rebalancing

## Investigation
1. Check Kafka Lag dashboard: `/d/axiom_kafka_lag`
2. Identify slow consumer group
3. Check consumer logs: `{consumer_group="<group>"} | json | line_format "{{.message}}"`
4. Verify consumer instances alive: `kafka_consumer_group_members{group="<group>"}`

## Mitigation
1. Scale consumer group (add instances)
2. Check for slow processing (DB locks, external API timeouts)
3. Increase consumer fetch size if network bound
4. Pause non-critical consumers temporarily

## Prevention
- Monitor lag trends
- Auto-scale consumers based on lag
- Optimize message processing
- Use batch processing where possible
```

---

## 🚀 Onboarding New Tenant

1. **Create Grafana Org**
   ```bash
   curl -X POST http://grafana:3000/api/orgs \
     -H 'Content-Type: application/json' \
     -d '{"name": "TenantCorp"}'
   ```

2. **Provision Datasources**
   - Datasources auto-provisioned with `X-Scope-OrgID: $__org.id`

3. **Verify Dashboards Imported**
   ```bash
   curl -s http://grafana:3000/api/search?orgId=<new-org-id> | jq '.[] | .uid'
   ```

4. **Configure Alerts**
   - Alerts inherit from provisioned rules
   - Customize notification channels per org

5. **Set Frontend Variables**
   ```typescript
   // Default tenant in dashboard URLs
   const dashboardUrl = `/core-admin/monitoring/d/axiom_sys_overview?orgId=${orgId}&var-tenant=${tenantId}`;
   ```

---

## 🔧 Troubleshooting

### Missing `tenant` Label
**Symptom**: Dashboard shows "No data"  
**Cause**: Metrics missing `tenant` label  
**Fix**: Ensure MDC context in backend, verify metric tags in Micrometer

### Incorrect Dashboard UID
**Symptom**: 404 when embedding dashboard  
**Cause**: UID mismatch or dashboard not provisioned  
**Fix**: Check `docker/grafana/provisioning/dashboards/<category>/<uid>.json`

### Recording Rule Missing
**Symptom**: Alert doesn't fire despite condition met  
**Cause**: Recording rule not loaded  
**Fix**: `promtool check rules` + restart Prometheus

### Loki Query Timeout
**Symptom**: Dashboard panels time out  
**Cause**: Too broad query, no label filters  
**Fix**: Add `{tenant="<tenant>"}` filter, reduce time range

---

## 📊 Metrics Inventory

### HTTP Metrics
- `http_server_requests_seconds_count` (uri, method, status, tenant, service)
- `http_server_requests_seconds_sum`
- `http_server_requests_seconds_bucket`

### JVM Metrics
- `jvm_memory_used_bytes` (area, id)
- `jvm_memory_max_bytes`
- `jvm_gc_pause_seconds_count` (action, cause)
- `jvm_threads_live`

### Database Metrics
- `hikaricp_connections_active`
- `hikaricp_connections_max`
- `hikaricp_connections_pending`

### Redis Metrics
- `redis_commands_total` (command, status)
- `redis_keyspace_hits_total`
- `redis_keyspace_misses_total`

### Kafka Metrics
- `kafka_consumer_lag` (group, topic, partition)
- `kafka_producer_record_send_total` (topic)
- `kafka_consumer_records_consumed_total` (group, topic)

### Security Metrics
- `keycloak_failed_login_attempts_total` (realm, tenant)
- `http_server_requests_seconds_count{status="403"}` (forbidden)
- `http_server_requests_seconds_count{status="429"}` (rate limited)

---

## 📅 Maintenance

### Weekly
- Review alert noise (false positives)
- Check dashboard rendering performance
- Verify recording rules coverage

### Monthly
- Audit dashboard usage (Grafana access logs)
- Review SLO targets (adjust if needed)
- Update runbooks with new learnings

### Quarterly
- Add new dashboards for new features
- Retire unused dashboards
- Update alert thresholds based on trends

---

**Version**: 1.0  
**Last Updated**: 2025-10-20  
**Maintained by**: Platform Team
