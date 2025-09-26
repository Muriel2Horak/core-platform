-- V2__init_multitenancy_and_user_directory.sql
-- Multitenancy and User Directory initialization

-- Enable UUID extension if not exists
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tenants table
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    realm TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create index on tenant key for fast lookups
CREATE INDEX idx_tenants_key ON tenants(key);

-- Users directory table
CREATE TABLE users_directory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    keycloak_user_id TEXT NULL,
    username TEXT NOT NULL,
    email TEXT NULL,
    first_name TEXT NULL,
    last_name TEXT NULL,
    display_name TEXT NULL,
    is_federated BOOLEAN NOT NULL DEFAULT FALSE,
    manager_id UUID NULL REFERENCES users_directory(id),
    status TEXT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create indexes for users_directory
CREATE INDEX idx_users_directory_tenant_id ON users_directory(tenant_id);
CREATE INDEX idx_users_directory_tenant_username ON users_directory(tenant_id, username);
CREATE INDEX idx_users_directory_tenant_email ON users_directory(tenant_id, email);
CREATE INDEX idx_users_directory_tenant_keycloak_user ON users_directory(tenant_id, keycloak_user_id);
CREATE INDEX idx_users_directory_manager ON users_directory(manager_id);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for users_directory updated_at
CREATE TRIGGER update_users_directory_updated_at BEFORE UPDATE ON users_directory
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Seed data: insert test tenant
INSERT INTO tenants (key, name, realm) VALUES 
('test-tenant', 'Test Tenant', 'core-platform')
ON CONFLICT (key) DO NOTHING;