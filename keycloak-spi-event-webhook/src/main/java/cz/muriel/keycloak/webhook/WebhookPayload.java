package cz.muriel.keycloak.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple POJO for webhook payload
 */
public class WebhookPayload {

  @JsonProperty("eventType")
  private String eventType;

  @JsonProperty("time")
  private long time;

  @JsonProperty("type")
  private String type;

  @JsonProperty("realm")
  private String realm;

  @JsonProperty("tenantKey")
  private String tenantKey;

  @JsonProperty("tenantId")
  private String tenantId;

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

  // Default constructor
  public WebhookPayload() {
  }

  // Constructor
  public WebhookPayload(String eventType, long time, String type, String realm, String tenantKey,
      String tenantId, String userId, String username, String email, String firstName,
      String lastName) {
    this.eventType = eventType;
    this.time = time;
    this.type = type;
    this.realm = realm;
    this.tenantKey = tenantKey;
    this.tenantId = tenantId;
    this.userId = userId;
    this.username = username;
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getRealm() {
    return realm;
  }

  public void setRealm(String realm) {
    this.realm = realm;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(String tenantKey) {
    this.tenantKey = tenantKey;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
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

  @Override
  public String toString() {
    return "WebhookPayload{" + "eventType='" + eventType + '\'' + ", time=" + time + ", type='"
        + type + '\'' + ", realm='" + realm + '\'' + ", tenantKey='" + tenantKey + '\''
        + ", tenantId='" + tenantId + '\'' + ", userId='" + userId + '\'' + ", username='"
        + username + '\'' + ", email='" + email + '\'' + ", firstName='" + firstName + '\''
        + ", lastName='" + lastName + '\'' + '}';
  }
}
