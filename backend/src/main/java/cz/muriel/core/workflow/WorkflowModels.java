package cz.muriel.core.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 🔄 Workflow Models
 */
public class WorkflowModels {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityState {
        private String entityType;
        private String entityId;
        private String tenantId;
        private String stateCode;
        private Instant since;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateTransition {
        private String entityType;
        private String fromCode;
        private String toCode;
        private String code;
        private Map<String, Object> guard;
        private Integer slaMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateLog {
        private Long id;
        private String entityType;
        private String entityId;
        private String tenantId;
        private String fromCode;
        private String toCode;
        private String changedBy;
        private Instant changedAt;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransitionResult {
        private boolean success;
        private String message;
        private EntityState newState;
        private SlaStatus slaStatus;
    }

    public enum SlaStatus {
        NONE,
        OK,
        WARN,
        BREACH
    }
}
