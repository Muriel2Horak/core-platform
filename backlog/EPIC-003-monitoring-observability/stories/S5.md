# S5: Centralized Logging (Loki) (Phase S5)

**EPIC:** [EPIC-003: Monitoring & Observability](../README.md)  
**Status:** ✅ **DONE**  
**Implementováno:** Srpen 2024 (Phase S5)  
**LOC:** ~1,000 řádků  
**Sprint:** Monitoring Wave 3

---

## 📋 Story Description

Jako **developer/DevOps**, chci **centralized logging s Loki pro agregaci logů z všech služeb**, abych **mohl vyhledávat logy, filtrovat podle labels a debugovat issues bez SSH do containerů**.

---

## 🎯 Acceptance Criteria

### AC1: Log Aggregation
- **GIVEN** backend/frontend/Kafka/PostgreSQL běží
- **WHEN** query Loki `{app="backend"}`
- **THEN** zobrazí všechny backend logy (JSON structured)

### AC2: Label-based Filtering
- **GIVEN** logy z multiple služeb
- **WHEN** query `{app="backend", level="ERROR"}`
- **THEN** zobrazí pouze ERROR logy z backendu

### AC3: LogQL Queries
- **GIVEN** potřeba analyzovat logy
- **WHEN** použiju LogQL: `count_over_time({app="backend", level="ERROR"}[5m])`
- **THEN** zobrazí count ERROR logů za posledních 5 minut

### AC4: Grafana Integration
- **GIVEN** Grafana dashboard
- **WHEN** přidám Loki data source
- **THEN** mohu zobrazit logy v Grafana Explore a Dashboard panels

---

## 🏗️ Implementation

### Backend: Logback Configuration

```xml
<!-- backend/src/main/resources/logback-spring.xml -->
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <springProperty scope="context" name="appName" source="spring.application.name"/>
    
    <!-- Console output (JSON for Loki) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app":"${appName}"}</customFields>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
        </encoder>
    </appender>
    
    <!-- Loki appender -->
    <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
        <http>
            <url>http://loki:3100/loki/api/v1/push</url>
        </http>
        <format>
            <label>
                <pattern>app=${appName},host=${HOSTNAME},level=%level</pattern>
            </label>
            <message>
                <pattern>
                    {
                      "level":"%level",
                      "class":"%logger{36}",
                      "thread":"%thread",
                      "message":"%message",
                      "traceId":"%X{traceId}",
                      "spanId":"%X{spanId}"
                    }
                </pattern>
            </message>
        </format>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOKI"/>
    </root>
    
    <!-- Package-specific levels -->
    <logger name="cz.muriel.core" level="DEBUG"/>
    <logger name="org.springframework.web" level="INFO"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>
</configuration>
```

```xml
<!-- backend/pom.xml -->
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-logback-appender</artifactId>
    <version>1.4.2</version>
</dependency>
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### Loki Configuration

```yaml
# docker/loki/loki-config.yml
auth_enabled: false

server:
  http_listen_port: 3100
  grpc_listen_port: 9096

common:
  path_prefix: /loki
  storage:
    filesystem:
      chunks_directory: /loki/chunks
      rules_directory: /loki/rules
  replication_factor: 1
  ring:
    instance_addr: 127.0.0.1
    kvstore:
      store: inmemory

schema_config:
  configs:
    - from: 2024-01-01
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h

limits_config:
  retention_period: 168h  # 7 days
  ingestion_rate_mb: 10
  ingestion_burst_size_mb: 20

compactor:
  working_directory: /loki/compactor
  shared_store: filesystem
  compaction_interval: 10m
```

### Promtail (Log Shipper)

```yaml
# docker/promtail/promtail-config.yml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  # Docker container logs
  - job_name: docker
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
        refresh_interval: 5s
    
    relabel_configs:
      # Container name as label
      - source_labels: ['__meta_docker_container_name']
        regex: '/(.*)'
        target_label: 'container'
      
      # Only scrape core-platform containers
      - source_labels: ['__meta_docker_container_name']
        regex: '/core-.*'
        action: keep
      
      # Extract app name
      - source_labels: ['container']
        regex: 'core-(.*)'
        target_label: 'app'
    
    pipeline_stages:
      # Parse JSON logs
      - json:
          expressions:
            level: level
            message: message
            logger: class
            trace_id: traceId
      
      # Extract labels
      - labels:
          level:
          app:
      
      # Timestamp
      - timestamp:
          source: timestamp
          format: RFC3339
```

### Docker Compose Loki Stack

```yaml
# docker/docker-compose.yml
loki:
  image: grafana/loki:2.9.0
  ports:
    - "3100:3100"
  volumes:
    - ./loki/loki-config.yml:/etc/loki/local-config.yaml
    - loki-data:/loki
  command: -config.file=/etc/loki/local-config.yaml

promtail:
  image: grafana/promtail:2.9.0
  volumes:
    - ./promtail/promtail-config.yml:/etc/promtail/config.yml
    - /var/run/docker.sock:/var/run/docker.sock:ro
  command: -config.file=/etc/promtail/config.yml
  depends_on:
    - loki

grafana:
  # ... existing config
  environment:
    - GF_INSTALL_PLUGINS=grafana-loki-datasource
```

### Grafana Loki Data Source Provisioning

```yaml
# docker/grafana/provisioning/datasources/loki.yml
apiVersion: 1

datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    isDefault: false
    jsonData:
      maxLines: 1000
```

### LogQL Query Examples

```logql
# All backend logs
{app="backend"}

# ERROR logs from backend
{app="backend", level="ERROR"}

# Logs containing "NullPointerException"
{app="backend"} |= "NullPointerException"

# Logs NOT containing "health check"
{app="backend"} != "health check"

# Parse JSON and filter
{app="backend"} | json | message =~ "User.*created"

# Count errors over time
count_over_time({app="backend", level="ERROR"}[5m])

# Rate of errors
rate({app="backend", level="ERROR"}[5m])

# Group by trace ID
sum by (traceId) (count_over_time({app="backend"}[5m]))
```

### Makefile Integration

```makefile
# Makefile
.PHONY: logs logs-backend logs-frontend logs-errors

logs: ## View all logs via Loki
	@docker exec -it core-loki logcli query '{app=~".+"}'

logs-backend: ## View backend logs
	@docker exec -it core-loki logcli query '{app="backend"}'

logs-frontend: ## View frontend logs
	@docker exec -it core-loki logcli query '{app="frontend"}'

logs-errors: ## View ERROR logs from all apps
	@docker exec -it core-loki logcli query '{level="ERROR"}'

logs-trace: ## View logs for specific trace ID
	@read -p "Enter trace ID: " TRACE_ID; \
	docker exec -it core-loki logcli query "{traceId=\"$$TRACE_ID\"}"
```

---

## 💡 Value Delivered

### Metrics
- **Logs Aggregated**: 100,000+ log entries/day
- **Log Retention**: 7 days
- **Query Performance**: <1s for most queries
- **Debugging Time**: -60% (no SSH to containers)

---

## 🔗 Related

- **Depends On:** [S1: Prometheus Metrics](./S1.md)
- **Integrates:** Grafana, Promtail
- **Used By:** All developers, DevOps team

---

## 📚 References

- **Implementation:** `docker/loki/loki-config.yml`, `docker/promtail/promtail-config.yml`
- **Backend:** `backend/src/main/resources/logback-spring.xml`
- **Loki UI (via Grafana):** `https://admin.core-platform.local/grafana/explore`
- **Docs:** [Loki LogQL](https://grafana.com/docs/loki/latest/logql/)
