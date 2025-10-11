package cz.muriel.core.reporting.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cz.muriel.core.reporting.cube.CubeSecurityContext;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BulkUpdateController - REST API for bulk operations
 * 
 * Provides endpoints for:
 * - POST /api/entities/{entity}/bulk-update: Initiate bulk update job
 * - GET /api/bulk-jobs/{jobId}: Get job status
 * - POST /api/bulk-jobs/{jobId}/cancel: Cancel running job
 * 
 * Features:
 * - Async processing with chunking (100 rows per chunk)
 * - Job status tracking (PENDING, RUNNING, COMPLETED, FAILED)
 * - Row-Level Security enforcement
 * - Progress reporting
 * 
 * Used by ExplorerGrid component for bulk Activate/Deactivate actions.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BulkUpdateController {

    private final DSLContext dsl;
    private final CubeSecurityContext cubeSecurityContext;
    
    // In-memory job tracker (production: use Redis or database)
    private final Map<String, BulkJob> jobs = new ConcurrentHashMap<>();
    
    private static final int CHUNK_SIZE = 100;

    /**
     * POST /api/entities/{entity}/bulk-update
     * 
     * Initiate bulk update operation.
     * 
     * @param entity Entity name
     * @param request Bulk update request
     * @param auth Spring Security authentication
     * @return Job response with jobId for status polling
     */
    @PostMapping("/entities/{entity}/bulk-update")
    public ResponseEntity<BulkJobResponse> bulkUpdate(
            @PathVariable String entity,
            @Valid @RequestBody BulkUpdateRequest request,
            Authentication auth) {
        
        log.info("POST /api/entities/{}/bulk-update: {} rows", 
                entity, request.getIds().size());

        // Validate request
        if (request.getIds().size() > 1000) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Maximum 1000 rows per bulk operation"
            );
        }

        // Get tenant ID for RLS
        String tenantIdStr = cubeSecurityContext.extractTenantId(auth);
        if (tenantIdStr == null) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tenant context available"
            );
        }
        UUID tenantId = UUID.fromString(tenantIdStr);

        // Create job
        String jobId = UUID.randomUUID().toString();
        BulkJob job = BulkJob.builder()
            .jobId(jobId)
            .entity(entity)
            .totalRows(request.getIds().size())
            .status(JobStatus.PENDING)
            .createdAt(OffsetDateTime.now())
            .createdBy(auth.getName())
            .build();
        
        jobs.put(jobId, job);

        // Start async processing
        CompletableFuture.runAsync(() -> {
            processBulkUpdate(jobId, entity, request, tenantId, auth.getName());
        });

        BulkJobResponse response = BulkJobResponse.builder()
            .jobId(jobId)
            .status(job.getStatus())
            .message("Bulk update job started")
            .build();

        return ResponseEntity.accepted().body(response);
    }

    /**
     * GET /api/bulk-jobs/{jobId}
     * 
     * Get job status and progress.
     */
    @GetMapping("/bulk-jobs/{jobId}")
    public ResponseEntity<BulkJobResponse> getJobStatus(@PathVariable String jobId) {
        BulkJob job = jobs.get(jobId);
        if (job == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Job not found: " + jobId
            );
        }

        BulkJobResponse response = BulkJobResponse.builder()
            .jobId(job.getJobId())
            .status(job.getStatus())
            .processedRows(job.getProcessedRows())
            .totalRows(job.getTotalRows())
            .successCount(job.getSuccessCount())
            .errorCount(job.getErrorCount())
            .message(job.getMessage())
            .errors(job.getErrors())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/bulk-jobs/{jobId}/cancel
     * 
     * Cancel running job.
     */
    @PostMapping("/bulk-jobs/{jobId}/cancel")
    public ResponseEntity<Void> cancelJob(@PathVariable String jobId) {
        BulkJob job = jobs.get(jobId);
        if (job == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Job not found: " + jobId
            );
        }

        if (job.getStatus() == JobStatus.RUNNING || job.getStatus() == JobStatus.PENDING) {
            job.setStatus(JobStatus.CANCELLED);
            job.setMessage("Job cancelled by user");
            log.info("Job {} cancelled", jobId);
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Process bulk update in background.
     */
    private void processBulkUpdate(
            String jobId, 
            String entity, 
            BulkUpdateRequest request,
            UUID tenantId,
            String username) {
        
        BulkJob job = jobs.get(jobId);
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(OffsetDateTime.now());

        try {
            // Derive table name (simple heuristic)
            String tableName = toSnakeCase(entity);
            Table<?> table = DSL.table(DSL.name(tableName));

            List<UUID> ids = request.getIds();
            Map<String, Object> updates = request.getUpdates();
            
            // Add metadata
            updates.put("updated_at", OffsetDateTime.now());
            updates.put("updated_by", username);

            // Process in chunks
            int totalChunks = (int) Math.ceil((double) ids.size() / CHUNK_SIZE);
            
            for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                // Check if job cancelled
                if (job.getStatus() == JobStatus.CANCELLED) {
                    log.info("Job {} cancelled, stopping", jobId);
                    return;
                }

                int fromIndex = chunkIndex * CHUNK_SIZE;
                int toIndex = Math.min(fromIndex + CHUNK_SIZE, ids.size());
                List<UUID> chunk = ids.subList(fromIndex, toIndex);

                // Execute UPDATE for chunk
                try {
                    int updated = dsl.update(table)
                        .set(buildFieldMap(updates))
                        .where(DSL.field("id").in(chunk))
                        .and(DSL.field("tenant_id").eq(tenantId))
                        .execute();

                    job.incrementSuccess(updated);
                    job.setProcessedRows(toIndex);

                    log.debug("Job {}: Chunk {}/{} updated {} rows", 
                            jobId, chunkIndex + 1, totalChunks, updated);

                } catch (Exception e) {
                    log.error("Job {}: Chunk {}/{} failed: {}", 
                            jobId, chunkIndex + 1, totalChunks, e.getMessage());
                    job.incrementError(chunk.size());
                    job.addError("Chunk " + (chunkIndex + 1) + ": " + e.getMessage());
                }
            }

            // Job completed
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(OffsetDateTime.now());
            job.setMessage("Bulk update completed: " + job.getSuccessCount() + 
                    " rows updated, " + job.getErrorCount() + " errors");

            log.info("Job {} completed: {} success, {} errors", 
                    jobId, job.getSuccessCount(), job.getErrorCount());

        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(OffsetDateTime.now());
            job.setMessage("Bulk update failed: " + e.getMessage());
            job.addError(e.getMessage());
        }
    }

    /**
     * Helper: Convert Map to jOOQ Field Map
     */
    private Map<Field<?>, Object> buildFieldMap(Map<String, Object> updates) {
        Map<Field<?>, Object> fieldMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            fieldMap.put(DSL.field(DSL.name(entry.getKey())), entry.getValue());
        }
        return fieldMap;
    }

    /**
     * Helper: Convert entity name to snake_case table name
     */
    private String toSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String snakeCase = input.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        if (!snakeCase.endsWith("s")) {
            snakeCase = snakeCase + "s";
        }
        return snakeCase;
    }

    // ===== DTOs =====

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BulkUpdateRequest {
        @NotEmpty(message = "IDs list cannot be empty")
        @Size(max = 1000, message = "Maximum 1000 rows per bulk operation")
        private List<UUID> ids;

        @NotNull(message = "Updates map cannot be null")
        private Map<String, Object> updates;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BulkJobResponse {
        private String jobId;
        private JobStatus status;
        private Integer processedRows;
        private Integer totalRows;
        private Integer successCount;
        private Integer errorCount;
        private String message;
        private List<String> errors;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BulkJob {
        private String jobId;
        private String entity;
        private JobStatus status;
        private Integer totalRows;
        @Builder.Default
        private AtomicInteger processedRows = new AtomicInteger(0);
        @Builder.Default
        private AtomicInteger successCount = new AtomicInteger(0);
        @Builder.Default
        private AtomicInteger errorCount = new AtomicInteger(0);
        @Builder.Default
        private List<String> errors = new ArrayList<>();
        private OffsetDateTime createdAt;
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;
        private String createdBy;
        private String message;

        public void setProcessedRows(int value) {
            if (processedRows == null) {
                processedRows = new AtomicInteger(value);
            } else {
                processedRows.set(value);
            }
        }

        public Integer getProcessedRows() {
            return processedRows != null ? processedRows.get() : 0;
        }

        public void incrementSuccess(int count) {
            if (successCount == null) {
                successCount = new AtomicInteger(count);
            } else {
                successCount.addAndGet(count);
            }
        }

        public Integer getSuccessCount() {
            return successCount != null ? successCount.get() : 0;
        }

        public void incrementError(int count) {
            if (errorCount == null) {
                errorCount = new AtomicInteger(count);
            } else {
                errorCount.addAndGet(count);
            }
        }

        public Integer getErrorCount() {
            return errorCount != null ? errorCount.get() : 0;
        }

        public void addError(String error) {
            if (errors == null) {
                errors = new ArrayList<>();
            }
            errors.add(error);
        }

        public List<String> getErrors() {
            return errors != null ? errors : Collections.emptyList();
        }
    }

    public enum JobStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
