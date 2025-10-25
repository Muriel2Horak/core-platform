package cz.muriel.core.monitoring;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Optional;

/**
 * 🔐 Grafana SSO Auth Bridge for Nginx auth_request
 * 
 * This controller provides the `/internal/auth/grafana` endpoint used by
 * Nginx's auth_request directive to authenticate users before proxying to
 * Grafana.
 * 
 * Flow: 1. Nginx intercepts request to /core-admin/monitoring/* 2. Makes
 * internal subrequest to /_auth/grafana (proxied to this endpoint) 3. This
 * endpoint validates Keycloak JWT from 'at' cookie 4. Mints short-lived Grafana
 * JWT and returns it in response headers 5. Nginx forwards request to Grafana
 * with X-Org-JWT header 6. Grafana validates JWT via BFF JWKS and
 * auto-provisions user
 * 
 * Security: - Returns 401 if no valid 'at' cookie - Returns 500 if JWT minting
 * fails - Returns 200 with Grafana-JWT and Grafana-Org-Id headers on success
 */
@RestController @RequiredArgsConstructor @Slf4j
public class GrafanaAuthBridgeController {

  private final JwtDecoder jwtDecoder;
  private final GrafanaJwtService grafanaJwtService;

  /**
   * Internal endpoint for Nginx auth_request to validate and convert Keycloak JWT
   * to Grafana JWT
   * 
   * This endpoint is called by Nginx before every request to Grafana. It must be
   * fast and lightweight.
   */
  @GetMapping("/internal/auth/grafana")
  public ResponseEntity<Void> authenticateForGrafana(HttpServletRequest request) {
    try {
      // Extract 'at' cookie (Keycloak access token)
      Optional<String> accessToken = extractCookie(request, "at");

      if (accessToken.isEmpty()) {
        log.warn("❌ Grafana auth failed: No 'at' cookie found");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }

      // Decode and validate Keycloak JWT
      Jwt keycloakJwt;
      try {
        keycloakJwt = jwtDecoder.decode(accessToken.get());
      } catch (JwtException e) {
        log.warn("❌ Grafana auth failed: Invalid JWT in 'at' cookie: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }

      // Mint Grafana JWT
      String grafanaJwt = grafanaJwtService.mintGrafanaJwtFromKeycloakJwt(keycloakJwt);

      // Get Grafana org ID from tenant
      String tenantId = keycloakJwt.getClaimAsString("tenant");
      if (tenantId == null || tenantId.isBlank()) {
        String issuer = keycloakJwt.getIssuer().toString();
        String[] parts = issuer.split("/realms/");
        if (parts.length > 1) {
          tenantId = parts[1];
        } else {
          tenantId = "admin"; // Fallback
        }
      }
      long grafanaOrgId = grafanaJwtService.getTenantRegistry().getGrafanaOrgId(tenantId);

      String username = keycloakJwt.getClaimAsString("preferred_username");
      log.debug("✅ Grafana auth success: user={}, tenant={}, orgId={}", username, tenantId,
          grafanaOrgId);

      // Return 200 with Grafana JWT in custom headers
      // Nginx will extract these via auth_request_set
      HttpHeaders headers = new HttpHeaders();
      headers.set("Grafana-JWT", grafanaJwt);
      headers.set("Grafana-Org-Id", String.valueOf(grafanaOrgId));

      return ResponseEntity.ok().headers(headers).build();

    } catch (Exception e) {
      log.error("❌ Grafana auth bridge error: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Extract cookie value by name
   */
  private Optional<String> extractCookie(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }

    return Arrays.stream(cookies).filter(cookie -> name.equals(cookie.getName()))
        .map(Cookie::getValue).findFirst();
  }
}
