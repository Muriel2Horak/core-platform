# INF-024: Test Framework Integration

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 3 dny, ~800 LOC  
**Owner:** QA + Development Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State (žádné test gates):**

```bash
# Deploy proces BEZ validace:
git push
  → Docker build (může mít syntax errors!)
  → Deploy to production (💥 BOOM!)

# Žádné pre-commit checks
# Žádné syntax validation
# Žádné unit tests jako gate
# E2E testy POUZE po deployi (pozdě!)
```

**Real Issues:**
- TypeScript syntax error deploynutý do produkce (5 hodin downtime)
- Backend změna bez unit testů → regression v API
- Database migration bez testů → corrupted data

### Goal

**Test Gates na KAŽDÉ úrovni:**

```
Developer Workflow (Fail Fast!)
  ├─ Pre-Commit Hook (2 sec)
  │  ├─ ESLint fix auto
  │  ├─ Prettier format
  │  └─ Checkstyle (Java)
  │
  ├─ Pre-Push Hook (10 sec)
  │  ├─ Unit tests (changed files only)
  │  ├─ TypeScript compilation
  │  └─ Java compilation
  │
  ├─ CI Pipeline (20 min)
  │  ├─ Full unit test suite
  │  ├─ Integration tests (Testcontainers)
  │  ├─ E2E smoke tests
  │  └─ Coverage thresholds
  │
  └─ Pre-Deploy Gate (5 min)
     ├─ Syntax validation
     ├─ Database migration dry-run
     └─ Smoke test staging environment
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Pre-Commit Hooks**
   - ESLint + Prettier (auto-fix)
   - Checkstyle (Java)
   - No commits with linting errors

2. ✅ **Unit Test Framework**
   - JUnit 5 + Mockito (backend)
   - Jest + React Testing Library (frontend)
   - Coverage threshold: 80%

3. ✅ **Integration Tests**
   - Testcontainers (PostgreSQL, Redis, Kafka)
   - API contract tests
   - Database migration validation

4. ✅ **E2E Test Framework**
   - Playwright pre-deploy smoke (critical paths)
   - Playwright post-deploy full (all features)

### Implementation

**File:** `.husky/pre-commit` (Git hook)

```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

echo "🔍 Running pre-commit checks..."

# Frontend linting
cd frontend
npm run lint:fix || {
  echo "❌ ESLint failed"
  exit 1
}

npm run format || {
  echo "❌ Prettier failed"
  exit 1
}

# Backend linting
cd ../backend
./mvnw checkstyle:check || {
  echo "❌ Checkstyle failed"
  exit 1
}

echo "✅ Pre-commit checks passed"
```

**File:** `.husky/pre-push` (Git hook)

```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

echo "🧪 Running pre-push tests..."

# Frontend unit tests (changed files only)
cd frontend
CHANGED_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.tsx?$')

if [ -n "$CHANGED_FILES" ]; then
  npm run test:changed || {
    echo "❌ Frontend unit tests failed"
    exit 1
  }
fi

# Backend unit tests (changed modules only)
cd ../backend
./mvnw test -Dtest='**/*Test.java' -DfailIfNoTests=false || {
  echo "❌ Backend unit tests failed"
  exit 1
}

echo "✅ Pre-push tests passed"
```

**File:** `package.json` (Husky setup)

```json
{
  "scripts": {
    "prepare": "husky install",
    "lint:fix": "eslint --fix src/**/*.{ts,tsx}",
    "format": "prettier --write src/**/*.{ts,tsx}",
    "test:changed": "jest --findRelatedTests --passWithNoTests",
    "test:ci": "jest --coverage --maxWorkers=4"
  },
  "devDependencies": {
    "husky": "^8.0.3",
    "lint-staged": "^15.1.0",
    "prettier": "^3.1.0",
    "eslint": "^8.54.0"
  },
  "lint-staged": {
    "*.{ts,tsx}": ["eslint --fix", "prettier --write"],
    "*.{json,md}": ["prettier --write"]
  }
}
```

**File:** `backend/pom.xml` (Testing frameworks)

```xml
<dependencies>
  <!-- Unit Testing -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>

  <!-- Mockito -->
  <dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
  </dependency>

  <!-- Integration Testing -->
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.0</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.0</version>
    <scope>test</scope>
  </dependency>

  <!-- REST Assured (API testing) -->
  <dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>

<build>
  <plugins>
    <!-- JaCoCo Coverage -->
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <version>0.8.11</version>
      <executions>
        <execution>
          <goals>
            <goal>prepare-agent</goal>
          </goals>
        </execution>
        <execution>
          <id>report</id>
          <phase>test</phase>
          <goals>
            <goal>report</goal>
          </goals>
        </execution>
        <execution>
          <id>check</id>
          <goals>
            <goal>check</goal>
          </goals>
          <configuration>
            <rules>
              <rule>
                <element>PACKAGE</element>
                <limits>
                  <limit>
                    <counter>LINE</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.80</minimum>
                  </limit>
                </limits>
              </rule>
            </rules>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

**File:** `backend/src/test/java/cz/muriel/core/BaseIntegrationTest.java`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    protected TestRestTemplate restTemplate;
    
    @BeforeEach
    void setUp() {
        // Clean database before each test
        cleanDatabase();
    }
}
```

**File:** `backend/src/test/java/cz/muriel/core/user/UserServiceIT.java`

```java
class UserServiceIT extends BaseIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void createUser_shouldPersistToDatabase() {
        // Given
        CreateUserRequest request = new CreateUserRequest("test@example.com", "Test User");
        
        // When
        UserDTO result = userService.createUser(request);
        
        // Then
        assertThat(result.id()).isNotNull();
        assertThat(result.email()).isEqualTo("test@example.com");
        
        // Verify database state
        Optional<User> saved = userRepository.findById(result.id());
        assertThat(saved).isPresent();
        assertThat(saved.get().getEmail()).isEqualTo("test@example.com");
    }
    
    @Test
    void createUser_withDuplicateEmail_shouldThrowException() {
        // Given
        userRepository.save(new User("test@example.com", "Existing"));
        CreateUserRequest request = new CreateUserRequest("test@example.com", "Duplicate");
        
        // When/Then
        assertThatThrownBy(() -> userService.createUser(request))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessageContaining("Email already exists");
    }
}
```

**File:** `backend/src/test/java/cz/muriel/core/api/UserApiIT.java` (REST API test)

```java
class UserApiIT extends BaseIntegrationTest {
    
    @Test
    void getUserById_shouldReturn200() {
        // Given
        User user = userRepository.save(new User("test@example.com", "Test User"));
        
        // When
        ResponseEntity<UserDTO> response = restTemplate
            .withBasicAuth("admin", "admin")
            .getForEntity("/api/users/" + user.getId(), UserDTO.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualTo("test@example.com");
    }
    
    @Test
    void createUser_withInvalidEmail_shouldReturn400() {
        // Given
        String invalidRequest = """
            {
              "email": "not-an-email",
              "name": "Test User"
            }
            """;
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/users", 
            invalidRequest, 
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
```

**File:** `frontend/jest.config.js`

```javascript
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/src/test/setup.ts'],
  
  // Coverage thresholds
  coverageThreshold: {
    global: {
      branches: 80,
      functions: 80,
      lines: 80,
      statements: 80,
    },
  },
  
  // Test patterns
  testMatch: [
    '**/__tests__/**/*.test.{ts,tsx}',
    '**/*.test.{ts,tsx}',
  ],
  
  // Module mappings
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
  },
};
```

**File:** `frontend/src/__tests__/components/UserList.test.tsx`

```tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserList } from '@/components/UserList';
import { mockUsers } from '../mocks/users';

// Mock API calls
jest.mock('@/api/users', () => ({
  fetchUsers: jest.fn(() => Promise.resolve(mockUsers)),
}));

describe('UserList', () => {
  it('renders user list', async () => {
    render(<UserList />);

    await waitFor(() => {
      expect(screen.getByText('test@example.com')).toBeInTheDocument();
    });
  });

  it('filters users by search term', async () => {
    const user = userEvent.setup();
    render(<UserList />);

    const searchInput = screen.getByPlaceholderText('Search users...');
    await user.type(searchInput, 'john');

    expect(screen.getByText('john@example.com')).toBeInTheDocument();
    expect(screen.queryByText('jane@example.com')).not.toBeInTheDocument();
  });

  it('handles API errors gracefully', async () => {
    const { fetchUsers } = require('@/api/users');
    fetchUsers.mockRejectedValueOnce(new Error('Network error'));

    render(<UserList />);

    await waitFor(() => {
      expect(screen.getByText('Failed to load users')).toBeInTheDocument();
    });
  });
});
```

**File:** `e2e/specs/smoke/critical-path.spec.ts` (Pre-deploy smoke)

```typescript
import { test, expect } from '@playwright/test';

test.describe('Critical Path Smoke Tests', () => {
  test('home page loads', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('h1')).toBeVisible();
  });

  test('user can login', async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name=email]', 'admin@example.com');
    await page.fill('[name=password]', 'admin');
    await page.click('button[type=submit]');

    await expect(page).toHaveURL('/dashboard');
  });

  test('API health check', async ({ request }) => {
    const response = await request.get('/api/actuator/health');
    expect(response.status()).toBe(200);
    
    const body = await response.json();
    expect(body.status).toBe('UP');
  });
});
```

**File:** `Makefile` (Test targets)

```makefile
# Unit tests only
.PHONY: test-unit
test-unit:
	cd backend && ./mvnw test -Dtest='**/*Test.java'
	cd frontend && npm run test -- --run

# Integration tests (Testcontainers)
.PHONY: test-integration
test-integration:
	cd backend && ./mvnw verify -Dtest='**/*IT.java'

# E2E smoke tests (critical paths only)
.PHONY: test-smoke
test-smoke:
	cd e2e && npm run test:pre

# Full test suite
.PHONY: test-all
test-all: test-unit test-integration test-smoke
	@echo "✅ All tests passed"

# Coverage report
.PHONY: test-coverage
test-coverage:
	cd backend && ./mvnw jacoco:report
	cd frontend && npm run test:coverage
	@echo "📊 Coverage reports generated"
```

**File:** `scripts/validate-deployment.sh` (Pre-deploy gate)

```bash
#!/bin/bash
set -euo pipefail

echo "🔍 Validating deployment..."

# 1. Syntax validation
echo "Checking TypeScript compilation..."
cd frontend && npm run typecheck || exit 1

echo "Checking Java compilation..."
cd ../backend && ./mvnw compile -DskipTests || exit 1

# 2. Database migration dry-run
echo "Validating database migrations..."
cd ../backend && ./mvnw flyway:validate || exit 1

# 3. Smoke tests
echo "Running smoke tests..."
cd ../e2e && npm run test:pre || exit 1

echo "✅ Deployment validation passed"
```

**Effort:** 3 dny  
**LOC:** ~800  
**Priority:** 🔥 CRITICAL

---

## 🧪 TESTING STRATEGY

### Test Pyramid

```
        E2E Tests (10%)
       /            \
      /  Integration  \
     /    Tests (30%)  \
    /____________________\
    Unit Tests (60%)
```

**Unit Tests:**
- Fast (< 1 sec per test)
- No external dependencies
- Mock everything

**Integration Tests:**
- Testcontainers (real DB, Redis, Kafka)
- API contract validation
- Database migrations

**E2E Tests:**
- Pre-deploy: Critical paths only (5-10 tests)
- Post-deploy: Full suite (50+ tests)

---

## 📊 COVERAGE THRESHOLDS

```xml
<!-- backend/pom.xml -->
<jacoco.line.coverage>0.80</jacoco.line.coverage>
<jacoco.branch.coverage>0.75</jacoco.branch.coverage>
```

```json
// frontend/jest.config.js
"coverageThreshold": {
  "global": {
    "lines": 80,
    "functions": 80,
    "branches": 80
  }
}
```

---

## 🔗 DEPENDENCIES

**Blocks:**
- INF-023: Enhanced CI/CD Pipeline (uses these test gates)

**Requires:**
- Docker (for Testcontainers)
- Node.js 20+ (for Jest/Playwright)

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
