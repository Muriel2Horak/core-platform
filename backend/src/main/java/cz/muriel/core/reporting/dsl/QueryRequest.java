package cz.muriel.core.reporting.dsl;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Query DSL for reporting API.
 * 
 * Represents a declarative query for reading data with dimensions, measures,
 * filters, ordering, and pagination.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryRequest {

  /**
   * Target entity (e.g., "users", "projects", "tasks").
   */
  @NotBlank(message = "Entity must be specified")
  private String entity;

  /**
   * List of dimension fields to group by.
   */
  @NotEmpty(message = "At least one dimension must be specified")
  private List<String> dimensions;

  /**
   * List of measures/aggregations to compute.
   */
  private List<Measure> measures;

  /**
   * Filters to apply.
   */
  private List<Filter> filters;

  /**
   * Ordering specifications.
   */
  private List<OrderBy> orderBy;

  /**
   * Pagination: maximum number of rows to return.
   */
  @Min(value = 1, message = "Limit must be at least 1")
  private Integer limit;

  /**
   * Pagination: number of rows to skip.
   */
  @Min(value = 0, message = "Offset cannot be negative")
  private Integer offset;

  /**
   * Time range filter (required for most queries).
   */
  @Valid
  private TimeRange timeRange;

  /**
   * Additional metadata or query hints.
   */
  private Map<String, Object> metadata;

  /**
   * Represents a measure/aggregation.
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class Measure {
    /**
     * Field name.
     */
    @NotBlank
    private String field;

    /**
     * Aggregation function (e.g., count, sum, avg, min, max).
     */
    @NotBlank
    private String aggregation;

    /**
     * Alias for the measure in results.
     */
    private String alias;
  }

  /**
   * Represents a filter condition.
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class Filter {
    /**
     * Field name.
     */
    @NotBlank
    private String field;

    /**
     * Operator (e.g., eq, ne, gt, lt, in, contains).
     */
    @NotBlank
    private String operator;

    /**
     * Value(s) to compare against.
     */
    @NotNull
    private Object value;
  }

  /**
   * Represents ordering specification.
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class OrderBy {
    /**
     * Field name or measure alias.
     */
    @NotBlank
    private String field;

    /**
     * Direction: asc or desc.
     */
    @NotBlank
    private String direction;
  }

  /**
   * Represents a time range.
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class TimeRange {
    /**
     * Start of time range (inclusive).
     */
    @NotNull(message = "Time range start is required")
    private Instant start;

    /**
     * End of time range (inclusive).
     */
    @NotNull(message = "Time range end is required")
    private Instant end;

    /**
     * Time dimension field name (default: "created_at").
     */
    private String timeDimension;
  }
}
