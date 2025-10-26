package cz.muriel.core.monitoring.loki;

import cz.muriel.core.monitoring.loki.dto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;

/**
 * 🔍 LOKI HTTP API CLIENT
 * 
 * REST client for Loki Query API with support for: - Log querying (query_range)
 * - Label discovery (labels, label values) - Circuit breaker for resilience -
 * Tenant isolation via LogQL filters
 * 
 * @see <a href=
 * "https://grafana.com/docs/loki/latest/reference/loki-http-api/">Loki HTTP
 * API</a>
 */
@Slf4j @Component @RequiredArgsConstructor @ConditionalOnProperty(name = "monitoring.loki.enabled", havingValue = "true", matchIfMissing = false)
public class LokiClient {

  private final RestTemplate restTemplate;

  @Value("${loki.url:http://loki:3100}")
  private String lokiUrl;

  @Value("${loki.query.max-entries:5000}")
  private Integer maxEntries;

  /**
   * Query logs within a time range
   * 
   * @param request Query parameters (LogQL, time range, limit)
   * @return Loki response with log streams
   */
  @CircuitBreaker(name = "loki", fallbackMethod = "queryLogsFallback")
  public LokiQueryResponse queryLogs(LokiQueryRequest request) {
    log.debug("📊 Querying Loki: query={}, start={}, end={}, limit={}", request.getQuery(),
        request.getStart(), request.getEnd(), request.getLimit());

    // Build URL with query parameters
    String url = UriComponentsBuilder.fromUriString(lokiUrl + "/loki/api/v1/query_range")
        .queryParam("query", request.getQuery())
        .queryParam("limit", Math.min(request.getLimit(), maxEntries))
        .queryParam("start", toNanoseconds(request.getStart()))
        .queryParam("end", toNanoseconds(request.getEnd()))
        .queryParam("direction", request.getDirection()).build(false) // Don't encode query (Loki
                                                                      // needs raw LogQL)
        .toUriString();

    try {
      ResponseEntity<LokiQueryResponse> response = restTemplate.exchange(url, HttpMethod.GET, null,
          LokiQueryResponse.class);

      if (response.getBody() == null || !"success".equals(response.getBody().getStatus())) {
        log.error("❌ Loki query failed: {}", response.getBody());
        throw new LokiClientException("Loki query returned non-success status");
      }

      log.debug("✅ Loki query OK: {} streams returned",
          response.getBody().getData().getResult().size());

      return response.getBody();

    } catch (Exception e) {
      log.error("❌ Loki query exception: {}", e.getMessage(), e);
      throw new LokiClientException("Failed to query Loki", e);
    }
  }

  /**
   * Get all available label names
   * 
   * @param start Start time (optional)
   * @param end End time (optional)
   * @return List of label names
   */
  @CircuitBreaker(name = "loki", fallbackMethod = "getLabelsFallback")
  public List<String> getLabels(Instant start, Instant end) {
    log.debug("📋 Fetching Loki labels");

    UriComponentsBuilder builder = UriComponentsBuilder
        .fromUriString(lokiUrl + "/loki/api/v1/labels");
    if (start != null) {
      builder.queryParam("start", toNanoseconds(start));
    }
    if (end != null) {
      builder.queryParam("end", toNanoseconds(end));
    }

    String url = builder.build().toUriString();

    try {
      ResponseEntity<LokiLabelsResponse> response = restTemplate.exchange(url, HttpMethod.GET, null,
          LokiLabelsResponse.class);

      if (response.getBody() == null || !"success".equals(response.getBody().getStatus())) {
        log.error("❌ Loki labels query failed");
        throw new LokiClientException("Loki labels query failed");
      }

      return response.getBody().getData();

    } catch (Exception e) {
      log.error("❌ Loki labels exception: {}", e.getMessage(), e);
      throw new LokiClientException("Failed to fetch labels from Loki", e);
    }
  }

  /**
   * Get all values for a specific label
   * 
   * @param label Label name (e.g., "tenant", "service", "level")
   * @param start Start time (optional)
   * @param end End time (optional)
   * @return List of label values
   */
  @CircuitBreaker(name = "loki", fallbackMethod = "getLabelValuesFallback")
  public List<String> getLabelValues(String label, Instant start, Instant end) {
    log.debug("📋 Fetching Loki label values for: {}", label);

    UriComponentsBuilder builder = UriComponentsBuilder
        .fromUriString(lokiUrl + "/loki/api/v1/label/" + label + "/values");

    if (start != null) {
      builder.queryParam("start", toNanoseconds(start));
    }
    if (end != null) {
      builder.queryParam("end", toNanoseconds(end));
    }

    String url = builder.build().toUriString();

    try {
      ResponseEntity<LokiLabelValuesResponse> response = restTemplate.exchange(url, HttpMethod.GET,
          null, LokiLabelValuesResponse.class);

      if (response.getBody() == null || !"success".equals(response.getBody().getStatus())) {
        log.error("❌ Loki label values query failed for label: {}", label);
        throw new LokiClientException("Loki label values query failed");
      }

      return response.getBody().getData();

    } catch (Exception e) {
      log.error("❌ Loki label values exception: {}", e.getMessage(), e);
      throw new LokiClientException("Failed to fetch label values from Loki", e);
    }
  }

  /**
   * Convert Instant to Loki nanosecond timestamp
   */
  private String toNanoseconds(Instant instant) {
    if (instant == null) {
      return null;
    }
    long nanos = instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
    return String.valueOf(nanos);
  }

  // ===== CIRCUIT BREAKER FALLBACKS =====

  private LokiQueryResponse queryLogsFallback(LokiQueryRequest request, Exception e) {
    log.error("⚠️ Circuit breaker fallback for queryLogs: {}", e.getMessage());
    LokiQueryResponse fallback = new LokiQueryResponse();
    fallback.setStatus("error");
    return fallback;
  }

  private List<String> getLabelsFallback(Instant start, Instant end, Exception e) {
    log.error("⚠️ Circuit breaker fallback for getLabels: {}", e.getMessage());
    return List.of();
  }

  private List<String> getLabelValuesFallback(String label, Instant start, Instant end,
      Exception e) {
    log.error("⚠️ Circuit breaker fallback for getLabelValues: {}", e.getMessage());
    return List.of();
  }

  /**
   * Custom exception for Loki client errors
   */
  public static class LokiClientException extends RuntimeException {
    public LokiClientException(String message) {
      super(message);
    }

    public LokiClientException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
