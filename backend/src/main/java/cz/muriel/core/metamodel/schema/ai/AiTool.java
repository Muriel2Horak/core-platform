package cz.muriel.core.metamodel.schema.ai;

import lombok.Data;
import java.util.Map;

/**
 * MCP (Model Context Protocol) tool declaration
 * 
 * Defines available AI tools and their contracts.
 * 
 * @since 2025-10-14
 */
@Data
public class AiTool {
  
  /**
   * Tool name/identifier (e.g., "ui_context.get_current_view")
   */
  private String name;
  
  /**
   * Human-readable description
   */
  private String description;
  
  /**
   * Input schema (JSON Schema)
   */
  private Map<String, Object> inputSchema;
  
  /**
   * Output schema (JSON Schema)
   */
  private Map<String, Object> outputSchema;
}
