package cz.muriel.core.controller.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for exporting application logs. Provides CSV download
 * functionality for monitoring and debugging.
 * 
 * @see <a href=
 * "https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-return-types">Spring
 * MVC Return Types</a>
 */
@Slf4j @RestController @RequestMapping("/api/admin/logs")
public class LogsExportController {

  /**
   * Test endpoint for E2E testing - no authentication required. REMOVE in
   * production!
   */
  @GetMapping(value = "/export/test", produces = "text/csv")
  public ResponseEntity<Resource> exportLogsTest() {
    return exportLogs();
  }

  /**
   * Export application logs as CSV file.
   * 
   * <p>
   * Currently generates sample log entries for testing purposes. In production,
   * this should read from actual log files or log aggregation system.
   * 
   * @return CSV file with log entries (timestamp, level, message)
   */
  @GetMapping(value = "/export", produces = "text/csv") @PreAuthorize("hasAnyAuthority('CORE_ROLE_ADMIN', 'CORE_ROLE_SYSTEM_ADMIN')")
  public ResponseEntity<Resource> exportLogs() {
    log.info("📥 Log export requested");

    try {
      // Generate CSV content
      String csvContent = generateLogsCsv();

      // Convert to resource
      ByteArrayResource resource = new ByteArrayResource(
          csvContent.getBytes(StandardCharsets.UTF_8));

      // Generate filename with timestamp
      String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-");
      String filename = "logs-export-" + timestamp + ".csv";

      log.info("✅ Log export generated: {} ({} bytes)", filename, csvContent.length());

      // Return file download response
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
          .contentType(MediaType.parseMediaType("text/csv")).contentLength(resource.contentLength())
          .body(resource);

    } catch (Exception e) {
      log.error("❌ Failed to export logs", e);
      throw new RuntimeException("Failed to export logs: " + e.getMessage(), e);
    }
  }

  /**
   * Generate sample CSV content with log entries.
   * 
   * <p>
   * In production, this should:
   * <ul>
   * <li>Read from log files in /var/log</li>
   * <li>Query log aggregation system (Loki, ELK, etc.)</li>
   * <li>Filter by time range, tenant, severity, etc.</li>
   * </ul>
   * 
   * @return CSV content as String
   */
  private String generateLogsCsv() {
    List<String> lines = new ArrayList<>();

    // CSV header
    lines.add("timestamp,level,message,component");

    // Generate sample log entries (last minute)
    Instant now = Instant.now();
    for (int i = 60; i >= 0; i--) {
      Instant timestamp = now.minusSeconds(i);
      String level = getLogLevel(i);
      String message = getLogMessage(i, level);
      String component = getComponent(i);

      lines.add(String.format("%s,%s,\"%s\",%s", DateTimeFormatter.ISO_INSTANT.format(timestamp),
          level, message.replace("\"", "\"\""), // Escape quotes in CSV
          component));
    }

    return String.join("\n", lines);
  }

  /**
   * Generate realistic log level based on position.
   */
  private String getLogLevel(int secondsAgo) {
    if (secondsAgo % 20 == 0)
      return "ERROR";
    if (secondsAgo % 10 == 0)
      return "WARN";
    if (secondsAgo % 5 == 0)
      return "INFO";
    return "DEBUG";
  }

  /**
   * Generate realistic log message based on level.
   */
  private String getLogMessage(int secondsAgo, String level) {
    return switch (level) {
    case "ERROR" -> "Failed to process request: Connection timeout";
    case "WARN" -> "High memory usage detected: 85%";
    case "INFO" -> "Request processed successfully: user-" + secondsAgo;
    default -> "Processing entity: id=" + secondsAgo;
    };
  }

  /**
   * Generate component name.
   */
  private String getComponent(int secondsAgo) {
    String[] components = { "cz.muriel.core.auth.AuthService", "cz.muriel.core.monitoring.BFF",
        "cz.muriel.core.entities.EntityService", "cz.muriel.core.workflow.WorkflowEngine" };
    return components[secondsAgo % components.length];
  }
}
