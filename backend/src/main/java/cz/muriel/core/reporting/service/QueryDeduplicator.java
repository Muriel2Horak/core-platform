package cz.muriel.core.reporting.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deduplicates identical concurrent queries using Single-Flight pattern.
 * 
 * When multiple requests for the same query arrive simultaneously, only ONE
 * query is executed against Cube.js. Other requests wait for the result and
 * share the same response.
 * 
 * This prevents thundering herd problems and reduces load on Cube.js.
 */
@Component @Slf4j
public class QueryDeduplicator {

  private final Map<String, CompletableFuture<Map<String, Object>>> inflightQueries = new ConcurrentHashMap<>();

  /**
   * Executes a query with deduplication. If an identical query is already in
   * flight, waits for its result. Otherwise, executes the query and shares the
   * result with waiters.
   *
   * @param query Cube.js query object
   * @param tenantId Tenant ID for fingerprinting
   * @param executor Function that executes the actual query
   * @return Query result
   */
  public Map<String, Object> executeWithDeduplication(Map<String, Object> query, String tenantId,
      java.util.function.Supplier<Map<String, Object>> executor) {
    String fingerprint = computeFingerprint(query, tenantId);

    CompletableFuture<Map<String, Object>> future = inflightQueries.computeIfAbsent(fingerprint,
        key -> {
          log.debug("Executing new query with fingerprint: {}", fingerprint);
          return CompletableFuture.supplyAsync(() -> {
            try {
              return executor.get();
            } finally {
              // Remove from inflight map after completion
              inflightQueries.remove(fingerprint);
            }
          });
        });

    try {
      if (inflightQueries.containsKey(fingerprint) && future != inflightQueries.get(fingerprint)) {
        log.info("Deduplicating query - waiting for in-flight result (fingerprint: {})",
            fingerprint);
      }
      return future.join(); // Wait for result
    } catch (Exception e) {
      // If execution failed, remove from inflight
      inflightQueries.remove(fingerprint);
      throw e;
    }
  }

  /**
   * Computes SHA-256 fingerprint of query + tenant for deduplication. Identical
   * queries (same dimensions, filters, tenant) get same fingerprint.
   *
   * @param query Cube.js query object
   * @param tenantId Tenant ID
   * @return SHA-256 hex string
   */
  private String computeFingerprint(Map<String, Object> query, String tenantId) {
    try {
      String queryString = query.toString() + ":" + tenantId;
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(queryString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return bytesToHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  /**
   * Converts byte array to hex string.
   */
  private String bytesToHex(byte[] bytes) {
    StringBuilder hexString = new StringBuilder(2 * bytes.length);
    for (byte b : bytes) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

  /**
   * Returns number of currently in-flight queries. Used for monitoring and
   * diagnostics.
   */
  public int getInflightCount() {
    return inflightQueries.size();
  }

  /**
   * Clears all in-flight queries. Used for testing and forced cache invalidation.
   */
  public void clear() {
    inflightQueries.clear();
  }
}
