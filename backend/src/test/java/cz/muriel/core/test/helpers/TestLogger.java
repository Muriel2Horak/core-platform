package cz.muriel.core.test.helpers;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * 🎨 Test Progress Logger
 * 
 * Beautiful UX helpers for test execution progress with emoji icons and
 * structured output. Use these methods to make your tests more readable and
 * user-friendly.
 * 
 * @example
 * 
 * <pre>
 * {@code
 * class MyTest {
 *   @Test
 *   void myTest() {
 *     TestLogger.testStart("My Test", 1, 10);
 *     TestLogger.step("Step 1: Doing something...");
 *     TestLogger.success("Action completed");
 *     TestLogger.testEnd();
 *   }
 * }
 * }
 * </pre>
 */
@Slf4j @UtilityClass
public class TestLogger {

  private static final String SEPARATOR = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
  private static final String DOUBLE_SEPARATOR = "═══════════════════════════════════════════════════";

  /**
   * Log test suite start
   */
  public static void suiteStart(String name) {
    log.info("\n");
    log.info(DOUBLE_SEPARATOR);
    log.info("🚀  {} - STARTING", name);
    log.info(DOUBLE_SEPARATOR);
    log.info("");
  }

  /**
   * Log test suite end
   */
  public static void suiteEnd(String name) {
    log.info("\n");
    log.info(DOUBLE_SEPARATOR);
    log.info("🏁  {} - COMPLETE", name);
    log.info(DOUBLE_SEPARATOR);
    log.info("");
  }

  /**
   * Log individual test start
   */
  public static void testStart(String name, int current, int total) {
    log.info("\n📝 TEST {}/{}: {}", current, total, name);
    log.info(SEPARATOR);
    log.info("");
  }

  /**
   * Log individual test start (without counter)
   */
  public static void testStart(String name) {
    log.info("\n📝 TEST: {}", name);
    log.info(SEPARATOR);
    log.info("");
  }

  /**
   * Log test end with result
   */
  public static void testEnd() {
    testEnd(true);
  }

  /**
   * Log test end with result
   */
  public static void testEnd(boolean passed) {
    if (passed) {
      log.info("\n✅ TEST PASSED - All assertions successful!\n");
    } else {
      log.error("\n❌ TEST FAILED - See errors above\n");
    }
  }

  /**
   * Log a step in the test
   */
  public static void step(String message, int stepNum) {
    log.info("\n🔧 Step {}: {}", stepNum, message);
  }

  /**
   * Log a step in the test (without number)
   */
  public static void step(String message) {
    log.info("\n🔧 {}", message);
  }

  /**
   * Log successful action
   */
  public static void success(String message) {
    log.info("   ✓ {}", message);
  }

  /**
   * Log successful action (no indent)
   */
  public static void successNoIndent(String message) {
    log.info("✓ {}", message);
  }

  /**
   * Log info message
   */
  public static void info(String message) {
    log.info("   ℹ️  {}", message);
  }

  /**
   * Log info message (no indent)
   */
  public static void infoNoIndent(String message) {
    log.info("ℹ️  {}", message);
  }

  /**
   * Log warning
   */
  public static void warn(String message) {
    log.warn("   ⚠️  {}", message);
  }

  /**
   * Log warning (no indent)
   */
  public static void warnNoIndent(String message) {
    log.warn("⚠️  {}", message);
  }

  /**
   * Log error
   */
  public static void error(String message) {
    log.error("   ❌ {}", message);
  }

  /**
   * Log error (no indent)
   */
  public static void errorNoIndent(String message) {
    log.error("❌ {}", message);
  }

  /**
   * Log action being performed
   */
  public static void action(String message) {
    log.info("\n🚀 {}", message);
  }

  /**
   * Log verification being performed
   */
  public static void verify(String message) {
    log.info("\n🧪 {}", message);
  }

  /**
   * Log search/query operation
   */
  public static void search(String message) {
    log.info("\n🔍 {}", message);
  }

  /**
   * Log cleanup operation
   */
  public static void cleanup(String message) {
    log.info("\n🧹 {}", message);
  }

  /**
   * Log setup operation
   */
  public static void setup(String message) {
    log.info("\n🔧 {}", message);
  }

  /**
   * Log data/result
   */
  public static void data(String label, Object value) {
    log.info("   📊 {}: {}", label, value);
  }

  /**
   * Log tree item
   */
  public static void treeItem(String label, Object value, boolean isLast) {
    String prefix = isLast ? "   └─ " : "   ├─ ";
    log.info("{}{}: {}", prefix, label, value);
  }

  /**
   * Log separator
   */
  public static void separator() {
    log.info(SEPARATOR);
  }

  /**
   * Log double separator
   */
  public static void doubleSeparator() {
    log.info(DOUBLE_SEPARATOR);
  }
}
