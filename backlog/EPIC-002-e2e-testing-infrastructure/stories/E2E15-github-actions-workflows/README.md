---
id: E2E15
epic: EPIC-002-e2e-testing-infrastructure
title: "GitHub Actions CI/CD Workflows Documentation"
priority: P1
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "4 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-002-e2e-testing-infrastructure/stories/E2E15-github-actions-workflows/README.md
    - backlog/EPIC-002-e2e-testing-infrastructure/README.md
---

# E2E15: GitHub Actions CI/CD Workflows Documentation

**Status:** 📝 **DOCUMENTATION**  
**Effort:** 4 hodiny  
**Priority:** 🔥 HIGH  
**Category:** CI/CD Pipeline

---

## 📖 Overview

Kompletní dokumentace všech GitHub Actions workflows, jejich účelu, kdy se spouští, a jak je dočasně vypnout během vývoje.

---

## 🎯 Acceptance Criteria

- ✅ Dokumentace všech workflow souborů v `.github/workflows-disabled/`
- ✅ Vysvětlení trigger conditions (push, PR, schedule)
- ✅ Popis jednotlivých jobs a jejich závislostí
- ✅ Návod jak dočasně vypnout/zapnout workflows
- ✅ Best practices pro CI/CD v projektu
- ✅ Troubleshooting guide (časté problémy)

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Inventura workflow souboru a jejich stavu | 1h | none |
| 2 | Detailni popis triggeru/jobu + dependencies | 2h | Task 1 |
| 3 | Navod na enable/disable + troubleshooting | 1h | Task 2 |

---

## 📁 Workflow Files Inventory

**Lokace:** `.github/workflows-disabled/` (vypnuté během vývoje)

| Workflow | Spouští se | Trvání | Účel |
|----------|-----------|--------|------|
| `ci.yml` | Push na `main`, PR | 15-20 min | Main CI pipeline (build + unit tests) |
| `pre-deploy.yml` | Push na `main` | 5-7 min | Pre-deploy smoke tests |
| `post-deploy.yml` | Po deploy | 20-30 min | Post-deploy full E2E tests |
| `e2e.yml` | PR, manuálně | 20-30 min | Full E2E test suite |
| `code-quality.yml` | PR | 5 min | Linting, formatting, SonarQube |
| `security-scan.yml` | Push, schedule (weekly) | 10 min | OWASP, dependency check |
| `naming-lint.yml` | PR | 1 min | Java naming conventions |
| `smoke.yml` | Push na `main` | 3-5 min | Quick health check |
| `reporting-tests.yml` | PR | 10 min | Reporting module tests |
| `streaming-tests.yml` | PR | 8 min | Kafka CDC tests |
| `tests-monitoring-bff.yml` | PR | 5 min | Monitoring BFF tests |
| `ai-preflight.yml` | PR | 2 min | AI/Copilot checks |
| `grafana-provisioning.yml` | Push | 3 min | Grafana dashboard validation |

---

## 🔧 Workflow Details

### 1. `ci.yml` - Main CI Pipeline

**Trigger:**
```yaml
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
```

**Jobs:**
```yaml
jobs:
  validate-env:
    - Check .env.template exists
    - Validate required environment variables
    - Run make env-validate
  
  backend-build:
    - mvn clean package -DskipTests
    - Build Docker image
    - Cache Maven dependencies
  
  backend-tests:
    - mvn test (unit tests)
    - JaCoCo coverage report
    - Fail if coverage < 70%
  
  frontend-build:
    - npm ci
    - npm run build
    - npm run typecheck
  
  frontend-tests:
    - npm run test (Vitest)
    - Coverage report
    - Fail if coverage < 80%
  
  integration-tests:
    depends-on: [backend-build, frontend-build]
    - docker compose up -d
    - Wait for services (make wait-for-services)
    - REST Assured API tests
    - Testcontainers (DB, Kafka, Redis)
```

**Environment Variables:**
```yaml
env:
  JAVA_VERSION: '21'
  NODE_VERSION: '20'
  POSTGRES_VERSION: '15'
  DATABASE_URL: jdbc:postgresql://localhost:5432/core_test
  KEYCLOAK_BASE_URL: http://localhost:8080
```

**Success Criteria:**
- ✅ All tests pass
- ✅ Coverage >= threshold
- ✅ No security vulnerabilities (high/critical)
- ✅ Docker images build successfully

---

### 2. `pre-deploy.yml` - Pre-Deploy Smoke Tests

**Trigger:**
```yaml
on:
  push:
    branches: [main]
  workflow_dispatch:  # Manual trigger
```

**Purpose:** Fast feedback BEFORE deployment (5-7 min)

**Tests:**
```typescript
// e2e/specs/smoke/
- authentication.spec.ts      // Login/logout
- core-entities.spec.ts        // CRUD operations
- workflow-basic.spec.ts       // State transitions
- monitoring-health.spec.ts    // /actuator/health
```

**Jobs:**
```yaml
jobs:
  smoke-tests:
    - docker compose up -d
    - Wait for backend (120s timeout)
    - npx playwright test --grep @smoke
    - Upload report artifacts
```

**Failure Handling:**
- ❌ Smoke fail → **BLOCK deployment**
- 📧 Notify team via Slack/email
- 📊 Generate HTML report

---

### 3. `post-deploy.yml` - Post-Deploy Full E2E

**Trigger:**
```yaml
on:
  deployment_status:
    types: [success]
  workflow_dispatch:
```

**Purpose:** Comprehensive validation AFTER deployment (20-30 min)

**Test Suites:**
```typescript
// e2e/specs/
- admin/        // Admin UI tests
- auth/         // SSO, roles, permissions
- entities/     // All CRUD operations
- workflow/     // Complex state machines
- monitoring/   // Grafana, Loki integration
- reporting/    // Cube.js queries
- a11y/         // Accessibility (Axe-core)
```

**Jobs:**
```yaml
jobs:
  full-e2e:
    strategy:
      matrix:
        browser: [chromium, firefox, webkit]
    steps:
      - npx playwright test --project=${{ matrix.browser }}
      - Upload screenshots/videos on failure
      - Generate Allure report
```

**Artifacts:**
- 📹 Videos of failed tests
- 📸 Screenshots at failure point
- 📊 Allure HTML report
- 📝 Test execution logs

---

### 4. `e2e.yml` - Full E2E Test Suite

**Trigger:**
```yaml
on:
  pull_request:
    paths:
      - 'frontend/**'
      - 'e2e/**'
  workflow_dispatch:
```

**Purpose:** Run full E2E tests on PRs touching UI/tests

**Optimizations:**
- 🎯 **Sharding:** Split tests across 4 workers
  ```yaml
  strategy:
    matrix:
      shard: [1, 2, 3, 4]
  steps:
    - npx playwright test --shard=${{ matrix.shard }}/4
  ```
- ⚡ **Caching:** node_modules, Playwright browsers
- 🔄 **Retry:** Flaky tests retry 2x before fail

---

### 5. `code-quality.yml` - Linting & Static Analysis

**Trigger:**
```yaml
on:
  pull_request:
```

**Jobs:**
```yaml
jobs:
  backend-quality:
    - mvn spotless:check         # Code formatting
    - mvn checkstyle:check       # Style violations
    - mvn pmd:check              # Code quality
    - SonarQube scan
  
  frontend-quality:
    - npm run lint               # ESLint
    - npm run format:check       # Prettier
    - npm run typecheck          # TypeScript
```

**Quality Gates:**
- ❌ Fail if: New code smells > 5
- ❌ Fail if: Coverage drops > 2%
- ❌ Fail if: Security hotspots found
- ⚠️ Warn if: Code duplication > 3%

---

### 6. `security-scan.yml` - Security Audits

**Trigger:**
```yaml
on:
  push:
    branches: [main]
  schedule:
    - cron: '0 2 * * 1'  # Every Monday 2 AM
```

**Scans:**
```yaml
jobs:
  dependency-check:
    - OWASP Dependency Check
    - npm audit (frontend)
    - mvn dependency:check (backend)
  
  code-scan:
    - Snyk code analysis
    - Trivy container scan
    - GitGuardian secrets detection
  
  license-compliance:
    - Check for GPL/AGPL licenses
    - Verify MIT/Apache compatibility
```

**Failure Thresholds:**
- 🔴 **CRITICAL:** Block merge, notify immediately
- 🟠 **HIGH:** Block merge, require fix
- 🟡 **MEDIUM:** Warn, allow merge with justification
- 🟢 **LOW:** Info only

---

## 🛠️ Managing Workflows (Enable/Disable)

### Disable ALL Workflows (During Development)

**Current State:** ✅ **DISABLED** (workflows in `workflows-disabled/`)

```bash
# Workflows are already disabled
ls -la .github/workflows/
# (empty directory)

ls -la .github/workflows-disabled/
# ai-preflight.yml
# ci.yml
# code-quality.yml
# e2e.yml
# ...
```

**Why disabled?**
- 🚧 Active development (EPIC-017 implementation)
- ⏱️ Save CI minutes (workflows can be expensive)
- 🔧 Prevent false failures during refactoring

---

### Enable Workflows (After Implementation Complete)

**Option 1: Enable All**
```bash
# Move all workflows back
mv .github/workflows-disabled/*.yml .github/workflows/

# Commit
git add .github/workflows/
git commit -m "ci: Re-enable GitHub Actions workflows"
git push
```

**Option 2: Enable Selectively (Recommended)**
```bash
# Enable only critical workflows first
mv .github/workflows-disabled/ci.yml .github/workflows/
mv .github/workflows-disabled/security-scan.yml .github/workflows/
mv .github/workflows-disabled/pre-deploy.yml .github/workflows/

# Test for a few days, then enable rest
mv .github/workflows-disabled/e2e.yml .github/workflows/
mv .github/workflows-disabled/post-deploy.yml .github/workflows/
```

**Option 3: Disable Specific Workflow Temporarily**
```yaml
# In workflow file, add condition
on:
  push:
    branches: [main]

jobs:
  test:
    if: false  # <-- DISABLE
    runs-on: ubuntu-latest
    steps:
      - run: echo "Disabled"
```

---

### Skip Workflows on Specific Commits

**Method 1: Commit Message**
```bash
git commit -m "docs: Update README [skip ci]"
git commit -m "wip: Work in progress [ci skip]"
```

**Method 2: Path Filtering**
```yaml
on:
  push:
    paths-ignore:
      - '**.md'       # Ignore markdown changes
      - 'docs/**'     # Ignore docs folder
      - '*.txt'       # Ignore text files
```

---

## 📊 Workflow Orchestration

### Dependency Graph

```
┌─────────────┐
│  ci.yml     │ ◄─── On every push/PR
└──────┬──────┘
       │
       ├─► backend-build
       │   └─► backend-tests
       │       └─► integration-tests
       │
       └─► frontend-build
           └─► frontend-tests
               └─► e2e (if paths match)

┌─────────────┐
│ pre-deploy  │ ◄─── On push to main
└──────┬──────┘
       │
       └─► smoke-tests (5-7 min)
           └─► ✅ PASS → Trigger deployment
               └─► deployment successful
                   └─► post-deploy.yml (20-30 min)

┌─────────────┐
│ security    │ ◄─── Weekly schedule + push
└──────┬──────┘
       │
       └─► dependency-check
           └─► code-scan
               └─► license-compliance
```

---

## 🐛 Troubleshooting

### Common Issues

**1. Workflow doesn't trigger**
```yaml
# Check trigger conditions
on:
  push:
    branches: [main]  # ← Are you pushing to main?
    paths:
      - 'backend/**'  # ← Does your change match paths?
```

**Solution:**
- Verify branch name matches
- Check path filters
- Use `workflow_dispatch` for manual trigger

---

**2. Tests timeout**
```yaml
jobs:
  test:
    timeout-minutes: 30  # ← Default 6h, reduce to fail fast
```

**Solution:**
- Set realistic timeouts
- Check for hanging processes (DB not starting)
- Review logs: Actions → Failed job → View raw logs

---

**3. Environment variables not set**
```yaml
env:
  DATABASE_URL: ${{ secrets.DATABASE_URL }}  # ← Secret not defined?
```

**Solution:**
- GitHub → Settings → Secrets and variables → Actions
- Add missing secrets
- For non-sensitive: Use `env:` in workflow

---

**4. Docker Compose fails**
```yaml
- name: Start services
  run: docker compose up -d
  env:
    COMPOSE_FILE: docker/docker-compose.yml  # ← Specify path
```

**Solution:**
- Use absolute paths
- Check `.env` file exists (copy from `.env.template`)
- Verify Docker daemon running

---

**5. Flaky E2E tests**
```typescript
// Use built-in retry
test.describe.configure({ retries: 2 });

test('flaky test', async ({ page }) => {
  await expect(page.locator('...')).toBeVisible({ timeout: 10000 });
});
```

**Solution:**
- Increase timeouts
- Add explicit waits
- Use `test.describe.configure({ retries: 2 })`

---

## 📈 Best Practices

### 1. Fast Feedback Loop

```yaml
# Run quick checks first, slow tests last
jobs:
  lint:           # 1-2 min ✅
    runs-on: ubuntu-latest
  
  unit-tests:     # 5 min ✅
    needs: [lint]
  
  integration:    # 15 min ⏱️
    needs: [unit-tests]
  
  e2e:            # 30 min 🐌
    needs: [integration]
```

**Benefit:** Fail fast on linting errors, don't waste time on E2E

---

### 2. Cache Dependencies

```yaml
- uses: actions/cache@v3
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
    restore-keys: |
      ${{ runner.os }}-maven-

- uses: actions/cache@v3
  with:
    path: ~/.npm
    key: ${{ runner.os }}-node-${{ hashFiles('**/package-lock.json') }}
```

**Benefit:** 5-10 min saved per run

---

### 3. Matrix Strategy for Parallel Tests

```yaml
strategy:
  matrix:
    os: [ubuntu-latest, windows-latest]
    node: [18, 20]
    browser: [chromium, firefox, webkit]
jobs:
  test:
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/setup-node@v3
        with:
          node-version: ${{ matrix.node }}
```

**Benefit:** Test multiple configurations in parallel

---

### 4. Conditional Job Execution

```yaml
jobs:
  deploy:
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest
  
  preview-deploy:
    if: github.event_name == 'pull_request'
    runs-on: ubuntu-latest
```

**Benefit:** Different workflows for PR vs main branch

---

### 5. Artifacts for Debugging

```yaml
- name: Upload test results
  if: failure()
  uses: actions/upload-artifact@v3
  with:
    name: playwright-report
    path: playwright-report/
    retention-days: 7

- name: Upload screenshots
  if: failure()
  uses: actions/upload-artifact@v3
  with:
    name: screenshots
    path: e2e/screenshots/
```

**Benefit:** Download artifacts to debug failures locally

---

## 🔐 Secrets Management

**Required Secrets:**
```
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
SONAR_TOKEN
SNYK_TOKEN
SLACK_WEBHOOK_URL
```

**How to add:**
1. GitHub → Settings → Secrets and variables → Actions
2. New repository secret
3. Name: `SONAR_TOKEN`
4. Value: `<your-token>`
5. Add secret

**Usage in workflow:**
```yaml
env:
  SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

---

## 📅 Scheduled Workflows

```yaml
on:
  schedule:
    - cron: '0 2 * * 1'  # Every Monday 2 AM (UTC)
```

**Use cases:**
- 🔒 Weekly security scans
- 📊 Dependency updates check (Dependabot)
- 🧹 Cleanup old artifacts
- 📈 Performance regression tests

---

## 🚀 Deployment Workflows

### Pre-Deploy Checklist

```yaml
jobs:
  pre-deploy-checks:
    - ✅ All tests pass
    - ✅ Coverage >= threshold
    - ✅ No security vulnerabilities
    - ✅ Smoke tests pass
    - ✅ Database migrations valid
    - ✅ Environment variables validated
```

### Deploy Flow

```
1. pre-deploy.yml (smoke tests)
   ↓
2. If PASS → Trigger deployment
   ↓
3. Deployment job (Docker push, K8s apply)
   ↓
4. post-deploy.yml (full E2E)
   ↓
5. If FAIL → Rollback
```

### Rollback Strategy

```yaml
- name: Rollback on failure
  if: failure()
  run: |
    kubectl rollout undo deployment/core-backend
    echo "Deployment rolled back"
```

---

## 📚 References

- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Playwright CI](https://playwright.dev/docs/ci)
- [Docker Compose in CI](https://docs.docker.com/compose/ci/)
- [SonarQube GitHub Action](https://github.com/SonarSource/sonarqube-scan-action)

---

**Last Updated:** 9. listopadu 2025  
**Status:** 📝 Documentation complete  
**Next Steps:** Enable workflows after EPIC-017 implementation
