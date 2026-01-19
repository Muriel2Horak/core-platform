---
id: INF-011
epic: EPIC-007-infrastructure-deployment
title: "Multi-Environment Configuration Isolation"
priority: P1
status: done
assignee: ""
created: 2025-11-08
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths:
    - docker/.env
    - docker/.env.development
    - docker/.env.production
    - docker/.env.staging
  test_paths: []
  docs_paths:
    - backlog/EPIC-007-infrastructure-deployment/stories/INF-011-environment-isolation/README.md
    - backlog/EPIC-007-infrastructure-deployment/README.md
---

# INF-011: Multi-Environment Configuration Isolation

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** ✅ **DONE**
**Priority:** HIGH  
**Effort:** 2 dny, ~500 LOC  
**Owner:** Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**
```bash
# Máme multiple .env soubory ale NEJSOU použité:
.env.development    # Exists but NOT used
.env.staging        # Exists but NOT used
.env.production     # Exists but NOT used

# Build používá pouze:
.env                # Single environment (problém!)
```

**Issues:**
- Dev/staging/prod používají STEJNÉ konfigurace
- Developer musí ručně editovat .env pro změnu prostředí
- Risk: Produkční secrets v dev prostředí

### Goal

**Environment-specific overrides:**

```bash
# Makefile:
ENV ?= development

up:
	docker compose --env-file .env \
	               --env-file docker/.env.$(ENV) \
	               up -d
```

**Environment files:**
```bash
.env.template                    # Defaults (committed)
docker/.env.development          # Dev overrides
docker/.env.staging              # Staging overrides
docker/.env.production           # Production overrides
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **3 Environment Configs**
   - Development: localhost, mock services, debug logging
   - Staging: test.domain.com, real services, info logging
   - Production: prod.domain.com, HA services, warn logging

2. ✅ **Easy Switching**
   ```bash
   make up ENV=development  # Dev environment
   make up ENV=staging      # Staging
   make up ENV=production   # Production (default)
   ```

3. ✅ **Validation**
   - Check: ENV value is valid (dev/staging/prod)
   - Check: Environment-specific .env file exists
   - Fail-fast if invalid

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [TASK-011-01: Environment override files](subtasks/TASK-011-01-env-files.md) | 6h | none |
| 2 | [TASK-011-02: Makefile wiring + validation](subtasks/TASK-011-02-makefile-validation.md) | 6h | TASK-011-01 |
| 3 | [TASK-011-03: Docs + usage guide](subtasks/TASK-011-03-docs-usage.md) | 4h | TASK-011-01, TASK-011-02 |

### Implementation

**File:** `docker/.env.development`

```bash
# Development Overrides
DOMAIN=localhost

# Use localhost services (no external deps)
KEYCLOAK_BASE_URL=http://localhost:8080
DATABASE_URL=jdbc:postgresql://localhost:5432/core

# Debug logging
LOG_LEVEL=DEBUG
SPRING_PROFILES_ACTIVE=development

# Disable SSL (localhost)
SSL_ENABLED=false
```

**File:** `docker/.env.production`

```bash
# Production Overrides
DOMAIN=core-platform.com

# Production URLs
KEYCLOAK_BASE_URL=https://auth.core-platform.com
DATABASE_URL=jdbc:postgresql://db-prod.internal:5432/core

# Production logging
LOG_LEVEL=WARN
SPRING_PROFILES_ACTIVE=production

# SSL required
SSL_ENABLED=true
```

**File:** `Makefile`

```makefile
# Environment selection
ENV ?= production
VALID_ENVS := development staging production

# Validate ENV
.PHONY: validate-env
validate-env:
ifeq ($(filter $(ENV),$(VALID_ENVS)),)
	@echo "❌ Invalid ENV: $(ENV)"
	@echo "💡 Valid values: $(VALID_ENVS)"
	@exit 1
endif
	@if [ ! -f docker/.env.$(ENV) ]; then \
		echo "❌ Environment file not found: docker/.env.$(ENV)"; \
		exit 1; \
	fi

# Updated up target
.PHONY: up
up: validate-env
	@echo "🚀 Starting $(ENV) environment..."
	docker compose --env-file .env \
	               --env-file docker/.env.$(ENV) \
	               up -d
```

**Effort:** 2 dny  
**LOC:** ~500  
**Priority:** HIGH

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
