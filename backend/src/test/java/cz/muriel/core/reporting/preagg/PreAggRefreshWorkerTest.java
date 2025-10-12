package cz.muriel.core.reporting.preagg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PreAggRefreshWorker.
 */
@ExtendWith(MockitoExtension.class)
class PreAggRefreshWorkerTest {

  @Mock
  private CubePreAggService cubePreAggService;

  @Mock
  private Acknowledgment ack;

  private PreAggRefreshWorker worker;

  @BeforeEach
  void setUp() {
    // Worker with 1s debounce window for testing
    worker = new PreAggRefreshWorker(cubePreAggService, true, 1000);
  }

  @Test
  void shouldTriggerPreAggRefreshForUserEntity() {
    // Given
    when(cubePreAggService.refreshForEntityType("User", "tenant-1")).thenReturn(true);
    
    Map<String, Object> event = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "User",
        "entityId", "user-123",
        "tenantId", "tenant-1",
        "timestamp", System.currentTimeMillis()
    );

    // When
    worker.handleEntityMutation(event, ack);

    // Then
    verify(cubePreAggService).refreshForEntityType("User", "tenant-1");
    verify(ack).acknowledge();
  }

  @Test
  void shouldDebounceMultipleEventsForSameEntityType() throws InterruptedException {
    // Given
    when(cubePreAggService.refreshForEntityType(anyString(), anyString())).thenReturn(true);
    
    Map<String, Object> event1 = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "User",
        "entityId", "user-1",
        "tenantId", "tenant-1"
    );
    
    Map<String, Object> event2 = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "User",
        "entityId", "user-2",
        "tenantId", "tenant-1"
    );

    // When
    worker.handleEntityMutation(event1, ack);
    worker.handleEntityMutation(event2, ack); // Should be debounced

    // Then
    verify(cubePreAggService, times(1)).refreshForEntityType("User", "tenant-1");
    verify(ack, times(2)).acknowledge();
  }

  @Test
  void shouldNotDebounceAfterWindowExpires() throws InterruptedException {
    // Given
    when(cubePreAggService.refreshForEntityType(anyString(), anyString())).thenReturn(true);
    
    Map<String, Object> event = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "User",
        "entityId", "user-1",
        "tenantId", "tenant-1"
    );

    // When
    worker.handleEntityMutation(event, ack);
    TimeUnit.MILLISECONDS.sleep(1100); // Wait for debounce window to expire
    worker.handleEntityMutation(event, ack);

    // Then
    verify(cubePreAggService, times(2)).refreshForEntityType("User", "tenant-1");
    verify(ack, times(2)).acknowledge();
  }

  @Test
  void shouldHandleDifferentEntityTypesIndependently() {
    // Given
    when(cubePreAggService.refreshForEntityType(anyString(), anyString())).thenReturn(true);
    
    Map<String, Object> userEvent = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "User",
        "entityId", "user-1",
        "tenantId", "tenant-1"
    );
    
    Map<String, Object> tenantEvent = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "Tenant",
        "entityId", "tenant-2",
        "tenantId", "tenant-1"
    );

    // When
    worker.handleEntityMutation(userEvent, ack);
    worker.handleEntityMutation(tenantEvent, ack);

    // Then
    verify(cubePreAggService).refreshForEntityType("User", "tenant-1");
    verify(cubePreAggService).refreshForEntityType("Tenant", "tenant-1");
    verify(ack, times(2)).acknowledge();
  }

  @Test
  void shouldSkipEventsWhenDisabled() {
    // Given
    PreAggRefreshWorker disabledWorker = new PreAggRefreshWorker(cubePreAggService, false, 1000);
    
    Map<String, Object> event = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "User",
        "entityId", "user-1",
        "tenantId", "tenant-1"
    );

    // When
    disabledWorker.handleEntityMutation(event, ack);

    // Then
    verify(cubePreAggService, never()).refreshForEntityType(anyString(), anyString());
    verify(ack).acknowledge();
  }

  @Test
  void shouldHandleDltMessages() {
    // Given
    Map<String, Object> dltEvent = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "User",
        "entityId", "user-1",
        "tenantId", "tenant-1",
        "errorReason", "Failed after 3 retries"
    );

    // When
    worker.handleDlt(dltEvent, ack);

    // Then
    verify(ack).acknowledge();
    // Should log error (checked manually or with log capture)
  }

  @Test
  void shouldClearDebounceCacheOnDemand() {
    // Given
    when(cubePreAggService.refreshForEntityType(anyString(), anyString())).thenReturn(true);
    
    Map<String, Object> event = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "User",
        "entityId", "user-1",
        "tenantId", "tenant-1"
    );

    // When
    worker.handleEntityMutation(event, ack); // First event
    worker.clearDebounceCache();
    worker.handleEntityMutation(event, ack); // Should NOT be debounced now

    // Then
    verify(cubePreAggService, times(2)).refreshForEntityType("User", "tenant-1");
  }

  @Test
  void shouldReturnDebounceStats() {
    // Given
    when(cubePreAggService.refreshForEntityType(anyString(), anyString())).thenReturn(true);
    
    Map<String, Object> userEvent = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "User",
        "entityId", "user-1",
        "tenantId", "tenant-1"
    );
    
    Map<String, Object> tenantEvent = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "Tenant",
        "entityId", "tenant-2",
        "tenantId", "tenant-1"
    );

    // When
    worker.handleEntityMutation(userEvent, ack);
    worker.handleEntityMutation(tenantEvent, ack);
    
    Map<String, Long> stats = worker.getDebounceStats();

    // Then
    assertThat(stats).containsKeys("User", "Tenant");
    assertThat(stats.get("User")).isNotNull();
    assertThat(stats.get("Tenant")).isNotNull();
  }
}
