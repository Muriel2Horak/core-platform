package cz.muriel.core.metamodel.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utility for generating deterministic UUIDs from entity data Ensures same
 * input always produces same UUID - critical for data portability
 */
@Slf4j
public class DeterministicUuidGenerator {

  /**
   * Generate deterministic UUID from source fields
   *
   * @param prefix Prefix for composite key (e.g., "role:", "user:")
   * @param sourceFields List of field names to include in hash
   * @param entityData Map of entity field values
   * @param algorithm Hash algorithm (sha256, md5)
   * @return Deterministic UUID
   */
  public static UUID generate(String prefix, List<String> sourceFields,
      Map<String, Object> entityData, String algorithm) {

    if (sourceFields == null || sourceFields.isEmpty()) {
      throw new IllegalArgumentException(
          "Source fields required for deterministic UUID generation");
    }

    // Build composite string: "prefix:field1Value:field2Value:..."
    StringBuilder composite = new StringBuilder(prefix != null ? prefix : "");

    for (String fieldName : sourceFields) {
      Object value = entityData.get(fieldName);
      if (value == null) {
        log.warn("Source field '{}' is null, using empty string", fieldName);
        composite.append(":");
      } else {
        composite.append(":").append(value.toString());
      }
    }

    String compositeKey = composite.toString();
    log.debug("Generating UUID from composite: {}", compositeKey);

    return generateFromString(compositeKey, algorithm != null ? algorithm : "sha256");
  }

  /**
   * Generate deterministic UUID from single string
   */
  public static UUID generateFromString(String input, String algorithm) {
    try {
      MessageDigest digest = MessageDigest
          .getInstance(algorithm.equalsIgnoreCase("md5") ? "MD5" : "SHA-256");

      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

      // Convert first 16 bytes to UUID
      long mostSigBits = 0;
      long leastSigBits = 0;

      for (int i = 0; i < 8; i++) {
        mostSigBits = (mostSigBits << 8) | (hash[i] & 0xff);
      }
      for (int i = 8; i < 16; i++) {
        leastSigBits = (leastSigBits << 8) | (hash[i] & 0xff);
      }

      // Set version (4) and variant (2) bits for UUID compliance
      mostSigBits &= ~(0xF000L << 48);
      mostSigBits |= (0x4000L << 48); // Version 4
      leastSigBits &= ~(0xC000000000000000L);
      leastSigBits |= 0x8000000000000000L; // Variant 2

      UUID result = new UUID(mostSigBits, leastSigBits);
      log.debug("Generated UUID: {} from input: {}", result, input);
      return result;

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Hash algorithm not available: " + algorithm, e);
    }
  }

  /**
   * Validate that UUID generation config is complete
   */
  public static void validateConfig(String prefix, List<String> sourceFields) {
    if (sourceFields == null || sourceFields.isEmpty()) {
      throw new IllegalArgumentException(
          "Deterministic ID generation requires sourceFields configuration");
    }
  }
}
