#!/usr/bin/env bash
set -euo pipefail

VAULT_ADDR=${VAULT_ADDR:-http://127.0.0.1:8200}
VAULT_TOKEN_FILE=${VAULT_TOKEN_FILE:-$HOME/.vault-token}

if [[ -z "${VAULT_TOKEN:-}" ]] && [[ -f "$VAULT_TOKEN_FILE" ]]; then
  VAULT_TOKEN="$(cat "$VAULT_TOKEN_FILE")"
fi

if [[ -z "${VAULT_TOKEN:-}" ]]; then
  echo "VAULT_TOKEN is required (set VAULT_TOKEN or VAULT_TOKEN_FILE)" >&2
  exit 1
fi

status_code=$(curl -s -o /dev/null -w "%{http_code}" "$VAULT_ADDR/v1/sys/health" || true)
if [[ ! "$status_code" =~ ^(200|429)$ ]]; then
  echo "❌ Vault health check failed (HTTP $status_code)"
  exit 1
fi

kv_code=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "X-Vault-Token: $VAULT_TOKEN" \
  "$VAULT_ADDR/v1/kv/metadata/core" || true)
if [[ ! "$kv_code" =~ ^(200|404)$ ]]; then
  echo "❌ Vault KV metadata check failed (HTTP $kv_code)"
  exit 1
fi

echo "✅ Vault runtime smoke checks passed"
