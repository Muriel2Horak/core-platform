package cz.muriel.core.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
  @NotBlank(message = "Username je povinný")
  @Size(min = 3, max = 50, message = "Username musí být 3-50 znaků")
  private String username;

  @NotBlank(message = "Email je povinný")
  @Email(message = "Neplatný email formát")
  private String email;

  @Size(max = 50, message = "Jméno nesmí být delší než 50 znaků")
  private String firstName;

  @Size(max = 50, message = "Příjmení nesmí být delší než 50 znaků")
  private String lastName;

  @Size(min = 8, message = "Heslo musí mít alespoň 8 znaků")
  private String temporaryPassword;

  private boolean enabled = true;
  private boolean requirePasswordChange = true;
}