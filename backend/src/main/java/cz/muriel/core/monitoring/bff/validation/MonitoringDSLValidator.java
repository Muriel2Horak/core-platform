package cz.muriel.core.monitoring.bff.validation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for monitoring DSL queries (LogQL, PromQL)
 * 
 * Provides security guardrails: - Prevents SQL injection attempts - Prevents
 * command injection attempts - Prevents XSS attempts - Enforces query
 * complexity limits - Enforces max query length
 */
@Component
public class MonitoringDSLValidator {

  private static final int MAX_QUERY_LENGTH = 5000;
  private static final int MAX_PIPE_OPERATORS = 50; // Max complexity

  // SQL injection patterns
  private static final Pattern[] SQL_INJECTION_PATTERNS = {
      Pattern.compile("\\b(DROP|ALTER|CREATE|TRUNCATE)\\s+(TABLE|DATABASE|INDEX)",
          Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\b(DELETE)\\s+FROM\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\b(UPDATE)\\s+\\w+\\s+SET\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\b(INSERT)\\s+INTO\\b", Pattern.CASE_INSENSITIVE), Pattern.compile("--;"),
      Pattern.compile("/\\*.*\\*/"),
      Pattern.compile("\\bUNION\\s+SELECT\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\bEXEC(UTE)?\\s*\\(", Pattern.CASE_INSENSITIVE) };

  // Command injection patterns
  private static final Pattern[] COMMAND_INJECTION_PATTERNS = {
      Pattern.compile("\\b(rm|cat|curl|wget|bash|sh|eval|exec)\\s", Pattern.CASE_INSENSITIVE),
      Pattern.compile("[;&|`$]\\s*(rm|cat|ls|pwd|whoami|id)", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\$\\([^)]+\\)"), // $(command)
      Pattern.compile("`[^`]+`") // `command`
  };

  // XSS patterns
  private static final Pattern[] XSS_PATTERNS = {
      Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE),
      Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
      Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
      Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
      Pattern.compile("onerror\\s*=", Pattern.CASE_INSENSITIVE),
      Pattern.compile("onload\\s*=", Pattern.CASE_INSENSITIVE) };

  /**
   * Validates a monitoring DSL query
   * 
   * @param query The query to validate (LogQL or PromQL)
   * @return List of validation error messages (empty if valid)
   */
  public List<String> validate(String query) {
    List<String> errors = new ArrayList<>();

    // Check for null/empty
    if (query == null) {
      errors.add("Query cannot be null");
      return errors;
    }

    String trimmed = query.trim();
    if (trimmed.isEmpty()) {
      errors.add("Query cannot be empty");
      return errors;
    }

    // Check max length
    if (query.length() > MAX_QUERY_LENGTH) {
      errors.add("Query exceeds maximum length of " + MAX_QUERY_LENGTH + " characters");
    }

    // Check complexity (number of pipe operators)
    long pipeCount = query.chars().filter(ch -> ch == '|').count();
    if (pipeCount > MAX_PIPE_OPERATORS) {
      errors.add("Query is too complex (too many pipe operators: " + pipeCount + ", max: "
          + MAX_PIPE_OPERATORS + ")");
    }

    // Check for SQL injection
    for (Pattern pattern : SQL_INJECTION_PATTERNS) {
      if (pattern.matcher(query).find()) {
        errors.add("Query contains potential SQL injection pattern");
        break;
      }
    }

    // Check for command injection
    for (Pattern pattern : COMMAND_INJECTION_PATTERNS) {
      if (pattern.matcher(query).find()) {
        errors.add("Query contains potential command injection pattern");
        break;
      }
    }

    // Check for XSS
    for (Pattern pattern : XSS_PATTERNS) {
      if (pattern.matcher(query).find()) {
        errors.add("Query contains potential XSS/HTML injection pattern");
        break;
      }
    }

    // Check brace balance (basic syntax check)
    if (!isBracesBalanced(query)) {
      errors.add("Query has unbalanced braces/brackets - syntax error");
    }

    return errors;
  }

  /**
   * Checks if braces are balanced in the query
   */
  private boolean isBracesBalanced(String query) {
    int curlyBraces = 0;
    int squareBrackets = 0;
    int parentheses = 0;

    for (char ch : query.toCharArray()) {
      switch (ch) {
      case '{':
        curlyBraces++;
        break;
      case '}':
        curlyBraces--;
        break;
      case '[':
        squareBrackets++;
        break;
      case ']':
        squareBrackets--;
        break;
      case '(':
        parentheses++;
        break;
      case ')':
        parentheses--;
        break;
      }

      // Check for negative (closing before opening)
      if (curlyBraces < 0 || squareBrackets < 0 || parentheses < 0) {
        return false;
      }
    }

    // All should be zero at the end
    return curlyBraces == 0 && squareBrackets == 0 && parentheses == 0;
  }
}
