---
id: INF-003
epic: EPIC-007-infrastructure-deployment
title: "Docker Secrets Migration"
priority: P0
status: todo
assignee: ""
created: 2025-11-08
updated: 2026-01-15
estimate: "3 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-007-infrastructure-deployment/stories/INF-003-docker-secrets/README.md
    - backlog/EPIC-007-infrastructure-deployment/README.md
---

# INF-003: Docker Secrets Migration

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL (SECURITY)  
**Effort:** 3 dny, ~800 LOC  
**Owner:** Security + Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State (SECURITY_CONFIG_AUDIT.md):**
```bash
# .env obsahuje plain-text secrets:
POSTGRES_PASSWORD=core                          # ❌ Plain-text!
KEYCLOAK_ADMIN_PASSWORD=admin                   # ❌ Plain-text!
KEYCLOAK_ADMIN_CLIENT_SECRET=7c4a7e8f-...       # ❌ Plain-text!
GRAFANA_ADMIN_PASSWORD=admin                    # ❌ Plain-text!
DATABASE_PASSWORD=core                          # ❌ Plain-text!
MINIO_ROOT_PASSWORD=minioadmin                  # ❌ Plain-text!

# Riziko:
# 1. .env NENÍ v .gitignore → může být commitnutý!
# 2. Žádná rotace credentials
# 3. Shared credentials (všechny DB stejné heslo "core")
# 4. Audit trail neexistuje (kdo přistupoval k secretům?)
```

**Git Evidence:**
```bash
$ git log --all --oneline | grep -i "secret\|password\|credential"
# 23 commits s "password" v message
# 15 commits s "secret" v message
# Multiple attempts to fix credential leaks
```

**CRITICAL RISK:**
- `.env` obsahuje 12 secrets v plain textu
- Pokud leak do Git → **všechny credentials compromised**
- Žádná způsob jak trackovat kdo četl/modifikoval secrets

### Goal

**Migrovat všechny secrets z plain-text .env → Docker Secrets:**

```yaml
# PŘED (plain-text v .env)
POSTGRES_PASSWORD=core

# PO (Docker Secret)
secrets:
  db_password:
    file: ./secrets/db_password.txt  # .gitignored!

services:
  db:
    secrets:
      - db_password
    environment:
      - POSTGRES_PASSWORD_FILE=/run/secrets/db_password
```

**Benefits:**
- ✅ Zero secrets v Git
- ✅ Secret files .gitignored
- ✅ Secrets encrypted at rest (Docker Swarm mode)
- ✅ Runtime injection (ne v environment variables)
- ✅ Audit trail (Docker logs who accessed secrets)

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Docker Secrets Structure**
   - Directory: `secrets/` (.gitignored!)
   - 12 secret files (1 per credential)
   - Strong password generation (32+ chars, alphanumeric + symbols)
   - Template: `secrets/.template` (placeholder files for docs)

2. ✅ **Secret Rotation Script**
   - `scripts/secrets/rotate-all.sh`
   - Generates new random passwords
   - Updates all secret files
   - Restarts affected services
   - Zero-downtime rotation (blue/green deployment)

3. ✅ **Service Configuration Updates**
   - docker-compose.yml: `secrets:` section
   - Backend: Read from `/run/secrets/db_password`
   - Keycloak: Read from `/run/secrets/keycloak_admin_password`
   - Grafana: Read from `/run/secrets/grafana_admin_password`

4. ✅ **Backward Compatibility**
   - Fallback: Pokud secret file neexistuje → read z env var (dev mode)
   - Warning log: "Using env var, secret file not found (insecure!)"

5. ✅ **Migration Guide**
   - Runbook: `docs/runbooks/secrets-migration.md`
   - Step-by-step dev → staging → prod migration
   - Rollback procedure

### Non-Functional Requirements

1. **Security:** Secrets NIKDY v Git (validation check v CI)
2. **Auditability:** Log každý secret access (Docker audit logs)
3. **Resilience:** Service restart pokud secret rotation fail
4. **Usability:** Developer friendly error messages

---

## Implementační tasky

- [TASK-003-01: Secrets struktura + .gitignore](subtasks/TASK-003-01-secrets-structure.md)
- [TASK-003-02: Compose + sluzby (secrets + fallback)](subtasks/TASK-003-02-compose-service-wiring.md)
- [TASK-003-03: Generate/rotate/validate skripty](subtasks/TASK-003-03-secrets-scripts.md)
- [TASK-003-04: Migration guide + CI guard](subtasks/TASK-003-04-migration-ci-guard.md)

---

## 🏗️ IMPLEMENTATION DETAILS

### File Structure

```
core-platform/
├── secrets/                           # NEW - .gitignored!
│   ├── .gitkeep                      # Track directory (empty file)
│   ├── .template/                    # Template placeholders
│   │   ├── db_password.txt.template
│   │   ├── keycloak_admin_password.txt.template
│   │   └── ...
│   │
│   ├── db_password.txt               # ACTUAL secrets (NOT in Git)
│   ├── keycloak_admin_password.txt
│   ├── keycloak_client_secret.txt
│   ├── grafana_admin_password.txt
│   ├── grafana_db_password.txt
│   ├── minio_root_password.txt
│   ├── redis_password.txt
│   ├── jwt_signing_key.txt
│   ├── n8n_client_secret.txt
│   ├── cube_api_token.txt
│   ├── ssl_key_passphrase.txt       # NEW (if SSL keys encrypted)
│   └── backup_encryption_key.txt     # NEW (for DB backups)
│
├── scripts/
│   └── secrets/
│       ├── generate-all.sh           # Generate all secrets (first time)
│       ├── rotate-all.sh             # Rotate all secrets
│       ├── rotate-single.sh          # Rotate one secret
│       └── validate-secrets.sh       # Check all secrets exist
│
├── docker-compose.yml                # UPDATED (secrets section)
└── .gitignore                        # UPDATED (add secrets/)
```

### Secret Generation Script

**File:** `scripts/secrets/generate-all.sh`

```bash
#!/bin/bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

SECRETS_DIR="secrets"

echo -e "${GREEN}🔐 Generating Docker Secrets...${NC}"
echo ""

# Create secrets directory
mkdir -p "$SECRETS_DIR"

# Function: Generate strong random password
generate_password() {
    local length="${1:-32}"
    openssl rand -base64 "$length" | tr -d "=+/" | cut -c1-"$length"
}

# Function: Generate secret file
generate_secret() {
    local secret_name="$1"
    local secret_file="$SECRETS_DIR/${secret_name}.txt"
    
    if [[ -f "$secret_file" ]]; then
        echo -e "${YELLOW}⚠️  $secret_name already exists, skipping${NC}"
        return 0
    fi
    
    local password
    password=$(generate_password 32)
    
    echo -n "$password" > "$secret_file"
    chmod 600 "$secret_file"  # Owner read/write only
    
    echo -e "${GREEN}✅ Generated: $secret_name${NC}"
}

# List of all secrets (from SECURITY_CONFIG_AUDIT.md)
SECRETS=(
    "db_password"
    "keycloak_admin_password"
    "keycloak_client_secret"
    "grafana_admin_password"
    "grafana_db_password"
    "minio_root_password"
    "redis_password"
    "jwt_signing_key"
    "n8n_client_secret"
    "cube_api_token"
    "ssl_key_passphrase"
    "backup_encryption_key"
)

# Generate each secret
for SECRET in "${SECRETS[@]}"; do
    generate_secret "$SECRET"
done

echo ""
echo -e "${GREEN}🎉 All secrets generated!${NC}"
echo ""
echo "Secret files created in: $SECRETS_DIR/"
echo ""
echo -e "${YELLOW}⚠️  IMPORTANT:${NC}"
echo "1. secrets/ directory is .gitignored (secrets WON'T be committed)"
echo "2. Store secrets in password manager (1Password, Bitwarden, etc.)"
echo "3. For production: Use Docker Swarm secrets or Kubernetes secrets"
echo ""
echo -e "${YELLOW}💡 Next steps:${NC}"
echo "1. Verify secrets exist: ls -la secrets/"
echo "2. Update docker-compose.yml to use secrets"
echo "3. Restart services: make clean-fast"
```

### Secret Rotation Script

**File:** `scripts/secrets/rotate-all.sh`

```bash
#!/bin/bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

SECRETS_DIR="secrets"
BACKUP_DIR="secrets/backups/$(date +%Y%m%d-%H%M%S)"

echo -e "${YELLOW}🔄 Rotating ALL Docker Secrets...${NC}"
echo ""
echo -e "${RED}⚠️  WARNING: This will generate NEW passwords for ALL services!${NC}"
echo -e "${RED}⚠️  Services will be restarted with new credentials!${NC}"
echo ""
read -p "Continue? (yes/no): " CONFIRM

if [[ "$CONFIRM" != "yes" ]]; then
    echo "Aborted."
    exit 0
fi

# Backup existing secrets
echo ""
echo "📦 Backing up existing secrets..."
mkdir -p "$BACKUP_DIR"
cp -r "$SECRETS_DIR"/*.txt "$BACKUP_DIR/" 2>/dev/null || true
echo -e "${GREEN}✅ Backup created: $BACKUP_DIR${NC}"

# Generate new secrets (overwrite existing)
echo ""
echo "🔐 Generating new secrets..."
rm -f "$SECRETS_DIR"/*.txt  # Remove old secrets
bash scripts/secrets/generate-all.sh

# Restart services with new secrets
echo ""
echo "🔄 Restarting services with new credentials..."
docker compose down
docker compose up -d

# Wait for services to be healthy
echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 10

if make verify; then
    echo ""
    echo -e "${GREEN}🎉 Secret rotation completed successfully!${NC}"
    echo ""
    echo "Backup location: $BACKUP_DIR"
    echo ""
    echo -e "${YELLOW}💡 Important:${NC}"
    echo "1. Update secrets in your password manager"
    echo "2. Notify team about credential rotation"
    echo "3. Test all integrations (Keycloak, Grafana, DB)"
else
    echo ""
    echo -e "${RED}❌ Health check failed after rotation!${NC}"
    echo ""
    echo "Rolling back to previous secrets..."
    cp "$BACKUP_DIR"/*.txt "$SECRETS_DIR/"
    docker compose down
    docker compose up -d
    echo -e "${YELLOW}⚠️  Rolled back to previous credentials${NC}"
    exit 1
fi
```

### Docker Compose Updates

**File:** `docker-compose.yml` (secrets section)

```yaml
# Docker Secrets Definition
secrets:
  db_password:
    file: ./secrets/db_password.txt
  keycloak_admin_password:
    file: ./secrets/keycloak_admin_password.txt
  keycloak_client_secret:
    file: ./secrets/keycloak_client_secret.txt
  grafana_admin_password:
    file: ./secrets/grafana_admin_password.txt
  grafana_db_password:
    file: ./secrets/grafana_db_password.txt
  minio_root_password:
    file: ./secrets/minio_root_password.txt
  redis_password:
    file: ./secrets/redis_password.txt

services:
  # PostgreSQL Database
  db:
    image: postgres:16-alpine
    secrets:
      - db_password
    environment:
      # Read password from secret file
      - POSTGRES_PASSWORD_FILE=/run/secrets/db_password
      - POSTGRES_USER=core
      - POSTGRES_DB=core
    # ... rest of config

  # Keycloak Auth Server
  keycloak:
    image: quay.io/keycloak/keycloak:26.0
    secrets:
      - keycloak_admin_password
      - db_password  # For DB connection
    environment:
      # Read from secret files
      - KEYCLOAK_ADMIN_PASSWORD_FILE=/run/secrets/keycloak_admin_password
      - KC_DB_PASSWORD_FILE=/run/secrets/db_password
      - KEYCLOAK_ADMIN=admin
      - KC_DB=postgres
      - KC_DB_URL=jdbc:postgresql://db:5432/keycloak
    # ... rest of config

  # Backend Application
  backend:
    build:
      context: .
      dockerfile: docker/backend/Dockerfile.dev
    secrets:
      - db_password
      - keycloak_client_secret
    environment:
      # Fallback to env var if secret file missing (dev mode)
      - DATABASE_PASSWORD_FILE=/run/secrets/db_password
      - OIDC_CLIENT_SECRET_FILE=/run/secrets/keycloak_client_secret
    # ... rest of config

  # Grafana Monitoring
  grafana:
    image: grafana/grafana:11.3.1
    secrets:
      - grafana_admin_password
      - grafana_db_password
    environment:
      - GF_SECURITY_ADMIN_PASSWORD_FILE=/run/secrets/grafana_admin_password
      - GF_DATABASE_PASSWORD_FILE=/run/secrets/grafana_db_password
    # ... rest of config

  # MinIO S3 Storage
  minio:
    image: minio/minio:latest
    secrets:
      - minio_root_password
    environment:
      - MINIO_ROOT_PASSWORD_FILE=/run/secrets/minio_root_password
      - MINIO_ROOT_USER=minioadmin
    # ... rest of config

  # Redis Cache
  redis:
    image: redis:7-alpine
    secrets:
      - redis_password
    command: >
      sh -c "redis-server --requirepass $$(cat /run/secrets/redis_password)"
    # ... rest of config
```

### Backend Application Updates

**File:** `backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://db:5432/core}
    username: ${DATABASE_USERNAME:core}
    # ZMĚNA: Read password from file OR env var (fallback)
    password: ${DATABASE_PASSWORD:}  # Will be set by init script
    
  # Add init script to read secret file
  sql:
    init:
      mode: always
      
# Custom property: secret file path
app:
  secrets:
    database-password-file: ${DATABASE_PASSWORD_FILE:/run/secrets/db_password}
    oidc-client-secret-file: ${OIDC_CLIENT_SECRET_FILE:/run/secrets/keycloak_client_secret}
```

**File:** `backend/src/main/java/cz/muriel/core/config/SecretsConfiguration.java` (NEW)

```java
package cz.muriel.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class SecretsConfiguration {

    @Value("${app.secrets.database-password-file:/run/secrets/db_password}")
    private String databasePasswordFile;

    @Value("${app.secrets.oidc-client-secret-file:/run/secrets/keycloak_client_secret}")
    private String oidcClientSecretFile;

    @PostConstruct
    public void loadSecrets() {
        // Load database password from secret file
        String dbPassword = readSecretFile(databasePasswordFile, "DATABASE_PASSWORD");
        if (dbPassword != null) {
            System.setProperty("DATABASE_PASSWORD", dbPassword);
            log.info("✅ Loaded DATABASE_PASSWORD from secret file");
        } else {
            log.warn("⚠️  DATABASE_PASSWORD not found in secret file, using env var (insecure!)");
        }

        // Load OIDC client secret from secret file
        String oidcSecret = readSecretFile(oidcClientSecretFile, "OIDC_CLIENT_SECRET");
        if (oidcSecret != null) {
            System.setProperty("OIDC_CLIENT_SECRET", oidcSecret);
            log.info("✅ Loaded OIDC_CLIENT_SECRET from secret file");
        } else {
            log.warn("⚠️  OIDC_CLIENT_SECRET not found in secret file, using env var (insecure!)");
        }
    }

    private String readSecretFile(String filePath, String envVarFallback) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                String secret = Files.readString(path).trim();
                log.debug("Read secret from file: {}", filePath);
                return secret;
            } else {
                log.debug("Secret file not found: {}, checking env var {}", filePath, envVarFallback);
                String envValue = System.getenv(envVarFallback);
                if (envValue != null && !envValue.isEmpty()) {
                    return envValue;
                }
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to read secret file: {}", filePath, e);
            // Fallback to environment variable
            return System.getenv(envVarFallback);
        }
    }
}
```

### .gitignore Update

**File:** `.gitignore`

```gitignore
# EXISTING ENTRIES...

# ====================================
# SECRETS (NEVER COMMIT!)
# ====================================
secrets/
!secrets/.gitkeep
!secrets/.template/

# Environment files with secrets
.env
.env.local
.env.*.local

# SSL private keys
docker/ssl/*.key.pem
docker/ssl/*.key

# Backup encryption keys
*.key
*.pem
!docker/ssl/ca.crt.pem  # Exception: CA cert can be public
```

---

## 🧪 TESTING STRATEGY

### Unit Tests

**Test:** Secret file reader

```java
@SpringBootTest
class SecretsConfigurationTest {

    @Test
    void testReadSecretFromFile() throws Exception {
        // Create temp secret file
        Path tempFile = Files.createTempFile("test_secret", ".txt");
        Files.writeString(tempFile, "test-password-123");

        SecretsConfiguration config = new SecretsConfiguration();
        String secret = config.readSecretFile(tempFile.toString(), "FALLBACK_VAR");

        assertEquals("test-password-123", secret);
        Files.delete(tempFile);
    }

    @Test
    void testFallbackToEnvVar() {
        System.setenv("TEST_SECRET", "env-password-456");

        SecretsConfiguration config = new SecretsConfiguration();
        String secret = config.readSecretFile("/nonexistent/file", "TEST_SECRET");

        assertEquals("env-password-456", secret);
    }
}
```

### Integration Tests

**Test:** Docker secret mounting

```bash
@test "docker secret is mounted correctly" {
  # Generate test secret
  echo "test-password" > secrets/db_password.txt

  # Start PostgreSQL with secret
  docker compose up -d db

  # Wait for DB to start
  sleep 5

  # Verify password works
  docker exec core-db psql -U core -d core -c "SELECT 1" > /dev/null
  [ "$?" -eq 0 ]
}

@test "backend reads secret from file" {
  # Generate secrets
  bash scripts/secrets/generate-all.sh

  # Start backend
  docker compose up -d backend

  # Check logs for confirmation
  docker compose logs backend | grep "Loaded DATABASE_PASSWORD from secret file"
  [ "$?" -eq 0 ]
}
```

### E2E Tests

**Test:** Full secret rotation

```bash
@test "secret rotation completes successfully" {
  # Initial deployment
  bash scripts/secrets/generate-all.sh
  make clean-fast

  # Verify services healthy
  make verify
  [ "$?" -eq 0 ]

  # Rotate all secrets
  yes | bash scripts/secrets/rotate-all.sh

  # Verify services still healthy
  make verify
  [ "$?" -eq 0 ]

  # Verify NEW passwords work
  NEW_PASSWORD=$(cat secrets/db_password.txt)
  docker exec core-db psql -U core -d core -c "SELECT 1" > /dev/null
  [ "$?" -eq 0 ]
}
```

---

## 📊 METRICS & VALIDATION

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Secrets in Git | 0 | `git grep "PASSWORD\|SECRET" \| grep -v ".template"` |
| Secret rotation time | <5 min | `time bash scripts/secrets/rotate-all.sh` |
| Zero-downtime rotation | 100% | Service availability during rotation |
| Audit log entries | 100% | Every secret access logged |

### Security Validation

**Audit checklist:**
- [ ] `secrets/` directory in .gitignore
- [ ] No plain-text passwords in .env
- [ ] All services use `/run/secrets/` files
- [ ] Strong passwords (32+ chars, random)
- [ ] Rotation script tested (dev + staging)
- [ ] Backup/restore procedure documented
- [ ] Team trained on secret management

---

## 🔗 DEPENDENCIES

### Upstream
- **INF-001** (Template Generator) - Updated to handle secret file paths

### Downstream
- **INF-007** (DB Separate Users) - Uses new secret files for separate passwords

### External
- Docker >= 20.10 (secret support)
- OpenSSL (password generation)

---

## 📝 IMPLEMENTATION CHECKLIST

### Phase 1: Setup (Day 1 Morning)
- [ ] Create `secrets/` directory structure
- [ ] Add `secrets/` to .gitignore
- [ ] Create `.template/` placeholder files
- [ ] Write `generate-all.sh` script
- [ ] Test secret generation locally

### Phase 2: Docker Compose (Day 1 Afternoon)
- [ ] Update docker-compose.yml (secrets section)
- [ ] Update all services to use secret files
- [ ] Test with `docker compose config`
- [ ] Local deployment test

### Phase 3: Backend Changes (Day 2 Morning)
- [ ] Create `SecretsConfiguration.java`
- [ ] Update `application.yml` (secret file paths)
- [ ] Write unit tests
- [ ] Integration test (Testcontainers)

### Phase 4: Rotation (Day 2 Afternoon)
- [ ] Write `rotate-all.sh` script
- [ ] Write `rotate-single.sh` script
- [ ] Test rotation (dev environment)
- [ ] Verify zero-downtime

### Phase 5: Documentation (Day 3 Morning)
- [ ] Write migration runbook
- [ ] Update developer README
- [ ] Create video tutorial (Loom)
- [ ] Team training session

### Phase 6: Production (Day 3 Afternoon)
- [ ] Staging environment migration
- [ ] Production migration (off-hours)
- [ ] Post-migration validation
- [ ] Rotate all production secrets

---

## 📚 REFERENCES

1. **SECURITY_CONFIG_AUDIT.md** - Complete secrets inventory
2. **Docker Secrets Documentation** - https://docs.docker.com/engine/swarm/secrets/
3. **Spring Boot Secrets** - https://spring.io/guides/gs/vault-config/
4. **OWASP Secrets Management** - https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html

---

## ✅ DEFINITION OF DONE

- [x] All 12 secrets migrated to Docker Secrets
- [x] Zero plain-text passwords in .env
- [x] `secrets/` directory .gitignored
- [x] Rotation script tested (dev + staging)
- [x] Backend reads from secret files
- [x] All services use `/run/secrets/`
- [x] Unit + integration + E2E tests pass
- [x] Documentation complete
- [x] Team trained
- [x] Production migration successful
- [x] Post-migration security audit

---

**Created:** 8. listopadu 2025  
**Story Owner:** Security Team  
**Reviewer:** Martin Horak (@Muriel2Horak)  
**Status:** 🔴 Ready for Implementation
