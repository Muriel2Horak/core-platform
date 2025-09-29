package cz.muriel.core.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 🔐 DYNAMIC JWT DECODER - Podporuje různé issuers pro jednotlivé tenanty
 * 
 * Každý tenant má vlastní realm a tím pádem vlastní issuer: - core-platform:
 * https://core-platform.local/realms/core-platform - ivigee:
 * https://ivigee.core-platform.local/realms/ivigee - acme:
 * https://acme.core-platform.local/realms/acme
 */
@Component @Slf4j
public class DynamicJwtDecoder implements JwtDecoder {

  @Value("${DOMAIN:core-platform.local}")
  private String baseDomain;

  @Value("${security.oauth2.audience:api}")
  private String allowedAudience;

  // Cache decoderů pro jednotlivé tenanty
  private final Map<String, JwtDecoder> decoderCache = new ConcurrentHashMap<>();

  @Override
  public Jwt decode(String token) throws JwtException {
    // 1. Nejprve dekódujeme token bez validace issuer pro získání tenant info
    String[] chunks = token.split("\\.");
    if (chunks.length != 3) {
      throw new JwtException("Invalid JWT format");
    }

    try {
      // Dekódujeme payload bez validace
      String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(payload);

      // Extrahujeme tenant z tokenu
      String tenantKey = extractTenantFromPayload(jsonNode);
      if (tenantKey == null) {
        tenantKey = "core-platform"; // fallback
      }

      // 2. Získáme nebo vytvoříme decoder pro daný tenant
      JwtDecoder decoder = getOrCreateDecoderForTenant(tenantKey);

      // 3. Validujeme token pomocí správného decoderu
      return decoder.decode(token);

    } catch (Exception e) {
      log.error("Failed to decode JWT token", e);
      throw new JwtException("Failed to decode JWT", e);
    }
  }

  /**
   * Extrahuje tenant key z JWT payload
   */
  private String extractTenantFromPayload(com.fasterxml.jackson.databind.JsonNode payload) {
    // 1. Zkusíme tenant claim
    if (payload.has("tenant")) {
      return payload.get("tenant").asText();
    }

    // 2. Zkusíme extrahovat z issuer
    if (payload.has("iss")) {
      String issuer = payload.get("iss").asText();
      // Očekáváme: https://tenant.core-platform.local/realms/tenant
      if (issuer.contains("/realms/")) {
        String realmPart = issuer.substring(issuer.lastIndexOf("/realms/") + 8);
        if (!realmPart.equals("core-platform")) {
          return realmPart;
        }

        // Zkusíme extrahovat z hostname části
        if (issuer.startsWith("https://") && issuer.contains(".core-platform.local")) {
          String hostname = issuer.substring(8); // remove https://
          if (hostname.contains("/")) {
            hostname = hostname.substring(0, hostname.indexOf("/"));
          }
          String[] parts = hostname.split("\\.");
          if (parts.length >= 3 && !parts[0].equals("core-platform")) {
            return parts[0]; // tenant subdomain
          }
        }
      }
    }

    return null;
  }

  /**
   * Získá nebo vytvoří JWT decoder pro daný tenant
   */
  private JwtDecoder getOrCreateDecoderForTenant(String tenantKey) {
    return decoderCache.computeIfAbsent(tenantKey, this::createDecoderForTenant);
  }

  /**
   * Vytvoří JWT decoder pro specifický tenant
   */
  private JwtDecoder createDecoderForTenant(String tenantKey) {
    log.info("🔐 Creating JWT decoder for tenant: {}", tenantKey);

    // Sestavíme issuer URI pro daný tenant
    String issuerUri;
    if ("core-platform".equals(tenantKey)) {
      issuerUri = String.format("https://%s/realms/core-platform", baseDomain);
    } else {
      issuerUri = String.format("https://%s.%s/realms/%s", tenantKey, baseDomain, tenantKey);
    }

    // Sestavíme JWK Set URI (interní Docker komunikace)
    String jwkSetUri = String.format("http://keycloak:8080/realms/%s/protocol/openid-connect/certs",
        tenantKey);

    log.debug("📍 Tenant: {} -> Issuer: {}, JWK Set: {}", tenantKey, issuerUri, jwkSetUri);

    try {
      // Vytvoříme decoder
      NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

      // Přidáme validátory
      OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
          new JwtTimestampValidator(), new JwtIssuerValidator(issuerUri),
          new JwtAudienceValidator(allowedAudience));

      decoder.setJwtValidator(validator);

      log.info("✅ JWT decoder created successfully for tenant: {}", tenantKey);
      return decoder;

    } catch (Exception e) {
      log.error("❌ Failed to create JWT decoder for tenant: {}", tenantKey, e);
      throw new JwtException("Failed to create JWT decoder for tenant: " + tenantKey, e);
    }
  }

  /**
   * Vymaže cache - pro testing nebo refresh
   */
  public void clearCache() {
    decoderCache.clear();
    log.info("🧹 JWT decoder cache cleared");
  }

  /**
   * Vrátí počet cached decoderů
   */
  public int getCacheSize() {
    return decoderCache.size();
  }
}
