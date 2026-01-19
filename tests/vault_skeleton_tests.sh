#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

rg -n "storage \"raft\"" docker/vault/config.hcl >/dev/null
rg -n "prometheus_retention_time" docker/vault/config.hcl >/dev/null
rg -n "job_name: vault-audit" docker/promtail/config.yml >/dev/null
rg -n "vault_audit" docker/docker-compose.vault.yml >/dev/null
rg -n "vault-smoke" Makefile >/dev/null

if [[ ! -x scripts/vault/vault-smoke.sh ]]; then
  echo "❌ vault-smoke script is not executable"
  exit 1
fi

echo "✅ Vault skeleton checks passed"
