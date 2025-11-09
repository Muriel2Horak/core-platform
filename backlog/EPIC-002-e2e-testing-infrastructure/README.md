# EPIC-002: Testing Infrastructure & Framework

**Status:** 🔵 **IN PROGRESS**  
**Priority:** P0 (Critical Foundation)  
**Effort:** ~60 hodin  
**LOC:** ~4,500 řádků (framework + mocks + test data + config + documentation)

---

## 🎯 Vision

**Standardizovaný testing framework** pro všechny typy testů (E2E, Unit, Integration, Backend) s automatickou evidencí pokrytí User Stories a integrací do CI/CD pipeline.

### Business Goals
- **Test-First Culture**: Každý feature má E2E + Unit + Backend testy
- **Traceability**: Každý test mapuje na User Story (CORE-XXX)
- **Automation**: Testy běží automaticky při build (make test-*)
- **Coverage Tracking**: Víme jaké US jsou otestované a které ne
- **Quality Gates**: Build failuje pokud testy selžou nebo coverage klesne

### Principles
```
Každá User Story vyžaduje:
✅ E2E test (Playwright) - end-to-end flow
✅ Unit testy (JUnit/Vitest) - business logika
✅ Backend test (REST Assured) - API contract
✅ Test ID tag - mapování na User Story (e.g., @CORE-123)
```

---

## 🏗️ Architecture

```
Testing Framework (Multi-Tier)
│
├── E2E Tests (Playwright)
│   ├── Pre-Deploy Smoke (5-7 min) - Critical paths
│   ├── Post-Deploy Full (20-30 min) - Complete platform
│   ├── Accessibility (Axe-core) - WCAG 2.1 AA
│   └── Test Tags: @CORE-XXX (User Story mapping)
│
├── Unit Tests
│   ├── Frontend (Vitest) - React components, hooks
│   ├── Backend (JUnit) - Service layer, business logic
│   └── Coverage Target: 80% line coverage
│
├── Integration Tests (Backend)
│   ├── REST Assured - API contract testing
│   ├── Testcontainers - DB + Kafka + Redis
│   └── Spring Boot Test - Full context loading
│
└── Test Registry (NEW!)
    ├── Database: test_registry table
    ├── Schema: test_id, user_story_id, type, status, coverage
    ├── API: GET /api/test-registry/{storyId}
    └── UI: Coverage dashboard (Grafana panel)
```

### Test Types

| Type | Framework | Purpose | Run Time | Coverage Target |
|------|-----------|---------|----------|-----------------|
| **E2E Smoke** | Playwright | Fast feedback (PRE-deploy) | 5-7 min | Critical paths |
| **E2E Full** | Playwright | Complete validation (POST-deploy) | 20-30 min | All features |
| **Unit Frontend** | Vitest | React components, hooks | 2-5 min | 80% lines |
| **Unit Backend** | JUnit | Service layer, business logic | 5-10 min | 80% lines |
| **Integration** | REST Assured | API contracts, DB, messaging | 10-15 min | All endpoints |
| **Accessibility** | Axe-core | WCAG 2.1 AA compliance | 3-5 min | All pages |

---

## 📋 Stories Overview

| ID | Story | Status | LOC | Effort | Value |
|----|-------|--------|-----|--------|-------|
| [S8](#s8-test-registry--tracking) | Test Registry & Tracking | 🔵 TODO | ~600 | 8h | Evidence testů |
| [S9](#s9-test-tagging-system) | Test ID Tagging System | 🔵 TODO | ~400 | 6h | Mapování US → testy |
| [S10](#s10-coverage-dashboard) | Coverage Dashboard | 🔵 TODO | ~500 | 8h | Visualizace pokrytí |
| [S11](#s11-cicd-quality-gates) | CI/CD Quality Gates | 🔵 TODO | ~400 | 6h | Automatická validace |
| [S12](#s12-testing-standards--guide) | Testing Standards & Guide | 🔵 TODO | ~600 | 8h | Dokumentace |
| [S13](#s13-mock-services) | Mock Services Integration | 🔵 TODO | ~800 | 12h | Mocking ext. služeb |
| [S14](#s14-test-data-management) | Test Data Management | 🔵 TODO | ~1,200 | 14h | Testovací data + izolace |
| [E2E15](#e2e15-github-actions-cicd-workflows) | **GitHub Actions CI/CD Workflows** | ✅ **DONE** | ~800 | 4h | **Dokumentace workflows** |
| **TOTAL** | | **1/8** | **~5,300** | **~66h** | **Complete test infrastructure** |

---

## 📖 Detailed Stories

### S8: Test Registry & Tracking

> **Evidence:** Databáze všech testů s mapováním na User Stories

**As a** developer  
**I want** centrální registr testů s vazbou na User Stories  
**So that** víme jaké US jsou otestované a které ne

#### Acceptance Criteria

**GIVEN** test s `@CORE-123` tagem  
**WHEN** test proběhne  
**THEN** zaznamená se do test_registry tabulky  
**AND** status (PASS/FAIL) se uloží  
**AND** coverage metriky se aktualizují

#### Implementation

**1. Database Schema**

```sql
-- backend/src/main/resources/db/migration/V999__test_registry.sql
CREATE TABLE test_registry (
    id BIGSERIAL PRIMARY KEY,
    test_id VARCHAR(255) NOT NULL UNIQUE,
    user_story_id VARCHAR(50),
    test_type VARCHAR(50) NOT NULL,
    test_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    last_run_at TIMESTAMP,
    duration_ms INTEGER,
    coverage_lines DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_test_registry_story ON test_registry(user_story_id);
CREATE INDEX idx_test_registry_type ON test_registry(test_type);
CREATE INDEX idx_test_registry_status ON test_registry(status);
```

**2. Backend Model**

```java
@Entity
@Table(name = "test_registry")
public class TestRegistry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String testId;
    
    private String userStoryId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestType testType;
    
    private String testName;
    private String filePath;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestStatus status;
    
    private LocalDateTime lastRunAt;
    private Integer durationMs;
    private BigDecimal coverageLines;
}

public enum TestType {
    E2E_SMOKE, E2E_FULL, UNIT_FE, UNIT_BE, INTEGRATION, A11Y
}

public enum TestStatus {
    PASS, FAIL, SKIP
}
```

**3. REST API**

```java
@RestController
@RequestMapping("/api/test-registry")
public class TestRegistryController {
    
    @GetMapping("/story/{storyId}")
    public List<TestRegistry> getTestsByStory(@PathVariable String storyId) {
        return testRegistryRepository.findByUserStoryId(storyId);
    }
    
    @GetMapping("/coverage")
    public Map<String, Object> getCoverageStats() {
        long totalStories = userStoryRepository.count();
        long testedStories = testRegistryRepository
            .countDistinctUserStoryIdByStatus(TestStatus.PASS);
        
        return Map.of(
            "totalStories", totalStories,
            "testedStories", testedStories,
            "coveragePercent", (testedStories * 100.0 / totalStories)
        );
    }
    
    @PostMapping
    public TestRegistry recordTestRun(@RequestBody TestRunRequest request) {
        return testRegistryService.record(request);
    }
}
```

**4. Playwright Reporter**

```typescript
// e2e/reporters/registry-reporter.ts
import { Reporter, TestCase, TestResult } from '@playwright/test/reporter';

class RegistryReporter implements Reporter {
  async onTestEnd(test: TestCase, result: TestResult) {
    const testId = extractTestId(test);
    const storyId = extractStoryTag(test);
    
    if (testId && storyId) {
      await fetch('http://localhost:8080/api/test-registry', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          testId,
          userStoryId: storyId,
          testType: 'E2E_SMOKE',
          testName: test.title,
          filePath: test.location.file,
          status: result.status === 'passed' ? 'PASS' : 'FAIL',
          durationMs: result.duration
        })
      });
    }
  }
}

function extractStoryTag(test: TestCase): string | null {
  const storyTag = test.tags.find(t => t.startsWith('@CORE-'));
  return storyTag?.substring(1);
}
```

**5. JUnit Listener**

```java
public class TestRegistryListener extends RunListener {
    @Override
    public void testFinished(Description description) {
        UserStory storyAnnotation = description.getAnnotation(UserStory.class);
        
        if (storyAnnotation != null) {
            TestRegistry record = new TestRegistry();
            record.setTestId(description.getMethodName());
            record.setUserStoryId(storyAnnotation.value());
            record.setTestType(TestType.UNIT_BE);
            record.setStatus(TestStatus.PASS);
            testRegistryRepository.save(record);
        }
    }
}

// Usage:
@Test
@UserStory("CORE-123")
public void testUserCreation() {
    // Test implementation
}
```

#### Acceptance Checklist

- [ ] Database schema created (test_registry table)
- [ ] Backend model + repository
- [ ] REST API (/api/test-registry)
- [ ] Playwright reporter (registry-reporter.ts)
- [ ] JUnit listener (@UserStory annotation)
- [ ] Coverage stats endpoint

---

### S9: Test Tagging System

> **Standardizace:** Konvence pro tagování testů pomocí User Story ID

**As a** developer  
**I want** standardní způsob tagování testů  
**So that** každý test mapuje na konkrétní User Story

#### Acceptance Criteria

**GIVEN** nový test pro User Story CORE-123  
**WHEN** vytvořím test  
**THEN** použiju tag `@CORE-123`  
**AND** test se automaticky registruje v test_registry  
**AND** můžu filtrovat testy pro danou US

#### Implementation

**1. Playwright Tagging**

```typescript
// e2e/specs/auth/login.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Login Flow @CORE-123', () => {
  test('should login with valid credentials @E2E-LOGIN-001', async ({ page }) => {
    await page.goto('/');
    await page.getByLabel('Username').fill('test');
    await page.getByLabel('Password').fill('Test.1234');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL(/\/admin/);
  });
});
```

**Tag Format:**
- `@CORE-XXX` - User Story ID (required)
- `@E2E-XXX-NNN` - Test ID (optional)
- `@SMOKE` - Smoke test flag
- `@CRITICAL` - Critical path flag

**2. JUnit Tagging**

```java
@Tag("CORE-123")
@Tag("UNIT_BE")
public class UserServiceTest {
    
    @Test
    @DisplayName("Should create user with valid data")
    @UserStory("CORE-123")
    public void testCreateUser() {
        // Test implementation
    }
}

// Custom annotation:
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserStory {
    String value(); // CORE-XXX
}
```

**3. Pre-commit Hook (Tag Validation)**

```bash
#!/bin/bash
# .git/hooks/pre-commit
git diff --cached --name-only | grep -E '\.(spec\.ts|test\.(ts|tsx|java))$' | while read file; do
  if ! grep -q '@CORE-' "$file"; then
    echo "❌ ERROR: Test file $file missing @CORE-XXX tag"
    exit 1
  fi
done
```

#### Acceptance Checklist

- [ ] Tagging convention documented (@CORE-XXX format)
- [ ] Playwright tag support
- [ ] JUnit @UserStory annotation
- [ ] Vitest tag support
- [ ] Pre-commit hook (tag validation)

---

### S10: Coverage Dashboard

> **Visualizace:** Grafana dashboard pro test coverage metriky

**As a** product owner  
**I want** dashboard zobrazující test coverage  
**So that** vidím jaké User Stories jsou otestované

#### Implementation

```json
{
  "dashboard": {
    "title": "Test Coverage Dashboard",
    "panels": [
      {
        "id": 1,
        "title": "Overall Test Coverage",
        "type": "gauge",
        "targets": [{
          "expr": "SELECT (COUNT(DISTINCT user_story_id) FROM test_registry WHERE status='PASS') / (COUNT(*) FROM user_stories) * 100"
        }],
        "thresholds": [
          {"value": 0, "color": "red"},
          {"value": 70, "color": "yellow"},
          {"value": 90, "color": "green"}
        ]
      },
      {
        "id": 2,
        "title": "Coverage by Test Type",
        "type": "bargauge"
      },
      {
        "id": 3,
        "title": "Untested User Stories",
        "type": "table"
      }
    ]
  }
}
```

#### Acceptance Checklist

- [ ] Grafana dashboard created
- [ ] Coverage gauge panel
- [ ] Coverage by type panel
- [ ] Untested stories table

---

### S11: CI/CD Quality Gates

> **Automation:** Automatická validace testů v CI/CD pipeline

**As a** DevOps engineer  
**I want** quality gates v CI/CD  
**So that** build failuje pokud testy selžou nebo coverage klesne

#### Implementation

```yaml
# .github/workflows/quality-gates.yml
name: Quality Gates
on: [pull_request]

jobs:
  e2e-smoke:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run E2E Smoke Tests
        run: |
          cd e2e
          npm ci
          npx playwright install --with-deps
          npm run test:pre
  
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - name: Backend Unit Tests
        run: |
          cd backend
          ./mvnw test
      - name: Check Coverage
        run: |
          COVERAGE=$(cat frontend/coverage/coverage-summary.json | jq '.total.lines.pct')
          if (( $(echo "$COVERAGE < 80" | bc -l) )); then
            echo "❌ Coverage $COVERAGE% is below 80%"
            exit 1
          fi
```

#### Acceptance Checklist

- [ ] E2E smoke tests in PR checks
- [ ] Unit test execution
- [ ] Coverage threshold validation (80%)
- [ ] Build fails on test failure

---

### S12: Testing Standards & Guide

> **Dokumentace:** Comprehensive testing guide pro vývojáře

#### Implementation

**File:** `docs/testing-guide.md`

```markdown
# Testing Guide - Core Platform

## Test Types

### E2E Tests (Playwright)
- **When**: End-to-end user flows
- **Location**: `e2e/specs/`
- **Tag**: `@CORE-XXX @E2E-XXX-NNN`

### Unit Tests (Frontend)
- **Framework**: Vitest + React Testing Library
- **Location**: `frontend/src/**/*.test.tsx`
- **Coverage**: 80% target

### Unit Tests (Backend)
- **Framework**: JUnit 5
- **Location**: `backend/src/test/`
- **Tag**: `@UserStory("CORE-XXX")`

## Writing Tests

### 1. Tag with User Story
```typescript
test.describe('Login Flow @CORE-123', () => {
  test('should login @E2E-LOGIN-001', async ({ page }) => {
    // Test
  });
});
```

### 2. Follow AAA Pattern
```typescript
test('should create user', async () => {
  // ARRANGE
  const user = { name: 'John' };
  
  // ACT
  await userService.create(user);
  
  // ASSERT
  expect(await userService.find(user.id)).toBeTruthy();
});
```

## Running Tests

```bash
# E2E Smoke
make test-e2e-pre

# Backend Unit
make test-backend

# All tests
make test-all
```
```

#### Acceptance Checklist

- [ ] Testing guide documentation
- [ ] Examples for all test types
- [ ] Tagging conventions
- [ ] Running instructions

---

### S13: Mock Services

> **Integration Testing:** WireMock pro mockování external služeb

**As a** developer  
**I want** mock servery pro Keycloak, MinIO, n8n webhooks  
**So that** integration testy jsou rychlé a spolehlivé (bez závislosti na external services)

#### Implementation

**1. WireMock Setup**

```xml
<!-- backend/pom.xml -->
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock-jre8-standalone</artifactId>
    <version>2.35.0</version>
    <scope>test</scope>
</dependency>
```

**2. Mock Keycloak Token**

```java
// Integration test base class
@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {
    
    @Container
    static WireMockContainer wireMock = new WireMockContainer("wiremock/wiremock:2.35.0");
    
    @BeforeEach
    void setupMocks() {
        wireMock.stubFor(
            post("/realms/admin/protocol/openid-connect/token")
                .willReturn(okJson("""
                    {
                        "access_token": "mock-token-123",
                        "token_type": "Bearer",
                        "expires_in": 300
                    }
                    """))
        );
    }
}
```

**3. Mock n8n Webhooks**

```java
@Test
void shouldTriggerWebhookOnUserCreation() {
    wireMock.stubFor(post("/webhook/user-created").willReturn(ok()));
    
    userService.createUser("testuser", "test@example.com");
    
    wireMock.verify(postRequestedFor(urlEqualTo("/webhook/user-created")));
}
```

#### Acceptance Checklist

- [ ] WireMock Testcontainer setup
- [ ] Keycloak mock (token, user API)
- [ ] MinIO mock (S3 upload/download)
- [ ] n8n webhook mock
- [ ] External API mock helpers
- [ ] Integration tests using mocks

**Details:** [S13 Full Story](./stories/S13.md)

---

### S14: Test Data Management

> **Test Data:** Automatické vytváření/mazání test dat + izolace od produkce

**As a** developer  
**I want** automatický systém pro test data (users, tenants, roles)  
**So that** testy mají konzistentní data A NIKDY se nedostanou do produkce

#### Critical Requirements

🔴 **SECURITY**: Test data NESMÍ být v produkci
- Environment-aware data seeding (pouze dev/test)
- Test user prefix (`test_*`, `e2e_*`)
- Automatic cleanup po testech
- Production safety checks

#### Implementation

**1. Test Data Seeders (Environment-Aware)**

```java
// backend/src/test-data/java/cz/muriel/core/testdata/TestDataSeeder.java
@Component
@Profile("!production") // CRITICAL: Only run in non-prod
public class TestDataSeeder {
    
    @PostConstruct
    public void seed() {
        if (isProdEnvironment()) {
            throw new IllegalStateException("❌ Test data seeder attempted to run in PRODUCTION!");
        }
        
        log.info("🌱 Seeding test data for environment: {}", environment);
        seedTestUsers();
        seedTestTenants();
        seedTestRoles();
    }
    
    private boolean isProdEnvironment() {
        return environment.getProperty("spring.profiles.active", "").contains("production");
    }
    
    private void seedTestUsers() {
        // ALWAYS prefix with 'test_' or 'e2e_'
        createUser("test_admin", "test-admin@example.com", Role.ADMIN);
        createUser("test_user", "test-user@example.com", Role.USER);
        createUser("e2e_login_user", "e2e@example.com", Role.USER);
    }
    
    private void seedTestTenants() {
        createTenant("test_tenant_alpha", TenantType.STANDARD);
        createTenant("test_tenant_beta", TenantType.PREMIUM);
    }
    
    private void seedTestRoles() {
        createRole("test_custom_role", Permission.READ, Permission.WRITE);
    }
}
```

**2. Production Safety Guards**

```java
// backend/src/main/java/cz/muriel/core/config/ProductionSafetyConfig.java
@Configuration
public class ProductionSafetyConfig {
    
    @Bean
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "production")
    public CommandLineRunner productionSafetyCheck(UserRepository userRepository) {
        return args -> {
            // Check for test users in production
            List<User> testUsers = userRepository.findByUsernameStartingWith("test_");
            if (!testUsers.isEmpty()) {
                log.error("❌ CRITICAL: {} test users found in PRODUCTION!", testUsers.size());
                throw new IllegalStateException("Test data detected in production environment!");
            }
            
            // Check for test tenants
            List<Tenant> testTenants = tenantRepository.findByNameStartingWith("test_");
            if (!testTenants.isEmpty()) {
                log.error("❌ CRITICAL: {} test tenants found in PRODUCTION!", testTenants.size());
                throw new IllegalStateException("Test tenants detected in production environment!");
            }
            
            log.info("✅ Production safety check passed - no test data found");
        };
    }
}
```

**3. Test Data Cleanup (After Each Test)**

```java
// backend/src/integration-test/java/cz/muriel/core/BaseIntegrationTest.java
@SpringBootTest
@Transactional
public abstract class BaseIntegrationTest {
    
    @Autowired
    protected TestDataManager testDataManager;
    
    @AfterEach
    void cleanupTestData() {
        testDataManager.deleteAllTestUsers();
        testDataManager.deleteAllTestTenants();
        testDataManager.deleteAllTestRoles();
    }
}

@Component
public class TestDataManager {
    
    public void deleteAllTestUsers() {
        userRepository.deleteByUsernameStartingWith("test_");
        userRepository.deleteByUsernameStartingWith("e2e_");
    }
    
    public void deleteAllTestTenants() {
        tenantRepository.deleteByNameStartingWith("test_");
    }
    
    public void deleteAllTestRoles() {
        roleRepository.deleteByNameStartingWith("test_");
    }
}
```

**4. Test Data Builders**

```java
// backend/src/integration-test/java/cz/muriel/core/testutil/TestDataBuilder.java
public class TestDataBuilder {
    
    public static User testUser() {
        return User.builder()
            .username("test_user_" + UUID.randomUUID().toString().substring(0, 8))
            .email("test-" + UUID.randomUUID() + "@example.com")
            .enabled(true)
            .build();
    }
    
    public static Tenant testTenant(String name) {
        return Tenant.builder()
            .name("test_" + name)
            .type(TenantType.STANDARD)
            .build();
    }
    
    public static Role testRole(String name, Permission... permissions) {
        return Role.builder()
            .name("test_" + name)
            .permissions(Set.of(permissions))
            .build();
    }
}
```

**5. E2E Test Data Setup (Playwright)**

```typescript
// e2e/helpers/test-data.ts
export class TestDataHelper {
  
  /**
   * Create test user via API (ONLY in test environment)
   */
  static async createTestUser(username: string, email: string): Promise<User> {
    const response = await fetch('http://localhost:8080/api/test-data/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: `e2e_${username}`, // ALWAYS prefix with e2e_
        email: `e2e-${email}`,
        password: 'Test.1234'
      })
    });
    
    if (!response.ok) {
      throw new Error('Failed to create test user');
    }
    
    return response.json();
  }
  
  /**
   * Cleanup all E2E test data
   */
  static async cleanup(): Promise<void> {
    await fetch('http://localhost:8080/api/test-data/cleanup', { method: 'DELETE' });
  }
}
```

**6. Database Constraints (Extra Safety)**

```sql
-- backend/src/main/resources/db/migration/V998__test_data_constraints.sql

-- Production safety: Prevent test data insertion in production
CREATE OR REPLACE FUNCTION prevent_test_data_in_production()
RETURNS TRIGGER AS $$
BEGIN
    IF current_setting('app.environment', true) = 'production' THEN
        IF NEW.username LIKE 'test_%' OR NEW.username LIKE 'e2e_%' THEN
            RAISE EXCEPTION 'Test users are not allowed in production environment';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_test_users_in_production
    BEFORE INSERT OR UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION prevent_test_data_in_production();

-- Similar trigger for tenants
CREATE TRIGGER check_test_tenants_in_production
    BEFORE INSERT OR UPDATE ON tenants
    FOR EACH ROW
    EXECUTE FUNCTION prevent_test_data_in_production();
```

**7. Test Data API Controller (Test Environment Only)**

```java
// backend/src/test-data/java/cz/muriel/core/testdata/TestDataController.java
@RestController
@RequestMapping("/api/test-data")
@Profile("!production") // CRITICAL: Only available in test/dev
public class TestDataController {
    
    @PostMapping("/users")
    public User createTestUser(@RequestBody CreateUserRequest request) {
        if (!request.getUsername().startsWith("test_") && !request.getUsername().startsWith("e2e_")) {
            throw new IllegalArgumentException("Test users must start with 'test_' or 'e2e_'");
        }
        
        return userService.createUser(request);
    }
    
    @DeleteMapping("/cleanup")
    public void cleanup() {
        testDataManager.deleteAllTestUsers();
        testDataManager.deleteAllTestTenants();
        testDataManager.deleteAllTestRoles();
    }
    
    @GetMapping("/seed")
    public void seed() {
        testDataSeeder.seed();
    }
}
```

#### Test Data Categories

| Category | Prefix | Example | Cleanup |
|----------|--------|---------|---------|
| **E2E Users** | `e2e_*` | `e2e_login_user` | After E2E suite |
| **Integration Users** | `test_*` | `test_admin` | After each test |
| **Tenants** | `test_*` | `test_tenant_alpha` | After each test |
| **Roles** | `test_*` | `test_custom_role` | After each test |

#### Production Safety Checklist

- [ ] ❌ **BLOCK**: Test data seeder @Profile("!production")
- [ ] ❌ **BLOCK**: Test data API controller @Profile("!production")
- [ ] ✅ **CHECK**: Production safety check on startup
- [ ] ✅ **VERIFY**: Database triggers prevent test_ inserts in prod
- [ ] ✅ **CLEANUP**: @AfterEach cleanup in integration tests
- [ ] ✅ **PREFIX**: All test data has 'test_' or 'e2e_' prefix
- [ ] ✅ **VALIDATE**: Pre-commit hook checks for hardcoded test credentials

#### Acceptance Checklist

- [ ] Test data seeders (users, tenants, roles)
- [ ] Production safety guards (@Profile, startup check)
- [ ] Database triggers (prevent test_ in production)
- [ ] Test data cleanup (@AfterEach)
- [ ] Test data builders (TestDataBuilder)
- [ ] E2E test data helpers (Playwright)
- [ ] Test data API (POST /api/test-data/users)
- [ ] Documentation (test data conventions)

**Details:** [S14 Full Story](./stories/S14.md)

---

## 🎯 Definition of Done

- [ ] Test registry database schema created
- [ ] REST API for test tracking (/api/test-registry)
- [ ] Playwright reporter registering tests
- [ ] JUnit listener for backend tests
- [ ] Test tagging system (@CORE-XXX)
- [ ] Pre-commit hook validating tags
- [ ] WireMock integration for external services
- [ ] Test data seeders (environment-aware)
- [ ] Production safety checks (prevent test data leak)
- [ ] Test data cleanup (automatic after tests)
- [ ] Grafana coverage dashboard
- [ ] CI/CD quality gates (GitHub Actions)
- [ ] Testing guide documentation
- [ ] 80%+ test coverage for new code

---

## 📈 Success Metrics

- **Coverage Tracking**: 100% testů mapuje na User Stories
- **Automation**: 100% testů běží v CI/CD
- **Quality**: <5% failed builds kvůli chybějícím testům
- **Visibility**: PO vidí coverage dashboard denně
- **Adoption**: Všichni deví píší testy před mergem PR
- **Data Safety**: 0 test users/tenants v produkci (automated checks)

---

## 🔗 Dependencies

- **EPIC-001**: Backlog system (User Stories existují)
- **EPIC-003**: CI/CD pipeline (GitHub Actions)
- Playwright 1.42+ (tag support)
- JUnit 5 (custom annotations)
- WireMock 2.35+ (HTTP mocking)
- Grafana (dashboards)
- PostgreSQL (test_registry table)

---

## E2E15: GitHub Actions CI/CD Workflows

> **Documentation:** Complete guide to all GitHub Actions workflows

**See:** [E2E15-github-actions-workflows/README.md](stories/E2E15-github-actions-workflows/README.md)

### Overview

Kompletní dokumentace všech 13 GitHub Actions workflows v `.github/workflows-disabled/`:

**Main Workflows:**
- `ci.yml` - Main CI pipeline (build + unit tests, 15-20 min)
- `pre-deploy.yml` - Pre-deploy smoke tests (5-7 min)
- `post-deploy.yml` - Post-deploy full E2E (20-30 min)
- `e2e.yml` - Full E2E test suite (20-30 min)

**Quality & Security:**
- `code-quality.yml` - Linting, SonarQube (5 min)
- `security-scan.yml` - OWASP, dependency check (10 min)
- `naming-lint.yml` - Java naming conventions (1 min)

**Specialized:**
- `reporting-tests.yml` - Reporting module (10 min)
- `streaming-tests.yml` - Kafka CDC (8 min)
- `tests-monitoring-bff.yml` - Monitoring BFF (5 min)

### Current Status

✅ **Workflows DISABLED** during EPIC-017 implementation
- Location: `.github/workflows-disabled/`
- Reason: Save CI minutes, prevent false failures
- Re-enable: After modular architecture complete

### Key Features Documented

1. **Workflow Trigger Conditions**
   - Push events (main, develop)
   - Pull requests
   - Scheduled runs (weekly security scans)
   - Manual dispatch

2. **Job Dependencies & Orchestration**
   - Fast feedback loop (lint → unit → integration → E2E)
   - Parallel matrix strategies (browsers, OS, Node versions)
   - Conditional execution (deploy only on main)

3. **Enable/Disable Workflows**
   ```bash
   # Disable all
   mv .github/workflows/*.yml .github/workflows-disabled/
   
   # Enable all
   mv .github/workflows-disabled/*.yml .github/workflows/
   
   # Enable selectively
   mv .github/workflows-disabled/ci.yml .github/workflows/
   ```

4. **Skip CI on Specific Commits**
   ```bash
   git commit -m "docs: Update README [skip ci]"
   ```

5. **Troubleshooting Guide**
   - Workflow doesn't trigger
   - Tests timeout
   - Environment variables missing
   - Docker Compose fails
   - Flaky E2E tests

6. **Best Practices**
   - Cache dependencies (Maven, npm)
   - Matrix strategy for parallel tests
   - Conditional job execution
   - Artifacts for debugging
   - Fast feedback loop

### Deployment Flow

```
pre-deploy.yml (smoke)
  ↓ PASS
Deployment
  ↓ SUCCESS
post-deploy.yml (full E2E)
  ↓ FAIL
Rollback
```

---

## 📅 Implementation Plan

### Week 1: Test Registry Foundation
- Day 1-2: Database schema + migration
- Day 3-4: Backend API (TestRegistryController)
- Day 5: Playwright reporter

### Week 2: Tagging & Validation
- Day 1-2: Test tagging system (@CORE-XXX)
- Day 3: JUnit @UserStory annotation
- Day 4: Pre-commit hook
- Day 5: Tag extraction utilities

### Week 3: Mocking & Test Data
- Day 1-2: WireMock setup (Keycloak, MinIO, n8n)
- Day 3-4: Test data seeders + production safety
- Day 5: Test data cleanup + builders

### Week 4: Dashboard & CI/CD
- Day 1-2: Grafana coverage dashboard
- Day 3-4: GitHub Actions quality gates (integrate with E2E15 docs)
- Day 5: Testing guide documentation

---

**Total Effort:** ~66 hours (4 týdny)  
**Priority:** P0 (Foundation for all future development)  
**Value:** Test-driven culture + visibility + automation + CI/CD transparency
