#!/usr/bin/env bash
# Pre-build test runner - spouští unit testy před Docker buildem
set -Eeuo pipefail

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

COMPONENT="${1:-all}"
SKIP_TESTS="${SKIP_TESTS:-false}"

if [ "$SKIP_TESTS" = "true" ]; then
    echo -e "${YELLOW}⚠️  SKIP_TESTS=true - Skipping all tests${NC}"
    exit 0
fi

echo ""
echo -e "${BLUE}🧪 Running pre-build tests for: $COMPONENT${NC}"
echo ""

TESTS_FAILED=false

# =============================================================================
# Backend Tests
# =============================================================================
if [ "$COMPONENT" = "backend" ] || [ "$COMPONENT" = "all" ]; then
    echo -e "${YELLOW}[1/2]${NC} Backend unit tests..."
    
    if [ -f "backend/pom.xml" ]; then
        cd backend
        
        # Run tests with quiet mode but show failures
        if ./mvnw test -q 2>&1 | tee /tmp/backend-test.log; then
            TESTS_COUNT=$(grep -o "Tests run: [0-9]*" /tmp/backend-test.log | tail -1 | grep -o "[0-9]*" || echo "0")
            echo -e "    ${GREEN}✅ PASS${NC} ($TESTS_COUNT tests)"
        else
            echo -e "    ${RED}❌ FAIL${NC}"
            echo ""
            echo "Last 30 lines of test output:"
            tail -30 /tmp/backend-test.log
            TESTS_FAILED=true
        fi
        
        cd ..
    else
        echo -e "    ${YELLOW}⚠️  SKIP${NC} (backend/pom.xml not found)"
    fi
fi

# =============================================================================
# Frontend Tests
# =============================================================================
if [ "$COMPONENT" = "frontend" ] || [ "$COMPONENT" = "all" ]; then
    echo -e "${YELLOW}[2/2]${NC} Frontend tests..."
    
    if [ -f "frontend/package.json" ]; then
        cd frontend
        
        # Check if test script exists
        if npm run | grep -q "test"; then
            # Run tests (Vitest/Jest)
            if npm test -- --run 2>&1 | tee /tmp/frontend-test.log; then
                TESTS_COUNT=$(grep -oE "[0-9]+ (test|passed)" /tmp/frontend-test.log | head -1 | grep -o "[0-9]*" || echo "0")
                echo -e "    ${GREEN}✅ PASS${NC} ($TESTS_COUNT tests)"
            else
                echo -e "    ${RED}❌ FAIL${NC}"
                echo ""
                echo "Last 30 lines of test output:"
                tail -30 /tmp/frontend-test.log
                TESTS_FAILED=true
            fi
        else
            echo -e "    ${YELLOW}⚠️  SKIP${NC} (no test script in package.json)"
        fi
        
        cd ..
    else
        echo -e "    ${YELLOW}⚠️  SKIP${NC} (frontend/package.json not found)"
    fi
fi

# =============================================================================
# Summary
# =============================================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ "$TESTS_FAILED" = "true" ]; then
    echo -e "${RED}❌ Pre-build tests FAILED${NC}"
    echo ""
    echo "💡 Options:"
    echo "   1. Fix the failing tests"
    echo "   2. Run with SKIP_TESTS=true to bypass (NOT RECOMMENDED)"
    echo "      Example: SKIP_TESTS=true make rebuild"
    echo ""
    exit 1
else
    echo -e "${GREEN}✅ All pre-build tests PASSED${NC}"
    echo ""
    echo "🏗️  Proceeding with Docker build..."
    exit 0
fi
