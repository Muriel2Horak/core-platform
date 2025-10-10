package cz.muriel.core.reporting.cube;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Cube.js query format.
 * 
 * Represents the JSON structure expected by Cube.js API.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL)
public class CubeQueryRequest {

  /**
   * List of measures (aggregations).
   */
  private List<String> measures;

  /**
   * List of dimensions (groupBy fields).
   */
  private List<String> dimensions;

  /**
   * Time dimensions with date ranges.
   */
  private List<TimeDimension> timeDimensions;

  /**
   * Filters.
   */
  private List<Filter> filters;

  /**
   * Ordering.
   */
  private List<List<String>> order;

  /**
   * Limit.
   */
  private Integer limit;

  /**
   * Offset.
   */
  private Integer offset;

  /**
   * Security context (tenant, roles).
   */
  private Map<String, Object> securityContext;

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class TimeDimension {
    private String dimension;
    private List<String> dateRange;
    private String granularity;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class Filter {
    private String member;
    private String operator;
    private List<Object> values;
  }
}
