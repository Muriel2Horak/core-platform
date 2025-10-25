#!/bin/bash
# Grafana User Active Org Setter - Manually set user's active organization
#
# Usage:
#   ./scripts/grafana/user-set-org.sh <login_or_email> <org_id>
#
# Examples:
#   ./scripts/grafana/user-set-org.sh test_admin 2
#   ./scripts/grafana/user-set-org.sh admin@example.com 1
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
if [ $# -ne 2 ]; then
  echo "Usage: $0 <login_or_email> <org_id>"
  echo ""
  echo "Examples:"
  echo "  $0 test_admin 2"
  echo "  $0 admin@example.com 1"
  exit 1
fi

LOGIN_OR_EMAIL="$1"
TARGET_ORG_ID="$2"

# Validate org_id is numeric
if ! [[ "$TARGET_ORG_ID" =~ ^[0-9]+$ ]]; then
  echo -e "${RED}Error: org_id must be numeric (got: ${TARGET_ORG_ID})${NC}"
  exit 1
fi

# Check password
if [ -z "$GRAFANA_ADMIN_PASSWORD" ]; then
  echo -e "${RED}Error: GRAFANA_ADMIN_PASSWORD not set${NC}"
  echo "Set it via environment or .env file"
  exit 1
fi

echo -e "${BLUE}🔄 Grafana Set Active Organization${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Step 1: Lookup user
echo -e "${YELLOW}Step 1/4: Looking up user '${LOGIN_OR_EMAIL}'${NC}"
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

echo -e "${GREEN}✅ User found: ID=${USER_ID}${NC}"
echo ""

# Step 2: Check current org
echo -e "${YELLOW}Step 2/4: Checking current active org${NC}"
USER_DETAILS=$(curl -s -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
  "${GRAFANA_URL}/api/users/${USER_ID}")

CURRENT_ORG_ID=$(echo "$USER_DETAILS" | grep -o '"orgId":[0-9]*' | head -1 | cut -d: -f2)
echo -e "${BLUE}Current org: ${CURRENT_ORG_ID}${NC}"

if [ "$CURRENT_ORG_ID" = "$TARGET_ORG_ID" ]; then
  echo -e "${GREEN}✅ User already in org ${TARGET_ORG_ID} - no change needed${NC}"
  exit 0
fi
echo ""

# Step 3: Verify user is member of target org
echo -e "${YELLOW}Step 3/4: Verifying user is member of org ${TARGET_ORG_ID}${NC}"
ORGS_RESPONSE=$(curl -s -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
  "${GRAFANA_URL}/api/users/${USER_ID}/orgs")

if ! echo "$ORGS_RESPONSE" | grep -q "\"orgId\":${TARGET_ORG_ID}"; then
  echo -e "${RED}❌ User is NOT a member of org ${TARGET_ORG_ID}${NC}"
  echo ""
  echo -e "${YELLOW}User's organizations:${NC}"
  echo "$ORGS_RESPONSE" | grep -o '{"orgId":[0-9]*,"name":"[^"]*","role":"[^"]*"}' | while IFS= read -r org; do
    ORG_ID=$(echo "$org" | grep -o '"orgId":[0-9]*' | cut -d: -f2)
    ORG_NAME=$(echo "$org" | grep -o '"name":"[^"]*"' | cut -d\" -f4)
    ORG_ROLE=$(echo "$org" | grep -o '"role":"[^"]*"' | cut -d\" -f4)
    echo "  • Org $ORG_ID: $ORG_NAME (role: $ORG_ROLE)"
  done
  exit 1
fi

echo -e "${GREEN}✅ User is member of org ${TARGET_ORG_ID}${NC}"
echo ""

# Step 4: Set active org
echo -e "${YELLOW}Step 4/4: Setting active org to ${TARGET_ORG_ID}${NC}"
# CRITICAL: Use /api/users/{userId}/using/{orgId} (plural 'users' - admin endpoint)
# NOT /api/user/using/{orgId} (singular 'user' - affects current user)
SET_ORG_RESPONSE=$(curl -s -X POST -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
  "${GRAFANA_URL}/api/users/${USER_ID}/using/${TARGET_ORG_ID}")

if echo "$SET_ORG_RESPONSE" | grep -q '"message".*"Active organization changed"'; then
  echo -e "${GREEN}✅ SUCCESS: Active org set to ${TARGET_ORG_ID}${NC}"
  echo ""
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${GREEN}Summary:${NC}"
  echo "  User ID: $USER_ID"
  echo "  Old Org: $CURRENT_ORG_ID"
  echo "  New Org: $TARGET_ORG_ID"
  echo ""
  echo -e "${YELLOW}💡 Verify: ./scripts/grafana/user-check.sh ${LOGIN_OR_EMAIL}${NC}"
else
  echo -e "${RED}❌ FAILED to set active org${NC}"
  echo "Response: $SET_ORG_RESPONSE"
  exit 1
fi
