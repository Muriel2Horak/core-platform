# Troubleshooting Guide

**Version:** 1.0  
**Last Updated:** 2025-10-12  
**Platform:** Core Platform (Docker Compose + Kubernetes)

---

## 🚨 Quick Diagnostics

### Health Check Script
```bash
#!/bin/bash
# Run from project root
echo "=== Core Platform Health Check ==="

# 1. Check Docker containers
echo -e "\n📦 Docker Containers:"
docker ps --filter "name=core-platform" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 2. Check backend health
echo -e "\n🔧 Backend Health:"
curl -s http://localhost:8080/actuator/health | jq

# 3. Check database connection
echo -e "\n🗄️ Database:"
docker exec core-platform-db-1 pg_isready -U core

# 4. Check Keycloak
echo -e "\n🔐 Keycloak:"
curl -s -o /dev/null -w "%{http_code}" https://admin.core-platform.local/health

# 5. Check Kafka
echo -e "\n📨 Kafka:"
docker exec core-platform-kafka-1 kafka-topics.sh --list --bootstrap-server localhost:9092 | head -5

# 6. Check frontend
echo -e "\n🌐 Frontend:"
curl -s -o /dev/null -w "%{http_code}" http://localhost:3001
```

---

## 🔧 Backend Issues

### Issue 1: Backend Won't Start

**Symptoms:**
- Container exits immediately
- `docker logs backend` shows errors
- HTTP 502 from Nginx

**Common Causes:**

**1. Keycloak Not Ready**
```log
ERROR [main] o.s.b.SpringApplication: Application run failed
...
Unable to connect to Keycloak: Connection refused
```

**Solution:**
```bash
# Wait for Keycloak to be healthy
docker-compose logs -f keycloak | grep "Keycloak.*started"

# Restart backend after Keycloak is ready
docker-compose restart backend
```

**2. Database Not Ready**
```log
ERROR [main] HikariPool: HikariPool-1 - Exception during pool initialization
...
Connection to db:5432 refused
```

**Solution:**
```bash
# Check database is running
docker exec core-platform-db-1 pg_isready -U core

# Check database logs
docker-compose logs db | tail -50

# Restart backend
docker-compose restart backend
```

**3. Flyway Migration Failed**
```log
ERROR [main] o.f.c.i.c.DbMigrate: Migration failed!
...
Checksum mismatch for migration V1__initial_schema.sql
```

**Solution:**
```bash
# Option 1: Repair Flyway metadata
docker exec -it core-platform-backend-1 bash
./mvnw flyway:repair

# Option 2: Reset database (⚠️ LOSES DATA)
docker-compose down -v
docker-compose up -d db
sleep 10
docker-compose up -d backend

# Option 3: Manually fix migration
docker exec -it core-platform-db-1 psql -U core -c "SELECT * FROM flyway_schema_history;"
# Delete corrupted migration record
docker exec -it core-platform-db-1 psql -U core -c "DELETE FROM flyway_schema_history WHERE version = '1';"
docker-compose restart backend
```

**4. Port Already in Use**
```log
ERROR [main] o.a.c.c.C.[Tomcat].[localhost].[/]: Exception starting filter
...
Address already in use (bind)
```

**Solution:**
```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change backend port in docker-compose.yml
# ports:
#   - "8081:8080"
```

---

### Issue 2: 401 Unauthorized Errors

**Symptoms:**
- All API calls return 401
- Swagger UI "Authorize" button doesn't work
- JWT token seems valid

**Diagnosis:**
```bash
# 1. Decode JWT token
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
echo $TOKEN | cut -d. -f2 | base64 -d | jq

# Check:
# - "iss" (issuer) matches Keycloak realm URL
# - "aud" (audience) includes "api"
# - "exp" (expiration) is in the future
# - "tenant" claim exists

# 2. Check Keycloak realm configuration
curl -s https://admin.core-platform.local/realms/admin/.well-known/openid-configuration | jq

# 3. Check backend logs
docker-compose logs backend | grep "JWT"
```

**Common Causes:**

**1. Issuer Mismatch**
```log
ERROR [http-nio-8080-exec-1] o.s.s.o.s.r.a.JwtAuthenticationProvider:
An error occurred while attempting to decode the Jwt: The iss claim is not valid
```

**Solution:**
```bash
# Check JWT issuer
echo $TOKEN | cut -d. -f2 | base64 -d | jq '.iss'
# Should be: "https://admin.core-platform.local/realms/admin"

# Fix in Keycloak realm-core-platform.json
# "issuer": "https://admin.core-platform.local/realms/admin"
docker-compose restart keycloak
```

**2. Audience Mismatch**
```log
ERROR [http-nio-8080-exec-1] o.s.s.o.s.r.a.JwtAuthenticationProvider:
An error occurred while attempting to decode the Jwt: The aud claim is not valid
```

**Solution:**
```properties
# backend/src/main/resources/application.properties
security.oauth2.audience=api

# Check Keycloak client configuration
# Audience mapper should add "api" to aud claim
```

**3. Token Expired**
```log
ERROR [http-nio-8080-exec-1] o.s.s.o.s.r.a.JwtAuthenticationProvider:
An error occurred while attempting to decode the Jwt: Jwt expired at 2025-10-12T10:00:00Z
```

**Solution:**
```bash
# Get new token
curl -X POST https://admin.core-platform.local/realms/admin/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=backend" \
  -d "username=admin@test.com" \
  -d "password=admin" \
  | jq -r '.access_token'
```

---

### Issue 3: 403 Forbidden Errors

**Symptoms:**
- Authentication succeeds (401 → 200)
- But authorization fails (403 Forbidden)
- User has correct roles in Keycloak

**Diagnosis:**
```bash
# 1. Check user roles in JWT
echo $TOKEN | cut -d. -f2 | base64 -d | jq '.realm_access.roles'

# 2. Check required roles in controller
grep -r "@PreAuthorize" backend/src/main/java/cz/muriel/core/controller/

# 3. Check backend logs
docker-compose logs backend | grep "Access Denied"
```

**Common Causes:**

**1. Missing Role Mapper in Keycloak**
```json
// JWT should contain:
{
  "realm_access": {
    "roles": ["CORE_ROLE_ADMIN", "CORE_ROLE_USER"]
  }
}

// If missing, check Keycloak client mappers
```

**Solution:**
```bash
# Add realm roles mapper in Keycloak
# Client: backend
# Mappers → Add Mapper → User Realm Role
# Name: realm roles
# Mapper Type: User Realm Role
# Token Claim Name: realm_access.roles
# Claim JSON Type: String
# Add to userinfo: ON
```

**2. Role Name Prefix Mismatch**
```java
// Controller expects:
@PreAuthorize("hasAuthority('CORE_ROLE_ADMIN')")

// But JWT contains:
// "roles": ["ADMIN"] (missing CORE_ROLE_ prefix)
```

**Solution:**
```properties
# backend/src/main/resources/application.properties
spring.security.oauth2.resourceserver.jwt.authorities-prefix=CORE_ROLE_

# Or fix roles in Keycloak to include prefix
```

---

## 🗄️ Database Issues

### Issue 4: Connection Pool Exhausted

**Symptoms:**
```log
ERROR [http-nio-8080-exec-10] com.zaxxer.hikari.pool.HikariPool:
HikariPool-1 - Connection is not available, request timed out after 30000ms.
```

**Diagnosis:**
```bash
# 1. Check active connections
docker exec -it core-platform-db-1 psql -U core -c \
  "SELECT COUNT(*) FROM pg_stat_activity WHERE datname='core';"

# 2. Check HikariCP metrics
curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq

# 3. Check slow queries
docker exec -it core-platform-db-1 psql -U core -c \
  "SELECT pid, now() - query_start AS duration, query
   FROM pg_stat_activity
   WHERE state = 'active' AND now() - query_start > interval '5 seconds'
   ORDER BY duration DESC;"
```

**Solutions:**

**1. Increase Pool Size**
```properties
# backend/src/main/resources/application.properties
spring.datasource.hikari.maximum-pool-size=50  # from 20
spring.datasource.hikari.minimum-idle=10  # from 5
```

**2. Find and Fix Slow Queries**
```bash
# Enable slow query logging
docker exec -it core-platform-db-1 psql -U core -c \
  "ALTER DATABASE core SET log_min_duration_statement = 1000;"  # Log queries >1s

# Check logs
docker-compose logs db | grep "duration:"
```

**3. Kill Long-Running Queries**
```bash
# Find long-running queries
docker exec -it core-platform-db-1 psql -U core -c \
  "SELECT pid, query FROM pg_stat_activity WHERE state = 'active' AND now() - query_start > interval '1 minute';"

# Kill specific query
docker exec -it core-platform-db-1 psql -U core -c "SELECT pg_terminate_backend(12345);"
```

---

### Issue 5: Database Migrations Fail

**Symptoms:**
```log
ERROR [main] o.f.c.i.c.DbMigrate: Migration V5__add_streaming_tables.sql failed
...
ERROR: relation "users" does not exist
```

**Diagnosis:**
```bash
# Check migration history
docker exec -it core-platform-db-1 psql -U core -c \
  "SELECT installed_rank, version, description, success
   FROM flyway_schema_history
   ORDER BY installed_rank;"
```

**Solutions:**

**1. Migration Depends on Missing Table**
```sql
-- V5__add_streaming_tables.sql tries to reference "users" table
-- but "users" table is in V4__add_users_table.sql which failed

-- Fix: Run V4 migration first
docker exec -it core-platform-backend-1 bash
./mvnw flyway:migrate -Dflyway.target=4
./mvnw flyway:migrate -Dflyway.target=5
```

**2. Checksum Mismatch (File Changed After Migration)**
```log
ERROR: Checksum mismatch for migration V3__add_tenants.sql
Expected: 123456789
Actual: 987654321
```

**Fix:**
```bash
# Repair Flyway metadata (recalculates checksums)
docker exec -it core-platform-backend-1 bash
./mvnw flyway:repair

# Or delete corrupt migration record and re-run
docker exec -it core-platform-db-1 psql -U core -c \
  "DELETE FROM flyway_schema_history WHERE version = '3';"
docker-compose restart backend
```

---

## 📨 Kafka Issues

### Issue 6: Kafka Consumer Lag

**Symptoms:**
```bash
# Check consumer lag
docker exec core-platform-kafka-1 kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group entity-lifecycle-consumer

# Output shows high LAG:
# TOPIC                  PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
# entity-lifecycle       0          1000            5000            4000  <-- HIGH LAG
```

**Diagnosis:**
```bash
# 1. Check consumer processing time
curl -s http://localhost:8080/actuator/metrics/kafka.consumer.time.avg | jq

# 2. Check consumer thread count
docker-compose logs backend | grep "KafkaMessageListenerContainer"

# 3. Check Kafka broker health
docker exec core-platform-kafka-1 kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

**Solutions:**

**1. Increase Consumer Concurrency**
```properties
# backend/src/main/resources/application.properties
spring.kafka.listener.concurrency=10  # from 3
```

**2. Increase Batch Size**
```properties
spring.kafka.consumer.max-poll-records=500  # from 100
spring.kafka.consumer.fetch.min.bytes=1048576  # 1 MB
```

**3. Check Consumer Code for Blocking Calls**
```java
// ❌ BAD: Blocking HTTP call in consumer
@KafkaListener(topics = "entity-lifecycle")
public void handleEvent(EntityLifecycleEvent event) {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.postForObject("http://slow-api/process", event, Void.class);  // BLOCKS
}

// ✅ GOOD: Async processing
@KafkaListener(topics = "entity-lifecycle")
@Async
public CompletableFuture<Void> handleEvent(EntityLifecycleEvent event) {
    return CompletableFuture.runAsync(() -> processEvent(event));
}
```

---

### Issue 7: Kafka Consumer Rebalancing Loop

**Symptoms:**
```log
INFO [org.apache.kafka.clients.consumer.internals.ConsumerCoordinator]:
[Consumer groupId=entity-lifecycle-consumer] Revoke previously assigned partitions entity-lifecycle-0
INFO [org.apache.kafka.clients.consumer.internals.ConsumerCoordinator]:
[Consumer groupId=entity-lifecycle-consumer] Setting offset for partition entity-lifecycle-0 to the committed offset FetchPosition{offset=1000, epoch=Optional.empty}
```

**Diagnosis:**
```bash
# Check session timeout
docker-compose logs backend | grep "session.timeout.ms"

# Check max poll interval
docker-compose logs backend | grep "max.poll.interval.ms"
```

**Solution:**
```properties
# backend/src/main/resources/application.properties
spring.kafka.consumer.properties.session.timeout.ms=30000  # 30 seconds
spring.kafka.consumer.properties.max.poll.interval.ms=300000  # 5 minutes
spring.kafka.consumer.properties.heartbeat.interval.ms=3000  # 3 seconds
```

---

## 🌐 Frontend Issues

### Issue 8: Frontend Build Fails

**Symptoms:**
```log
ERROR in ./src/components/UserList.tsx
Module not found: Can't resolve '@mui/material/Button'
```

**Solution:**
```bash
# Clear node_modules and reinstall
cd frontend
rm -rf node_modules package-lock.json
npm install

# Or rebuild Docker image
docker-compose build frontend --no-cache
```

---

### Issue 9: CORS Errors

**Symptoms:**
```log
Access to fetch at 'http://localhost:8080/api/users' from origin 'http://localhost:3001' has been blocked by CORS policy
```

**Solution:**
```java
// backend/src/main/java/cz/muriel/core/backend/config/SecurityConfig.java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:3001"));  // Add dev frontend
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

## 🔐 Keycloak Issues

### Issue 10: Keycloak Import Failed

**Symptoms:**
```log
ERROR [org.keycloak.services] KC-SERVICES0010: Failed to import realm
...
Realm with name 'admin' already exists
```

**Solution:**
```bash
# Option 1: Delete existing realm
docker exec -it core-platform-keycloak-1 bash
/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin
/opt/keycloak/bin/kcadm.sh delete realms/admin

# Option 2: Use override import
docker-compose down keycloak
docker volume rm core-platform_keycloak-data
docker-compose up -d keycloak
```

---

## 📊 Performance Issues

### Issue 11: High Memory Usage

**Symptoms:**
```bash
# Check container memory
docker stats core-platform-backend-1

# Output shows:
# CONTAINER              MEM USAGE / LIMIT     MEM %
# core-platform-backend  1.8GiB / 2GiB         90%  <-- HIGH
```

**Diagnosis:**
```bash
# 1. Heap dump
docker exec core-platform-backend-1 jmap -dump:format=b,file=/tmp/heapdump.hprof 1

# 2. Analyze with Eclipse MAT or VisualVM
docker cp core-platform-backend-1:/tmp/heapdump.hprof ./

# 3. Check JVM metrics
curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq
```

**Solutions:**

**1. Increase JVM Heap**
```yaml
# docker-compose.yml
backend:
  environment:
    JAVA_OPTS: "-Xmx2g -Xms1g"  # from -Xmx1g
```

**2. Find Memory Leaks**
```bash
# Check for growing objects
curl -s http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:heap | jq
```

---

## 🆘 Emergency Recovery

### Complete Reset (⚠️ LOSES ALL DATA)

```bash
# Stop all services
docker-compose down

# Remove volumes
docker volume rm core-platform_db-data
docker volume rm core-platform_keycloak-data
docker volume rm core-platform_kafka-data

# Restart
docker-compose up -d
```

---

## 📞 Escalation

If issue persists after troubleshooting:

1. **Check GitHub Issues:** https://github.com/Muriel2Horak/core-platform/issues
2. **Slack:** #core-platform-support
3. **Email:** support@core-platform.local
4. **On-call Engineer:** +420 XXX XXX XXX (CRITICAL issues only)

---

## 🔗 Related Documentation

- [DEPLOYMENT_DOCKER_COMPOSE.md](./DEPLOYMENT_DOCKER_COMPOSE.md)
- [SECURITY_RUNBOOK.md](./SECURITY_RUNBOOK.md)
- [PERFORMANCE_PROFILING.md](./PERFORMANCE_PROFILING.md)
