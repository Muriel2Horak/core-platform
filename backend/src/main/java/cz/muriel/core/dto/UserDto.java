package cz.muriel.core.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserDto {
  private String id;
  private String username;
  private String email;
  private String firstName;
  private String lastName;
  private boolean enabled;
  private boolean emailVerified;
  private Instant createdTimestamp;
  private List<String> roles;

  // 🏢 Organizační struktura
  private String department; // Oddělení
  private String position; // Pozice/funkce
  private String manager; // Nadřízený (username)
  private String managerName; // Celé jméno nadřízeného
  private String costCenter; // Nákladové středisko
  private String location; // Lokace/pobočka
  private String phone; // Telefon

  // 👥 Zástupství
  private String deputy; // Zástup (username)
  private String deputyName; // Celé jméno zástupu
  private LocalDate deputyFrom; // Zástup od kdy
  private LocalDate deputyTo; // Zástup do kdy
  private String deputyReason; // Důvod zástupství (dovolená, nemoc, služební cesta)

  // 📷 Profilová fotka a identita
  private String profilePicture; // S3 klíč pro profilovou fotku
  private String profilePictureUrl; // Kompletní URL pro profilovou fotku
  private String identityProvider; // Zdroj uživatele (local, ldap, saml, etc.)
  private String identityProviderAlias; // Alias providera (např. "company-ad")
  private boolean isLocalUser; // True pokud je uživatel lokální v Keycloak
  private String federatedUsername; // Username z federovaného systému (např. AD)

  // 📊 Rozšířené atributy (pro budoucí rozšíření)
  private Map<String, String> customAttributes;
}
