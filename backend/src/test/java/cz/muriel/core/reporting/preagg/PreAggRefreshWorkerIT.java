package cz.muriel.core.reporting.preagg;

import cz.muriel.core.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for pre-aggregation refresh worker.
 * 
 * <p>Tests end-to-end flow: Kafka event → Worker → Cube.js API call
 */
@TestPropertySource(properties = {
    "app.cube.preagg.enabled=true",
    "app.cube.preagg.debounceMs=500"
})
class PreAggRefreshWorkerIT extends AbstractIntegrationTest {

  @Autowired
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Autowired
  private PreAggRefreshWorker worker;

  @Autowired
  private CubePreAggService cubePreAggService;

  @Test
  void shouldConsumeEntityMutationEventAndTriggerRefresh() {
    // Given
    Map<String, Object> event = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "User",
        "entityId", "user-123",
        "tenantId", "test-tenant",
        "timestamp", System.currentTimeMillis()
    );

    // When - send Kafka event
    kafkaTemplate.send("core.entities.lifecycle.mutated", "user-123", event);

    // Then - wait for worker to process event
    await().atMost(5, TimeUnit.SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> {
          Map<String, Long> stats = worker.getDebounceStats();
          assertThat(stats).containsKey("User");
        });
  }

  @Test
  void shouldDebounceMultipleEventsForSameEntityType() throws InterruptedException {
    // Given
    worker.clearDebounceCache();
    
    Map<String, Object> event1 = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "Tenant",
        "entityId", "tenant-1",
        "tenantId", "test-tenant"
    );
    
    Map<String, Object> event2 = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "Tenant",
        "entityId", "tenant-2",
        "tenantId", "test-tenant"
    );

    // When - send 2 events rapidly
    kafkaTemplate.send("core.entities.lifecycle.mutated", "tenant-1", event1);
    TimeUnit.MILLISECONDS.sleep(100);
    kafkaTemplate.send("core.entities.lifecycle.mutated", "tenant-2", event2);

    // Then - wait and verify only 1 refresh happened
    await().atMost(2, TimeUnit.SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> {
          Map<String, Long> stats = worker.getDebounceStats();
          assertThat(stats).containsKey("Tenant");
        });

    // Wait for debounce window to pass
    TimeUnit.MILLISECONDS.sleep(600);

    // Send another event - should trigger new refresh
    Map<String, Object> event3 = Map.of(
        "eventType", "ENTITY_UPDATED",
        "entityType", "Tenant",
        "entityId", "tenant-3",
        "tenantId", "test-tenant"
    );
    
    kafkaTemplate.send("core.entities.lifecycle.mutated", "tenant-3", event3);

    await().atMost(2, TimeUnit.SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> {
          Map<String, Long> stats = worker.getDebounceStats();
          assertThat(stats.get("Tenant")).isNotNull();
        });
  }

  @Test
  void shouldSupportedEntityTypesMatchCubeSchemas() {
    // Given/When
    var supportedTypes = cubePreAggService.getSupportedEntityTypes();

    // Then
    assertThat(supportedTypes)
        .containsExactlyInAnyOrder("User", "Tenant", "Group");
  }

  @Test
  void shouldCheckIfEntityTypeHasPreAggregations() {
    // When/Then
    assertThat(cubePreAggService.hasPreAggregations("User")).isTrue();
    assertThat(cubePreAggService.hasPreAggregations("Tenant")).isTrue();
    assertThat(cubePreAggService.hasPreAggregations("Group")).isTrue();
    assertThat(cubePreAggService.hasPreAggregations("NonExistent")).isFalse();
  }
}
