#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

rg -n "job_name: 'vault'" docker/prometheus/prometheus.yml >/dev/null
rg -n "axiom_vault.yml" docker/prometheus/prometheus.yml >/dev/null || true

if [[ ! -f docker/prometheus/alerts/axiom_vault.yml ]]; then
  echo "❌ missing Vault alert rules"
  exit 1
fi

echo "✅ Vault monitoring checks passed"
