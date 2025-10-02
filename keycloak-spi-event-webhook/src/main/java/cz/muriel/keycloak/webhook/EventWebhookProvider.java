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
    if (!enabledTypes.contains(event.getType().name())) {
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

      // Skip events from admin realm (system admin realm)
      if ("admin".equals(tenantKey)) {
        logger.debugf("Skipping event from admin realm (system admin realm)");
        return;
      }

      WebhookPayload payload = new WebhookPayload();
      payload.setEventType(event.getType().name());
      payload.setTime(event.getTime());
      payload.setRealm(realm.getName());
      payload.setTenantKey(tenantKey); // realm name IS the tenant key
      payload.setUserId(event.getUserId());
      payload.setUsername(user.getUsername());
      payload.setEmail(user.getEmail());
      payload.setFirstName(user.getFirstName());
      payload.setLastName(user.getLastName());

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

      // Skip events from admin realm (system admin realm)
      if ("admin".equals(tenantKey)) {
        logger.debugf("Skipping admin event from admin realm");
        return;
      }

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
