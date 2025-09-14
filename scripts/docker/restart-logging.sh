#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

"${SCRIPT_DIR}/install_loki_plugin.sh"

cd "${REPO_ROOT}/docker"

echo "Recreating services (backend, frontend) with Loki logging driver..."
docker compose up -d --no-deps --force-recreate backend frontend

echo "Done. Check logs in Grafana (http://localhost:3001)."
