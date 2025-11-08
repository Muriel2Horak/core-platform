# T1: Migrate PostgreSQL Credentials to Docker Secrets

**Parent Story:** INF-003 Docker Secrets Migration  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 4 hours  
**Owner:** DevOps

---

## 🎯 Objective

Migrate PostgreSQL credentials from plain-text `.env` to Docker Secrets.

---

## 📋 Tasks

### 1. Create Secret Files

```bash
# secrets/postgres_user.txt
echo "core" > secrets/postgres_user.txt

# secrets/postgres_password.txt
openssl rand -base64 32 > secrets/postgres_password.txt

# secrets/postgres_db.txt
echo "core" > secrets/postgres_db.txt
```

### 2. Update docker-compose.yml

```yaml
services:
  db:
    secrets:
      - postgres_user
      - postgres_password
      - postgres_db
    environment:
      POSTGRES_USER_FILE: /run/secrets/postgres_user
      POSTGRES_PASSWORD_FILE: /run/secrets/postgres_password
      POSTGRES_DB_FILE: /run/secrets/postgres_db

secrets:
  postgres_user:
    file: ./secrets/postgres_user.txt
  postgres_password:
    file: ./secrets/postgres_password.txt
  postgres_db:
    file: ./secrets/postgres_db.txt
```

### 3. Update Backend application.yml

```yaml
spring:
  datasource:
    username: ${POSTGRES_USER}  # Read from Docker env (injected from secret)
    password: ${POSTGRES_PASSWORD}
```

### 4. Test

```bash
make clean-fast
make logs-backend | grep -i "datasource"
# Verify connection successful
```

---

## ✅ Acceptance Criteria

- [ ] Secrets files created in `secrets/` directory
- [ ] `.gitignore` updated (secrets/*.txt)
- [ ] docker-compose.yml uses Docker Secrets
- [ ] Backend connects to PostgreSQL successfully
- [ ] No plain-text passwords in `.env`

---

## 🔗 Dependencies

- Requires Docker Compose 1.25+ (secrets support)
- Blocks INF-007 (DB separate users)
