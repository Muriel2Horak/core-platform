-- =====================================================
-- U5: Rollback for V5__make_service_account_token_nullable.sql
-- =====================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'grafana_tenant_bindings') THEN
        ALTER TABLE grafana_tenant_bindings
        ALTER COLUMN service_account_token SET NOT NULL;
    END IF;
END $$;
