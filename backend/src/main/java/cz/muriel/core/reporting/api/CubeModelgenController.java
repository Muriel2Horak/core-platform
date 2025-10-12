package cz.muriel.core.reporting.api;

import cz.muriel.core.reporting.modelgen.CubeModelgenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * REST API for Cube.js schema generation from metamodel.
 * 
 * <p>
 * Admin-only endpoints for generating Cube.js schemas.
 */
@Slf4j @RestController @RequestMapping("/api/admin/cube/modelgen") @RequiredArgsConstructor
public class CubeModelgenController {

  private final CubeModelgenService modelgenService;

  /**
   * Export all metamodel entities to Cube.js schemas.
   * 
   * POST /api/admin/cube/modelgen/export-all
   * 
   * @return List of generated file paths
   */
  @PostMapping("/export-all") @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> exportAll() {
    log.info("Exporting all Cube.js schemas from metamodel");

    List<Path> files = modelgenService.exportAll();

    return ResponseEntity.ok(Map.of("message", "Cube.js schemas exported successfully", "count",
        files.size(), "files", files.stream().map(Path::toString).toList()));
  }

  /**
   * Export single entity to Cube.js schema.
   * 
   * POST /api/admin/cube/modelgen/export/{entityName}
   */
  @PostMapping("/export/{entityName}") @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> exportEntity(@PathVariable String entityName) {
    log.info("Exporting Cube.js schema for entity: {}", entityName);

    try {
      Path file = modelgenService.exportEntity(entityName);

      return ResponseEntity.ok(Map.of("message", "Cube.js schema exported successfully", "entity",
          entityName, "file", file.toString()));
    } catch (Exception e) {
      log.error("Failed to export Cube.js schema for entity: {}", entityName, e);
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Failed to export schema", "message", e.getMessage()));
    }
  }

  /**
   * Preview generated Cube.js schema without writing to file.
   * 
   * GET /api/admin/cube/modelgen/preview/{entityName}
   */
  @GetMapping("/preview/{entityName}") @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<String> preview(@PathVariable String entityName) {
    log.info("Previewing Cube.js schema for entity: {}", entityName);

    try {
      String jsCode = modelgenService.preview(entityName);
      return ResponseEntity.ok(jsCode);
    } catch (Exception e) {
      log.error("Failed to preview Cube.js schema for entity: {}", entityName, e);
      return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
  }

  /**
   * Get modelgen configuration status.
   * 
   * GET /api/admin/cube/modelgen/status
   */
  @GetMapping("/status") @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> status() {
    return ResponseEntity.ok(Map.of("autoExportEnabled", modelgenService.isAutoExportEnabled(),
        "outputDir", "docker/cube/schema"));
  }
}
