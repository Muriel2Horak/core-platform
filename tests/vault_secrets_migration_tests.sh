#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

rg -n "kv/data/core" docker/vault-agent/templates/backend.env.ctmpl >/dev/null
rg -n "kv/data/core" docker/vault-agent/templates/keycloak.env.ctmpl >/dev/null
rg -n "kv/data/core" docker/vault-agent/templates/grafana-admin-password.ctmpl >/dev/null
rg -n "kv/data/core" docker/vault-agent/templates/grafana-db-password.ctmpl >/dev/null
rg -n "kv/data/core" docker/vault-agent/templates/redis-password.ctmpl >/dev/null
rg -n "kv/data/core" docker/vault-agent/templates/minio.env.ctmpl >/dev/null
rg -n "kv/data/core" docker/vault-agent/templates/cube.env.ctmpl >/dev/null

rg -n "kv/core" scripts/vault/bootstrap-vault.sh >/dev/null
rg -n "vault-list-secrets" Makefile >/dev/null
rg -n "vault-push-secrets" Makefile >/dev/null

if [[ ! -x scripts/vault/list-secrets.sh ]]; then
  echo "❌ list-secrets script is not executable"
  exit 1
fi

echo "✅ Vault secrets migration checks passed"
