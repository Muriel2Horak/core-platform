-- Seed data for multitenancy testing
-- This script ensures core-platform tenant exists and creates test users for smoke tests

-- Ensure pgcrypto extension exists (for UUID generation)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- FIXED: Ensure core-platform tenant exists (instead of creating test tenants)
INSERT INTO tenants (key, name, realm) 
VALUES ('core-platform', 'Core Platform', 'core-platform')
ON CONFLICT (key) DO NOTHING;

-- Get core-platform tenant ID for foreign key references
DO $$
DECLARE
    core_tenant_id UUID;
BEGIN
    -- Get core-platform tenant ID
    SELECT id INTO core_tenant_id FROM tenants WHERE key = 'core-platform';
    
    -- Insert test users for core-platform tenant (for testing user directory search)
    INSERT INTO users_directory (tenant_id, username, email, first_name, last_name, display_name, is_federated, status)
    VALUES 
        (core_tenant_id, 'alice_core', 'alice@core-platform.local', 'Alice', 'Core', 'Alice Core', false, 'ACTIVE'),
        (core_tenant_id, 'bob_core', 'bob@core-platform.local', 'Bob', 'Core', 'Bob Core', false, 'ACTIVE')
    ON CONFLICT (tenant_id, username) DO NOTHING;
    
END $$;