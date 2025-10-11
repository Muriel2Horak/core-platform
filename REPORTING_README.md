# Reporting Module - Complete Documentation

**Version**: 1.0.0  
**Last Updated**: 2025-01-10  
**Status**: Production-Ready ✅

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Features](#features)
4. [API Reference](#api-reference)
5. [Configuration](#configuration)
6. [Security](#security)
7. [Performance](#performance)
8. [Monitoring](#monitoring)
9. [Troubleshooting](#troubleshooting)
10. [Development](#development)

---

## Overview

The Reporting Module provides a **comprehensive, secure, and performant** data exploration and visualization platform for multi-tenant applications. Built on **Cube.js** for analytics, it enforces strict **Row-Level Security (RLS)**, **rate limiting**, and **OWASP compliance**.

### Key Capabilities
- 📊 **Interactive Data Explorer**: AG Grid-based table with inline editing, sorting, filtering
- 📈 **Chart Visualization**: ECharts integration (bar, line, pie charts)
- 🔒 **Multi-Tenant Security**: RLS enforced at Cube.js level (tenant_id isolation)
- ⚡ **High Performance**: Query deduplication, Circuit Breaker, pre-aggregations
- 🛡️ **OWASP Compliant**: ASVS 4.0 verified, SAST/DAST scanned, dependency-checked

### Technology Stack
- **Backend**: Spring Boot 3.x, jOOQ, PostgreSQL 16, Resilience4j
- **Analytics Engine**: Cube.js 0.35+ (Node.js)
- **Frontend**: React 18.2, AG Grid Community 31.3, ECharts 5.5, MUI 5.15
- **Monitoring**: Prometheus, Grafana, Loki
- **CI/CD**: GitHub Actions (tests, security scans)

---

## Architecture

```
┌─────────────┐
│   Browser   │
│  (React UI) │
└──────┬──────┘
       │ HTTPS
       ▼
┌─────────────────────────────────┐
│   Nginx (Reverse Proxy)         │
│  - HTTPS termination             │
│  - Static file serving           │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│   Spring Boot Backend           │
│  ┌────────────────────────────┐ │
│  │ ReportQueryController      │ │ ← POST /api/reports/query
│  │ EntityCrudController       │ │ ← PATCH /api/entities/{entity}/{id}
│  │ BulkUpdateController       │ │ ← POST /api/entities/{entity}/bulk-update
│  └────────┬───────────────────┘ │
│           │                      │
│  ┌────────▼───────────────────┐ │
│  │ CubeQueryService           │ │
│  │  - QueryDeduplicator       │ │ ← Single-Flight pattern
│  │  - Circuit Breaker         │ │ ← Resilience4j
│  │  - WebClient (Cube.js API) │ │
│  └────────┬───────────────────┘ │
│           │                      │
│  ┌────────▼───────────────────┐ │
│  │ Security Filters           │ │
│  │  - RateLimitFilter         │ │ ← 120 req/min per tenant
│  │  - SecurityHeadersFilter   │ │ ← CSP, HSTS, X-Frame-Options
│  │  - ContentTypeValidation   │ │ ← JSON-only enforcement
│  └────────────────────────────┘ │
└─────────┬───────────────────────┘
          │
          ▼
┌─────────────────────────────────┐
│   Cube.js Analytics Engine      │
│  ┌────────────────────────────┐ │
│  │ Cube Schemas (RLS)         │ │
│  │  - User.js (filter tenant) │ │
│  │  - Company.js              │ │
│  │  - UserDirectory.js        │ │
│  └────────┬───────────────────┘ │
│           │                      │
│  ┌────────▼───────────────────┐ │
│  │ Pre-Aggregations           │ │ ← Redis cache
│  │  - 1h refresh cycle        │ │
│  └────────────────────────────┘ │
└─────────┬───────────────────────┘
          │
          ▼
┌─────────────────────────────────┐
│   PostgreSQL 16 Database        │
│  - Row-Level Security (RLS)     │
│  - Partitioning (by tenant_id)  │
│  - Indexes on common queries    │
└─────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility |
|-----------|----------------|
| **Nginx** | HTTPS, static files, reverse proxy |
| **Spring Boot** | REST API, auth, business logic, rate limiting |
| **CubeQueryService** | Cube.js client, Circuit Breaker, deduplication |
| **QueryDeduplicator** | Prevent duplicate concurrent queries (Single-Flight) |
| **Security Filters** | Rate limit, security headers, Content-Type validation |
| **Cube.js** | Analytics queries, RLS enforcement, pre-aggregations |
| **PostgreSQL** | Data persistence, RLS, indexes |

---

## Features

### 1. Data Explorer (Frontend)
- **Grid**: AG Grid Community with 100k+ row support
- **Inline Editing**: PATCH updates with optimistic locking (If-Match)
- **Bulk Operations**: Async bulk updates (max 1000 rows, chunked)
- **Export**: CSV download
- **Sorting & Filtering**: Server-side (Cube.js)
- **Charts**: Bar, Line, Pie (ECharts 5.5)

### 2. Query Engine (Cube.js)
- **RLS**: `tenant_id` filter on all queries (JWT-based)
- **Pre-Aggregations**: 1h refresh cycle (Redis cache)
- **Measures**: count, sum, avg, min, max
- **Dimensions**: All entity fields
- **Time Dimensions**: created_at, updated_at (granularity: day/week/month)

### 3. API Endpoints

#### **POST /api/reports/query**
Execute Cube.js query.

**Request**:
```json
{
  "query": {
    "dimensions": ["User.name", "User.email"],
    "measures": ["User.count"],
    "filters": [
      {
        "member": "User.createdAt",
        "operator": "inDateRange",
        "values": ["2024-01-01", "2024-12-31"]
      }
    ],
    "limit": 100
  }
}
```

**Response**:
```json
{
  "data": [
    {"User.name": "John", "User.email": "john@example.com", "User.count": "1"}
  ],
  "metadata": {
    "executionTime": 123,
    "totalRows": 1
  }
}
```

**Headers**:
- `Authorization: Bearer <JWT>` (required)
- `Content-Type: application/json` (required)

**Status Codes**:
- `200`: Success
- `400`: Invalid query
- `401`: Unauthorized
- `415`: Wrong Content-Type
- `429`: Rate limit exceeded
- `503`: Circuit Breaker open

---

#### **GET /api/reports/metadata/{entity}/spec**
Get entity metadata for UI rendering.

**Response**:
```json
{
  "entityName": "User",
  "tableName": "users",
  "fields": [
    {"name": "id", "type": "uuid", "editable": false},
    {"name": "name", "type": "string", "editable": true},
    {"name": "email", "type": "string", "editable": true}
  ],
  "editableFields": ["name", "email"],
  "version": "1.0.0"
}
```

**Headers**:
- `X-Spec-Version: 1.0.0` (response)

---

#### **PATCH /api/entities/{entity}/{id}**
Update single entity (inline editing).

**Request**:
```json
{
  "name": "Updated Name",
  "email": "newemail@example.com"
}
```

**Headers**:
- `If-Match: "version-123"` (required for optimistic locking)

**Response**:
```json
{
  "id": "uuid",
  "name": "Updated Name",
  "email": "newemail@example.com",
  "version": "version-124"
}
```

**Status Codes**:
- `200`: Success
- `400`: Non-editable field
- `404`: Entity not found
- `409`: Conflict (stale version)
- `428`: Precondition Required (missing If-Match)

---

#### **POST /api/entities/{entity}/bulk-update**
Async bulk update (background job).

**Request**:
```json
{
  "ids": [1, 2, 3],
  "updates": {
    "status": "active"
  }
}
```

**Response**:
```json
{
  "jobId": "job-uuid-123",
  "status": "PENDING",
  "totalRows": 3,
  "createdAt": "2025-01-10T10:00:00Z"
}
```

---

#### **GET /api/bulk-jobs/{jobId}**
Get bulk job status.

**Response**:
```json
{
  "jobId": "job-uuid-123",
  "status": "RUNNING",
  "processedRows": 150,
  "totalRows": 300,
  "successCount": 145,
  "errorCount": 5,
  "errors": [
    {"rowId": 42, "error": "Version conflict"}
  ],
  "progress": 50.0
}
```

**Status Values**: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`

---

## Configuration

### Backend (application.yml)
```yaml
cube:
  api:
    url: http://cube:4000
    secret: ${CUBE_API_SECRET}

spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/core_platform
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  
  redis:
    host: redis
    port: 6379

resilience4j:
  circuitbreaker:
    instances:
      cubeQueryCircuitBreaker:
        sliding-window-size: 10
        failure-rate-threshold: 50
        slow-call-duration-threshold: 5s
        wait-duration-in-open-state: 30s

management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

### Frontend (.env)
```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_CUBE_API_URL=http://localhost:4000
VITE_ENABLE_DEBUG_MODE=false
```

### Cube.js (cube.js)
```javascript
module.exports = {
  contextToAppId: ({ securityContext }) => `CUBEJS_APP_${securityContext.tenant_id}`,
  scheduledRefreshContexts: () => [
    { securityContext: { tenant_id: 'tenant-1' } },
    { securityContext: { tenant_id: 'tenant-2' } }
  ],
  preAggregationsSchema: 'pre_aggregations',
  queryRewrite: (query, { securityContext }) => {
    if (!securityContext.tenant_id) {
      throw new Error('Tenant ID required');
    }
    return query;
  }
};
```

---

## Security

### Authentication
- **JWT tokens** extracted from `Authorization: Bearer <token>` header
- Token must contain `tenant_id` claim
- 401 Unauthorized if missing/invalid

### Authorization
- **Row-Level Security (RLS)** enforced in Cube.js schemas:
  ```javascript
  sql: `SELECT * FROM users WHERE tenant_id = ${SECURITY_CONTEXT.tenant_id.unsafeValue()}`
  ```
- No cross-tenant data leakage

### Rate Limiting
- **120 requests/minute** per tenant (Bucket4j + Redis)
- 429 Too Many Requests with `Retry-After` header
- Isolated buckets per tenant

### Security Headers
- `Content-Security-Policy: default-src 'self'; script-src 'self'; frame-ancestors 'none'`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security: max-age=31536000`
- `Referrer-Policy: strict-origin-when-cross-origin`

### Input Validation
- Content-Type must be `application/json` (415 if not)
- @Valid annotations on all DTOs
- Field-level validation (editable fields only)

### Logging
- **PII redaction** (LogRedactor):
  - Emails: `user***@domain.com`
  - Phones: `***PHONE***`
  - Credit cards: `****-****-****-****`
- Audit logs: `updated_by`, `updated_at` for all changes

---

## Performance

### Query Deduplication
- **Single-Flight pattern**: Identical concurrent queries deduplicated
- SHA-256 fingerprinting (query + tenant)
- ~80% reduction in duplicate Cube.js queries

### Circuit Breaker
- **Failure threshold**: 50% in sliding window of 10 calls
- **Wait duration**: 30 seconds in OPEN state
- **Half-open test**: 5 calls
- Prevents cascade failures to Cube.js

### Pre-Aggregations
- **Refresh cycle**: 1 hour
- **Cache**: Redis
- **Rollups**: count, sum, avg by day/week/month

### Metrics (Prometheus)
- `reporting.query.inflight`: In-flight deduplicated queries
- `reporting.circuit_breaker.state`: CB state (0=CLOSED, 1=OPEN)
- `reporting.circuit_breaker.failure_rate`: Failure %
- `http_server_requests_seconds`: Query latency (p50, p95, p99)

---

## Monitoring

### Grafana Dashboard
Import: `docker/grafana/dashboards/reporting-performance.json`

**Panels**:
1. Query Response Time (p50, p95, p99)
2. Request Rate (req/min, 2xx/4xx/5xx)
3. Circuit Breaker State (CLOSED/HALF_OPEN/OPEN)
4. Circuit Breaker Failure Rate (%)
5. In-Flight Queries (deduplication effectiveness)
6. Rate Limit Hit Rate (429/min)
7. Top 10 Slowest Queries

**Alerts** (recommended):
- Circuit Breaker OPEN → Slack notification
- Failure rate > 30% → PagerDuty
- p99 latency > 5s → Email

---

## Troubleshooting

### Issue: 503 Service Unavailable
**Cause**: Circuit Breaker OPEN (Cube.js failures)  
**Fix**:
1. Check Cube.js logs: `docker logs cube`
2. Verify PostgreSQL connection
3. Wait 30s for Circuit Breaker to auto-recover
4. Manual reset: `curl -X POST /actuator/circuitbreakers/cubeQueryCircuitBreaker/reset`

### Issue: 429 Too Many Requests
**Cause**: Rate limit exceeded (120 req/min)  
**Fix**:
1. Implement client-side throttling
2. Use query deduplication
3. Increase limit in `RateLimitFilter` if justified

### Issue: Slow Queries (p99 > 5s)
**Cause**: Missing pre-aggregations or indexes  
**Fix**:
1. Check Grafana "Top Slowest Queries" panel
2. Add pre-aggregation in Cube.js schema
3. Add database index on filter columns

### Issue: 409 Conflict (Concurrent Update)
**Cause**: Optimistic locking version mismatch  
**Fix**:
1. Frontend: Retry with latest version (GET → PATCH)
2. User notification: "Data changed, please refresh"

---

## Development

### Local Setup
```bash
# Start all services
docker compose -f docker/docker-compose.yml up -d

# Run backend
cd backend && ./mvnw spring-boot:run

# Run frontend
cd frontend && npm install && npm run dev

# Access
# - Frontend: http://localhost:3000
# - Backend API: http://localhost:8080
# - Cube.js: http://localhost:4000
# - Grafana: http://localhost:3001
```

### Running Tests
```bash
# Backend unit tests
cd backend && ./mvnw test

# Backend integration tests
cd backend && ./mvnw test -Dtest="**/*IT"

# Frontend E2E tests
cd frontend && npm run test:e2e
```

### CI/CD
- **Tests**: `.github/workflows/reporting-tests.yml` (on PR)
- **Security Scans**: `.github/workflows/security-scans.yml` (weekly)
- **Merge Gates**: All tests + scans must pass

---

## Additional Resources

- **Operations Runbook**: [REPORTING_OPERATIONS_RUNBOOK.md](./REPORTING_OPERATIONS_RUNBOOK.md)
- **Security Checklist**: [SECURITY_CHECKLIST.md](./SECURITY_CHECKLIST.md)
- **API Collection**: Postman collection in `docs/reporting-api.postman.json`
- **Cube.js Docs**: https://cube.dev/docs
- **OWASP ASVS**: https://owasp.org/www-project-application-security-verification-standard/

---

**Support**: reporting-support@muriel.cz  
**Security Issues**: security@muriel.cz  
**License**: Proprietary
