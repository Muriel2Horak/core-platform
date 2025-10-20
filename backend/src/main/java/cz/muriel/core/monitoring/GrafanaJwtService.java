package cz.muriel.core.monitoring;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for minting short-lived Grafana JWT tokens from Keycloak identity
 * 
 * Security: - TTL: 120 seconds - JTI replay protection via Redis - HS256
 * signature (shared secret with Grafana) - Rate limiting in controller
 */
@Service @Slf4j
public class GrafanaJwtService {

  private final StringRedisTemplate redisTemplate;
  private final GrafanaTenantRegistry tenantRegistry;

  @Value("${grafana.jwt.ttl:120}")
  private int jwtTtl;

  @Value("${grafana.jwt.secret}")
  private String jwtSecret;

  public GrafanaJwtService(StringRedisTemplate redisTemplate,
      GrafanaTenantRegistry tenantRegistry) {
    this.redisTemplate = redisTemplate;
    this.tenantRegistry = tenantRegistry;
  }

  /**
   * Mint short-lived Grafana JWT from Keycloak authentication
   */
  public String mintGrafanaJwt(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      throw new IllegalArgumentException("Authentication must be JwtAuthenticationToken");
    }

    Jwt kcToken = jwtAuth.getToken();

    // Extract claims
    String username = kcToken.getClaimAsString("preferred_username");
    String email = kcToken.getClaimAsString("email");
    String name = kcToken.getClaimAsString("name");

    // Get tenant from realm or tenant claim
    String realm = extractRealm(kcToken);
    String tenantId = kcToken.getClaimAsString("tenant");
    if (tenantId == null || tenantId.isBlank()) {
      tenantId = realm; // Fallback to realm as tenant
    }

    // Map tenant to Grafana org
    int grafanaOrgId = tenantRegistry.getGrafanaOrgId(tenantId);

    // Map Keycloak roles to Grafana role
    String grafanaRole = mapKeycloakRoleToGrafana(authentication);

    // Generate JWT
    String jti = UUID.randomUUID().toString();
    Instant now = Instant.now();
    Instant expiry = now.plusSeconds(jwtTtl);

    String jwt = JWT.create().withSubject(username).withClaim("email", email)
        .withClaim("name", name != null ? name : username).withClaim("orgId", grafanaOrgId)
        .withClaim("role", grafanaRole).withIssuedAt(now).withExpiresAt(expiry).withJWTId(jti)
        .sign(Algorithm.HMAC256(jwtSecret));

    // Store JTI for replay protection
    redisTemplate.opsForValue().set("grafana:jti:" + jti, "used", jwtTtl, TimeUnit.SECONDS);

    log.debug("Minted Grafana JWT for user={}, tenant={}, orgId={}, role={}, ttl={}s", username,
        tenantId, grafanaOrgId, grafanaRole, jwtTtl);

    return jwt;
  }

  /**
   * Map Keycloak roles to Grafana role (Viewer|Editor|Admin)
   */
  private String mapKeycloakRoleToGrafana(Authentication authentication) {
    Set<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
        .map(a -> a.replace("ROLE_", "")).collect(java.util.stream.Collectors.toSet());

    if (roles.contains("CORE_ROLE_ADMIN")) {
      return "Admin";
    }
    if (roles.contains("CORE_ROLE_MONITORING")) {
      return "Editor";
    }
    if (roles.contains("CORE_TENANT_ADMIN")) {
      return "Editor";
    }
    if (roles.contains("CORE_ROLE_TENANT_MONITORING")) {
      return "Viewer";
    }

    return "Viewer"; // Default
  }

  /**
   * Map Keycloak roles from JWT to Grafana role (Viewer|Editor|Admin)
   */
  private String mapKeycloakRoleToGrafanaFromJwt(Jwt jwt) {
    // Extract roles from realm_access.roles
    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess != null && realmAccess.get("roles") instanceof List) {
      @SuppressWarnings("unchecked")
      List<String> roles = (List<String>) realmAccess.get("roles");

      if (roles.contains("CORE_ROLE_ADMIN")) {
        return "Admin";
      }
      if (roles.contains("CORE_ROLE_MONITORING")) {
        return "Editor";
      }
      if (roles.contains("CORE_TENANT_ADMIN")) {
        return "Editor";
      }
      if (roles.contains("CORE_ROLE_TENANT_MONITORING")) {
        return "Viewer";
      }
    }

    return "Viewer"; // Default
  }

  /**
   * Mint short-lived Grafana JWT directly from Keycloak JWT (for auth_request)
   * This is used by Nginx auth_request bridge where we only have the Keycloak JWT
   */
  public String mintGrafanaJwtFromKeycloakJwt(Jwt kcToken) {
    // Extract claims
    String username = kcToken.getClaimAsString("preferred_username");
    String email = kcToken.getClaimAsString("email");
    String name = kcToken.getClaimAsString("name");

    // Get tenant from realm or tenant claim
    String realm = extractRealm(kcToken);
    String tenantId = kcToken.getClaimAsString("tenant");
    if (tenantId == null || tenantId.isBlank()) {
      tenantId = realm; // Fallback to realm as tenant
    }

    // Map tenant to Grafana org
    int grafanaOrgId = tenantRegistry.getGrafanaOrgId(tenantId);

    // Map Keycloak roles to Grafana role
    String grafanaRole = mapKeycloakRoleToGrafanaFromJwt(kcToken);

    // Generate JWT
    String jti = UUID.randomUUID().toString();
    Instant now = Instant.now();
    Instant expiry = now.plusSeconds(jwtTtl);

    String jwt = JWT.create().withSubject(username).withClaim("email", email)
        .withClaim("name", name != null ? name : username).withClaim("orgId", grafanaOrgId)
        .withClaim("role", grafanaRole).withIssuedAt(now).withExpiresAt(expiry).withJWTId(jti)
        .sign(Algorithm.HMAC256(jwtSecret));

    // Store JTI for replay protection
    redisTemplate.opsForValue().set("grafana:jti:" + jti, "used", jwtTtl, TimeUnit.SECONDS);

    log.debug("Minted Grafana JWT for user={}, tenant={}, orgId={}, role={}, ttl={}s", username,
        tenantId, grafanaOrgId, grafanaRole, jwtTtl);

    return jwt;
  }

  /**
   * Extract realm from Keycloak JWT issuer
   */
  private String extractRealm(Jwt token) {
    String issuer = token.getIssuer().toString();
    // Format: https://admin.core-platform.local/realms/admin
    String[] parts = issuer.split("/realms/");
    if (parts.length > 1) {
      return parts[1];
    }
    return "admin"; // Fallback
  }
}
