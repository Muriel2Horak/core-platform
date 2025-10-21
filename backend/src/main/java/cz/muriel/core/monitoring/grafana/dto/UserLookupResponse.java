package cz.muriel.core.monitoring.grafana.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Response from Grafana GET /api/users/lookup?loginOrEmail={email}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLookupResponse {
    private Long id;
    private String email;
    private String name;
    private String login;
    private Boolean isAdmin;
    private Boolean isDisabled;
}
