---
id: INF-023
epic: EPIC-007-infrastructure-deployment
title: "Enhanced CI/CD Pipeline"
priority: P0
status: todo
assignee: ""
created: 2025-11-08
updated: 2026-01-15
estimate: "4 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-007-infrastructure-deployment/stories/INF-023-enhanced-cicd/README.md
    - backlog/EPIC-007-infrastructure-deployment/README.md
---


# INF-023: Enhanced CI/CD Pipeline

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 4 dny, ~1100 LOC  
**Owner:** DevOps + Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State (make-based deployment):**

```bash
# Developer workflow je chaotické:
make clean-fast      # 10-15 minut, ŽÁDNÝ feedback
make test-backend    # Bez cache, pokaždé full rebuild
make deploy          # Manual trigger, ŽÁDNÁ automatizace

# Issues:
- Žádné test gates (syntax → unit → integration → deploy)
- Žádný artifact caching (Maven .m2, npm cache)
- Serial execution (backend → frontend → e2e, 40 minut)
- Manual deployment (zapomene se verify)
```

**User Frustration:**  
> "skrze make se to moc neosvěčlo" - Make targets jsou neintuitivní, pomalé, bez progress feedbacku

### Goal

**Modern CI/CD Pipeline:**

```
GitHub Actions (Multi-Stage Pipeline)
  ├─ Stage 1: Syntax & Lint (2 min)
  │  ├─ Checkstyle (Java)
  │  ├─ ESLint (TypeScript)
  │  └─ Fail fast if syntax errors
  │
  ├─ Stage 2: Unit Tests (Parallel, 5 min)
  │  ├─ Backend: JUnit + Mockito
  │  ├─ Frontend: Jest + React Testing Library
  │  └─ Coverage reports to CodeCov
  │
  ├─ Stage 3: Integration Tests (8 min)
  │  ├─ Testcontainers (PostgreSQL, Redis, Kafka)
  │  ├─ Backend API tests
  │  └─ Database migration validation
  │
  ├─ Stage 4: Build Artifacts (Parallel, 3 min)
  │  ├─ Backend: Maven package (with cache!)
  │  ├─ Frontend: Vite build
  │  └─ Docker images (multi-stage)
  │
  ├─ Stage 5: E2E Tests (10 min)
  │  ├─ Deploy to ephemeral environment
  │  ├─ Playwright pre-deployment smoke
  │  ├─ Playwright post-deployment full
  │  └─ Cleanup environment
  │
  └─ Stage 6: Deploy (2 min)
     ├─ Production: Tag-based trigger
     ├─ Staging: Auto-deploy on main merge
     └─ Rollback: Previous Docker tag
```

**Total:** ~30 min (vs 40 min make clean), **parallel execution**, **fast feedback**

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Multi-Stage Pipeline**
   - 6 distinct stages (syntax, unit, integration, build, e2e, deploy)
   - Parallel job execution
   - Fail-fast on syntax/lint errors

2. ✅ **Test Gates**
   - Syntax check before unit tests
   - Unit tests before integration tests
   - E2E tests before production deploy

3. ✅ **Artifact Caching**
   - Maven `.m2` cache (save 3-5 min)
   - npm `.npm` cache (save 2-3 min)
   - Docker layer cache

4. ✅ **Deployment Automation**
   - Staging: Auto-deploy on main merge
   - Production: Manual approval + tag trigger
   - Rollback: One-click previous version

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: Multi-Stage Pipeline](subtasks/T1-multi-stage-pipeline.md) | 6h | none |
| 2 | [T2: Artifact Caching](subtasks/T2-artifact-caching.md) | 4h | T1 |
| 3 | [T3: Quality Gates](subtasks/T3-quality-gates.md) | 6h | T2 |
| 4 | [T4: Deployment Automation](subtasks/T4-deployment-automation.md) | 10h | T3 |
| 5 | [T5: Rollback Workflow](subtasks/T5-rollback-workflow.md) | 6h | T4 |

### Implementation

**File:** `.github/workflows/ci.yml` (Main pipeline)

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
  workflow_dispatch:
    inputs:
      environment:
        description: 'Deployment environment'
        required: true
        type: choice
        options:
          - staging
          - production

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
        run: ./mvnw test -Dtest='**/*Test.java' -DfailIfNoTests=false

      - name: Upload Coverage to CodeCov
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
        run: ./mvnw verify -Dtest='**/*IT.java' -DfailIfNoTests=false
        env:
          SPRING_PROFILES_ACTIVE: test

      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: integration-test-results
          path: backend/target/surefire-reports/

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

  build-frontend:
    needs: integration-tests
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

      - name: Build Production Bundle
        working-directory: ./frontend
        run: npm run build
        env:
          VITE_API_BASE: https://api.core-platform.com

      - name: Upload Artifact
        uses: actions/upload-artifact@v3
        with:
          name: frontend-dist
          path: frontend/dist/

  # ========================================
  # STAGE 5: DOCKER IMAGES
  # ========================================
  build-docker-images:
    needs: [build-backend, build-frontend]
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build Backend Image
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          file: ./docker/backend/Dockerfile
          push: true
          tags: |
            core-platform/backend:${{ github.sha }}
            core-platform/backend:latest
          cache-from: type=registry,ref=core-platform/backend:cache
          cache-to: type=registry,ref=core-platform/backend:cache,mode=max

      - name: Build Frontend Image
        uses: docker/build-push-action@v5
        with:
          context: ./frontend
          file: ./docker/frontend/Dockerfile
          push: true
          tags: |
            core-platform/frontend:${{ github.sha }}
            core-platform/frontend:latest
          cache-from: type=registry,ref=core-platform/frontend:cache
          cache-to: type=registry,ref=core-platform/frontend:cache,mode=max

  # ========================================
  # STAGE 6: E2E TESTS (Ephemeral Environment)
  # ========================================
  e2e-tests:
    needs: build-docker-images
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - uses: actions/checkout@v4

      - name: Create Ephemeral Environment
        run: |
          docker compose -f docker/docker-compose.yml \
            -f docker/docker-compose.ci.yml up -d
        env:
          BACKEND_IMAGE: core-platform/backend:${{ github.sha }}
          FRONTEND_IMAGE: core-platform/frontend:${{ github.sha }}

      - name: Wait for Services
        run: bash scripts/wait-for-services.sh

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}

      - name: Install Playwright
        working-directory: ./e2e
        run: |
          npm ci
          npx playwright install --with-deps

      - name: Run Pre-Deploy Smoke Tests
        working-directory: ./e2e
        run: npm run test:pre
        env:
          PRE_BASE_URL: http://localhost

      - name: Run Post-Deploy Full Tests
        working-directory: ./e2e
        run: npm run test:post
        env:
          POST_BASE_URL: http://localhost

      - name: Upload Playwright Report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: e2e/playwright-report/

      - name: Cleanup Environment
        if: always()
        run: docker compose -f docker/docker-compose.yml down -v

  # ========================================
  # STAGE 7: DEPLOY
  # ========================================
  deploy-staging:
    needs: e2e-tests
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - uses: actions/checkout@v4

      - name: Deploy to Staging
        run: |
          echo "🚀 Deploying to staging..."
          # SSH to staging server
          ssh ${{ secrets.STAGING_HOST }} "cd /app && \
            docker compose pull && \
            docker compose up -d --no-build"

      - name: Verify Deployment
        run: |
          bash scripts/verify-deployment.sh staging

  deploy-production:
    needs: e2e-tests
    if: startsWith(github.ref, 'refs/tags/v')
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4

      - name: Deploy to Production
        run: |
          echo "🚀 Deploying to production..."
          ssh ${{ secrets.PROD_HOST }} "cd /app && \
            docker compose pull && \
            docker compose up -d --no-build"

      - name: Verify Deployment
        run: bash scripts/verify-deployment.sh production

      - name: Create Rollback Point
        run: |
          git tag rollback-$(date +%Y%m%d-%H%M%S) ${{ github.sha }}
          git push --tags
```

**File:** `.github/workflows/rollback.yml` (One-click rollback)

```yaml
name: Rollback Deployment

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Environment to rollback'
        required: true
        type: choice
        options:
          - staging
          - production
      tag:
        description: 'Git tag to rollback to'
        required: true

jobs:
  rollback:
    runs-on: ubuntu-latest
    environment: ${{ inputs.environment }}
    steps:
      - uses: actions/checkout@v4
        with:
          ref: ${{ inputs.tag }}

      - name: Rollback Deployment
        run: |
          echo "⏪ Rolling back ${{ inputs.environment }} to ${{ inputs.tag }}"
          
          ssh ${{ secrets.HOST }} "cd /app && \
            git checkout ${{ inputs.tag }} && \
            docker compose pull && \
            docker compose up -d --no-build"

      - name: Verify Rollback
        run: bash scripts/verify-deployment.sh ${{ inputs.environment }}
```

**File:** `docker/docker-compose.ci.yml` (CI overrides)

```yaml
# Overrides for CI environment
services:
  backend:
    image: ${BACKEND_IMAGE}
    environment:
      SPRING_PROFILES_ACTIVE: ci

  frontend:
    image: ${FRONTEND_IMAGE}

  db:
    tmpfs:
      - /var/lib/postgresql/data  # In-memory DB for CI
```

**File:** `scripts/verify-deployment.sh`

```bash
#!/bin/bash
set -euo pipefail

ENVIRONMENT=$1
BASE_URL=""

case $ENVIRONMENT in
  staging)
    BASE_URL="https://staging.core-platform.com"
    ;;
  production)
    BASE_URL="https://core-platform.com"
    ;;
esac

echo "🔍 Verifying deployment at $BASE_URL..."

# Health checks
curl -f "$BASE_URL/api/actuator/health" || exit 1
curl -f "$BASE_URL/" || exit 1

echo "✅ Deployment verified"
```

**Effort:** 4 dny  
**LOC:** ~1100  
**Priority:** 🔥 CRITICAL

---

## 🧪 TESTING

### Pipeline Tests

**File:** `.github/workflows/test-pipeline.yml`

```yaml
name: Test Pipeline

on:
  pull_request:
    paths:
      - '.github/workflows/**'

jobs:
  validate-workflow:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Validate Workflow Syntax
        run: |
          yamllint .github/workflows/*.yml
```

---

## 📊 METRICS

**Pipeline Performance:**

| Stage | Duration | Parallelizable |
|-------|----------|----------------|
| Syntax & Lint | 2 min | ✅ |
| Unit Tests | 5 min | ✅ (backend + frontend parallel) |
| Integration Tests | 8 min | ❌ |
| Build Artifacts | 3 min | ✅ (backend + frontend parallel) |
| Docker Images | 4 min | ✅ (with cache) |
| E2E Tests | 10 min | ❌ |
| Deploy | 2 min | ❌ |

**Total:** ~30 min (vs 40 min `make clean`)

**Cache Savings:**
- Maven `.m2` cache: 3-5 min per build
- npm cache: 2-3 min per build
- Docker layer cache: 5-10 min per build

---

## 🔗 DEPENDENCIES

**Replaces:**
- Makefile targets (make deploy, make test-backend-full)

**Requires:**
- GitHub Actions runners
- Docker Hub credentials

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
