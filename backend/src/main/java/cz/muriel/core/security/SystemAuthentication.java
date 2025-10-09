package cz.muriel.core.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * System-level authentication for internal operations like Keycloak sync that
 * should bypass normal permission checks.
 */
public class SystemAuthentication implements Authentication {

  private static final List<GrantedAuthority> AUTHORITIES = List
      .of(new SimpleGrantedAuthority("ROLE_SYSTEM"), new SimpleGrantedAuthority("SYSTEM"));

  private boolean authenticated = true;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return AUTHORITIES;
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getDetails() {
    return "System internal operation";
  }

  @Override
  public Object getPrincipal() {
    return "system";
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    this.authenticated = isAuthenticated;
  }

  @Override
  public String getName() {
    return "system";
  }
}
