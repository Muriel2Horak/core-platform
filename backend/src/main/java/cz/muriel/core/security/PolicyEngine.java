package cz.muriel.core.security;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;

import java.util.Set;

/**
 * 🔐 PolicyEngine Interface
 * 
 * Centralizované rozhraní pro vyhodnocování politik přístupu. Integrováno s
 * metamodelem pro dynamické RBAC/ABAC.
 * 
 * @author core-platform
 * @version 2.0
 * @since 2025-10-08
 */
public interface PolicyEngine {

  /**
   * Zkontroluje, zda má uživatel oprávnění k akci nad entitou.
   * 
   * @param auth Spring Security Authentication
   * @param entityType typ entity (např. "UserProfile", "Tenant", "Role")
   * @param action akce (např. "read", "create", "update", "delete")
   * @param contextId ID konkrétní entity (nullable pro create/list operace)
   * @return true pokud má oprávnění, false jinak
   */
  boolean check(Authentication auth, String entityType, String action, @Nullable Object contextId);

  /**
   * Vrátí seznam sloupců, které uživatel může vidět pro danou entitu a akci.
   * Slouží pro column-level security (projekce/masking).
   * 
   * @param auth Spring Security Authentication
   * @param entityType typ entity
   * @param action akce (typicky "read")
   * @return set názvů sloupců, které lze zobrazit
   */
  Set<String> projectColumns(Authentication auth, String entityType, String action);

  /**
   * Zkontroluje oprávnění s rozšířeným kontextem (custom atributy).
   * 
   * @param auth Spring Security Authentication
   * @param entityType typ entity
   * @param action akce
   * @param contextId ID entity
   * @param additionalContext další kontextové parametry pro vyhodnocení
   * @return true pokud má oprávnění
   */
  default boolean checkWithContext(Authentication auth, String entityType, String action,
      @Nullable Object contextId, java.util.Map<String, Object> additionalContext) {
    return check(auth, entityType, action, contextId);
  }

  /**
   * Vrátí filtr pro row-level security (WHERE clause).
   * 
   * @param auth Spring Security Authentication
   * @param entityType typ entity
   * @param action akce
   * @return SQL WHERE fragment nebo prázdný string pokud bez omezení
   */
  default String getRowFilter(Authentication auth, String entityType, String action) {
    return "";
  }

  /**
   * Zkontroluje, zda má uživatel danou roli.
   * 
   * @param auth Spring Security Authentication
   * @param role název role (např. "CORE_ROLE_ADMIN")
   * @return true pokud má roli
   */
  default boolean hasRole(Authentication auth, String role) {
    if (auth == null)
      return false;
    return auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals(role) || a.getAuthority().equals("ROLE_" + role));
  }
}
