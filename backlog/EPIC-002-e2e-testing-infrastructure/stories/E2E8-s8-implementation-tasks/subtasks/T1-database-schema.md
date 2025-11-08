# T1: Database Schema Migration

**Story:** [S8: Test Registry & Tracking](../S8.md)  
**Status:** 🔵 TODO  
**Effort:** ~1 hodina  
**LOC:** ~50 řádků

---

## 🎯 Objective

Vytvořit PostgreSQL tabulku `test_registry` pro evidenci všech testů s vazbou na User Stories.

---

## 📋 Requirements

### Schema Design

**Table:** `test_registry`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | Auto-increment ID |
| `test_id` | VARCHAR(255) | NOT NULL, UNIQUE | Unique test identifier (e.g., "E2E-LOGIN-001") |
| `user_story_id` | VARCHAR(50) | - | User Story ID (e.g., "CORE-123") |
| `test_type` | VARCHAR(50) | NOT NULL | Test type enum (E2E_SMOKE, UNIT_BE, etc.) |
| `test_name` | VARCHAR(500) | NOT NULL | Human-readable test name |
| `file_path` | VARCHAR(1000) | - | Test file path |
| `status` | VARCHAR(20) | NOT NULL | Test status (PASS, FAIL, SKIP) |
| `last_run_at` | TIMESTAMP | - | Last execution timestamp |
| `duration_ms` | INTEGER | - | Test execution time in ms |
| `coverage_lines` | DECIMAL(5,2) | - | Line coverage percentage |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Record creation time |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update time |

### Indexes

- `idx_test_registry_story` - ON `user_story_id` (coverage queries)
- `idx_test_registry_type` - ON `test_type` (filter by test type)
- `idx_test_registry_status` - ON `status` (filter by pass/fail)

---

## 💻 Implementation

### File Location

```
backend/src/main/resources/db/migration/V999__test_registry.sql
```

### Migration Script

```sql
-- Test Registry Table
-- Stores all test executions with mapping to User Stories

CREATE TABLE test_registry (
    id BIGSERIAL PRIMARY KEY,
    test_id VARCHAR(255) NOT NULL UNIQUE,
    user_story_id VARCHAR(50),
    test_type VARCHAR(50) NOT NULL,
    test_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    last_run_at TIMESTAMP,
    duration_ms INTEGER,
    coverage_lines DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for query performance
CREATE INDEX idx_test_registry_story ON test_registry(user_story_id);
CREATE INDEX idx_test_registry_type ON test_registry(test_type);
CREATE INDEX idx_test_registry_status ON test_registry(status);

-- Comments for documentation
COMMENT ON TABLE test_registry IS 'Registry of all test executions with User Story mapping';
COMMENT ON COLUMN test_registry.test_id IS 'Unique test identifier (e.g., E2E-LOGIN-001)';
COMMENT ON COLUMN test_registry.user_story_id IS 'User Story ID (e.g., CORE-123)';
COMMENT ON COLUMN test_registry.test_type IS 'Test type: E2E_SMOKE, E2E_FULL, UNIT_FE, UNIT_BE, INTEGRATION, A11Y';
COMMENT ON COLUMN test_registry.status IS 'Test status: PASS, FAIL, SKIP';
COMMENT ON COLUMN test_registry.duration_ms IS 'Test execution time in milliseconds';
COMMENT ON COLUMN test_registry.coverage_lines IS 'Line coverage percentage (0-100)';
```

---

## 🧪 Testing

### Manual Verification

```bash
# 1. Run migration
cd backend
./mvnw flyway:migrate

# 2. Verify table exists
psql -U core -d core -c "\d test_registry"

# Expected output:
#                                      Table "public.test_registry"
#     Column      |            Type             | Nullable |       Default        
# ----------------+-----------------------------+----------+----------------------
#  id             | bigint                      | not null | nextval('...')
#  test_id        | character varying(255)      | not null | 
#  user_story_id  | character varying(50)       |          | 
#  ...

# 3. Verify indexes
psql -U core -d core -c "\di test_registry*"

# Expected: idx_test_registry_story, idx_test_registry_type, idx_test_registry_status

# 4. Insert test record
psql -U core -d core -c "
INSERT INTO test_registry (test_id, user_story_id, test_type, test_name, status)
VALUES ('E2E-TEST-001', 'CORE-123', 'E2E_SMOKE', 'Test login flow', 'PASS');
"

# 5. Query test record
psql -U core -d core -c "SELECT * FROM test_registry WHERE user_story_id = 'CORE-123';"
```

### Makefile Integration

```makefile
# Add to Makefile
db-migrate:
	cd backend && ./mvnw flyway:migrate

db-status:
	cd backend && ./mvnw flyway:info

db-rollback:
	cd backend && ./mvnw flyway:undo
```

---

## ✅ Acceptance Criteria

- [ ] Migration file created: `V999__test_registry.sql`
- [ ] Table `test_registry` created in database
- [ ] All columns defined with correct types
- [ ] UNIQUE constraint on `test_id`
- [ ] 3 indexes created (story, type, status)
- [ ] Table comments added for documentation
- [ ] Migration runs successfully: `make db-migrate`
- [ ] Test insert/query works

---

## 🎯 Definition of Done

- [ ] Migration file committed to Git
- [ ] Migration executed on dev database
- [ ] `\d test_registry` shows correct schema
- [ ] `\di` shows 3 indexes
- [ ] Test data inserted successfully
- [ ] Coverage query works: `SELECT COUNT(DISTINCT user_story_id) FROM test_registry`

---

## 📝 Notes

### Version Number (V999)

Použij `V999` jako placeholder. Před mergem do main:

1. Check latest version: `ls backend/src/main/resources/db/migration/ | sort -V | tail -1`
2. Rename: `V999` → `V<next>` (e.g., `V023__test_registry.sql`)

### Rollback Strategy

Flyway nepodporuje automatický rollback. Pokud je potřeba rollback:

```sql
-- backend/src/main/resources/db/migration/U999__test_registry.sql
DROP TABLE IF EXISTS test_registry CASCADE;
```

Pak: `./mvnw flyway:undo`

---

## 🔗 Next Tasks

- **T2**: [Backend Model & Repository](./T2-backend-model-repository.md) - JPA entity pro test_registry
- **T3**: [REST API Controller](./T3-rest-api-controller.md) - Endpoints pro registry

---

**Back to:** [S8 Tasks](./README.md) | [S8 Story](../S8.md)
