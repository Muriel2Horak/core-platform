package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.Map;

/**
 * Lifecycle action to execute at specific point in entity lifecycle
 */
@Data
public class LifecycleAction {

  /**
   * Action type: - setField: Set field value - generateId: Generate deterministic
   * ID - validate: Run validation - audit: Log action - notify: Send notification
   * - execute: Execute custom code
   */
  private String type;

  /**
   * Target field name (for setField, generateId)
   */
  private String field;

  /**
   * Value or expression (for setField) Can be: - Static value: "ACTIVE" -
   * Expression: "${user.id}" - Function: "now()", "uuid()"
   */
  private String value;

  /**
   * Additional parameters for the action
   */
  private Map<String, Object> params;
}
