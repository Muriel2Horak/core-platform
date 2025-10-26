package cz.muriel.core.monitoring.loki.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Response from Loki query_range API
 * 
 * @see <a href=
 * "https://grafana.com/docs/loki/latest/reference/loki-http-api/#query-logs-within-a-range-of-time">Loki
 * API Docs</a>
 */
@Data
public class LokiQueryResponse {

  private String status; // "success" or "error"

  private LokiData data;

  @Data
  public static class LokiData {
    private String resultType; // "streams" or "matrix"
    private List<LokiStream> result;
    private LokiStats stats;
  }

  @Data
  public static class LokiStream {
    private Map<String, String> stream; // Labels (tenant, service, level, etc.)
    private List<LokiEntry> values;
  }

  @Data
  public static class LokiEntry {
    @JsonProperty("0")
    private String timestamp; // Nanoseconds since epoch

    @JsonProperty("1")
    private String line; // Log line content
  }

  @Data
  public static class LokiStats {
    private Summary summary;

    @Data
    public static class Summary {
      private Long bytesProcessedPerSecond;
      private Long linesProcessedPerSecond;
      private Long totalBytesProcessed;
      private Long totalLinesProcessed;
      private Double execTime;
    }
  }
}
