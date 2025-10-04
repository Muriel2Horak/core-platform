package cz.muriel.keycloak.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
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
  private final Set<String> enabledTypes;
  private final ObjectMapper objectMapper;
  private final WebhookClient webhookClient;

  public EventWebhookProvider(KeycloakSession session, String endpointUrl, String secret,
      Set<String> enabledTypes) {
    this.session = session;
    this.endpointUrl = endpointUrl;
    this.secret = secret;
    this.enabledTypes = enabledTypes;
    this.objectMapper = new ObjectMapper();
    this.webhookClient = new WebhookClient();
  }

  @Override
  public void onEvent(Event event) {
    // 🧪 DEBUGGING: Temporarily allow ALL event types to see what Keycloak sends
    logger.infof("🔍 DEBUG: Received event type: %s from realm: %s", event.getType().name(),
        session.getContext().getRealm().getName());

    // 🎯 EVENT MAPPING: Map Keycloak event types to backend event types
    String mappedEventType = mapKeycloakEventType(event.getType().name());

    // Check if the mapped event type is enabled
    if (!enabledTypes.contains(mappedEventType)) {
      logger.debugf("Ignoring event type: %s (mapped to %s)", event.getType().name(),
          mappedEventType);
      return;
    }

    try {
      RealmModel realm = session.getContext().getRealm();
      UserModel user = session.users().getUserById(realm, event.getUserId());

      if (user == null) {
        logger.warnf("User not found for event: %s", event.getUserId());
        return;
      }

      // 🎯 CLEAN ARCHITECTURE: realm name IS the tenant key
      String tenantKey = realm.getName();

      // 🧪 TESTING: Allow admin realm events for testing purposes
      // TODO: Re-enable this check for production
      /*
       * if ("admin".equals(tenantKey)) {
       * logger.debugf("Skipping event from admin realm (system admin realm)");
       * return; }
       */
      logger.infof("📧 Processing event from realm: %s, type: %s, user: %s", tenantKey,
          event.getType().name(), user.getUsername());

      WebhookPayload payload = new WebhookPayload();
      payload.setEventType(mappedEventType); // ✅ Use mapped event type
      payload.setTime(event.getTime());
      payload.setRealm(realm.getName());
      payload.setTenantKey(tenantKey); // realm name IS the tenant key
      payload.setUserId(event.getUserId());
      payload.setUsername(user.getUsername());
      payload.setEmail(user.getEmail());
      payload.setFirstName(user.getFirstName());
      payload.setLastName(user.getLastName());

      // ✅ NOVÉ: Načítání custom atributů
      Map<String, String> flatAttributes = new HashMap<>();
      Map<String, java.util.List<String>> userAttributes = user.getAttributes();
      if (userAttributes != null) {
        userAttributes.forEach((key, values) -> {
          if (values != null && !values.isEmpty()) {
            // Pro webhook použijeme pouze první hodnotu (flattened)
            flatAttributes.put(key, values.get(0));
          }
        });
      }
      payload.setAttributes(flatAttributes);

      // ✅ NOVÉ: Načítání rolí
      Map<String, Object> rolesMap = new HashMap<>();
      java.util.List<String> realmRoles = user.getRealmRoleMappingsStream()
          .map(role -> role.getName()).collect(java.util.stream.Collectors.toList());
      rolesMap.put("realm", realmRoles);
      payload.setRoles(rolesMap);

      // ✅ NOVÉ: Načítání skupin - opraveno getName() místo getPath()
      java.util.List<String> groupsList = user.getGroupsStream().map(group -> group.getName()) // ✅
                                                                                               // Opraveno:
                                                                                               // getName()
                                                                                               // místo
                                                                                               // getPath()
          .collect(java.util.stream.Collectors.toList());
      payload.setGroups(groupsList);

      sendWebhook(payload);

    } catch (Exception e) {
      logger.errorf(e, "Failed to process event: %s", event.getType());
    }
  }

  @Override
  public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
    if (!enabledTypes.contains(adminEvent.getOperationType().name())) {
      return;
    }

    try {
      RealmModel realm = session.getContext().getRealm();

      // 🎯 CLEAN ARCHITECTURE: realm name IS the tenant key
      String tenantKey = realm.getName();

      // 🧪 TESTING: Allow admin realm events for testing purposes
      // TODO: Re-enable this check for production
      /*
       * if ("admin".equals(tenantKey)) {
       * logger.debugf("Skipping admin event from admin realm"); return; }
       */

      logger.infof("🔍 DEBUG: Received admin event type: %s from realm: %s",
          adminEvent.getOperationType().name(), tenantKey);

      WebhookPayload payload = new WebhookPayload();
      payload.setEventType(adminEvent.getOperationType().name());
      payload.setTime(adminEvent.getTime());
      payload.setRealm(realm.getName());
      payload.setTenantKey(tenantKey); // realm name IS the tenant key

      sendWebhook(payload);

    } catch (Exception e) {
      logger.errorf(e, "Failed to process admin event: %s", adminEvent.getOperationType());
    }
  }

  private void sendWebhook(WebhookPayload payload) {
    if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
      logger.debug("Webhook endpoint URL not configured, skipping");
      return;
    }

    try {
      String jsonPayload = objectMapper.writeValueAsString(payload);
      byte[] body = jsonPayload.getBytes(StandardCharsets.UTF_8);

      String signature = computeHmacSha256(body, secret);

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("X-KC-Signature", "sha256=" + signature);
      headers.put("X-Realm", payload.getRealm());
      headers.put("X-Tenant-Key", payload.getTenantKey());

      logger.debugf("Sending webhook: url=%s, type=%s, signature=%s", endpointUrl,
          payload.getEventType(), signature.substring(0, 8) + "...");

      HttpResponse<String> response = webhookClient.sendJson(endpointUrl, body, headers);
      int statusCode = response.statusCode();

      if (statusCode >= 200 && statusCode < 300) {
        logger.infof("Webhook sent successfully: type=%s, status=%d", payload.getEventType(),
            statusCode);
      } else {
        logger.warnf("Webhook failed: type=%s, status=%d, response=%s", payload.getEventType(),
            statusCode, response.body());
      }

    } catch (Exception e) {
      logger.warnf(e, "Failed to send webhook for event: %s", payload.getEventType());
    }
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

  /**
   * Extracts user ID from resource path like "users/user-123" -> "user-123"
   */
  String extractUserIdFromPath(String resourcePath) {
    if (resourcePath == null || resourcePath.trim().isEmpty()) {
      return null;
    }

    String[] parts = resourcePath.split("/");
    if (parts.length < 2 || !"users".equals(parts[0])) {
      return null;
    }

    String userId = parts[1];
    return userId.isEmpty() ? null : userId;
  }

  /**
   * Maps Keycloak event types to backend event types.
   */
  private String mapKeycloakEventType(String keycloakEventType) {
    switch (keycloakEventType) {
    case "REGISTER":
      return "USER_CREATED";
    case "UPDATE_PROFILE":
      return "USER_UPDATED"; // ✅ KLÍČOVÉ MAPOVÁNÍ!
    case "UPDATE_PASSWORD":
      return "USER_UPDATED";
    case "UPDATE_EMAIL":
      return "USER_UPDATED";
    case "LOGIN":
      return "LOGIN"; // Keep as is for logging
    case "LOGOUT":
      return "LOGOUT"; // Keep as is for logging
    case "LOGIN_ERROR":
      return "LOGIN_ERROR"; // Keep as is for logging
    default:
      return keycloakEventType; // Default to the same type if no mapping exists
    }
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
