package cz.muriel.core.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}