package cz.muriel.keycloak.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class WebhookPayload {

  @JsonProperty("eventType")
  private String eventType;

  @JsonProperty("time")
  private long time;

  @JsonProperty("realmId")
  private String realmId;

  @JsonProperty("realmName")
  private String realmName;

  @JsonProperty("tenantKey")
  private String tenantKey;

  @JsonProperty("userId")
  private String userId;

  @JsonProperty("username")
  private String username;

  @JsonProperty("email")
  private String email;

  @JsonProperty("firstName")
  private String firstName;

  @JsonProperty("lastName")
  private String lastName;

  @JsonProperty("enabled")
  private Boolean enabled;

  @JsonProperty("attributes")
  private Map<String, String> attributes;

  @JsonProperty("roles")
  private Map<String, Object> roles;

  @JsonProperty("groups")
  private List<String> groups;

  // Default constructor
  public WebhookPayload() {
  }

  // Builder constructor
  private WebhookPayload(WebhookPayloadBuilder builder) {
    this.eventType = builder.eventType;
    this.time = builder.time;
    this.realmId = builder.realmId;
    this.realmName = builder.realmName;
    this.tenantKey = builder.tenantKey;
    this.userId = builder.userId;
    this.username = builder.username;
    this.email = builder.email;
    this.firstName = builder.firstName;
    this.lastName = builder.lastName;
    this.enabled = builder.enabled;
    this.attributes = builder.attributes;
    this.roles = builder.roles;
    this.groups = builder.groups;
  }

  public static WebhookPayloadBuilder builder() {
    return new WebhookPayloadBuilder();
  }

  // Getters and Setters
  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public String getRealmId() {
    return realmId;
  }

  public void setRealmId(String realmId) {
    this.realmId = realmId;
  }

  public String getRealmName() {
    return realmName;
  }

  public void setRealmName(String realmName) {
    this.realmName = realmName;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(String tenantKey) {
    this.tenantKey = tenantKey;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public Map<String, Object> getRoles() {
    return roles;
  }

  public void setRoles(Map<String, Object> roles) {
    this.roles = roles;
  }

  public List<String> getGroups() {
    return groups;
  }

  public void setGroups(List<String> groups) {
    this.groups = groups;
  }

  @Override
  public String toString() {
    return "WebhookPayload{" + "eventType='" + eventType + '\'' + ", time=" + time + ", realmId='"
        + realmId + '\'' + ", realmName='" + realmName + '\'' + ", tenantKey='" + tenantKey + '\''
        + ", userId='" + userId + '\'' + ", username='" + username + '\'' + ", email='" + email
        + '\'' + ", firstName='" + firstName + '\'' + ", lastName='" + lastName + '\''
        + ", enabled=" + enabled + '}';
  }

  public static class WebhookPayloadBuilder {
    private String eventType;
    private long time;
    private String realmId;
    private String realmName;
    private String tenantKey;
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private Map<String, String> attributes;
    private Map<String, Object> roles;
    private List<String> groups;

    public WebhookPayloadBuilder eventType(String eventType) {
      this.eventType = eventType;
      return this;
    }

    public WebhookPayloadBuilder time(long time) {
      this.time = time;
      return this;
    }

    public WebhookPayloadBuilder realmId(String realmId) {
      this.realmId = realmId;
      return this;
    }

    public WebhookPayloadBuilder realmName(String realmName) {
      this.realmName = realmName;
      return this;
    }

    public WebhookPayloadBuilder tenantKey(String tenantKey) {
      this.tenantKey = tenantKey;
      return this;
    }

    public WebhookPayloadBuilder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public WebhookPayloadBuilder username(String username) {
      this.username = username;
      return this;
    }

    public WebhookPayloadBuilder email(String email) {
      this.email = email;
      return this;
    }

    public WebhookPayloadBuilder firstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public WebhookPayloadBuilder lastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    public WebhookPayloadBuilder enabled(Boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public WebhookPayloadBuilder attributes(Map<String, String> attributes) {
      this.attributes = attributes;
      return this;
    }

    public WebhookPayloadBuilder roles(Map<String, Object> roles) {
      this.roles = roles;
      return this;
    }

    public WebhookPayloadBuilder groups(List<String> groups) {
      this.groups = groups;
      return this;
    }

    public WebhookPayload build() {
      return new WebhookPayload(this);
    }
  }
}
