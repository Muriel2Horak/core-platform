package cz.muriel.core.reporting.api;

import cz.muriel.core.reporting.support.MetamodelSpecService;
import cz.muriel.core.reporting.support.EntitySpec;
import cz.muriel.core.reporting.cube.CubeSecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * EntityCrudController - REST API for inline editing
 * 
 * Provides PATCH endpoint for single-row updates with:
 * - Optimistic locking (If-Match header with version)
 * - Row-Level Security (tenant_id filter)
 * - Field-level validation against entity spec
 * - Audit logging
 * 
 * Used by ExplorerGrid component for inline cell editing.
 */
@Slf4j
@RestController
@RequestMapping("/api/entities")
@RequiredArgsConstructor
public class EntityCrudController {

    private final DSLContext dsl;
    private final MetamodelSpecService specService;
    private final CubeSecurityContext cubeSecurityContext;

    /**
     * PATCH /api/entities/{entity}/{id}
     * 
     * Update single entity record with optimistic locking.
     * 
     * @param entity Entity name (e.g., "User", "Tenant")
     * @param id Record UUID
     * @param patch Map of field -> value to update
     * @param ifMatch Version number from If-Match header (required)
     * @param auth Spring Security authentication
     * @return Updated record with new version
     */
    @PatchMapping("/{entity}/{id}")
    public ResponseEntity<Map<String, Object>> patchEntity(
            @PathVariable String entity,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> patch,
            @RequestHeader(value = "If-Match", required = false) Integer ifMatch,
            Authentication auth) {
        
        log.info("PATCH /api/entities/{}/{} with {} fields, version={}", 
                entity, id, patch.size(), ifMatch);

        // 1. Validate If-Match header
        if (ifMatch == null) {
            throw new ResponseStatusException(
                HttpStatus.PRECONDITION_REQUIRED, 
                "If-Match header is required for updates"
            );
        }

        // 2. Get entity spec for validation
        EntitySpec spec = specService.getFullEntitySpec(entity);
        if (spec == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, 
                "Entity not found: " + entity
            );
        }

        // 3. Validate editable fields
        for (String fieldName : patch.keySet()) {
            if (!spec.getEditableFields().contains(fieldName)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Field '" + fieldName + "' is not editable"
                );
            }
        }

        // 4. Get table reference
        String tableName = spec.getTableName();
        Table<?> table = DSL.table(DSL.name(tableName));

        // 5. Build RLS filter (tenant_id = current user's tenant)
        String tenantIdStr = cubeSecurityContext.extractTenantId(auth);
        if (tenantIdStr == null) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tenant context available"
            );
        }
        UUID tenantId = UUID.fromString(tenantIdStr);

        // 6. Fetch current record with version check
        Record currentRecord = dsl.selectFrom(table)
            .where(DSL.field("id").eq(id))
            .and(DSL.field("tenant_id").eq(tenantId))
            .and(DSL.field("version").eq(ifMatch))
            .fetchOne();

        if (currentRecord == null) {
            // Check if record exists at all
            Integer currentVersion = dsl.select(DSL.field("version", Integer.class))
                .from(table)
                .where(DSL.field("id").eq(id))
                .and(DSL.field("tenant_id").eq(tenantId))
                .fetchOne(DSL.field("version", Integer.class));

            if (currentVersion == null) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Record not found or access denied"
                );
            } else {
                // Version mismatch - concurrent modification
                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("message", "Concurrent modification detected. Please reload and try again.");
                errorBody.put("currentVersion", currentVersion);
                errorBody.put("providedVersion", ifMatch);
                
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody);
            }
        }

        // 7. Build UPDATE statement with version increment
        Map<String, Object> updates = new HashMap<>(patch);
        updates.put("version", ifMatch + 1);
        updates.put("updated_at", OffsetDateTime.now());
        updates.put("updated_by", auth.getName());

        // 8. Execute UPDATE
        int rowsUpdated = dsl.update(table)
            .set(buildFieldMap(updates))
            .where(DSL.field("id").eq(id))
            .and(DSL.field("tenant_id").eq(tenantId))
            .and(DSL.field("version").eq(ifMatch))
            .execute();

        if (rowsUpdated == 0) {
            // Race condition - someone else updated between our SELECT and UPDATE
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Concurrent modification detected during update"
            );
        }

        // 9. Fetch updated record
        Record updatedRecord = dsl.selectFrom(table)
            .where(DSL.field("id").eq(id))
            .and(DSL.field("tenant_id").eq(tenantId))
            .fetchOne();

        Map<String, Object> result = updatedRecord.intoMap();

        // 10. Audit log
        log.info("Updated {}/{}: {} fields changed by {}", 
                entity, id, patch.keySet(), auth.getName());

        return ResponseEntity.ok()
            .header("ETag", String.valueOf(result.get("version")))
            .body(result);
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
}
