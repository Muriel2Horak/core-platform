package cz.muriel.core.monitoring.websocket;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.muriel.core.monitoring.bff.service.MonitoringMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringLiveWebSocketHandler extends TextWebSocketHandler {

  private static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(15);
  private final MonitoringMetricsService monitoringMetricsService;
  private final ObjectMapper objectMapper;
  private final JwtDecoder jwtDecoder;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
  private final Map<String, ScheduledFuture<?>> schedules = new ConcurrentHashMap<>();
  private final Map<String, String> tenants = new ConcurrentHashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String tenant = resolveTenant(session);
    if (tenant == null || tenant.isBlank()) {
      log.warn("Monitoring WS missing tenant, closing session {}", session.getId());
      session.close(CloseStatus.NOT_ACCEPTABLE);
      return;
    }

    tenants.put(session.getId(), tenant);
    ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() ->
        sendSummary(session), 0, DEFAULT_INTERVAL.getSeconds(), TimeUnit.SECONDS);
    schedules.put(session.getId(), future);

    log.info("Monitoring WS connected: sessionId={}, tenant={}", session.getId(), tenant);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonNode node = objectMapper.readTree(message.getPayload());
    String type = Optional.ofNullable(node.get("type")).map(JsonNode::asText).orElse("REFRESH");

    if ("REFRESH".equalsIgnoreCase(type)) {
      sendSummary(session);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    ScheduledFuture<?> future = schedules.remove(session.getId());
    if (future != null) {
      future.cancel(true);
    }
    tenants.remove(session.getId());
    log.info("Monitoring WS closed: sessionId={}, status={}", session.getId(), status);
  }

  private void sendSummary(WebSocketSession session) {
    if (!session.isOpen()) {
      return;
    }
    String tenant = tenants.get(session.getId());
    if (tenant == null) {
      return;
    }

    try {
      Map<String, Object> summary = monitoringMetricsService.getMetricsSummary(tenant, 1);
      summary.put("type", "metrics-summary");
      session.sendMessage(new TextMessage(objectMapper.writeValueAsString(summary)));
    } catch (Exception ex) {
      log.warn("Monitoring WS send failed: sessionId={}, error={}", session.getId(), ex.getMessage());
    }
  }

  private String resolveTenant(WebSocketSession session) {
    String token = extractToken(session);
    if (token != null) {
      try {
        Jwt jwt = jwtDecoder.decode(token);
        String tenant = jwt.getClaimAsString("tenant");
        if (tenant != null && !tenant.isBlank()) {
          return tenant;
        }
      } catch (Exception ex) {
        log.debug("Monitoring WS token decode failed: {}", ex.getMessage());
      }
    }

    return extractQueryParam(session.getUri(), "tenant");
  }

  private String extractToken(WebSocketSession session) {
    String header = session.getHandshakeHeaders().getFirst("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring("Bearer ".length());
    }

    return extractQueryParam(session.getUri(), "token");
  }

  private String extractQueryParam(URI uri, String name) {
    if (uri == null || uri.getQuery() == null) {
      return null;
    }

    String[] parts = uri.getQuery().split("&");
    for (String part : parts) {
      String[] kv = part.split("=", 2);
      if (kv.length == 2 && kv[0].equals(name)) {
        return kv[1];
      }
    }

    return null;
  }
}
