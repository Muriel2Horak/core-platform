package cz.muriel.core.metamodel.schema;

import lombok.Data;

/**
 * Global metamodel configuration
 */
@Data
public class GlobalMetamodelConfig {
  private StreamingGlobalConfig streaming = new StreamingGlobalConfig();
}
