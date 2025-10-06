package cz.muriel.core.repository.keycloak;

import cz.muriel.core.entity.ChangeEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 🔄 Repository pro CDC Change Events z Keycloak databáze
 */
@Repository
public interface ChangeEventRepository extends JpaRepository<ChangeEventEntity, Long> {

  /**
   * Najde všechny nezpracované eventy seřazené podle času vytvoření
   */
  List<ChangeEventEntity> findByProcessedFalseOrderByCreatedAtAsc();

  /**
   * Najde nezpracované eventy s limitem (pro batch processing)
   */
  @Query("SELECT c FROM ChangeEventEntity c WHERE c.processed = false ORDER BY c.createdAt ASC")
  List<ChangeEventEntity> findUnprocessedEventsWithLimit(@Param("limit") int limit);

  /**
   * Spočítá nezpracované eventy
   */
  long countByProcessedFalse();

  /**
   * Najde eventy starší než určité datum (pro cleanup)
   */
  List<ChangeEventEntity> findByProcessedTrueAndProcessedAtBefore(LocalDateTime dateTime);

  /**
   * Smaže zpracované eventy starší než určité datum
   */
  void deleteByProcessedTrueAndProcessedAtBefore(LocalDateTime dateTime);

  /**
   * Najde eventy podle realm_id
   */
  List<ChangeEventEntity> findByRealmIdAndProcessedFalseOrderByCreatedAtAsc(String realmId);

  /**
   * Najde eventy podle typu
   */
  List<ChangeEventEntity> findByEventTypeAndProcessedFalseOrderByCreatedAtAsc(String eventType);
}
