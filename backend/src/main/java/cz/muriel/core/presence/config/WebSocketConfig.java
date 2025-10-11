package cz.muriel.core.presence.config;

import cz.muriel.core.presence.handler.PresenceWebSocketHandler;
import lombok.RequiredArgsConstructor;
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
 * Protocol:
 * - Client → Server: {"type":"SUB", "entity":"Order", "id":"123", "tenantId":"t1"}
 * - Server → Client: {"type":"PRESENCE", "users":["user1","user2"]}
 * - Client → Server: {"type":"HB"} every 30s
 * - Client → Server: {"type":"LOCK", "field":"totalAmount"}
 * - Server → Client: {"type":"LOCK_ACK", "field":"totalAmount", "success":true}
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = false)
public class WebSocketConfig implements WebSocketConfigurer {

  private final PresenceWebSocketHandler presenceWebSocketHandler;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(presenceWebSocketHandler, "/ws/presence")
        .setAllowedOrigins("*"); // TODO: Configure CORS properly for production
  }
}
