# S1: n8n Platform Deployment

> **Foundation:** Deploy n8n Community Edition with PostgreSQL backend and webhook support

## 📋 Story

**As a** platform administrator  
**I want** n8n workflow automation platform deployed with persistent storage  
**So that** users can create and execute automated workflows

## 🎯 Acceptance Criteria

**GIVEN** Docker Compose is configured  
**WHEN** running `docker compose up -d n8n`  
**THEN** n8n is accessible at http://n8n:5678 (internal network)  
**AND** PostgreSQL stores workflow definitions and execution history  
**AND** webhooks are functional at /webhook/* endpoints

## 🏗️ Implementation

### 1. Docker Compose Configuration

```yaml
# docker/docker-compose.yml (add n8n service)

services:
  # ... existing services ...

  n8n:
    image: n8nio/n8n:1.15.0
    container_name: core-n8n
    restart: unless-stopped
    
    environment:
      # Database
      - DB_TYPE=postgresdb
      - DB_POSTGRESDB_HOST=core-db
      - DB_POSTGRESDB_PORT=5432
      - DB_POSTGRESDB_DATABASE=n8n
      - DB_POSTGRESDB_USER=${N8N_DB_USER:-n8n_user}
      - DB_POSTGRESDB_PASSWORD=${N8N_DB_PASSWORD:-n8n_password}
      
      # n8n Configuration
      - N8N_BASIC_AUTH_ACTIVE=false  # Disabled, using external SSO
      - N8N_HOST=${N8N_HOST:-admin.core-platform.local}
      - N8N_PORT=5678
      - N8N_PROTOCOL=https
      - N8N_EDITOR_BASE_URL=https://admin.core-platform.local/n8n
      - WEBHOOK_URL=https://admin.core-platform.local/n8n
      
      # Execution Settings
      - EXECUTIONS_DATA_SAVE_ON_ERROR=all
      - EXECUTIONS_DATA_SAVE_ON_SUCCESS=all
      - EXECUTIONS_DATA_SAVE_MANUAL_EXECUTIONS=true
      - EXECUTIONS_DATA_PRUNE=true
      - EXECUTIONS_DATA_MAX_AGE=30  # 30 days retention
      
      # Timezone
      - GENERIC_TIMEZONE=Europe/Prague
      - TZ=Europe/Prague
    
    volumes:
      # Workflow files (optional, DB is primary storage)
      - n8n-data:/home/node/.n8n
      
      # Custom nodes (if needed)
      - ./n8n/custom-nodes:/home/node/.n8n/custom
    
    ports:
      - "5678:5678"  # Exposed for internal access only
    
    networks:
      - core-net
    
    depends_on:
      - db
    
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:5678/healthz"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    
    labels:
      - "logging=loki"

volumes:
  # ... existing volumes ...
  n8n-data:
    driver: local
```

### 2. PostgreSQL Database Setup

```sql
-- docker/postgres/init-n8n-db.sql

-- Create n8n database
CREATE DATABASE n8n OWNER core;

-- Create n8n user (if separate user desired)
CREATE USER n8n_user WITH PASSWORD 'n8n_password';
GRANT ALL PRIVILEGES ON DATABASE n8n TO n8n_user;

-- Connect to n8n database
\c n8n

-- Grant schema permissions
GRANT ALL ON SCHEMA public TO n8n_user;
GRANT ALL ON ALL TABLES IN SCHEMA public TO n8n_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO n8n_user;
```

### 3. Environment Variables

```bash
# .env.template (add n8n variables)

# n8n Configuration
N8N_HOST=admin.core-platform.local
N8N_DB_USER=n8n_user
N8N_DB_PASSWORD=n8n_password  # Change in production!
N8N_ENCRYPTION_KEY=<generate-with-openssl-rand-base64-32>
```

### 4. Webhook Configuration

n8n webhooks will be accessible at:
- **Production**: `https://admin.core-platform.local/n8n/webhook/<webhook-id>`
- **Test**: `https://admin.core-platform.local/n8n/webhook-test/<webhook-id>`

**Important**: Webhooks must be publicly accessible (no authentication) for external integrations.

## ✅ Testing

### Smoke Tests

```bash
# 1. Start n8n
docker compose up -d n8n

# 2. Wait for initialization
sleep 30

# 3. Health check
curl http://localhost:5678/healthz
# Expected: {"status":"ok"}

# 4. Check database connection
docker exec core-n8n n8n --version
# Expected: n8n version 1.15.0

# 5. Test webhook endpoint (after Nginx setup)
curl https://admin.core-platform.local/n8n/webhook/test
# Expected: 404 (webhook doesn't exist yet, but endpoint is reachable)
```

### Database Verification

```bash
# Connect to PostgreSQL
docker exec -it core-db psql -U n8n_user -d n8n

# Check n8n tables (created on first run)
\dt
# Expected tables: workflow_entity, execution_entity, credentials_entity, etc.

# Exit
\q
```

## 📊 Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Startup Time** | <60s | Container healthy within 60s |
| **Health Check** | 200 OK | /healthz returns success |
| **Database Connection** | Connected | n8n writes to PostgreSQL |
| **Webhook Availability** | 99.9% | Webhook endpoints respond |
| **Execution Retention** | 30 days | Old executions pruned automatically |

## 📁 File Structure

```
docker/
├── docker-compose.yml          # n8n service definition
├── postgres/
│   └── init-n8n-db.sql        # n8n database creation
└── n8n/
    └── custom-nodes/          # (Optional) Custom n8n nodes

.env.template                  # n8n environment variables
```

## 🎯 Acceptance Checklist

- [x] n8n Docker service defined in `docker-compose.yml`
- [x] PostgreSQL database `n8n` created with proper permissions
- [x] Environment variables configured in `.env.template`
- [x] n8n accessible at http://localhost:5678 (internal)
- [x] Health check endpoint /healthz returns 200 OK
- [x] Execution data saved to PostgreSQL
- [x] Execution retention set to 30 days
- [x] Webhooks endpoints configured (will be tested after Nginx setup)

## 🚀 Deployment Steps

```bash
# 1. Generate encryption key
openssl rand -base64 32
# Add to .env as N8N_ENCRYPTION_KEY

# 2. Create .env from template
cp .env.template .env
vim .env  # Set N8N_DB_PASSWORD and N8N_ENCRYPTION_KEY

# 3. Start PostgreSQL (if not running)
docker compose up -d db

# 4. Create n8n database
docker exec -it core-db psql -U core -f /docker-entrypoint-initdb.d/init-n8n-db.sql

# 5. Start n8n
docker compose up -d n8n

# 6. Check logs
docker logs -f core-n8n
# Expected: "n8n is now ready" message

# 7. Verify health
curl http://localhost:5678/healthz
# Expected: {"status":"ok"}
```

## 📝 Notes

- **Basic Auth Disabled**: n8n's built-in auth is disabled (N8N_BASIC_AUTH_ACTIVE=false) because we use Keycloak SSO
- **Database Primary**: Workflow definitions stored in PostgreSQL, not JSON files
- **Execution Pruning**: Automatic cleanup after 30 days (configurable via EXECUTIONS_DATA_MAX_AGE)
- **Timezone**: Set to Europe/Prague (adjust for your region)
- **Encryption Key**: Used for encrypting credentials in database - MUST be set and backed up

## 🔗 Dependencies

- **PostgreSQL**: core-db service (existing)
- **Docker Compose**: Service orchestration
- **Network**: core-net (existing Docker network)

## ⏭️ Next Steps

After S1 completes:

→ **S2**: Keycloak SSO Integration (configure OIDC client for n8n)  
→ **S3**: Nginx Reverse Proxy (route /n8n/* to n8n service with SSO)

---

**Estimated Effort**: ~4 hours (setup 2h, testing 1h, docs 1h)  
**LOC**: ~400 lines (config 200, SQL 50, docs 150)

> **Security Layer:** Authelia deployment with Redis sessions, OIDC client, 2FA support

## 📋 Story

**As a** platform administrator  
**I want** Authelia authentication gateway deployed  
**So that** n8n UI is protected by SSO with 2FA capability

## 🎯 Acceptance Criteria

**GIVEN** user accesses n8n UI  
**WHEN** not authenticated  
**THEN** redirected to Authelia login page  
**AND** after Keycloak SSO login, redirected back to n8n  
**AND** session stored in Redis with 1h expiration

**GIVEN** admin user enabled 2FA  
**WHEN** logging in  
**THEN** prompted for TOTP code after password  
**AND** login fails with invalid TOTP

## 🏗️ Implementation

### Docker Compose Configuration

```yaml
# docker/docker-compose.authelia.yml
version: '3.8'

services:
  authelia:
    image: authelia/authelia:4.38
    container_name: core-authelia
    restart: unless-stopped
    environment:
      - TZ=Europe/Prague
      - AUTHELIA_JWT_SECRET_FILE=/run/secrets/authelia_jwt_secret
      - AUTHELIA_SESSION_SECRET_FILE=/run/secrets/authelia_session_secret
      - AUTHELIA_STORAGE_ENCRYPTION_KEY_FILE=/run/secrets/authelia_storage_encryption_key
      - AUTHELIA_IDENTITY_PROVIDERS_OIDC_HMAC_SECRET_FILE=/run/secrets/authelia_oidc_hmac_secret
      - AUTHELIA_IDENTITY_PROVIDERS_OIDC_ISSUER_PRIVATE_KEY_FILE=/run/secrets/authelia_oidc_private_key
      - AUTHELIA_REDIS_PASSWORD=${AUTHELIA_REDIS_PASSWORD}
    volumes:
      - ./authelia/configuration.yml:/config/configuration.yml:ro
      - ./authelia/users_database.yml:/config/users_database.yml:ro
      - authelia_data:/config
    secrets:
      - authelia_jwt_secret
      - authelia_session_secret
      - authelia_storage_encryption_key
      - authelia_oidc_hmac_secret
      - authelia_oidc_private_key
    networks:
      - core-network
    expose:
      - 9091
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:9091/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  authelia_redis:
    image: redis:7.2-alpine
    container_name: core-authelia-redis
    restart: unless-stopped
    command: redis-server --requirepass ${AUTHELIA_REDIS_PASSWORD}
    volumes:
      - authelia_redis_data:/data
    networks:
      - core-network
    expose:
      - 6379
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3

secrets:
  authelia_jwt_secret:
    file: ./secrets/authelia_jwt_secret.txt
  authelia_session_secret:
    file: ./secrets/authelia_session_secret.txt
  authelia_storage_encryption_key:
    file: ./secrets/authelia_storage_encryption_key.txt
  authelia_oidc_hmac_secret:
    file: ./secrets/authelia_oidc_hmac_secret.txt
  authelia_oidc_private_key:
    file: ./secrets/authelia_oidc_private_key.pem

volumes:
  authelia_data:
    driver: local
  authelia_redis_data:
    driver: local

networks:
  core-network:
    external: true
```

### Authelia Configuration

```yaml
# docker/authelia/configuration.yml
---
theme: auto
default_2fa_method: totp

server:
  host: 0.0.0.0
  port: 9091
  path: ""
  enable_pprof: false
  enable_expvars: false

log:
  level: info
  format: json
  file_path: ""

totp:
  disable: false
  issuer: core-platform.local
  algorithm: sha1
  digits: 6
  period: 30
  skew: 1
  secret_size: 32

webauthn:
  disable: false
  display_name: Core Platform
  attestation_conveyance_preference: indirect
  user_verification: preferred
  timeout: 60s

authentication_backend:
  password_reset:
    disable: false
  refresh_interval: 5m
  
  ldap:
    implementation: custom
    url: ldap://keycloak:389
    timeout: 5s
    start_tls: false
    base_dn: dc=core-platform,dc=local
    username_attribute: uid
    additional_users_dn: ou=users
    users_filter: (&({username_attribute}={input})(objectClass=person))
    additional_groups_dn: ou=groups
    groups_filter: (&(member={dn})(objectClass=groupOfNames))
    group_name_attribute: cn
    mail_attribute: mail
    display_name_attribute: displayName
    user: cn=admin,dc=core-platform,dc=local
    password: ${LDAP_PASSWORD}

session:
  name: authelia_session
  domain: core-platform.local
  secret: file:///run/secrets/authelia_session_secret
  expiration: 1h
  inactivity: 5m
  remember_me_duration: 1M
  
  redis:
    host: authelia_redis
    port: 6379
    password: ${AUTHELIA_REDIS_PASSWORD}
    database_index: 0
    maximum_active_connections: 10
    minimum_idle_connections: 0

regulation:
  max_retries: 3
  find_time: 2m
  ban_time: 10m

storage:
  encryption_key: file:///run/secrets/authelia_storage_encryption_key
  local:
    path: /config/db.sqlite3

notifier:
  disable_startup_check: false
  filesystem:
    filename: /config/notification.txt

access_control:
  default_policy: deny
  
  rules:
    # Health checks bypass auth
    - domain: "core-platform.local"
      policy: bypass
      resources:
        - "^/health$"
        - "^/api/health$"
    
    # n8n webhooks bypass auth (public integrations)
    - domain: "core-platform.local"
      policy: bypass
      resources:
        - "^/n8n/webhook/.*$"
        - "^/n8n/webhook-test/.*$"
    
    # Admin workflows require 2FA
    - domain: "core-platform.local"
      policy: two_factor
      subject:
        - ["group:n8n-admins"]
      resources:
        - "^/n8n/workflows/.*$"
        - "^/n8n/executions/.*$"
        - "^/n8n/settings/.*$"
    
    # Regular users need 1FA
    - domain: "core-platform.local"
      policy: one_factor
      subject:
        - ["group:n8n-users"]
      resources:
        - "^/n8n/.*$"
    
    # Viewers read-only with 1FA
    - domain: "core-platform.local"
      policy: one_factor
      subject:
        - ["group:n8n-viewers"]
      resources:
        - "^/n8n/workflows$"
        - "^/n8n/executions$"

identity_providers:
  oidc:
    hmac_secret: file:///run/secrets/authelia_oidc_hmac_secret
    issuer_private_key: file:///run/secrets/authelia_oidc_private_key
    
    access_token_lifespan: 1h
    authorize_code_lifespan: 1m
    id_token_lifespan: 1h
    refresh_token_lifespan: 90d
    
    enable_client_debug_messages: false
    minimum_parameter_entropy: 8
    
    cors:
      endpoints:
        - authorization
        - token
        - revocation
        - introspection
      allowed_origins:
        - https://core-platform.local
      allowed_origins_from_client_redirect_uris: true
    
    clients:
      - id: n8n-workflow-engine
        description: n8n Workflow Automation Platform
        secret: "$pbkdf2-sha512$310000$c8p78n7pUMln0jzvd4aK4Q$JNRBzwAo0ek5qKn50cFzzvE9RXV88h1wJn5KGiHCHoA"  # CHANGE ME!
        public: false
        authorization_policy: two_factor
        
        redirect_uris:
          - https://core-platform.local/n8n/rest/oauth2-credential/callback
          - https://core-platform.local/authelia/callback
        
        scopes:
          - openid
          - profile
          - email
          - groups
        
        grant_types:
          - authorization_code
          - refresh_token
        
        response_types:
          - code
        
        response_modes:
          - form_post
          - query
          - fragment
        
        userinfo_signing_algorithm: none
```

### Secret Generation Script

```bash
#!/bin/bash
# scripts/generate_authelia_secrets.sh

set -e

SECRETS_DIR="docker/secrets"
mkdir -p "$SECRETS_DIR"

echo "🔐 Generating Authelia secrets..."

# JWT Secret (64 chars)
if [ ! -f "$SECRETS_DIR/authelia_jwt_secret.txt" ]; then
    openssl rand -base64 64 | tr -d '\n' > "$SECRETS_DIR/authelia_jwt_secret.txt"
    echo "✅ Generated authelia_jwt_secret.txt"
fi

# Session Secret (64 chars)
if [ ! -f "$SECRETS_DIR/authelia_session_secret.txt" ]; then
    openssl rand -base64 64 | tr -d '\n' > "$SECRETS_DIR/authelia_session_secret.txt"
    echo "✅ Generated authelia_session_secret.txt"
fi

# Storage Encryption Key (64 chars)
if [ ! -f "$SECRETS_DIR/authelia_storage_encryption_key.txt" ]; then
    openssl rand -base64 64 | tr -d '\n' > "$SECRETS_DIR/authelia_storage_encryption_key.txt"
    echo "✅ Generated authelia_storage_encryption_key.txt"
fi

# OIDC HMAC Secret (64 chars)
if [ ! -f "$SECRETS_DIR/authelia_oidc_hmac_secret.txt" ]; then
    openssl rand -base64 64 | tr -d '\n' > "$SECRETS_DIR/authelia_oidc_hmac_secret.txt"
    echo "✅ Generated authelia_oidc_hmac_secret.txt"
fi

# OIDC RSA Key Pair (2048-bit)
if [ ! -f "$SECRETS_DIR/authelia_oidc_private_key.pem" ]; then
    openssl genrsa -out "$SECRETS_DIR/authelia_oidc_private_key.pem" 2048
    openssl rsa -in "$SECRETS_DIR/authelia_oidc_private_key.pem" \
                -pubout -out "$SECRETS_DIR/authelia_oidc_public_key.pem"
    echo "✅ Generated OIDC RSA key pair"
fi

# n8n OIDC Client Secret (for Keycloak)
if [ ! -f "$SECRETS_DIR/n8n_oidc_client_secret.txt" ]; then
    CLIENT_SECRET=$(openssl rand -base64 32 | tr -d '\n')
    echo "$CLIENT_SECRET" > "$SECRETS_DIR/n8n_oidc_client_secret.txt"
    echo "✅ Generated n8n_oidc_client_secret.txt"
    echo ""
    echo "⚠️  IMPORTANT: Save this client secret for Keycloak configuration:"
    echo "   Client Secret: $CLIENT_SECRET"
    echo ""
    echo "🔒 Now hash this secret for Authelia configuration.yml:"
    echo "   Run: docker run --rm authelia/authelia:latest authelia crypto hash generate pbkdf2 --password \"$CLIENT_SECRET\""
fi

echo ""
echo "✅ All secrets generated in $SECRETS_DIR"
echo "🔒 NEVER commit these files to Git!"
echo "📋 Next steps:"
echo "   1. Hash client secret for Authelia"
echo "   2. Update configuration.yml with hashed secret"
echo "   3. Create n8n client in Keycloak with client secret"
echo "   4. Start Authelia: docker-compose -f docker/docker-compose.authelia.yml up -d"
```

### Hash Client Secret Script

```bash
#!/bin/bash
# scripts/hash_authelia_client_secret.sh

set -e

SECRETS_DIR="docker/secrets"
CLIENT_SECRET_FILE="$SECRETS_DIR/n8n_oidc_client_secret.txt"

if [ ! -f "$CLIENT_SECRET_FILE" ]; then
    echo "❌ Error: $CLIENT_SECRET_FILE not found!"
    echo "   Run: ./scripts/generate_authelia_secrets.sh first"
    exit 1
fi

CLIENT_SECRET=$(cat "$CLIENT_SECRET_FILE")

echo "🔐 Hashing client secret for Authelia configuration..."
echo ""

HASHED_SECRET=$(docker run --rm authelia/authelia:latest \
    authelia crypto hash generate pbkdf2 --password "$CLIENT_SECRET" 2>&1 | grep "Digest:" | awk '{print $2}')

echo "✅ Hashed secret (copy to configuration.yml):"
echo ""
echo "  clients:"
echo "    - id: n8n-workflow-engine"
echo "      secret: \"$HASHED_SECRET\""
echo ""
echo "📋 Original secret (for Keycloak client configuration):"
echo "   $CLIENT_SECRET"
```

## 📊 Production Metrics

- **Deployment time:** <5 minutes
- **Auth latency:** <200ms P95
- **Session storage:** Redis (100k+ sessions)
- **2FA adoption:** 90% admin users within 30 days
- **Availability:** 99.9% (Authelia + Redis combined)
- **Security:** 0 bypass except webhooks + health checks

---

**Story Points:** 3  
**Estimate:** 800 LOC  
**Dependencies:** Docker, Redis, Keycloak (EPIC-003)
