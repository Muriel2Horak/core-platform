# DATABASE SECURITY: Separate User Accounts Plan

**Created:** 27. října 2025  
**Purpose:** Security hardening - separate DB users per service with minimal permissions

---

## 🎯 CURRENT PROBLEM

**Single user for all databases:**
```sql
-- Everything uses same credentials:
User: core / postgres
Password: core
Access: ALL databases (core, keycloak, grafana)
```

**Security Risks:**
- ❌ Service compromise = full DB access
- ❌ No audit trail (who accessed what?)
- ❌ Violates principle of least privilege
- ❌ Cannot revoke access granularly

---

## ✅ TARGET STATE

### Separate Users per Database

```sql
-- Core Application Database
User: core_app
Password: <generate-strong-password>
Access: ONLY database 'core'

-- Keycloak Database  
User: keycloak_app
Password: <generate-strong-password>
Access: ONLY database 'keycloak'

-- Grafana Database
User: grafana_app
Password: <generate-strong-password>
Access: ONLY database 'grafana'
```

**Benefits:**
- ✅ Service isolation (backend can't touch Keycloak DB)
- ✅ Audit trail (know which service made changes)
- ✅ Granular access control
- ✅ Easier credential rotation per service

---

## 📋 MIGRATION PLAN

### Phase 1: Create Separate Users (No Downtime)

```sql
-- Connect to PostgreSQL
-- docker exec -it core-db psql -U postgres

-- 1. Create separate users
CREATE USER core_app WITH PASSWORD 'GENERATE_STRONG_PASSWORD_1';
CREATE USER keycloak_app WITH PASSWORD 'GENERATE_STRONG_PASSWORD_2';
CREATE USER grafana_app WITH PASSWORD 'GENERATE_STRONG_PASSWORD_3';

-- 2. Grant minimal permissions
GRANT ALL PRIVILEGES ON DATABASE core TO core_app;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak_app;
GRANT ALL PRIVILEGES ON DATABASE grafana TO grafana_app;

-- 3. Grant schema permissions
\c core
GRANT ALL ON SCHEMA public TO core_app;
GRANT ALL ON ALL TABLES IN SCHEMA public TO core_app;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO core_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO core_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO core_app;

\c keycloak
GRANT ALL ON SCHEMA public TO keycloak_app;
GRANT ALL ON ALL TABLES IN SCHEMA public TO keycloak_app;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO keycloak_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO keycloak_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO keycloak_app;

\c grafana
GRANT ALL ON SCHEMA public TO grafana_app;
GRANT ALL ON ALL TABLES IN SCHEMA public TO grafana_app;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO grafana_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO grafana_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO grafana_app;
```

### Phase 2: Update .env Configuration

**Add to `.env.template`:**
```bash
# === DATABASE CREDENTIALS (SEPARATE PER SERVICE) ===

# Core Application DB
DATABASE_USERNAME=core_app
DATABASE_PASSWORD=<STRONG_PASSWORD_1>

# Keycloak DB
KEYCLOAK_DB_USERNAME=keycloak_app
KEYCLOAK_DB_PASSWORD=<STRONG_PASSWORD_2>

# Grafana DB
GRAFANA_DB_USERNAME=grafana_app
GRAFANA_DB_PASSWORD=<STRONG_PASSWORD_3>

# PostgreSQL admin (for migrations only)
POSTGRES_USER=postgres
POSTGRES_PASSWORD=<STRONG_POSTGRES_ADMIN_PASSWORD>
```

### Phase 3: Update docker-compose.yml

**Backend service:**
```yaml
environment:
  DATABASE_URL: jdbc:postgresql://core-db:5432/core
  DATABASE_USERNAME: ${DATABASE_USERNAME:-core_app}
  DATABASE_PASSWORD: ${DATABASE_PASSWORD}
```

**Keycloak service:**
```yaml
environment:
  KC_DB_USERNAME: ${KEYCLOAK_DB_USERNAME:-keycloak_app}
  KC_DB_PASSWORD: ${KEYCLOAK_DB_PASSWORD}
```

**Grafana service:**
```yaml
environment:
  GF_DATABASE_USER: ${GRAFANA_DB_USERNAME:-grafana_app}
  GF_DATABASE_PASSWORD: ${GRAFANA_DB_PASSWORD}
```

### Phase 4: Testing & Validation

```bash
# 1. Generate passwords
openssl rand -base64 32  # For DATABASE_PASSWORD
openssl rand -base64 32  # For KEYCLOAK_DB_PASSWORD
openssl rand -base64 32  # For GRAFANA_DB_PASSWORD

# 2. Update .env with new credentials
vim .env
# (add new variables)

# 3. Restart services one by one
make restart-backend
make restart-keycloak
make logs-backend | grep datasource  # Verify connection
make logs-keycloak | grep database   # Verify connection

# 4. Verify permissions
docker exec core-db psql -U core_app -d core -c "SELECT 1;"
# Should succeed

docker exec core-db psql -U core_app -d keycloak -c "SELECT 1;"
# Should FAIL (no access to other DB)

# 5. Full smoke test
make verify
make test-backend
```

### Phase 5: Revoke Old User (After Validation)

```sql
-- ONLY after confirming all services work with new users!

-- Revoke old user permissions
REVOKE ALL PRIVILEGES ON DATABASE core FROM core;
REVOKE ALL PRIVILEGES ON DATABASE keycloak FROM core;
REVOKE ALL PRIVILEGES ON DATABASE grafana FROM core;

-- Optional: Drop old user (if not used elsewhere)
-- DROP USER core;
```

---

## 🔒 PASSWORD GENERATION

```bash
# Generate strong passwords (32 bytes = 256 bits)
echo "DATABASE_PASSWORD=$(openssl rand -base64 32)"
echo "KEYCLOAK_DB_PASSWORD=$(openssl rand -base64 32)"
echo "GRAFANA_DB_PASSWORD=$(openssl rand -base64 32)"
echo "POSTGRES_PASSWORD=$(openssl rand -base64 32)"

# Example output:
# DATABASE_PASSWORD=7x4K9mP2vL8nQ6wR5tY3uZ1aB4cD7eF9
# KEYCLOAK_DB_PASSWORD=8a5B2cD4eF6gH9jK1mN3pQ5rS7tU9vW
# GRAFANA_DB_PASSWORD=9b6C3dE5fG7hJ0kL2nP4qR6sT8uV0wX
```

**Store securely:**
- Development: `.env` (gitignored)
- Staging: Environment-specific `.env.staging`
- Production: Secrets manager (HashiCorp Vault, AWS Secrets Manager)

---

## 🧪 TESTING CHECKLIST

- [ ] All three users created in PostgreSQL
- [ ] Permissions granted per database
- [ ] .env.template updated with new variables
- [ ] docker-compose.yml updated to use new env vars
- [ ] Backend starts and connects successfully
- [ ] Keycloak starts and connects successfully
- [ ] Grafana starts and connects successfully
- [ ] Backend CANNOT access Keycloak DB (verified)
- [ ] Keycloak CANNOT access Core DB (verified)
- [ ] All unit tests pass (`make test-backend`)
- [ ] Integration tests pass (`make verify-full`)
- [ ] E2E tests pass (`make test-e2e-pre`)
- [ ] Old 'core' user revoked (after validation period)

---

## 📅 ROLLOUT TIMELINE

**Week 1: Preparation**
- Create SQL script for user creation
- Generate strong passwords
- Update .env.template
- Test in local environment

**Week 2: Staging Deployment**
- Apply changes to staging
- Monitor for 48 hours
- Validate all services functional
- Performance baseline check

**Week 3: Production Deployment**
- Scheduled maintenance window
- Apply changes to production
- Immediate verification tests
- Monitor for 7 days before revoking old user

**Week 4: Cleanup**
- Revoke old 'core' user permissions
- Update documentation
- Archive old credentials securely

---

## 🚨 ROLLBACK PLAN

If issues occur:

```bash
# 1. Revert .env to old credentials
cp .env.backup .env

# 2. Restart affected service
make restart-backend  # or restart-keycloak, etc.

# 3. Verify old credentials still work
docker exec core-db psql -U core -d core -c "SELECT 1;"

# 4. Investigate issue
make logs-backend
make doctor
```

---

## 📖 REFERENCES

- PostgreSQL User Management: https://www.postgresql.org/docs/current/user-manag.html
- GRANT Syntax: https://www.postgresql.org/docs/current/sql-grant.html
- Principle of Least Privilege: https://owasp.org/www-community/Access_Control
- Docker Secrets: https://docs.docker.com/engine/swarm/secrets/

---

**Status:** 📝 PLANNING STAGE  
**Next Step:** Generate passwords and create SQL script  
**Owner:** DevOps Team  
**Review Date:** 2025-11-03
