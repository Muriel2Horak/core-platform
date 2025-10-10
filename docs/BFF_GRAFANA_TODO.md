# BFF + Grafana Scenes - TODO List

## ✅ Completed (5 commits)

- [x] Backend BFF module (MonitoringProxyController, MonitoringProxyService, TenantOrgService)
- [x] Rate limiting (Bucket4j, 100 req/min per user)
- [x] Audit logging (structured JSON logs)
- [x] Grafana OIDC configuration (admin realm, role mapping)
- [x] Nginx admin server block (`admin.<DOMAIN>/monitoring`)
- [x] Frontend Grafana Scenes (Reports.jsx, GrafanaSceneDataSource)
- [x] TypeScript CLI tools (grafana-org-admin.ts)
- [x] Documentation (MONITORING_BFF_ARCHITECTURE.md, runbook)
- [x] Test stubs (TenantOrgServiceImplTest, MonitoringProxyServiceTest)

**Branch**: `feature/streaming-dashboard`  
**Commits**: 
- 7831b63: Backend BFF proxy
- 154cf06: Grafana OIDC + Nginx
- 873f8ad: Frontend Scenes
- 3165f92: CLI tools
- c0c7e93: Documentation + test stubs

---

## 🔄 In Progress

### 1. Integration Tests (Priority: High)

**Setup WireMock for BFF tests**:
```java
// backend/src/test/java/cz/muriel/core/monitoring/bff/service/MonitoringProxyServiceIntegrationTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 8089)
class MonitoringProxyServiceIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void forwardQuery_shouldProxyToGrafana() {
        // Stub Grafana API
        stubFor(post(urlEqualTo("/api/ds/query"))
            .willReturn(okJson("{\"results\":{}}")));
        
        // Test BFF endpoint
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("test-user", "password")
            .postForEntity("/api/monitoring/ds/query", requestBody, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify headers
        verify(postRequestedFor(urlEqualTo("/api/ds/query"))
            .withHeader("Authorization", matching("Bearer glsa_.*"))
            .withHeader("X-Grafana-Org-Id", equalTo("2")));
    }
}
```

**Dependencies needed**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-contract-stub-runner</artifactId>
    <scope>test</scope>
</dependency>
```

**Estimated effort**: 4 hours

---

### 2. E2E Tests (Priority: High)

**Playwright test for /reports page**:
```typescript
// tests/e2e/reports.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Reports Page (Grafana Scenes)', () => {
    test.beforeEach(async ({ page }) => {
        // Login
        await page.goto('https://app.core-platform.local/login');
        await page.fill('input[name="username"]', 'test-user@test-tenant.com');
        await page.fill('input[name="password"]', 'password');
        await page.click('button[type="submit"]');
    });

    test('should load Grafana Scenes panels', async ({ page }) => {
        await page.goto('/reports');
        
        // Wait for Scenes to mount
        await expect(page.locator('.grafana-scene-container')).toBeVisible();
        
        // Check tabs
        await expect(page.locator('text=Application')).toBeVisible();
        await expect(page.locator('text=Infrastructure')).toBeVisible();
        await expect(page.locator('text=Logs')).toBeVisible();
        
        // Check data loaded
        const panel = page.locator('.grafana-panel').first();
        await expect(panel).toContainText(/\d+/); // Any number (metric value)
    });

    test('should execute BFF query on time range change', async ({ page }) => {
        await page.goto('/reports');
        
        // Intercept BFF request
        await page.route('/api/monitoring/ds/query', route => {
            expect(route.request().method()).toBe('POST');
            expect(route.request().headers()['authorization']).toBeTruthy();
            route.fulfill({ 
                status: 200, 
                body: JSON.stringify({ results: {} }) 
            });
        });
        
        // Change time range
        await page.click('button[aria-label="Time range"]');
        await page.click('text=Last 1 hour');
        
        // Verify request sent
        await page.waitForRequest('/api/monitoring/ds/query');
    });
});
```

**Estimated effort**: 6 hours

---

## 📋 Pending Tasks

### 3. Production Hardening (Priority: Medium)

- [ ] **Vault integration for SAT storage**
  ```java
  @Service
  @Profile("production")
  public class VaultTenantOrgService implements TenantOrgService {
      @Autowired private VaultTemplate vaultTemplate;
      
      public TenantBinding resolve(Jwt jwt) {
          String tenantId = extractTenantId(jwt);
          VaultResponse response = vaultTemplate.read("secret/grafana/orgs/" + tenantId);
          // ...
      }
  }
  ```
  **Effort**: 3 hours

- [ ] **Circuit breaker for WebClient**
  ```java
  @Bean
  public WebClient grafanaClient() {
      return WebClient.builder()
          .filter(ExchangeFilterFunctions.circuitBreaker(
              CircuitBreakerConfig.custom()
                  .failureRateThreshold(50)
                  .waitDurationInOpenState(Duration.ofSeconds(30))
                  .build()
          ))
          .build();
  }
  ```
  **Effort**: 2 hours

- [ ] **Distributed rate limiting (Redis)**
  ```java
  @Bean
  public ProxyManager<String> proxyManager(RedissonClient redisson) {
      return Bucket4j.extension(RedissonBucket4j.class)
          .proxyManagerForRedisson(redisson);
  }
  ```
  **Effort**: 3 hours

- [ ] **CORS configuration**
  ```java
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
      CorsConfiguration config = new CorsConfiguration();
      config.setAllowedOrigins(List.of("https://app.core-platform.local"));
      config.setAllowedMethods(List.of("GET", "POST"));
      config.setAllowCredentials(true);
      
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/api/monitoring/**", config);
      return source;
  }
  ```
  **Effort**: 1 hour

- [ ] **Structured logging (JSON format)**
  ```xml
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="net.logstash.logback.encoder.LogstashEncoder">
          <includeMdc>true</includeMdc>
      </encoder>
  </appender>
  ```
  **Effort**: 1 hour

- [ ] **Secrets scanning in CI**
  ```yaml
  # .github/workflows/security.yml
  - name: Scan for secrets
    uses: trufflesecurity/trufflehog@main
    with:
      path: ./
      base: ${{ github.event.repository.default_branch }}
      head: HEAD
  ```
  **Effort**: 1 hour

**Total effort**: 11 hours

---

### 4. Automated Token Rotation (Priority: Low)

- [ ] **Cron job for monthly rotation**
  ```bash
  # k8s/cronjob-rotate-grafana-tokens.yaml
  apiVersion: batch/v1
  kind: CronJob
  metadata:
    name: rotate-grafana-tokens
  spec:
    schedule: "0 0 1 * *"  # 1st day of month
    jobTemplate:
      spec:
        template:
          spec:
            containers:
            - name: rotator
              image: core-platform/tools:latest
              command:
                - /bin/sh
                - -c
                - |
                  for TENANT in core-platform test-tenant acme-corp; do
                    npx tsx /tools/grafana-org-admin.ts rotate-sat --org $ORG_ID --name $TENANT-viewer
                    # Update Vault
                    vault kv put secret/grafana/orgs/$TENANT serviceAccountToken=$NEW_TOKEN
                  done
                  # Restart backend
                  kubectl rollout restart deployment/backend
  ```
  **Effort**: 4 hours

- [ ] **Notification on rotation**
  ```java
  @Component
  public class TokenRotationListener {
      @Autowired private SlackClient slack;
      
      @EventListener
      public void onTokenRotated(TokenRotatedEvent event) {
          slack.sendMessage("#platform-alerts", 
              String.format("🔄 Grafana token rotated for tenant: %s (org: %d)", 
                  event.getTenantId(), event.getOrgId())
          );
      }
  }
  ```
  **Effort**: 2 hours

**Total effort**: 6 hours

---

### 5. Monitoring & Alerting (Priority: Medium)

- [ ] **Grafana dashboard for BFF health**
  ```promql
  # BFF request rate
  sum(rate(http_server_requests_seconds_count{uri=~"/api/monitoring.*"}[5m])) by (uri)
  
  # BFF error rate
  sum(rate(http_server_requests_seconds_count{uri=~"/api/monitoring.*",status=~"5.."}[5m])) 
    / sum(rate(http_server_requests_seconds_count{uri=~"/api/monitoring.*"}[5m]))
  
  # P95 latency
  histogram_quantile(0.95, 
    sum(rate(http_server_requests_seconds_bucket{uri=~"/api/monitoring.*"}[5m])) by (le, uri)
  )
  
  # Rate limit hits
  sum(rate(monitoring_bff_rate_limit_exceeded_total[5m])) by (tenant)
  ```
  **Effort**: 3 hours

- [ ] **Prometheus alerts**
  ```yaml
  # alerts/monitoring-bff.yml
  groups:
    - name: monitoring_bff
      rules:
        - alert: BFFHighErrorRate
          expr: |
            sum(rate(http_server_requests_seconds_count{uri=~"/api/monitoring.*",status=~"5.."}[5m])) 
            / sum(rate(http_server_requests_seconds_count{uri=~"/api/monitoring.*"}[5m])) > 0.05
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "BFF error rate > 5%"
            runbook: "https://docs.core-platform.local/runbooks/monitoring#bff-high-error-rate"
        
        - alert: BFFSlowQueries
          expr: |
            histogram_quantile(0.95, 
              sum(rate(http_server_requests_seconds_bucket{uri="/api/monitoring/ds/query"}[5m])) by (le)
            ) > 5
          for: 10m
          labels:
            severity: warning
          annotations:
            summary: "BFF P95 latency > 5s"
  ```
  **Effort**: 2 hours

- [ ] **PagerDuty integration**
  ```yaml
  # alertmanager.yml
  receivers:
    - name: 'pagerduty-monitoring'
      pagerduty_configs:
        - service_key: <pagerduty-integration-key>
          severity: '{{ .CommonLabels.severity }}'
          description: '{{ .CommonAnnotations.summary }}'
          details:
            runbook: '{{ .CommonAnnotations.runbook }}'
  ```
  **Effort**: 2 hours

**Total effort**: 7 hours

---

### 6. Performance Optimizations (Priority: Low)

- [ ] **Connection pooling tuning**
  ```java
  // MonitoringBffConfig.java
  @Bean
  public WebClient grafanaClient() {
      ConnectionProvider provider = ConnectionProvider.builder("grafana-pool")
          .maxConnections(100)
          .maxIdleTime(Duration.ofSeconds(20))
          .maxLifeTime(Duration.ofMinutes(5))
          .pendingAcquireTimeout(Duration.ofSeconds(5))
          .evictInBackground(Duration.ofSeconds(30))
          .build();
      
      return WebClient.builder()
          .clientConnector(new ReactorClientHttpConnector(
              HttpClient.create(provider)
          ))
          .build();
  }
  ```
  **Effort**: 2 hours

- [ ] **Query result caching**
  ```java
  @Cacheable(
      value = "grafana-queries", 
      key = "#jwt.subject + ':' + #body.hashCode()",
      unless = "#result == null"
  )
  public ResponseEntity<String> forwardQuery(Jwt jwt, Map<String, Object> body) {
      // ...
  }
  
  // Cache config
  @Bean
  public CacheManager cacheManager() {
      return new CaffeineCacheManager("grafana-queries");
  }
  
  @Bean
  public Caffeine<Object, Object> caffeineConfig() {
      return Caffeine.newBuilder()
          .expireAfterWrite(30, TimeUnit.SECONDS)  // Short TTL for real-time data
          .maximumSize(1000);
  }
  ```
  **Effort**: 3 hours

- [ ] **Prometheus recording rules**
  ```yaml
  # prometheus/rules/monitoring.yml
  groups:
    - name: bff_recording_rules
      interval: 30s
      rules:
        - record: monitoring:bff:request_rate:5m
          expr: sum(rate(http_server_requests_seconds_count{uri=~"/api/monitoring.*"}[5m])) by (uri)
        
        - record: monitoring:bff:error_rate:5m
          expr: |
            sum(rate(http_server_requests_seconds_count{uri=~"/api/monitoring.*",status=~"5.."}[5m])) 
            / sum(rate(http_server_requests_seconds_count{uri=~"/api/monitoring.*"}[5m]))
  ```
  **Effort**: 2 hours

**Total effort**: 7 hours

---

### 7. Documentation Updates (Priority: Medium)

- [ ] **API documentation (OpenAPI/Swagger)**
  ```java
  @RestController
  @RequestMapping("/api/monitoring")
  @Tag(name = "Monitoring BFF", description = "Backend-for-Frontend proxy for Grafana")
  public class MonitoringProxyController {
      
      @Operation(
          summary = "Query datasource",
          description = "Proxies datasource queries to Grafana with tenant isolation"
      )
      @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Query successful"),
          @ApiResponse(responseCode = "401", description = "Unauthorized (invalid JWT)"),
          @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
      })
      @PostMapping("/ds/query")
      public ResponseEntity<String> queryDatasource(...) {
          // ...
      }
  }
  ```
  **Effort**: 3 hours

- [ ] **Frontend Scenes usage guide**
  ```markdown
  # docs/frontend/GRAFANA_SCENES_GUIDE.md
  
  ## Adding a New Panel
  
  ```javascript
  import { SceneQueryRunner, VizPanel } from '@grafana/scenes';
  
  const newPanel = new VizPanel({
    title: 'CPU Usage',
    pluginId: 'timeseries',
    $data: new SceneQueryRunner({
      datasource: new GrafanaSceneDataSource(),
      queries: [
        {
          refId: 'A',
          expr: 'rate(process_cpu_seconds_total[5m])',
          legendFormat: '{{instance}}'
        }
      ]
    })
  });
  ```
  **Effort**: 2 hours

- [ ] **Tenant onboarding guide**
  ```markdown
  # docs/TENANT_ONBOARDING.md
  
  ## Step-by-Step: Add New Tenant to Monitoring
  
  1. Create Grafana org:
     ```bash
     npx tsx tools/grafana-org-admin.ts create-org --name new-tenant
     # Output: orgId=4
     ```
  
  2. Create service account:
     ```bash
     npx tsx tools/grafana-org-admin.ts create-sa --org 4 --name new-tenant-viewer
     # Save token to Vault
     ```
  
  3. Provision datasources:
     ```bash
     npx tsx tools/grafana-org-admin.ts provision-ds --org 4 --tenant new-tenant
     docker compose restart grafana
     ```
  
  4. Update backend config (see MONITORING_BFF_ARCHITECTURE.md#deployment-guide)
  ```
  **Effort**: 2 hours

**Total effort**: 7 hours

---

## 📊 Effort Summary

| Category | Tasks | Estimated Hours |
|----------|-------|-----------------|
| Integration Tests | WireMock setup | 4h |
| E2E Tests | Playwright tests | 6h |
| Production Hardening | Vault, circuit breaker, rate limiting, CORS, logging, secrets scan | 11h |
| Token Rotation | Automated cron job, notifications | 6h |
| Monitoring & Alerting | Grafana dashboard, Prometheus alerts, PagerDuty | 7h |
| Performance | Connection pooling, caching, recording rules | 7h |
| Documentation | API docs, Scenes guide, onboarding guide | 7h |
| **TOTAL** | | **48 hours** (~6 days) |

---

## 🎯 Next Steps (Recommended Priority)

### Week 1: Testing & Hardening
1. ✅ Integration tests (4h)
2. ✅ E2E tests (6h)
3. ✅ CORS + secrets scanning (2h)
4. ✅ Vault integration (3h)

### Week 2: Monitoring & Docs
5. ✅ Grafana BFF dashboard (3h)
6. ✅ Prometheus alerts (2h)
7. ✅ API documentation (3h)
8. ✅ Tenant onboarding guide (2h)

### Week 3: Performance & Automation
9. ✅ Circuit breaker (2h)
10. ✅ Query caching (3h)
11. ✅ Automated token rotation (6h)
12. ✅ PagerDuty integration (2h)

---

## 🔗 References

- [MONITORING_BFF_ARCHITECTURE.md](./docs/MONITORING_BFF_ARCHITECTURE.md) - Complete architecture guide
- [BFF_GRAFANA_SCENES_IMPLEMENTATION.md](./docs/BFF_GRAFANA_SCENES_IMPLEMENTATION.md) - Implementation summary
- [MONITORING_INCIDENT_RUNBOOK.md](./docs/runbooks/MONITORING_INCIDENT_RUNBOOK.md) - Incident response procedures
- [Grafana Scenes Docs](https://grafana.com/docs/grafana/latest/developers/scenes/) - Official documentation

---

**Last Updated**: 2025-10-10  
**Status**: ✅ Core implementation complete, 48h of tasks remaining
