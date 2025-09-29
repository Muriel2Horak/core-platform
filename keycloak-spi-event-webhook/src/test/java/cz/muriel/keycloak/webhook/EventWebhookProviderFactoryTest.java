package cz.muriel.keycloak.webhook;

import org.junit.jupiter.api.Test;
import org.keycloak.Config;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EventWebhookProviderFactoryTest {

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
    assertEquals(6, result1.size());
    assertTrue(result1.contains("USER_CREATED"));
    assertTrue(result1.contains("USER_UPDATED"));
    assertTrue(result1.contains("USER_DELETED"));

    Set<String> result2 = factory.parseEnabledTypes("");
    assertEquals(6, result2.size());
    assertTrue(result2.contains("USER_CREATED"));
  }

  @Test
  void shouldTrimWhitespaceInEnabledTypes() {
    EventWebhookProviderFactory factory = new EventWebhookProviderFactory();

    String typesStr = " USER_CREATED , USER_UPDATED , USER_DELETED ";
    Set<String> result = factory.parseEnabledTypes(typesStr);

    assertEquals(3, result.size());
    assertTrue(result.contains("USER_CREATED"));
    assertTrue(result.contains("USER_UPDATED"));
    assertTrue(result.contains("USER_DELETED"));
  }

  @Test
  void shouldGetConfigValueWithFallback() {
    EventWebhookProviderFactory factory = new EventWebhookProviderFactory();

    // Mock scope that returns null for all values
    Config.Scope mockScope = new Config.Scope() {
      @Override
      public String get(String key) {
        return null;
      }

      @Override
      public String get(String key, String defaultValue) {
        return defaultValue;
      }

      @Override
      public String[] getArray(String key) {
        return new String[0];
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
    };

    // Test environment variable fallback
    String result = factory.getConfigValue(mockScope, "KC_EVENT_WEBHOOK_URL", "endpoint-url",
        "default");
    assertEquals("default", result);
  }
}
