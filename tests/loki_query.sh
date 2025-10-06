#!/bin/bash
# Loki log query helper script
# Usage: loki_query.sh <service|all|errors|tail> [time_range]

set -e

LOKI_URL="http://localhost:3100"

SERVICE="$1"
TIME_RANGE="${2:-10m}"

# Convert time range to nanoseconds
case "$TIME_RANGE" in
    *m)
        MINUTES="${TIME_RANGE%m}"
        START_NS=$(($(date '+%s') - (MINUTES * 60)))000000000
        ;;
    *h)
        HOURS="${TIME_RANGE%h}"
        START_NS=$(($(date '+%s') - (HOURS * 3600)))000000000
        ;;
    *)
        # Default 10 minutes
        START_NS=$(($(date '+%s') - 600))000000000
        ;;
esac

END_NS=$(date '+%s')000000000

# Build query based on service
case "$SERVICE" in
    backend)
        QUERY='{container="core-backend"}'
        ;;
    frontend)
        QUERY='{container="core-frontend"}'
        ;;
    keycloak)
        QUERY='{container="core-keycloak"}'
        ;;
    db|database)
        QUERY='{container="core-db"}'
        ;;
    all)
        QUERY='{container=~"core-.*"}'
        ;;
    errors)
        QUERY='{container=~"core-.*",level="ERROR"}'
        ;;
    tail)
        # Live tail mode
        echo "📋 Live tailing ${2:-backend} logs from Loki..."
        echo "Press Ctrl+C to stop"
        TARGET_SERVICE="${2:-backend}"
        while true; do
            TAIL_START=$(($(date '+%s') - 60))000000000
            TAIL_END=$(date '+%s')000000000
            curl -s "${LOKI_URL}/loki/api/v1/query_range?query={container=\"core-${TARGET_SERVICE}\"}&start=${TAIL_START}&end=${TAIL_END}&limit=10" 2>/dev/null | \
                jq -r '.data.result[]?.values[]?[1]' 2>/dev/null | tail -5
            sleep 2
        done
        exit 0
        ;;
    *)
        echo "❌ Usage: $0 <backend|frontend|keycloak|db|all|errors|tail> [time_range]"
        echo ""
        echo "Examples:"
        echo "  $0 backend 10m      # Backend logs last 10 minutes"
        echo "  $0 frontend 1h      # Frontend logs last hour"
        echo "  $0 errors 30m       # All errors last 30 minutes"
        echo "  $0 tail backend     # Live tail backend logs"
        exit 1
        ;;
esac

# Fetch logs from Loki
echo "📋 Fetching logs from Loki..."
curl -s "${LOKI_URL}/loki/api/v1/query_range?query=${QUERY}&start=${START_NS}&end=${END_NS}&limit=100" 2>/dev/null | \
    jq -r '.data.result[]?.values[]?[1]' 2>/dev/null | \
    tail -50 || {
        echo "❌ Failed to fetch logs from Loki"
        echo "💡 Is Loki running? Check: curl -s http://localhost:3100/ready"
        exit 1
    }