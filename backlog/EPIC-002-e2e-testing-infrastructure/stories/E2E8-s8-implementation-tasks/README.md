---
id: S8
epic: EPIC-002-e2e-testing-infrastructure
title: "Implementation Tasks"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "8 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-002-e2e-testing-infrastructure/stories/E2E8-s8-implementation-tasks/README.md
    - backlog/EPIC-002-e2e-testing-infrastructure/README.md
---

# S8 Implementation Tasks

**Story:** Test Registry & Tracking  
**Status:** 🔵 TODO (0/5 tasks)  
**Total Effort:** ~8 hours

---

## 📋 Task Checklist

| Task | Description | Status | Effort | LOC |
|------|-------------|--------|--------|-----|
| [T1](./T1-database-schema.md) | Database Schema Migration | 🔵 TODO | 1h | ~50 |
| [T2](./T2-backend-model-repository.md) | Backend Model & Repository | 🔵 TODO | 2h | ~150 |
| [T3](./T3-rest-api-controller.md) | REST API Controller | 🔵 TODO | 2h | ~200 |
| [T4](./T4-playwright-reporter.md) | Playwright Reporter | 🔵 TODO | 2h | ~150 |
| [T5](./T5-junit-listener.md) | JUnit Listener | 🔵 TODO | 1h | ~50 |

**Total:** ~600 LOC

---

## 🔄 Implementation Order

### Phase 1: Backend Foundation (T1-T2)
1. **T1**: Create database migration
   - Define schema: test_registry table
   - Add indexes: user_story_id, test_type, status
   - Run migration: `make db-migrate`

2. **T2**: Implement backend model
   - JPA entity: TestRegistry.java
   - Repository: TestRegistryRepository.java
   - Service: TestRegistryService.java

### Phase 2: API Layer (T3)
3. **T3**: Build REST API
   - Controller: TestRegistryController.java
   - Endpoints: GET /story/{id}, GET /coverage, POST /
   - Unit tests: TestRegistryControllerTest.java

### Phase 3: Test Integration (T4-T5)
4. **T4**: Playwright reporter
   - Reporter: e2e/reporters/registry-reporter.ts
   - Tag extraction: extractStoryTag(), extractTestId()
   - Config: playwright.config.ts (add reporter)

5. **T5**: JUnit listener
   - Listener: TestRegistryListener.java
   - Annotation: @UserStory("CORE-XXX")
   - Config: Register listener in test runner

---

## 🧪 Testing Strategy

### Unit Tests
- `TestRegistryServiceTest.java` - Service layer logic
- `TestRegistryControllerTest.java` - REST API endpoints
- `RegistryReporterTest.ts` - Playwright reporter

### Integration Tests
- E2E test execution → DB record created
- JUnit test with @UserStory → DB record created
- Coverage endpoint returns correct percentage

### Manual Verification
```bash
# 1. Run E2E test with @CORE-123 tag
cd e2e
npx playwright test --grep @CORE-123

# 2. Verify DB record
psql -U core -d core -c "SELECT * FROM test_registry WHERE user_story_id = 'CORE-123';"

# 3. Check coverage API
curl http://localhost:8080/api/test-registry/coverage
```

---

## 📦 Deliverables

- [ ] `V999__test_registry.sql` migration
- [ ] `TestRegistry.java` entity
- [ ] `TestRegistryRepository.java` repository
- [ ] `TestRegistryService.java` service
- [ ] `TestRegistryController.java` controller
- [ ] `registry-reporter.ts` Playwright reporter
- [ ] `TestRegistryListener.java` JUnit listener
- [ ] `@UserStory` annotation
- [ ] Unit tests (80%+ coverage)
- [ ] Integration tests (full flow)
- [ ] API documentation (README)

---

## 🎯 Definition of Done

- [ ] All 5 tasks completed
- [ ] Database migration executed successfully
- [ ] REST API endpoints responding correctly
- [ ] Playwright reporter registering E2E tests
- [ ] JUnit listener registering Unit tests
- [ ] 80%+ test coverage for new code
- [ ] Integration tests passing
- [ ] Documentation updated
- [ ] Code reviewed and merged

---

## 🔗 Dependencies

- PostgreSQL database (for migration)
- Spring Boot (backend framework)
- Playwright 1.42+ (Reporter API)
- JUnit 5 (RunListener API)

---

**Back to Story:** [S8.md](../S8.md)
