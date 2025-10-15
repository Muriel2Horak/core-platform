package cz.muriel.core.metamodel.schema.ai;

import lombok.Data;
import java.util.Map;

/**
 * AI prompts configuration
 * 
 * System prompts for different AI agent types.
 * 
 * @since 2025-10-14
 */
@Data
public class AiPrompts {

  /**
   * System prompt for user-facing agent
   */
  private Map<String, String> userAgent;

  /**
   * System prompt for developer/ops agent
   */
  private Map<String, String> devAgent;
}
