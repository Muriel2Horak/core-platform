package cz.muriel.core.tenant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {

  private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

  public static void setTenantKey(String tenantKey) {
    CURRENT_TENANT.set(tenantKey);
    log.debug("Tenant context set to: {}", tenantKey);
  }

  public static String getTenantKey() {
    return CURRENT_TENANT.get();
  }

  public static void clear() {
    String currentTenant = CURRENT_TENANT.get();
    CURRENT_TENANT.remove();
    log.debug("Tenant context cleared from: {}", currentTenant);
  }

  public static boolean hasTenant() {
    return CURRENT_TENANT.get() != null;
  }
}
