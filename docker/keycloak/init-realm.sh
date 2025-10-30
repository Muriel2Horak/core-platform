#!/bin/bash
set -e

echo "🔍 Checking if admin realm needs initialization..."

# Keycloak admin credentials
KEYCLOAK_URL="${KEYCLOAK_URL:-https://localhost:8443}"
KC_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KC_PASS="${KEYCLOAK_ADMIN_PASSWORD:-admin}"

# Wait for Keycloak to be ready
echo "⏳ Waiting for Keycloak to be ready..."
for i in {1..60}; do
  if curl -k -s "${KEYCLOAK_URL}/health/ready" | grep -q "UP" 2>/dev/null; then
    echo "✅ Keycloak is ready"
    break
  fi
  if [ $i -eq 60 ]; then
    echo "❌ Keycloak failed to start after 60 seconds"
    exit 1
  fi
  sleep 2
done

# Get admin token
echo "🔑 Getting admin access token..."
ADMIN_TOKEN=$(curl -k -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=${KC_ADMIN}" \
  -d "password=${KC_PASS}" \
  -d "grant_type=password" | jq -r '.access_token')

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
  echo "❌ Failed to get admin token"
  exit 1
fi

# Check if admin realm exists
echo "🔍 Checking if 'admin' realm exists..."
REALM_EXISTS=$(curl -k -s -o /dev/null -w "%{http_code}" \
  "${KEYCLOAK_URL}/admin/realms/admin" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}")

if [ "$REALM_EXISTS" = "200" ]; then
  echo "✅ Realm 'admin' exists - checking admin-client..."
  
  # Get all clients in admin realm
  CLIENTS=$(curl -k -s "${KEYCLOAK_URL}/admin/realms/admin/clients" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")
  
  # Check if admin-client exists
  CLIENT_ID=$(echo "$CLIENTS" | jq -r '.[] | select(.clientId=="admin-client") | .id')
  
  if [ -n "$CLIENT_ID" ] && [ "$CLIENT_ID" != "null" ]; then
    echo "✅ admin-client already exists (ID: ${CLIENT_ID})"
    
    # Update client secret if needed
    echo "🔄 Updating admin-client secret..."
    curl -k -s -X PUT "${KEYCLOAK_URL}/admin/realms/admin/clients/${CLIENT_ID}" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      -H "Content-Type: application/json" \
      -d '{
        "clientId": "admin-client",
        "enabled": true,
        "publicClient": false,
        "directAccessGrantsEnabled": true,
        "standardFlowEnabled": true,
        "secret": "'"${KEYCLOAK_ADMIN_CLIENT_SECRET}"'",
        "redirectUris": [
          "https://'"${DOMAIN}"'/*",
          "https://admin.'"${DOMAIN}"'/*",
          "http://localhost:3000/*",
          "http://localhost/*"
        ],
        "webOrigins": [
          "https://'"${DOMAIN}"'",
          "https://admin.'"${DOMAIN}"'",
          "http://localhost:3000",
          "http://localhost"
        ]
      }'
    
    echo "✅ admin-client updated successfully"
  else
    echo "⚠️ admin-client NOT found - creating..."
    
    # Create admin-client
    curl -k -s -X POST "${KEYCLOAK_URL}/admin/realms/admin/clients" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      -H "Content-Type: application/json" \
      -d '{
        "clientId": "admin-client",
        "name": "Admin Client",
        "description": "Client for admin authentication and E2E tests",
        "enabled": true,
        "publicClient": false,
        "bearerOnly": false,
        "standardFlowEnabled": true,
        "implicitFlowEnabled": false,
        "directAccessGrantsEnabled": true,
        "serviceAccountsEnabled": false,
        "secret": "'"${KEYCLOAK_ADMIN_CLIENT_SECRET}"'",
        "redirectUris": [
          "https://'"${DOMAIN}"'/*",
          "https://admin.'"${DOMAIN}"'/*",
          "http://localhost:3000/*",
          "http://localhost/*"
        ],
        "webOrigins": [
          "https://'"${DOMAIN}"'",
          "https://admin.'"${DOMAIN}"'",
          "http://localhost:3000",
          "http://localhost"
        ],
        "attributes": {
          "access.token.lifespan": "3600"
        }
      }'
    
    echo "✅ admin-client created successfully"
  fi
else
  echo "⚠️ Realm 'admin' does NOT exist - will be imported from realm-admin.json on startup"
fi

echo "✅ Realm initialization complete"
