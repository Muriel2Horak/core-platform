package cz.muriel.core.monitoring.grafana;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import cz.muriel.core.monitoring.grafana.entity.GrafanaTenantBinding;
import cz.muriel.core.monitoring.grafana.repository.GrafanaTenantBindingRepository;
import cz.muriel.core.test.AbstractIntegrationTest;
import cz.muriel.core.test.wiremock.WireMockExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🧪 Integration Tests for Grafana Provisioning Service
 * 
 * Tests the complete tenant lifecycle with Grafana integration: - Tenant
 * creation → Grafana org + service account creation - Tenant deletion → Grafana
 * org deletion + binding cleanup - Error handling when Grafana is unavailable -
 * Idempotency of provisioning operations
 * 
 * Uses WireMock to simulate Grafana API without Docker dependency. WireMock
 * runs on fixed port 8089 configured in application-test.yml.
 */
@Slf4j @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) @ExtendWith(WireMockExtension.class)
class GrafanaProvisioningServiceIT extends AbstractIntegrationTest {

  @Autowired
  private GrafanaProvisioningService provisioningService;

  @Autowired
  private GrafanaTenantBindingRepository bindingRepository;

  private final String testTenantId = "test-tenant-" + System.currentTimeMillis();

  @BeforeEach
  void setUp(WireMockServer wireMock) {
    log.info("\n");
    log.info("🧹 ═══════════════════════════════════════════════════");
    log.info("🧹  TEST SETUP - Cleaning existing test data");
    log.info("🧹 ═══════════════════════════════════════════════════");
    log.info("   ℹ️  WireMock running on port: {}", wireMock.port());
    // Clean up any existing test data
    bindingRepository.findByTenantId(testTenantId).ifPresent(binding -> {
      log.info("   ℹ️  Removing existing test binding for tenant: {}", testTenantId);
      bindingRepository.delete(binding);
    });
    log.info("✅ Setup complete\n");
  }

  @AfterEach
  void tearDown() {
    log.info("\n🧹 Cleanup: Removing test data...");
    // Clean up test data
    bindingRepository.findByTenantId(testTenantId).ifPresent(binding -> {
      bindingRepository.delete(binding);
      log.info("   ✓ Test binding removed");
    });
    log.info("");
  }

  @Test
  void provisionTenant_shouldCreateGrafanaOrgAndServiceAccount(WireMockServer wireMock) {
    log.info("\n📝 TEST 1/8: Provision Tenant - Create Grafana Org & Service Account");
    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

    // Given: Mock Grafana API responses
    long expectedOrgId = 42L;
    long expectedSaId = 123L;
    String expectedToken = "glsa_test_token_abc123";

    log.info("🔧 Step 1: Setting up WireMock stubs...");
    // Mock: Create organization
    wireMock.stubFor(WireMock.post("/api/orgs")
        .willReturn(WireMock.aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json").withBody(String
                .format("{\"orgId\": %d, \"message\": \"Organization created\"}", expectedOrgId))));
    log.info("   ✓ POST /api/orgs → orgId: {}", expectedOrgId);

    // Mock: Create service account (uses X-Grafana-Org-Id header)
    wireMock.stubFor(WireMock.post("/api/serviceaccounts")
        .withHeader("X-Grafana-Org-Id", WireMock.equalTo(String.valueOf(expectedOrgId)))
        .willReturn(WireMock.aResponse().withStatus(201)
            .withHeader("Content-Type", "application/json").withBody(
                String.format("{\"id\": %d, \"name\": \"sa-%s\"}", expectedSaId, testTenantId))));
    log.info("   ✓ POST /api/serviceaccounts → saId: {}", expectedSaId);

    // Mock: Create service account token (uses X-Grafana-Org-Id header)
    wireMock.stubFor(WireMock.post(String.format("/api/serviceaccounts/%d/tokens", expectedSaId))
        .withHeader("X-Grafana-Org-Id", WireMock.equalTo(String.valueOf(expectedOrgId))).willReturn(
            WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody(String.format("{\"key\": \"%s\"}", expectedToken))));
    log.info("   ✓ POST /api/serviceaccounts/{}/tokens → token: {}***", expectedSaId,
        expectedToken.substring(0, 10));

    // When: Provision tenant
    log.info("\n🚀 Step 2: Provisioning tenant '{}'...", testTenantId);
    GrafanaTenantBinding result = provisioningService.provisionTenant(testTenantId);
    log.info("   ✓ Provisioning completed");

    // Then: Verify binding created and returned
    log.info("\n🧪 Step 3: Verifying results...");
    assertThat(result).isNotNull();
    assertThat(result.getTenantId()).isEqualTo(testTenantId);
    log.info("   ✓ Binding returned from service");

    // Verify binding in database
    Optional<GrafanaTenantBinding> binding = bindingRepository.findByTenantId(testTenantId);
    assertThat(binding).isPresent();
    assertThat(binding.get().getTenantId()).isEqualTo(testTenantId);
    assertThat(binding.get().getGrafanaOrgId()).isEqualTo(expectedOrgId);
    assertThat(binding.get().getServiceAccountId()).isEqualTo(expectedSaId);
    assertThat(binding.get().getServiceAccountName()).isEqualTo("sa-" + testTenantId);
    assertThat(binding.get().getServiceAccountToken()).isEqualTo(expectedToken);

    log.info("   ✓ Binding saved to database");
    log.info("   ✓ Grafana Org ID: {}", expectedOrgId);
    log.info("   ✓ Service Account ID: {}", expectedSaId);
    log.info("   ✓ Service Account Name: sa-{}", testTenantId);
    log.info("   ✓ Token format validated");

    // Verify Grafana API was called correctly
    log.info("\n🔍 Step 4: Verifying API calls...");
    wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/api/orgs"))
        .withRequestBody(WireMock.containing("Tenant: " + testTenantId)));
    log.info("   ✓ Organization created with correct name");

    wireMock.verify(WireMock.postRequestedFor(WireMock.urlPathEqualTo("/api/serviceaccounts"))
        .withHeader("X-Grafana-Org-Id", WireMock.equalTo(String.valueOf(expectedOrgId)))
        .withRequestBody(WireMock.containing("sa-" + testTenantId)));
    log.info("   ✓ Service account created in correct org");

    wireMock.verify(WireMock
        .postRequestedFor(
            WireMock.urlPathEqualTo(String.format("/api/serviceaccounts/%d/tokens", expectedSaId)))
        .withHeader("X-Grafana-Org-Id", WireMock.equalTo(String.valueOf(expectedOrgId))));
    log.info("   ✓ Token created for service account");

    log.info("\n✅ TEST PASSED - All assertions successful!\n");
  }

  @Test
  void provisionTenant_shouldBeIdempotent_whenCalledMultipleTimes(WireMockServer wireMock) {
    log.info("\n📝 TEST 2/8: Idempotency - Multiple Provision Calls");
    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

    // Given: Mock Grafana API
    String tenant1 = "tenant-idempotent-" + System.currentTimeMillis();

    log.info("🔧 Step 1: Setting up WireMock stubs...");
    wireMock.stubFor(WireMock.post("/api/orgs").willReturn(
        WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody("{\"orgId\": 42, \"message\": \"Organization created\"}")));

    wireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/api/serviceaccounts")).willReturn(
        WireMock.aResponse().withStatus(201).withHeader("Content-Type", "application/json")
            .withBody("{\"id\": 123, \"name\": \"sa-test\"}")));

    wireMock.stubFor(WireMock.post(WireMock.urlPathMatching("/api/serviceaccounts/.*/tokens"))
        .willReturn(WireMock.aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json").withBody("{\"key\": \"glsa_token\"}")));
    log.info("   ✓ Stubs configured");

    // When: Provision same tenant twice
    log.info("\n🚀 Step 2: Provisioning tenant FIRST time...");
    provisioningService.provisionTenant(tenant1);
    long countAfterFirst = bindingRepository.count();
    log.info("   ✓ First provisioning completed - Binding count: {}", countAfterFirst);

    log.info("\n🔁 Step 3: Provisioning SAME tenant SECOND time...");
    provisioningService.provisionTenant(tenant1); // Should not create duplicate but return existing
    long countAfterSecond = bindingRepository.count();
    log.info("   ✓ Second provisioning completed - Binding count: {}", countAfterSecond);

    // Then: Only one binding should exist
    log.info("\n🧪 Step 4: Verifying idempotency...");
    assertThat(countAfterSecond).isEqualTo(countAfterFirst);
    log.info("   ✓ Binding count unchanged ({} == {})", countAfterSecond, countAfterFirst);

    Optional<GrafanaTenantBinding> binding = bindingRepository.findByTenantId(tenant1);
    assertThat(binding).isPresent();
    log.info("   ✓ Only one binding exists for tenant: {}", tenant1);

    log.info("\n✅ TEST PASSED - Idempotency verified!\n");

    // Cleanup
    bindingRepository.delete(binding.get());
  }

  @Test
  void provisionTenant_shouldHandleGrafanaUnavailable_gracefully(WireMockServer wireMock) {
    log.info("\n📝 TEST 3/8: Error Handling - Grafana Unavailable");
    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

    // Given: Mock Grafana API returning 503
    log.info("🔧 Step 1: Simulating Grafana unavailability (503 error)...");
    wireMock.stubFor(WireMock.post("/api/orgs").willReturn(
        WireMock.aResponse().withStatus(503).withBody("{\"error\": \"Service Unavailable\"}")));
    log.info("   ✓ WireMock configured to return HTTP 503");

    // When: Attempt to provision (should throw exception)
    log.info("\n🚀 Step 2: Attempting to provision tenant...");
    boolean exceptionCaught = false;
    try {
      provisioningService.provisionTenant(testTenantId);
    } catch (GrafanaProvisioningException e) {
      // Expected exception
      exceptionCaught = true;
      log.info("   ✓ Expected exception caught: {}", e.getMessage());
    }

    // Then: Binding should NOT be created
    log.info("\n🧪 Step 3: Verifying graceful failure...");
    assertThat(exceptionCaught).isTrue();
    log.info("   ✓ GrafanaProvisioningException was thrown");

    Optional<GrafanaTenantBinding> binding = bindingRepository.findByTenantId(testTenantId);
    assertThat(binding).isEmpty();
    log.info("   ✓ No binding created in database (rollback successful)");

    log.info("\n✅ TEST PASSED - Error handled gracefully!\n");
  }

  @Test
  void deprovisionTenant_shouldDeleteGrafanaOrg(WireMockServer wireMock) {
    log.info("\n📝 TEST 4/8: Deprovision Tenant - Delete Grafana Org");
    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

    // Given: Existing tenant binding
    log.info("🔧 Step 1: Creating test binding in database...");
    GrafanaTenantBinding binding = new GrafanaTenantBinding();
    binding.setTenantId(testTenantId);
    binding.setGrafanaOrgId(42L);
    binding.setServiceAccountId(123L);
    binding.setServiceAccountName("sa-" + testTenantId);
    binding.setServiceAccountToken("glsa_token");
    bindingRepository.saveAndFlush(binding);
    log.info("   ✓ Test binding created (orgId: 42, saId: 123)");

    // Mock: Delete organization
    log.info("\n🔧 Step 2: Setting up WireMock for DELETE /api/orgs/42...");
    wireMock.stubFor(WireMock.delete("/api/orgs/42").willReturn(
        WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody("{\"message\": \"Organization deleted\"}")));
    log.info("   ✓ Stub configured");

    // When: Deprovision tenant
    log.info("\n🗑️  Step 3: Deprovisioning tenant...");
    provisioningService.deprovisionTenant(testTenantId);
    log.info("   ✓ Deprovision completed");

    // Then: Binding should be deleted
    log.info("\n🧪 Step 4: Verifying cleanup...");
    Optional<GrafanaTenantBinding> deletedBinding = bindingRepository.findByTenantId(testTenantId);
    assertThat(deletedBinding).isEmpty();
    log.info("   ✓ Binding deleted from database");

    // Verify Grafana API was called
    wireMock.verify(WireMock.deleteRequestedFor(WireMock.urlEqualTo("/api/orgs/42")));
    log.info("   ✓ DELETE /api/orgs/42 was called");

    log.info("\n✅ TEST PASSED - Deprovisioning successful!\n");
  }

  @Test
  void deprovisionTenant_shouldHandleMissingBinding_gracefully() {
    log.info("\n📝 TEST 5/8: Deprovision - Handle Missing Binding");
    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

    // Given: No existing binding
    log.info("ℹ️  No setup needed - testing missing binding scenario");

    // When: Attempt to deprovision (should throw exception)
    log.info("\n🗑️  Step 1: Attempting to deprovision non-existent tenant...");
    boolean exceptionCaught = false;
    try {
      provisioningService.deprovisionTenant("non-existent-tenant");
    } catch (GrafanaProvisioningException e) {
      // Expected - binding not found
      exceptionCaught = true;
      log.info("   ✓ Expected exception caught: {}", e.getMessage());
    }

    // Then: No exception thrown for cleanup check
    log.info("\n🧪 Step 2: Verifying graceful handling...");
    assertThat(exceptionCaught).isTrue();
    log.info("   ✓ GrafanaProvisioningException thrown as expected");

    assertThat(bindingRepository.findByTenantId("non-existent-tenant")).isEmpty();
    log.info("   ✓ Database remains clean (no ghost records)");

    log.info("\n✅ TEST PASSED - Missing binding handled gracefully!\n");
  }

  @Test
  void getTenantBinding_shouldReturnTenantBinding() {
    log.info("\n📝 TEST 6/8: Get Tenant Binding - Return Existing");
    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

    // Given: Existing binding
    log.info("🔧 Step 1: Creating test binding...");
    GrafanaTenantBinding binding = new GrafanaTenantBinding();
    binding.setTenantId(testTenantId);
    binding.setGrafanaOrgId(42L);
    binding.setServiceAccountId(123L);
    binding.setServiceAccountName("sa-" + testTenantId);
    binding.setServiceAccountToken("glsa_secret_token");
    bindingRepository.save(binding);
    log.info("   ✓ Binding created with token: glsa_secret***");

    // When: Get binding via service
    log.info("\n🔍 Step 2: Retrieving binding via service...");
    GrafanaTenantBinding result = provisioningService.getTenantBinding(testTenantId);
    log.info("   ✓ Binding retrieved");

    // Then: Binding is returned
    log.info("\n🧪 Step 3: Verifying binding data...");
    assertThat(result).isNotNull();
    log.info("   ✓ Binding is not null");

    assertThat(result.getTenantId()).isEqualTo(testTenantId);
    log.info("   ✓ Tenant ID matches: {}", testTenantId);

    assertThat(result.getGrafanaOrgId()).isEqualTo(42L);
    log.info("   ✓ Grafana Org ID: 42");

    assertThat(result.getServiceAccountToken()).isEqualTo("glsa_secret_token");
    log.info("   ✓ Service Account Token verified");

    log.info("\n✅ TEST PASSED - Binding retrieval successful!\n");
  }

  @Test
  void getTenantBinding_shouldReturnNull_whenTenantNotProvisioned() {
    log.info("\n📝 TEST 7/8: Get Tenant Binding - Return Null for Non-Existent");
    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

    log.info("ℹ️  No setup needed - testing non-existent tenant");

    // When: Get binding for non-existent tenant via service
    log.info("\n🔍 Step 1: Retrieving binding for non-existent tenant...");
    GrafanaTenantBinding result = provisioningService.getTenantBinding("non-existent");
    log.info("   ✓ Query completed");

    // Then: Null result
    log.info("\n🧪 Step 2: Verifying null return...");
    assertThat(result).isNull();
    log.info("   ✓ Returned null (as expected)");

    log.info("\n✅ TEST PASSED - Null handling correct!\n");
  }

  @Test
  void provisionTenant_shouldGenerateUniqueServiceAccountNames(WireMockServer wireMock) {
    log.info("\n📝 TEST 8/8: Unique Service Account Names - Multi-Tenant");
    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

    // Given: Mock Grafana API
    log.info("🔧 Step 1: Setting up WireMock stubs...");
    wireMock.stubFor(WireMock.post("/api/orgs").willReturn(WireMock.aResponse().withStatus(200)
        .withHeader("Content-Type", "application/json").withBody("{\"orgId\": 100}")));

    wireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/api/serviceaccounts")).willReturn(
        WireMock.aResponse().withStatus(201).withHeader("Content-Type", "application/json")
            .withBody("{\"id\": 200, \"name\": \"sa-test\"}")));

    wireMock.stubFor(WireMock.post(WireMock.urlPathMatching("/api/serviceaccounts/.*/tokens"))
        .willReturn(WireMock.aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json").withBody("{\"key\": \"glsa_token\"}")));
    log.info("   ✓ Stubs configured");

    // When: Provision two different tenants
    String tenant1 = "tenant-1-" + System.currentTimeMillis();
    String tenant2 = "tenant-2-" + System.currentTimeMillis();

    log.info("\n🚀 Step 2: Provisioning first tenant '{}'...", tenant1);
    provisioningService.provisionTenant(tenant1);
    log.info("   ✓ First tenant provisioned");

    log.info("\n🚀 Step 3: Provisioning second tenant '{}'...", tenant2);
    provisioningService.provisionTenant(tenant2);
    log.info("   ✓ Second tenant provisioned");

    // Then: Service account names should be different
    log.info("\n🧪 Step 4: Verifying unique service account names...");
    Optional<GrafanaTenantBinding> binding1 = bindingRepository.findByTenantId(tenant1);
    Optional<GrafanaTenantBinding> binding2 = bindingRepository.findByTenantId(tenant2);

    assertThat(binding1).isPresent();
    assertThat(binding2).isPresent();
    log.info("   ✓ Both bindings found");

    String sa1 = binding1.get().getServiceAccountName();
    String sa2 = binding2.get().getServiceAccountName();

    assertThat(sa1).isNotEqualTo(sa2);
    log.info("   ✓ Service Account 1: {}", sa1);
    log.info("   ✓ Service Account 2: {}", sa2);
    log.info("   ✓ Names are unique (different)");

    log.info("\n✅ TEST PASSED - Unique naming verified!\n");

    // Cleanup
    bindingRepository.delete(binding1.get());
    bindingRepository.delete(binding2.get());
    log.info("🧹 Cleanup: Both test bindings removed\n");
  }
}
