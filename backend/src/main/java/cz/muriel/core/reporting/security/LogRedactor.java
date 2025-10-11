package cz.muriel.core.reporting.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Log Redaction Utility
 * 
 * Redacts sensitive fields from logs to prevent PII leakage: - Passwords, API
 * keys, tokens - Email addresses (partially masked) - Phone numbers - Credit
 * card numbers - SSN/national IDs
 * 
 * Usage: ```java log.info("User data: {}", logRedactor.redact(userData)); ```
 * 
 * OWASP Compliance: - A1:2021 - Broken Access Control (prevent PII leakage in
 * logs) - A4:2021 - Insecure Design (secure logging practices)
 */
@Slf4j @Component
public class LogRedactor {

  private static final String REDACTED = "***REDACTED***";
  private static final String EMAIL_MASK = "$1***@$3";

  // Sensitive field names (case-insensitive)
  private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(
      Arrays.asList("password", "passwd", "pwd", "secret", "token", "api_key", "apikey", "auth",
          "authorization", "credential", "private_key", "privatekey", "ssn", "social_security",
          "national_id", "tax_id", "credit_card", "card_number", "cvv", "ccv", "bank_account",
          "iban", "swift", "routing_number"));

  // Patterns for sensitive data
  private static final Pattern EMAIL_PATTERN = Pattern.compile("(\\w{1,3})\\w+(@)(\\w+\\.\\w+)");

  private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d[\\d\\s()-]{7,}\\d");

  private static final Pattern CREDIT_CARD_PATTERN = Pattern
      .compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");

  /**
   * Redact sensitive fields from a Map
   */
  public Map<String, Object> redact(Map<String, Object> data) {
    if (data == null) {
      return null;
    }

    Map<String, Object> redacted = new java.util.HashMap<>(data);

    for (Map.Entry<String, Object> entry : data.entrySet()) {
      String key = entry.getKey().toLowerCase();
      Object value = entry.getValue();

      // Check if field name is sensitive
      if (isSensitiveField(key)) {
        redacted.put(entry.getKey(), REDACTED);
      }
      // Check if value contains sensitive patterns
      else if (value instanceof String strValue) {
        redacted.put(entry.getKey(), redactString(strValue));
      }
      // Recursively redact nested maps
      else if (value instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) value;
        redacted.put(entry.getKey(), redact(nestedMap));
      }
    }

    return redacted;
  }

  /**
   * Redact sensitive data from a string
   */
  public String redactString(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }

    String result = input;

    // Redact email addresses (keep first 3 chars + domain)
    result = EMAIL_PATTERN.matcher(result).replaceAll(EMAIL_MASK);

    // Redact phone numbers
    result = PHONE_PATTERN.matcher(result).replaceAll("***PHONE***");

    // Redact credit card numbers
    result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("****-****-****-****");

    return result;
  }

  /**
   * Check if field name is sensitive
   */
  private boolean isSensitiveField(String fieldName) {
    String lower = fieldName.toLowerCase();

    // Exact match
    if (SENSITIVE_FIELDS.contains(lower)) {
      return true;
    }

    // Partial match (contains sensitive keyword)
    for (String sensitive : SENSITIVE_FIELDS) {
      if (lower.contains(sensitive)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Redact object for logging (handles Map, String, etc.)
   */
  public Object redactForLogging(Object obj) {
    if (obj == null) {
      return null;
    }

    if (obj instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) obj;
      return redact(map);
    }

    if (obj instanceof String str) {
      return redactString(str);
    }

    return obj;
  }
}
