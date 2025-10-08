package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.Map;

/**
 * Transition configuration for workflow
 */
@Data
public class TransitionConfig {
    private String code;
    private String from;
    private String to;
    private String label;
    private Map<String, Object> guard;
    private Integer slaMinutes;
}
