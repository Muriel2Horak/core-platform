#!/usr/bin/env bash
set -euo pipefail

VAULT_ADDR=${VAULT_ADDR:-http://127.0.0.1:8200}
VAULT_CONTAINER=${VAULT_CONTAINER:-core-vault}
VAULT_TOKEN_FILE=${VAULT_TOKEN_FILE:-$HOME/.vault-token}
SNAPSHOT_PATH=${SNAPSHOT_PATH:-/vault/data/vault.snap}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing dependency: $1" >&2
    exit 1
  }
}

require_cmd docker

if [[ -z "${VAULT_TOKEN:-}" ]] && [[ -f "$VAULT_TOKEN_FILE" ]]; then
  VAULT_TOKEN="$(cat "$VAULT_TOKEN_FILE")"
fi

if [[ -z "${VAULT_TOKEN:-}" ]]; then
  echo "VAULT_TOKEN is required (set VAULT_TOKEN or VAULT_TOKEN_FILE)" >&2
  exit 1
fi

docker exec -e VAULT_ADDR="$VAULT_ADDR" -e VAULT_TOKEN="$VAULT_TOKEN" \
  "$VAULT_CONTAINER" vault operator raft snapshot restore -force "$SNAPSHOT_PATH"

echo "✅ Vault snapshot restored from $SNAPSHOT_PATH"
