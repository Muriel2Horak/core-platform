package cz.muriel.keycloak.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EventWebhookProvider implements EventListenerProvider {

  private static final Logger logger = Logger.getLogger(EventWebhookProvider.class);

  private final KeycloakSession session;
  private final String webhookUrl;
  private final String webhookSecret;
  private final ObjectMapper objectMapper;
  private final CloseableHttpClient httpClient;

  // Tracked user events
  private static final Set<EventType> TRACKED_USER_EVENTS = Set.of(EventType.REGISTER,
      EventType.UPDATE_PROFILE, EventType.DELETE_ACCOUNT);

  // Tracked admin events
  private static final Set<OperationType> TRACKED_OPERATIONS = Set.of(OperationType.CREATE,
      OperationType.UPDATE, OperationType.DELETE);

  private static final Set<ResourceType> TRACKED_RESOURCES = Set.of(ResourceType.USER,
      ResourceType.REALM_ROLE, ResourceType.CLIENT_ROLE, ResourceType.GROUP_MEMBERSHIP);

  public EventWebhookProvider(KeycloakSession session, String webhookUrl, String webhookSecret) {
    this.session = session;
    this.webhookUrl = webhookUrl;
    this.webhookSecret = webhookSecret;
    this.objectMapper = new ObjectMapper();
    this.httpClient = HttpClients.createDefault();
  }

  @Override
  public void onEvent(Event event) {
    if (!TRACKED_USER_EVENTS.contains(event.getType())) {
      return;
    }

    try {
      RealmModel realm = session.realms().getRealm(event.getRealmId());
      if (realm == null) {
        logger.warnf("Realm not found for event: %s", event.getRealmId());
        return;
      }

      UserModel user = session.users().getUserById(realm, event.getUserId());
      if (user == null) {
        logger.warnf("User not found for event: %s", event.getUserId());
        return;
      }

      WebhookPayload payload = createUserEventPayload(event, realm, user);
      sendWebhookAsync(payload);

    } catch (Exception e) {
      logger.errorf(e, "Failed to process user event: %s", event.getType());
    }
  }

  @Override
  public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
    if (!TRACKED_OPERATIONS.contains(adminEvent.getOperationType())
        || !TRACKED_RESOURCES.contains(adminEvent.getResourceType())) {
      return;
    }

    try {
      RealmModel realm = session.realms().getRealm(adminEvent.getRealmId());
      if (realm == null) {
        logger.warnf("Realm not found for admin event: %s", adminEvent.getRealmId());
        return;
      }

      WebhookPayload payload = createAdminEventPayload(adminEvent, realm);
      sendWebhookAsync(payload);

    } catch (Exception e) {
      logger.errorf(e, "Failed to process admin event: %s/%s", adminEvent.getResourceType(),
          adminEvent.getOperationType());
    }
  }

  private WebhookPayload createUserEventPayload(Event event, RealmModel realm, UserModel user) {
    String eventType = mapUserEventType(event.getType());

    return WebhookPayload.builder().eventType(eventType)
        .time(Instant.ofEpochMilli(event.getTime()).toEpochMilli()).realmId(realm.getId())
        .realmName(realm.getName()).tenantKey(realm.getName()) // Use realm name as tenant key
        .userId(user.getId()).username(user.getUsername()).email(user.getEmail())
        .firstName(user.getFirstName()).lastName(user.getLastName()).enabled(user.isEnabled())
        .attributes(flattenAttributes(user.getAttributes())).roles(getUserRoles(user, realm))
        .groups(getUserGroups(user)).build();
  }

  private WebhookPayload createAdminEventPayload(AdminEvent adminEvent, RealmModel realm) {
    String eventType = mapAdminEventType(adminEvent.getResourceType(),
        adminEvent.getOperationType());

    WebhookPayload.WebhookPayloadBuilder builder = WebhookPayload.builder().eventType(eventType)
        .time(adminEvent.getTime()).realmId(realm.getId()).realmName(realm.getName())
        .tenantKey(realm.getName()); // Use realm name as tenant key

    // Extract user ID from resource path if it's a user-related event
    String resourcePath = adminEvent.getResourcePath();
    if (adminEvent.getResourceType() == ResourceType.USER && resourcePath != null) {
      String userId = extractUserIdFromPath(resourcePath);
      if (userId != null) {
        UserModel user = session.users().getUserById(realm, userId);
        if (user != null) {
          builder.userId(user.getId()).username(user.getUsername()).email(user.getEmail())
              .firstName(user.getFirstName()).lastName(user.getLastName()).enabled(user.isEnabled())
              .attributes(flattenAttributes(user.getAttributes())).roles(getUserRoles(user, realm))
              .groups(getUserGroups(user));
        }
      }
    }

    return builder.build();
  }

  private String mapUserEventType(EventType eventType) {
    return switch (eventType) {
    case REGISTER -> "USER_CREATED";
    case UPDATE_PROFILE -> "USER_UPDATED";
    case DELETE_ACCOUNT -> "USER_DELETED";
    default -> "USER_UPDATED";
    };
  }

  private String mapAdminEventType(ResourceType resourceType, OperationType operationType) {
    String prefix = switch (resourceType) {
    case USER -> "USER";
    case REALM_ROLE -> "ROLE";
    case CLIENT_ROLE -> "ROLE";
    case GROUP_MEMBERSHIP -> "GROUP_MEMBERSHIP";
    default -> "UNKNOWN";
    };

    String suffix = switch (operationType) {
    case CREATE -> "CREATED";
    case UPDATE -> "UPDATED";
    case DELETE -> "DELETED";
    default -> "UPDATED";
    };

    return prefix + "_" + suffix;
  }

  private Map<String, String> flattenAttributes(Map<String, List<String>> attributes) {
    Map<String, String> flattened = new HashMap<>();

    if (attributes != null) {
      attributes.forEach((key, values) -> {
        if (values != null && !values.isEmpty()) {
          // Join multiple values with semicolon
          flattened.put(key, String.join(";", values));
        }
      });
    }

    return flattened;
  }

  private Map<String, Object> getUserRoles(UserModel user, RealmModel realm) {
    Map<String, Object> roles = new HashMap<>();

    // Realm roles
    Set<String> realmRoles = user.getRealmRoleMappingsStream().map(role -> role.getName())
        .collect(java.util.stream.Collectors.toSet());
    roles.put("realm", new ArrayList<>(realmRoles));

    // Client roles
    Map<String, List<String>> clientRoles = new HashMap<>();
    realm.getClientsStream().forEach(client -> {
      Set<String> clientRoleNames = user.getClientRoleMappingsStream(client)
          .map(role -> role.getName()).collect(java.util.stream.Collectors.toSet());
      if (!clientRoleNames.isEmpty()) {
        clientRoles.put(client.getClientId(), new ArrayList<>(clientRoleNames));
      }
    });
    roles.put("clients", clientRoles);

    return roles;
  }

  private List<String> getUserGroups(UserModel user) {
    return user.getGroupsStream().map(group -> {
      // Build full path for group
      var current = group;
      var pathSegments = new ArrayList<String>();

      while (current != null) {
        pathSegments.add(0, current.getName());
        current = current.getParent();
      }

      return "/" + String.join("/", pathSegments);
    }).collect(java.util.stream.Collectors.toList());
  }

  private String extractUserIdFromPath(String resourcePath) {
    // Resource path format: "users/{userId}" or "users/{userId}/..."
    if (resourcePath.startsWith("users/")) {
      String[] parts = resourcePath.split("/");
      if (parts.length >= 2) {
        return parts[1];
      }
    }
    return null;
  }

  private void sendWebhookAsync(WebhookPayload payload) {
    CompletableFuture.runAsync(() -> {
      try {
        sendWebhook(payload);
      } catch (Exception e) {
        logger.errorf(e, "Failed to send webhook for event: %s", payload.getEventType());
      }
    });
  }

  private void sendWebhook(WebhookPayload payload) throws Exception {
    if (webhookUrl == null || webhookUrl.isEmpty()) {
      logger.debug("Webhook URL not configured, skipping event");
      return;
    }

    HttpPost request = new HttpPost(webhookUrl);
    request.setHeader("Content-Type", "application/json");
    request.setHeader("X-Webhook-Secret", webhookSecret);

    String jsonPayload = objectMapper.writeValueAsString(payload);
    request.setEntity(new StringEntity(jsonPayload, StandardCharsets.UTF_8));

    httpClient.execute(request, response -> {
      int statusCode = response.getCode();
      if (statusCode >= 200 && statusCode < 300) {
        logger.debugf("Webhook sent successfully for event: %s (status: %d)",
            payload.getEventType(), statusCode);
      } else {
        logger.warnf("Webhook failed for event: %s (status: %d)", payload.getEventType(),
            statusCode);
      }
      return null;
    });
  }

  @Override
  public void close() {
    try {
      if (httpClient != null) {
        httpClient.close();
      }
    } catch (Exception e) {
      logger.warn("Failed to close HTTP client", e);
    }
  }
}
