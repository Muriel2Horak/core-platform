package cz.muriel.core.auth.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for HMAC operations and secure comparisons.
 */
public class HmacUtils {

  private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

  /**
   * Computes HMAC-SHA256 for the given data and secret.
   * 
   * @param data the data to sign
   * @param secret the secret key
   * @return the HMAC as hex string
   * @throws RuntimeException if HMAC computation fails
   */
  public static String computeHmacSha256(byte[] data, String secret) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
      SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
          HMAC_SHA256_ALGORITHM);
      mac.init(secretKey);
      byte[] hmacBytes = mac.doFinal(data);
      return bytesToHex(hmacBytes);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException("Failed to compute HMAC-SHA256", e);
    }
  }

  /**
   * Performs constant-time comparison of two byte arrays to prevent timing
   * attacks.
   * 
   * @param a first array
   * @param b second array
   * @return true if arrays are equal, false otherwise
   */
  public static boolean slowEquals(byte[] a, byte[] b) {
    if (a == null || b == null) {
      return a == b;
    }

    if (a.length != b.length) {
      return false;
    }

    int result = 0;
    for (int i = 0; i < a.length; i++) {
      result |= a[i] ^ b[i];
    }

    return result == 0;
  }

  /**
   * Performs constant-time comparison of two strings to prevent timing attacks.
   * 
   * @param a first string
   * @param b second string
   * @return true if strings are equal, false otherwise
   */
  public static boolean slowEquals(String a, String b) {
    if (a == null || b == null) {
      return a == b;
    }

    byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
    byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
    return slowEquals(aBytes, bBytes);
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }
}
