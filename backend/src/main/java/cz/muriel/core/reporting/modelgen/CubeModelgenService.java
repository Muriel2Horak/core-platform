package cz.muriel.core.reporting.modelgen;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.EntitySchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for exporting Cube.js schemas from metamodel to filesystem.
 * 
 * <p>Generates .js files in Cube.js schema directory from registered metamodel entities.
 * 
 * <p>Usage:
 * <pre>
 * modelgenService.exportAll();
 * // → Writes docker/cube/schema/User.js, Tenant.js, etc.
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CubeModelgenService {

  private final MetamodelRegistry metamodelRegistry;
  private final CubeSchemaGenerator generator;

  @Value("${app.cube.schema.output-dir:docker/cube/schema}")
  private String outputDir;

  @Value("${app.cube.schema.auto-export:false}")
  private boolean autoExport;

  /**
   * Export all registered metamodel entities to Cube.js schemas.
   * 
   * @return List of generated file paths
   */
  public List<Path> exportAll() {
    log.info("Starting Cube.js schema export from metamodel...");
    
    List<Path> generatedFiles = new ArrayList<>();
    Map<String, EntitySchema> allSchemas = metamodelRegistry.getAllSchemas();

    for (EntitySchema schema : allSchemas.values()) {
      try {
        Path file = export(schema);
        generatedFiles.add(file);
        log.info("Generated Cube.js schema: {}", file);
      } catch (Exception e) {
        log.error("Failed to generate Cube.js schema for entity: {}", schema.getEntity(), e);
      }
    }

    log.info("Cube.js schema export complete: {} files generated", generatedFiles.size());
    return generatedFiles;
  }

  /**
   * Export single entity schema to Cube.js .js file.
   * 
   * @param schema Entity schema to export
   * @return Path to generated file
   */
  public Path export(EntitySchema schema) throws IOException {
    String jsCode = generator.generate(schema);
    Path outputPath = getOutputPath(schema.getEntity());

    // Ensure output directory exists
    Files.createDirectories(outputPath.getParent());

    // Write file
    Files.writeString(
        outputPath, 
        jsCode, 
        StandardOpenOption.CREATE, 
        StandardOpenOption.TRUNCATE_EXISTING
    );

    log.debug("Exported Cube.js schema: {} → {}", schema.getEntity(), outputPath);
    return outputPath;
  }

  /**
   * Export specific entity by name.
   */
  public Path exportEntity(String entityName) throws IOException {
    EntitySchema schema = metamodelRegistry.getSchemaOrThrow(entityName);
    return export(schema);
  }

  /**
   * Preview generated Cube.js schema without writing to file.
   */
  public String preview(String entityName) {
    EntitySchema schema = metamodelRegistry.getSchemaOrThrow(entityName);
    return generator.generate(schema);
  }

  /**
   * Get output file path for entity.
   */
  private Path getOutputPath(String entityName) {
    String fileName = entityName + ".js";
    return Paths.get(outputDir, fileName);
  }

  /**
   * Check if auto-export is enabled.
   */
  public boolean isAutoExportEnabled() {
    return autoExport;
  }
}
