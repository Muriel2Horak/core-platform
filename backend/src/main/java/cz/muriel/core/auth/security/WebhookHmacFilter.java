package cz.muriel.core.auth.security;

import cz.muriel.core.web.CachedBodyHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🔐 HMAC-SHA256 filter pro Keycloak webhook autentizaci Ověřuje X-KC-Signature
 * hlavičku proti raw body requestu
 */
@Component
public class WebhookHmacFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(WebhookHmacFilter.class);
  private static final String WEBHOOK_PATH = "/internal/keycloak/events";
  private static final Pattern SIGNATURE_PATTERN = Pattern.compile("^sha256=([0-9a-fA-F]{64})$");

  private final String hmacSecret;

  public WebhookHmacFilter(@Value("${keycloak.webhook.hmac-secret}") String hmacSecret) {
    this.hmacSecret = hmacSecret;
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    // Aktivní jen pro POST /internal/keycloak/events
    return !"POST".equalsIgnoreCase(request.getMethod())
        || !WEBHOOK_PATH.equals(request.getServletPath());
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull FilterChain chain)
      throws ServletException, IOException {

    logger.debug("Processing webhook HMAC validation for {} {}", request.getMethod(),
        request.getServletPath());

    // Načti celé tělo
    byte[] body = request.getInputStream().readAllBytes();
    logger.debug("Read request body: {} bytes", body.length);

    // Ověř HMAC header
    String signatureHeader = request.getHeader("X-KC-Signature");
    if (signatureHeader == null) {
      logger.warn("Missing X-KC-Signature header for webhook request");
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    Matcher matcher = SIGNATURE_PATTERN.matcher(signatureHeader.trim());
    if (!matcher.matches()) {
      logger.warn("Invalid signature format in X-KC-Signature header: {}", signatureHeader);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    // Spočítej a porovnej HMAC
    String expectedSignature = matcher.group(1).toLowerCase();
    String computedSignature = HmacUtils.computeHmacSha256(body, hmacSecret);

    boolean hmacValid = HmacUtils.slowEquals(expectedSignature, computedSignature);
    logger.debug("HMAC validation result: {}", hmacValid);

    if (!hmacValid) {
      logger.warn("HMAC signature mismatch for webhook request");
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    logger.debug("HMAC validation successful, proceeding with cached request");

    // Důležité: vytvoř wrapped request s cached body a pokračuj
    CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request, body);
    chain.doFilter(wrappedRequest, response);
  }
}
