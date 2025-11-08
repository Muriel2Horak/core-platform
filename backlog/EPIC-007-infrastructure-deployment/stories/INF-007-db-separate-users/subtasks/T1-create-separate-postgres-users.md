# T1: Create Separate PostgreSQL Users per Service

**Parent Story:** INF-007 DB Separate Users per Service  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 3 hours  
**Owner:** Backend + DevOps

---

## 🎯 Objective

Create dedicated PostgreSQL users for backend, keycloak, grafana with least-privilege permissions.

---

## 📋 Tasks

### 1. Create SQL Migration Script

**File:** `backend/src/main/resources/db/migration/V999__create_separate_users.sql`

```sql
-- Backend user (application access)
CREATE USER core_app WITH PASSWORD 'CHANGE_ME_IN_PRODUCTION';
GRANT CONNECT ON DATABASE core TO core_app;
GRANT USAGE ON SCHEMA public TO core_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO core_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO core_app;

-- Keycloak user (auth database)
CREATE USER keycloak_app WITH PASSWORD 'CHANGE_ME_IN_PRODUCTION';
GRANT CONNECT ON DATABASE keycloak TO keycloak_app;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak_app;

-- Grafana user (dashboards)
CREATE USER grafana_app WITH PASSWORD 'CHANGE_ME_IN_PRODUCTION';
GRANT CONNECT ON DATABASE grafana TO grafana_app;
GRANT ALL PRIVILEGES ON DATABASE grafana TO grafana_app;

-- Revoke public access
REVOKE ALL ON DATABASE core FROM public;
REVOKE ALL ON DATABASE keycloak FROM public;
REVOKE ALL ON DATABASE grafana FROM public;
```

### 2. Create Docker Secrets

```bash
# secrets/db_backend_password.txt
openssl rand -base64 32 > secrets/db_backend_password.txt

# secrets/db_keycloak_password.txt
openssl rand -base64 32 > secrets/db_keycloak_password.txt

# secrets/db_grafana_password.txt
openssl rand -base64 32 > secrets/db_grafana_password.txt
```

### 3. Update docker-compose.yml

```yaml
services:
  backend:
    environment:
      DATABASE_USERNAME: core_app
      DATABASE_PASSWORD_FILE: /run/secrets/db_backend_password
    secrets:
      - db_backend_password

  keycloak:
    environment:
      KC_DB_USERNAME: keycloak_app
      KC_DB_PASSWORD_FILE: /run/secrets/db_keycloak_password
    secrets:
      - db_keycloak_password

  grafana:
    environment:
      GF_DATABASE_USER: grafana_app
      GF_DATABASE_PASSWORD_FILE: /run/secrets/db_grafana_password
    secrets:
      - db_grafana_password

secrets:
  db_backend_password:
    file: ./secrets/db_backend_password.txt
  db_keycloak_password:
    file: ./secrets/db_keycloak_password.txt
  db_grafana_password:
    file: ./secrets/db_grafana_password.txt
```

### 4. Test Permissions

```bash
# Test backend user (should have SELECT/INSERT/UPDATE/DELETE)
psql -U core_app -d core -c "SELECT * FROM users LIMIT 1;"

# Test backend CANNOT access keycloak DB
psql -U core_app -d keycloak -c "SELECT * FROM user_entity LIMIT 1;"
# Expected: ERROR: permission denied

# Test keycloak user isolated
psql -U keycloak_app -d core -c "SELECT * FROM users LIMIT 1;"
# Expected: ERROR: permission denied
```

---

## ✅ Acceptance Criteria

- [ ] 3 separate users created (core_app, keycloak_app, grafana_app)
- [ ] Each user can ONLY access their own database
- [ ] Backend cannot read Keycloak tables
- [ ] Keycloak cannot read backend tables
- [ ] Passwords in Docker Secrets (not plain-text)
- [ ] Migration script tested in dev environment

---

## 🔗 Dependencies

- Requires INF-003 T1 (Docker Secrets pattern)
- Blocks security audit compliance
