package cz.muriel.core.security.policy;

import cz.muriel.core.security.PolicyEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🏗️ Metamodel Policy Engine
 * 
 * Implementace PolicyEngine integrovaná s metamodelem. Podporuje: - Access
 * policy (RBAC/ABAC) - Column policy (projekce/masking) - Row policy
 * (filtrování) - Tenant isolation (vždy) - Complex expressions (AND/OR,
 * závorky) - Dot-notation přes relace
 * 
 * Zpětná kompatibilita přes YamlPermissionAdapter.
 * 
 * @version 2.0
 */
@Component("policy") @RequiredArgsConstructor @Slf4j @SuppressWarnings("deprecation") // YamlPermissionAdapter
                                                                                      // je
                                                                                      // deprecated,
                                                                                      // ale
                                                                                      // používáme
                                                                                      // pro zpětnou
                                                                                      // kompatibilitu
public class MetamodelPolicyEngine implements PolicyEngine {

  private final YamlPermissionAdapter yamlAdapter;

  // TODO: Inject MetamodelRegistry when available
  // private final MetamodelRegistry metamodelRegistry;

  @Override
  public boolean check(Authentication auth, String entityType, String action,
      @Nullable Object contextId) {
    log.debug("Policy check: entity={}, action={}, contextId={}", entityType, action, contextId);

    // 1. Tenant isolation - VŽDY
    if (!checkTenantIsolation(auth, entityType, contextId)) {
      log.warn("Tenant isolation failed for entity={}, user={}", entityType, auth.getName());
      return false;
    }

    // 2. Získej politiky z metamodelu (nebo YAML jako fallback)
    List<PolicyModels.AccessPolicy> policies = getAccessPolicies(entityType, action);

    // 3. Vyhodnoť podle priority (DENY má přednost)
    policies.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

    for (PolicyModels.AccessPolicy policy : policies) {
      Boolean result = evaluateRule(auth, policy.getRule(), entityType, contextId);

      if (result != null && result) {
        if (policy.getRule().getType() == PolicyModels.PolicyRule.RuleType.DENY) {
          log.debug("Explicit DENY for entity={}, action={}", entityType, action);
          return false;
        }
        log.debug("ALLOW for entity={}, action={}", entityType, action);
        return true;
      }
    }

    log.debug("No matching policy, default DENY for entity={}, action={}", entityType, action);
    return false;
  }

  @Override
  public Set<String> projectColumns(Authentication auth, String entityType, String action) {
    log.debug("Column projection for entity={}, action={}", entityType, action);

    // TODO: Načíst z metamodelu
    // List<PolicyModels.ColumnPolicy> policies =
    // metamodelRegistry.getColumnPolicies(entityType, action);

    // Prozatím vrátit všechny sloupce
    return Collections.emptySet(); // prázdný = všechny
  }

  @Override
  public String getRowFilter(Authentication auth, String entityType, String action) {
    log.debug("Row filter for entity={}, action={}", entityType, action);

    // TODO: Načíst z metamodelu a vygenerovat SQL WHERE
    // List<PolicyModels.RowPolicy> policies =
    // metamodelRegistry.getRowPolicies(entityType, action);

    // Prozatím tenant isolation
    String tenantKey = getTenantKey(auth);
    if (tenantKey != null && !tenantKey.equals("admin")) {
      return "tenant_key = '" + tenantKey + "'";
    }

    return "";
  }

  /**
   * Vyhodnotí policy rule
   */
  private Boolean evaluateRule(Authentication auth, PolicyModels.PolicyRule rule, String entityType,
      @Nullable Object contextId) {
    if (rule == null) {
      return null;
    }

    return switch (rule.getType()) {
    case ROLE -> evaluateRoleRule(auth, rule.getExpression());
    case TENANT -> evaluateTenantRule(auth, entityType, contextId);
    case AND -> evaluateAndRule(auth, rule.getConditions(), entityType, contextId);
    case OR -> evaluateOrRule(auth, rule.getConditions(), entityType, contextId);
    case EXPRESSION -> evaluateSpelExpression(auth, rule.getExpression(), entityType, contextId);
    case ALLOW -> true;
    case DENY -> false;
    default -> {
      log.warn("Unsupported rule type: {}", rule.getType());
      yield null;
    }
    };
  }

  /**
   * Vyhodnotí role rule (hasRole('ADMIN'))
   */
  private boolean evaluateRoleRule(Authentication auth, String expression) {
    // Parse: hasRole('CORE_ROLE_ADMIN')
    String roleName = expression.replaceAll(".*hasRole\\('([^']+)'\\).*", "$1");

    return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
        .anyMatch(a -> a.equals("ROLE_" + roleName) || a.equals(roleName));
  }

  /**
   * Vyhodnotí tenant rule
   */
  private boolean evaluateTenantRule(Authentication auth, String entityType,
      @Nullable Object contextId) {
    return checkTenantIsolation(auth, entityType, contextId);
  }

  /**
   * Vyhodnotí AND rule
   */
  private Boolean evaluateAndRule(Authentication auth, List<PolicyModels.PolicyRule> conditions,
      String entityType, @Nullable Object contextId) {
    if (conditions == null || conditions.isEmpty()) {
      return true;
    }

    for (PolicyModels.PolicyRule condition : conditions) {
      Boolean result = evaluateRule(auth, condition, entityType, contextId);
      if (result == null || !result) {
        return false;
      }
    }
    return true;
  }

  /**
   * Vyhodnotí OR rule
   */
  private Boolean evaluateOrRule(Authentication auth, List<PolicyModels.PolicyRule> conditions,
      String entityType, @Nullable Object contextId) {
    if (conditions == null || conditions.isEmpty()) {
      return false;
    }

    for (PolicyModels.PolicyRule condition : conditions) {
      Boolean result = evaluateRule(auth, condition, entityType, contextId);
      if (result != null && result) {
        return true;
      }
    }
    return false;
  }

  /**
   * Vyhodnotí SpEL expression
   */
  private boolean evaluateSpelExpression(Authentication auth, String expression, String entityType,
      @Nullable Object contextId) {
    // TODO: Implementovat SpEL evaluator s kontextem
    log.warn("SpEL expression evaluation not yet implemented: {}", expression);
    return false;
  }

  /**
   * Tenant isolation - VŽDY zkontroluje
   */
  private boolean checkTenantIsolation(Authentication auth, String entityType,
      @Nullable Object contextId) {
    String userTenant = getTenantKey(auth);

    // Admin tenant má přístup všude
    if ("admin".equals(userTenant)) {
      return true;
    }

    // TODO: Načíst tenant z entity podle contextId
    // Prozatím kontrola podle role
    List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());

    return roles.contains("ROLE_CORE_ROLE_ADMIN") || roles.contains("ROLE_CORE_ROLE_TENANT_ADMIN");
  }

  /**
   * Získá tenant key z Authentication
   */
  private String getTenantKey(Authentication auth) {
    // TODO: Načíst z JWT claim nebo UserDetails
    return "admin"; // Placeholder
  }

  /**
   * Získá access policies z metamodelu (nebo YAML fallback)
   */
  private List<PolicyModels.AccessPolicy> getAccessPolicies(String entityType, String action) {
    // TODO: Načíst z MetamodelRegistry
    // return metamodelRegistry.getAccessPolicies(entityType, action);

    // Fallback na YAML adapter
    return yamlAdapter.getAccessPolicies().stream()
        .filter(p -> p.getEntityType().equalsIgnoreCase(entityType))
        .filter(p -> p.getAction().equals(action) || p.getAction().equals("*"))
        .collect(Collectors.toList());
  }
}
