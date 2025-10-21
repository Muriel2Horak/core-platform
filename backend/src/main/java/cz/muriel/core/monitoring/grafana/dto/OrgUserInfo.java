package cz.muriel.core.monitoring.grafana.dto;

import lombok.Data;

@Data
public class OrgUserInfo {
  private Long orgId;
  private Long userId;
  private String email;
  private String login;
  private String role;
}
