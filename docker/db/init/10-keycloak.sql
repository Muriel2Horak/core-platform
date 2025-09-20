-- Inicializace Postgres pro core-platform (idempotentní)
-- Vytváří databáze a uživatele pro backend i Keycloak

DO $$
BEGIN
  -- Vytvoření role pro Keycloak
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'keycloak') THEN
    CREATE ROLE keycloak LOGIN PASSWORD 'keycloak';
  END IF;
  
  -- Poznámka: role 'core' už existuje (je to default POSTGRES_USER)
END
$$;

-- Vytvoření DB proběhne ve skriptu 20-keycloak-db.sh (mimo transakci)
