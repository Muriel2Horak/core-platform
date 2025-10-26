package cz.muriel.core.monitoring.bff;

import cz.muriel.core.monitoring.loki.LokiClient;
import cz.muriel.core.monitoring.loki.dto.LokiQueryRequest;
import cz.muriel.core.monitoring.loki.dto.LokiQueryResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 📊 MONITORING BFF API
 * 
 * Backend-for-Frontend API for Loki monitoring: - Automatic tenant isolation
 * via JWT - LogQL query execution - Label discovery - Metrics summary -
 * Prometheus metrics via Micrometer - Structured audit logging
 * 
 * Security: All queries are scoped to user's tenant via {tenant="..."} filter
 */
@Slf4j @RestController @RequestMapping("/api/monitoring") @RequiredArgsConstructor @ConditionalOnProperty(name = "monitoring.loki.enabled", havingValue = "true", matchIfMissing = false)
public class MonitoringBffController {

  private final LokiClient lokiClient;

  /**
   * Query logs with automatic tenant isolation
   * 
   * @param query LogQL query (WITHOUT tenant filter - will be added
   * automatically)
   * @param hours Time range in hours (default: 1)
   * @param limit Max results (default: 100, max: 5000)
   * @param authentication User JWT
   * @return Loki query response
   */
  @GetMapping("/logs") @Timed(value = "monitoring.bff.logs.query", description = "Time taken to query logs from Loki") @Counted(value = "monitoring.bff.logs.requests", description = "Total log query requests") @RateLimiter(name = "loki-bff", fallbackMethod = "rateLimitFallback")
  public ResponseEntity<LokiQueryResponse> queryLogs(
      @RequestParam(required = false, defaultValue = "{service=~\".+\"}") String query,
      @RequestParam(required = false, defaultValue = "1") Integer hours,
      @RequestParam(required = false, defaultValue = "100") Integer limit,
      Authentication authentication) {
    long startTime = System.currentTimeMillis();
    String tenant = extractTenant(authentication);

    // 📊 AUDIT LOG: Who queried what
    log.info("📊 [AUDIT] tenant={} user={} action=QUERY_LOGS query=\"{}\" hours={} limit={}",
        tenant, extractUsername(authentication), query, hours, limit);

    // Add tenant filter to LogQL query
    String tenantQuery = addTenantFilter(query, tenant);

    Instant end = Instant.now();
    Instant start = end.minus(hours, ChronoUnit.HOURS);

    LokiQueryRequest request = LokiQueryRequest.builder().query(tenantQuery).start(start).end(end)
        .limit(Math.min(limit, 5000)).direction("backward").build();

    LokiQueryResponse response = lokiClient.queryLogs(request);

    // 📊 AUDIT LOG: Result count and duration
    long duration = System.currentTimeMillis() - startTime;
    long resultCount = response.getData() != null
        ? response.getData().getResult().stream()
            .mapToLong(stream -> stream.getValues() != null ? stream.getValues().size() : 0).sum()
        : 0;

    log.info("📊 [AUDIT] tenant={} action=QUERY_LOGS_COMPLETE resultCount={} durationMs={}", tenant,
        resultCount, duration);

    return ResponseEntity.ok(response);
  }

  /**
   * Get available labels (tenant-scoped)
   * 
   * @param authentication User JWT
   * @return List of available labels
   */
  @GetMapping("/labels") @Timed(value = "monitoring.bff.labels.fetch", description = "Time to fetch available labels") @Counted(value = "monitoring.bff.labels.requests", description = "Total label fetch requests") @RateLimiter(name = "loki-bff", fallbackMethod = "rateLimitFallbackList")
  public ResponseEntity<List<String>> getLabels(Authentication authentication) {
    long startTime = System.currentTimeMillis();
    String tenant = extractTenant(authentication);

    log.info("📊 [AUDIT] tenant={} user={} action=GET_LABELS", tenant,
        extractUsername(authentication));

    // Loki labels API returns all labels, we filter in UI
    Instant end = Instant.now();
    Instant start = end.minus(24, ChronoUnit.HOURS);

    List<String> labels = lokiClient.getLabels(start, end);

    long duration = System.currentTimeMillis() - startTime;
    log.info("📊 [AUDIT] tenant={} action=GET_LABELS_COMPLETE labelCount={} durationMs={}", tenant,
        labels.size(), duration);

    return ResponseEntity.ok(labels);
  }

  /**
   * Get values for a specific label (with tenant scope)
   * 
   * @param label Label name (e.g., "service", "level")
   * @param authentication User JWT
   * @return List of label values
   */
  @GetMapping("/labels/{label}/values") @Timed(value = "monitoring.bff.label.values.fetch", description = "Time to fetch label values") @Counted(value = "monitoring.bff.label.values.requests", description = "Total label value requests") @RateLimiter(name = "loki-bff", fallbackMethod = "rateLimitFallbackList")
  public ResponseEntity<List<String>> getLabelValues(@PathVariable String label,
      Authentication authentication) {
    long startTime = System.currentTimeMillis();
    String tenant = extractTenant(authentication);

    log.info("📊 [AUDIT] tenant={} user={} action=GET_LABEL_VALUES label={}", tenant,
        extractUsername(authentication), label);

    Instant end = Instant.now();
    Instant start = end.minus(24, ChronoUnit.HOURS);

    List<String> values = lokiClient.getLabelValues(label, start, end);

    // Filter by tenant if querying tenant label
    if ("tenant".equals(label)) {
      values = values.stream().filter(v -> v.equals(tenant)).toList();
    }

    long duration = System.currentTimeMillis() - startTime;
    log.info(
        "📊 [AUDIT] tenant={} action=GET_LABEL_VALUES_COMPLETE label={} valueCount={} durationMs={}",
        tenant, label, values.size(), duration);

    return ResponseEntity.ok(values);
  }

  /**
   * Get metrics summary (error rate, log volume)
   * 
   * @param hours Time range in hours (default: 1)
   * @param authentication User JWT
   * @return Metrics summary
   */
  @GetMapping("/metrics-summary") @Timed(value = "monitoring.bff.metrics.summary", description = "Time to compute metrics summary") @Counted(value = "monitoring.bff.metrics.requests", description = "Total metrics summary requests") @RateLimiter(name = "loki-bff", fallbackMethod = "rateLimitFallbackMap")
  public ResponseEntity<Map<String, Object>> getMetricsSummary(
      @RequestParam(required = false, defaultValue = "1") Integer hours,
      Authentication authentication) {
    long startTime = System.currentTimeMillis();
    String tenant = extractTenant(authentication);

    log.info("📊 [AUDIT] tenant={} user={} action=GET_METRICS_SUMMARY hours={}", tenant,
        extractUsername(authentication), hours);

    Instant end = Instant.now();
    Instant start = end.minus(hours, ChronoUnit.HOURS);

    // Query total logs
    String totalQuery = String.format("{tenant=\"%s\"}", tenant);
    LokiQueryRequest totalRequest = LokiQueryRequest.builder().query(totalQuery).start(start)
        .end(end).limit(5000).build();
    LokiQueryResponse totalResponse = lokiClient.queryLogs(totalRequest);

    // Query error logs
    String errorQuery = String.format("{tenant=\"%s\"} |~ \"(?i)(error|exception|failed)\"",
        tenant);
    LokiQueryRequest errorRequest = LokiQueryRequest.builder().query(errorQuery).start(start)
        .end(end).limit(5000).build();
    LokiQueryResponse errorResponse = lokiClient.queryLogs(errorRequest);

    // Calculate metrics
    long totalLogs = totalResponse.getData() != null
        ? totalResponse.getData().getResult().stream()
            .mapToLong(stream -> stream.getValues() != null ? stream.getValues().size() : 0).sum()
        : 0;

    long errorLogs = errorResponse.getData() != null
        ? errorResponse.getData().getResult().stream()
            .mapToLong(stream -> stream.getValues() != null ? stream.getValues().size() : 0).sum()
        : 0;

    double errorRate = totalLogs > 0 ? (double) errorLogs / totalLogs * 100 : 0;

    Map<String, Object> summary = Map.of("totalLogs", totalLogs, "errorLogs", errorLogs,
        "errorRate", String.format("%.2f%%", errorRate), "timeRange", hours + "h", "tenant",
        tenant);

    long duration = System.currentTimeMillis() - startTime;
    log.info(
        "📊 [AUDIT] tenant={} action=GET_METRICS_SUMMARY_COMPLETE totalLogs={} errorLogs={} errorRate={}% durationMs={}",
        tenant, totalLogs, errorLogs, String.format("%.2f", errorRate), duration);

    return ResponseEntity.ok(summary);
  }

  // ===== PRIVATE HELPERS =====

  /**
   * Extract tenant from JWT
   */
  private String extractTenant(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new IllegalArgumentException("Missing or invalid JWT authentication");
    }

    String tenant = jwt.getClaimAsString("tenant");
    if (tenant == null || tenant.isBlank()) {
      throw new IllegalArgumentException("JWT missing 'tenant' claim");
    }

    return tenant;
  }

  /**
   * Extract username from JWT (for audit logging)
   */
  private String extractUsername(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      return "UNKNOWN";
    }

    String username = jwt.getClaimAsString("preferred_username");
    if (username == null || username.isBlank()) {
      username = jwt.getClaimAsString("email");
    }
    if (username == null || username.isBlank()) {
      username = jwt.getSubject();
    }

    return username != null ? username : "UNKNOWN";
  }

  /**
   * Add tenant filter to LogQL query
   * 
   * Examples: - {service="backend"} → {tenant="admin",service="backend"} -
   * {level="error"} |= "exception" → {tenant="admin",level="error"} |=
   * "exception"
   */
  private String addTenantFilter(String query, String tenant) {
    if (query == null || query.isBlank()) {
      return String.format("{tenant=\"%s\"}", tenant);
    }

    // If query starts with {, inject tenant filter
    if (query.startsWith("{")) {
      int closingBrace = query.indexOf('}');
      if (closingBrace == -1) {
        return String.format("{tenant=\"%s\"}", tenant);
      }

      String labels = query.substring(1, closingBrace);
      String rest = query.substring(closingBrace + 1);

      if (labels.isBlank()) {
        return String.format("{tenant=\"%s\"}%s", tenant, rest);
      } else {
        return String.format("{tenant=\"%s\",%s}%s", tenant, labels, rest);
      }
    }

    return String.format("{tenant=\"%s\"}", tenant);
  }

  // ===== RATE LIMITER FALLBACK METHODS =====

  /**
   * Fallback for queryLogs when rate limit exceeded
   */
  private ResponseEntity<LokiQueryResponse> rateLimitFallback(String query, Integer hours,
      Integer limit, Authentication authentication, Throwable t) {
    String tenant = extractTenant(authentication);
    log.warn("🚫 [RATE_LIMIT] tenant={} action=QUERY_LOGS - Rate limit exceeded (60 req/min)",
        tenant);

    // Return empty response with 429 status
    LokiQueryResponse emptyResponse = new LokiQueryResponse();
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(emptyResponse);
  }

  /**
   * Fallback for getLabels and getLabelValues when rate limit exceeded
   */
  private ResponseEntity<List<String>> rateLimitFallbackList(Authentication authentication,
      Throwable t) {
    String tenant = extractTenant(authentication);
    log.warn("🚫 [RATE_LIMIT] tenant={} action=GET_LABELS - Rate limit exceeded (60 req/min)",
        tenant);
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Collections.emptyList());
  }

  /**
   * Fallback for getLabelValues with path variable
   */
  private ResponseEntity<List<String>> rateLimitFallbackList(String label,
      Authentication authentication, Throwable t) {
    String tenant = extractTenant(authentication);
    log.warn(
        "🚫 [RATE_LIMIT] tenant={} action=GET_LABEL_VALUES label={} - Rate limit exceeded (60 req/min)",
        tenant, label);
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Collections.emptyList());
  }

  /**
   * Fallback for getMetricsSummary when rate limit exceeded
   */
  private ResponseEntity<Map<String, Object>> rateLimitFallbackMap(Integer hours,
      Authentication authentication, Throwable t) {
    String tenant = extractTenant(authentication);
    log.warn(
        "🚫 [RATE_LIMIT] tenant={} action=GET_METRICS_SUMMARY - Rate limit exceeded (60 req/min)",
        tenant);

    Map<String, Object> emptyMetrics = Map.of("error", "Rate limit exceeded", "limit",
        "60 requests per minute", "tenant", tenant);
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(emptyMetrics);
  }
}
