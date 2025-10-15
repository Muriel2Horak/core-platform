#!/usr/bin/env bash
# Watch build progress in separate terminal

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TRACKER="$SCRIPT_DIR/build-progress-tracker.sh"
STATE_FILE="${BUILD_PROGRESS_STATE:-/tmp/make-progress-shared}"

# Clear screen first
clear

# Show waiting message until build starts
while [ ! -f "$STATE_FILE" ]; do
    tput cup 0 0 2>/dev/null || true
    echo "⏳ Waiting for build to start..."
    echo ""
    echo "   State file: $STATE_FILE"
    sleep 1
done

# Now start watching with updates every second
watch -n 1 -t -c "$TRACKER draw"
