-- =====================================================
-- V1: CORE PLATFORM DATABASE INITIALIZATION
-- Kompletní init pro Core Platform DB s multitenancy
-- =====================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- For deterministic UUID generation

-- =====================================================
-- 1) TENANTS - Minimální registr pro multi-tenancy
-- =====================================================

CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY,  -- 🔧 REMOVED DEFAULT - používáme deterministic UUID
    key TEXT UNIQUE NOT NULL,
    keycloak_realm_id TEXT UNIQUE  -- 🆕 Keycloak realm UUID pro CDC mapping
);

CREATE INDEX idx_tenants_key ON tenants(key);
CREATE INDEX idx_tenants_keycloak_realm_id ON tenants(keycloak_realm_id);

COMMENT ON TABLE tenants IS 'Minimal tenant registry - display names fetched from Keycloak realms';
COMMENT ON COLUMN tenants.key IS 'Keycloak realm name (source of truth)';
COMMENT ON COLUMN tenants.keycloak_realm_id IS 'Keycloak realm UUID for CDC event mapping';

-- =====================================================
-- 🔐 FUNCTION: Generate deterministic UUID for tenant
-- Ensures consistent tenant IDs across environments
-- =====================================================
CREATE OR REPLACE FUNCTION generate_tenant_uuid(p_tenant_key VARCHAR)
RETURNS UUID AS $$
DECLARE
    v_composite VARCHAR;
    v_hash BYTEA;
    v_uuid UUID;
BEGIN
    -- Create composite key: "tenant:" + tenant_key
    v_composite := 'tenant:' || p_tenant_key;
    
    -- Generate SHA-256 hash
    v_hash := digest(v_composite, 'sha256');
    
    -- Extract first 16 bytes and convert to UUID (Version 4 format)
    v_uuid := CAST(
        lpad(to_hex(get_byte(v_hash, 0)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 1)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 2)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 3)), 2, '0') || '-' ||
        lpad(to_hex(get_byte(v_hash, 4)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 5)), 2, '0') || '-' ||
        '4' || lpad(to_hex(get_byte(v_hash, 6) & 15), 1, '0') ||
        lpad(to_hex(get_byte(v_hash, 7)), 2, '0') || '-' ||
        lpad(to_hex((get_byte(v_hash, 8) & 63) | 128), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 9)), 2, '0') || '-' ||
        lpad(to_hex(get_byte(v_hash, 10)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 11)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 12)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 13)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 14)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 15)), 2, '0')
    AS UUID);
    
    RETURN v_uuid;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Seed admin tenant with deterministic UUID (keycloak_realm_id will be populated by KeycloakInitializationService)
INSERT INTO tenants (id, key) 
VALUES (generate_tenant_uuid('admin'), 'admin') 
ON CONFLICT (key) DO NOTHING;

-- =====================================================
-- 2) USER DIRECTORY - Unified user directory
-- =====================================================

CREATE TABLE IF NOT EXISTS users_directory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_key TEXT NOT NULL REFERENCES tenants(key) ON DELETE CASCADE,
    keycloak_user_id TEXT,
    username TEXT NOT NULL,
    email TEXT,
    first_name TEXT,
    last_name TEXT,
    display_name TEXT,
    is_federated BOOLEAN NOT NULL DEFAULT FALSE,
    manager_id UUID REFERENCES users_directory(id),
    status TEXT DEFAULT 'ACTIVE',
    
    -- Keycloak sync fields
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    roles_json TEXT,
    groups_json TEXT,
    
    -- Organization attributes
    phone_number TEXT,
    department TEXT,
    title TEXT,
    position TEXT,
    manager_username TEXT,
    cost_center TEXT,
    location TEXT,
    phone TEXT,
    
    -- Deputy/substitute management
    deputy_username TEXT,
    deputy_from DATE,
    deputy_to DATE,
    deputy_reason TEXT,
    
    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Users directory indexes
CREATE INDEX idx_users_directory_tenant_key ON users_directory(tenant_key);
CREATE INDEX idx_users_directory_tenant_username ON users_directory(tenant_key, username);
CREATE INDEX idx_users_directory_tenant_email ON users_directory(tenant_key, email);
CREATE INDEX idx_users_directory_tenant_keycloak_user ON users_directory(tenant_key, keycloak_user_id);
CREATE INDEX idx_users_directory_manager ON users_directory(manager_id);
CREATE INDEX idx_users_directory_active ON users_directory(tenant_key, active);
CREATE INDEX idx_users_directory_deleted_at ON users_directory(deleted_at) WHERE deleted_at IS NOT NULL;

-- =====================================================
-- 3) ROLES - Keycloak role synchronization
-- =====================================================

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_role_id TEXT NOT NULL,
    tenant_key TEXT NOT NULL REFERENCES tenants(key) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    composite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_roles_keycloak_id_tenant UNIQUE (keycloak_role_id, tenant_key)
);

CREATE INDEX idx_roles_tenant_key ON roles(tenant_key);
CREATE INDEX idx_roles_keycloak_id ON roles(keycloak_role_id);
CREATE INDEX idx_roles_name ON roles(tenant_key, name);
CREATE INDEX idx_roles_tenant_name ON roles(tenant_key, name);
CREATE INDEX idx_roles_composite ON roles(composite) WHERE composite = true;

-- =====================================================
-- 4) ROLE COMPOSITES - Composite role mappings
-- =====================================================

CREATE TABLE IF NOT EXISTS role_composites (
    parent_role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    child_role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    
    PRIMARY KEY (parent_role_id, child_role_id)
);

CREATE INDEX idx_role_composites_parent ON role_composites(parent_role_id);
CREATE INDEX idx_role_composites_child ON role_composites(child_role_id);

-- =====================================================
-- 5) ROLE HIERARCHY - Legacy composite roles (deprecated, use role_composites)
-- =====================================================

CREATE TABLE IF NOT EXISTS role_hierarchy (
    parent_role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    child_role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    
    PRIMARY KEY (parent_role_id, child_role_id)
);

CREATE INDEX idx_role_hierarchy_parent ON role_hierarchy(parent_role_id);
CREATE INDEX idx_role_hierarchy_child ON role_hierarchy(child_role_id);

-- =====================================================
-- 6) GROUPS - Keycloak group synchronization
-- =====================================================

CREATE TABLE IF NOT EXISTS groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_group_id TEXT NOT NULL,
    tenant_key TEXT NOT NULL REFERENCES tenants(key) ON DELETE CASCADE,
    name TEXT NOT NULL,
    path TEXT NOT NULL,
    parent_group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_groups_keycloak_id_tenant UNIQUE (keycloak_group_id, tenant_key),
    CONSTRAINT uk_group_path_tenant UNIQUE (path, tenant_key)
);

CREATE INDEX idx_groups_tenant_key ON groups(tenant_key);
CREATE INDEX idx_groups_keycloak_id ON groups(keycloak_group_id);
CREATE INDEX idx_groups_path ON groups(tenant_key, path);
CREATE INDEX idx_groups_parent ON groups(parent_group_id) WHERE parent_group_id IS NOT NULL;
CREATE INDEX idx_groups_tenant_path ON groups(tenant_key, path);
CREATE INDEX idx_groups_tenant_name ON groups(tenant_key, name);

-- =====================================================
-- 7) USER-ROLE MAPPINGS
-- =====================================================

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users_directory(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- =====================================================
-- 8) USER-GROUP MAPPINGS
-- =====================================================

CREATE TABLE IF NOT EXISTS user_groups (
    user_id UUID NOT NULL REFERENCES users_directory(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    PRIMARY KEY (user_id, group_id)
);

CREATE INDEX idx_user_groups_user_id ON user_groups(user_id);
CREATE INDEX idx_user_groups_group_id ON user_groups(group_id);

-- =====================================================
-- 9) EVENT LOG - Idempotence tracking
-- =====================================================

CREATE TABLE IF NOT EXISTS kc_event_log (
    event_hash VARCHAR(64) PRIMARY KEY,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_kc_event_log_created_at ON kc_event_log(created_at);

-- =====================================================
-- 10) HELPER FUNCTIONS
-- =====================================================

-- Auto-update timestamp trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Cleanup old event logs
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

-- =====================================================
-- 🔐 FUNCTION: Regenerate deterministic UUID for roles
-- Used for migrating existing roles to deterministic IDs
-- =====================================================
CREATE OR REPLACE FUNCTION regenerate_role_uuid(
    p_keycloak_role_id VARCHAR,
    p_tenant_key VARCHAR
) RETURNS UUID AS $$
DECLARE
    v_composite VARCHAR;
    v_hash BYTEA;
    v_uuid UUID;
BEGIN
    -- Create composite key: "role:tenant:keycloak_id"
    v_composite := 'role:' || p_tenant_key || ':' || p_keycloak_role_id;
    
    -- Generate SHA-256 hash
    v_hash := digest(v_composite, 'sha256');
    
    -- Extract first 16 bytes and convert to UUID
    v_uuid := CAST(
        lpad(to_hex(get_byte(v_hash, 0)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 1)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 2)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 3)), 2, '0') || '-' ||
        lpad(to_hex(get_byte(v_hash, 4)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 5)), 2, '0') || '-' ||
        '4' || lpad(to_hex(get_byte(v_hash, 6) & 15), 1, '0') ||
        lpad(to_hex(get_byte(v_hash, 7)), 2, '0') || '-' ||
        lpad(to_hex((get_byte(v_hash, 8) & 63) | 128), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 9)), 2, '0') || '-' ||
        lpad(to_hex(get_byte(v_hash, 10)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 11)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 12)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 13)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 14)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 15)), 2, '0')
    AS UUID);
    
    RETURN v_uuid;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- =====================================================
-- 🔐 FUNCTION: Regenerate deterministic UUID for groups
-- =====================================================
CREATE OR REPLACE FUNCTION regenerate_group_uuid(
    p_keycloak_group_id VARCHAR,
    p_tenant_key VARCHAR
) RETURNS UUID AS $$
DECLARE
    v_composite VARCHAR;
    v_hash BYTEA;
    v_uuid UUID;
BEGIN
    -- Create composite key: "group:tenant:keycloak_id"
    v_composite := 'group:' || p_tenant_key || ':' || p_keycloak_group_id;
    
    -- Generate SHA-256 hash
    v_hash := digest(v_composite, 'sha256');
    
    -- Extract first 16 bytes and convert to UUID
    v_uuid := CAST(
        lpad(to_hex(get_byte(v_hash, 0)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 1)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 2)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 3)), 2, '0') || '-' ||
        lpad(to_hex(get_byte(v_hash, 4)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 5)), 2, '0') || '-' ||
        '4' || lpad(to_hex(get_byte(v_hash, 6) & 15), 1, '0') ||
        lpad(to_hex(get_byte(v_hash, 7)), 2, '0') || '-' ||
        lpad(to_hex((get_byte(v_hash, 8) & 63) | 128), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 9)), 2, '0') || '-' ||
        lpad(to_hex(get_byte(v_hash, 10)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 11)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 12)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 13)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 14)), 2, '0') ||
        lpad(to_hex(get_byte(v_hash, 15)), 2, '0')
    AS UUID);
    
    RETURN v_uuid;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- =====================================================
-- 11) TRIGGERS
-- =====================================================

CREATE TRIGGER update_users_directory_updated_at 
    BEFORE UPDATE ON users_directory
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_roles_updated_at 
    BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_groups_updated_at 
    BEFORE UPDATE ON groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 12) COMMENTS
-- =====================================================

COMMENT ON TABLE users_directory IS 'Unified user directory with Keycloak integration';
COMMENT ON TABLE roles IS 'Synchronized roles from Keycloak';
COMMENT ON TABLE groups IS 'Synchronized groups from Keycloak with hierarchical structure';
COMMENT ON TABLE user_roles IS 'Many-to-Many mapping between users and roles';
COMMENT ON TABLE user_groups IS 'Many-to-Many mapping between users and groups';
COMMENT ON TABLE kc_event_log IS 'Event log for idempotence tracking';
COMMENT ON TABLE role_composites IS 'Composite role mappings (parent contains child roles)';

-- =====================================================
-- VERIFICATION
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '✅ V1 Core Platform DB initialized successfully';
    RAISE NOTICE '📊 Tables created: tenants, users_directory, roles, role_composites, groups, user_roles, user_groups, kc_event_log';
    RAISE NOTICE '🔐 Deterministic UUID functions created for backup/restore consistency';
END $$;
