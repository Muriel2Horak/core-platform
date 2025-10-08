package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.Map;

/**
 * State configuration for workflow
 */
@Data
public class StateConfig {
    private String code;
    private String label;
    private String description;
}
