package cz.muriel.core.test;

import cz.muriel.core.monitoring.grafana.entity.GrafanaTenantBinding;
import cz.muriel.core.monitoring.grafana.repository.GrafanaTenantBindingRepository;
import cz.muriel.core.tenant.TenantResolver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Optional;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * 🧪 MOCK TEST CONFIGURATION
 * 
 * Provides mock beans for testing without full database setup.
 */
@TestConfiguration
public class MockTestConfig {

  @Bean @Primary
  public GrafanaTenantBindingRepository mockGrafanaTenantBindingRepository() {
    GrafanaTenantBindingRepository mock = mock(GrafanaTenantBindingRepository.class);

    // Default bindings for common test tenants
    GrafanaTenantBinding corePlatformBinding = new GrafanaTenantBinding();
    corePlatformBinding.setId(1L);
    corePlatformBinding.setTenantId("core-platform");
    corePlatformBinding.setGrafanaOrgId(1L);
    corePlatformBinding.setServiceAccountId(1L);
    corePlatformBinding.setServiceAccountName("sa-core-platform");
    corePlatformBinding.setServiceAccountToken("test-token-core");

    GrafanaTenantBinding testTenantBinding = new GrafanaTenantBinding();
    testTenantBinding.setId(2L);
    testTenantBinding.setTenantId("test-tenant");
    testTenantBinding.setGrafanaOrgId(2L);
    testTenantBinding.setServiceAccountId(2L);
    testTenantBinding.setServiceAccountName("sa-test-tenant");
    testTenantBinding.setServiceAccountToken("test-token-t1");

    GrafanaTenantBinding testTenantUnderscoreBinding = new GrafanaTenantBinding();
    testTenantUnderscoreBinding.setId(3L);
    testTenantUnderscoreBinding.setTenantId("test_tenant");
    testTenantUnderscoreBinding.setGrafanaOrgId(3L);
    testTenantUnderscoreBinding.setServiceAccountId(3L);
    testTenantUnderscoreBinding.setServiceAccountName("sa-test-tenant-underscore");
    testTenantUnderscoreBinding.setServiceAccountToken("test-token-t1-underscore");

    GrafanaTenantBinding tenantABinding = new GrafanaTenantBinding();
    tenantABinding.setId(4L);
    tenantABinding.setTenantId("TENANT_A");
    tenantABinding.setGrafanaOrgId(4L);
    tenantABinding.setServiceAccountId(4L);
    tenantABinding.setServiceAccountName("sa-tenant-a");
    tenantABinding.setServiceAccountToken("test-token-tenant-a");

    // Setup lenient stubbing
    lenient().when(mock.findByTenantId("core-platform"))
        .thenReturn(Optional.of(corePlatformBinding));
    lenient().when(mock.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenantBinding));
    lenient().when(mock.findByTenantId("test_tenant"))
        .thenReturn(Optional.of(testTenantUnderscoreBinding));
    lenient().when(mock.findByTenantId("TENANT_A")).thenReturn(Optional.of(tenantABinding));
    lenient().when(mock.findByTenantId("unknown-tenant")).thenReturn(Optional.empty());

    lenient().when(mock.findByGrafanaOrgId(1L)).thenReturn(Optional.of(corePlatformBinding));
    lenient().when(mock.findByGrafanaOrgId(2L)).thenReturn(Optional.of(testTenantBinding));
    lenient().when(mock.findByGrafanaOrgId(3L))
        .thenReturn(Optional.of(testTenantUnderscoreBinding));
    lenient().when(mock.findByGrafanaOrgId(4L)).thenReturn(Optional.of(tenantABinding));

    lenient().when(mock.existsByTenantId("core-platform")).thenReturn(true);
    lenient().when(mock.existsByTenantId("test-tenant")).thenReturn(true);
    lenient().when(mock.existsByTenantId("test_tenant")).thenReturn(true);
    lenient().when(mock.existsByTenantId("TENANT_A")).thenReturn(true);
    lenient().when(mock.existsByTenantId("unknown-tenant")).thenReturn(false);

    return mock;
  }

  @Bean @Primary
  public TenantResolver mockTenantResolver() {
    TenantResolver mock = mock(TenantResolver.class);
    // Výchozí behavior - vrací "test-tenant"
    lenient().when(mock.resolveTenantKey()).thenReturn("test-tenant");
    return mock;
  }
}
