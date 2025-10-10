package cz.muriel.core.monitoring.bff.controller;

import cz.muriel.core.monitoring.bff.service.MonitoringProxyService;
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
 * BFF Controller for proxying Grafana HTTP API requests.
 * All requests are authenticated via JWT and mapped to tenant-specific Grafana orgs.
 * Never exposes service account tokens to the browser.
 */
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Slf4j
public class MonitoringProxyController {

    private final MonitoringProxyService service;

    /**
     * POST /api/monitoring/ds/query
     * Used by Grafana Scenes to query datasources (Prometheus, Loki, etc.)
     */
    @PostMapping("/ds/query")
    public ResponseEntity<String> query(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> body
    ) {
        log.info("Received ds/query request from user: {}", jwt.getSubject());
        return service.forwardQuery(jwt, body);
    }

    /**
     * GET /api/monitoring/datasources
     * Returns available datasources for the tenant's org.
     * Response is minimal metadata (uid, name, type) - sensitive data filtered.
     */
    @GetMapping("/datasources")
    public ResponseEntity<String> datasources(@AuthenticationPrincipal Jwt jwt) {
        log.info("Received datasources request from user: {}", jwt.getSubject());
        return service.forwardGet(jwt, "/api/datasources");
    }

    /**
     * GET /api/monitoring/dashboards/uid/{uid}
     * Returns dashboard definition for embedding in Scenes.
     */
    @GetMapping("/dashboards/uid/{uid}")
    public ResponseEntity<String> dashboard(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String uid
    ) {
        String encodedUid = UriUtils.encode(uid, StandardCharsets.UTF_8);
        log.info("Received dashboard request for uid: {} from user: {}", uid, jwt.getSubject());
        return service.forwardGet(jwt, "/api/dashboards/uid/" + encodedUid);
    }

    /**
     * GET /api/monitoring/health
     * Health check - verify Grafana is reachable.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return service.healthCheck();
    }

    /**
     * Block any non-whitelisted endpoints.
     * This prevents users from modifying Grafana configuration.
     */
    @RequestMapping(value = "/**", method = {RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<String> blockMutations() {
        log.warn("Blocked mutation request to monitoring BFF");
        return ResponseEntity.status(403)
            .body("{\"error\":\"Mutations not allowed via BFF\"}");
    }
}
