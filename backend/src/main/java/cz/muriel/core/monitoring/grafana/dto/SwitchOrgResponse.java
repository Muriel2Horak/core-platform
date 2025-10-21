package cz.muriel.core.monitoring.grafana.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Response from POST /api/users/{userId}/using/{orgId}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SwitchOrgResponse {
    private String message;
}
