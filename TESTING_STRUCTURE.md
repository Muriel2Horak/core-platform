# Testing Structure - Core Platform

## 📁 Repository Structure

```
core-platform/
├── backend/src/test/          # Backend unit tests (JUnit 5)
├── frontend/src/**/*.test.tsx # Frontend unit tests (Vitest)
├── e2e/                       # E2E tests (Playwright)
│   ├── config/                # Configuration
│   ├── helpers/               # Login, API helpers
│   ├── scripts/               # Scaffold, teardown
│   └── specs/                 # Test specifications
│       ├── pre/               # PRE-DEPLOY smoke tests
│       └── post/              # POST-DEPLOY full E2E
└── tests/                     # Legacy integration tests
    ├── multitenancy_smoke.sh  # Multitenancy API tests
    ├── streaming_integration_test.sh
    └── test_tenant_api.sh
```

## 🎯 Test Types

### 1. Unit Tests
**Location**: `backend/src/test/`, `frontend/src/**/*.test.tsx`
**Framework**: JUnit 5 (backend), Vitest (frontend)
**Target**: Individual components, services, utilities
**Run**: `make test-backend`, `make test-frontend`, `make test-all`
**Speed**: Fast (~2-5 min)

### 2. E2E Tests (NEW - Playwright)
**Location**: `e2e/`
**Framework**: Playwright with TypeScript
**Target**: Full user flows, GUI validation
**Run**: 
- `make test-e2e-pre` - PRE-DEPLOY smoke (5-7 min)
- `make test-e2e-post` - POST-DEPLOY full (20-30 min)

**Two-Tier Strategy:**
- **PRE-DEPLOY**: Fast smoke tests before deployment (gate)
- **POST-DEPLOY**: Full E2E scenarios on deployed environment

### 3. Legacy Integration Tests
**Location**: `tests/`
**Framework**: Bash scripts
**Target**: API-level integration, multitenancy
**Run**: `make test-mt`, `make verify`
**Status**: ⚠️ Consider migrating to Playwright

## 🚀 Recommended Test Flow

### Local Development
```bash
# 1. Unit tests (fast feedback)
make test-all

# 2. E2E smoke tests (pre-deploy gate)
make test-e2e-pre

# 3. Full validation (optional)
make test-e2e-post
```

### CI/CD Pipeline
```bash
# Pre-deployment gate
make ci-test-pipeline  # unit + E2E PRE

# Post-deployment validation
make ci-post-deploy    # E2E POST on deployed env
```

## 📊 Test Coverage Goals

- **Unit Tests**: ≥80% coverage
- **E2E PRE**: Critical paths (login, RBAC, CRUD, workflow)
- **E2E POST**: End-to-end scenarios with data lifecycle

## 🔄 Migration Plan

### Phase 1: ✅ DONE
- [x] Create E2E infrastructure with Playwright
- [x] Implement PRE-DEPLOY smoke tests
- [x] Implement POST-DEPLOY full E2E
- [x] Integrate into Makefile and CI/CD

### Phase 2: 🔄 TODO
- [ ] Migrate `multitenancy_smoke.sh` to Playwright
- [ ] Migrate `streaming_integration_test.sh` to Playwright
- [ ] Deprecate bash scripts in `tests/`
- [ ] Consolidate all E2E tests under `e2e/`

### Phase 3: 📋 FUTURE
- [ ] Add visual regression tests
- [ ] Add performance tests
- [ ] Add accessibility tests (a11y)
- [ ] API contract tests (Pact/OpenAPI)

## 🛠️ Test Commands Reference

### Unit Tests
```bash
make test-backend       # Backend unit tests
make test-frontend      # Frontend unit tests
make test-all          # All unit tests
```

### E2E Tests (Playwright)
```bash
make e2e-setup         # Install dependencies (once)
make test-e2e-pre      # PRE-DEPLOY smoke (5-7 min)
make test-e2e-post     # POST-DEPLOY full (20-30 min)
make test-e2e          # All E2E tests
make e2e-report        # View HTML report
```

### Legacy Integration Tests
```bash
make test-mt           # Multitenancy API tests
make verify            # Quick smoke tests
make verify-full       # Full integration tests
```

### CI/CD
```bash
make ci-test-pipeline  # Full CI pipeline
make ci-post-deploy    # Post-deployment validation
make test-comprehensive # Unit + integration + E2E PRE
```

## 📝 Writing New Tests

### Unit Test
**Backend**: `backend/src/test/java/com/example/MyServiceTest.java`
```java
@SpringBootTest
class MyServiceTest {
    @Test
    void shouldDoSomething() {
        // arrange, act, assert
    }
}
```

**Frontend**: `frontend/src/components/MyComponent.test.tsx`
```typescript
import { render, screen } from '@testing-library/react';

describe('MyComponent', () => {
  it('should render', () => {
    render(<MyComponent />);
    expect(screen.getByText('Hello')).toBeInTheDocument();
  });
});
```

### E2E Test (Playwright)
**PRE-DEPLOY**: `e2e/specs/pre/05_my_smoke.spec.ts`
```typescript
import { test, expect } from '@playwright/test';
import { loginAsTestUser } from '../../helpers/login';

test.describe('My Feature Smoke', () => {
  test('should work', async ({ page }) => {
    await loginAsTestUser(page);
    // ... test critical path only
  });
});
```

**POST-DEPLOY**: `e2e/specs/post/60_my_full_scenario.spec.ts`
```typescript
import { test, expect } from '@playwright/test';
import { loginAsAdmin } from '../../helpers/login';
import { createEntity, deleteEntity } from '../../helpers/api';

test.describe('My Full Scenario', () => {
  test('should complete full flow', async ({ page }) => {
    // Create test data
    const entity = await createEntity('MyEntity');
    
    // Test full scenario
    await loginAsAdmin(page);
    // ... full user flow
    
    // Cleanup
    await deleteEntity(entity.id);
  });
});
```

## 🎯 Test Strategy

### What to Test Where

**Unit Tests**:
- Business logic
- Utility functions
- Component rendering
- Service methods

**E2E PRE (Smoke)**:
- Login flow
- Critical CRUD operations
- Menu/navigation
- Basic workflow UI

**E2E POST (Full)**:
- End-to-end user scenarios
- Data lifecycle (create → update → delete)
- Multi-step workflows
- Cross-feature integration

**Legacy Integration**:
- API contracts (until migrated)
- Multitenancy validation (until migrated)

## 🚨 Don't Test in E2E

- Edge cases (use unit tests)
- Error handling details (use unit tests)
- Performance optimization (use dedicated tools)
- Component variations (use unit tests)

## ✅ Best Practices

1. **Fast PRE-DEPLOY tests**: Keep under 7 minutes
2. **Isolated POST-DEPLOY tests**: Use scaffold/teardown
3. **No DB access**: Test via GUI and APIs only
4. **Parallel execution**: Enable where possible
5. **Clear test names**: `should_do_something_when_condition`
6. **Cleanup**: Always clean up test data
7. **Retry on flake**: Configure retries for network issues
8. **Screenshots**: Capture on failure for debugging

## 📚 Further Reading

- [E2E Makefile Integration](./E2E_MAKEFILE_INTEGRATION.md)
- [E2E README](./e2e/README.md)
- [Legacy Tests README](./tests/README_tests.txt)
- [Playwright Docs](https://playwright.dev)
