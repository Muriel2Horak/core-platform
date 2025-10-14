-- =====================================================
-- W5: WORKFLOW RUNTIME (DB + API + Events)
-- Migration: Workflow Instances, Events, Timers
-- Author: GitHub Copilot
-- Date: 2025-10-14
-- =====================================================

-- ===========================================
-- 1. WORKFLOW INSTANCES
-- ===========================================
-- Tracks runtime workflow instance execution context
CREATE TABLE IF NOT EXISTS workflow_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    workflow_version_id UUID,  -- Reference to workflow_versions if applicable
    current_state_code TEXT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'RUNNING', -- RUNNING, COMPLETED, FAILED, CANCELLED
    error_message TEXT,
    context JSONB, -- Runtime context/variables
    created_by TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_workflow_instance_entity UNIQUE (entity_type, entity_id, tenant_id)
);

CREATE INDEX idx_workflow_instances_tenant ON workflow_instances(tenant_id);
CREATE INDEX idx_workflow_instances_entity ON workflow_instances(entity_type, entity_id);
CREATE INDEX idx_workflow_instances_status ON workflow_instances(status);
CREATE INDEX idx_workflow_instances_state ON workflow_instances(current_state_code);
CREATE INDEX idx_workflow_instances_started ON workflow_instances(started_at);
CREATE INDEX idx_workflow_instances_completed ON workflow_instances(completed_at) WHERE completed_at IS NOT NULL;
CREATE INDEX idx_workflow_instances_context ON workflow_instances USING gin(context);

COMMENT ON TABLE workflow_instances IS 'W5: Runtime workflow instance execution tracking';
COMMENT ON COLUMN workflow_instances.context IS 'Runtime variables and execution context in JSON format';
COMMENT ON COLUMN workflow_instances.status IS 'Overall workflow instance status: RUNNING, COMPLETED, FAILED, CANCELLED';

-- ===========================================
-- 2. WORKFLOW EVENTS
-- ===========================================
-- Immutable event log for workflow state changes
CREATE TABLE IF NOT EXISTS workflow_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL DEFAULT gen_random_uuid(), -- Unique event ID for deduplication
    tenant_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    workflow_instance_id UUID REFERENCES workflow_instances(id) ON DELETE CASCADE,
    event_type TEXT NOT NULL, -- ENTER_STATE, EXIT_STATE, ACTION_APPLIED, TIMER_FIRED, ERROR
    from_state_code TEXT,
    to_state_code TEXT,
    transition_code TEXT,
    actor TEXT NOT NULL, -- User or system that triggered the event
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    duration_ms BIGINT, -- Duration for state/action execution
    metadata JSONB, -- Additional event data
    error_details TEXT, -- Error message if applicable
    
    CONSTRAINT uq_workflow_event_id UNIQUE (event_id)
);

CREATE INDEX idx_workflow_events_tenant ON workflow_events(tenant_id);
CREATE INDEX idx_workflow_events_entity ON workflow_events(entity_type, entity_id);
CREATE INDEX idx_workflow_events_instance ON workflow_events(workflow_instance_id);
CREATE INDEX idx_workflow_events_type ON workflow_events(event_type);
CREATE INDEX idx_workflow_events_timestamp ON workflow_events(timestamp DESC);
CREATE INDEX idx_workflow_events_composite ON workflow_events(entity_type, entity_id, tenant_id, timestamp DESC);
CREATE INDEX idx_workflow_events_metadata ON workflow_events USING gin(metadata);

COMMENT ON TABLE workflow_events IS 'W5: Immutable workflow event log for state changes and actions';
COMMENT ON COLUMN workflow_events.event_id IS 'Unique event ID for idempotency and deduplication';
COMMENT ON COLUMN workflow_events.duration_ms IS 'Duration of state/action execution in milliseconds';

-- Retention policy: Partition by month (optional, for future optimization)
-- CREATE TABLE workflow_events_y2025m10 PARTITION OF workflow_events
--     FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');

-- ===========================================
-- 3. WORKFLOW TIMERS
-- ===========================================
-- Scheduled timers for delayed actions and SLA tracking
CREATE TABLE IF NOT EXISTS workflow_timers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    workflow_instance_id UUID REFERENCES workflow_instances(id) ON DELETE CASCADE,
    timer_type TEXT NOT NULL, -- DELAY, SLA_WARNING, SLA_BREACH, ESCALATION
    state_code TEXT NOT NULL, -- State this timer is associated with
    scheduled_at TIMESTAMPTZ NOT NULL, -- When to fire
    fired_at TIMESTAMPTZ, -- Actual fire time (NULL if not fired yet)
    status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, FIRED, CANCELLED
    action TEXT, -- Action to take when fired (e.g., 'escalate', 'notify')
    metadata JSONB, -- Additional timer configuration
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_timer_status CHECK (status IN ('PENDING', 'FIRED', 'CANCELLED'))
);

CREATE INDEX idx_workflow_timers_tenant ON workflow_timers(tenant_id);
CREATE INDEX idx_workflow_timers_entity ON workflow_timers(entity_type, entity_id);
CREATE INDEX idx_workflow_timers_instance ON workflow_timers(workflow_instance_id);
CREATE INDEX idx_workflow_timers_scheduled ON workflow_timers(scheduled_at) WHERE status = 'PENDING';
CREATE INDEX idx_workflow_timers_pending ON workflow_timers(status, scheduled_at) WHERE status = 'PENDING';
CREATE INDEX idx_workflow_timers_state ON workflow_timers(state_code, status);
CREATE INDEX idx_workflow_timers_type ON workflow_timers(timer_type);

COMMENT ON TABLE workflow_timers IS 'W5: Scheduled timers for workflow delays and SLA tracking';
COMMENT ON COLUMN workflow_timers.timer_type IS 'Type of timer: DELAY, SLA_WARNING, SLA_BREACH, ESCALATION';
COMMENT ON COLUMN workflow_timers.action IS 'Action to execute when timer fires';

-- ===========================================
-- 4. ENHANCED STATE LOG
-- ===========================================
-- Add workflow_instance_id to existing entity_state_log for better traceability
ALTER TABLE entity_state_log 
ADD COLUMN IF NOT EXISTS workflow_instance_id UUID REFERENCES workflow_instances(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_state_log_instance ON entity_state_log(workflow_instance_id) 
    WHERE workflow_instance_id IS NOT NULL;

-- ===========================================
-- 5. VIEWS FOR ANALYTICS
-- ===========================================

-- Active workflows with duration
CREATE OR REPLACE VIEW v_active_workflows AS
SELECT 
    wi.id,
    wi.tenant_id,
    wi.entity_type,
    wi.entity_id,
    wi.current_state_code,
    wi.status,
    wi.started_at,
    EXTRACT(EPOCH FROM (NOW() - wi.started_at))::BIGINT * 1000 AS duration_ms,
    es.since AS current_state_since,
    EXTRACT(EPOCH FROM (NOW() - es.since))::BIGINT * 1000 AS state_duration_ms,
    es.sla_minutes
FROM workflow_instances wi
LEFT JOIN entity_state es ON 
    wi.entity_type = es.entity_type AND 
    wi.entity_id = es.entity_id AND 
    wi.tenant_id = es.tenant_id
WHERE wi.status = 'RUNNING';

COMMENT ON VIEW v_active_workflows IS 'W5: Active workflows with current state and duration metrics';

-- Workflow timeline aggregates
CREATE OR REPLACE VIEW v_workflow_timeline AS
SELECT 
    we.entity_type,
    we.entity_id,
    we.tenant_id,
    we.workflow_instance_id,
    we.event_type,
    we.from_state_code,
    we.to_state_code,
    we.transition_code,
    we.timestamp,
    we.duration_ms,
    we.actor,
    LAG(we.timestamp) OVER (PARTITION BY we.entity_type, we.entity_id, we.tenant_id ORDER BY we.timestamp) AS prev_timestamp,
    EXTRACT(EPOCH FROM (we.timestamp - LAG(we.timestamp) OVER (PARTITION BY we.entity_type, we.entity_id, we.tenant_id ORDER BY we.timestamp)))::BIGINT * 1000 AS time_since_prev_ms
FROM workflow_events we
ORDER BY we.entity_type, we.entity_id, we.timestamp DESC;

COMMENT ON VIEW v_workflow_timeline IS 'W5: Workflow event timeline with duration calculations';

-- ===========================================
-- 6. RETENTION POLICY (optional)
-- ===========================================
-- Future: Implement retention policy for workflow_events older than X months
-- CREATE OR REPLACE FUNCTION cleanup_old_workflow_events()
-- RETURNS void AS $$
-- BEGIN
--     DELETE FROM workflow_events WHERE timestamp < NOW() - INTERVAL '6 months';
-- END;
-- $$ LANGUAGE plpgsql;

-- ===========================================
-- 7. GRANT PERMISSIONS
-- ===========================================
-- Grant access to application user (adjust username as needed)
-- GRANT SELECT, INSERT, UPDATE ON workflow_instances TO app_user;
-- GRANT SELECT, INSERT ON workflow_events TO app_user;
-- GRANT SELECT, INSERT, UPDATE ON workflow_timers TO app_user;
