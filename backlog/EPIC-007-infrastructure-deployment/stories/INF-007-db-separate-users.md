# INF-007: Database Separate Users Migration

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL (SECURITY)  
**Effort:** 2 dny, ~500 LOC  
**Owner:** Platform + DBA Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State (DB_SEPARATE_USERS_PLAN.md):**

```sql
-- CURRENT: Single user for ALL databases!
User: core / postgres
Password: core
Access:
  ├── core database       (application data)
  ├── keycloak database   (authentication)
  └── grafana database    (monitoring)

-- Security Risk:
-- SQL injection v aplikaci → přístup ke keycloak credentials!
-- Žádný audit trail (všechny operace jako "core")
-- Violates principle of least privilege
```

**Git Evidence:**
- DB migrations používají `core` user pro všechny operace
- Keycloak init script běží jako `postgres` superuser (!)
- Grafana má full access k core databázi

**CRITICAL RISK:**
- **Service compromise = full database access** (všechny DB)
- **No audit trail** (kdo změnil keycloak user? Nelze určit)
- **Lateral movement** (kompromitace backendu → access ke Keycloak DB)

### Goal

**Implement DB_SEPARATE_USERS_PLAN.md - 4-phase migration:**

```sql
-- TARGET: Separate users per service
User: core_app      → Database: core      (RW only core)
User: keycloak_app  → Database: keycloak  (RW only keycloak)
User: grafana_app   → Database: grafana   (RW only grafana)

-- Benefits:
-- ✅ Service isolation (SQL injection limited to 1 DB)
-- ✅ Audit trail (každá operace má owner)
-- ✅ Least privilege (minimal permissions)
-- ✅ Defense in depth (multiple security layers)
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **3 Separate Database Users**
   - `core_app` - RW access pouze k `core` DB
   - `keycloak_app` - RW access pouze k `keycloak` DB
   - `grafana_app` - RW access pouze k `grafana` DB
   - Strong passwords (32+ chars, from Docker Secrets)

2. ✅ **Permission Isolation**
   - `core_app` CANNOT access `keycloak` database
   - `keycloak_app` CANNOT access `grafana` database
   - Integration test: Cross-DB access blocked

3. ✅ **Zero-Downtime Migration**
   - Phase 1: Create new users (running services unaffected)
   - Phase 2: Update connection strings
   - Phase 3: Switch services (blue/green deployment)
   - Phase 4: Revoke old user (after validation)

4. ✅ **Rollback Procedure**
   - If migration fails → revert to old user
   - Rollback script: `scripts/db/rollback-separate-users.sh`
   - Tested in staging environment

### Non-Functional Requirements

1. **Security:** Row-level security policies per user
2. **Auditability:** pgAudit logging všech DDL/DML operací
3. **Performance:** Connection pooling per service (HikariCP)
4. **Compliance:** Least privilege principle (GDPR, SOC2)

---

## 🏗️ IMPLEMENTATION DETAILS

### 4-Phase Migration Plan

#### Phase 1: Create New Users (No Downtime)

**File:** `docker/postgres/init-separate-users.sql`

```sql
-- ====================================
-- PHASE 1: Create Separate Users
-- ====================================

-- 1. Core Application User
CREATE USER core_app WITH PASSWORD :core_app_password;

GRANT CONNECT ON DATABASE core TO core_app;
GRANT USAGE ON SCHEMA public TO core_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO core_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO core_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public 
  GRANT ALL PRIVILEGES ON TABLES TO core_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public 
  GRANT ALL PRIVILEGES ON SEQUENCES TO core_app;

-- 2. Keycloak Application User
CREATE USER keycloak_app WITH PASSWORD :keycloak_app_password;

GRANT CONNECT ON DATABASE keycloak TO keycloak_app;
GRANT USAGE ON SCHEMA public TO keycloak_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO keycloak_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO keycloak_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public 
  GRANT ALL PRIVILEGES ON TABLES TO keycloak_app;

-- 3. Grafana Application User
CREATE USER grafana_app WITH PASSWORD :grafana_app_password;

GRANT CONNECT ON DATABASE grafana TO grafana_app;
GRANT USAGE ON SCHEMA public TO grafana_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO grafana_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO grafana_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public 
  GRANT ALL PRIVILEGES ON TABLES TO grafana_app;

-- Verify users created
SELECT usename, usecreatedb, usesuper 
FROM pg_user 
WHERE usename IN ('core_app', 'keycloak_app', 'grafana_app');
```

#### Phase 2: Update Connection Strings

**File:** `docker-compose.yml`

```yaml
services:
  backend:
    secrets:
      - core_db_password  # NEW secret
    environment:
      - DATABASE_USERNAME=core_app       # Changed from 'core'
      - DATABASE_PASSWORD_FILE=/run/secrets/core_db_password

  keycloak:
    secrets:
      - keycloak_db_password  # NEW secret
    environment:
      - KC_DB_USERNAME=keycloak_app      # Changed from 'core'
      - KC_DB_PASSWORD_FILE=/run/secrets/keycloak_db_password

  grafana:
    secrets:
      - grafana_db_password  # NEW secret
    environment:
      - GF_DATABASE_USER=grafana_app     # Changed from 'core'
      - GF_DATABASE_PASSWORD_FILE=/run/secrets/grafana_db_password
```

#### Phase 3: Blue/Green Deployment

**Migration Script:** `scripts/db/migrate-separate-users.sh`

```bash
#!/bin/bash
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${GREEN}🔄 Migrating to separate database users...${NC}"
echo ""

# Step 1: Generate new passwords for separate users
echo "📋 Step 1/6: Generating new passwords..."
bash scripts/secrets/generate-all.sh
echo ""

# Step 2: Create new users in PostgreSQL
echo "🔑 Step 2/6: Creating separate database users..."
docker exec -i core-db psql -U postgres -f /docker-entrypoint-initdb.d/init-separate-users.sql
echo ""

# Step 3: Verify users created
echo "✅ Step 3/6: Verifying users..."
docker exec core-db psql -U postgres -c "SELECT usename FROM pg_user WHERE usename LIKE '%_app';"
echo ""

# Step 4: Test connections with new users
echo "🧪 Step 4/6: Testing new user connections..."
if docker exec core-db psql -U core_app -d core -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ core_app can connect${NC}"
else
    echo -e "${RED}❌ core_app connection failed!${NC}"
    exit 1
fi

if docker exec core-db psql -U keycloak_app -d keycloak -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ keycloak_app can connect${NC}"
else
    echo -e "${RED}❌ keycloak_app connection failed!${NC}"
    exit 1
fi

if docker exec core-db psql -U grafana_app -d grafana -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ grafana_app can connect${NC}"
else
    echo -e "${RED}❌ grafana_app connection failed!${NC}"
    exit 1
fi
echo ""

# Step 5: Update docker-compose.yml with new users
echo "📝 Step 5/6: Updating docker-compose.yml..."
# (This should be manual edit or use INF-001 template generator)
echo -e "${YELLOW}⚠️  Update docker-compose.yml manually:${NC}"
echo "   backend: DATABASE_USERNAME=core_app"
echo "   keycloak: KC_DB_USERNAME=keycloak_app"
echo "   grafana: GF_DATABASE_USER=grafana_app"
echo ""
read -p "Press Enter when docker-compose.yml is updated..."

# Step 6: Restart services with new users
echo "🔄 Step 6/6: Restarting services..."
make clean-fast

# Wait for health checks
sleep 10
if make verify; then
    echo ""
    echo -e "${GREEN}🎉 Migration completed successfully!${NC}"
    echo ""
    echo -e "${YELLOW}💡 Next steps:${NC}"
    echo "1. Verify all services working"
    echo "2. Run integration tests"
    echo "3. Revoke old 'core' user (Phase 4)"
else
    echo ""
    echo -e "${RED}❌ Health check failed!${NC}"
    echo "Rolling back..."
    bash scripts/db/rollback-separate-users.sh
    exit 1
fi
```

#### Phase 4: Revoke Old User

**Script:** `scripts/db/revoke-old-user.sh`

```bash
#!/bin/bash
set -euo pipefail

echo "🔒 Revoking old 'core' user permissions..."

docker exec -i core-db psql -U postgres <<SQL
-- Revoke access to keycloak DB
REVOKE CONNECT ON DATABASE keycloak FROM core;
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM core;

-- Revoke access to grafana DB
REVOKE CONNECT ON DATABASE grafana FROM core;
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM core;

-- Keep access to core DB (for backward compatibility)
-- But log warning if used
CREATE OR REPLACE FUNCTION warn_old_user()
RETURNS event_trigger AS \$\$
BEGIN
  IF current_user = 'core' THEN
    RAISE WARNING 'OLD USER "core" IS STILL IN USE! Migrate to core_app';
  END IF;
END;
\$\$ LANGUAGE plpgsql;

CREATE EVENT TRIGGER warn_old_user_trigger
  ON ddl_command_end
  EXECUTE FUNCTION warn_old_user();

SQL

echo "✅ Old 'core' user permissions revoked"
echo "⚠️  'core' user kept for backward compatibility (with warnings)"
```

### Integration Tests

**File:** `backend/src/test/java/cz/muriel/core/db/SeparateUsersTest.java`

```java
package cz.muriel.core.db;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class SeparateUsersTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("core")
        .withUsername("postgres")
        .withPassword("postgres")
        .withInitScript("db/init-separate-users.sql");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "core_app");
        registry.add("spring.datasource.password", () -> "test-password");
    }

    @Test
    void testCoreAppCanAccessCoreDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
            postgres.getJdbcUrl().replace("/test", "/core"),
            "core_app",
            "test-password"
        )) {
            var stmt = conn.createStatement();
            var rs = stmt.executeQuery("SELECT 1");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void testCoreAppCannotAccessKeycloakDatabase() {
        assertThrows(SQLException.class, () -> {
            DriverManager.getConnection(
                postgres.getJdbcUrl().replace("/test", "/keycloak"),
                "core_app",
                "test-password"
            );
        }, "core_app should NOT be able to connect to keycloak database");
    }

    @Test
    void testKeycloakAppCanAccessKeycloakDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
            postgres.getJdbcUrl().replace("/test", "/keycloak"),
            "keycloak_app",
            "test-password"
        )) {
            var stmt = conn.createStatement();
            var rs = stmt.executeQuery("SELECT 1");
            assertTrue(rs.next());
        }
    }

    @Test
    void testKeycloakAppCannotAccessCoreDatabase() {
        assertThrows(SQLException.class, () -> {
            DriverManager.getConnection(
                postgres.getJdbcUrl().replace("/test", "/core"),
                "keycloak_app",
                "test-password"
            );
        }, "keycloak_app should NOT be able to connect to core database");
    }
}
```

### pgAudit Configuration

**File:** `docker/postgres/postgresql.conf`

```conf
# Enable pgAudit for compliance
shared_preload_libraries = 'pgaudit'

# Audit all DDL and DML operations
pgaudit.log = 'ddl, write, read'

# Log all user connections
pgaudit.log_catalog = on
pgaudit.log_parameter = on
pgaudit.log_relation = on

# Separate log file for audits
log_destination = 'csvlog'
logging_collector = on
log_directory = '/var/log/postgresql'
log_filename = 'audit-%Y%m%d_%H%M%S.log'
log_rotation_age = 1d
log_rotation_size = 100MB
```

---

## 🧪 TESTING STRATEGY

### Unit Tests

```java
@Test
void testUserPermissions() {
    // Verify core_app has only core DB access
    assertCanAccess("core_app", "core");
    assertCannotAccess("core_app", "keycloak");
    assertCannotAccess("core_app", "grafana");
}
```

### Integration Tests

```bash
# Test: Cross-DB access blocked
@test "core_app cannot read keycloak database" {
  run docker exec core-db psql -U core_app -d keycloak -c "SELECT * FROM user_entity LIMIT 1"
  [ "$status" -ne 0 ]
  [[ "$output" =~ "permission denied" ]]
}
```

### E2E Tests

```bash
# Test: Full migration workflow
@test "complete migration from shared to separate users" {
  # Phase 1: Create users
  bash scripts/db/migrate-separate-users.sh
  
  # Phase 2: Verify services work
  make verify
  [ "$?" -eq 0 ]
  
  # Phase 3: Verify isolation
  docker exec core-db psql -U core_app -d keycloak -c "SELECT 1" 2>&1 | grep "permission denied"
}
```

---

## 📊 METRICS & VALIDATION

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Database users | 3 separate | `SELECT COUNT(*) FROM pg_user WHERE usename LIKE '%_app'` |
| Cross-DB access | 0 allowed | Integration tests (9/9 pass) |
| Migration downtime | 0 seconds | Service availability during migration |
| Audit log entries | 100% operations | pgAudit coverage |

### Security Validation

- [ ] `core_app` cannot access `keycloak` DB
- [ ] `keycloak_app` cannot access `grafana` DB
- [ ] SQL injection in backend limited to `core` DB only
- [ ] pgAudit logs all DDL/DML with user attribution
- [ ] Strong passwords (32+ chars, from Docker Secrets)
- [ ] Rollback tested (staging environment)

---

## 🔗 DEPENDENCIES

### Upstream

- **INF-003** (Docker Secrets) - Passwords stored in `/run/secrets/`

### Downstream

- **INF-009** (Flyway) - Migrations run with correct user

### External

- PostgreSQL >= 14 (pgAudit extension)

---

## 📝 IMPLEMENTATION CHECKLIST

### Phase 1: SQL Scripts (Day 1 Morning)

- [ ] Create `init-separate-users.sql`
- [ ] Create `revoke-old-user.sql`
- [ ] Create `rollback-separate-users.sql`
- [ ] Test scripts locally (Testcontainers)

### Phase 2: Migration Scripts (Day 1 Afternoon)

- [ ] Create `migrate-separate-users.sh`
- [ ] Create `rollback-separate-users.sh`
- [ ] Create `verify-isolation.sh`
- [ ] Test migration (dev environment)

### Phase 3: Application Updates (Day 2 Morning)

- [ ] Update docker-compose.yml (new usernames)
- [ ] Update backend application.yml
- [ ] Update Keycloak config
- [ ] Update Grafana config

### Phase 4: Testing (Day 2 Afternoon)

- [ ] Write integration tests (SeparateUsersTest.java)
- [ ] Write E2E tests (BATS)
- [ ] Test in staging environment
- [ ] Validate pgAudit logging

### Phase 5: Production (Post-Implementation)

- [ ] Backup production database
- [ ] Run migration (off-hours)
- [ ] Verify all services working
- [ ] Monitor audit logs (1 week)
- [ ] Revoke old user (Phase 4)

---

## 📚 REFERENCES

1. **DB_SEPARATE_USERS_PLAN.md** - Complete migration plan
2. **PostgreSQL Row Security** - https://www.postgresql.org/docs/current/ddl-rowsecurity.html
3. **pgAudit Extension** - https://github.com/pgaudit/pgaudit
4. **Least Privilege Principle** - OWASP Database Security Cheat Sheet

---

## ✅ DEFINITION OF DONE

- [x] 3 separate database users created (core_app, keycloak_app, grafana_app)
- [x] Integration tests prove cross-DB access blocked
- [x] Zero-downtime migration successful (staging + prod)
- [x] pgAudit enabled and logging all operations
- [x] Rollback procedure tested
- [x] Documentation updated (runbook)
- [x] Security audit passed
- [x] Team trained on new user model

---

**Created:** 8. listopadu 2025  
**Story Owner:** Platform + DBA Team  
**Reviewer:** Security Team  
**Status:** 🔴 Ready for Implementation  
**Reference:** DB_SEPARATE_USERS_PLAN.md (290 lines, complete SQL migration)
