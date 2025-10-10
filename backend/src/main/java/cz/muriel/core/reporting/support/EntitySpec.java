package cz.muriel.core.reporting.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Specification for an entity from metamodel perspective.
 * 
 * Defines allowed dimensions, measures, filters for reporting.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EntitySpec {

  /**
   * Entity name.
   */
  private String entityName;

  /**
   * Metamodel version.
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

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class FieldSpec {
    private String name;
    private String type;
    private boolean editable;
    private boolean filterable;
    private boolean sortable;
    private List<String> allowedOperators;
  }
}
