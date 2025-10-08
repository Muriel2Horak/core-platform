package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Policy rule supporting RBAC + ABAC Supports: anyOf, allOf, role, group,
 * sameUser, eq, ne, contains, in
 */
@Data
public class PolicyRule {
  // Logical operators
  private List<PolicyRule> anyOf;
  private List<PolicyRule> allOf;

  // RBAC
  private String role;
  private String group;

  // ABAC
  private Boolean sameUser;
  private Condition eq;
  private Condition ne;
  private Condition contains;
  private Condition in;
}
