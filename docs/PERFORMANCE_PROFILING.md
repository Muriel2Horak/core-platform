# 📈 Performance Profiling Guide

**Status:** ✅ Active  
**Last Updated:** 2025-10-12 (S8)

---

## 🎯 Overview

This guide describes how to profile and monitor performance of the core-platform backend application using JVM metrics, Hibernate statistics, and Micrometer.

---

## 🔧 JVM Performance Metrics

### Enabled Metrics

The backend exports the following JVM metrics to Prometheus/Grafana:

#### Memory Metrics
- **Heap Memory:** `jvm.memory.used{area="heap"}`, `jvm.memory.max{area="heap"}`
- **Non-Heap Memory:** `jvm.memory.used{area="nonheap"}`
- **Memory Pools:** Eden, Survivor, Old Generation, Metaspace

#### Garbage Collection
- **GC Count:** `jvm.gc.pause_count{action="end of major GC"}`
- **GC Duration:** `jvm.gc.pause_seconds{action="end of minor GC"}`
- **GC Overhead:** Percentage of time spent in GC vs application code

#### Threads
- **Thread Count:** `jvm.threads.live`, `jvm.threads.peak`, `jvm.threads.daemon`
- **Thread States:** Runnable, Blocked, Waiting, Timed Waiting

#### Class Loading
- **Classes Loaded:** `jvm.classes.loaded`, `jvm.classes.unloaded`

### Configuration

All JVM metrics are enabled by default in `application.properties`:

```properties
# Micrometer JVM metrics
management.metrics.enable.jvm=true
management.metrics.enable.jvm.memory=true
management.metrics.enable.jvm.gc=true
management.metrics.enable.jvm.threads=true
management.metrics.enable.jvm.classes=true
```

---

## 🗄️ Hibernate Query Profiling

### Enabled Statistics

Hibernate statistics track database query performance:

- **Query Execution Time:** Time spent executing SQL queries
- **Query Count:** Number of queries executed per transaction
- **Cache Hit Ratio:** L2 cache effectiveness
- **N+1 Query Detection:** Identifies inefficient lazy loading

### Configuration

Enable Hibernate statistics in `application.properties`:

```properties
# Hibernate statistics (S8)
spring.jpa.properties.hibernate.generate_statistics=true
spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=100

# Show slow queries in logs
logging.level.org.hibernate.stat=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### Viewing Statistics

Hibernate statistics are exported as Micrometer metrics:

```bash
# View via Actuator endpoint
curl http://localhost:8080/actuator/metrics/hibernate.query.execution.max

# View in Grafana dashboard
# Dashboard: "Hibernate Performance" (S8)
```

---

## 📊 Custom Business Metrics

### API Endpoint Latency

All REST endpoints are automatically instrumented with `@Timed` annotation (via Spring Boot Actuator):

```java
@GetMapping("/api/users/{id}")
@Timed(value = "api.users.get", description = "Get user by ID", percentiles = {0.95, 0.99})
public ResponseEntity<User> getUser(@PathVariable Long id) {
    // ...
}
```

**Metrics Exported:**
- `http.server.requests` - Request count, duration, status codes
- `api.users.get_seconds{quantile="0.95"}` - 95th percentile latency
- `api.users.get_seconds{quantile="0.99"}` - 99th percentile latency

### Kafka Consumer Metrics

Kafka consumers export performance metrics:

```properties
# Kafka consumer lag
kafka.consumer.lag{group="core-platform.presence-mutating"}

# Message processing time
kafka.consumer.processing_time_seconds{topic="core.entities.lifecycle.mutating"}
```

### Pre-Aggregation Refresh Metrics

Cube.js pre-aggregation refresh tracked via `CubePreAggService`:

```properties
# Refresh requests
cube.preagg.refresh_requests_total{entity="User"}

# Refresh duration
cube.preagg.refresh_duration_seconds{entity="User"}

# Debounce cache size
cube.preagg.debounce_cache_size
```

---

## 🔍 Profiling Workflow

### 1. Local Profiling (Development)

#### Step 1: Start Application with Profiling Enabled

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xms512m -Xmx2g -XX:+UseG1GC -XX:+PrintGCDetails"
```

#### Step 2: Generate Load

```bash
# Option 1: Manual API calls
curl http://localhost:8080/api/users

# Option 2: Load testing with Apache Bench
ab -n 1000 -c 10 http://localhost:8080/api/users

# Option 3: K6 load test
k6 run tests/load/api-load-test.js
```

#### Step 3: Analyze Metrics

**Via Actuator:**
```bash
# JVM memory
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# GC pauses
curl http://localhost:8080/actuator/metrics/jvm.gc.pause

# HTTP requests
curl http://localhost:8080/actuator/metrics/http.server.requests
```

**Via Grafana:**
1. Open http://localhost:3001 (Grafana)
2. Navigate to "JVM Performance" dashboard
3. Select time range and service instance

### 2. Production Profiling

#### Continuous Monitoring

1. **Prometheus Scraping:** Metrics scraped every 15s
2. **Grafana Dashboards:** Real-time visualization
3. **Alerting:** PagerDuty/Slack alerts on threshold breaches

#### Baseline Metrics (S8)

| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| **Heap Usage** | <70% | >80% | >90% |
| **GC Pause** | <100ms (p99) | >200ms | >500ms |
| **API Latency (p95)** | <200ms | >500ms | >1000ms |
| **API Latency (p99)** | <500ms | >1000ms | >2000ms |
| **DB Query Time (avg)** | <50ms | >100ms | >200ms |
| **Kafka Consumer Lag** | <100 msgs | >1000 msgs | >10000 msgs |
| **Thread Count** | <100 | >200 | >300 |

---

## 🛠️ Troubleshooting Common Issues

### High GC Overhead (>10%)

**Symptoms:**
- `jvm.gc.pause_seconds` frequently >100ms
- CPU spikes during GC pauses

**Diagnosis:**
```bash
# Check GC logs
tail -f backend/logs/gc.log

# Analyze heap dump
jmap -dump:format=b,file=heap.bin <PID>
jhat heap.bin
```

**Solutions:**
1. Increase heap size: `-Xmx4g`
2. Tune GC algorithm: `-XX:+UseG1GC -XX:MaxGCPauseMillis=200`
3. Review object retention (memory leaks)

### Slow Database Queries

**Symptoms:**
- `hibernate.query.execution.max` >200ms
- High `hibernate.session.open_count`

**Diagnosis:**
```sql
-- PostgreSQL slow query log
SELECT * FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;
```

**Solutions:**
1. Add database indexes (check `EXPLAIN ANALYZE`)
2. Enable L2 cache for frequently accessed entities
3. Use batch fetching to avoid N+1 queries
4. Review and optimize JPA queries

### High API Latency

**Symptoms:**
- `http.server.requests_seconds{quantile="0.99"}` >1s
- Slow response times in Grafana

**Diagnosis:**
```bash
# Enable request logging
logging.level.org.springframework.web=TRACE

# Check thread pool saturation
curl http://localhost:8080/actuator/metrics/executor.pool.size
```

**Solutions:**
1. Increase thread pool size: `server.tomcat.threads.max=200`
2. Add caching for expensive operations
3. Optimize database queries (see above)
4. Review synchronous vs asynchronous processing

### Kafka Consumer Lag

**Symptoms:**
- `kafka.consumer.lag` >1000 messages
- Delayed event processing

**Diagnosis:**
```bash
# Check consumer group lag
kafka-consumer-groups --bootstrap-server localhost:9092 --group core-platform.presence-mutating --describe
```

**Solutions:**
1. Increase consumer concurrency: `spring.kafka.listener.concurrency=5`
2. Optimize message processing logic
3. Add more consumer instances (horizontal scaling)
4. Review retry policies (S7 `@CriticalRetry`, etc.)

---

## 📈 Performance Testing

### Load Testing Scenarios

#### Scenario 1: API Endpoint Stress Test
```bash
# 1000 requests, 50 concurrent users
ab -n 1000 -c 50 http://localhost:8080/api/users
```

#### Scenario 2: Database Query Performance
```bash
# Run query performance test suite
./mvnw test -Dtest=PerformanceTest
```

#### Scenario 3: Kafka Throughput Test
```bash
# Produce 10k messages, measure consumer lag
kafka-producer-perf-test \
  --topic core.entities.lifecycle.mutated \
  --num-records 10000 \
  --throughput 1000 \
  --producer-props bootstrap.servers=localhost:9092
```

---

## 🔗 Related Resources

- **Grafana Dashboards:** http://localhost:3001
- **Prometheus Metrics:** http://localhost:9090
- **Actuator Endpoints:** http://localhost:8080/actuator
- **JVM Tuning Guide:** https://docs.oracle.com/en/java/javase/17/gctuning/
- **Hibernate Performance:** https://docs.jboss.org/hibernate/orm/6.2/userguide/html_single/Hibernate_User_Guide.html#performance

---

**Maintained by:** Platform Team  
**Questions:** #platform-performance Slack channel
