-- V2_1__optimize_tenants_table.sql  
-- Optimized minimal tenants table

-- Remove unnecessary name column (can be fetched from Keycloak)
ALTER TABLE tenants DROP COLUMN IF EXISTS name;
ALTER TABLE tenants DROP COLUMN IF EXISTS created_at;

-- Add comment explaining the purpose
COMMENT ON TABLE tenants IS 'Minimal tenant registry for UUID foreign keys and webhook validation. Display names fetched from Keycloak realms.';
COMMENT ON COLUMN tenants.id IS 'UUID foreign key for efficient Hibernate filtering';
COMMENT ON COLUMN tenants.key IS 'Keycloak realm name - source of truth for tenant data';