package cz.muriel.keycloak.webhook;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.*;

public class EventWebhookProviderFactory implements EventListenerProviderFactory {

  private static final Logger logger = Logger.getLogger(EventWebhookProviderFactory.class);
  private static final String PROVIDER_ID = "muriel-webhook";

  private String endpointUrl;
  private String secret;
  private Map<String, RealmTenant> realmTenantMap;
  private Set<String> enabledTypes;

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return new EventWebhookProvider(session, endpointUrl, secret, realmTenantMap, enabledTypes);
  }

  @Override
  public void init(Config.Scope config) {
    // Load configuration with fallback to environment variables
    endpointUrl = getConfigValue(config, "endpoint-url",
        "KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_ENDPOINT_URL");
    secret = getConfigValue(config, "secret", "KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_SECRET");

    String realmTenantMapStr = getConfigValue(config, "realm-tenant-map",
        "KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_REALM_TENANT_MAP");
    String enabledTypesStr = getConfigValue(config, "enabled-types",
        "KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_ENABLED_TYPES");

    // Parse realm-tenant-map
    realmTenantMap = parseRealmTenantMap(realmTenantMapStr);

    // Parse enabled-types (default to all if not specified)
    enabledTypes = parseEnabledTypes(enabledTypesStr);

    logger.infof("Muriel webhook provider initialized: endpoint=%s, realms=%d, enabledTypes=%s",
        endpointUrl, realmTenantMap.size(), enabledTypes);
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    // No post-initialization needed
  }

  @Override
  public void close() {
    // No cleanup needed
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  // Package-private methods for testing
  String getConfigValue(Config.Scope config, String configKey, String envKey) {
    String value = config.get(configKey);
    if (value == null) {
      value = System.getenv(envKey);
    }
    return value;
  }

  Map<String, RealmTenant> parseRealmTenantMap(String mapStr) {
    Map<String, RealmTenant> result = new HashMap<>();

    if (mapStr == null || mapStr.trim().isEmpty()) {
      logger.warn("Realm-tenant-map not configured");
      return result;
    }

    try {
      String[] entries = mapStr.split(",");
      for (String entry : entries) {
        String[] parts = entry.trim().split(":");
        if (parts.length == 3) {
          String realm = parts[0].trim();
          String tenantKey = parts[1].trim();
          String tenantId = parts[2].trim();
          result.put(realm, new RealmTenant(tenantKey, tenantId));
        } else {
          logger.warnf(
              "Invalid realm-tenant-map entry format: %s (expected realm:tenantKey:tenantId)",
              entry);
        }
      }
    } catch (Exception e) {
      logger.errorf(e, "Failed to parse realm-tenant-map: %s", mapStr);
    }

    return result;
  }

  Set<String> parseEnabledTypes(String typesStr) {
    if (typesStr == null || typesStr.trim().isEmpty()) {
      // Default to all supported types
      return Set.of("USER_CREATED", "USER_UPDATED", "USER_DELETED");
    }

    try {
      String[] types = typesStr.split(",");
      Set<String> result = new HashSet<>();
      for (String type : types) {
        result.add(type.trim());
      }
      return result;
    } catch (Exception e) {
      logger.errorf(e, "Failed to parse enabled-types: %s", typesStr);
      return Set.of("USER_CREATED", "USER_UPDATED", "USER_DELETED");
    }
  }
}
