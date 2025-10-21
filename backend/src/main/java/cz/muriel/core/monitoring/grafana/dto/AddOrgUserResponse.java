package cz.muriel.core.monitoring.grafana.dto;

import lombok.Data;

@Data
public class AddOrgUserResponse {
  private String message;
  private Long userId;
}
