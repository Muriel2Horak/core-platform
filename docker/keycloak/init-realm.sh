#!/bin/bash
set -e

echo "🔍 Checking if admin realm needs initialization..."

# Keycloak paths
KC_BIN="/opt/keycloak/bin"
KCADM="${KC_BIN}/kcadm.sh"

# Keycloak admin credentials
KEYCLOAK_URL="${KEYCLOAK_URL:-https://localhost:8443}"
KC_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KC_PASS="${KEYCLOAK_ADMIN_PASSWORD:-admin}"

# Wait for Keycloak to be ready
echo "⏳ Waiting for Keycloak to be ready..."
for i in {1..60}; do
  if ${KC_BIN}/kc.sh show-config 2>/dev/null | grep -q "Keycloak"; then
    echo "✅ Keycloak is ready"
    break
  fi
  if [ $i -eq 60 ]; then
    echo "❌ Keycloak failed to start after 60 seconds"
    exit 1
  fi
  sleep 2
done

# Authenticate kcadm
echo "🔑 Authenticating with Keycloak Admin CLI..."
${KCADM} config credentials \
  --server "${KEYCLOAK_URL}" \
  --realm master \
  --user "${KC_ADMIN}" \
  --password "${KC_PASS}" \
  --config /tmp/kcadm.config

# Check if admin realm exists
echo "🔍 Checking if 'admin' realm exists..."
if ${KCADM} get realms/admin --config /tmp/kcadm.config >/dev/null 2>&1; then
  echo "✅ Realm 'admin' exists - checking admin-client..."
  
  # Get admin-client ID
  CLIENT_ID=$(${KCADM} get clients --config /tmp/kcadm.config -r admin -q clientId=admin-client --fields id --format csv --noquotes 2>/dev/null | tail -1)
  
  if [ -n "$CLIENT_ID" ] && [ "$CLIENT_ID" != "id" ]; then
    echo "✅ admin-client already exists (ID: ${CLIENT_ID})"
    
    # Update client secret
    echo "🔄 Updating admin-client secret..."
    ${KCADM} update clients/${CLIENT_ID} \
      --config /tmp/kcadm.config \
      -r admin \
      -s "secret=${KEYCLOAK_ADMIN_CLIENT_SECRET}"
    
    echo "✅ admin-client updated successfully"
  else
    echo "⚠️ admin-client NOT found - creating..."
    
    # Create admin-client
    ${KCADM} create clients \
      --config /tmp/kcadm.config \
      -r admin \
      -s clientId=admin-client \
      -s name="Admin Client" \
      -s description="Client for admin authentication and E2E tests" \
      -s enabled=true \
      -s publicClient=false \
      -s bearerOnly=false \
      -s standardFlowEnabled=true \
      -s implicitFlowEnabled=false \
      -s directAccessGrantsEnabled=true \
      -s serviceAccountsEnabled=false \
      -s "secret=${KEYCLOAK_ADMIN_CLIENT_SECRET}" \
      -s 'redirectUris=["https://'"${DOMAIN}"'/*","https://admin.'"${DOMAIN}"'/*","http://localhost:3000/*","http://localhost/*"]' \
      -s 'webOrigins=["https://'"${DOMAIN}"'","https://admin.'"${DOMAIN}"'","http://localhost:3000","http://localhost"]'
    
    echo "✅ admin-client created successfully"
  fi
else
  echo "⚠️ Realm 'admin' does NOT exist - will be imported from realm-admin.json on startup"
fi

echo "✅ Realm initialization complete"
