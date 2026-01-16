#!/bin/bash
set -euo pipefail

# Vytvoření databází "core", "keycloak" a "grafana" (pokud neexistují)
# Běží po vytvoření app rolí, databáze jsou vlastníkem separátních uživatelů.

CORE_DB_NAME="${DB_INTERNAL_NAME:-core}"
CORE_DB_USER="${DATABASE_USERNAME:-core_app}"
KEYCLOAK_DB_NAME="${KEYCLOAK_DB_NAME:-keycloak}"
KEYCLOAK_DB_USER="${KEYCLOAK_DB_USERNAME:-keycloak_app}"
GRAFANA_DB_NAME="${GRAFANA_DB_NAME:-grafana}"
GRAFANA_DB_USER="${GRAFANA_DB_USERNAME:-grafana_app}"

# Vytvoření databáze "core" (pro backend)
exists_core=$(psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" -tAc "SELECT 1 FROM pg_database WHERE datname='${CORE_DB_NAME}'")
if [ "$exists_core" != "1" ]; then
  echo "Creating database '${CORE_DB_NAME}' owned by role '${CORE_DB_USER}'..."
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" -c "CREATE DATABASE ${CORE_DB_NAME} OWNER ${CORE_DB_USER};"
else
  echo "Database '${CORE_DB_NAME}' already exists, skipping."
fi

# Vytvoření databáze "keycloak" (pro Keycloak)
exists_keycloak=$(psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" -tAc "SELECT 1 FROM pg_database WHERE datname='${KEYCLOAK_DB_NAME}'")
if [ "$exists_keycloak" != "1" ]; then
  echo "Creating database '${KEYCLOAK_DB_NAME}' owned by role '${KEYCLOAK_DB_USER}'..."
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" -c "CREATE DATABASE ${KEYCLOAK_DB_NAME} OWNER ${KEYCLOAK_DB_USER};"
else
  echo "Database '${KEYCLOAK_DB_NAME}' already exists, skipping."
fi

# Vytvoření databáze "grafana" (pro Grafanu)
exists_grafana=$(psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" -tAc "SELECT 1 FROM pg_database WHERE datname='${GRAFANA_DB_NAME}'")
if [ "$exists_grafana" != "1" ]; then
  echo "Creating database '${GRAFANA_DB_NAME}' owned by role '${GRAFANA_DB_USER}'..."
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" -c "CREATE DATABASE ${GRAFANA_DB_NAME} OWNER ${GRAFANA_DB_USER};"
else
  echo "Database '${GRAFANA_DB_NAME}' already exists, skipping."
fi
