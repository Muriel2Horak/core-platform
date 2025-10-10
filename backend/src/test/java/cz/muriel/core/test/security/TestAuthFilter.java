package cz.muriel.core.test.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Test Authentication Filter - Mock JWT authentication without Keycloak
 * 
 * Activated ONLY in 'test' profile. Reads HTTP header X-Test-Auth to create
 * SecurityContext with JwtAuthenticationToken.
 * 
 * Header format: tenant=TENANT_A;roles=ROLE_USER,ROLE_REPORT,ROLE_ADMIN
 * 
 * Example: X-Test-Auth: tenant=test-tenant;roles=ROLE_USER,ROLE_REPORT
 * 
 * This eliminates the need for Keycloak in tests while maintaining the same
 * authentication mechanism.
 */
@Slf4j @Component @Profile("test") @Order(1) // Execute before Spring Security filter chain
public class TestAuthFilter extends OncePerRequestFilter {

  private static final String TEST_AUTH_HEADER = "X-Test-Auth";
  private static final String DEFAULT_SUBJECT = "test-user";
  private static final String DEFAULT_TENANT = "test-tenant";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String testAuth = request.getHeader(TEST_AUTH_HEADER);

    if (testAuth != null && !testAuth.isEmpty()) {
      try {
        Map<String, String> authParams = parseAuthHeader(testAuth);

        String tenant = authParams.getOrDefault("tenant", DEFAULT_TENANT);
        String rolesStr = authParams.getOrDefault("roles", "ROLE_USER");
        String subject = authParams.getOrDefault("subject", DEFAULT_SUBJECT);

        List<GrantedAuthority> authorities = Arrays.stream(rolesStr.split(",")).map(String::trim)
            .filter(role -> !role.isEmpty()).map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        // Create JWT claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        claims.put("tenant", tenant);
        claims.put("tenant_id", tenant);
        claims.put("preferred_username", subject);
        claims.put("realm_access", Map.of("roles",
            authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList())));

        // Create mock JWT
        Jwt jwt = new Jwt("test-token", Instant.now(), Instant.now().plusSeconds(3600),
            Map.of("alg", "none", "typ", "JWT"), claims);

        // Create authentication token
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);

        // Set in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Test authentication set: tenant={}, roles={}, subject={}", tenant, rolesStr,
            subject);

      } catch (Exception e) {
        log.error("Failed to parse test auth header: {}", testAuth, e);
      }
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Parse header format: tenant=TENANT_A;roles=ROLE_USER,ROLE_ADMIN
   */
  private Map<String, String> parseAuthHeader(String header) {
    Map<String, String> params = new HashMap<>();

    String[] pairs = header.split(";");
    for (String pair : pairs) {
      String[] keyValue = pair.split("=", 2);
      if (keyValue.length == 2) {
        params.put(keyValue[0].trim(), keyValue[1].trim());
      }
    }

    return params;
  }
}
