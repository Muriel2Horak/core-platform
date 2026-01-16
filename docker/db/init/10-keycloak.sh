#!/bin/bash
# Inicializace Postgres pro core-platform (idempotentní)
# Vytváří databáze a uživatele pro backend i Keycloak s env proměnnými

set -e

echo "🗄️ Initializing PostgreSQL users and databases..."

# Keycloak database credentials z env proměnných
KEYCLOAK_DB_USER="${KEYCLOAK_DB_USERNAME:-keycloak_app}"
KEYCLOAK_DB_PASS="${KEYCLOAK_DB_PASSWORD:-keycloak}"
if [ -n "${KEYCLOAK_DB_PASSWORD_FILE:-}" ] && [ -f "$KEYCLOAK_DB_PASSWORD_FILE" ]; then
  KEYCLOAK_DB_PASS="$(cat "$KEYCLOAK_DB_PASSWORD_FILE")"
fi
KEYCLOAK_DB="${KEYCLOAK_DB_NAME:-keycloak}"

echo "Creating Keycloak database user: $KEYCLOAK_DB_USER"

# Vytvoření role pro Keycloak s heslem z env proměnné
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	DO \$\$
	BEGIN
	  -- Vytvoření role pro Keycloak s dynamickým heslem
	  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '$KEYCLOAK_DB_USER') THEN
	    CREATE ROLE $KEYCLOAK_DB_USER LOGIN PASSWORD '$KEYCLOAK_DB_PASS';
	    RAISE NOTICE 'Created Keycloak user: $KEYCLOAK_DB_USER';
	  ELSE
	    RAISE NOTICE 'Keycloak user already exists: $KEYCLOAK_DB_USER';
	  END IF;
	  
	  -- Poznámka: role 'core' už existuje (je to default POSTGRES_USER)
	END
	\$\$;
EOSQL

echo "✅ PostgreSQL initialization completed successfully"
