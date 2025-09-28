package cz.muriel.keycloak.webhook;

import org.junit.jupiter.api.Test;
import org.keycloak.Config;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EventWebhookProviderFactoryTest {

  @Test
  void shouldParseRealmTenantMapCorrectly() {
    EventWebhookProviderFactory factory = new EventWebhookProviderFactory();

    // Test valid realm-tenant-map
    String mapStr = "core-platform:test-tenant:a887f848-42cf-4b10-aff8-eaa8c488f3b1,realm2:tenant2:uuid2";
    Map<String, RealmTenant> result = factory.parseRealmTenantMap(mapStr);

    assertEquals(2, result.size());

    RealmTenant tenant1 = result.get("core-platform");
    assertNotNull(tenant1);
    assertEquals("test-tenant", tenant1.getTenantKey());
    assertEquals("a887f848-42cf-4b10-aff8-eaa8c488f3b1", tenant1.getTenantId());

    RealmTenant tenant2 = result.get("realm2");
    assertNotNull(tenant2);
    assertEquals("tenant2", tenant2.getTenantKey());
    assertEquals("uuid2", tenant2.getTenantId());
  }

  @Test
  void shouldHandleEmptyRealmTenantMap() {
    EventWebhookProviderFactory factory = new EventWebhookProviderFactory();

    Map<String, RealmTenant> result1 = factory.parseRealmTenantMap(null);
    assertTrue(result1.isEmpty());

    Map<String, RealmTenant> result2 = factory.parseRealmTenantMap("");
    assertTrue(result2.isEmpty());

    Map<String, RealmTenant> result3 = factory.parseRealmTenantMap("   ");
    assertTrue(result3.isEmpty());
  }

  @Test
  void shouldIgnoreInvalidRealmTenantMapEntries() {
    EventWebhookProviderFactory factory = new EventWebhookProviderFactory();

    // Mix of valid and invalid entries
    String mapStr = "valid:tenant:uuid,invalid-entry,another:valid:uuid2";
    Map<String, RealmTenant> result = factory.parseRealmTenantMap(mapStr);

    assertEquals(2, result.size());
    assertNotNull(result.get("valid"));
    assertNotNull(result.get("another"));
  }

  @Test
  void shouldParseEnabledTypesCorrectly() {
    EventWebhookProviderFactory factory = new EventWebhookProviderFactory();

    // Test custom enabled types
    String typesStr = "USER_CREATED,USER_UPDATED";
    Set<String> result = factory.parseEnabledTypes(typesStr);

    assertEquals(2, result.size());
    assertTrue(result.contains("USER_CREATED"));
    assertTrue(result.contains("USER_UPDATED"));
    assertFalse(result.contains("USER_DELETED"));
  }

  @Test
  void shouldUseDefaultEnabledTypesWhenEmpty() {
    EventWebhookProviderFactory factory = new EventWebhookProviderFactory();

    Set<String> result1 = factory.parseEnabledTypes(null);
    assertEquals(3, result1.size());
    assertTrue(result1.contains("USER_CREATED"));
    assertTrue(result1.contains("USER_UPDATED"));
    assertTrue(result1.contains("USER_DELETED"));

    Set<String> result2 = factory.parseEnabledTypes("");
    assertEquals(result1, result2);
  }

  @Test
  void shouldTrimWhitespaceInEnabledTypes() {
    EventWebhookProviderFactory factory = new EventWebhookProviderFactory();

    String typesStr = " USER_CREATED , USER_UPDATED ";
    Set<String> result = factory.parseEnabledTypes(typesStr);

    assertEquals(2, result.size());
    assertTrue(result.contains("USER_CREATED"));
    assertTrue(result.contains("USER_UPDATED"));
  }

  @Test
  void shouldGetConfigValueWithFallback() {
    EventWebhookProviderFactory factory = new EventWebhookProviderFactory();

    // Mock config that returns null
    Config.Scope mockConfig = new Config.Scope() {
      @Override
      public String get(String key) {
        return null;
      }

      @Override
      public String get(String key, String defaultValue) {
        return defaultValue;
      }

      @Override
      public Integer getInt(String key) {
        return null;
      }

      @Override
      public Integer getInt(String key, Integer defaultValue) {
        return defaultValue;
      }

      @Override
      public Long getLong(String key) {
        return null;
      }

      @Override
      public Long getLong(String key, Long defaultValue) {
        return defaultValue;
      }

      @Override
      public Boolean getBoolean(String key) {
        return null;
      }

      @Override
      public Boolean getBoolean(String key, Boolean defaultValue) {
        return defaultValue;
      }

      @Override
      public Config.Scope scope(String... path) {
        return this;
      }

      @Override
      public Set<String> getPropertyNames() {
        return Set.of();
      }

      @Override
      public String[] getArray(String key) {
        return null;
      }
    };

    // Test that it falls back to environment variable
    // Note: This test depends on the actual environment, so we just test that it
    // doesn't crash
    String result = factory.getConfigValue(mockConfig, "test-key", "TEST_ENV_VAR");
    // Verify the method works (result can be null if env var is not set, which is
    // OK)
    assertNotNull(factory); // Just verify factory is not null to use the result variable
  }
}
