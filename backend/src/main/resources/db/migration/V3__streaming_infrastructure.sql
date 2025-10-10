-- =====================================================
-- V3: STREAMING INFRASTRUCTURE - DB QUEUES & OUTBOX
-- Command queue, work state tracking, and outbox pattern
-- =====================================================

-- 3.1) COMMAND QUEUE - Inbound commands with priority support
CREATE TABLE IF NOT EXISTS command_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    operation VARCHAR(20) NOT NULL, -- CREATE, UPDATE, DELETE, BULK_UPDATE, etc.
    payload JSONB NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'normal', -- critical, high, normal, bulk
    status VARCHAR(20) NOT NULL DEFAULT 'pending', -- pending, processing, completed, failed, dlq
    available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    operation_id UUID, -- Group related operations (e.g., bulk import)
    correlation_id UUID NOT NULL DEFAULT gen_random_uuid(), -- End-to-end tracing
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for command queue
CREATE INDEX idx_command_queue_status_priority_available 
    ON command_queue(status, priority DESC, available_at) 
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

COMMENT ON TABLE command_queue IS 'Command queue for async entity operations with priority support';
COMMENT ON COLUMN command_queue.operation_id IS 'Groups related operations (e.g., bulk import batch)';
COMMENT ON COLUMN command_queue.correlation_id IS 'End-to-end tracing across queue -> worker -> outbox -> kafka';
COMMENT ON COLUMN command_queue.available_at IS 'When command becomes available for processing (for delayed execution)';

-- 3.2) WORK STATE - Tracks entity-level locking and processing state
CREATE TABLE IF NOT EXISTS work_state (
    entity VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'idle', -- idle, updating, error
    locked_by VARCHAR(100), -- Worker/process identifier
    started_at TIMESTAMPTZ,
    ttl TIMESTAMPTZ, -- Lock expiry time
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    PRIMARY KEY (entity, entity_id)
);

CREATE INDEX idx_work_state_status ON work_state(status);
CREATE INDEX idx_work_state_ttl ON work_state(ttl) WHERE ttl IS NOT NULL;

COMMENT ON TABLE work_state IS 'Entity-level work state tracking for single-writer semantics';
COMMENT ON COLUMN work_state.locked_by IS 'Identifier of worker/process holding the lock';
COMMENT ON COLUMN work_state.ttl IS 'Lock expiry timestamp for automatic cleanup of stale locks';

-- 3.3) OUTBOX FINAL - Transactional outbox for event publishing
CREATE TABLE IF NOT EXISTS outbox_final (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    operation VARCHAR(20) NOT NULL, -- CREATED, UPDATED, DELETED
    diff_json JSONB, -- Diff payload (changed fields only)
    snapshot_json JSONB, -- Full snapshot (optional, based on config)
    headers_json JSONB, -- Kafka headers (correlation_id, timestamp, etc.)
    correlation_id UUID NOT NULL,
    sent_at TIMESTAMPTZ, -- NULL until successfully published to Kafka
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for outbox
CREATE INDEX idx_outbox_final_sent_at 
    ON outbox_final(sent_at) 
    WHERE sent_at IS NULL;

CREATE INDEX idx_outbox_final_entity_entity_id 
    ON outbox_final(entity, entity_id);

CREATE INDEX idx_outbox_final_correlation_id 
    ON outbox_final(correlation_id);

CREATE INDEX idx_outbox_final_created_at 
    ON outbox_final(created_at);

COMMENT ON TABLE outbox_final IS 'Transactional outbox for publishing events to Kafka';
COMMENT ON COLUMN outbox_final.diff_json IS 'Changed fields only (efficient payload mode)';
COMMENT ON COLUMN outbox_final.snapshot_json IS 'Full entity snapshot (optional, config-driven)';
COMMENT ON COLUMN outbox_final.sent_at IS 'Timestamp when event was successfully published to Kafka (NULL = pending)';

-- 3.4) Helper function: Update timestamp trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to command_queue
DROP TRIGGER IF EXISTS trigger_command_queue_updated_at ON command_queue;
CREATE TRIGGER trigger_command_queue_updated_at
    BEFORE UPDATE ON command_queue
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to work_state
DROP TRIGGER IF EXISTS trigger_work_state_updated_at ON work_state;
CREATE TRIGGER trigger_work_state_updated_at
    BEFORE UPDATE ON work_state
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Log migration
DO $$
BEGIN
    RAISE NOTICE '✅ Streaming infrastructure tables created: command_queue, work_state, outbox_final';
END $$;
