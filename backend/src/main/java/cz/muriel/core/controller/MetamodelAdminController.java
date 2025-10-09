package cz.muriel.core.controller;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.MetamodelSchemaGenerator;
import cz.muriel.core.metamodel.schema.SchemaDiff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Admin API for Metamodel hot reload and schema management
 */
@RestController
@RequestMapping("/api/admin/metamodel")
@RequiredArgsConstructor
@Slf4j
public class MetamodelAdminController {

  private final MetamodelRegistry registry;
  private final MetamodelSchemaGenerator schemaGenerator;

  /**
   * Reload metamodel YAML files and detect schema changes
   * 
   * GET /api/admin/metamodel/reload
   */
  @GetMapping("/reload")
  public ResponseEntity<Map<String, Object>> reloadMetamodel() {
    log.info("🔄 Admin triggered metamodel reload");
    
    try {
      // 1. Reload YAML definitions
      registry.reload();
      log.info("✅ Metamodel YAML reloaded");
      
      // 2. Detect schema changes for all entities
      Map<String, SchemaDiff> diffs = new HashMap<>();
      for (var entry : registry.getAllSchemas().entrySet()) {
        SchemaDiff diff = schemaGenerator.detectChanges(entry.getValue());
        if (!diff.getColumnChanges().isEmpty()) {
          diffs.put(entry.getKey(), diff);
        }
      }
      
      // 3. Build response
      Map<String, Object> response = new HashMap<>();
      response.put("status", "success");
      response.put("message", "Metamodel reloaded successfully");
      response.put("entitiesCount", registry.getAllSchemas().size());
      response.put("changesDetected", diffs.size());
      response.put("changes", buildChangeSummary(diffs));
      
      if (diffs.isEmpty()) {
        log.info("✅ No schema changes detected");
      } else {
        log.warn("⚠️ Schema changes detected in {} entities - review required", diffs.size());
      }
      
      return ResponseEntity.ok(response);
      
    } catch (Exception e) {
      log.error("❌ Failed to reload metamodel: {}", e.getMessage(), e);
      
      Map<String, Object> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Failed to reload metamodel: " + e.getMessage());
      
      return ResponseEntity.internalServerError().body(error);
    }
  }
  
  /**
   * Apply safe schema changes automatically
   * 
   * POST /api/admin/metamodel/apply-safe-changes
   */
  @PostMapping("/apply-safe-changes")
  public ResponseEntity<Map<String, Object>> applySafeChanges() {
    log.info("🔨 Admin triggered safe schema changes application");
    
    try {
      // Re-run schema generation (will apply safe changes)
      schemaGenerator.generateSchema();
      
      Map<String, Object> response = new HashMap<>();
      response.put("status", "success");
      response.put("message", "Safe schema changes applied successfully");
      
      return ResponseEntity.ok(response);
      
    } catch (Exception e) {
      log.error("❌ Failed to apply changes: {}", e.getMessage(), e);
      
      Map<String, Object> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Failed to apply changes: " + e.getMessage());
      
      return ResponseEntity.internalServerError().body(error);
    }
  }
  
  /**
   * Get current schema status for all entities
   * 
   * GET /api/admin/metamodel/status
   */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    log.info("📊 /status endpoint called");
    Map<String, Object> response = new HashMap<>();
    
    response.put("status", "success");
    response.put("entitiesCount", registry.getAllSchemas().size());
    response.put("entities", registry.getAllSchemas().keySet());
    log.info("✅ Basic info collected, entities: {}", registry.getAllSchemas().size());
    
    // Detect changes for status
    Map<String, SchemaDiff> diffs = new HashMap<>();
    log.info("🔍 Starting change detection for {} entities", registry.getAllSchemas().size());
    
    for (var entry : registry.getAllSchemas().entrySet()) {
      log.info("🔍 Detecting changes for entity: {}", entry.getKey());
      SchemaDiff diff = schemaGenerator.detectChanges(entry.getValue());
      log.info("✅ Detected {} changes for {}", diff.getColumnChanges().size(), entry.getKey());
      
      if (!diff.getColumnChanges().isEmpty()) {
        diffs.put(entry.getKey(), diff);
      }
    }
    
    log.info("🔍 Building change summary...");
    response.put("pendingChanges", diffs.size());
    response.put("changes", buildChangeSummary(diffs));
    
    log.info("✅ Returning status response");
    return ResponseEntity.ok(response);
  }
  
  private Map<String, Object> buildChangeSummary(Map<String, SchemaDiff> diffs) {
    Map<String, Object> summary = new HashMap<>();
    
    for (var entry : diffs.entrySet()) {
      String entityType = entry.getKey();
      SchemaDiff diff = entry.getValue();
      
      Map<String, Object> entitySummary = new HashMap<>();
      entitySummary.put("tableName", diff.getTableName());
      entitySummary.put("totalChanges", diff.getColumnChanges().size());
      entitySummary.put("hasRiskyChanges", diff.hasRiskyChanges());
      
      // Categorize changes
      long safeChanges = diff.getColumnChanges().stream()
          .filter(c -> !c.isRisky())
          .count();
      long riskyChanges = diff.getColumnChanges().stream()
          .filter(SchemaDiff.ColumnChange::isRisky)
          .count();
      
      entitySummary.put("safeChanges", safeChanges);
      entitySummary.put("riskyChanges", riskyChanges);
      
      // List all changes
      List<Map<String, String>> changesList = new ArrayList<>();
      for (var change : diff.getColumnChanges()) {
        Map<String, String> changeInfo = new HashMap<>();
        changeInfo.put("type", change.getType().toString());
        changeInfo.put("column", change.getColumnName());
        changeInfo.put("risky", String.valueOf(change.isRisky()));
        
        if (change.getOldType() != null) {
          changeInfo.put("oldType", change.getOldType());
        }
        if (change.getNewType() != null) {
          changeInfo.put("newType", change.getNewType());
        }
        if (change.getRiskDescription() != null) {
          changeInfo.put("warning", change.getRiskDescription());
        }
        
        changesList.add(changeInfo);
      }
      
      entitySummary.put("details", changesList);
      summary.put(entityType, entitySummary);
    }
    
    return summary;
  }
}
