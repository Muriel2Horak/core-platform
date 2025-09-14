package cz.muriel.core.auth.dto;

import lombok.Data;

@Data
public class LoginRequest {
  private String username;
  private String password;
}