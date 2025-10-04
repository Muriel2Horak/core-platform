-- V2__init_multitenancy_and_user_directory.sql
-- Multitenancy and User Directory initialization - COMPLETE WITH KEYCLOAK INTEGRATION

-- ⚠️ BEZPEČNOSTNÍ KONTROLA: Zkontrolovat, zda už existuje users_directory s UUID tenant_id
DO $$
DECLARE
    has_old_structure BOOLEAN := FALSE;
    table_exists BOOLEAN := FALSE;
BEGIN
    -- Zkontrolovat, zda tabulka users_directory existuje
    SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_name = 'users_directory'
    ) INTO table_exists;
    
    IF table_exists THEN
        -- Zkontrolovat, zda má starý sloupec tenant_id typu UUID
        SELECT EXISTS (
            SELECT FROM information_schema.columns 
            WHERE table_name = 'users_directory' 
            AND column_name = 'tenant_id' 
            AND data_type = 'uuid'
        ) INTO has_old_structure;
        
        IF has_old_structure THEN
            RAISE EXCEPTION 'MIGRATION ERROR: Found existing users_directory table with UUID tenant_id. Please run V4__migrate_tenant_id_to_tenant_key.sql migration first, or use a fresh database.';
        END IF;
        
        -- Zkontrolovat, zda už má novou strukturu s tenant_key
        IF EXISTS (
            SELECT FROM information_schema.columns 
            WHERE table_name = 'users_directory' 
            AND column_name = 'tenant_key' 
            AND data_type = 'text'
        ) THEN
            RAISE NOTICE 'V2 Migration skipped: users_directory already has new tenant_key structure';
            -- Ukončit bez chyby - tabulka už má správnou strukturu
            RETURN;
        END IF;
    END IF;
    
    RAISE NOTICE 'V2 Migration: Proceeding with fresh database initialization';
END $$;

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tenants table (optimized - minimal structure)
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key TEXT UNIQUE NOT NULL
);

-- Create index on tenant key for fast lookups
CREATE INDEX IF NOT EXISTS idx_tenants_key ON tenants(key);

-- Create Keycloak event log table for idempotence
CREATE TABLE IF NOT EXISTS kc_event_log (
    event_hash VARCHAR(64) PRIMARY KEY,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_kc_event_log_created_at ON kc_event_log(created_at);

-- Users directory table with NEW tenant_key structure AND Keycloak integration
CREATE TABLE IF NOT EXISTS users_directory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_key TEXT NOT NULL REFERENCES tenants(key) ON DELETE CASCADE,
    keycloak_user_id TEXT NULL,
    username TEXT NOT NULL,
    email TEXT NULL,
    first_name TEXT NULL,
    last_name TEXT NULL,
    display_name TEXT NULL,
    is_federated BOOLEAN NOT NULL DEFAULT FALSE,
    manager_id UUID NULL REFERENCES users_directory(id),
    status TEXT NULL DEFAULT 'ACTIVE',
    -- Keycloak sync columns
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ NULL,
    roles_json TEXT NULL,
    groups_json TEXT NULL,
    phone_number TEXT NULL,
    department TEXT NULL,
    title TEXT NULL,
    -- ✅ NOVÉ: Rozšířené organizační atributy
    position TEXT NULL,
    manager_username TEXT NULL,
    cost_center TEXT NULL,
    location TEXT NULL,
    phone TEXT NULL,
    -- ✅ NOVÉ: Zástupství
    deputy_username TEXT NULL,
    deputy_from DATE NULL,
    deputy_to DATE NULL,
    deputy_reason TEXT NULL,
    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create indexes for users_directory (basic)
CREATE INDEX IF NOT EXISTS idx_users_directory_tenant_key ON users_directory(tenant_key);
CREATE INDEX IF NOT EXISTS idx_users_directory_tenant_username ON users_directory(tenant_key, username);
CREATE INDEX IF NOT EXISTS idx_users_directory_tenant_email ON users_directory(tenant_key, email);
CREATE INDEX IF NOT EXISTS idx_users_directory_tenant_keycloak_user ON users_directory(tenant_key, keycloak_user_id);
CREATE INDEX IF NOT EXISTS idx_users_directory_manager ON users_directory(manager_id);

-- Create indexes for Keycloak sync columns
CREATE INDEX IF NOT EXISTS idx_users_directory_active ON users_directory(tenant_key, active);
CREATE INDEX IF NOT EXISTS idx_users_directory_deleted_at ON users_directory(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_directory_phone ON users_directory(tenant_key, phone_number) WHERE phone_number IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_directory_department ON users_directory(tenant_key, department) WHERE department IS NOT NULL;

-- ✅ NOVÉ: Indexy pro organizační atributy
CREATE INDEX IF NOT EXISTS idx_users_directory_position ON users_directory(tenant_key, position) WHERE position IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_directory_manager_username ON users_directory(tenant_key, manager_username) WHERE manager_username IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_directory_location ON users_directory(tenant_key, location) WHERE location IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_directory_cost_center ON users_directory(tenant_key, cost_center) WHERE cost_center IS NOT NULL;

-- ✅ NOVÉ: Indexy pro zástupství
CREATE INDEX IF NOT EXISTS idx_users_directory_deputy ON users_directory(tenant_key, deputy_username) WHERE deputy_username IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_directory_deputy_dates ON users_directory(deputy_from, deputy_to) WHERE deputy_from IS NOT NULL OR deputy_to IS NOT NULL;

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Function to cleanup old event logs (optional - for maintenance)
CREATE OR REPLACE FUNCTION cleanup_old_event_logs()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM kc_event_log 
    WHERE created_at < NOW() - INTERVAL '30 days';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Trigger for users_directory updated_at
DROP TRIGGER IF EXISTS update_users_directory_updated_at ON users_directory;
CREATE TRIGGER update_users_directory_updated_at BEFORE UPDATE ON users_directory
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Seed data: insert admin tenant (systémová správa)
INSERT INTO tenants (key) VALUES 
('admin')
ON CONFLICT (key) DO NOTHING;

-- Comment the tables for documentation
COMMENT ON TABLE tenants IS 'Minimal tenant registry for UUID foreign keys and webhook validation. Display names fetched from Keycloak realms.';
COMMENT ON COLUMN tenants.id IS 'UUID foreign key for efficient Hibernate filtering';
COMMENT ON COLUMN tenants.key IS 'Keycloak realm name - source of truth for tenant data';
COMMENT ON TABLE kc_event_log IS 'Log of processed Keycloak webhook events for idempotence';
COMMENT ON TABLE users_directory IS 'Unified user directory with Keycloak integration';
COMMENT ON COLUMN users_directory.active IS 'User active status from Keycloak (enabled flag)';
COMMENT ON COLUMN users_directory.deleted_at IS 'Timestamp when user was soft deleted';
COMMENT ON COLUMN users_directory.roles_json IS 'JSON representation of user roles from Keycloak';
COMMENT ON COLUMN users_directory.groups_json IS 'JSON representation of user groups from Keycloak';
COMMENT ON COLUMN users_directory.phone_number IS 'User phone number from Keycloak attributes';
COMMENT ON COLUMN users_directory.department IS 'User department from Keycloak attributes';
COMMENT ON COLUMN users_directory.title IS 'User job title from Keycloak attributes';

-- ✅ NOVÉ: Komentáře pro organizační atributy
COMMENT ON COLUMN users_directory.position IS 'User position/role from Keycloak attributes';
COMMENT ON COLUMN users_directory.manager_username IS 'Manager username from Keycloak attributes';
COMMENT ON COLUMN users_directory.cost_center IS 'Cost center from Keycloak attributes';
COMMENT ON COLUMN users_directory.location IS 'Office location from Keycloak attributes';
COMMENT ON COLUMN users_directory.phone IS 'Primary phone number from Keycloak attributes';

-- ✅ NOVÉ: Komentáře pro zástupství
COMMENT ON COLUMN users_directory.deputy_username IS 'Deputy username for absence coverage';
COMMENT ON COLUMN users_directory.deputy_from IS 'Start date of deputy assignment';
COMMENT ON COLUMN users_directory.deputy_to IS 'End date of deputy assignment';
COMMENT ON COLUMN users_directory.deputy_reason IS 'Reason for deputy assignment (vacation, sick leave, etc.)';