#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

rg -n "vault-pki-setup" Makefile >/dev/null
rg -n "vault-pki-issue" Makefile >/dev/null
rg -n "edge.bundle.ctmpl" docker/vault-agent/config.hcl >/dev/null
rg -n "render-edge-cert.sh" docker/vault-agent/config.hcl >/dev/null
rg -n "storage \"raft\"" docker/vault/config.hcl >/dev/null
rg -n "SSL_VOLUME" .env.template >/dev/null

if [[ ! -x scripts/vault/pki-setup.sh ]]; then
  echo "❌ pki-setup script is not executable"
  exit 1
fi

if [[ ! -x scripts/vault/pki-issue.sh ]]; then
  echo "❌ pki-issue script is not executable"
  exit 1
fi

echo "✅ Vault PKI checks passed"
