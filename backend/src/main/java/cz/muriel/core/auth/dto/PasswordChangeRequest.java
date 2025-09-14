package cz.muriel.core.auth.dto;

import lombok.Data;

@Data
public class PasswordChangeRequest {
  private String currentPassword;
  private String newPassword;
}