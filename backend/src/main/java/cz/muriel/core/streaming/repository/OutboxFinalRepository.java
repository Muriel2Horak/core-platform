package cz.muriel.core.streaming.repository;

import cz.muriel.core.streaming.entity.OutboxFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for OutboxFinal
 */
@Repository
public interface OutboxFinalRepository extends JpaRepository<OutboxFinal, UUID> {

    /**
     * Fetch unsent messages for publishing
     * Using native query with FOR UPDATE SKIP LOCKED for parallel dispatchers
     */
    @Query(value = """
        SELECT * FROM outbox_final 
        WHERE sent_at IS NULL 
        ORDER BY created_at 
        LIMIT :batchSize 
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxFinal> fetchUnsentMessages(@Param("batchSize") int batchSize);

    /**
     * Count unsent messages
     */
    @Query("SELECT COUNT(o) FROM OutboxFinal o WHERE o.sentAt IS NULL")
    long countUnsent();

    /**
     * Find by correlation_id
     */
    List<OutboxFinal> findByCorrelationId(UUID correlationId);
}
