# Reporting Module - Operations Runbook

**Version**: 1.0.0  
**Last Updated**: 2025-01-10  
**Audience**: DevOps, SRE, On-Call Engineers

---

## 📋 Table of Contents

1. [Service Overview](#service-overview)
2. [Deployment](#deployment)
3. [Monitoring & Alerts](#monitoring--alerts)
4. [Incident Response](#incident-response)
5. [Common Issues](#common-issues)
6. [Backup & Recovery](#backup--recovery)
7. [Performance Tuning](#performance-tuning)
8. [Security Operations](#security-operations)

---

## Service Overview

### Components
- **Backend API**: Spring Boot (port 8080)
- **Cube.js**: Analytics engine (port 4000)
- **Frontend**: React SPA served by Nginx (port 80/443)
- **PostgreSQL**: Database (port 5432)
- **Redis**: Cache & rate limiting (port 6379)

### Dependencies
```
Frontend → Nginx → Backend API → Cube.js → PostgreSQL
                            ↓
                         Redis (rate limits)
```

### Health Checks
- **Backend**: `GET /actuator/health`
- **Cube.js**: `GET /cubejs-api/v1/health`
- **PostgreSQL**: `pg_isready`
- **Redis**: `PING` command

---

## Deployment

### Prerequisites
- Docker 24+
- Docker Compose 2.20+
- JDK 21 (for backend build)
- Node.js 20+ (for frontend build)

### Production Deployment Steps

#### 1. Pre-Deployment Checklist
- [ ] Review CHANGELOG.md for breaking changes
- [ ] Run security scans: `./scripts/run-security-scans.sh`
- [ ] Verify all tests pass: `./scripts/run-all-tests.sh`
- [ ] Check database migrations: `./backend/src/main/resources/db/migration`
- [ ] Notify team in #deployments channel
- [ ] Schedule deployment window (low-traffic period)

#### 2. Database Migration
```bash
# Backup database first (see Backup section)
cd backend
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://prod-db:5432/core_platform
```

#### 3. Backend Deployment
```bash
# Build JAR
cd backend
./mvnw clean package -DskipTests

# Stop old container
docker compose -f docker/docker-compose.prod.yml stop backend

# Deploy new version
docker compose -f docker/docker-compose.prod.yml up -d backend

# Verify health
curl -f http://localhost:8080/actuator/health || exit 1
```

#### 4. Frontend Deployment
```bash
# Build static files
cd frontend
npm run build

# Deploy to Nginx
docker compose -f docker/docker-compose.prod.yml up -d nginx

# Verify
curl -f http://localhost:80/ || exit 1
```

#### 5. Cube.js Deployment
```bash
# Update schemas
docker cp docker/cube/schema cube-container:/cube/conf/schema

# Restart Cube.js
docker compose -f docker/docker-compose.prod.yml restart cube

# Verify
curl -f http://localhost:4000/cubejs-api/v1/health || exit 1
```

#### 6. Post-Deployment Validation
- [ ] Run smoke tests: `./scripts/smoke-test.sh`
- [ ] Check Grafana dashboard for errors
- [ ] Verify Circuit Breaker state (should be CLOSED)
- [ ] Monitor logs for 15 minutes
- [ ] Update deployment tracking: `#deployments` channel

### Rollback Procedure
```bash
# 1. Revert database migration (if needed)
cd backend
./mvnw flyway:undo

# 2. Rollback to previous Docker image
docker compose -f docker/docker-compose.prod.yml down
docker tag core-platform-backend:previous core-platform-backend:latest
docker compose -f docker/docker-compose.prod.yml up -d

# 3. Verify health
curl -f http://localhost:8080/actuator/health
```

---

## Monitoring & Alerts

### Grafana Dashboard
**URL**: http://grafana.muriel.cz/d/reporting-module  
**Panels**:
- Query Response Time (p50, p95, p99)
- Request Rate (req/min)
- Circuit Breaker State
- Error Rate (4xx, 5xx)
- Rate Limit Hits (429)

### Critical Alerts

#### 1. Circuit Breaker OPEN
**Severity**: P1 (Critical)  
**Trigger**: `reporting_circuit_breaker_state == 1`  
**Response**:
1. Check Cube.js health: `docker logs cube`
2. Verify PostgreSQL connection: `psql -h postgres -U core_user -c "SELECT 1"`
3. Review error logs: `docker logs backend | grep ERROR`
4. If Cube.js unhealthy: `docker restart cube`
5. Manual CB reset: `curl -X POST http://localhost:8080/actuator/circuitbreakers/cubeQueryCircuitBreaker/reset`

#### 2. High Error Rate (>5%)
**Severity**: P2 (High)  
**Trigger**: `rate(http_server_requests_total{status=~"5.."}[5m]) > 0.05`  
**Response**:
1. Check backend logs: `docker logs backend --tail=100`
2. Identify failing endpoint: Grafana "Top Errors" panel
3. Review recent deployments (possible regression)
4. If widespread: Rollback to previous version

#### 3. High Latency (p99 > 5s)
**Severity**: P2 (High)  
**Trigger**: `histogram_quantile(0.99, http_server_requests_seconds_bucket) > 5`  
**Response**:
1. Check "Top Slowest Queries" in Grafana
2. Review Cube.js pre-aggregations: `docker exec cube cat /cube/conf/schema/*.js`
3. Check database slow queries: `SELECT * FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10`
4. Consider adding index or pre-aggregation

#### 4. Rate Limit Saturation (>100 429/min)
**Severity**: P3 (Medium)  
**Trigger**: `rate(http_server_requests_total{status="429"}[1m]) * 60 > 100`  
**Response**:
1. Identify tenant hitting limit: Check logs for `tenant_id`
2. Contact tenant to reduce request rate
3. If legitimate traffic spike: Temporarily increase limit in `RateLimitFilter`

### Log Aggregation
**Stack**: Promtail → Loki → Grafana  
**Query Examples**:
```logql
# All backend errors
{job="backend"} |= "ERROR"

# Circuit Breaker events
{job="backend"} |= "Circuit Breaker"

# Rate limit hits
{job="backend"} |= "Rate limit exceeded"

# Slow queries (>2s)
{job="cube"} |= "query took" |~ "([2-9]|[1-9][0-9]+)s"
```

---

## Incident Response

### Severity Levels

| Level | Response Time | Example |
|-------|---------------|---------|
| **P1** | 15 min | Service down, data loss |
| **P2** | 1 hour | High error rate, Circuit Breaker open |
| **P3** | 4 hours | Degraded performance |
| **P4** | 1 business day | Minor UI issue |

### Incident Workflow

#### 1. Detection
- Alert triggers (Grafana, PagerDuty)
- User report (#support channel)
- Monitoring dashboard anomaly

#### 2. Triage
- Assign incident commander
- Create incident channel: `#incident-YYYY-MM-DD-HH-MM`
- Update status page: https://status.muriel.cz
- Gather initial context (logs, metrics, user reports)

#### 3. Investigation
```bash
# Check service health
docker compose ps

# Recent logs (last 100 lines)
docker logs backend --tail=100
docker logs cube --tail=100

# Database connectivity
psql -h postgres -U core_user -d core_platform -c "SELECT 1"

# Redis connectivity
redis-cli -h redis PING

# Circuit Breaker state
curl http://localhost:8080/actuator/circuitbreakers | jq
```

#### 4. Mitigation
- If backend issue: Restart service (`docker restart backend`)
- If Cube.js issue: Restart (`docker restart cube`)
- If database issue: Check connections (`SELECT * FROM pg_stat_activity`)
- If Circuit Breaker stuck: Manual reset (see Alerts section)

#### 5. Resolution
- Verify fix with smoke tests
- Monitor for 30 minutes
- Update incident channel with resolution
- Close incident ticket

#### 6. Post-Mortem
- Schedule blameless post-mortem (within 48h)
- Document in `docs/incidents/YYYY-MM-DD-<title>.md`
- Identify action items (Jira tickets)
- Update runbook if needed

---

## Common Issues

### Issue: "JWT token invalid"
**Symptoms**: 401 Unauthorized, "Invalid JWT signature"  
**Cause**: Token expired or wrong secret  
**Fix**:
```bash
# Verify JWT secret in environment
docker exec backend env | grep JWT_SECRET

# Regenerate token
curl -X POST http://keycloak/auth/realms/core-platform/protocol/openid-connect/token \
  -d "client_id=core-platform" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin"
```

### Issue: "Tenant not found in security context"
**Symptoms**: 403 Forbidden, "Missing tenant_id"  
**Cause**: JWT missing `tenant_id` claim  
**Fix**:
```bash
# Decode JWT to verify claims
echo "<JWT>" | cut -d. -f2 | base64 -d | jq

# Ensure Keycloak mapper includes tenant_id
# Admin Console → Clients → core-platform → Mappers → tenant_id
```

### Issue: Cube.js queries timing out
**Symptoms**: 503 Service Unavailable, "Slow call threshold exceeded"  
**Cause**: Missing database index or pre-aggregation  
**Fix**:
```sql
-- Check slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Add index
CREATE INDEX idx_users_tenant_created ON users(tenant_id, created_at);
```

### Issue: Pre-aggregations not refreshing
**Symptoms**: Stale data in charts  
**Cause**: Pre-aggregation build error or schedule issue  
**Fix**:
```bash
# Check Cube.js logs
docker logs cube | grep "pre-aggregation"

# Force refresh
curl -X POST http://localhost:4000/cubejs-api/v1/pre-aggregations/jobs \
  -H "Authorization: Bearer <JWT>" \
  -d '{"action": "refresh", "selector": {"cubes": ["User"]}}'
```

---

## Backup & Recovery

### Database Backup

#### Daily Automated Backup
```bash
# Cron job (2 AM daily)
0 2 * * * /usr/local/bin/backup-postgres.sh

# Script: /usr/local/bin/backup-postgres.sh
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/postgres"
pg_dump -h postgres -U core_user -d core_platform | gzip > $BACKUP_DIR/core_platform_$DATE.sql.gz
# Retention: 30 days
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete
```

#### Manual Backup
```bash
pg_dump -h postgres -U core_user -d core_platform > backup.sql
```

### Database Restore
```bash
# Stop backend to prevent writes
docker compose stop backend

# Restore from backup
psql -h postgres -U core_user -d core_platform < backup.sql

# Verify data
psql -h postgres -U core_user -d core_platform -c "SELECT COUNT(*) FROM users"

# Start backend
docker compose start backend
```

### Redis Backup
```bash
# Trigger RDB snapshot
redis-cli BGSAVE

# Copy dump.rdb
docker cp redis:/data/dump.rdb ./redis_backup_$(date +%Y%m%d).rdb
```

### Configuration Backup
```bash
# Backup all configs
tar -czf config_backup_$(date +%Y%m%d).tar.gz \
  backend/src/main/resources/application.yml \
  docker/cube/cube.js \
  docker/cube/schema/*.js \
  .env
```

---

## Performance Tuning

### Database Optimization

#### Analyze Query Plans
```sql
EXPLAIN ANALYZE SELECT * FROM users WHERE tenant_id = 'tenant-1' AND created_at > '2024-01-01';
```

#### Add Indexes
```sql
-- Tenant isolation
CREATE INDEX idx_users_tenant ON users(tenant_id);

-- Time-based queries
CREATE INDEX idx_users_created ON users(created_at);

-- Composite index
CREATE INDEX idx_users_tenant_created ON users(tenant_id, created_at);
```

#### Vacuum & Analyze
```bash
# Cron job (weekly)
0 3 * * 0 psql -h postgres -U core_user -d core_platform -c "VACUUM ANALYZE"
```

### Cube.js Pre-Aggregations

#### Add Rollup
```javascript
// docker/cube/schema/User.js
preAggregations: {
  usersByDay: {
    type: 'rollup',
    dimensions: [User.tenantId],
    timeDimension: User.createdAt,
    granularity: 'day',
    measures: [User.count],
    refreshKey: {
      every: '1 hour'
    }
  }
}
```

#### Monitor Pre-Agg Build
```bash
# Check Cube.js logs
docker logs cube | grep "Building pre-aggregation"

# View pre-agg storage
docker exec cube ls -lh /cube/conf/.cubestore
```

### Rate Limit Tuning
```java
// Increase limit to 240 req/min
Bandwidth limit = Bandwidth.simple(240, Duration.ofMinutes(1));
```

### Circuit Breaker Tuning
```yaml
# More lenient threshold (70%)
resilience4j:
  circuitbreaker:
    instances:
      cubeQueryCircuitBreaker:
        failure-rate-threshold: 70
```

---

## Security Operations

### Rotate JWT Secret
```bash
# 1. Generate new secret
NEW_SECRET=$(openssl rand -base64 32)

# 2. Update environment variable
echo "JWT_SECRET=$NEW_SECRET" >> .env

# 3. Restart backend
docker compose restart backend

# 4. Update Keycloak client secret
# Keycloak Admin → Clients → core-platform → Credentials
```

### Review Audit Logs
```sql
-- All entity updates in last 24h
SELECT entity_type, entity_id, updated_by, updated_at, changes
FROM audit_log
WHERE updated_at > NOW() - INTERVAL '24 hours'
ORDER BY updated_at DESC;

-- Suspicious activity (>100 updates/hour by one user)
SELECT updated_by, COUNT(*) as update_count
FROM audit_log
WHERE updated_at > NOW() - INTERVAL '1 hour'
GROUP BY updated_by
HAVING COUNT(*) > 100;
```

### Update Dependencies
```bash
# Backend
cd backend && ./mvnw versions:display-dependency-updates

# Frontend
cd frontend && npm outdated

# Run security scans after update
./.github/workflows/security-scans.yml
```

### Certificate Renewal
```bash
# Check expiry
openssl x509 -in ssl/muriel.cz.crt -noout -dates

# Renew with Let's Encrypt
certbot renew --nginx
```

---

## Emergency Contacts

| Role | Name | Phone | Email |
|------|------|-------|-------|
| **On-Call Engineer** | Rotation | +420 XXX XXX XXX | oncall@muriel.cz |
| **DevOps Lead** | [Name] | +420 XXX XXX XXX | devops@muriel.cz |
| **Security Lead** | [Name] | +420 XXX XXX XXX | security@muriel.cz |
| **CTO** | [Name] | +420 XXX XXX XXX | cto@muriel.cz |

---

## Appendix

### Useful Commands
```bash
# Container logs (live)
docker compose logs -f backend

# Database connection count
psql -h postgres -U core_user -c "SELECT COUNT(*) FROM pg_stat_activity"

# Redis info
redis-cli INFO | grep connected_clients

# Backend thread dump
docker exec backend jstack 1 > threaddump.txt

# Cube.js memory usage
docker stats cube --no-stream
```

### Configuration Files
- Backend: `backend/src/main/resources/application.yml`
- Cube.js: `docker/cube/cube.js`
- Nginx: `docker/nginx/nginx.conf`
- Docker Compose: `docker/docker-compose.prod.yml`

---

**Last Updated**: 2025-01-10  
**Next Review**: 2025-04-10
