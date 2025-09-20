package cz.muriel.core.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {
  @NotBlank(message = "Nové heslo je povinné")
  @Size(min = 8, message = "Nové heslo musí mít alespoň 8 znaků")
  private String newPassword;

  private boolean requirePasswordChange = true;
}