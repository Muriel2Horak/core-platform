package cz.muriel.core.metamodel.schema;

import lombok.Data;

/**
 * Global streaming configuration in metamodel
 */
@Data
public class StreamingGlobalConfig {
  /**
   * Global enable/disable for streaming
   */
  private boolean enabled = false;

  /**
   * Default partitions for entity topics
   */
  private int defaultPartitions = 3;

  /**
   * Default replication factor
   */
  private int defaultReplicationFactor = 1;

  /**
   * Default retention hours for event topics
   */
  private int defaultRetentionHours = 168; // 7 days

  /**
   * Default retention minutes for inflight topics
   */
  private int defaultInflightRetentionMinutes = 30;

  /**
   * Default command retention hours
   */
  private int defaultCommandRetentionHours = 24;

  /**
   * Default event payload mode
   */
  private String defaultEventPayloadMode = "diff";

  /**
   * Default snapshot inclusion
   */
  private boolean defaultIncludeSnapshot = false;

  /**
   * Default strict reads
   */
  private boolean defaultStrictReads = false;

  /**
   * Default worker batch size
   */
  private int defaultWorkerBatchSize = 100;

  /**
   * Default max retries
   */
  private int defaultMaxRetries = 3;

  /**
   * Default backoff multiplier
   */
  private double defaultBackoffMultiplier = 2.0;

  /**
   * Default initial backoff ms
   */
  private long defaultInitialBackoffMs = 100L;

  /**
   * Default max backoff ms
   */
  private long defaultMaxBackoffMs = 30000L;

  /**
   * Default worker concurrency
   */
  private int defaultWorkerConcurrency = 2;

  /**
   * DLQ topic name
   */
  private String dlqTopic = "dlq.events";

  /**
   * Outbox DLQ topic name
   */
  private String outboxDlqTopic = "dlq.outbox";

  /**
   * Dispatcher poll interval ms
   */
  private long dispatcherPollIntervalMs = 100L;

  /**
   * Worker poll interval ms
   */
  private long workerPollIntervalMs = 100L;

  /**
   * Work state TTL in minutes
   */
  private int workStateTtlMinutes = 30;

  /**
   * Lock expiry check interval in minutes
   */
  private int lockExpiryCheckIntervalMinutes = 5;
}
