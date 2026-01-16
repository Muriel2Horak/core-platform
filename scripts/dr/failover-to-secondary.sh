#!/usr/bin/env bash
set -euo pipefail

DRY_RUN=false

for arg in "$@"; do
  case "$arg" in
    --dry-run)
      DRY_RUN=true
      ;;
    --dry-run=false)
      DRY_RUN=false
      ;;
    *)
      ;;
  esac
done

SECONDARY_HOST=${SECONDARY_HOST:-dr.core-platform.com}
PRIMARY_HOST=${PRIMARY_HOST:-core-platform.com}
HOSTED_ZONE_ID=${HOSTED_ZONE_ID:-}
SLACK_WEBHOOK=${SLACK_WEBHOOK:-}
HEALTHCHECK_URL=${HEALTHCHECK_URL:-https://${SECONDARY_HOST}/health}

resolve_ip() {
  local host=$1
  if command -v dig >/dev/null 2>&1; then
    dig +short "$host" | head -1
  else
    getent hosts "$host" | awk '{print $1}' | head -1
  fi
}

log() {
  printf '%s\n' "$1"
}

log "Initiating failover to secondary region"
log "Primary: $PRIMARY_HOST"
log "Secondary: $SECONDARY_HOST"

if [[ "$DRY_RUN" == "false" ]]; then
  if ! curl -sf "$HEALTHCHECK_URL" >/dev/null; then
    log "Secondary health check failed: $HEALTHCHECK_URL"
    exit 1
  fi

  if [[ -z "$HOSTED_ZONE_ID" ]]; then
    log "HOSTED_ZONE_ID is required for DNS switch"
    exit 1
  fi
fi

SECONDARY_IP=$(resolve_ip "$SECONDARY_HOST")
if [[ -z "$SECONDARY_IP" ]]; then
  log "Unable to resolve secondary IP for $SECONDARY_HOST"
  exit 1
fi

change_batch=$(mktemp)
cat > "$change_batch" <<JSON
{
  "Changes": [
    {
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "${PRIMARY_HOST}",
        "Type": "A",
        "TTL": 60,
        "ResourceRecords": [{"Value": "${SECONDARY_IP}"}]
      }
    }
  ]
}
JSON

if [[ "$DRY_RUN" == "true" ]]; then
  log "Dry run enabled - skipping DNS update"
  cat "$change_batch"
  rm -f "$change_batch"
  exit 0
fi

if ! command -v aws >/dev/null 2>&1; then
  log "aws cli is required for DNS update"
  exit 1
fi

aws route53 change-resource-record-sets \
  --hosted-zone-id "$HOSTED_ZONE_ID" \
  --change-batch "file://${change_batch}"

rm -f "$change_batch"

if [[ -n "$SLACK_WEBHOOK" ]]; then
  curl -s -X POST "$SLACK_WEBHOOK" \
    -H "Content-Type: application/json" \
    -d "{\"text\":\"Failover to secondary region completed\"}" \
    >/dev/null || true
fi

log "Failover completed. Monitor https://${SECONDARY_HOST}"
