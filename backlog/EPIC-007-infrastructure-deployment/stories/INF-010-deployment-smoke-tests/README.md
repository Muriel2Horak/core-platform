---
id: INF-010
epic: EPIC-007-infrastructure-deployment
title: "Post-Deployment Smoke Tests"
priority: P1
status: todo
assignee: ""
created: 2025-11-08
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-007-infrastructure-deployment/stories/INF-010-deployment-smoke-tests/README.md
    - backlog/EPIC-007-infrastructure-deployment/README.md
---

# INF-010: Post-Deployment Smoke Tests

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** HIGH  
**Effort:** 2 dny, ~500 LOC  
**Owner:** Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**

```bash
# Deploy proces:
make deploy ENV=production
# → Deploy completes
# → ŽÁDNÉ automatic verification
# → Service může být DOWN ale deploy "succeeded"
```

**Issues:**
- Deploy succeed ale aplikace nefunguje
- Zjištěno až kdy user reportuje chybu
- Rollback decision delayed

### Goal

**Post-deploy smoke tests:**

```bash
make deploy ENV=production
# → Deploy
# → Automatic smoke tests (5-7 min)
# → Rollback if ANY test fails
```

**Tests:**
- ✅ Backend /health returns 200
- ✅ Frontend homepage loads
- ✅ Keycloak login page accessible
- ✅ Grafana dashboard loads
- ✅ Database connectivity
- ✅ Kafka connectivity

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **5 Critical Smoke Tests**
   - Backend health check
   - Frontend availability
   - Authentication flow
   - Database queries
   - Message bus connectivity

2. ✅ **Fast Execution**
   - Total runtime: <5 minutes
   - Parallel execution where possible
   - Fail-fast on first error

3. ✅ **Auto-Rollback**
   - If ANY test fails → rollback
   - Notification to Slack/email
   - Preserve logs for debugging

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [TASK-010-01: Smoke test script](subtasks/TASK-010-01-smoke-test-script.md) | 6h | none |
| 2 | [TASK-010-02: Deploy hook + rollback trigger](subtasks/TASK-010-02-deploy-hook-rollback.md) | 6h | TASK-010-01 |
| 3 | [TASK-010-03: Notifications + logs](subtasks/TASK-010-03-notifications-logs.md) | 4h | TASK-010-01, TASK-010-02 |

### Implementation

**File:** `scripts/deploy/smoke-tests.sh`

```bash
#!/bin/bash
set -euo pipefail

BASE_URL=${BASE_URL:-https://core-platform.com}
TIMEOUT=30

echo "🔥 Running post-deployment smoke tests..."

# Test 1: Backend Health
echo "1️⃣  Testing backend health..."
HTTP_CODE=$(curl -sk -o /dev/null -w "%{http_code}" \
    -m $TIMEOUT \
    $BASE_URL/api/actuator/health)

if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ Backend health check failed: $HTTP_CODE"
    exit 1
fi
echo "✅ Backend healthy"

# Test 2: Frontend Homepage
echo "2️⃣  Testing frontend..."
HTTP_CODE=$(curl -sk -o /dev/null -w "%{http_code}" \
    -m $TIMEOUT \
    $BASE_URL/)

if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ Frontend unreachable: $HTTP_CODE"
    exit 1
fi
echo "✅ Frontend accessible"

# Test 3: Keycloak Login Page
echo "3️⃣  Testing Keycloak..."
HTTP_CODE=$(curl -sk -o /dev/null -w "%{http_code}" \
    -m $TIMEOUT \
    $BASE_URL/realms/admin/.well-known/openid-configuration)

if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ Keycloak unreachable: $HTTP_CODE"
    exit 1
fi
echo "✅ Keycloak accessible"

# Test 4: Database Query
echo "4️⃣  Testing database..."
DB_RESULT=$(docker compose exec -T db psql -U core -d core \
    -tAc "SELECT 1")

if [ "$DB_RESULT" != "1" ]; then
    echo "❌ Database query failed"
    exit 1
fi
echo "✅ Database responsive"

# Test 5: Kafka Connectivity
echo "5️⃣  Testing Kafka..."
KAFKA_TOPICS=$(docker compose exec -T kafka kafka-topics.sh \
    --bootstrap-server localhost:9092 \
    --list 2>/dev/null | wc -l)

if [ "$KAFKA_TOPICS" -lt 1 ]; then
    echo "❌ Kafka unreachable"
    exit 1
fi
echo "✅ Kafka accessible"

# Test 6: Authentication Flow
echo "6️⃣  Testing auth flow..."
TOKEN=$(curl -sk -X POST \
    $BASE_URL/realms/admin/protocol/openid-connect/token \
    -d "client_id=admin-client" \
    -d "client_secret=$OIDC_CLIENT_SECRET" \
    -d "grant_type=client_credentials" | \
    jq -r .access_token)

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
    echo "❌ Authentication failed"
    exit 1
fi
echo "✅ Authentication working"

echo ""
echo "🎉 All smoke tests passed!"
```

**File:** `scripts/deploy/deploy-with-tests.sh`

```bash
#!/bin/bash
set -euo pipefail

ENV=${ENV:-staging}
BASE_URL=${BASE_URL:-https://staging.core-platform.com}

echo "🚀 Deploying to $ENV..."

# 1. Create backup point
BACKUP_TAG="pre-deploy-$(date +%Y%m%d-%H%M%S)"
docker tag core-platform/backend:latest core-platform/backend:$BACKUP_TAG
echo "💾 Backup tagged: $BACKUP_TAG"

# 2. Deploy
make up ENV=$ENV

# 3. Wait for services
echo "⏳ Waiting for services to start..."
sleep 30

# 4. Run smoke tests
if bash scripts/deploy/smoke-tests.sh; then
    echo "✅ Deployment successful!"
    exit 0
else
    echo "❌ Smoke tests failed! Rolling back..."
    
    # Rollback
    docker tag core-platform/backend:$BACKUP_TAG core-platform/backend:latest
    make up ENV=$ENV
    
    # Notify
    curl -X POST https://slack.com/webhooks/YOUR_WEBHOOK \
         -d "{\"text\": \"❌ Deployment to $ENV failed. Rolled back.\"}"
    
    exit 1
fi
```

**Makefile Integration:**

```makefile
.PHONY: deploy
deploy: validate-env
	@echo "🚀 Deploying to $(ENV)..."
	bash scripts/deploy/deploy-with-tests.sh

.PHONY: smoke-tests
smoke-tests:
	@echo "🔥 Running smoke tests..."
	bash scripts/deploy/smoke-tests.sh
```

**Effort:** 2 dny  
**LOC:** ~500  
**Blocks:** Production deployments

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
