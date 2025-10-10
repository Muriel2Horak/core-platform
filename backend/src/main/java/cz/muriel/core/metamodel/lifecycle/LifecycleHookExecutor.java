package cz.muriel.core.metamodel.lifecycle;

import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.LifecycleAction;
import cz.muriel.core.metamodel.schema.LifecycleConfig;
import cz.muriel.core.metamodel.util.DeterministicUuidGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Executes lifecycle hooks defined in entity schema
 */
@Component @Slf4j
public class LifecycleHookExecutor {

  /**
   * Execute beforeCreate hooks
   */
  public void executeBeforeCreate(EntitySchema schema, Map<String, Object> entity) {
    if (schema.getLifecycle() == null)
      return;

    List<LifecycleAction> hooks = schema.getLifecycle().getBeforeCreate();
    if (hooks == null)
      return;

    log.debug("Executing {} beforeCreate hooks for {}", hooks.size(), schema.getEntity());
    for (LifecycleAction action : hooks) {
      executeAction(action, schema, entity, "beforeCreate");
    }
  }

  /**
   * Execute afterCreate hooks
   */
  public void executeAfterCreate(EntitySchema schema, Map<String, Object> entity) {
    executeHooks(schema.getLifecycle(),
        schema.getLifecycle() != null ? schema.getLifecycle().getAfterCreate() : null, schema,
        entity, "afterCreate");
  }

  /**
   * Execute beforeUpdate hooks
   */
  public void executeBeforeUpdate(EntitySchema schema, Map<String, Object> entity) {
    executeHooks(schema.getLifecycle(),
        schema.getLifecycle() != null ? schema.getLifecycle().getBeforeUpdate() : null, schema,
        entity, "beforeUpdate");
  }

  /**
   * Execute afterUpdate hooks
   */
  public void executeAfterUpdate(EntitySchema schema, Map<String, Object> entity) {
    executeHooks(schema.getLifecycle(),
        schema.getLifecycle() != null ? schema.getLifecycle().getAfterUpdate() : null, schema,
        entity, "afterUpdate");
  }

  /**
   * Execute beforeDelete hooks
   */
  public void executeBeforeDelete(EntitySchema schema, Map<String, Object> entity) {
    executeHooks(schema.getLifecycle(),
        schema.getLifecycle() != null ? schema.getLifecycle().getBeforeDelete() : null, schema,
        entity, "beforeDelete");
  }

  /**
   * Execute afterDelete hooks
   */
  public void executeAfterDelete(EntitySchema schema, Map<String, Object> entity) {
    executeHooks(schema.getLifecycle(),
        schema.getLifecycle() != null ? schema.getLifecycle().getAfterDelete() : null, schema,
        entity, "afterDelete");
  }

  private void executeHooks(LifecycleConfig config, List<LifecycleAction> hooks,
      EntitySchema schema, Map<String, Object> entity, String phase) {
    if (config == null || hooks == null)
      return;

    log.debug("Executing {} {} hooks for {}", hooks.size(), phase, schema.getEntity());
    for (LifecycleAction action : hooks) {
      executeAction(action, schema, entity, phase);
    }
  }

  /**
   * Execute single lifecycle action
   */
  private void executeAction(LifecycleAction action, EntitySchema schema,
      Map<String, Object> entity, String phase) {

    log.debug("Executing {} action: {} on {}", phase, action.getType(), action.getField());

    switch (action.getType()) {
    case "setField":
      executeSetField(action, entity);
      break;

    case "generateId":
      executeGenerateId(action, schema, entity);
      break;

    case "setTimestamp":
      executeSetTimestamp(action, entity);
      break;

    case "validate":
      executeValidate(action, entity);
      break;

    case "audit":
      executeAudit(action, schema, entity, phase);
      break;

    default:
      log.warn("Unknown lifecycle action type: {}", action.getType());
    }
  }

  /**
   * Set field to static value or expression
   */
  private void executeSetField(LifecycleAction action, Map<String, Object> entity) {
    String field = action.getField();
    String value = action.getValue();

    if (field == null || value == null) {
      log.warn("setField action missing field or value");
      return;
    }

    // Handle special functions
    Object resolvedValue = resolveValue(value, entity);
    entity.put(field, resolvedValue);
    log.debug("Set field '{}' to '{}'", field, resolvedValue);
  }

  /**
   * Generate deterministic ID
   */
  private void executeGenerateId(LifecycleAction action, EntitySchema schema,
      Map<String, Object> entity) {

    if (schema.getIdGeneration() == null) {
      log.warn("generateId action but no idGeneration config in schema");
      return;
    }

    UUID id = DeterministicUuidGenerator.generate(schema.getIdGeneration().getPrefix(),
        schema.getIdGeneration().getSourceFields(), entity,
        schema.getIdGeneration().getAlgorithm());

    String idField = action.getField() != null ? action.getField() : schema.getIdField();
    entity.put(idField, id);
    log.debug("Generated deterministic ID: {}", id);
  }

  /**
   * Set timestamp field
   */
  private void executeSetTimestamp(LifecycleAction action, Map<String, Object> entity) {
    String field = action.getField();
    if (field == null) {
      log.warn("setTimestamp action missing field");
      return;
    }

    entity.put(field, LocalDateTime.now());
    log.debug("Set timestamp field '{}' to now", field);
  }

  /**
   * Execute validation
   */
  private void executeValidate(LifecycleAction action, Map<String, Object> entity) {
    Map<String, Object> params = action.getParams();
    if (params == null || params.isEmpty()) {
      log.debug("Validation action without params, skipping");
      return;
    }

    String field = action.getField();
    Object value = entity.get(field);

    // Check "required" validation
    if (Boolean.TRUE.equals(params.get("required")) && value == null) {
      throw new IllegalArgumentException(
          String.format("Field '%s' is required but was null", field));
    }

    // Check "minLength" validation for strings
    if (value instanceof String str && params.containsKey("minLength")) {
      int minLength = (int) params.get("minLength");
      if (str.length() < minLength) {
        throw new IllegalArgumentException(String.format(
            "Field '%s' must be at least %d characters, got %d", field, minLength, str.length()));
      }
    }

    // Check "maxLength" validation for strings
    if (value instanceof String str && params.containsKey("maxLength")) {
      int maxLength = (int) params.get("maxLength");
      if (str.length() > maxLength) {
        throw new IllegalArgumentException(String.format(
            "Field '%s' must be at most %d characters, got %d", field, maxLength, str.length()));
      }
    }

    // Check "min" validation for numbers
    if (value instanceof Number num && params.containsKey("min")) {
      double min = ((Number) params.get("min")).doubleValue();
      if (num.doubleValue() < min) {
        throw new IllegalArgumentException(
            String.format("Field '%s' must be >= %s, got %s", field, min, num));
      }
    }

    // Check "max" validation for numbers
    if (value instanceof Number num && params.containsKey("max")) {
      double max = ((Number) params.get("max")).doubleValue();
      if (num.doubleValue() > max) {
        throw new IllegalArgumentException(
            String.format("Field '%s' must be <= %s, got %s", field, max, num));
      }
    }

    // Check "pattern" validation (regex)
    if (value instanceof String str && params.containsKey("pattern")) {
      String pattern = (String) params.get("pattern");
      if (!str.matches(pattern)) {
        throw new IllegalArgumentException(
            String.format("Field '%s' does not match pattern: %s", field, pattern));
      }
    }

    log.debug("Validation passed for field '{}'", field);
  }

  /**
   * Audit logging
   */
  private void executeAudit(LifecycleAction action, EntitySchema schema, Map<String, Object> entity,
      String phase) {
    log.info("AUDIT: {} {} on {}, entity: {}", phase, schema.getEntity(),
        entity.get(schema.getIdField()), action.getParams());
  }

  /**
   * Resolve value from expression or function
   */
  private Object resolveValue(String value, Map<String, Object> entity) {
    if (value == null)
      return null;

    // Handle functions
    if (value.equals("now()")) {
      return LocalDateTime.now();
    }
    if (value.equals("uuid()")) {
      return UUID.randomUUID();
    }

    // Handle expressions ${field}
    if (value.startsWith("${") && value.endsWith("}")) {
      String fieldName = value.substring(2, value.length() - 1);
      return entity.get(fieldName);
    }

    // Static value
    return value;
  }
}
