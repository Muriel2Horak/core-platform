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
STEP_NUM="${2:-0}"  # Step number for progress tracking (0 = no tracking)
SKIP_TESTS="${SKIP_TESTS:-false}"
# Třídy testů, které skipnout (oddělené čárkou)
SKIP_TEST_CLASSES="${SKIP_TEST_CLASSES:-}"

LOG_DIR="diagnostics/tests"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$LOG_DIR"

# Initialize log paths
BACKEND_LOG=""
FRONTEND_LOG=""

if [ "$SKIP_TESTS" = "true" ]; then
    echo -e "${YELLOW}⚠️  SKIP_TESTS=true - Skipping all tests${NC}"
    exit 0
fi

echo ""
echo -e "${BLUE}🧪 Running pre-build tests for: $COMPONENT${NC}"
if [ -n "$SKIP_TEST_CLASSES" ]; then
    echo -e "${YELLOW}   (Skipping: $SKIP_TEST_CLASSES)${NC}"
fi
echo ""

TESTS_FAILED=false

# =============================================================================
# Backend Tests
# =============================================================================
if [ "$COMPONENT" = "backend" ] || [ "$COMPONENT" = "all" ]; then
    echo -e "${YELLOW}[1/2]${NC} Backend unit tests..."

    if [ -f "backend/pom.xml" ]; then
        cd backend

        BACKEND_LOG="../$LOG_DIR/backend-${TIMESTAMP}.log"

        # Build test pattern - match *Test, *Tests, *IT, *IntegrationTest
        TEST_PATTERN=""
        if [ -n "$SKIP_TEST_CLASSES" ]; then
            IFS=',' read -ra CLASSES <<< "$SKIP_TEST_CLASSES"
            for CLASS in "${CLASSES[@]}"; do
                TRIMMED="$(echo "$CLASS" | xargs)"
                [ -z "$TRIMMED" ] && continue
                if [ -z "$TEST_PATTERN" ]; then
                    TEST_PATTERN="!$TRIMMED"
                else
                    TEST_PATTERN="$TEST_PATTERN,!$TRIMMED"
                fi
            done
        fi

        # Maven args - use -q (quiet) only when NOT tracking progress
        # Parser needs [INFO] Running lines to count tests
        if [ "$STEP_NUM" -gt 0 ]; then
            MAVEN_ARGS=("-DtrimStackTrace=true" "-Dsurefire.printSummary=true" "-Dsurefire.reportFormat=brief" "-Dmaven.test.failure.ignore=false")
        else
            MAVEN_ARGS=("-q" "-DtrimStackTrace=true" "-Dsurefire.printSummary=true" "-Dsurefire.reportFormat=brief" "-Dmaven.test.failure.ignore=false")
        fi
        
        if [ -n "$TEST_PATTERN" ]; then
            MAVEN_ARGS+=("-Dtest=$TEST_PATTERN")
        fi
        MAVEN_ARGS+=("test")

        # Disable Testcontainers checks that open Safari on macOS
        export TESTCONTAINERS_CHECKS_DISABLE=true
        export TESTCONTAINERS_RYUK_DISABLED=true
        
        set +e  # Temporarily disable exit on error
        if [ "$STEP_NUM" -gt 0 ]; then
            # With real-time progress tracking (absolute path from workspace root)
            # Use stdbuf/gstdbuf for line-buffered output so parser gets lines immediately
            # On macOS, stdbuf is called gstdbuf (from coreutils)
            STDBUF_CMD=""
            if command -v stdbuf &> /dev/null; then
                STDBUF_CMD="stdbuf -oL"
            elif command -v gstdbuf &> /dev/null; then
                STDBUF_CMD="gstdbuf -oL"
            fi
            
            if [ -n "$STDBUF_CMD" ]; then
                $STDBUF_CMD ./mvnw "${MAVEN_ARGS[@]}" 2>&1 | $STDBUF_CMD tee "$BACKEND_LOG" | bash ../scripts/build/test-progress-parser.sh "$STEP_NUM" "backend"
            else
                # Fallback without stdbuf - output may be buffered
                ./mvnw "${MAVEN_ARGS[@]}" 2>&1 | tee "$BACKEND_LOG" | bash ../scripts/build/test-progress-parser.sh "$STEP_NUM" "backend"
            fi
            TEST_EXIT=$?
        else
            # Without progress tracking (fallback)
            ./mvnw "${MAVEN_ARGS[@]}" 2>&1 | tee "$BACKEND_LOG"
            TEST_EXIT=$?
        fi
        set -e
        
        # Parse and show summary
        if grep -q "Tests run:" "$BACKEND_LOG" 2>/dev/null; then
            SUMMARY_LINE=$(grep -E "Tests run:" "$BACKEND_LOG" | tail -1)
            echo "    📊 $SUMMARY_LINE"
        fi
        
        if [ $TEST_EXIT -eq 0 ]; then
            echo -e "    ${GREEN}✅ PASS${NC}"
        else
            echo -e "    ${RED}❌ FAIL${NC}"
            echo ""
            echo "    🔍 Full log: $BACKEND_LOG"
            if [ -f "$BACKEND_LOG" ]; then
                tail -40 "$BACKEND_LOG"
            else
                echo "    (log file not found)"
            fi
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
        
        FRONTEND_LOG="../$LOG_DIR/frontend-${TIMESTAMP}.log"

        # Check if test script exists
        if npm run | grep -q "test"; then
            # Run tests (Vitest/Jest) - check based on output, not exit code
            set +e
            if [ "$STEP_NUM" -gt 0 ]; then
                # With real-time progress tracking (absolute path from workspace root)
                npm test -- --run 2>&1 | tee "$FRONTEND_LOG" | bash ../scripts/build/test-progress-parser.sh "$STEP_NUM" "frontend"
            else
                # Without progress tracking (fallback)
                npm test -- --run 2>&1 | tee "$FRONTEND_LOG"
            fi
            set -e
            
            # Check if tests passed based on output
            if grep -q "Test Files.*passed" "$FRONTEND_LOG" && ! grep -q "FAIL" "$FRONTEND_LOG"; then
                SUMMARY_LINE=$(grep -E "Test Files" "$FRONTEND_LOG" | tail -1)
                if [ -n "$SUMMARY_LINE" ]; then
                    echo -e "    ${GREEN}✅ PASS${NC} $SUMMARY_LINE"
                else
                    echo -e "    ${GREEN}✅ PASS${NC}"
                fi
            else
                echo -e "    ${RED}❌ FAIL${NC}"
                echo ""
                echo "    🔍 Full log: $FRONTEND_LOG"
                if [ -f "$FRONTEND_LOG" ]; then
                    tail -40 "$FRONTEND_LOG"
                else
                    echo "    (log file not found)"
                fi
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
# Summary & Error Analysis
# =============================================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ "$TESTS_FAILED" = "true" ]; then
    echo -e "${RED}❌ Pre-build tests FAILED${NC}"
    echo ""
    
    # Analyze errors and create summary for Copilot
    ERROR_SUMMARY="$LOG_DIR/error-summary-${TIMESTAMP}.md"
    
    {
        echo "# Test Failure Analysis - $TIMESTAMP"
        echo ""
        echo "## 📋 Summary"
        echo ""
        
        # Backend errors
        if [ -f "$BACKEND_LOG" ]; then
            echo "### Backend Tests"
            FAILED_TESTS=$(grep -E "\[ERROR\].*Test.*FAILURE" "$BACKEND_LOG" 2>/dev/null | wc -l | xargs)
            ERROR_TESTS=$(grep -E "\[ERROR\].*Test.*ERROR" "$BACKEND_LOG" 2>/dev/null | wc -l | xargs)
            echo "- Failed: $FAILED_TESTS"
            echo "- Errors: $ERROR_TESTS"
            echo "- Log: \`$BACKEND_LOG\`"
            echo ""
            
            # Extract failed test names
            echo "#### Failed Tests:"
            grep -E "\[ERROR\].*-- Time elapsed:" "$BACKEND_LOG" 2>/dev/null | sed 's/\[ERROR\] /- /' || echo "- (parsing failed)"
            echo ""
            
            # Extract key error messages
            echo "#### Key Errors:"
            echo "\`\`\`"
            grep -A 3 "java.lang.*Exception\|AssertionError\|ERROR" "$BACKEND_LOG" 2>/dev/null | head -50
            echo "\`\`\`"
            echo ""
        fi
        
        # Frontend errors
        if [ -f "$FRONTEND_LOG" ]; then
            echo "### Frontend Tests"
            echo "- Log: \`$FRONTEND_LOG\`"
            echo ""
            
            echo "#### Failed Tests:"
            grep -E "FAIL|✖" "$FRONTEND_LOG" 2>/dev/null | head -20 | sed 's/^/- /'
            echo ""
        fi
        
        echo "## 🤖 Action Required"
        echo ""
        echo "**For GitHub Copilot:**"
        echo "1. Analyze the errors above"
        echo "2. Identify root cause (integration test timing, missing mocks, etc.)"
        echo "3. Propose fix or suggest adding to SKIP_TEST_CLASSES if it's a known flaky test"
        echo ""
        echo "**Quick Skip (if needed):**"
        echo "\`\`\`bash"
        echo "SKIP_TEST_CLASSES=\"TestClassName\" make rebuild"
        echo "\`\`\`"
        
    } > "$ERROR_SUMMARY"
    
    echo "📝 Error analysis saved to: $ERROR_SUMMARY"
    echo ""
    echo -e "${YELLOW}🤖 GitHub Copilot - please analyze:${NC}"
    cat "$ERROR_SUMMARY"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    exit 1
else
    echo -e "${GREEN}✅ All pre-build tests PASSED${NC}"
    echo ""
    
    # Show logs location even on success
    echo "📁 Test logs saved to:"
    [ -f "$BACKEND_LOG" ] && echo "   - Backend:  $BACKEND_LOG"
    [ -f "$FRONTEND_LOG" ] && echo "   - Frontend: $FRONTEND_LOG"
    echo ""
    echo "🏗️  Proceeding with Docker build..."
    exit 0
fi
