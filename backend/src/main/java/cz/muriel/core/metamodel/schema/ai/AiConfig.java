package cz.muriel.core.metamodel.schema.ai;

import lombok.Data;
import java.util.List;

/**
 * AI configuration for entity/global metamodel
 * 
 * Defines policies, prompts, and tools for AI agent interactions. All
 * configurations default to META_ONLY mode for security.
 * 
 * @since 2025-10-14
 */
@Data
public class AiConfig {

  /**
   * AI policies for data visibility and safety
   */
  private AiPolicies policies;

  /**
   * AI prompts for system and user agents
   */
  private AiPrompts prompts;

  /**
   * MCP tool contracts available for this entity
   */
  private List<AiTool> tools;
}
