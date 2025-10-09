package cz.muriel.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 🌐 WebSocket Configuration Properties All values configurable via
 * application.properties with app.websocket.* prefix
 */
@Configuration @ConfigurationProperties(prefix = "app.websocket") @Data
public class WebSocketProperties {
  private String allowedOrigins = "https://*.core-platform.local,http://localhost:*"; // Fallback -
                                                                                      // use
                                                                                      // ${APP_WEBSOCKET_ALLOWED_ORIGINS}
                                                                                      // in
                                                                                      // properties
  private int heartbeatIntervalSeconds = 10;
  private int presenceTtlSeconds = 30;
  private int rateLimitEventsPerWindow = 10;
  private int rateLimitWindowSeconds = 5;
}
