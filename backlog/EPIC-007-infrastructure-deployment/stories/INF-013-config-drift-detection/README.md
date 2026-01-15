---
id: INF-013
epic: EPIC-007-infrastructure-deployment
title: "Configuration Drift Detection"
priority: P2
status: todo
assignee: ""
created: 2025-11-08
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-007-infrastructure-deployment/stories/INF-013-config-drift-detection/README.md
    - backlog/EPIC-007-infrastructure-deployment/README.md
---

# INF-013: Configuration Drift Detection

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

```bash
# Templates v Gitu:
docker-compose.template.yml
.env.template
realm-admin.template.json

# Running config:
docker-compose.yml           # Může být editovaný ručně!
.env                        # Developer změnil LOCAL
realm-admin.json            # Keycloak admin změnil v UI

# ŽÁDNÁ validace že running == template
```

**Issues:**
- Developer edituje `.env` místo `.env.template` → Git miss
- Keycloak admin změní client v UI → realm.json drift
- Nginx config změněn ručně → template ignorován

### Goal

**Automated drift detection:**

```bash
# Daily CI check:
make config-drift-check
# → Compare: Git templates vs. running configs
# → Report: Differences
# → Alert: Drift detected
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Drift Detection**
   - Compare: `.env.template` vs. `.env`
   - Compare: `docker-compose.template.yml` vs. `docker-compose.yml`
   - Compare: `realm-admin.template.json` vs. Keycloak export

2. ✅ **CI Integration**
   - Daily cron: Check drift
   - PR check: Detect template changes without regeneration
   - Fail-fast if drift detected

3. ✅ **Auto-Fix Option**
   ```bash
   make config-sync  # Regenerate ALL configs from templates
   ```

### Implementation

**File:** `scripts/config/detect-drift.sh`

```bash
#!/bin/bash
set -euo pipefail

DRIFT_DETECTED=0

echo "🔍 Checking configuration drift..."

# 1. Check .env vs .env.template
echo "📄 Checking .env..."
if ! diff -q .env.template .env >/dev/null 2>&1; then
    echo "⚠️  .env has drifted from .env.template"
    diff -u .env.template .env || true
    DRIFT_DETECTED=1
fi

# 2. Check docker-compose.yml vs. template
echo "📄 Checking docker-compose.yml..."
# Generate expected from template
envsubst < docker-compose.template.yml > /tmp/docker-compose-expected.yml
if ! diff -q /tmp/docker-compose-expected.yml docker-compose.yml >/dev/null 2>&1; then
    echo "⚠️  docker-compose.yml has drifted from template"
    diff -u /tmp/docker-compose-expected.yml docker-compose.yml || true
    DRIFT_DETECTED=1
fi

# 3. Check Keycloak realm vs. template
echo "📄 Checking Keycloak realm..."
# Export current realm from Keycloak
docker compose exec keycloak /opt/keycloak/bin/kc.sh export \
    --realm admin \
    --file /tmp/realm-export.json

# Generate expected from template
envsubst < docker/keycloak/realm-admin.template.json > /tmp/realm-expected.json

# Compare (ignore dynamic fields like timestamps)
jq 'del(.id, .clients[].id, .users[].id)' /tmp/realm-export.json > /tmp/realm-normalized.json
jq 'del(.id, .clients[].id, .users[].id)' /tmp/realm-expected.json > /tmp/expected-normalized.json

if ! diff -q /tmp/realm-normalized.json /tmp/expected-normalized.json >/dev/null 2>&1; then
    echo "⚠️  Keycloak realm has drifted from template"
    diff -u /tmp/expected-normalized.json /tmp/realm-normalized.json || true
    DRIFT_DETECTED=1
fi

# Summary
if [ $DRIFT_DETECTED -eq 0 ]; then
    echo "✅ No configuration drift detected"
    exit 0
else
    echo "❌ Configuration drift detected!"
    echo "💡 Run: make config-sync to fix"
    exit 1
fi
```

**File:** `.github/workflows/config-drift.yml`

```yaml
name: Configuration Drift Check

on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
  pull_request:
    paths:
      - '*.template*'
      - 'docker/**/*.template*'

jobs:
  check-drift:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Start services
        run: make up
      
      - name: Detect configuration drift
        run: bash scripts/config/detect-drift.sh
      
      - name: Notify on drift
        if: failure()
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "⚠️ Configuration drift detected in ${{ github.repository }}"
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

**Makefile Targets:**

```makefile
.PHONY: config-drift-check
config-drift-check:
	@echo "🔍 Checking configuration drift..."
	bash scripts/config/detect-drift.sh

.PHONY: config-sync
config-sync:
	@echo "🔄 Regenerating all configs from templates..."
	make env-generate
	make compose-generate
	bash docker/keycloak/generate-realm.sh
	@echo "✅ Configs synchronized with templates"
```

**Effort:** 2 dny  
**LOC:** ~400  
**Priority:** MEDIUM

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
