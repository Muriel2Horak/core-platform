package cz.muriel.core.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.config.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 👥 WebSocket Handler for Presence & Editing Indicators
 */
@Component @RequiredArgsConstructor @Slf4j
public class PresenceWebSocketHandler extends TextWebSocketHandler {

  private final RedisTemplate<String, Object> redisTemplate;
  private final WebSocketProperties webSocketProperties;
  private final ObjectMapper objectMapper;

  // Rate limiting: userId -> last event timestamps
  private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

  @Override
  public void afterConnectionEstablished(@NonNull WebSocketSession session) {
    String userId = (String) session.getAttributes().get("userId");
    String tenantId = (String) session.getAttributes().get("tenantId");

    log.info("WebSocket connection established: userId={}, tenant={}", userId, tenantId);

    session.getAttributes().put("rateLimiter",
        new RateLimiter(webSocketProperties.getRateLimitEventsPerWindow(),
            webSocketProperties.getRateLimitWindowSeconds()));
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    Map<String, Object> attrs = session.getAttributes();
    String userId = (String) attrs.get("userId");
    String username = (String) attrs.get("username");
    String tenantId = (String) attrs.get("tenantId");

    // Rate limiting
    RateLimiter limiter = (RateLimiter) session.getAttributes().get("rateLimiter");
    if (!limiter.allowEvent()) {
      log.warn("Rate limit exceeded for user: {}", userId);
      try {
        sendError(session,
            "Rate limit exceeded: max " + webSocketProperties.getRateLimitEventsPerWindow()
                + " events per " + webSocketProperties.getRateLimitWindowSeconds() + "s");
      } catch (Exception ex) {
        log.error("Failed to send error message", ex);
      }
      return;
    }

    try {
      TextMessage msg = (TextMessage) message;
      @SuppressWarnings("unchecked")
      Map<String, Object> payload = objectMapper.readValue(msg.getPayload(), Map.class);
      String action = (String) payload.get("action");
      String entityType = (String) payload.get("entityType");
      String entityId = (String) payload.get("entityId");

      String channel = "presence:" + tenantId + ":" + entityType + ":" + entityId;

      switch (action) {
      case "join" -> handleJoin(session, channel, userId, username, tenantId, entityType, entityId);
      case "leave" -> handleLeave(session, channel, userId, tenantId, entityType, entityId);
      case "heartbeat" -> handleHeartbeat(session, channel, userId, username);
      case "editStart" -> handleEditStart(session, tenantId, entityType, entityId, userId, username,
          (String) payload.get("fieldId"));
      case "editStop" -> handleEditStop(session, tenantId, entityType, entityId, userId,
          (String) payload.get("fieldId"));
      default -> sendError(session, "Unknown action: " + action);
      }
    } catch (Exception e) {
      log.error("Error handling message", e);
      try {
        sendError(session, "Failed to process message");
      } catch (Exception ex) {
        log.error("Failed to send error message", ex);
      }
    }
  }

  private void handleJoin(WebSocketSession session, String channel, String userId, String username,
      String tenantId, String entityType, String entityId) {
    String presenceKey = channel + ":" + userId;

    // Set presence with TTL
    redisTemplate.opsForValue().set(presenceKey,
        Map.of("userId", userId, "username", username, "ts", Instant.now().toString()),
        webSocketProperties.getPresenceTtlSeconds(), TimeUnit.SECONDS);

    // Add to members set
    redisTemplate.opsForSet().add("presence_members:" + channel, userId);

    log.debug("User joined: channel={}, userId={}", channel, userId);

    // Broadcast to channel subscribers
    broadcastPresenceEvent(channel, "join", userId, username);
  }

  private void handleLeave(WebSocketSession session, String channel, String userId, String tenantId,
      String entityType, String entityId) {
    String presenceKey = channel + ":" + userId;

    redisTemplate.delete(presenceKey);
    redisTemplate.opsForSet().remove("presence_members:" + channel, userId);

    log.debug("User left: channel={}, userId={}", channel, userId);

    broadcastPresenceEvent(channel, "leave", userId, null);
  }

  private void handleHeartbeat(WebSocketSession session, String channel, String userId,
      String username) {
    String presenceKey = channel + ":" + userId;

    // Refresh TTL
    redisTemplate.expire(presenceKey, webSocketProperties.getPresenceTtlSeconds(),
        TimeUnit.SECONDS);

    log.trace("Heartbeat: channel={}, userId={}", channel, userId);
  }

  private void handleEditStart(WebSocketSession session, String tenantId, String entityType,
      String entityId, String userId, String username, String fieldId) {
    String editKey = "editing:" + tenantId + ":" + entityType + ":" + entityId
        + (fieldId != null ? ":" + fieldId : "") + ":" + userId;

    redisTemplate.opsForValue()
        .set(editKey,
            Map.of("userId", userId, "username", username, "fieldId",
                fieldId != null ? fieldId : "", "ts", Instant.now().toString()),
            30, TimeUnit.SECONDS);

    log.debug("Edit started: entity={}/{}, field={}, user={}", entityType, entityId, fieldId,
        userId);
  }

  private void handleEditStop(WebSocketSession session, String tenantId, String entityType,
      String entityId, String userId, String fieldId) {
    String editKey = "editing:" + tenantId + ":" + entityType + ":" + entityId
        + (fieldId != null ? ":" + fieldId : "") + ":" + userId;

    redisTemplate.delete(editKey);

    log.debug("Edit stopped: entity={}/{}, field={}, user={}", entityType, entityId, fieldId,
        userId);
  }

  private void broadcastPresenceEvent(String channel, String event, String userId,
      String username) {
    // In production, use Redis Pub/Sub to broadcast to all connected instances
    redisTemplate.convertAndSend(channel, Map.of("event", event, "userId", userId, "username",
        username != null ? username : "", "ts", Instant.now().toString()));
  }

  private void sendError(WebSocketSession session, String message) throws Exception {
    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("error", message))));
  }

  @Override
  public void afterConnectionClosed(@NonNull WebSocketSession session,
      @NonNull CloseStatus status) {
    String userId = (String) session.getAttributes().get("userId");
    log.info("WebSocket connection closed: userId={}, status={}", userId, status);

    rateLimiters.remove(userId);
  }

  /**
   * Simple token bucket rate limiter
   */
  private static class RateLimiter {
    private final int maxEvents;
    private final long windowMs;
    private long lastReset;
    private int eventCount;

    public RateLimiter(int maxEvents, int windowSeconds) {
      this.maxEvents = maxEvents;
      this.windowMs = windowSeconds * 1000L;
      this.lastReset = System.currentTimeMillis();
      this.eventCount = 0;
    }

    public synchronized boolean allowEvent() {
      long now = System.currentTimeMillis();
      if (now - lastReset >= windowMs) {
        lastReset = now;
        eventCount = 0;
      }

      if (eventCount < maxEvents) {
        eventCount++;
        return true;
      }

      return false;
    }
  }
}
