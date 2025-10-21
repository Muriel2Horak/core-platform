package cz.muriel.core.monitoring.grafana.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class AddOrgUserRequest {
  private String loginOrEmail;
  private String role; // Admin, Editor, Viewer
}
