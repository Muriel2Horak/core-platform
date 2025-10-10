package cz.muriel.core.monitoring.bff.service;

import cz.muriel.core.monitoring.bff.model.TenantBinding;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Service for resolving tenant to Grafana org mapping. Returns the Grafana
 * organization ID and service account token for a given tenant.
 */
public interface TenantOrgService {

  /**
   * Resolve tenant from JWT and return Grafana org binding.
   * 
   * @param jwt JWT token from authenticated user
   * @return TenantBinding with orgId and service account token
   * @throws IllegalArgumentException if tenant cannot be resolved
   * @throws IllegalStateException if org mapping is not configured
   */
  TenantBinding resolve(Jwt jwt);

  /**
   * Get tenant ID from JWT claims. Checks multiple claim locations: tenant_id,
   * tenant, realm_access.groups
   * 
   * @param jwt JWT token
   * @return tenant ID
   */
  String extractTenantId(Jwt jwt);
}
