-- =====================================================
-- V5: Workflow, States, SLA & Documents (Phase 2)
-- =====================================================

-- ====== ENTITY STATE MANAGEMENT ======

-- Current state of entities
CREATE TABLE entity_state (
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    state_code TEXT NOT NULL,
    since TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (entity_type, entity_id, tenant_id)
);

CREATE INDEX idx_entity_state_tenant ON entity_state(tenant_id, entity_type, entity_id);
CREATE INDEX idx_entity_state_code ON entity_state(state_code);

-- State transitions configuration
CREATE TABLE state_transition (
    entity_type TEXT NOT NULL,
    from_code TEXT,  -- NULL means initial state
    to_code TEXT NOT NULL,
    code TEXT PRIMARY KEY,  -- unique transition code
    guard JSONB,  -- conditions for transition
    sla_minutes INTEGER,  -- SLA in minutes
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_state_transition_lookup ON state_transition(entity_type, from_code, to_code);

-- State change audit log
CREATE TABLE entity_state_log (
    id BIGSERIAL PRIMARY KEY,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    from_code TEXT,
    to_code TEXT NOT NULL,
    changed_by TEXT NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB
);

CREATE INDEX idx_entity_state_log_entity ON entity_state_log(entity_type, entity_id, tenant_id, changed_at DESC);
CREATE INDEX idx_entity_state_log_tenant ON entity_state_log(tenant_id, changed_at DESC);


-- ====== DOCUMENT MANAGEMENT ======

-- Document metadata
CREATE TABLE document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id TEXT NOT NULL,
    entity_type TEXT,  -- optional: link to entity
    entity_id TEXT,
    filename TEXT NOT NULL,
    content_type TEXT NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key TEXT NOT NULL,  -- MinIO object key
    version_id TEXT,  -- MinIO version ID
    uploaded_by TEXT NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB
);

CREATE INDEX idx_document_tenant ON document(tenant_id, uploaded_at DESC);
CREATE INDEX idx_document_entity ON document(entity_type, entity_id);
CREATE INDEX idx_document_uploaded_by ON document(uploaded_by);

-- Document fulltext index
CREATE TABLE document_index (
    document_id UUID PRIMARY KEY REFERENCES document(id) ON DELETE CASCADE,
    tenant_id TEXT NOT NULL,
    content_tsv TSVECTOR NOT NULL
);

CREATE INDEX idx_document_index_fts ON document_index USING GIN(content_tsv);
CREATE INDEX idx_document_index_tenant ON document_index(tenant_id);


-- ====== PRESENCE TRACKING ======

-- Presence activity log (for analytics, optional)
CREATE TABLE presence_activity (
    id BIGSERIAL PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    username TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    action TEXT NOT NULL,  -- join, leave, heartbeat
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_presence_activity_entity ON presence_activity(entity_type, entity_id, timestamp DESC);
CREATE INDEX idx_presence_activity_user ON presence_activity(user_id, timestamp DESC);

-- Retention: keep only last 7 days
CREATE INDEX idx_presence_activity_cleanup ON presence_activity(timestamp);


-- ====== HELPER FUNCTIONS ======

-- Function to calculate SLA status
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
    warn_threshold := p_sla_minutes * 0.8;  -- 80% = warning
    
    IF elapsed_minutes >= p_sla_minutes THEN
        RETURN 'BREACH';
    ELSIF elapsed_minutes >= warn_threshold THEN
        RETURN 'WARN';
    ELSE
        RETURN 'OK';
    END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;


-- ====== SAMPLE DATA ======

-- Insert sample transitions for UserProfile entity
INSERT INTO state_transition (entity_type, from_code, to_code, code, guard, sla_minutes) VALUES
    ('UserProfile', NULL, 'draft', 'CREATE_DRAFT', NULL, NULL),
    ('UserProfile', 'draft', 'active', 'ACTIVATE', '{"expression": "hasRole(''CORE_ROLE_ADMIN'')"}', 60),
    ('UserProfile', 'active', 'suspended', 'SUSPEND', '{"expression": "hasRole(''CORE_ROLE_ADMIN'')"}', 30),
    ('UserProfile', 'suspended', 'active', 'REACTIVATE', '{"expression": "hasRole(''CORE_ROLE_ADMIN'')"}', 30),
    ('UserProfile', 'active', 'archived', 'ARCHIVE', '{"expression": "hasRole(''CORE_ROLE_ADMIN'')"}', NULL),
    ('UserProfile', 'suspended', 'archived', 'ARCHIVE_SUSPENDED', '{"expression": "hasRole(''CORE_ROLE_ADMIN'')"}', NULL);

COMMENT ON TABLE entity_state IS 'Current state of entities with timestamp';
COMMENT ON TABLE state_transition IS 'Allowed state transitions with guards and SLA';
COMMENT ON TABLE entity_state_log IS 'Audit log of all state changes';
COMMENT ON TABLE document IS 'Document metadata linked to MinIO storage';
COMMENT ON TABLE document_index IS 'Fulltext search index for documents';
COMMENT ON TABLE presence_activity IS 'Presence activity log for analytics';
