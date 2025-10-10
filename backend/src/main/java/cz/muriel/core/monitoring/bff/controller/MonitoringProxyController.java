package cz.muriel.core.monitoring.bff.controller;

import cz.muriel.core.monitoring.bff.service.MonitoringProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * BFF Controller for proxying Grafana HTTP API requests. All requests are
 * authenticated via JWT and mapped to tenant-specific Grafana orgs. Never
 * exposes service account tokens to the browser.
 */
@RestController @RequestMapping("/api/monitoring") @RequiredArgsConstructor @Slf4j @Tag(name = "Monitoring BFF", description = "Backend-for-Frontend proxy for Grafana with tenant isolation") @SecurityRequirement(name = "bearerAuth")
public class MonitoringProxyController {

  private final MonitoringProxyService service;

  /**
   * POST /api/monitoring/ds/query Used by Grafana Scenes to query datasources
   * (Prometheus, Loki, etc.)
   */
  @PostMapping("/ds/query") @Operation(summary = "Query datasource", description = "Proxies datasource queries to Grafana with automatic tenant isolation. "
      + "Adds service account token and X-Grafana-Org-Id header based on JWT tenant claim.") @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Query successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class), examples = @ExampleObject(value = "{\"results\":{\"A\":{\"frames\":[...]}}}"))),
          @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
          @ApiResponse(responseCode = "429", description = "Rate limit exceeded (100 req/min per tenant)"),
          @ApiResponse(responseCode = "500", description = "Internal server error or Grafana unavailable"),
          @ApiResponse(responseCode = "503", description = "Circuit breaker open - Grafana service unavailable") })
  public ResponseEntity<String> query(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Grafana datasource query request", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Prometheus query example", value = "{\"queries\":[{\"refId\":\"A\",\"expr\":\"up\",\"range\":true,\"instant\":false}],\"from\":\"now-1h\",\"to\":\"now\"}"))) @RequestBody Map<String, Object> body) {
    log.info("Received ds/query request from user: {}", jwt.getSubject());
    return service.forwardQuery(jwt, body);
  }

  /**
   * GET /api/monitoring/datasources Returns available datasources for the
   * tenant's org. Response is minimal metadata (uid, name, type) - sensitive data
   * filtered.
   */
  @GetMapping("/datasources") @Operation(summary = "List datasources", description = "Returns available datasources for the tenant's Grafana organization. "
      + "Response contains minimal metadata (uid, name, type) - sensitive data like credentials are filtered.") @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Datasources retrieved successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "[{\"uid\":\"prometheus\",\"name\":\"Prometheus\",\"type\":\"prometheus\"}]"))),
          @ApiResponse(responseCode = "401", description = "Unauthorized"),
          @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<String> datasources(
      @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    log.info("Received datasources request from user: {}", jwt.getSubject());
    return service.forwardGet(jwt, "/api/datasources");
  }

  /**
   * GET /api/monitoring/dashboards/uid/{uid} Returns dashboard definition for
   * embedding in Scenes.
   */
  @GetMapping("/dashboards/uid/{uid}") @Operation(summary = "Get dashboard by UID", description = "Returns dashboard definition for embedding in Grafana Scenes. "
      + "Dashboard must exist in the tenant's Grafana organization.") @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Dashboard retrieved successfully", content = @Content(mediaType = "application/json")),
          @ApiResponse(responseCode = "401", description = "Unauthorized"),
          @ApiResponse(responseCode = "404", description = "Dashboard not found"),
          @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<String> dashboard(
      @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
      @Parameter(description = "Dashboard unique identifier", example = "tenant-app-metrics") @PathVariable String uid) {
    String encodedUid = UriUtils.encode(uid, StandardCharsets.UTF_8);
    log.info("Received dashboard request for uid: {} from user: {}", uid, jwt.getSubject());
    return service.forwardGet(jwt, "/api/dashboards/uid/" + encodedUid);
  }

  /**
   * GET /api/monitoring/health Health check - verify Grafana is reachable.
   */
  @GetMapping("/health") @Operation(summary = "Health check", description = "Verifies that Grafana service is reachable and responsive. "
      + "Returns Grafana's health status without requiring authentication.") @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Grafana is healthy", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"database\":\"ok\"}"))),
          @ApiResponse(responseCode = "503", description = "Grafana is unavailable", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"status\":\"error\",\"message\":\"Grafana unavailable\"}"))) })
  public ResponseEntity<String> health() {
    return service.healthCheck();
  }

  /**
   * Block any non-whitelisted endpoints. This prevents users from modifying
   * Grafana configuration.
   */
  @RequestMapping(value = "/**", method = { RequestMethod.PUT, RequestMethod.DELETE,
      RequestMethod.PATCH }) @Operation(summary = "Block mutations", description = "Security endpoint that blocks all PUT/DELETE/PATCH requests to prevent "
          + "unauthorized modifications to Grafana configuration via BFF.") @ApiResponse(responseCode = "403", description = "Mutation not allowed", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\":\"Mutations not allowed via BFF\"}")))
  public ResponseEntity<String> blockMutations() {
    log.warn("Blocked mutation request to monitoring BFF");
    return ResponseEntity.status(403).body("{\"error\":\"Mutations not allowed via BFF\"}");
  }
}
