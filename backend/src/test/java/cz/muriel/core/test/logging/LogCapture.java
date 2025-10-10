package cz.muriel.core.test.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Test utility for capturing log output
 * 
 * Usage:
 * 
 * <pre>
 * LogCapture logCapture = LogCapture.forClass(MyService.class);
 * logCapture.start();
 * 
 * // Execute code that logs
 * myService.doSomething();
 * 
 * // Verify logs
 * assertThat(logCapture.getMessages()).contains("Expected log message");
 * assertThat(logCapture.getMessages()).doesNotContain("SECRET_TOKEN");
 * 
 * logCapture.stop();
 * </pre>
 */
public class LogCapture {

  private final Logger logger;
  private final ListAppender<ILoggingEvent> listAppender;

  private LogCapture(Logger logger) {
    this.logger = logger;
    this.listAppender = new ListAppender<>();
    this.listAppender.setContext(logger.getLoggerContext());
  }

  /**
   * Creates a log capture for the specified class
   */
  public static LogCapture forClass(Class<?> clazz) {
    Logger logger = (Logger) LoggerFactory.getLogger(clazz);
    return new LogCapture(logger);
  }

  /**
   * Creates a log capture for the specified logger name
   */
  public static LogCapture forLogger(String loggerName) {
    Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
    return new LogCapture(logger);
  }

  /**
   * Creates a log capture for the root logger
   */
  public static LogCapture forRoot() {
    Logger logger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    return new LogCapture(logger);
  }

  /**
   * Starts capturing logs
   */
  public void start() {
    listAppender.start();
    logger.addAppender(listAppender);
  }

  /**
   * Stops capturing logs
   */
  public void stop() {
    logger.detachAppender(listAppender);
    listAppender.stop();
  }

  /**
   * Gets all captured log events
   */
  public List<ILoggingEvent> getEvents() {
    return listAppender.list;
  }

  /**
   * Gets all captured log messages as strings
   */
  public List<String> getMessages() {
    return listAppender.list.stream().map(ILoggingEvent::getFormattedMessage)
        .collect(Collectors.toList());
  }

  /**
   * Gets all captured logs (level + message)
   */
  public List<String> getFormattedLogs() {
    return listAppender.list.stream()
        .map(event -> event.getLevel() + ": " + event.getFormattedMessage())
        .collect(Collectors.toList());
  }

  /**
   * Clears all captured logs
   */
  public void clear() {
    listAppender.list.clear();
  }

  /**
   * Gets the number of captured log events
   */
  public int size() {
    return listAppender.list.size();
  }

  /**
   * Checks if any log contains the specified text
   */
  public boolean contains(String text) {
    return getMessages().stream().anyMatch(msg -> msg.contains(text));
  }

  /**
   * Checks if no log contains the specified text
   */
  public boolean doesNotContain(String text) {
    return getMessages().stream().noneMatch(msg -> msg.contains(text));
  }
}
