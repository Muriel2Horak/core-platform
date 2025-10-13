package cz.muriel.core.monitoring.bff.service;

import cz.muriel.core.monitoring.bff.model.TenantBinding;
import cz.muriel.core.monitoring.grafana.entity.GrafanaTenantBinding;
import cz.muriel.core.monitoring.grafana.repository.GrafanaTenantBindingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TenantOrgServiceImplTest {

  @Mock
  private GrafanaTenantBindingRepository bindingRepository;

  private TenantOrgServiceImpl tenantOrgService;

  @BeforeEach
  void setUp() {
    tenantOrgService = new TenantOrgServiceImpl(bindingRepository);

    // Setup mock bindings for core-platform (lenient to avoid
    // UnnecessaryStubbingException)
    GrafanaTenantBinding coreBinding = new GrafanaTenantBinding();
    coreBinding.setTenantId("core-platform");
    coreBinding.setGrafanaOrgId(1L);
    coreBinding.setServiceAccountToken("glsa_test_core_token");

    lenient().when(bindingRepository.findByTenantId("core-platform"))
        .thenReturn(Optional.of(coreBinding));

    // Setup mock bindings for test-tenant
    GrafanaTenantBinding testBinding = new GrafanaTenantBinding();
    testBinding.setTenantId("test-tenant");
    testBinding.setGrafanaOrgId(2L);
    testBinding.setServiceAccountToken("glsa_test_tenant_token");

    lenient().when(bindingRepository.findByTenantId("test-tenant"))
        .thenReturn(Optional.of(testBinding));

    // Setup empty binding for unknown tenants
    lenient().when(bindingRepository.findByTenantId("unknown-tenant")).thenReturn(Optional.empty());
  }

  @Test
  void resolve_shouldReturnCoreOrgForCoreAdmin() {
    // Given: JWT with tenant_id = core-platform
    Jwt jwt = createJwt("core-platform", "admin");

    // When
    TenantBinding binding = tenantOrgService.resolve(jwt);

    // Then
    assertThat(binding).isNotNull();
    assertThat(binding.tenantId()).isEqualTo("core-platform");
    assertThat(binding.orgId()).isEqualTo(1L);
    assertThat(binding.serviceAccountToken()).startsWith("glsa_");
  }

  @Test
  void resolve_shouldReturnTestTenantOrg() {
    // Given: JWT with tenant_id = test-tenant
    Jwt jwt = createJwt("test-tenant", "user");

    // When
    TenantBinding binding = tenantOrgService.resolve(jwt);

    // Then
    assertThat(binding).isNotNull();
    assertThat(binding.tenantId()).isEqualTo("test-tenant");
    assertThat(binding.orgId()).isEqualTo(2L);
    assertThat(binding.serviceAccountToken()).startsWith("glsa_");
  }

  @Test
  void resolve_shouldThrowExceptionForUnknownTenant() {
    // Given: JWT with unknown tenant
    Jwt jwt = createJwt("unknown-tenant", "user");

    // When/Then
    assertThatThrownBy(() -> tenantOrgService.resolve(jwt))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Grafana organization not configured for tenant: unknown-tenant");
  }

  @Test
  void resolve_shouldThrowExceptionForMissingTenantClaim() {
    // Given: JWT without tenant_id claim
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").claim("sub", "user123").build();

    // When/Then
    assertThatThrownBy(() -> tenantOrgService.resolve(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No tenant_id found in JWT token");
  }

  @Test
  void resolve_shouldFallbackToTenantClaimIfTenantIdMissing() {
    // Given: JWT with 'tenant' claim instead of 'tenant_id'
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").claim("sub", "user123")
        .claim("tenant", "test-tenant").build();

    // When
    TenantBinding binding = tenantOrgService.resolve(jwt);

    // Then
    assertThat(binding.tenantId()).isEqualTo("test-tenant");
    assertThat(binding.orgId()).isEqualTo(2L);
  }

  @Test
  void resolve_shouldExtractTenantFromRoles() {
    // Given: JWT with tenant in realm_access.roles claim
    // Note: TENANT_TEST_TENANT converts to test_tenant (with underscore)
    // So we need to add a binding for that format
    GrafanaTenantBinding underscoreBinding = new GrafanaTenantBinding();
    underscoreBinding.setTenantId("test_tenant");
    underscoreBinding.setGrafanaOrgId(3L);
    underscoreBinding.setServiceAccountToken("glsa_test_underscore_token");

    lenient().when(bindingRepository.findByTenantId("test_tenant"))
        .thenReturn(Optional.of(underscoreBinding));

    Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").claim("sub", "user123")
        .claim("realm_access",
            java.util.Map.of("roles", java.util.List.of("TENANT_TEST_TENANT", "ROLE_USER")))
        .build();

    // When
    TenantBinding binding = tenantOrgService.resolve(jwt);

    // Then
    assertThat(binding.tenantId()).isEqualTo("test_tenant");
  }

  // Helper method to create test JWTs
  private Jwt createJwt(String tenantId, String username) {
    return Jwt.withTokenValue("token").header("alg", "RS256").claim("sub", username)
        .claim("tenant_id", tenantId).claim("preferred_username", username).build();
  }
}
