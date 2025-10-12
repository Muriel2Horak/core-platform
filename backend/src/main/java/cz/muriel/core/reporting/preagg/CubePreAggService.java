package cz.muriel.core.reporting.preagg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Map;
import java.util.Set;

/**
 * Service for triggering Cube.js pre-aggregation refresh via API.
 * 
 * <p>
 * Cube.js Pre-aggregation API: POST /cubejs-api/v1/pre-aggregations/jobs
 * 
 * <p>
 * Supported entity types and their Cube.js schema mappings:
 * <ul>
 * <li>User → Users.js</li>
 * <li>Tenant → Tenants.js</li>
 * <li>Group → Groups.js</li>
 * </ul>
 * 
 * @see <a href="https://cube.dev/docs/rest-api#v1pre-aggregationsjobs">Cube.js
 * Pre-agg API</a>
 */
@Slf4j @Service @RequiredArgsConstructor
public class CubePreAggService {

  private final RestClient cubeRestClient;

  @Value("${app.cube.preagg.timeout:30000}")
  private long timeoutMs;

  // Mapping: entityType -> Cube.js schema name
  private static final Map<String, String> ENTITY_TO_CUBE_SCHEMA = Map.of("User", "Users", "Tenant",
      "Tenants", "Group", "Groups");

  // Cube schemas that have pre-aggregations configured
  private static final Set<String> SCHEMAS_WITH_PREAGG = Set.of("Users", "Tenants", "Groups");

  /**
   * Trigger pre-aggregation refresh for entity type.
   * 
   * @param entityType Entity type (e.g., "User", "Tenant")
   * @param tenantId Tenant ID (for multi-tenancy context, can be null)
   * @return true if refresh was triggered, false if no pre-agg exists for this
   * entity
   */
  public boolean refreshForEntityType(String entityType, String tenantId) {
    String cubeSchema = ENTITY_TO_CUBE_SCHEMA.get(entityType);

    if (cubeSchema == null) {
      log.debug("No Cube.js schema mapping for entityType={}", entityType);
      return false;
    }

    if (!SCHEMAS_WITH_PREAGG.contains(cubeSchema)) {
      log.debug("Schema {} has no pre-aggregations configured", cubeSchema);
      return false;
    }

    return refreshSchema(cubeSchema, tenantId);
  }

  /**
   * Trigger pre-aggregation refresh for Cube.js schema.
   * 
   * <p>
   * Uses Cube.js REST API: POST /cubejs-api/v1/pre-aggregations/jobs
   * 
   * <p>
   * Request body:
   * 
   * <pre>
   * {
   *   "action": "post",
   *   "selector": {
   *     "contexts": [{ "securityContext": { "tenantId": "tenant-1" } }],
   *     "timezones": ["UTC"],
   *     "cubes": ["Users"]
   *   }
   * }
   * </pre>
   * 
   * @param cubeSchema Cube.js schema name (e.g., "Users")
   * @param tenantId Tenant ID for security context (can be null)
   * @return true if refresh job created successfully
   */
  public boolean refreshSchema(String cubeSchema, String tenantId) {
    log.debug("Triggering pre-aggregation refresh: schema={}, tenant={}", cubeSchema, tenantId);

    try {
      // Build request body
      Map<String, Object> securityContext = tenantId != null ? Map.of("tenantId", tenantId)
          : Map.of();

      Map<String, Object> requestBody = Map.of("action", "post", "selector",
          Map.of("contexts", new Object[] { Map.of("securityContext", securityContext) },
              "timezones", new String[] { "UTC" }, "cubes", new String[] { cubeSchema }));

      // Call Cube.js API
      var response = cubeRestClient.post().uri("/cubejs-api/v1/pre-aggregations/jobs")
          .contentType(MediaType.APPLICATION_JSON).body(requestBody).retrieve().toEntity(Map.class);

      if (response.getStatusCode() == HttpStatus.OK) {
        log.info("Pre-aggregation refresh job created: schema={}, tenant={}, response={}",
            cubeSchema, tenantId, response.getBody());
        return true;
      } else {
        log.warn("Pre-aggregation refresh returned non-OK status: {}", response.getStatusCode());
        return false;
      }

    } catch (HttpStatusCodeException e) {
      log.error("Failed to trigger pre-aggregation refresh: schema={}, status={}, body={}",
          cubeSchema, e.getStatusCode(), e.getResponseBodyAsString(), e);
      return false;

    } catch (Exception e) {
      log.error("Unexpected error triggering pre-aggregation refresh: schema={}", cubeSchema, e);
      return false;
    }
  }

  /**
   * Get all supported entity types that have Cube.js pre-aggregations.
   */
  public Set<String> getSupportedEntityTypes() {
    return ENTITY_TO_CUBE_SCHEMA.keySet();
  }

  /**
   * Check if entity type has pre-aggregations configured.
   */
  public boolean hasPreAggregations(String entityType) {
    String cubeSchema = ENTITY_TO_CUBE_SCHEMA.get(entityType);
    return cubeSchema != null && SCHEMAS_WITH_PREAGG.contains(cubeSchema);
  }
}
