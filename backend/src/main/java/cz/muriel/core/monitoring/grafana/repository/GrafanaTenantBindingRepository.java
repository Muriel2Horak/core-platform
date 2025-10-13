package cz.muriel.core.monitoring.grafana.repository;

import cz.muriel.core.monitoring.grafana.entity.GrafanaTenantBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 🗄️ GRAFANA TENANT BINDING REPOSITORY
 */
@Repository
public interface GrafanaTenantBindingRepository extends JpaRepository<GrafanaTenantBinding, Long> {

  Optional<GrafanaTenantBinding> findByTenantId(String tenantId);

  Optional<GrafanaTenantBinding> findByGrafanaOrgId(Long grafanaOrgId);

  void deleteByTenantId(String tenantId);

  boolean existsByTenantId(String tenantId);
}
