package cz.muriel.core.reporting.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Specification for an entity from metamodel perspective.
 * 
 * Defines allowed dimensions, measures, filters for reporting. Extended for
 * full UI spec generation (PHASE 2).
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EntitySpec {

  /**
   * Entity name.
   */
  private String entityName;

  /**
   * Table name in database (e.g., "users_directory").
   */
  private String tableName;

  /**
   * Metamodel version checksum.
   */
  private String specVersion;

  /**
   * Allowed dimension fields.
   */
  private Set<String> allowedDimensions;

  /**
   * Allowed measure fields.
   */
  private Set<String> allowedMeasures;

  /**
   * Allowed filter fields.
   */
  private Set<String> allowedFilters;

  /**
   * Allowed aggregation functions.
   */
  private Set<String> allowedAggregations;

  /**
   * Field definitions.
   */
  private List<FieldSpec> fields;

  /**
   * Whether time range is required.
   */
  private boolean requiresTimeRange;

  /**
   * Default time dimension field.
   */
  private String defaultTimeDimension;

  /**
   * Editable fields (for inline edit).
   */
  private Set<String> editableFields;

  /**
   * Relations to other entities (for drill-down/joins).
   */
  private List<RelationSpec> relations;

  /**
   * Field validations (required, pattern, min/max).
   */
  private Map<String, ValidationSpec> validations;

  /**
   * Enum value definitions.
   */
  private Map<String, List<String>> enums;

  /**
   * Default view configuration (columns, sort, filters).
   */
  private DefaultViewSpec defaultView;

  /**
   * Drill-down definitions.
   */
  private List<DrilldownSpec> drilldowns;

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class FieldSpec {
    private String name;
    private String type;
    private boolean editable;
    private boolean filterable;
    private boolean sortable;
    private List<String> allowedOperators;
    private String label;
    private boolean required;
    private boolean sensitive;
    private boolean adminOnly;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class RelationSpec {
    private String name;
    private String targetEntity;
    private String relationType; // "manyToOne", "oneToMany", "manyToMany"
    private String foreignKey;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class ValidationSpec {
    private boolean required;
    private String pattern;
    private Integer minLength;
    private Integer maxLength;
    private Number min;
    private Number max;
    private String errorMessage;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class DefaultViewSpec {
    private List<String> columns;
    private String sortBy;
    private String sortOrder; // "asc" or "desc"
    private Map<String, Object> defaultFilters;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class DrilldownSpec {
    private String name;
    private String targetEntity;
    private Map<String, String> fieldMapping; // source field -> target field
  }
}
