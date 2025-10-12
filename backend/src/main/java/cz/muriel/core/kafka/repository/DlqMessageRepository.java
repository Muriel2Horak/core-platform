package cz.muriel.core.kafka.repository;

import cz.muriel.core.kafka.entity.DlqMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for DLQ messages
 */
@Repository
public interface DlqMessageRepository extends JpaRepository<DlqMessage, UUID> {

  /**
   * Find pending DLQ messages by topic
   */
  Page<DlqMessage> findByOriginalTopicAndStatus(String originalTopic, DlqMessage.DlqStatus status,
      Pageable pageable);

  /**
   * Find all pending DLQ messages
   */
  Page<DlqMessage> findByStatus(DlqMessage.DlqStatus status, Pageable pageable);

  /**
   * Count pending messages by topic
   */
  long countByOriginalTopicAndStatus(String originalTopic, DlqMessage.DlqStatus status);

  /**
   * Count all pending messages
   */
  long countByStatus(DlqMessage.DlqStatus status);

  /**
   * Find by exception type
   */
  Page<DlqMessage> findByExceptionTypeAndStatus(String exceptionType, DlqMessage.DlqStatus status,
      Pageable pageable);

  /**
   * Find by consumer group
   */
  Page<DlqMessage> findByConsumerGroupAndStatus(String consumerGroup, DlqMessage.DlqStatus status,
      Pageable pageable);

  /**
   * Get pending messages for replay (oldest first)
   */
  @Query("SELECT d FROM DlqMessage d WHERE d.status = 'PENDING' ORDER BY d.createdAt ASC")
  List<DlqMessage> findPendingMessagesForReplay(Pageable pageable);

  /**
   * Count pending messages by consumer group
   */
  @Query("SELECT d.consumerGroup, COUNT(d) FROM DlqMessage d WHERE d.status = 'PENDING' GROUP BY d.consumerGroup")
  List<Object[]> countPendingByConsumerGroup();
}
