-- =====================================================
-- U8: Rollback for V8__make_keycloak_group_id_nullable.sql
-- =====================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'groups') THEN
        DROP INDEX IF EXISTS uq_groups_keycloak_id_tenant_partial;

        IF EXISTS (SELECT 1 FROM groups WHERE keycloak_group_id IS NULL) THEN
            RAISE EXCEPTION 'Cannot rollback: groups.keycloak_group_id contains NULL values';
        END IF;

        ALTER TABLE groups
            ALTER COLUMN keycloak_group_id SET NOT NULL;

        ALTER TABLE groups
            ADD CONSTRAINT uq_groups_keycloak_id_tenant UNIQUE (keycloak_group_id, tenant_id);
    END IF;
END $$;
