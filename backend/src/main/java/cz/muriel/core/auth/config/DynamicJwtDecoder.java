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

/**
 * 🔐 DYNAMIC JWT DECODER
 * 
 * Dynamicky konfiguruje JWT decoder podle tenant subdomény z HTTP requestu.
 * Každý tenant má vlastní realm a tím pádem vlastní issuer: - admin:
 * https://admin.core-platform.local/realms/admin - ivigee:
 * https://ivigee.core-platform.local/realms/ivigee - acme:
 * https://acme.core-platform.local/realms/acme
 */
@Component @Slf4j
public class DynamicJwtDecoder implements JwtDecoder {

  @Value("${DOMAIN:core-platform.local}")
  private String baseDomain;

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
   * 🏗️ CREATE JWT DECODER: Vytvoří nový JWT decoder pro tenant
   */
  private JwtDecoder createJwtDecoder(String tenantKey) {
    // Unified issuer URI construction - no special cases
    String issuerUri = String.format("https://%s.%s/realms/%s", tenantKey, baseDomain, tenantKey);

    log.info("🔧 Creating JWT decoder for tenant: {} with issuer: {}", tenantKey, issuerUri);

    return JwtDecoders.fromIssuerLocation(issuerUri);
  }
}
