package cz.muriel.core.monitoring.loki.dto;

import lombok.Data;

import java.util.List;

/**
 * Response from Loki labels API
 */
@Data
public class LokiLabelsResponse {
  private String status;
  private List<String> data; // List of label names (e.g., ["tenant", "service", "level"])
}
