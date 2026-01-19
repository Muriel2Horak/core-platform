#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

ACME_FILE="${ACME_FILE:-docker/ssl/acme.json}"
ALERT_DAYS="${ALERT_DAYS:-30}"
DRY_RUN=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    *)
      echo "Unknown argument: $1"
      exit 1
      ;;
  esac
done

if [[ ! -f "$ACME_FILE" ]]; then
  echo "❌ ACME file not found: $ACME_FILE"
  exit 1
fi

CERT_B64=$(jq -r '.letsencrypt.Certificates[0].certificate' "$ACME_FILE")
if [[ -z "$CERT_B64" || "$CERT_B64" == "null" ]]; then
  echo "❌ No certificate data in $ACME_FILE"
  exit 1
fi

EXPIRY_RAW=$(echo "$CERT_B64" | base64 -d | openssl x509 -noout -enddate | cut -d= -f2)

parse_date() {
  local raw="$1"
  if date -d "$raw" +%s >/dev/null 2>&1; then
    date -d "$raw" +%s
    return
  fi
  if date -j -f "%b %e %H:%M:%S %Y %Z" "$raw" +%s >/dev/null 2>&1; then
    date -j -f "%b %e %H:%M:%S %Y %Z" "$raw" +%s
    return
  fi
  echo "0"
}

EXPIRY_TS=$(parse_date "$EXPIRY_RAW")
NOW_TS=$(date +%s)
DAYS_LEFT=$(( (EXPIRY_TS - NOW_TS) / 86400 ))

echo "📅 Let's Encrypt cert expires in ${DAYS_LEFT} days"

if [[ "$DAYS_LEFT" -lt "$ALERT_DAYS" ]]; then
  echo "⚠️  Certificate expiry below ${ALERT_DAYS} days"
  if [[ "$DRY_RUN" == "false" ]]; then
    docker restart traefik >/dev/null
    echo "✅ Traefik restarted to renew certificates"
  fi
fi
