package cz.muriel.core.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@CrossOrigin(origins = { "http://localhost:3000", "http://frontend:3000" })
public class FrontendLogsController {

  private static final Logger logger = LoggerFactory.getLogger("FRONTEND");
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private static final String LOKI_URL = "http://loki:3100/loki/api/v1/push";

  public FrontendLogsController(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    this.objectMapper = new ObjectMapper();
  }

  @PostMapping("/api/logs/frontend")
  public ResponseEntity<String> receiveFrontendLog(@RequestBody Map<String, Object> logEntry,
      HttpServletRequest request) {
    try {
      // Získáme real IP adresu z requestu
      String clientIp = getRealClientIp(request);

      // Normalizujeme a obohacujeme log entry
      Map<String, Object> enrichedLogEntry = enrichLogEntry(logEntry, clientIp, request);

      // Pošli log přímo do Loki s container=core-frontend
      sendToLoki(enrichedLogEntry);

      return ResponseEntity.ok("Log sent to Loki");
    } catch (Exception e) {
      // Pouze chyby logujeme do backend konzole
      logger.error("Error processing frontend log", e);
      return ResponseEntity.internalServerError().body("Error processing log");
    }
  }

  // Získání real IP adresy z různých headerů
  private String getRealClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    String xRealIp = request.getHeader("X-Real-IP");
    String xForwardedProto = request.getHeader("X-Forwarded-Proto");

    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      // X-Forwarded-For může obsahovat více IP adres, první je real client IP
      return xForwardedFor.split(",")[0].trim();
    }

    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }

    // Fallback na remote address
    return request.getRemoteAddr();
  }

  private Map<String, Object> enrichLogEntry(Map<String, Object> logEntry, String clientIp,
      HttpServletRequest request) {
    Map<String, Object> enriched = new HashMap<>(logEntry);

    // Přidáme IP adresu
    enriched.put("clientIp", clientIp);

    // Přidáme User-Agent pro lepší tracking
    String userAgent = request.getHeader("User-Agent");
    if (userAgent != null) {
      enriched.put("userAgent", userAgent);
    }

    // Normalizujeme level
    String level = (String) enriched.get("level");
    if (level != null) {
      enriched.put("level", level.toUpperCase());
    }

    // Přidáme session info pokud je dostupné
    String sessionId = request.getSession(false) != null ? request.getSession().getId() : null;
    if (sessionId != null) {
      enriched.put("sessionId", sessionId);
    }

    return enriched;
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

    // Labels pro Loki - důležité pro filtrování a indexování
    Map<String, String> labels = new HashMap<>();
    labels.put("container", "core-frontend");
    labels.put("service", "core-platform-frontend");
    labels.put("source", "frontend");

    // Přidáme level do labelů pro lepší filtrování v Grafaně
    String level = (String) logEntry.getOrDefault("level", "INFO");
    labels.put("level", level.toLowerCase());

    // Přidáme operaci do labelů
    String operation = (String) logEntry.get("operation");
    if (operation != null) {
      String normalizedOperation = operation.toLowerCase()
          .replace("_", "-")
          .replace(" ", "-");
      labels.put("operation", normalizedOperation);
    }

    // Kategorie pro audit/security logy
    @SuppressWarnings("unchecked")
    Map<String, Object> details = (Map<String, Object>) logEntry.get("details");
    if (details != null) {
      String category = (String) details.get("category");
      if (category != null) {
        labels.put("category", category.toLowerCase());
      }

      // Přidáme HTTP status do labelů pro API calls
      Object httpStatus = details.get("http_status");
      if (httpStatus != null) {
        String statusCategory = categorizeHttpStatus(httpStatus.toString());
        labels.put("status_category", statusCategory);
      }
    }

    // Přidáme typ události
    String eventType = determineEventType(logEntry);
    labels.put("event_type", eventType);

    stream.put("stream", labels);

    // Values - timestamp + formatted message
    String timestamp = getLogTimestamp(logEntry);
    String message = formatLogMessage(logEntry);

    List<List<String>> values = List.of(List.of(timestamp, message));
    stream.put("values", values);

    return stream;
  }

  private String categorizeHttpStatus(String status) {
    try {
      int statusCode = Integer.parseInt(status);
      if (statusCode >= 200 && statusCode < 300)
        return "success";
      if (statusCode >= 300 && statusCode < 400)
        return "redirect";
      if (statusCode >= 400 && statusCode < 500)
        return "client_error";
      if (statusCode >= 500)
        return "server_error";
      return "unknown";
    } catch (NumberFormatException e) {
      return "unknown";
    }
  }

  private String determineEventType(Map<String, Object> logEntry) {
    String operation = (String) logEntry.get("operation");
    String level = (String) logEntry.get("level");

    @SuppressWarnings("unchecked")
    Map<String, Object> details = (Map<String, Object>) logEntry.get("details");

    // Security události
    if (details != null && "security".equals(details.get("category"))) {
      return "security";
    }

    // Audit události
    if (details != null && "audit".equals(details.get("category"))) {
      return "audit";
    }

    // Error události
    if ("ERROR".equalsIgnoreCase(level)) {
      return "error";
    }

    // API call události
    if (operation != null && (operation.contains("api") ||
        operation.contains("request") ||
        operation.contains("call"))) {
      return "api";
    }

    // User interaction události
    if (operation != null && (operation.contains("click") ||
        operation.contains("navigation") ||
        operation.contains("user"))) {
      return "user_interaction";
    }

    // Performance události
    if (operation != null && (operation.contains("performance") ||
        operation.contains("timing") ||
        operation.contains("metric"))) {
      return "performance";
    }

    return "general";
  }

  private String getLogTimestamp(Map<String, Object> logEntry) {
    String timestampStr = (String) logEntry.get("timestamp");
    try {
      // Konvertuj ISO timestamp na Loki nanoseconds
      OffsetDateTime timestamp = OffsetDateTime.parse(timestampStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
      return String.valueOf(timestamp.toInstant().toEpochMilli() * 1_000_000);
    } catch (Exception e) {
      // Fallback na current time
      return String.valueOf(Instant.now().toEpochMilli() * 1_000_000);
    }
  }

  private String formatLogMessage(Map<String, Object> logEntry) {
    String timestamp = (String) logEntry.get("timestamp");
    String level = (String) logEntry.get("level");
    String clientIp = (String) logEntry.get("clientIp");
    String login = (String) logEntry.get("login");
    String operation = (String) logEntry.get("operation");
    String message = (String) logEntry.get("message");

    @SuppressWarnings("unchecked")
    Map<String, Object> details = (Map<String, Object>) logEntry.get("details");

    StringBuilder sb = new StringBuilder();

    // Formát: [LEVEL] timestamp IP login OPERATION: message [details]
    if (level != null) {
      sb.append("[").append(level).append("] ");
    }

    if (timestamp != null) {
      try {
        OffsetDateTime dt = OffsetDateTime.parse(timestamp);
        sb.append(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))).append(" ");
      } catch (Exception e) {
        sb.append(timestamp).append(" ");
      }
    }

    if (clientIp != null) {
      sb.append("IP:").append(clientIp).append(" ");
    }

    if (login != null && !"anonymous".equals(login) && !"null".equals(login)) {
      sb.append("User:").append(login).append(" ");
    }

    if (operation != null) {
      sb.append("[").append(operation).append("] ");
    }

    if (message != null) {
      sb.append(message);
    }

    // Přidáme strukturované detaily
    if (details != null && !details.isEmpty()) {
      sb.append(" | ");
      appendStructuredDetails(sb, details);
    }

    return sb.toString();
  }

  private void appendStructuredDetails(StringBuilder sb, Map<String, Object> details) {
    boolean first = true;

    // Prioritní informace první
    String[] priorityKeys = { "category", "http_status", "duration", "error_code", "url" };

    for (String key : priorityKeys) {
      Object value = details.get(key);
      if (value != null) {
        if (!first)
          sb.append(", ");
        sb.append(key).append(":").append(formatDetailValue(value));
        first = false;
      }
    }

    // Ostatní detaily
    for (Map.Entry<String, Object> entry : details.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      // Přeskočíme už zpracované klíče
      boolean isPriorityKey = false;
      for (String priorityKey : priorityKeys) {
        if (priorityKey.equals(key)) {
          isPriorityKey = true;
          break;
        }
      }

      if (!isPriorityKey && value != null) {
        if (!first)
          sb.append(", ");
        sb.append(key).append(":").append(formatDetailValue(value));
        first = false;
      }
    }
  }

  private String formatDetailValue(Object value) {
    if (value == null)
      return "null";

    if (value instanceof String) {
      String str = (String) value;
      // Zkrátíme dlouhé stringy
      return str.length() > 100 ? str.substring(0, 100) + "..." : str;
    }

    if (value instanceof Map || value instanceof List) {
      try {
        String json = objectMapper.writeValueAsString(value);
        return json.length() > 200 ? json.substring(0, 200) + "..." : json;
      } catch (JsonProcessingException e) {
        return value.toString();
      }
    }

    return value.toString();
  }
}