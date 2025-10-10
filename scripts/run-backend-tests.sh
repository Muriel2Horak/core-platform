#!/bin/bash

# Backend Integration Tests Runner
# Runs tests with test profile (H2, Caffeine, WireMock, no Docker)
#
# Usage:
#   ./scripts/run-backend-tests.sh [test-class]
#
# Examples:
#   ./scripts/run-backend-tests.sh                          # Run all tests
#   ./scripts/run-backend-tests.sh MonitoringQueryIT        # Run specific test

set -e

# Change to backend directory
cd "$(dirname "$0")/../backend"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}🧪 Running Backend Tests with Test Profile...${NC}"
echo ""

# Set test profile
export SPRING_PROFILES_ACTIVE=test

# Run tests
if [ -z "$1" ]; then
    # Run all tests
    ./mvnw test -Dspring.profiles.active=test
else
    # Run specific test
    ./mvnw test -Dtest="$1" -Dspring.profiles.active=test
fi

# Check exit code
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ All tests passed!${NC}"
else
    echo ""
    echo -e "${RED}❌ Tests failed!${NC}"
    exit 1
fi
