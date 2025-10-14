-- W9: Workflow Versioning Schema
-- Manages workflow definition versions and migrations

CREATE TABLE IF NOT EXISTS workflow_versions (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    version INTEGER NOT NULL,
    schema_definition JSONB NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT false,
    migration_notes TEXT,
    
    CONSTRAINT workflow_versions_unique UNIQUE (entity_type, version)
);

CREATE INDEX idx_workflow_versions_entity ON workflow_versions(entity_type);
CREATE INDEX idx_workflow_versions_active ON workflow_versions(entity_type, is_active) WHERE is_active = true;

-- Track instance-to-version mapping
CREATE TABLE IF NOT EXISTS workflow_instance_versions (
    workflow_instance_id BIGINT PRIMARY KEY REFERENCES workflow_instances(id) ON DELETE CASCADE,
    version_id BIGINT NOT NULL REFERENCES workflow_versions(id),
    migrated_from_version_id BIGINT REFERENCES workflow_versions(id),
    migrated_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_instance_versions_version ON workflow_instance_versions(version_id);

-- Version migration audit trail
CREATE TABLE IF NOT EXISTS workflow_version_migrations (
    id BIGSERIAL PRIMARY KEY,
    from_version_id BIGINT NOT NULL REFERENCES workflow_versions(id),
    to_version_id BIGINT NOT NULL REFERENCES workflow_versions(id),
    instance_count INTEGER NOT NULL DEFAULT 0,
    migration_strategy VARCHAR(50) NOT NULL, -- 'IMMEDIATE', 'LAZY', 'MANUAL'
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', -- 'IN_PROGRESS', 'COMPLETED', 'FAILED'
    error_message TEXT,
    
    created_by VARCHAR(255) NOT NULL
);

CREATE INDEX idx_version_migrations_status ON workflow_version_migrations(status);
CREATE INDEX idx_version_migrations_dates ON workflow_version_migrations(started_at, completed_at);

COMMENT ON TABLE workflow_versions IS 'W9: Workflow schema versions with migration support';
COMMENT ON TABLE workflow_instance_versions IS 'W9: Maps instances to their schema version';
COMMENT ON TABLE workflow_version_migrations IS 'W9: Tracks version migration processes';
