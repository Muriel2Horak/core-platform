package cz.muriel.core.reporting.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Service for executing Cube.js queries with Circuit Breaker protection. Wraps
 * Cube.js HTTP API with resilience patterns.
 */
@Service @Slf4j @RequiredArgsConstructor
public class CubeQueryService {

  private final WebClient cubeWebClient;
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final QueryDeduplicator queryDeduplicator;

  /**
   * Executes a Cube.js query with Circuit Breaker protection. Circuit Breaker is
   * per-tenant to isolate failures.
   *
   * @param query Cube.js query object (dimensions, measures, filters, etc.)
   * @param tenantId Tenant ID for RLS and CB isolation
   * @return Query result from Cube.js
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> executeQuery(Map<String, Object> query, String tenantId) {
    // Use query deduplication to prevent duplicate concurrent queries
    return queryDeduplicator.executeWithDeduplication(query, tenantId, () -> {
      CircuitBreaker circuitBreaker = circuitBreakerRegistry
          .circuitBreaker("cubeQueryCircuitBreaker-" + tenantId);

      return circuitBreaker.executeSupplier(() -> {
        log.debug("Executing Cube.js query for tenant {}: {}", tenantId, query);

        return cubeWebClient.post().uri("/cubejs-api/v1/load")
            .header("Authorization", "Bearer " + tenantId) // Cube.js uses tenant as JWT
            .bodyValue(Map.of("query", query)).retrieve().bodyToMono(Map.class).block();
      });
    });
  }

  /**
   * Fetches entity metadata (schema) from Cube.js. Uses separate Circuit Breaker
   * with more lenient threshold (70%).
   *
   * @param entityName Entity name (e.g., "User", "Company")
   * @param tenantId Tenant ID for RLS
   * @return Entity metadata
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> getEntityMetadata(String entityName, String tenantId) {
    CircuitBreaker circuitBreaker = circuitBreakerRegistry
        .circuitBreaker("cubeMetadataCircuitBreaker-" + tenantId);

    return circuitBreaker.executeSupplier(() -> {
      log.debug("Fetching metadata for entity {} (tenant {})", entityName, tenantId);

      return cubeWebClient.get().uri("/cubejs-api/v1/meta")
          .header("Authorization", "Bearer " + tenantId).retrieve().bodyToMono(Map.class).block();
    });
  }
}
