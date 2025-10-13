-- V3__grafana_tenant_bindings.sql
-- 🔗 GRAFANA TENANT BINDINGS TABLE
-- Stores mapping between tenants and Grafana organizations with service account tokens

CREATE TABLE grafana_tenant_bindings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    grafana_org_id BIGINT NOT NULL,
    service_account_id BIGINT NOT NULL,
    service_account_name VARCHAR(200) NOT NULL,
    service_account_token VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_grafana_tenant_bindings_tenant_id ON grafana_tenant_bindings(tenant_id);
CREATE INDEX idx_grafana_tenant_bindings_grafana_org_id ON grafana_tenant_bindings(grafana_org_id);

-- Comments
COMMENT ON TABLE grafana_tenant_bindings IS 'Mapping between tenants and Grafana organizations with service account tokens for monitoring';
COMMENT ON COLUMN grafana_tenant_bindings.tenant_id IS 'Tenant identifier (e.g., core-platform, test-tenant)';
COMMENT ON COLUMN grafana_tenant_bindings.grafana_org_id IS 'Grafana organization ID';
COMMENT ON COLUMN grafana_tenant_bindings.service_account_id IS 'Grafana service account ID';
COMMENT ON COLUMN grafana_tenant_bindings.service_account_name IS 'Grafana service account name';
COMMENT ON COLUMN grafana_tenant_bindings.service_account_token IS 'Grafana service account token (glsa_...)';
COMMENT ON COLUMN grafana_tenant_bindings.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN grafana_tenant_bindings.updated_at IS 'Record last update timestamp';
