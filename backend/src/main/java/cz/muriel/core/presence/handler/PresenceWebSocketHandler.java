package cz.muriel.core.presence.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.presence.PresenceService;
import cz.muriel.core.presence.dto.PresenceMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * WebSocket handler for real-time presence tracking
 * 
 * Session Management:
 * - Each WebSocketSession is mapped to a PresenceContext (entity, id, tenantId, userId)
 * - On SUB: store context, add user to Redis, broadcast PRESENCE update
 * - On UNSUB: remove from Redis, cleanup context
 * - On HB: refresh Redis TTL, refresh lock TTL if holding locks
 * - On disconnect: cleanup all locks and presence
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = false)
public class PresenceWebSocketHandler extends TextWebSocketHandler {

  private final PresenceService presenceService;
  private final ObjectMapper objectMapper;

  // Session ID → PresenceContext
  private final ConcurrentMap<String, PresenceContext> sessions = new ConcurrentHashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    log.info("WebSocket connection established: sessionId={}", session.getId());
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    String payload = message.getPayload();
    log.debug("WebSocket message received: sessionId={}, payload={}", session.getId(), payload);

    try {
      JsonNode node = objectMapper.readTree(payload);
      String type = node.get("type").asText();

      switch (type) {
        case "SUB" -> handleSubscribe(session, node);
        case "UNSUB" -> handleUnsubscribe(session, node);
        case "HB" -> handleHeartbeat(session);
        case "LOCK" -> handleLock(session, node);
        case "UNLOCK" -> handleUnlock(session, node);
        default -> {
          log.warn("Unknown message type: {}", type);
          sendError(session, "Unknown message type: " + type);
        }
      }
    } catch (Exception e) {
      log.error("Error handling WebSocket message", e);
      sendError(session, "Invalid message format");
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    log.info("WebSocket connection closed: sessionId={}, status={}", session.getId(), status);
    cleanup(session);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    log.error("WebSocket transport error: sessionId={}", session.getId(), exception);
    cleanup(session);
  }

  // ========== Message Handlers ==========

  private void handleSubscribe(WebSocketSession session, JsonNode node) throws IOException {
    String userId = node.get("userId").asText();
    String tenantId = node.get("tenantId").asText();
    String entity = node.get("entity").asText();
    String id = node.get("id").asText();

    // Store context
    PresenceContext context = new PresenceContext(userId, tenantId, entity, id);
    sessions.put(session.getId(), context);

    // Subscribe in Redis
    presenceService.subscribe(userId, tenantId, entity, id);

    // Send current presence state
    Set<Object> users = presenceService.getPresence(tenantId, entity, id);
    boolean isStale = presenceService.isStale(tenantId, entity, id);
    String busyBy = presenceService.getBusyBy(tenantId, entity, id);
    Long version = presenceService.getVersion(tenantId, entity, id);

    PresenceMessage response = PresenceMessage.builder()
        .type("PRESENCE")
        .users(users)
        .stale(isStale)
        .busyBy(busyBy)
        .version(version)
        .build();

    sendMessage(session, response);
    log.info("User {} subscribed to {}:{} (tenant: {})", userId, entity, id, tenantId);
  }

  private void handleUnsubscribe(WebSocketSession session, JsonNode node) throws IOException {
    PresenceContext context = sessions.get(session.getId());
    if (context == null) {
      log.warn("UNSUB from session without context: {}", session.getId());
      return;
    }

    presenceService.unsubscribe(context.userId, context.tenantId, context.entity, context.id);
    sessions.remove(session.getId());

    sendMessage(session, PresenceMessage.builder().type("UNSUB_ACK").build());
    log.info("User {} unsubscribed from {}:{}", context.userId, context.entity, context.id);
  }

  private void handleHeartbeat(WebSocketSession session) throws IOException {
    PresenceContext context = sessions.get(session.getId());
    if (context == null) {
      log.warn("HB from session without context: {}", session.getId());
      sendError(session, "Not subscribed");
      return;
    }

    presenceService.heartbeat(context.userId, context.tenantId, context.entity, context.id);

    // Refresh all locks held by this user
    context.locks.forEach(field -> 
        presenceService.refreshLock(context.userId, context.tenantId, context.entity, context.id, field)
    );

    sendMessage(session, PresenceMessage.builder().type("HB_ACK").build());
  }

  private void handleLock(WebSocketSession session, JsonNode node) throws IOException {
    PresenceContext context = sessions.get(session.getId());
    if (context == null) {
      sendError(session, "Not subscribed");
      return;
    }

    String field = node.get("field").asText();
    boolean success = presenceService.acquireLock(
        context.userId, context.tenantId, context.entity, context.id, field
    );

    if (success) {
      context.locks.add(field);
    }

    PresenceMessage response = PresenceMessage.builder()
        .type("LOCK_ACK")
        .field(field)
        .success(success)
        .build();

    sendMessage(session, response);
  }

  private void handleUnlock(WebSocketSession session, JsonNode node) throws IOException {
    PresenceContext context = sessions.get(session.getId());
    if (context == null) {
      sendError(session, "Not subscribed");
      return;
    }

    String field = node.get("field").asText();
    presenceService.releaseLock(context.userId, context.tenantId, context.entity, context.id, field);
    context.locks.remove(field);

    PresenceMessage response = PresenceMessage.builder()
        .type("UNLOCK_ACK")
        .field(field)
        .build();

    sendMessage(session, response);
  }

  // ========== Cleanup ==========

  private void cleanup(WebSocketSession session) {
    PresenceContext context = sessions.remove(session.getId());
    if (context == null) {
      return;
    }

    // Release all locks
    context.locks.forEach(field -> 
        presenceService.releaseLock(context.userId, context.tenantId, context.entity, context.id, field)
    );

    // Unsubscribe
    presenceService.unsubscribe(context.userId, context.tenantId, context.entity, context.id);

    log.info("Session cleaned up: userId={}, entity={}:{}", 
        context.userId, context.entity, context.id);
  }

  // ========== Utilities ==========

  private void sendMessage(WebSocketSession session, PresenceMessage message) throws IOException {
    String json = objectMapper.writeValueAsString(message);
    session.sendMessage(new TextMessage(json));
  }

  private void sendError(WebSocketSession session, String error) throws IOException {
    PresenceMessage message = PresenceMessage.builder()
        .type("ERROR")
        .error(error)
        .build();
    sendMessage(session, message);
  }

  // ========== Session Context ==========

  private static class PresenceContext {
    final String userId;
    final String tenantId;
    final String entity;
    final String id;
    final Set<String> locks = ConcurrentHashMap.newKeySet(); // Fields locked by this user

    PresenceContext(String userId, String tenantId, String entity, String id) {
      this.userId = userId;
      this.tenantId = tenantId;
      this.entity = entity;
      this.id = id;
    }
  }
}
