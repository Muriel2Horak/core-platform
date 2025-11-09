# EPIC-020: Secure SDLC & Quality Gates

**Status:** 🔴 **0% IMPLEMENTED**  
**Priority:** P0 (CROSS-CUTTING BASELINE)  
**Owner:** Security + DevOps + Quality  
**Created:** 9. listopadu 2025  
**Dependencies:** EPIC-000 (Security), EPIC-002 (E2E), EPIC-003 (Monitoring), EPIC-007 (Infra), EPIC-012 (Vault)

---

## 🎯 Vize

**Každý merge do main větve i každý build je automaticky kontrolován** z hlediska:
- 🔒 **Bezpečnosti** (závislosti, kontejnery, kód)
- 📊 **Kvality** (coverage, code smells, duplikace)
- ✅ **Compliance** (OWASP Top 10, secret handling, hardcoded credentials)

**Cokoliv, co neprojde quality gate, se nedostane do releasu.**

### Proč tento EPIC?

1. **AI-Generated Code Risk**
   - Část kódu (metamodel, workflow templates, integrace) je generovaná AI/Copilot
   - Potřebujeme tvrdé automatické kontroly proti škodlivému/nekvalitnímu výstupu
   - Human review NENÍ dostatečný (rychlost iterací, lidská chyba)

2. **Enterprise Readiness**
   - Virelio/Core Platform je určen pro enterprise použití
   - Vyžaduje auditovatelný Secure SDLC
   - Compliance s OWASP, NIST, security best practices

3. **Continuous Assurance**
   - Security a quality NEJSOU one-time activity
   - Kontroly běží na každém PR, nightly, release
   - Výsledky logované do Loki pro audit trail

---

## 🏛️ Scope & Boundaries

### CO TENTO EPIC ŘEŠÍ

✅ **Statická analýza kódu (SAST)**
- Java/Spring Boot backend
- TypeScript/React frontend
- YAML/Docker/IaC konfigurace

✅ **Dependency & container scanning (SCA + Image Scanning)**
- Maven dependencies (OWASP Dependency-Check)
- Docker images (Trivy)
- Kubernetes manifests (Trivy/kube-linter)

✅ **Secret scanning & secret policy**
- GitLeaks/TruffleHog pro repo skenování
- Pre-commit hooks, PR checks, periodic scans

✅ **DAST / aplikační bezpečnostní testy (lightweight)**
- OWASP ZAP headless (nightly/on-demand)
- Proti lokálnímu prostředí (core-platform.local)

✅ **Infrastructure as Code & config lint**
- Dockerfile (hadolint)
- Nginx config (custom lint)
- K8s manifests (kube-linter/kube-score)
- GitHub Actions (actionlint)

✅ **CI orchestration**
- Definice co běží kdy (PR, nightly, release)
- Integrace do GitHub Actions
- Quality gate enforcement

✅ **AI-generated code guardrails**
- Checklist pro security relevantní kód
- Dokumentace co vyžaduje ruční review
- Automatické detekce rizikových patterns

### CO TENTO EPIC NEŘEŠÍ

❌ **Architektura bezpečnosti** → to je EPIC-000 (Keycloak, RBAC, multitenancy)  
❌ **Runtime monitoring/observability** → to je EPIC-003 (Loki, Prometheus, Grafana)  
❌ **Secrets management** → to je EPIC-012 (Vault integration)  
❌ **E2E test framework** → to je EPIC-002 (Playwright POM, tagging)

**Tento EPIC pouze doplňuje kontroly nad těmito oblastmi, ne jejich implementaci.**

---

## 🔗 Vztah k Ostatním EPICům

```text
┌─────────────────────────────────────────────────────────────┐
│ EPIC-020: Secure SDLC & Quality Gates (CROSS-CUTTING)      │
│ - Kontroluje výstupy všech ostatních EPICů                 │
│ - Vynucuje quality baseline                                │
└────────────┬────────────────────────────────────────────────┘
             │
    ┌────────┼────────┬────────────┬───────────┬──────────┐
    ▼        ▼        ▼            ▼           ▼          ▼
┌────────┐ ┌────┐ ┌──────┐ ┌──────────┐ ┌────────┐ ┌────────┐
│EPIC-000│ │002 │ │ 003  │ │   007    │ │  012   │ │  017   │
│Security│ │E2E │ │Monitor│ │  Infra   │ │ Vault  │ │Modular │
└────────┘ └────┘ └──────┘ └──────────┘ └────────┘ └────────┘
```

**EPIC-000 (Security):**
- Definuje bezpečnostní architekturu (Keycloak realms, RBAC, tenant isolation)
- EPIC-020 KONTROLUJE dodržování těchto principů v kódu (SAST, linting)

**EPIC-002 (E2E Testing):**
- Definuje E2E framework (Playwright, POM, tagging)
- EPIC-020 ORCHESTRUJE kdy E2E testy běží (PR smoke vs. nightly full)

**EPIC-003 (Monitoring):**
- Poskytuje Loki pro centralizované logy
- EPIC-020 LOGUJE všechny security/quality findings do Loki

**EPIC-007 (Infrastructure):**
- Definuje Docker Compose, Nginx, K8s setup
- EPIC-020 LINTUJE Dockerfiles, Nginx config, K8s manifests

**EPIC-012 (Vault):**
- Řeší secret management (KV, PKI, rotation)
- EPIC-020 SKENUJE repo proti plaintext secrets, vynucuje Vault usage

**EPIC-017 (Modular Architecture):**
- Moduly nad CORE platformou
- EPIC-020 VALIDUJE že moduly dodržují quality gates (ne jen CORE)

---

## 📋 Quality Gates & Pipeline Flow

### PR Pipeline (Mandatory - Blocking Merge)

```bash
┌─────────────────────────────────────────────────────────────┐
│ Pull Request → main                                         │
└───────────────────┬─────────────────────────────────────────┘
                    │
    ┌───────────────┼───────────────┬───────────────┐
    ▼               ▼               ▼               ▼
┌─────────┐  ┌───────────┐  ┌──────────┐  ┌──────────────┐
│ Unit    │  │ SAST      │  │ SCA      │  │ Secret Scan  │
│ Tests   │  │ (SonarQube│  │ (OWASP DC│  │ (GitLeaks)   │
│         │  │ /CodeQL)  │  │  Trivy)  │  │              │
└─────────┘  └───────────┘  └──────────┘  └──────────────┘
    │               │               │               │
    └───────────────┴───────────────┴───────────────┘
                    │
                    ▼
            ┌───────────────┐
            │ Quality Gate  │
            │ Decision      │
            └───────┬───────┘
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
    ✅ PASS                  ❌ FAIL
    Merge allowed           Block merge
                            Comment PR
```

**PR Quality Gates (Blocking):**
- ✅ Unit tests pass (95%+ coverage for new code)
- ✅ Integration tests pass
- ✅ SAST: No Critical/High vulnerabilities (new code)
- ✅ SAST: No blocker issues (SonarQube Quality Gate)
- ✅ SCA: No Critical/High CVEs in dependencies (or whitelisted)
- ✅ Container scan: No Critical/High CVEs in images (or whitelisted)
- ✅ Secret scan: No plaintext secrets detected
- ✅ IaC lint: No errors in Dockerfile/Nginx/K8s configs

### Nightly Pipeline (Full Regression)

```bash
┌─────────────────────────────────────────────────────────────┐
│ Scheduled: Every day 2am                                    │
└───────────────────┬─────────────────────────────────────────┘
                    │
    ┌───────────────┼───────────────┬───────────────┐
    ▼               ▼               ▼               ▼
┌─────────┐  ┌───────────┐  ┌──────────┐  ┌──────────────┐
│ E2E     │  │ DAST      │  │ Perf     │  │ Full Secret  │
│ Full    │  │ (OWASP ZAP│  │ Baseline │  │ Scan         │
│ (30 min)│  │  Headless)│  │          │  │ (repo-wide)  │
└─────────┘  └───────────┘  └──────────┘  └──────────────┘
    │               │               │               │
    └───────────────┴───────────────┴───────────────┘
                    │
                    ▼
            ┌───────────────┐
            │ Report to     │
            │ Loki + Slack  │
            └───────────────┘
```

**Nightly Gates (Non-blocking, Alerting):**
- 📊 E2E test results (trend tracking)
- 📊 DAST findings (OWASP Top 10 checks)
- 📊 Performance regression detection
- 📊 Full repo secret scan (catch accidental commits)

### Release Pipeline (Candidate Branch)

```bash
┌─────────────────────────────────────────────────────────────┐
│ release/* branch                                            │
└───────────────────┬─────────────────────────────────────────┘
                    │
    ┌───────────────┼───────────────┬───────────────┐
    ▼               ▼               ▼               ▼
┌─────────┐  ┌───────────┐  ┌──────────┐  ┌──────────────┐
│ E2E     │  │ SAST      │  │ SCA      │  │ Security     │
│ Smoke   │  │ Full Scan │  │ Full Scan│  │ Audit        │
│ (7 min) │  │ (all code)│  │ (all deps│  │ (checklist)  │
└─────────┘  └───────────┘  └──────────┘  └──────────────┘
    │               │               │               │
    └───────────────┴───────────────┴───────────────┘
                    │
                    ▼
            ┌───────────────┐
            │ Release       │
            │ Sign-off      │
            └───────────────┘
```

**Release Quality Gates (Strict):**
- ✅ All PR gates pass (on release branch)
- ✅ E2E smoke tests pass (critical paths)
- ✅ Full SAST scan clean (or exceptions documented)
- ✅ Full SCA scan clean (or exceptions documented)
- ✅ Security audit checklist completed (manual sign-off)

---

## 🛠️ Tools & Technology Stack

| Category | Tool | Purpose | Integration |
|----------|------|---------|-------------|
| **SAST** | SonarQube Community | Code quality + security (Java, TS) | GitHub Actions |
| **SAST (alt)** | GitHub CodeQL | Advanced security analysis | GitHub native |
| **SCA (dependencies)** | OWASP Dependency-Check | Maven dependency CVE scanning | Maven plugin |
| **Container Scanning** | Trivy | Docker image + filesystem CVE scan | GitHub Actions |
| **Secret Scanning** | GitLeaks | Detect plaintext secrets in repo | Pre-commit + PR |
| **DAST** | OWASP ZAP Headless | Dynamic web app security testing | Nightly job |
| **IaC Lint** | hadolint | Dockerfile best practices | GitHub Actions |
| **IaC Lint** | kube-linter | Kubernetes manifest validation | GitHub Actions |
| **IaC Lint** | yamllint | YAML syntax/style validation | Pre-commit |
| **IaC Lint** | actionlint | GitHub Actions workflow validation | Pre-commit |
| **Coverage** | JaCoCo (Java) | Unit test coverage reporting | Maven + SonarQube |
| **Coverage** | Vitest (TS) | Frontend test coverage | npm + SonarQube |
| **Audit Logging** | Loki | Centralized security findings log | EPIC-003 integration |

---

## 🎯 Success Metrics

- **Security Baseline:**
  - 0 Critical/High vulnerabilities in production
  - 0 plaintext secrets in repo (outside test fixtures)
  - 100% Docker images scanned before deployment

- **Quality Baseline:**
  - 80%+ line coverage for new code
  - 70%+ branch coverage for new code
  - 0 blocker SonarQube issues

- **Compliance:**
  - OWASP Top 10 coverage (DAST checks)
  - Audit trail: All security findings logged to Loki
  - Security checklist sign-off for releases

- **AI Code Governance:**
  - 100% AI-generated code reviewed against security checklist
  - 0 direct DB access in AI-generated workflows
  - 0 hardcoded credentials in AI outputs

---

## 📋 Stories

### SECQ1: Statická Analýza & Quality Gate (SonarQube/CodeQL) (~800 LOC, 3 days)

**Goal**: Deploy SonarQube Community Edition a nastavit quality gates pro Java + TypeScript

**Deliverables:**

1. **SonarQube Setup**
   - Docker Compose service: SonarQube + PostgreSQL
   - Dostupné na: `https://admin.core-platform.local/sonar`
   - Nginx reverse proxy config
   - Initial admin setup + quality profiles

2. **Backend Integration (Java/Spring Boot)**
   - Maven plugin: `sonar-maven-plugin`
   - JaCoCo coverage plugin (XML report)
   - Quality profile: "Sonar way" + custom security rules
   - Sonar properties:
     ```properties
     sonar.projectKey=core-platform-backend
     sonar.sources=src/main/java
     sonar.tests=src/test/java
     sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
     ```

3. **Frontend Integration (TypeScript/React)**
   - SonarScanner for JavaScript
   - Vitest coverage plugin (lcov report)
   - Quality profile: "Sonar way (TypeScript)"
   - Sonar properties:
     ```properties
     sonar.projectKey=core-platform-frontend
     sonar.sources=src
     sonar.tests=src/**/*.test.ts,src/**/*.spec.ts
     sonar.javascript.lcov.reportPaths=coverage/lcov.info
     ```

4. **Quality Gates**
   - Gate 1: No Critical/High vulnerabilities (new code)
   - Gate 2: No blocker issues
   - Gate 3: Line coverage ≥ 80% (new code)
   - Gate 4: Branch coverage ≥ 70% (new code)
   - Gate 5: Code duplication ≤ 3% (new code)

5. **GitHub Actions Integration**
   ```yaml
   # .github/workflows/pr-quality.yml
   - name: SonarQube Scan
     run: |
       mvn clean verify sonar:sonar \
         -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }} \
         -Dsonar.login=${{ secrets.SONAR_TOKEN }}
   
   - name: Quality Gate Check
     run: |
       status=$(curl "$SONAR_HOST_URL/api/qualitygates/project_status?projectKey=core-platform-backend" | jq -r '.projectStatus.status')
       if [ "$status" != "OK" ]; then
         echo "Quality gate failed!"
         exit 1
       fi
   ```

6. **Alternative: GitHub CodeQL**
   - Pokud SonarQube je příliš heavy, použít CodeQL (GitHub native)
   - `.github/workflows/codeql-analysis.yml`
   - Podporuje Java, TypeScript, YAML
   - Automaticky detekuje OWASP Top 10 issues

**Acceptance Criteria:**
- ✅ SonarQube běží na `https://admin.core-platform.local/sonar`
- ✅ Backend + frontend scan funguje (`mvn sonar:sonar`, `npm run sonar`)
- ✅ Quality gates blokují PR s critical issues
- ✅ Coverage reports viditelné v SonarQube UI
- ✅ GitHub Actions failne na quality gate failure

**Effort:** ~3 days | **Details:** [stories/SECQ1.md](./stories/SECQ1.md)

---

### SECQ2: Dependency & Container Scanning (OWASP DC, Trivy) (~600 LOC, 2 days)

**Goal**: Skenovat Maven dependencies a Docker images proti CVE databázím

**Deliverables:**

1. **OWASP Dependency-Check (Backend)**
   - Maven plugin: `dependency-check-maven`
   - Konfigurace:
     ```xml
     <plugin>
       <groupId>org.owasp</groupId>
       <artifactId>dependency-check-maven</artifactId>
       <version>9.0.0</version>
       <configuration>
         <failBuildOnCVSS>7</failBuildOnCVSS> <!-- High/Critical -->
         <suppressionFile>owasp-suppressions.xml</suppressionFile>
       </configuration>
     </plugin>
     ```
   - Whitelist: `owasp-suppressions.xml` (dokumentované false positives)

2. **Trivy Container Scanning**
   - GitHub Actions integration:
     ```yaml
     - name: Scan Backend Image
       uses: aquasecurity/trivy-action@master
       with:
         image-ref: core-platform/backend:latest
         format: 'sarif'
         output: 'trivy-results.sarif'
         severity: 'CRITICAL,HIGH'
     
     - name: Upload to GitHub Security
       uses: github/codeql-action/upload-sarif@v2
       with:
         sarif_file: 'trivy-results.sarif'
     ```
   - Scan images:
     - `core-platform/backend`
     - `core-platform/frontend`
     - `core-platform/nginx`
     - `quay.io/keycloak/keycloak:26.0` (base image check)

3. **Trivy Filesystem Scanning**
   - Scan Dockerfiles a configs:
     ```bash
     trivy fs --severity CRITICAL,HIGH ./docker/
     trivy config --severity CRITICAL,HIGH .
     ```

4. **CVE Whitelist & Exceptions**
   - `security/cve-exceptions.yml`:
     ```yaml
     exceptions:
       - cve: CVE-2023-12345
         reason: "False positive - not applicable to our usage"
         expires: 2025-12-31
         approved_by: "security-team"
     ```
   - Automatické expiry check (fail pokud expired)

5. **GitHub Actions Integration**
   - PR pipeline:
     ```yaml
     - name: Dependency Check
       run: mvn dependency-check:check
     
     - name: Trivy Scan
       run: |
         docker compose build backend
         trivy image --exit-code 1 --severity CRITICAL,HIGH core-platform/backend:latest
     ```

6. **Loki Logging**
   - Všechny findings logovat do Loki:
     ```json
     {
       "level": "warning",
       "service": "security-scanner",
       "scanner": "trivy",
       "cve": "CVE-2024-1234",
       "severity": "HIGH",
       "package": "spring-boot-starter-web:3.2.0",
       "fixed_version": "3.2.1"
     }
     ```

**Acceptance Criteria:**
- ✅ `mvn dependency-check:check` failne na HIGH/CRITICAL CVEs
- ✅ Trivy scan blokuje PR s unpatched CVEs
- ✅ CVE exceptions správně whitelistují known issues
- ✅ Findings viditelné v GitHub Security tab
- ✅ Všechny CVEs logovány do Loki

**Effort:** ~2 days | **Details:** [stories/SECQ2.md](./stories/SECQ2.md)

---

### SECQ3: Secret Scanning & Policies (GitLeaks/TruffleHog) (~400 LOC, 1.5 days)

**Goal**: Detekovat a blokovat plaintext secrets v repozitáři

**Deliverables:**

1. **GitLeaks Setup**
   - Installation: `brew install gitleaks` (macOS) nebo Docker image
   - Configuration: `.gitleaks.toml`
     ```toml
     [allowlist]
       paths = [
         "e2e/fixtures/test-secrets.json",  # Test data
         "docs/examples/*"                  # Documentation
       ]
       regexes = [
         "test.password",                   # Test pattern
         "KEYCLOAK_ADMIN=admin"             # Known dev default
       ]
     ```

2. **Pre-commit Hook**
   - `.git/hooks/pre-commit`:
     ```bash
     #!/bin/bash
     gitleaks protect --staged --verbose --config .gitleaks.toml
     if [ $? -ne 0 ]; then
       echo "❌ Secret detected! Commit blocked."
       echo "Run: gitleaks detect --verbose"
       exit 1
     fi
     ```
   - Instalace: `make install-git-hooks`

3. **GitHub Actions PR Check**
   ```yaml
   # .github/workflows/secret-scan.yml
   - name: GitLeaks Scan
     uses: gitleaks/gitleaks-action@v2
     env:
       GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
   ```

4. **Periodic Full Scan (Nightly)**
   - Skenovat celý repo (ne jen diff):
     ```bash
     gitleaks detect --source . --report-path gitleaks-report.json
     ```
   - Report do Loki:
     ```bash
     cat gitleaks-report.json | jq -c '.[] | {level: "critical", service: "gitleaks", secret: .Description, file: .File}'
     ```

5. **Secret Remediation Runbook**
   - `docs/SECURITY_SECRET_REMEDIATION.md`:
     ```markdown
     ## Pokud byl secret commitnut:
     
     1. OKAMŽITĚ rotuj secret (Vault, Keycloak, DB password)
     2. Odstraň z Git history:
        ```bash
        git filter-branch --force --index-filter \
          "git rm --cached --ignore-unmatch path/to/secret" \
          --prune-empty --tag-name-filter cat -- --all
        ```
     3. Force push (POUZE pokud nikdo není na branch):
        ```bash
        git push origin --force --all
        ```
     4. Audituj kdy byl secret použit (Loki logs)
     5. Notify security team
     ```

6. **`.env` Protection**
   - Ověřit že `.env` je v `.gitignore`:
     ```bash
     grep "^\.env$" .gitignore || (echo "❌ .env missing in .gitignore!" && exit 1)
     ```
   - Pre-commit check:
     ```bash
     git diff --cached --name-only | grep -q "^\.env$" && \
       echo "❌ Cannot commit .env!" && exit 1
     ```

**Acceptance Criteria:**
- ✅ Pre-commit hook blokuje commit s plaintext secrets
- ✅ PR failne pokud GitLeaks najde secrets
- ✅ `.env` není commitovatelný (pre-commit check)
- ✅ Nightly scan detekuje všechny secrets v repo
- ✅ Secret remediation runbook dokumentován

**Effort:** ~1.5 days | **Details:** [stories/SECQ3.md](./stories/SECQ3.md)

---

### SECQ4: DAST Smoke Test (OWASP ZAP Headless) (~500 LOC, 2 days)

**Goal**: Nightly DAST scan proti běžícímu lokálnímu prostředí

**Deliverables:**

1. **OWASP ZAP Setup**
   - Docker image: `owasp/zap2docker-stable`
   - Headless mode (no GUI)
   - Target: `https://core-platform.local`

2. **ZAP Baseline Scan**
   - GitHub Actions (nightly):
     ```yaml
     # .github/workflows/dast-nightly.yml
     - name: Start Core Platform
       run: make up
     
     - name: Wait for Services
       run: make wait-for-services
     
     - name: ZAP Baseline Scan
       run: |
         docker run --rm --network=host \
           -v $(pwd)/security:/zap/wrk:rw \
           owasp/zap2docker-stable \
           zap-baseline.py \
           -t https://core-platform.local \
           -r zap-report.html \
           -J zap-report.json \
           -c zap-rules.conf
     
     - name: Upload Report
       uses: actions/upload-artifact@v3
       with:
         name: zap-report
         path: security/zap-report.html
     ```

3. **ZAP Configuration**
   - `security/zap-rules.conf`:
     ```conf
     # Ignore rules
     10009 IGNORE  # In Page Banner Info Leak (false positive)
     
     # Alert rules (OWASP Top 10)
     40012 WARN    # Cross Site Scripting (Reflected)
     40014 WARN    # Cross Site Scripting (Persistent)
     40018 WARN    # SQL Injection
     90019 WARN    # Server Side Code Injection
     ```

4. **Non-Destructive Testing**
   - Disable active scanning (baseline only):
     ```bash
     zap-baseline.py --auto  # Passive scan only
     ```
   - Spider config (safe crawling):
     ```conf
     spider.maxDepth=3
     spider.maxChildren=10
     spider.excludePatterns=.*logout.*,.*delete.*
     ```

5. **Reporting**
   - HTML report: GitHub Actions artifact
   - JSON report → Loki:
     ```bash
     cat zap-report.json | jq -c '.site[0].alerts[] | {
       level: (.riskdesc | split(" ")[0] | ascii_downcase),
       service: "zap-dast",
       alert: .alert,
       url: .url,
       description: .desc
     }'
     ```

6. **Slack Notifications**
   - Pokud HIGH/MEDIUM alerts:
     ```bash
     high_count=$(jq '[.site[0].alerts[] | select(.riskcode == "3")] | length' zap-report.json)
     if [ $high_count -gt 0 ]; then
       curl -X POST $SLACK_WEBHOOK \
         -d "{\"text\": \"⚠️ DAST scan found $high_count HIGH alerts!\"}"
     fi
     ```

**Acceptance Criteria:**
- ✅ ZAP baseline scan běží nightly
- ✅ Report dostupný jako GitHub artifact
- ✅ Findings logovány do Loki
- ✅ Slack notifikace na HIGH alerts
- ✅ Žádné destruktivní operace (read-only spider)

**Effort:** ~2 days | **Details:** [stories/SECQ4.md](./stories/SECQ4.md)

---

### SECQ5: IaC/Docker/Nginx Linting & Misconfig Detection (~400 LOC, 1.5 days)

**Goal**: Lint všech config souborů pro security misconfigurations

**Deliverables:**

1. **hadolint (Dockerfile Linting)**
   - Installation: `brew install hadolint`
   - Pre-commit hook:
     ```yaml
     # .pre-commit-config.yaml
     - repo: https://github.com/hadolint/hadolint
       rev: v2.12.0
       hooks:
         - id: hadolint-docker
           args: [--ignore, DL3008, --ignore, DL3009]  # Whitelist rules
     ```
   - GitHub Actions:
     ```yaml
     - name: Lint Dockerfiles
       run: |
         find . -name 'Dockerfile*' -exec hadolint {} \;
     ```

2. **yamllint (YAML Linting)**
   - Configuration: `.yamllint.yml`
     ```yaml
     extends: default
     rules:
       line-length:
         max: 120
       indentation:
         spaces: 2
     ```
   - Targets:
     - `docker-compose.yml`
     - `docker-compose.template.yml`
     - `.github/workflows/*.yml`
     - `kubernetes/*.yaml` (až budou)

3. **actionlint (GitHub Actions Linting)**
   - Installation: `brew install actionlint`
   - Pre-commit:
     ```bash
     actionlint .github/workflows/*.yml
     ```

4. **Nginx Config Validation**
   - Custom script: `scripts/lint-nginx.sh`
     ```bash
     #!/bin/bash
     docker run --rm -v $(pwd)/docker/nginx:/etc/nginx nginx:alpine \
       nginx -t -c /etc/nginx/nginx-ssl.conf
     
     # Security checks
     grep -q "ssl_protocols TLSv1.2 TLSv1.3" docker/nginx/nginx-ssl.conf || \
       (echo "❌ Insecure TLS protocols!" && exit 1)
     
     grep -q "add_header Strict-Transport-Security" docker/nginx/nginx-ssl.conf || \
       (echo "❌ Missing HSTS header!" && exit 1)
     ```

5. **kube-linter (Kubernetes Manifests)**
   - Pro budoucí K8s deployment:
     ```yaml
     - name: Lint K8s Manifests
       run: kube-linter lint kubernetes/
     ```
   - Rules:
     - No privileged containers
     - No host network/PID/IPC
     - Resource limits defined
     - Non-root user

6. **Pre-commit Integration**
   - Install: `pip install pre-commit`
   - Config: `.pre-commit-config.yaml`
     ```yaml
     repos:
       - repo: https://github.com/hadolint/hadolint
         rev: v2.12.0
         hooks: [hadolint-docker]
       
       - repo: https://github.com/adrienverge/yamllint
         rev: v1.32.0
         hooks: [yamllint]
       
       - repo: https://github.com/rhysd/actionlint
         rev: v1.6.26
         hooks: [actionlint]
     ```
   - Install hooks: `make install-git-hooks`

**Acceptance Criteria:**
- ✅ hadolint validuje všechny Dockerfiles
- ✅ yamllint validuje YAML soubory
- ✅ actionlint validuje GitHub Actions workflows
- ✅ Nginx config validace (syntax + security headers)
- ✅ Pre-commit hooks blokují commit s lint errors
- ✅ PR failne na lint failures

**Effort:** ~1.5 days | **Details:** [stories/SECQ5.md](./stories/SECQ5.md)

---

### SECQ6: CI Orchestrator - Quality Gate Pipeline (~600 LOC, 2 days)

**Goal**: Definovat a implementovat orchestraci všech quality checks

**Deliverables:**

1. **Pipeline Matrix**
   | Check | PR | Nightly | Release |
   |-------|----|---------| --------|
   | Unit Tests | ✅ | ✅ | ✅ |
   | Integration Tests | ✅ | ✅ | ✅ |
   | SAST (SonarQube) | ✅ | ✅ | ✅ Full |
   | SCA (OWASP DC) | ✅ | ✅ | ✅ |
   | Container Scan (Trivy) | ✅ | ✅ | ✅ |
   | Secret Scan (GitLeaks) | ✅ | ✅ Full | ✅ |
   | IaC Lint | ✅ | - | ✅ |
   | E2E Smoke | - | ✅ | ✅ |
   | E2E Full | - | ✅ | ✅ |
   | DAST (ZAP) | - | ✅ | ✅ |
   | Performance | - | ✅ | - |

2. **GitHub Actions Workflows**

   **PR Workflow:**
   ```yaml
   # .github/workflows/pr-quality-gates.yml
   name: PR Quality Gates
   
   on:
     pull_request:
       branches: [main]
   
   jobs:
     unit-tests:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - name: Backend Unit Tests
           run: cd backend && mvn test
         - name: Frontend Unit Tests
           run: cd frontend && npm test
     
     integration-tests:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - name: Start Services
           run: docker compose up -d core-db redis kafka
         - name: Backend Integration Tests
           run: cd backend && mvn verify -Pintegration
     
     sast:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - name: SonarQube Scan
           run: mvn sonar:sonar -Dsonar.login=${{ secrets.SONAR_TOKEN }}
         - name: Quality Gate
           run: scripts/check-sonar-quality-gate.sh
     
     sca:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - name: OWASP Dependency Check
           run: mvn dependency-check:check
         - name: Trivy Container Scan
           run: |
             docker compose build backend
             trivy image --exit-code 1 --severity CRITICAL,HIGH core-platform/backend
     
     secret-scan:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
           with:
             fetch-depth: 0  # Full history for GitLeaks
         - uses: gitleaks/gitleaks-action@v2
     
     iac-lint:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - name: Lint Dockerfiles
           run: find . -name 'Dockerfile*' -exec hadolint {} \;
         - name: Lint YAML
           run: yamllint .
         - name: Lint GitHub Actions
           run: actionlint
   ```

   **Nightly Workflow:**
   ```yaml
   # .github/workflows/nightly-regression.yml
   name: Nightly Regression
   
   on:
     schedule:
       - cron: '0 2 * * *'  # 2am UTC
     workflow_dispatch:  # Manual trigger
   
   jobs:
     e2e-full:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - name: Start Platform
           run: make up
         - name: E2E Full Suite
           run: cd e2e && npm run test:full
     
     dast:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - name: Start Platform
           run: make up
         - name: ZAP Baseline Scan
           run: scripts/run-zap-scan.sh
     
     secret-scan-full:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
           with:
             fetch-depth: 0
         - name: Full Repo Scan
           run: gitleaks detect --source . --report-path gitleaks-full.json
   ```

3. **Quality Gate Decision Logic**
   - Script: `scripts/check-sonar-quality-gate.sh`
     ```bash
     #!/bin/bash
     PROJECT_KEY="core-platform-backend"
     STATUS=$(curl -s "$SONAR_HOST/api/qualitygates/project_status?projectKey=$PROJECT_KEY" | jq -r '.projectStatus.status')
     
     if [ "$STATUS" != "OK" ]; then
       echo "❌ Quality gate FAILED: $STATUS"
       
       # Get detailed metrics
       curl -s "$SONAR_HOST/api/measures/component?component=$PROJECT_KEY&metricKeys=bugs,vulnerabilities,code_smells,coverage" | \
         jq '.component.measures'
       
       exit 1
     fi
     
     echo "✅ Quality gate PASSED"
     ```

4. **Makefile Integration**
   ```makefile
   # Quality gate targets
   .PHONY: quality-gate-pr quality-gate-nightly quality-gate-release
   
   quality-gate-pr:
   	@echo "Running PR quality gates..."
   	$(MAKE) test-backend
   	$(MAKE) test-frontend
   	$(MAKE) sast-scan
   	$(MAKE) sca-scan
   	$(MAKE) secret-scan
   	$(MAKE) iac-lint
   
   quality-gate-nightly:
   	@echo "Running nightly quality gates..."
   	$(MAKE) quality-gate-pr
   	$(MAKE) test-e2e-full
   	$(MAKE) dast-scan
   
   quality-gate-release:
   	@echo "Running release quality gates..."
   	$(MAKE) quality-gate-pr
   	$(MAKE) test-e2e-smoke
   	$(MAKE) security-audit-checklist
   ```

5. **Reporting Dashboard**
   - Loki query pro overview:
     ```logql
     {service=~"sonarqube|trivy|gitleaks|zap-dast"} |= "level" | json
     ```
   - Grafana dashboard: `security/grafana-quality-gates-dashboard.json`
     - Panel 1: SAST findings trend
     - Panel 2: CVE count over time
     - Panel 3: Secret scan alerts
     - Panel 4: Quality gate pass/fail rate

6. **Branch Protection Rules**
   - GitHub Settings → Branches → `main`:
     - ✅ Require status checks before merge
     - ✅ Required checks:
       - `unit-tests`
       - `integration-tests`
       - `sast`
       - `sca`
       - `secret-scan`
       - `iac-lint`

**Acceptance Criteria:**
- ✅ PR pipeline běží všechny mandatory checks (<15 min)
- ✅ Nightly pipeline běží full regression (<60 min)
- ✅ Release pipeline má strict gates
- ✅ Branch protection vynucuje quality gates
- ✅ Grafana dashboard zobrazuje quality trends
- ✅ Makefile targets umožňují lokální spuštění

**Effort:** ~2 days | **Details:** [stories/SECQ6.md](./stories/SECQ6.md)

---

### SECQ7: AI-Generated Code Guardrails (~300 LOC, 1 day)

**Goal**: Dokumentovat a vynucovat bezpečné použití AI-generovaného kódu

**Deliverables:**

1. **AI Code Security Checklist**
   - `docs/AI_CODE_SECURITY_CHECKLIST.md`:
     ```markdown
     # AI-Generated Code Security Checklist
     
     Před merge AI-generovaného kódu, ověř:
     
     ## 🔴 CRITICAL - Vyžaduje ruční review
     
     - [ ] Autentizace/autorizace logika (Keycloak, JWT, RBAC)
     - [ ] Crypto operace (hashing, encryption, signing)
     - [ ] Práce se secrety (Vault access, DB credentials)
     - [ ] SQL queries (SQL injection risk)
     - [ ] File operations (path traversal risk)
     - [ ] Network calls (SSRF risk)
     - [ ] Deserialization (RCE risk)
     
     ## 🟡 MEDIUM - Automated checks + spot review
     
     - [ ] API endpoints (validace inputů)
     - [ ] Database entity definitions
     - [ ] Business logic (state transitions)
     - [ ] Frontend components (XSS prevention)
     
     ## 🟢 LOW - Automated checks sufficient
     
     - [ ] UI layout/styling
     - [ ] Documentation
     - [ ] Test code (fixtures, mocks)
     - [ ] Configuration templates
     ```

2. **Automated Pattern Detection**
   - Script: `scripts/detect-risky-ai-patterns.sh`
     ```bash
     #!/bin/bash
     # Detect potentially risky patterns in AI-generated code
     
     RISKY_PATTERNS=(
       "execut.*Runtime"           # Runtime.exec() - command injection risk
       "new.*ProcessBuilder"       # ProcessBuilder - command injection
       "eval\("                    # JavaScript eval() - code injection
       "deserialize"               # Deserialization - RCE risk
       "setCookie.*httpOnly.*false" # Insecure cookies
       "\.createQuery\("           # Raw SQL - injection risk
       "password.*=.*\""           # Hardcoded passwords
       "private.*key.*=.*\""       # Hardcoded keys
     )
     
     for pattern in "${RISKY_PATTERNS[@]}"; do
       if git diff HEAD --unified=0 | grep -iE "$pattern"; then
         echo "⚠️  Risky pattern detected: $pattern"
         echo "   → Manual security review REQUIRED"
       fi
     done
     ```

3. **PR Template Enhancement**
   - `.github/PULL_REQUEST_TEMPLATE.md`:
     ```markdown
     ## AI-Generated Code Disclosure
     
     - [ ] This PR contains AI-generated code (GitHub Copilot, ChatGPT, other)
     - [ ] I have reviewed AI suggestions for security issues
     - [ ] Critical security code (auth, crypto, secrets) reviewed by human
     - [ ] No hardcoded credentials or private keys
     - [ ] All external inputs validated
     
     **AI Tool Used:** _[GitHub Copilot / ChatGPT / Claude / Other]_
     
     **Files with AI-generated code:**
     - 
     
     **Security review completed by:** _[Name]_
     ```

4. **Code Owner Rules**
   - `CODEOWNERS`:
     ```
     # Security-critical paths require security team review
     /backend/src/main/java/**/auth/**         @security-team
     /backend/src/main/java/**/security/**     @security-team
     /backend/src/main/java/**/crypto/**       @security-team
     /docker/vault/**                          @security-team
     /.github/workflows/**                     @devops-team
     ```

5. **AI Code Metrics**
   - Tracking v Loki:
     ```json
     {
       "level": "info",
       "service": "ai-code-tracker",
       "pr_number": 123,
       "ai_tool": "github-copilot",
       "files_changed": 5,
       "ai_generated_lines": 150,
       "human_reviewed": true,
       "security_review": true
     }
     ```

6. **Developer Training**
   - `docs/AI_CODE_BEST_PRACTICES.md`:
     ```markdown
     # Best Practices pro AI-Assisted Development
     
     ## ✅ DO
     
     - Review každý AI návrh před acceptem
     - Test AI-generated code stejně jako human code
     - Použij AI pro boilerplate, ne security logic
     - Deklaruj AI použití v PR description
     
     ## ❌ DON'T
     
     - Neacceptuj AI kód slepě (zejména security)
     - Nežádej AI o generování credentials/keys
     - Nepouštěj AI-generated commands bez review
     - Necommituj AI output s citlivými daty
     ```

**Acceptance Criteria:**
- ✅ AI Code Security Checklist dokumentován
- ✅ Risky pattern detection běží na PR
- ✅ PR template vyžaduje AI disclosure
- ✅ CODEOWNERS vynucuje security review
- ✅ Developer training materials dostupné

**Effort:** ~1 day | **Details:** [stories/SECQ7.md](./stories/SECQ7.md)

---

### SECQ8: Documentation & Onboarding (~400 LOC, 1.5 days)

**Goal**: Dokumentovat celý security pipeline a onboarding pro nové devs

**Deliverables:**

1. **Security Pipeline Documentation**
   - `docs/SECURITY_PIPELINE.md`:
     ```markdown
     # Security Pipeline Guide
     
     ## Overview
     
     Core-platform má 3-tier quality gate systém:
     - **PR gates** (mandatory, blocking)
     - **Nightly gates** (regression, alerting)
     - **Release gates** (strict, sign-off required)
     
     ## Tools
     
     | Tool | Purpose | When |
     |------|---------|------|
     | SonarQube | SAST + quality | PR, nightly |
     | OWASP DC | Dependency CVE | PR, release |
     | Trivy | Container CVE | PR, release |
     | GitLeaks | Secret scan | PR, nightly |
     | OWASP ZAP | DAST | Nightly |
     
     ## How to Run Locally
     
     ```bash
     # Full quality gate suite
     make quality-gate-pr
     
     # Individual checks
     make sast-scan
     make sca-scan
     make secret-scan
     make iac-lint
     ```
     
     ## Troubleshooting
     
     ### Quality Gate Failed
     
     1. Check SonarQube dashboard: https://admin.core-platform.local/sonar
     2. Review specific findings
     3. Fix issues or whitelist (with justification)
     4. Re-run: `make quality-gate-pr`
     
     ### CVE Detected
     
     1. Check if CVE is applicable (trivy/OWASP DC report)
     2. If false positive: add to `security/cve-exceptions.yml`
     3. If real: update dependency or apply patch
     4. Document in PR why exception is safe
     ```

2. **Developer Onboarding Checklist**
   - `docs/ONBOARDING_SECURITY.md`:
     ```markdown
     # Security Onboarding for Developers
     
     ## Week 1: Setup
     
     - [ ] Install Git hooks: `make install-git-hooks`
     - [ ] Install tools: hadolint, yamllint, gitleaks
     - [ ] Read: `SECURITY_PIPELINE.md`
     - [ ] Read: `AI_CODE_SECURITY_CHECKLIST.md`
     - [ ] Access: SonarQube dashboard (get credentials from team)
     
     ## Week 2: Practice
     
     - [ ] Run local quality gates on sample PR
     - [ ] Fix a SonarQube code smell
     - [ ] Review a security-critical PR
     - [ ] Complete AI code review checklist
     
     ## Week 3: Certification
     
     - [ ] Pass security quiz (OWASP Top 10, tenant isolation)
     - [ ] Shadow senior dev on security review
     - [ ] Authorized as CODEOWNER for non-critical paths
     ```

3. **Runbooks**

   **Secret Leak Runbook:**
   - `docs/runbooks/SECRET_LEAK_RESPONSE.md`

   **CVE Response Runbook:**
   - `docs/runbooks/CVE_RESPONSE.md`

   **Quality Gate Bypass Runbook:**
   - `docs/runbooks/QUALITY_GATE_BYPASS.md` (emergency only)

4. **Metrics Dashboard**
   - Grafana dashboard JSON: `security/grafana-security-metrics.json`
   - Panels:
     - Security findings trend (last 30 days)
     - Quality gate pass rate
     - Mean time to remediate CVE
     - AI-generated code percentage

5. **Audit Log Queries**
   - `docs/SECURITY_AUDIT_QUERIES.md`:
     ```markdown
     # Loki Queries pro Security Audit
     
     ## All security findings (last 7 days)
     ```logql
     {service=~"sonarqube|trivy|gitleaks|zap"} |= "level" | json | line_format "{{.level}} {{.service}} {{.message}}"
     ```
     
     ## Critical CVEs only
     ```logql
     {service="trivy", severity="CRITICAL"} | json
     ```
     
     ## Secret scan alerts
     ```logql
     {service="gitleaks"} |= "secret" | json
     ```
     ```

6. **FAQ**
   - `docs/SECURITY_FAQ.md`:
     ```markdown
     # Security Pipeline FAQ
     
     **Q: Můžu skipnout quality gate pro hotfix?**
     A: Ne. Použij emergency bypass process (vyžaduje security lead approval).
     
     **Q: Co dělat když SonarQube reportuje false positive?**
     A: 1) Ověř že je to opravdu FP, 2) Přidej do SonarQube "Won't Fix" s odůvodněním, 3) Dokumentuj v PR.
     
     **Q: Trivy našel CVE v base image, co teď?**
     A: 1) Check fixed version, 2) Update base image tag, 3) Pokud fix neexistuje, whitelist + monitor.
     ```

**Acceptance Criteria:**
- ✅ `SECURITY_PIPELINE.md` pokrývá všechny tools a workflows
- ✅ Onboarding checklist dokumentován
- ✅ Runbooks pro incident response
- ✅ Grafana dashboard pro security metrics
- ✅ Loki audit queries dokumentovány
- ✅ FAQ odpovídá na common questions

**Effort:** ~1.5 days | **Details:** [stories/SECQ8.md](./stories/SECQ8.md)

---

## 📊 Implementation Roadmap

### Phase 1: Foundation (Week 1, ~8 days effort)

**Stories:** SECQ1, SECQ2, SECQ3

**Deliverables:**
- ✅ SonarQube deployed + quality gates
- ✅ OWASP Dependency-Check + Trivy scanning
- ✅ GitLeaks secret scanning + pre-commit hooks

**Outcome:** PR pipeline má mandatory security checks

### Phase 2: Advanced Scanning (Week 2, ~4 days effort)

**Stories:** SECQ4, SECQ5

**Deliverables:**
- ✅ OWASP ZAP DAST nightly scans
- ✅ IaC/Docker/Nginx linting

**Outcome:** Nightly pipeline má regression + security tests

### Phase 3: Orchestration (Week 3, ~3.5 days effort)

**Stories:** SECQ6, SECQ7

**Deliverables:**
- ✅ CI orchestrator (PR/nightly/release workflows)
- ✅ AI code guardrails + checklist

**Outcome:** Kompletní quality gate pipeline + AI governance

### Phase 4: Documentation (Week 4, ~1.5 days effort)

**Stories:** SECQ8

**Deliverables:**
- ✅ Security pipeline docs
- ✅ Onboarding materials
- ✅ Runbooks + FAQ

**Outcome:** Team je trained a dokumentace kompletní

---

## 🔗 Integration Points

### EPIC-000 (Security Platform Hardening)

**EPIC-000 definuje:**
- ✅ Keycloak realms per tenant
- ✅ RBAC model
- ✅ Network isolation

**EPIC-020 vynucuje:**
- ✅ SAST checks pro auth/RBAC kód
- ✅ Container scanning pro Keycloak images
- ✅ Secret scanning pro Keycloak credentials

### EPIC-002 (E2E Testing)

**EPIC-002 poskytuje:**
- ✅ Playwright E2E framework
- ✅ Test tagging (@SMOKE, @REGRESSION)

**EPIC-020 orchestruje:**
- ✅ E2E smoke tests na PR (optional)
- ✅ E2E full tests na nightly (mandatory)

### EPIC-003 (Monitoring/Observability)

**EPIC-003 poskytuje:**
- ✅ Loki centralizované logy

**EPIC-020 využívá:**
- ✅ Log všech security findings do Loki
- ✅ Grafana dashboards pro security metrics

### EPIC-007 (Infrastructure Deployment)

**EPIC-007 poskytuje:**
- ✅ Docker Compose setup
- ✅ Nginx reverse proxy

**EPIC-020 validuje:**
- ✅ Dockerfile linting (hadolint)
- ✅ Nginx config validation
- ✅ Container CVE scanning (Trivy)

### EPIC-012 (Vault Integration)

**EPIC-012 poskytuje:**
- ✅ HashiCorp Vault pro secrets
- ✅ PKI certifikáty

**EPIC-020 vynucuje:**
- ✅ Secret scanning proti plaintext secrets
- ✅ Vault usage policy (no hardcoded credentials)

### EPIC-017 (Modular Architecture)

**EPIC-017 poskytuje:**
- ✅ Moduly nad CORE

**EPIC-020 validuje:**
- ✅ Quality gates platí i pro moduly (ne jen CORE)
- ✅ Module dependencies skenování (OWASP DC)

---

## 🎓 AI-Generated Code Governance

### Rizika AI Kódu

1. **Security Vulnerabilities**
   - AI model může navrhovat zranitelný kód (SQL injection, XSS)
   - Nedostatečná validace inputů
   - Insecure defaults

2. **Compliance Violations**
   - Hardcoded secrets/credentials
   - GPL licence dependencies (license conflicts)
   - Privacy violations (GDPR)

3. **Quality Issues**
   - Code smells, duplikace
   - Chybějící error handling
   - Performance anti-patterns

### Ochranná Opatření

**Automatické Kontroly (100% AI kódu):**
- ✅ SAST (SonarQube/CodeQL) - detekuje OWASP Top 10
- ✅ SCA (OWASP DC) - kontroluje dependencies
- ✅ Secret scan (GitLeaks) - blokuje hardcoded secrets
- ✅ Unit tests - AI kód musí mít 80%+ coverage

**Manuální Review (Security-Critical AI Kód):**
- ✅ Auth/RBAC logika → security team review
- ✅ Crypto operace → security team review
- ✅ Vault access → security team review
- ✅ Database migrations → senior dev review

**Tracking & Audit:**
- ✅ PR template disclosure (AI tool used)
- ✅ Loki logs (AI-generated code metrics)
- ✅ CODEOWNERS (enforce reviews)

---

## 📈 Metrics & KPIs

### Security Metrics

- **Vulnerability Density:** CVEs per 1000 LOC (target: <0.5)
- **Mean Time to Remediate (MTTR):** Days od detekce do fix (target: <7 days for HIGH)
- **Secret Leak Rate:** Secrets per 100 commits (target: 0)
- **Quality Gate Pass Rate:** % PRs that pass first time (target: >85%)

### Quality Metrics

- **Code Coverage:** Line + branch coverage (target: 80%/70%)
- **Code Smells:** Per 1000 LOC (target: <5)
- **Technical Debt Ratio:** % of time to fix issues (target: <5%)
- **Duplication:** % duplicated code (target: <3%)

### Compliance Metrics

- **Audit Log Completeness:** % findings logged to Loki (target: 100%)
- **Security Review Coverage:** % AI code reviewed (target: 100% critical)
- **Onboarding Completion:** % devs completed security training (target: 100%)

---

## 🚀 Quick Start

### Developer Setup

```bash
# 1. Install tools
brew install hadolint yamllint gitleaks actionlint

# 2. Install Git hooks
make install-git-hooks

# 3. Run local quality gate
make quality-gate-pr

# 4. Check results
make logs-quality-gates
```

### CI/CD Setup

```bash
# 1. Configure GitHub Secrets
# - SONAR_TOKEN
# - SLACK_WEBHOOK

# 2. Enable branch protection
# Settings → Branches → main → Require status checks

# 3. Test PR pipeline
git checkout -b test/quality-gates
git commit --allow-empty -m "test: quality gates"
git push origin test/quality-gates
# → Open PR, watch checks run
```

---

## 📚 References

- **OWASP Top 10:** https://owasp.org/www-project-top-ten/
- **NIST SSDF:** https://csrc.nist.gov/publications/detail/sp/800-218/final
- **SonarQube Docs:** https://docs.sonarqube.org/
- **Trivy Docs:** https://aquasecurity.github.io/trivy/
- **GitLeaks Docs:** https://github.com/gitleaks/gitleaks
- **OWASP ZAP:** https://www.zaproxy.org/docs/

---

## 🛡️ Security Contact

Pro security findings nebo questions:
- **Security Lead:** [security@virelio.com]
- **Emergency:** Slack channel `#security-incidents`
- **Vulnerability Disclosure:** `security/SECURITY.md`

---

**Status:** 🔴 0% IMPLEMENTED  
**Next:** SECQ1 (SonarQube setup)  
**Owner:** DevOps + Security Team
