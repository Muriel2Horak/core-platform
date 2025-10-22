package cz.muriel.core.monitoring.grafana.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to create new user in Grafana via POST /api/admin/users
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class CreateUserRequest {
  private String email;
  private String name;
  private String login;
  private String password;
}
