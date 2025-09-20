package cz.muriel.core.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequest {
  @NotBlank(message = "Současné heslo je povinné")
  private String currentPassword;

  @NotBlank(message = "Nové heslo je povinné")
  @Size(min = 8, message = "Nové heslo musí mít alespoň 8 znaků")
  private String newPassword;

  @NotBlank(message = "Potvrzení hesla je povinné")
  private String confirmPassword;
}