#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "❌ Docker CLI not found"
  exit 1
fi

if [[ ! -f .env ]]; then
  echo "❌ .env file not found"
  echo "💡 Copy from template: cp .env.template .env"
  exit 1
fi

set -a
source .env
set +a

CORE_DB_NAME="${DB_INTERNAL_NAME:-core}"
KEYCLOAK_DB_NAME="${KEYCLOAK_DB_NAME:-keycloak}"
GRAFANA_DB_NAME="${GRAFANA_DB_NAME:-grafana}"

CORE_DB_USER="${DATABASE_USERNAME:-core_app}"
KEYCLOAK_DB_USER="${KEYCLOAK_DB_USERNAME:-keycloak_app}"
GRAFANA_DB_USER="${GRAFANA_DB_USERNAME:-grafana_app}"

CORE_DB_PASS="${DATABASE_PASSWORD:-core}"
KEYCLOAK_DB_PASS="${KEYCLOAK_DB_PASSWORD:-keycloak}"
GRAFANA_DB_PASS="${GRAFANA_DB_PASSWORD:-grafana}"

DB_CONTAINER_NAME="core-db"
STARTED_DB="false"

if ! docker ps --format '{{.Names}}' | grep -q "^core-db$"; then
  if [[ "${START_DB:-false}" == "true" ]]; then
    DB_CONTAINER_NAME="core-db-test"
    STARTED_DB="true"
    echo "▶️  Starting isolated test database container..."
    docker run -d --name "$DB_CONTAINER_NAME" \
      -e POSTGRES_USER="$DB_INTERNAL_USERNAME" \
      -e POSTGRES_PASSWORD="$DB_INTERNAL_PASSWORD" \
      -e POSTGRES_DB="$DB_INTERNAL_NAME" \
      -e DATABASE_USERNAME="$CORE_DB_USER" \
      -e DATABASE_PASSWORD="$CORE_DB_PASS" \
      -e KEYCLOAK_DB_NAME="$KEYCLOAK_DB_NAME" \
      -e KEYCLOAK_DB_USERNAME="$KEYCLOAK_DB_USER" \
      -e KEYCLOAK_DB_PASSWORD="$KEYCLOAK_DB_PASS" \
      -e GRAFANA_DB_NAME="$GRAFANA_DB_NAME" \
      -e GRAFANA_DB_USERNAME="$GRAFANA_DB_USER" \
      -e GRAFANA_DB_PASSWORD="$GRAFANA_DB_PASS" \
      -v "$PROJECT_ROOT/docker/db/init:/docker-entrypoint-initdb.d:ro" \
      postgres:16 >/dev/null

    for i in $(seq 1 30); do
      if docker exec "$DB_CONTAINER_NAME" pg_isready -U "$DB_INTERNAL_USERNAME" -d "$DB_INTERNAL_NAME" >/dev/null 2>&1; then
        break
      fi
      sleep 2
    done
  else
    echo "❌ core-db container is not running"
    echo "💡 Start it with: START_DB=true make test-db-separate-users"
    exit 1
  fi
fi

echo "🧪 DB separate users tests"

echo "▶️  core_app can access core DB"
PGPASSWORD="$CORE_DB_PASS" docker exec "$DB_CONTAINER_NAME" psql -U "$CORE_DB_USER" -d "$CORE_DB_NAME" -c "SELECT 1;" >/dev/null

echo "▶️  keycloak_app can access keycloak DB"
PGPASSWORD="$KEYCLOAK_DB_PASS" docker exec "$DB_CONTAINER_NAME" psql -U "$KEYCLOAK_DB_USER" -d "$KEYCLOAK_DB_NAME" -c "SELECT 1;" >/dev/null

echo "▶️  grafana_app can access grafana DB"
PGPASSWORD="$GRAFANA_DB_PASS" docker exec "$DB_CONTAINER_NAME" psql -U "$GRAFANA_DB_USER" -d "$GRAFANA_DB_NAME" -c "SELECT 1;" >/dev/null

echo "▶️  core_app cannot access keycloak DB"
if PGPASSWORD="$CORE_DB_PASS" docker exec "$DB_CONTAINER_NAME" psql -U "$CORE_DB_USER" -d "$KEYCLOAK_DB_NAME" -c "SELECT 1;" >/dev/null 2>&1; then
  echo "❌ core_app should not access keycloak DB"
  exit 1
fi

echo "▶️  keycloak_app cannot access core DB"
if PGPASSWORD="$KEYCLOAK_DB_PASS" docker exec "$DB_CONTAINER_NAME" psql -U "$KEYCLOAK_DB_USER" -d "$CORE_DB_NAME" -c "SELECT 1;" >/dev/null 2>&1; then
  echo "❌ keycloak_app should not access core DB"
  exit 1
fi

echo "▶️  grafana_app cannot access core DB"
if PGPASSWORD="$GRAFANA_DB_PASS" docker exec "$DB_CONTAINER_NAME" psql -U "$GRAFANA_DB_USER" -d "$CORE_DB_NAME" -c "SELECT 1;" >/dev/null 2>&1; then
  echo "❌ grafana_app should not access core DB"
  exit 1
fi

echo "✅ Separate users isolation checks passed"

if [[ "$STARTED_DB" == "true" ]]; then
  docker rm -f "$DB_CONTAINER_NAME" >/dev/null
fi
