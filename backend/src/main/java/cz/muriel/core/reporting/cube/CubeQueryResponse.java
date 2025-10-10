package cz.muriel.core.reporting.cube;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Cube.js API response.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL)
public class CubeQueryResponse {

  /**
   * Query results.
   */
  private List<Map<String, Object>> data;

  /**
   * Query metadata.
   */
  private Map<String, Object> query;

  /**
   * Last refresh time.
   */
  private String lastRefreshTime;

  /**
   * Annotation (for errors).
   */
  private Map<String, Object> annotation;

  /**
   * Slow query flag.
   */
  private Boolean slowQuery;
}
