package cz.muriel.core.monitoring.grafana.dto;

import lombok.Data;
import java.util.List;

@Data
public class ServiceAccountSearchResponse {
  private Integer totalCount;
  private List<ServiceAccountInfo> serviceAccounts;
  private Integer page;
  private Integer perPage;
}
