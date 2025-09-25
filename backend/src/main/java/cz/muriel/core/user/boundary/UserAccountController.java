package cz.muriel.core.user.boundary;

import cz.muriel.core.user.control.UserAccountService;
import cz.muriel.core.user.entity.UserAccount;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

/**
 * REST Controller pro správu uživatelských účtů.
 */
@RestController @RequestMapping("/api/users")
public class UserAccountController {

    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    /**
     * Endpoint pro nahrání profilového obrázku.
     *
     * @param userId ID uživatele, kterému se obrázek nahrává.
     * @param file Soubor s obrázkem.
     * @return Odpověď s aktualizovanými daty uživatele.
     * @throws IOException Pokud dojde k chybě při zpracování souboru.
     */
    @PostMapping("/{userId}/profile-picture")
    public ResponseEntity<UserAccount> uploadProfilePicture(@PathVariable UUID userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        UserAccount updatedUser = userAccountService.updateProfilePicture(userId, file);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Endpoint pro získání URL profilového obrázku.
     *
     * @param userId ID uživatele.
     * @return Odpověď s URL adresou nebo 404 Not Found, pokud obrázek neexistuje.
     */
    @GetMapping("/{userId}/profile-picture-url")
    public ResponseEntity<String> getProfilePictureUrl(@PathVariable UUID userId) {
        URL url = userAccountService.getProfilePictureUrl(userId);
        if (url != null) {
            return ResponseEntity.ok(url.toString());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
