package com.platform.workflow.versioning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.workflow.versioning.WorkflowVersionService.MigrationStrategy;
import com.platform.workflow.versioning.WorkflowVersionService.WorkflowVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🧪 W9: WorkflowVersionService Unit Tests
 * 
 * Tests version management:
 * - Version creation and auto-increment
 * - Version activation (deactivates others)
 * - Version retrieval (active, specific, all)
 * - Instance migration
 * - Bulk migration initiation
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkflowVersionServiceTest {

    @Autowired
    private WorkflowVersionService versionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Clean up test data
        jdbcTemplate.update("DELETE FROM workflow_version_migrations");
        jdbcTemplate.update("DELETE FROM workflow_instance_versions");
        jdbcTemplate.update("DELETE FROM workflow_versions");
    }

    @Test
    void createVersion_shouldAutoIncrementVersionNumber() throws Exception {
        // Given
        JsonNode schema1 = objectMapper.readTree("{\"states\": [\"DRAFT\", \"SUBMITTED\"]}");
        JsonNode schema2 = objectMapper.readTree("{\"states\": [\"DRAFT\", \"SUBMITTED\", \"APPROVED\"]}");

        // When
        Long v1Id = versionService.createVersion("ORDER", schema1, "admin", "Initial version");
        Long v2Id = versionService.createVersion("ORDER", schema2, "admin", "Added APPROVED state");

        // Then
        Optional<WorkflowVersion> v1 = versionService.getVersion("ORDER", 1);
        Optional<WorkflowVersion> v2 = versionService.getVersion("ORDER", 2);

        assertThat(v1).isPresent();
        assertThat(v1.get().version()).isEqualTo(1);
        assertThat(v1.get().entityType()).isEqualTo("ORDER");

        assertThat(v2).isPresent();
        assertThat(v2.get().version()).isEqualTo(2);
    }

    @Test
    void activateVersion_shouldDeactivateOthers() throws Exception {
        // Given
        JsonNode schema1 = objectMapper.readTree("{\"states\": [\"DRAFT\"]}");
        JsonNode schema2 = objectMapper.readTree("{\"states\": [\"DRAFT\", \"SUBMITTED\"]}");

        Long v1Id = versionService.createVersion("ORDER", schema1, "admin", "V1");
        Long v2Id = versionService.createVersion("ORDER", schema2, "admin", "V2");

        // When
        versionService.activateVersion(v1Id);

        // Then
        Optional<WorkflowVersion> active = versionService.getActiveVersion("ORDER");
        assertThat(active).isPresent();
        assertThat(active.get().id()).isEqualTo(v1Id);
        assertThat(active.get().isActive()).isTrue();

        Optional<WorkflowVersion> v2 = versionService.getVersion("ORDER", 2);
        assertThat(v2.get().isActive()).isFalse();

        // When - activate v2
        versionService.activateVersion(v2Id);

        // Then
        active = versionService.getActiveVersion("ORDER");
        assertThat(active.get().id()).isEqualTo(v2Id);

        Optional<WorkflowVersion> v1 = versionService.getVersion("ORDER", 1);
        assertThat(v1.get().isActive()).isFalse();
    }

    @Test
    void getActiveVersion_shouldReturnEmpty_whenNoActiveVersion() {
        // When
        Optional<WorkflowVersion> active = versionService.getActiveVersion("NONEXISTENT");

        // Then
        assertThat(active).isEmpty();
    }

    @Test
    void getAllVersions_shouldReturnInDescendingOrder() throws Exception {
        // Given
        JsonNode schema = objectMapper.readTree("{}");
        versionService.createVersion("ORDER", schema, "admin", "V1");
        versionService.createVersion("ORDER", schema, "admin", "V2");
        versionService.createVersion("ORDER", schema, "admin", "V3");

        // When
        List<WorkflowVersion> versions = versionService.getAllVersions("ORDER");

        // Then
        assertThat(versions).hasSize(3);
        assertThat(versions.get(0).version()).isEqualTo(3);
        assertThat(versions.get(1).version()).isEqualTo(2);
        assertThat(versions.get(2).version()).isEqualTo(1);
    }

    @Test
    void migrateInstance_shouldUpdateVersionMapping() throws Exception {
        // Given
        JsonNode schema1 = objectMapper.readTree("{\"states\": [\"DRAFT\"]}");
        JsonNode schema2 = objectMapper.readTree("{\"states\": [\"DRAFT\", \"SUBMITTED\"]}");

        Long v1Id = versionService.createVersion("ORDER", schema1, "admin", "V1");
        Long v2Id = versionService.createVersion("ORDER", schema2, "admin", "V2");

        // Create instance on v1
        Long instanceId = jdbcTemplate.queryForObject(
                "INSERT INTO workflow_instances (entity_type, entity_id, current_state, tenant_id) " +
                "VALUES ('ORDER', 'ORD-123', 'DRAFT', 'T1') RETURNING id",
                Long.class
        );

        jdbcTemplate.update(
                "INSERT INTO workflow_instance_versions (workflow_instance_id, version_id) VALUES (?, ?)",
                instanceId, v1Id
        );

        // When
        versionService.migrateInstance(instanceId, v2Id, MigrationStrategy.IMMEDIATE);

        // Then
        Long currentVersion = jdbcTemplate.queryForObject(
                "SELECT version_id FROM workflow_instance_versions WHERE workflow_instance_id = ?",
                Long.class,
                instanceId
        );

        assertThat(currentVersion).isEqualTo(v2Id);

        Long migratedFrom = jdbcTemplate.queryForObject(
                "SELECT migrated_from_version_id FROM workflow_instance_versions WHERE workflow_instance_id = ?",
                Long.class,
                instanceId
        );

        assertThat(migratedFrom).isEqualTo(v1Id);
    }

    @Test
    void startMigration_shouldCreateMigrationRecord() throws Exception {
        // Given
        JsonNode schema1 = objectMapper.readTree("{}");
        JsonNode schema2 = objectMapper.readTree("{}");

        Long v1Id = versionService.createVersion("ORDER", schema1, "admin", "V1");
        Long v2Id = versionService.createVersion("ORDER", schema2, "admin", "V2");

        // When
        Long migrationId = versionService.startMigration(
                v1Id,
                v2Id,
                MigrationStrategy.LAZY,
                "admin"
        );

        // Then
        assertThat(migrationId).isNotNull();

        String strategy = jdbcTemplate.queryForObject(
                "SELECT migration_strategy FROM workflow_version_migrations WHERE id = ?",
                String.class,
                migrationId
        );

        assertThat(strategy).isEqualTo("LAZY");
    }

    @Test
    void versionIsolation_shouldNotAffectDifferentEntityTypes() throws Exception {
        // Given
        JsonNode schema = objectMapper.readTree("{}");

        // When
        versionService.createVersion("ORDER", schema, "admin", "Order V1");
        versionService.createVersion("ORDER", schema, "admin", "Order V2");
        versionService.createVersion("INVOICE", schema, "admin", "Invoice V1");

        // Then
        List<WorkflowVersion> orderVersions = versionService.getAllVersions("ORDER");
        List<WorkflowVersion> invoiceVersions = versionService.getAllVersions("INVOICE");

        assertThat(orderVersions).hasSize(2);
        assertThat(invoiceVersions).hasSize(1);
        assertThat(invoiceVersions.get(0).version()).isEqualTo(1); // Version counter is per entity type
    }

    @Test
    void createVersion_shouldStoreSchemaAsJson() throws Exception {
        // Given
        JsonNode complexSchema = objectMapper.readTree("""
                {
                    "states": ["DRAFT", "SUBMITTED", "APPROVED"],
                    "transitions": [
                        {"from": "DRAFT", "to": "SUBMITTED", "event": "submit"},
                        {"from": "SUBMITTED", "to": "APPROVED", "event": "approve"}
                    ]
                }
                """);

        // When
        Long versionId = versionService.createVersion("ORDER", complexSchema, "admin", "Complex workflow");

        // Then
        Optional<WorkflowVersion> version = versionService.getVersion("ORDER", 1);
        assertThat(version).isPresent();

        JsonNode retrieved = objectMapper.readTree(version.get().schemaDefinition());
        assertThat(retrieved.path("states").size()).isEqualTo(3);
        assertThat(retrieved.path("transitions").size()).isEqualTo(2);
    }
}
