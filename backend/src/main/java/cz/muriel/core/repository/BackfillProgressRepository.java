package cz.muriel.core.repository;

import cz.muriel.core.entity.BackfillProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackfillProgressRepository extends JpaRepository<BackfillProgress, UUID> {

  Optional<BackfillProgress> findByTenantIdAndStatus(String tenantId, String status);

  Optional<BackfillProgress> findFirstByTenantIdOrderByCreatedAtDesc(String tenantId);

  Optional<BackfillProgress> findByTenantId(String tenantId);

  boolean existsByTenantId(String tenantId);

  // 🎯 POLLING: Najdi další záznamy ke zpracování (PENDING nebo IN_PROGRESS s
  // chybou)
  @Query("SELECT bp FROM BackfillProgress bp WHERE bp.status = 'PENDING' OR bp.status = 'IN_PROGRESS' ORDER BY bp.createdAt ASC")
  List<BackfillProgress> findNextToProcess(Pageable pageable);

  // 📊 MONITORING: Počet záznamů podle statusu
  long countByStatus(String status);
}
