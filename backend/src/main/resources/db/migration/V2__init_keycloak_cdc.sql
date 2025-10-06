-- =====================================================
-- V2: KEYCLOAK CDC (Change Data Capture) INITIALIZATION
-- Jednoduchá CDC tabulka BEZ payload logiky na úrovni DB
-- Backend si načte data z Keycloak API podle event_type a entity_id
-- =====================================================

-- =====================================================
-- 1) CHANGE EVENTS TABLE - Minimalistická CDC queue
-- =====================================================

CREATE TABLE IF NOT EXISTS change_events (
    id BIGSERIAL PRIMARY KEY,
    event_type TEXT NOT NULL,           -- USER_CREATED, USER_UPDATED, USER_DELETED, ROLE_CREATED, etc.
    entity_id TEXT NOT NULL,            -- Keycloak entity ID (user_id, role_id, group_id)
    realm_id TEXT NOT NULL,             -- Keycloak realm_id
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMPTZ NULL
);

-- Indexy pro efektivní polling a zpracování
CREATE INDEX idx_change_events_processed ON change_events(processed) WHERE processed = FALSE;
CREATE INDEX idx_change_events_created_at ON change_events(created_at);
CREATE INDEX idx_change_events_entity_id ON change_events(entity_id);
CREATE INDEX idx_change_events_realm_id ON change_events(realm_id);

COMMENT ON TABLE change_events IS 'Minimalistická CDC queue - backend si načte data z Keycloak API';
COMMENT ON COLUMN change_events.event_type IS 'Type of event (USER_CREATED, USER_UPDATED, etc.)';
COMMENT ON COLUMN change_events.entity_id IS 'Keycloak entity ID (user, role, or group)';
COMMENT ON COLUMN change_events.realm_id IS 'Keycloak realm ID';

-- =====================================================
-- 2) CLEANUP FUNCTION - Mazání starých zpracovaných eventů
-- =====================================================

CREATE OR REPLACE FUNCTION cleanup_processed_change_events(older_than_days INTEGER DEFAULT 7)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM change_events 
    WHERE processed = TRUE 
      AND processed_at < NOW() - (older_than_days || ' days')::INTERVAL;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_processed_change_events IS 'Cleanup function for old processed events';

-- =====================================================
-- VERIFICATION
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '✅ V2 Keycloak CDC initialized successfully';
    RAISE NOTICE '📊 Tables created: change_events';
    RAISE NOTICE '🔧 No triggers installed yet - will be added after Keycloak tables exist';
END $$;
