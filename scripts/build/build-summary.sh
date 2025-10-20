#!/usr/bin/env bash
# Build Summary - vytvoří přehledné shrnutí build procesu
set -Eeuo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

LOG_FILE="${1:-diagnostics/build.log}"
START_TIME="${2:-}"
BUILD_TYPE="${3:-rebuild}"  # clean, rebuild, clean-fast

# Calculate duration
if [ -n "$START_TIME" ]; then
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    MINUTES=$((DURATION / 60))
    SECONDS=$((DURATION % 60))
    DURATION_STR="${MINUTES}m ${SECONDS}s"
else
    DURATION_STR="unknown"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${BOLD}📊 BUILD SUMMARY${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Build info
echo -e "${CYAN}🏗️  Build Info:${NC}"
echo "   Type: $BUILD_TYPE"
echo "   Duration: $DURATION_STR"
echo "   Log: $LOG_FILE"
echo ""

# Parse log file for key events
echo -e "${CYAN}📦 Build Steps:${NC}"

# Check what was built/processed
if grep -q "Building Keycloak image" "$LOG_FILE" 2>/dev/null; then
    if grep -q "exporting to image" "$LOG_FILE" 2>/dev/null; then
        echo -e "   ${GREEN}✅${NC} Keycloak image built"
    else
        echo -e "   ${RED}❌${NC} Keycloak image failed"
    fi
fi

if grep -q "building with .* instance" "$LOG_FILE" 2>/dev/null; then
    # Count Docker builds
    DOCKER_BUILDS=$(grep -c "building with .* instance" "$LOG_FILE" 2>/dev/null || echo "0")
    echo "   🐳 Docker builds attempted: $DOCKER_BUILDS"
fi

if grep -q "Starting Docker Compose" "$LOG_FILE" 2>/dev/null; then
    echo -e "   ${GREEN}✅${NC} Docker Compose started"
fi

if grep -q "Backend is ready" "$LOG_FILE" 2>/dev/null; then
    echo -e "   ${GREEN}✅${NC} Backend became ready"
elif grep -q "Backend failed to become ready" "$LOG_FILE" 2>/dev/null; then
    echo -e "   ${RED}❌${NC} Backend failed to start"
elif grep -q "Waiting for backend" "$LOG_FILE" 2>/dev/null; then
    echo -e "   ${YELLOW}⏳${NC} Backend startup in progress"
fi

echo ""

# Check for errors
echo -e "${CYAN}🔍 Error Analysis:${NC}"

ERROR_PATTERNS=(
    "503 Service Unavailable:failed to fetch oauth token"
    "502 Bad Gateway:quay.io manifest"
    "ERROR:general error"
    "failed to solve:build failed"
    "Container .* failed:container crash"
)

ERRORS_FOUND=0
for pattern in "${ERROR_PATTERNS[@]}"; do
    IFS=':' read -r search_term description <<< "$pattern"
    if grep -q "$search_term" "$LOG_FILE" 2>/dev/null; then
        echo -e "   ${RED}❌${NC} $description"
        ERRORS_FOUND=$((ERRORS_FOUND + 1))
    fi
done

if [ $ERRORS_FOUND -eq 0 ]; then
    echo -e "   ${GREEN}✅${NC} No critical errors found"
fi

echo ""

# Docker registry issues
if grep -q "503 Service Unavailable" "$LOG_FILE" 2>/dev/null || \
   grep -q "502 Bad Gateway" "$LOG_FILE" 2>/dev/null; then
    echo -e "${YELLOW}⚠️  Docker Registry Issues Detected:${NC}"
    
    if grep -q "auth.docker.io" "$LOG_FILE" 2>/dev/null; then
        echo "   🐳 Docker Hub (docker.io) - unavailable"
    fi
    
    if grep -q "quay.io" "$LOG_FILE" 2>/dev/null; then
        echo "   📦 Quay.io - unavailable"
    fi
    
    echo ""
    echo -e "${CYAN}💡 Recommendation:${NC}"
    echo "   Docker registries are experiencing outages (intermittent)."
    echo "   Wait 5-10 minutes and retry: ${BOLD}make clean${NC}"
    echo ""
fi

# Current container status
echo -e "${CYAN}🐳 Container Status:${NC}"
docker ps --filter "name=core-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null | \
    awk 'NR==1 {print "   " $0; next} {print "   " $0}' || echo "   (unable to fetch)"

echo ""

# Health summary
echo -e "${CYAN}🏥 Health Status:${NC}"
HEALTHY=$(docker ps --filter "name=core-" --filter "health=healthy" --format "{{.Names}}" 2>/dev/null | wc -l || echo "0")
UNHEALTHY=$(docker ps --filter "name=core-" --filter "health=unhealthy" --format "{{.Names}}" 2>/dev/null | wc -l || echo "0")
TOTAL=$(docker ps --filter "name=core-" --format "{{.Names}}" 2>/dev/null | wc -l || echo "0")

if [ "$TOTAL" -gt 0 ]; then
    echo "   Healthy: $HEALTHY / $TOTAL"
    if [ "$UNHEALTHY" -gt 0 ]; then
        echo -e "   ${RED}Unhealthy: $UNHEALTHY${NC}"
        docker ps --filter "name=core-" --filter "health=unhealthy" --format "      - {{.Names}}" 2>/dev/null
    fi
else
    echo -e "   ${RED}No containers running${NC}"
fi

echo ""

# Images created
echo -e "${CYAN}📦 Images:${NC}"
docker images --filter "reference=core-platform/*" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedSince}}" 2>/dev/null | \
    awk 'NR==1 {print "   " $0; next} {print "   " $0}' || echo "   (unable to fetch)"

echo ""

# Next steps based on outcome
if [ $ERRORS_FOUND -gt 0 ]; then
    echo -e "${YELLOW}📋 Next Steps:${NC}"
    
    if grep -q "503 Service Unavailable\|502 Bad Gateway" "$LOG_FILE" 2>/dev/null; then
        echo "   1. Wait 5-10 minutes for Docker registries to recover"
        echo "   2. Retry: ${BOLD}make clean${NC} or ${BOLD}make clean-fast${NC}"
        echo "   3. Check registry status: ${BOLD}make check-registries${NC}"
    else
        echo "   1. Check logs: ${BOLD}cat $LOG_FILE${NC}"
        echo "   2. Check specific container: ${BOLD}docker logs core-<service>${NC}"
        echo "   3. Try rebuild: ${BOLD}make rebuild${NC}"
    fi
else
    echo -e "${GREEN}✨ Success! Next Steps:${NC}"
    echo "   1. Verify environment: ${BOLD}make verify${NC}"
    echo "   2. Check services at: ${BOLD}https://core-platform.local${NC}"
    echo "   3. View logs: ${BOLD}make logs${NC}"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Save summary to separate file
SUMMARY_FILE="${LOG_FILE%.log}-summary.txt"
{
    echo "BUILD SUMMARY - $(date)"
    echo "================================"
    echo ""
    echo "Build Type: $BUILD_TYPE"
    echo "Duration: $DURATION_STR"
    echo "Log File: $LOG_FILE"
    echo ""
    echo "Errors Found: $ERRORS_FOUND"
    echo "Containers Running: $TOTAL"
    echo "Healthy: $HEALTHY"
    echo "Unhealthy: $UNHEALTHY"
    echo ""
    echo "Full details above."
} > "$SUMMARY_FILE"

echo "📄 Summary saved to: $SUMMARY_FILE"
echo ""
