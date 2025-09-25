package cz.muriel.core.keycloak.events;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

/**
 * Event Listener pro posílání událostí o změnách uživatelů do backendu.
 */
public class BackendEventListener implements EventListenerProvider {

    private final KeycloakSession session;
    private final Client httpClient;
    private final String backendUrl;

    public BackendEventListener(KeycloakSession session) {
        this.session = session;
        this.httpClient = ClientBuilder.newClient();
        this.backendUrl = System.getProperty("backend.url", "http://backend:8080");
    }

    @Override
    public void onEvent(Event event) {
        // Reagujeme pouze na události týkající se uživatelů
        if (event.getType() == EventType.REGISTER || event.getType() == EventType.UPDATE_PROFILE
                || event.getType() == EventType.DELETE_ACCOUNT) {

            String userId = event.getUserId();
            if (userId != null) {
                UserModel user = session.users().getUserById(session.getContext().getRealm(),
                        userId);
                if (user != null) {
                    sendUserEventToBackend(event.getType().toString(), user);
                }
            }
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        // Zde můžeme přidat další logiku pro admin události
    }

    private void sendUserEventToBackend(String eventType, UserModel user) {
        try {
            Response response = httpClient.target(backendUrl + "/api/webhooks/keycloak/user-events")
                    .request(MediaType.APPLICATION_JSON).header("X-Keycloak-Event", eventType)
                    .post(Entity.json(user));

            if (response.getStatus() != 200) {
                System.err.println("Failed to send event to backend: " + response.getStatus());
            }
        } catch (Exception e) {
            System.err.println("Error sending event to backend: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
