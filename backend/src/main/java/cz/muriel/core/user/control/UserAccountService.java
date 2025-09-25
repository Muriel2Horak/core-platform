package cz.muriel.core.user.control;

import cz.muriel.core.storage.service.StorageService;
import cz.muriel.core.user.boundary.UserAccountRepository;
import cz.muriel.core.user.entity.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

/**
 * Servisní vrstva pro správu uživatelských účtů.
 */
@Service
public class UserAccountService {

    private final UserAccountRepository userRepository;
    private final StorageService storageService;

    public UserAccountService(UserAccountRepository userRepository, StorageService storageService) {
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    /**
     * Nahraje a přiřadí profilový obrázek k uživatelskému účtu.
     *
     * @param userId ID uživatele.
     * @param file Soubor s obrázkem.
     * @return Aktualizovaný uživatelský účet.
     * @throws IOException Pokud dojde k chybě při nahrávání souboru.
     */
    @Transactional
    public UserAccount updateProfilePicture(UUID userId, MultipartFile file) throws IOException {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId)); // V
                                                                                               // praxi
                                                                                               // použít
                                                                                               // vlastní
                                                                                               // výjimku

        // Pokud uživatel již má obrázek, smažeme starý
        if (user.getProfilePictureKey() != null && !user.getProfilePictureKey().isEmpty()) {
            storageService.deleteFile(user.getProfilePictureKey());
        }

        // Nahrajeme nový obrázek a uložíme jeho klíč
        String fileKey = storageService.uploadFile(file);
        user.setProfilePictureKey(fileKey);

        return userRepository.save(user);
    }

    /**
     * Získá URL profilového obrázku uživatele.
     *
     * @param userId ID uživatele.
     * @return URL obrázku nebo null, pokud obrázek neexistuje.
     */
    public URL getProfilePictureUrl(UUID userId) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getProfilePictureKey() == null) {
            return null;
        }

        return storageService.getFileUrl(user.getProfilePictureKey());
    }
}
