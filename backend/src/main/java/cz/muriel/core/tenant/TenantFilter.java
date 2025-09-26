package cz.muriel.core.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component @RequiredArgsConstructor @Slf4j @Order(2) // After security filter
public class TenantFilter extends OncePerRequestFilter {

  private final TenantResolver tenantResolver;

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    try {
      // Only process authenticated requests
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated() && !isAnonymous(auth)) {
        String tenantKey = tenantResolver.resolveTenantKey();

        // Validate tenant exists
        tenantResolver.resolveTenantId(tenantKey);

        // Set tenant context
        TenantContext.setTenantKey(tenantKey);

        // Set MDC for logging
        MDC.put("tenant", tenantKey);

        log.debug("Tenant context established: {}", tenantKey);
      }

      filterChain.doFilter(request, response);

    } catch (TenantNotFoundException e) {
      log.error("Tenant validation failed: {}", e.getMessage());
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.getWriter()
          .write("{\"error\":\"Invalid tenant\",\"message\":\"" + e.getMessage() + "\"}");
      response.setContentType("application/json");
      return;
    } finally {
      // Always clean up
      TenantContext.clear();
      MDC.remove("tenant");
    }
  }

  private boolean isAnonymous(Authentication auth) {
    return auth.getAuthorities().stream()
        .anyMatch(grantedAuth -> "ROLE_ANONYMOUS".equals(grantedAuth.getAuthority()));
  }
}
