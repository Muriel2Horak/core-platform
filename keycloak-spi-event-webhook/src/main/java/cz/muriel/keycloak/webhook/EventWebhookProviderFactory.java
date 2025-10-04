package cz.muriel.keycloak.webhook;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.HashSet;

public class EventWebhookProviderFactory implements EventListenerProviderFactory {

  private static final Logger logger = Logger.getLogger(EventWebhookProviderFactory.class);

  private String endpointUrl;
  private String secret;
  private Set<String> enabledTypes;

  // Default event types to process
  private static final Set<String> DEFAULT_ENABLED_TYPES = Set.of("USER_CREATED", "USER_UPDATED",
      "USER_DELETED", "UPDATE_PROFILE", "REGISTER", "LOGIN", "LOGOUT", "ROLE_CREATED",
      "ROLE_UPDATED", "ROLE_DELETED");

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return new EventWebhookProvider(session, endpointUrl, secret, enabledTypes);
  }

  @Override
  public void init(Config.Scope scope) {
    logger.info("🔧 Initializing EventWebhookProvider (SIMPLIFIED)...");

    // Get webhook configuration
    this.endpointUrl = getConfigValue(scope, "KC_EVENT_WEBHOOK_URL", "endpoint-url", null);
    this.secret = getConfigValue(scope, "KC_EVENT_WEBHOOK_SECRET", "secret",
        "webhook-secret-change-me");

    // Parse enabled event types
    String enabledTypesStr = getConfigValue(scope, "KC_ENABLED_TYPES", "enabled-types", null);
    this.enabledTypes = parseEnabledTypes(enabledTypesStr);

    logger.infof("✅ EventWebhookProvider initialized:");
    logger.infof("   📍 Endpoint URL: %s", endpointUrl != null ? endpointUrl : "NOT CONFIGURED");
    logger.infof("   🔐 Secret configured: %s", secret != null ? "YES" : "NO");
    logger.infof("   📋 Enabled types: %s", enabledTypes);
    logger.infof("   🎯 SIMPLIFIED: Using realm name as tenant key (no mapping needed)");

    if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
      logger.warn("⚠️  Webhook endpoint URL not configured - webhooks will be skipped");
    }
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    // No post-initialization needed
  }

  @Override
  public void close() {
    logger.info("🔒 EventWebhookProviderFactory closed");
  }

  @Override
  public String getId() {
    return "muriel-webhook";
  }

  // Helper method to get configuration values with fallbacks
  String getConfigValue(Config.Scope scope, String envVar, String configKey, String defaultValue) {
    // Try environment variable first
    String envValue = System.getenv(envVar);
    if (envValue != null && !envValue.trim().isEmpty()) {
      return envValue.trim();
    }

    // Try Keycloak config
    String configValue = scope.get(configKey);
    if (configValue != null && !configValue.trim().isEmpty()) {
      return configValue.trim();
    }

    // Return default
    return defaultValue;
  }

  // Parse enabled event types
  Set<String> parseEnabledTypes(String typesStr) {
    if (typesStr == null || typesStr.trim().isEmpty()) {
      logger.debug("No custom enabled types configured, using defaults");
      return new HashSet<>(DEFAULT_ENABLED_TYPES);
    }

    Set<String> result = new HashSet<>();
    try {
      String[] types = typesStr.split(",");
      for (String type : types) {
        String trimmed = type.trim().toUpperCase();
        if (!trimmed.isEmpty()) {
          result.add(trimmed);
        }
      }
      logger.debugf("Parsed enabled types: %s", result);
    } catch (Exception e) {
      logger.errorf(e, "Failed to parse enabled types: %s", typesStr);
      return new HashSet<>(DEFAULT_ENABLED_TYPES);
    }

    return result.isEmpty() ? new HashSet<>(DEFAULT_ENABLED_TYPES) : result;
  }
}
