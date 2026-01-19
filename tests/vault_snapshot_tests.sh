#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

rg -n "vault-snapshot" Makefile >/dev/null
rg -n "vault-restore" Makefile >/dev/null

if [[ ! -x scripts/vault/snapshot-save.sh ]]; then
  echo "❌ snapshot-save script is not executable"
  exit 1
fi

if [[ ! -x scripts/vault/snapshot-restore.sh ]]; then
  echo "❌ snapshot-restore script is not executable"
  exit 1
fi

echo "✅ Vault snapshot checks passed"
