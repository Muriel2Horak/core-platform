package cz.muriel.core.user.boundary;

import cz.muriel.core.user.control.KeycloakSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller pro správu synchronizace uživatelů s Keycloakem.
 */
@RestController @RequestMapping("/api/admin/sync")
public class KeycloakSyncController {

    private final KeycloakSyncService syncService;

    public KeycloakSyncController(KeycloakSyncService syncService) {
        this.syncService = syncService;
    }

    /**
     * Endpoint pro spuštění synchronizace všech uživatelů. Pouze pro
     * administrátory.
     */
    @PostMapping("/users") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> synchronizeUsers() {
        try {
            syncService.synchronizeAllUsers();
            return ResponseEntity.ok("Synchronizace uživatelů byla dokončena");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Chyba při synchronizaci: " + e.getMessage());
        }
    }
}
