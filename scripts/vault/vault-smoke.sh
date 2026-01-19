#!/usr/bin/env bash
set -euo pipefail

VAULT_ADDR=${VAULT_ADDR:-http://127.0.0.1:8200}
VAULT_CONTAINER=${VAULT_CONTAINER:-core-vault}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing dependency: $1" >&2
    exit 1
  }
}

require_cmd curl
require_cmd docker

status_code=$(curl -s -o /dev/null -w "%{http_code}" "$VAULT_ADDR/v1/sys/health" || true)
if [[ ! "$status_code" =~ ^(200|429)$ ]]; then
  echo "❌ Vault health check failed (HTTP $status_code)"
  exit 1
fi

audit_check=$(docker exec "$VAULT_CONTAINER" sh -c 'test -f /vault/logs/audit.log')
if [[ $? -ne 0 ]]; then
  echo "❌ Vault audit log missing at /vault/logs/audit.log"
  exit 1
fi

echo "✅ Vault smoke checks passed"
