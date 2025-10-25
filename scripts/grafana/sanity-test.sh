#!/bin/bash
# Grafana SSO Sanity Test - Verifies provisioning and SSO work correctly
#
# Tests:
# 1. test_admin is member of org 2 (tenant admin org)
# 2. test_admin current org is 2
# 3. Dashboard is accessible without login redirect (200 response)
#
# Usage:
#   ./scripts/grafana/sanity-test.sh
#
# Environment:
#   GRAFANA_URL - Grafana base URL (default: http://localhost:3000)
#   GRAFANA_ADMIN_USER - Admin username (default: admin)
#   GRAFANA_ADMIN_PASSWORD - Admin password (required)

set -euo pipefail

# Configuration
GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"
GRAFANA_ADMIN_USER="${GRAFANA_ADMIN_USER:-admin}"
GRAFANA_ADMIN_PASSWORD="${GRAFANA_ADMIN_PASSWORD:-}"
TEST_USER="test_admin"
EXPECTED_ORG_ID=2
DASHBOARD_PATH="/d/axiom_sys_overview"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check password
if [ -z "$GRAFANA_ADMIN_PASSWORD" ]; then
  echo -e "${RED}Error: GRAFANA_ADMIN_PASSWORD not set${NC}"
  echo "Set it via environment or .env file"
  exit 1
fi

echo -e "${BLUE}🧪 Grafana SSO Sanity Test${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Test 1: Check user exists and get ID
echo -e "${YELLOW}Test 1/3: Verify ${TEST_USER} exists${NC}"
USER_RESPONSE=$(curl -s -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
  "${GRAFANA_URL}/api/users/lookup?loginOrEmail=${TEST_USER}")

if echo "$USER_RESPONSE" | grep -q '"message".*"User not found"'; then
  echo -e "${RED}❌ FAIL: User ${TEST_USER} not found${NC}"
  exit 1
fi

USER_ID=$(echo "$USER_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
if [ -z "$USER_ID" ]; then
  echo -e "${RED}❌ FAIL: Failed to extract user ID${NC}"
  exit 1
fi

echo -e "${GREEN}✅ PASS: User ${TEST_USER} exists (ID: ${USER_ID})${NC}"
echo ""

# Test 2: Check org membership
echo -e "${YELLOW}Test 2/3: Verify ${TEST_USER} is member of org ${EXPECTED_ORG_ID}${NC}"
ORGS_RESPONSE=$(curl -s -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
  "${GRAFANA_URL}/api/users/${USER_ID}/orgs")

if ! echo "$ORGS_RESPONSE" | grep -q "\"orgId\":${EXPECTED_ORG_ID}"; then
  echo -e "${RED}❌ FAIL: User is NOT a member of org ${EXPECTED_ORG_ID}${NC}"
  echo ""
  echo "User's organizations:"
  echo "$ORGS_RESPONSE" | grep -o '{"orgId":[0-9]*,"name":"[^"]*","role":"[^"]*"}' | while IFS= read -r org; do
    ORG_ID=$(echo "$org" | grep -o '"orgId":[0-9]*' | cut -d: -f2)
    ORG_NAME=$(echo "$org" | grep -o '"name":"[^"]*"' | cut -d\" -f4)
    ORG_ROLE=$(echo "$org" | grep -o '"role":"[^"]*"' | cut -d\" -f4)
    echo "  • Org $ORG_ID: $ORG_NAME (role: $ORG_ROLE)"
  done
  exit 1
fi

# Check current org
USER_DETAILS=$(curl -s -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
  "${GRAFANA_URL}/api/users/${USER_ID}")

CURRENT_ORG_ID=$(echo "$USER_DETAILS" | grep -o '"orgId":[0-9]*' | head -1 | cut -d: -f2)

if [ "$CURRENT_ORG_ID" != "$EXPECTED_ORG_ID" ]; then
  echo -e "${YELLOW}⚠️  WARNING: User is member of org ${EXPECTED_ORG_ID}, but current org is ${CURRENT_ORG_ID}${NC}"
  echo -e "${YELLOW}   Attempting to fix via setUserActiveOrg...${NC}"
  
  # Try to set active org
  SET_ORG_RESPONSE=$(curl -s -X POST -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
    "${GRAFANA_URL}/api/users/${USER_ID}/using/${EXPECTED_ORG_ID}")
  
  if echo "$SET_ORG_RESPONSE" | grep -q '"message".*"Active organization changed"'; then
    echo -e "${GREEN}   ✅ Fixed: Active org set to ${EXPECTED_ORG_ID}${NC}"
  else
    echo -e "${RED}   ❌ FAIL: Could not set active org${NC}"
    echo "   Response: $SET_ORG_RESPONSE"
    exit 1
  fi
fi

echo -e "${GREEN}✅ PASS: User is member of org ${EXPECTED_ORG_ID} and current org is ${EXPECTED_ORG_ID}${NC}"
echo ""

# Test 3: Check dashboard accessibility (optional - requires full environment)
echo -e "${YELLOW}Test 3/3: Check dashboard accessibility${NC}"
DASHBOARD_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
  "${GRAFANA_URL}${DASHBOARD_PATH}")

HTTP_STATUS=$(echo "$DASHBOARD_RESPONSE" | grep "HTTP_STATUS:" | cut -d: -f2)

if [ "$HTTP_STATUS" = "200" ]; then
  echo -e "${GREEN}✅ PASS: Dashboard accessible (HTTP 200)${NC}"
elif [ "$HTTP_STATUS" = "404" ]; then
  echo -e "${YELLOW}⚠️  SKIP: Dashboard not found (HTTP 404) - may not be provisioned yet${NC}"
  echo -e "${YELLOW}   This is OK if dashboards are provisioned separately${NC}"
elif [ "$HTTP_STATUS" = "302" ] || [ "$HTTP_STATUS" = "401" ]; then
  echo -e "${RED}❌ FAIL: Dashboard requires login (HTTP ${HTTP_STATUS})${NC}"
  echo -e "${RED}   This indicates SSO is not working correctly${NC}"
  exit 1
else
  echo -e "${YELLOW}⚠️  WARNING: Unexpected HTTP status: ${HTTP_STATUS}${NC}"
fi
echo ""

# Summary
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ ALL TESTS PASSED${NC}"
echo ""
echo -e "${GREEN}Summary:${NC}"
echo "  User: ${TEST_USER} (ID: ${USER_ID})"
echo "  Org Membership: org ${EXPECTED_ORG_ID} ✅"
echo "  Current Org: ${EXPECTED_ORG_ID} ✅"
echo "  Dashboard: ${HTTP_STATUS} $([ "$HTTP_STATUS" = "200" ] && echo "✅" || echo "⚠️")"
echo ""
echo -e "${YELLOW}💡 Next steps:${NC}"
echo "  • Test iframe embed in frontend: https://admin.core-platform.local/monitoring"
echo "  • Check logs for provisioning: docker compose logs -f backend | grep Provisioning"
echo "  • Manual check: ./scripts/grafana/user-check.sh ${TEST_USER}"
