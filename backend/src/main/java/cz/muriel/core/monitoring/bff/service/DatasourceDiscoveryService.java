package cz.muriel.core.monitoring.bff.service;

import cz.muriel.core.monitoring.bff.model.TenantBinding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor @Slf4j
public class DatasourceDiscoveryService {

  private final TenantOrgService tenantOrgService;
  private final WebClient.Builder webClientBuilder;

  /**
   * Discovers the Prometheus datasource UID for the current tenant's Grafana org.
   * 
   * @return The datasource UID, or null if not found
   */
  public String getPrometheusDataSourceUid() {
    try {
      // Get JWT from security context
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
        log.warn("No JWT found in security context");
        return null;
      }

      // Get org ID and token for current tenant
      TenantBinding binding = tenantOrgService.resolve(jwt);
      Long orgId = binding.orgId();
      String token = binding.serviceAccountToken();

      if (orgId == null || token == null) {
        log.warn("No Grafana org or token found for current tenant");
        return null;
      }

      log.debug("Looking for Prometheus datasource in org {}", orgId);

      // Query Grafana for datasources
      WebClient webClient = webClientBuilder.baseUrl("http://grafana:3000").build();

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> datasources = webClient.get().uri("/api/datasources")
          .header("Authorization", "Bearer " + token)
          .header("X-Grafana-Org-Id", String.valueOf(orgId)).retrieve().bodyToMono(List.class)
          .onErrorResume(e -> {
            log.error("Failed to query Grafana datasources", e);
            return Mono.just(List.of());
          }).block();

      if (datasources == null || datasources.isEmpty()) {
        log.warn("No datasources found in org {}", orgId);
        return null;
      }

      // Find Prometheus datasource
      for (Map<String, Object> ds : datasources) {
        String type = (String) ds.get("type");
        if ("prometheus".equals(type)) {
          String uid = (String) ds.get("uid");
          log.info("Found Prometheus datasource with UID: {} in org {}", uid, orgId);
          return uid;
        }
      }

      log.warn("No Prometheus datasource found in org {}", orgId);
      return null;

    } catch (Exception e) {
      log.error("Error discovering Prometheus datasource", e);
      return null;
    }
  }
}
