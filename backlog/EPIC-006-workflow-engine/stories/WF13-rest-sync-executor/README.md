# WF13: REST_SYNC Executor - Synchronní REST API Volání

**Typ:** TASK  
**Epic:** EPIC-006 (Workflow Engine - Internal Layer)  
**Fase:** Phase 2 (Typed Executors)  
**Priorita:** HIGH (kritická integrace s externími systémy)  
**Effort:** 1,200 LOC, 5 dní  
**Dependencies:** W7 (Executor Framework)  
**Status:** ⏳ TODO

---

## 🎯 Cíl

Implementovat **REST_SYNC executor** pro synchronní volání REST API v rámci workflow kroků. Executor musí podporovat:
- OpenAPI 3.0/Swagger spec parsing
- HTTP client code generation (nebo runtime proxy)
- Template substitution pro URL/body/headers (`${variable}`)
- Retry s exponential backoff (Resilience4j)
- Circuit breaker pattern
- Timeout handling
- Idempotence via correlation ID
- Response validation (JSON schema)

**Use Case:**
```yaml
steps:
  - id: "create-jira-ticket"
    type: "REST_SYNC"
    config:
      openApiSpec: "https://jira.company.com/openapi.json"
      operationId: "createIssue"
      endpoint: "POST /rest/api/3/issue"
      requestTemplate:
        fields:
          project:
            key: "PROJ"
          summary: "${workflow.context.ticketTitle}"
          description: "${workflow.context.description}"
          issuetype:
            name: "Task"
      headers:
        Authorization: "Bearer ${secrets.jiraApiToken}"
      timeout: 30s
      retry:
        maxAttempts: 3
        backoff: EXPONENTIAL
        initialDelay: 1s
        maxDelay: 10s
      circuitBreaker:
        failureThreshold: 5
        waitDuration: 60s
      idempotencyKey: "${workflow.instanceId}-${step.id}"
    outputMapping:
      jiraTicketId: "$.id"
      jiraTicketKey: "$.key"
```

---

## 📋 Požadavky

### Funkční Požadavky

1. **OpenAPI Spec Support**
   - Parse OpenAPI 3.0 / Swagger 2.0 specs
   - Extract operations (operationId, path, method, parameters, requestBody, responses)
   - Schema validation (request/response against JSON schema)
   - Auto-generate client methods (nebo runtime proxy)

2. **HTTP Client**
   - Support všech HTTP metod (GET, POST, PUT, PATCH, DELETE)
   - Custom headers (včetně Authentication)
   - Request body (JSON, form-data, multipart)
   - Response parsing (JSON, XML, plain text)
   - SSL/TLS support (včetně self-signed certifikátů)

3. **Template Substitution**
   - URL parameters: `/users/${userId}/orders`
   - Request body: `{"name": "${entity.name}"}`
   - Headers: `Authorization: Bearer ${secrets.apiToken}`
   - Query params: `?filter=${workflow.context.filter}`
   - Context sources: `workflow.context.*`, `entity.*`, `secrets.*`, `step.input.*`

4. **Resilience Patterns**
   - **Retry**: Exponential backoff, configurable maxAttempts
   - **Circuit Breaker**: Failure threshold, open/half-open/closed states
   - **Timeout**: Per-request timeout, global timeout
   - **Fallback**: Default response on failure

5. **Idempotence**
   - Correlation ID header (`X-Idempotency-Key`)
   - Persistent storage (workflow_rest_calls table)
   - Duplicate detection (same idempotency key → return cached response)

6. **Error Handling**
   - HTTP 4xx → business error (validation, authorization)
   - HTTP 5xx → retriable error (server issue)
   - Network timeout → retriable error
   - Circuit breaker open → skip retry, return cached fallback

### Non-Funkční Požadavky

1. **Performance**
   - Max 5s p99 latency (excluding external API response time)
   - Connection pooling (max 50 connections per host)
   - Keep-alive connections

2. **Security**
   - Secrets injection from Vault/environment (ne plain-text)
   - TLS 1.2+ enforcement
   - Certificate validation (with option to disable for dev)
   - SSRF protection (whitelist allowed hosts)

3. **Observability**
   - Prometheus metrics (request count, latency, error rate per endpoint)
   - OpenTelemetry tracing (distributed trace ID)
   - Structured logs (request/response payloads for debugging)

---

## 🗄️ Database Schema

```sql
-- Idempotence tracking & response cache
CREATE TABLE workflow_rest_calls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_instance_id UUID NOT NULL REFERENCES workflow_instances(id) ON DELETE CASCADE,
    step_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    
    -- Request details
    http_method VARCHAR(10) NOT NULL,
    url TEXT NOT NULL,
    headers JSONB,
    request_body TEXT,
    
    -- Response details
    status_code INTEGER,
    response_headers JSONB,
    response_body TEXT,
    
    -- Timing
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    duration_ms INTEGER,
    
    -- Retry tracking
    attempt_number INTEGER DEFAULT 1,
    
    -- Error handling
    error_message TEXT,
    circuit_breaker_state VARCHAR(20), -- CLOSED, OPEN, HALF_OPEN
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE (idempotency_key)
);

CREATE INDEX idx_rest_calls_workflow_instance ON workflow_rest_calls(workflow_instance_id);
CREATE INDEX idx_rest_calls_idempotency_key ON workflow_rest_calls(idempotency_key);
CREATE INDEX idx_rest_calls_created_at ON workflow_rest_calls(created_at);

-- OpenAPI spec cache (avoid re-parsing same spec)
CREATE TABLE workflow_openapi_specs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    spec_url TEXT NOT NULL UNIQUE,
    spec_content JSONB NOT NULL,
    spec_version VARCHAR(20), -- "3.0", "2.0"
    
    -- Parsed operations (for fast lookup)
    operations JSONB, -- {"/users": {"GET": {...}, "POST": {...}}}
    
    -- Cache metadata
    fetched_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP, -- TTL for cache invalidation
    etag VARCHAR(255), -- HTTP ETag for conditional requests
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_openapi_specs_url ON workflow_openapi_specs(spec_url);
```

---

## 🔧 Implementace

### 1. Java Implementation

**File:** `backend/src/main/java/cz/muriel/core/workflow/executor/RestSyncExecutor.java`

```java
package cz.muriel.core.workflow.executor;

import cz.muriel.core.workflow.model.WorkflowExecution;
import cz.muriel.core.workflow.model.WorkflowStep;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestSyncExecutor implements WorkflowExecutor {
    
    private final WebClient.Builder webClientBuilder;
    private final OpenApiSpecParser openApiParser;
    private final TemplateEngine templateEngine;
    private final WorkflowRestCallRepository restCallRepository;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MetricsService metricsService;
    
    @Override
    public boolean supports(String executorType) {
        return "REST_SYNC".equals(executorType);
    }
    
    @Override
    public Map<String, Object> execute(WorkflowExecution execution, WorkflowStep step) {
        log.info("Executing REST_SYNC step: workflowInstanceId={}, stepId={}", 
            execution.getInstanceId(), step.getId());
        
        RestSyncConfig config = parseConfig(step.getConfig());
        
        // 1. Check idempotency (already executed?)
        String idempotencyKey = resolveIdempotencyKey(config, execution, step);
        Optional<WorkflowRestCall> existingCall = restCallRepository
            .findByIdempotencyKey(idempotencyKey);
        
        if (existingCall.isPresent()) {
            log.info("REST call already executed (idempotent), returning cached response");
            return existingCall.get().getResponseBody();
        }
        
        // 2. Resolve template variables
        String resolvedUrl = templateEngine.resolve(config.getEndpoint(), execution.getContext());
        Map<String, String> resolvedHeaders = templateEngine.resolveMap(config.getHeaders(), execution.getContext());
        String resolvedBody = templateEngine.resolve(config.getRequestBody(), execution.getContext());
        
        // 3. Build HTTP client with resilience patterns
        CircuitBreaker circuitBreaker = buildCircuitBreaker(config);
        Retry retry = buildRetry(config);
        
        WebClient client = webClientBuilder
            .baseUrl(config.getBaseUrl())
            .defaultHeaders(headers -> resolvedHeaders.forEach(headers::add))
            .build();
        
        // 4. Execute HTTP request with retry + circuit breaker
        WorkflowRestCall call = new WorkflowRestCall();
        call.setWorkflowInstanceId(execution.getInstanceId());
        call.setStepId(step.getId());
        call.setIdempotencyKey(idempotencyKey);
        call.setHttpMethod(config.getMethod());
        call.setUrl(resolvedUrl);
        call.setHeaders(resolvedHeaders);
        call.setRequestBody(resolvedBody);
        call.setStartedAt(Instant.now());
        
        try {
            String response = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
                    return executeHttpCall(client, config, resolvedUrl, resolvedBody)
                        .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                        .block();
                })
            ).get();
            
            call.setResponseBody(response);
            call.setStatusCode(200); // TODO: get actual status from WebClient
            call.setCompletedAt(Instant.now());
            call.setDurationMs((int) Duration.between(call.getStartedAt(), call.getCompletedAt()).toMillis());
            
            restCallRepository.save(call);
            
            // 5. Parse response & apply output mapping
            Map<String, Object> output = parseResponse(response, config.getOutputMapping());
            
            metricsService.recordRestCall(config.getEndpoint(), "success", call.getDurationMs());
            
            return output;
            
        } catch (Exception e) {
            log.error("REST call failed: {}", e.getMessage(), e);
            call.setErrorMessage(e.getMessage());
            call.setCompletedAt(Instant.now());
            restCallRepository.save(call);
            
            metricsService.recordRestCall(config.getEndpoint(), "error", 0);
            
            throw new WorkflowExecutionException("REST_SYNC failed: " + e.getMessage(), e);
        }
    }
    
    private Mono<String> executeHttpCall(WebClient client, RestSyncConfig config, String url, String body) {
        return switch (config.getMethod()) {
            case "GET" -> client.get().uri(url).retrieve().bodyToMono(String.class);
            case "POST" -> client.post().uri(url).bodyValue(body).retrieve().bodyToMono(String.class);
            case "PUT" -> client.put().uri(url).bodyValue(body).retrieve().bodyToMono(String.class);
            case "PATCH" -> client.patch().uri(url).bodyValue(body).retrieve().bodyToMono(String.class);
            case "DELETE" -> client.delete().uri(url).retrieve().bodyToMono(String.class);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + config.getMethod());
        };
    }
    
    private CircuitBreaker buildCircuitBreaker(RestSyncConfig config) {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(config.getCircuitBreakerFailureThreshold())
            .waitDurationInOpenState(Duration.ofSeconds(config.getCircuitBreakerWaitDuration()))
            .slidingWindowSize(10)
            .build();
        
        return circuitBreakerRegistry.circuitBreaker("rest-sync-" + config.getEndpoint(), cbConfig);
    }
    
    private Retry buildRetry(RestSyncConfig config) {
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(config.getRetryMaxAttempts())
            .waitDuration(Duration.ofMillis(config.getRetryInitialDelayMs()))
            .intervalFunction(IntervalFunction.ofExponentialBackoff(
                config.getRetryInitialDelayMs(), 
                config.getRetryBackoffMultiplier()
            ))
            .build();
        
        return Retry.of("rest-sync-retry", retryConfig);
    }
    
    @Override
    public void compensate(WorkflowExecution execution, WorkflowStep step) {
        log.warn("REST_SYNC compensation not implemented (idempotent calls)");
        // TODO: Implement compensation logic (e.g., DELETE call to reverse POST)
    }
}
```

**File:** `backend/src/main/java/cz/muriel/core/workflow/executor/RestSyncConfig.java`

```java
@Data
public class RestSyncConfig {
    private String openApiSpec; // URL nebo inline YAML
    private String operationId; // e.g., "createIssue"
    private String endpoint; // e.g., "POST /rest/api/3/issue"
    private String baseUrl; // e.g., "https://jira.company.com"
    private String method; // GET, POST, PUT, PATCH, DELETE
    private Map<String, String> headers;
    private String requestBody; // JSON template
    private Integer timeoutSeconds = 30;
    private RetryConfig retry;
    private CircuitBreakerConfig circuitBreaker;
    private String idempotencyKey; // Template: "${workflow.instanceId}-${step.id}"
    private Map<String, String> outputMapping; // JSONPath → variable name
    
    @Data
    public static class RetryConfig {
        private Integer maxAttempts = 3;
        private String backoff = "EXPONENTIAL"; // FIXED, EXPONENTIAL
        private Long initialDelayMs = 1000L;
        private Long maxDelayMs = 10000L;
        private Double backoffMultiplier = 2.0;
    }
    
    @Data
    public static class CircuitBreakerConfig {
        private Integer failureThreshold = 5; // % failure rate to open circuit
        private Integer waitDuration = 60; // seconds to wait before half-open
    }
}
```

**File:** `backend/src/main/java/cz/muriel/core/workflow/executor/TemplateEngine.java`

```java
@Component
public class TemplateEngine {
    
    public String resolve(String template, Map<String, Object> context) {
        if (template == null) return null;
        
        // Simple ${variable} substitution
        String result = template;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        
        // Support nested paths: ${entity.name}, ${workflow.context.ticketTitle}
        // TODO: Use SpEL or JsonPath for complex expressions
        
        return result;
    }
    
    public Map<String, String> resolveMap(Map<String, String> map, Map<String, Object> context) {
        if (map == null) return Map.of();
        
        Map<String, String> resolved = new HashMap<>();
        map.forEach((key, value) -> resolved.put(key, resolve(value, context)));
        return resolved;
    }
}
```

**File:** `backend/src/main/java/cz/muriel/core/workflow/executor/OpenApiSpecParser.java`

```java
@Component
public class OpenApiSpecParser {
    
    public OpenApiSpec parse(String specUrl) {
        // Use Swagger Parser or OpenAPI Generator
        // Parse YAML/JSON → extract operations
        
        // Cache parsed spec in workflow_openapi_specs table
        
        // Return OpenApiSpec object with:
        // - operations: Map<String, Operation> (operationId → Operation)
        // - schemas: Map<String, Schema> (for validation)
    }
}
```

---

### 2. Database Entities

**File:** `backend/src/main/java/cz/muriel/core/workflow/model/WorkflowRestCall.java`

```java
@Entity
@Table(name = "workflow_rest_calls")
@Data
public class WorkflowRestCall {
    
    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;
    
    @Column(name = "workflow_instance_id", nullable = false)
    private UUID workflowInstanceId;
    
    @Column(name = "step_id", nullable = false, length = 100)
    private String stepId;
    
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;
    
    @Column(name = "http_method", length = 10)
    private String httpMethod;
    
    @Column(name = "url", columnDefinition = "TEXT")
    private String url;
    
    @Type(JsonBinaryType.class)
    @Column(name = "headers", columnDefinition = "jsonb")
    private Map<String, String> headers;
    
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;
    
    @Column(name = "status_code")
    private Integer statusCode;
    
    @Type(JsonBinaryType.class)
    @Column(name = "response_headers", columnDefinition = "jsonb")
    private Map<String, String> responseHeaders;
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(name = "started_at")
    private Instant startedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "duration_ms")
    private Integer durationMs;
    
    @Column(name = "attempt_number")
    private Integer attemptNumber = 1;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "circuit_breaker_state", length = 20)
    private String circuitBreakerState;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
```

---

### 3. Repository

**File:** `backend/src/main/java/cz/muriel/core/workflow/repository/WorkflowRestCallRepository.java`

```java
public interface WorkflowRestCallRepository extends JpaRepository<WorkflowRestCall, UUID> {
    
    Optional<WorkflowRestCall> findByIdempotencyKey(String idempotencyKey);
    
    List<WorkflowRestCall> findByWorkflowInstanceId(UUID workflowInstanceId);
    
    @Query("SELECT wrc FROM WorkflowRestCall wrc WHERE wrc.createdAt < :cutoff")
    List<WorkflowRestCall> findOldCalls(@Param("cutoff") Instant cutoff);
}
```

---

### 4. Tests

**File:** `backend/src/test/java/cz/muriel/core/workflow/executor/RestSyncExecutorTest.java`

```java
@SpringBootTest
@Testcontainers
class RestSyncExecutorTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    
    @Container
    static MockServerContainer mockServer = new MockServerContainer(
        DockerImageName.parse("mockserver/mockserver:latest")
    );
    
    @Autowired
    private RestSyncExecutor executor;
    
    @Autowired
    private WorkflowRestCallRepository repository;
    
    @Test
    void shouldExecuteGetRequest() {
        // Given: Mock server returns user data
        mockServer.getClient()
            .when(request().withPath("/users/123"))
            .respond(response()
                .withStatusCode(200)
                .withBody("{\"id\": 123, \"name\": \"John Doe\"}")
            );
        
        WorkflowStep step = WorkflowStep.builder()
            .id("get-user")
            .type("REST_SYNC")
            .config(Map.of(
                "endpoint", "GET /users/123",
                "baseUrl", mockServer.getEndpoint(),
                "outputMapping", Map.of("userId", "$.id", "userName", "$.name")
            ))
            .build();
        
        // When
        Map<String, Object> output = executor.execute(mockExecution(), step);
        
        // Then
        assertThat(output).containsEntry("userId", 123);
        assertThat(output).containsEntry("userName", "John Doe");
        
        // Verify idempotency record created
        List<WorkflowRestCall> calls = repository.findByWorkflowInstanceId(mockExecution().getInstanceId());
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).getStatusCode()).isEqualTo(200);
    }
    
    @Test
    void shouldRetryOnServerError() {
        // Given: Mock server fails 2 times, then succeeds
        mockServer.getClient()
            .when(request().withPath("/api/data"), Times.exactly(2))
            .respond(response().withStatusCode(500));
        
        mockServer.getClient()
            .when(request().withPath("/api/data"), Times.exactly(1))
            .respond(response().withStatusCode(200).withBody("{\"status\": \"ok\"}"));
        
        WorkflowStep step = WorkflowStep.builder()
            .config(Map.of(
                "endpoint", "GET /api/data",
                "retry", Map.of("maxAttempts", 3, "initialDelayMs", 100)
            ))
            .build();
        
        // When
        Map<String, Object> output = executor.execute(mockExecution(), step);
        
        // Then
        assertThat(output).containsEntry("status", "ok");
        
        // Verify retry attempts
        mockServer.getClient().verify(
            request().withPath("/api/data"),
            VerificationTimes.exactly(3)
        );
    }
    
    @Test
    void shouldRespectIdempotency() {
        // Given: Call already executed
        String idempotencyKey = "workflow-123-step-abc";
        WorkflowRestCall existingCall = new WorkflowRestCall();
        existingCall.setIdempotencyKey(idempotencyKey);
        existingCall.setResponseBody("{\"cached\": true}");
        repository.save(existingCall);
        
        WorkflowStep step = WorkflowStep.builder()
            .config(Map.of("idempotencyKey", idempotencyKey))
            .build();
        
        // When
        Map<String, Object> output = executor.execute(mockExecution(), step);
        
        // Then
        assertThat(output).containsEntry("cached", true);
        
        // Verify NO new HTTP call made
        mockServer.getClient().verify(request(), VerificationTimes.exactly(0));
    }
}
```

---

### 5. Metrics

**Prometheus Metrics:**

```java
@Component
public class RestSyncMetrics {
    
    private final Counter restCallsTotal;
    private final Histogram restCallDuration;
    
    public RestSyncMetrics(MeterRegistry registry) {
        this.restCallsTotal = Counter.builder("workflow_rest_calls_total")
            .description("Total REST API calls")
            .tag("endpoint", "")
            .tag("status", "")
            .register(registry);
        
        this.restCallDuration = Histogram.builder("workflow_rest_call_duration_ms")
            .description("REST call duration in milliseconds")
            .tag("endpoint", "")
            .register(registry);
    }
    
    public void recordRestCall(String endpoint, String status, int durationMs) {
        restCallsTotal.increment();
        restCallDuration.record(durationMs);
    }
}
```

**Grafana Dashboard:**
- Panel: REST Calls Total (by endpoint, status)
- Panel: REST Call Latency (p50, p95, p99)
- Panel: Circuit Breaker State
- Panel: Retry Rate

---

## ✅ Acceptance Criteria

1. **Funkční:**
   - [ ] Executor podporuje GET, POST, PUT, PATCH, DELETE
   - [ ] Template substitution funguje pro URL, body, headers
   - [ ] Retry s exponential backoff (configurable maxAttempts)
   - [ ] Circuit breaker ochrana (failure threshold → open state)
   - [ ] Idempotence via correlation ID (duplicate calls → cached response)
   - [ ] Timeout handling (per-request + global)
   - [ ] Output mapping (JSONPath → workflow context)

2. **Performance:**
   - [ ] p99 latency < 5s (excluding external API)
   - [ ] Connection pooling (max 50 per host)
   - [ ] Keep-alive connections

3. **Security:**
   - [ ] Secrets injection (ne plain-text)
   - [ ] TLS 1.2+ enforcement
   - [ ] SSRF protection (whitelist hosts)

4. **Observability:**
   - [ ] Prometheus metrics (calls, latency, errors)
   - [ ] Structured logs (request/response payloads)
   - [ ] OpenTelemetry tracing

5. **Testy:**
   - [ ] Unit testy: TemplateEngine, OpenApiSpecParser
   - [ ] Integration testy: MockServer, Testcontainers
   - [ ] Test retry logic (3 attempts, exponential backoff)
   - [ ] Test circuit breaker (failure threshold → open)
   - [ ] Test idempotency (duplicate calls)

---

## 📝 Implementation Notes

### OpenAPI Spec Parsing

**Option 1: Swagger Parser Library**
```xml
<dependency>
    <groupId>io.swagger.parser.v3</groupId>
    <artifactId>swagger-parser</artifactId>
    <version>2.1.16</version>
</dependency>
```

**Option 2: OpenAPI Generator (codegen)**
```bash
openapi-generator-cli generate \
  -i https://jira.company.com/openapi.json \
  -g java \
  -o generated-client/
```

**Recommendation:** Use **Swagger Parser** for runtime parsing + validation.

### Template Engine

**Option 1: Simple String Replace** (current implementation)
- Pros: Lightweight, no dependencies
- Cons: Limited (no nested paths, no expressions)

**Option 2: Spring Expression Language (SpEL)**
```java
ExpressionParser parser = new SpelExpressionParser();
Expression exp = parser.parseExpression("entity.name");
String value = exp.getValue(context, String.class);
```

**Option 3: Handlebars**
```java
Handlebars handlebars = new Handlebars();
Template template = handlebars.compileInline("Hello {{entity.name}}");
String result = template.apply(context);
```

**Recommendation:** Use **SpEL** for complex expressions, fallback to string replace for simple cases.

### Resilience4j Configuration

**application.yml:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      rest-sync-default:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        sliding-window-size: 10
        
  retry:
    instances:
      rest-sync-default:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
```

---

## 🚀 Future Enhancements

1. **GraphQL Support** (WF13.1)
   - GraphQL query/mutation execution
   - Schema introspection
   - Variable substitution

2. **SOAP/XML Support** (WF13.2)
   - WSDL parsing
   - SOAP envelope generation
   - XML response parsing

3. **OAuth2 Token Management** (WF13.3)
   - Auto token refresh
   - Client credentials flow
   - Authorization code flow

4. **Webhook Callback** (WF13.4)
   - Async REST calls (POST → webhook callback)
   - Correlation ID tracking

---

**Related Stories:**
- W7: Workflow Executor Framework (foundation)
- WF14: KAFKA_COMMAND Executor (async messaging)
- WF15: EXTERNAL_TASK Executor (n8n integration)
- WF17: Workflow Instance Runtime (orchestration)
