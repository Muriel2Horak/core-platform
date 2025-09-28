package cz.muriel.keycloak.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EventWebhookProvider implements EventListenerProvider {

  private static final Logger logger = Logger.getLogger(EventWebhookProvider.class);

  private final KeycloakSession session;
  private final String endpointUrl;
  private final String secret;
  private final Map<String, RealmTenant> realmTenantMap;
  private final Set<String> enabledTypes;
  private final ObjectMapper objectMapper;
  private final WebhookClient webhookClient;

  // Event type mappings
  private static final Map<EventType, String> USER_EVENT_MAPPING = Map.of(EventType.REGISTER,
      "USER_CREATED", EventType.UPDATE_PROFILE, "USER_UPDATED");

  private static final Map<OperationType, String> ADMIN_OPERATION_MAPPING = Map.of(
      OperationType.CREATE, "USER_CREATED", OperationType.UPDATE, "USER_UPDATED",
      OperationType.DELETE, "USER_DELETED");

  public EventWebhookProvider(KeycloakSession session, String endpointUrl, String secret,
      Map<String, RealmTenant> realmTenantMap, Set<String> enabledTypes) {
    this.session = session;
    this.endpointUrl = endpointUrl;
    this.secret = secret;
    this.realmTenantMap = realmTenantMap;
    this.enabledTypes = enabledTypes;
    this.objectMapper = new ObjectMapper();
    this.webhookClient = new WebhookClient();
  }

  @Override
  public void onEvent(Event event) {
    String eventType = USER_EVENT_MAPPING.get(event.getType());
    if (eventType == null) {
      logger.debugf("Ignoring user event type: %s", event.getType());
      return;
    }

    if (!enabledTypes.contains(eventType)) {
      logger.debugf("Event type %s is not enabled", eventType);
      return;
    }

    try {
      RealmModel realm = session.realms().getRealm(event.getRealmId());
      if (realm == null) {
        logger.warnf("Realm not found for event: %s", event.getRealmId());
        return;
      }

      String realmName = realm.getName();
      RealmTenant realmTenant = realmTenantMap.get(realmName);
      if (realmTenant == null) {
        logger.warnf("No tenant mapping found for realm: %s", realmName);
        return;
      }

      UserModel user = session.users().getUserById(realm, event.getUserId());
      if (user == null) {
        logger.warnf("User not found for event: %s", event.getUserId());
        return;
      }

      WebhookPayload payload = new WebhookPayload(eventType, realmName, realmTenant.getTenantKey(),
          realmTenant.getTenantId(), user.getId(), user.getUsername(), user.getEmail(),
          user.getFirstName(), user.getLastName());

      logger.infof("Processing user event: type=%s, realm=%s, user=%s", eventType, realmName,
          user.getUsername());

      sendWebhook(payload, realmName, realmTenant);

    } catch (Exception e) {
      logger.errorf(e, "Failed to process user event: %s", event.getType());
    }
  }

  @Override
  public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
    // Only handle USER resource type
    if (adminEvent.getResourceType() != ResourceType.USER) {
      return;
    }

    String eventType = ADMIN_OPERATION_MAPPING.get(adminEvent.getOperationType());
    if (eventType == null) {
      logger.debugf("Ignoring admin operation: %s", adminEvent.getOperationType());
      return;
    }

    if (!enabledTypes.contains(eventType)) {
      logger.debugf("Event type %s is not enabled", eventType);
      return;
    }

    try {
      RealmModel realm = session.realms().getRealm(adminEvent.getRealmId());
      if (realm == null) {
        logger.warnf("Realm not found for admin event: %s", adminEvent.getRealmId());
        return;
      }

      String realmName = realm.getName();
      RealmTenant realmTenant = realmTenantMap.get(realmName);
      if (realmTenant == null) {
        logger.warnf("No tenant mapping found for realm: %s", realmName);
        return;
      }

      // Extract user ID from resource path
      String userId = extractUserIdFromPath(adminEvent.getResourcePath());
      if (userId == null) {
        logger.warnf("Could not extract user ID from resource path: %s",
            adminEvent.getResourcePath());
        return;
      }

      WebhookPayload payload;
      UserModel user = session.users().getUserById(realm, userId);

      if (user != null) {
        // User exists - full data
        payload = new WebhookPayload(eventType, realmName, realmTenant.getTenantKey(),
            realmTenant.getTenantId(), user.getId(), user.getUsername(), user.getEmail(),
            user.getFirstName(), user.getLastName());
      } else {
        // User doesn't exist (e.g., DELETE) - minimal data
        payload = new WebhookPayload(eventType, realmName, realmTenant.getTenantKey(),
            realmTenant.getTenantId(), userId, null, // username not available
            null, // email not available
            null, // firstName not available
            null // lastName not available
        );
      }

      logger.infof("Processing admin event: type=%s, realm=%s, userId=%s", eventType, realmName,
          userId);

      sendWebhook(payload, realmName, realmTenant);

    } catch (Exception e) {
      logger.errorf(e, "Failed to process admin event: %s/%s", adminEvent.getResourceType(),
          adminEvent.getOperationType());
    }
  }

  private void sendWebhook(WebhookPayload payload, String realmName, RealmTenant realmTenant) {
    if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
      logger.debug("Webhook endpoint URL not configured, skipping");
      return;
    }

    try {
      // Serialize payload to JSON
      String jsonPayload = objectMapper.writeValueAsString(payload);
      byte[] body = jsonPayload.getBytes(StandardCharsets.UTF_8);

      // Compute HMAC-SHA256 signature
      String signature = computeHmacSha256(body, secret);

      // Prepare headers
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("X-KC-Signature", "sha256=" + signature);
      headers.put("X-Realm", realmName);
      headers.put("X-Tenant-Key", realmTenant.getTenantKey());
      headers.put("X-Tenant-Id", realmTenant.getTenantId());

      logger.debugf("Sending webhook: url=%s, type=%s, signature=%s", endpointUrl,
          payload.getType(), signature.substring(0, 8) + "...");

      // Send request
      HttpResponse<String> response = webhookClient.sendJson(endpointUrl, body, headers);
      int statusCode = response.statusCode();

      if (statusCode >= 200 && statusCode < 300) {
        logger.infof("Webhook sent successfully: type=%s, status=%d", payload.getType(),
            statusCode);
      } else {
        logger.warnf("Webhook failed: type=%s, status=%d, response=%s", payload.getType(),
            statusCode, response.body());
      }

    } catch (Exception e) {
      logger.warnf(e, "Failed to send webhook for event: %s", payload.getType());
    }
  }

  // Package-private methods for testing
  String extractUserIdFromPath(String resourcePath) {
    // Resource path format: "users/{userId}" or "users/{userId}/..."
    if (resourcePath != null && resourcePath.startsWith("users/")) {
      String[] parts = resourcePath.split("/");
      if (parts.length >= 2) {
        return parts[1];
      }
    }
    return null;
  }

  String computeHmacSha256(byte[] data, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
          "HmacSHA256");
      mac.init(secretKey);
      byte[] hmacBytes = mac.doFinal(data);
      return bytesToHex(hmacBytes);
    } catch (Exception e) {
      throw new RuntimeException("Failed to compute HMAC-SHA256", e);
    }
  }

  String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  @Override
  public void close() {
    try {
      if (webhookClient != null) {
        webhookClient.close();
      }
    } catch (Exception e) {
      logger.warn("Failed to close webhook client", e);
    }
  }
}
