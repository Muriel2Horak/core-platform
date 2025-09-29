package cz.muriel.core.repository;

import cz.muriel.core.entity.UserDirectoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDirectoryRepository extends JpaRepository<UserDirectoryEntity, UUID> {

  Optional<UserDirectoryEntity> findByUsernameIgnoreCase(String username);

  Optional<UserDirectoryEntity> findByKeycloakUserId(String keycloakUserId);

  Optional<UserDirectoryEntity> findByEmail(String email);

  // 🔍 MISSING METHOD: Case-insensitive email search
  Optional<UserDirectoryEntity> findByEmailIgnoreCase(String email);

  @Query("SELECT u FROM UserDirectoryEntity u WHERE "
      + "(LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR "
      + " LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) OR "
      + " LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')) OR "
      + " LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :q, '%')))")
  List<UserDirectoryEntity> search(@Param("q") String query, Pageable pageable);

  @Query("SELECT u FROM UserDirectoryEntity u WHERE "
      + "(LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR "
      + " LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) OR "
      + " LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')) OR "
      + " LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :q, '%')))")
  Page<UserDirectoryEntity> searchWithPagination(@Param("q") String query, Pageable pageable);

  /**
   * 🆕 Search by directory source (AD/Local)
   */
  Page<UserDirectoryEntity> findByIsFederated(boolean isFederated, Pageable pageable);

  /**
   * 🆕 Search by query and directory source
   */
  @Query("SELECT u FROM UserDirectoryEntity u WHERE " + "u.isFederated = :isFederated AND ("
      + " LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR "
      + " LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) OR "
      + " LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')) OR "
      + " LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :q, '%')))")
  Page<UserDirectoryEntity> searchByQueryAndSource(@Param("q") String query,
      @Param("isFederated") boolean isFederated, Pageable pageable);

  List<UserDirectoryEntity> findByManagerId(UUID managerId);

  List<UserDirectoryEntity> findByStatus(String status);

  // 🔍 EXTENDED SEARCH: Rozšířené vyhledávání pro tenant discovery
  List<UserDirectoryEntity> findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
      String firstName, String lastName);

  List<UserDirectoryEntity> findByDisplayNameContainingIgnoreCase(String displayName);

  List<UserDirectoryEntity> findByFirstNameContainingIgnoreCase(String firstName);

  List<UserDirectoryEntity> findByLastNameContainingIgnoreCase(String lastName);

  // 📊 TENANT STATISTICS: Počítání uživatelů
  long countByTenantId(UUID tenantId);
}
