# INF-008: Database Migration Rollback Strategy

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** HIGH  
**Effort:** 2 dny, ~500 LOC  
**Owner:** DBA Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**

```sql
-- Flyway má V migrations:
V1__initial_schema.sql
V2__add_workflows.sql
V3__n8n_integration.sql

-- ALE CHYBÍ U (undo) migrations!
-- Pokud V3 fail → NELZE rollback
-- Musíš ručně DROP tables
```

**Issues:**
- Deploy fail → schema corrupted
- Rollback vyžaduje DBA manual fix
- Downtime při rollback

### Goal

**Bidirectional migrations:**

```
db/migration/
├── V1__initial_schema.sql      # Forward
├── U1__initial_schema.sql      # Backward (rollback)
├── V2__add_workflows.sql
├── U2__add_workflows.sql
```

**Rollback command:**

```bash
make db-rollback VERSION=2  # Roll back to V2
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **U Scripts for Every V Script**
   - Mandatory: Each V migration MUSÍ mít U counterpart
   - CI check: Fail if U script missing

2. ✅ **Safe Rollback**
   - Rollback preserves DATA (only reverses schema)
   - Transaction-based (atomic)
   - Dry-run mode

3. ✅ **Testing**
   - Test: V migration → U rollback → V re-apply
   - Data integrity check

### Implementation

**File:** `backend/src/main/resources/db/migration/core/U2__add_workflows.sql`

```sql
-- Rollback for V2__add_workflows.sql
BEGIN;

-- Drop new indexes
DROP INDEX IF EXISTS idx_workflows_status;
DROP INDEX IF EXISTS idx_workflows_tenant_id;

-- Drop new tables
DROP TABLE IF EXISTS workflow_executions CASCADE;
DROP TABLE IF EXISTS workflows CASCADE;

-- Restore old table structure (if modified)
-- ALTER TABLE ... ;

COMMIT;
```

**File:** `scripts/db/rollback.sh`

```bash
#!/bin/bash
set -euo pipefail

TARGET_VERSION=${1:-}

if [[ -z "$TARGET_VERSION" ]]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 2  # Roll back to V2"
    exit 1
fi

echo "⏪ Rolling back to version $TARGET_VERSION..."

# 1. Backup current DB
BACKUP_FILE="/tmp/db-backup-$(date +%Y%m%d-%H%M%S).sql"
pg_dump -h localhost -U postgres -d core > $BACKUP_FILE
echo "💾 Backup saved: $BACKUP_FILE"

# 2. Get current version
CURRENT_VERSION=$(psql -h localhost -U postgres -d core -tAc \
    "SELECT MAX(version) FROM flyway_schema_history WHERE success=true")

echo "📊 Current version: $CURRENT_VERSION"
echo "🎯 Target version: $TARGET_VERSION"

# 3. Execute undo migrations
for V in $(seq $CURRENT_VERSION -1 $(($TARGET_VERSION + 1))); do
    UNDO_FILE="backend/src/main/resources/db/migration/core/U${V}__*.sql"
    
    if [ ! -f $UNDO_FILE ]; then
        echo "❌ Undo migration not found: $UNDO_FILE"
        exit 1
    fi
    
    echo "🔄 Executing: $UNDO_FILE"
    psql -h localhost -U postgres -d core < $UNDO_FILE
done

# 4. Update Flyway history
psql -h localhost -U postgres -d core <<EOF
DELETE FROM flyway_schema_history 
WHERE version::int > $TARGET_VERSION;
EOF

echo "✅ Rollback completed to version $TARGET_VERSION"
echo "💡 Backup available at: $BACKUP_FILE"
```

**File:** `scripts/db/validate-undo-scripts.sh` (CI check)

```bash
#!/bin/bash
set -euo pipefail

MISSING=()

# Find all V migrations
for V_FILE in backend/src/main/resources/db/migration/core/V*.sql; do
    VERSION=$(basename $V_FILE | sed 's/V\([0-9]*\).*/\1/')
    U_FILE="backend/src/main/resources/db/migration/core/U${VERSION}__*.sql"
    
    if [ ! -f $U_FILE ]; then
        MISSING+=("U${VERSION}")
    fi
done

if [ ${#MISSING[@]} -eq 0 ]; then
    echo "✅ All V migrations have U counterparts"
    exit 0
else
    echo "❌ Missing undo migrations:"
    printf '%s\n' "${MISSING[@]}"
    exit 1
fi
```

**Makefile Targets:**

```makefile
.PHONY: db-rollback
db-rollback:
	@if [ -z "$(VERSION)" ]; then \
		echo "❌ Usage: make db-rollback VERSION=2"; \
		exit 1; \
	fi
	bash scripts/db/rollback.sh $(VERSION)

.PHONY: db-rollback-test
db-rollback-test:
	@echo "🧪 Testing rollback flow..."
	# Apply V3
	./mvnw flyway:migrate -Dflyway.target=3
	# Rollback to V2
	bash scripts/db/rollback.sh 2
	# Re-apply to V3
	./mvnw flyway:migrate -Dflyway.target=3
	@echo "✅ Rollback test passed"
```

**Effort:** 2 dny  
**LOC:** ~500  
**Blocks:** Production deployments

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
