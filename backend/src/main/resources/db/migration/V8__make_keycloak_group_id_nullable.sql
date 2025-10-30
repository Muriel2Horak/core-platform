-- V8: Make keycloak_group_id nullable to support API-created groups
-- Groups can be created via API (keycloak_group_id = null) or synced from Keycloak

ALTER TABLE groups 
    ALTER COLUMN keycloak_group_id DROP NOT NULL;

-- Update unique constraint to allow null keycloak_group_id
ALTER TABLE groups 
    DROP CONSTRAINT IF EXISTS uk_group_keycloak_tenant;

-- Add partial unique index (only for non-null keycloak_group_id)
CREATE UNIQUE INDEX uk_group_keycloak_tenant_partial 
    ON groups (keycloak_group_id, tenant_key) 
    WHERE keycloak_group_id IS NOT NULL;
