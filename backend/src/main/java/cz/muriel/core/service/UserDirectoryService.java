package cz.muriel.core.service;

import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.repository.UserDirectoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j @Transactional
public class UserDirectoryService {

  private final UserDirectoryRepository userDirectoryRepository;
  private final TenantService tenantService;

  /**
   * Find user by username (case insensitive)
   */
  public Optional<UserDirectoryEntity> findByUsername(String username) {
    return userDirectoryRepository.findByUsernameIgnoreCase(username);
  }

  /**
   * Find user by Keycloak user ID
   */
  public Optional<UserDirectoryEntity> findByKeycloakUserId(String keycloakUserId) {
    return userDirectoryRepository.findByKeycloakUserId(keycloakUserId);
  }

  /**
   * Find user by email
   */
  public Optional<UserDirectoryEntity> findByEmail(String email) {
    return userDirectoryRepository.findByEmail(email);
  }

  /**
   * Search users with fulltext search
   */
  public List<UserDirectoryEntity> search(String query) {
    if (query == null || query.trim().isEmpty()) {
      // Return first 50 users if no query
      return userDirectoryRepository.findAll(PageRequest.of(0, 50, Sort.by("username")))
          .getContent();
    }

    return userDirectoryRepository.search(query.trim(), PageRequest.of(0, 50, Sort.by("username")));
  }

  /**
   * Create or update user in directory
   */
  public UserDirectoryEntity createOrUpdate(UserDirectoryEntity user) {
    UUID tenantId = tenantService.getCurrentTenantIdOrThrow();
    user.setTenantId(tenantId);

    log.debug("Creating/updating user in directory: {} for tenant: {}", user.getUsername(),
        tenantId);
    return userDirectoryRepository.save(user);
  }

  /**
   * Find users by manager
   */
  public List<UserDirectoryEntity> findByManager(UUID managerId) {
    return userDirectoryRepository.findByManagerId(managerId);
  }

  /**
   * Find users by status
   */
  public List<UserDirectoryEntity> findByStatus(String status) {
    return userDirectoryRepository.findByStatus(status);
  }

  /**
   * Get user by ID
   */
  public Optional<UserDirectoryEntity> findById(UUID id) {
    return userDirectoryRepository.findById(id);
  }

  /**
   * Delete user from directory
   */
  public void deleteById(UUID id) {
    userDirectoryRepository.deleteById(id);
  }
}
