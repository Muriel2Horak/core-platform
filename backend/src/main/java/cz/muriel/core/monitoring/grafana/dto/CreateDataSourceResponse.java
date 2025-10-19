package cz.muriel.core.monitoring.grafana.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📊 CREATE DATASOURCE RESPONSE
 * 
 * Response DTO po vytvoření Grafana datasource
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class CreateDataSourceResponse {
  private Long id;
  private String uid;
  private String name;
  private String type;
  private String url;
  private Boolean isDefault;
  private String message;
}
