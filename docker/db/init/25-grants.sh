#!/usr/bin/env bash
set -euo pipefail

echo "🔐 Granting database privileges for service users..."

CORE_DB_NAME="${DB_INTERNAL_NAME:-core}"
CORE_DB_USER="${DATABASE_USERNAME:-core_app}"

KEYCLOAK_DB_NAME="${KEYCLOAK_DB_NAME:-keycloak}"
KEYCLOAK_DB_USER="${KEYCLOAK_DB_USERNAME:-keycloak_app}"

GRAFANA_DB_NAME="${GRAFANA_DB_NAME:-grafana}"
GRAFANA_DB_USER="${GRAFANA_DB_USERNAME:-grafana_app}"

grant_db() {
  local db_name="$1"
  local db_user="$2"

  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db_name" <<-EOSQL
REVOKE ALL ON DATABASE "${db_name}" FROM public;
GRANT CONNECT ON DATABASE "${db_name}" TO "${db_user}";

GRANT USAGE ON SCHEMA public TO "${db_user}";
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "${db_user}";
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO "${db_user}";

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT ALL PRIVILEGES ON TABLES TO "${db_user}";
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT ALL PRIVILEGES ON SEQUENCES TO "${db_user}";
EOSQL
}

grant_db "$CORE_DB_NAME" "$CORE_DB_USER"

grant_db "$KEYCLOAK_DB_NAME" "$KEYCLOAK_DB_USER"

grant_db "$GRAFANA_DB_NAME" "$GRAFANA_DB_USER"
