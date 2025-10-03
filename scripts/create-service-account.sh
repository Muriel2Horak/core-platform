#!/bin/bash

# 🔐 SECURITY: Automatic Service Account Creation Script
# This script creates a secure service account for backend admin operations

set -e

echo "🔐 Creating secure Service Account in Keycloak..."

# Configuration - using env variables instead of hardcoded values
KEYCLOAK_URL="http://localhost:8081"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASS="${KEYCLOAK_ADMIN_PASSWORD:-admin123}"
REALM="core-platform"
CLIENT_ID="backend-admin-service"
CLIENT_NAME="Backend Admin Service"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Step 1: Getting admin token...${NC}"

# Get admin token
ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASS}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$ADMIN_TOKEN" = "null" ] || [ -z "$ADMIN_TOKEN" ]; then
  echo -e "${RED}❌ Failed to get admin token. Is Keycloak running on ${KEYCLOAK_URL}?${NC}"
  exit 1
fi

echo -e "${GREEN}✅ Admin token obtained${NC}"

echo -e "${YELLOW}Step 2: Creating service account client...${NC}"

# Create service account client
CLIENT_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "'${CLIENT_ID}'",
    "name": "'${CLIENT_NAME}'",
    "description": "Secure service account for backend admin operations",
    "enabled": true,
    "clientAuthenticatorType": "client-secret",
    "serviceAccountsEnabled": true,
    "standardFlowEnabled": false,
    "implicitFlowEnabled": false,
    "directAccessGrantsEnabled": false,
    "authorizationServicesEnabled": false,
    "publicClient": false,
    "protocol": "openid-connect"
  }')

echo -e "${GREEN}✅ Service account client created${NC}"

echo -e "${YELLOW}Step 3: Getting client secret...${NC}"

# Get client UUID
CLIENT_UUID=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" | jq -r '.[0].id')

if [ "$CLIENT_UUID" = "null" ] || [ -z "$CLIENT_UUID" ]; then
  echo -e "${RED}❌ Failed to get client UUID${NC}"
  exit 1
fi

# Get client secret
CLIENT_SECRET=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${CLIENT_UUID}/client-secret" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" | jq -r '.value')

if [ "$CLIENT_SECRET" = "null" ] || [ -z "$CLIENT_SECRET" ]; then
  echo -e "${RED}❌ Failed to get client secret${NC}"
  exit 1
fi

echo -e "${GREEN}✅ Client secret obtained${NC}"

echo -e "${YELLOW}Step 4: Configuring service account roles...${NC}"

# Get service account user ID
SERVICE_ACCOUNT_USER_ID=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${CLIENT_UUID}/service-account-user" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" | jq -r '.id')

# Get realm-management client UUID
REALM_MGMT_CLIENT_UUID=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=realm-management" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" | jq -r '.[0].id')

# Get manage-users role
MANAGE_USERS_ROLE=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${REALM_MGMT_CLIENT_UUID}/roles/manage-users" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}")

# Assign manage-users role to service account
curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${SERVICE_ACCOUNT_USER_ID}/role-mappings/clients/${REALM_MGMT_CLIENT_UUID}" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "[${MANAGE_USERS_ROLE}]"

echo -e "${GREEN}✅ Service account roles configured (manage-users only)${NC}"

echo -e "${YELLOW}Step 5: Updating .env file...${NC}"

# Update .env file with client secret
ENV_FILE="../.env"
if [ -f "$ENV_FILE" ]; then
  # Use sed to replace the placeholder
  sed -i.backup "s/KEYCLOAK_ADMIN_CLIENT_SECRET=PLACEHOLDER_WILL_BE_SET_AFTER_KEYCLOAK_SETUP/KEYCLOAK_ADMIN_CLIENT_SECRET=${CLIENT_SECRET}/" "$ENV_FILE"
  echo -e "${GREEN}✅ .env file updated with client secret${NC}"
else
  echo -e "${RED}❌ .env file not found at ${ENV_FILE}${NC}"
  exit 1
fi

echo -e "${GREEN}🎉 SUCCESS! Service Account created successfully${NC}"
echo ""
echo -e "${YELLOW}📋 Service Account Details:${NC}"
echo -e "Client ID: ${GREEN}${CLIENT_ID}${NC}"
echo -e "Client Secret: ${GREEN}${CLIENT_SECRET}${NC}"
echo -e "Roles: ${GREEN}manage-users${NC}"
echo ""
echo -e "${YELLOW}📝 Next Steps:${NC}"
echo "1. Restart backend container to load new environment variables"
echo "2. Test admin operations (password change, profile update)"
echo "3. Monitor audit logs for security events"
echo ""
echo -e "${RED}🚨 SECURITY REMINDER:${NC}"
echo "- Client secret is now in .env file (never commit this!)"
echo "- Service account has minimal required permissions"
echo "- All admin operations are audited"