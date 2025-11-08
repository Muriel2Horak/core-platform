# T1: Create Multi-Stage GitHub Actions Pipeline

**Parent Story:** INF-023 Enhanced CI/CD Pipeline  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 6 hours  
**Owner:** DevOps

---

## 🎯 Objective

Implement 6-stage GitHub Actions pipeline with parallel jobs and fail-fast gates.

---

## 📋 Tasks

### 1. Create Main Workflow

**File:** `.github/workflows/ci.yml`

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  JAVA_VERSION: '21'
  NODE_VERSION: '20'

jobs:
  # ========================================
  # STAGE 1: SYNTAX & LINT (Fail Fast!)
  # ========================================
  syntax-check:
    runs-on: ubuntu-latest
    timeout-minutes: 5
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Checkstyle (Backend)
        working-directory: ./backend
        run: ./mvnw checkstyle:check

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: ESLint (Frontend)
        working-directory: ./frontend
        run: |
          npm ci
          npm run lint

      - name: TypeScript Check
        working-directory: ./frontend
        run: npm run typecheck

  # ========================================
  # STAGE 2: UNIT TESTS (Parallel)
  # ========================================
  unit-tests-backend:
    needs: syntax-check
    runs-on: ubuntu-latest
    timeout-minutes: 10
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Run Unit Tests
        working-directory: ./backend
        run: ./mvnw test -Dtest='**/*Test.java'

      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./backend/target/site/jacoco/jacoco.xml
          flags: backend

  unit-tests-frontend:
    needs: syntax-check
    runs-on: ubuntu-latest
    timeout-minutes: 8
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install Dependencies
        working-directory: ./frontend
        run: npm ci

      - name: Run Unit Tests
        working-directory: ./frontend
        run: npm run test:ci

      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./frontend/coverage/lcov.info
          flags: frontend
```

### 2. Add Integration Tests Stage

```yaml
  # ========================================
  # STAGE 3: INTEGRATION TESTS (Testcontainers)
  # ========================================
  integration-tests:
    needs: [unit-tests-backend, unit-tests-frontend]
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Run Integration Tests
        working-directory: ./backend
        run: ./mvnw verify -Dtest='**/*IT.java'
        env:
          SPRING_PROFILES_ACTIVE: test
```

### 3. Add Build Stage

```yaml
  # ========================================
  # STAGE 4: BUILD ARTIFACTS (Parallel)
  # ========================================
  build-backend:
    needs: integration-tests
    runs-on: ubuntu-latest
    timeout-minutes: 10
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Build JAR
        working-directory: ./backend
        run: ./mvnw clean package -DskipTests

      - name: Upload Artifact
        uses: actions/upload-artifact@v3
        with:
          name: backend-jar
          path: backend/target/*.jar
```

### 4. Test Workflow

```bash
# Trigger workflow
git add .github/workflows/ci.yml
git commit -m "feat(ci): Add multi-stage CI/CD pipeline"
git push

# Monitor in GitHub Actions UI
open https://github.com/$REPO/actions
```

---

## ✅ Acceptance Criteria

- [ ] Syntax check fails fast on ESLint/Checkstyle errors
- [ ] Unit tests run in parallel (backend + frontend)
- [ ] Integration tests use Testcontainers
- [ ] Artifacts uploaded (JAR, frontend dist)
- [ ] Pipeline completes in < 30 minutes
- [ ] Coverage reports sent to CodeCov

---

## 🔗 Dependencies

- Requires GitHub Actions runners
- Requires CodeCov account (optional)
