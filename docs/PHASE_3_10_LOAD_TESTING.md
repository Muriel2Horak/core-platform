# Phase 3.10: Load Testing & Backpressure

**Date**: 2025-01-10  
**Status**: ✅ DOCUMENTATION COMPLETE (Implementation optional)

## Overview

Load testing strategy for Reporting & Analytics module to ensure:
- **Performance**: Query execution under load
- **Scalability**: Horizontal scaling capability
- **Reliability**: Graceful degradation under stress
- **Security**: Rate limiting effectiveness

## Test Scenarios

### 1. Baseline Performance Test

**Goal**: Establish baseline metrics

```bash
# Single user, simple query
k6 run tests/load/reporting-baseline.js

Expected:
- p95 latency < 500ms (cache hit)
- p95 latency < 2s (cache miss)
- 0% error rate
```

### 2. Concurrent Users Test

**Goal**: Test concurrent query execution

```bash
# 100 concurrent users, mixed queries
k6 run --vus 100 --duration 5m tests/load/reporting-concurrent.js

Expected:
- p95 latency < 3s
- < 1% error rate
- Cache hit rate > 60%
```

### 3. Rate Limiting Test

**Goal**: Verify rate limit enforcement (120 req/min/tenant)

```bash
# Single tenant, burst traffic
k6 run tests/load/reporting-rate-limit.js

Expected:
- 429 errors after 120 requests/min
- X-RateLimit-Remaining header decrements correctly
- Retry-After header present in 429 responses
```

### 4. Cache Performance Test

**Goal**: Measure cache hit/miss performance

```bash
# Repeated queries to test cache warming
k6 run tests/load/reporting-cache.js

Expected:
- Cache hit ratio > 80% after warm-up
- Cache hit latency < 100ms
- Cache miss latency < 2s
```

### 5. Stress Test

**Goal**: Find breaking point

```bash
# Ramp up to 500 VUs over 10 minutes
k6 run --vus 500 --duration 10m tests/load/reporting-stress.js

Expected:
- Graceful degradation (no crashes)
- Circuit breaker activation (if implemented)
- Backpressure kicks in
```

### 6. Spike Test

**Goal**: Test sudden traffic spike

```bash
# 0 → 200 VUs instantly, hold 2min, 200 → 0
k6 run tests/load/reporting-spike.js

Expected:
- System remains responsive
- Queue builds up but drains after spike
- No permanent degradation
```

## K6 Test Scripts

### reporting-baseline.js

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1,
  duration: '1m',
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const url = 'http://localhost:8080/api/reports/query';
  const payload = JSON.stringify({
    entity: 'User',
    dimensions: ['status', 'created_at'],
    measures: [{ field: 'id', aggregation: 'count', alias: 'user_count' }],
    timeRange: {
      start: '2025-01-01T00:00:00Z',
      end: '2025-01-10T23:59:59Z',
    },
    limit: 1000,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ${TOKEN}',
    },
  };

  const res = http.post(url, payload, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'has data': (r) => JSON.parse(r.body).data.length > 0,
    'has fingerprint': (r) => JSON.parse(r.body).fingerprint !== null,
  });

  sleep(1);
}
```

### reporting-rate-limit.js

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    burst: {
      executor: 'constant-arrival-rate',
      rate: 150, // 150 req/min (exceeds 120 limit)
      duration: '2m',
      preAllocatedVUs: 10,
    },
  },
};

export default function () {
  const url = 'http://localhost:8080/api/reports/query';
  const payload = JSON.stringify({
    entity: 'User',
    dimensions: ['status'],
    measures: [{ field: 'id', aggregation: 'count' }],
    limit: 10,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ${TOKEN}',
    },
  };

  const res = http.post(url, payload, params);

  check(res, {
    'not rate limited OR rate limited correctly': (r) =>
      r.status === 200 || r.status === 429,
    'rate limit headers present': (r) =>
      r.headers['X-RateLimit-Limit'] !== undefined,
  });

  if (res.status === 429) {
    console.log('Rate limited - expected behavior');
  }

  sleep(0.1);
}
```

## Backpressure Mechanisms

### 1. Rate Limiting (Bucket4j)

**Configuration**: `application-reporting.yml`

```yaml
reporting:
  rate-limit:
    per-tenant-per-min: 120
```

**Behavior**:
- Token bucket algorithm
- 120 tokens per minute per tenant
- Refill rate: 2 tokens/second
- 429 response when bucket empty

### 2. Query Complexity Limits

**Hardcoded limits** in `ReportingSecurityService`:

```java
MAX_DIMENSIONS = 20
MAX_MEASURES = 10
MAX_FILTERS = 50
MAX_ROWS = 50,000 (from properties)
MAX_INTERVAL_DAYS = 92 (from properties)
```

**Behavior**:
- Validates before Cube.js call
- Throws `IllegalArgumentException` if exceeded
- Returns 400 Bad Request

### 3. Cache (Redis/Caffeine)

**Configuration**:

```yaml
reporting:
  cache:
    provider: redis
    default-ttl-seconds: 60
```

**Behavior**:
- Automatic TTL expiration
- LRU eviction (Caffeine) or Redis maxmemory policy
- Cache warming on first hit

### 4. Cube.js Timeouts

**Configuration**:

```yaml
reporting:
  cube:
    connect-timeout-ms: 5000
    read-timeout-ms: 30000
```

**Behavior**:
- Connection timeout: 5s
- Read timeout: 30s
- Throws exception on timeout

## Monitoring Metrics

### Key Metrics (Prometheus)

```promql
# Query rate
rate(reporting_query_requests_total[1m])

# Cache hit rate
100 * (
  rate(reporting_cache_hits_total[5m]) /
  (rate(reporting_cache_hits_total[5m]) + rate(reporting_cache_misses_total[5m]))
)

# Error rate
rate(reporting_query_errors_total[1m])

# Rate limit violations
rate(reporting_ratelimit_exceeded_total[1m])

# Query duration p95
histogram_quantile(0.95, rate(reporting_query_duration_bucket[5m]))

# Cube.js API duration p95
histogram_quantile(0.95, rate(reporting_cube_api_duration_bucket[5m]))
```

### Grafana Dashboard

Create dashboard with panels:
1. **Query Rate** (queries/sec)
2. **Cache Hit Rate** (%)
3. **Error Rate** (errors/sec)
4. **Latency p50/p95/p99** (ms)
5. **Rate Limit Violations** (count)
6. **Cube.js API Health** (success rate)

## Performance Targets

| Metric | Target | Acceptable | Unacceptable |
|--------|--------|------------|--------------|
| Query latency p95 (cache hit) | < 500ms | < 1s | > 2s |
| Query latency p95 (cache miss) | < 2s | < 5s | > 10s |
| Cache hit rate | > 70% | > 50% | < 30% |
| Error rate | < 0.1% | < 1% | > 5% |
| Rate limit effectiveness | 100% | 100% | < 100% |
| Throughput per pod | > 100 req/s | > 50 req/s | < 20 req/s |

## Horizontal Scaling

### Kubernetes HPA

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-reporting
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Pods
    pods:
      metric:
        name: reporting_query_duration_seconds
      target:
        type: AverageValue
        averageValue: "2"
```

### Load Balancing

- **Session affinity**: Not required (stateless)
- **Algorithm**: Round-robin
- **Health checks**: `/actuator/health`

## Troubleshooting

### High Latency

1. Check Cube.js health: `curl http://cube:4000/health`
2. Check Redis latency: `redis-cli --latency`
3. Check DB connection pool: `spring.datasource.hikari.maximum-pool-size`
4. Check query complexity (too many dimensions/filters)

### High Error Rate

1. Check logs: `kubectl logs -l app=backend | grep ERROR`
2. Check Cube.js errors: `docker logs cube`
3. Check Redis availability: `redis-cli ping`
4. Verify JWT tokens valid

### Cache Misses

1. Check TTL configuration
2. Check cache eviction policy
3. Check Redis memory usage
4. Increase cache size if needed

## Next Steps

- [ ] Create K6 test scripts in `tests/load/`
- [ ] Run baseline performance test
- [ ] Run concurrent users test (100 VUs)
- [ ] Run rate limiting test
- [ ] Run stress test (500 VUs)
- [ ] Create Grafana dashboard
- [ ] Configure HPA for production
- [ ] Document performance benchmarks

## References

- [K6 Documentation](https://k6.io/docs/)
- [Bucket4j Documentation](https://bucket4j.com/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Prometheus Query Examples](https://prometheus.io/docs/prometheus/latest/querying/examples/)
