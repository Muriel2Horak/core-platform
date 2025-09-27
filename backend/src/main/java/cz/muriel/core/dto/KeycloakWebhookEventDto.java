package cz.muriel.core.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KeycloakWebhookEventDto {
  // Basic event fields
  private String eventType;
  private String type;
  private Long time;
  private Long timestamp;

  // Realm fields
  private String realmId;
  private String realmName;
  private String realm;
  private String tenantKey;

  // User fields
  private String userId;
  private String username;
  private String email;
  private String firstName;
  private String lastName;
  private Boolean enabled;

  // User details
  private Map<String, String> attributes;
  private Map<String, Object> roles;
  private List<String> groups;

  // Convenience getters for backward compatibility
  public String getEventType() {
    return eventType != null ? eventType : type;
  }

  public Long getTime() {
    return time != null ? time : timestamp;
  }

  public String getRealm() {
    return realm != null ? realm : realmName;
  }
}
