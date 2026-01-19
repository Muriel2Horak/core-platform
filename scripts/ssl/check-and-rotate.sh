#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

CERT_FILE="${CERT_FILE:-docker/ssl/cert.pem}"
ROTATE_DAYS="${ROTATE_DAYS:-7}"
ALERT_DAYS="${ALERT_DAYS:-30}"
DRY_RUN=false
FORCE_ROTATE=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --force)
      FORCE_ROTATE=true
      shift
      ;;
    *)
      echo "Unknown argument: $1"
      exit 1
      ;;
  esac
done

LOG_DIR="diagnostics"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/ssl-rotation.log"

if [[ ! -f "$CERT_FILE" ]]; then
  echo "❌ Certificate not found: $CERT_FILE" | tee -a "$LOG_FILE"
  exit 1
fi

EXPIRY_RAW=$(openssl x509 -in "$CERT_FILE" -noout -enddate | cut -d= -f2)

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

echo "📅 SSL certificate expires in ${DAYS_LEFT} days" | tee -a "$LOG_FILE"

if [[ "$DAYS_LEFT" -lt "$ALERT_DAYS" ]]; then
  echo "⚠️  SSL certificate below alert threshold (${ALERT_DAYS} days)" | tee -a "$LOG_FILE"
fi

if [[ "$DAYS_LEFT" -lt "$ROTATE_DAYS" || "$FORCE_ROTATE" == "true" ]]; then
  echo "🔄 Rotating SSL certificate..." | tee -a "$LOG_FILE"

  if [[ "$DRY_RUN" == "false" ]]; then
    cp "$CERT_FILE" "${CERT_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
    bash docker/ssl/generate-ssl.sh
  fi

  if docker ps --format '{{.Names}}' | grep -q '^core-nginx$'; then
    if [[ "$DRY_RUN" == "false" ]]; then
      docker exec core-nginx nginx -s reload
    fi
    echo "✅ Nginx reloaded" | tee -a "$LOG_FILE"
  fi

  if docker ps --format '{{.Names}}' | grep -q '^core-keycloak$'; then
    if [[ "$DRY_RUN" == "false" ]]; then
      docker restart core-keycloak >/dev/null
    fi
    echo "✅ Keycloak restarted" | tee -a "$LOG_FILE"
  fi

  NEW_EXPIRY=$(openssl x509 -in "$CERT_FILE" -noout -enddate | cut -d= -f2)
  echo "✅ Rotation complete. New expiry: $NEW_EXPIRY" | tee -a "$LOG_FILE"

  if [[ -n "${SSL_ROTATION_WEBHOOK_URL:-}" ]]; then
    curl -sS -X POST "$SSL_ROTATION_WEBHOOK_URL" \
      -H "Content-Type: application/json" \
      -d "{\"text\":\"✅ SSL cert rotated. New expiry: $NEW_EXPIRY\"}" >/dev/null || true
  fi
fi
