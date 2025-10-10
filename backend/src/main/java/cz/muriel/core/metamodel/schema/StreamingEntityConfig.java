package cz.muriel.core.metamodel.schema;

import lombok.Data;

/**
 * Streaming configuration for entity metamodel
 */
@Data
public class StreamingEntityConfig {
  /**
   * Whether streaming is enabled for this entity (override global)
   */
  private Boolean enabled;

  /**
   * Number of partitions for this entity's topics
   */
  private Integer partitions = 3;

  /**
   * Replication factor for topics
   */
  private Integer replicationFactor = 1;

  /**
   * Retention time in hours for event topics (compacted)
   */
  private Integer retentionHours = 168; // 7 days

  /**
   * Retention time in minutes for inflight topics
   */
  private Integer inflightRetentionMinutes = 30;

  /**
   * Command topic retention in hours
   */
  private Integer commandRetentionHours = 24;

  /**
   * Event payload mode: full|diff|minimal
   */
  private String eventPayloadMode = "diff";

  /**
   * Enable snapshot in events
   */
  private Boolean includeSnapshot = false;

  /**
   * Strict reads mode - return 423 when entity is updating
   */
  private Boolean strictReads = false;

  /**
   * Worker batch size
   */
  private Integer workerBatchSize = 100;

  /**
   * Max retry attempts
   */
  private Integer maxRetries = 3;

  /**
   * Backoff multiplier for retries
   */
  private Double backoffMultiplier = 2.0;

  /**
   * Initial backoff delay in milliseconds
   */
  private Long initialBackoffMs = 100L;

  /**
   * Max backoff delay in milliseconds
   */
  private Long maxBackoffMs = 30000L;

  /**
   * Worker concurrency (number of parallel workers)
   */
  private Integer workerConcurrency = 2;

  /**
   * Priority weights for command queues
   */
  private PriorityWeights priorityWeights = new PriorityWeights();

  @Data
  public static class PriorityWeights {
    private int critical = 40;
    private int high = 30;
    private int normal = 20;
    private int bulk = 10;
  }
}
