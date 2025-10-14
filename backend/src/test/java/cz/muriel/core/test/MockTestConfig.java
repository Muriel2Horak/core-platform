package cz.muriel.core.test;

import cz.muriel.core.monitoring.grafana.entity.GrafanaTenantBinding;
import cz.muriel.core.monitoring.grafana.repository.GrafanaTenantBindingRepository;
import cz.muriel.core.tenant.TenantResolver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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

    // In-memory storage for bindings created during tests
    ConcurrentHashMap<String, GrafanaTenantBinding> bindings = new ConcurrentHashMap<>();
    AtomicLong idGenerator = new AtomicLong(100L); // Start IDs from 100 to avoid conflicts

    // Default bindings for common test tenants
    GrafanaTenantBinding corePlatformBinding = new GrafanaTenantBinding();
    corePlatformBinding.setId(1L);
    corePlatformBinding.setTenantId("core-platform");
    corePlatformBinding.setGrafanaOrgId(1L);
    corePlatformBinding.setServiceAccountId(1L);
    corePlatformBinding.setServiceAccountName("sa-core-platform");
    corePlatformBinding.setServiceAccountToken("test-token-core");
    bindings.put("core-platform", corePlatformBinding);

    GrafanaTenantBinding testTenantBinding = new GrafanaTenantBinding();
    testTenantBinding.setId(2L);
    testTenantBinding.setTenantId("test-tenant");
    testTenantBinding.setGrafanaOrgId(2L);
    testTenantBinding.setServiceAccountId(2L);
    testTenantBinding.setServiceAccountName("sa-test-tenant");
    testTenantBinding.setServiceAccountToken("test-token-t1");
    bindings.put("test-tenant", testTenantBinding);

    GrafanaTenantBinding testTenantUnderscoreBinding = new GrafanaTenantBinding();
    testTenantUnderscoreBinding.setId(3L);
    testTenantUnderscoreBinding.setTenantId("test_tenant");
    testTenantUnderscoreBinding.setGrafanaOrgId(3L);
    testTenantUnderscoreBinding.setServiceAccountId(3L);
    testTenantUnderscoreBinding.setServiceAccountName("sa-test-tenant-underscore");
    testTenantUnderscoreBinding.setServiceAccountToken("test-token-t1-underscore");
    bindings.put("test_tenant", testTenantUnderscoreBinding);

    GrafanaTenantBinding tenantABinding = new GrafanaTenantBinding();
    tenantABinding.setId(4L);
    tenantABinding.setTenantId("TENANT_A");
    tenantABinding.setGrafanaOrgId(4L);
    tenantABinding.setServiceAccountId(4L);
    tenantABinding.setServiceAccountName("sa-tenant-a");
    tenantABinding.setServiceAccountToken("test-token-tenant-a");
    bindings.put("TENANT_A", tenantABinding);

    // Setup dynamic stubbing that uses in-memory map
    lenient().when(mock.findByTenantId(org.mockito.ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> Optional.ofNullable(bindings.get(invocation.getArgument(0))));

    lenient().when(mock.findByGrafanaOrgId(org.mockito.ArgumentMatchers.anyLong()))
        .thenAnswer(invocation -> bindings.values().stream()
            .filter(b -> b.getGrafanaOrgId().equals(invocation.getArgument(0))).findFirst());

    lenient().when(mock.existsByTenantId(org.mockito.ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> bindings.containsKey(invocation.getArgument(0)));

    // Mock save() and saveAndFlush() to store in memory and return the input entity
    lenient().when(mock.save(org.mockito.ArgumentMatchers.any(GrafanaTenantBinding.class)))
        .thenAnswer(invocation -> {
          GrafanaTenantBinding binding = invocation.getArgument(0);
          if (binding.getId() == null) {
            binding.setId(idGenerator.incrementAndGet());
          }
          bindings.put(binding.getTenantId(), binding);
          return binding;
        });

    lenient().when(mock.saveAndFlush(org.mockito.ArgumentMatchers.any(GrafanaTenantBinding.class)))
        .thenAnswer(invocation -> {
          GrafanaTenantBinding binding = invocation.getArgument(0);
          if (binding.getId() == null) {
            binding.setId(idGenerator.incrementAndGet());
          }
          bindings.put(binding.getTenantId(), binding);
          return binding;
        });

    // Mock deleteByTenantId() to remove from memory
    lenient().doAnswer(invocation -> {
      bindings.remove(invocation.getArgument(0));
      return null;
    }).when(mock).deleteByTenantId(org.mockito.ArgumentMatchers.anyString());

    lenient().doAnswer(invocation -> {
      GrafanaTenantBinding binding = invocation.getArgument(0);
      bindings.remove(binding.getTenantId());
      return null;
    }).when(mock).delete(org.mockito.ArgumentMatchers.any(GrafanaTenantBinding.class));

    return mock;
  }

  @Bean @Primary
  public TenantResolver mockTenantResolver() {
    TenantResolver mock = mock(TenantResolver.class);
    // Výchozí behavior - vrací "test-tenant"
    lenient().when(mock.resolveTenantKey()).thenReturn("test-tenant");
    return mock;
  }

  @Bean @Primary @SuppressWarnings("unchecked")
  public KafkaTemplate<String, String> mockKafkaTemplate() {
    return mock(KafkaTemplate.class);
  }
}
