package cz.muriel.core.workflow;

import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.StateConfig;
import cz.muriel.core.metamodel.schema.TransitionConfig;
import cz.muriel.core.test.AbstractIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 🧪 W5: Workflow API Integration Tests
 * 
 * Tests workflow runtime API endpoints against real PostgreSQL
 * (Testcontainers): - GET /api/workflows/{entity}/{id}/graph - GET
 * /api/workflows/{entity}/{id}/state - GET /api/workflows/{entity}/{id}/history
 * - GET /api/workflows/{entity}/{id}/forecast
 * 
 * @since 2025-10-14
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) @AutoConfigureMockMvc
class WorkflowApiIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private final String tenantId = "test-tenant";
  private final String entityType = "TestOrder";
  private final String entityId = UUID.randomUUID().toString();

  @BeforeEach
  void setUp() {
    // Clean up test data
    jdbcTemplate.update("DELETE FROM workflow_timers WHERE tenant_id = ?", tenantId);
    jdbcTemplate.update("DELETE FROM workflow_events WHERE tenant_id = ?", tenantId);
    jdbcTemplate.update("DELETE FROM entity_state_log WHERE tenant_id = ?", tenantId);
    jdbcTemplate.update("DELETE FROM entity_state WHERE tenant_id = ?", tenantId);
    jdbcTemplate.update("DELETE FROM state_transition WHERE entity_type = ?", entityType);

    // Register mock entity schema
    registerTestSchema();

    // Insert test transitions
    insertTestTransitions();
  }

  // ============================================
  // GRAPH API TESTS
  // ============================================

  @Test @WithMockUser(username = "test-user", authorities = { "CORE_ROLE_USER" })
  void testGetWorkflowGraph_withCurrentState() throws Exception {
    // Arrange: Set entity to PENDING state
    insertEntityState("PENDING");

    // Act & Assert
    mockMvc
        .perform(get("/api/workflows/{entity}/{id}/graph", entityType, entityId)
            .header("X-Tenant-Id", tenantId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.entityType").value(entityType))
        .andExpect(jsonPath("$.entityId").value(entityId))
        .andExpect(jsonPath("$.currentState").value("PENDING"))
        .andExpect(jsonPath("$.nodes").isArray())
        .andExpect(jsonPath("$.nodes.length()").value(greaterThanOrEqualTo(3)))
        .andExpect(jsonPath("$.nodes[?(@.code=='PENDING')].current").value(hasItem(true)))
        .andExpect(jsonPath("$.edges").isArray())
        .andExpect(jsonPath("$.edges.length()").value(greaterThan(0)));
  }

  @Test @WithMockUser(username = "test-user", authorities = { "CORE_ROLE_USER" })
  void testGetWorkflowGraph_noCurrentState() throws Exception {
    // Act & Assert: Entity has no state yet
    mockMvc
        .perform(get("/api/workflows/{entity}/{id}/graph", entityType, entityId)
            .header("X-Tenant-Id", tenantId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.currentState").isEmpty())
        .andExpect(jsonPath("$.nodes").isArray());
  }

  // ============================================
  // STATE DETAIL API TESTS
  // ============================================

  @Test @WithMockUser(username = "test-user", authorities = { "CORE_ROLE_USER" })
  void testGetStateDetail_withAllowedAndBlockedTransitions() throws Exception {
    // Arrange
    insertEntityState("PENDING");

    // Act & Assert
    mockMvc
        .perform(get("/api/workflows/{entity}/{id}/state", entityType, entityId)
            .header("X-Tenant-Id", tenantId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.currentState.stateCode").value("PENDING"))
        .andExpect(jsonPath("$.allowedTransitions").isArray())
        .andExpect(jsonPath("$.blockedTransitions").isArray())
        .andExpect(jsonPath("$.slaStatus").exists())
        .andExpect(jsonPath("$.stateAgeMs").value(greaterThanOrEqualTo(0)));
  }

  @Test @WithMockUser(username = "test-user", authorities = { "CORE_ROLE_USER" })
  void testGetStateDetail_slaWarning() throws Exception {
    // Arrange: Insert state that is close to SLA breach
    Instant oldTime = Instant.now().minusSeconds(50 * 60); // 50 minutes ago
    jdbcTemplate.update(
        "INSERT INTO entity_state (entity_type, entity_id, tenant_id, state_code, since, sla_minutes) VALUES (?, ?, ?, ?, ?, ?)",
        entityType, entityId, tenantId, "PENDING", oldTime, 60 // SLA: 60 minutes
    );

    // Act & Assert
    mockMvc
        .perform(get("/api/workflows/{entity}/{id}/state", entityType, entityId)
            .header("X-Tenant-Id", tenantId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.slaStatus").value("WARN")); // 80%
                                                                                      // threshold
                                                                                      // exceeded
  }

  // ============================================
  // HISTORY API TESTS
  // ============================================

  @Test @WithMockUser(username = "test-user", authorities = { "CORE_ROLE_USER" })
  void testGetHistory_withTransitions() throws Exception {
    // Arrange: Create history
    insertEntityState("APPROVED");
    insertStateLog(null, "PENDING", "create", Instant.now().minusSeconds(300));
    insertStateLog("PENDING", "APPROVED", "approve", Instant.now().minusSeconds(120));

    // Act & Assert
    mockMvc
        .perform(get("/api/workflows/{entity}/{id}/history", entityType, entityId)
            .header("X-Tenant-Id", tenantId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.entityType").value(entityType))
        .andExpect(jsonPath("$.entries").isArray())
        .andExpect(jsonPath("$.entries.length()").value(2))
        .andExpect(jsonPath("$.entries[0].toState").value("APPROVED"))
        .andExpect(jsonPath("$.entries[1].toState").value("PENDING"))
        .andExpect(jsonPath("$.totalDurationMs").value(greaterThan(0)));
  }

  @Test @WithMockUser(username = "test-user", authorities = { "CORE_ROLE_USER" })
  void testGetHistory_empty() throws Exception {
    // Act & Assert: No history yet
    mockMvc
        .perform(get("/api/workflows/{entity}/{id}/history", entityType, entityId)
            .header("X-Tenant-Id", tenantId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.entries").isArray())
        .andExpect(jsonPath("$.entries.length()").value(0));
  }

  // ============================================
  // FORECAST API TESTS
  // ============================================

  @Test @WithMockUser(username = "test-user", authorities = { "CORE_ROLE_USER" })
  void testGetForecast_withPendingTimers() throws Exception {
    // Arrange
    insertEntityState("PENDING");
    insertPendingTimer("SLA_WARNING", Instant.now().plusSeconds(3600), "notify");

    // Act & Assert
    mockMvc
        .perform(get("/api/workflows/{entity}/{id}/forecast", entityType, entityId)
            .header("X-Tenant-Id", tenantId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.currentState").value("PENDING"))
        .andExpect(jsonPath("$.nextSteps").isArray())
        .andExpect(jsonPath("$.pendingTimers").isArray())
        .andExpect(jsonPath("$.pendingTimers.length()").value(1))
        .andExpect(jsonPath("$.pendingTimers[0].type").value("SLA_WARNING"))
        .andExpect(jsonPath("$.pendingTimers[0].remainingMs").value(greaterThan(0)));
  }

  // ============================================
  // HELPER METHODS
  // ============================================

  private void registerTestSchema() {
    EntitySchema schema = new EntitySchema();
    schema.setEntity(entityType);

    List<StateConfig> states = new ArrayList<>();
    states.add(createState("PENDING", "Pending"));
    states.add(createState("APPROVED", "Approved"));
    states.add(createState("SHIPPED", "Shipped"));
    schema.setStates(states);

    List<TransitionConfig> transitions = new ArrayList<>();
    transitions.add(createTransition("approve", "PENDING", "APPROVED", "Approve", 30));
    transitions.add(createTransition("ship", "APPROVED", "SHIPPED", "Ship", 60));
    schema.setTransitions(transitions);

    // Note: MetamodelRegistry might be immutable at runtime,
    // so we rely on state_transition table inserts instead
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

  private void insertTestTransitions() {
    jdbcTemplate.update(
        "INSERT INTO state_transition (entity_type, from_code, to_code, code, sla_minutes) VALUES (?, ?, ?, ?, ?)",
        entityType, "PENDING", "APPROVED", "approve", 30);
    jdbcTemplate.update(
        "INSERT INTO state_transition (entity_type, from_code, to_code, code, sla_minutes) VALUES (?, ?, ?, ?, ?)",
        entityType, "APPROVED", "SHIPPED", "ship", 60);
  }

  private void insertEntityState(String stateCode) {
    jdbcTemplate.update(
        "INSERT INTO entity_state (entity_type, entity_id, tenant_id, state_code, since) VALUES (?, ?, ?, ?, ?)",
        entityType, entityId, tenantId, stateCode, Instant.now());
  }

  private void insertStateLog(String fromCode, String toCode, String transitionCode,
      Instant changedAt) {
    jdbcTemplate.update(
        "INSERT INTO entity_state_log (entity_type, entity_id, tenant_id, from_code, to_code, transition_code, changed_by, changed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        entityType, entityId, tenantId, fromCode, toCode, transitionCode, "test-user", changedAt);
  }

  private void insertPendingTimer(String timerType, Instant scheduledAt, String action) {
    jdbcTemplate.update(
        "INSERT INTO workflow_timers (id, tenant_id, entity_type, entity_id, timer_type, state_code, scheduled_at, status, action) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(), tenantId, entityType, entityId, timerType, "PENDING", scheduledAt,
        "PENDING", action);
  }
}
