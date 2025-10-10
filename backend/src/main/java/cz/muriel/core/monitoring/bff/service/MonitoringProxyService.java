package cz.muriel.core.monitoring.bff.service;

import cz.muriel.core.monitoring.bff.model.TenantBinding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Service for proxying requests to Grafana HTTP API. Adds service account token
 * and X-Grafana-Org-Id header. Never exposes tokens to the browser.
 */
@Service @RequiredArgsConstructor @Slf4j
public class MonitoringProxyService {

  private final TenantOrgService tenantOrgService;
  private final WebClient grafanaClient;

  /**
   * Forward POST /api/ds/query request to Grafana. Used by Grafana Scenes to
   * query datasources.
   */
  public ResponseEntity<String> forwardQuery(Jwt jwt, Map<String, Object> body) {
    TenantBinding binding = tenantOrgService.resolve(jwt);
    String tenantId = binding.tenantId();

    log.info("Forwarding query for tenant {} to org {}", tenantId, binding.orgId());
    log.debug("Query body: {}", body);

    try {
      String response = grafanaClient.post().uri("/api/ds/query")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + binding.serviceAccountToken())
          .header("X-Grafana-Org-Id", String.valueOf(binding.orgId())).bodyValue(body).retrieve()
          .bodyToMono(String.class).timeout(Duration.ofSeconds(30)).block();

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error forwarding query for tenant {}: {}", tenantId, e.getMessage());
      return ResponseEntity.internalServerError().body("{\"error\":\"Failed to query Grafana\"}");
    }
  }

  /**
   * Forward GET request to Grafana.
   */
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
