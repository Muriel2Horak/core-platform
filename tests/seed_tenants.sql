-- Seed data for multitenancy testing
-- This script creates test tenants and users for smoke tests

-- Ensure pgcrypto extension exists (for UUID generation)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Insert test tenants (s povinným realm sloupcem)
INSERT INTO tenants (key, name, realm) VALUES ('test-tenant', 'Test Tenant Company', 'core-platform');
INSERT INTO tenants (key, name, realm) VALUES ('company-b', 'Company B Ltd', 'core-platform');

-- Get tenant IDs for foreign key references
DO $$
DECLARE
    tenant1_id UUID;
    tenant2_id UUID;
BEGIN
    -- Get tenant IDs
    SELECT id INTO tenant1_id FROM tenants WHERE key = 'test-tenant';
    SELECT id INTO tenant2_id FROM tenants WHERE key = 'company-b';
    
    -- Insert users for test-tenant (bez ON CONFLICT, protože constraint neexistuje)
    INSERT INTO users_directory (tenant_id, username, email, first_name, last_name, display_name, is_federated, status)
    VALUES 
        (tenant1_id, 'alice_test', 'alice@test-tenant.com', 'Alice', 'Test', 'Alice Test', false, 'ACTIVE'),
        (tenant1_id, 'bob_test', 'bob@test-tenant.com', 'Bob', 'Test', 'Bob Test', false, 'ACTIVE');
    
    -- Insert users for company-b
    INSERT INTO users_directory (tenant_id, username, email, first_name, last_name, display_name, is_federated, status)
    VALUES 
        (tenant2_id, 'charlie_companyb', 'charlie@company-b.com', 'Charlie', 'Company', 'Charlie Company', false, 'ACTIVE');
    
END $$;