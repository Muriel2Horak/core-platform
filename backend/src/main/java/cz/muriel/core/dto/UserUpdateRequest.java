package cz.muriel.core.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class UserUpdateRequest {
  @Email(message = "Neplatný email formát")
  private String email;

  @Size(max = 50, message = "Jméno nesmí být delší než 50 znaků")
  private String firstName;

  @Size(max = 50, message = "Příjmení nesmí být delší než 50 znaků")
  private String lastName;

  private Boolean enabled;

  // 🏢 Organizační struktura
  @Size(max = 100, message = "Oddělení nesmí být delší než 100 znaků")
  private String department;

  @Size(max = 100, message = "Pozice nesmí být delší než 100 znaků")
  private String position;

  @Size(max = 50, message = "Username nadřízeného nesmí být delší než 50 znaků")
  private String manager;

  @Size(max = 50, message = "Nákladové středisko nesmí být delší než 50 znaků")
  private String costCenter;

  @Size(max = 100, message = "Lokace nesmí být delší než 100 znaků")
  private String location;

  @Size(max = 20, message = "Telefon nesmí být delší než 20 znaků")
  private String phone;

  // 👥 Zástupství
  @Size(max = 50, message = "Username zástupu nesmí být delší než 50 znaků")
  private String deputy;

  private LocalDate deputyFrom;

  private LocalDate deputyTo;

  @Size(max = 200, message = "Důvod zástupství nesmí být delší než 200 znaků")
  private String deputyReason;

  // 📷 Profilová fotka
  @Size(max = 1000000, message = "Profilová fotka nesmí být větší než 1MB")
  private String profilePicture; // URL nebo base64 string profilové fotky
}
