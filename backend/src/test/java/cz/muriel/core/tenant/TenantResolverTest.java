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
import org.springframework.test.util.ReflectionTestUtils;

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
    ReflectionTestUtils.setField(tenantResolver, "tenantClaimName", "tenant");
  }

  @Test
  void shouldResolveTenantFromJwtClaim() {
    // Given
    Jwt jwt = createJwtWithTenantClaim(testTenantKey);
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // When
    String result = tenantResolver.resolveTenantKey();

    // Then
    assertThat(result).isEqualTo(testTenantKey);
  }

  @Test
  void shouldThrowExceptionWhenJwtClaimMissing() {
    // Given
    Jwt jwt = createJwtWithoutTenantClaim();
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
  void shouldHandleCustomTenantClaimName() {
    // Given
    ReflectionTestUtils.setField(tenantResolver, "tenantClaimName", "custom_tenant");
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256")
        .claim("custom_tenant", testTenantKey).claim("sub", "user123").issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600)).build();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // When
    String result = tenantResolver.resolveTenantKey();

    // Then
    assertThat(result).isEqualTo(testTenantKey);
  }

  private Jwt createJwtWithTenantClaim(String tenantKey) {
    return Jwt.withTokenValue("token").header("alg", "RS256").claim("tenant", tenantKey)
        .claim("sub", "user123").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }

  private Jwt createJwtWithoutTenantClaim() {
    return Jwt.withTokenValue("token").header("alg", "RS256").claim("sub", "user123")
        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build();
  }
}
