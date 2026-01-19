#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

rg -n "vault-oidc-setup" Makefile >/dev/null

if [[ ! -x scripts/vault/oidc-setup.sh ]]; then
  echo "❌ oidc-setup script is not executable"
  exit 1
fi

if [[ ! -f docs/VAULT_CI_OIDC.md ]]; then
  echo "❌ missing docs/VAULT_CI_OIDC.md"
  exit 1
fi

echo "✅ Vault OIDC checks passed"
