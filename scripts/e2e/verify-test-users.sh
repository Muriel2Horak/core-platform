#!/bin/bash

###############################################################################
# E2E Test Users Verification Script
# 
# Verifies that test users exist and have correct roles assigned.
# Usage: ./scripts/e2e/verify-test-users.sh
###############################################################################

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Config
KEYCLOAK_URL="${KEYCLOAK_URL:-https://admin.core-platform.local}"
REALM="${KEYCLOAK_REALM:-admin}"
CLIENT_ID="${KEYCLOAK_CLIENT_ID:-web}"

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  E2E Test Users Verification${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Function to get auth token
get_token() {
  local username=$1
  local password=$2
  
  local response=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "client_id=${CLIENT_ID}" \
    -d "username=${username}" \
    -d "password=${password}" \
    -d "grant_type=password" 2>/dev/null)
  
  local token=$(echo "$response" | jq -r '.access_token // empty' 2>/dev/null)
  
  if [ -z "$token" ] || [ "$token" == "null" ]; then
    echo ""
  else
    echo "$token"
  fi
}

# Function to get user info
get_user_info() {
  local token=$1
  
  curl -s -H "Authorization: Bearer $token" \
    "${KEYCLOAK_URL}/api/users/me" 2>/dev/null
}

# Function to verify user
verify_user() {
  local username=$1
  local password=$2
  local expected_admin=$3
  
  echo -e "${BLUE}Verifying user: ${username}${NC}"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  
  # Try to login
  echo -n "  🔐 Attempting login... "
  local token=$(get_token "$username" "$password")
  
  if [ -z "$token" ]; then
    echo -e "${RED}FAILED${NC}"
    echo -e "     ${RED}✗ Cannot obtain auth token${NC}"
    echo -e "     ${YELLOW}User may not exist or credentials are incorrect${NC}"
    echo ""
    return 1
  fi
  
  echo -e "${GREEN}OK${NC}"
  
  # Get user info
  echo -n "  👤 Fetching user info... "
  local user_info=$(get_user_info "$token")
  
  if [ -z "$user_info" ]; then
    echo -e "${RED}FAILED${NC}"
    echo -e "     ${RED}✗ Cannot fetch user info${NC}"
    echo ""
    return 1
  fi
  
  echo -e "${GREEN}OK${NC}"
  
  # Parse user data
  local display_name=$(echo "$user_info" | jq -r '.displayName // .username // "Unknown"')
  local user_id=$(echo "$user_info" | jq -r '.id // "Unknown"')
  local roles=$(echo "$user_info" | jq -r '.roles[]? // empty' 2>/dev/null | tr '\n' ', ' | sed 's/,$//')
  
  echo -e "  ℹ️  Display Name: ${display_name}"
  echo -e "  ℹ️  User ID: ${user_id}"
  
  # Check roles
  if [ -z "$roles" ]; then
    echo -e "  ${YELLOW}⚠️  Roles: NONE${NC}"
  else
    echo -e "  ✅ Roles: ${roles}"
  fi
  
  # Verify admin access if expected
  if [ "$expected_admin" == "true" ]; then
    echo -n "  🔍 Checking admin access... "
    
    if echo "$roles" | grep -qi "admin"; then
      echo -e "${GREEN}OK${NC}"
      echo -e "     ✓ Has admin role"
    else
      echo -e "${YELLOW}WARNING${NC}"
      echo -e "     ${YELLOW}⚠️  No admin role found${NC}"
      echo -e "     ${YELLOW}Expected roles: CORE_ROLE_ADMIN, CORE_ROLE_USER_MANAGER${NC}"
    fi
    
    # Check Studio access
    if echo "$roles" | grep -qi "studio"; then
      echo -e "     ✓ Has Studio role"
    else
      echo -e "     ${YELLOW}⚠️  No Studio role found${NC}"
    fi
    
    # Check Workflow access
    if echo "$roles" | grep -qi "workflow"; then
      echo -e "     ✓ Has Workflow role"
    else
      echo -e "     ${YELLOW}⚠️  No Workflow designer role found${NC}"
    fi
  else
    echo -n "  🔍 Checking user access... "
    
    if echo "$roles" | grep -qi "user"; then
      echo -e "${GREEN}OK${NC}"
      echo -e "     ✓ Has user role"
    else
      echo -e "${YELLOW}WARNING${NC}"
      echo -e "     ${YELLOW}⚠️  No user role found${NC}"
    fi
    
    # Verify NO admin access
    if echo "$roles" | grep -qi "admin"; then
      echo -e "     ${YELLOW}⚠️  WARNING: Has admin role (unexpected for regular user)${NC}"
    else
      echo -e "     ✓ No admin role (correct)"
    fi
  fi
  
  echo ""
  return 0
}

# Main verification
echo "Testing connection to: ${KEYCLOAK_URL}"
echo "Realm: ${REALM}"
echo "Client: ${CLIENT_ID}"
echo ""

# Verify test user (regular)
verify_user "test" "Test.1234" "false"

# Verify test_admin user
verify_user "test_admin" "Test.1234" "true"

# Summary
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  Summary${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "✅ Verification complete"
echo ""
echo "📋 Next steps if issues found:"
echo "  1. Check if Keycloak is running: docker ps | grep keycloak"
echo "  2. Access Keycloak admin: ${KEYCLOAK_URL}/admin"
echo "  3. Create missing users in realm '${REALM}'"
echo "  4. Assign required roles (CORE_ROLE_ADMIN, CORE_ROLE_USER, etc.)"
echo ""
echo "📚 Documentation: e2e/config/test-users.md"
echo ""

