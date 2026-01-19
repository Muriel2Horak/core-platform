#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

rg -n "vault-rotate-backend-db-pass" Makefile >/dev/null
rg -n "vault-smoke-runtime" Makefile >/dev/null

if [[ ! -x scripts/vault/rotate-backend-db-pass.sh ]]; then
  echo "❌ rotate-backend-db-pass script is not executable"
  exit 1
fi

if [[ ! -x scripts/vault/vault-smoke-runtime.sh ]]; then
  echo "❌ vault-smoke-runtime script is not executable"
  exit 1
fi

rg -n "kv/core/postgres" scripts/vault/rotate-backend-db-pass.sh >/dev/null

rg -n "VAULT_RUNBOOK" docs/VAULT_RUNBOOK.md >/dev/null || true

if [[ ! -f docs/SECRETS_INVENTORY.md ]]; then
  echo "❌ missing docs/SECRETS_INVENTORY.md"
  exit 1
fi

echo "✅ Vault rotation checks passed"
