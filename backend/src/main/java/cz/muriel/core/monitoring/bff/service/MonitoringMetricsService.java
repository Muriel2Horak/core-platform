package cz.muriel.core.monitoring.bff.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import cz.muriel.core.monitoring.loki.LokiClient;
import cz.muriel.core.monitoring.loki.dto.LokiQueryRequest;
import cz.muriel.core.monitoring.loki.dto.LokiQueryResponse;
import cz.muriel.core.monitoring.prometheus.PrometheusClient;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonitoringMetricsService {

  private final LokiClient lokiClient;
  private final PrometheusClient prometheusClient;

  private static final Map<String, String> METRIC_QUERIES = Map.of(
      "cpu_usage", "avg(rate(process_cpu_usage[5m]))",
      "memory_used", "sum(jvm_memory_used_bytes)",
      "http_requests", "sum(rate(http_server_requests_seconds_count[5m]))"
  );

  public Map<String, Object> getMetricsSummary(String tenant, int hours) {
    Instant end = Instant.now();
    Instant start = end.minus(hours, ChronoUnit.HOURS);

    String totalQuery = String.format("{tenant=\"%s\"}", tenant);
    LokiQueryRequest totalRequest = LokiQueryRequest.builder().query(totalQuery).start(start)
        .end(end).limit(5000).build();
    LokiQueryResponse totalResponse = lokiClient.queryLogs(totalRequest);

    String errorQuery = String.format("{tenant=\"%s\"} |~ \"(?i)(error|exception|failed)\"",
        tenant);
    LokiQueryRequest errorRequest = LokiQueryRequest.builder().query(errorQuery).start(start)
        .end(end).limit(5000).build();
    LokiQueryResponse errorResponse = lokiClient.queryLogs(errorRequest);

    long totalLogs = totalResponse.getData() != null
        ? totalResponse.getData().getResult().stream()
            .mapToLong(stream -> stream.getValues() != null ? stream.getValues().size() : 0).sum()
        : 0;

    long errorLogs = errorResponse.getData() != null
        ? errorResponse.getData().getResult().stream()
            .mapToLong(stream -> stream.getValues() != null ? stream.getValues().size() : 0).sum()
        : 0;

    double errorRate = totalLogs > 0 ? (double) errorLogs / totalLogs * 100 : 0;

    Map<String, Object> summary = new HashMap<>();
    summary.put("totalLogs", totalLogs);
    summary.put("errorLogs", errorLogs);
    summary.put("errorRate", String.format("%.2f%%", errorRate));
    summary.put("timeRange", hours + "h");
    summary.put("tenant", tenant);
    summary.put("timestamp", Instant.now().toString());
    return summary;
  }

  public Map<String, Object> queryMetric(String metricName, Instant start, Instant end, Duration step) {
    String query = METRIC_QUERIES.get(metricName);
    if (query == null) {
      return Map.of("status", "error", "message", "Unknown metric: " + metricName);
    }

    return prometheusClient.queryRange(query, start, end, step);
  }

  public Map<String, String> listMetrics() {
    return METRIC_QUERIES;
  }
}
