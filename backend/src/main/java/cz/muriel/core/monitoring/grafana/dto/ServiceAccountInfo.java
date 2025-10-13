package cz.muriel.core.monitoring.grafana.dto;

import lombok.Data;

@Data
public class ServiceAccountInfo {
  private Long id;
  private String name;
  private String login;
  private Long orgId;
  private String role;
  private Boolean isDisabled;
  private Integer tokens;
}
