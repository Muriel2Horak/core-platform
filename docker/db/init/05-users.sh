#!/usr/bin/env bash
set -euo pipefail

echo "🗄️ Creating service database users..."

read_secret() {
  local value="$1"
  local file="${2:-}"
  if [[ -n "$file" && -f "$file" ]]; then
    cat "$file"
  else
    echo "$value"
  fi
}

CORE_DB_USER="${DATABASE_USERNAME:-core_app}"
CORE_DB_PASS="$(read_secret "${DATABASE_PASSWORD:-core}" "${DATABASE_PASSWORD_FILE:-}")"

KEYCLOAK_DB_USER="${KEYCLOAK_DB_USERNAME:-keycloak_app}"
KEYCLOAK_DB_PASS="$(read_secret "${KEYCLOAK_DB_PASSWORD:-keycloak}" "${KEYCLOAK_DB_PASSWORD_FILE:-}")"

GRAFANA_DB_USER="${GRAFANA_DB_USERNAME:-grafana_app}"
GRAFANA_DB_PASS="$(read_secret "${GRAFANA_DB_PASSWORD:-grafana}" "${GRAFANA_DB_PASSWORD_FILE:-}")"

escape_sql() {
  echo "$1" | sed "s/'/''/g"
}

CORE_DB_USER_ESCAPED="$(escape_sql "$CORE_DB_USER")"
CORE_DB_PASS_ESCAPED="$(escape_sql "$CORE_DB_PASS")"
KEYCLOAK_DB_USER_ESCAPED="$(escape_sql "$KEYCLOAK_DB_USER")"
KEYCLOAK_DB_PASS_ESCAPED="$(escape_sql "$KEYCLOAK_DB_PASS")"
GRAFANA_DB_USER_ESCAPED="$(escape_sql "$GRAFANA_DB_USER")"
GRAFANA_DB_PASS_ESCAPED="$(escape_sql "$GRAFANA_DB_PASS")"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${CORE_DB_USER_ESCAPED}') THEN
    EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', '${CORE_DB_USER_ESCAPED}', '${CORE_DB_PASS_ESCAPED}');
    RAISE NOTICE 'Created core app user: %', '${CORE_DB_USER_ESCAPED}';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${KEYCLOAK_DB_USER_ESCAPED}') THEN
    EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', '${KEYCLOAK_DB_USER_ESCAPED}', '${KEYCLOAK_DB_PASS_ESCAPED}');
    RAISE NOTICE 'Created keycloak app user: %', '${KEYCLOAK_DB_USER_ESCAPED}';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${GRAFANA_DB_USER_ESCAPED}') THEN
    EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', '${GRAFANA_DB_USER_ESCAPED}', '${GRAFANA_DB_PASS_ESCAPED}');
    RAISE NOTICE 'Created grafana app user: %', '${GRAFANA_DB_USER_ESCAPED}';
  END IF;
END \$\$;
EOSQL
