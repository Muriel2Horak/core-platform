package com.example.platform.workflow.version;

import com.fasterxml.jackson.databind.JsonNode;
import com.platform.workflow.versioning.WorkflowVersionService;
import com.platform.workflow.versioning.WorkflowVersionService.MigrationStrategy;
import com.platform.workflow.versioning.WorkflowVersionService.WorkflowVersion;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 🔄 W9: Workflow Versioning REST API
 * 
 * Manages workflow schema versions with:
 * - Version CRUD operations
 * - Version activation/deactivation
 * - Instance migration between versions
 * - Migration history tracking
 * 
 * Security: Requires ROLE_WORKFLOW_ADMIN for mutations
 * 
 * @since 2025-01-14
 */
@RestController
@RequestMapping("/api/v1/workflows/versions")
@RequiredArgsConstructor
@Slf4j
public class WorkflowVersionController {

    private final WorkflowVersionService versionService;

    /**
     * Create new workflow version
     */
    @PostMapping
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @Timed(value = "workflow.version.create", description = "Time to create workflow version")
    public ResponseEntity<Long> createVersion(
            @RequestBody CreateVersionRequest request) {
        
        log.info("Creating workflow version for entity_type={}", request.entityType());
        
        Long versionId = versionService.createVersion(
                request.entityType(),
                request.schemaDefinition(),
                request.createdBy(),
                request.notes()
        );
        
        return ResponseEntity.ok(versionId);
    }

    /**
     * Activate specific version (deactivates others)
     */
    @PostMapping("/{versionId}/activate")
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @Timed(value = "workflow.version.activate", description = "Time to activate workflow version")
    public ResponseEntity<Void> activateVersion(@PathVariable Long versionId) {
        log.info("Activating workflow version id={}", versionId);
        versionService.activateVersion(versionId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get active version for entity type
     */
    @GetMapping("/active/{entityType}")
    @Timed(value = "workflow.version.get_active", description = "Time to get active version")
    public ResponseEntity<WorkflowVersion> getActiveVersion(@PathVariable String entityType) {
        return versionService.getActiveVersion(entityType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get specific version by ID
     */
    @GetMapping("/{versionId}")
    @Timed(value = "workflow.version.get", description = "Time to get workflow version")
    public ResponseEntity<WorkflowVersion> getVersion(
            @PathVariable Long versionId,
            @RequestParam String entityType,
            @RequestParam int version) {
        return versionService.getVersion(entityType, version)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all versions for entity type
     */
    @GetMapping("/entity/{entityType}")
    @Timed(value = "workflow.version.list", description = "Time to list workflow versions")
    public ResponseEntity<List<WorkflowVersion>> getAllVersions(@PathVariable String entityType) {
        return ResponseEntity.ok(versionService.getAllVersions(entityType));
    }

    /**
     * Migrate workflow instance to new version
     */
    @PostMapping("/migrate")
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @Timed(value = "workflow.version.migrate", description = "Time to migrate workflow instance")
    public ResponseEntity<Void> migrateInstance(@RequestBody MigrateInstanceRequest request) {
        log.info("Migrating workflow instance={} to version={}", 
                request.instanceId(), request.toVersionId());
        
        versionService.migrateInstance(
                request.instanceId(),
                request.toVersionId(),
                request.strategy()
        );
        
        return ResponseEntity.ok().build();
    }

    /**
     * Start bulk migration for entity type
     */
    @PostMapping("/migrate/bulk")
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @Timed(value = "workflow.version.migrate_bulk", description = "Time to start bulk migration")
    public ResponseEntity<Long> startBulkMigration(@RequestBody BulkMigrationRequest request) {
        log.info("Starting bulk migration from version={} to version={}", 
                request.fromVersionId(), request.toVersionId());
        
        Long migrationId = versionService.startMigration(
                request.fromVersionId(),
                request.toVersionId(),
                request.strategy(),
                request.initiatedBy()
        );
        
        return ResponseEntity.accepted().body(migrationId);
    }

    /**
     * Get migration history for instance
     */
    @GetMapping("/migrations/instance/{instanceId}")
    @Timed(value = "workflow.version.migration_history", description = "Time to get migration history")
    public ResponseEntity<List<Map<String, Object>>> getMigrationHistory(@PathVariable Long instanceId) {
        // TODO: Implement migration history query
        return ResponseEntity.ok(List.of());
    }

    // ===== Request DTOs =====

    public record CreateVersionRequest(
            String entityType,
            JsonNode schemaDefinition,
            String createdBy,
            String notes
    ) {}

    public record MigrateInstanceRequest(
            Long instanceId,
            Long toVersionId,
            MigrationStrategy strategy
    ) {}

    public record BulkMigrationRequest(
            Long fromVersionId,
            Long toVersionId,
            MigrationStrategy strategy,
            String initiatedBy
    ) {}
}
