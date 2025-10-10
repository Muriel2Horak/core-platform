package cz.muriel.core.streaming.repository;

import cz.muriel.core.streaming.entity.CommandQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repository for CommandQueue
 */
@Repository
public interface CommandQueueRepository extends JpaRepository<CommandQueue, UUID> {

  /**
   * Fetch pending commands for processing with pessimistic lock and SKIP LOCKED
   * This ensures no two workers process the same command
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query(value = """
      SELECT * FROM command_queue
      WHERE status = 'pending'
        AND available_at <= :now
      ORDER BY
        CASE priority
          WHEN 'critical' THEN 1
          WHEN 'high' THEN 2
          WHEN 'normal' THEN 3
          WHEN 'bulk' THEN 4
          ELSE 5
        END,
        available_at
      LIMIT :batchSize
      FOR UPDATE SKIP LOCKED
      """, nativeQuery = true)
  List<CommandQueue> fetchPendingCommandsForProcessing(@Param("now") Instant now,
      @Param("batchSize") int batchSize);

  /**
   * Count pending commands by priority
   */
  @Query("SELECT COUNT(c) FROM CommandQueue c WHERE c.status = 'pending' AND c.priority = :priority")
  long countPendingByPriority(@Param("priority") String priority);

  /**
   * Count pending commands
   */
  long countByStatus(String status);

  /**
   * Find commands by operation_id
   */
  List<CommandQueue> findByOperationId(UUID operationId);

  /**
   * Find commands by correlation_id
   */
  List<CommandQueue> findByCorrelationId(UUID correlationId);

  /**
   * Find DLQ messages with pagination
   */
  org.springframework.data.domain.Page<CommandQueue> findByStatus(String status,
      org.springframework.data.domain.Pageable pageable);

  /**
   * Find DLQ messages by entity
   */
  org.springframework.data.domain.Page<CommandQueue> findByStatusAndEntity(String status,
      String entity, org.springframework.data.domain.Pageable pageable);

  /**
   * Find DLQ messages by entity and error type
   */
  org.springframework.data.domain.Page<CommandQueue> findByStatusAndEntityAndErrorType(
      String status, String entity, String errorType,
      org.springframework.data.domain.Pageable pageable);
}
