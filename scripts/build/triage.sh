#!/usr/bin/env bash
# Triage - analyzuje chyby, navrhuje opravy
set -Eeuo pipefail

LOG_FILE="${1:-diagnostics/build.log}"
JSON_REPORT="${2:-diagnostics/build-report.json}"

# Colors
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🔬 Running triage analysis..."

# Ensure jq is installed
if ! command -v jq &> /dev/null; then
    echo "⚠️  jq not found, installing minimal report"
    cat > "$JSON_REPORT" <<EOF
{
  "buildTs": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "status": "FAILED",
  "error": "jq not installed - cannot generate detailed report"
}
EOF
    exit 0
fi

# Initialize report data
BUILD_TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)
ERRORS=()
CONTAINERS=()
CAUSES=()
FIXES=()

# Extract last 200 lines + error context from log
if [ -f "$LOG_FILE" ]; then
    ERROR_CONTEXT=$(tail -n 200 "$LOG_FILE" | grep -nE 'ERROR|Exception|failed|panic|stack trace|BUILD FAILED' || true)
else
    ERROR_CONTEXT=""
fi

# Get container status
COMPOSE_PS=$(docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml ps --format json 2>/dev/null || echo '[]')

# Analyze each problematic service
PROBLEM_CONTAINERS=$(echo "$COMPOSE_PS" | jq -r '.[] | select(.State != "running") | .Name' 2>/dev/null || true)

for container in $PROBLEM_CONTAINERS; do
    if [ -z "$container" ]; then
        continue
    fi
    
    echo "  Analyzing: $container"
    
    # Inspect container
    INSPECT=$(docker inspect "$container" 2>/dev/null || echo '{}')
    
    STATE=$(echo "$INSPECT" | jq -r '.[0].State.Status // "unknown"')
    RESTART_COUNT=$(echo "$INSPECT" | jq -r '.[0].RestartCount // 0')
    EXIT_CODE=$(echo "$INSPECT" | jq -r '.[0].State.ExitCode // 0')
    OOMKILLED=$(echo "$INSPECT" | jq -r '.[0].State.OOMKilled // false')
    HEALTH=$(echo "$INSPECT" | jq -r '.[0].State.Health.Status // "none"')
    
    # Get logs
    LOGS=$(docker logs --tail=500 "$container" 2>&1 || echo "")
    
    # Add to containers array
    CONTAINERS+=("{\"name\":\"$container\",\"state\":\"$STATE\",\"restartCount\":$RESTART_COUNT,\"exitCode\":$EXIT_CODE,\"oomKilled\":$OOMKILLED,\"health\":\"$HEALTH\"}")
    
    # Heuristic analysis
    # Port conflicts
    if echo "$LOGS" | grep -qiE "bind.*address already in use|port.*already allocated"; then
        PORT=$(echo "$LOGS" | grep -oE ":[0-9]{2,5}" | head -1 || echo "unknown")
        CAUSES+=("\"Port conflict detected on $PORT for $container\"")
        FIXES+=("{\"title\":\"Fix port conflict for $container\",\"steps\":[\"Check docker-compose.yml ports for $container\",\"Kill process using $PORT: lsof -ti:$PORT | xargs kill -9\",\"Or change port in docker-compose.yml\"],\"runnable\":false}")
    fi
    
    # Keycloak host mismatch
    if echo "$LOGS" | grep -qiE "redirect.*uri.*mismatch|invalid.*redirect|frontendUrl"; then
        CAUSES+=("\"Keycloak redirect/host mismatch in $container\"")
        FIXES+=("{\"title\":\"Fix Keycloak host configuration\",\"steps\":[\"Check KEYCLOAK_FRONTEND_URL in .env\",\"Update docker/realm-core-platform.json with correct URLs\",\"Restart keycloak: docker compose restart keycloak\"],\"runnable\":false}")
    fi
    
    # DB migration failures
    if echo "$LOGS" | grep -qiE "migration.*failed|relation.*already exists|syntax error.*SQL|Flyway"; then
        CAUSES+=("\"Database migration failure in $container\"")
        FIXES+=("{\"title\":\"Fix database migration\",\"steps\":[\"Check flyway_schema_history table for failed migrations\",\"Revert last migration or fix SQL syntax\",\"Run: make db-clean-migrate (DEV only!)\"],\"runnable\":false}")
    fi
    
    # npm/pnpm failures
    if echo "$LOGS" | grep -qiE "npm.*ERR|pnpm.*ERR|lockfile.*mismatch|ENOENT.*package.json"; then
        CAUSES+=("\"Node.js dependency installation failure in $container\"")
        FIXES+=("{\"title\":\"Fix Node.js dependencies\",\"steps\":[\"Clear pnpm cache: pnpm store prune\",\"Delete node_modules and reinstall\",\"Check package.json and lockfile consistency\"],\"runnable\":false}")
    fi
    
    # Maven failures
    if echo "$LOGS" | grep -qiE "Maven.*BUILD FAILURE|corrupt.*JAR|Could not resolve dependencies"; then
        CAUSES+=("\"Maven build failure in $container\"")
        FIXES+=("{\"title\":\"Fix Maven build\",\"steps\":[\"Clear Maven cache: rm -rf ~/.m2/repository\",\"Run: cd backend && ./mvnw clean install -U -T 1C -DskipTests\",\"Check pom.xml for dependency conflicts\"],\"runnable\":false}")
    fi
    
    # OOM
    if [ "$OOMKILLED" = "true" ]; then
        CAUSES+=("\"Container $container was OOM killed\"")
        FIXES+=("{\"title\":\"Increase memory for $container\",\"steps\":[\"Add or increase mem_limit in docker-compose.yml\",\"Example: mem_limit: 2g\",\"Check application memory settings (Java heap, Node.js max-old-space)\"],\"runnable\":false}")
    fi
    
    # Disk space
    if echo "$LOGS" | grep -qiE "no space left on device|disk.*full"; then
        CAUSES+=("\"Disk space exhausted\"")
        FIXES+=("{\"title\":\"Free up disk space\",\"steps\":[\"Run: docker system prune -f\",\"Run: docker volume prune -f\",\"Check: df -h\"],\"runnable\":false}")
    fi
    
    # If no logs and Loki available, try Loki
    if [ -z "$LOGS" ] && [ -n "${LOKI_URL:-}" ]; then
        echo "  Trying Loki for $container logs..."
        LOKI_LOGS=$(bash scripts/build/loki.sh query "$container" 10 2>/dev/null || echo "")
        
        if [ -n "$LOKI_LOGS" ]; then
            LOGS="$LOKI_LOGS"
            echo "  Got logs from Loki"
        fi
    fi
done

# Build JSON arrays
ERRORS_JSON="[]"
if [ ${#ERRORS[@]} -gt 0 ]; then
    ERRORS_JSON=$(printf '%s\n' "${ERRORS[@]}" | jq -s '.')
fi

CONTAINERS_JSON="[]"
if [ ${#CONTAINERS[@]} -gt 0 ]; then
    CONTAINERS_JSON=$(printf '%s\n' "${CONTAINERS[@]}" | jq -s '.')
fi

CAUSES_JSON="[]"
if [ ${#CAUSES[@]} -gt 0 ]; then
    CAUSES_JSON=$(printf '%s\n' "${CAUSES[@]}" | jq -s '.')
fi

FIXES_JSON="[]"
if [ ${#FIXES[@]} -gt 0 ]; then
    FIXES_JSON=$(printf '%s\n' "${FIXES[@]}" | jq -s '.')
fi

# Write JSON report
cat > "$JSON_REPORT" <<EOF
{
  "buildTs": "$BUILD_TS",
  "status": "FAILED",
  "errors": $ERRORS_JSON,
  "containers": $CONTAINERS_JSON,
  "suspectedCauses": $CAUSES_JSON,
  "recommendedFixes": $FIXES_JSON,
  "artifacts": {
    "logFile": "$LOG_FILE"
  }
}
EOF

echo "✅ Triage report saved: $JSON_REPORT"

# Print top 3 hints
HINT_COUNT=0
for cause in "${CAUSES[@]}"; do
    if [ $HINT_COUNT -ge 3 ]; then
        break
    fi
    echo -e "${YELLOW}COPILOT_HINT: $cause${NC}"
    HINT_COUNT=$((HINT_COUNT + 1))
done

if [ ${#FIXES[@]} -gt 0 ]; then
    echo -e "${YELLOW}COPILOT_HINT: Check $JSON_REPORT for ${#FIXES[@]} recommended fix(es)${NC}"
fi
