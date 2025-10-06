-- =====================================================
-- KEYCLOAK DATABASE TRIGGERS - V6 ULTRA-FAST (NO PAYLOAD)
-- Synchronizuje změny z Keycloak DB do Core DB
-- Filozofie: Triggery jsou ULTRA-RYCHLÉ - jen INSERT ID + typ
-- Backend si data dotáhne sám z Keycloak Admin API
-- =====================================================

-- Připojení do Keycloak databáze jako uživatel 'keycloak'
\c keycloak keycloak

-- 🔧 Installing Keycloak triggers - V6 ULTRA-FAST SYSTEM

-- =====================================================
-- 1️⃣ CHANGE EVENTS TABLE - MINIMÁLNÍ STRUKTURA
-- =====================================================

DROP TABLE IF EXISTS change_events CASCADE;

CREATE TABLE change_events (
    id BIGSERIAL PRIMARY KEY,
    event_type TEXT NOT NULL CHECK (event_type IN (
        'USER_CREATED', 'USER_UPDATED', 'USER_DELETED',
        'ROLE_CREATED', 'ROLE_UPDATED', 'ROLE_DELETED',
        'GROUP_CREATED', 'GROUP_UPDATED', 'GROUP_DELETED',
        'USER_ROLE_ASSIGNED', 'USER_ROLE_REMOVED',
        'USER_GROUP_ASSIGNED', 'USER_GROUP_REMOVED'
    )),
    entity_id TEXT NOT NULL,           -- ID entity (user_id, role_id, group_id)
    realm_id TEXT NOT NULL,            -- Keycloak realm
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMPTZ
    -- ❌ BEZ PAYLOAD - backend si data dotáhne sám!
);

-- Indexy pro efektivní polling
CREATE INDEX idx_change_events_unprocessed ON change_events(processed, created_at) WHERE NOT processed;
CREATE INDEX idx_change_events_realm ON change_events(realm_id, processed);
CREATE INDEX idx_change_events_entity ON change_events(entity_id, event_type);

-- =====================================================
-- 2️⃣ ULTRA-FAST TRIGGER FUNCTIONS (NO LOGIC, NO PAYLOAD)
-- =====================================================

-- 👤 USER_ENTITY změny
CREATE OR REPLACE FUNCTION fn_user_entity_change()
RETURNS TRIGGER AS $$
BEGIN
    -- ULTRA-FAST: jen INSERT, žádná logika
    INSERT INTO change_events (event_type, entity_id, realm_id)
    VALUES (
        CASE TG_OP
            WHEN 'INSERT' THEN 'USER_CREATED'
            WHEN 'UPDATE' THEN 'USER_UPDATED'
            WHEN 'DELETE' THEN 'USER_DELETED'
        END,
        COALESCE(NEW.id, OLD.id),
        COALESCE(NEW.realm_id, OLD.realm_id)
    );
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- 🔐 USER_ROLE_MAPPING změny
CREATE OR REPLACE FUNCTION fn_user_role_change()
RETURNS TRIGGER AS $$
DECLARE
    v_realm_id TEXT;
BEGIN
    -- Zjisti realm_id (minimální logika)
    SELECT realm_id INTO v_realm_id
    FROM user_entity 
    WHERE id = COALESCE(NEW.user_id, OLD.user_id)
    LIMIT 1;
    
    -- ULTRA-FAST INSERT
    INSERT INTO change_events (event_type, entity_id, realm_id)
    VALUES (
        CASE TG_OP
            WHEN 'DELETE' THEN 'USER_ROLE_REMOVED'
            ELSE 'USER_ROLE_ASSIGNED'
        END,
        COALESCE(NEW.user_id, OLD.user_id),
        COALESCE(v_realm_id, 'master')
    );
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- 👥 USER_GROUP_MEMBERSHIP změny
CREATE OR REPLACE FUNCTION fn_user_group_change()
RETURNS TRIGGER AS $$
DECLARE
    v_realm_id TEXT;
BEGIN
    -- Zjisti realm_id (minimální logika)
    SELECT realm_id INTO v_realm_id
    FROM user_entity 
    WHERE id = COALESCE(NEW.user_id, OLD.user_id)
    LIMIT 1;
    
    -- ULTRA-FAST INSERT
    INSERT INTO change_events (event_type, entity_id, realm_id)
    VALUES (
        CASE TG_OP
            WHEN 'DELETE' THEN 'USER_GROUP_REMOVED'
            ELSE 'USER_GROUP_ASSIGNED'
        END,
        COALESCE(NEW.user_id, OLD.user_id),
        COALESCE(v_realm_id, 'master')
    );
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- 🎭 KEYCLOAK_ROLE změny
CREATE OR REPLACE FUNCTION fn_role_change()
RETURNS TRIGGER AS $$
BEGIN
    -- ULTRA-FAST: jen INSERT
    INSERT INTO change_events (event_type, entity_id, realm_id)
    VALUES (
        CASE TG_OP
            WHEN 'INSERT' THEN 'ROLE_CREATED'
            WHEN 'UPDATE' THEN 'ROLE_UPDATED'
            WHEN 'DELETE' THEN 'ROLE_DELETED'
        END,
        COALESCE(NEW.id, OLD.id),
        COALESCE(NEW.realm_id, OLD.realm_id, 'master')
    );
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- 📁 KEYCLOAK_GROUP změny
CREATE OR REPLACE FUNCTION fn_group_change()
RETURNS TRIGGER AS $$
BEGIN
    -- ULTRA-FAST: jen INSERT
    INSERT INTO change_events (event_type, entity_id, realm_id)
    VALUES (
        CASE TG_OP
            WHEN 'INSERT' THEN 'GROUP_CREATED'
            WHEN 'UPDATE' THEN 'GROUP_UPDATED'
            WHEN 'DELETE' THEN 'GROUP_DELETED'
        END,
        COALESCE(NEW.id, OLD.id),
        COALESCE(NEW.realm_id, OLD.realm_id)
    );
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 3️⃣ DROP EXISTING TRIGGERS (idempotent)
-- =====================================================

DROP TRIGGER IF EXISTS trig_user_entity_insert ON user_entity;
DROP TRIGGER IF EXISTS trig_user_entity_update ON user_entity;
DROP TRIGGER IF EXISTS trig_user_entity_delete ON user_entity;

DROP TRIGGER IF EXISTS trig_user_role_insert ON user_role_mapping;
DROP TRIGGER IF EXISTS trig_user_role_delete ON user_role_mapping;

DROP TRIGGER IF EXISTS trig_user_group_insert ON user_group_membership;
DROP TRIGGER IF EXISTS trig_user_group_delete ON user_group_membership;

DROP TRIGGER IF EXISTS trig_role_insert ON keycloak_role;
DROP TRIGGER IF EXISTS trig_role_update ON keycloak_role;
DROP TRIGGER IF EXISTS trig_role_delete ON keycloak_role;

DROP TRIGGER IF EXISTS trig_group_insert ON keycloak_group;
DROP TRIGGER IF EXISTS trig_group_update ON keycloak_group;
DROP TRIGGER IF EXISTS trig_group_delete ON keycloak_group;

-- =====================================================
-- 4️⃣ CREATE TRIGGERS
-- =====================================================

-- 👤 USER_ENTITY triggers
CREATE TRIGGER trig_user_entity_insert
    AFTER INSERT ON user_entity
    FOR EACH ROW EXECUTE FUNCTION fn_user_entity_change();

CREATE TRIGGER trig_user_entity_update
    AFTER UPDATE ON user_entity
    FOR EACH ROW EXECUTE FUNCTION fn_user_entity_change();

CREATE TRIGGER trig_user_entity_delete
    AFTER DELETE ON user_entity
    FOR EACH ROW EXECUTE FUNCTION fn_user_entity_change();

-- 🔐 USER_ROLE_MAPPING triggers
CREATE TRIGGER trig_user_role_insert
    AFTER INSERT ON user_role_mapping
    FOR EACH ROW EXECUTE FUNCTION fn_user_role_change();

CREATE TRIGGER trig_user_role_delete
    AFTER DELETE ON user_role_mapping
    FOR EACH ROW EXECUTE FUNCTION fn_user_role_change();

-- 👥 USER_GROUP_MEMBERSHIP triggers
CREATE TRIGGER trig_user_group_insert
    AFTER INSERT ON user_group_membership
    FOR EACH ROW EXECUTE FUNCTION fn_user_group_change();

CREATE TRIGGER trig_user_group_delete
    AFTER DELETE ON user_group_membership
    FOR EACH ROW EXECUTE FUNCTION fn_user_group_change();

-- 🎭 KEYCLOAK_ROLE triggers
CREATE TRIGGER trig_role_insert
    AFTER INSERT ON keycloak_role
    FOR EACH ROW EXECUTE FUNCTION fn_role_change();

CREATE TRIGGER trig_role_update
    AFTER UPDATE ON keycloak_role
    FOR EACH ROW EXECUTE FUNCTION fn_role_change();

CREATE TRIGGER trig_role_delete
    AFTER DELETE ON keycloak_role
    FOR EACH ROW EXECUTE FUNCTION fn_role_change();

-- 📁 KEYCLOAK_GROUP triggers
CREATE TRIGGER trig_group_insert
    AFTER INSERT ON keycloak_group
    FOR EACH ROW EXECUTE FUNCTION fn_group_change();

CREATE TRIGGER trig_group_update
    AFTER UPDATE ON keycloak_group
    FOR EACH ROW EXECUTE FUNCTION fn_group_change();

CREATE TRIGGER trig_group_delete
    AFTER DELETE ON keycloak_group
    FOR EACH ROW EXECUTE FUNCTION fn_group_change();

-- =====================================================
-- 5️⃣ VERIFICATION
-- =====================================================

DO $$
DECLARE
    trigger_count INTEGER;
    event_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO trigger_count
    FROM information_schema.triggers
    WHERE trigger_schema = 'public'
    AND trigger_name LIKE 'trig_%';
    
    SELECT COUNT(*) INTO event_count
    FROM change_events;
    
    RAISE NOTICE '✅ Keycloak triggers installed: % triggers active', trigger_count;
    RAISE NOTICE '📊 Current events in queue: %', event_count;
    
    IF trigger_count < 11 THEN
        RAISE WARNING '⚠️ Expected 11 triggers, found only %', trigger_count;
    END IF;
    
    -- Závěrečné statistiky
    RAISE NOTICE '🎉 Keycloak trigger system V6 ULTRA-FAST installation complete!';
    RAISE NOTICE '⚡ Triggers: MINIMAL overhead - just INSERT id + type + realm';
    RAISE NOTICE '❌ NO PAYLOAD - backend fetches data from Keycloak Admin API';
    RAISE NOTICE '🔄 Backend polls change_events every 10 seconds';
END $$;
