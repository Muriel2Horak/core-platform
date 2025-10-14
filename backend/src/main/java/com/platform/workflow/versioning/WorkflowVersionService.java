package com.platform.workflow.versioning;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 🔄 W9: Workflow Version Service
 * 
 * Manages workflow schema versions and migrations:
 * - Create new versions
 * - Activate/deactivate versions
 * - Migrate instances between versions
 * - Version comparison and compatibility
 * 
 * @since 2025-01-14
 */
@Service
public class WorkflowVersionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowVersionService.class);

    private final JdbcTemplate jdbcTemplate;

    public WorkflowVersionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Create new workflow version
     */
    public Long createVersion(String entityType, JsonNode schemaDefinition, String createdBy, String notes) {
        // Get next version number
        Integer nextVersion = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(version), 0) + 1 FROM workflow_versions WHERE entity_type = ?",
            Integer.class,
            entityType
        );

        // Insert new version
        Long versionId = jdbcTemplate.queryForObject(
            """
            INSERT INTO workflow_versions (entity_type, version, schema_definition, created_by, migration_notes, created_at)
            VALUES (?, ?, ?::jsonb, ?, ?, ?)
            RETURNING id
            """,
            Long.class,
            entityType,
            nextVersion,
            schemaDefinition.toString(),
            createdBy,
            notes,
            Instant.now()
        );

        log.info("Created workflow version: entity={}, version={}, id={}", entityType, nextVersion, versionId);
        
        return versionId;
    }

    /**
     * Activate version (sets as active for new instances)
     */
    public void activateVersion(Long versionId) {
        // Get entity type for this version
        String entityType = jdbcTemplate.queryForObject(
            "SELECT entity_type FROM workflow_versions WHERE id = ?",
            String.class,
            versionId
        );

        // Deactivate all other versions for this entity
        jdbcTemplate.update(
            "UPDATE workflow_versions SET is_active = false WHERE entity_type = ?",
            entityType
        );

        // Activate this version
        jdbcTemplate.update(
            "UPDATE workflow_versions SET is_active = true WHERE id = ?",
            versionId
        );

        log.info("Activated workflow version: entity={}, versionId={}", entityType, versionId);
    }

    /**
     * Get active version for entity type
     */
    public Optional<WorkflowVersion> getActiveVersion(String entityType) {
        List<WorkflowVersion> versions = jdbcTemplate.query(
            """
            SELECT id, entity_type, version, schema_definition, created_by, created_at, is_active, migration_notes
            FROM workflow_versions
            WHERE entity_type = ? AND is_active = true
            """,
            (rs, rowNum) -> new WorkflowVersion(
                rs.getLong("id"),
                rs.getString("entity_type"),
                rs.getInt("version"),
                rs.getString("schema_definition"),
                rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getBoolean("is_active"),
                rs.getString("migration_notes")
            ),
            entityType
        );

        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(0));
    }

    /**
     * Get specific version
     */
    public Optional<WorkflowVersion> getVersion(String entityType, int version) {
        List<WorkflowVersion> versions = jdbcTemplate.query(
            """
            SELECT id, entity_type, version, schema_definition, created_by, created_at, is_active, migration_notes
            FROM workflow_versions
            WHERE entity_type = ? AND version = ?
            """,
            (rs, rowNum) -> new WorkflowVersion(
                rs.getLong("id"),
                rs.getString("entity_type"),
                rs.getInt("version"),
                rs.getString("schema_definition"),
                rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getBoolean("is_active"),
                rs.getString("migration_notes")
            ),
            entityType,
            version
        );

        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(0));
    }

    /**
     * Get all versions for entity type
     */
    public List<WorkflowVersion> getAllVersions(String entityType) {
        return jdbcTemplate.query(
            """
            SELECT id, entity_type, version, schema_definition, created_by, created_at, is_active, migration_notes
            FROM workflow_versions
            WHERE entity_type = ?
            ORDER BY version DESC
            """,
            (rs, rowNum) -> new WorkflowVersion(
                rs.getLong("id"),
                rs.getString("entity_type"),
                rs.getInt("version"),
                rs.getString("schema_definition"),
                rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getBoolean("is_active"),
                rs.getString("migration_notes")
            ),
            entityType
        );
    }

    /**
     * Migrate instance to new version
     */
    public void migrateInstance(Long instanceId, Long toVersionId, MigrationStrategy strategy) {
        // Get current version
        Long currentVersionId = jdbcTemplate.queryForObject(
            "SELECT version_id FROM workflow_instance_versions WHERE workflow_instance_id = ?",
            Long.class,
            instanceId
        );

        if (currentVersionId.equals(toVersionId)) {
            log.warn("Instance {} already on version {}", instanceId, toVersionId);
            return;
        }

        // Update instance version mapping
        jdbcTemplate.update(
            """
            UPDATE workflow_instance_versions
            SET version_id = ?, migrated_from_version_id = ?, migrated_at = ?
            WHERE workflow_instance_id = ?
            """,
            toVersionId,
            currentVersionId,
            Instant.now(),
            instanceId
        );

        log.info("Migrated instance {} from version {} to {} using strategy {}", 
            instanceId, currentVersionId, toVersionId, strategy);
    }

    /**
     * Start bulk migration
     */
    public Long startMigration(Long fromVersionId, Long toVersionId, MigrationStrategy strategy, String createdBy) {
        Long migrationId = jdbcTemplate.queryForObject(
            """
            INSERT INTO workflow_version_migrations 
            (from_version_id, to_version_id, migration_strategy, created_by, started_at)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """,
            Long.class,
            fromVersionId,
            toVersionId,
            strategy.name(),
            createdBy,
            Instant.now()
        );

        log.info("Started migration {}: {} → {} using {}", 
            migrationId, fromVersionId, toVersionId, strategy);

        return migrationId;
    }

    public enum MigrationStrategy {
        IMMEDIATE,  // Migrate all instances now
        LAZY,       // Migrate on next access
        MANUAL      // Require manual intervention
    }

    public record WorkflowVersion(
        Long id,
        String entityType,
        int version,
        String schemaDefinition,
        String createdBy,
        Instant createdAt,
        boolean isActive,
        String migrationNotes
    ) {}
}
