-- Seed data for multitenancy smoke tests
-- This script ensures admin tenant exists and creates test users for smoke tests

-- Ensure pgcrypto extension exists (for UUID generation)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Insert admin tenant if not exists (renamed from core-platform)
INSERT INTO tenants (key, created_at, updated_at) 
VALUES ('admin', NOW(), NOW()) 
ON CONFLICT (key) DO NOTHING;

-- Get admin tenant ID for foreign key references
DO $$
DECLARE
    admin_tenant_id UUID;
BEGIN
    -- Get admin tenant ID
    SELECT id INTO admin_tenant_id FROM tenants WHERE key = 'admin';
    
    -- Insert test users for admin tenant (for testing user directory search)
    INSERT INTO users_directory (tenant_id, username, email, first_name, last_name, display_name, is_federated, status)
    VALUES 
        (admin_tenant_id, 'alice_core', 'alice@core-platform.local', 'Alice', 'Core', 'Alice Core', false, 'ACTIVE'),
        (admin_tenant_id, 'bob_core', 'bob@core-platform.local', 'Bob', 'Core', 'Bob Core', false, 'ACTIVE')
    ON CONFLICT (tenant_id, username) DO NOTHING;
    
END $$;