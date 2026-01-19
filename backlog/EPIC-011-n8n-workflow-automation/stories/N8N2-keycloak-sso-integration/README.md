---
id: N8N2
epic: EPIC-011-n8n-workflow-automation
title: "Keycloak SSO Integration"
priority: P1
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "1 day"
path_mapping:
  code_paths:
    - docker/docker-compose.yml
    - docker/keycloak/realm-core-platform.template.json
    - docker/keycloak/realm-tenant-template.json
    - docker/keycloak/realm-admin.template.json
    - docker/keycloak/clients
  test_paths: []
  docs_paths:
    - docs/KEYCLOAK_BOOTSTRAP_GUIDE.md
    - docs/auth-keycloak-setup.md
---

# S2: Keycloak SSO Integration

> **Authentication:** Configure Keycloak OIDC client for n8n SSO login

## 📋 Story

**As a** platform administrator  
**I want** Keycloak configured as SSO provider for n8n  
**So that** users authenticate once and access n8n with their existing credentials

## 🎯 Acceptance Criteria

**GIVEN** Keycloak is running  
**WHEN** accessing n8n UI at `/n8n`  
**THEN** user is redirected to Keycloak login  
**AND** successful login grants access to n8n  
**AND** user roles (WF_ADMIN/WF_EDITOR/WF_READER) are enforced  
**AND** roles are exposed in the token and forwarded as `X-Auth-Request-Roles`

## 🏗️ Implementation

### 1. Create Keycloak Client

**Script:** `scripts/keycloak/create-n8n-client.sh`

```bash
#!/bin/bash
set -e

KEYCLOAK_URL="${KEYCLOAK_BASE_URL:-https://admin.core-platform.local}"
REALM="admin"
CLIENT_ID="n8n-client"

echo "🔐 Creating Keycloak client for n8n..."

# Login to Keycloak admin
ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=${KEYCLOAK_ADMIN_PASSWORD}" \
  -d "grant_type=password" | jq -r '.access_token')

# Create n8n client
curl -X POST "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "n8n-client",
    "name": "n8n Workflow Automation",
    "enabled": true,
    "protocol": "openid-connect",
    "publicClient": false,
    "standardFlowEnabled": true,
    "redirectUris": [
      "https://admin.core-platform.local/n8n/*",
      "https://admin.core-platform.local/oauth2/callback"
    ],
    "webOrigins": ["https://admin.core-platform.local"]
  }'

# Get client secret
CLIENT_UUID=$(curl -s "$KEYCLOAK_URL/admin/realms/$REALM/clients?clientId=$CLIENT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

CLIENT_SECRET=$(curl -s "$KEYCLOAK_URL/admin/realms/$REALM/clients/$CLIENT_UUID/client-secret" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.value')

echo "✅ Client created"
echo "Client ID: $CLIENT_ID"
echo "Client Secret: $CLIENT_SECRET"
echo ""
echo "⚠️  Add to .env: N8N_OAUTH_CLIENT_SECRET=$CLIENT_SECRET"
```

### 2. Create Roles

```bash
# Create WF_READER role
curl -X POST "$KEYCLOAK_URL/admin/realms/$REALM/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "WF_READER",
    "description": "n8n workflow read-only access"
  }'

# Create WF_EDITOR role
curl -X POST "$KEYCLOAK_URL/admin/realms/$REALM/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "WF_EDITOR",
    "description": "n8n workflow edit/run access"
  }'

# Create WF_ADMIN role
curl -X POST "$KEYCLOAK_URL/admin/realms/$REALM/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "WF_ADMIN",
    "description": "n8n workflow admin/governance access"
  }'

# Create WF_PROXY_SERVICE role
curl -X POST "$KEYCLOAK_URL/admin/realms/$REALM/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "WF_PROXY_SERVICE",
    "description": "service role for n8n proxy/governance"
  }'
```

Also create a confidential client `n8n-internal` (service account) for registry sync and store its client secret in Vault.

### 3. OAuth2 Proxy Configuration

**File:** `docker/docker-compose.yml`

```yaml
services:
  oauth2-proxy-n8n:
    image: quay.io/oauth2-proxy/oauth2-proxy:v7.5.1
    container_name: core-oauth2-proxy-n8n
    restart: unless-stopped
    environment:
      OAUTH2_PROXY_PROVIDER: oidc
      OAUTH2_PROXY_OIDC_ISSUER_URL: ${KEYCLOAK_BASE_URL}/realms/admin
      OAUTH2_PROXY_CLIENT_ID: n8n-client
      OAUTH2_PROXY_CLIENT_SECRET: ${N8N_OAUTH_CLIENT_SECRET}
      OAUTH2_PROXY_REDIRECT_URL: https://admin.core-platform.local/oauth2/callback
      OAUTH2_PROXY_SCOPE: "openid email profile roles"
      OAUTH2_PROXY_OIDC_GROUPS_CLAIM: roles
      OAUTH2_PROXY_ALLOWED_GROUPS: "WF_ADMIN,WF_EDITOR,WF_READER"
      OAUTH2_PROXY_COOKIE_SECRET: ${OAUTH2_PROXY_COOKIE_SECRET}
      OAUTH2_PROXY_COOKIE_SECURE: true
      OAUTH2_PROXY_EMAIL_DOMAINS: "*"
      OAUTH2_PROXY_UPSTREAMS: http://n8n-proxy:3000
      OAUTH2_PROXY_HTTP_ADDRESS: 0.0.0.0:4180
      OAUTH2_PROXY_SKIP_AUTH_ROUTES: "^/webhook.*"
    ports:
      - "4180:4180"
    networks:
      - core-net
    depends_on:
      - keycloak
      - n8n
```

### 4. Secrets via Vault (recommended)

Store OAuth2-Proxy secrets in Vault and template them into the runtime container.

**Vault secrets:**
- `secret/<env>/keycloak.oauth2_proxy_client_secret`
- `secret/<env>/keycloak.cookie_secret`

**Vault agent templates:**
- `docker/vault-agent/templates/oauth2-proxy.env.ctmpl`
- `docker/vault-agent/templates/oauth2-proxy-flower.env.ctmpl`

**Runtime files:**
- `/run/secrets/oauth2-proxy.env`
- `/run/secrets/oauth2-proxy-flower.env`

## ✅ Testing

```bash
# 1. Create client
bash scripts/keycloak/create-n8n-client.sh

# 2. Start OAuth2 Proxy
docker compose up -d oauth2-proxy-n8n

# 3. Test redirect
curl -I http://localhost:4180/
# Expected: 302 redirect to Keycloak

# 4. Browser test
# Open: https://admin.core-platform.local/n8n
# Expected: Keycloak login → n8n UI
```

## 🎯 Acceptance Checklist

- [x] Keycloak client created (n8n-client)
- [x] Roles created (WF_ADMIN, WF_EDITOR, WF_READER, WF_PROXY_SERVICE)
- [x] OAuth2 Proxy deployed
- [x] SSO login flow works
- [x] Webhooks bypass authentication

---

**Effort**: ~3 hours  
**LOC**: ~300 lines
