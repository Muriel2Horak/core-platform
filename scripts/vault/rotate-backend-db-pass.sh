#!/usr/bin/env bash
set -euo pipefail

VAULT_ADDR=${VAULT_ADDR:-http://127.0.0.1:8200}
VAULT_CONTAINER=${VAULT_CONTAINER:-core-vault}
VAULT_TOKEN_FILE=${VAULT_TOKEN_FILE:-$HOME/.vault-token}
DB_NAME=${DB_NAME:-core}
DB_USER=${DB_USER:-core_app}
DB_CONTAINER=${DB_CONTAINER:-core-db}
SECRET_PATH=${SECRET_PATH:-kv/core/postgres}
RESTART_BACKEND=${RESTART_BACKEND:-true}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing dependency: $1" >&2
    exit 1
  }
}

require_cmd docker
require_cmd jq
require_cmd openssl

if [[ -z "${VAULT_TOKEN:-}" ]] && [[ -f "$VAULT_TOKEN_FILE" ]]; then
  VAULT_TOKEN="$(cat "$VAULT_TOKEN_FILE")"
fi

if [[ -z "${VAULT_TOKEN:-}" ]]; then
  echo "VAULT_TOKEN is required (set VAULT_TOKEN or VAULT_TOKEN_FILE)" >&2
  exit 1
fi

vault_exec() {
  docker exec -e VAULT_ADDR="$VAULT_ADDR" -e VAULT_TOKEN="$VAULT_TOKEN" "$VAULT_CONTAINER" vault "$@"
}

new_password=$(openssl rand -base64 24 | tr -d '=+/')

# Fetch current db credentials
current_json=$(vault_exec kv get -format=json "$SECRET_PATH")
current_user=$(echo "$current_json" | jq -r '.data.data.username')
current_db=$(echo "$current_json" | jq -r '.data.data.database')

if [[ -n "$current_user" ]]; then
  DB_USER="$current_user"
fi
if [[ -n "$current_db" ]]; then
  DB_NAME="$current_db"
fi

# Update DB user password in Postgres
if ! docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$DB_CONTAINER" \
  psql -U "${POSTGRES_USER:-postgres}" -d postgres \
  -c "ALTER USER \"${DB_USER}\" WITH PASSWORD '${new_password}';" >/dev/null; then
  echo "❌ Failed to rotate database password" >&2
  exit 1
fi

# Write new secret to Vault
vault_exec kv put "$SECRET_PATH" \
  username="$DB_USER" \
  database="$DB_NAME" \
  password="$new_password" >/dev/null

echo "✅ Vault secret updated for ${DB_USER}@${DB_NAME}"

if [[ "$RESTART_BACKEND" == "true" ]]; then
  if docker ps --format '{{.Names}}' | grep -q '^core-backend$'; then
    docker restart core-backend >/dev/null
    echo "🔄 Restarted backend to pick up new secret"
  else
    echo "ℹ️ Backend container not running, skipped restart"
  fi
fi
