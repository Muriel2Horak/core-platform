# Pull Request

## 📝 Description
<!-- Describe what this PR does and why -->

## 🔗 Related Issues
<!-- Link to related issues/tickets -->
Closes #

## 🧪 Testing Checklist

### For Backend Changes (Java/Spring Boot)
- [ ] **All integration tests extend `AbstractIntegrationTest`**
  - [ ] No direct `@Testcontainers` usage (use base class)
  - [ ] Tests use `schemaName` for DB isolation
  - [ ] Tests use `topicSuffix` for Kafka isolation
  
- [ ] **No `@DirtiesContext` added** (unless absolutely necessary)
  - [ ] If added: Documented why in test Javadoc
  - [ ] If added: Used `AFTER_CLASS` not `AFTER_EACH_TEST_METHOD` (unless critical)
  
- [ ] **Async operations use Awaitility**
  - [ ] No `Thread.sleep()` for waiting (use `await().atMost(...)`)
  - [ ] Timeouts are reasonable (5-30 seconds max)
  
- [ ] **Tests pass in parallel**
  - [ ] `./mvnw verify -Dsurefire.threads=8` passes locally
  - [ ] No flaky tests (ran 3+ times successfully)
  
- [ ] **Maven Enforcer passes**
  - [ ] `./mvnw validate` passes
  - [ ] No new dependency convergence issues
  - [ ] No duplicate classes errors

### For Frontend Changes (React/TypeScript)
- [ ] **Tests pass in parallel**
  - [ ] `npm test -- --run` passes locally
  - [ ] No test interference or race conditions
  
- [ ] **Type checking passes**
  - [ ] `npm run typecheck` passes
  - [ ] No `@ts-ignore` added (unless documented why)
  
- [ ] **Linting passes**
  - [ ] `npm run lint` passes
  - [ ] ESLint rules followed

### For Database Changes (Flyway/SQL)
- [ ] **Migration scripts are idempotent**
  - [ ] Can run twice without errors
  - [ ] Handles existing data gracefully
  
- [ ] **Migration numbering is correct**
  - [ ] Version or repeatable migration
  - [ ] No conflicts with other migrations
  
- [ ] **Backward compatibility**
  - [ ] Old code works with new schema (if zero-downtime)
  - [ ] No breaking changes without migration path

## 🚀 Deployment Notes
<!-- Any special deployment considerations? -->

### Configuration Changes
- [ ] No new env vars (or documented in `.env.example`)
- [ ] No breaking config changes (or migration guide provided)

### Performance Impact
- [ ] No significant performance regression
- [ ] Large queries reviewed for indexes
- [ ] N+1 queries avoided (JPA/Hibernate)

## 📸 Screenshots (if applicable)
<!-- UI changes, API responses, etc. -->

## ✅ Pre-Merge Checklist
- [ ] Code reviewed by at least 1 person
- [ ] All CI checks pass (build, tests, linting)
- [ ] Documentation updated (README, CHANGELOG, etc.)
- [ ] No merge conflicts with target branch
- [ ] Commits are clean and squashable (if needed)

---

## 🧠 Reviewer Guidelines

### What to Look For
1. **Test Quality**
   - Tests are isolated (use `AbstractIntegrationTest` pattern)
   - No hardcoded waits (`Thread.sleep`)
   - Proper use of `Awaitility` for async
   - Tests are deterministic (no flakiness)

2. **Code Quality**
   - Follows existing patterns
   - No obvious performance issues
   - Error handling is appropriate
   - Logging is meaningful

3. **Security**
   - No secrets in code
   - Input validation present
   - SQL injection risks mitigated
   - CSRF/XSS protections in place

4. **Database**
   - Migrations are safe
   - Indexes added for new queries
   - No data loss risks

### Testing the PR
```bash
# Checkout PR
gh pr checkout <PR_NUMBER>

# Backend: Run tests in parallel
cd backend
./mvnw verify -Dsurefire.threads=8

# Frontend: Run tests
cd frontend
npm test -- --run

# Full build
cd .. && make build
```
