package cz.muriel.core.metamodel;

import cz.muriel.core.metamodel.schema.EntitySchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for metamodel schemas Thread-safe and supports runtime
 * reload
 */
@Slf4j @Component @RequiredArgsConstructor
public class MetamodelRegistry {

  private final MetamodelLoader loader;
  private final Map<String, EntitySchema> schemas = new ConcurrentHashMap<>();

  @PostConstruct
  public void init() {
    reload();
  }

  /**
   * Reload all schemas from classpath
   */
  public synchronized void reload() {
    log.info("Reloading metamodel registry...");
    Map<String, EntitySchema> newSchemas = loader.loadSchemas();

    schemas.clear();
    schemas.putAll(newSchemas);

    log.info("Metamodel registry reloaded with {} schemas", schemas.size());
  }

  /**
   * Get schema by entity type
   */
  public Optional<EntitySchema> getSchema(String entityType) {
    return Optional.ofNullable(schemas.get(entityType));
  }

  /**
   * Get schema by entity type or throw exception
   */
  public EntitySchema getSchemaOrThrow(String entityType) {
    return getSchema(entityType)
        .orElseThrow(() -> new IllegalArgumentException("Unknown entity type: " + entityType));
  }

  /**
   * Check if entity type exists
   */
  public boolean hasSchema(String entityType) {
    return schemas.containsKey(entityType);
  }

  /**
   * Get all registered entity types
   */
  public Map<String, EntitySchema> getAllSchemas() {
    return Map.copyOf(schemas);
  }

  /**
   * Get schema by table name (reverse lookup)
   */
  public Optional<EntitySchema> getSchemaByTable(String tableName) {
    return schemas.values().stream().filter(s -> s.getTable().equals(tableName)).findFirst();
  }
}
