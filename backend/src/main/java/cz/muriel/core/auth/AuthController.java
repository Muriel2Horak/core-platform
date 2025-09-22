package cz.muriel.core.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  public AuthController(KeycloakClient kc, ObjectMapper om, JwtDecoder jwtDecoder) {
    this.kc = kc;
    this.om = om;
    this.jwtDecoder = jwtDecoder;
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
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "missing_bearer"));
    }
    String token = authHeader.substring(7);
    try {
      Jwt jwt = jwtDecoder.decode(token);
      Instant now = Instant.now();
      Instant exp = jwt.getExpiresAt();
      Duration maxAge = (exp != null && exp.isAfter(now)) ? Duration.between(now, exp)
          : Duration.ofMinutes(5);
      setCookie(resp, ACCESS_COOKIE, token, maxAge);

      Map<String, Object> userMap = new HashMap<>(basicUserFromJwt(token));
      try {
        JsonNode me = kc.userinfo(token);
        Map<String, Object> ui = om.convertValue(me, Map.class);
        if (ui != null)
          userMap.putAll(ui);
      } catch (Exception ignore) {
      }

      Map<String, Object> body = new HashMap<>();
      body.putAll(userMap);
      body.put("roles", rolesFromJwt(token));
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_token"));
    }
  }

  /**
   * 🔑 Alternative userinfo endpoint for direct JWT access
   */
  @GetMapping("/auth/userinfo")
  public Map<String, Object> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
    return Map.of("username", jwt.getClaimAsString("preferred_username"), "email",
        jwt.getClaimAsString("email"), "roles", jwt.getClaimAsStringList("realm_access.roles"));
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

  @SuppressWarnings("unchecked")
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
    ResponseCookie cookie = ResponseCookie.from(name, value).httpOnly(true).secure(false).path("/")
        .sameSite("Lax").maxAge(maxAge).build();
    resp.addHeader("Set-Cookie", cookie.toString());
  }

  private static void clearCookie(HttpServletResponse resp, String name) {
    ResponseCookie cookie = ResponseCookie.from(name, "").httpOnly(true).secure(false).path("/")
        .sameSite("Lax").maxAge(Duration.ZERO).build();
    resp.addHeader("Set-Cookie", cookie.toString());
  }
}
