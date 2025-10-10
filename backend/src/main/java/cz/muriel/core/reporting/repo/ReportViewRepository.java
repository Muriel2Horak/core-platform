package cz.muriel.core.reporting.repo;

import cz.muriel.core.reporting.model.ReportView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ReportView entity.
 */
@Repository
public interface ReportViewRepository extends JpaRepository<ReportView, UUID> {

  /**
   * Find all views for a tenant and entity.
   */
  List<ReportView> findByTenantIdAndEntity(UUID tenantId, String entity);

  /**
   * Find user's accessible views (private, group, tenant, global).
   */
  @Query("""
      SELECT v FROM ReportView v
      WHERE v.tenantId = :tenantId
      AND v.entity = :entity
      AND (
          v.scope = 'GLOBAL'
          OR (v.scope = 'TENANT' AND v.tenantId = :tenantId)
          OR (v.scope = 'GROUP' AND v.groupId IN :groupIds)
          OR (v.scope = 'PRIVATE' AND v.ownerId = :userId)
      )
      ORDER BY v.createdAt DESC
      """)
  List<ReportView> findAccessibleViews(@Param("tenantId") UUID tenantId,
      @Param("entity") String entity, @Param("userId") UUID userId,
      @Param("groupIds") List<UUID> groupIds);

  /**
   * Find default view for entity.
   */
  Optional<ReportView> findByTenantIdAndEntityAndIsDefaultTrue(UUID tenantId, String entity);

  /**
   * Find user's private views.
   */
  List<ReportView> findByOwnerIdAndEntity(UUID ownerId, String entity);

  /**
   * Find group views.
   */
  List<ReportView> findByGroupIdAndEntity(UUID groupId, String entity);
}
