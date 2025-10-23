#!/usr/bin/env sh
# 🔐 Production-grade JWKS fetcher with atomic writes, ETag support, and validation
# Prevents partial file reads and unnecessary updates

set -eu

# Configuration
JWKS_URL="${JWKS_URL:-http://backend:8080/.well-known/jwks.json}"
JWKS_FILE="${JWKS_FILE:-/var/lib/grafana/jwks.json}"
ETAG_FILE="${JWKS_FILE}.etag"
TMP_FILE="${JWKS_FILE}.tmp"
LOCK_FILE="${JWKS_FILE}.lock"

# Logging function
log() {
    echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] $*"
}

# Cleanup on exit
cleanup() {
    rm -f "$TMP_FILE" "${ETAG_FILE}.new" "$LOCK_FILE" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

# Simple file-based lock (prevent concurrent fetches)
if [ -f "$LOCK_FILE" ]; then
    log "⚠️  Lock file exists, another fetch in progress - exiting"
    exit 0
fi
touch "$LOCK_FILE"

log "📥 Fetching JWKS from $JWKS_URL..."

# Build curl command with conditional ETag
CURL_CMD="curl -fsSL --max-time 10"
ETAG_HEADER=""

if [ -f "$ETAG_FILE" ]; then
    ETAG=$(cat "$ETAG_FILE" 2>/dev/null || echo "")
    if [ -n "$ETAG" ]; then
        ETAG_HEADER="-H If-None-Match:$ETAG"
        log "   Using ETag: $ETAG"
    fi
fi

# Fetch with response headers
HTTP_RESPONSE=$(mktemp)
HTTP_CODE=$($CURL_CMD -w "%{http_code}" -D "$HTTP_RESPONSE" $ETAG_HEADER -o "$TMP_FILE" "$JWKS_URL" 2>&1 || echo "000")

# Check HTTP status
case "$HTTP_CODE" in
    200)
        log "   ✅ HTTP 200 - JWKS downloaded"
        ;;
    304)
        log "   ℹ️  HTTP 304 - Not Modified (ETag match)"
        rm -f "$TMP_FILE" "$HTTP_RESPONSE"
        exit 0
        ;;
    000)
        log "   ❌ Connection failed or timeout"
        rm -f "$TMP_FILE" "$HTTP_RESPONSE"
        exit 1
        ;;
    *)
        log "   ❌ HTTP $HTTP_CODE - Failed to fetch JWKS"
        rm -f "$TMP_FILE" "$HTTP_RESPONSE"
        exit 1
        ;;
esac

# Validate JSON structure
if ! jq -e '.keys | length > 0' "$TMP_FILE" >/dev/null 2>&1; then
    log "❌ Invalid JWKS JSON (missing .keys array or empty)"
    rm -f "$TMP_FILE" "$HTTP_RESPONSE"
    exit 1
fi

# Extract and log key IDs
KIDS=$(jq -r '.keys[].kid' "$TMP_FILE" 2>/dev/null | tr '\n' ',' | sed 's/,$//')
if [ -z "$KIDS" ]; then
    log "⚠️  Warning: No key IDs (kid) found in JWKS"
fi

# Extract new ETag from response headers
NEW_ETAG=$(awk '/^etag:/||/^ETag:/ {gsub(/\r/,""); print $2; exit}' "$HTTP_RESPONSE" 2>/dev/null || echo "")

# Atomic move to final location
mv "$TMP_FILE" "$JWKS_FILE"
log "✅ JWKS updated successfully"

if [ -n "$KIDS" ]; then
    log "   Keys: $KIDS"
fi

# Update ETag file if present
if [ -n "$NEW_ETAG" ]; then
    echo "$NEW_ETAG" > "$ETAG_FILE"
    log "   ETag saved: $NEW_ETAG"
fi

# Cleanup response headers
rm -f "$HTTP_RESPONSE"

log "🎉 JWKS fetch completed"
exit 0
