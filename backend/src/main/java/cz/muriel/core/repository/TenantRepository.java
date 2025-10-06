package cz.muriel.core.repository;

import cz.muriel.core.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

  Optional<Tenant> findByKey(String key);

  Optional<Tenant> findByKeycloakRealmId(String keycloakRealmId); // 🆕 CDC mapping

  boolean existsByKey(String key);

  // Nepotrebujeme žádné další metody - používáme pouze findByKey, findById a
  // findAll
}
