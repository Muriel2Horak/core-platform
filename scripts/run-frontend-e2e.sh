#!/bin/bash

# Frontend E2E Tests Runner
# Prerequisites:
# 1. Backend must be running in test profile
# 2. Node modules installed (npm install)
# 3. Playwright browsers installed (npm run playwright:install)
#
# Usage:
#   ./scripts/run-frontend-e2e.sh [options]
#
# Options:
#   --headed        Run in headed mode (show browser)
#   --ui            Run with Playwright UI
#   --debug         Run with debug mode
#
# Examples:
#   ./scripts/run-frontend-e2e.sh                # Run all E2E tests (headless)
#   ./scripts/run-frontend-e2e.sh --headed       # Run with visible browser
#   ./scripts/run-frontend-e2e.sh --ui           # Run with Playwright UI

set -e

# Change to frontend directory
cd "$(dirname "$0")/../frontend"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}🎭 Running Frontend E2E Tests...${NC}"
echo ""

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}⚠️  node_modules not found. Running npm install...${NC}"
    npm install
fi

# Check if Playwright browsers are installed
if ! npx playwright --version > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Playwright browsers not found. Installing...${NC}"
    npm run playwright:install
fi

# Set default E2E_BASE_URL if not set
export E2E_BASE_URL=${E2E_BASE_URL:-http://localhost:8080}

# Check if backend is running
echo -e "${YELLOW}🔍 Checking if backend is running at ${E2E_BASE_URL}...${NC}"
if ! curl -s -o /dev/null -w "%{http_code}" "${E2E_BASE_URL}/actuator/health" | grep -q "200\|401"; then
    echo -e "${RED}❌ Backend is not running at ${E2E_BASE_URL}${NC}"
    echo ""
    echo "Please start backend first:"
    echo "  cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=test"
    echo ""
    exit 1
fi

echo -e "${GREEN}✅ Backend is running${NC}"
echo ""

# Parse arguments
MODE="headless"
if [[ "$*" == *"--headed"* ]]; then
    MODE="headed"
elif [[ "$*" == *"--ui"* ]]; then
    MODE="ui"
elif [[ "$*" == *"--debug"* ]]; then
    MODE="debug"
fi

# Run tests based on mode
case $MODE in
    headed)
        echo -e "${GREEN}Running in headed mode...${NC}"
        npm run test:e2e:headed
        ;;
    ui)
        echo -e "${GREEN}Opening Playwright UI...${NC}"
        npm run test:e2e:ui
        ;;
    debug)
        echo -e "${GREEN}Running in debug mode...${NC}"
        npx playwright test --debug
        ;;
    *)
        echo -e "${GREEN}Running in headless mode...${NC}"
        npm run test:e2e
        ;;
esac

# Check exit code
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ All E2E tests passed!${NC}"
    echo ""
    echo "📊 View HTML report:"
    echo "  cd frontend && npx playwright show-report"
else
    echo ""
    echo -e "${RED}❌ E2E tests failed!${NC}"
    echo ""
    echo "📊 View HTML report:"
    echo "  cd frontend && npx playwright show-report"
    exit 1
fi
