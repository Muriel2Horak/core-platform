#!/bin/bash
# Loki query script for multitenancy smoke tests
# Usage: ./loki_query.sh <TENANT_KEY>

set -euo pipefail

TENANT_KEY="${1:-}"
if [[ -z "$TENANT_KEY" ]]; then
    echo "Usage: $0 <TENANT_KEY>"
    exit 1
fi

# Load environment variables
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "$SCRIPT_DIR/.env" ]]; then
    set -a
    source "$SCRIPT_DIR/.env"
    set +a
else
    echo "Error: .env file not found at $SCRIPT_DIR/.env"
    exit 1
fi

# Create artifacts directory
mkdir -p "$SCRIPT_DIR/../artifacts"

# Query Loki for tenant-specific logs
QUERY="{service=\"${SERVICE_LABEL}\"} |= \"tenant\\\":\\\"${TENANT_KEY}\\\"\""
OUTPUT_FILE="$SCRIPT_DIR/../artifacts/loki_${TENANT_KEY}.json"

echo "Querying Loki for tenant: $TENANT_KEY"
echo "Query: $QUERY"

# Query last 5 minutes of logs
START_TIME=$(date -u -v-5M +%s)000000000  # 5 minutes ago in nanoseconds
END_TIME=$(date -u +%s)000000000          # now in nanoseconds

# Try to query Loki
if curl -s \
    --max-time 10 \
    -G \
    -d "query=${QUERY}" \
    -d "start=${START_TIME}" \
    -d "end=${END_TIME}" \
    -d "limit=50" \
    "${LOKI_BASE}/loki/api/v1/query_range" \
    -o "$OUTPUT_FILE"; then
    
    # Count the number of log entries
    LOG_COUNT=$(jq -r '.data.result[] | .values | length' "$OUTPUT_FILE" 2>/dev/null | awk '{sum += $1} END {print sum+0}')
    
    echo "Found $LOG_COUNT log entries for tenant '$TENANT_KEY'"
    echo "Results saved to: $OUTPUT_FILE"
    
    # Return count for summary
    echo "$LOG_COUNT"
else
    echo "Warning: Failed to query Loki for tenant '$TENANT_KEY'"
    echo "0" > "$OUTPUT_FILE"
    echo "0"
fi