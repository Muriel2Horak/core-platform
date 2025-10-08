package cz.muriel.core.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Filter to set tenant_id in database session for RLS
 */
@Slf4j @Component @Order(1)
public class TenantContextFilter implements Filter {

  private final DataSource dataSource;

  public TenantContextFilter(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth != null && auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String tenantId = jwt.getClaimAsString("tenant_id");

      if (tenantId != null) {
        try (Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SET LOCAL app.tenant_id = ?")) {

          stmt.setString(1, tenantId);
          stmt.execute();

          log.debug("Set tenant_id={} for request {}", tenantId, httpRequest.getRequestURI());

        } catch (Exception e) {
          log.error("Failed to set tenant context", e);
        }
      }
    }

    chain.doFilter(request, response);
  }
}
