-- V8: Make keycloak_group_id nullable to support API-created groups
-- Groups can be created via API (keycloak_group_id = null) or synced from Keycloak

ALTER TABLE groups 
    ALTER COLUMN keycloak_group_id DROP NOT NULL;

-- Drop existing unique constraint (uq_groups_keycloak_id_tenant)
ALTER TABLE groups 
    DROP CONSTRAINT IF EXISTS uq_groups_keycloak_id_tenant;

-- Add partial unique index (only for non-null keycloak_group_id)
-- This allows multiple NULL keycloak_group_id values (API-created groups)
-- but enforces uniqueness for non-NULL values (Keycloak-synced groups)
CREATE UNIQUE INDEX uq_groups_keycloak_id_tenant_partial 
    ON groups (keycloak_group_id, tenant_id) 
    WHERE keycloak_group_id IS NOT NULL;
