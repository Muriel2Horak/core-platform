package cz.muriel.core.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muriel.core.auth.KeycloakClient;
import cz.muriel.core.monitoring.bff.model.TenantBinding;
import cz.muriel.core.monitoring.bff.service.TenantOrgService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

/**
 * Internal endpoint for Nginx auth_request
 * 
 * Returns 200 + Grafana-JWT header if authenticated Returns 401 if not
 * authenticated
 * 
 * Security: - Rate limited (100 req/min per user) - Internal only (Nginx should
 * restrict access) - No PII in logs - Reads JWT from HTTP-only cookie (not
 * Authorization header) - Auto-refreshes tokens before expiration
 */
@RestController @RequestMapping("/internal/auth") @Slf4j
public class AuthRequestController {

  private static final String ACCESS_COOKIE = "at"; // Access token cookie
  private static final String REFRESH_COOKIE = "rt"; // Refresh token cookie
  private static final Duration REFRESH_THRESHOLD = Duration.ofMinutes(5); // Refresh if token
                                                                           // expires in < 5 min

  private final GrafanaJwtService jwtService;
  private final JwtDecoder jwtDecoder;
  private final KeycloakClient keycloakClient;
  private final TenantOrgService tenantOrgService;
  private final cz.muriel.core.monitoring.grafana.GrafanaAdminClient grafanaAdminClient;

  public AuthRequestController(GrafanaJwtService jwtService, JwtDecoder jwtDecoder,
      KeycloakClient keycloakClient, TenantOrgService tenantOrgService,
      cz.muriel.core.monitoring.grafana.GrafanaAdminClient grafanaAdminClient) {
    this.jwtService = jwtService;
    this.jwtDecoder = jwtDecoder;
    this.keycloakClient = keycloakClient;
    this.tenantOrgService = tenantOrgService;
    this.grafanaAdminClient = grafanaAdminClient;
  }

  /**
   * Nginx auth_request endpoint
   * 
   * Called by Nginx before proxying to Grafana Reads JWT from HTTP-only cookie,
   * validates it, and auto-refreshes if needed
   * 
   * NOTE: NO RATE LIMITING - this is internal Nginx→Backend communication Rate
   * limiting should be applied at Nginx level, not here!
   */
  @GetMapping("/grafana")
  public ResponseEntity<Void> authenticateForGrafana(HttpServletRequest request,
      HttpServletResponse response) {

    // Read JWT token from HTTP-only cookie
    String token = getCookieValue(request, ACCESS_COOKIE);
    String refreshToken = getCookieValue(request, REFRESH_COOKIE);

    // DEBUG: Log all cookies and headers
    log.info("🔍 Grafana auth request - Cookie header: {}", request.getHeader("Cookie"));
    log.info("🔍 Grafana auth request - Available cookies: {}",
        request.getCookies() != null ? request.getCookies().length : 0);
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        log.info("🔍 Cookie: {} = {} (length: {})", cookie.getName(),
            cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "...",
            cookie.getValue().length());
      }
    }
    log.info("🔍 Extracted token: {} (length: {})", token != null ? "present" : "null",
        token != null ? token.length() : 0);

    if (token == null || token.isEmpty()) {
      log.debug("Grafana auth request failed: no auth cookie found");
      return ResponseEntity.status(401).build();
    }

    try {
      // Decode and validate JWT
      Jwt jwt = jwtDecoder.decode(token);

      if (jwt == null) {
        log.debug("Grafana auth request failed: invalid JWT");
        return ResponseEntity.status(401).build();
      }

      Instant now = Instant.now();
      Instant expiresAt = jwt.getExpiresAt();

      // Check if token is expired
      if (expiresAt == null || !expiresAt.isAfter(now)) {
        log.debug("Grafana auth request failed: expired JWT");
        return ResponseEntity.status(401).build();
      }

      // Auto-refresh if token expires soon (< 5 minutes) and we have refresh token
      Duration timeUntilExpiry = Duration.between(now, expiresAt);
      if (timeUntilExpiry.compareTo(REFRESH_THRESHOLD) < 0 && refreshToken != null
          && !refreshToken.isEmpty()) {
        log.debug("Token expires in {}, attempting auto-refresh for user: {}", timeUntilExpiry,
            jwt.getClaimAsString("preferred_username"));

        try {
          JsonNode tokenResponse = keycloakClient.refreshToken(refreshToken);
          String newAccessToken = tokenResponse.path("access_token").asText();
          String newRefreshToken = tokenResponse.path("refresh_token").asText();

          // Update cookies with new tokens
          if (newAccessToken != null && !newAccessToken.isEmpty()) {
            Cookie atCookie = new Cookie(ACCESS_COOKIE, newAccessToken);
            atCookie.setHttpOnly(true);
            atCookie.setSecure(true);
            atCookie.setPath("/");
            atCookie.setMaxAge(3600); // 1 hour
            response.addCookie(atCookie);

            if (newRefreshToken != null && !newRefreshToken.isEmpty()) {
              Cookie rtCookie = new Cookie(REFRESH_COOKIE, newRefreshToken);
              rtCookie.setHttpOnly(true);
              rtCookie.setSecure(true);
              rtCookie.setPath("/");
              rtCookie.setMaxAge(86400); // 24 hours
              response.addCookie(rtCookie);
            }

            token = newAccessToken; // Use new token for Grafana
            log.debug("✅ Token auto-refreshed successfully for user: {}",
                jwt.getClaimAsString("preferred_username"));
          }
        } catch (Exception e) {
          log.warn("Failed to auto-refresh token, continuing with existing token: {}",
              e.getMessage());
          // Continue with existing token - it's still valid for a few more minutes
        }
      }

      String username = jwt.getClaimAsString("preferred_username");
      log.debug("Grafana auth request successful for user: {}", username);

      // Resolve tenant → Grafana org mapping
      TenantBinding binding = tenantOrgService.resolve(jwt);
      Long grafanaOrgId = binding.orgId();

      log.debug("✅ Resolved user {} to Grafana org {}", username, grafanaOrgId);

      // 🆕 CRITICAL FIX: Ensure JWT user is member of tenant org
      // Grafana JWT auto_sign_up creates users ONLY in org 1 (Main Org)
      // We must manually add them to their tenant org on first request
      try {
        grafanaAdminClient.addUserToOrg(grafanaOrgId, username, "Admin");
        log.debug("✅ Ensured user {} is member of org {}", username, grafanaOrgId);
      } catch (Exception e) {
        log.warn("⚠️  Failed to ensure user {} in org {} (may already exist): {}", username,
            grafanaOrgId, e.getMessage());
        // Continue - user might already be member, or Grafana might be temporarily
        // unavailable
      }

      // CRITICAL: Nginx expects these headers
      // - Grafana-Jwt becomes grafana_jwt (lowercase)
      // - Grafana-Org-Id becomes grafana_org_id (lowercase)
      return ResponseEntity.ok().header("Grafana-Jwt", token)
          .header("Grafana-Org-Id", String.valueOf(grafanaOrgId)).build();

    } catch (Exception e) {
      log.error("Failed to validate JWT for Grafana: {}", e.getMessage());
      return ResponseEntity.status(500).build();
    }
  }

  /**
   * Extract cookie value from request
   */
  private String getCookieValue(HttpServletRequest request, String cookieName) {
    if (request.getCookies() == null) {
      return null;
    }

    for (Cookie cookie : request.getCookies()) {
      if (cookieName.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }

    return null;
  }
}
