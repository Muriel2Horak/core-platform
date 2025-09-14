-- Inicializace Postgres pro Keycloak (idempotentní)

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'keycloak') THEN
    CREATE ROLE keycloak LOGIN PASSWORD 'keycloak';
  END IF;
END
$$;

-- Vytvoření DB proběhne ve skriptu 20-keycloak-db.sh (mimo transakci)
