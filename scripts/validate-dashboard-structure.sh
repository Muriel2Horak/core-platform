#!/usr/bin/env bash

# validate-dashboard-structure.sh
# Validates that dashboard JSON files are in proper subdirectories

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

DASHBOARD_ROOT="docker/grafana/provisioning/dashboards"
EXIT_CODE=0

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📋 VALIDATING DASHBOARD STRUCTURE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check if root directory exists
if [[ ! -d "$DASHBOARD_ROOT" ]]; then
  echo -e "${RED}✗ ERROR${NC} - Dashboard root directory not found: $DASHBOARD_ROOT"
  exit 1
fi

# Find any JSON files directly in the root
LOOSE_FILES=$(find "$DASHBOARD_ROOT" -maxdepth 1 -name "*.json" -type f 2>/dev/null || true)

if [[ -n "$LOOSE_FILES" ]]; then
  echo -e "${RED}✗ FAIL${NC} - Found dashboard JSON files in root directory:"
  echo ""
  echo "$LOOSE_FILES" | while read -r file; do
    echo "  - $(basename "$file")"
  done
  echo ""
  echo -e "${YELLOW}ℹ INFO${NC} - Dashboard JSON files must be in subdirectories:"
  echo "  - docker/grafana/provisioning/dashboards/custom/"
  echo "  - docker/grafana/provisioning/dashboards/system/"
  echo "  - docker/grafana/provisioning/dashboards/advanced/"
  echo "  - docker/grafana/provisioning/dashboards/streaming/"
  echo "  - docker/grafana/provisioning/dashboards/security/"
  echo "  - docker/grafana/provisioning/dashboards/audit/"
  echo "  - docker/grafana/provisioning/dashboards/monitoring-bff/"
  echo ""
  EXIT_CODE=1
else
  echo -e "${GREEN}✓ PASS${NC} - No loose JSON files in root directory"
fi

# Validate that expected subdirectories exist
EXPECTED_DIRS=(
  "custom"
  "system"
  "advanced"
  "streaming"
  "security"
  "audit"
  "monitoring-bff"
)

MISSING_DIRS=()
for dir in "${EXPECTED_DIRS[@]}"; do
  if [[ ! -d "$DASHBOARD_ROOT/$dir" ]]; then
    MISSING_DIRS+=("$dir")
  fi
done

if [[ ${#MISSING_DIRS[@]} -gt 0 ]]; then
  echo -e "${YELLOW}⚠ WARN${NC} - Expected directories missing:"
  for dir in "${MISSING_DIRS[@]}"; do
    echo "  - $DASHBOARD_ROOT/$dir"
  done
else
  echo -e "${GREEN}✓ PASS${NC} - All expected subdirectories exist"
fi

# Count dashboards in each subdirectory
echo ""
echo "📊 Dashboard counts per folder:"
for dir in "${EXPECTED_DIRS[@]}"; do
  if [[ -d "$DASHBOARD_ROOT/$dir" ]]; then
    COUNT=$(find "$DASHBOARD_ROOT/$dir" -name "*.json" -type f | wc -l | tr -d ' ')
    echo "  - $dir: $COUNT dashboard(s)"
  fi
done

echo ""
if [[ $EXIT_CODE -eq 0 ]]; then
  echo -e "${GREEN}✅ VALIDATION PASSED${NC}"
else
  echo -e "${RED}❌ VALIDATION FAILED${NC}"
fi

exit $EXIT_CODE
