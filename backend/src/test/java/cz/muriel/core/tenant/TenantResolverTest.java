package cz.muriel.core.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantResolverTest {

  @Mock
  private SecurityContext securityContext;

  @InjectMocks
  private TenantResolver tenantResolver;

  private final String testTenantKey = "test-tenant";

  @BeforeEach
  void setUp() {
    SecurityContextHolder.setContext(securityContext);
    // No longer need to set tenantClaimName - it's extracted from JWT issuer/realm
  }

  @Test
  void shouldResolveTenantFromJwtIssuer() {
    // Given
    Jwt jwt = createJwtWithRealmInIssuer(testTenantKey);
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // When
    String result = tenantResolver.resolveTenantKey();

    // Then
    assertThat(result).isEqualTo(testTenantKey);
  }

  @Test
  void shouldThrowExceptionWhenRealmMissing() {
    // Given
    Jwt jwt = createJwtWithoutRealm();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // When & Then
    assertThatThrownBy(() -> tenantResolver.resolveTenantKey())
        .isInstanceOf(TenantNotFoundException.class)
        .hasMessage("Tenant could not be determined - access denied");
  }

  @Test
  void shouldThrowExceptionWhenNoAuthentication() {
    // Given
    when(securityContext.getAuthentication()).thenReturn(null);

    // When & Then
    assertThatThrownBy(() -> tenantResolver.resolveTenantKey())
        .isInstanceOf(TenantNotFoundException.class)
        .hasMessage("Tenant could not be determined - access denied");
  }

  @Test
  void shouldResolveTenantFromRealmClaim() {
    // Given - JWT with 'realm' claim as fallback
    Jwt jwt = createJwtWithRealmClaim(testTenantKey);
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // When
    String result = tenantResolver.resolveTenantKey();

    // Then
    assertThat(result).isEqualTo(testTenantKey);
  }

  private Jwt createJwtWithRealmInIssuer(String tenantKey) {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .claim("sub", "user123")
        .issuer("http://localhost:8081/realms/" + tenantKey)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }

  private Jwt createJwtWithRealmClaim(String tenantKey) {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .claim("sub", "user123")
        .claim("realm", tenantKey)
        .issuer("http://localhost:8081/auth")  // No realm in issuer
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }

  private Jwt createJwtWithoutRealm() {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .claim("sub", "user123")
        .issuer("http://localhost:8081/auth")  // No realm anywhere
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }
}
