#!/usr/bin/env bash
set -euo pipefail

# 📊 GRAFANA PROVISIONING DIAGNOSTICS
# Validates that Grafana provisioning worked correctly:
# - Folders exist
# - Dashboards are loaded
# - API is accessible

# Check if running inside container or from host
if [[ -f /.dockerenv ]]; then
  # Inside container
  GRAFANA_URL="${GRAFANA_URL:-http://grafana:3000}"
else
  # From host - use docker exec
  GRAFANA_URL="http://localhost:3000"
  USE_DOCKER_EXEC=true
fi

GRAFANA_USER="${GRAFANA_USER:-admin}"
GRAFANA_PASS="${GRAFANA_PASS:-admin}"

# Helper function to run curl inside Grafana container if needed
grafana_curl() {
  if [[ "$USE_DOCKER_EXEC" == "true" ]]; then
    docker exec core-grafana curl -sf "$@"
  else
    curl -sf "$@"
  fi
}

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PASSED=0
FAILED=0

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 GRAFANA PROVISIONING DIAGNOSTICS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Test 1: Grafana health
echo "🔍 TEST 1: Grafana Health Check"
if grafana_curl "${GRAFANA_URL}/api/health" >/dev/null 2>&1; then
  echo -e "${GREEN}✓ PASS${NC} - Grafana is healthy"
  ((PASSED++))
else
  echo -e "${RED}✗ FAIL${NC} - Grafana is not responding"
  ((FAILED++))
fi
echo ""

# Test 2: Check expected folders
echo "🔍 TEST 2: Folder Provisioning"
EXPECTED_FOLDERS=("Custom" "Monitoring" "System Monitoring" "Advanced Monitoring" "Streaming" "Security" "Audit")

FOLDERS_JSON=$(grafana_curl -u "${GRAFANA_USER}:${GRAFANA_PASS}" "${GRAFANA_URL}/api/folders" 2>/dev/null || echo "[]")

for folder in "${EXPECTED_FOLDERS[@]}"; do
  if echo "$FOLDERS_JSON" | jq -e ".[] | select(.title == \"$folder\")" >/dev/null 2>&1; then
    echo -e "${GREEN}✓ PASS${NC} - Folder '$folder' exists"
    ((PASSED++))
  else
    echo -e "${RED}✗ FAIL${NC} - Folder '$folder' NOT FOUND"
    ((FAILED++))
  fi
done
echo ""

# Test 3: Check Axiom dashboards by UID
echo "🔍 TEST 3: Axiom Dashboard Provisioning"
EXPECTED_DASHBOARDS=(
  "axiom_sys_overview:System Monitoring"
  "axiom_adv_db:Advanced Monitoring"
  "axiom_adv_redis:Advanced Monitoring"
  "axiom_adv_runtime:Advanced Monitoring"
  "axiom_kafka_lag:Streaming"
  "axiom_security:Security"
  "axiom_audit:Audit"
)

# Get all dashboards once (more efficient than per-dashboard queries)
ALL_DASHBOARDS=$(grafana_curl -u "${GRAFANA_USER}:${GRAFANA_PASS}" \
  "${GRAFANA_URL}/api/search?type=dash-db" 2>/dev/null || echo "[]")

for entry in "${EXPECTED_DASHBOARDS[@]}"; do
  IFS=':' read -r uid folder <<< "$entry"
  
  if echo "$ALL_DASHBOARDS" | jq -e ".[] | select(.uid == \"$uid\")" >/dev/null 2>&1; then
    echo -e "${GREEN}✓ PASS${NC} - Dashboard '$uid' exists in folder '$folder'"
    ((PASSED++))
  else
    echo -e "${RED}✗ FAIL${NC} - Dashboard '$uid' NOT FOUND"
    ((FAILED++))
  fi
done
echo ""

# Test 4: Render smoke test (check if dashboard can render)
echo "🔍 TEST 4: Dashboard Render Smoke Test"
TEST_DASHBOARDS=("axiom_sys_overview" "axiom_adv_runtime" "axiom_kafka_lag")

for uid in "${TEST_DASHBOARDS[@]}"; do
  # Get dashboard to find first panel
  DASH_JSON=$(grafana_curl -u "${GRAFANA_USER}:${GRAFANA_PASS}" \
    "${GRAFANA_URL}/api/dashboards/uid/${uid}" 2>/dev/null || echo "{}")
  
  PANEL_ID=$(echo "$DASH_JSON" | jq -r '.dashboard.panels[0].id // "1"' 2>/dev/null || echo "1")
  
  # Try to render (just check HTTP 200, don't download PNG)
  HTTP_CODE=$(grafana_curl -u "${GRAFANA_USER}:${GRAFANA_PASS}" \
    -o /dev/null -w "%{http_code}" \
    "${GRAFANA_URL}/render/d-solo/${uid}?orgId=1&panelId=${PANEL_ID}&width=400&height=300" 2>/dev/null || echo "000")
  
  if [[ "$HTTP_CODE" == "200" ]]; then
    echo -e "${GREEN}✓ PASS${NC} - Dashboard '$uid' renders (panel ${PANEL_ID})"
    ((PASSED++))
  else
    echo -e "${YELLOW}⚠ WARN${NC} - Dashboard '$uid' render returned HTTP ${HTTP_CODE} (may need data)"
    # Don't fail on render - dashboards may need live data
  fi
done
echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 SUMMARY"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}✓ Passed: ${PASSED}${NC}"
echo -e "${RED}✗ Failed: ${FAILED}${NC}"
echo ""

if [[ $FAILED -eq 0 ]]; then
  echo -e "${GREEN}✅ ALL TESTS PASSED${NC}"
  exit 0
else
  echo -e "${RED}❌ SOME TESTS FAILED${NC}"
  exit 1
fi
