package cz.muriel.core.monitoring.grafana.dto;

import lombok.Data;

@Data
public class CreateServiceAccountTokenResponse {
  private Long id;
  private String name;
  private String key; // The actual service account token
}
