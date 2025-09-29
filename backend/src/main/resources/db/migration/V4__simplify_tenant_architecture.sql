-- V4__simplify_tenant_architecture.sql
-- Zjednodušení tenant architektury: realm = tenant key

-- 🧹 CLEAN UP: Remove redundant 'realm' column from tenants table (if exists)
-- Since realm name = tenant key, we don't need separate realm column
ALTER TABLE tenants DROP COLUMN IF EXISTS realm;

-- 🎯 SIMPLIFIED: Update tenant table comment
COMMENT ON TABLE tenants IS 'Tenant registry - simplified architecture where tenant.key = Keycloak realm name';
COMMENT ON COLUMN tenants.key IS 'Tenant identifier (equals Keycloak realm name)';
-- REMOVED: COMMENT ON COLUMN tenants.name - sloupec name neexistuje (je @Transient v entity)

-- 🧹 CLEAN UP: Remove unused indexes that might cause performance issues
DROP INDEX IF EXISTS idx_tenants_realm; -- This index doesn't exist yet, but just in case

-- ✅ KEEP: Essential indexes for performance
-- idx_tenants_key - already exists and needed
-- idx_users_directory_tenant_id - already exists and needed

-- 🆕 ADD: Sample tenant for core-platform realm (for admin users)
INSERT INTO tenants (key) VALUES 
('core-platform')
ON CONFLICT (key) DO NOTHING;

-- 📋 FINAL STATE SUMMARY:
-- tenants table: id, key (name je @Transient - načítá se z Keycloak)
-- users_directory: unchanged - still references tenant_id
-- Logic: tenant.key = Keycloak realm name (1:1 mapping)