-- =====================================================
-- V1: CORE PLATFORM DATABASE INITIALIZATION
-- Kompletní init pro DEV/CI prostředí
-- Obsahuje: Core Platform + Metamodel + Workflow + Documents
-- =====================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- For deterministic UUID generation

-- =====================================================
-- SECTION 1: TENANTS & USER DIRECTORY
-- =====================================================

-- 1.1) TENANTS - Minimální registr pro multi-tenancy
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY,
    key TEXT UNIQUE NOT NULL,
    keycloak_realm_id TEXT UNIQUE
);

CREATE INDEX idx_tenants_key ON tenants(key);
CREATE INDEX idx_tenants_keycloak_realm_id ON tenants(keycloak_realm_id);

COMMENT ON TABLE tenants IS 'Minimal tenant registry - display names fetched from Keycloak realms';
COMMENT ON COLUMN tenants.key IS 'Keycloak realm name (source of truth)';
COMMENT ON COLUMN tenants.keycloak_realm_id IS 'Keycloak realm UUID for CDC event mapping';

-- 1.2) FUNCTION: Generate deterministic UUID for tenant
CREATE OR REPLACE FUNCTION generate_tenant_uuid(p_tenant_key VARCHAR)
RETURNS UUID AS $$
DECLARE
    v_composite VARCHAR;
    v_hash BYTEA;
    v_uuid UUID;
BEGIN
    v_composite := 'tenant:' || p_tenant_key;
    v_hash := digest(v_composite, 'sha256');
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

-- Seed admin tenant
INSERT INTO tenants (id, key) 
VALUES (generate_tenant_uuid('admin'), 'admin') 
ON CONFLICT (key) DO NOTHING;

-- 1.3) USER DIRECTORY - Unified user directory
CREATE TABLE IF NOT EXISTS users_directory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
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
    
    -- Metamodel fields
    version BIGINT DEFAULT 0 NOT NULL,
    
    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_users_directory_tenant ON users_directory(tenant_id);
CREATE INDEX idx_users_directory_tenant_username ON users_directory(tenant_id, username);
CREATE INDEX idx_users_directory_tenant_email ON users_directory(tenant_id, email);
CREATE INDEX idx_users_directory_tenant_keycloak_user ON users_directory(tenant_id, keycloak_user_id);
CREATE INDEX idx_users_directory_manager ON users_directory(manager_id);
CREATE INDEX idx_users_directory_active ON users_directory(tenant_id, active);
CREATE INDEX idx_users_directory_deleted_at ON users_directory(deleted_at) WHERE deleted_at IS NOT NULL;

-- =====================================================
-- SECTION 2: ROLES & GROUPS
-- =====================================================

-- 2.1) ROLES - Keycloak role synchronization
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_role_id TEXT NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    composite BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_roles_keycloak_id_tenant UNIQUE (keycloak_role_id, tenant_id)
);

CREATE INDEX idx_roles_tenant ON roles(tenant_id);
CREATE INDEX idx_roles_keycloak_id ON roles(keycloak_role_id);
CREATE INDEX idx_roles_name ON roles(tenant_id, name);
CREATE INDEX idx_roles_composite ON roles(composite) WHERE composite = true;

-- 2.2) ROLE COMPOSITES
CREATE TABLE IF NOT EXISTS role_composites (
    parent_role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    child_role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (parent_role_id, child_role_id)
);

CREATE INDEX idx_role_composites_parent ON role_composites(parent_role_id);
CREATE INDEX idx_role_composites_child ON role_composites(child_role_id);

-- 2.3) ROLE HIERARCHY (deprecated, use role_composites)
CREATE TABLE IF NOT EXISTS role_hierarchy (
    parent_role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    child_role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (parent_role_id, child_role_id)
);

CREATE INDEX idx_role_hierarchy_parent ON role_hierarchy(parent_role_id);
CREATE INDEX idx_role_hierarchy_child ON role_hierarchy(child_role_id);

-- 2.4) GROUPS - Keycloak group synchronization
CREATE TABLE IF NOT EXISTS groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_group_id TEXT NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    path TEXT NOT NULL,
    parent_group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_groups_keycloak_id_tenant UNIQUE (keycloak_group_id, tenant_id),
    CONSTRAINT uk_group_path_tenant UNIQUE (path, tenant_id)
);

CREATE INDEX idx_groups_tenant ON groups(tenant_id);
CREATE INDEX idx_groups_keycloak_id ON groups(keycloak_group_id);
CREATE INDEX idx_groups_path ON groups(tenant_id, path);
CREATE INDEX idx_groups_parent ON groups(parent_group_id) WHERE parent_group_id IS NOT NULL;
CREATE INDEX idx_groups_tenant_name ON groups(tenant_id, name);

-- =====================================================
-- SECTION 3: USER MAPPINGS
-- =====================================================

-- 3.1) USER-ROLE MAPPINGS
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users_directory(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- 3.2) USER-GROUP MAPPINGS
CREATE TABLE IF NOT EXISTS user_groups (
    user_id UUID NOT NULL REFERENCES users_directory(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, group_id)
);

CREATE INDEX idx_user_groups_user_id ON user_groups(user_id);
CREATE INDEX idx_user_groups_group_id ON user_groups(group_id);

-- =====================================================
-- SECTION 4: EVENT LOG & SYNC TRACKING
-- =====================================================

-- 4.1) EVENT LOG - Idempotence tracking
CREATE TABLE IF NOT EXISTS kc_event_log (
    event_hash VARCHAR(64) PRIMARY KEY,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_kc_event_log_created_at ON kc_event_log(created_at);

-- 4.2) SYNC EXECUTION TRACKING
CREATE TABLE IF NOT EXISTS sync_executions (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    total_items INTEGER,
    processed_items INTEGER,
    initiated_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS sync_execution_errors (
    sync_execution_id VARCHAR(36) NOT NULL,
    error_message VARCHAR(1000),
    CONSTRAINT fk_sync_errors FOREIGN KEY (sync_execution_id) REFERENCES sync_executions(id) ON DELETE CASCADE
);

CREATE INDEX idx_sync_status ON sync_executions(status);
CREATE INDEX idx_sync_tenant ON sync_executions(tenant_id);
CREATE INDEX idx_sync_start_time ON sync_executions(start_time DESC);
CREATE INDEX idx_sync_type ON sync_executions(type);
CREATE INDEX idx_sync_status_tenant ON sync_executions(status, tenant_id);

COMMENT ON TABLE sync_executions IS 'Historie synchronizačních operací z Keycloak';
COMMENT ON TABLE sync_execution_errors IS 'Chybové zprávy synchronizací';

-- =====================================================
-- SECTION 5: METAMODEL - EDIT LOCKS & USER PROFILE
-- =====================================================

-- 5.1) EDIT LOCKS - Soft locking
CREATE TABLE IF NOT EXISTS edit_locks (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    lock_type TEXT NOT NULL CHECK (lock_type IN ('soft')),
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    
    CONSTRAINT uq_lock UNIQUE (tenant_id, entity_type, entity_id)
);

CREATE INDEX idx_lock_exp ON edit_locks(expires_at);
CREATE INDEX idx_lock_user ON edit_locks(user_id);
CREATE INDEX idx_lock_tenant_entity ON edit_locks(tenant_id, entity_type, entity_id);

COMMENT ON TABLE edit_locks IS 'Metamodel Phase 1: Soft locks with auto-expiry';

-- 5.2) USER PROFILE - Reference entity
CREATE TABLE IF NOT EXISTS user_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id TEXT NOT NULL REFERENCES tenants(key) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users_directory(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    email TEXT,
    phone TEXT,
    department TEXT,
    position TEXT,
    bio TEXT,
    avatar_url TEXT,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    CONSTRAINT uq_user_profile_user UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_user_profile_tenant ON user_profile(tenant_id);
CREATE INDEX idx_user_profile_user ON user_profile(user_id);
CREATE INDEX idx_user_profile_email ON user_profile(email);

COMMENT ON TABLE user_profile IS 'Metamodel Phase 1: Reference entity with ABAC policies';

-- =====================================================
-- SECTION 6: WORKFLOW & STATE MANAGEMENT (Phase 2.2)
-- =====================================================

-- 6.1) ENTITY STATE - Current state tracking
CREATE TABLE IF NOT EXISTS entity_state (
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    state_code TEXT NOT NULL,
    since TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sla_minutes INTEGER,
    
    PRIMARY KEY (entity_type, entity_id, tenant_id)
);

CREATE INDEX idx_entity_state_tenant ON entity_state(tenant_id);
CREATE INDEX idx_entity_state_type ON entity_state(entity_type);
CREATE INDEX idx_entity_state_code ON entity_state(state_code);
CREATE INDEX idx_entity_state_sla ON entity_state(tenant_id, entity_type, sla_minutes) 
    WHERE sla_minutes IS NOT NULL;

-- 6.2) STATE TRANSITION - Workflow configuration
CREATE TABLE IF NOT EXISTS state_transition (
    id BIGSERIAL PRIMARY KEY,
    entity_type TEXT NOT NULL,
    from_code TEXT,
    to_code TEXT NOT NULL,
    code TEXT NOT NULL,
    guard JSONB,
    sla_minutes INTEGER,
    
    CONSTRAINT uq_transition UNIQUE (entity_type, code)
);

CREATE INDEX idx_transition_type ON state_transition(entity_type);
CREATE INDEX idx_transition_from ON state_transition(entity_type, from_code);
CREATE INDEX idx_transition_to ON state_transition(entity_type, to_code);
CREATE INDEX idx_transition_guard ON state_transition USING gin(guard);

-- 6.3) ENTITY STATE LOG - Audit trail
CREATE TABLE IF NOT EXISTS entity_state_log (
    id BIGSERIAL PRIMARY KEY,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    from_code TEXT,
    to_code TEXT NOT NULL,
    transition_code TEXT NOT NULL,
    changed_by TEXT NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_state_log_entity ON entity_state_log(entity_type, entity_id, tenant_id);
CREATE INDEX idx_state_log_changed_at ON entity_state_log(changed_at);
CREATE INDEX idx_state_log_tenant ON entity_state_log(tenant_id);

-- =====================================================
-- SECTION 7: DOCUMENTS & FULLTEXT SEARCH (Phase 2.3)
-- =====================================================

-- 7.1) DOCUMENT - Document metadata
CREATE TABLE IF NOT EXISTS document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    filename TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key TEXT NOT NULL,
    checksum_sha256 TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    uploaded_by TEXT NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_document_tenant ON document(tenant_id);
CREATE INDEX idx_document_entity ON document(entity_type, entity_id, tenant_id);
CREATE INDEX idx_document_storage_key ON document(storage_key);
CREATE INDEX idx_document_uploaded_at ON document(uploaded_at);

-- 7.2) DOCUMENT INDEX - Fulltext search
CREATE TABLE IF NOT EXISTS document_index (
    document_id UUID PRIMARY KEY REFERENCES document(id) ON DELETE CASCADE,
    tenant_id TEXT NOT NULL,
    extracted_text TEXT,
    search_vector TSVECTOR,
    indexed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_document_index_tenant ON document_index(tenant_id);
CREATE INDEX idx_document_index_fts ON document_index USING gin(search_vector);

-- =====================================================
-- SECTION 8: PRESENCE ANALYTICS (Phase 2.1)
-- =====================================================

CREATE TABLE IF NOT EXISTS presence_activity (
    id BIGSERIAL PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    entity_type TEXT,
    entity_id TEXT,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_presence_activity_tenant ON presence_activity(tenant_id);
CREATE INDEX idx_presence_activity_user ON presence_activity(user_id);
CREATE INDEX idx_presence_activity_timestamp ON presence_activity(timestamp);
CREATE INDEX idx_presence_activity_cleanup ON presence_activity(timestamp);

-- =====================================================
-- SECTION 9: ROW LEVEL SECURITY (RLS)
-- =====================================================

-- Enable RLS on all tenant-isolated tables
ALTER TABLE users_directory ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_profile ENABLE ROW LEVEL SECURITY;

-- Create tenant isolation policies
CREATE POLICY tenant_isolation_users ON users_directory
    USING (tenant_id::text = current_setting('app.tenant_id', true));

CREATE POLICY tenant_isolation_roles ON roles
    USING (tenant_id::text = current_setting('app.tenant_id', true));

CREATE POLICY tenant_isolation_groups ON groups
    USING (tenant_id::text = current_setting('app.tenant_id', true));

CREATE POLICY tenant_isolation_user_profile ON user_profile
    USING (tenant_id = current_setting('app.tenant_id', true));

-- =====================================================
-- SECTION 10: HELPER FUNCTIONS
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

-- Deterministic UUID for roles
CREATE OR REPLACE FUNCTION regenerate_role_uuid(
    p_keycloak_role_id VARCHAR,
    p_tenant_key VARCHAR
) RETURNS UUID AS $$
DECLARE
    v_composite VARCHAR;
    v_hash BYTEA;
    v_uuid UUID;
BEGIN
    v_composite := 'role:' || p_tenant_key || ':' || p_keycloak_role_id;
    v_hash := digest(v_composite, 'sha256');
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

-- Deterministic UUID for groups
CREATE OR REPLACE FUNCTION regenerate_group_uuid(
    p_keycloak_group_id VARCHAR,
    p_tenant_key VARCHAR
) RETURNS UUID AS $$
DECLARE
    v_composite VARCHAR;
    v_hash BYTEA;
    v_uuid UUID;
BEGIN
    v_composite := 'group:' || p_tenant_key || ':' || p_keycloak_group_id;
    v_hash := digest(v_composite, 'sha256');
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

-- Version auto-increment trigger
CREATE OR REPLACE FUNCTION increment_version()
RETURNS TRIGGER AS $$
BEGIN
    NEW.version = OLD.version + 1;
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- SLA status calculation
CREATE OR REPLACE FUNCTION calculate_sla_status(
    p_since TIMESTAMPTZ,
    p_sla_minutes INTEGER
) RETURNS TEXT AS $$
DECLARE
    elapsed_minutes INTEGER;
    warn_threshold INTEGER;
BEGIN
    IF p_sla_minutes IS NULL THEN
        RETURN 'NONE';
    END IF;
    
    elapsed_minutes := EXTRACT(EPOCH FROM (NOW() - p_since)) / 60;
    warn_threshold := p_sla_minutes * 0.8;
    
    IF elapsed_minutes >= p_sla_minutes THEN
        RETURN 'BREACH';
    ELSIF elapsed_minutes >= warn_threshold THEN
        RETURN 'WARN';
    ELSE
        RETURN 'OK';
    END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- =====================================================
-- SECTION 11: TRIGGERS
-- =====================================================

-- Updated_at triggers
CREATE TRIGGER update_users_directory_updated_at 
    BEFORE UPDATE ON users_directory
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_roles_updated_at 
    BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_groups_updated_at 
    BEFORE UPDATE ON groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Version increment triggers
CREATE TRIGGER user_profile_version_trigger
    BEFORE UPDATE ON user_profile
    FOR EACH ROW EXECUTE FUNCTION increment_version();

CREATE TRIGGER users_directory_version_trigger
    BEFORE UPDATE ON users_directory
    FOR EACH ROW EXECUTE FUNCTION increment_version();

CREATE TRIGGER roles_version_trigger
    BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION increment_version();

CREATE TRIGGER groups_version_trigger
    BEFORE UPDATE ON groups
    FOR EACH ROW EXECUTE FUNCTION increment_version();

-- =====================================================
-- SECTION 12: SAMPLE DATA FOR DEV/CI
-- =====================================================

-- Sample state transitions for UserProfile entity
INSERT INTO state_transition (entity_type, from_code, to_code, code, guard, sla_minutes) VALUES
    ('UserProfile', NULL, 'draft', 'CREATE_DRAFT', NULL, NULL),
    ('UserProfile', 'draft', 'active', 'ACTIVATE', '{"expression": "hasRole(''CORE_ROLE_ADMIN'')"}', 60),
    ('UserProfile', 'active', 'suspended', 'SUSPEND', '{"expression": "hasRole(''CORE_ROLE_ADMIN'')"}', 30),
    ('UserProfile', 'suspended', 'active', 'REACTIVATE', '{"expression": "hasRole(''CORE_ROLE_ADMIN'')"}', 30),
    ('UserProfile', 'active', 'archived', 'ARCHIVE', '{"expression": "hasRole(''CORE_ROLE_ADMIN'')"}', NULL),
    ('UserProfile', 'suspended', 'archived', 'ARCHIVE_SUSPENDED', '{"expression": "hasRole(''CORE_ROLE_ADMIN'')"}', NULL);

-- Sample user profiles (for admin tenant)
INSERT INTO user_profile (tenant_id, user_id, full_name, email, department, position)
SELECT 
    'admin' as tenant_id,
    ud.id as user_id,
    COALESCE(ud.display_name, ud.first_name || ' ' || ud.last_name) as full_name,
    ud.email,
    ud.department,
    ud.position
FROM users_directory ud
WHERE ud.tenant_id = generate_tenant_uuid('admin')
  AND NOT EXISTS (SELECT 1 FROM user_profile up WHERE up.user_id = ud.id)
LIMIT 5;

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON TABLE users_directory IS 'Unified user directory with Keycloak integration';
COMMENT ON TABLE roles IS 'Synchronized roles from Keycloak';
COMMENT ON TABLE groups IS 'Synchronized groups from Keycloak with hierarchical structure';
COMMENT ON TABLE user_roles IS 'Many-to-Many mapping between users and roles';
COMMENT ON TABLE user_groups IS 'Many-to-Many mapping between users and groups';
COMMENT ON TABLE kc_event_log IS 'Event log for idempotence tracking';
COMMENT ON TABLE role_composites IS 'Composite role mappings (parent contains child roles)';
COMMENT ON TABLE entity_state IS 'Current state of entities with timestamp (Phase 2.2)';
COMMENT ON TABLE state_transition IS 'Allowed state transitions with guards and SLA (Phase 2.2)';
COMMENT ON TABLE entity_state_log IS 'Audit log of all state changes (Phase 2.2)';
COMMENT ON TABLE document IS 'Document metadata linked to MinIO storage (Phase 2.3)';
COMMENT ON TABLE document_index IS 'Fulltext search index for documents (Phase 2.3)';
COMMENT ON TABLE presence_activity IS 'Presence activity log for analytics (Phase 2.1)';

-- =====================================================
-- PHASE 3: REPORTING & ANALYTICS
-- =====================================================

-- Saved report views
CREATE TABLE report_view (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('private', 'group', 'tenant', 'global')),
    owner_id UUID REFERENCES users_directory(id) ON DELETE SET NULL,
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    definition JSONB NOT NULL,
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    CHECK (
        (scope = 'private' AND owner_id IS NOT NULL) OR
        (scope = 'group' AND group_id IS NOT NULL) OR
        (scope IN ('tenant', 'global'))
    )
);

CREATE INDEX idx_report_view_tenant ON report_view(tenant_id);
CREATE INDEX idx_report_view_owner ON report_view(owner_id) WHERE owner_id IS NOT NULL;
CREATE INDEX idx_report_view_entity ON report_view(entity);
CREATE INDEX idx_report_view_scope ON report_view(scope);

-- Bulk update jobs
CREATE TABLE reporting_job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users_directory(id),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity VARCHAR(255) NOT NULL,
    where_json JSONB NOT NULL,
    patch_json JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED')),
    dry_run BOOLEAN NOT NULL DEFAULT false,
    total_rows INTEGER,
    affected_rows INTEGER,
    message TEXT,
    idempotency_key VARCHAR(255),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_reporting_job_tenant ON reporting_job(tenant_id);
CREATE INDEX idx_reporting_job_status ON reporting_job(status);
CREATE INDEX idx_reporting_job_created_by ON reporting_job(created_by);
CREATE INDEX idx_reporting_job_idempotency ON reporting_job(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_reporting_job_created_at ON reporting_job(created_at DESC);

-- Job execution events
CREATE TABLE reporting_job_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES reporting_job(id) ON DELETE CASCADE,
    ts TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    level VARCHAR(10) NOT NULL CHECK (level IN ('INFO', 'WARN', 'ERROR')),
    message TEXT NOT NULL
);

CREATE INDEX idx_reporting_job_event_job ON reporting_job_event(job_id, ts);

-- Audit change log
CREATE TABLE audit_change (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ts TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    actor UUID NOT NULL REFERENCES users_directory(id),
    entity VARCHAR(255) NOT NULL,
    entity_id UUID NOT NULL,
    op VARCHAR(10) NOT NULL CHECK (op IN ('INSERT', 'UPDATE', 'DELETE')),
    before JSONB,
    after JSONB,
    job_id UUID REFERENCES reporting_job(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_change_tenant ON audit_change(tenant_id);
CREATE INDEX idx_audit_change_entity ON audit_change(entity, entity_id);
CREATE INDEX idx_audit_change_actor ON audit_change(actor);
CREATE INDEX idx_audit_change_job ON audit_change(job_id) WHERE job_id IS NOT NULL;
CREATE INDEX idx_audit_change_ts ON audit_change(ts DESC);

COMMENT ON TABLE report_view IS 'Saved report views with scope-based sharing (Phase 3.4)';
COMMENT ON TABLE reporting_job IS 'Bulk update jobs with idempotency (Phase 3.5)';
COMMENT ON TABLE reporting_job_event IS 'Job execution events (Phase 3.5)';
COMMENT ON TABLE audit_change IS 'Audit log for all entity changes (Phase 3.5)';

-- =====================================================
-- SECTION 7: STREAMING INFRASTRUCTURE
-- Command Queue + Outbox Pattern + Work State Locking
-- =====================================================

-- 7.1) COMMAND_QUEUE - Asynchronous command queue with priority and retry logic
CREATE TABLE IF NOT EXISTS command_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL CHECK (operation IN ('CREATE', 'UPDATE', 'DELETE', 'BULK_UPDATE')),
    payload JSONB NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'normal' CHECK (priority IN ('critical', 'high', 'normal', 'bulk')),
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'processing', 'completed', 'failed', 'dlq')),
    available_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    operation_id UUID,
    correlation_id UUID NOT NULL DEFAULT gen_random_uuid(),
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_command_queue_status_priority_available 
    ON command_queue(status, priority, available_at) 
    WHERE status = 'pending';

CREATE INDEX idx_command_queue_entity_entity_id 
    ON command_queue(entity, entity_id);

CREATE INDEX idx_command_queue_operation_id 
    ON command_queue(operation_id) 
    WHERE operation_id IS NOT NULL;

CREATE INDEX idx_command_queue_correlation_id 
    ON command_queue(correlation_id);

CREATE INDEX idx_command_queue_tenant_id 
    ON command_queue(tenant_id);

COMMENT ON TABLE command_queue IS 'Async command queue with priority-based processing and retry logic';
COMMENT ON COLUMN command_queue.priority IS 'critical > high > normal > bulk';
COMMENT ON COLUMN command_queue.status IS 'pending → processing → completed/failed/dlq';
COMMENT ON COLUMN command_queue.available_at IS 'When command becomes available for processing (retry backoff)';
COMMENT ON COLUMN command_queue.operation_id IS 'Groups related commands (e.g., bulk operation)';
COMMENT ON COLUMN command_queue.correlation_id IS 'Traces command across system';

-- 7.2) WORK_STATE - Pessimistic locking for single-writer semantics
CREATE TABLE IF NOT EXISTS work_state (
    entity VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    state VARCHAR(20) NOT NULL DEFAULT 'idle' CHECK (state IN ('idle', 'updating')),
    locked_at TIMESTAMPTZ,
    locked_by VARCHAR(255),
    PRIMARY KEY (entity, entity_id, tenant_id)
);

CREATE INDEX idx_work_state_locked_at 
    ON work_state(locked_at) 
    WHERE state = 'updating';

CREATE INDEX idx_work_state_tenant_id 
    ON work_state(tenant_id);

COMMENT ON TABLE work_state IS 'Entity-level locking with TTL-based expiry (5min default)';
COMMENT ON COLUMN work_state.state IS 'idle ↔ updating (single writer semantics)';
COMMENT ON COLUMN work_state.locked_at IS 'Lock acquisition timestamp for TTL expiry';
COMMENT ON COLUMN work_state.locked_by IS 'Worker ID that acquired lock';

-- 7.3) OUTBOX_FINAL - Transactional outbox for Kafka publishing
CREATE TABLE IF NOT EXISTS outbox_final (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    partition_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ,
    correlation_id UUID
);

CREATE INDEX idx_outbox_final_sent_at 
    ON outbox_final(sent_at) 
    WHERE sent_at IS NULL;

CREATE INDEX idx_outbox_final_entity 
    ON outbox_final(entity, entity_id);

CREATE INDEX idx_outbox_final_tenant_id 
    ON outbox_final(tenant_id);

CREATE INDEX idx_outbox_final_correlation_id 
    ON outbox_final(correlation_id) 
    WHERE correlation_id IS NOT NULL;

COMMENT ON TABLE outbox_final IS 'Transactional outbox pattern for at-least-once Kafka delivery';
COMMENT ON COLUMN outbox_final.partition_key IS 'Kafka partition key: {entity}#{entityId} for ordering';
COMMENT ON COLUMN outbox_final.sent_at IS 'NULL = pending, NOT NULL = published to Kafka';

-- =====================================================
-- SECTION 11: GRAFANA TENANT BINDINGS
-- =====================================================

-- Mapping between tenants and Grafana organizations with service account tokens
CREATE TABLE grafana_tenant_bindings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    grafana_org_id BIGINT NOT NULL,
    service_account_id BIGINT NOT NULL,
    service_account_name VARCHAR(200) NOT NULL,
    service_account_token VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_grafana_tenant_bindings_tenant_id ON grafana_tenant_bindings(tenant_id);
CREATE INDEX idx_grafana_tenant_bindings_grafana_org_id ON grafana_tenant_bindings(grafana_org_id);

COMMENT ON TABLE grafana_tenant_bindings IS 'Mapping between tenants and Grafana organizations with service account tokens for monitoring';
COMMENT ON COLUMN grafana_tenant_bindings.tenant_id IS 'Tenant identifier (e.g., core-platform, test-tenant)';
COMMENT ON COLUMN grafana_tenant_bindings.grafana_org_id IS 'Grafana organization ID';
COMMENT ON COLUMN grafana_tenant_bindings.service_account_id IS 'Grafana service account ID';
COMMENT ON COLUMN grafana_tenant_bindings.service_account_name IS 'Grafana service account name';
COMMENT ON COLUMN grafana_tenant_bindings.service_account_token IS 'Grafana service account token (glsa_...)';
COMMENT ON COLUMN grafana_tenant_bindings.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN grafana_tenant_bindings.updated_at IS 'Record last update timestamp';

-- =====================================================
-- VERIFICATION
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '✅ V1 Core Platform DB initialized successfully';
    RAISE NOTICE '📊 Core tables: tenants, users_directory, roles, groups, user_roles, user_groups';
    RAISE NOTICE '📊 Metamodel tables: edit_locks, user_profile';
    RAISE NOTICE '📊 Workflow tables: entity_state, state_transition, entity_state_log';
    RAISE NOTICE '📊 Document tables: document, document_index';
    RAISE NOTICE '📊 Analytics tables: presence_activity';
    RAISE NOTICE '📊 Streaming tables: command_queue, work_state, outbox_final';
    RAISE NOTICE '📊 Monitoring tables: grafana_tenant_bindings';
    RAISE NOTICE '🔐 RLS policies enabled for tenant isolation';
    RAISE NOTICE '⚙️ Triggers: version increment, updated_at auto-update';
END $$;
