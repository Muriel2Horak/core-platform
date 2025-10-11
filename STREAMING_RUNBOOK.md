# Streaming Operations Runbook

Operační příručka pro správu streamingové infrastruktury.

## 🚨 Incident Response

### 1. Queue Backlog Alert (Queue Depth > 1000)

**Symptomy:**
- Alert: `StreamingQueueDepthHigh` nebo `StreamingQueueDepthCritical`
- Dashboard ukazuje rostoucí queue depth
- Uživatelé hlásí pomalé odpovědi

**Diagnosis:**
```sql
-- Zkontrolovat hloubku po prioritách
SELECT priority, COUNT(*), 
       MIN(created_at) as oldest,
       MAX(created_at) as newest
FROM command_queue
WHERE status = 'pending'
GROUP BY priority;

-- Najít nejstarší stuck příkazy
SELECT * FROM command_queue
WHERE status = 'pending'
  AND created_at < NOW() - INTERVAL '1 hour'
ORDER BY created_at
LIMIT 20;
```

**Resolution:**
1. **Škálovat workery**: Zvýšit `workerConcurrency` v metamodelu
2. **Přidat backend instance**: `docker compose up -d --scale backend=3`
3. **Bulk retry**: Pokud jsou příkazy stuck, retry je ručně:
```bash
curl -X POST http://localhost/api/admin/streaming/dlq/retry-all
```

### 2. Unsent Outbox Growing (> 100 messages)

**Symptomy:**
- Alert: `StreamingOutboxUnsentGrowing`
- Dispatcher se nedokáže připojit k Kafce
- Nebo je Kafka přetížená

**Diagnosis:**
```bash
# Check Kafka broker status
docker compose ps kafka
docker compose logs kafka | tail -50

# Check dispatcher logs
docker compose logs backend | grep DispatcherService

# Check Kafka topics
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

**Resolution:**
1. **Kafka down**: Restartovat Kafka
```bash
docker compose restart kafka
```

2. **Network issues**: Check connectivity
```bash
docker compose exec backend ping kafka
docker compose exec backend nc -zv kafka 9092
```

3. **Partition leader election**: Počkat 30s na auto-recovery

### 3. High DLQ Rate

**Symptomy:**
- Alert: `StreamingDLQMessagesDetected`
- Dashboard ukazuje rostoucí DLQ count
- Specific error types dominují

**Diagnosis:**
```sql
-- Top error types
SELECT error_message, COUNT(*) as count
FROM command_queue
WHERE status = 'dlq'
GROUP BY error_message
ORDER BY count DESC
LIMIT 10;

-- DLQ by entity
SELECT entity, COUNT(*) as count
FROM command_queue
WHERE status = 'dlq'
GROUP BY entity
ORDER BY count DESC;
```

**Resolution:**

**ValidationException:**
```bash
# Inspect first few DLQ messages
curl http://localhost/api/admin/streaming/dlq?entity=user&errorType=ValidationException

# Fix validation logic in code
# Deploy fix
# Retry DLQ
curl -X POST http://localhost/api/admin/streaming/dlq/retry-all?errorType=ValidationException
```

**Timeout/NetworkException:**
```bash
# Increase retry backoff
# Update metamodel: maxRetries: 5, maxBackoffMs: 60000
# Restart backend
# Retry DLQ
```

### 4. High P95 Latency (> 30s)

**Symptomy:**
- Alert: `StreamingLatencyP95High` nebo `Critical`
- Slow command processing
- Database contention

**Diagnosis:**
```sql
-- Check database locks
SELECT * FROM pg_stat_activity
WHERE state = 'active'
  AND query LIKE '%command_queue%'
  AND query_start < NOW() - INTERVAL '30 seconds';

-- Check work_state locks
SELECT * FROM work_state
WHERE state = 'updating'
  AND locked_at < NOW() - INTERVAL '5 minutes';
```

**Resolution:**
1. **Database tuning**: Optimize indexes
```sql
ANALYZE command_queue;
REINDEX TABLE command_queue;
```

2. **Stuck locks**: Force unlock
```sql
DELETE FROM work_state
WHERE state = 'updating'
  AND locked_at < NOW() - INTERVAL '10 minutes';
```

3. **Reduce batch size**: Lower `workerBatchSize` in metamodel

### 5. Lock Expiry Rate High (> 10/5min)

**Symptomy:**
- Alert: `StreamingLocksExpiring`
- Workers timeout frequently
- Work state thrashing

**Diagnosis:**
```sql
-- Find slow operations
SELECT entity, entity_id, 
       locked_at,
       NOW() - locked_at as lock_duration
FROM work_state
WHERE state = 'updating'
ORDER BY locked_at;
```

**Resolution:**
1. **Increase lock TTL**: Upravit `WorkStateService.LOCK_TTL_MINUTES` z 5 na 10
2. **Optimize worker logic**: Profilovat slow entity updates
3. **Split large batches**: Použít `BULK_UPDATE` s menšími chunky

## 🔧 Maintenance Tasks

### Daily Checks

```bash
# 1. Queue health
curl http://localhost:9090/api/v1/query?query=core_stream_cmd_queue_depth

# 2. DLQ count
curl http://localhost/api/admin/streaming/dlq | jq '.totalElements'

# 3. Kafka lag
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group core-platform-events

# 4. Error rate
curl http://localhost:9090/api/v1/query?query=rate(core_stream_worker_error_total[5m])
```

### Weekly Tasks

```bash
# 1. Vacuum old data
docker compose exec db psql -U postgres -d core_platform -c "
  DELETE FROM command_queue 
  WHERE status = 'completed' 
    AND updated_at < NOW() - INTERVAL '7 days';
"

# 2. Clean DLQ after resolution
curl -X DELETE http://localhost/api/admin/streaming/dlq/cleanup?olderThan=7d

# 3. Review Grafana alerts
open http://localhost:3001/alerting/list

# 4. Check Kafka disk usage
docker exec kafka df -h /var/lib/kafka/data
```

### Monthly Tasks

```bash
# 1. Review retention policies
# Check topic configs
docker exec kafka kafka-configs.sh \
  --bootstrap-server localhost:9092 \
  --describe --all --entity-type topics

# 2. Optimize Kafka segments
docker exec kafka kafka-log-dirs.sh \
  --bootstrap-server localhost:9092 \
  --describe

# 3. Database vacuum full (low traffic window)
docker compose exec db psql -U postgres -d core_platform -c "
  VACUUM FULL ANALYZE command_queue;
  VACUUM FULL ANALYZE outbox_final;
  VACUUM FULL ANALYZE work_state;
"
```

## 🔍 Debugging Commands

### Check Worker Status

```bash
# Worker throughput
curl http://localhost:9090/api/v1/query?query=rate(core_stream_worker_success_total[1m])

# Worker errors by entity
curl http://localhost:9090/api/v1/query?query=core_stream_worker_error_total

# Worker active threads
docker compose exec backend jps -lm
docker compose exec backend jstack <PID> | grep WorkerService
```

### Check Kafka Status

```bash
# Topics list
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# Topic details
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 \
  --describe --topic user-events

# Consumer lag
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group core-platform-events

# Consume recent messages
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic user-events \
  --from-beginning \
  --max-messages 10
```

### Check Database Performance

```sql
-- Slow queries
SELECT query, calls, mean_exec_time, max_exec_time
FROM pg_stat_statements
WHERE query LIKE '%command_queue%'
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename IN ('command_queue', 'outbox_final', 'work_state')
ORDER BY idx_scan DESC;

-- Table bloat
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE tablename IN ('command_queue', 'outbox_final', 'work_state');
```

## 📊 Performance Tuning

### Worker Pool Sizing

**Formula**: `workers = (CPU cores × target CPU utilization) / (1 - blocking coefficient)`

```yaml
# Low blocking (CPU-bound): workers = cores × 1.5
streaming:
  workerConcurrency: 3  # for 2-core machine

# High blocking (I/O-bound): workers = cores × 3
streaming:
  workerConcurrency: 6  # for 2-core machine
```

### Batch Size Optimization

**Small batches** (< 50): Lower latency, higher overhead
**Large batches** (> 500): Higher latency, better throughput

```yaml
streaming:
  workerBatchSize: 100  # Balanced default
  # For bulk imports: 500
  # For real-time: 20
```

### Kafka Partitions

**Formula**: `partitions ≥ max(target throughput / partition throughput, num workers)`

```bash
# Measure partition throughput
docker exec kafka kafka-run-class.sh kafka.tools.ProducerPerformance \
  --topic user-events \
  --num-records 10000 \
  --record-size 1024 \
  --throughput -1 \
  --producer-props bootstrap.servers=localhost:9092

# Increase partitions if needed
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 \
  --alter --topic user-events --partitions 6
```

## 🧪 Testing & Troubleshooting

### Reading Test Reports

**Playwright E2E Report** (po CI běhu nebo lokálním `npm run test:e2e`)
```bash
cd frontend/playwright-report
npx playwright show-report

# Sections:
# - Test results: PASSED/FAILED/SKIPPED
# - Traces: Network requests, screenshots, console logs
# - Videos: Test playback (if configured)
```

**Key Playwright Troubleshooting:**
1. **Test timeout**: Zvýšit `timeout: 60000` v test suite
2. **Iframe not loading**: Check Grafana CORS headers, network tab
3. **Metrics counter = 0**: Run mini flow first, wait 5s for Kafka propagation
4. **DLQ replay 404**: Verify backend admin API authentication

**JaCoCo Coverage Report**
```bash
open backend/target/site/jacoco/index.html

# Red lines: Not covered (need unit/IT tests)
# Yellow lines: Partially covered (branches)
# Green lines: Fully covered

# Coverage breakdown:
# - Instructions: Bytecode coverage
# - Branches: if/else, switch
# - Lines: Source line coverage (70% minimum)
```

**OWASP Dependency-Check Report**
```bash
open backend/target/dependency-check-report.html

# Sections:
# - Summary: Total dependencies, vulnerabilities by severity
# - Dependency list: CVE-xxxx links to NVD
# - Suppressed: owasp-suppressions.xml entries

# Resolve HIGH/CRITICAL:
# 1. Upgrade dependency to patched version
# 2. If no fix: Add suppression with justification
# 3. Re-run: ./mvnw verify -P security
```

### Smoke Test Failures

**infra-smoke-test.sh** error patterns:

```bash
# Error: "Backend health endpoint not UP"
# Fix: Check backend logs for startup errors
docker compose --profile streaming logs backend | tail -100

# Error: "Kafka topic 'streaming.entity.events' not found"
# Fix: Restart Kafka, topics auto-created on first produce
docker compose restart kafka
sleep 30 && ./scripts/infra-smoke-test.sh

# Error: "Grafana dashboards not provisioned (expected 3)"
# Fix: Check Grafana provisioning logs
docker compose logs grafana | grep -i provision

# Error: "Mini flow: Command still PENDING"
# Fix: Check worker logs, Kafka consumer lag
docker compose logs backend | grep WorkerService
```

### Alert Validation Failures

**validate-alerts.sh** error patterns:

```bash
# Error: "YAML syntax errors"
# Fix: Run yamllint manually to see line numbers
yamllint docker/prometheus/alerts.yml

# Error: "PromQL syntax errors detected"
# Fix: Check Prometheus UI -> Status -> Rules
# Copy failing expression to Prometheus query page for testing

# Error: "Missing alert: StreamingXYZ"
# Fix: Ensure alert name matches exactly (case-sensitive)
grep "alert:" docker/prometheus/alerts.yml

# Error: "Alert XYZ missing 'severity' label"
# Fix: Add under labels: section
#   labels:
#     severity: warning  # or critical
#     component: streaming
```

### Consumer Lag Troubleshooting

```bash
# Check consumer lag
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group core-platform-events

# Output columns:
# - CURRENT-OFFSET: Last consumed offset
# - LOG-END-OFFSET: Latest message offset
# - LAG: Difference (should be < 100 under normal load)

# If LAG > 1000:
# 1. Scale backend instances: docker compose up -d --scale backend=3
# 2. Increase workerConcurrency in metamodel
# 3. Check for slow queries in WorkerService logs
```

## 🔐 Security Checklist

- [ ] Kafka SASL/SCRAM authentication enabled (production)
- [ ] Grafana OAuth2 with Keycloak configured
- [ ] Admin API requires PlatformAdmin role
- [ ] DLQ sensitive data masked in logs
- [ ] Audit log for DLQ retry/delete operations
- [ ] Network policies: backend ↔ kafka, frontend ↔ grafana

## 📞 Escalation

**Level 1 - Ops**: Self-service via Admin UI, runbook procedures
**Level 2 - Platform Team**: Complex incidents, schema changes
**Level 3 - Vendor Support**: Kafka broker failures, data corruption

**Contact:**
- Ops Slack: `#core-platform-ops`
- Platform Team: `#core-platform-dev`
- On-call: PagerDuty `streaming-infra`

## 📚 References

- [Streaming README](./STREAMING_README.md)
- [Grafana Dashboards](http://localhost:3001/dashboards)
- [Prometheus Alerts](http://localhost:9090/alerts)
- [Kafka UI](http://localhost:8090)
