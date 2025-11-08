# T2: Configure Dynamic Database Credentials

**Parent Story:** INF-021 HashiCorp Vault Integration  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 6 hours  
**Owner:** Backend + DevOps

---

## 🎯 Objective

Configure Vault to generate dynamic PostgreSQL credentials with 24-hour TTL.

---

## 📋 Tasks

### 1. Configure Vault Database Connection

```bash
#!/bin/bash
vault login $ROOT_TOKEN

# Configure PostgreSQL connection
vault write database/config/core-db \
    plugin_name=postgresql-database-plugin \
    allowed_roles="backend,keycloak,grafana" \
    connection_url="postgresql://{{username}}:{{password}}@db:5432/core?sslmode=disable" \
    username="postgres" \
    password="$POSTGRES_ADMIN_PASSWORD"

echo "✅ Database connection configured"
```

### 2. Create Dynamic Role for Backend

```bash
# Create role with 24h TTL
vault write database/roles/backend \
    db_name=core-db \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
        GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

echo "✅ Backend role created"
```

### 3. Add Spring Cloud Vault Dependency

**File:** `backend/pom.xml`

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-vault-config</artifactId>
</dependency>
```

### 4. Configure Spring Boot

**File:** `backend/src/main/resources/bootstrap.yml`

```yaml
spring:
  application:
    name: core-backend
  
  cloud:
    vault:
      uri: http://vault:8200
      authentication: TOKEN
      token: ${VAULT_TOKEN}  # From env var
      
      # Database secrets (dynamic)
      database:
        enabled: true
        role: backend
        backend: database
        
        # Auto-renew credentials
        renewal:
          interval: 30m
          renew-threshold: 80

# Datasource uses Vault-provided credentials
  datasource:
    url: ${DATABASE_URL}
    username: ${spring.cloud.vault.database.username}
    password: ${spring.cloud.vault.database.password}
```

### 5. Update docker-compose.yml

```yaml
services:
  backend:
    environment:
      VAULT_TOKEN: ${VAULT_BACKEND_TOKEN}  # Service-specific token
    depends_on:
      - vault
      - db
```

### 6. Test Dynamic Credentials

```bash
# Generate credentials
vault read database/creds/backend
# Output:
# Key                Value
# ---                -----
# lease_id           database/creds/backend/abc123
# lease_duration     1h
# username           v-backend-abc123def
# password           random-generated-password

# Test connection
psql -U v-backend-abc123def -d core -c "SELECT 1;"

# Check expiration
psql -U postgres -d core -c "SELECT usename, valuntil FROM pg_user WHERE usename LIKE 'v-backend%';"
```

---

## ✅ Acceptance Criteria

- [ ] Vault database secrets engine configured
- [ ] Dynamic role created for backend
- [ ] Spring Boot fetches credentials from Vault
- [ ] Backend connects to PostgreSQL successfully
- [ ] Credentials auto-renewed every 30 minutes
- [ ] Old credentials revoked after expiration

---

## 🔗 Dependencies

- Requires T1 (Vault deployed)
- Requires PostgreSQL admin credentials
- Blocks INF-003 (replaces Docker Secrets)
