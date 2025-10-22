-- V6: Add Grafana org binding for admin tenant
-- This ensures the admin tenant (used by test_admin) has Grafana Org ID 2 mapping
-- Org 2 is created by Grafana entrypoint script before provisioning runs

INSERT INTO grafana_tenant_bindings (
    tenant_id,
    grafana_org_id,
    service_account_id,
    service_account_name,
    service_account_token,
    created_at,
    updated_at
) VALUES (
    'admin',                    -- tenant_id: matches Keycloak admin realm
    2,                          -- grafana_org_id: Org 2 created by entrypoint.sh
    0,                          -- service_account_id: 0 = not configured yet (column is NOT NULL)
    'admin-tenant-sa',          -- service_account_name: placeholder
    NULL,                       -- service_account_token: not used yet (nullable as of V5)
    NOW(),
    NOW()
)
ON CONFLICT (tenant_id) DO NOTHING;  -- Skip if already exists (e.g. manual insert)
