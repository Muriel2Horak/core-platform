# N8N1: n8n Platform Deployment - Docker & PostgreSQL

**Typ:** TASK  
**Epic:** EPIC-011 (n8n External Orchestration Layer)  
**Fase:** Phase 3 (n8n Deployment)  
**Priorita:** HIGH (foundation)  
**Effort:** 400 LOC, 1 den  
**Dependencies:** PostgreSQL infrastructure  
**Status:** ⏳ TODO

---

## 🎯 Cíl

Deploy **n8n workflow automation platform** jako Docker service s:
- PostgreSQL backend (persistence)
- Environment variables configuration
- Volume mounts (workflows, credentials)
- Network integration (core-network)
- Health checks

---

## 📋 Požadavky

### Funkční Požadavky

1. **Docker Service**
   - Image: `n8nio/n8n:latest`
   - PostgreSQL backend (ne SQLite)
   - Persistent volumes
   - Internal port 5678

2. **Database**
   - Separátní PostgreSQL database: `n8n`
   - User: `n8n_app` (separate from `core`)
   - Auto-init schema (n8n migrations)

3. **Configuration**
   - Environment variables (`.env`)
   - Webhook URL: `https://admin.core-platform.local/n8n/webhook`
   - Basic auth DISABLED (SSO via Keycloak)

4. **Health Check**
   - Endpoint: `GET /healthz`
   - Interval: 30s
   - Retries: 3

---

## 🔧 Implementace

### 1. Docker Compose Service

**File:** `docker/docker-compose.yml` (add n8n service)

```yaml
services:
  n8n:
    image: n8nio/n8n:latest
    container_name: core-n8n
    restart: unless-stopped
    networks:
      - core-network
    ports:
      - "5678"  # Internal only, NOT published to host
    environment:
      # Database
      - DB_TYPE=postgresdb
      - DB_POSTGRESDB_DATABASE=${N8N_DB_NAME:-n8n}
      - DB_POSTGRESDB_HOST=core-db
      - DB_POSTGRESDB_PORT=5432
      - DB_POSTGRESDB_USER=${N8N_DB_USERNAME:-n8n_app}
      - DB_POSTGRESDB_PASSWORD=${N8N_DB_PASSWORD}
      
      # URLs
      - N8N_HOST=admin.${DOMAIN}
      - N8N_PORT=5678
      - N8N_PROTOCOL=https
      - WEBHOOK_URL=https://admin.${DOMAIN}/n8n/webhook
      
      # Auth (disabled, will use Keycloak SSO)
      - N8N_BASIC_AUTH_ACTIVE=false
      
      # Paths
      - N8N_USER_FOLDER=/home/node/.n8n
      
      # Execution
      - EXECUTIONS_PROCESS=main
      - EXECUTIONS_DATA_SAVE_ON_ERROR=all
      - EXECUTIONS_DATA_SAVE_ON_SUCCESS=all
      - EXECUTIONS_DATA_MAX_AGE=168  # 7 days
      
      # Timezone
      - GENERIC_TIMEZONE=Europe/Prague
      
    volumes:
      - n8n-data:/home/node/.n8n
    depends_on:
      - db
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:5678/healthz"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s
    labels:
      - "com.core-platform.service=n8n"
      - "com.core-platform.description=Workflow Automation Platform"

volumes:
  n8n-data:
    driver: local
```

---

### 2. Environment Variables

**File:** `.env.template` (add n8n variables)

```bash
# 🔄 N8N CONFIGURATION
N8N_DB_NAME=n8n
N8N_DB_USERNAME=n8n_app
N8N_DB_PASSWORD=n8n_secure_password_change_in_prod
```

---

### 3. PostgreSQL Database Init

**File:** `docker/postgres/init-n8n-db.sql` (create n8n database)

```sql
-- Create n8n database and user
CREATE DATABASE n8n;

-- Create separate user for n8n
CREATE USER n8n_app WITH ENCRYPTED PASSWORD 'n8n_secure_password_change_in_prod';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE n8n TO n8n_app;

-- Connect to n8n database
\c n8n

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO n8n_app;

-- Grant default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO n8n_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO n8n_app;

COMMENT ON DATABASE n8n IS 'n8n workflow automation platform database';
```

**Update:** `docker/postgres/init-multi-db.sh` (add n8n to init)

```bash
#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Core application database
    CREATE DATABASE core;
    COMMENT ON DATABASE core IS 'Main application database';
    
    -- Keycloak authentication database
    CREATE DATABASE keycloak;
    COMMENT ON DATABASE keycloak IS 'Keycloak SSO database';
    
    -- Grafana dashboards database
    CREATE DATABASE grafana;
    COMMENT ON DATABASE grafana IS 'Grafana monitoring database';
    
    -- n8n workflow automation database
    CREATE DATABASE n8n;
    COMMENT ON DATABASE n8n IS 'n8n workflow automation platform database';
    
    -- Create separate users (security best practice)
    CREATE USER n8n_app WITH ENCRYPTED PASSWORD '${N8N_DB_PASSWORD:-n8n_secure_password}';
    GRANT ALL PRIVILEGES ON DATABASE n8n TO n8n_app;
EOSQL

# Grant schema privileges for n8n
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "n8n" <<-EOSQL
    GRANT ALL ON SCHEMA public TO n8n_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO n8n_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO n8n_app;
EOSQL
```

---

### 4. Makefile Integration

**File:** `Makefile` (add n8n targets)

```makefile
# N8N deployment targets

.PHONY: n8n-up
n8n-up: ## Start n8n service
	@echo "🚀 Starting n8n..."
	docker compose -f docker/docker-compose.yml up -d n8n
	@echo "✅ n8n started: https://admin.${DOMAIN}/n8n/"

.PHONY: n8n-down
n8n-down: ## Stop n8n service
	docker compose -f docker/docker-compose.yml stop n8n

.PHONY: n8n-restart
n8n-restart: ## Restart n8n service
	docker compose -f docker/docker-compose.yml restart n8n

.PHONY: n8n-logs
n8n-logs: ## Show n8n logs
	docker compose -f docker/docker-compose.yml logs -f n8n

.PHONY: n8n-shell
n8n-shell: ## Open n8n container shell
	docker exec -it core-n8n sh

.PHONY: n8n-db-backup
n8n-db-backup: ## Backup n8n database
	@echo "💾 Backing up n8n database..."
	docker exec core-db pg_dump -U ${N8N_DB_USERNAME} n8n > backups/n8n-backup-$(shell date +%Y%m%d-%H%M%S).sql
	@echo "✅ Backup saved to backups/"

.PHONY: n8n-reset
n8n-reset: ## Reset n8n database (⚠️ DESTRUCTIVE)
	@echo "⚠️  WARNING: This will DELETE all n8n workflows and credentials!"
	@read -p "Type 'yes' to confirm: " confirm && [ "$$confirm" = "yes" ] || exit 1
	docker exec core-db psql -U postgres -c "DROP DATABASE n8n; CREATE DATABASE n8n;"
	docker compose -f docker/docker-compose.yml restart n8n
	@echo "✅ n8n database reset"
```

---

### 5. Health Check Script

**File:** `scripts/n8n-health-check.sh`

```bash
#!/bin/bash
set -e

N8N_URL="${N8N_URL:-https://admin.core-platform.local/n8n}"

echo "🏥 Checking n8n health..."

# Check n8n API
HTTP_CODE=$(curl -k -s -o /dev/null -w "%{http_code}" "${N8N_URL}/healthz")

if [ "$HTTP_CODE" -eq 200 ]; then
    echo "✅ n8n is healthy (HTTP $HTTP_CODE)"
    exit 0
else
    echo "❌ n8n is unhealthy (HTTP $HTTP_CODE)"
    exit 1
fi
```

---

### 6. Documentation

**File:** `docs/n8n/README.md`

```markdown
# n8n Workflow Automation Platform

## Access

- **URL:** https://admin.core-platform.local/n8n/
- **Auth:** Keycloak SSO (see N8N2-keycloak-sso)

## Architecture

```
Browser → Nginx (reverse proxy) → n8n:5678 → PostgreSQL (n8n database)
```

## Database

- **Database:** `n8n`
- **User:** `n8n_app`
- **Schema:** Auto-migrated by n8n on startup
- **Tables:** `execution_entity`, `workflow_entity`, `credentials_entity`, ...

## Workflows Storage

- **Location:** Docker volume `n8n-data`
- **Path:** `/home/node/.n8n/`
- **Contents:**
  - `workflows/` - Exported workflow JSONs
  - `credentials/` - Encrypted credentials
  - `nodes/` - Custom nodes (if any)

## Backup & Restore

```bash
# Backup database
make n8n-db-backup

# Backup workflows (export from UI)
# Settings → Workflows → Export All

# Restore database
docker exec -i core-db psql -U n8n_app n8n < backups/n8n-backup-20251108.sql

# Restore workflows (import in UI)
# Settings → Workflows → Import
```

## Troubleshooting

### n8n not starting

```bash
# Check logs
make n8n-logs

# Common issues:
# 1. Database connection failed → check N8N_DB_PASSWORD
# 2. Port conflict → check docker ps | grep 5678
# 3. Volume permissions → chown -R 1000:1000 /path/to/n8n-data
```

### Reset n8n (destructive)

```bash
make n8n-reset
```
```

---

## ✅ Acceptance Criteria

1. **Deployment:**
   - [ ] n8n Docker service running
   - [ ] PostgreSQL backend connected
   - [ ] Persistent volumes mounted
   - [ ] Health check passing

2. **Configuration:**
   - [ ] Environment variables configured
   - [ ] Database `n8n` created
   - [ ] User `n8n_app` with privileges

3. **Integration:**
   - [ ] Part of core-network
   - [ ] Makefile targets (`n8n-up`, `n8n-logs`)

4. **Testy:**
   - [ ] Health check script passes
   - [ ] Database connection verified
   - [ ] Workflows persist across restarts

---

**Related Stories:**
- N8N2: Keycloak SSO (authentication)
- N8N3: Nginx Proxy (reverse proxy)
- N8N6: BFF API (integration bridge)
