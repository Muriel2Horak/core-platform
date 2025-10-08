package cz.muriel.core.security.policy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 🔐 Policy Models
 * 
 * Modely pro definice politik v metamodelu
 */
public class PolicyModels {

  /**
   * Access Policy - kdo má přístup k jaké akci nad entitou
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class AccessPolicy {
    private String entityType;
    private String action; // read, create, update, delete, list
    private PolicyRule rule;
    private int priority; // vyšší = dřív se vyhodnotí
  }

  /**
   * Column Policy - masking/projekce sloupců
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class ColumnPolicy {
    private String entityType;
    private String action;
    private Set<String> visibleColumns; // null = všechny
    private Set<String> hiddenColumns; // null = žádné
    private PolicyRule rule;
  }

  /**
   * Row Policy - filtrování řádků
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class RowPolicy {
    private String entityType;
    private String action;
    private String filterExpression; // např. "tenant.key = :tenantKey"
    private PolicyRule rule;
  }

  /**
   * Policy Rule - podmínka vyhodnocení
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class PolicyRule {
    private RuleType type;
    private String expression; // např. "hasRole('ADMIN') OR (hasRole('TENANT_ADMIN') AND
                               // sameTenant())"
    private List<PolicyRule> conditions; // pro AND/OR

    public enum RuleType {
      ALLOW, // Explicitně povolit
      DENY, // Explicitně zakázat
      ROLE, // Kontrola role
      GROUP, // Kontrola skupiny
      ATTRIBUTE, // Kontrola atributu entity
      STATE, // Kontrola stavu entity
      TENANT, // Tenant isolation
      EXPRESSION, // SpEL expression
      AND, // Logické AND
      OR // Logické OR
    }
  }

  /**
   * Menu Policy - UI menu items
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class MenuPolicy {
    private String id;
    private String label;
    private String path;
    private String icon;
    private PolicyRule rule;
    private List<MenuPolicy> submenu;
    private int order;
  }

  /**
   * Feature Policy - UI feature flags
   */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class FeaturePolicy {
    private String featureId;
    private String description;
    private PolicyRule rule;
  }
}
