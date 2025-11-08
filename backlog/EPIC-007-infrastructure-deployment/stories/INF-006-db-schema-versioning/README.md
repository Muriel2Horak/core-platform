# INF-006: Database Schema Version Control

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** MEDIUM  
**Effort:** 2 dny, ~500 LOC  
**Owner:** DBA + Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**

```sql
-- Schema changes jsou OUTSIDE version control:
-- 1. Developer ručně upraví SQL
-- 2. Aplikuje přes psql
-- 3. Commitne do Git (možná)
-- 4. Schema drift mezi prostředími
```

**Issues:**
- Nelze track WHO změnil schema
- Nelze rollback schema změny
- Dev/staging/prod mají různá schemata

### Goal

**Git-based schema versioning:**

```bash
# Schema stored in Git
db/schema/
├── core.sql              # Generated schema dump
├── keycloak.sql
└── grafana.sql

# Drift detection
make db-schema-diff       # Compare Git vs. running DB
make db-schema-update     # Update Git from running DB
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Schema Dumps in Git**
   - Daily cron: Dump schema to Git
   - Only structure (no data)
   - Commit with timestamp

2. ✅ **Drift Detection**
   - Compare: Git schema vs. live DB
   - Report: Added/removed tables, columns, indexes
   - CI check: Fail if drift detected

3. ✅ **Migration Generation**
   - Auto-generate Flyway migration from schema diff
   - Review + edit before applying

### Implementation

**File:** `scripts/db/dump-schema.sh`

```bash
#!/bin/bash
set -euo pipefail

SCHEMA_DIR=db/schema
mkdir -p $SCHEMA_DIR

# Dump schema (no data)
for DB in core keycloak grafana; do
    echo "📦 Dumping $DB schema..."
    pg_dump -h localhost -U postgres -d $DB \
            --schema-only \
            --no-owner \
            --no-acl \
            > $SCHEMA_DIR/$DB.sql
done

# Commit to Git
git add $SCHEMA_DIR/*.sql
git commit -m "chore(db): Update schema dump $(date +%Y-%m-%d)" || true
```

**File:** `scripts/db/schema-diff.sh`

```bash
#!/bin/bash
set -euo pipefail

DB=${1:-core}
SCHEMA_FILE=db/schema/$DB.sql

echo "🔍 Detecting schema drift for $DB..."

# Dump current schema
pg_dump -h localhost -U postgres -d $DB \
        --schema-only --no-owner --no-acl \
        > /tmp/$DB-current.sql

# Compare
diff -u $SCHEMA_FILE /tmp/$DB-current.sql > /tmp/$DB-drift.diff || true

if [ -s /tmp/$DB-drift.diff ]; then
    echo "⚠️  Schema drift detected:"
    cat /tmp/$DB-drift.diff
    exit 1
else
    echo "✅ Schema in sync"
fi
```

**File:** `.github/workflows/schema-drift.yml`

```yaml
name: Schema Drift Check

on:
  pull_request:
    paths:
      - 'db/schema/**'
      - 'backend/src/main/resources/db/migration/**'

jobs:
  check-drift:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Start database
        run: make up-db
      
      - name: Apply migrations
        run: make db-migrate
      
      - name: Check schema drift
        run: bash scripts/db/schema-diff.sh core
      
      - name: Fail on drift
        if: failure()
        run: |
          echo "❌ Schema drift detected!"
          echo "💡 Run: make db-schema-update"
          exit 1
```

**Makefile Targets:**

```makefile
.PHONY: db-schema-dump
db-schema-dump:
	@echo "📦 Dumping database schemas..."
	bash scripts/db/dump-schema.sh

.PHONY: db-schema-diff
db-schema-diff:
	@echo "🔍 Checking schema drift..."
	bash scripts/db/schema-diff.sh core
	bash scripts/db/schema-diff.sh keycloak
	bash scripts/db/schema-diff.sh grafana

.PHONY: db-schema-update
db-schema-update: db-schema-dump
	@echo "✅ Schema files updated in db/schema/"
```

**Effort:** 2 dny  
**LOC:** ~500  
**Blocks:** CI/CD (schema drift check)

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
