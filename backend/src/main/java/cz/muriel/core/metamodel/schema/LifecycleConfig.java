package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.List;

/**
 * Lifecycle hooks configuration
 */
@Data
public class LifecycleConfig {

  /**
   * Actions to execute before entity creation
   */
  private List<LifecycleAction> beforeCreate;

  /**
   * Actions to execute after entity creation
   */
  private List<LifecycleAction> afterCreate;

  /**
   * Actions to execute before entity update
   */
  private List<LifecycleAction> beforeUpdate;

  /**
   * Actions to execute after entity update
   */
  private List<LifecycleAction> afterUpdate;

  /**
   * Actions to execute before entity deletion
   */
  private List<LifecycleAction> beforeDelete;

  /**
   * Actions to execute after entity deletion
   */
  private List<LifecycleAction> afterDelete;
}
