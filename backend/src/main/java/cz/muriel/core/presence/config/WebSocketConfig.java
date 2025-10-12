package cz.muriel.core.presence.config;

import cz.muriel.core.presence.handler.PresenceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for real-time presence tracking
 * 
 * Endpoint: ws://{host}/ws/presence
 * 
 * Protocol: - Client → Server: {"type":"SUB", "entity":"Order", "id":"123",
 * "tenantId":"t1"} - Server → Client: {"type":"PRESENCE",
 * "users":["user1","user2"]} - Client → Server: {"type":"HB"} every 30s -
 * Client → Server: {"type":"LOCK", "field":"totalAmount"} - Server → Client:
 * {"type":"LOCK_ACK", "field":"totalAmount", "success":true}
 */
@Configuration @EnableWebSocket @RequiredArgsConstructor @Slf4j @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = false) @ConditionalOnMissingBean(name = "webSocketConfig")
public class WebSocketConfig implements WebSocketConfigurer {

  private final PresenceWebSocketHandler presenceWebSocketHandler;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    // For production: Replace "*" with specific frontend origins (e.g.,
    // "https://app.muriel.cz")
    // Use environment variable ALLOWED_ORIGINS to configure dynamically
    String allowedOrigins = System.getenv().getOrDefault("ALLOWED_ORIGINS", "*");

    registry.addHandler(presenceWebSocketHandler, "/ws/presence")
        .setAllowedOrigins(allowedOrigins.split(","));

    log.info("Presence WebSocket registered at /ws/presence (allowed origins: {})", allowedOrigins);
  }
}
