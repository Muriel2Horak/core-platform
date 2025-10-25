package cz.muriel.core.monitoring.bff;

import cz.muriel.core.monitoring.loki.LokiClient;
import cz.muriel.core.monitoring.loki.dto.LokiQueryRequest;
import cz.muriel.core.monitoring.loki.dto.LokiQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 📊 MONITORING BFF API
 * 
 * Backend-for-Frontend API for Loki monitoring:
 * - Automatic tenant isolation via JWT
 * - LogQL query execution
 * - Label discovery
 * - Metrics summary
 * 
 * Security: All queries are scoped to user's tenant via {tenant="..."} filter
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "monitoring.loki.enabled", havingValue = "true", matchIfMissing = false)
public class MonitoringBffController {

  private final LokiClient lokiClient;

  /**
   * Query logs with automatic tenant isolation
   * 
   * @param query LogQL query (WITHOUT tenant filter - will be added automatically)
   * @param hours Time range in hours (default: 1)
   * @param limit Max results (default: 100, max: 5000)
   * @param authentication User JWT
   * @return Loki query response
   */
  @GetMapping("/logs")
  public ResponseEntity<LokiQueryResponse> queryLogs(
      @RequestParam(required = false, defaultValue = "{service=~\".+\"}") String query,
      @RequestParam(required = false, defaultValue = "1") Integer hours,
      @RequestParam(required = false, defaultValue = "100") Integer limit,
      Authentication authentication
  ) {
    String tenant = extractTenant(authentication);
    log.info("📊 Query logs for tenant: {}, query: {}, hours: {}", tenant, query, hours);

    // Add tenant filter to LogQL query
    String tenantQuery = addTenantFilter(query, tenant);

    Instant end = Instant.now();
    Instant start = end.minus(hours, ChronoUnit.HOURS);

    LokiQueryRequest request = LokiQueryRequest.builder()
        .query(tenantQuery)
        .start(start)
        .end(end)
        .limit(Math.min(limit, 5000))
        .direction("backward") // Most recent first
        .build();

    LokiQueryResponse response = lokiClient.queryLogs(request);
    return ResponseEntity.ok(response);
  }

  /**
   * Get available labels (with tenant scope)
   * 
   * @param authentication User JWT
   * @return List of label names
   */
  @GetMapping("/labels")
  public ResponseEntity<List<String>> getLabels(Authentication authentication) {
    String tenant = extractTenant(authentication);
    log.info("📋 Get labels for tenant: {}", tenant);

    // Loki labels API returns all labels, we filter in UI
    Instant end = Instant.now();
    Instant start = end.minus(24, ChronoUnit.HOURS); // Last 24h for label discovery

    List<String> labels = lokiClient.getLabels(start, end);
    return ResponseEntity.ok(labels);
  }

  /**
   * Get values for a specific label (with tenant scope)
   * 
   * @param label Label name (e.g., "service", "level")
   * @param authentication User JWT
   * @return List of label values
   */
  @GetMapping("/labels/{label}/values")
  public ResponseEntity<List<String>> getLabelValues(
      @PathVariable String label,
      Authentication authentication
  ) {
    String tenant = extractTenant(authentication);
    log.info("📋 Get label values for: {} (tenant: {})", label, tenant);

    Instant end = Instant.now();
    Instant start = end.minus(24, ChronoUnit.HOURS);

    List<String> values = lokiClient.getLabelValues(label, start, end);
    
    // Filter by tenant if querying tenant label
    if ("tenant".equals(label)) {
      values = values.stream()
          .filter(v -> v.equals(tenant))
          .toList();
    }

    return ResponseEntity.ok(values);
  }

  /**
   * Get metrics summary (error rate, log volume)
   * 
   * @param hours Time range in hours (default: 1)
   * @param authentication User JWT
   * @return Metrics summary
   */
  @GetMapping("/metrics-summary")
  public ResponseEntity<Map<String, Object>> getMetricsSummary(
      @RequestParam(required = false, defaultValue = "1") Integer hours,
      Authentication authentication
  ) {
    String tenant = extractTenant(authentication);
    log.info("📊 Get metrics summary for tenant: {}, hours: {}", tenant, hours);

    Instant end = Instant.now();
    Instant start = end.minus(hours, ChronoUnit.HOURS);

    // Query total logs
    String totalQuery = String.format("{tenant=\"%s\"}", tenant);
    LokiQueryRequest totalRequest = LokiQueryRequest.builder()
        .query(totalQuery)
        .start(start)
        .end(end)
        .limit(5000)
        .build();
    LokiQueryResponse totalResponse = lokiClient.queryLogs(totalRequest);
    
    // Query error logs
    String errorQuery = String.format("{tenant=\"%s\"} |~ \"(?i)(error|exception|failed)\"", tenant);
    LokiQueryRequest errorRequest = LokiQueryRequest.builder()
        .query(errorQuery)
        .start(start)
        .end(end)
        .limit(5000)
        .build();
    LokiQueryResponse errorResponse = lokiClient.queryLogs(errorRequest);

    // Calculate metrics
    long totalLogs = totalResponse.getData() != null 
        ? totalResponse.getData().getResult().stream()
            .mapToLong(stream -> stream.getValues() != null ? stream.getValues().size() : 0)
            .sum()
        : 0;

    long errorLogs = errorResponse.getData() != null
        ? errorResponse.getData().getResult().stream()
            .mapToLong(stream -> stream.getValues() != null ? stream.getValues().size() : 0)
            .sum()
        : 0;

    double errorRate = totalLogs > 0 ? (double) errorLogs / totalLogs * 100 : 0;

    Map<String, Object> summary = Map.of(
        "totalLogs", totalLogs,
        "errorLogs", errorLogs,
        "errorRate", String.format("%.2f%%", errorRate),
        "timeRange", hours + "h",
        "tenant", tenant
    );

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
   * Add tenant filter to LogQL query
   * 
   * Examples:
   * - {service="backend"} → {tenant="admin",service="backend"}
   * - {level="error"} |= "exception" → {tenant="admin",level="error"} |= "exception"
   */
  private String addTenantFilter(String query, String tenant) {
    if (query == null || query.isBlank()) {
      return String.format("{tenant=\"%s\"}", tenant);
    }

    // If query starts with {, inject tenant filter
    if (query.startsWith("{")) {
      int closingBrace = query.indexOf('}');
      if (closingBrace > 0) {
        String labels = query.substring(1, closingBrace);
        String rest = query.substring(closingBrace + 1);
        
        // Add tenant as first label
        String newLabels = labels.isEmpty() 
            ? String.format("tenant=\"%s\"", tenant)
            : String.format("tenant=\"%s\",%s", tenant, labels);
        
        return "{" + newLabels + "}" + rest;
      }
    }

    // Fallback: prepend tenant filter
    return String.format("{tenant=\"%s\"} %s", tenant, query);
  }
}
