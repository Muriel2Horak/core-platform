package cz.muriel.core.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * 🆔 UUID Version 7 Generator
 * 
 * Generates time-ordered UUIDs that are:
 * - Globally unique (safe for parallel/distributed systems)
 * - Never repeat (even across different databases)
 * - Sortable by creation time
 * - Better for database indexes than random UUIDs
 * 
 * Format: [timestamp_ms (48 bits)][version (4 bits)][random (12 bits)][variant (2 bits)][random (62 bits)]
 * 
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-peabody-dispatch-new-uuid-format">UUID v7 RFC Draft</a>
 */
public class UUIDv7Generator {

  private static final SecureRandom RANDOM = new SecureRandom();

  /**
   * Generate a new UUID v7
   * 
   * @return time-ordered UUID that is globally unique
   */
  public static UUID generate() {
    return generate(Instant.now());
  }

  /**
   * Generate UUID v7 with specific timestamp (useful for testing)
   * 
   * @param timestamp the timestamp to embed in the UUID
   * @return time-ordered UUID
   */
  public static UUID generate(Instant timestamp) {
    long milliseconds = timestamp.toEpochMilli();

    // Generate random bytes for the rest
    byte[] randomBytes = new byte[10];
    RANDOM.nextBytes(randomBytes);

    // Build most significant bits: [timestamp_ms (48 bits)][version (4 bits)][random (12 bits)]
    long mostSigBits = 0;

    // Timestamp (48 bits)
    mostSigBits |= (milliseconds & 0xFFFF_FFFF_FFFFL) << 16;

    // Version 7 (4 bits) + random (12 bits)
    mostSigBits |= (0x7L << 12); // Version 7
    mostSigBits |= ((randomBytes[0] & 0x0FL) << 8) | (randomBytes[1] & 0xFFL);

    // Build least significant bits: [variant (2 bits)][random (62 bits)]
    long leastSigBits = 0;

    // Variant (2 bits = '10' for RFC 4122)
    leastSigBits |= (0x2L << 62);

    // Random (62 bits)
    for (int i = 2; i < 10; i++) {
      leastSigBits <<= 8;
      leastSigBits |= (randomBytes[i] & 0xFFL);
    }

    return new UUID(mostSigBits, leastSigBits);
  }

  /**
   * Extract timestamp from UUID v7
   * 
   * @param uuid the UUID v7 to extract timestamp from
   * @return the timestamp embedded in the UUID
   * @throws IllegalArgumentException if UUID is not version 7
   */
  public static Instant getTimestamp(UUID uuid) {
    if (uuid.version() != 7) {
      throw new IllegalArgumentException("UUID is not version 7: " + uuid);
    }

    long mostSigBits = uuid.getMostSignificantBits();
    long milliseconds = mostSigBits >>> 16;

    return Instant.ofEpochMilli(milliseconds);
  }

  /**
   * Check if UUID is version 7
   * 
   * @param uuid the UUID to check
   * @return true if UUID is version 7
   */
  public static boolean isUUIDv7(UUID uuid) {
    return uuid != null && uuid.version() == 7;
  }
}
