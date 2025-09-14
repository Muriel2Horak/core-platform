package cz.muriel.core.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.time.Instant;

@RestController
@CrossOrigin(origins = { "http://localhost:3000", "http://frontend:3000" })
public class FrontendLogsController {

  private static final Logger logger = LoggerFactory.getLogger("FRONTEND");
  private final RestTemplate restTemplate;
  private static final String LOKI_URL = "http://loki:3100/loki/api/v1/push";

  public FrontendLogsController(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @PostMapping("/api/frontend-logs")
  public ResponseEntity<String> receiveFrontendLog(@RequestBody Map<String, Object> logEntry) {
    try {
      // Pošli log přímo do Loki s container=core-frontend (bez logování do backend
      // konzole)
      sendToLoki(logEntry);

      return ResponseEntity.ok("Log sent to Loki");
    } catch (Exception e) {
      // Pouze chyby logujeme do backend konzole
      logger.error("Error processing frontend log", e);
      return ResponseEntity.internalServerError().body("Error processing log");
    }
  }

  private void sendToLoki(Map<String, Object> logEntry) {
    try {
      // Vytvoř Loki payload
      Map<String, Object> lokiPayload = new HashMap<>();

      // Streams array
      List<Map<String, Object>> streams = List.of(createLokiStream(logEntry));
      lokiPayload.put("streams", streams);

      // Headers
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      // Pošli do Loki
      HttpEntity<Map<String, Object>> request = new HttpEntity<>(lokiPayload, headers);
      restTemplate.postForObject(LOKI_URL, request, String.class);

    } catch (Exception e) {
      logger.error("Failed to send log to Loki", e);
    }
  }

  private Map<String, Object> createLokiStream(Map<String, Object> logEntry) {
    Map<String, Object> stream = new HashMap<>();

    // Labels - nastavíme container=core-frontend
    Map<String, String> labels = new HashMap<>();
    labels.put("container", "core-frontend");
    labels.put("level", (String) logEntry.getOrDefault("level", "info"));
    if (logEntry.get("component") != null) {
      labels.put("component", (String) logEntry.get("component"));
    }
    stream.put("stream", labels);

    // Values - timestamp + message
    String timestamp = String.valueOf(Instant.now().toEpochMilli() * 1_000_000); // nanoseconds
    String message = formatLogMessage(logEntry);

    List<List<String>> values = List.of(List.of(timestamp, message));
    stream.put("values", values);

    return stream;
  }

  private String formatLogMessage(Map<String, Object> logEntry) {
    String level = (String) logEntry.get("level");
    String message = (String) logEntry.get("message");
    String component = (String) logEntry.get("component");
    String category = (String) logEntry.get("category");
    String url = (String) logEntry.get("url");

    StringBuilder sb = new StringBuilder();

    if (level != null)
      sb.append("[").append(level.toUpperCase()).append("] ");
    if (component != null)
      sb.append(component.toUpperCase()).append(": ");
    if (message != null)
      sb.append(message);
    if (url != null)
      sb.append(" (").append(url).append(")");
    if (category != null)
      sb.append(" [").append(category).append("]");

    return sb.toString();
  }
}