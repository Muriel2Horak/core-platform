package com.platform.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.awaitility.Awaitility;

import cz.muriel.core.BackendApplication;
import cz.muriel.core.test.AbstractKafkaIntegrationTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🧪 W6: Presence Lock Integration Test
 * 
 * Tests the interaction between: 1. Kafka presence lock signals (workflow.locks
 * topic) 2. Frontend read-only mode activation 3. ActionsBar disable/enable
 * based on lock status
 * 
 * Flow: - User A opens workflow → publishes lock signal to Kafka - User B
 * receives lock signal via SSE → ActionsBar becomes read-only - User A closes
 * workflow → publishes unlock signal - User B receives unlock signal →
 * ActionsBar becomes editable again
 * 
 * @since 2025-10-14
 */
@SpringBootTest(classes = BackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PresenceLockIT extends AbstractKafkaIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  private String baseUrl() {
    return "http://localhost:" + port;
  }

  @Test
  void testPresenceLockSignalDisablesActions() throws Exception {
    String entityType = "order";
    String entityId = "test-123";
    String lockingUser = "user-a@example.com";

    // 1. Publish lock signal to Kafka
    String lockEvent = objectMapper.writeValueAsString(
        new LockEvent(entityType, entityId, "LOCK", lockingUser, System.currentTimeMillis()));

    kafkaTemplate.send("workflow.locks", entityId, lockEvent).get(5, TimeUnit.SECONDS);

    // 2. Frontend polls /api/workflows/{entity}/{id}/lock-status
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI
            .create(baseUrl() + "/api/workflows/" + entityType + "/" + entityId + "/lock-status"))
        .GET().build();

    // Wait for lock signal to propagate (Kafka → Backend cache)
    Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> {
          HttpResponse<String> response = client.send(request,
              HttpResponse.BodyHandlers.ofString());
          assertThat(response.statusCode()).isEqualTo(200);

          JsonNode json = objectMapper.readTree(response.body());
          assertThat(json.get("locked").asBoolean()).isTrue();
          assertThat(json.get("lockedBy").asText()).isEqualTo(lockingUser);
        });
  }

  @Test
  void testPresenceUnlockSignalEnablesActions() throws Exception {
    String entityType = "invoice";
    String entityId = "inv-456";
    String lockingUser = "user-b@example.com";

    // 1. Lock first
    String lockEvent = objectMapper.writeValueAsString(
        new LockEvent(entityType, entityId, "LOCK", lockingUser, System.currentTimeMillis()));
    kafkaTemplate.send("workflow.locks", entityId, lockEvent).get(5, TimeUnit.SECONDS);

    // 2. Unlock after 2s
    Thread.sleep(2000);

    String unlockEvent = objectMapper.writeValueAsString(
        new LockEvent(entityType, entityId, "UNLOCK", lockingUser, System.currentTimeMillis()));
    kafkaTemplate.send("workflow.locks", entityId, unlockEvent).get(5, TimeUnit.SECONDS);

    // 3. Verify lock is removed
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI
            .create(baseUrl() + "/api/workflows/" + entityType + "/" + entityId + "/lock-status"))
        .GET().build();

    Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> {
          HttpResponse<String> response = client.send(request,
              HttpResponse.BodyHandlers.ofString());
          assertThat(response.statusCode()).isEqualTo(200);

          JsonNode json = objectMapper.readTree(response.body());
          assertThat(json.get("locked").asBoolean()).isFalse();
        });
  }

  @Test
  void testMultipleUsersPresenceLock() throws Exception {
    String entityType = "contract";
    String entityId = "ctr-789";

    // User A locks
    String lockA = objectMapper.writeValueAsString(new LockEvent(entityType, entityId, "LOCK",
        "user-a@example.com", System.currentTimeMillis()));
    kafkaTemplate.send("workflow.locks", entityId, lockA).get(5, TimeUnit.SECONDS);

    // User B tries to lock (should fail or get queued)
    String lockB = objectMapper.writeValueAsString(new LockEvent(entityType, entityId, "LOCK",
        "user-b@example.com", System.currentTimeMillis() + 1000));
    kafkaTemplate.send("workflow.locks", entityId, lockB).get(5, TimeUnit.SECONDS);

    // Verify User A still holds the lock (first-come-first-served)
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI
            .create(baseUrl() + "/api/workflows/" + entityType + "/" + entityId + "/lock-status"))
        .GET().build();

    Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode()).isEqualTo(200);

      JsonNode json = objectMapper.readTree(response.body());
      assertThat(json.get("locked").asBoolean()).isTrue();
      assertThat(json.get("lockedBy").asText()).isEqualTo("user-a@example.com");
    });
  }

  @Test
  void testLockExpiration() throws Exception {
    String entityType = "task";
    String entityId = "tsk-101";

    // Lock with very old timestamp (should be expired)
    long oldTimestamp = System.currentTimeMillis() - Duration.ofMinutes(10).toMillis();
    String lockEvent = objectMapper.writeValueAsString(
        new LockEvent(entityType, entityId, "LOCK", "stale-user@example.com", oldTimestamp));
    kafkaTemplate.send("workflow.locks", entityId, lockEvent).get(5, TimeUnit.SECONDS);

    // Backend should auto-expire locks older than 5 minutes
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI
            .create(baseUrl() + "/api/workflows/" + entityType + "/" + entityId + "/lock-status"))
        .GET().build();

    Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode()).isEqualTo(200);

      JsonNode json = objectMapper.readTree(response.body());
      // Should be unlocked due to expiration
      assertThat(json.get("locked").asBoolean()).isFalse();
    });
  }

  // Helper record for lock events
  record LockEvent(String entityType, String entityId, String action, // LOCK or UNLOCK
      String userId, long timestamp) {
  }
}
