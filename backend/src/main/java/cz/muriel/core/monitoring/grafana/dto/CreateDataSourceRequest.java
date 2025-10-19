package cz.muriel.core.monitoring.grafana.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 📊 CREATE DATASOURCE REQUEST
 * 
 * Request DTO pro vytvoření Grafana datasource
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateDataSourceRequest {
  private String name;
  private String type;
  private String url;
  private String access; // "proxy" or "direct"
  private Boolean isDefault;
  private Map<String, Object> jsonData;
  private Map<String, String> secureJsonData;
}
