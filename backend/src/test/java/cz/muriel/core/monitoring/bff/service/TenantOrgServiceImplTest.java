package cz.muriel.core.monitoring.bff.service;

import cz.muriel.core.monitoring.bff.model.TenantBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.*;

class TenantOrgServiceImplTest {

  private TenantOrgServiceImpl tenantOrgService;

  @BeforeEach
  void setUp() {
    // Set environment variables for test tokens
    System.setProperty("GRAFANA_SAT_CORE_PLATFORM", "glsa_test_core_token");
    System.setProperty("GRAFANA_SAT_TEST_TENANT", "glsa_test_tenant_token");

    // Initialize service (uses hardcoded dev mappings)
    tenantOrgService = new TenantOrgServiceImpl();
    tenantOrgService.init();
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
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Could not determine tenant ID from JWT");
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
  void resolve_shouldExtractTenantFromGroups() {
    // Given: JWT with tenant in groups claim
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").claim("sub", "user123")
        .claim("realm_access",
            java.util.Map.of("groups", java.util.List.of("TENANT_test-tenant", "ROLE_USER")))
        .build();

    // When
    TenantBinding binding = tenantOrgService.resolve(jwt);

    // Then
    assertThat(binding.tenantId()).isEqualTo("test-tenant");
  }

  // Helper method to create test JWTs
  private Jwt createJwt(String tenantId, String username) {
    return Jwt.withTokenValue("token").header("alg", "RS256").claim("sub", username)
        .claim("tenant_id", tenantId).claim("preferred_username", username).build();
  }
}
