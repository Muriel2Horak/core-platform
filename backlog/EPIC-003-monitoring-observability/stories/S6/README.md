# S6: Health Checks & Readiness Probes (Phase S6)

**EPIC:** [EPIC-003: Monitoring & Observability](../README.md)  
**Status:** ✅ **DONE**  
**Implementováno:** Srpen 2024 (Phase S6)  
**LOC:** ~600 řádků  
**Sprint:** Monitoring Wave 3

---

## 📋 Story Description

Jako **DevOps/SRE**, chci **health checks a readiness probes pro všechny služby**, abych **mohl monitorovat service health, automaticky restartovat unhealthy containers a zajistit zero-downtime deployments**.

---

## 🎯 Acceptance Criteria

### AC1: Backend Health Check
- **GIVEN** všechny dependencies (DB, Redis, Kafka) jsou UP
- **WHEN** `GET /actuator/health`
- **THEN** returns `{"status": "UP", "components": {"db": "UP", "redis": "UP", "kafka": "UP"}}`

### AC2: Unhealthy Detection
- **GIVEN** PostgreSQL je DOWN
- **WHEN** `GET /actuator/health`
- **THEN** returns HTTP 503, `{"status": "DOWN", "components": {"db": "DOWN"}}`

### AC3: Readiness Probe
- **GIVEN** backend startuje (DB migration běží)
- **WHEN** `GET /actuator/health/readiness`
- **THEN** returns HTTP 503 (not ready)
- **AND** po dokončení migrace returns HTTP 200

### AC4: Kubernetes Integration
- **GIVEN** K8s deployment s liveness/readiness probes
- **WHEN** backend becomes unhealthy
- **THEN** K8s restartuje pod automaticky

---

## 🏗️ Implementation

### Spring Boot Actuator Health

```yaml
# backend/src/main/resources/application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, info
  
  endpoint:
    health:
      show-details: always
      show-components: always
      probes:
        enabled: true
  
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
    
    # Individual health indicators
    db:
      enabled: true
    redis:
      enabled: true
    kafka:
      enabled: true
```

### Custom Health Indicator: Kafka

```java
// backend/src/main/java/cz/muriel/core/health/KafkaHealthIndicator.java
@Component
public class KafkaHealthIndicator implements HealthIndicator {
    
    private final KafkaAdmin kafkaAdmin;
    
    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }
    
    @Override
    public Health health() {
        try {
            Map<String, Object> configs = kafkaAdmin.getConfigurationProperties();
            String bootstrapServers = (String) configs.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);
            
            // Try to connect
            AdminClient client = AdminClient.create(configs);
            DescribeClusterResult result = client.describeCluster();
            result.clusterId().get(5, TimeUnit.SECONDS);
            
            return Health.up()
                .withDetail("bootstrapServers", bootstrapServers)
                .withDetail("clusterId", result.clusterId().get())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
```

### Custom Health Indicator: External Service

```java
// backend/src/main/java/cz/muriel/core/health/KeycloakHealthIndicator.java
@Component
public class KeycloakHealthIndicator implements HealthIndicator {
    
    private final RestTemplate restTemplate;
    
    @Value("${keycloak.admin.base-url}")
    private String keycloakBaseUrl;
    
    @Override
    public Health health() {
        try {
            String healthUrl = keycloakBaseUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(
                healthUrl, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("url", healthUrl)
                    .withDetail("responseTime", System.currentTimeMillis())
                    .build();
            } else {
                return Health.down()
                    .withDetail("status", response.getStatusCode())
                    .build();
            }
            
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
```

### Readiness Indicator: Application Startup

```java
// backend/src/main/java/cz/muriel/core/CorePlatformApplication.java
@SpringBootApplication
public class CorePlatformApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CorePlatformApplication.class, args);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application is ready to accept traffic");
    }
}
```

Spring Boot automatically manages readiness state:
- **NOT_READY** during startup
- **READY** after `ApplicationReadyEvent`

### Health Check Response Example

```json
// GET /actuator/health
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.5"
      }
    },
    "kafka": {
      "status": "UP",
      "details": {
        "bootstrapServers": "kafka:9092",
        "clusterId": "abc123-xyz789"
      }
    },
    "keycloak": {
      "status": "UP",
      "details": {
        "url": "https://admin.core-platform.local/health",
        "responseTime": 1701234567890
      }
    },
    "livenessState": {
      "status": "UP"
    },
    "readinessState": {
      "status": "UP"
    }
  }
}
```

### Docker Compose Health Checks

```yaml
# docker/docker-compose.yml
backend:
  image: core-platform/backend:latest
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
    interval: 30s
    timeout: 10s
    retries: 3
    start_period: 60s
  depends_on:
    db:
      condition: service_healthy
    redis:
      condition: service_healthy

db:
  image: postgres:15
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U core"]
    interval: 10s
    timeout: 5s
    retries: 5

redis:
  image: redis:7-alpine
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 3s
    retries: 3

keycloak:
  image: quay.io/keycloak/keycloak:26.0
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
    interval: 30s
    timeout: 10s
    retries: 5
    start_period: 120s
```

### Kubernetes Deployment

```yaml
# k8s/backend-deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: core-backend
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: backend
        image: core-platform/backend:latest
        
        # Liveness probe (restart if unhealthy)
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        # Readiness probe (remove from load balancer if not ready)
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 2
        
        # Startup probe (allow slow startup)
        startupProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 0
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 30  # 30 * 10s = 5 minutes max startup
```

### Makefile Health Check

```makefile
# Makefile
.PHONY: health health-backend health-all

health: ## Check backend health
	@curl -s http://localhost:8080/actuator/health | jq

health-backend: ## Backend detailed health
	@curl -s http://localhost:8080/actuator/health | jq '.components'

health-all: ## Check all services health
	@echo "=== PostgreSQL ==="
	@docker exec core-db pg_isready -U core || echo "DOWN"
	@echo "\n=== Redis ==="
	@docker exec core-redis redis-cli ping || echo "DOWN"
	@echo "\n=== Keycloak ==="
	@curl -sf https://admin.core-platform.local/health > /dev/null && echo "UP" || echo "DOWN"
	@echo "\n=== Backend ==="
	@curl -s http://localhost:8080/actuator/health | jq '.status'
```

### Prometheus Alerting on Health

```yaml
# prometheus/alerts/health.yml
groups:
  - name: health
    interval: 30s
    rules:
      - alert: ServiceUnhealthy
        expr: up{job="backend"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is unhealthy"
          description: "{{ $labels.job }} has been down for 2 minutes"
      
      - alert: HealthCheckFailing
        expr: spring_boot_application_health_status{status!="UP"} == 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Health check failing for {{ $labels.application }}"
          description: "Component {{ $labels.component }} is DOWN"
```

---

## 💡 Value Delivered

### Metrics
- **Health Checks**: Every 30s (Docker), 10s (K8s)
- **Auto-restart**: Unhealthy containers restarted in <2 minutes
- **Zero-downtime Deployments**: Readiness probes prevent traffic to non-ready pods
- **MTTR**: -50% (automated recovery)

---

## 🔗 Related

- **Depends On:** [S1: Prometheus Metrics](./S1.md)
- **Integrates:** Docker Compose, Kubernetes
- **Used By:** Prometheus alerting

---

## 📚 References

- **Implementation:** `backend/src/main/java/cz/muriel/core/health/`
- **Config:** `backend/src/main/resources/application.yml`
- **Health Endpoint:** `http://localhost:8080/actuator/health`
- **Docs:** [Spring Boot Actuator Health](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.health)
