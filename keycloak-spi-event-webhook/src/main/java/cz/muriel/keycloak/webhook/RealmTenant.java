package cz.muriel.keycloak.webhook;

/**
 * Simple data class representing a realm-tenant mapping for testing purposes
 */
public class RealmTenant {
  private final String tenantKey;
  private final String tenantId;

  public RealmTenant(String tenantKey, String tenantId) {
    this.tenantKey = tenantKey;
    this.tenantId = tenantId;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public String getTenantId() {
    return tenantId;
  }

  @Override
  public String toString() {
    return "RealmTenant{" + "tenantKey='" + tenantKey + '\'' + ", tenantId='" + tenantId + '\''
        + '}';
  }
}
