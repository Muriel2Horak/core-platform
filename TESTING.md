# Testing Guide - Monitoring BFF

This guide explains how to run tests for the Monitoring BFF without Docker, databases, or external services.

## Overview

The test suite includes:
- **Backend Integration Tests** (WireMock + H2) - No Docker/Redis/Grafana/Keycloak needed
- **Frontend E2E Tests** (Playwright) - Tests against test profile backend

## Prerequisites

- **Backend**: JDK 21, Maven
- **Frontend**: Node 20+, pnpm
- **E2E**: Playwright installed (`pnpm playwright:install`)

## Quick Start

### Backend Integration Tests

```bash
# Run all integration tests (test profile automatically activated)
cd backend
./mvnw test -Dspring.profiles.active=test

# Run specific test class
./mvnw test -Dtest=MonitoringQueryIT -Dspring.profiles.active=test
```

### Frontend E2E Tests

```bash
# 1. Start backend in test mode (terminal 1)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=test

# 2. Run E2E tests (terminal 2)
cd frontend
E2E_BASE_URL=http://localhost:8080 pnpm test:e2e

# Or run in headed mode (see browser)
E2E_BASE_URL=http://localhost:8080 pnpm test:e2e:headed
```

## Test Profile Features

The `test` profile (`application-test.yml`) provides:

### No External Dependencies
- ✅ **No Keycloak**: Mock JWT via `X-Test-Auth` header
- ✅ **No Grafana**: WireMock on random port  
- ✅ **No Redis**: Caffeine in-memory cache
- ✅ **No PostgreSQL**: H2 in-memory (PostgreSQL mode)
- ✅ **No Kafka**: Streaming disabled

### Mock Authentication

Use `X-Test-Auth` header instead of real JWT:

```bash
X-Test-Auth: tenant=TENANT_A;roles=ROLE_USER,ROLE_REPORT
```

Format: `tenant=<ID>;roles=<COMMA_SEPARATED_ROLES>;subject=<USERNAME>`

### Test Configuration Highlights

```yaml
server:
  port: 0  # Random port for parallel tests

reporting:
  cache:
    provider: caffeine  # No Redis
  rate-limit:
    per-tenant-per-min: 9999  # Disabled for tests

monitoring:
  grafana:
    base-url: http://localhost:${WIREMOCK_PORT:0}  # Set by WireMock
```

## Backend Integration Tests

### Test Coverage

#### MonitoringQueryIT
- ✅ Valid DSL query → 200 OK
- ✅ BFF adds `Authorization: Bearer <SAT>` and `X-Grafana-Org-Id`
- ✅ Client forged headers are removed (security)
- ✅ Invalid DSL (missing timeRange) → 400 Bad Request
- ✅ Grafana 500 error → 502 + Circuit Breaker opens
- ✅ Grafana timeout → 503 Service Unavailable
- ✅ Cache HIT/MISS verification
- ✅ Rate limit → 429 with Retry-After

#### SecurityIsolationIT
- ✅ Tenant A can only access Tenant A data
- ✅ Forged `X-Grafana-Org-Id` header is ignored
- ✅ Missing tenant → 403 Forbidden

### Running Specific Tests

```bash
# All monitoring BFF tests
./mvnw test -Dtest="cz.muriel.core.monitoring.bff.*IT"

# Specific test method
./mvnw test -Dtest=MonitoringQueryIT#validDSL_shouldReturn200_andAddCorrectHeaders
```

## Frontend E2E Tests

### Test Coverage

#### reports.spec.ts
- ✅ Page loads and renders charts/tables
- ✅ Change time range → new API call → data updates
- ✅ Apply filters → filtered results
- ✅ Rate limit (429) → toast notification
- ✅ Unauthorized access → redirect to login

### Running E2E Tests

```bash
# Install Playwright browsers (first time only)
pnpm playwright:install

# Run all E2E tests (headless)
E2E_BASE_URL=http://localhost:8080 pnpm test:e2e

# Run in headed mode (visible browser)
E2E_BASE_URL=http://localhost:8080 pnpm test:e2e:headed

# Run specific test file
E2E_BASE_URL=http://localhost:8080 pnpm playwright test tests/e2e/reports.spec.ts

# Debug mode
E2E_BASE_URL=http://localhost:8080 pnpm playwright test --debug
```

### E2E Test Structure

```typescript
test.beforeEach(async ({ page }) => {
  // Set mock auth header
  await page.setExtraHTTPHeaders({
    'X-Test-Auth': 'tenant=TENANT_A;roles=ROLE_USER'
  });
});

test('should load reports and display data', async ({ page }) => {
  await page.goto('/reports');
  
  // Wait for API call
  await page.waitForResponse(resp => 
    resp.url().includes('/api/monitoring/ds/query') && resp.status() === 200
  );
  
  // Verify UI updated
  await expect(page.locator('.chart-container')).toBeVisible();
});
```

## CI/CD Integration

### GitHub Actions Workflow

Tests run in CI without Docker:

```yaml
# .github/workflows/tests-monitoring-bff.yml
jobs:
  backend-integration:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - run: cd backend && ./mvnw test -Dspring.profiles.active=test
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: surefire-reports
          path: backend/target/surefire-reports/

  frontend-e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: cd frontend && pnpm install
      - run: cd frontend && pnpm playwright:install --with-deps
      - name: Start backend
        run: cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=test &
      - name: Wait for backend
        run: npx wait-on http://127.0.0.1:8080/actuator/health
      - run: cd frontend && E2E_BASE_URL=http://127.0.0.1:8080 pnpm test:e2e
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: frontend/playwright-report/
```

## Troubleshooting

### Backend Tests Fail to Start

**Problem**: `Address already in use`  
**Solution**: Use `server.port=0` for random port (already in application-test.yml)

**Problem**: `Could not find or load main class`  
**Solution**: Run `./mvnw clean install` first

### WireMock Port Issues

**Problem**: Tests can't connect to WireMock  
**Solution**: Check logs for `WireMock server started on port XXXXX`, verify `WIREMOCK_PORT` system property is set

### E2E Tests Timeout

**Problem**: `page.goto()` times out  
**Solution**: Ensure backend is running on correct port:
```bash
# Check backend is up
curl http://localhost:8080/actuator/health

# Check logs for actual port
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=test | grep "Tomcat started"
```

### Authentication Failures

**Problem**: `403 Forbidden` in tests  
**Solution**: Verify `X-Test-Auth` header format:
```
X-Test-Auth: tenant=TENANT_A;roles=ROLE_USER,ROLE_REPORT
```

### Cache Not Working

**Problem**: Cache always MISS  
**Solution**: Verify Caffeine is used (not Redis) in test profile:
```bash
# Check logs for
[main] INFO  c.m.c.m.b.c.MonitoringBffConfig - Using Caffeine cache
```

## Metrics and Observability

### Check Metrics in Tests

```bash
# Circuit breaker state
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state

# Cache stats
curl http://localhost:8080/actuator/metrics/cache.gets

# Custom metrics
curl http://localhost:8080/actuator/metrics/report_query_latency
```

### Verify Logs Don't Leak Secrets

Tests assert that Authorization headers are redacted in logs:

```java
@Test
void logs_shouldNotContainTokens() {
  // ... make request with token
  // Assert logs don't contain "Bearer" or token value
}
```

## Best Practices

### 1. Test Isolation
- Each test resets WireMock stubs (`wireMockServer.resetAll()`)
- Use `@DirtiesContext` sparingly (slow)
- Prefer stateless tests

### 2. Mock Realistic Responses
```java
wireMockServer.stubFor(post("/api/ds/query")
  .willReturn(aResponse()
    .withStatus(200)
    .withHeader("Content-Type", "application/json")
    .withBodyFile("grafana-query-response.json")));  // Use fixture files
```

### 3. Test Negative Cases
- Invalid inputs
- Network failures
- Circuit breaker scenarios
- Rate limiting

### 4. Fast Feedback
- Integration tests: ~10s total
- E2E tests: ~30s per spec
- Run affected tests only during development

## Development Workflow

```bash
# 1. Write test (RED)
vim backend/src/test/java/.../MyNewFeatureIT.java

# 2. Run test to see failure
./mvnw test -Dtest=MyNewFeatureIT -Dspring.profiles.active=test

# 3. Implement feature (GREEN)
vim backend/src/main/java/.../MyNewFeature.java

# 4. Run test to verify
./mvnw test -Dtest=MyNewFeatureIT -Dspring.profiles.active=test

# 5. Refactor if needed
# 6. Run all tests before commit
./mvnw test -Dspring.profiles.active=test
```

## Next Steps

- Add performance tests with WireMock delays
- Add contract tests (Pact/Spring Cloud Contract)
- Add security tests (OWASP ZAP)
- Add load tests (Gatling/k6)

## Support

For issues or questions:
1. Check this guide
2. Review test logs in `backend/target/surefire-reports/`
3. Review Playwright reports in `frontend/playwright-report/`
4. Ask in team chat #testing
