#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -f "$SCRIPT_DIR/.env" ]]; then
  set -a
  source "$SCRIPT_DIR/.env"
  set +a
fi

BASE_URL="${BE_BASE:-https://admin.core-platform.local}"
API_PATH="${BE_API_PATH:-/api}"
KC_BASE="${KC_BASE:-$BASE_URL}"
KC_REALM="${KC_REALM:-admin}"
KC_CLIENT_ID="${KC_CLIENT_ID:-web}"
KC_CLIENT_SECRET="${KC_CLIENT_SECRET:-}"

NONADMIN_USER="${TEST_USER1:-test}"
NONADMIN_PASSWORD="${TEST_PASSWORD1:-Test.1234}"
ADMIN_USER="${TEST_USER2:-test_admin}"
ADMIN_PASSWORD="${TEST_PASSWORD2:-Test.1234}"

TOKEN_ENDPOINT="${KC_BASE}/realms/${KC_REALM}/protocol/openid-connect/token"

log_info() {
  echo "[INFO] $1"
}

log_error() {
  echo "[ERROR] $1" >&2
}

require_deps() {
  for dep in curl jq; do
    if ! command -v "$dep" >/dev/null 2>&1; then
      log_error "Missing dependency: $dep"
      exit 1
    fi
  done
}

get_token() {
  local username="$1"
  local password="$2"

  if [[ -n "$KC_CLIENT_SECRET" ]]; then
    curl -s -k -X POST "$TOKEN_ENDPOINT" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "grant_type=password" \
      -d "client_id=$KC_CLIENT_ID" \
      -d "client_secret=$KC_CLIENT_SECRET" \
      -d "username=$username" \
      -d "password=$password"
  else
    curl -s -k -X POST "$TOKEN_ENDPOINT" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "grant_type=password" \
      -d "client_id=$KC_CLIENT_ID" \
      -d "username=$username" \
      -d "password=$password"
  fi
}

assert_status_range() {
  local status="$1"
  local label="$2"

  if [[ "$status" != 2* ]]; then
    log_error "$label expected 2xx, got $status"
    return 1
  fi
  return 0
}

assert_forbidden() {
  local status="$1"
  local label="$2"

  if [[ "$status" != "401" && "$status" != "403" ]]; then
    log_error "$label expected 401/403, got $status"
    return 1
  fi
  return 0
}

require_deps

log_info "Fetching admin token for RBAC smoke test"
admin_response="$(get_token "$ADMIN_USER" "$ADMIN_PASSWORD")"
admin_token="$(echo "$admin_response" | jq -r '.access_token')"
if [[ -z "$admin_token" || "$admin_token" == "null" ]]; then
  log_error "Admin token missing (check TEST_USER2/TEST_PASSWORD2)"
  exit 1
fi

log_info "Fetching non-admin token for RBAC smoke test"
user_response="$(get_token "$NONADMIN_USER" "$NONADMIN_PASSWORD")"
user_token="$(echo "$user_response" | jq -r '.access_token')"
if [[ -z "$user_token" || "$user_token" == "null" ]]; then
  log_error "User token missing (check TEST_USER1/TEST_PASSWORD1)"
  exit 1
fi

endpoint="${BASE_URL}${API_PATH}/admin/tenants"
log_info "Checking admin access to ${endpoint}"
admin_status="$(curl -s -k -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $admin_token" "$endpoint")"
assert_status_range "$admin_status" "Admin access" || exit 1

log_info "Checking non-admin access to ${endpoint}"
user_status="$(curl -s -k -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $user_token" "$endpoint")"
assert_forbidden "$user_status" "Non-admin access" || exit 1

log_info "RBAC smoke test passed"
