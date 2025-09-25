package cz.muriel.core.user.boundary;

import cz.muriel.core.user.control.KeycloakSyncService;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pro příjem webhook událostí z Keycloaku.
 */
@RestController @RequestMapping("/api/webhooks/keycloak")
public class KeycloakWebhookController {

    private final KeycloakSyncService syncService;

    public KeycloakWebhookController(KeycloakSyncService syncService) {
        this.syncService = syncService;
    }

    /**
     * Endpoint pro příjem událostí o změnách uživatelů v Keycloaku.
     */
    @PostMapping("/user-events")
    public ResponseEntity<Void> handleUserEvent(@RequestHeader("X-Keycloak-Event") String eventType,
            @RequestBody UserRepresentation user) {

        try {
            // Zpracujeme událost podle jejího typu
            switch (eventType) {
            case "REGISTER":
            case "UPDATE_PROFILE":
                syncService.synchronizeUser(user);
                break;
            case "DELETE_ACCOUNT":
                syncService.deleteUser(user.getId());
                break;
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Log chybu a vrať 500
            System.err.println("Error processing user event: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
