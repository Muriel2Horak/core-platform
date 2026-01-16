#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

DRY_RUN=false
BASE_URL="${BASE_URL:-}"
TIMEOUT="${TIMEOUT:-20}"
SMOKE_FORCE_RESOLVE="${SMOKE_FORCE_RESOLVE:-false}"
SMOKE_RESOLVE_IP="${SMOKE_RESOLVE_IP:-127.0.0.1}"
KAFKA_PORT="${KAFKA_PORT:-9092}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --base-url)
      BASE_URL="$2"
      shift 2
      ;;
    --timeout)
      TIMEOUT="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1"
      exit 1
      ;;
  esac
done

if [[ -z "$BASE_URL" ]]; then
  if [[ -f .env ]]; then
    set -a
    source .env
    set +a
  fi
  DOMAIN="${DOMAIN:-core-platform.local}"
  BASE_URL="https://admin.${DOMAIN}"
fi

GRAFANA_URL="${GRAFANA_PUBLIC_URL:-https://ops.${DOMAIN:-core-platform.local}/grafana}"
OIDC_CLIENT_ID="${OIDC_CLIENT_ID:-admin-client}"
OIDC_CLIENT_SECRET="${OIDC_CLIENT_SECRET:-${KEYCLOAK_ADMIN_CLIENT_SECRET:-}}"

LOG_DIR="diagnostics"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/smoke-test-$(date +%Y%m%d-%H%M%S).log"

resolve_flag_for_url() {
  local url="$1"
  local host scheme port host_port

  scheme="$(echo "$url" | sed -E 's#^([a-z]+)://.*#\1#')"
  host_port="$(echo "$url" | sed -E 's#^[a-z]+://([^/]+).*#\1#')"
  host="${host_port%%:*}"

  if [[ "$host_port" == *:* ]]; then
    port="${host_port##*:}"
  else
    if [[ "$scheme" == "http" ]]; then
      port=80
    else
      port=443
    fi
  fi

  if [[ "$SMOKE_FORCE_RESOLVE" == "true" ]] || ! getent hosts "$host" >/dev/null 2>&1; then
    echo "--resolve ${host}:${port}:${SMOKE_RESOLVE_IP}"
  fi
}

BASE_URL_RESOLVE="$(resolve_flag_for_url "$BASE_URL")"
GRAFANA_URL_RESOLVE="$(resolve_flag_for_url "$GRAFANA_URL")"

run_check() {
  local name="$1"
  local command="$2"

  echo "▶️  $name" | tee -a "$LOG_FILE"
  if [[ "$DRY_RUN" == "true" ]]; then
    echo "    DRY RUN" | tee -a "$LOG_FILE"
    return 0
  fi

  if eval "$command" >> "$LOG_FILE" 2>&1; then
    echo "    ✅ PASS" | tee -a "$LOG_FILE"
    return 0
  fi

  echo "    ❌ FAIL" | tee -a "$LOG_FILE"
  return 1
}

echo "🔥 Running post-deployment smoke tests..." | tee -a "$LOG_FILE"
echo "   BASE_URL: $BASE_URL" | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"

run_check "Backend health" \
  "curl -sk -m $TIMEOUT $BASE_URL_RESOLVE '$BASE_URL/api/actuator/health' | jq -e '.status == \"UP\"'"

run_check "Frontend homepage" \
  "curl -sk -m $TIMEOUT $BASE_URL_RESOLVE '$BASE_URL/' -o /dev/null"

run_check "Keycloak OIDC config" \
  "curl -sk -m $TIMEOUT $BASE_URL_RESOLVE '$BASE_URL/auth/realms/admin/.well-known/openid-configuration' | jq -e '.issuer'"

run_check "Grafana health" \
  "curl -sk -m $TIMEOUT $GRAFANA_URL_RESOLVE '$GRAFANA_URL/api/health' | jq -e '.database == \"ok\"'"

run_check "Database connectivity" \
  "docker exec core-db pg_isready -U '${DB_INTERNAL_USERNAME:-core}' -d '${DB_INTERNAL_NAME:-core}'"

if docker ps --format '{{.Names}}' | grep -q '^core-kafka$'; then
  if command -v nc >/dev/null 2>&1; then
    run_check "Kafka connectivity" \
      "nc -z -w 2 127.0.0.1 $KAFKA_PORT"
  else
    run_check "Kafka connectivity" \
      "bash -c '</dev/tcp/127.0.0.1/$KAFKA_PORT'"
  fi
else
  echo "▶️  Kafka connectivity (skipped - container not running)" | tee -a "$LOG_FILE"
fi

if [[ -n "$OIDC_CLIENT_SECRET" ]]; then
  run_check "Auth flow (client credentials)" \
    "curl -sk -m $TIMEOUT $BASE_URL_RESOLVE -X POST '$BASE_URL/auth/realms/admin/protocol/openid-connect/token' \
      -d 'client_id=$OIDC_CLIENT_ID' \
      -d 'client_secret=$OIDC_CLIENT_SECRET' \
      -d 'grant_type=client_credentials' | jq -e '.access_token'"
else
  echo "▶️  Auth flow (skipped - OIDC_CLIENT_SECRET missing)" | tee -a "$LOG_FILE"
fi

echo "" | tee -a "$LOG_FILE"
echo "✅ Smoke tests completed" | tee -a "$LOG_FILE"
