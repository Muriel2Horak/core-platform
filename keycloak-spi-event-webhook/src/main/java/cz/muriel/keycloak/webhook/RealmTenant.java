package cz.muriel.keycloak.webhook;

/**
 * Data class representing tenant mapping for a realm
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    RealmTenant that = (RealmTenant) o;
    return java.util.Objects.equals(tenantKey, that.tenantKey)
        && java.util.Objects.equals(tenantId, that.tenantId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(tenantKey, tenantId);
  }
}
