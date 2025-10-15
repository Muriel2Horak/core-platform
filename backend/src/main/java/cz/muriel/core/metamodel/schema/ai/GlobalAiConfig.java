package cz.muriel.core.metamodel.schema.ai;

import lombok.Data;
import java.util.List;

/**
 * Global AI configuration from global-config.yaml
 * 
 * @since 2025-10-14
 */
@Data
public class GlobalAiConfig {

  /**
   * Is AI feature enabled globally? Master kill-switch. If false, all AI
   * endpoints return 404/disabled
   */
  private Boolean enabled = false;

  /**
   * Global AI mode
   */
  private AiVisibilityMode mode = AiVisibilityMode.META_ONLY;

  /**
   * Global AI policies
   */
  private AiPolicies policies = new AiPolicies();

  /**
   * Global AI prompts
   */
  private AiPrompts prompts = new AiPrompts();

  /**
   * Global AI tools
   */
  private List<AiTool> tools = List.of();
}
