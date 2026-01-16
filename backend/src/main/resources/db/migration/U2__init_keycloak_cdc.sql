-- =====================================================
-- U2: Rollback for V2__init_keycloak_cdc.sql
-- =====================================================

BEGIN;

DROP FUNCTION IF EXISTS cleanup_processed_change_events(INTEGER);
DROP TABLE IF EXISTS change_events CASCADE;

COMMIT;
