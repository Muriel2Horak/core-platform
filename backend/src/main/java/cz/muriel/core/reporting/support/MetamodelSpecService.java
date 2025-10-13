package cz.muriel.core.reporting.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.FieldSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for retrieving entity specifications from metamodel.
 * 
 * Provides allowed fields, aggregations, and restrictions for reporting
 * queries.
 * 
 * PHASE 2: Extended with full UI spec generation (relations, validations,
 * enums, etc.)
 */
@Slf4j @Service @RequiredArgsConstructor
public class MetamodelSpecService {

  private final MetamodelRegistry metamodelRegistry;
  private final ObjectMapper objectMapper;

  /**
   * Get full entity specification for UI rendering (PHASE 2).
   * 
   * Includes: - Dimensions, measures, filters - Editable fields - Relations (for
   * drill-down) - Validations - Enums - Default view config - Drilldowns
   * 
   * @param entityName Entity name
   * @return Complete EntitySpec with checksum versioning
   */
  public EntitySpec getFullEntitySpec(String entityName) {
    EntitySchema entitySchema = metamodelRegistry.getSchemaOrThrow(entityName);

    Set<String> allowedDimensions = new HashSet<>();
    Set<String> allowedMeasures = new HashSet<>();
    Set<String> allowedFilters = new HashSet<>();
    Set<String> editableFields = new HashSet<>();
    List<EntitySpec.FieldSpec> fieldSpecs = new ArrayList<>();
    List<EntitySpec.RelationSpec> relations = new ArrayList<>();
    Map<String, EntitySpec.ValidationSpec> validations = new HashMap<>();
    Map<String, List<String>> enums = new HashMap<>();

    // Extract fields
    for (FieldSchema field : entitySchema.getFields()) {
      String fieldName = field.getName();

      // Dimensions
      if (isDimensionField(field)) {
        allowedDimensions.add(fieldName);
      }

      // Measures
      if (isMeasureField(field)) {
        allowedMeasures.add(fieldName);
      }

      // Filters
      if (isFilterableField(field)) {
        allowedFilters.add(fieldName);
      }

      // Editable
      if (isEditableField(field)) {
        editableFields.add(fieldName);
      }

      // Build field spec
      EntitySpec.FieldSpec fieldSpec = EntitySpec.FieldSpec.builder().name(fieldName)
          .type(field.getType()).editable(isEditableField(field))
          .filterable(isFilterableField(field)).sortable(isSortableField(field))
          .allowedOperators(getAllowedOperators(field)).label(formatLabel(fieldName))
          .required(Boolean.TRUE.equals(field.getRequired())).sensitive(isSensitiveField(field))
          .adminOnly(isAdminOnlyField(fieldName)) // Check if field requires admin privileges
          .build();

      fieldSpecs.add(fieldSpec);

      // Extract validations
      if (Boolean.TRUE.equals(field.getRequired()) || field.getMaxLength() != null) {
        EntitySpec.ValidationSpec validation = EntitySpec.ValidationSpec.builder()
            .required(Boolean.TRUE.equals(field.getRequired())).maxLength(field.getMaxLength())
            .errorMessage(buildValidationMessage(field)).build();
        validations.put(fieldName, validation);
      }
    }

    // Extract relations from reference fields
    for (FieldSchema field : entitySchema.getFields()) {
      if (field.getType().equals("ref") || field.getType().equals("manyToOne")) {
        EntitySpec.RelationSpec relationSpec = EntitySpec.RelationSpec.builder()
            .name(field.getName()).targetEntity(field.getRefEntity()).relationType("manyToOne")
            .foreignKey(field.getName()).build();
        relations.add(relationSpec);
      } else if (field.getType().equals("oneToMany") || field.getType().equals("manyToMany")) {
        EntitySpec.RelationSpec relationSpec = EntitySpec.RelationSpec.builder()
            .name(field.getName()).targetEntity(field.getTargetEntity())
            .relationType(field.getType()).foreignKey(field.getJoinColumn()).build();
        relations.add(relationSpec);
      }
    }

    // Build default view
    EntitySpec.DefaultViewSpec defaultView = buildDefaultView(entitySchema, fieldSpecs);

    // Build drilldowns
    List<EntitySpec.DrilldownSpec> drilldowns = buildDrilldowns(entitySchema, relations);

    // Compute checksum version
    String specVersion = computeChecksum(entitySchema);

    String timeDimension = findTimeDimension(entitySchema);
    boolean requiresTimeRange = timeDimension != null;

    // Derive table name from entity name (convert to snake_case)
    String tableName = toSnakeCase(entityName);

    return EntitySpec.builder().entityName(entityName).tableName(tableName).specVersion(specVersion)
        .allowedDimensions(allowedDimensions).allowedMeasures(allowedMeasures)
        .allowedFilters(allowedFilters).allowedAggregations(getAllowedAggregations())
        .fields(fieldSpecs).requiresTimeRange(requiresTimeRange).defaultTimeDimension(timeDimension)
        .editableFields(editableFields).relations(relations).validations(validations).enums(enums)
        .defaultView(defaultView).drilldowns(drilldowns).build();
  }

  /**
   * Get basic entity specification for reporting (legacy method).
   * 
   * @param entityName Entity name
   * @return EntitySpec with allowed fields and operations
   */
  public EntitySpec getEntitySpec(String entityName) {
    return getFullEntitySpec(entityName);
  }

  /**
   * Validate query against entity spec.
   */
  public void validateQuery(String entityName, List<String> dimensions, List<String> measures,
      List<String> filters) {
    EntitySpec spec = getEntitySpec(entityName);

    // Validate dimensions
    if (dimensions != null) {
      for (String dim : dimensions) {
        if (!spec.getAllowedDimensions().contains(dim)) {
          throw new IllegalArgumentException(
              "Invalid dimension '" + dim + "' for entity '" + entityName + "'");
        }
      }
    }

    // Validate measures
    if (measures != null) {
      for (String measure : measures) {
        if (!spec.getAllowedMeasures().contains(measure)) {
          throw new IllegalArgumentException(
              "Invalid measure '" + measure + "' for entity '" + entityName + "'");
        }
      }
    }

    // Validate filters
    if (filters != null) {
      for (String filter : filters) {
        if (!spec.getAllowedFilters().contains(filter)) {
          throw new IllegalArgumentException(
              "Invalid filter field '" + filter + "' for entity '" + entityName + "'");
        }
      }
    }
  }

  private boolean isEditableField(FieldSchema field) {
    // Primary keys and generated fields are not editable
    // Also exclude 'version' field (JPA @Version for optimistic locking)
    return !Boolean.TRUE.equals(field.getPk()) && !Boolean.TRUE.equals(field.getGenerated())
        && !"version".equalsIgnoreCase(field.getName());
  }

  private boolean isDimensionField(FieldSchema field) {
    String typeName = field.getType();
    return typeName.equals("string") || typeName.equals("uuid") || typeName.equals("enum")
        || typeName.equals("boolean") || typeName.equals("email") || field.getName().endsWith("_id")
        || field.getName().equals("id");
  }

  private boolean isMeasureField(FieldSchema field) {
    String typeName = field.getType();
    return typeName.equals("integer") || typeName.equals("long") || typeName.equals("double")
        || typeName.equals("decimal");
  }

  private boolean isFilterableField(FieldSchema field) {
    // Exclude sensitive fields
    String name = field.getName().toLowerCase();
    if (name.contains("password") || name.contains("secret") || name.contains("token")) {
      return false;
    }
    return true;
  }

  private boolean isSortableField(FieldSchema field) {
    // Most fields are sortable except TEXT/JSON/BLOB
    String typeName = field.getType();
    return !typeName.equals("text") && !typeName.equals("json") && !typeName.equals("jsonb")
        && !typeName.equals("blob");
  }

  private List<String> getAllowedOperators(FieldSchema field) {
    String type = field.getType();
    switch (type) {
    case "string":
    case "text":
    case "email":
      return Arrays.asList("eq", "ne", "contains", "startsWith", "endsWith", "in", "notIn");
    case "integer":
    case "long":
    case "double":
    case "decimal":
    case "date":
    case "timestamp":
      return Arrays.asList("eq", "ne", "gt", "gte", "lt", "lte", "in", "notIn", "between");
    case "boolean":
      return Arrays.asList("eq", "ne");
    case "uuid":
      return Arrays.asList("eq", "ne", "in", "notIn");
    case "enum":
      return Arrays.asList("eq", "ne", "in", "notIn");
    default:
      return Arrays.asList("eq", "ne");
    }
  }

  private String findTimeDimension(EntitySchema entitySchema) {
    // Look for created_at, updated_at, or timestamp fields
    for (FieldSchema field : entitySchema.getFields()) {
      String name = field.getName().toLowerCase();
      if (name.equals("created_at") || name.equals("createdat")) {
        return field.getName();
      }
    }
    for (FieldSchema field : entitySchema.getFields()) {
      String name = field.getName().toLowerCase();
      if (name.equals("updated_at") || name.equals("updatedat")) {
        return field.getName();
      }
    }
    for (FieldSchema field : entitySchema.getFields()) {
      if (field.getType().equals("timestamp")) {
        return field.getName();
      }
    }
    return null;
  }

  private Set<String> getAllowedAggregations() {
    return Set.of("count", "sum", "avg", "min", "max", "countDistinct");
  }

  /**
   * Format field name as human-readable label. Examples: "user_id" -> "User ID",
   * "firstName" -> "First Name"
   */
  private String formatLabel(String fieldName) {
    // Replace underscores with spaces
    String label = fieldName.replace("_", " ");

    // Split camelCase
    label = label.replaceAll("([a-z])([A-Z])", "$1 $2");

    // Capitalize first letter of each word
    String[] words = label.split(" ");
    StringBuilder result = new StringBuilder();
    for (String word : words) {
      if (!word.isEmpty()) {
        result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase())
            .append(" ");
      }
    }
    return result.toString().trim();
  }

  /**
   * Check if field contains sensitive data (password, token, secret).
   */
  private boolean isSensitiveField(FieldSchema field) {
    String name = field.getName().toLowerCase();
    return name.contains("password") || name.contains("secret") || name.contains("token")
        || name.contains("credential");
  }

  /**
   * Build validation error message for field.
   */
  private String buildValidationMessage(FieldSchema field) {
    if (Boolean.TRUE.equals(field.getRequired())) {
      return formatLabel(field.getName()) + " is required";
    }
    if (field.getMaxLength() != null) {
      return formatLabel(field.getName()) + " must be less than " + field.getMaxLength()
          + " characters";
    }
    return null;
  }

  /**
   * Build default view configuration.
   */
  private EntitySpec.DefaultViewSpec buildDefaultView(EntitySchema entitySchema,
      List<EntitySpec.FieldSpec> fieldSpecs) {

    // Default columns: first 5 non-sensitive, non-technical fields
    List<String> defaultColumns = fieldSpecs.stream().filter(f -> !f.isSensitive())
        .filter(f -> !f.getName().endsWith("_id")).filter(f -> !f.getName().equals("version"))
        .limit(5).map(EntitySpec.FieldSpec::getName).collect(Collectors.toList());

    // Default sort by created_at desc
    String sortBy = findTimeDimension(entitySchema);
    if (sortBy == null) {
      sortBy = "id";
    }

    return EntitySpec.DefaultViewSpec.builder().columns(defaultColumns).sortBy(sortBy)
        .sortOrder("desc").defaultFilters(new HashMap<>()).build();
  }

  /**
   * Build drilldown definitions based on relations.
   */
  private List<EntitySpec.DrilldownSpec> buildDrilldowns(EntitySchema entitySchema,
      List<EntitySpec.RelationSpec> relations) {

    List<EntitySpec.DrilldownSpec> drilldowns = new ArrayList<>();

    // Create drilldown for each manyToOne relation
    for (EntitySpec.RelationSpec relation : relations) {
      if ("manyToOne".equals(relation.getRelationType())) {
        Map<String, String> fieldMapping = new HashMap<>();
        fieldMapping.put(relation.getForeignKey(), "id");

        EntitySpec.DrilldownSpec drilldown = EntitySpec.DrilldownSpec.builder()
            .name("View " + formatLabel(relation.getTargetEntity()))
            .targetEntity(relation.getTargetEntity()).fieldMapping(fieldMapping).build();

        drilldowns.add(drilldown);
      }
    }

    return drilldowns;
  }

  /**
   * Compute SHA-256 checksum of entity schema for versioning.
   */
  private String computeChecksum(EntitySchema entitySchema) {
    try {
      // Serialize schema to JSON string
      String json = objectMapper.writeValueAsString(entitySchema);

      // Compute SHA-256 hash
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));

      // Convert to hex string (first 16 chars)
      StringBuilder hexString = new StringBuilder();
      for (int i = 0; i < Math.min(hash.length, 8); i++) {
        String hex = Integer.toHexString(0xff & hash[i]);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      log.warn("Failed to compute schema checksum: {}", e.getMessage());
      return "v1.0"; // Fallback
    }
  }

  /**
   * Check if field should be restricted to admin users only. Fields like
   * tenant_id, version, created_at, updated_at are typically admin-only.
   */
  private boolean isAdminOnlyField(String fieldName) {
    String normalized = fieldName.toLowerCase();
    return normalized.equals("tenant_id") || normalized.equals("version")
        || normalized.equals("created_at") || normalized.equals("updated_at")
        || normalized.equals("createdat") || normalized.equals("updatedat");
  }

  /**
   * Convert CamelCase or PascalCase to snake_case.
   * 
   * Examples: - "User" -> "users" - "UserDirectory" -> "user_directory" -
   * "TenantRegistry" -> "tenants_registry"
   */
  private String toSnakeCase(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }

    // Insert underscore before uppercase letters (except first)
    String snakeCase = input.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();

    // Pluralize if needed (simple heuristic: add 's' if not already plural)
    // This is a simplification - production code might use a proper pluralization
    // library
    if (!snakeCase.endsWith("s")) {
      snakeCase = snakeCase + "s";
    }

    return snakeCase;
  }
}
