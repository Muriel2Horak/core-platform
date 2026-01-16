-- =====================================================
-- U4: Rollback for V4__workflow_versioning.sql
-- =====================================================

BEGIN;

DROP TABLE IF EXISTS workflow_version_migrations CASCADE;
DROP TABLE IF EXISTS workflow_instance_versions CASCADE;
DROP TABLE IF EXISTS workflow_versions CASCADE;

COMMIT;
