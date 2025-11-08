# T2: Migrate Keycloak Credentials to Docker Secrets

**Parent Story:** INF-003 Docker Secrets Migration  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 3 hours  
**Owner:** DevOps

---

## 🎯 Objective

Migrate Keycloak admin credentials and client secrets from `.env` to Docker Secrets.

---

## 📋 Tasks

### 1. Create Secret Files

```bash
# secrets/keycloak_admin_user.txt
echo "admin" > secrets/keycloak_admin_user.txt

# secrets/keycloak_admin_password.txt
openssl rand -base64 32 > secrets/keycloak_admin_password.txt

# secrets/keycloak_client_secret.txt
uuidgen > secrets/keycloak_client_secret.txt
```

### 2. Update docker-compose.yml

```yaml
services:
  keycloak:
    secrets:
      - keycloak_admin_user
      - keycloak_admin_password
    environment:
      KEYCLOAK_ADMIN_FILE: /run/secrets/keycloak_admin_user
      KEYCLOAK_ADMIN_PASSWORD_FILE: /run/secrets/keycloak_admin_password

secrets:
  keycloak_admin_user:
    file: ./secrets/keycloak_admin_user.txt
  keycloak_admin_password:
    file: ./secrets/keycloak_admin_password.txt
  keycloak_client_secret:
    file: ./secrets/keycloak_client_secret.txt
```

### 3. Update Backend application.yml

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-secret: ${KEYCLOAK_CLIENT_SECRET}  # From Docker env
```

### 4. Update realm-admin.template.json

```json
{
  "clients": [{
    "clientId": "admin-client",
    "secret": "${KEYCLOAK_CLIENT_SECRET}"  # Substituted at build time
  }]
}
```

---

## ✅ Acceptance Criteria

- [ ] Keycloak admin credentials in Docker Secrets
- [ ] Client secret in Docker Secrets
- [ ] Backend OAuth2 authentication works
- [ ] Keycloak admin console login successful
- [ ] No secrets in `.env` file

---

## 🔗 Dependencies

- Requires T1 (PostgreSQL secrets pattern established)
- Blocks backend startup
