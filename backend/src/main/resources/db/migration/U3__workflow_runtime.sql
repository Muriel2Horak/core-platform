-- =====================================================
-- U3: Rollback for V3__workflow_runtime.sql
-- =====================================================

BEGIN;

DROP VIEW IF EXISTS v_workflow_timeline;
DROP VIEW IF EXISTS v_active_workflows;

DROP INDEX IF EXISTS idx_state_log_instance;
ALTER TABLE entity_state_log DROP COLUMN IF EXISTS workflow_instance_id;

DROP TABLE IF EXISTS workflow_timers CASCADE;
DROP TABLE IF EXISTS workflow_events CASCADE;
DROP TABLE IF EXISTS workflow_instances CASCADE;

COMMIT;
