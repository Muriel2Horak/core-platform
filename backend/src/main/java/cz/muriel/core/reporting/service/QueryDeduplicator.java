package cz.muriel.core.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

  private static final ObjectMapper FINGERPRINT_MAPPER = new ObjectMapper()
      .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

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

    CompletableFuture<Map<String, Object>> placeholder = new CompletableFuture<>();
    CompletableFuture<Map<String, Object>> existing = inflightQueries.putIfAbsent(fingerprint,
        placeholder);
    CompletableFuture<Map<String, Object>> future = existing != null ? existing : placeholder;

    if (existing == null) {
      log.debug("Executing new query with fingerprint: {}", fingerprint);
      try {
        placeholder.complete(executor.get());
      } catch (Exception e) {
        placeholder.completeExceptionally(e);
      } finally {
        inflightQueries.remove(fingerprint, placeholder);
      }
    }

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
  String computeFingerprint(Map<String, Object> query, String tenantId) {
    try {
      Object canonical = canonicalize(query);
      String queryString = FINGERPRINT_MAPPER.writeValueAsString(canonical) + ":" + tenantId;
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(queryString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return bytesToHex(hash);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize query for fingerprinting", e);
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

  private Object canonicalize(Object value) {
    if (value instanceof Map<?, ?> map) {
      var sorted = new java.util.TreeMap<String, Object>();
      for (var entry : map.entrySet()) {
        sorted.put(String.valueOf(entry.getKey()), canonicalize(entry.getValue()));
      }
      return sorted;
    }
    if (value instanceof java.util.List<?> list) {
      var canonical = new java.util.ArrayList<>(list.size());
      for (var item : list) {
        canonical.add(canonicalize(item));
      }
      return canonical;
    }
    return value;
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
