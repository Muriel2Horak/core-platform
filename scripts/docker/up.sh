#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

"${SCRIPT_DIR}/install_loki_plugin.sh" || true

cd "${REPO_ROOT}/docker"

# ✅ Automatické generování Keycloak realm konfigurace pro development
echo "🔑 Generuji Keycloak realm konfiguraci pro development..."
if [[ -f "${REPO_ROOT}/docker/keycloak/generate-realm.sh" ]]; then
    cd "${REPO_ROOT}/docker/keycloak"
    # Nastavíme DOMAIN pro development
    export DOMAIN=core-platform.local
    ./generate-realm.sh
    echo "✅ Realm konfigurace vygenerována pro core-platform.local"
    cd "${REPO_ROOT}/docker"
else
    echo "⚠️  generate-realm.sh nenalezen - pokračuji bez generování"
fi

# ✅ OPRAVENO: Kontrola existencie loki-driver override súboru
LOKI_DRIVER_COMPOSE="${REPO_ROOT}/docker/docker-compose.loki-driver.yml"
PLUGIN_ENABLED=$(docker plugin inspect loki --format '{{.Enabled}}' 2>/dev/null || echo "false")

if [ "${PLUGIN_ENABLED}" = "true" ] && [ -f "${LOKI_DRIVER_COMPOSE}" ]; then
  echo "Using Loki logging driver override."
  docker compose -f docker-compose.yml -f docker-compose.loki-driver.yml up -d --build
else
  if [ "${PLUGIN_ENABLED}" = "true" ]; then
    echo "⚠️  Loki plugin enabled but docker-compose.loki-driver.yml not found. Using base compose."
  else
    echo "Loki plugin not enabled. Using base compose (Promtail)."
  fi
  docker compose -f docker-compose.yml up -d --build
fi

echo "✅ Done! Services:"
echo "   Frontend:  https://core-platform.local"
echo "   Keycloak:  https://core-platform.local/admin/"
echo "   Grafana:   http://localhost:3001"
echo "   Loki:      http://localhost:3100"
echo "   Promtail:  http://localhost:9080"
