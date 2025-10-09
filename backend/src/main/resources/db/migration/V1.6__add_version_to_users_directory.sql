-- Add version column to users_directory for optimistic locking
ALTER TABLE users_directory 
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Create index on version for faster lookups
CREATE INDEX idx_users_directory_version ON users_directory(version);

-- Create trigger function to auto-increment version on UPDATE
CREATE OR REPLACE FUNCTION increment_user_version()
RETURNS TRIGGER AS $$
BEGIN
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to auto-increment version before UPDATE
DROP TRIGGER IF EXISTS trigger_increment_user_version ON users_directory;
CREATE TRIGGER trigger_increment_user_version
    BEFORE UPDATE ON users_directory
    FOR EACH ROW
    EXECUTE FUNCTION increment_user_version();

-- Log migration
DO $$
BEGIN
    RAISE NOTICE '✅ Added version column with auto-increment trigger to users_directory';
END $$;
