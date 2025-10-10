package cz.muriel.core.streaming.repository;

import cz.muriel.core.streaming.entity.WorkState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WorkState
 */
@Repository
public interface WorkStateRepository extends JpaRepository<WorkState, WorkState.WorkStateId> {

    /**
     * Find work state by entity and entity_id
     */
    Optional<WorkState> findByEntityAndEntityId(String entity, UUID entityId);

    /**
     * Find expired locks for cleanup
     */
    @Query("SELECT w FROM WorkState w WHERE w.ttl IS NOT NULL AND w.ttl < :now AND w.status = 'updating'")
    List<WorkState> findExpiredLocks(@Param("now") Instant now);

    /**
     * Release expired locks
     */
    @Modifying
    @Query("UPDATE WorkState w SET w.status = 'idle', w.lockedBy = NULL, w.ttl = NULL WHERE w.ttl < :now AND w.status = 'updating'")
    int releaseExpiredLocks(@Param("now") Instant now);

    /**
     * Count by status
     */
    long countByStatus(String status);
}
