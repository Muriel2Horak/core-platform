-- =====================================================
-- REPEATABLE: CACHE INVALIDATION NOTIFICATIONS
-- Send NOTIFY events for cache invalidation on entity changes
-- =====================================================

-- Function to send cache invalidation NOTIFY
CREATE OR REPLACE FUNCTION notify_cache_invalidation()
RETURNS TRIGGER AS $$
DECLARE
    v_payload JSONB;
    v_entity_type TEXT;
BEGIN
    -- Determine entity type from table name
    v_entity_type := TG_TABLE_NAME;
    
    -- Build NOTIFY payload
    v_payload := jsonb_build_object(
        'entityType', v_entity_type,
        'entityId', COALESCE(NEW.id::TEXT, OLD.id::TEXT),
        'tenantId', COALESCE(
            NEW.tenant_id, 
            NEW.tenant_key, 
            OLD.tenant_id, 
            OLD.tenant_key
        ),
        'operation', TG_OP,
        'timestamp', NOW()
    );
    
    -- Send NOTIFY to change_events channel
    PERFORM pg_notify('change_events', v_payload::TEXT);
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Attach triggers to all cacheable entities
DROP TRIGGER IF EXISTS notify_users_directory_change ON users_directory;
CREATE TRIGGER notify_users_directory_change
    AFTER INSERT OR UPDATE OR DELETE ON users_directory
    FOR EACH ROW
    EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS notify_roles_change ON roles;
CREATE TRIGGER notify_roles_change
    AFTER INSERT OR UPDATE OR DELETE ON roles
    FOR EACH ROW
    EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS notify_groups_change ON groups;
CREATE TRIGGER notify_groups_change
    AFTER INSERT OR UPDATE OR DELETE ON groups
    FOR EACH ROW
    EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS notify_user_profile_change ON user_profile;
CREATE TRIGGER notify_user_profile_change
    AFTER INSERT OR UPDATE OR DELETE ON user_profile
    FOR EACH ROW
    EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS notify_document_change ON document;
CREATE TRIGGER notify_document_change
    AFTER INSERT OR UPDATE OR DELETE ON document
    FOR EACH ROW
    EXECUTE FUNCTION notify_cache_invalidation();

COMMENT ON FUNCTION notify_cache_invalidation IS 'Send PostgreSQL NOTIFY for Redis cache invalidation';
