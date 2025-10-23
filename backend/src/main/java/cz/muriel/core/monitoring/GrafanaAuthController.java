package cz.muriel.core.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Grafana SSO Bridge Controller
 * 
 * Provides JWT minting endpoint for Nginx auth_request This endpoint is called
 * by Nginx for every Grafana request to inject JWT token
 */
@RestController @RequestMapping("/internal/auth") @Slf4j
public class GrafanaAuthController {

  private final GrafanaJwtService grafanaJwtService;

  public GrafanaAuthController(GrafanaJwtService grafanaJwtService) {
    this.grafanaJwtService = grafanaJwtService;
  }

  /**
   * Extract realm from Keycloak JWT issuer claim
   */
  private String extractRealm(Jwt jwt) {
    String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
    // issuer format: https://keycloak:8443/realms/{realm}
    if (issuer.contains("/realms/")) {
      return issuer.substring(issuer.lastIndexOf("/") + 1);
    }
    return "admin"; // Default fallback
  }

  /**
   * Nginx auth_request endpoint - mints Grafana JWT from session
   * 
   * Flow: 1. User authenticated via /api/auth/session (stores Keycloak JWT in
   * Redis session) 2. Nginx calls this endpoint with session cookie 3. Backend
   * extracts Keycloak JWT from session 4. Backend mints short-lived Grafana JWT
   * (5 min TTL) 5. Nginx injects JWT via X-Org-JWT header
   * 
   * @return JWT token in response header "Grafana-JWT" and body
   */
  @GetMapping("/grafana")
  public ResponseEntity<Map<String, Object>> grafanaAuth(Authentication authentication) {
    try {
      log.debug("🔐 Grafana auth request from user: {}",
          authentication != null ? authentication.getName() : "anonymous");

      if (authentication == null || !authentication.isAuthenticated()) {
        log.warn("❌ Grafana auth failed: User not authenticated");
        return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
      }

      // Extract Keycloak JWT from authentication
      if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
        log.error("❌ Grafana auth failed: Authentication is not JwtAuthenticationToken");
        return ResponseEntity.status(401).body(Map.of("error", "Invalid authentication type"));
      }

      Jwt kcToken = jwtAuth.getToken();

      // Mint Grafana JWT
      String grafanaJwt = grafanaJwtService.mintGrafanaJwtFromKeycloakJwt(kcToken);

      // Extract tenant and orgId from token
      String realm = extractRealm(kcToken);
      String tenantId = kcToken.getClaimAsString("tenant");
      if (tenantId == null || tenantId.isBlank()) {
        tenantId = realm; // Fallback to realm as tenant
      }
      int orgId = grafanaJwtService.getTenantRegistry().getGrafanaOrgId(tenantId);

      log.debug("✅ Grafana JWT minted for user={}, tenant={}, orgId={}", authentication.getName(),
          tenantId, orgId);

      // Return JWT in both header (for Nginx) and body (for debugging)
      return ResponseEntity.ok().header("Grafana-JWT", grafanaJwt)
          .header("Grafana-Org-Id", String.valueOf(orgId)).body(Map.of("token", grafanaJwt, "orgId",
              orgId, "user", authentication.getName(), "tenant", tenantId));

    } catch (Exception e) {
      log.error("❌ Failed to mint Grafana JWT: {}", e.getMessage(), e);
      return ResponseEntity.status(500).body(Map.of("error", "Failed to mint JWT"));
    }
  }
}
