package cz.muriel.keycloak.webhook;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class EventWebhookProviderFactory implements EventListenerProviderFactory {

  private static final String PROVIDER_ID = "core-platform-webhook";

  private String webhookUrl;
  private String webhookSecret;

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return new EventWebhookProvider(session, webhookUrl, webhookSecret);
  }

  @Override
  public void init(Config.Scope config) {
    webhookUrl = config.get("webhookUrl");
    webhookSecret = config.get("webhookSecret");

    // Also try environment variables as fallback
    if (webhookUrl == null) {
      webhookUrl = System.getenv("KC_EVENT_WEBHOOK_URL");
    }
    if (webhookSecret == null) {
      webhookSecret = System.getenv("KC_EVENT_WEBHOOK_SECRET");
    }
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
}
