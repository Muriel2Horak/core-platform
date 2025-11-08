# N8N6: Backend-for-Frontend (BFF) API - External Task Bridge

**Typ:** TASK  
**Epic:** EPIC-011 (n8n External Orchestration Layer)  
**Fase:** Phase 3 (n8n Deployment)  
**Priorita:** CRITICAL  
**Effort:** 800 LOC, 3 dny  
**Dependencies:** WF15 (EXTERNAL_TASK Executor), N8N1, N8N2 (Keycloak SSO)  
**Status:** ⏳ TODO

---

## 🎯 Cíl

Vytvořit **BFF API** jako integration bridge mezi Core Platform a n8n:
- Spring Boot controller pro External Task endpoints
- JWT authentication (n8n volá s Keycloak token)
- Tenant isolation (multi-tenancy support)
- Audit logging (track všechny n8n calls)

---

## 📋 Požadavky

### Funkční Požadavky

1. **REST API Endpoints**
   - `GET /api/n8n/external-tasks/poll` - Poll pending tasks
   - `POST /api/n8n/external-tasks/{taskId}/complete` - Complete task
   - `POST /api/n8n/external-tasks/{taskId}/fail` - Fail task
   - `POST /api/n8n/external-tasks/{taskId}/heartbeat` - Worker heartbeat

2. **Security**
   - JWT authentication (Keycloak issuer validation)
   - Tenant filtering (pouze tasks z tenant context)
   - Audit log všech API calls

3. **Integration**
   - Deleguje na `ExternalTaskService` (WF15)
   - CORS config pro n8n webhook callbacks
   - Rate limiting (prevent abuse)

---

## 🔧 Implementace

### 1. BFF Controller

**File:** `backend/src/main/java/cz/muriel/core/n8n/N8nExternalTaskBffController.java`

```java
package cz.muriel.core.n8n;

import cz.muriel.core.workflow.executor.external.ExternalTaskService;
import cz.muriel.core.workflow.executor.external.model.ExternalTask;
import cz.muriel.core.workflow.executor.external.model.ExternalTaskCompleteRequest;
import cz.muriel.core.workflow.executor.external.model.ExternalTaskFailRequest;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/n8n/external-tasks")
@RequiredArgsConstructor
public class N8nExternalTaskBffController {
    
    private final ExternalTaskService externalTaskService;
    private final N8nAuditLogger auditLogger;
    
    /**
     * Poll for pending external tasks (long-polling).
     * n8n worker calls this repeatedly to get work.
     */
    @GetMapping("/poll")
    @Timed("n8n.external_tasks.poll")
    public ResponseEntity<ExternalTask> pollTasks(
            @RequestParam(required = false) String workerId,
            @RequestParam(defaultValue = "30000") Long timeoutMs,
            Authentication authentication
    ) {
        String tenantId = extractTenantId(authentication);
        log.debug("n8n poll request: workerId={}, tenant={}", workerId, tenantId);
        
        // Long-polling: wait up to 30s for task
        ExternalTask task = externalTaskService.pollTask(workerId, tenantId, timeoutMs);
        
        if (task != null) {
            auditLogger.logPoll(workerId, task.getId(), tenantId);
            return ResponseEntity.ok(task);
        } else {
            return ResponseEntity.noContent().build();  // 204 No Content
        }
    }
    
    /**
     * Complete task with result.
     */
    @PostMapping("/{taskId}/complete")
    @Timed("n8n.external_tasks.complete")
    public ResponseEntity<Void> completeTask(
            @PathVariable UUID taskId,
            @RequestBody ExternalTaskCompleteRequest request,
            Authentication authentication
    ) {
        String tenantId = extractTenantId(authentication);
        log.info("n8n completing task: taskId={}, tenant={}", taskId, tenantId);
        
        externalTaskService.completeTask(taskId, request.getResult(), tenantId);
        auditLogger.logComplete(taskId, tenantId, request.getResult());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Fail task with error.
     */
    @PostMapping("/{taskId}/fail")
    @Timed("n8n.external_tasks.fail")
    public ResponseEntity<Void> failTask(
            @PathVariable UUID taskId,
            @RequestBody ExternalTaskFailRequest request,
            Authentication authentication
    ) {
        String tenantId = extractTenantId(authentication);
        log.warn("n8n failing task: taskId={}, error={}, tenant={}", taskId, request.getError(), tenantId);
        
        externalTaskService.failTask(taskId, request.getError(), tenantId);
        auditLogger.logFail(taskId, tenantId, request.getError());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Worker heartbeat (prevent timeout).
     */
    @PostMapping("/{taskId}/heartbeat")
    @Timed("n8n.external_tasks.heartbeat")
    public ResponseEntity<Void> heartbeat(
            @PathVariable UUID taskId,
            @RequestParam String workerId,
            Authentication authentication
    ) {
        String tenantId = extractTenantId(authentication);
        log.trace("n8n heartbeat: taskId={}, workerId={}, tenant={}", taskId, workerId, tenantId);
        
        externalTaskService.updateHeartbeat(taskId, workerId);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Extract tenant ID from JWT claims.
     */
    private String extractTenantId(Authentication authentication) {
        if (authentication == null) {
            return "default";  // Fallback pro dev
        }
        // JWT claim "tenant_id" injected by Keycloak mapper
        return (String) authentication.getPrincipal();
    }
}
```

---

### 2. Audit Logger

**File:** `backend/src/main/java/cz/muriel/core/n8n/N8nAuditLogger.java`

```java
package cz.muriel.core.n8n;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class N8nAuditLogger {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void logPoll(String workerId, UUID taskId, String tenantId) {
        Map<String, Object> event = Map.of(
            "event", "n8n.task.poll",
            "workerId", workerId,
            "taskId", taskId,
            "tenantId", tenantId,
            "timestamp", Instant.now()
        );
        kafkaTemplate.send("audit-events", event);
        log.info("Audit: n8n poll - workerId={}, taskId={}, tenant={}", workerId, taskId, tenantId);
    }
    
    public void logComplete(UUID taskId, String tenantId, Map<String, Object> result) {
        Map<String, Object> event = Map.of(
            "event", "n8n.task.complete",
            "taskId", taskId,
            "tenantId", tenantId,
            "result", result,
            "timestamp", Instant.now()
        );
        kafkaTemplate.send("audit-events", event);
        log.info("Audit: n8n complete - taskId={}, tenant={}", taskId, tenantId);
    }
    
    public void logFail(UUID taskId, String tenantId, String error) {
        Map<String, Object> event = Map.of(
            "event", "n8n.task.fail",
            "taskId", taskId,
            "tenantId", tenantId,
            "error", error,
            "timestamp", Instant.now()
        );
        kafkaTemplate.send("audit-events", event);
        log.warn("Audit: n8n fail - taskId={}, tenant={}, error={}", taskId, tenantId, error);
    }
}
```

---

### 3. Security Configuration

**File:** `backend/src/main/java/cz/muriel/core/n8n/N8nSecurityConfig.java`

```java
package cz.muriel.core.n8n;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class N8nSecurityConfig {
    
    @Bean
    public SecurityFilterChain n8nApiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/n8n/**")
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()  // Require JWT authentication
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .issuerUri("https://admin.core-platform.local/realms/admin")
                )
            )
            .cors(cors -> cors.configurationSource(n8nCorsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable);  // n8n POST requests (CSRF not needed with JWT)
        
        return http.build();
    }
    
    /**
     * CORS config: allow n8n webhook callbacks from n8n container.
     */
    @Bean
    public CorsConfigurationSource n8nCorsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "https://admin.core-platform.local",  // n8n UI
            "http://n8n:5678"  // n8n internal
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/n8n/**", config);
        return source;
    }
}
```

---

### 4. Rate Limiting

**File:** `backend/src/main/java/cz/muriel/core/n8n/N8nRateLimiter.java`

```java
package cz.muriel.core.n8n;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class N8nRateLimiter implements HandlerInterceptor {
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
        String workerId = request.getParameter("workerId");
        if (workerId == null) {
            workerId = "anonymous";
        }
        
        Bucket bucket = buckets.computeIfAbsent(workerId, this::createBucket);
        
        if (bucket.tryConsume(1)) {
            return true;  // Allow request
        } else {
            log.warn("Rate limit exceeded for workerId={}", workerId);
            response.setStatus(429);  // Too Many Requests
            return false;
        }
    }
    
    private Bucket createBucket(String workerId) {
        // 100 requests per minute per worker
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
```

---

### 5. Integration Test

**File:** `backend/src/test/java/cz/muriel/core/n8n/N8nExternalTaskBffControllerTest.java`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class N8nExternalTaskBffControllerTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldPollExternalTask() {
        // Given: External task created
        // (setup in @BeforeEach)
        
        // When: n8n polls
        ResponseEntity<ExternalTask> response = restTemplate
            .withBasicAuth("n8n-worker", "password")
            .getForEntity("/api/n8n/external-tasks/poll?workerId=worker-1", ExternalTask.class);
        
        // Then: Task returned
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getType()).isEqualTo("EXTERNAL_TASK");
    }
    
    @Test
    void shouldCompleteTask() {
        // Given: Polled task
        UUID taskId = UUID.randomUUID();
        
        // When: n8n completes
        ExternalTaskCompleteRequest request = new ExternalTaskCompleteRequest(
            Map.of("jiraTicketKey", "PROJ-123")
        );
        ResponseEntity<Void> response = restTemplate
            .withBasicAuth("n8n-worker", "password")
            .postForEntity("/api/n8n/external-tasks/" + taskId + "/complete", request, Void.class);
        
        // Then: 200 OK
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

---

## ✅ Acceptance Criteria

1. **API Endpoints:**
   - [ ] `GET /api/n8n/external-tasks/poll` - returns task nebo 204 No Content
   - [ ] `POST /api/n8n/external-tasks/{id}/complete` - completes task
   - [ ] `POST /api/n8n/external-tasks/{id}/fail` - fails task
   - [ ] `POST /api/n8n/external-tasks/{id}/heartbeat` - updates timestamp

2. **Security:**
   - [ ] JWT authentication required (Keycloak issuer validation)
   - [ ] Tenant ID extracted from JWT claim
   - [ ] CORS allows n8n origin

3. **Rate Limiting:**
   - [ ] 100 requests/minute per worker ID
   - [ ] Returns 429 Too Many Requests when exceeded

4. **Audit:**
   - [ ] All API calls logged to Kafka `audit-events` topic
   - [ ] Logs include: event, workerId, taskId, tenantId, timestamp

5. **Integration Tests:**
   - [ ] Poll test passes
   - [ ] Complete test passes
   - [ ] Fail test passes
   - [ ] Rate limit test passes

6. **Metrics:**
   - [ ] Prometheus metrics: `n8n_external_tasks_poll_total`, `n8n_external_tasks_complete_total`, `n8n_external_tasks_fail_total`
   - [ ] Latency histogram: `n8n_external_tasks_poll_duration_ms`

---

**Related Stories:**
- WF15: EXTERNAL_TASK Executor (backend service layer)
- N8N2: Keycloak SSO (JWT authentication)
- N8N4: Workflow Templates (n8n consumes this API)
