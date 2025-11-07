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
**AND** user roles (n8n-users, n8n-admins) are enforced

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
# Create n8n-users role
curl -X POST "$KEYCLOAK_URL/admin/realms/$REALM/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "n8n-users",
    "description": "n8n workflow users (read-only)"
  }'

# Create n8n-admins role
curl -X POST "$KEYCLOAK_URL/admin/realms/$REALM/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "n8n-admins",
    "description": "n8n administrators (full access)"
  }'
```

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
      OAUTH2_PROXY_COOKIE_SECRET: ${OAUTH2_PROXY_COOKIE_SECRET}
      OAUTH2_PROXY_COOKIE_SECURE: true
      OAUTH2_PROXY_EMAIL_DOMAINS: "*"
      OAUTH2_PROXY_UPSTREAMS: http://n8n:5678
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
- [x] Roles created (n8n-users, n8n-admins)
- [x] OAuth2 Proxy deployed
- [x] SSO login flow works
- [x] Webhooks bypass authentication

---

**Effort**: ~3 hours  
**LOC**: ~300 lines
