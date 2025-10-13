#!/usr/bin/env bash
# Post-Deployment Check - smoke testy po úspěšném deployu
set -Eeuo pipefail

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${BLUE}🔍 Running post-deployment checks...${NC}"
echo ""

CHECKS_PASSED=0
CHECKS_TOTAL=0
CHECKS_FAILED=0

# Function to run a check
run_check() {
    local name="$1"
    local command="$2"
    
    CHECKS_TOTAL=$((CHECKS_TOTAL + 1))
    echo -e "${YELLOW}[$CHECKS_TOTAL]${NC} $name"
    
    if eval "$command" > /dev/null 2>&1; then
        echo -e "    ${GREEN}✅ PASS${NC}"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
        return 0
    else
        echo -e "    ${RED}❌ FAIL${NC}"
        CHECKS_FAILED=$((CHECKS_FAILED + 1))
        return 1
    fi
}

# =============================================================================
# 1. Container Health Checks
# =============================================================================
echo -e "${BLUE}📦 Container Health:${NC}"

# Check all containers are running
run_check "All containers running" \
    "[ \$(docker ps --filter 'name=core-platform' --filter 'status=running' -q | wc -l) -ge 5 ]"

# Check no containers restarting
run_check "No containers restarting" \
    "! docker ps --format '{{.Status}}' --filter 'name=core-platform' | grep -q 'Restarting'"

# Check backend health
run_check "Backend health endpoint" \
    "curl -sf http://localhost:8080/actuator/health | jq -e '.status == \"UP\"'"

# Check Keycloak health
run_check "Keycloak health endpoint" \
    "curl -sf http://localhost:8081/health | jq -e '.status == \"UP\"'"

# Check database connectivity
run_check "Database connectivity" \
    "docker exec core-db pg_isready -U \${POSTGRES_USER:-platform}"

# =============================================================================
# 2. API Smoke Tests
# =============================================================================
echo ""
echo -e "${BLUE}🌐 API Endpoints:${NC}"

# Check API root
run_check "API root accessible" \
    "curl -sf http://localhost:8080/api | jq -e 'has(\"_links\")'"

# Check API docs
run_check "API docs accessible" \
    "curl -sf http://localhost:8080/swagger-ui/index.html -o /dev/null"

# Check actuator endpoints
run_check "Actuator info endpoint" \
    "curl -sf http://localhost:8080/actuator/info"

run_check "Actuator metrics endpoint" \
    "curl -sf http://localhost:8080/actuator/metrics"

# =============================================================================
# 3. Frontend Checks
# =============================================================================
echo ""
echo -e "${BLUE}🎨 Frontend:${NC}"

# Check frontend accessible via proxy
run_check "Frontend accessible (HTTPS)" \
    "curl -sfk https://core-platform.local/ -o /dev/null"

run_check "Admin frontend accessible" \
    "curl -sfk https://admin.core-platform.local/ -o /dev/null"

# =============================================================================
# 4. Observability Stack
# =============================================================================
echo ""
echo -e "${BLUE}📊 Observability:${NC}"

# Check Grafana
run_check "Grafana accessible" \
    "curl -sf http://localhost:3001/api/health | jq -e '.database == \"ok\"'"

# Check Loki
run_check "Loki ready" \
    "curl -sf http://localhost:3100/ready"

# Check Prometheus
run_check "Prometheus healthy" \
    "curl -sf http://localhost:9090/-/healthy -o /dev/null"

# =============================================================================
# 5. Keycloak Configuration
# =============================================================================
echo ""
echo -e "${BLUE}🔐 Keycloak:${NC}"

# Check realm exists
run_check "Core Platform realm exists" \
    "curl -sf http://localhost:8081/realms/core-platform/.well-known/openid-configuration | jq -e '.issuer'"

# Check admin console accessible
run_check "Keycloak admin console" \
    "curl -sf http://localhost:8081/admin/ -o /dev/null"

# =============================================================================
# 6. Optional: Quick Multitenancy Test
# =============================================================================
if [ "${RUN_FULL_TESTS:-false}" = "true" ]; then
    echo ""
    echo -e "${BLUE}🧪 Full Integration Tests:${NC}"
    
    # Run multitenancy smoke test
    if [ -f "tests/multitenancy_smoke.sh" ]; then
        echo -e "${YELLOW}Running multitenancy smoke tests...${NC}"
        if bash tests/multitenancy_smoke.sh; then
            echo -e "    ${GREEN}✅ PASS${NC}"
            CHECKS_PASSED=$((CHECKS_PASSED + 1))
        else
            echo -e "    ${RED}❌ FAIL${NC}"
            CHECKS_FAILED=$((CHECKS_FAILED + 1))
        fi
        CHECKS_TOTAL=$((CHECKS_TOTAL + 1))
    fi
    
    # Run streaming integration test
    if [ -f "tests/streaming_integration_test.sh" ]; then
        echo -e "${YELLOW}Running streaming integration tests...${NC}"
        if bash tests/streaming_integration_test.sh; then
            echo -e "    ${GREEN}✅ PASS${NC}"
            CHECKS_PASSED=$((CHECKS_PASSED + 1))
        else
            echo -e "    ${RED}❌ FAIL${NC}"
            CHECKS_FAILED=$((CHECKS_FAILED + 1))
        fi
        CHECKS_TOTAL=$((CHECKS_TOTAL + 1))
    fi
fi

# =============================================================================
# Summary
# =============================================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $CHECKS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ All checks passed! ($CHECKS_PASSED/$CHECKS_TOTAL)${NC}"
    echo ""
    echo "🎉 Environment is ready to use!"
    echo ""
    echo "📍 Access points:"
    echo "   Frontend:  https://core-platform.local/"
    echo "   Admin:     https://admin.core-platform.local/"
    echo "   API:       http://localhost:8080/api"
    echo "   Keycloak:  http://localhost:8081/admin/"
    echo "   Grafana:   http://localhost:3001/"
    exit 0
else
    echo -e "${RED}❌ Some checks failed: $CHECKS_FAILED/$CHECKS_TOTAL${NC}"
    echo -e "${GREEN}✅ Passed: $CHECKS_PASSED${NC}"
    echo ""
    echo "💡 Troubleshooting:"
    echo "   1. Check logs: make logs-errors"
    echo "   2. Check containers: docker ps"
    echo "   3. Check diagnostics: ls -lh diagnostics/"
    echo "   4. Run full tests: RUN_FULL_TESTS=true make verify"
    exit 1
fi
