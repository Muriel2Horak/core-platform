# 📊 Cube.js Setup & Configuration Guide

> **Last Updated:** 11. října 2025  
> **Version:** 1.0.0  
> **Status:** ✅ Production Ready

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Installation](#installation)
4. [Configuration](#configuration)
5. [Schema Development](#schema-development)
6. [Row-Level Security (RLS)](#row-level-security-rls)
7. [Pre-Aggregations](#pre-aggregations)
8. [Testing](#testing)
9. [Monitoring](#monitoring)
10. [Troubleshooting](#troubleshooting)

---

## Overview

Cube.js serves as the **semantic layer** for the Core Platform reporting module, providing:

- **Unified API** for data queries across all entities
- **Row-Level Security (RLS)** enforcing tenant isolation
- **Pre-aggregations** for performance optimization
- **Caching** via Redis for fast query responses
- **Developer Experience** with SQL-based schema definitions

### Key Benefits

- ✅ **Multi-tenancy** - Automatic tenant filtering via `SECURITY_CONTEXT`
- ✅ **Performance** - Pre-computed rollups refresh hourly/daily
- ✅ **Security** - RLS prevents cross-tenant data leaks
- ✅ **Scalability** - Redis-backed caching and queue system
- ✅ **Flexibility** - GraphQL-like query DSL with dimensions/measures

---

## Architecture

```
┌─────────────┐
│   Frontend  │
│  (React)    │
└──────┬──────┘
       │ HTTP
       ▼
┌─────────────────┐
│  Backend BFF    │
│  (Spring Boot)  │
└────────┬────────┘
         │ HTTP
         ▼
┌──────────────────┐     ┌──────────┐
│    Cube.js       │────▶│  Redis   │
│ (Semantic Layer) │     │ (Cache)  │
└────────┬─────────┘     └──────────┘
         │ SQL
         ▼
┌──────────────────┐
│   PostgreSQL     │
│   (Core DB)      │
└──────────────────┘
```

### Data Flow

1. **Frontend** sends query to `/api/reports/query` (BFF)
2. **BFF** extracts tenant ID from JWT and forwards to Cube.js
3. **Cube.js** applies RLS filter, checks cache
4. **Cache miss** → SQL query to PostgreSQL → cache result
5. **Cache hit** → return cached data (60s TTL)
6. **Response** flows back through BFF to frontend

---

## Installation

### 1. Docker Compose Setup

Cube.js is already configured in `docker/docker-compose.yml`:

```yaml
cube:
  image: cubejs/cube:latest
  container_name: core-cube
  ports:
    - "4000:4000"
  environment:
    - CUBEJS_DB_TYPE=postgres
    - CUBEJS_DB_HOST=db
    - CUBEJS_DB_NAME=core
    - CUBEJS_API_SECRET=${CUBE_API_SECRET}
    - CUBEJS_REDIS_URL=redis://redis:6379
  volumes:
    - ./cube/schema:/cube/conf/schema:ro
  depends_on:
    - db
    - redis
```

### 2. Environment Variables

Add to `.env`:

```bash
CUBE_PORT=4000
CUBE_API_SECRET=dev_secret_change_in_production_min_32_chars
CUBE_DEV_MODE=true
```

⚠️ **Production:** Set `CUBE_DEV_MODE=false` and use a strong secret (32+ chars).

### 3. Start Services

```bash
# Start all services including Cube.js
docker compose up -d

# Check Cube.js health
curl http://localhost:4000/cubejs-api/v1/meta
```

Expected response:
```json
{
  "cubes": [
    {
      "name": "Users",
      "title": "Users",
      "measures": [...],
      "dimensions": [...]
    },
    ...
  ]
}
```

---

## Configuration

### Database Connection

Cube.js connects to the same PostgreSQL database as the backend:

```javascript
// Configured via environment variables
CUBEJS_DB_TYPE=postgres
CUBEJS_DB_HOST=db
CUBEJS_DB_PORT=5432
CUBEJS_DB_NAME=core
CUBEJS_DB_USER=core
CUBEJS_DB_PASS=core
```

### Caching Strategy

```javascript
// Redis-based caching
CUBEJS_REDIS_URL=redis://redis:6379
CUBEJS_CACHE_AND_QUEUE_DRIVER=redis

// Cache TTL (default 60s)
// Configured in BFF ReportingProperties
```

### API Security

```javascript
CUBEJS_API_SECRET=<strong-secret>

// In production, use Kubernetes secrets:
kubectl create secret generic cube-secrets \
  --from-literal=api-secret=$(openssl rand -base64 32)
```

---

## Schema Development

### File Structure

```
docker/cube/schema/
├── Users.js       # User directory cube
├── Tenants.js     # Tenant registry cube
├── Groups.js      # Keycloak groups cube
└── README.md      # Schema documentation
```

### Basic Cube Template

```javascript
cube(`EntityName`, {
  sql: `
    SELECT * FROM table_name
    WHERE tenant_id = ${SECURITY_CONTEXT.tenantId.filter('tenant_id')}
  `,
  
  dimensions: {
    id: {
      sql: `id`,
      type: `string`,
      primaryKey: true
    },
    
    name: {
      sql: `name`,
      type: `string`
    }
  },
  
  measures: {
    count: {
      type: `count`
    }
  },
  
  preAggregations: {
    main: {
      measures: [count],
      dimensions: [name],
      refreshKey: {
        every: `1 hour`
      }
    }
  }
});
```

### Dimension Types

| Type | SQL Example | Use Case |
|------|-------------|----------|
| `string` | `username` | Text fields |
| `number` | `version` | Numeric fields |
| `boolean` | `active` | True/false flags |
| `time` | `created_at` | Timestamps |
| `geo` | `latitude, longitude` | Geographic data |

### Measure Types

| Type | Description | Example |
|------|-------------|---------|
| `count` | Row count | `SELECT COUNT(*)` |
| `sum` | Summation | `SELECT SUM(amount)` |
| `avg` | Average | `SELECT AVG(score)` |
| `min` | Minimum | `SELECT MIN(date)` |
| `max` | Maximum | `SELECT MAX(date)` |
| `countDistinct` | Unique count | `SELECT COUNT(DISTINCT user_id)` |

---

## Row-Level Security (RLS)

### Implementation

Every cube **MUST** filter by `tenant_id` to enforce multi-tenancy:

```javascript
cube(`Users`, {
  sql: `
    SELECT * FROM users_directory
    WHERE tenant_id = ${SECURITY_CONTEXT.tenantId.filter('tenant_id')}
      AND deleted_at IS NULL
  `
});
```

### Security Context Flow

1. **JWT Token** arrives at BFF with claim `tenant: "test-tenant"`
2. **BFF** extracts tenant ID via `TenantContext.getCurrentTenantId()`
3. **Cube.js Request** includes header `X-Tenant-Id: <uuid>`
4. **Cube Schema** applies `SECURITY_CONTEXT.tenantId.filter()`
5. **SQL Query** includes `WHERE tenant_id = '<uuid>'`

### Testing RLS

```bash
# Query with Tenant A token
curl -H "Authorization: Bearer $TOKEN_A" \
  http://localhost:8080/api/reports/query \
  -d '{"entity":"User","measures":["count"]}'
  
# Returns only Tenant A data

# Query with Tenant B token
curl -H "Authorization: Bearer $TOKEN_B" \
  http://localhost:8080/api/reports/query \
  -d '{"entity":"User","measures":["count"]}'
  
# Returns only Tenant B data
```

### RLS Validation Checklist

- [ ] Every cube SQL includes `tenant_id` filter
- [ ] `SECURITY_CONTEXT.tenantId` is used (not hardcoded)
- [ ] Joins maintain tenant isolation
- [ ] Integration tests verify cross-tenant protection

---

## Pre-Aggregations

### Why Pre-Aggregations?

- **10-100x faster** queries for common reports
- **Reduced database load** - queries hit Redis cache
- **Automatic refresh** - incremental updates every 1-6 hours

### Example: Daily Status Counts

```javascript
preAggregations: {
  dailyStatusCounts: {
    measures: [count, activeCount, inactiveCount],
    dimensions: [status],
    timeDimension: createdAt,
    granularity: `day`,
    refreshKey: {
      every: `1 hour`,
      incremental: true,
      updateWindow: `7 day`
    },
    partitionGranularity: `month`,
    buildRangeStart: {
      sql: `SELECT DATE_TRUNC('month', NOW() - INTERVAL '3 month')`
    },
    buildRangeEnd: {
      sql: `SELECT DATE_TRUNC('day', NOW())`
    }
  }
}
```

### Refresh Strategies

| Strategy | Use Case | Refresh Frequency |
|----------|----------|-------------------|
| `every: '1 hour'` | Near real-time | High update rate |
| `every: '6 hours'` | Daily reports | Moderate updates |
| `every: '1 day'` | Historical trends | Low update rate |
| `incremental: true` | Large datasets | Only new data |

### Partitioning

```javascript
partitionGranularity: `month`  // One partition per month
buildRangeStart: { sql: `...` } // Last 3 months
buildRangeEnd: { sql: `...` }   // Today
```

Benefits:
- **Faster queries** - only relevant partitions scanned
- **Efficient storage** - old partitions dropped automatically
- **Incremental builds** - only new partitions computed

### Pre-Aggregation Status

```bash
# Check pre-aggregation status via Cube.js API
curl http://localhost:4000/cubejs-api/v1/pre-aggregations \
  -H "Authorization: Bearer $CUBE_API_SECRET"
```

---

## Testing

### 1. Health Check

```bash
# Test Cube.js is running
curl http://localhost:4000/cubejs-api/v1/meta

# Expected: 200 OK with cubes list
```

### 2. Query Test

```bash
# Test basic query
curl -X POST http://localhost:4000/cubejs-api/v1/load \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUBE_API_SECRET" \
  -d '{
    "query": {
      "measures": ["Users.count"],
      "timeDimensions": []
    }
  }'

# Expected: 200 OK with data array
```

### 3. RLS Test

```bash
# Create test script
./scripts/cube/test-rls.sh

# Should verify:
# - Tenant A sees only their users
# - Tenant B sees only their users
# - Cross-tenant queries fail
```

### 4. Cache Test

```bash
# First query (MISS)
time curl -X POST http://localhost:4000/cubejs-api/v1/load ...
# Response time: ~500ms

# Second query (HIT)
time curl -X POST http://localhost:4000/cubejs-api/v1/load ...
# Response time: ~50ms (10x faster)
```

---

## Monitoring

### Metrics Endpoint

```bash
# Prometheus metrics
curl http://localhost:4000/metrics
```

Key metrics:
- `cubejs_cache_hit_rate` - Cache effectiveness
- `cubejs_query_duration_seconds` - Query latency (p50/p95/p99)
- `cubejs_pre_aggregations_total` - Pre-agg usage
- `cubejs_queue_size` - Query queue depth

### Grafana Dashboard

Dashboard available at: `docker/grafana/provisioning/dashboards/reporting-bff.json`

Panels:
- **Query Latency** (p50/p95/p99)
- **Cache Hit Rate** (target >80%)
- **Queries per Minute**
- **Error Rate** (5xx responses)
- **Pre-Aggregation Status**

### Logs

```bash
# View Cube.js logs
docker compose logs -f cube

# Filter errors
docker compose logs cube | grep ERROR
```

---

## Troubleshooting

### Issue: `ECONNREFUSED` to PostgreSQL

**Symptom:**
```
Error: connect ECONNREFUSED 127.0.0.1:5432
```

**Solution:**
```bash
# Check database is running
docker compose ps db

# Check database health
docker compose exec db pg_isready -U core -d core

# Restart Cube.js
docker compose restart cube
```

---

### Issue: Pre-Aggregation Not Building

**Symptom:**
```
Pre-aggregation dailyStatusCounts is not being used
```

**Solution:**
```bash
# Check pre-aggregation build logs
docker compose logs cube | grep "Pre-aggregation"

# Force rebuild
curl -X POST http://localhost:4000/cubejs-api/v1/pre-aggregations/jobs \
  -H "Authorization: Bearer $CUBE_API_SECRET" \
  -d '{"action":"invalidate","selector":{"cubes":["Users"]}}'
```

---

### Issue: Cache Not Working

**Symptom:**
All queries show `X-Cache: MISS`

**Solution:**
```bash
# Check Redis connection
docker compose exec cube sh -c 'redis-cli -h redis ping'
# Expected: PONG

# Check cache config
docker compose exec cube env | grep REDIS

# Clear cache
docker compose exec redis redis-cli FLUSHALL
```

---

### Issue: RLS Filter Not Applied

**Symptom:**
Query returns data from all tenants

**Solution:**
```javascript
// Verify SECURITY_CONTEXT is set in query
{
  "query": {...},
  "securityContext": {
    "tenantId": "uuid-here"
  }
}

// Check cube schema has filter:
WHERE tenant_id = ${SECURITY_CONTEXT.tenantId.filter('tenant_id')}
```

---

## Security Checklist

Before production deployment:

- [ ] `CUBE_API_SECRET` rotated (32+ chars)
- [ ] `CUBE_DEV_MODE=false` in production
- [ ] All cubes enforce `tenant_id` RLS filter
- [ ] Redis password configured (`CUBEJS_REDIS_PASSWORD`)
- [ ] Network policy isolates Cube.js (only BFF access)
- [ ] Pre-aggregations partitioned by tenant
- [ ] Logs redact sensitive fields
- [ ] Rate limiting configured in BFF
- [ ] Circuit breaker enabled for Cube.js calls
- [ ] Penetration test completed

---

## References

- [Cube.js Documentation](https://cube.dev/docs/)
- [Multi-Tenancy Guide](https://cube.dev/docs/security/context)
- [Pre-Aggregations](https://cube.dev/docs/caching/pre-aggregations/getting-started)
- [Core Platform Reporting README](../REPORTING_README.md)

---

**End of CUBE_SETUP.md**
