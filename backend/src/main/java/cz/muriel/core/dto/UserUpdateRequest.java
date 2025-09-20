package cz.muriel.core.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
  @Email(message = "Neplatný email formát")
  private String email;

  @Size(max = 50, message = "Jméno nesmí být delší než 50 znaků")
  private String firstName;

  @Size(max = 50, message = "Příjmení nesmí být delší než 50 znaků")
  private String lastName;

  private Boolean enabled;
}