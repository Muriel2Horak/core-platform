package cz.muriel.core.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.auth.dto.LoginRequest;
import cz.muriel.core.auth.dto.PasswordChangeRequest;
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
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import cz.muriel.core.auth.KeycloakClient;

@RestController
@RequestMapping("/api")
public class AuthController {
  private static final String ACCESS_COOKIE = "at";
  private static final String REFRESH_COOKIE = "rt";

  private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

  private final KeycloakClient kc;
  private final ObjectMapper om;
  private final JwtDecoder jwtDecoder;
  private final RestTemplate restTemplate;

  // Loki configuration
  private static final String LOKI_URL = "http://core-loki:3100/loki/api/v1/push";

  public AuthController(KeycloakClient kc, ObjectMapper om, JwtDecoder jwtDecoder, RestTemplate restTemplate) {
    this.kc = kc;
    this.om = om;
    this.jwtDecoder = jwtDecoder;
    this.restTemplate = restTemplate;
  }

  @PostMapping("/auth/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletResponse resp) {
    try {
      JsonNode tok = kc.tokenByPassword(req.getUsername(), req.getPassword());
      String access = tok.path("access_token").asText(null);
      String refresh = tok.path("refresh_token").asText(null);
      int expiresIn = tok.path("expires_in").asInt(300);
      if (access == null || refresh == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_credentials"));
      }
      setCookie(resp, ACCESS_COOKIE, access, Duration.ofSeconds(expiresIn));
      setCookie(resp, REFRESH_COOKIE, refresh, Duration.ofDays(7));

      Map<String, Object> userMap = new HashMap<>(basicUserFromJwt(access));
      try {
        JsonNode me = kc.userinfo(access);
        Map<String, Object> ui = om.convertValue(me, Map.class);
        if (ui != null)
          userMap.putAll(ui);
      } catch (Exception ignore) {
      }

      logger.info("Uživatel {} se úspěšně přihlásil v {}.", req.getUsername(), Instant.now());

      Map<String, Object> body = new HashMap<>();
      body.put("accessToken", access); // Přidání tokenu do odpovědi
      body.putAll(userMap);
      body.put("roles", rolesFromJwt(access));
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "login_failed", "detail", e.getMessage()));
    }
  }

  @GetMapping("/auth/me")
  public ResponseEntity<?> me(HttpServletRequest request) {
    try {
      String at = getCookie(request, ACCESS_COOKIE).orElse(null);
      if (at == null)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

      Map<String, Object> userMap = new HashMap<>(basicUserFromJwt(at));
      try {
        JsonNode me = kc.userinfo(at);
        Map<String, Object> ui = om.convertValue(me, Map.class);
        if (ui != null)
          userMap.putAll(ui);
      } catch (Exception ignore) {
      }

      Map<String, Object> body = new HashMap<>();
      body.putAll(userMap);
      body.put("roles", rolesFromJwt(at));
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
    }
  }

  @PostMapping("/auth/logout")
  public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
    try {
      String rt = getCookie(request, REFRESH_COOKIE).orElse(null);
      if (rt != null) {
        try {
          kc.logout(rt);
        } catch (Exception ignore) {
        }
      }
    } finally {
      clearCookie(response, ACCESS_COOKIE);
      clearCookie(response, REFRESH_COOKIE);
    }
    logger.info("Uživatel se úspěšně odhlásil v {}.", Instant.now());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/auth/change-password")
  public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest req, @AuthenticationPrincipal Jwt jwt) {
    try {
      String userId = jwt.getSubject();
      kc.changePassword(userId, req.getNewPassword());
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "change_failed", "detail", e.getMessage()));
    }
  }

  @PostMapping("/auth/session")
  public ResponseEntity<?> establishSession(@RequestHeader(value = "Authorization", required = false) String authHeader,
      HttpServletResponse resp) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "missing_bearer"));
    }
    String token = authHeader.substring(7);
    try {
      Jwt jwt = jwtDecoder.decode(token);
      Instant now = Instant.now();
      Instant exp = jwt.getExpiresAt();
      Duration maxAge = (exp != null && exp.isAfter(now)) ? Duration.between(now, exp) : Duration.ofMinutes(5);
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

  @GetMapping("/me")
  public Map<String, Object> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
    return Map.of(
        "username", jwt.getClaimAsString("preferred_username"),
        "email", jwt.getClaimAsString("email"),
        "roles", jwt.getClaimAsStringList("realm_access.roles"));
  }

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

  private static void setCookie(HttpServletResponse resp, String name, String value, Duration maxAge) {
    ResponseCookie cookie = ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(false)
        .path("/")
        .sameSite("Lax")
        .maxAge(maxAge)
        .build();
    resp.addHeader("Set-Cookie", cookie.toString());
  }

  private static void clearCookie(HttpServletResponse resp, String name) {
    ResponseCookie cookie = ResponseCookie.from(name, "")
        .httpOnly(true)
        .secure(false)
        .path("/")
        .sameSite("Lax")
        .maxAge(Duration.ZERO)
        .build();
    resp.addHeader("Set-Cookie", cookie.toString());
  }
}
