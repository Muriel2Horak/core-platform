package cz.muriel.core.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.util.StringUtils;

public class CookieBearerTokenResolver implements BearerTokenResolver {
  private final String cookieName;

  public CookieBearerTokenResolver(String cookieName) {
    this.cookieName = cookieName;
  }

  @Override
  public String resolve(HttpServletRequest request) {
    String path = request.getRequestURI();
    // Nepokoušej se číst bearer token na auth endpointech
    if (path != null && path.startsWith("/api/auth/")) {
      return null;
    }
    // 1) Cookie
    if (request.getCookies() != null) {
      for (Cookie c : request.getCookies()) {
        if (cookieName.equals(c.getName()) && StringUtils.hasText(c.getValue())) {
          return c.getValue();
        }
      }
    }
    // 2) Authorization header
    String auth = request.getHeader("Authorization");
    if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
      return auth.substring(7);
    }
    return null;
  }
}
