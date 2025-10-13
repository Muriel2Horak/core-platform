package cz.muriel.core.monitoring.grafana.dto;

import lombok.Data;

@Data
public class CreateServiceAccountResponse {
  private Long id;
  private String name;
  private String role;
  private String login;
  private Boolean isDisabled;
}
