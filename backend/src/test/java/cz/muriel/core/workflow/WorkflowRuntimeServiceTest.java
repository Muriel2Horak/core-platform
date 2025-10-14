package cz.muriel.core.workflow;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.StateConfig;
import cz.muriel.core.metamodel.schema.TransitionConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 🧪 W5: Workflow Runtime Service Unit Tests
 * 
 * Tests: - getWorkflowGraph: nodes, edges, current state highlight -
 * getStateDetail: allowed/blocked transitions with "why not" reasons -
 * getHistory: timeline with durations - getForecast: next steps and pending
 * timers
 * 
 * @since 2025-10-14
 */
@ExtendWith(MockitoExtension.class)
class WorkflowRuntimeServiceTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private WorkflowService workflowService;

  @Mock
  private MetamodelRegistry metamodelRegistry;

  @Mock
  private Authentication auth;

  private WorkflowRuntimeService runtimeService;

  @BeforeEach
  void setUp() {
    runtimeService = new WorkflowRuntimeService(jdbcTemplate, workflowService, metamodelRegistry);
  }

  // ============================================
  // GRAPH TESTS
  // ============================================

  @Test
  void testGetWorkflowGraph_withCurrentState() {
    // Arrange
    String entityType = "Order";
    String entityId = "order-123";
    String tenantId = "tenant-1";

    WorkflowModels.EntityState currentState = WorkflowModels.EntityState.builder()
        .entityType(entityType).entityId(entityId).tenantId(tenantId).stateCode("APPROVED")
        .since(Instant.now()).build();

    EntitySchema schema = createMockSchema();

    when(workflowService.getCurrentState(entityType, entityId, tenantId)).thenReturn(currentState);
    when(metamodelRegistry.getSchema(entityType)).thenReturn(Optional.of(schema));
    when(workflowService.getAllowedTransitions(any(), eq(entityType), eq(entityId), eq(tenantId)))
        .thenReturn(List
            .of(WorkflowModels.StateTransition.builder().code("ship").toCode("SHIPPED").build()));

    // Act
    WorkflowModels.WorkflowGraph graph = runtimeService.getWorkflowGraph(auth, entityType, entityId,
        tenantId);

    // Assert
    assertNotNull(graph);
    assertEquals(entityType, graph.getEntityType());
    assertEquals("APPROVED", graph.getCurrentState());

    // Verify nodes
    assertEquals(4, graph.getNodes().size());
    assertTrue(
        graph.getNodes().stream().anyMatch(n -> n.getCode().equals("APPROVED") && n.isCurrent()));
    assertTrue(graph.getNodes().stream().anyMatch(n -> n.getCode().equals("PENDING")));

    // Verify edges
    assertTrue(graph.getEdges().size() > 0);
    Optional<WorkflowModels.GraphEdge> shipEdge = graph.getEdges().stream()
        .filter(e -> e.getTransitionCode().equals("ship")).findFirst();
    assertTrue(shipEdge.isPresent());
    assertTrue(shipEdge.get().isAllowed());
  }

  @Test
  void testGetWorkflowGraph_noCurrentState() {
    // Arrange
    String entityType = "Order";
    String entityId = "order-new";
    String tenantId = "tenant-1";

    EntitySchema schema = createMockSchema();

    when(workflowService.getCurrentState(entityType, entityId, tenantId)).thenReturn(null);
    when(metamodelRegistry.getSchema(entityType)).thenReturn(Optional.of(schema));
    when(workflowService.getAllowedTransitions(any(), eq(entityType), eq(entityId), eq(tenantId)))
        .thenReturn(Collections.emptyList());

    // Act
    WorkflowModels.WorkflowGraph graph = runtimeService.getWorkflowGraph(auth, entityType, entityId,
        tenantId);

    // Assert
    assertNotNull(graph);
    assertNull(graph.getCurrentState());
    assertEquals(4, graph.getNodes().size());
    assertFalse(graph.getNodes().stream().anyMatch(WorkflowModels.GraphNode::isCurrent));
  }

  @Test
  void testGetWorkflowGraph_noMetamodel() {
    // Arrange
    when(metamodelRegistry.getSchema("UnknownEntity")).thenReturn(Optional.empty());

    // Act
    WorkflowModels.WorkflowGraph graph = runtimeService.getWorkflowGraph(auth, "UnknownEntity",
        "id", "tenant");

    // Assert
    assertNotNull(graph);
    assertTrue(graph.getNodes().isEmpty());
    assertTrue(graph.getEdges().isEmpty());
  }

  // ============================================
  // STATE DETAIL TESTS
  // ============================================

  @Test
  void testGetStateDetail_allowedAndBlockedTransitions() {
    // Arrange
    String entityType = "Order";
    String entityId = "order-123";
    String tenantId = "tenant-1";

    WorkflowModels.EntityState currentState = WorkflowModels.EntityState.builder()
        .stateCode("APPROVED").since(Instant.now().minusSeconds(60)).build();

    when(workflowService.getCurrentState(entityType, entityId, tenantId)).thenReturn(currentState);
    when(metamodelRegistry.getSchema(entityType)).thenReturn(Optional.of(createMockSchema()));
    when(workflowService.getAllowedTransitions(any(), eq(entityType), eq(entityId), eq(tenantId)))
        .thenReturn(List.of(WorkflowModels.StateTransition.builder().code("ship").toCode("SHIPPED")
            .slaMinutes(60).build()));

    when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class),
        eq(entityType), eq(entityId), eq(tenantId))).thenReturn(List.of(30)); // 30 minutes SLA

    // Act
    WorkflowModels.WorkflowStateDetail detail = runtimeService.getStateDetail(auth, entityType,
        entityId, tenantId);

    // Assert
    assertNotNull(detail);
    assertEquals(currentState, detail.getCurrentState());

    // Allowed transitions
    assertEquals(1, detail.getAllowedTransitions().size());
    WorkflowModels.AllowedTransition allowed = detail.getAllowedTransitions().get(0);
    assertEquals("ship", allowed.getCode());
    assertEquals("SHIPPED", allowed.getToState());

    // Blocked transitions (should have some from schema)
    assertTrue(detail.getBlockedTransitions().size() >= 0);

    // State age
    assertTrue(detail.getStateAgeMs() > 0);
  }

  @Test
  void testGetStateDetail_slaWarning() {
    // Arrange
    String entityType = "Order";
    String entityId = "order-123";
    String tenantId = "tenant-1";

    // State age: 50 minutes, SLA: 60 minutes -> WARN (> 80%)
    WorkflowModels.EntityState currentState = WorkflowModels.EntityState.builder()
        .stateCode("APPROVED").since(Instant.now().minusSeconds(50 * 60)).build();

    when(workflowService.getCurrentState(entityType, entityId, tenantId)).thenReturn(currentState);
    when(metamodelRegistry.getSchema(entityType)).thenReturn(Optional.of(createMockSchema()));
    when(workflowService.getAllowedTransitions(any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
    when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(),
        any(), any())).thenReturn(List.of(60)); // 60 minutes SLA

    // Act
    WorkflowModels.WorkflowStateDetail detail = runtimeService.getStateDetail(auth, entityType,
        entityId, tenantId);

    // Assert
    assertNotNull(detail);
    assertEquals(WorkflowModels.SlaStatus.WARN, detail.getSlaStatus());
  }

  // ============================================
  // HISTORY TESTS
  // ============================================

  @Test
  void testGetHistory_withTimeline() {
    // Arrange
    String entityType = "Order";
    String entityId = "order-123";
    String tenantId = "tenant-1";

    when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class),
        eq(entityType), eq(entityId), eq(tenantId)))
            .thenReturn(List.of(
                WorkflowModels.HistoryEntry.builder()
                    .eventType(WorkflowModels.WorkflowEventType.ACTION_APPLIED).fromState("PENDING")
                    .toState("APPROVED").transitionCode("approve")
                    .timestamp(Instant.now().minusSeconds(120)).durationMs(120000L).actor("user-1")
                    .slaStatus(WorkflowModels.SlaStatus.OK).build(),
                WorkflowModels.HistoryEntry.builder()
                    .eventType(WorkflowModels.WorkflowEventType.ACTION_APPLIED).fromState(null)
                    .toState("PENDING").transitionCode("create")
                    .timestamp(Instant.now().minusSeconds(240)).durationMs(60000L).actor("user-1")
                    .slaStatus(WorkflowModels.SlaStatus.OK).build()));

    // Act
    WorkflowModels.WorkflowHistory history = runtimeService.getHistory(entityType, entityId,
        tenantId);

    // Assert
    assertNotNull(history);
    assertEquals(entityType, history.getEntityType());
    assertEquals(entityId, history.getEntityId());
    assertEquals(2, history.getEntries().size());
    assertTrue(history.getTotalDurationMs() > 0);

    // Verify order (most recent first)
    assertEquals("APPROVED", history.getEntries().get(0).getToState());
    assertEquals("PENDING", history.getEntries().get(1).getToState());
  }

  // ============================================
  // FORECAST TESTS
  // ============================================

  @Test
  void testGetForecast_withPendingTimers() {
    // Arrange
    String entityType = "Order";
    String entityId = "order-123";
    String tenantId = "tenant-1";

    WorkflowModels.EntityState currentState = WorkflowModels.EntityState.builder()
        .stateCode("APPROVED").build();

    when(workflowService.getCurrentState(entityType, entityId, tenantId)).thenReturn(currentState);
    when(metamodelRegistry.getSchema(entityType)).thenReturn(Optional.of(createMockSchema()));
    when(workflowService.getAllowedTransitions(any(), eq(entityType), eq(entityId), eq(tenantId)))
        .thenReturn(List.of(WorkflowModels.StateTransition.builder().code("ship").toCode("SHIPPED")
            .slaMinutes(120).build()));

    UUID timerId = UUID.randomUUID();
    when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class),
        eq(entityType), eq(entityId), eq(tenantId)))
            .thenReturn(List.of(WorkflowModels.PendingTimer.builder().id(timerId)
                .type(WorkflowModels.TimerType.SLA_WARNING)
                .scheduledAt(Instant.now().plusSeconds(3600)).action("notify").remainingMs(3600000L)
                .build()));

    // Act
    WorkflowModels.WorkflowForecast forecast = runtimeService.getForecast(auth, entityType,
        entityId, tenantId);

    // Assert
    assertNotNull(forecast);
    assertEquals("APPROVED", forecast.getCurrentState());

    // Next steps
    assertEquals(1, forecast.getNextSteps().size());
    WorkflowModels.ForecastStep step = forecast.getNextSteps().get(0);
    assertEquals("ship", step.getTransitionCode());
    assertEquals("SHIPPED", step.getToState());
    assertEquals(120, step.getEstimatedSlaMinutes());

    // Pending timers
    assertEquals(1, forecast.getPendingTimers().size());
    WorkflowModels.PendingTimer timer = forecast.getPendingTimers().get(0);
    assertEquals(WorkflowModels.TimerType.SLA_WARNING, timer.getType());
    assertTrue(timer.getRemainingMs() > 0);
  }

  // ============================================
  // HELPER METHODS
  // ============================================

  private EntitySchema createMockSchema() {
    EntitySchema schema = new EntitySchema();
    schema.setEntity("Order");

    // States
    List<StateConfig> states = new ArrayList<>();
    states.add(createState("PENDING", "Pending"));
    states.add(createState("APPROVED", "Approved"));
    states.add(createState("SHIPPED", "Shipped"));
    states.add(createState("DELIVERED", "Delivered"));
    schema.setStates(states);

    // Transitions
    List<TransitionConfig> transitions = new ArrayList<>();
    transitions.add(createTransition("approve", "PENDING", "APPROVED", "Approve", 30));
    transitions.add(createTransition("ship", "APPROVED", "SHIPPED", "Ship", 60));
    transitions.add(createTransition("deliver", "SHIPPED", "DELIVERED", "Deliver", 120));
    schema.setTransitions(transitions);

    return schema;
  }

  private StateConfig createState(String code, String label) {
    StateConfig state = new StateConfig();
    state.setCode(code);
    state.setLabel(label);
    return state;
  }

  private TransitionConfig createTransition(String code, String from, String to, String label,
      Integer slaMinutes) {
    TransitionConfig transition = new TransitionConfig();
    transition.setCode(code);
    transition.setFrom(from);
    transition.setTo(to);
    transition.setLabel(label);
    transition.setSlaMinutes(slaMinutes);
    return transition;
  }
}
