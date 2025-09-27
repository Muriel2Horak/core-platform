package cz.muriel.core.repository;

import cz.muriel.core.entity.KeycloakEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeycloakEventLogRepository extends JpaRepository<KeycloakEventLog, String> {

  boolean existsByEventHash(String eventHash);
}
