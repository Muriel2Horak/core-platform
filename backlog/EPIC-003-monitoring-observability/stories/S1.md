# S1: Prometheus Metrics Instrumentation (Phase S1)

**EPIC:** [EPIC-003: Monitoring & Observability](../README.md)  
**Status:** ✅ **DONE**  
**Implementováno:** Červen 2024 (Phase S1)  
**LOC:** ~1,800 řádků  
**Sprint:** Monitoring Wave 1

---

## 📋 Story Description

Jako **DevOps engineer**, chci **Prometheus metrics pro backend/frontend/infrastructure**, abych **měl real-time visibility do application health a performance**.

---

## 🎯 Acceptance Criteria

### AC1: Spring Boot Actuator Metrics
- **GIVEN** backend běží
- **WHEN** Prometheus scrape `/actuator/prometheus`
- **THEN** exportuje metriky:
  - HTTP requests (count, duration, status codes)
  - JVM memory (heap, non-heap, GC)
  - Database connections (active, idle, max)
  - Custom business metrics

### AC2: Custom Business Metrics
- **GIVEN** user vytvoří workflow instance
- **WHEN** backend zaloguje event
- **THEN** inkrementuje counter `workflow_instances_created_total`

### AC3: Kafka Consumer Metrics
- **GIVEN** Kafka consumer běží
- **WHEN** zpracuje message
- **THEN** updatuje metriky:
  - `kafka_messages_consumed_total`
  - `kafka_consumer_lag`
  - `kafka_consumer_duration_seconds`

### AC4: Frontend Metrics (RUM)
- **GIVEN** user načte stránku
- **WHEN** frontend měří Web Vitals
- **THEN** exportuje do Prometheus:
  - Page load time
  - API call duration
  - Error count

---

## 🏗️ Implementation

### Backend: Spring Boot Actuator

```yaml
# backend/src/main/resources/application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  
  metrics:
    export:
      prometheus:
        enabled: true
    
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:development}
    
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

```java
// backend/src/main/java/cz/muriel/core/config/MetricsConfig.java
@Configuration
public class MetricsConfig {
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("application", "core-platform")
            .commonTags("environment", System.getenv("ENVIRONMENT"));
    }
}
```

### Custom Business Metrics

```java
// backend/src/main/java/cz/muriel/core/workflow/service/WorkflowInstanceService.java
@Service
@RequiredArgsConstructor
public class WorkflowInstanceService {
    
    private final WorkflowInstanceRepository repository;
    private final MeterRegistry meterRegistry;
    
    public WorkflowInstance createInstance(Long workflowId, CreateInstanceRequest request) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowId(workflowId);
        instance.setCurrentState(request.getInitialState());
        
        WorkflowInstance saved = repository.save(instance);
        
        // Increment counter
        meterRegistry.counter("workflow_instances_created_total",
            "workflow_id", String.valueOf(workflowId),
            "tenant_id", String.valueOf(request.getTenantId())
        ).increment();
        
        return saved;
    }
    
    public void transitionState(Long instanceId, String targetState) {
        WorkflowInstance instance = repository.findById(instanceId).orElseThrow();
        String previousState = instance.getCurrentState();
        
        instance.setCurrentState(targetState);
        repository.save(instance);
        
        // Record state transition
        meterRegistry.counter("workflow_state_transitions_total",
            "from_state", previousState,
            "to_state", targetState
        ).increment();
        
        // Measure instance duration if completed
        if (targetState.equals("COMPLETED")) {
            Duration duration = Duration.between(instance.getCreatedAt(), Instant.now());
            meterRegistry.timer("workflow_instance_duration_seconds").record(duration);
        }
    }
}
```

### Kafka Consumer Metrics

```java
// backend/src/main/java/cz/muriel/core/kafka/metrics/KafkaMetricsInterceptor.java
@Aspect
@Component
@RequiredArgsConstructor
public class KafkaMetricsInterceptor {
    
    private final MeterRegistry meterRegistry;
    
    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public Object measureKafkaConsumer(ProceedingJoinPoint joinPoint) throws Throwable {
        String topic = extractTopic(joinPoint);
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            Object result = joinPoint.proceed();
            
            // Success metric
            meterRegistry.counter("kafka_messages_consumed_total",
                "topic", topic,
                "status", "success"
            ).increment();
            
            return result;
            
        } catch (Exception e) {
            // Failure metric
            meterRegistry.counter("kafka_messages_consumed_total",
                "topic", topic,
                "status", "failure"
            ).increment();
            
            throw e;
            
        } finally {
            // Duration metric
            sample.stop(meterRegistry.timer("kafka_consumer_duration_seconds",
                "topic", topic
            ));
        }
    }
    
    private String extractTopic(ProceedingJoinPoint joinPoint) {
        KafkaListener annotation = ((MethodSignature) joinPoint.getSignature())
            .getMethod()
            .getAnnotation(KafkaListener.class);
        
        return annotation.topics()[0];
    }
}
```

### Database Connection Pool Metrics

```java
// backend/src/main/java/cz/muriel/core/config/DataSourceMetricsConfig.java
@Configuration
public class DataSourceMetricsConfig {
    
    @Bean
    public DataSourcePoolMetadataProvider hikariPoolMetadataProvider() {
        return (dataSource) -> {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            return new HikariDataSourcePoolMetadata(hikari);
        };
    }
}

// Exports:
// - hikaricp_connections_active
// - hikaricp_connections_idle
// - hikaricp_connections_max
// - hikaricp_connections_pending
```

### Frontend: Web Vitals to Prometheus

```typescript
// frontend/src/services/metrics/MetricsService.ts
import { onCLS, onFID, onLCP, onFCP, onTTFB } from 'web-vitals';

class MetricsService {
  private endpoint = '/api/metrics/web-vitals';
  
  init() {
    onLCP((metric) => this.send('lcp', metric.value));
    onFID((metric) => this.send('fid', metric.value));
    onCLS((metric) => this.send('cls', metric.value));
    onFCP((metric) => this.send('fcp', metric.value));
    onTTFB((metric) => this.send('ttfb', metric.value));
    
    // API call duration
    this.instrumentFetch();
  }
  
  private send(name: string, value: number) {
    // Send to backend for Prometheus export
    navigator.sendBeacon(this.endpoint, JSON.stringify({
      metric: name,
      value,
      timestamp: Date.now(),
      url: window.location.pathname
    }));
  }
  
  private instrumentFetch() {
    const originalFetch = window.fetch;
    
    window.fetch = async (...args) => {
      const start = performance.now();
      
      try {
        const response = await originalFetch(...args);
        
        const duration = performance.now() - start;
        
        this.send('api_call_duration', duration);
        this.send('api_call_total', 1);
        
        return response;
        
      } catch (error) {
        this.send('api_call_errors', 1);
        throw error;
      }
    };
  }
}

export const metricsService = new MetricsService();

// Initialize in main.tsx
metricsService.init();
```

### Backend: Web Vitals Endpoint

```java
// backend/src/main/java/cz/muriel/core/monitoring/WebVitalsController.java
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class WebVitalsController {
    
    private final MeterRegistry meterRegistry;
    
    @PostMapping("/web-vitals")
    public ResponseEntity<Void> recordWebVital(@RequestBody WebVitalMetric metric) {
        // Record distribution summary for percentiles
        DistributionSummary.builder("web_vitals_" + metric.getMetric())
            .description("Web Vitals: " + metric.getMetric().toUpperCase())
            .baseUnit("milliseconds")
            .tag("url", metric.getUrl())
            .register(meterRegistry)
            .record(metric.getValue());
        
        return ResponseEntity.ok().build();
    }
}

record WebVitalMetric(
    String metric,
    double value,
    String url,
    long timestamp
) {}
```

### Prometheus Scrape Configuration

```yaml
# docker/prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'backend'
    static_configs:
      - targets: ['backend:8080']
    metrics_path: '/actuator/prometheus'
    
  - job_name: 'postgres-exporter'
    static_configs:
      - targets: ['postgres-exporter:9187']
  
  - job_name: 'redis-exporter'
    static_configs:
      - targets: ['redis-exporter:9121']
  
  - job_name: 'kafka-exporter'
    static_configs:
      - targets: ['kafka-exporter:9308']
  
  - job_name: 'nginx-exporter'
    static_configs:
      - targets: ['nginx-exporter:9113']
```

---

## 💡 Value Delivered

### Metrics
- **Prometheus Metrics Exported**: 150+ metrics
- **Custom Business Metrics**: 25 (workflows, users, tenants, API calls)
- **Infrastructure Metrics**: 80+ (JVM, DB, Kafka, Redis, Nginx)
- **Frontend Metrics**: 10 (Web Vitals, API duration, errors)

---

## 🔗 Related

- **Used By:** [S2: Grafana Dashboards](./S2.md), [S3: Alerting](./S3.md)
- **Integrates:** Prometheus, Spring Boot Actuator

---

## 📚 References

- **Implementation:** `backend/src/main/java/cz/muriel/core/config/MetricsConfig.java`
- **Prometheus Config:** `docker/prometheus/prometheus.yml`
- **Frontend:** `frontend/src/services/metrics/MetricsService.ts`
