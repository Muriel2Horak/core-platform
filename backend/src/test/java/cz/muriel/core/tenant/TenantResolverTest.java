package cz.muriel.core.tenant;

import cz.muriel.core.entity.Tenant;
import cz.muriel.core.repository.TenantRepository;
import cz.muriel.core.cache.TenantCache;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantResolverTest {

  @Mock
  private TenantRepository tenantRepository;

  @Mock
  private TenantCache tenantCache;

  @Mock
  private SecurityContext securityContext;

  @InjectMocks
  private TenantResolver tenantResolver;

  private final UUID testTenantId = UUID.randomUUID();
  private final String testTenantKey = "test-tenant";
  private final String defaultTenantKey = "default-tenant";

  @BeforeEach
  void setUp() {
    SecurityContextHolder.setContext(securityContext);
    ReflectionTestUtils.setField(tenantResolver, "tenantClaimName", "tenant");
    ReflectionTestUtils.setField(tenantResolver, "defaultTenantKey", defaultTenantKey);
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
  void shouldUseDefaultWhenJwtClaimMissing() {
    // Given
    Jwt jwt = createJwtWithoutTenantClaim();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // When
    String result = tenantResolver.resolveTenantKey();

    // Then
    assertThat(result).isEqualTo(defaultTenantKey);
  }

  @Test
  void shouldUseDefaultWhenNoAuthentication() {
    // Given
    when(securityContext.getAuthentication()).thenReturn(null);

    // When
    String result = tenantResolver.resolveTenantKey();

    // Then
    assertThat(result).isEqualTo(defaultTenantKey);
  }

  @Test
  void shouldResolveTenantIdFromCache() {
    // Given
    Tenant cachedTenant = Tenant.builder().id(testTenantId).key(testTenantKey).name("Test Tenant")
        .realm("core-platform").build();
    when(tenantCache.get(testTenantKey)).thenReturn(Optional.of(cachedTenant));

    // When
    UUID result = tenantResolver.resolveTenantId(testTenantKey);

    // Then
    assertThat(result).isEqualTo(testTenantId);
    verify(tenantRepository, never()).findByKey(anyString());
  }

  @Test
  void shouldResolveTenantIdFromDatabaseAndCache() {
    // Given
    when(tenantCache.get(testTenantKey)).thenReturn(Optional.empty());

    Tenant tenant = Tenant.builder().id(testTenantId).key(testTenantKey).name("Test Tenant")
        .realm("core-platform").build();
    when(tenantRepository.findByKey(testTenantKey)).thenReturn(Optional.of(tenant));

    // When
    UUID result = tenantResolver.resolveTenantId(testTenantKey);

    // Then
    assertThat(result).isEqualTo(testTenantId);
    verify(tenantCache).put(testTenantKey, tenant);
  }

  @Test
  void shouldThrowExceptionWhenTenantNotFound() {
    // Given
    when(tenantCache.get(testTenantKey)).thenReturn(Optional.empty());
    when(tenantRepository.findByKey(testTenantKey)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> tenantResolver.resolveTenantId(testTenantKey))
        .isInstanceOf(TenantNotFoundException.class)
        .hasMessage("Tenant not found: " + testTenantKey);
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
