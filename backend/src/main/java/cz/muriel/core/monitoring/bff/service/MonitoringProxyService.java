package cz.muriel.core.monitoring.bff.service;

import cz.muriel.core.monitoring.bff.model.TenantBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Service for proxying requests to Grafana HTTP API. Adds service account token
 * and X-Grafana-Org-Id header. Never exposes tokens to the browser.
 */
@Service @Slf4j
public class MonitoringProxyService {

  private final TenantOrgService tenantOrgService;
  private final WebClient grafanaClient;

  public MonitoringProxyService(TenantOrgService tenantOrgService,
      @Qualifier("grafanaWebClient") WebClient grafanaClient) {
    this.tenantOrgService = tenantOrgService;
    this.grafanaClient = grafanaClient;
  }

  /**
   * Forward POST /api/ds/query request to Grafana. Used by Grafana Scenes to
   * query datasources.
   * 
   * Cached for 30s using cache key: user:{subject}:query:{bodyHashCode} Cache
   * eviction strategy: TTL-based (30s for real-time data)
   */
  @Cacheable(value = "grafana-queries", key = "#jwt.subject + ':query:' + #body.hashCode()", unless = "#result.statusCode.value() != 200")
  public ResponseEntity<String> forwardQuery(Jwt jwt, Map<String, Object> body) {
    TenantBinding binding = tenantOrgService.resolve(jwt);
    String tenantId = binding.tenantId();

    log.info("Forwarding query for tenant {} to org {}", tenantId, binding.orgId());
    log.info("Query body: {}", body);

    try {
      String response = grafanaClient.post().uri("/api/ds/query")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + binding.serviceAccountToken())
          .header("X-Grafana-Org-Id", String.valueOf(binding.orgId())).bodyValue(body).retrieve()
          .bodyToMono(String.class).timeout(Duration.ofSeconds(30)).block();

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error forwarding query for tenant {}: {}", tenantId, e.getMessage(), e);
      log.error("Failed query body was: {}", body);
      return ResponseEntity.internalServerError().body("{\"error\":\"Failed to query Grafana\"}");
    }
  }

  /**
   * Forward GET request to Grafana.
   * 
   * Cached for 30s for dashboard metadata (unlikely to change frequently)
   */
  @Cacheable(value = "grafana-dashboards", key = "#jwt.subject + ':' + #path", unless = "#result.statusCode.value() != 200")
  public ResponseEntity<String> forwardGet(Jwt jwt, String path) {
    TenantBinding binding = tenantOrgService.resolve(jwt);
    String tenantId = binding.tenantId();

    log.info("Forwarding GET {} for tenant {} to org {}", path, tenantId, binding.orgId());

    try {
      String response = grafanaClient.get().uri(path)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + binding.serviceAccountToken())
          .header("X-Grafana-Org-Id", String.valueOf(binding.orgId())).retrieve()
          .bodyToMono(String.class).timeout(Duration.ofSeconds(10)).block();

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error forwarding GET {} for tenant {}: {}", path, tenantId, e.getMessage());
      return ResponseEntity.internalServerError()
          .body("{\"error\":\"Failed to fetch from Grafana\"}");
    }
  }

  /**
   * Health check - verify Grafana is reachable.
   */
  public ResponseEntity<String> healthCheck() {
    try {
      String response = grafanaClient.get().uri("/api/health").retrieve().bodyToMono(String.class)
          .timeout(Duration.ofSeconds(5)).block();

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Grafana health check failed: {}", e.getMessage());
      return ResponseEntity.status(503)
          .body("{\"status\":\"error\",\"message\":\"Grafana unavailable\"}");
    }
  }
}
