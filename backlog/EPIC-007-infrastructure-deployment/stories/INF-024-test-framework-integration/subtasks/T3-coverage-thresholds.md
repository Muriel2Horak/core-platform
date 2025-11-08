# T3: Enforce Code Coverage Thresholds

**Parent Story:** INF-024 Test Framework Integration  
**Status:** 🔴 TODO  
**Priority:** 🔥 HIGH  
**Effort:** 2 hours  
**Owner:** DevOps

---

## 🎯 Objective

Enforce 80% coverage threshold for backend (JaCoCo) and frontend (Jest), failing builds on violations.

---

## 📋 Tasks

### 1. Configure JaCoCo (Backend)

**File:** `backend/pom.xml`

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <version>0.8.11</version>
      <executions>
        <!-- Prepare agent before tests -->
        <execution>
          <id>prepare-agent</id>
          <goals>
            <goal>prepare-agent</goal>
          </goals>
        </execution>

        <!-- Generate report after tests -->
        <execution>
          <id>report</id>
          <phase>test</phase>
          <goals>
            <goal>report</goal>
          </goals>
        </execution>

        <!-- Enforce coverage thresholds -->
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
                  <!-- Line coverage: 80% -->
                  <limit>
                    <counter>LINE</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.80</minimum>
                  </limit>

                  <!-- Branch coverage: 75% -->
                  <limit>
                    <counter>BRANCH</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.75</minimum>
                  </limit>

                  <!-- Method coverage: 70% -->
                  <limit>
                    <counter>METHOD</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.70</minimum>
                  </limit>
                </limits>
              </rule>

              <!-- Per-package rules -->
              <rule>
                <element>PACKAGE</element>
                <limits>
                  <limit>
                    <counter>LINE</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.75</minimum>
                  </limit>
                </limits>
              </rule>
            </rules>
          </configuration>
        </execution>
      </executions>

      <configuration>
        <!-- Exclude generated code -->
        <excludes>
          <exclude>**/config/**</exclude>
          <exclude>**/dto/**</exclude>
          <exclude>**/*Application.class</exclude>
        </excludes>
      </configuration>
    </plugin>
  </plugins>
</build>
```

### 2. Configure Jest Coverage (Frontend)

**File:** `frontend/package.json`

```json
{
  "scripts": {
    "test": "jest",
    "test:ci": "jest --ci --coverage --maxWorkers=2",
    "test:coverage": "jest --coverage && open coverage/lcov-report/index.html"
  },
  "jest": {
    "preset": "ts-jest",
    "testEnvironment": "jsdom",
    "setupFilesAfterEnv": ["<rootDir>/src/test/setup.ts"],
    "collectCoverageFrom": [
      "src/**/*.{ts,tsx}",
      "!src/**/*.d.ts",
      "!src/main.tsx",
      "!src/vite-env.d.ts",
      "!src/test/**"
    ],
    "coverageThreshold": {
      "global": {
        "lines": 80,
        "functions": 75,
        "branches": 75,
        "statements": 80
      }
    },
    "coverageReporters": ["text", "lcov", "html", "json-summary"]
  }
}
```

### 3. Create Coverage Report Script

**File:** `scripts/test/coverage-report.sh`

```bash
#!/bin/bash
set -e

echo "📊 Code Coverage Report"
echo "======================="

# Backend coverage
echo ""
echo "☕ Backend (JaCoCo)"
cd backend
./mvnw clean test jacoco:report

BACKEND_COVERAGE=$(grep -oP 'Total.*?(\d+)%' target/site/jacoco/index.html | grep -oP '\d+' || echo "0")
echo "Backend line coverage: $BACKEND_COVERAGE%"

if [ "$BACKEND_COVERAGE" -lt 80 ]; then
  echo "❌ Backend coverage $BACKEND_COVERAGE% < 80% threshold"
  BACKEND_FAIL=1
else
  echo "✅ Backend coverage meets threshold"
  BACKEND_FAIL=0
fi

# Frontend coverage
echo ""
echo "⚛️ Frontend (Jest)"
cd ../frontend
npm run test:ci

FRONTEND_COVERAGE=$(jq -r '.total.lines.pct' coverage/coverage-summary.json)
echo "Frontend line coverage: $FRONTEND_COVERAGE%"

if (( $(echo "$FRONTEND_COVERAGE < 80" | bc -l) )); then
  echo "❌ Frontend coverage $FRONTEND_COVERAGE% < 80% threshold"
  FRONTEND_FAIL=1
else
  echo "✅ Frontend coverage meets threshold"
  FRONTEND_FAIL=0
fi

# Summary
echo ""
echo "📈 Coverage Summary"
echo "==================="
echo "Backend:  $BACKEND_COVERAGE% (threshold: 80%)"
echo "Frontend: $FRONTEND_COVERAGE% (threshold: 80%)"

if [ $BACKEND_FAIL -eq 1 ] || [ $FRONTEND_FAIL -eq 1 ]; then
  echo ""
  echo "❌ Coverage thresholds not met - build failed"
  exit 1
else
  echo ""
  echo "✅ All coverage thresholds met"
  exit 0
fi
```

### 4. Add Coverage Check to CI/CD

**File:** `.github/workflows/ci.yml`

```yaml
  coverage-check:
    needs: [unit-tests-backend, unit-tests-frontend]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Run Coverage Report
        run: bash scripts/test/coverage-report.sh

      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./backend/target/site/jacoco/jacoco.xml,./frontend/coverage/lcov.info
          fail_ci_if_error: true
```

### 5. Test Coverage Enforcement

```bash
# Test backend coverage threshold
cd backend
./mvnw clean verify

# Expected output if < 80%:
# [ERROR] Rule violated for bundle core-platform: lines covered ratio is 0.75, but expected minimum is 0.80
# BUILD FAILURE

# Test frontend coverage threshold
cd frontend
npm run test:ci

# Expected output if < 80%:
# Jest: "global" coverage threshold for lines (80%) not met: 75%
# npm ERR! Test failed

# Test combined coverage script
bash scripts/test/coverage-report.sh
```

---

## ✅ Acceptance Criteria

- [ ] JaCoCo configured with 80% line, 75% branch, 70% method coverage
- [ ] Jest configured with 80% coverage threshold
- [ ] Maven build fails if backend coverage < 80%
- [ ] NPM test fails if frontend coverage < 80%
- [ ] Coverage reports uploaded to CodeCov
- [ ] Coverage badge visible in README
- [ ] CI/CD pipeline enforces thresholds

---

## 🔗 Dependencies

- **REQUIRES:** T2 (Testcontainers)
- **BLOCKS:** T4 (E2E Framework)
