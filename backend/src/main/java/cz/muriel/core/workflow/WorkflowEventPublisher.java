package cz.muriel.core.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 🔄 W5: Workflow Event Publisher
 * 
 * Publishes workflow events to Kafka for downstream processing. Events:
 * ENTER_STATE, EXIT_STATE, ACTION_APPLIED
 * 
 * @since 2025-10-14
 */
@Service @RequiredArgsConstructor @Slf4j
public class WorkflowEventPublisher {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final String TOPIC = "workflow.events";

  /**
   * Publish ENTER_STATE event
   */
  public void publishEnterState(String tenantId, String entityType, String entityId,
      String stateCode, String actor, UUID workflowInstanceId) {

    WorkflowModels.WorkflowEvent event = WorkflowModels.WorkflowEvent.builder()
        .eventId(UUID.randomUUID()).tenantId(tenantId).entityType(entityType).entityId(entityId)
        .workflowInstanceId(workflowInstanceId)
        .eventType(WorkflowModels.WorkflowEventType.ENTER_STATE).toStateCode(stateCode).actor(actor)
        .timestamp(Instant.now()).build();

    publishEvent(event);
  }

  /**
   * Publish EXIT_STATE event
   */
  public void publishExitState(String tenantId, String entityType, String entityId,
      String stateCode, String actor, Long durationMs, UUID workflowInstanceId) {

    WorkflowModels.WorkflowEvent event = WorkflowModels.WorkflowEvent.builder()
        .eventId(UUID.randomUUID()).tenantId(tenantId).entityType(entityType).entityId(entityId)
        .workflowInstanceId(workflowInstanceId)
        .eventType(WorkflowModels.WorkflowEventType.EXIT_STATE).fromStateCode(stateCode)
        .actor(actor).timestamp(Instant.now()).durationMs(durationMs).build();

    publishEvent(event);
  }

  /**
   * Publish ACTION_APPLIED event
   */
  public void publishActionApplied(String tenantId, String entityType, String entityId,
      String fromState, String toState, String transitionCode, String actor, Long durationMs,
      UUID workflowInstanceId) {

    WorkflowModels.WorkflowEvent event = WorkflowModels.WorkflowEvent.builder()
        .eventId(UUID.randomUUID()).tenantId(tenantId).entityType(entityType).entityId(entityId)
        .workflowInstanceId(workflowInstanceId)
        .eventType(WorkflowModels.WorkflowEventType.ACTION_APPLIED).fromStateCode(fromState)
        .toStateCode(toState).transitionCode(transitionCode).actor(actor).timestamp(Instant.now())
        .durationMs(durationMs).build();

    publishEvent(event);
  }

  /**
   * Publish ERROR event
   */
  public void publishError(String tenantId, String entityType, String entityId, String stateCode,
      String errorDetails, String actor, UUID workflowInstanceId) {

    WorkflowModels.WorkflowEvent event = WorkflowModels.WorkflowEvent.builder()
        .eventId(UUID.randomUUID()).tenantId(tenantId).entityType(entityType).entityId(entityId)
        .workflowInstanceId(workflowInstanceId).eventType(WorkflowModels.WorkflowEventType.ERROR)
        .fromStateCode(stateCode).actor(actor).timestamp(Instant.now()).errorDetails(errorDetails)
        .build();

    publishEvent(event);
  }

  /**
   * Internal: Publish event to Kafka and persist to DB
   */
  private void publishEvent(WorkflowModels.WorkflowEvent event) {
    try {
      String key = event.getEntityType() + ":" + event.getEntityId();
      String value = objectMapper.writeValueAsString(toEventPayload(event));

      kafkaTemplate.send(TOPIC, key, value).whenComplete((result, ex) -> {
        if (ex != null) {
          log.error("Failed to publish workflow event: {}", event.getEventId(), ex);
        } else {
          log.debug("Published workflow event: {} to topic {}", event.getEventId(), TOPIC);
        }
      });

    } catch (Exception e) {
      log.error("Failed to serialize workflow event: {}", event.getEventId(), e);
    }
  }

  /**
   * Convert event to Kafka payload format
   */
  private Map<String, Object> toEventPayload(WorkflowModels.WorkflowEvent event) {
    Map<String, Object> payload = new java.util.HashMap<>();
    payload.put("eventId", event.getEventId().toString());
    payload.put("tenantId", event.getTenantId());
    payload.put("entityType", event.getEntityType());
    payload.put("entityId", event.getEntityId());
    payload.put("workflowInstanceId",
        event.getWorkflowInstanceId() != null ? event.getWorkflowInstanceId().toString() : null);
    payload.put("eventType", event.getEventType().name());
    payload.put("fromStateCode", event.getFromStateCode());
    payload.put("toStateCode", event.getToStateCode());
    payload.put("transitionCode", event.getTransitionCode());
    payload.put("actor", event.getActor());
    payload.put("timestamp", event.getTimestamp().toString());
    payload.put("durationMs", event.getDurationMs());
    payload.put("errorDetails", event.getErrorDetails());
    return payload;
  }
}
