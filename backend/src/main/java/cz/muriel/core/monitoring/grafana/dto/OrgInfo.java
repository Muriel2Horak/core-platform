package cz.muriel.core.monitoring.grafana.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🏢 ORGANIZATION INFO
 * 
 * Response from GET /api/orgs - seznam organizací
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrgInfo {
  private Long id;
  private String name;
}
