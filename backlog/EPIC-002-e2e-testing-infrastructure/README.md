# EPIC-002: Testing Infrastructure & Framework

**Status:** 🔵 **IN PROGRESS**  
**Priority:** P0 (Critical Foundation)  
**Effort:** ~30 hodin  
**LOC:** ~2,500 řádků (framework + config + documentation)

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
| [TF-001](#tf-001-test-registry--tracking) | Test Registry & Tracking | 🔵 TODO | ~600 | 8h | Evidence testů |
| [TF-002](#tf-002-test-tagging-system) | Test ID Tagging System | 🔵 TODO | ~400 | 6h | Mapování US → testy |
| [TF-003](#tf-003-coverage-dashboard) | Coverage Dashboard | 🔵 TODO | ~500 | 8h | Visualizace pokrytí |
| [TF-004](#tf-004-cicd-quality-gates) | CI/CD Quality Gates | 🔵 TODO | ~400 | 6h | Automatická validace |
| [TF-005](#tf-005-testing-standards--guide) | Testing Standards & Guide | 🔵 TODO | ~600 | 8h | Dokumentace |
| **TOTAL** | | **0/5** | **~2,500** | **~36h** | **Test infrastructure** |

---

## 📖 Detailed Stories

### TF-001: Test Registry & Tracking

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

### TF-002: Test Tagging System

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

### TF-003: Coverage Dashboard

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

### TF-004: CI/CD Quality Gates

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

### TF-005: Testing Standards & Guide

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

## 🎯 Definition of Done

- [ ] Test registry database schema created
- [ ] REST API for test tracking (/api/test-registry)
- [ ] Playwright reporter registering tests
- [ ] JUnit listener for backend tests
- [ ] Test tagging system (@CORE-XXX)
- [ ] Pre-commit hook validating tags
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

---

## 🔗 Dependencies

- **EPIC-001**: Backlog system (User Stories existují)
- **EPIC-003**: CI/CD pipeline (GitHub Actions)
- Playwright 1.42+ (tag support)
- JUnit 5 (custom annotations)
- Grafana (dashboards)
- PostgreSQL (test_registry table)

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

### Week 3: Dashboard & CI/CD
- Day 1-2: Grafana coverage dashboard
- Day 3-4: GitHub Actions quality gates
- Day 5: Testing guide documentation

---

**Total Effort:** ~36 hours (3 týdny)  
**Priority:** P0 (Foundation for all future development)  
**Value:** Test-driven culture + visibility + automation
