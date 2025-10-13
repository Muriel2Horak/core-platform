-- =====================================================
-- KEYCLOAK CDC TRIGGERS
-- ⚠️ IMPORTANT: This must be applied to KEYCLOAK database!
-- =====================================================

-- =====================================================
-- 1) TRIGGER FUNCTION - Insert do change_events
-- =====================================================

CREATE OR REPLACE FUNCTION keycloak_cdc_notify()
RETURNS TRIGGER AS $$
DECLARE
    event_type TEXT;
    realm_val TEXT;
BEGIN
    -- Determine event type
    IF TG_OP = 'INSERT' THEN
        event_type := TG_TABLE_NAME || '_CREATED';
        realm_val := NEW.realm_id;
    ELSIF TG_OP = 'UPDATE' THEN
        event_type := TG_TABLE_NAME || '_UPDATED';
        realm_val := NEW.realm_id;
    ELSIF TG_OP = 'DELETE' THEN
        event_type := TG_TABLE_NAME || '_DELETED';
        realm_val := OLD.realm_id;
    END IF;

    -- Insert change event
    INSERT INTO change_events (event_type, entity_id, realm_id)
    VALUES (
        event_type,
        COALESCE(NEW.id, OLD.id),
        realm_val
    );

    RETURN NULL; -- AFTER trigger
EXCEPTION
    WHEN OTHERS THEN
        -- Don't fail the original operation if CDC fails
        RAISE WARNING 'CDC trigger failed: %', SQLERRM;
        RETURN NULL;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION keycloak_cdc_notify IS 'CDC trigger function - logs changes to change_events table';

-- =====================================================
-- 2) USER_ENTITY TRIGGERS
-- =====================================================

DROP TRIGGER IF EXISTS user_entity_cdc_trigger ON user_entity;

CREATE TRIGGER user_entity_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON user_entity
FOR EACH ROW
EXECUTE FUNCTION keycloak_cdc_notify();

COMMENT ON TRIGGER user_entity_cdc_trigger ON user_entity IS 'CDC trigger for user changes';

-- =====================================================
-- 3) REALM TRIGGERS (optional - pro realm changes)
-- =====================================================

DROP TRIGGER IF EXISTS realm_cdc_trigger ON realm;

CREATE TRIGGER realm_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON realm
FOR EACH ROW
EXECUTE FUNCTION keycloak_cdc_notify();

COMMENT ON TRIGGER realm_cdc_trigger ON realm IS 'CDC trigger for realm changes';

-- =====================================================
-- VERIFICATION
-- =====================================================

DO $$
DECLARE
    user_trigger_count INTEGER;
    realm_trigger_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO user_trigger_count
    FROM pg_trigger
    WHERE tgname = 'user_entity_cdc_trigger';

    SELECT COUNT(*) INTO realm_trigger_count
    FROM pg_trigger
    WHERE tgname = 'realm_cdc_trigger';

    IF user_trigger_count > 0 AND realm_trigger_count > 0 THEN
        RAISE NOTICE '✅ Keycloak CDC triggers installed successfully';
        RAISE NOTICE '📊 Triggers: user_entity_cdc_trigger, realm_cdc_trigger';
    ELSE
        RAISE WARNING '⚠️ Some triggers may not have been created';
    END IF;
END $$;
