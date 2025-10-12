#!/usr/bin/env bash
# Crashloop watcher - detekuje kontejnery, které se restartují
set -Eeuo pipefail

INTERVAL=5
RESTART_THRESHOLD=1

# Store restart counts
declare -A RESTART_COUNTS
declare -A LAST_ALERT

echo "👁️  Watching for crashloops (checking every ${INTERVAL}s)..."
echo "Press Ctrl+C to stop"
echo ""

while true; do
    CONTAINERS=$(docker ps --filter "name=core-platform" --format "{{.Names}}" 2>/dev/null || true)
    
    if [ -z "$CONTAINERS" ]; then
        echo "⚠️  No containers found"
        sleep $INTERVAL
        continue
    fi
    
    for container in $CONTAINERS; do
        CURRENT_RESTART=$(docker inspect "$container" --format='{{.RestartCount}}' 2>/dev/null || echo "0")
        PREVIOUS_RESTART=${RESTART_COUNTS[$container]:-0}
        
        # Store current count
        RESTART_COUNTS[$container]=$CURRENT_RESTART
        
        # Detect restart
        if [ "$CURRENT_RESTART" -gt "$PREVIOUS_RESTART" ]; then
            RESTARTS_DELTA=$((CURRENT_RESTART - PREVIOUS_RESTART))
            
            echo "🔥 CRASHLOOP DETECTED: $container (restarts: $CURRENT_RESTART, +$RESTARTS_DELTA)"
            
            # Gather diagnostics
            BUILD_TS=$(date +%Y%m%d-%H%M%S)
            CRASH_FILE=".tmp/crash-${container}-${BUILD_TS}.json"
            
            echo "📝 Saving diagnostics to: $CRASH_FILE"
            
            # Collect container info
            docker inspect "$container" > "$CRASH_FILE" 2>/dev/null || echo "{}" > "$CRASH_FILE"
            
            # Append logs
            echo "" >> "$CRASH_FILE"
            echo "=== LAST 300 LOG LINES ===" >> "$CRASH_FILE"
            docker logs --tail=300 "$container" >> "$CRASH_FILE" 2>&1 || echo "Failed to get logs" >> "$CRASH_FILE"
            
            # Extract key info
            EXIT_CODE=$(docker inspect "$container" --format='{{.State.ExitCode}}' 2>/dev/null || echo "unknown")
            OOMKILLED=$(docker inspect "$container" --format='{{.State.OOMKilled}}' 2>/dev/null || echo "unknown")
            HEALTH=$(docker inspect "$container" --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' 2>/dev/null || echo "unknown")
            
            echo "  Exit Code: $EXIT_CODE"
            echo "  OOMKilled: $OOMKILLED"
            echo "  Health: $HEALTH"
            echo ""
            
            # Print hints for Copilot
            echo "COPILOT_HINT: Container $container is crashlooping. Check diagnostics in $CRASH_FILE"
            
            if [ "$OOMKILLED" = "true" ]; then
                echo "COPILOT_HINT: Container was OOM killed. Consider increasing memory limits in docker-compose.yml"
            fi
            
            if [ "$EXIT_CODE" != "0" ] && [ "$EXIT_CODE" != "unknown" ]; then
                echo "COPILOT_HINT: Container exited with code $EXIT_CODE. Check logs for error messages."
            fi
            
            echo ""
            
            LAST_ALERT[$container]=$(date +%s)
        fi
    done
    
    sleep $INTERVAL
done
