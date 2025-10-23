# 📊 Grafana Authentication Observability - Implementation Plan

## Overview
Comprehensive monitoring for Grafana JWT authentication including JWKS refresh health, auth error tracking, and alerting.

## 1. Loki Label Configuration

### Backend Application Logs
Add structured labels to Spring Boot logging for Loki ingestion:

```yaml
# backend/src/main/resources/logback-spring.xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
        <url>http://loki:3100/loki/api/v1/push</url>
    </http>
    <format>
        <label>
            <pattern>app=auth,environment=${ENVIRONMENT:-dev},host=${HOSTNAME}</pattern>
        </label>
        <message>
            <pattern>
{
  "level": "%level",
  "logger": "%logger{36}",
  "thread": "%thread",
  "message": "%message",
  "exception": "%exception{full}",
  "mdc": {
    "userId": "%X{userId}",
    "tenantId": "%X{tenantId}",
    "requestId": "%X{requestId}",
    "authType": "%X{authType}"
  }
}
            </pattern>
        </message>
    </format>
</appender>
```

### Key MDC Context Fields
- `authType`: "keycloak", "jwt", "session"
- `userId`: Authenticated user ID
- `tenantId`: Current tenant context
- `requestId`: Request correlation ID
- `errorCode`: Auth error classification (INVALID_TOKEN, EXPIRED_SESSION, etc.)

### Auth-Specific Log Patterns
```java
// GrafanaAuthController.java
log.info("JWT minted for user: {}", username);
MDC.put("authType", "jwt");
MDC.put("userId", user.getId());
MDC.put("tenantId", session.getTenantId());

// On errors
log.warn("JWT validation failed: {}", errorReason);
MDC.put("errorCode", "INVALID_JWT_SIGNATURE");
```

## 2. JWKS Refresher Metrics

### Add Health Metrics to fetch-jwks.sh
Extend script to write metrics file for Prometheus node_exporter textfile collector:

```bash
# In fetch-jwks.sh after successful fetch:
cat > /var/lib/grafana/metrics/jwks_refresh.prom <<EOF
# HELP jwks_refresh_last_success_timestamp_seconds Last successful JWKS refresh
# TYPE jwks_refresh_last_success_timestamp_seconds gauge
jwks_refresh_last_success_timestamp_seconds $(date +%s)

# HELP jwks_refresh_total Total JWKS refresh attempts
# TYPE jwks_refresh_total counter
jwks_refresh_total{status="success"} $SUCCESS_COUNT

# HELP jwks_keys_count Number of keys in JWKS
# TYPE jwks_keys_count gauge
jwks_keys_count $KIDS_COUNT
EOF
```

### Add Node Exporter to Docker Compose
```yaml
node-exporter:
  image: prom/node-exporter:v1.8.2
  container_name: core-node-exporter
  volumes:
    - grafana_data:/var/lib/grafana:ro
  command:
    - '--path.rootfs=/host'
    - '--collector.textfile.directory=/var/lib/grafana/metrics'
  networks:
    - core-net
```

## 3. Grafana Dashboard: Auth Health

Create dashboard JSON: `/docker/grafana/provisioning/dashboards/auth-health.json`

### Panels

#### 3.1 JWKS Refresh Status
```promql
# Time since last successful refresh
(time() - jwks_refresh_last_success_timestamp_seconds) / 60
```
- **Alert**: > 15 minutes (3x refresh interval)
- **Visualization**: Stat panel with thresholds (green <10, yellow <15, red >15)

#### 3.2 Auth Errors by Type
```logql
# Loki query for auth errors
{app="auth"} |~ "error|failed|invalid" 
| json 
| errorCode != "" 
| unwrap errorCode 
| __error__=""
```
- **Group by**: errorCode
- **Visualization**: Time series with legend

#### 3.3 JWT Validation Success Rate
```logql
# Success rate calculation
sum(rate({app="auth"} | json | message =~ "JWT minted" [5m])) 
/ 
sum(rate({app="auth"} | json | message =~ "JWT" [5m]))
```
- **Visualization**: Gauge (0-100%)
- **Alert**: < 95%

#### 3.4 Session Creation Rate
```logql
rate({app="auth"} | json | message =~ "Session created" [5m])
```
- **Visualization**: Time series
- **Use**: Identify traffic patterns

#### 3.5 Top Auth Error Messages
```logql
topk(10, 
  sum by (message) (
    rate({app="auth", level="ERROR"} [15m])
  )
)
```
- **Visualization**: Bar chart
- **Use**: Quick error triage

#### 3.6 JWKS Key Count
```promql
jwks_keys_count
```
- **Alert**: < 1 (no keys loaded)
- **Visualization**: Stat panel

## 4. Alerting Rules

### 4.1 JWKS Staleness Alert
```yaml
# /docker/grafana/provisioning/alerting/auth-alerts.yml
apiVersion: 1
groups:
  - orgId: 1
    name: auth-health
    folder: Auth
    interval: 1m
    rules:
      - uid: jwks-stale
        title: JWKS Refresh Stale
        condition: A
        data:
          - refId: A
            queryType: prometheus
            model: |
              (time() - jwks_refresh_last_success_timestamp_seconds) / 3600
        annotations:
          summary: "JWKS has not been refreshed in over 1 hour"
          description: "Last successful refresh: {{ $value }} hours ago"
        labels:
          severity: critical
          component: auth
        for: 5m
```

### 4.2 Auth Error Spike Alert
```yaml
- uid: auth-error-spike
  title: Authentication Error Spike
  condition: A
  data:
    - refId: A
      queryType: loki
      model: |
        sum(rate({app="auth", level="ERROR"} [5m]))
  annotations:
    summary: "Auth error rate > 10/min"
  labels:
    severity: warning
  for: 5m
  noDataState: OK
  execErrState: Error
```

### 4.3 JWT Validation Failure Rate
```yaml
- uid: jwt-validation-failures
  title: High JWT Validation Failure Rate
  condition: A
  data:
    - refId: A
      queryType: loki
      model: |
        (
          sum(rate({app="auth"} | json | errorCode =~ "INVALID_JWT.*" [5m]))
          /
          sum(rate({app="auth"} | json | message =~ "JWT" [5m]))
        ) * 100
  annotations:
    summary: "JWT validation failure rate > 5%"
    description: "Current rate: {{ $value }}%"
  labels:
    severity: warning
  for: 10m
```

## 5. Implementation Checklist

### Phase 1: Logging Enhancement
- [ ] Add Loki appender to backend Spring Boot
- [ ] Implement MDC context in auth controllers
- [ ] Add structured error codes for auth failures
- [ ] Test Loki ingestion with label filters

### Phase 2: Metrics Collection
- [ ] Extend fetch-jwks.sh with metrics export
- [ ] Add node-exporter to docker-compose.yml
- [ ] Create shared volume for metrics
- [ ] Verify Prometheus scraping

### Phase 3: Dashboard & Alerts
- [ ] Create auth-health.json dashboard
- [ ] Add dashboard to provisioning
- [ ] Create auth-alerts.yml with 3 alert rules
- [ ] Test alert firing with simulated failures

### Phase 4: Testing & Validation
- [ ] Generate auth errors to test Loki queries
- [ ] Verify dashboard panels display data
- [ ] Test alert triggers (stop jwks-refresher, inject errors)
- [ ] Document runbook for alert resolution

## 6. Grafana Dashboard Provisioning

```yaml
# /docker/grafana/provisioning/dashboards/auth.yml
apiVersion: 1

providers:
  - name: 'Auth Dashboards'
    orgId: 1
    folder: 'Authentication'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 30
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards/auth
```

## 7. Expected Outcomes

✅ **Visibility**: Real-time auth health monitoring  
✅ **Alerting**: Proactive notification on JWKS staleness or auth failures  
✅ **Debugging**: Structured logs for error triage  
✅ **Metrics**: Historical tracking of auth success rates  
✅ **Production-Ready**: Observability integrated into deploy pipeline
