#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env}"
VAULT_ADDR="${VAULT_ADDR:-http://127.0.0.1:8200}"
VAULT_CONTAINER="${VAULT_CONTAINER:-core-vault}"
APPROLE_DIR="${APPROLE_DIR:-$ROOT_DIR/.vault-approle}"
REPORT_DIR="${REPORT_DIR:-$ROOT_DIR/reports/security}"
UNSEAL_KEY_FILE="${UNSEAL_KEY_FILE:-$HOME/.vault-unseal-key}"
VAULT_TOKEN_FILE="${VAULT_TOKEN_FILE:-$HOME/.vault-token}"

MODE="full"
for arg in "$@"; do
  case "$arg" in
    --seed-only) MODE="seed" ;;
    --approle-only) MODE="approle" ;;
  esac
done

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing dependency: $1" >&2
    exit 1
  }
}

require_cmd docker
require_cmd curl
require_cmd jq

vault_exec() {
  docker exec -i -e VAULT_ADDR="$VAULT_ADDR" -e VAULT_TOKEN="${VAULT_TOKEN:-}" "$VAULT_CONTAINER" vault "$@"
}

wait_for_vault() {
  local timeout=60
  local waited=0
  while true; do
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" "$VAULT_ADDR/v1/sys/health" 2>/dev/null || echo "000")
    if [[ "$code" =~ ^(200|429|472|473|501|503)$ ]]; then
      return 0
    fi
    if [ "$waited" -ge "$timeout" ]; then
      echo "Vault not responding after ${timeout}s (HTTP $code)" >&2
      return 1
    fi
    sleep 2
    waited=$((waited + 2))
  done
}

load_token_from_file() {
  if [ -z "${VAULT_TOKEN:-}" ] && [ -f "$VAULT_TOKEN_FILE" ]; then
    VAULT_TOKEN="$(cat "$VAULT_TOKEN_FILE")"
    export VAULT_TOKEN
  fi
}

require_token() {
  if [ -z "${VAULT_TOKEN:-}" ]; then
    echo "VAULT_TOKEN is not set. Run vault-bootstrap or set VAULT_TOKEN/VAULT_TOKEN_FILE." >&2
    exit 1
  fi
}

init_and_unseal() {
  mkdir -p "$REPORT_DIR"

  local status
  status=$(curl -s "$VAULT_ADDR/v1/sys/health" 2>/dev/null || echo "{}")
  local initialized
  initialized=$(echo "$status" | jq -r '.initialized // false')
  local sealed
  sealed=$(echo "$status" | jq -r '.sealed // true')

  if [ "$initialized" != "true" ]; then
    echo "Initializing Vault..."
    local init_json
    init_json=$(vault_exec operator init -key-shares=1 -key-threshold=1 -format=json)

    local timestamp
    timestamp=$(date +"%Y%m%d-%H%M%S")
    local init_file="$REPORT_DIR/vault-init-$timestamp.json"
    echo "$init_json" > "$init_file"
    chmod 600 "$init_file"

    local unseal_key
    unseal_key=$(echo "$init_json" | jq -r '.unseal_keys_b64[0]')
    local root_token
    root_token=$(echo "$init_json" | jq -r '.root_token')

    echo "$unseal_key" > "$UNSEAL_KEY_FILE"
    chmod 600 "$UNSEAL_KEY_FILE"

    echo "$root_token" > "$VAULT_TOKEN_FILE"
    chmod 600 "$VAULT_TOKEN_FILE"

    VAULT_TOKEN="$root_token"
    export VAULT_TOKEN

    vault_exec operator unseal "$unseal_key" >/dev/null
    echo "Vault initialized and unsealed. Init data saved to $init_file"
    return 0
  fi

  if [ "$sealed" = "true" ]; then
    if [ ! -f "$UNSEAL_KEY_FILE" ]; then
      echo "Vault is sealed and no unseal key found at $UNSEAL_KEY_FILE" >&2
      exit 1
    fi
    vault_exec operator unseal "$(cat "$UNSEAL_KEY_FILE")" >/dev/null
    echo "Vault unsealed"
  fi

  load_token_from_file
}

ensure_kv() {
  load_token_from_file
  if ! vault_exec secrets list -format=json | jq -e '."secret/"' >/dev/null 2>&1; then
    vault_exec secrets enable -path=secret kv-v2 >/dev/null
  fi
}

enable_audit() {
  load_token_from_file
  require_token

  if ! vault_exec audit list -format=json | jq -e '."file/"' >/dev/null 2>&1; then
    vault_exec audit enable file file_path=/vault/logs/audit.log >/dev/null
    echo "Vault audit enabled (file)"
  fi
}

setup_approle() {
  load_token_from_file
  vault_exec auth enable approle >/dev/null 2>&1 || true

cat <<'POLICY' | vault_exec policy write core-platform-agent -
path "secret/data/core-platform/*" {
  capabilities = ["read"]
}

path "secret/metadata/core-platform/*" {
  capabilities = ["read", "list"]
}

path "pki_int/issue/edge-tls" {
  capabilities = ["create", "update", "read"]
}
POLICY

  vault_exec write auth/approle/role/core-platform-agent \
    token_policies="core-platform-agent" \
    token_ttl="24h" \
    token_max_ttl="48h" >/dev/null

  mkdir -p "$APPROLE_DIR"

  vault_exec read -field=role_id auth/approle/role/core-platform-agent/role-id > "$APPROLE_DIR/agent-role-id"
  vault_exec write -field=secret_id -f auth/approle/role/core-platform-agent/secret-id > "$APPROLE_DIR/agent-secret-id"

  chmod 600 "$APPROLE_DIR/agent-role-id" "$APPROLE_DIR/agent-secret-id"
  echo "AppRole credentials written to $APPROLE_DIR"
}

seed_secrets() {
  if [ ! -f "$ENV_FILE" ]; then
    echo "Missing env file: $ENV_FILE" >&2
    exit 1
  fi

  set -a
  # shellcheck source=/dev/null
  . "$ENV_FILE"
  set +a

  local db_username="${DB_INTERNAL_USERNAME:-${DATABASE_USERNAME:-core}}"
  local db_name="${DB_INTERNAL_NAME:-${DB_NAME:-core}}"
  local db_password="${DB_INTERNAL_PASSWORD:-${DATABASE_PASSWORD:-}}"

  local keycloak_db_username="${KEYCLOAK_DB_USERNAME:-keycloak}"
  local keycloak_db_name="${KEYCLOAK_DB_NAME:-keycloak}"
  local keycloak_db_password="${KEYCLOAK_DB_PASSWORD:-}"

  local keycloak_admin_password="${KEYCLOAK_ADMIN_PASSWORD:-}"
  local keycloak_admin_client_secret="${KEYCLOAK_ADMIN_CLIENT_SECRET:-}"
  local keycloak_client_secret="${KEYCLOAK_CLIENT_SECRET:-}"

  local grafana_admin_password="${GRAFANA_PASSWORD:-}"
  local grafana_db_password="${GRAFANA_DB_PASSWORD:-}"
  local grafana_oidc_secret="${GRAFANA_OIDC_SECRET:-}"
  local grafana_jwt_secret="${GRAFANA_JWT_SECRET:-}"

  local pgadmin_email="${PGADMIN_EMAIL:-}"
  local pgadmin_password="${PGADMIN_PASSWORD:-}"

  local redis_password="${REDIS_PASSWORD:-}"
  local minio_access_key="${MINIO_ACCESS_KEY:-}"
  local minio_secret_key="${MINIO_SECRET_KEY:-}"
  local cube_api_secret="${CUBE_API_SECRET:-}"

  vault_exec kv put secret/core-platform/postgres \
    username="$db_username" \
    database="$db_name" \
    password="$db_password" >/dev/null

  vault_exec kv put secret/core-platform/keycloak-db \
    username="$keycloak_db_username" \
    database="$keycloak_db_name" \
    password="$keycloak_db_password" >/dev/null

  vault_exec kv put secret/core-platform/keycloak \
    admin_password="$keycloak_admin_password" \
    admin_client_secret="$keycloak_admin_client_secret" \
    client_secret="$keycloak_client_secret" \
    test_user_password="${TEST_USER_PASSWORD:-}" \
    test_admin_password="${TEST_ADMIN_PASSWORD:-}" >/dev/null

  vault_exec kv put secret/core-platform/grafana \
    admin_password="$grafana_admin_password" \
    db_password="$grafana_db_password" \
    oidc_secret="$grafana_oidc_secret" \
    jwt_secret="$grafana_jwt_secret" >/dev/null

  vault_exec kv put secret/core-platform/pgadmin \
    email="$pgadmin_email" \
    password="$pgadmin_password" >/dev/null

  vault_exec kv put secret/core-platform/redis \
    password="$redis_password" >/dev/null

  vault_exec kv put secret/core-platform/minio \
    access_key="$minio_access_key" \
    secret_key="$minio_secret_key" >/dev/null

  vault_exec kv put secret/core-platform/cube \
    api_secret="$cube_api_secret" >/dev/null

  echo "Vault secrets seeded under secret/core-platform/*"
}

wait_for_vault

case "$MODE" in
  seed)
    load_token_from_file
    require_token
    enable_audit
    ensure_kv
    seed_secrets
    ;;
  approle)
    load_token_from_file
    require_token
    enable_audit
    setup_approle
    ;;
  *)
    init_and_unseal
    enable_audit
    ensure_kv
    setup_approle
    seed_secrets
    ;;
esac
