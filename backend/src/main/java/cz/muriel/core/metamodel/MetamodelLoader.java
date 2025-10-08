package cz.muriel.core.metamodel;

import cz.muriel.core.metamodel.schema.EntitySchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

/**
 * Loads metamodel YAML files from classpath
 */
@Slf4j @Component
public class MetamodelLoader {

  private static final String METAMODEL_LOCATION = "classpath:metamodel/*.yaml";

  private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
  private final Yaml yaml = new Yaml();

  /**
   * Load all metamodel schemas from classpath
   */
  public Map<String, EntitySchema> loadSchemas() {
    Map<String, EntitySchema> schemas = new HashMap<>();

    try {
      Resource[] resources = resolver.getResources(METAMODEL_LOCATION);
      log.info("Found {} metamodel files", resources.length);

      for (Resource resource : resources) {
        try (InputStream is = resource.getInputStream()) {
          EntitySchema schema = yaml.load(is);

          // Validation
          validateSchema(schema, resource.getFilename());

          schemas.put(schema.getEntity(), schema);
          log.info("Loaded metamodel: {} -> {}", schema.getEntity(), schema.getTable());

        } catch (Exception e) {
          log.error("Failed to load metamodel from {}: {}", resource.getFilename(), e.getMessage());
          throw new RuntimeException("Invalid metamodel: " + resource.getFilename(), e);
        }
      }

      log.info("Successfully loaded {} metamodel schemas", schemas.size());
      return schemas;

    } catch (Exception e) {
      log.error("Failed to load metamodel schemas", e);
      throw new RuntimeException("Failed to load metamodel", e);
    }
  }

  /**
   * Validate required fields in schema
   */
  private void validateSchema(EntitySchema schema, String filename) {
    List<String> errors = new ArrayList<>();

    if (schema.getEntity() == null || schema.getEntity().isBlank()) {
      errors.add("entity name is required");
    }
    if (schema.getTable() == null || schema.getTable().isBlank()) {
      errors.add("table name is required");
    }
    if (schema.getIdField() == null || schema.getIdField().isBlank()) {
      errors.add("idField is required");
    }
    if (schema.getFields() == null || schema.getFields().isEmpty()) {
      errors.add("at least one field is required");
    }

    // Validate ID field exists
    if (schema.getFields() != null) {
      boolean hasIdField = schema.getFields().stream()
          .anyMatch(f -> f.getName().equals(schema.getIdField()));
      if (!hasIdField) {
        errors.add("idField '" + schema.getIdField() + "' not found in fields");
      }

      // Validate version field if specified
      if (schema.getVersionField() != null) {
        boolean hasVersionField = schema.getFields().stream()
            .anyMatch(f -> f.getName().equals(schema.getVersionField()));
        if (!hasVersionField) {
          errors.add("versionField '" + schema.getVersionField() + "' not found in fields");
        }
      }

      // Validate tenant field if specified
      if (schema.getTenantField() != null) {
        boolean hasTenantField = schema.getFields().stream()
            .anyMatch(f -> f.getName().equals(schema.getTenantField()));
        if (!hasTenantField) {
          errors.add("tenantField '" + schema.getTenantField() + "' not found in fields");
        }
      }
    }

    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(
          "Invalid metamodel in " + filename + ": " + String.join(", ", errors));
    }
  }
}
