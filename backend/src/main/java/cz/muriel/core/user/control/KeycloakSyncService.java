package cz.muriel.core.user.control;

import cz.muriel.core.user.boundary.UserAccountRepository;
import cz.muriel.core.user.entity.UserAccount;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Služba pro synchronizaci uživatelů mezi Keycloakem a lokální databází.
 */
@Service
public class KeycloakSyncService {

    private final UserAccountRepository userRepository;
    private final String keycloakServerUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakSyncService(UserAccountRepository userRepository,
            @Value("${keycloak.admin.base-url}") String keycloakServerUrl,
            @Value("${keycloak.admin.realm}") String realm,
            @Value("${keycloak.admin.client-id}") String clientId,
            @Value("${keycloak.admin.client-secret}") String clientSecret) {
        this.userRepository = userRepository;
        this.keycloakServerUrl = keycloakServerUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Provede jednorázovou synchronizaci všech uživatelů z Keycloaku.
     */
    @Transactional
    public void synchronizeAllUsers() {
        Keycloak keycloak = createKeycloakClient();

        // Načteme všechny uživatele z Keycloaku
        List<UserRepresentation> keycloakUsers = keycloak.realm(realm).users().list();

        for (UserRepresentation keycloakUser : keycloakUsers) {
            synchronizeUser(keycloakUser);
        }
    }

    /**
     * Synchronizuje jednoho uživatele z Keycloaku do lokální databáze.
     */
    @Transactional
    public void synchronizeUser(UserRepresentation keycloakUser) {
        UUID userId = UUID.fromString(keycloakUser.getId());

        // Najdeme nebo vytvoříme uživatele v lokální databázi
        UserAccount userAccount = userRepository.findById(userId).orElse(new UserAccount());

        // Aktualizujeme data
        userAccount.setId(userId);
        userAccount.setUsername(keycloakUser.getUsername());
        userAccount.setEmail(keycloakUser.getEmail());
        userAccount.setFirstName(keycloakUser.getFirstName());
        userAccount.setLastName(keycloakUser.getLastName());
        userAccount.setEnabled(keycloakUser.isEnabled());

        userRepository.save(userAccount);
    }

    /**
     * Smaže uživatele z lokální databáze. Používá se při příjmu DELETE_ACCOUNT
     * eventu z Keycloaku.
     */
    @Transactional
    public void deleteUser(String userId) {
        UUID userUuid = UUID.fromString(userId);
        userRepository.deleteById(userUuid);
    }

    /**
     * Vytvoří instanci Keycloak klienta pro přístup k Admin REST API.
     */
    private Keycloak createKeycloakClient() {
        return KeycloakBuilder.builder().serverUrl(keycloakServerUrl).realm(realm)
                .clientId(clientId).clientSecret(clientSecret).grantType("client_credentials")
                .build();
    }
}
