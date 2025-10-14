#!/usr/bin/env bash
# Wait for Docker containers to be healthy
set -Eeuo pipefail

TIMEOUT=180
INTERVAL=5

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --timeout)
            TIMEOUT="$2"
            shift 2
            ;;
        --interval)
            INTERVAL="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "⏳ Waiting for containers to be healthy (timeout: ${TIMEOUT}s)..."

START_TIME=$(date +%s)

while true; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))
    
    if [ $ELAPSED -gt $TIMEOUT ]; then
        echo "❌ Timeout after ${TIMEOUT}s waiting for containers"
        echo ""
        echo "Unhealthy containers:"
        docker ps --filter "name=core-" --format "table {{.Names}}\t{{.Status}}" | grep -v "Up.*healthy" || true
        exit 1
    fi
    
    # Get all core- containers (core-backend, core-frontend, etc.)
    CONTAINERS=$(docker ps --filter "name=core-" --format "{{.Names}}" 2>/dev/null || true)
    
    if [ -z "$CONTAINERS" ]; then
        echo "⚠️  No containers found"
        sleep $INTERVAL
        continue
    fi
    
    ALL_HEALTHY=true
    UNHEALTHY_LIST=""
    
    for container in $CONTAINERS; do
        # Check if container has health check
        HAS_HEALTH=$(docker inspect "$container" --format='{{if .State.Health}}true{{else}}false{{end}}' 2>/dev/null || echo "false")
        
        if [ "$HAS_HEALTH" = "true" ]; then
            HEALTH_STATUS=$(docker inspect "$container" --format='{{.State.Health.Status}}' 2>/dev/null || echo "unknown")
            
            if [ "$HEALTH_STATUS" != "healthy" ]; then
                ALL_HEALTHY=false
                UNHEALTHY_LIST="${UNHEALTHY_LIST}  - $container: $HEALTH_STATUS\n"
            fi
        else
            # No health check, just check if running
            STATE=$(docker inspect "$container" --format='{{.State.Status}}' 2>/dev/null || echo "unknown")
            
            if [ "$STATE" != "running" ]; then
                ALL_HEALTHY=false
                UNHEALTHY_LIST="${UNHEALTHY_LIST}  - $container: $STATE (no healthcheck)\n"
            fi
        fi
        
        # Check restart count
        RESTART_COUNT=$(docker inspect "$container" --format='{{.RestartCount}}' 2>/dev/null || echo "0")
        
        if [ "$RESTART_COUNT" -gt 0 ]; then
            echo "⚠️  $container has restarted $RESTART_COUNT times"
        fi
    done
    
    if $ALL_HEALTHY; then
        echo "✅ All containers are healthy!"
        exit 0
    fi
    
    echo -e "⏳ Waiting... (${ELAPSED}s/${TIMEOUT}s)"
    echo -e "$UNHEALTHY_LIST"
    
    sleep $INTERVAL
done
