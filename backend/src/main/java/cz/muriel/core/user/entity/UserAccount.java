package cz.muriel.core.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Entita reprezentující uživatelský účet v lokální databázi. Slouží jako
 * lokální kopie dat z Keycloaku s rozšířením o další atributy, jako je
 * například klíč k profilovému obrázku v S3.
 */
@Entity @Table(name = "user_account") @Getter @Setter
public class UserAccount {

    /**
     * Primární klíč, odpovídá ID uživatele v Keycloaku (sub).
     */
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    private String firstName;

    private String lastName;

    private boolean enabled;

    /**
     * Klíč (název souboru) profilového obrázku uloženého v S3 úložišti. Může být
     * null, pokud uživatel nemá žádný obrázek.
     */
    private String profilePictureKey;

}
