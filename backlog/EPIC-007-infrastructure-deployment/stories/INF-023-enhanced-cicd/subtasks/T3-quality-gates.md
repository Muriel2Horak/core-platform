# T3: Implement Test Gates & Quality Checks

**Parent Story:** INF-023 Enhanced CI/CD Pipeline  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 4 hours  
**Owner:** DevOps

---

## 🎯 Objective

Enforce quality gates: syntax → unit → integration → E2E with fail-fast and coverage thresholds.

---

## 📋 Tasks

### 1. Configure JaCoCo Coverage Enforcement

**File:** `backend/pom.xml`

```xml
<build>
  <plugins>
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
          <id>check-coverage</id>
          <phase>verify</phase>
          <goals>
            <goal>check</goal>
          </goals>
          <configuration>
            <rules>
              <rule>
                <element>BUNDLE</element>
                <limits>
                  <limit>
                    <counter>LINE</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.80</minimum> <!-- 80% line coverage -->
                  </limit>
                  <limit>
                    <counter>BRANCH</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.75</minimum> <!-- 75% branch coverage -->
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

### 2. Configure Jest Coverage Thresholds

**File:** `frontend/package.json`

```json
{
  "jest": {
    "coverageThreshold": {
      "global": {
        "lines": 80,
        "functions": 75,
        "branches": 75,
        "statements": 80
      }
    }
  },
  "scripts": {
    "test:ci": "jest --ci --coverage --maxWorkers=2",
    "test:coverage": "jest --coverage && open coverage/lcov-report/index.html"
  }
}
```

### 3. Create Quality Gate Workflow

**File:** `.github/workflows/ci.yml`

```yaml
  quality-gate:
    needs: [unit-tests-backend, unit-tests-frontend]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # =====================================
      # Gate 1: Code Coverage
      # =====================================
      - name: Download Coverage Reports
        uses: actions/download-artifact@v3
        with:
          name: coverage-reports
          path: ./coverage

      - name: Coverage Gate (Backend)
        run: |
          COVERAGE=$(grep -oP 'Total.*?(\d+)%' coverage/backend/index.html | grep -oP '\d+')
          if [ "$COVERAGE" -lt 80 ]; then
            echo "❌ Backend coverage $COVERAGE% < 80%"
            exit 1
          fi
          echo "✅ Backend coverage: $COVERAGE%"

      - name: Coverage Gate (Frontend)
        run: |
          COVERAGE=$(jq -r '.total.lines.pct' coverage/frontend/coverage-summary.json)
          if [ "$COVERAGE" -lt 80 ]; then
            echo "❌ Frontend coverage $COVERAGE% < 80%"
            exit 1
          fi
          echo "✅ Frontend coverage: $COVERAGE%"

      # =====================================
      # Gate 2: Security Vulnerabilities
      # =====================================
      - name: Security Scan (Trivy)
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'  # Fail on HIGH/CRITICAL

      # =====================================
      # Gate 3: Dependency Vulnerabilities
      # =====================================
      - name: Dependency Check (Maven)
        working-directory: ./backend
        run: ./mvnw dependency-check:check

      - name: Dependency Check (NPM Audit)
        working-directory: ./frontend
        run: |
          npm audit --audit-level=high || exit 1

      # =====================================
      # Gate 4: E2E Pre-Deploy Smoke Tests
      # =====================================
      - name: Run E2E Smoke Tests
        run: make test-e2e-pre
        timeout-minutes: 10
```

### 4. Create Coverage Badge

**File:** `.github/workflows/ci.yml` (add after tests)

```yaml
  coverage-badge:
    needs: quality-gate
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4

      - name: Generate Coverage Badge
        uses: codecov/codecov-action@v3
        with:
          token: ${{ secrets.CODECOV_TOKEN }}
          files: ./coverage/backend/jacoco.xml,./coverage/frontend/lcov.info
          flags: all
          fail_ci_if_error: true
```

**Add to README.md:**

```markdown
[![codecov](https://codecov.io/gh/USER/REPO/branch/main/graph/badge.svg)](https://codecov.io/gh/USER/REPO)
```

### 5. Create Failed Quality Gate Alert

**File:** `.github/workflows/quality-gate-failed.yml`

```yaml
name: Quality Gate Failed Notification

on:
  workflow_run:
    workflows: ["CI/CD Pipeline"]
    types: [completed]

jobs:
  notify-failure:
    if: ${{ github.event.workflow_run.conclusion == 'failure' }}
    runs-on: ubuntu-latest
    steps:
      - name: Send Slack Alert
        uses: slackapi/slack-github-action@v1
        with:
          webhook-url: ${{ secrets.SLACK_WEBHOOK }}
          payload: |
            {
              "text": "❌ Quality Gate Failed",
              "blocks": [
                {
                  "type": "section",
                  "text": {
                    "type": "mrkdwn",
                    "text": "*Quality Gate Failed*\n\nWorkflow: ${{ github.event.workflow_run.name }}\nBranch: ${{ github.event.workflow_run.head_branch }}\nCommit: ${{ github.event.workflow_run.head_sha }}"
                  }
                }
              ]
            }
```

### 6. Test Quality Gates

```bash
# Test coverage threshold failure
cd backend
./mvnw test jacoco:check
# Expected: BUILD FAILURE if coverage < 80%

# Test security scan
trivy fs --severity HIGH,CRITICAL .
# Expected: Exit code 1 if vulnerabilities found

# Test E2E smoke
make test-e2e-pre
# Expected: Fail fast on broken endpoints
```

---

## ✅ Acceptance Criteria

- [ ] Backend coverage threshold 80% enforced (JaCoCo)
- [ ] Frontend coverage threshold 80% enforced (Jest)
- [ ] Security scan fails on HIGH/CRITICAL vulnerabilities
- [ ] Dependency audit fails on vulnerable packages
- [ ] E2E smoke tests run before full E2E suite
- [ ] Failed gate sends Slack notification
- [ ] Coverage badge visible in README

---

## 🔗 Dependencies

- **BLOCKS:** T4 (Deployment Automation)
- **REQUIRES:** T1 (Multi-Stage Pipeline), T2 (Caching)
