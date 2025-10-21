package cz.muriel.core.monitoring.grafana.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * User's organization membership info from GET /api/users/{id}/orgs
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserOrgInfo {
    private Long orgId;
    private String name;
    private String role;
    
    @JsonProperty("isGrafanaAdmin")
    private Boolean isGrafanaAdmin;
}
