-- V3__kc_event_webhook_and_projection.sql
-- Keycloak webhook events and user projection enhancements

-- Create Keycloak event log table for idempotence
CREATE TABLE kc_event_log (
    event_hash VARCHAR(64) PRIMARY KEY,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create index for performance
CREATE INDEX idx_kc_event_log_created_at ON kc_event_log(created_at);

-- Add new columns to users_directory for Keycloak sync
ALTER TABLE users_directory 
ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL,
ADD COLUMN IF NOT EXISTS roles_json TEXT NULL,
ADD COLUMN IF NOT EXISTS groups_json TEXT NULL,
ADD COLUMN IF NOT EXISTS phone_number TEXT NULL,
ADD COLUMN IF NOT EXISTS department TEXT NULL,
ADD COLUMN IF NOT EXISTS title TEXT NULL;

-- Create indexes for new columns
CREATE INDEX IF NOT EXISTS idx_users_directory_active ON users_directory(tenant_id, active);
CREATE INDEX IF NOT EXISTS idx_users_directory_deleted_at ON users_directory(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_directory_phone ON users_directory(tenant_id, phone_number) WHERE phone_number IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_directory_department ON users_directory(tenant_id, department) WHERE department IS NOT NULL;

-- Update existing users to be active by default
UPDATE users_directory SET active = TRUE WHERE active IS NULL;

-- Function to cleanup old event logs (optional - for maintenance)
CREATE OR REPLACE FUNCTION cleanup_old_event_logs()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM kc_event_log 
    WHERE created_at < NOW() - INTERVAL '30 days';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Comment the tables for documentation
COMMENT ON TABLE kc_event_log IS 'Log of processed Keycloak webhook events for idempotence';
COMMENT ON COLUMN users_directory.active IS 'User active status from Keycloak (enabled flag)';
COMMENT ON COLUMN users_directory.deleted_at IS 'Timestamp when user was soft deleted';
COMMENT ON COLUMN users_directory.roles_json IS 'JSON representation of user roles from Keycloak';
COMMENT ON COLUMN users_directory.groups_json IS 'JSON representation of user groups from Keycloak';
COMMENT ON COLUMN users_directory.phone_number IS 'User phone number from Keycloak attributes';
COMMENT ON COLUMN users_directory.department IS 'User department from Keycloak attributes';
COMMENT ON COLUMN users_directory.title IS 'User job title from Keycloak attributes';