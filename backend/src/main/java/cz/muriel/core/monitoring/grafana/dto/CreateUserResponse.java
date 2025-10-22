package cz.muriel.core.monitoring.grafana.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Response from Grafana POST /api/admin/users
 */
@Data @JsonIgnoreProperties(ignoreUnknown = true)
public class CreateUserResponse {
  private Long id;
  private String message;
}
