package cz.muriel.core.reporting.app;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Reporting module.
 * 
 * Configured via application-reporting.yml.
 */
@Data @Validated @ConfigurationProperties(prefix = "reporting")
public class ReportingProperties {

  /**
   * Master toggle for reporting features.
   */
  private boolean enabled = true;

  /**
   * Maximum number of rows that can be returned in a single query.
   */
  @Min(1)
  private int maxRows = 50000;

  /**
   * Maximum time interval (in days) allowed for queries.
   */
  @Min(1)
  private int maxIntervalDays = 92;

  /**
   * Default cache TTL in seconds.
   */
  @Min(0)
  private int defaultTtlSeconds = 60;

  @NotNull
  private CacheConfig cache = new CacheConfig();

  @NotNull
  private RateLimitConfig rateLimit = new RateLimitConfig();

  @NotNull
  private CubeConfig cube = new CubeConfig();

  @NotNull
  private BulkConfig bulk = new BulkConfig();

  @Data
  public static class CacheConfig {
    /**
     * Cache provider: redis or caffeine.
     */
    @NotBlank
    private String provider = "redis";

    /**
     * Redis key prefix for report cache.
     */
    @NotBlank
    private String keyPrefix = "rpt:";
  }

  @Data
  public static class RateLimitConfig {
    /**
     * Maximum requests per tenant per minute.
     */
    @Min(1)
    private int perTenantPerMin = 120;
  }

  @Data
  public static class CubeConfig {
    /**
     * Base URL of Cube.js API.
     */
    @NotBlank
    private String baseUrl = "http://cube:4000";

    /**
     * API token for Cube.js authentication.
     */
    private String apiToken;

    /**
     * Connection timeout in milliseconds.
     */
    @Min(100)
    private int connectTimeoutMs = 5000;

    /**
     * Read timeout in milliseconds.
     */
    @Min(1000)
    private int readTimeoutMs = 30000;
  }

  @Data
  public static class BulkConfig {
    /**
     * Number of rows to process per chunk in bulk operations.
     */
    @Min(1)
    private int chunkSize = 1000;

    /**
     * Maximum total rows that can be affected by a bulk operation.
     */
    @Min(1)
    private int maxAffectRows = 500000;

    /**
     * Maximum concurrent bulk jobs.
     */
    @Min(1)
    private int queueConcurrency = 2;

    /**
     * Timeout for bulk operations in seconds.
     */
    @Min(1)
    private int timeoutSeconds = 300;
  }
}
