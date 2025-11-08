# N8N2: Keycloak SSO Integration - OAuth2/OIDC Authentication

**Typ:** TASK  
**Epic:** EPIC-011 (n8n External Orchestration Layer)  
**Fase:** Phase 3 (n8n Deployment)  
**Priorita:** HIGH (security)  
**Effort:** 300 LOC, 1 den  
**Dependencies:** N8N1 (Platform Deployment), Keycloak  
**Status:** ⏳ TODO

---

## 🎯 Cíl

Integr ovat **Keycloak SSO** pro n8n authentication via OAuth2/OIDC:
- Keycloak client `n8n-client`
- Client roles: `n8n-users`, `n8n-admins`
- JWT token configuration
- Auto-redirect to Keycloak login

---

## 📋 Požadavky

### Funkční Požadavky

1. **Keycloak Client**
   - Client ID: `n8n-client`
   - Protocol: `openid-connect`
   - Redirect URIs: `https://admin.${DOMAIN}/n8n/*`
   - Client roles: `n8n-users`, `n8n-admins`

2. **n8n Configuration**
   - OAuth2 provider: Keycloak
   - Auth URL: `https://admin.${DOMAIN}/realms/admin/protocol/openid-connect/auth`
   - Token URL: `https://admin.${DOMAIN}/realms/admin/protocol/openid-connect/token`
   - Userinfo URL: `https://admin.${DOMAIN}/realms/admin/protocol/openid-connect/userinfo`

3. **User Mapping**
   - Email → n8n username
   - Roles → n8n permissions (owner/member/viewer)

---

## 🔧 Implementace

### 1. Keycloak Client Configuration

**File:** `docker/keycloak/realm-admin.template.json` (add n8n-client)

```json
{
  "clients": [
    {
      "clientId": "n8n-client",
      "name": "n8n Workflow Automation",
      "description": "n8n workflow automation platform OAuth2 client",
      "enabled": true,
      "protocol": "openid-connect",
      "publicClient": false,
      "clientAuthenticatorType": "client-secret",
      "secret": "${N8N_CLIENT_SECRET}",
      "redirectUris": [
        "https://admin.${DOMAIN}/n8n/*",
        "https://admin.${DOMAIN}/n8n/rest/oauth2-credential/callback"
      ],
      "webOrigins": [
        "https://admin.${DOMAIN}"
      ],
      "standardFlowEnabled": true,
      "directAccessGrantsEnabled": false,
      "serviceAccountsEnabled": false,
      "authorizationServicesEnabled": false,
      "fullScopeAllowed": true,
      "attributes": {
        "access.token.lifespan": "3600"
      },
      "protocolMappers": [
        {
          "name": "n8n-email",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-usermodel-property-mapper",
          "consentRequired": false,
          "config": {
            "userinfo.token.claim": "true",
            "user.attribute": "email",
            "id.token.claim": "true",
            "access.token.claim": "true",
            "claim.name": "email",
            "jsonType.label": "String"
          }
        },
        {
          "name": "n8n-roles",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-usermodel-client-role-mapper",
          "consentRequired": false,
          "config": {
            "multivalued": "true",
            "userinfo.token.claim": "true",
            "id.token.claim": "true",
            "access.token.claim": "true",
            "claim.name": "n8n_roles",
            "jsonType.label": "String"
          }
        }
      ]
    }
  ],
  "roles": {
    "client": {
      "n8n-client": [
        {
          "name": "n8n-users",
          "description": "n8n regular users"
        },
        {
          "name": "n8n-admins",
          "description": "n8n administrators (full access)"
        }
      ]
    }
  }
}
```

---

### 2. n8n OAuth2 Configuration

**File:** `docker/docker-compose.yml` (update n8n environment)

```yaml
services:
  n8n:
    environment:
      # ... existing vars ...
      
      # OAuth2 / OIDC Authentication
      - N8N_AUTH_MODE=oauth2
      - N8N_OAUTH2_PROVIDER=keycloak
      - N8N_OAUTH2_CLIENT_ID=n8n-client
      - N8N_OAUTH2_CLIENT_SECRET=${N8N_CLIENT_SECRET}
      - N8N_OAUTH2_AUTHORIZATION_URL=https://admin.${DOMAIN}/realms/admin/protocol/openid-connect/auth
      - N8N_OAUTH2_TOKEN_URL=https://admin.${DOMAIN}/realms/admin/protocol/openid-connect/token
      - N8N_OAUTH2_USERINFO_URL=https://admin.${DOMAIN}/realms/admin/protocol/openid-connect/userinfo
      - N8N_OAUTH2_SCOPE=openid email profile
      - N8N_OAUTH2_USER_ID_CLAIM=email
      - N8N_OAUTH2_USER_NAME_CLAIM=email
      - N8N_OAUTH2_ROLE_CLAIM=n8n_roles
      
      # SSL/TLS (allow self-signed certs for dev)
      - NODE_TLS_REJECT_UNAUTHORIZED=0  # ⚠️ Remove in prod!
```

---

### 3. Environment Variables

**File:** `.env.template` (add OAuth2 client secret)

```bash
# 🔐 N8N OAUTH2 CONFIGURATION
N8N_CLIENT_SECRET=n8n-client-secret-change-in-prod
```

---

### 4. User Provisioning Script

**File:** `scripts/n8n-provision-users.sh`

```bash
#!/bin/bash
set -e

KEYCLOAK_URL="${KEYCLOAK_URL:-https://admin.core-platform.local}"
KEYCLOAK_REALM="admin"
KEYCLOAK_CLIENT="n8n-client"

echo "👥 Provisioning n8n users in Keycloak..."

# Get admin token
ADMIN_TOKEN=$(curl -k -s -X POST \
  "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  | jq -r '.access_token')

# Create test n8n user
curl -k -X POST \
  "${KEYCLOAK_URL}/admin/realms/${KEYCLOAK_REALM}/users" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "n8n-admin@core-platform.local",
    "email": "n8n-admin@core-platform.local",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{
      "type": "password",
      "value": "n8n123",
      "temporary": false
    }]
  }'

# Assign n8n-admins role
USER_ID=$(curl -k -s -G \
  "${KEYCLOAK_URL}/admin/realms/${KEYCLOAK_REALM}/users" \
  --data-urlencode "email=n8n-admin@core-platform.local" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  | jq -r '.[0].id')

curl -k -X POST \
  "${KEYCLOAK_URL}/admin/realms/${KEYCLOAK_REALM}/users/${USER_ID}/role-mappings/clients/${N8N_CLIENT_ID}" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '[{
    "name": "n8n-admins"
  }]'

echo "✅ n8n-admin user created: n8n-admin@core-platform.local / n8n123"
```

---

### 5. Integration Test

**File:** `backend/src/test/java/cz/muriel/core/n8n/N8nKeycloakSSOTest.java`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class N8nKeycloakSSOTest {
    
    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0")
        .withRealmImportFile("keycloak/realm-admin-test.json");
    
    @Test
    void shouldAuthenticateWithKeycloak() {
        // Given: Keycloak running with n8n-client
        String authUrl = keycloak.getAuthServerUrl() + "/realms/admin/protocol/openid-connect/auth";
        
        // When: User redirects to Keycloak
        WebClient client = WebClient.create();
        String response = client.get()
            .uri(authUrl + "?client_id=n8n-client&redirect_uri=https://admin.core-platform.local/n8n/")
            .retrieve()
            .bodyToMono(String.class)
            .block();
        
        // Then: Keycloak login page shown
        assertThat(response).contains("Sign in to your account");
    }
}
```

---

## ✅ Acceptance Criteria

1. **Keycloak:**
   - [ ] Client `n8n-client` created
   - [ ] Roles `n8n-users`, `n8n-admins` configured
   - [ ] Redirect URIs correct

2. **n8n:**
   - [ ] OAuth2 authentication enabled
   - [ ] Auto-redirect to Keycloak login
   - [ ] JWT token validation

3. **User Flow:**
   - [ ] User opens https://admin.core-platform.local/n8n/
   - [ ] Redirects to Keycloak login
   - [ ] Login successful → back to n8n
   - [ ] Email mapped to n8n username

4. **Testy:**
   - [ ] Integration test: Keycloak OAuth2 flow
   - [ ] Test role mapping

---

**Related Stories:**
- N8N1: Platform Deployment
- N8N3: Nginx Proxy (reverse proxy for OAuth callback)
