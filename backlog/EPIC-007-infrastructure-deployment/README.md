# EPIC-007: Infrastructure & Deployment Excellence

**Status:** 🔴 TODO (0% Complete)  
**Priority:** 🔥 CRITICAL  
**Owner:** DevOps + Platform Team  
**Created:** 8. listopadu 2025  
**Target:** Q4 2025

> **CRITICAL GAP**: Deployment & environment management byl největší blocker progress projektu. Tento EPIC řeší systematicky všechny infrastructure pain points zjištěné během vývoje.

---

## 📋 EXECUTIVE SUMMARY

### Problém

Z **98 deployment-related Git commits** vyplývá:
- 🔴 **Template chaos**: 3 substitution mechanismy (envsubst, Docker ${}, Spring ${})
- 🔴 **SSL/TLS hell**: Self-signed certs, hardcoded paths, rotation nightmare
- 🔴 **Secrets plain-text**: 47 env vars, 12 secrets/passwords v plain textu
- 🔴 **Shared DB user**: Všechny služby běží jako `core` → security risk
- 🔴 **No env separation**: Dev/Test/Prod používají stejné konfigurace
- 🔴 **Build complexity**: `make clean` vs `clean-fast` vs `rebuild` matoucí

### Řešení

**8-Phase Infrastructure Overhaul:**
1. **Template System** - Unifikovaný build-time config generation
2. **Secrets Management** - Plain-text → Vault/Docker Secrets migration
3. **SSL/TLS Automation** - Let's Encrypt + auto-rotation
4. **DB Security** - Separate users per service (least privilege)
5. **Flyway Coordination** - Multi-DB versioning + rollback
6. **Environment Separation** - Dev/Test/Prod config isolation
7. **Build Orchestration** - Simplified Makefile + CI/CD pipeline
8. **Monitoring & Validation** - Build Doctor + health checks

---

## 🎯 SUCCESS CRITERIA

| Metric | Current | Target | Status |
|--------|---------|--------|--------|
| **Deployment Time** | 30-40 min (`make clean`) | <10 min rebuild | 🔴 |
| **Config Sources** | 6 různých míst | 2 (templates + vault) | 🔴 |
| **Secrets in Git** | .env plain-text | 0 (all in Vault) | 🔴 |
| **SSL Cert Rotation** | Manual (90 days) | Auto Let's Encrypt | 🔴 |
| **DB Users** | 1 shared (`core`) | 1 per service (3 total) | 🔴 |
| **Environment Parity** | Dev ≠ Prod | Dev == Prod (12-factor) | 🔴 |
| **Build Doctor Pass Rate** | N/A | 100% pass before deploy | 🔴 |
| **Rollback Time** | Unknown | <5 min (Flyway) | 🔴 |

---

## 📊 CURRENT STATE ANALYSIS

### Configuration Sources (6 Different Places!)

```
1. .env.template (47 variables)
   ├─→ envsubst → docker-compose.yml
   ├─→ envsubst → realm-admin.json
   └─→ envsubst → nginx-ssl.conf

2. application.yml (Spring Boot)
   └─→ ${ENV_VAR} → runtime

3. docker-compose.template.yml
   └─→ ${VARIABLE} → Docker Compose

4. realm-admin.template.json
   └─→ envsubst → Keycloak import

5. nginx-ssl.conf.template
   └─→ envsubst → Nginx runtime

6. Hardcoded values v kódu
   └─→ application.properties (⚠️ BYPASSES env vars!)
```

### Secrets Inventory (12 Plain-Text!)

| Secret | Location | Rotation | Encrypted |
|--------|----------|----------|-----------|
| `POSTGRES_PASSWORD` | .env | Manual | ❌ |
| `DATABASE_PASSWORD` | .env | Manual | ❌ |
| `KEYCLOAK_ADMIN_PASSWORD` | .env | Manual | ❌ |
| `KEYCLOAK_ADMIN_CLIENT_SECRET` | .env | Manual | ❌ |
| `GRAFANA_ADMIN_PASSWORD` | .env | Manual | ❌ |
| `GRAFANA_DB_PASSWORD` | .env | Manual | ❌ |
| `MINIO_ROOT_PASSWORD` | .env | Manual | ❌ |
| `REDIS_PASSWORD` | .env | Empty! | ❌ |
| `SSL_KEY` | docker/ssl/server.key.pem | Manual | ❌ |
| `JWT_SIGNING_KEY` | Runtime generated | None | ❌ |
| `N8N_CLIENT_SECRET` | .env | Manual | ❌ |
| `CUBE_API_TOKEN` | .env | Empty | ❌ |

### Template System Flows

```mermaid
graph LR
    A[.env.template] -->|envsubst| B[docker-compose.yml]
    A -->|envsubst| C[realm-admin.json]
    A -->|envsubst| D[nginx-ssl.conf]
    E[application.yml] -->|Spring ${}| F[Backend Runtime]
    B -->|Docker Compose| G[Containers]
    C -->|Keycloak Import| H[Auth Server]
    D -->|Nginx Config| I[Reverse Proxy]
```

### Build Process Pain Points

**Make Targets Confusion:**
- `make up` - Start (může failnout kvůli old image)
- `make rebuild` - S cache (rychlé, ale může být stale)
- `make rebuild-clean` - Bez cache (slow, ale clean)
- `make clean` - Rebuild + FULL E2E (30-40 min!)
- `make clean-fast` - Rebuild BEZ E2E (dev mode, 5-10 min)
- `make dev-up` - DEPRECATED (nefunguje!)

**Problémy:**
- Developer neví který target použít
- E2E testy běží i když nejsou potřeba
- Build failures obtížné debugovat

---

## 🏗️ ARCHITECTURE OVERVIEW

### Target Infrastructure

```
┌─────────────────────────────────────────────────────────────┐
│ ENVIRONMENT LAYER (Dev / Test / Prod)                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  .env.{env}  →  Vault (Secrets)  →  12-Factor Config       │
│     ↓                ↓                      ↓               │
│  Templates   →   envsubst    →    Runtime Configs          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────────────┐
│ SSL/TLS LAYER (Automated)                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Let's Encrypt  →  ACME Challenge  →  Auto-Renewal (80d)   │
│        ↓                                                    │
│  Cert Manager  →  Kubernetes Secrets  →  Ingress          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────────────┐
│ DATABASE LAYER (Isolated Users)                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  PostgreSQL                                                 │
│   ├── core DB       →  User: core_app      (RW core only)  │
│   ├── keycloak DB   →  User: keycloak_app  (RW keycloak)   │
│   └── grafana DB    →  User: grafana_app   (RW grafana)    │
│                                                             │
│  Flyway Migrations (Versioned)                             │
│   ├── V1__initial_schema.sql                               │
│   ├── V2__add_workflows.sql                                │
│   └── V3__n8n_integration.sql                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────────────┐
│ BUILD ORCHESTRATION LAYER                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  make build    →  Build Doctor (pre-flight checks)         │
│       ↓                ↓                                    │
│  Docker Build  →  Health Checks  →  Integration Tests      │
│       ↓                                                     │
│  Deployment    →  Smoke Tests   →  Rollback (if fail)      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 USER STORIES BREAKDOWN

### Phase 1: Template System Unification (Week 1)

#### INF-001: Centralized Template Generator
**Goal:** Single source of truth pro všechny konfigurace  
**Effort:** 400 LOC, 2 dny  
**Priority:** 🔥 CRITICAL

**Requirements:**
- Unifikovaný `scripts/generate-all-configs.sh`
- Validace že template variables existují v .env
- Idempotent (safe re-run)
- Pre-commit hook (CI check)

**Deliverables:**
- `scripts/generate-all-configs.sh` (master generator)
- `.env.template` validation script
- GitHub Actions workflow (template check)

---

#### INF-002: Template Syntax Standardization
**Goal:** 1 mechanismus místo 3 (envsubst, Docker ${}, Spring ${})  
**Effort:** 600 LOC, 3 dny  
**Priority:** HIGH

**Requirements:**
- Konvertovat všechny templates na envsubst syntax
- Spring Boot application.yml → external properties file
- Docker Compose → všechny env vars v .env (ne v compose)

**Migration:**
```yaml
# PŘED (Docker Compose)
environment:
  - DATABASE_URL=${DATABASE_URL}

# PO (envsubst template)
environment:
  - DATABASE_URL=$DATABASE_URL
```

---

### Phase 2: Secrets Management (Week 2)

#### INF-003: Docker Secrets Migration
**Goal:** Plain-text → Docker Secrets pro všechny credentials  
**Effort:** 800 LOC, 3 dny  
**Priority:** 🔥 CRITICAL

**Requirements:**
- Create secrets: `db_password`, `keycloak_admin_password`, atd.
- Update services: read from `/run/secrets/`
- Rotate all passwords (strong random generation)
- Zero secrets v Git

**Implementation:**
```yaml
# docker-compose.yml
secrets:
  db_password:
    file: ./secrets/db_password.txt  # .gitignored!

services:
  backend:
    secrets:
      - db_password
    environment:
      - DATABASE_PASSWORD_FILE=/run/secrets/db_password
```

---

#### INF-004: HashiCorp Vault Integration (Optional)
**Goal:** Centrální secret management s audit logging  
**Effort:** 1,200 LOC, 5 dní  
**Priority:** MEDIUM (po INF-003)

**Requirements:**
- Vault server deployment (Docker)
- Spring Cloud Vault client
- Secret rotation API
- Audit logging

---

### Phase 3: SSL/TLS Automation (Week 3)

#### INF-005: Let's Encrypt ACME Integration
**Goal:** Auto SSL certificate generation + renewal  
**Effort:** 600 LOC, 2 dny  
**Priority:** HIGH

**Requirements:**
- Traefik jako ACME client
- DNS-01 challenge (wildcard certs)
- Auto-renewal (80 days before expiry)
- Cert storage v volume (persist přes restart)

**Traefik config:**
```yaml
certificatesResolvers:
  letsencrypt:
    acme:
      email: admin@core-platform.local
      storage: /letsencrypt/acme.json
      dnsChallenge:
        provider: cloudflare
```

---

#### INF-006: SSL Certificate Monitoring
**Goal:** Alert když cert expiruje <30 days  
**Effort:** 300 LOC, 1 den  
**Priority:** MEDIUM

**Requirements:**
- Prometheus exporter (cert expiry metric)
- Grafana dashboard (SSL health)
- Alert rule (Slack notification)

---

### Phase 4: Database Security (Week 4)

#### INF-007: Separate DB Users Migration
**Goal:** 1 DB user per service (least privilege)  
**Effort:** 500 LOC, 2 dny  
**Priority:** 🔥 CRITICAL

**Requirements:**
- Create users: `core_app`, `keycloak_app`, `grafana_app`
- Grant minimal permissions (only own DB)
- Update connection strings
- Validate isolation (integration test)

**SQL Migration:**
```sql
-- Create separate users
CREATE USER core_app WITH PASSWORD '<vault-secret>';
GRANT ALL ON DATABASE core TO core_app;

-- Revoke old shared user
REVOKE ALL ON DATABASE keycloak FROM core;
```

**Reference:** `DB_SEPARATE_USERS_PLAN.md`

---

#### INF-008: Database Connection Pooling Tuning
**Goal:** Optimalizovat HikariCP settings per environment  
**Effort:** 400 LOC, 1 den  
**Priority:** MEDIUM

**Requirements:**
- Dev: `maximumPoolSize=5` (low load)
- Prod: `maximumPoolSize=20` (high load)
- Connection leak detection
- Metrics export (Prometheus)

---

### Phase 5: Flyway Multi-DB Coordination (Week 5)

#### INF-009: Flyway Migration Framework
**Goal:** Versioned DB migrations s rollback support  
**Effort:** 800 LOC, 3 dny  
**Priority:** HIGH

**Requirements:**
- 3 Flyway instances (core, keycloak, grafana DBs)
- Migration scripts: `V{version}__{description}.sql`
- Rollback scripts: `U{version}__{description}.sql`
- Pre-deployment validation (`make db-validate`)

**Directory structure:**
```
backend/src/main/resources/db/migration/
├── core/
│   ├── V1__initial_schema.sql
│   ├── V2__add_workflows.sql
│   └── U2__rollback_workflows.sql
├── keycloak/
│   └── V1__custom_tables.sql
└── grafana/
    └── V1__custom_dashboards.sql
```

---

#### INF-010: DB Backup & Restore Automation
**Goal:** Automated daily backups s point-in-time recovery  
**Effort:** 600 LOC, 2 dny  
**Priority:** MEDIUM

**Requirements:**
- pg_dump daily cron job
- Retention: 7 daily, 4 weekly, 12 monthly
- S3/MinIO storage
- Restore script: `make db-restore BACKUP=2025-11-08`

---

### Phase 6: Environment Separation (Week 6)

#### INF-011: Dev/Test/Prod Configuration Isolation
**Goal:** 3 oddělené .env soubory s minimal drift  
**Effort:** 500 LOC, 2 dny  
**Priority:** HIGH

**Requirements:**
- `.env.development` (localhost, mock services)
- `.env.test` (Testcontainers, ephemeral)
- `.env.production` (real services, SSL)
- Validation: environment parity check

**Makefile:**
```makefile
ENV ?= development

up:
	docker compose --env-file .env.$(ENV) up -d
```

---

#### INF-012: Feature Flags System
**Goal:** Toggle features per environment bez redeploy  
**Effort:** 800 LOC, 3 dny  
**Priority:** MEDIUM

**Requirements:**
- Spring Cloud Config Server
- Feature flags table (DB)
- Admin UI (enable/disable features)
- Canary rollout support

---

### Phase 7: Build Orchestration Simplification (Week 7)

#### INF-013: Simplified Makefile Targets
**Goal:** Méně targets, clear naming, fail-fast  
**Effort:** 400 LOC, 1 den  
**Priority:** HIGH

**Requirements:**
- `make dev` - Development mode (hot reload)
- `make build` - Build all images
- `make test` - All tests (unit + integration)
- `make deploy ENV=prod` - Deploy to environment
- Remove: `dev-up`, `clean-fast`, confusing aliases

---

#### INF-014: Build Doctor Pre-Flight Checks
**Goal:** Validace před buildem (prevent wasted time)  
**Effort:** 600 LOC, 2 dny  
**Priority:** 🔥 CRITICAL

**Requirements:**
- Check: .env completeness (všechny vars set)
- Check: Docker daemon running
- Check: Network ports available (80, 443, 8080)
- Check: Disk space (>10GB free)
- Exit early pokud checks fail

**Implementation:**
```bash
# scripts/build-doctor.sh
check_env_vars || exit 1
check_docker_daemon || exit 1
check_ports || exit 1
check_disk_space || exit 1
```

**Reference:** `BUILD_DOCTOR_IMPLEMENTATION.md`

---

#### INF-015: CI/CD Pipeline Integration
**Goal:** GitHub Actions workflow s fail-fast gates  
**Effort:** 800 LOC, 3 dny  
**Priority:** HIGH

**Requirements:**
- Pre-deploy gate: `make test` (unit tests)
- Deploy: `make deploy ENV=test`
- Post-deploy gate: `make test-e2e-pre` (smoke tests)
- Rollback: `make rollback` (pokud post-deploy fail)

**GitHub Actions workflow:**
```yaml
jobs:
  pre-deploy:
    runs-on: ubuntu-latest
    steps:
      - run: make test
      - run: make build

  deploy:
    needs: pre-deploy
    runs-on: ubuntu-latest
    steps:
      - run: make deploy ENV=test

  post-deploy:
    needs: deploy
    runs-on: ubuntu-latest
    steps:
      - run: make test-e2e-pre
      - if: failure()
        run: make rollback
```

---

### Phase 8: Monitoring & Validation (Week 8)

#### INF-016: Infrastructure Health Dashboard
**Goal:** Grafana dashboard pro infra metrics  
**Effort:** 400 LOC, 1 den  
**Priority:** MEDIUM

**Requirements:**
- Panels: SSL cert expiry, DB connection pool, disk usage
- Alerts: cert <30 days, disk >80%, DB connections >80%
- Prometheus exporters

---

#### INF-017: Deployment Audit Logging
**Goal:** Track všechny deployments s rollback info  
**Effort:** 600 LOC, 2 dny  
**Priority:** MEDIUM

**Requirements:**
- Deployment log: timestamp, user, version, environment
- Store v DB table: `deployments`
- API: `GET /api/deployments` (history)
- Slack notification: každý deployment

---

#### INF-018: Chaos Engineering Tests
**Goal:** Validace resilience (service failures, network partition)  
**Effort:** 800 LOC, 3 dny  
**Priority:** LOW (future)

**Requirements:**
- Chaos Monkey integration
- Test scenarios: DB failure, Keycloak down, network delay
- Auto-recovery validation

---

## 📈 IMPLEMENTATION ROADMAP

### Week 1-2: Foundation (CRITICAL PATH)
- ✅ INF-001: Template Generator (2d)
- ✅ INF-002: Template Syntax (3d)
- ✅ INF-003: Docker Secrets (3d)
- ✅ INF-007: DB Users (2d)

**Deliverable:** Secure config management baseline

### Week 3-4: Automation
- ✅ INF-005: Let's Encrypt (2d)
- ✅ INF-009: Flyway (3d)
- ✅ INF-011: Environment Isolation (2d)
- ✅ INF-014: Build Doctor (2d)

**Deliverable:** Automated deployment pipeline

### Week 5-6: Optimization
- ✅ INF-006: SSL Monitoring (1d)
- ✅ INF-008: Connection Pooling (1d)
- ✅ INF-010: DB Backup (2d)
- ✅ INF-013: Makefile Simplification (1d)
- ✅ INF-015: CI/CD Pipeline (3d)

**Deliverable:** Production-ready infrastructure

### Week 7-8: Polish (Optional)
- 🔵 INF-004: Vault (5d)
- 🔵 INF-012: Feature Flags (3d)
- 🔵 INF-016: Health Dashboard (1d)
- 🔵 INF-017: Audit Logging (2d)

**Deliverable:** Enterprise-grade observability

---

## 🧪 TESTING STRATEGY

### Unit Tests
- Template generation idempotence
- Environment variable validation
- Secret rotation scripts

### Integration Tests
- DB user isolation (cannot access other DBs)
- SSL cert renewal flow
- Flyway migration rollback

### E2E Tests
- Full deployment: dev → test → prod
- Rollback scenario
- Chaos: kill DB → auto-recovery

---

## 📊 METRICS & KPIs

| Metric | Baseline | Target | Measurement |
|--------|----------|--------|-------------|
| Deployment Time | 30-40 min | <10 min | `make deploy` duration |
| Secrets in Git | 12 | 0 | `git grep PASSWORD` |
| Config Sources | 6 | 2 | Manual audit |
| SSL Rotation | Manual | Auto | Cert expiry alerts |
| Rollback Time | N/A | <5 min | `make rollback` duration |
| Build Doctor Pass | N/A | 100% | CI/CD gate |

---

## 🔗 DEPENDENCIES

### External
- **Let's Encrypt** - ACME protocol
- **HashiCorp Vault** - Secret management (optional)
- **Traefik** - Reverse proxy + ACME client

### Internal
- **EPIC-006** - Workflow executors (DB migrations)
- **EPIC-011** - n8n deployment (SSL certs)
- **EPIC-002** - E2E tests (deployment validation)

---

## 📚 REFERENCE DOCUMENTS

1. **SECURITY_CONFIG_AUDIT.md** - 1,293 LOC audit (47 env vars, 12 secrets)
2. **DB_SEPARATE_USERS_PLAN.md** - DB isolation migration plan
3. **copilot-instructions.md** - Template system rules
4. **copilot-golden-rules.md** - Build process documentation
5. **BUILD_DOCTOR_IMPLEMENTATION.md** - Pre-flight check specs
6. **MAKE_CLEAN_EXPLAINED.md** - Build target documentation

---

## 🎯 SUCCESS DEFINITION

**MVP (Week 4):**
- ✅ Zero secrets v Git (Docker Secrets)
- ✅ Separate DB users (security isolation)
- ✅ Template generator (single source of truth)
- ✅ Build Doctor (pre-flight checks)

**Production-Ready (Week 6):**
- ✅ Let's Encrypt SSL (auto-renewal)
- ✅ Flyway migrations (versioned + rollback)
- ✅ Dev/Test/Prod separation
- ✅ CI/CD pipeline (fail-fast gates)

**Enterprise-Grade (Week 8):**
- 🔵 Vault integration (audit logging)
- 🔵 Feature flags (canary rollout)
- 🔵 Health dashboard (infrastructure metrics)

---

**Last Updated:** 8. listopadu 2025  
**Epic Owner:** Martin Horak (@Muriel2Horak)  
**Status:** 🔴 Planning Phase
