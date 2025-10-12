#!/usr/bin/env bash
# Build Doctor Wrapper - zachycuje chyby, měří čas, spouští triage
set -Eeuo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Ensure diagnostics directory exists
mkdir -p "$(dirname "${LOG_FILE:-diagnostics/build.log}")"

START_TIME=$(date +%s)
EXIT_CODE=0

cleanup() {
    local exit_code=$?
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    
    if [ $exit_code -ne 0 ]; then
        echo ""
        echo -e "${RED}❌ Build failed after ${DURATION}s${NC}"
        echo ""
        
        # Gather context
        echo "🔍 Gathering diagnostics..."
        
        # Docker Compose PS
        echo "📋 Docker Compose Status:"
        docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml ps 2>&1 || true
        
        # Docker PS for restart counts
        echo ""
        echo "🔄 Container Restart Counts:"
        docker ps --format "table {{.Names}}\t{{.Status}}" --filter "name=core-platform" 2>&1 || true
        
        # Containers with restarts
        echo ""
        echo "⚠️  Containers with issues:"
        docker inspect $(docker ps -aq --filter "name=core-platform") 2>/dev/null | \
            jq -r '.[] | select(.RestartCount > 0 or .State.Health.Status != "healthy") | 
                   "\(.Name): RestartCount=\(.RestartCount), Health=\(.State.Health.Status // "none")"' 2>&1 || true
        
        # Run triage
        if [ -f "scripts/build/triage.sh" ]; then
            echo ""
            echo "🔬 Running triage analysis..."
            bash scripts/build/triage.sh "${LOG_FILE:-diagnostics/build.log}" "${JSON_REPORT:-diagnostics/build-report.json}" || true
            
            # Print JSON report for Copilot
            if [ -f "${JSON_REPORT:-diagnostics/build-report.json}" ]; then
                echo ""
                echo "##[COPILOT_START_JSON]"
                cat "${JSON_REPORT:-diagnostics/build-report.json}"
                echo "##[COPILOT_END_JSON]"
                echo ""
                echo "COPILOT_HINT: Analyze JSON above and suggest concrete fixes. If fix is config-only, propose exact diff."
            fi
        fi
        
        exit $exit_code
    else
        echo ""
        echo -e "${GREEN}✅ Build succeeded in ${DURATION}s${NC}"
        
        # Success report
        if [ -f "scripts/build/triage.sh" ]; then
            cat > "${JSON_REPORT:-diagnostics/build-report.json}" <<EOF
{
  "buildTs": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "status": "SUCCESS",
  "durationSeconds": ${DURATION},
  "containers": $(docker inspect $(docker ps -aq --filter "name=core-platform") 2>/dev/null | \
      jq '[.[] | {name: .Name, state: .State.Status, health: (.State.Health.Status // "none"), restartCount: .RestartCount}]' 2>&1 || echo '[]')
}
EOF
            echo ""
            echo "##[COPILOT_START_JSON]"
            cat "${JSON_REPORT:-diagnostics/build-report.json}"
            echo "##[COPILOT_END_JSON]"
        fi
    fi
}

trap cleanup EXIT ERR

# Execute the command passed as arguments
echo "🏗️  Executing: $*"
echo "📝 Log: ${LOG_FILE:-diagnostics/build.log}"
echo ""

exec "$@"
