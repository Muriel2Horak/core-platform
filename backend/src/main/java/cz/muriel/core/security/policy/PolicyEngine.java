package cz.muriel.core.security.policy;

import org.springframework.security.core.Authentication;

import java.util.Set;

/**
 * Policy engine for RBAC + ABAC evaluation
 */
public interface PolicyEngine {

  /**
   * Check if action is permitted on entity
   * 
   * @param auth Current authentication
   * @param entityType Entity type from metamodel
   * @param action Action: read, create, update, delete
   * @param entity Optional entity instance for ABAC (can be null for list
   * queries)
   * @return true if permitted
   */
  boolean check(Authentication auth, String entityType, String action, Object entity);

  /**
   * Get allowed column names for action Returns whitelist of columns user can
   * access
   * 
   * @param auth Current authentication
   * @param entityType Entity type from metamodel
   * @param action Action: read, create, update, delete
   * @return Set of allowed column names (empty = deny all)
   */
  Set<String> projectColumns(Authentication auth, String entityType, String action);

  /**
   * Check if user has specific role
   */
  boolean hasRole(Authentication auth, String role);

  /**
   * Get tenant ID from authentication
   */
  String getTenantId(Authentication auth);

  /**
   * Get user ID from authentication
   */
  String getUserId(Authentication auth);
}
