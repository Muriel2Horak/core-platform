package cz.muriel.core.repository;

import cz.muriel.core.entity.UserChangeEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 🔄 Repository pro User Change Events
 * 
 * Bulk operace pro efektivní zpracování změnových eventů z Postgres triggers.
 * 
 * 🔥 HOT RELOAD TEST - This comment tests DevTools auto-restart
 */
@Repository
public interface UserChangeEventRepository extends JpaRepository<UserChangeEventEntity, Long> {

        /**
         * Najde nezpracované eventy s SKIP LOCKED pro paralelní instancování
         */
        @Query(value = """
                        SELECT * FROM user_change_events
                        WHERE processed = false
                        ORDER BY created_at ASC
                        LIMIT :batchSize
                        FOR UPDATE SKIP LOCKED
                        """, nativeQuery = true)
        List<UserChangeEventEntity> findUnprocessedEventsWithLock(
                        @Param("batchSize") int batchSize);

        /**
         * Najde nezpracované eventy omezené na určitý počet (pro fallback processing)
         */
        @Query("SELECT e FROM UserChangeEventEntity e " + "WHERE e.processed = false "
                        + "ORDER BY e.createdAt ASC")
        List<UserChangeEventEntity> findUnprocessedEventsLimited(@Param("batchSize") int batchSize,
                        Pageable pageable);

        /**
         * Jednodušší varianta pro fallback processing
         */
        @Query(value = """
                        SELECT * FROM user_change_events
                        WHERE processed = false
                        ORDER BY created_at ASC
                        LIMIT :limit
                        """, nativeQuery = true)
        List<UserChangeEventEntity> findUnprocessedEventsLimited(@Param("limit") int limit);

        /**
         * Najde nezpracované eventy starší než zadaný čas (pro fallback job)
         */
        @Query("SELECT e FROM UserChangeEventEntity e " + "WHERE e.processed = false "
                        + "AND e.createdAt <= :olderThan " + "ORDER BY e.createdAt ASC")
        List<UserChangeEventEntity> findUnprocessedEventsOlderThan(
                        @Param("olderThan") LocalDateTime olderThan);

        /**
         * Najde nezpracované eventy pro specifické user IDs (pro notifikace)
         */
        @Query("SELECT e FROM UserChangeEventEntity e " + "WHERE e.processed = false "
                        + "AND e.userId IN :userIds " + "ORDER BY e.createdAt ASC")
        List<UserChangeEventEntity> findUnprocessedEventsByUserIds(
                        @Param("userIds") Set<UUID> userIds);

        /**
         * OPTIMIZED: Bulk označení eventů jako zpracované podle user IDs a timestamp
         */
        @Modifying @Query("UPDATE UserChangeEventEntity e "
                        + "SET e.processed = true, e.processedAt = :processedAt "
                        + "WHERE e.userId IN :userIds AND e.processed = false "
                        + "AND e.createdAt <= :beforeTimestamp")
        int markEventsAsProcessedByUserIds(@Param("userIds") Set<UUID> userIds,
                        @Param("beforeTimestamp") LocalDateTime beforeTimestamp,
                        @Param("processedAt") LocalDateTime processedAt);

        /**
         * OPTIMIZED: Overloaded metoda s automatickým nastavením processedAt na NOW()
         */
        @Modifying @Query("UPDATE UserChangeEventEntity e "
                        + "SET e.processed = true, e.processedAt = CURRENT_TIMESTAMP "
                        + "WHERE e.userId IN :userIds AND e.processed = false "
                        + "AND e.createdAt <= :beforeTimestamp")
        int markEventsAsProcessedByUserIds(@Param("userIds") Set<UUID> userIds,
                        @Param("beforeTimestamp") LocalDateTime beforeTimestamp);

        /**
         * Bulk označení eventů jako zpracované
         */
        @Modifying @Query("UPDATE UserChangeEventEntity e "
                        + "SET e.processed = true, e.processedAt = :processedAt "
                        + "WHERE e.id IN :eventIds")
        int markEventsAsProcessed(@Param("eventIds") List<Long> eventIds,
                        @Param("processedAt") LocalDateTime processedAt);

        /**
         * Počet nezpracovaných eventů (pro monitoring)
         */
        @Query("SELECT COUNT(e) FROM UserChangeEventEntity e WHERE e.processed = false")
        long countUnprocessedEvents();

        /**
         * OPTIMIZED: Počet nezpracovaných eventů podle realm
         */
        @Query("SELECT e.realmId, COUNT(e) FROM UserChangeEventEntity e "
                        + "WHERE e.processed = false " + "GROUP BY e.realmId")
        List<Object[]> countUnprocessedEventsByRealm();

        /**
         * OPTIMIZED: Statistiky pro monitoring dashboard
         */
        @Query(value = """
                        SELECT
                            COUNT(*) as totalEvents,
                            COUNT(*) FILTER (WHERE processed = false) as unprocessedEvents,
                            COUNT(DISTINCT user_id) FILTER (WHERE processed = false) as uniqueUnprocessedUsers,
                            COUNT(DISTINCT realm_id) as uniqueRealms
                        FROM user_change_events
                        """, nativeQuery = true)
        Object[] getEventStatistics();

        /**
         * OPTIMIZED: Cleanup starých zpracovaných eventů v batch
         */
        @Modifying @Query(value = """
                        DELETE FROM user_change_events
                        WHERE id IN (
                            SELECT id FROM user_change_events
                            WHERE processed = true
                            AND processed_at < :olderThan
                            ORDER BY processed_at ASC
                            LIMIT :batchSize
                        )
                        """, nativeQuery = true)
        int deleteProcessedEventsOlderThanBatch(@Param("olderThan") LocalDateTime olderThan,
                        @Param("batchSize") int batchSize);

        /**
         * Cleanup starých zpracovaných eventů
         */
        @Modifying @Query("DELETE FROM UserChangeEventEntity e " + "WHERE e.processed = true "
                        + "AND e.processedAt < :olderThan")
        int deleteProcessedEventsOlderThan(@Param("olderThan") LocalDateTime olderThan);

        /**
         * Najde eventy podle realm (tenant) pro debugging
         */
        @Query("SELECT e FROM UserChangeEventEntity e " + "WHERE e.realmId = :realmId "
                        + "AND e.processed = false " + "ORDER BY e.createdAt DESC")
        List<UserChangeEventEntity> findUnprocessedEventsByRealm(@Param("realmId") String realmId);

        /**
         * OPTIMIZED: Najde nejstarší nezpracované eventy pro monitoring
         */
        @Query("SELECT e FROM UserChangeEventEntity e " + "WHERE e.processed = false "
                        + "ORDER BY e.createdAt ASC")
        List<UserChangeEventEntity> findOldestUnprocessedEvents(Pageable pageable);

        /**
         * OPTIMIZED: Průměrná doba zpracování eventů
         */
        @Query(value = """
                        SELECT AVG(EXTRACT(EPOCH FROM processed_at) - EXTRACT(EPOCH FROM created_at))
                        FROM user_change_events
                        WHERE processed = true
                        AND processed_at IS NOT NULL
                        AND processed_at >= :since
                        """, nativeQuery = true)
        Double getAverageProcessingTimeSeconds(@Param("since") LocalDateTime since);

        /**
         * OPTIMIZED: Počet eventů zpracovaných za poslední období
         */
        @Query("SELECT COUNT(e) FROM UserChangeEventEntity e " + "WHERE e.processed = true "
                        + "AND e.processedAt >= :since")
        long countEventsProcessedSince(@Param("since") LocalDateTime since);
}
