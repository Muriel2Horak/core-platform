#!/bin/bash
set -euo pipefail

# Vytvoření databází "core" a "keycloak" (pokud neexistují)
# Běží v oficiálním Postgres entrypointu po 10-keycloak.sql, takže role "keycloak" už existuje.

# Vytvoření databáze "core" (pro backend)
exists_core=$(psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" -tAc "SELECT 1 FROM pg_database WHERE datname='core'")
if [ "$exists_core" != "1" ]; then
  echo "Creating database 'core' owned by role 'core'..."
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" -c "CREATE DATABASE core OWNER core;"
else
  echo "Database 'core' already exists, skipping."
fi

# Vytvoření databáze "keycloak" (pro Keycloak)
exists_keycloak=$(psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" -tAc "SELECT 1 FROM pg_database WHERE datname='keycloak'")
if [ "$exists_keycloak" != "1" ]; then
  echo "Creating database 'keycloak' owned by role 'keycloak'..."
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" -c "CREATE DATABASE keycloak OWNER keycloak;"
else
  echo "Database 'keycloak' already exists, skipping."
fi
