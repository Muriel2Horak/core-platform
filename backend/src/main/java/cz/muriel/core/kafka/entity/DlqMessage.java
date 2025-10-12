package cz.muriel.core.kafka.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 💀 DLQ Message Entity
 * 
 * Stores failed Kafka messages for manual inspection and replay.
 * 
 * Created by DltManager when messages fail after all retry attempts.
 */
@Entity
@Table(name = "dlq_messages", indexes = {
    @Index(name = "idx_dlq_topic_status", columnList = "original_topic, status"),
    @Index(name = "idx_dlq_created_at", columnList = "created_at"),
    @Index(name = "idx_dlq_consumer_group", columnList = "consumer_group"),
    @Index(name = "idx_dlq_exception_type", columnList = "exception_type")
})
@Data
@NoArgsConstructor
public class DlqMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "original_topic", nullable = false, length = 255)
  private String originalTopic;

  @Column(name = "partition")
  private Integer partition;

  @Column(name = "offset_value")
  private Long offsetValue;

  @Column(name = "message_key", length = 255)
  private String messageKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "stack_trace", columnDefinition = "TEXT")
  private String stackTrace;

  @Column(name = "retry_count", nullable = false)
  private Integer retryCount = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private DlqStatus status = DlqStatus.PENDING;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "replayed_at")
  private Instant replayedAt;

  @Column(name = "consumer_group", length = 255)
  private String consumerGroup;

  @Column(name = "exception_type", length = 255)
  private String exceptionType;

  public enum DlqStatus {
    PENDING,
    REPLAYED,
    DISCARDED
  }
}
