# Reporting & Analytics Module - Operations Runbook

**Version**: 1.0  
**Date**: 2025-01-10  
**Module**: Reporting & Analytics (Phase 3)

## Table of Contents

1. [Quick Start](#quick-start)
2. [Architecture Overview](#architecture-overview)
3. [Configuration](#configuration)
4. [Deployment](#deployment)
5. [Monitoring](#monitoring)
6. [Troubleshooting](#troubleshooting)
7. [Maintenance](#maintenance)
8. [Emergency Procedures](#emergency-procedures)

---

## Quick Start

### Enable Reporting Module

```yaml
# application-reporting.yml
reporting:
  enabled: true
```

### Execute Query

```bash
curl -X POST http://localhost:8080/api/reports/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "entity": "User",
    "dimensions": ["status"],
    "measures": [{"field": "id", "aggregation": "count"}],
    "timeRange": {
      "start": "2025-01-01T00:00:00Z",
      "end": "2025-01-10T23:59:59Z"
    },
    "limit": 1000
  }'
```

### Check Health

```bash
curl http://localhost:8080/api/reports/health
```

---

## Architecture Overview

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ POST /api/reports/query
       ▼
┌─────────────────────────┐
│  ReportQueryController  │
│  - Rate limit (120/min) │
│  - Security validation  │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────┐
│   ReportQueryService    │
│  - Cache check (Redis)  │
│  - Fingerprint gen      │
│  - Metrics recording    │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────┐
│     CubeMapper          │
│  - DSL → Cube.js query  │
│  - RLS filter injection │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────┐
│      CubeClient         │
│  - HTTP POST to Cube.js │
│  - Error handling       │
│  - Retry logic          │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────┐
│      Cube.js API        │
│  - Semantic layer       │
│  - SQL generation       │
│  - Query execution      │
└─────────────────────────┘
```

### Components

| Component | Purpose | Tech Stack |
|-----------|---------|------------|
| **ReportQueryController** | REST API endpoint | Spring MVC |
| **ReportQueryService** | Business logic, cache | Spring Cache |
| **CubeMapper** | Query translation | Custom |
| **CubeClient** | HTTP client | RestClient |
| **RateLimitFilter** | Traffic control | Bucket4j |
| **ReportingMetrics** | Observability | Micrometer |
| **Cache** | Performance | Redis/Caffeine |
| **Cube.js** | Semantic layer | Node.js |

---

## Configuration

### application-reporting.yml

```yaml
reporting:
  enabled: true
  max-rows: 50000
  max-interval-days: 92
  default-ttl-seconds: 60
  
  cache:
    provider: redis  # or: caffeine
    key-prefix: "rpt:"
  
  rate-limit:
    per-tenant-per-min: 120
  
  cube:
    base-url: http://cube:4000
    api-token: ${CUBE_API_TOKEN}
    connect-timeout-ms: 5000
    read-timeout-ms: 30000
  
  bulk:
    chunk-size: 1000
    max-affect-rows: 500000
    queue-concurrency: 2
    timeout-seconds: 300
```

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `CUBE_BASE_URL` | ✅ | `http://cube:4000` | Cube.js API URL |
| `CUBE_API_TOKEN` | ❌ | - | Bearer token for Cube.js |
| `REDIS_HOST` | ✅ | `localhost` | Redis host |
| `REDIS_PORT` | ✅ | `6379` | Redis port |
| `REDIS_PASSWORD` | ❌ | - | Redis password |

### Feature Toggles

```java
@ConditionalOnProperty(name = "reporting.enabled", havingValue = "true")
```

Disable reporting:

```yaml
reporting:
  enabled: false
```

---

## Deployment

### Docker Compose

```yaml
services:
  backend:
    image: core-platform/backend:latest
    environment:
      - REPORTING_ENABLED=true
      - CUBE_BASE_URL=http://cube:4000
      - CUBE_API_TOKEN=${CUBE_API_TOKEN}
      - REDIS_HOST=redis
    depends_on:
      - redis
      - cube

  cube:
    image: cubejs/cube:latest
    ports:
      - "4000:4000"
    environment:
      - CUBEJS_DB_TYPE=postgres
      - CUBEJS_DB_HOST=postgres
      - CUBEJS_DB_NAME=${DB_NAME}
      - CUBEJS_DB_USER=${DB_USER}
      - CUBEJS_DB_PASS=${DB_PASSWORD}

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-reporting
spec:
  replicas: 3
  selector:
    matchLabels:
      app: backend
      module: reporting
  template:
    spec:
      containers:
      - name: backend
        image: core-platform/backend:latest
        env:
        - name: REPORTING_ENABLED
          value: "true"
        - name: CUBE_BASE_URL
          value: "http://cube-service:4000"
        - name: REDIS_HOST
          value: "redis-service"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
```

### Health Checks

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
```

---

## Monitoring

### Key Metrics (Prometheus)

```promql
# Query rate
rate(reporting_query_requests_total[1m])

# Cache hit rate
100 * rate(reporting_cache_hits_total[5m]) / 
  (rate(reporting_cache_hits_total[5m]) + rate(reporting_cache_misses_total[5m]))

# Error rate
rate(reporting_query_errors_total{error_type="server_error"}[5m])

# p95 latency
histogram_quantile(0.95, rate(reporting_query_duration_bucket[5m]))

# Rate limit violations
rate(reporting_ratelimit_exceeded_total[1m])
```

### Alerts

```yaml
groups:
- name: reporting_alerts
  rules:
  - alert: HighErrorRate
    expr: rate(reporting_query_errors_total[5m]) > 0.05
    for: 5m
    annotations:
      summary: "High error rate in reporting module"

  - alert: LowCacheHitRate
    expr: |
      100 * rate(reporting_cache_hits_total[10m]) /
      (rate(reporting_cache_hits_total[10m]) + rate(reporting_cache_misses_total[10m])) < 30
    for: 10m
    annotations:
      summary: "Cache hit rate below 30%"

  - alert: SlowQueries
    expr: histogram_quantile(0.95, rate(reporting_query_duration_bucket[5m])) > 5
    for: 5m
    annotations:
      summary: "p95 query latency above 5 seconds"
```

### Logs

Structured logging with MDC:

```
2025-01-10 12:34:56 [http-nio-8080-exec-1] INFO  c.m.c.r.app.ReportQueryService - 
  [requestId=abc123] [tenantId=tenant-1] [userId=user-456] - 
  Executing report query: entity=User, measures=[count]
```

**Key log patterns**:

```bash
# Cache hits
grep "Cache HIT" logs/backend.log

# Cache misses
grep "Cache MISS" logs/backend.log

# Rate limit violations
grep "Rate limit exceeded" logs/backend.log

# Cube.js errors
grep "Cube.js.*error" logs/backend.log
```

---

## Troubleshooting

### High Latency

**Symptoms**: p95 latency > 5s

**Diagnosis**:

```bash
# Check Cube.js health
curl http://cube:4000/health

# Check Redis latency
redis-cli --latency

# Check DB connections
curl http://localhost:8080/actuator/metrics/hikari.connections.active
```

**Solutions**:

1. Scale Cube.js replicas
2. Increase Redis memory
3. Increase DB connection pool
4. Add query complexity limits

### Cache Misses

**Symptoms**: Cache hit rate < 30%

**Diagnosis**:

```bash
# Check Redis memory
redis-cli info memory

# Check cache evictions
redis-cli info stats | grep evicted_keys

# Check TTL
curl http://localhost:8080/actuator/configprops | jq '.reporting'
```

**Solutions**:

1. Increase Redis `maxmemory`
2. Increase TTL from 60s to 300s
3. Change eviction policy to `allkeys-lru`
4. Scale Redis (cluster mode)

### Rate Limit Errors

**Symptoms**: Many 429 responses

**Diagnosis**:

```bash
# Check rate limit metrics
curl http://localhost:8080/actuator/metrics/reporting.ratelimit.exceeded

# Check tenant activity
grep "tenant_id" logs/backend.log | sort | uniq -c | sort -nr
```

**Solutions**:

1. Increase limit: `per-tenant-per-min: 240`
2. Add tenant-specific limits
3. Implement query batching
4. Cache more aggressively

### Cube.js Connection Errors

**Symptoms**: `Cube.js server error` in logs

**Diagnosis**:

```bash
# Check Cube.js logs
docker logs cube --tail 100

# Test Cube.js directly
curl -X POST http://cube:4000/cubejs-api/v1/load \
  -H "Authorization: Bearer $CUBE_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"measures":["Users.count"]}'
```

**Solutions**:

1. Restart Cube.js: `docker restart cube`
2. Check DB connectivity from Cube.js
3. Verify `CUBE_API_TOKEN` matches
4. Check Cube.js schema files

---

## Maintenance

### Cache Invalidation

```bash
# Invalidate all reports
curl -X POST http://localhost:8080/api/admin/cache/clear \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Invalidate by entity
curl -X POST http://localhost:8080/api/admin/cache/invalidate/entity/User \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Invalidate by tenant
curl -X POST http://localhost:8080/api/admin/cache/invalidate/tenant/tenant-123 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Cube.js Schema Updates

After updating Cube.js schema:

```bash
# Restart Cube.js to reload schema
docker restart cube

# Clear cache to force re-query
redis-cli FLUSHDB
```

### Database Migrations

If adding new reporting tables:

```sql
-- Add to V1__init.sql
CREATE TABLE new_reporting_table (...);
```

Then:

```bash
./mvnw flyway:migrate
```

---

## Emergency Procedures

### 1. Disable Reporting Module

If reporting is causing backend instability:

```yaml
# application-reporting.yml
reporting:
  enabled: false
```

Restart backend:

```bash
kubectl rollout restart deployment/backend
```

### 2. Fallback to Caffeine Cache

If Redis is down:

```yaml
reporting:
  cache:
    provider: caffeine
```

### 3. Circuit Breaker (Manual)

If Cube.js is unresponsive:

```bash
# Scale Cube.js to 0
kubectl scale deployment cube --replicas=0

# Set reporting.enabled=false
kubectl set env deployment/backend REPORTING_ENABLED=false
```

### 4. Emergency Rate Limit Reduction

If under DDoS:

```yaml
reporting:
  rate-limit:
    per-tenant-per-min: 10  # Reduce from 120
```

---

## Contacts

| Role | Contact | Escalation |
|------|---------|------------|
| **Module Owner** | Backend Team | #backend-team |
| **On-Call** | SRE Team | PagerDuty |
| **Cube.js Expert** | Data Team | #data-team |
| **Redis Expert** | Infrastructure Team | #infrastructure |

---

## References

- [Phase 3 Implementation Plan](./PHASE_3_IMPLEMENTATION_PLAN.md)
- [Reporting README](./REPORTING_README.md)
- [Load Testing Guide](./PHASE_3_10_LOAD_TESTING.md)
- [Cube.js Documentation](https://cube.dev/docs)
- [Bucket4j Documentation](https://bucket4j.com/)
