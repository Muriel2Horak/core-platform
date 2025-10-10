package cz.muriel.core.metamodel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

/**
 * Loads metamodel YAML files from classpath
 */
@Slf4j @Component
public class MetamodelLoader {

  private static final String METAMODEL_LOCATION = "classpath:metamodel/*.yaml";
  private static final String GLOBAL_CONFIG_LOCATION = "classpath:metamodel/global-config.yaml";

  private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  /**
   * Load global metamodel configuration
   */
  public GlobalMetamodelConfig loadGlobalConfig() {
    try {
      Resource resource = resolver.getResource(GLOBAL_CONFIG_LOCATION);
      if (!resource.exists()) {
        log.warn("Global config not found, using defaults");
        return new GlobalMetamodelConfig();
      }

      try (InputStream is = resource.getInputStream()) {
        GlobalMetamodelConfig config = yamlMapper.readValue(is, GlobalMetamodelConfig.class);
        log.info("Loaded global metamodel config: streaming.enabled={}",
            config.getStreaming().isEnabled());
        return config;
      }
    } catch (Exception e) {
      log.error("Failed to load global config, using defaults", e);
      return new GlobalMetamodelConfig();
    }
  }

  /**
   * Load all metamodel schemas from classpath
   */
  public Map<String, EntitySchema> loadSchemas() {
    Map<String, EntitySchema> schemas = new HashMap<>();

    try {
      Resource[] resources = resolver.getResources(METAMODEL_LOCATION);
      log.info("Found {} metamodel files", resources.length);

      for (Resource resource : resources) {
        // Skip global-config.yaml as it's loaded separately by loadGlobalConfig()
        if (resource.getFilename() != null && resource.getFilename().equals("global-config.yaml")) {
          log.debug("Skipping global-config.yaml in schema loading");
          continue;
        }

        try (InputStream is = resource.getInputStream()) {
          EntitySchema schema = yamlMapper.readValue(is, EntitySchema.class);

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
