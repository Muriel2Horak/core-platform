#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

"${SCRIPT_DIR}/install_loki_plugin.sh" || true

cd "${REPO_ROOT}/docker"

PLUGIN_ENABLED=$(docker plugin inspect loki --format '{{.Enabled}}' 2>/dev/null || echo "false")
if [ "${PLUGIN_ENABLED}" = "true" ]; then
  echo "Using Loki logging driver override."
  docker compose -f docker-compose.yml -f docker-compose.loki-driver.yml up -d --build
else
  echo "Loki plugin not enabled. Using base compose (Promtail)."
  docker compose -f docker-compose.yml up -d --build
fi

echo "Done. Grafana: http://localhost:3001, Loki: http://localhost:3100, Promtail: http://localhost:9080"
