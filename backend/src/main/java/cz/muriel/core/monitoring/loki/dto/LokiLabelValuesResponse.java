package cz.muriel.core.monitoring.loki.dto;

import lombok.Data;

import java.util.List;

/**
 * Response from Loki label values API
 */
@Data
public class LokiLabelValuesResponse {
  private String status;
  private List<String> data; // List of label values (e.g., ["admin", "test-tenant"])
}
