package cz.muriel.core.entities;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.lifecycle.LifecycleHookExecutor;
import cz.muriel.core.metamodel.relationship.RelationshipResolver;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.FieldSchema;
import cz.muriel.core.security.policy.PolicyEngine;
import cz.muriel.core.util.UUIDv7Generator;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generic CRUD service for metamodel entities
 */
@Slf4j @Service @RequiredArgsConstructor
public class MetamodelCrudService {

  private final MetamodelRegistry registry;
  private final PolicyEngine policyEngine;
  private final EntityManager entityManager;
  private final LifecycleHookExecutor lifecycleExecutor;
  private final RelationshipResolver relationshipResolver;

  /**
   * List entities with filtering, sorting and pagination
   */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> list(String entityType, Map<String, String> filters, String sort,
      int page, int size, Authentication auth) {
    EntitySchema schema = registry.getSchemaOrThrow(entityType);

    // Check read permission (entity-level, no specific instance)
    if (!policyEngine.check(auth, entityType, "read", null)) {
      throw new AccessDeniedException("No permission to read " + entityType);
    }

    // Get allowed columns
    Set<String> allowedColumns = policyEngine.projectColumns(auth, entityType, "read");
    if (allowedColumns.isEmpty()) {
      // Empty set means all columns for backward compatibility
      allowedColumns = schema.getFields().stream().map(FieldSchema::getName)
          .collect(Collectors.toSet());
    }

    // Build SQL query
    String columns = String.join(", ", allowedColumns);
    StringBuilder sql = new StringBuilder("SELECT " + columns + " FROM " + schema.getTable());

    // Apply filters
    List<String> whereClauses = new ArrayList<>();
    for (var entry : filters.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if (key.endsWith("__like")) {
        String field = key.substring(0, key.length() - 6);
        whereClauses.add(field + " LIKE '%" + sanitize(value) + "%'");
      } else if (key.endsWith("__in")) {
        String field = key.substring(0, key.length() - 4);
        String[] values = value.split(",");
        String inList = Arrays.stream(values).map(v -> "'" + sanitize(v) + "'")
            .collect(Collectors.joining(", "));
        whereClauses.add(field + " IN (" + inList + ")");
      } else {
        whereClauses.add(key + " = '" + sanitize(value) + "'");
      }
    }

    if (!whereClauses.isEmpty()) {
      sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
    }

    // Apply sorting
    if (sort != null && !sort.isBlank()) {
      if (sort.startsWith("-")) {
        sql.append(" ORDER BY ").append(sort.substring(1)).append(" DESC");
      } else {
        sql.append(" ORDER BY ").append(sort).append(" ASC");
      }
    }

    // Apply pagination
    sql.append(" LIMIT ").append(size).append(" OFFSET ").append(page * size);

    // Execute query
    @SuppressWarnings("unchecked")
    List<Object[]> results = entityManager.createNativeQuery(sql.toString()).getResultList();

    // Map to response
    List<String> columnList = new ArrayList<>(allowedColumns);
    return results.stream().map(row -> mapRowToMap(row, columnList)).collect(Collectors.toList());
  }

  /**
   * Get entity by ID
   * 
   * ⚠️ REQUIRES_NEW: Get fresh data from DB, not from parent transaction's read
   * view This is critical for retry loops that need to see updated version
   * numbers
   */
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public Map<String, Object> getById(String entityType, String id, Authentication auth) {
    EntitySchema schema = registry.getSchemaOrThrow(entityType);

    // Load entity
    Object entity = findEntityById(schema, id);
    if (entity == null) {
      throw new EntityNotFoundException(entityType, id);
    }

    // Check permission with entity context
    if (!policyEngine.check(auth, entityType, "read", entity)) {
      throw new AccessDeniedException("No permission to read this " + entityType);
    }

    // Get allowed columns
    Set<String> allowedColumns = policyEngine.projectColumns(auth, entityType, "read");

    // Project to map
    return projectEntityToMap(entity, allowedColumns, schema);
  }

  /**
   * Create new entity
   */
  @Transactional
  public Map<String, Object> create(String entityType, Map<String, Object> data,
      Authentication auth) {
    EntitySchema schema = registry.getSchemaOrThrow(entityType);

    // ✨ LIFECYCLE: Execute beforeCreate hooks
    lifecycleExecutor.executeBeforeCreate(schema, data);

    // 🆔 AUTO-GENERATE UUID v7: If no ID provided, generate time-ordered globally
    // unique UUID
    // This ensures:
    // - Never repeats (even across different databases/environments)
    // - Safe for parallel/distributed systems
    // - Sortable by creation time
    // - No need for manual UUID generation in sync services
    if (schema.getIdField() != null && !data.containsKey(schema.getIdField())) {
      UUID generatedId = UUIDv7Generator.generate();
      data.put(schema.getIdField(), generatedId);
      log.debug("Generated UUID v7 for {}: {}", entityType, generatedId);
    }

    // Add tenant_id from JWT (only if not already set - important for
    // SystemAuthentication)
    String tenantId = getTenantId(auth);
    if (schema.getTenantField() != null && tenantId != null
        && !data.containsKey(schema.getTenantField())) {
      data.put(schema.getTenantField(), tenantId);
    }

    // Set version to 0
    if (schema.getVersionField() != null) {
      data.put(schema.getVersionField(), 0L);
    }

    // Check permission (with data as pseudo-entity)
    if (!policyEngine.check(auth, entityType, "create", data)) {
      throw new AccessDeniedException("No permission to create " + entityType);
    }

    // Execute native INSERT
    String insertSql = buildInsertSql(schema, data);
    int affected = entityManager.createNativeQuery(insertSql).executeUpdate();

    if (affected == 0) {
      throw new RuntimeException("Failed to create entity");
    }

    // Get generated ID
    Object id = data.get(schema.getIdField());

    // ✨ RELATIONSHIPS: Save M:N relationships
    relationshipResolver.saveRelationships(schema, id, data);

    // ✨ LIFECYCLE: Execute afterCreate hooks
    lifecycleExecutor.executeAfterCreate(schema, data);

    // Return created entity
    return getById(entityType, id.toString(), auth);
  }

  /**
   * Update entity with optimistic locking
   * 
   * ⚠️ REQUIRES_NEW: Each update gets fresh transaction to see latest DB state
   * This is critical for retry loops that need to read updated version numbers
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Map<String, Object> update(String entityType, String id, long expectedVersion,
      Map<String, Object> data, Authentication auth) {
    EntitySchema schema = registry.getSchemaOrThrow(entityType);

    // Load current entity
    Object entity = findEntityById(schema, id);
    if (entity == null) {
      throw new EntityNotFoundException(entityType, id);
    }

    // Check permission
    if (!policyEngine.check(auth, entityType, "update", entity)) {
      throw new AccessDeniedException("No permission to update this " + entityType);
    }

    // Check version
    Long currentVersion = extractVersion(entity, schema);
    if (currentVersion == null || currentVersion != expectedVersion) {
      throw new VersionMismatchException("Version mismatch",
          currentVersion != null ? currentVersion : 0L, projectEntityToMap(entity,
              policyEngine.projectColumns(auth, entityType, "read"), schema));
    }

    // 🔒 Filter out system/protected fields
    Map<String, Object> safeData = filterSystemFields(data, schema);

    // 🔍 Detect actual changes by comparing with current entity
    Map<String, Object> currentData = projectEntityToMap(entity,
        policyEngine.projectColumns(auth, entityType, "read"), schema);
    Map<String, Object> changedFields = detectChanges(currentData, safeData);

    // ⚡ Skip update if no changes
    if (changedFields.isEmpty()) {
      log.debug("No changes detected for {} with id {}, skipping update", entityType, id);
      return currentData; // Return current data without update
    }

    log.debug("Updating {} fields for {} with id {}: {}", changedFields.size(), entityType, id,
        changedFields.keySet());

    // ✨ LIFECYCLE: Execute beforeUpdate hooks
    lifecycleExecutor.executeBeforeUpdate(schema, changedFields);

    // Build UPDATE with version check (only changed fields)
    String updateSql = buildUpdateSql(schema, id, changedFields, expectedVersion);
    int affected = entityManager.createNativeQuery(updateSql).executeUpdate();

    if (affected == 0) {
      // Version mismatch or entity deleted
      throw new VersionMismatchException("Update failed - version mismatch or entity deleted",
          currentVersion, null);
    }

    // ✨ RELATIONSHIPS: Update M:N relationships
    relationshipResolver.saveRelationships(schema, id, safeData);

    // ✨ LIFECYCLE: Execute afterUpdate hooks
    lifecycleExecutor.executeAfterUpdate(schema, changedFields);

    // Return updated entity
    return getById(entityType, id, auth);
  }

  /**
   * Delete entity
   */
  @Transactional
  public void delete(String entityType, String id, Authentication auth) {
    EntitySchema schema = registry.getSchemaOrThrow(entityType);

    // Load entity
    Object entity = findEntityById(schema, id);
    if (entity == null) {
      throw new EntityNotFoundException(entityType, id);
    }

    // Check permission
    if (!policyEngine.check(auth, entityType, "delete", entity)) {
      throw new AccessDeniedException("No permission to delete this " + entityType);
    }

    // Convert entity to map for lifecycle hooks
    Map<String, Object> entityMap = projectEntityToMap(entity,
        policyEngine.projectColumns(auth, entityType, "read"), schema);

    // ✨ LIFECYCLE: Execute beforeDelete hooks
    lifecycleExecutor.executeBeforeDelete(schema, entityMap);

    // ✨ RELATIONSHIPS: Delete M:N junction records
    relationshipResolver.deleteRelationships(schema, id);

    // Execute DELETE
    String deleteSql = String.format("DELETE FROM %s WHERE %s = '%s'", schema.getTable(),
        schema.getIdField(), id);

    entityManager.createNativeQuery(deleteSql).executeUpdate();
    log.info("Deleted entity: {} id={}", entityType, id);

    // ✨ LIFECYCLE: Execute afterDelete hooks
    lifecycleExecutor.executeAfterDelete(schema, entityMap);
  }

  // Helper methods

  private String sanitize(String value) {
    if (value == null)
      return "";
    // Basic SQL injection prevention
    return value.replace("'", "''").replace(";", "");
  }

  private Object findEntityById(EntitySchema schema, String id) {
    // ✅ Filter out relationship fields (manyToOne, oneToMany, manyToMany)
    // as they are not actual database columns
    List<String> columns = schema.getFields().stream().filter(f -> !isRelationshipField(f))
        .map(FieldSchema::getName).collect(Collectors.toList());

    String columnList = String.join(", ", columns);
    String sql = String.format("SELECT %s FROM %s WHERE %s = :id", columnList, schema.getTable(),
        schema.getIdField());

    try {
      return entityManager.createNativeQuery(sql).setParameter("id", UUID.fromString(id))
          .getSingleResult();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Check if field represents a JPA relationship (not a database column)
   */
  private boolean isRelationshipField(FieldSchema field) {
    String type = field.getType();
    return "manyToOne".equals(type) || "oneToMany".equals(type) || "manyToMany".equals(type);
  }

  private Long extractVersion(Object entity, EntitySchema schema) {
    if (schema.getVersionField() == null)
      return null;

    try {
      if (entity instanceof Map) {
        Object version = ((Map<?, ?>) entity).get(schema.getVersionField());
        return version != null ? ((Number) version).longValue() : null;
      }
    } catch (Exception e) {
      log.warn("Failed to extract version: {}", e.getMessage());
    }

    return null;
  }

  private String getTenantId(Authentication auth) {
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      return jwt.getClaimAsString("tenant_id");
    }
    return "admin";
  }

  /**
   * Map native query result row to Map
   */
  private Map<String, Object> mapRowToMap(Object[] row, List<String> columns) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < columns.size() && i < row.length; i++) {
      result.put(columns.get(i), row[i]);
    }
    return result;
  }

  /**
   * Project entity to Map with allowed columns
   */
  private Map<String, Object> projectEntityToMap(Object entity, Set<String> allowedColumns,
      EntitySchema schema) {
    Map<String, Object> result = new LinkedHashMap<>();

    if (entity instanceof Object[] row) {
      // From native query result
      List<String> allColumns = schema.getFields().stream().map(FieldSchema::getName)
          .collect(Collectors.toList());

      for (int i = 0; i < allColumns.size() && i < row.length; i++) {
        String col = allColumns.get(i);
        if (allowedColumns.isEmpty() || allowedColumns.contains(col)) {
          result.put(col, row[i]);
        }
      }
    } else if (entity instanceof Map<?, ?> map) {
      // From Map
      for (var entry : map.entrySet()) {
        String key = entry.getKey().toString();
        if (allowedColumns.isEmpty() || allowedColumns.contains(key)) {
          result.put(key, entry.getValue());
        }
      }
    }

    return result;
  }

  /**
   * Build INSERT SQL statement
   */
  private String buildInsertSql(EntitySchema schema, Map<String, Object> data) {
    List<String> columns = new ArrayList<>();
    List<String> values = new ArrayList<>();

    for (var entry : data.entrySet()) {
      columns.add(entry.getKey());
      values.add(formatValue(entry.getValue()));
    }

    return String.format("INSERT INTO %s (%s) VALUES (%s)", schema.getTable(),
        String.join(", ", columns), String.join(", ", values));
  }

  /**
   * Build UPDATE SQL statement with version check
   */
  private String buildUpdateSql(EntitySchema schema, String id, Map<String, Object> data,
      long expectedVersion) {
    List<String> sets = new ArrayList<>();

    for (var entry : data.entrySet()) {
      // Skip ID field and version field (version is managed by trigger)
      if (!entry.getKey().equals(schema.getIdField())
          && !entry.getKey().equals(schema.getVersionField())) {
        sets.add(entry.getKey() + " = " + formatValue(entry.getValue()));
      }
    }

    // Increment version via trigger, just check current version
    String sql = String.format("UPDATE %s SET %s WHERE %s = '%s'", schema.getTable(),
        String.join(", ", sets), schema.getIdField(), id);

    if (schema.getVersionField() != null) {
      sql += " AND " + schema.getVersionField() + " = " + expectedVersion;
    }

    return sql;
  }

  /**
   * Format value for SQL statement
   */
  private String formatValue(Object value) {
    if (value == null) {
      return "NULL";
    }

    if (value instanceof String) {
      return "'" + sanitize(value.toString()) + "'";
    }

    if (value instanceof Number || value instanceof Boolean) {
      return value.toString();
    }

    if (value instanceof UUID) {
      return "'" + value.toString() + "'";
    }

    // Default: convert to string
    return "'" + sanitize(value.toString()) + "'";
  }

  /**
   * 🔒 Filter out system/protected fields that should not be updated from
   * external input
   * 
   * System fields include: - ID field (primary key) - Version field (managed by
   * optimistic locking) - Timestamp fields with auto-generation (created_at,
   * updated_at) - Any field marked as 'generated' in schema
   */
  private Map<String, Object> filterSystemFields(Map<String, Object> data, EntitySchema schema) {
    Map<String, Object> filtered = new HashMap<>(data);

    // Remove ID field
    if (schema.getIdField() != null) {
      filtered.remove(schema.getIdField());
    }

    // Remove version field (managed by trigger)
    if (schema.getVersionField() != null) {
      filtered.remove(schema.getVersionField());
    }

    // Remove auto-generated timestamp fields
    filtered.remove("created_at");
    filtered.remove("updated_at");

    // Remove fields marked as 'generated' in schema
    if (schema.getFields() != null) {
      schema.getFields().stream().filter(f -> Boolean.TRUE.equals(f.getGenerated()))
          .forEach(f -> filtered.remove(f.getName()));
    }

    return filtered;
  }

  /**
   * 🔍 Detect actual changes by comparing new data with current entity state
   * 
   * This optimizes updates by only sending changed fields to the database.
   * Benefits: - Reduces database load - Avoids unnecessary trigger executions -
   * Provides clearer audit trail
   * 
   * @return Map containing only fields that have changed
   */
  private Map<String, Object> detectChanges(Map<String, Object> currentData,
      Map<String, Object> newData) {
    Map<String, Object> changes = new HashMap<>();

    for (Map.Entry<String, Object> entry : newData.entrySet()) {
      String key = entry.getKey();
      Object newValue = entry.getValue();
      Object currentValue = currentData.get(key);

      // Check if value has actually changed
      if (!valuesEqual(currentValue, newValue)) {
        changes.put(key, newValue);
      }
    }

    return changes;
  }

  /**
   * Compare two values for equality, handling nulls and different types
   */
  private boolean valuesEqual(Object v1, Object v2) {
    if (v1 == null && v2 == null)
      return true;
    if (v1 == null || v2 == null)
      return false;

    // Handle numbers with different precision (e.g., Integer vs Long)
    if (v1 instanceof Number && v2 instanceof Number) {
      return ((Number) v1).doubleValue() == ((Number) v2).doubleValue();
    }

    // Handle timestamps/dates
    if (v1 instanceof java.time.temporal.Temporal && v2 instanceof java.time.temporal.Temporal) {
      return v1.toString().equals(v2.toString());
    }

    // Standard equality
    return v1.equals(v2);
  }
}
