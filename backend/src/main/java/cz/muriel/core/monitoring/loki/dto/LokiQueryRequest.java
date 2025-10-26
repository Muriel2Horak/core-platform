package cz.muriel.core.monitoring.loki.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Request for Loki query_range API
 * 
 * @see <a href=
 * "https://grafana.com/docs/loki/latest/reference/loki-http-api/#query-logs-within-a-range-of-time">Loki
 * API Docs</a>
 */
@Data @Builder
public class LokiQueryRequest {

  /**
   * LogQL query expression Example: {tenant="admin",service="backend"} |= "ERROR"
   */
  private String query;

  /**
   * The max number of entries to return (default: 100, max: 5000)
   */
  @Builder.Default
  private Integer limit = 100;

  /**
   * Start timestamp (nanoseconds or RFC3339)
   */
  private Instant start;

  /**
   * End timestamp (nanoseconds or RFC3339)
   */
  private Instant end;

  /**
   * Query direction: forward or backward (default: backward)
   */
  @Builder.Default
  private String direction = "backward";

  /**
   * Step for range queries (e.g., "1m", "5m")
   */
  private String step;
}
