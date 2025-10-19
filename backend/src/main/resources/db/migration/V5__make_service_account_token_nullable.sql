-- Make service_account_token nullable to support idempotent token creation
-- When a token already exists in Grafana, we save a binding with empty token
-- since Grafana API doesn't support retrieving existing tokens

-- Only alter if table exists (handles case where V1 was cleaned and re-run)
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'grafana_tenant_bindings') THEN
        ALTER TABLE grafana_tenant_bindings 
        ALTER COLUMN service_account_token DROP NOT NULL;
        
        COMMENT ON COLUMN grafana_tenant_bindings.service_account_token IS 'Grafana service account token (glsa_...). May be empty if token already existed in Grafana during provisioning (Grafana API limitation - cannot retrieve existing tokens).';
    END IF;
END $$;

