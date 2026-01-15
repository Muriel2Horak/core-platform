---
id: INF-001
epic: EPIC-007-infrastructure-deployment
title: "Centralized Template Generator"
priority: P0
status: todo
assignee: ""
created: 2025-11-08
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-007-infrastructure-deployment/stories/INF-001-template-generator/README.md
    - backlog/EPIC-007-infrastructure-deployment/README.md
---

# INF-001: Centralized Template Generator

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 2 dny, ~400 LOC  
**Owner:** Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**
```bash
# Template generation je scattered:
docker/keycloak/generate-realm.sh       # Keycloak realm only
docker/nginx/Dockerfile                 # envsubst inline
Makefile                                # Compose generation
# Žádná validace že všechny vars existují
```

**Git Evidence:**
- 15 commits s "template" v message
- 3 různé substitution mechanismy (envsubst, Docker ${}, Spring ${})
- Developer musí vědět který script volat kdy

**Pain Points:**
1. Developer zapomene regenerovat template → runtime fail
2. Chybějící env var → cryptic error deep v buildu
3. Template syntax inconsistent (${VAR} vs $VAR)

### Goal

**Single source of truth pro template generation:**
```bash
make generate-configs  # Generuje VŠE z templates
```

**Validace:**
- Zkontroluje `.env` existenci
- Ověří že všechny `${VARIABLES}` mají hodnotu
- Fail-fast pokud chybí required vars

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Master Generator Script**
   - `scripts/generate-all-configs.sh`
   - Volá všechny template generators (realm, nginx, compose)
   - Idempotent (safe re-run)
   - Exit code 0 = success, non-zero = fail

2. ✅ **Environment Variable Validation**
   - Před generováním: validace `.env` completeness
   - Check: všechny `${VAR}` v templates mají hodnotu v `.env`
   - Report: které variables chybí (friendly error message)

3. ✅ **Pre-Commit Hook**
   - Git pre-commit: automaticky volá `make generate-configs`
   - Fail pokud templates nejsou sync s `.env`
   - Developer notification: "Run `make generate-configs` to fix"

4. ✅ **CI/CD Integration**
   - GitHub Actions workflow: template validation check
   - Fail PR pokud generated files nejsou commitnuté
   - Comment on PR: "Generated configs out of sync"

### Non-Functional Requirements

1. **Performance:** <5s pro full generation
2. **Reliability:** 0% false positives (no spurious failures)
3. **Usability:** Clear error messages (ne "envsubst: command not found")

---

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [TASK-001-01: Generator skripty + Makefile target](subtasks/TASK-001-01-generator-scripts.md) | 6h | none |
| 2 | [TASK-001-02: Validace env promennych pro templates](subtasks/TASK-001-02-env-validation-templates.md) | 4h | TASK-001-01 |
| 3 | [TASK-001-03: Pre-commit kontrola (lefthook)](subtasks/TASK-001-03-lefthook-precommit.md) | 2h | TASK-001-01 |
| 4 | [TASK-001-04: CI template-check workflow](subtasks/TASK-001-04-ci-template-check.md) | 4h | TASK-001-02 |

---

## 🏗️ IMPLEMENTATION DETAILS

### File Structure

```
core-platform/
├── scripts/
│   ├── generate-all-configs.sh      # Master generator (NEW)
│   ├── validate-env.sh               # Env var validation (ENHANCED)
│   └── templates/
│       ├── generate-realm.sh         # Keycloak realm (EXISTS)
│       ├── generate-nginx.sh         # Nginx config (NEW)
│       └── generate-compose.sh       # Docker Compose (NEW)
│
├── .env.template                     # Source of truth (EXISTS)
├── .env                             # Generated copy (EXISTS)
│
├── .githooks/
│   └── pre-commit                    # Git hook (NEW)
│
└── .github/
    └── workflows/
        └── template-check.yml        # CI workflow (NEW)
```

### Master Generator Script

**File:** `scripts/generate-all-configs.sh`

```bash
#!/bin/bash
set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${GREEN}🔧 Generating all configurations from templates...${NC}"
echo ""

# Step 1: Validate .env exists and is complete
echo "📋 Step 1/4: Validating .env file..."
if ! bash scripts/validate-env.sh; then
    echo -e "${RED}❌ Environment validation failed!${NC}"
    echo -e "${YELLOW}💡 Tip: Copy .env.template to .env and fill in required values${NC}"
    exit 1
fi
echo -e "${GREEN}✅ .env validation passed${NC}"
echo ""

# Step 2: Generate Keycloak realm config
echo "🔑 Step 2/4: Generating Keycloak realm config..."
if bash docker/keycloak/generate-realm.sh; then
    echo -e "${GREEN}✅ realm-admin.json generated${NC}"
else
    echo -e "${RED}❌ Keycloak realm generation failed!${NC}"
    exit 1
fi
echo ""

# Step 3: Generate Nginx config
echo "🌐 Step 3/4: Generating Nginx config..."
if bash scripts/templates/generate-nginx.sh; then
    echo -e "${GREEN}✅ nginx-ssl.conf generated${NC}"
else
    echo -e "${RED}❌ Nginx config generation failed!${NC}"
    exit 1
fi
echo ""

# Step 4: Generate Docker Compose config
echo "🐳 Step 4/4: Generating Docker Compose config..."
if bash scripts/templates/generate-compose.sh; then
    echo -e "${GREEN}✅ docker-compose.yml generated${NC}"
else
    echo -e "${RED}❌ Docker Compose generation failed!${NC}"
    exit 1
fi
echo ""

echo -e "${GREEN}🎉 All configurations generated successfully!${NC}"
echo ""
echo "Generated files:"
echo "  - docker/keycloak/realm-admin.json"
echo "  - docker/nginx/nginx-ssl.conf"
echo "  - docker-compose.yml"
echo ""
echo -e "${YELLOW}💡 Next steps:${NC}"
echo "  1. Review generated files"
echo "  2. Commit changes: git add <files> && git commit"
echo "  3. Build: make clean-fast"
```

### Environment Validation Script

**File:** `scripts/validate-env.sh` (ENHANCED)

```bash
#!/bin/bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Check .env existence
if [[ ! -f .env ]]; then
    echo -e "${RED}❌ .env file not found!${NC}"
    echo -e "${YELLOW}💡 Create .env from template:${NC}"
    echo "   cp .env.template .env"
    echo "   vim .env  # Fill in required values"
    exit 1
fi

# Load .env
set -a
source .env
set +a

# List of REQUIRED variables (from SECURITY_CONFIG_AUDIT.md)
REQUIRED_VARS=(
    "DOMAIN"
    "POSTGRES_PASSWORD"
    "DATABASE_URL"
    "DATABASE_USERNAME"
    "DATABASE_PASSWORD"
    "KEYCLOAK_ADMIN"
    "KEYCLOAK_ADMIN_PASSWORD"
    "KEYCLOAK_BASE_URL"
    "KEYCLOAK_ADMIN_CLIENT_SECRET"
    "OIDC_CLIENT_ID"
    "OIDC_CLIENT_SECRET"
    "OIDC_ISSUER_URI"
    "REDIS_HOST"
    "KAFKA_BOOTSTRAP_SERVERS"
    "GRAFANA_ADMIN_USER"
    "GRAFANA_ADMIN_PASSWORD"
)

# Check each required variable
MISSING_VARS=()
for VAR in "${REQUIRED_VARS[@]}"; do
    if [[ -z "${!VAR:-}" ]]; then
        MISSING_VARS+=("$VAR")
    fi
done

# Report results
if [[ ${#MISSING_VARS[@]} -eq 0 ]]; then
    echo -e "${GREEN}✅ All required environment variables are set${NC}"
    exit 0
else
    echo -e "${RED}❌ Missing required environment variables:${NC}"
    for VAR in "${MISSING_VARS[@]}"; do
        echo "   - $VAR"
    done
    echo ""
    echo -e "${YELLOW}💡 Add missing variables to .env file${NC}"
    exit 1
fi
```

### Nginx Template Generator

**File:** `scripts/templates/generate-nginx.sh` (NEW)

```bash
#!/bin/bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

TEMPLATE="docker/nginx/nginx-ssl.conf.template"
OUTPUT="docker/nginx/nginx-ssl.conf"

if [[ ! -f "$TEMPLATE" ]]; then
    echo "❌ Template not found: $TEMPLATE"
    exit 1
fi

# Load .env
set -a
source .env
set +a

# Generate config using envsubst
envsubst < "$TEMPLATE" > "$OUTPUT"

# Validate generated config (syntax check)
if docker run --rm -v "$(pwd)/docker/nginx:/etc/nginx:ro" nginx:alpine nginx -t; then
    echo "✅ Nginx config syntax valid"
    exit 0
else
    echo "❌ Nginx config syntax error!"
    exit 1
fi
```

### Docker Compose Generator

**File:** `scripts/templates/generate-compose.sh` (NEW)

```bash
#!/bin/bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

TEMPLATE="docker-compose.template.yml"
OUTPUT="docker-compose.yml"

if [[ ! -f "$TEMPLATE" ]]; then
    echo "❌ Template not found: $TEMPLATE"
    exit 1
fi

# Load .env
set -a
source .env
set +a

# Generate Docker Compose file
envsubst < "$TEMPLATE" > "$OUTPUT"

# Validate syntax
if docker compose -f "$OUTPUT" config > /dev/null 2>&1; then
    echo "✅ Docker Compose config syntax valid"
    exit 0
else
    echo "❌ Docker Compose config syntax error!"
    docker compose -f "$OUTPUT" config
    exit 1
fi
```

### Git Pre-Commit Hook

**File:** `.githooks/pre-commit`

```bash
#!/bin/bash
set -euo pipefail

# Check if generated configs are in sync with templates
echo "🔍 Checking template consistency..."

if bash scripts/generate-all-configs.sh --dry-run; then
    # Dry-run passed, check if files differ
    if git diff --exit-code docker/keycloak/realm-admin.json docker/nginx/nginx-ssl.conf docker-compose.yml > /dev/null; then
        echo "✅ Generated configs are in sync"
        exit 0
    else
        echo "❌ Generated configs are out of sync with templates!"
        echo ""
        echo "💡 Run this to fix:"
        echo "   make generate-configs"
        echo "   git add <generated files>"
        exit 1
    fi
else
    echo "❌ Template generation failed!"
    echo ""
    echo "💡 Fix .env or templates, then retry commit"
    exit 1
fi
```

**Install hook:**
```bash
# Makefile target
.PHONY: install-hooks
install-hooks:
	@echo "Installing Git hooks..."
	@cp .githooks/pre-commit .git/hooks/pre-commit
	@chmod +x .git/hooks/pre-commit
	@echo "✅ Git hooks installed"
```

### GitHub Actions Workflow

**File:** `.github/workflows/template-check.yml`

```yaml
name: Template Consistency Check

on:
  pull_request:
    paths:
      - '.env.template'
      - 'docker-compose.template.yml'
      - 'docker/keycloak/realm-admin.template.json'
      - 'docker/nginx/nginx-ssl.conf.template'
      - 'scripts/generate-all-configs.sh'
      - 'scripts/templates/**'

jobs:
  check-templates:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Create dummy .env from template
        run: cp .env.template .env
      
      - name: Generate configs
        run: bash scripts/generate-all-configs.sh
      
      - name: Check if generated files are committed
        run: |
          if ! git diff --exit-code docker/keycloak/realm-admin.json docker/nginx/nginx-ssl.conf docker-compose.yml; then
            echo "❌ Generated configs are not committed!"
            echo ""
            echo "Generated files differ from committed versions."
            echo "Please run: make generate-configs"
            echo "Then commit the generated files."
            exit 1
          fi
          echo "✅ Generated configs are in sync"
      
      - name: Comment on PR (if failed)
        if: failure()
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: '❌ **Template Consistency Check Failed**\n\nGenerated configuration files are not in sync with templates.\n\n**To fix:**\n```bash\nmake generate-configs\ngit add docker/keycloak/realm-admin.json docker/nginx/nginx-ssl.conf docker-compose.yml\ngit commit --amend --no-edit\ngit push --force\n```'
            })
```

---

## 🧪 TESTING STRATEGY

### Unit Tests

**Test:** `scripts/generate-all-configs.sh`

```bash
# Test 1: Success case (all vars present)
@test "generate-all-configs succeeds when .env is complete" {
  cp .env.template .env
  # Fill all required vars
  run bash scripts/generate-all-configs.sh
  [ "$status" -eq 0 ]
}

# Test 2: Fail case (missing vars)
@test "generate-all-configs fails when DOMAIN missing" {
  cp .env.template .env
  sed -i '/^DOMAIN=/d' .env
  run bash scripts/generate-all-configs.sh
  [ "$status" -ne 0 ]
  [[ "$output" =~ "DOMAIN" ]]
}

# Test 3: Idempotence
@test "generate-all-configs is idempotent" {
  bash scripts/generate-all-configs.sh
  HASH1=$(md5sum docker-compose.yml | cut -d' ' -f1)
  
  bash scripts/generate-all-configs.sh
  HASH2=$(md5sum docker-compose.yml | cut -d' ' -f1)
  
  [ "$HASH1" == "$HASH2" ]
}
```

### Integration Tests

**Test:** Pre-commit hook

```bash
# Test 1: Hook blocks commit when configs out of sync
@test "pre-commit hook blocks commit when configs dirty" {
  # Modify template
  echo "# Comment" >> docker-compose.template.yml
  
  # Don't regenerate
  git add docker-compose.template.yml
  
  # Try commit (should fail)
  run git commit -m "Test"
  [ "$status" -ne 0 ]
}

# Test 2: Hook allows commit when configs in sync
@test "pre-commit hook allows commit when configs clean" {
  # Modify template
  echo "# Comment" >> docker-compose.template.yml
  
  # Regenerate
  make generate-configs
  
  # Commit (should succeed)
  git add docker-compose.template.yml docker-compose.yml
  run git commit -m "Test"
  [ "$status" -eq 0 ]
}
```

### E2E Tests

**Test:** Full workflow

```bash
@test "developer workflow: edit template → generate → commit" {
  # 1. Edit .env.template
  sed -i 's/^DOMAIN=.*/DOMAIN=new-domain.local/' .env.template
  
  # 2. Generate configs
  run make generate-configs
  [ "$status" -eq 0 ]
  
  # 3. Verify domain in generated files
  grep -q "new-domain.local" docker/keycloak/realm-admin.json
  grep -q "new-domain.local" docker/nginx/nginx-ssl.conf
  grep -q "new-domain.local" docker-compose.yml
  
  # 4. Commit
  git add .env.template docker-compose.yml docker/keycloak/realm-admin.json docker/nginx/nginx-ssl.conf
  run git commit -m "Change domain"
  [ "$status" -eq 0 ]
}
```

---

## 📊 METRICS & VALIDATION

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Template generation time | <5s | `time make generate-configs` |
| Pre-commit hook runtime | <3s | `time .git/hooks/pre-commit` |
| CI check runtime | <30s | GitHub Actions duration |
| False positive rate | 0% | Manual validation (10 runs) |

### Validation Checklist

Before merging to main:

- [ ] `make generate-configs` succeeds with clean .env
- [ ] `make generate-configs` fails when DOMAIN missing
- [ ] Pre-commit hook blocks dirty templates
- [ ] CI workflow fails on template drift
- [ ] All 3 generators (realm, nginx, compose) work
- [ ] Error messages are developer-friendly
- [ ] Documentation updated (README.md)

---

## 🔗 DEPENDENCIES

### Upstream
- `.env.template` existence (EPIC-007 baseline)
- Template files existence:
  - `docker-compose.template.yml`
  - `docker/keycloak/realm-admin.template.json`
  - `docker/nginx/nginx-ssl.conf.template`

### Downstream
- **INF-002** (Template Syntax) - Uses output of this generator
- **INF-014** (Build Doctor) - Calls `generate-configs` in pre-flight

### External Tools
- `envsubst` (GNU gettext)
- `docker` (for config validation)
- `bash` >= 4.0

---

## 📝 IMPLEMENTATION CHECKLIST

### Phase 1: Script Creation (Day 1)
- [ ] Create `scripts/generate-all-configs.sh`
- [ ] Create `scripts/validate-env.sh` (enhanced)
- [ ] Create `scripts/templates/generate-nginx.sh`
- [ ] Create `scripts/templates/generate-compose.sh`
- [ ] Test all scripts manually

### Phase 2: Git Integration (Day 1)
- [ ] Create `.githooks/pre-commit`
- [ ] Add `make install-hooks` target to Makefile
- [ ] Test hook with dirty/clean templates

### Phase 3: CI/CD (Day 2)
- [ ] Create `.github/workflows/template-check.yml`
- [ ] Test workflow on PR
- [ ] Verify PR comment on failure

### Phase 4: Testing (Day 2)
- [ ] Write unit tests (BATS framework)
- [ ] Write integration tests
- [ ] E2E workflow validation

### Phase 5: Documentation (Day 2)
- [ ] Update README.md (developer workflow)
- [ ] Update copilot-instructions.md
- [ ] Create runbook: `docs/runbooks/template-generation.md`

---

## 📚 REFERENCES

1. **SECURITY_CONFIG_AUDIT.md** - Template system analysis
2. **copilot-golden-rules.md** - Template workflow rules
3. **docker/keycloak/generate-realm.sh** - Existing realm generator (reference)
4. **Makefile** - Integration points

---

## ✅ DEFINITION OF DONE

- [x] Master generator script (`generate-all-configs.sh`) vytvořen
- [x] Environment validation script (`validate-env.sh`) funguje
- [x] Pre-commit hook blokuje dirty templates
- [x] CI workflow validuje template sync
- [x] Všechny unit/integration/E2E testy pass
- [x] Documentation commitnuta
- [x] PR reviewed & merged
- [x] Developer onboarding: 1 nový dev úspěšně použil workflow

---

**Created:** 8. listopadu 2025  
**Story Owner:** Platform Team  
**Reviewer:** Martin Horak (@Muriel2Horak)  
**Status:** 🔴 Ready for Implementation
