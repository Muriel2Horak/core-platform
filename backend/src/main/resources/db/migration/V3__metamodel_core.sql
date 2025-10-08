-- =====================================================
-- V3: METAMODEL CORE - FÁZE 1
-- Přidává podporu pro metamodel-driven CRUD s ABAC
-- =====================================================

-- =====================================================
-- 1) VERSION COLUMN pro existující entity
-- =====================================================

-- Přidáme version sloupec pro optimistic locking
ALTER TABLE users_directory ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE groups ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- =====================================================
-- 2) EDIT LOCKS - Soft locking pro editaci entit
-- =====================================================

CREATE TABLE IF NOT EXISTS edit_locks (
    id BIGSERIAL PRIMARY KEY,
    tenant_id TEXT NOT NULL,
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

COMMENT ON TABLE edit_locks IS 'Soft locks for entity editing - prevents concurrent modifications';
COMMENT ON COLUMN edit_locks.lock_type IS 'Currently only soft locks supported';
COMMENT ON COLUMN edit_locks.expires_at IS 'Lock auto-expires after TTL';

-- =====================================================
-- 3) USER PROFILE - Referenční entita pro metamodel
-- =====================================================

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

COMMENT ON TABLE user_profile IS 'Extended user profile managed via metamodel CRUD';

-- =====================================================
-- 4) ROW LEVEL SECURITY (RLS) - Tenant Isolation
-- =====================================================

-- Enable RLS na user_profile
ALTER TABLE user_profile ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_user_profile ON user_profile
    USING (tenant_id = current_setting('app.tenant_id', true));

-- Enable RLS na users_directory (pokud již není)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'users_directory' AND policyname = 'tenant_isolation_users'
    ) THEN
        ALTER TABLE users_directory ENABLE ROW LEVEL SECURITY;
        CREATE POLICY tenant_isolation_users ON users_directory
            USING (tenant_key = current_setting('app.tenant_id', true));
    END IF;
END $$;

-- Enable RLS na roles
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'roles' AND policyname = 'tenant_isolation_roles'
    ) THEN
        ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
        CREATE POLICY tenant_isolation_roles ON roles
            USING (tenant_key = current_setting('app.tenant_id', true));
    END IF;
END $$;

-- Enable RLS na groups
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'groups' AND policyname = 'tenant_isolation_groups'
    ) THEN
        ALTER TABLE groups ENABLE ROW LEVEL SECURITY;
        CREATE POLICY tenant_isolation_groups ON groups
            USING (tenant_key = current_setting('app.tenant_id', true));
    END IF;
END $$;

-- =====================================================
-- 5) HELPER FUNCTIONS
-- =====================================================

-- Funkce pro automatické zvýšení version při UPDATE
CREATE OR REPLACE FUNCTION increment_version()
RETURNS TRIGGER AS $$
BEGIN
    NEW.version = OLD.version + 1;
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers pro auto-increment version
DROP TRIGGER IF EXISTS user_profile_version_trigger ON user_profile;
CREATE TRIGGER user_profile_version_trigger
    BEFORE UPDATE ON user_profile
    FOR EACH ROW
    EXECUTE FUNCTION increment_version();

DROP TRIGGER IF EXISTS users_directory_version_trigger ON users_directory;
CREATE TRIGGER users_directory_version_trigger
    BEFORE UPDATE ON users_directory
    FOR EACH ROW
    EXECUTE FUNCTION increment_version();

DROP TRIGGER IF EXISTS roles_version_trigger ON roles;
CREATE TRIGGER roles_version_trigger
    BEFORE UPDATE ON roles
    FOR EACH ROW
    EXECUTE FUNCTION increment_version();

DROP TRIGGER IF EXISTS groups_version_trigger ON groups;
CREATE TRIGGER groups_version_trigger
    BEFORE UPDATE ON groups
    FOR EACH ROW
    EXECUTE FUNCTION increment_version();

-- =====================================================
-- 6) SEED DATA pro testování
-- =====================================================

-- Přidáme testovací user profile (pro admin tenanta)
INSERT INTO user_profile (tenant_id, user_id, full_name, email, department, position)
SELECT 
    'admin' as tenant_id,
    ud.id as user_id,
    COALESCE(ud.display_name, ud.first_name || ' ' || ud.last_name) as full_name,
    ud.email,
    ud.department,
    ud.position
FROM users_directory ud
WHERE ud.tenant_key = 'admin' 
  AND NOT EXISTS (
      SELECT 1 FROM user_profile up WHERE up.user_id = ud.id
  )
LIMIT 5; -- Jen prvních 5 pro test

COMMENT ON TABLE edit_locks IS 'Metamodel Phase 1: Soft locks with auto-expiry';
COMMENT ON TABLE user_profile IS 'Metamodel Phase 1: Reference entity with ABAC policies';
