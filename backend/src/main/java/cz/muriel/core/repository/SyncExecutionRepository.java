package cz.muriel.core.repository;

import cz.muriel.core.entity.SyncExecution;
import cz.muriel.core.entity.SyncExecution.SyncStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SyncExecutionRepository extends JpaRepository<SyncExecution, String> {

    Page<SyncExecution> findByStatus(SyncStatus status, Pageable pageable);

    Page<SyncExecution> findByTenantId(UUID tenantId, Pageable pageable);

    Page<SyncExecution> findByStatusAndTenantId(SyncStatus status, UUID tenantId,
            Pageable pageable);

    @Query("SELECT s FROM SyncExecution s WHERE s.status = :status AND s.startTime >= :since ORDER BY s.startTime DESC")
    List<SyncExecution> findRecentByStatus(@Param("status") SyncStatus status,
            @Param("since") LocalDateTime since);

    @Query("SELECT s FROM SyncExecution s WHERE s.tenantId = :tenantId ORDER BY s.startTime DESC")
    Page<SyncExecution> findByTenantIdOrderByStartTimeDesc(@Param("tenantId") UUID tenantId,
            Pageable pageable);

    List<SyncExecution> findByStatusIn(List<SyncStatus> statuses);
}
