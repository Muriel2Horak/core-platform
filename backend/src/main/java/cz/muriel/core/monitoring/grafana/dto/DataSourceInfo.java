package cz.muriel.core.monitoring.grafana.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📊 DATASOURCE INFO
 * 
 * DTO pro informace o Grafana datasource
 */
@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class DataSourceInfo {
  private Long id;
  private Long orgId;
  private String uid;
  private String name;
  private String type;
  private String url;
  private Boolean isDefault;
  private String access;
}
