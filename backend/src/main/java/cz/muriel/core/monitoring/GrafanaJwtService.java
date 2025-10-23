package cz.muriel.core.monitoring;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import cz.muriel.core.monitoring.jwks.JwksKeyProvider;
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
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Service for minting short-lived Grafana JWT tokens from Keycloak identity
 * 
 * Security: - TTL: 300 seconds (5 min, matches Keycloak access token) - JTI
 * replay protection via Redis - RS256 signature (asymmetric, verified via BFF
 * JWKS) - Multi-realm support (realm-agnostic issuer) - Rate limiting in
 * controller
 */
@Service @Slf4j
public class GrafanaJwtService {

  private final StringRedisTemplate redisTemplate;
  private final GrafanaTenantRegistry tenantRegistry;
  private final JwksKeyProvider keyProvider;

  @Value("${grafana.jwt.ttl:300}")
  private int jwtTtl;

  @Value("${grafana.jwt.issuer:https://admin.core-platform.local/bff}")
  private String issuer;

  public GrafanaJwtService(StringRedisTemplate redisTemplate, GrafanaTenantRegistry tenantRegistry,
      JwksKeyProvider keyProvider) {
    this.redisTemplate = redisTemplate;
    this.tenantRegistry = tenantRegistry;
    this.keyProvider = keyProvider;
  }

  /**
   * Mint short-lived Grafana JWT from Keycloak authentication DEPRECATED: Use
   * mintGrafanaJwtFromKeycloakJwt() instead Kept for backward compatibility with
   * old endpoints
   */
  @Deprecated
  public String mintGrafanaJwt(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      throw new IllegalArgumentException("Authentication must be JwtAuthenticationToken");
    }

    Jwt kcToken = jwtAuth.getToken();
    return mintGrafanaJwtFromKeycloakJwt(kcToken);
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
   * 
   * Uses RS256 (asymmetric) signing - Grafana verifies via BFF JWKS endpoint
   */
  public String mintGrafanaJwtFromKeycloakJwt(Jwt kcToken) {
    try {
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

      // Generate JWT with RS256
      String jti = UUID.randomUUID().toString();
      Instant now = Instant.now();
      Instant expiry = now.plusSeconds(jwtTtl);

      // Build JWT header with kid
      JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyProvider.getKeyId())
          .build();

      // Build JWT claims
      JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(issuer).subject(username)
          .claim("email", email).claim("name", name != null ? name : username)
          .claim("preferred_username", username).claim("orgId", grafanaOrgId)
          .claim("role", grafanaRole).issueTime(Date.from(now)).expirationTime(Date.from(expiry))
          .jwtID(jti).build();

      // Sign JWT with RS256
      SignedJWT signedJWT = new SignedJWT(header, claims);
      JWSSigner signer = new RSASSASigner(keyProvider.getRsaKey());
      signedJWT.sign(signer);

      String jwt = signedJWT.serialize();

      // Store JTI for replay protection
      redisTemplate.opsForValue().set("grafana:jti:" + jti, "used", jwtTtl, TimeUnit.SECONDS);

      log.debug(
          "✅ Minted RS256 Grafana JWT for user={}, tenant={}, orgId={}, role={}, ttl={}s, kid={}",
          username, tenantId, grafanaOrgId, grafanaRole, jwtTtl, keyProvider.getKeyId());

      return jwt;

    } catch (Exception e) {
      log.error("❌ Failed to mint Grafana JWT: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to mint Grafana JWT", e);
    }
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
