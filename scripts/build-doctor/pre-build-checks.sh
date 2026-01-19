#!/usr/bin/env bash
set -euo pipefail

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="${BUILD_DOCTOR_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
cd "$PROJECT_ROOT"

echo -e "${GREEN}🔍 Build Doctor: Running pre-flight checks...${NC}"
echo ""

run_check() {
  local label="$1"
  local script="$2"

  echo "▶️  $label"
  if bash "$script"; then
    echo -e "${GREEN}✅ $label passed${NC}"
    echo ""
    return 0
  fi

  echo -e "${RED}❌ $label failed${NC}"
  echo ""
  return 1
}

run_check "Environment validation" "scripts/build-doctor/check-env.sh"
run_check "Docker daemon" "scripts/build-doctor/check-docker.sh"
run_check "Port availability" "scripts/build-doctor/check-ports.sh"
run_check "Disk space" "scripts/build-doctor/check-disk.sh"
run_check "Template sync" "scripts/build-doctor/check-templates.sh"

echo -e "${GREEN}🎉 All pre-flight checks passed${NC}"
