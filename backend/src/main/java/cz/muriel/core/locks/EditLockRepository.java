package cz.muriel.core.locks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EditLockRepository extends JpaRepository<EditLock, Long> {

  Optional<EditLock> findByTenantIdAndEntityTypeAndEntityId(UUID tenantId, String entityType,
      String entityId);

  @Modifying @Query("DELETE FROM EditLock e WHERE e.expiresAt < :now")
  int deleteExpired(Instant now);

  @Modifying @Query("DELETE FROM EditLock e WHERE e.tenantId = :tenantId AND e.entityType = :entityType AND e.entityId = :entityId")
  void deleteByTenantAndTypeAndId(UUID tenantId, String entityType, String entityId);
}
