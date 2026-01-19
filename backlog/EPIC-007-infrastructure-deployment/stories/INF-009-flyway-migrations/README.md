---
id: INF-009
epic: EPIC-007-infrastructure-deployment
title: "Flyway Multi-DB Migration Coordination"
priority: P1
status: done
assignee: ""
created: 2025-11-08
updated: 2026-01-15
estimate: "3 days"
path_mapping:
  code_paths:
    - backend/src/main/resources/application.yml
    - backend/src/main/resources/db/migration/
  test_paths: []
  docs_paths:
    - backlog/EPIC-007-infrastructure-deployment/stories/INF-009-flyway-migrations/README.md
    - backlog/EPIC-007-infrastructure-deployment/README.md
---

# INF-009: Flyway Multi-DB Migration Coordination

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** ✅ **DONE**
**Priority:** HIGH  
**Effort:** 3 dny, ~800 LOC  
**Owner:** Platform + DBA Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**
```sql
-- Manual SQL migrations scattered:
docker/postgres/init-multi-db.sql          # Initial schema
docker/postgres/init-separate-users.sql    # User creation
docker/keycloak/init-keycloak.sql         # Keycloak tables
-- Žádná versioning
-- Žádná rollback strategie
-- Ručně aplikované při buildu
```

**Issues:**
- Schema drift mezi dev/staging/prod
- Nelze rollback migrace
- Žádný audit (kdo změnil co kdy)

### Goal

**Flyway-based migrations:**

```
backend/src/main/resources/db/migration/
├── core/
│   ├── V1__initial_schema.sql
│   ├── V2__add_workflows.sql
│   ├── V3__n8n_integration.sql
│   └── U3__rollback_n8n.sql
├── keycloak/
│   └── V1__custom_attributes.sql
└── grafana/
    └── V1__custom_dashboards.sql
```

**Benefits:**
- ✅ Versioned migrations
- ✅ Rollback support (U scripts)
- ✅ Schema history audit
- ✅ Pre-deployment validation

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **3 Flyway Instances**
   - Core DB migrations
   - Keycloak DB migrations
   - Grafana DB migrations

2. ✅ **Version Control**
   - Naming: `V{version}__{description}.sql`
   - Rollback: `U{version}__{description}.sql`
   - Validation: `make db-validate`

3. ✅ **Migration Orchestration**
   - Pre-deploy: Validate all migrations
   - Deploy: Apply pending migrations
   - Post-deploy: Verify schema state
   - Rollback: Execute U scripts

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [TASK-009-01: Flyway configs + directories](subtasks/TASK-009-01-flyway-configs.md) | 8h | none |
| 2 | [TASK-009-02: Migration orchestration scripts](subtasks/TASK-009-02-migrate-orchestrator.md) | 8h | TASK-009-01 |
| 3 | [TASK-009-03: CI validation + pre-deploy checks](subtasks/TASK-009-03-ci-validation.md) | 8h | TASK-009-02 |

### Implementation

**File:** `backend/src/main/resources/application.yml`

```yaml
spring:
  flyway:
    enabled: true
    locations:
      - classpath:db/migration/core
    baseline-on-migrate: true
    validate-on-migrate: true

# Separate Flyway configs for other DBs
flyway:
  keycloak:
    url: ${KEYCLOAK_DATABASE_URL}
    user: ${KEYCLOAK_DB_USERNAME}
    password: ${KEYCLOAK_DB_PASSWORD}
    locations:
      - classpath:db/migration/keycloak
  
  grafana:
    url: ${GRAFANA_DATABASE_URL}
    user: ${GRAFANA_DB_USERNAME}
    password: ${GRAFANA_DB_PASSWORD}
    locations:
      - classpath:db/migration/grafana
```

**Migration Script:**

```bash
#!/bin/bash
# scripts/db/migrate-all.sh
set -euo pipefail

echo "🔄 Running database migrations..."

# 1. Validate migrations
echo "📋 Validating migration scripts..."
./mvnw flyway:validate -Dflyway.configFiles=flyway-core.conf
./mvnw flyway:validate -Dflyway.configFiles=flyway-keycloak.conf
./mvnw flyway:validate -Dflyway.configFiles=flyway-grafana.conf

# 2. Apply migrations
echo "🚀 Applying migrations..."
./mvnw flyway:migrate -Dflyway.configFiles=flyway-core.conf
./mvnw flyway:migrate -Dflyway.configFiles=flyway-keycloak.conf
./mvnw flyway:migrate -Dflyway.configFiles=flyway-grafana.conf

# 3. Verify
echo "✅ Verifying schema state..."
./mvnw flyway:info -Dflyway.configFiles=flyway-core.conf
```

**Rollback Script:**

```bash
#!/bin/bash
# scripts/db/rollback.sh
VERSION=$1

if [[ -z "$VERSION" ]]; then
    echo "Usage: $0 <version>"
    exit 1
fi

echo "⏪ Rolling back to version $VERSION..."

# Execute undo migrations
./mvnw flyway:undo -Dflyway.target=$VERSION
```

**Effort:** 3 dny  
**LOC:** ~800  
**Blocks:** Production deployments

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
