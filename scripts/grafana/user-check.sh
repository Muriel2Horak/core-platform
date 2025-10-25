#!/bin/bash
# Grafana User Diagnostics - Check user info, org membership, and current org
#
# Usage:
#   ./scripts/grafana/user-check.sh <login_or_email>
#
# Examples:
#   ./scripts/grafana/user-check.sh test_admin
#   ./scripts/grafana/user-check.sh admin@example.com
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

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check arguments
if [ $# -ne 1 ]; then
  echo "Usage: $0 <login_or_email>"
  echo ""
  echo "Examples:"
  echo "  $0 test_admin"
  echo "  $0 admin@example.com"
  exit 1
fi

LOGIN_OR_EMAIL="$1"

# Check password
if [ -z "$GRAFANA_ADMIN_PASSWORD" ]; then
  echo -e "${RED}Error: GRAFANA_ADMIN_PASSWORD not set${NC}"
  echo "Set it via environment or .env file"
  exit 1
fi

echo -e "${BLUE}🔍 Grafana User Diagnostics${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Step 1: Lookup user
echo -e "${YELLOW}Step 1/3: Looking up user '${LOGIN_OR_EMAIL}'${NC}"
USER_RESPONSE=$(curl -s -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
  "${GRAFANA_URL}/api/users/lookup?loginOrEmail=${LOGIN_OR_EMAIL}")

# Check if user exists
if echo "$USER_RESPONSE" | grep -q '"message".*"User not found"'; then
  echo -e "${RED}❌ User not found: ${LOGIN_OR_EMAIL}${NC}"
  exit 1
fi

# Extract user ID
USER_ID=$(echo "$USER_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
if [ -z "$USER_ID" ]; then
  echo -e "${RED}❌ Failed to extract user ID from response${NC}"
  echo "$USER_RESPONSE"
  exit 1
fi

echo -e "${GREEN}✅ User found:${NC}"
echo "$USER_RESPONSE" | grep -o '"id":[0-9]*,"email":"[^"]*","name":"[^"]*","login":"[^"]*"' | \
  sed 's/"id":/  ID: /; s/,"email":"/\n  Email: /; s/,"name":"/\n  Name: /; s/,"login":"/\n  Login: /; s/"$//'
echo ""

# Step 2: Get user's orgs
echo -e "${YELLOW}Step 2/3: Fetching user's organizations${NC}"
ORGS_RESPONSE=$(curl -s -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
  "${GRAFANA_URL}/api/users/${USER_ID}/orgs")

echo -e "${GREEN}✅ User is member of these organizations:${NC}"
echo "$ORGS_RESPONSE" | grep -o '{"orgId":[0-9]*,"name":"[^"]*","role":"[^"]*"}' | while IFS= read -r org; do
  ORG_ID=$(echo "$org" | grep -o '"orgId":[0-9]*' | cut -d: -f2)
  ORG_NAME=$(echo "$org" | grep -o '"name":"[^"]*"' | cut -d\" -f4)
  ORG_ROLE=$(echo "$org" | grep -o '"role":"[^"]*"' | cut -d\" -f4)
  echo "  • Org $ORG_ID: $ORG_NAME (role: $ORG_ROLE)"
done
echo ""

# Step 3: Get current org
echo -e "${YELLOW}Step 3/3: Checking current active organization${NC}"
# Get user's full details which includes current org
USER_DETAILS=$(curl -s -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
  "${GRAFANA_URL}/api/users/${USER_ID}")

CURRENT_ORG_ID=$(echo "$USER_DETAILS" | grep -o '"orgId":[0-9]*' | head -1 | cut -d: -f2)
if [ -z "$CURRENT_ORG_ID" ]; then
  echo -e "${RED}❌ Failed to extract current org ID${NC}"
  echo "$USER_DETAILS"
  exit 1
fi

echo -e "${GREEN}✅ Current active organization:${NC}"
echo "  Org ID: $CURRENT_ORG_ID"
echo ""

# Summary
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}Summary:${NC}"
echo "  User ID: $USER_ID"
echo "  Current Org: $CURRENT_ORG_ID"
echo "  Total Orgs: $(echo "$ORGS_RESPONSE" | grep -o '"orgId":' | wc -l | tr -d ' ')"
echo ""

# Expected state check (for test_admin)
if [ "$LOGIN_OR_EMAIL" = "test_admin" ]; then
  if [ "$CURRENT_ORG_ID" = "2" ]; then
    echo -e "${GREEN}✅ PASS: test_admin current org is 2 (expected for tenant admin)${NC}"
  else
    echo -e "${RED}❌ FAIL: test_admin current org is $CURRENT_ORG_ID (expected 2)${NC}"
    echo -e "${YELLOW}💡 Fix: Run ./scripts/grafana/user-set-org.sh test_admin 2${NC}"
  fi
fi
