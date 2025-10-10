package cz.muriel.core.streaming.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox Final Entity
 * Transactional outbox for publishing events to Kafka
 */
@Data
@Entity
@Table(name = "outbox_final", indexes = {
    @Index(name = "idx_outbox_final_sent_at", columnList = "sent_at"),
    @Index(name = "idx_outbox_final_entity_entity_id", columnList = "entity, entity_id"),
    @Index(name = "idx_outbox_final_correlation_id", columnList = "correlation_id"),
    @Index(name = "idx_outbox_final_created_at", columnList = "created_at")
})
public class OutboxFinal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String entity;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 20)
    private String operation; // CREATED, UPDATED, DELETED

    @Column(name = "diff_json", columnDefinition = "jsonb")
    private String diffJson;

    @Column(name = "snapshot_json", columnDefinition = "jsonb")
    private String snapshotJson;

    @Column(name = "headers_json", columnDefinition = "jsonb")
    private String headersJson;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
