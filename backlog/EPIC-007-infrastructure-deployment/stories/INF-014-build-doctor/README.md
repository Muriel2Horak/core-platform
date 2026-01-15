---
id: INF-014
epic: EPIC-007-infrastructure-deployment
title: "Build Doctor Pre-Flight Checks"
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
    - backlog/EPIC-007-infrastructure-deployment/stories/INF-014-build-doctor/README.md
    - backlog/EPIC-007-infrastructure-deployment/README.md
---

# INF-014: Build Doctor Pre-Flight Checks

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 2 dny, ~600 LOC  
**Owner:** Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**
```bash
# Developer běží build → fail po 20 minutách:
make clean
# ... 20 minutes later ...
# ERROR: Port 443 already in use!
# ERROR: Disk full (10MB free)
# ERROR: .env missing DOMAIN variable

# 😤 Ztraceno 20 minut!
```

**Git Evidence:**
- BUILD_DOCTOR_IMPLEMENTATION.md existuje ale není enforced
- Makefile má `doctor` target ale není pre-requisite pro `build`
- Chybí validace před dlouhými builds

**Goal:**

```bash
# Pre-flight checks PŘED buildem:
make build
# 🔍 Build Doctor: Running pre-flight checks...
# ✅ .env file exists and complete
# ✅ Docker daemon running
# ✅ Ports available (80, 443, 8080)
# ✅ Disk space sufficient (>10GB)
# ✅ Templates in sync
# 🚀 Starting build...
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Mandatory Pre-Flight Checks**
   - .env completeness (all required vars set)
   - Docker daemon status
   - Port availability (80, 443, 8080, 5432, 6379, 9092)
   - Disk space (>10GB free)
   - Templates sync (generated configs match templates)

2. ✅ **Fail-Fast Behavior**
   - Exit immediately if ANY check fails
   - Clear error message (what failed + how to fix)
   - No build started if pre-flight fails

3. ✅ **Auto-Run on Build Targets**
   - `make build` → automatic doctor check
   - `make up` → automatic doctor check
   - `make clean` → automatic doctor check
   - `make deploy` → automatic doctor check

4. ✅ **Bypass Option**
   - `SKIP_DOCTOR=true make build` (for CI/CD)
   - Warning logged when skipped

### Non-Functional Requirements

1. **Speed:** <10s for all checks
2. **Reliability:** Zero false positives
3. **Usability:** Friendly error messages

---

## Implementační tasky

- [TASK-014-01: Build Doctor core checks](subtasks/TASK-014-01-build-doctor-core.md)
- [TASK-014-02: Makefile integration + skip flag](subtasks/TASK-014-02-makefile-integration.md)
- [TASK-014-03: Template sync check](subtasks/TASK-014-03-template-sync-check.md)
- [TASK-014-04: Docs + usage guide](subtasks/TASK-014-04-docs-usage.md)

---

## 🏗️ IMPLEMENTATION

### File Structure

```
scripts/build-doctor/
├── Makefile.doctor              # Makefile module
├── pre-build-checks.sh          # All pre-flight checks
├── check-env.sh                 # .env validation
├── check-docker.sh              # Docker daemon check
├── check-ports.sh               # Port availability
├── check-disk.sh                # Disk space check
└── check-templates.sh           # Template sync check
```

### Master Check Script

**File:** `scripts/build-doctor/pre-build-checks.sh`

```bash
#!/bin/bash
set -euo pipefail

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${GREEN}🔍 Build Doctor: Running pre-flight checks...${NC}"
echo ""

CHECKS_PASSED=0
CHECKS_FAILED=0

# Check 1: .env file
echo "📋 Check 1/5: Environment file..."
if bash scripts/build-doctor/check-env.sh; then
    echo -e "${GREEN}✅ .env complete${NC}"
    ((CHECKS_PASSED++))
else
    echo -e "${RED}❌ .env validation failed${NC}"
    ((CHECKS_FAILED++))
fi
echo ""

# Check 2: Docker daemon
echo "🐳 Check 2/5: Docker daemon..."
if bash scripts/build-doctor/check-docker.sh; then
    echo -e "${GREEN}✅ Docker running${NC}"
    ((CHECKS_PASSED++))
else
    echo -e "${RED}❌ Docker check failed${NC}"
    ((CHECKS_FAILED++))
fi
echo ""

# Check 3: Port availability
echo "🔌 Check 3/5: Port availability..."
if bash scripts/build-doctor/check-ports.sh; then
    echo -e "${GREEN}✅ All ports available${NC}"
    ((CHECKS_PASSED++))
else
    echo -e "${RED}❌ Port check failed${NC}"
    ((CHECKS_FAILED++))
fi
echo ""

# Check 4: Disk space
echo "💾 Check 4/5: Disk space..."
if bash scripts/build-doctor/check-disk.sh; then
    echo -e "${GREEN}✅ Sufficient disk space${NC}"
    ((CHECKS_PASSED++))
else
    echo -e "${RED}❌ Disk space check failed${NC}"
    ((CHECKS_FAILED++))
fi
echo ""

# Check 5: Template sync
echo "📄 Check 5/5: Template synchronization..."
if bash scripts/build-doctor/check-templates.sh; then
    echo -e "${GREEN}✅ Templates in sync${NC}"
    ((CHECKS_PASSED++))
else
    echo -e "${RED}❌ Template sync failed${NC}"
    ((CHECKS_FAILED++))
fi
echo ""

# Summary
if [ $CHECKS_FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 All checks passed ($CHECKS_PASSED/5)${NC}"
    echo -e "${GREEN}🚀 Ready to build!${NC}"
    exit 0
else
    echo -e "${RED}❌ $CHECKS_FAILED checks failed${NC}"
    echo -e "${YELLOW}💡 Fix issues above and retry${NC}"
    exit 1
fi
```

### Individual Check Scripts

**File:** `scripts/build-doctor/check-ports.sh`

```bash
#!/bin/bash
set -euo pipefail

REQUIRED_PORTS=(80 443 8080 5432 6379 9092 3000 4000)
BLOCKED_PORTS=()

for PORT in "${REQUIRED_PORTS[@]}"; do
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        BLOCKED_PORTS+=($PORT)
        PROCESS=$(lsof -Pi :$PORT -sTCP:LISTEN | tail -1 | awk '{print $1}')
        echo "⚠️  Port $PORT in use (process: $PROCESS)"
    fi
done

if [ ${#BLOCKED_PORTS[@]} -eq 0 ]; then
    exit 0
else
    echo ""
    echo "💡 Free ports with:"
    for PORT in "${BLOCKED_PORTS[@]}"; do
        echo "   sudo lsof -ti:$PORT | xargs kill -9"
    done
    exit 1
fi
```

### Makefile Integration

**File:** `Makefile` (updated)

```makefile
# Include Build Doctor module
include scripts/build-doctor/Makefile.doctor

# Override SKIP_DOCTOR via env var
SKIP_DOCTOR ?= false

# Wrapped build target
.PHONY: build
build: doctor-pre-build
ifeq ($(SKIP_DOCTOR),true)
	@echo "⚠️  Skipping Build Doctor (SKIP_DOCTOR=true)"
endif
	@$(MAKE) build-impl

.PHONY: build-impl
build-impl:
	@echo "🔨 Building all images..."
	docker compose build

# All major targets use doctor
up: doctor-pre-build up-impl
clean: doctor-pre-build clean-impl
deploy: doctor-pre-build deploy-impl
```

---

## 🧪 TESTING

### Unit Tests

```bash
@test "check-env fails when DOMAIN missing" {
  # Create .env without DOMAIN
  cp .env.template .env
  sed -i '/^DOMAIN=/d' .env
  
  run bash scripts/build-doctor/check-env.sh
  [ "$status" -ne 0 ]
  [[ "$output" =~ "DOMAIN" ]]
}
```

---

## ✅ DEFINITION OF DONE

- [x] 5 pre-flight checks implemented
- [x] Makefile integrates doctor automatically
- [x] Fail-fast behavior works
- [x] Error messages helpful
- [x] <10s execution time
- [x] Tests pass

---

**Reference:** BUILD_DOCTOR_IMPLEMENTATION.md
