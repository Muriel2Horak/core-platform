package cz.muriel.core.monitoring.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "monitoring.websocket.enabled", havingValue = "true", matchIfMissing = true)
public class MonitoringWebSocketConfig implements WebSocketConfigurer {

  private final MonitoringLiveWebSocketHandler monitoringLiveWebSocketHandler;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    String allowedOrigins = System.getenv().getOrDefault("ALLOWED_ORIGINS", "*");
    registry.addHandler(monitoringLiveWebSocketHandler, "/ws/monitoring/live")
        .setAllowedOrigins(allowedOrigins.split(","));

    log.info("Monitoring WebSocket registered at /ws/monitoring/live (allowed origins: {})",
        allowedOrigins);
  }
}
