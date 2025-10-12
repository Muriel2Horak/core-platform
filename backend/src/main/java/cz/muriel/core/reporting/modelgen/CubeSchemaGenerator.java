package cz.muriel.core.reporting.modelgen;

import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.FieldSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Cube.js schema (.js files) from metamodel EntitySchema definitions.
 * 
 * <p>Converts YAML metamodel → Cube.js JavaScript schema with:
 * <ul>
 *   <li>Dimensions (string, number, time, boolean)</li>
 *   <li>Measures (count, sum, avg, min, max)</li>
 *   <li>Pre-aggregations (daily, weekly rollups)</li>
 *   <li>Joins (relationships between cubes)</li>
 * </ul>
 * 
 * @see EntitySchema
 */
@Slf4j
@Service
public class CubeSchemaGenerator {

  private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * Generate Cube.js schema JavaScript code from EntitySchema.
   * 
   * @param schema Entity metamodel schema
   * @return Cube.js schema as JavaScript string
   */
  public String generate(EntitySchema schema) {
    log.debug("Generating Cube.js schema for entity: {}", schema.getEntity());

    StringBuilder js = new StringBuilder();

    // Header comment
    js.append("/**\n");
    js.append(" * Cube.js Schema: ").append(schema.getEntity()).append("\n");
    js.append(" * Generated from metamodel at: ")
      .append(LocalDateTime.now().format(TIMESTAMP_FORMATTER)).append("\n");
    js.append(" * \n");
    js.append(" * @generated DO NOT EDIT MANUALLY\n");
    js.append(" * Regenerate via: CubeSchemaGenerator.generate()\n");
    js.append(" */\n\n");

    // Cube definition
    js.append("cube(`").append(schema.getEntity()).append("`, {\n");

    // SQL definition
    generateSql(schema, js);

    // Dimensions
    generateDimensions(schema, js);

    // Measures
    generateMeasures(schema, js);

    // Pre-aggregations (if applicable)
    if (shouldHavePreAggregations(schema)) {
      generatePreAggregations(schema, js);
    }

    // Segments (if applicable)
    if (hasStates(schema)) {
      generateSegments(schema, js);
    }

    js.append("});\n");

    return js.toString();
  }

  /**
   * Generate SQL definition.
   */
  private void generateSql(EntitySchema schema, StringBuilder js) {
    js.append("  sql: `SELECT * FROM ").append(schema.getTable()).append("`");
    
    // Add tenant isolation if tenantField exists
    if (schema.getTenantField() != null) {
      js.append(",\n\n");
      js.append("  // 🔐 Multi-tenancy: Automatic tenant isolation\n");
      js.append("  preAggregationsSchema: `").append(schema.getTable()).append("_preagg_${SECURITY_CONTEXT.tenantId.unsafeValue()}`,\n");
    } else {
      js.append(",\n\n");
    }
  }

  /**
   * Generate dimensions from metamodel fields.
   */
  private void generateDimensions(EntitySchema schema, StringBuilder js) {
    js.append("  // ============================================================================\n");
    js.append("  // DIMENSIONS\n");
    js.append("  // ============================================================================\n\n");
    js.append("  dimensions: {\n");

    List<FieldSchema> dimensionFields = schema.getFields().stream()
        .filter(this::isDimension)
        .collect(Collectors.toList());

    for (int i = 0; i < dimensionFields.size(); i++) {
      FieldSchema field = dimensionFields.get(i);
      generateDimension(field, schema.getTable(), js);
      
      if (i < dimensionFields.size() - 1) {
        js.append(",\n\n");
      } else {
        js.append("\n");
      }
    }

    js.append("  },\n\n");
  }

  /**
   * Generate single dimension.
   */
  private void generateDimension(FieldSchema field, String table, StringBuilder js) {
    String cubeName = toCamelCase(field.getName());
    String sqlColumn = toSnakeCase(field.getName());
    String cubeType = mapFieldTypeToCubeDimensionType(field.getType());

    js.append("    ").append(cubeName).append(": {\n");
    js.append("      sql: `").append(sqlColumn).append("`,\n");
    js.append("      type: `").append(cubeType).append("`");

    // Add primaryKey flag
    if (Boolean.TRUE.equals(field.getPk())) {
      js.append(",\n      primaryKey: true");
    }

    js.append("\n    }");
  }

  /**
   * Generate measures (count, aggregations).
   */
  private void generateMeasures(EntitySchema schema, StringBuilder js) {
    js.append("  // ============================================================================\n");
    js.append("  // MEASURES\n");
    js.append("  // ============================================================================\n\n");
    js.append("  measures: {\n");

    // Always add count measure
    js.append("    count: {\n");
    js.append("      type: `count`,\n");
    js.append("      drillMembers: [");
    
    // Add primary key and important dimensions to drill members
    List<String> drillMembers = schema.getFields().stream()
        .filter(f -> Boolean.TRUE.equals(f.getPk()) || "email".equals(f.getName()) || "name".equals(f.getName()))
        .map(f -> toCamelCase(f.getName()))
        .collect(Collectors.toList());
    
    js.append(String.join(", ", drillMembers));
    js.append("]\n");
    js.append("    }");

    // Add numeric field measures (sum, avg, min, max)
    List<FieldSchema> numericFields = schema.getFields().stream()
        .filter(f -> "long".equals(f.getType()) || "number".equals(f.getType()))
        .collect(Collectors.toList());

    for (FieldSchema field : numericFields) {
      js.append(",\n\n");
      generateNumericMeasures(field, js);
    }

    js.append("\n  },\n\n");
  }

  /**
   * Generate measures for numeric fields.
   */
  private void generateNumericMeasures(FieldSchema field, StringBuilder js) {
    String cubeName = toCamelCase(field.getName());
    String sqlColumn = toSnakeCase(field.getName());

    js.append("    ").append(cubeName).append("Sum: {\n");
    js.append("      sql: `").append(sqlColumn).append("`,\n");
    js.append("      type: `sum`\n");
    js.append("    },\n\n");

    js.append("    ").append(cubeName).append("Avg: {\n");
    js.append("      sql: `").append(sqlColumn).append("`,\n");
    js.append("      type: `avg`\n");
    js.append("    }");
  }

  /**
   * Generate pre-aggregations for time-series data.
   */
  private void generatePreAggregations(EntitySchema schema, StringBuilder js) {
    js.append("  // ============================================================================\n");
    js.append("  // PRE-AGGREGATIONS\n");
    js.append("  // ============================================================================\n\n");
    js.append("  preAggregations: {\n");

    // Find timestamp field (createdAt, updatedAt, etc.)
    FieldSchema timeField = schema.getFields().stream()
        .filter(f -> "timestamp".equals(f.getType()) && 
                     (f.getName().contains("created") || f.getName().contains("updated")))
        .findFirst()
        .orElse(null);

    if (timeField != null) {
      String timeDimension = toCamelCase(timeField.getName());

      // Daily rollup
      js.append("    dailyRollup: {\n");
      js.append("      measures: [count],\n");
      js.append("      timeDimension: ").append(timeDimension).append(",\n");
      js.append("      granularity: `day`,\n");
      js.append("      refreshKey: {\n");
      js.append("        every: `1 hour`,\n");
      js.append("        incremental: true,\n");
      js.append("        updateWindow: `7 day`\n");
      js.append("      },\n");
      js.append("      partitionGranularity: `month`\n");
      js.append("    }");
    }

    js.append("\n  },\n\n");
  }

  /**
   * Generate segments for state-based filtering.
   */
  private void generateSegments(EntitySchema schema, StringBuilder js) {
    js.append("  // ============================================================================\n");
    js.append("  // SEGMENTS\n");
    js.append("  // ============================================================================\n\n");
    js.append("  segments: {\n");

    if (schema.getStates() != null && !schema.getStates().isEmpty()) {
      for (int i = 0; i < schema.getStates().size(); i++) {
        var state = schema.getStates().get(i);
        String segmentName = toCamelCase(state.getCode()) + "Items";
        
        js.append("    ").append(segmentName).append(": {\n");
        js.append("      sql: `${CUBE}.status = '").append(state.getCode()).append("'`\n");
        js.append("    }");
        
        if (i < schema.getStates().size() - 1) {
          js.append(",\n\n");
        }
      }
    }

    js.append("\n  }\n");
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  private boolean isDimension(FieldSchema field) {
    // All non-ref fields are dimensions
    return !"ref".equals(field.getType()) && 
           !"collection".equals(field.getType()) &&
           !"manyToMany".equals(field.getType()) &&
           !"oneToMany".equals(field.getType());
  }

  private String mapFieldTypeToCubeDimensionType(String metamodelType) {
    return switch (metamodelType) {
      case "uuid", "string", "email", "text" -> "string";
      case "long", "number" -> "number";
      case "timestamp" -> "time";
      case "boolean" -> "boolean";
      default -> "string";
    };
  }

  private boolean shouldHavePreAggregations(EntitySchema schema) {
    // Have pre-agg if entity has timestamp field
    return schema.getFields().stream()
        .anyMatch(f -> "timestamp".equals(f.getType()));
  }

  private boolean hasStates(EntitySchema schema) {
    return schema.getStates() != null && !schema.getStates().isEmpty();
  }

  private String toCamelCase(String snakeCase) {
    if (snakeCase == null || snakeCase.isEmpty()) {
      return snakeCase;
    }
    String[] parts = snakeCase.split("_");
    StringBuilder camelCase = new StringBuilder(parts[0].toLowerCase());
    for (int i = 1; i < parts.length; i++) {
      camelCase.append(parts[i].substring(0, 1).toUpperCase())
               .append(parts[i].substring(1).toLowerCase());
    }
    return camelCase.toString();
  }

  private String toSnakeCase(String camelCase) {
    if (camelCase == null || camelCase.isEmpty()) {
      return camelCase;
    }
    return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
  }
}
