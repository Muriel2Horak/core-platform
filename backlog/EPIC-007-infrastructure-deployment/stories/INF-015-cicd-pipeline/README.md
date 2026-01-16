---
id: INF-015
epic: EPIC-007-infrastructure-deployment
title: "CI/CD Pipeline Integration"
priority: P1
status: todo
assignee: ""
created: 2025-11-08
updated: 2026-01-15
estimate: "3 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-007-infrastructure-deployment/stories/INF-015-cicd-pipeline/README.md
    - backlog/EPIC-007-infrastructure-deployment/README.md
---


# INF-015: CI/CD Pipeline Integration

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** HIGH  
**Effort:** 3 dny, ~800 LOC  
**Owner:** Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**
```yaml
# .github/workflows/ má partial coverage:
- backend-tests.yml      # Unit tests only
- frontend-tests.yml     # Lint + unit tests
# CHYBÍ:
# - Integration tests v CI
# - E2E tests v CI
# - Deployment automation
# - Rollback mechanism
```

**Issues:**
- Manual deployment (error-prone)
- Žádné smoke tests před release
- Nelze rollback pokud deploy fail

### Goal

**Complete CI/CD pipeline:**

```
PR → Unit Tests → Integration Tests → Deploy to Staging
  → E2E Tests → Manual Approval → Deploy to Production
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **3-Stage Pipeline**
   - Pre-deploy: Unit + integration tests
   - Deploy: Staging → smoke tests
   - Post-deploy: E2E tests → production

2. ✅ **Fail-Fast Gates**
   - Unit test fail → stop pipeline
   - Smoke test fail → rollback staging
   - E2E fail → block production deploy

3. ✅ **Rollback Automation**
   ```bash
   make rollback ENV=staging VERSION=v1.2.3
   ```

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [TASK-015-01: CI tests + gates](subtasks/TASK-015-01-ci-tests-gates.md) | 8h | none |
| 2 | [TASK-015-02: Staging deploy + smoke tests](subtasks/TASK-015-02-staging-deploy-smoke.md) | 8h | TASK-015-01 |
| 3 | [TASK-015-03: Production deploy + rollback](subtasks/TASK-015-03-prod-deploy-rollback.md) | 8h | TASK-015-02 |

### Implementation

**File:** `.github/workflows/deploy.yml`

```yaml
name: Deploy Pipeline

on:
  push:
    branches: [main]

jobs:
  # Gate 1: Unit Tests
  test-unit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Backend unit tests
        run: make test-backend
      - name: Frontend unit tests
        run: make test-frontend

  # Gate 2: Integration Tests
  test-integration:
    needs: test-unit
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Start services
        run: make up ENV=test
      - name: Integration tests
        run: make test-backend-full
      - name: Cleanup
        run: make down

  # Gate 3: Deploy to Staging
  deploy-staging:
    needs: test-integration
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - name: Deploy
        run: |
          ssh deploy@staging.core-platform.com \
            "cd /opt/core-platform && make deploy ENV=staging"
      
      - name: Smoke tests
        run: |
          make test-e2e-pre BASE_URL=https://staging.core-platform.com
      
      - name: Rollback on failure
        if: failure()
        run: |
          ssh deploy@staging.core-platform.com \
            "cd /opt/core-platform && make rollback"

  # Gate 4: E2E Tests
  test-e2e:
    needs: deploy-staging
    runs-on: ubuntu-latest
    steps:
      - name: Full E2E suite
        run: |
          make test-e2e-post BASE_URL=https://staging.core-platform.com

  # Gate 5: Deploy to Production (manual approval)
  deploy-production:
    needs: test-e2e
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Deploy to production
        run: |
          ssh deploy@prod.core-platform.com \
            "cd /opt/core-platform && make deploy ENV=production"
      
      - name: Post-deploy verification
        run: |
          make verify BASE_URL=https://core-platform.com
      
      - name: Rollback on failure
        if: failure()
        run: |
          ssh deploy@prod.core-platform.com \
            "cd /opt/core-platform && make rollback"
```

**Rollback Script:**

```bash
#!/bin/bash
# scripts/deploy/rollback.sh
set -euo pipefail

ENV=${ENV:-staging}
BACKUP_DIR=/opt/core-platform/backups

echo "⏪ Rolling back $ENV environment..."

# 1. Get previous version
PREVIOUS=$(ls -t $BACKUP_DIR | head -2 | tail -1)

# 2. Restore Docker images
docker load < $BACKUP_DIR/$PREVIOUS/images.tar

# 3. Restore database
bash scripts/db/restore-backup.sh $BACKUP_DIR/$PREVIOUS/db-dump.sql

# 4. Restart services
make up ENV=$ENV

# 5. Verify
make verify

echo "✅ Rollback to $PREVIOUS completed"
```

**Effort:** 3 dny  
**LOC:** ~800  
**Priority:** HIGH

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
