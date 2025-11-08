# REFACTORING TASKS - Infrastructure Modernization

**Epic:** EPIC-007 Infrastructure & Deployment  
**Purpose:** Technical debt cleanup discovered during infrastructure audit  
**Priority:** HIGH (blocks clean implementation of INF stories)  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

Během analýzy infrastructure (SECURITY_CONFIG_AUDIT.md, Git history, Makefile) jsme objevili **kritický tech debt**, který musí být vyřešen PŘED nebo BĚHEM implementace INF stories.

---

## 🔧 REFACTORING TASKS

### REFACTOR-001: Consolidate .env Variants

**Problem:**
```bash
# Našli jsme 5 různých .env souborů:
.env                      # Main (generated)
.env.template            # Template (source)
.env.development         # Dev (unused!)
.env.staging             # Staging (unused!)
.env.production          # Production (unused!)
.env.test                # Test (unused!)
.env.example             # Example (outdated!)
.env.backup              # Backup (manual!)
```

**Current State:**
- Soubory existují ale NEJSOU používané
- Build process ignoruje .env.development/staging/production
- Developer neví který soubor editovat

**Target State:**
```bash
.env.template                    # Source of truth (commit to Git)
.env                            # Generated (local, .gitignored)
docker/.env.development         # Dev overrides (used by docker-compose)
docker/.env.staging             # Staging overrides
docker/.env.production          # Production overrides
```

**Implementation:**

```bash
#!/bin/bash
# scripts/refactor/consolidate-env-files.sh

# 1. Move environment-specific files to docker/
mv .env.development docker/.env.development
mv .env.staging docker/.env.staging
mv .env.production docker/.env.production
mv .env.test docker/.env.test

# 2. Delete duplicates/backups
rm -f .env.example .env.backup

# 3. Update Makefile to use environment-specific files
# Makefile:
# ENV ?= development
# up:
#     docker compose --env-file .env --env-file docker/.env.$(ENV) up -d

# 4. Update .gitignore
cat >> .gitignore << EOF
# Environment files
.env
.env.local
.env.*.local
!docker/.env.*  # Exception: docker env files are committed
EOF
```

**Effort:** 1 den  
**LOC:** ~200 (scripts + Makefile updates)  
**Blocks:** INF-011 (Environment Isolation)

---

### REFACTOR-002: Remove Hardcoded DB URLs from application.properties

**Problem (SECURITY_CONFIG_AUDIT.md page 7):**

```properties
# backend/src/main/resources/application.properties
spring.datasource.url=jdbc:postgresql://db:5432/core          # ❌ HARDCODED!
keycloak.datasource.url=jdbc:postgresql://db:5432/keycloak    # ❌ HARDCODED!

# PROBLÉM: Tyto hodnoty PŘEPÍŠÍ environment variables!
# Properties file má VYŠŠÍ prioritu než env vars
```

**Impact:**
- `DATABASE_URL` z `.env` se IGNORUJE
- Nelze override DB host/port v produkci
- Dev Container nemůže použít jiný DB host

**Target State:**

```yaml
# backend/src/main/resources/application.yml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://core-db:5432/core}
    username: ${DATABASE_USERNAME:core}
    password: ${DATABASE_PASSWORD}

keycloak:
  datasource:
    url: ${KEYCLOAK_DATABASE_URL:jdbc:postgresql://core-db:5432/keycloak}
    username: ${KEYCLOAK_DB_USERNAME:core}
    password: ${KEYCLOAK_DB_PASSWORD}
```

**Implementation:**

```bash
#!/bin/bash
# scripts/refactor/remove-hardcoded-db-urls.sh

# 1. Delete hardcoded lines from application.properties
sed -i '/^spring\.datasource\.url=/d' backend/src/main/resources/application.properties
sed -i '/^keycloak\.datasource\.url=/d' backend/src/main/resources/application.properties

# 2. Verify application.yml has ${DATABASE_URL} placeholders
grep -q '${DATABASE_URL' backend/src/main/resources/application.yml || {
    echo "ERROR: application.yml missing DATABASE_URL placeholder!"
    exit 1
}

# 3. Test backend startup
make rebuild-backend
make verify
```

**Effort:** 0.5 dne  
**LOC:** ~50 (cleanup + verification)  
**Blocks:** INF-001 (Template Generator), INF-011 (Environment Isolation)  
**CRITICAL:** Ano - aplikace nemůže číst .env vars správně!

---

### REFACTOR-003: Fix .env.example Database Name Mismatch

**Problem:**

```bash
# .env.example:44
DATABASE_URL=jdbc:postgresql://core-db:5432/core_platform  # ❌ WRONG DB NAME!

# Actual DB name (docker/postgres/init-multi-db.sh):
POSTGRES_DB=core  # ✅ CORRECT
```

**Impact:**
- Nový developer kopíruje `.env.example` → aplikace nenastartuje
- Error: `database "core_platform" does not exist`

**Target State:**

```bash
# .env.example
DATABASE_URL=jdbc:postgresql://core-db:5432/core  # ✅ FIXED
```

**Implementation:**

```bash
#!/bin/bash
# scripts/refactor/fix-env-example.sh

# Fix database name in .env.example
sed -i 's|core_platform|core|g' .env.example
sed -i 's|core_platform|core|g' .env.backup

# Verify
grep -q "core_db:5432/core" .env.example || {
    echo "ERROR: .env.example still has wrong DB name!"
    exit 1
}

echo "✅ .env.example fixed"
```

**Effort:** 0.1 dne (trivial fix)  
**LOC:** ~10  
**Priority:** MEDIUM (confusing for new devs)

---

### REFACTOR-004: Unify Nginx Template Generation

**Problem:**

```bash
# Našli jsme 3 různé nginx template soubory:
docker/nginx/nginx-ssl.conf.template
docker/nginx/nginx-ssl.conf.template.backup
docker/nginx/nginx-ssl.conf.template.bak

# Není jasné který je "source of truth"
```

**Target State:**

```bash
docker/nginx/
├── nginx-ssl.conf.template       # Source (committed)
├── nginx-ssl.conf                # Generated (gitignored)
└── generate-nginx.sh             # Generator script
```

**Implementation:**

```bash
#!/bin/bash
# scripts/refactor/cleanup-nginx-templates.sh

# 1. Keep only the main template
rm -f docker/nginx/nginx-ssl.conf.template.backup
rm -f docker/nginx/nginx-ssl.conf.template.bak

# 2. Add nginx-ssl.conf to .gitignore
echo "docker/nginx/nginx-ssl.conf" >> .gitignore

# 3. Create generator script (used by INF-001)
cat > docker/nginx/generate-nginx.sh << 'EOF'
#!/bin/bash
set -euo pipefail
source .env
envsubst < docker/nginx/nginx-ssl.conf.template > docker/nginx/nginx-ssl.conf
nginx -t -c docker/nginx/nginx-ssl.conf  # Validate syntax
EOF

chmod +x docker/nginx/generate-nginx.sh
```

**Effort:** 0.5 dne  
**LOC:** ~100  
**Blocks:** INF-001 (Template Generator)

---

### REFACTOR-005: Deprecate Non-Functional Dev Targets

**Problem (copilot-instructions.md):**

```makefile
# Makefile targets that DON'T WORK:
make dev-up         # ❌ NEFUNGUJE
make dev-watch      # ❌ NEFUNGUJE
make dev-down       # ❌ NEFUNGUJE
make dev-clean      # ❌ NEFUNGUJE

# Developer je matený:
# "Který target použít pro dev mode?"
```

**Target State:**

```makefile
# Makefile - remove deprecated targets, add deprecation warnings

# OLD TARGETS (deprecated)
.PHONY: dev-up
dev-up:
	@echo "❌ DEPRECATED: 'make dev-up' nefunguje v tomto projektu!"
	@echo "💡 Use instead: make clean-fast (5-10 min rebuild bez E2E)"
	@exit 1

.PHONY: dev-watch
dev-watch:
	@echo "❌ DEPRECATED: 'make dev-watch' nefunguje!"
	@echo "💡 Use instead: make rebuild-backend (backend hot reload)"
	@exit 1

# NEW RECOMMENDED WORKFLOW
.PHONY: dev
dev: clean-fast
	@echo "✅ Dev environment started (without E2E tests)"
	@echo "💡 Backend: make rebuild-backend"
	@echo "💡 Frontend: hot reload automaticky"
```

**Implementation:**

```bash
#!/bin/bash
# scripts/refactor/deprecate-dev-targets.sh

# 1. Add deprecation warnings to Makefile
cat >> Makefile << 'EOF'

# ====================================
# DEPRECATED TARGETS (DO NOT USE)
# ====================================
.PHONY: dev-up dev-watch dev-down dev-clean
dev-up dev-watch dev-down dev-clean:
	@echo "❌ DEPRECATED: This target doesn't work in this project!"
	@echo "💡 Use instead:"
	@echo "   make clean-fast    # Dev rebuild (5-10 min, no E2E)"
	@echo "   make rebuild       # Fast rebuild (2-5 min, unit tests)"
	@exit 1
EOF

# 2. Update README.md with correct dev workflow
sed -i 's/make dev-up/make clean-fast/g' README.md
sed -i 's/make dev-watch/make rebuild-backend/g' README.md
```

**Effort:** 0.5 dne  
**LOC:** ~150  
**Priority:** HIGH (developer confusion)

---

### REFACTOR-006: Centralize Build Doctor Configuration

**Problem:**

```makefile
# Makefile má build doctor config scattered:
# Line 15: BUILD_DOCTOR_ENABLED
# Line 143: doctor target
# Line 287: wait-for-services
# Line 512: verify target
# Není clear ownership
```

**Target State:**

```makefile
# Makefile - Build Doctor section (lines 1-50)

# ====================================
# BUILD DOCTOR CONFIGURATION
# ====================================
BUILD_DOCTOR_ENABLED ?= true
BUILD_DOCTOR_LOG_DIR := diagnostics
BUILD_DOCTOR_TIMESTAMP := $(shell date +%Y%m%d-%H%M%S)

# Include build doctor module
include scripts/build-doctor/Makefile.doctor

# All build targets use doctor
build: doctor-pre-build docker-build doctor-post-build
up: doctor-pre-up docker-up doctor-post-up
clean: doctor-pre-clean clean-impl doctor-post-clean
```

**Implementation:**

```bash
#!/bin/bash
# scripts/refactor/centralize-build-doctor.sh

# 1. Extract build doctor logic to separate file
mkdir -p scripts/build-doctor
cat > scripts/build-doctor/Makefile.doctor << 'EOF'
# Build Doctor Module

.PHONY: doctor-pre-build
doctor-pre-build:
	@echo "🔍 Build Doctor: Pre-build checks..."
	@bash scripts/build-doctor/pre-build-checks.sh

.PHONY: doctor-post-build
doctor-post-build:
	@echo "✅ Build Doctor: Post-build validation..."
	@bash scripts/build-doctor/post-build-checks.sh
EOF

# 2. Update main Makefile to include module
sed -i '15a include scripts/build-doctor/Makefile.doctor' Makefile
```

**Effort:** 1 den  
**LOC:** ~300  
**Blocks:** INF-014 (Build Doctor Pre-Flight Checks)

---

### REFACTOR-007: Remove Duplicate docker-compose Files

**Problem:**

```bash
# Našli jsme multiple docker-compose backups:
docker-compose.yml
docker-compose.yml.backup
docker-compose.yml.20241015
docker-compose.template.yml
.devcontainer/docker-compose.devcontainer.yml

# Není clear který je "active"
```

**Target State:**

```bash
docker-compose.template.yml              # Source (committed)
docker-compose.yml                       # Generated (gitignored)
.devcontainer/docker-compose.devcontainer.yml  # Dev overlay (committed)
```

**Implementation:**

```bash
#!/bin/bash
# scripts/refactor/cleanup-docker-compose.sh

# 1. Remove backups
rm -f docker-compose.yml.backup
rm -f docker-compose.yml.20241015
rm -f docker-compose.yml.bak

# 2. Add docker-compose.yml to .gitignore (generated file)
echo "docker-compose.yml" >> .gitignore

# 3. Keep template + devcontainer overlay only
ls docker-compose* .devcontainer/docker-compose*
```

**Effort:** 0.5 dne  
**LOC:** ~50  
**Priority:** MEDIUM

---

### REFACTOR-008: Standardize Secret File Naming

**Problem:**

```bash
# Inconsistent naming:
docker/ssl/server.key.pem      # SSL key
docker/ssl/server.crt.pem      # SSL cert
docker/ssl/ca.crt.pem          # CA cert

# But environment vars use different names:
SSL_KEY_PATH=./docker/ssl/server.key.pem
SSL_CERT_PATH=./docker/ssl/server.crt.pem
```

**Target State:**

```bash
# Standardized naming (align with env vars):
docker/ssl/
├── ssl-key.pem          # Renamed from server.key.pem
├── ssl-cert.pem         # Renamed from server.crt.pem
├── ca-cert.pem          # Renamed from ca.crt.pem
└── generate-ssl.sh      # Updated to use new names

# Environment vars:
SSL_KEY_PATH=./docker/ssl/ssl-key.pem
SSL_CERT_PATH=./docker/ssl/ssl-cert.pem
CA_CERT_PATH=./docker/ssl/ca-cert.pem
```

**Implementation:**

```bash
#!/bin/bash
# scripts/refactor/rename-ssl-files.sh

cd docker/ssl

# Rename files
mv server.key.pem ssl-key.pem
mv server.crt.pem ssl-cert.pem
mv ca.crt.pem ca-cert.pem

# Update references in docker-compose.yml, Dockerfile, etc.
find ../.. -type f -name "*.yml" -o -name "Dockerfile*" | xargs sed -i 's|server\.key\.pem|ssl-key.pem|g'
find ../.. -type f -name "*.yml" -o -name "Dockerfile*" | xargs sed -i 's|server\.crt\.pem|ssl-cert.pem|g'

# Update .env.template
sed -i 's|server\.key\.pem|ssl-key.pem|g' ../../.env.template
sed -i 's|server\.crt\.pem|ssl-cert.pem|g' ../../.env.template
```

**Effort:** 0.5 dne  
**LOC:** ~100  
**Blocks:** INF-005 (Let's Encrypt SSL)

---

## 📊 REFACTORING ROADMAP

### Phase 1: Critical Blockers (Week 1)

**Must complete BEFORE starting INF stories:**

```bash
✅ REFACTOR-002: Remove hardcoded DB URLs        (0.5d) - CRITICAL
✅ REFACTOR-004: Unify Nginx templates           (0.5d) - Blocks INF-001
✅ REFACTOR-006: Centralize Build Doctor         (1d)   - Blocks INF-014
```

**Total:** 2 days

### Phase 2: Template System Cleanup (Week 1-2)

**Complete DURING INF-001 implementation:**

```bash
✅ REFACTOR-001: Consolidate .env variants       (1d)   - Part of INF-011
✅ REFACTOR-007: Remove docker-compose duplicates (0.5d)
✅ REFACTOR-008: Standardize SSL naming          (0.5d) - Before INF-005
```

**Total:** 2 days

### Phase 3: Developer Experience (Week 2)

**Can be done in parallel with INF stories:**

```bash
✅ REFACTOR-003: Fix .env.example                (0.1d) - Trivial
✅ REFACTOR-005: Deprecate dev-* targets         (0.5d)
```

**Total:** 0.6 days

---

## 🧪 VALIDATION CHECKLIST

Po každém refactoringu:

- [ ] `make clean-fast` úspěšný
- [ ] `make verify` pass
- [ ] Unit tests pass (`make test-backend`)
- [ ] E2E smoke tests pass (`make test-e2e-pre`)
- [ ] Git commit s detailním message
- [ ] Documentation updated

---

## 📈 METRICS

| Refactoring | Before | After | Improvement |
|-------------|--------|-------|-------------|
| .env files | 8 duplicates | 3 organized | -62% files |
| docker-compose | 4 copies | 2 (template + overlay) | -50% files |
| Hardcoded URLs | 2 in properties | 0 (all from env) | 100% fixed |
| Nginx templates | 3 variants | 1 source | -66% files |
| Build Doctor | Scattered | Centralized module | Maintainable |

---

## ✅ DEFINITION OF DONE

**All refactorings complete when:**

- [x] Zero hardcoded configuration values
- [x] All .env variants organized (docker/.env.{env})
- [x] No duplicate templates (1 source per config)
- [x] Deprecated targets fail with helpful messages
- [x] Build Doctor centralized in `scripts/build-doctor/`
- [x] SSL files consistently named
- [x] All tests pass post-refactoring
- [x] Documentation updated

---

**Created:** 8. listopadu 2025  
**Owner:** Platform Team  
**Status:** 🔴 Ready to Start (Week 1 - Critical Blockers)  
**Total Effort:** ~5 days (can be parallelized with INF stories)
