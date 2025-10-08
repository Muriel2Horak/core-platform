package cz.muriel.core.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 🔧 Policy Methods
 * 
 * Helper metody pro použití v Spring Security @PreAuthorize SpEL expressions.
 * 
 * Použití:
 * 
 * <pre>
 * {@code @PreAuthorize("@policy.canRead('UserProfile', #id)")}
 * {@code @PreAuthorize("@policy.canWrite('Tenant', #tenantId)")}
 * {@code @PreAuthorize("@policy.canDelete('Role', #roleName)")}
 * </pre>
 * 
 * @version 2.0
 */
@Component("policyMethods") @RequiredArgsConstructor @Slf4j
public class PolicyMethods {

  private final PolicyEngine policyEngine;

  /**
   * Alias pro @policy.check
   */
  public boolean check(String entityType, String action, Object id) {
    return policyEngine.check(getAuth(), entityType, action, id);
  }

  /**
   * Zkontroluje READ permission
   */
  public boolean canRead(String entityType, Object id) {
    return policyEngine.check(getAuth(), entityType, "read", id);
  }

  /**
   * Zkontroluje CREATE permission
   */
  public boolean canCreate(String entityType) {
    return policyEngine.check(getAuth(), entityType, "create", null);
  }

  /**
   * Zkontroluje UPDATE permission
   */
  public boolean canUpdate(String entityType, Object id) {
    return policyEngine.check(getAuth(), entityType, "update", id);
  }

  /**
   * Zkontroluje WRITE permission (alias pro update nebo create)
   */
  public boolean canWrite(String entityType, Object id) {
    if (id == null) {
      return canCreate(entityType);
    }
    return canUpdate(entityType, id);
  }

  /**
   * Zkontroluje DELETE permission
   */
  public boolean canDelete(String entityType, Object id) {
    return policyEngine.check(getAuth(), entityType, "delete", id);
  }

  /**
   * Zkontroluje LIST permission
   */
  public boolean canList(String entityType) {
    return policyEngine.check(getAuth(), entityType, "list", null);
  }

  /**
   * Zkontroluje ASSIGN permission (např. přiřazení role)
   */
  public boolean canAssign(String entityType, Object id) {
    return policyEngine.check(getAuth(), entityType, "assign", id);
  }

  /**
   * Zkontroluje EXECUTE permission (např. spuštění akce)
   */
  public boolean canExecute(String entityType, String actionName, Object id) {
    return policyEngine.check(getAuth(), entityType, "execute:" + actionName, id);
  }

  /**
   * Získá aktuální Authentication
   */
  private Authentication getAuth() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      throw new IllegalStateException("No authentication found in security context");
    }
    return auth;
  }
}
