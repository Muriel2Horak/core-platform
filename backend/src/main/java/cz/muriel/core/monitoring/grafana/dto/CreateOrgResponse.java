package cz.muriel.core.monitoring.grafana.dto;

import lombok.Data;

@Data
public class CreateOrgResponse {
  private Long orgId;
  private String message;
}
