package cz.muriel.core.metamodel.schema;

import cz.muriel.core.metamodel.schema.ai.GlobalAiConfig;
import lombok.Data;

/**
 * Global metamodel configuration
 */
@Data
public class GlobalMetamodelConfig {
  private StreamingGlobalConfig streaming = new StreamingGlobalConfig();
  
  // 🤖 AI global configuration (since 2025-10-14)
  private GlobalAiConfig ai = new GlobalAiConfig();
}
