package cz.muriel.core.security.policy;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.*;
import cz.muriel.core.security.PolicyEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🏗️ Metamodel Policy Engine
 * 
 * Implementace PolicyEngine integrovaná s metamodelem. Podporuje: - Access
 * policy (RBAC/ABAC) - Column policy (projekce/masking) - Tenant isolation
 * (RLS) - anyOf/allOf operators - Dot-notation (1 level deep)
 * 
 * @version 3.0 - Using MetamodelRegistry
 */
@Component("metamodelPolicy") @RequiredArgsConstructor @Slf4j
public class MetamodelPolicyEngine implements PolicyEngine {

  private final MetamodelRegistry registry;

  private static final String ROLE_ADMIN = "CORE_ROLE_ADMIN";

  @Override
  public boolean check(Authentication auth, String entityType, String action,
      @Nullable Object contextId) {
    log.debug("Policy check: entity={}, action={}, contextId={}", entityType, action, contextId);

    if (auth == null) {
      log.debug("No authentication, denying access");
      return false;
    }

    // Admin bypass
    if (hasRole(auth, ROLE_ADMIN)) {
      log.debug("CORE_ROLE_ADMIN detected, granting access");
      return true;
    }

    // Get schema
    Optional<EntitySchema> schemaOpt = registry.getSchema(entityType);
    if (schemaOpt.isEmpty()) {
      log.warn("No schema found for entity type: {}", entityType);
      return false;
    }

    EntitySchema schema = schemaOpt.get();

    if (schema.getAccessPolicy() == null) {
      log.warn("No access policy for entity {}, denying access", entityType);
      return false;
    }

    // Get rule for action
    PolicyRule rule = getActionRule(schema.getAccessPolicy(), action);
    if (rule == null) {
      log.debug("No policy rule for action {} on entity {}, denying", action, entityType);
      return false;
    }

    // Tenant isolation (if not admin and entity has tenantField)
    if (!hasRole(auth, ROLE_ADMIN) && schema.getTenantField() != null && contextId != null) {
      String userTenantId = getTenantId(auth);
      String entityTenantId = extractFieldValue(contextId, schema.getTenantField());

      if (entityTenantId != null && !entityTenantId.equals(userTenantId)) {
        log.debug("Tenant isolation violation: user={}, entity={}", userTenantId, entityTenantId);
        return false;
      }
    }

    boolean result = evaluatePolicyRule(auth, rule, contextId);
    log.debug("Policy check result: entity={}, action={}, result={}", entityType, action, result);
    return result;
  }

  @Override
  public Set<String> projectColumns(Authentication auth, String entityType, String action) {
    log.debug("Column projection for entity={}, action={}", entityType, action);

    Optional<EntitySchema> schemaOpt = registry.getSchema(entityType);
    if (schemaOpt.isEmpty()) {
      return Collections.emptySet();
    }

    EntitySchema schema = schemaOpt.get();

    // Admin sees all columns
    if (hasRole(auth, ROLE_ADMIN)) {
      return schema.getFields().stream().map(FieldSchema::getName).collect(Collectors.toSet());
    }

    Set<String> allowedColumns = new HashSet<>();

    // Base columns from entity-level policy
    if (schema.getAccessPolicy() != null && check(auth, entityType, action, null)) {
      // Start with all columns
      allowedColumns.addAll(
          schema.getFields().stream().map(FieldSchema::getName).collect(Collectors.toSet()));
    }

    // Filter by column-level policies
    if (schema.getAccessPolicy() != null && schema.getAccessPolicy().getColumns() != null) {
      for (var entry : schema.getAccessPolicy().getColumns().entrySet()) {
        String columnName = entry.getKey();
        ColumnPolicy colPolicy = entry.getValue();

        PolicyRule colRule = action.equals("read") || action.equals("create") ? colPolicy.getRead()
            : colPolicy.getWrite();

        if (colRule != null && !evaluatePolicyRule(auth, colRule, null)) {
          allowedColumns.remove(columnName);
        }
      }
    }

    log.debug("Column projection result: entity={}, action={}, columns={}", entityType, action,
        allowedColumns);
    return allowedColumns;
  }

  @Override
  public String getRowFilter(Authentication auth, String entityType, String action) {
    String tenantId = getTenantId(auth);

    // Admin sees all
    if (hasRole(auth, ROLE_ADMIN)) {
      return "";
    }

    // Tenant isolation
    Optional<EntitySchema> schemaOpt = registry.getSchema(entityType);
    if (schemaOpt.isPresent() && schemaOpt.get().getTenantField() != null) {
      return schemaOpt.get().getTenantField() + " = '" + tenantId + "'";
    }

    return "";
  }

  /**
   * Get policy rule for action
   */
  private PolicyRule getActionRule(AccessPolicy policy, String action) {
    return switch (action.toLowerCase()) {
    case "read" -> policy.getRead();
    case "create" -> policy.getCreate();
    case "update" -> policy.getUpdate();
    case "delete" -> policy.getDelete();
    default -> null;
    };
  }

  /**
   * Evaluate policy rule recursively
   */
  private boolean evaluatePolicyRule(Authentication auth, PolicyRule rule,
      @Nullable Object entity) {
    if (rule == null) {
      return false;
    }

    // Logical operators
    if (rule.getAnyOf() != null && !rule.getAnyOf().isEmpty()) {
      return rule.getAnyOf().stream().anyMatch(r -> evaluatePolicyRule(auth, r, entity));
    }

    if (rule.getAllOf() != null && !rule.getAllOf().isEmpty()) {
      return rule.getAllOf().stream().allMatch(r -> evaluatePolicyRule(auth, r, entity));
    }

    // RBAC
    if (rule.getRole() != null) {
      return hasRole(auth, rule.getRole());
    }

    if (rule.getGroup() != null) {
      // Groups are mapped to roles for now (future: explicit group membership)
      return hasRole(auth, rule.getGroup());
    }

    // ABAC
    if (rule.getSameUser() != null && rule.getSameUser()) {
      if (entity == null)
        return false;
      String entityUserId = extractFieldValue(entity, "user_id");
      String currentUserId = getUserId(auth);
      return entityUserId != null && entityUserId.equals(currentUserId);
    }

    if (rule.getEq() != null) {
      return evaluateCondition(auth, rule.getEq(), entity, Objects::equals);
    }

    if (rule.getNe() != null) {
      return evaluateCondition(auth, rule.getNe(), entity, (a, b) -> !Objects.equals(a, b));
    }

    if (rule.getContains() != null) {
      return evaluateCondition(auth, rule.getContains(), entity,
          (a, b) -> a != null && b != null && a.toString().contains(b.toString()));
    }

    if (rule.getIn() != null) {
      return evaluateCondition(auth, rule.getIn(), entity, (a, b) -> b != null
          && Arrays.asList(b.toString().split(",")).contains(a != null ? a.toString() : ""));
    }

    return false;
  }

  /**
   * Evaluate condition (eq, ne, contains, in)
   */
  private boolean evaluateCondition(Authentication auth, Condition condition,
      @Nullable Object entity, java.util.function.BiPredicate<Object, Object> predicate) {
    Object leftValue = resolveValue(auth, condition.getLeft(), entity);
    Object rightValue = resolveValue(auth, condition.getRight(), entity);

    return predicate.test(leftValue, rightValue);
  }

  /**
   * Resolve value from expression Supports: ${entity.field}, ${user.claim},
   * literal values
   */
  private Object resolveValue(Authentication auth, String expression, @Nullable Object entity) {
    if (expression == null) {
      return null;
    }

    if (expression.startsWith("${") && expression.endsWith("}")) {
      String path = expression.substring(2, expression.length() - 1);

      if (path.startsWith("entity.")) {
        String fieldName = path.substring(7);
        return entity != null ? extractFieldValue(entity, fieldName) : null;
      }

      if (path.startsWith("user.")) {
        String claim = path.substring(5);
        return switch (claim) {
        case "tenant_id" -> getTenantId(auth);
        case "user_id", "id" -> getUserId(auth);
        default -> getClaimValue(auth, claim);
        };
      }
    }

    return expression; // Literal value
  }

  /**
   * Extract field value from entity (supports Map and Bean)
   */
  private String extractFieldValue(Object entity, String fieldName) {
    if (entity == null) {
      return null;
    }

    try {
      if (entity instanceof Map<?, ?> map) {
        Object value = map.get(fieldName);
        return value != null ? value.toString() : null;
      }

      // Bean property access via reflection
      String methodName = "get" + Character.toUpperCase(fieldName.charAt(0))
          + fieldName.substring(1);
      var method = entity.getClass().getMethod(methodName);
      Object value = method.invoke(entity);
      return value != null ? value.toString() : null;

    } catch (Exception e) {
      log.debug("Failed to extract field {} from entity: {}", fieldName, e.getMessage());
      return null;
    }
  }

  /**
   * Get claim value from JWT
   */
  private String getClaimValue(Authentication auth, String claim) {
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      return jwt.getClaimAsString(claim);
    }
    return null;
  }

  @Override
  public boolean hasRole(Authentication auth, String role) {
    if (auth == null) {
      return false;
    }

    return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
        .anyMatch(a -> a.equals(role) || a.equals("ROLE_" + role));
  }

  private String getTenantId(Authentication auth) {
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      return jwt.getClaimAsString("tenant_id");
    }
    return "admin"; // fallback
  }

  private String getUserId(Authentication auth) {
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String sub = jwt.getSubject();
      return sub != null ? sub : jwt.getClaimAsString("user_id");
    }
    return auth != null ? auth.getName() : "unknown";
  }
}
