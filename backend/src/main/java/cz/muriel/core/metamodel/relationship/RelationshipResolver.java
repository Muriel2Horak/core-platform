package cz.muriel.core.metamodel.relationship;

import cz.muriel.core.metamodel.schema.FieldSchema;
import cz.muriel.core.metamodel.schema.EntitySchema;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Resolves Many-to-Many and other relationships for metamodel entities
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RelationshipResolver {

  private final EntityManager entityManager;

  /**
   * Load M:N relationships for an entity
   *
   * @param schema Entity schema
   * @param entity Entity data map
   * @param relationships List of relationship fields to load
   * @return Entity with populated relationship fields
   */
  public Map<String, Object> loadRelationships(EntitySchema schema, Map<String, Object> entity,
      List<String> relationships) {

    if (relationships == null || relationships.isEmpty()) {
      return entity;
    }

    Object entityId = entity.get(schema.getIdField());
    if (entityId == null) {
      log.warn("Cannot load relationships for entity without ID");
      return entity;
    }

    for (String relationshipName : relationships) {
      FieldSchema field = findField(schema, relationshipName);
      if (field == null) {
        log.warn("Relationship field '{}' not found in schema", relationshipName);
        continue;
      }

      if ("manyToMany".equals(field.getType())) {
        loadManyToMany(entity, entityId, field);
      } else if ("oneToMany".equals(field.getType())) {
        loadOneToMany(entity, entityId, field);
      } else if ("manyToOne".equals(field.getType()) || "ref".equals(field.getType())) {
        loadManyToOne(entity, field);
      }
    }

    return entity;
  }

  /**
   * Load M:N relationship via junction table
   */
  private void loadManyToMany(Map<String, Object> entity, Object entityId, FieldSchema field) {
    String joinTable = field.getJoinTable();
    String joinColumn = field.getJoinColumn();
    String inverseJoinColumn = field.getInverseJoinColumn();
    String targetEntity = field.getTargetEntity();

    if (joinTable == null || joinColumn == null || inverseJoinColumn == null) {
      log.warn("M:N field '{}' missing junction table configuration", field.getName());
      return;
    }

    // Query: SELECT target_id FROM junction_table WHERE entity_id = ?
    String sql = String.format(
        "SELECT %s FROM %s WHERE %s = '%s'",
        inverseJoinColumn, joinTable, joinColumn, entityId);

    log.debug("Loading M:N relationship '{}': {}", field.getName(), sql);

    @SuppressWarnings("unchecked")
    List<Object> targetIds = entityManager.createNativeQuery(sql).getResultList();

    entity.put(field.getName(), targetIds);
    log.debug("Loaded {} related entities for '{}'", targetIds.size(), field.getName());
  }

  /**
   * Load 1:N relationship
   */
  private void loadOneToMany(Map<String, Object> entity, Object entityId, FieldSchema field) {
    String targetEntity = field.getTargetEntity();
    String refField = field.getRefField(); // Foreign key field in target entity

    if (refField == null) {
      log.warn("1:N field '{}' missing refField", field.getName());
      return;
    }

    // TODO: Need target table name - requires registry lookup
    log.debug("1:N relationship '{}' - placeholder", field.getName());
    entity.put(field.getName(), Collections.emptyList());
  }

  /**
   * Load N:1 relationship (single reference)
   */
  private void loadManyToOne(Map<String, Object> entity, FieldSchema field) {
    Object foreignKeyValue = entity.get(field.getName());
    if (foreignKeyValue == null) {
      return;
    }

    // For now, just keep the ID - could fetch full entity
    log.debug("N:1 relationship '{}' = {}", field.getName(), foreignKeyValue);
  }

  /**
   * Save M:N relationships (manage junction table)
   */
  public void saveRelationships(EntitySchema schema, Object entityId, Map<String, Object> data) {
    for (FieldSchema field : schema.getFields()) {
      if ("manyToMany".equals(field.getType())) {
        saveManyToMany(entityId, field, data.get(field.getName()));
      }
    }
  }

  /**
   * Save M:N relationship - clear old and insert new junction records
   */
  private void saveManyToMany(Object entityId, FieldSchema field, Object relationshipData) {
    String joinTable = field.getJoinTable();
    String joinColumn = field.getJoinColumn();
    String inverseJoinColumn = field.getInverseJoinColumn();

    if (joinTable == null || joinColumn == null || inverseJoinColumn == null) {
      return;
    }

    if (relationshipData == null) {
      // Clear all relationships
      String deleteSql = String.format(
          "DELETE FROM %s WHERE %s = '%s'",
          joinTable, joinColumn, entityId);
      entityManager.createNativeQuery(deleteSql).executeUpdate();
      log.debug("Cleared M:N relationships for '{}'", field.getName());
      return;
    }

    // Clear existing relationships
    String deleteSql = String.format(
        "DELETE FROM %s WHERE %s = '%s'",
        joinTable, joinColumn, entityId);
    entityManager.createNativeQuery(deleteSql).executeUpdate();

    // Insert new relationships
    if (relationshipData instanceof Collection<?>) {
      Collection<?> targetIds = (Collection<?>) relationshipData;
      for (Object targetId : targetIds) {
        String insertSql = String.format(
            "INSERT INTO %s (%s, %s) VALUES ('%s', '%s')",
            joinTable, joinColumn, inverseJoinColumn, entityId, targetId);
        entityManager.createNativeQuery(insertSql).executeUpdate();
      }
      log.debug("Saved {} M:N relationships for '{}'", targetIds.size(), field.getName());
    }
  }

  /**
   * Delete M:N relationships (cascade delete from junction table)
   */
  public void deleteRelationships(EntitySchema schema, Object entityId) {
    for (FieldSchema field : schema.getFields()) {
      if ("manyToMany".equals(field.getType())) {
        deleteManyToMany(entityId, field);
      }
    }
  }

  /**
   * Delete all junction records for entity
   */
  private void deleteManyToMany(Object entityId, FieldSchema field) {
    String joinTable = field.getJoinTable();
    String joinColumn = field.getJoinColumn();

    if (joinTable == null || joinColumn == null) {
      return;
    }

    String deleteSql = String.format(
        "DELETE FROM %s WHERE %s = '%s'",
        joinTable, joinColumn, entityId);

    int deleted = entityManager.createNativeQuery(deleteSql).executeUpdate();
    log.debug("Deleted {} M:N junction records for '{}'", deleted, field.getName());
  }

  private FieldSchema findField(EntitySchema schema, String fieldName) {
    return schema.getFields().stream()
        .filter(f -> f.getName().equals(fieldName))
        .findFirst()
        .orElse(null);
  }
}
