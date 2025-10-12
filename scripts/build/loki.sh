#!/usr/bin/env bash
# Loki helper - queries Loki for logs
set -Eeuo pipefail

COMMAND="${1:-query}"
SERVICE="${2:-all}"
MINUTES="${3:-10}"

# Load env vars
if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

if [ -f .env.local ]; then
    set -a
    source .env.local
    set +a
fi

# Check if Loki is configured
if [ -z "${LOKI_URL:-}" ]; then
    echo "⚠️  LOKI_URL not configured in .env or .env.local"
    exit 0
fi

LOKI_TENANT="${LOKI_TENANT:-core-platform}"
LOKI_LABEL_SELECTOR="${LOKI_LABEL_SELECTOR:-{compose_project=\"core-platform\"}}"

# Function to query Loki
loki_query() {
    local service_filter="$1"
    local minutes="$2"
    local limit="${3:-1000}"
    
    # Build query
    local query="$LOKI_LABEL_SELECTOR"
    
    if [ "$service_filter" != "all" ]; then
        query="${LOKI_LABEL_SELECTOR}|~\"$service_filter\""
    fi
    
    # Calculate time range
    local end_time=$(date +%s)
    local start_time=$((end_time - minutes * 60))
    
    # Query Loki
    curl -s -G \
        -H "X-Scope-OrgID: $LOKI_TENANT" \
        --data-urlencode "query=$query" \
        --data-urlencode "limit=$limit" \
        --data-urlencode "start=${start_time}000000000" \
        --data-urlencode "end=${end_time}000000000" \
        "$LOKI_URL/loki/api/v1/query_range" | \
    jq -r '.data.result[].values[][1]' 2>/dev/null || echo ""
}

case "$COMMAND" in
    query)
        loki_query "$SERVICE" "$MINUTES"
        ;;
    tail)
        # Not implemented for Build Doctor - use existing logs-tail
        echo "Use 'make logs-tail' for live tailing"
        ;;
    *)
        echo "Usage: $0 query <service> <minutes>"
        exit 1
        ;;
esac
