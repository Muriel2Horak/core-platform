#!/bin/bash
set -euo pipefail

# Vytvoření tabulky backfill_progress pro sledování postupu backfillu uživatelů z Keycloaku
# Tabulka je idempotentní - vytvoří se pouze pokud neexistuje

echo "🔄 Creating backfill_progress table if not exists..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "core" <<-EOSQL
	-- Tabulka pro sledování postupu backfillu
	CREATE TABLE IF NOT EXISTS backfill_progress (
	    id BIGSERIAL PRIMARY KEY,
	    tenant_id VARCHAR(255) NOT NULL UNIQUE,
	    total_users INTEGER NOT NULL DEFAULT 0,
	    processed_users INTEGER NOT NULL DEFAULT 0,
	    failed_users INTEGER NOT NULL DEFAULT 0,
	    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
	    started_at TIMESTAMP,
	    completed_at TIMESTAMP,
	    last_processed_keycloak_id VARCHAR(255),
	    error_message TEXT,
	    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
	);

	-- Index pro rychlé vyhledávání podle tenant_id
	CREATE INDEX IF NOT EXISTS idx_backfill_progress_tenant_id 
	    ON backfill_progress(tenant_id);

	-- Index pro sledování statusu
	CREATE INDEX IF NOT EXISTS idx_backfill_progress_status 
	    ON backfill_progress(status);

	-- Trigger pro automatickou aktualizaci updated_at
	CREATE OR REPLACE FUNCTION update_backfill_progress_updated_at()
	RETURNS TRIGGER AS \$\$
	BEGIN
	    NEW.updated_at = CURRENT_TIMESTAMP;
	    RETURN NEW;
	END;
	\$\$ LANGUAGE plpgsql;

	DROP TRIGGER IF EXISTS trigger_update_backfill_progress_updated_at ON backfill_progress;
	CREATE TRIGGER trigger_update_backfill_progress_updated_at
	    BEFORE UPDATE ON backfill_progress
	    FOR EACH ROW
	    EXECUTE FUNCTION update_backfill_progress_updated_at();

	DO \$\$
	BEGIN
	    RAISE NOTICE 'Table backfill_progress created/verified successfully';
	END \$\$;
EOSQL

echo "✅ backfill_progress table initialization completed"
