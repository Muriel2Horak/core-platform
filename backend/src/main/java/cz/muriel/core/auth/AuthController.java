package cz.muriel.core.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.dto.UserDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * 🔐 Clean Keycloak Auth Controller Obsahuje pouze endpointy pro komunikaci s
 * Keycloak - žádné hybridní API
 */
@RestController @RequestMapping("/api")
public class AuthController {
  private static final String ACCESS_COOKIE = "at";
  private static final String REFRESH_COOKIE = "rt";

  private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

  private final KeycloakClient kc;
  private final ObjectMapper om;
  private final JwtDecoder jwtDecoder;
  private final KeycloakAdminService keycloakAdminService;

  public AuthController(KeycloakClient kc, ObjectMapper om, JwtDecoder jwtDecoder,
      KeycloakAdminService keycloakAdminService) {
    this.kc = kc;
    this.om = om;
    this.jwtDecoder = jwtDecoder;
    this.keycloakAdminService = keycloakAdminService;
  }

  /**
   * 🔑 Logout endpoint - odhlásí uživatele z Keycloak
   */
  @PostMapping("/auth/logout")
  public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
    String userId = "unknown";
    try {
      String at = getCookie(request, ACCESS_COOKIE).orElse(null);
      if (at != null) {
        try {
          Jwt jwt = jwtDecoder.decode(at);
          userId = jwt.getClaimAsString("preferred_username");
        } catch (Exception ignore) {
        }
      }

      String rt = getCookie(request, REFRESH_COOKIE).orElse(null);
      if (rt != null) {
        try {
          kc.logout(rt);
        } catch (Exception ignore) {
        }
      }

      auditLogger.info("LOGOUT_SUCCESS: User {} successfully logged out", userId);
    } finally {
      clearCookie(response, ACCESS_COOKIE);
      clearCookie(response, REFRESH_COOKIE);
    }
    return ResponseEntity.noContent().build();
  }

  /**
   * 🔑 Session endpoint - vytvoří session z Keycloak tokenu Používá se po
   * Keycloak redirectu pro vytvoření cookies
   */
  @PostMapping("/auth/session")
  public ResponseEntity<?> establishSession(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      HttpServletResponse resp) {

    auditLogger.info("🔄 SESSION_REQUEST: New session request");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      auditLogger.warn("❌ SESSION_FAILED: Missing or invalid Authorization header");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "missing_bearer"));
    }

    String token = authHeader.substring(7);
    auditLogger.debug("🎫 SESSION_TOKEN: Extracted token with length: {}", token.length());

    try {
      auditLogger.debug("🔍 SESSION_JWT_DECODE: Decoding JWT token...");
      Jwt jwt = jwtDecoder.decode(token);

      auditLogger.info("✅ SESSION_JWT_DECODED: JWT successfully decoded for subject: {}",
          jwt.getSubject());

      Instant now = Instant.now();
      Instant exp = jwt.getExpiresAt();
      Duration maxAge = (exp != null && exp.isAfter(now)) ? Duration.between(now, exp)
          : Duration.ofMinutes(5);

      auditLogger.debug("🍪 SESSION_COOKIE: Setting access token cookie with maxAge: {} seconds",
          maxAge.getSeconds());
      setCookie(resp, ACCESS_COOKIE, token, maxAge);

      Map<String, Object> userMap = new HashMap<>(basicUserFromJwt(token));

      auditLogger.debug("👤 SESSION_USERINFO: Getting user info from Keycloak...");

      try {
        JsonNode me = kc.userinfo(token);
        Map<String, Object> ui = om.convertValue(me,
            new TypeReference<Map<String, Object>>() {});
        if (ui != null) {
          userMap.putAll(ui);
          auditLogger.debug("✅ SESSION_KEYCLOAK_USERINFO: Keycloak userinfo retrieved");
        }
      } catch (Exception e) {
        auditLogger.warn("⚠️ SESSION_KEYCLOAK_USERINFO_FAILED: Failed to get Keycloak userinfo: {}",
            e.getMessage());
      }

      Map<String, Object> body = new HashMap<>();
      body.putAll(userMap);
      body.put("roles", rolesFromJwt(token));

      auditLogger.info("✅ SESSION_SUCCESS: Session established successfully for user: {}",
          body.get("username"));

      return ResponseEntity.ok(body);
    } catch (Exception e) {
      auditLogger.error("❌ SESSION_FAILED: Failed to establish session: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_token"));
    }
  }

  /**
   * 🔑 Alternative userinfo endpoint for direct JWT access
   */
  @GetMapping("/auth/userinfo")
  public Map<String, Object> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
    auditLogger.info("👤 USERINFO_REQUEST: User info requested");

    // 🔧 NULL CHECK: Prevent 500 error when JWT is null
    if (jwt == null) {
      auditLogger.error("❌ USERINFO_FAILED: JWT token is null");
      throw new org.springframework.security.access.AccessDeniedException(
          "No valid JWT token found");
    }

    auditLogger.debug("🔍 USERINFO_JWT: Processing JWT token for subject: {}", jwt.getSubject());

    try {
      Map<String, Object> userInfo = new HashMap<>();

      // Basic user info
      userInfo.put("username", jwt.getClaimAsString("preferred_username"));
      userInfo.put("email", jwt.getClaimAsString("email"));
      userInfo.put("name", jwt.getClaimAsString("name"));
      userInfo.put("firstName", jwt.getClaimAsString("given_name"));
      userInfo.put("lastName", jwt.getClaimAsString("family_name"));

      // ✅ NOVÉ: Přidání dalších užitečných informací
      userInfo.put("id", jwt.getSubject()); // Uživatelské ID
      userInfo.put("email_verified", jwt.getClaimAsBoolean("email_verified"));
      userInfo.put("session_state", jwt.getClaimAsString("session_state"));
      userInfo.put("session_id", jwt.getClaimAsString("sid"));

      // Token timing info
      Instant expiresAt = jwt.getExpiresAt();
      if (expiresAt != null) {
        userInfo.put("token_expires_at", expiresAt.getEpochSecond());
      }
      Instant issuedAt = jwt.getIssuedAt();
      if (issuedAt != null) {
        userInfo.put("token_issued_at", issuedAt.getEpochSecond());
      }

      auditLogger.debug("📋 USERINFO_BASIC: Basic user info extracted");

      // Extract roles properly
      List<String> roles = new ArrayList<>();
      Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
      if (realmAccess != null && realmAccess.get("roles") instanceof List<?> realmRoles) {
        for (Object role : realmRoles) {
          if (role instanceof String roleName) {
            roles.add(roleName);
          }
        }
        auditLogger.debug("🎭 USERINFO_ROLES: Extracted {} roles from realm_access", roles.size());
      } else {
        auditLogger.warn("⚠️ USERINFO_ROLES: No realm_access roles found");
      }
      userInfo.put("roles", roles);

      // Extract tenant from JWT - either from direct claim or from issuer (realm)
      String tenant = extractTenantFromJwt(jwt);
      if (tenant != null && !tenant.isEmpty()) {
        userInfo.put("tenant", tenant);
        auditLogger.debug("✅ USERINFO_TENANT: Tenant extracted: {}", tenant);
      }

      // ✅ NOVÉ: Načtení kompletních organizačních atributů z Keycloak
      try {
        String userId = jwt.getSubject();
        if (userId != null) {
          // Použijeme KeycloakAdminService pro získání kompletních dat
          UserDto fullUserData = keycloakAdminService.getUserById(userId);
          if (fullUserData != null) {
            // 🏢 Organizační struktura
            if (fullUserData.getDepartment() != null)
              userInfo.put("department", fullUserData.getDepartment());
            if (fullUserData.getPosition() != null)
              userInfo.put("position", fullUserData.getPosition());
            if (fullUserData.getManager() != null)
              userInfo.put("manager", fullUserData.getManager());
            if (fullUserData.getManagerName() != null)
              userInfo.put("managerName", fullUserData.getManagerName());
            if (fullUserData.getCostCenter() != null)
              userInfo.put("costCenter", fullUserData.getCostCenter());
            if (fullUserData.getLocation() != null)
              userInfo.put("location", fullUserData.getLocation());
            if (fullUserData.getPhone() != null)
              userInfo.put("phone", fullUserData.getPhone());

            // 👥 Zástupství
            if (fullUserData.getDeputy() != null)
              userInfo.put("deputy", fullUserData.getDeputy());
            if (fullUserData.getDeputyName() != null)
              userInfo.put("deputyName", fullUserData.getDeputyName());
            if (fullUserData.getDeputyFrom() != null)
              userInfo.put("deputyFrom", fullUserData.getDeputyFrom());
            if (fullUserData.getDeputyTo() != null)
              userInfo.put("deputyTo", fullUserData.getDeputyTo());
            if (fullUserData.getDeputyReason() != null)
              userInfo.put("deputyReason", fullUserData.getDeputyReason());

            // 📷 Profilová fotka
            if (fullUserData.getProfilePicture() != null)
              userInfo.put("profilePicture", fullUserData.getProfilePicture());

            auditLogger.debug("✅ USERINFO_EXTENDED: Extended organizational attributes loaded");
          }
        }
      } catch (Exception e) {
        auditLogger.warn("⚠️ USERINFO_EXTENDED_FAILED: Failed to load extended user data: {}",
            e.getMessage());
        // Continue without extended data - basic info is still available
      }

      auditLogger.info("✅ USERINFO_SUCCESS: User info retrieved successfully");

      return userInfo;
    } catch (Exception e) {
      // Log the error for debugging
      auditLogger.error("❌ USERINFO_FAILED: Error getting user info from JWT: {}", e.getMessage(),
          e);

      // 🔧 SAFE FALLBACK: Only try to extract data if JWT is not null
      if (jwt != null) {
        try {
          auditLogger.debug("🔄 USERINFO_FALLBACK: Attempting fallback user info extraction...");
          Map<String, Object> fallback = new HashMap<>();
          fallback.put("username", jwt.getClaimAsString("preferred_username"));
          fallback.put("email", jwt.getClaimAsString("email"));
          fallback.put("roles", List.of());

          // Try to extract tenant even in error case
          String tenant = extractTenantFromJwt(jwt);
          if (tenant != null && !tenant.isEmpty()) {
            fallback.put("tenant", tenant);
          }

          auditLogger.info("✅ USERINFO_FALLBACK_SUCCESS: Fallback user info returned");
          return fallback;
        } catch (Exception ignored) {
          auditLogger.error("❌ USERINFO_FALLBACK_FAILED: Even fallback extraction failed");
        }
      }

      // Final fallback - throw proper authentication exception
      auditLogger
          .error("💥 USERINFO_FINAL_FAILURE: All attempts failed, throwing AccessDeniedException");
      throw new org.springframework.security.access.AccessDeniedException(
          "Failed to extract user info from JWT");
    }
  }

  /**
   * 📝 Frontend logging endpoint - receives logs from frontend with tenant
   * context
   */
  @PostMapping("/auth/logs")
  public ResponseEntity<?> receiveFrontendLogs(@RequestBody Map<String, Object> logData,
      @AuthenticationPrincipal Jwt jwt) {

    try {
      String level = (String) logData.get("level");
      String message = (String) logData.get("message");
      Object dataObj = logData.get("data");

      // Safe cast for data object
      Map<String, Object> data = null;
      if (dataObj instanceof Map<?, ?> map) {
        data = coerceMap(map);
      }

      // Log based on level - MDC context will automatically include tenant info
      switch (level != null ? level.toUpperCase() : "INFO") {
      case "ERROR":
        auditLogger.error("🌐 FRONTEND_ERROR: {} - {}", message, data);
        break;
      case "WARN":
        auditLogger.warn("🌐 FRONTEND_WARN: {} - {}", message, data);
        break;
      case "DEBUG":
        auditLogger.debug("🌐 FRONTEND_DEBUG: {} - {}", message, data);
        break;
      default:
        auditLogger.info("🌐 FRONTEND_INFO: {} - {}", message, data);
      }

      return ResponseEntity.ok(Map.of("status", "logged"));

    } catch (Exception e) {
      auditLogger.error("❌ FRONTEND_LOG_FAILED: Failed to process frontend log: {}",
          e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "Failed to process log"));
    }
  }

  // Utility methods
  private Map<String, Object> basicUserFromJwt(String jwtStr) {
    try {
      Jwt jwt = jwtDecoder.decode(jwtStr);
      Map<String, Object> m = new HashMap<>();
      Object pu = jwt.getClaim("preferred_username");
      Object name = jwt.getClaim("name");
      Object email = jwt.getClaim("email");
      Object given = jwt.getClaim("given_name");
      Object family = jwt.getClaim("family_name");
      if (pu != null)
        m.put("username", pu);
      if (name != null)
        m.put("name", name);
      if (email != null)
        m.put("email", email);
      if (given != null)
        m.put("firstName", given);
      if (family != null)
        m.put("lastName", family);
      return m;
    } catch (Exception e) {
      return Map.of();
    }
  }

  private List<String> rolesFromJwt(String jwtStr) {
    try {
      Jwt jwt = jwtDecoder.decode(jwtStr);
      Map<String, Object> realmAccess = jwt.getClaim("realm_access");
      if (realmAccess == null)
        return List.of();
      Object roles = realmAccess.get("roles");
      if (roles instanceof Collection<?> col) {
        List<String> list = new ArrayList<>();
        for (Object o : col)
          list.add(String.valueOf(o));
        return list;
      }
    } catch (Exception ignore) {
    }
    return List.of();
  }

  private static Optional<String> getCookie(HttpServletRequest req, String name) {
    if (req.getCookies() == null)
      return Optional.empty();
    for (Cookie c : req.getCookies())
      if (name.equals(c.getName()))
        return Optional.ofNullable(c.getValue());
    return Optional.empty();
  }

  private static void setCookie(HttpServletResponse resp, String name, String value,
      Duration maxAge) {
    // CRITICAL: Cookie must work for iframe cross-site requests
    // - httpOnly: prevents XSS attacks
    // - secure: HTTPS only (set to true in production)
    // - sameSite=None: allows cross-site iframe embedding
    // (admin.core-platform.local -> core-platform.local)
    // - domain=.core-platform.local: wildcard for all subdomains
    ResponseCookie cookie = ResponseCookie.from(name, value).httpOnly(true).secure(true) // Changed
                                                                                         // from
                                                                                         // false -
                                                                                         // HTTPS
                                                                                         // required
        .path("/").sameSite("None") // Changed from Lax - allows iframe embedding
        .domain(".core-platform.local") // NEW - wildcard domain
        .maxAge(maxAge).build();
    resp.addHeader("Set-Cookie", cookie.toString());
  }

  private static void clearCookie(HttpServletResponse resp, String name) {
    ResponseCookie cookie = ResponseCookie.from(name, "").httpOnly(true).secure(true) // Changed
                                                                                      // from false
        .path("/").sameSite("None") // Changed from Lax
        .domain(".core-platform.local") // NEW - wildcard domain
        .maxAge(Duration.ZERO).build();
    resp.addHeader("Set-Cookie", cookie.toString());
  }

  private Map<String, Object> coerceMap(Map<?, ?> source) {
    Map<String, Object> result = new HashMap<>();
    for (Map.Entry<?, ?> entry : source.entrySet()) {
      String key = entry.getKey() != null ? entry.getKey().toString() : "null";
      result.put(key, entry.getValue());
    }
    return result;
  }

  // Extract tenant from JWT helper method
  private String extractTenantFromJwt(Jwt jwt) {
    if (jwt == null)
      return "unknown";

    String tenant = jwt.getClaimAsString("tenant");
    if (tenant == null || tenant.isEmpty()) {
      String issuer = jwt.getClaimAsString("iss");
      if (issuer != null && issuer.contains("/realms/")) {
        tenant = issuer.substring(issuer.lastIndexOf("/realms/") + 8);
        if (tenant.contains("/")) {
          tenant = tenant.substring(0, tenant.indexOf("/"));
        }
        if (tenant.contains("?")) {
          tenant = tenant.substring(0, tenant.indexOf("?"));
        }
      }
    }
    return tenant != null && !tenant.isEmpty() ? tenant : "unknown";
  }
}
