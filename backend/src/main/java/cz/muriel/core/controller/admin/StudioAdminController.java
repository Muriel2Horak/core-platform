package cz.muriel.core.controller.admin;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.EntitySchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * S10: Metamodel Studio Admin API
 * 
 * Phase S10-A: Health check + RBAC Phase S10-B: Read-only viewer (export
 * entities) Phase S10-C: Editors (validate) Phase S10-D: Diff/Propose/Approve
 * Phase S10-E: Workflow steps editor
 * 
 * Note: MetamodelAdminController already has: - GET /api/admin/metamodel/reload
 * (hot reload + diff) - POST /api/admin/metamodel/apply-safe-changes - GET
 * /api/admin/metamodel/status
 */
@Slf4j @RestController @RequestMapping("/api/admin/studio") @RequiredArgsConstructor @PreAuthorize("hasAuthority('CORE_ADMIN_STUDIO')")
public class StudioAdminController {

  private final MetamodelRegistry metamodelRegistry;

  /**
   * W0: Health check
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    log.info("🎨 Studio Admin API health check");
    return ResponseEntity.ok(Map.of("status", "ok", "phase", "S10-B", "message",
        "Studio Admin API ready - metamodel export available"));
  }

  /**
   * S10-B: Export all entities from metamodel
   * 
   * GET /api/admin/studio/entities
   * 
   * Returns all entity schemas with fields, relations, validations
   */
  @GetMapping("/entities")
  public ResponseEntity<Map<String, Object>> exportEntities() {
    log.info("📦 Exporting all entities from metamodel");

    try {
      Map<String, EntitySchema> schemas = metamodelRegistry.getAllSchemas();

      // Convert to simplified DTO for frontend
      List<Map<String, Object>> entities = schemas.entrySet().stream().map(entry -> {
        EntitySchema schema = entry.getValue();
        Map<String, Object> entityDto = new LinkedHashMap<>();
        entityDto.put("name", entry.getKey());
        entityDto.put("entity", schema.getEntity());
        entityDto.put("table", schema.getTable());
        entityDto.put("idField", schema.getIdField());
        entityDto.put("versionField", schema.getVersionField());
        entityDto.put("tenantField", schema.getTenantField());
        entityDto.put("fields", schema.getFields());
        entityDto.put("accessPolicy", schema.getAccessPolicy());
        entityDto.put("ui", schema.getUi());
        entityDto.put("navigation", schema.getNavigation());
        entityDto.put("features", schema.getFeatures());
        entityDto.put("fulltext", schema.getFulltext());
        entityDto.put("states", schema.getStates());
        entityDto.put("transitions", schema.getTransitions());
        entityDto.put("idGeneration", schema.getIdGeneration());
        entityDto.put("lifecycle", schema.getLifecycle());
        entityDto.put("streaming", schema.getStreaming());
        return entityDto;
      }).collect(Collectors.toList());

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("status", "success");
      response.put("entitiesCount", entities.size());
      response.put("entities", entities);

      log.info("✅ Exported {} entities", entities.size());
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("❌ Failed to export entities: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", "Failed to export entities: " + e.getMessage()));
    }
  }

  /**
   * S10-B: Get single entity detail
   * 
   * GET /api/admin/studio/entities/{entity}
   */
  @GetMapping("/entities/{entity}")
  public ResponseEntity<Map<String, Object>> getEntity(@PathVariable String entity) {
    log.info("📋 Getting entity detail: {}", entity);

    Optional<EntitySchema> schema = metamodelRegistry.getSchema(entity);
    if (schema.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    EntitySchema s = schema.get();
    Map<String, Object> entityDto = new LinkedHashMap<>();
    entityDto.put("name", entity);
    entityDto.put("entity", s.getEntity());
    entityDto.put("table", s.getTable());
    entityDto.put("idField", s.getIdField());
    entityDto.put("versionField", s.getVersionField());
    entityDto.put("tenantField", s.getTenantField());
    entityDto.put("fields", s.getFields());
    entityDto.put("accessPolicy", s.getAccessPolicy());
    entityDto.put("ui", s.getUi());
    entityDto.put("navigation", s.getNavigation());
    entityDto.put("features", s.getFeatures());
    entityDto.put("fulltext", s.getFulltext());
    entityDto.put("states", s.getStates());
    entityDto.put("transitions", s.getTransitions());
    entityDto.put("idGeneration", s.getIdGeneration());
    entityDto.put("lifecycle", s.getLifecycle());
    entityDto.put("streaming", s.getStreaming());

    return ResponseEntity.ok(entityDto);
  }

  // TODO S10-C: PUT /api/admin/studio/entities/{entity}
  // TODO S10-C: POST /api/admin/studio/validate
  // TODO S10-D: POST /api/admin/studio/preview
  // TODO S10-D: POST /api/admin/studio/proposals
  // TODO S10-D: POST /api/admin/studio/proposals/{id}/approve
}
