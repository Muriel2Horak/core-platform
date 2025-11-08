# INF-018: Infrastructure Documentation & Runbooks

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** MEDIUM  
**Effort:** 2 dny, ~400 LOC  
**Owner:** Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**

```
# Dokumentace je ROZPTÝLENÁ:
- README.md (částečně zastaralé)
- copilot-instructions.md (build rules)
- SECURITY_CONFIG_AUDIT.md (security)
- DB_SEPARATE_USERS_PLAN.md (DB migration)
- Multiple *.md files (neorganizované)

# CHYBÍ:
- Deployment runbook (krok-za-krokem)
- Troubleshooting guide
- Architecture overview
- Onboarding docs pro nové devy
```

**Issues:**
- Nový developer neví kde začít
- Ops team hledá procedures v multiple files
- Deployment knowledge v hlavách lidí

### Goal

**Centralized documentation:**

```
docs/
├── infrastructure/
│   ├── ARCHITECTURE.md         # System overview
│   ├── DEPLOYMENT.md           # Deployment runbook
│   ├── TROUBLESHOOTING.md      # Common issues
│   └── RUNBOOKS.md             # Operational procedures
├── onboarding/
│   ├── DEVELOPER_SETUP.md      # Dev environment setup
│   └── FIRST_DEPLOYMENT.md     # First-time deploy guide
└── security/
    ├── SECRETS_MANAGEMENT.md   # Credentials handling
    └── SSL_CERTIFICATES.md     # SSL procedures
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Complete Runbooks**
   - Deployment procedure (step-by-step)
   - Rollback procedure
   - Troubleshooting common issues
   - Emergency procedures (DR)

2. ✅ **Architecture Docs**
   - System diagram (services + dependencies)
   - Data flow diagrams
   - Network topology

3. ✅ **Onboarding Guides**
   - Developer setup (first day)
   - First deployment walkthrough
   - Code review checklist

### Implementation

**File:** `docs/infrastructure/DEPLOYMENT.md`

```markdown
# Deployment Runbook

## Pre-Deployment Checklist

- [ ] All tests passing (unit + integration + E2E)
- [ ] Secrets rotated (if needed)
- [ ] Database migrations tested
- [ ] Backup taken (database + configs)
- [ ] Rollback plan ready
- [ ] Stakeholders notified

## Deployment Procedure

### 1. Pre-Deploy Checks (5 min)

```bash
# Verify environment
make env-validate
make doctor

# Run tests
make test-backend-full
make test-e2e-pre
```

### 2. Deploy to Staging (30 min)

```bash
# Deploy
make deploy ENV=staging

# Smoke tests
make smoke-tests BASE_URL=https://staging.core-platform.com

# E2E tests
make test-e2e-post BASE_URL=https://staging.core-platform.com
```

### 3. Production Deployment (1-2 hours)

```bash
# Create backup
bash scripts/backup/pg-backup.sh

# Deploy
make deploy ENV=production

# Smoke tests
make smoke-tests BASE_URL=https://core-platform.com

# Monitor logs
make logs-errors
```

### 4. Post-Deploy Verification (15 min)

```bash
# Health checks
curl -k https://core-platform.com/api/actuator/health

# Verify auth flow
curl -X POST https://core-platform.com/realms/admin/protocol/openid-connect/token \
  -d "client_id=admin-client" \
  -d "client_secret=$OIDC_CLIENT_SECRET" \
  -d "grant_type=client_credentials"

# Check logs
make logs-backend | grep -i error
```

---

## Rollback Procedure

### If Deployment Fails

```bash
# 1. Stop services
make down

# 2. Rollback to previous version
docker tag core-platform/backend:backup core-platform/backend:latest

# 3. Restore database (if migrations ran)
bash scripts/backup/pg-restore.sh core /var/backups/latest.sql.gz

# 4. Restart
make up ENV=production

# 5. Verify
make smoke-tests
```

---

## Common Issues

### Issue: Backend won't start

**Symptoms:**
```
Backend container exiting with code 1
```

**Diagnosis:**
```bash
# Check logs
make logs-backend | grep -i error

# Common causes:
# - Database connection failed
# - Missing environment variables
# - Port already in use
```

**Fix:**
```bash
# Verify database is running
docker compose ps db

# Check DATABASE_URL
make env-validate

# Check ports
lsof -i :8080
```

### Issue: Keycloak realm import failed

**Symptoms:**
```
Keycloak logs: "Realm 'admin' import failed"
```

**Fix:**
```bash
# Regenerate realm config
bash docker/keycloak/generate-realm.sh

# Rebuild Keycloak image
make rebuild-keycloak
```
```

**File:** `docs/infrastructure/ARCHITECTURE.md`

```markdown
# Core Platform Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Nginx (SSL Termination)              │
│         Port 443 → https://core-platform.com            │
└─────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
  ┌──────────┐     ┌──────────┐     ┌──────────┐
  │ Frontend │     │ Backend  │     │ Keycloak │
  │  (React) │     │  (Java)  │     │  (Auth)  │
  └──────────┘     └──────────┘     └──────────┘
        │                 │                 │
        └─────────────────┴─────────────────┘
                          │
        ┌─────────────────┼─────────────────┬─────────┐
        ▼                 ▼                 ▼         ▼
  ┌──────────┐     ┌──────────┐     ┌──────────┐  ┌────────┐
  │ Database │     │  Redis   │     │  Kafka   │  │ MinIO  │
  │  (Core)  │     │ (Cache)  │     │ (Events) │  │  (S3)  │
  └──────────┘     └──────────┘     └──────────┘  └────────┘
```

## Services

### Backend (Spring Boot)
- **Port:** 8080 (internal)
- **Database:** PostgreSQL (core DB)
- **Cache:** Redis
- **Auth:** Keycloak OAuth2

### Frontend (Vite + React)
- **Port:** 3000 (dev), 80 (prod via Nginx)
- **API:** Backend via `/api` proxy

### Keycloak
- **Port:** 8443 (HTTPS internal)
- **Database:** PostgreSQL (keycloak DB)
- **Realm:** admin

### PostgreSQL
- **Port:** 5432 (internal)
- **Databases:** core, keycloak, grafana

## Data Flow

1. User → Nginx (443) → Frontend (80)
2. Frontend → Nginx → Backend (/api/*)
3. Backend → Keycloak (OAuth2 validation)
4. Backend → PostgreSQL (queries)
5. Backend → Kafka (CDC events)
```

**File:** `docs/onboarding/DEVELOPER_SETUP.md`

```markdown
# Developer Setup Guide

## Prerequisites

- Docker Desktop 4.20+
- Git
- Make
- VS Code (recommended)

## First-Time Setup

### 1. Clone Repository

```bash
git clone https://github.com/your-org/core-platform.git
cd core-platform
```

### 2. Environment Setup

```bash
# Copy environment template
cp .env.template .env

# Edit secrets (ask team for production values)
vim .env
```

### 3. Start Services

```bash
# First build (20-30 min)
make clean-fast

# Subsequent starts (2-5 min)
make up
```

### 4. Verify Setup

```bash
# Check all services healthy
make verify

# Open frontend
open https://localhost
```

## Daily Workflow

```bash
# Start development
make up

# Watch logs
make logs-backend    # Backend logs
make logs-frontend   # Frontend logs

# Run tests
make test-backend    # Backend unit tests
make test-frontend   # Frontend tests

# Stop services
make down
```

## Troubleshooting

See [TROUBLESHOOTING.md](../infrastructure/TROUBLESHOOTING.md)
```

**Effort:** 2 dny  
**LOC:** ~400 (markdown docs)  
**Priority:** MEDIUM

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
