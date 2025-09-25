package cz.muriel.core.keycloak.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class BackendEventListenerFactory implements EventListenerProviderFactory {

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new BackendEventListener(session);
    }

    @Override
    public void init(Config.Scope config) {
        // Inicializační logika, pokud je potřeba
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Post-inicializační logika
    }

    @Override
    public void close() {
        // Úklid zdrojů
    }

    @Override
    public String getId() {
        return "backend-event-listener";
    }
}
