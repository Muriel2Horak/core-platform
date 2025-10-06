#!/bin/bash
# Dev Container Environment Sanity Check
# Tests all critical services and configurations

set -e

echo "🧪 Dev Container Environment Sanity Check"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

FAILED=0

# Helper functions
check_pass() {
    echo -e "${GREEN}✅ $1${NC}"
}

check_fail() {
    echo -e "${RED}❌ $1${NC}"
    FAILED=$((FAILED + 1))
}

check_warn() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# 1. Check Docker Compose is running
echo "📦 Checking Docker Compose services..."
if docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml ps | grep -q "Up"; then
    check_pass "Docker Compose services are running"
else
    check_fail "Docker Compose services not running - run 'make dev:up' first"
fi
echo ""

# 2. Check database connectivity
echo "🗄️  Checking database..."
if docker exec core-db pg_isready -U core -d core >/dev/null 2>&1; then
    check_pass "Database is ready and accepting connections"
else
    check_fail "Database is not ready"
fi
echo ""

# 3. Check backend health
echo "☕ Checking backend..."
BACKEND_HEALTH=$(curl -s http://localhost:8080/actuator/health 2>/dev/null | jq -r '.status // "DOWN"')
if [ "$BACKEND_HEALTH" = "UP" ]; then
    check_pass "Backend health check: UP"
    
    # Check Java debug port
    if nc -z localhost 5005 2>/dev/null; then
        check_pass "Java debug port (5005) is accessible"
    else
        check_warn "Java debug port (5005) not accessible - debugging may not work"
    fi
else
    check_fail "Backend health check: $BACKEND_HEALTH"
fi
echo ""

# 4. Check frontend
echo "🌐 Checking frontend..."
if curl -s -o /dev/null -w "%{http_code}" https://core-platform.local/ --insecure 2>/dev/null | grep -q "200"; then
    check_pass "Frontend is accessible at https://core-platform.local/"
else
    check_fail "Frontend is not accessible"
fi
echo ""

# 5. Check Keycloak
echo "🔐 Checking Keycloak..."
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/health/ready 2>/dev/null | grep -q "200"; then
    check_pass "Keycloak is ready"
else
    check_fail "Keycloak is not ready"
fi
echo ""

# 6. Check file permissions (important for watch mode)
echo "📁 Checking file permissions..."
if [ -w frontend/src ]; then
    check_pass "Frontend source directory is writable"
else
    check_fail "Frontend source directory is not writable"
fi

if [ -w backend/src ]; then
    check_pass "Backend source directory is writable"
else
    check_fail "Backend source directory is not writable"
fi
echo ""

# 7. Check monitoring stack
echo "📊 Checking monitoring stack..."
if curl -s http://localhost:3001 >/dev/null 2>&1; then
    check_pass "Grafana is accessible at http://localhost:3001"
else
    check_warn "Grafana is not accessible (optional)"
fi

if curl -s http://localhost:3100/ready >/dev/null 2>&1; then
    check_pass "Loki is ready"
else
    check_warn "Loki is not ready (optional)"
fi
echo ""

# 8. Check volumes
echo "💾 Checking Docker volumes..."
if docker volume ls | grep -q "maven-cache"; then
    check_pass "Maven cache volume exists"
else
    check_warn "Maven cache volume not found - rebuilds may be slower"
fi
echo ""

# Final summary
echo "=========================================="
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 All critical checks passed!${NC}"
    echo ""
    echo "Your Dev Container is ready for development!"
    echo ""
    echo "Next steps:"
    echo "  • Edit code in frontend/src/ or backend/src/"
    echo "  • Changes will auto-rebuild (watch mode)"
    echo "  • Attach debugger to localhost:5005 (Java)"
    echo "  • View logs: make dev:logs"
    exit 0
else
    echo -e "${RED}⚠️  $FAILED check(s) failed${NC}"
    echo ""
    echo "Please fix the issues above before continuing."
    echo "Run 'make dev:up' to start services if not running."
    exit 1
fi
