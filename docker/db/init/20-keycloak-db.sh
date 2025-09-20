#!/bin/bash
set -euo pipefail

# Vytvoření DB "keycloak" (pokud neexistuje)
# Běží v oficiálním Postgres entrypointu po 10-keycloak.sql, takže role "keycloak" už existuje.

exists=$(psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" -tAc "SELECT 1 FROM pg_database WHERE datname='keycloak'")
if [ "$exists" != "1" ]; then
  echo "Creating database 'keycloak' owned by role 'keycloak'..."
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" -c "CREATE DATABASE keycloak OWNER keycloak;"
else
  echo "Database 'keycloak' already exists, skipping."
fi
