package cz.muriel.core.auth.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
  private String firstName;
  private String lastName;
  private String email;
}