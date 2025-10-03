package cz.muriel.core.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 🔐 DYNAMIC JWT DECODER
 * 
 * Dynamicky konfiguruje JWT decoder podle tenant subdomény z HTTP requestu.
 * Každý tenant má vlastní realm a tím pádem vlastní issuer: - admin:
 * https://admin.core-platform.local/realms/admin - ivigee:
 * https://ivigee.core-platform.local/realms/ivigee - acme:
 * https://acme.core-platform.local/realms/acme
 * 
 * 🔧 FIX: Používá interní Docker síť pro stažení JWK Set, ale validuje externí
 * issuer
 */
@Component @Slf4j
public class DynamicJwtDecoder implements JwtDecoder {

  @Value("${DOMAIN:core-platform.local}")
  private String baseDomain;

  // 🔧 NEW: Internal Keycloak URL for JWK Set download
  @Value("${KEYCLOAK_INTERNAL_BASE_URL:https://keycloak:8443}")
  private String keycloakInternalBaseUrl;

  private final Map<String, JwtDecoder> decoderCache = new ConcurrentHashMap<>();

  @Override
  public Jwt decode(String token) throws JwtException {
    try {
      String tenantKey = extractTenantFromRequest();
      log.debug("🔐 Resolving JWT decoder for tenant: {}", tenantKey);

      JwtDecoder decoder = getOrCreateDecoder(tenantKey);
      return decoder.decode(token);

    } catch (Exception e) {
      log.error("🔐 JWT decoding failed", e);
      throw new JwtException("JWT decoding failed: " + e.getMessage());
    }
  }

  /**
   * 🔍 EXTRACT TENANT: Získá tenant klíč z aktuálního HTTP requestu
   */
  private String extractTenantFromRequest() {
    try {
      // Get current HTTP request via Spring context
      RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
      if (attrs instanceof ServletRequestAttributes) {
        HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();
        String tenantKey = extractTenantFromHostname(request.getServerName());
        log.debug("🌐 Extracted tenant from hostname {}: {}", request.getServerName(), tenantKey);
        return tenantKey;
      }

      log.warn("🌐 No HTTP request context available, using admin fallback");
      return "admin"; // fallback (renamed from core-platform)

    } catch (Exception e) {
      log.warn("🌐 Failed to extract tenant from request: {}", e.getMessage());
      return "admin"; // fallback (renamed from core-platform)
    }
  }

  /**
   * 🌐 EXTRACT TENANT FROM HOSTNAME: Unified tenant extraction
   */
  private String extractTenantFromHostname(String hostname) {
    if (hostname == null || hostname.isEmpty()) {
      return "admin"; // fallback (renamed from core-platform)
    }

    log.debug("🌐 Processing hostname: {}", hostname);

    // Direct hostname mapping - unified logic for all tenants
    if (hostname.contains(".")) {
      String[] parts = hostname.split("\\.");
      if (parts.length >= 3) {
        String subdomain = parts[0];
        log.debug("🎯 Extracted subdomain: {}", subdomain);
        return subdomain; // This will be "admin", "tenant1", "tenant2", etc.
      }
    }

    // Fallback for localhost or direct domain access
    return "admin"; // fallback (renamed from core-platform)
  }

  /**
   * 🔧 GET OR CREATE DECODER: Vytvoří nebo vrátí cached JWT decoder pro tenant
   */
  private JwtDecoder getOrCreateDecoder(String tenantKey) {
    return decoderCache.computeIfAbsent(tenantKey, this::createJwtDecoder);
  }

  /**
   * 🏗️ CREATE JWT DECODER: Vytvoří nový JWT decoder pro tenant 🔧 FIX: Používá
   * interní Keycloak URL pro JWK Set, ale validuje externí issuer
   */
  private JwtDecoder createJwtDecoder(String tenantKey) {
    // 1. External issuer URI (for token validation) - what's in the JWT token
    String expectedIssuer = String.format("https://%s.%s/realms/%s", tenantKey, baseDomain,
        tenantKey);

    // 2. Internal JWK Set URI (for key download) - Docker network
    String jwkSetUri = String.format("%s/realms/%s/protocol/openid-connect/certs",
        keycloakInternalBaseUrl, tenantKey);

    log.info("🔧 Creating JWT decoder for tenant: {}", tenantKey);
    log.info("   Expected issuer: {}", expectedIssuer);
    log.info("   JWK Set URI: {}", jwkSetUri);

    try {
      // 🔧 FIX: Use NimbusJwtDecoder with custom JWK Set URI and issuer validation
      NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
          .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
          .build();

      // Set the expected issuer for validation
      decoder.setJwtValidator(createJwtValidator(expectedIssuer));

      return decoder;

    } catch (Exception e) {
      log.error("🔧 Failed to create JWT decoder for tenant {}: {}", tenantKey, e.getMessage());
      throw new RuntimeException("Failed to create JWT decoder for tenant: " + tenantKey, e);
    }
  }

  /**
   * 🛡️ CREATE JWT VALIDATOR: Creates validator that checks issuer and expiration
   */
  private org.springframework.security.oauth2.core.OAuth2TokenValidator<Jwt> createJwtValidator(
      String expectedIssuer) {
    List<org.springframework.security.oauth2.core.OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();

    // Issuer validation
    validators.add(new JwtIssuerValidator(expectedIssuer));

    // Timestamp validation (not expired)
    validators.add(new JwtTimestampValidator());

    return new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
        validators);
  }
}
