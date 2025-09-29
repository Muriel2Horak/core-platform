package cz.muriel.core.service;

import cz.muriel.core.entity.Tenant;
import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.repository.UserDirectoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
   * Search users with pagination and filtering (tenant-scoped)
   */
  public Page<UserDirectoryEntity> searchWithPagination(String query, String source,
      Pageable pageable) {
    log.debug("Searching users with query='{}', source='{}', pageable={}", query, source, pageable);

    if (query == null || query.trim().isEmpty()) {
      if (source != null && !source.trim().isEmpty()) {
        return searchBySource(source, pageable);
      }
      return userDirectoryRepository.findAll(pageable);
    }

    // Search with source filter if provided
    if (source != null && !source.trim().isEmpty()) {
      return searchByQueryAndSource(query.trim(), source, pageable);
    }

    return userDirectoryRepository.searchWithPagination(query.trim(), pageable);
  }

  /**
   * Search across all tenants (pouze pro core-admin)
   */
  public Page<UserDirectoryEntity> searchAllTenantsWithPagination(String query, String source,
      Pageable pageable) {
    log.debug("Cross-tenant search: query='{}', source='{}', pageable={}", query, source, pageable);

    // Cross-tenant search vyžaduje speciální implementaci na úrovni repository
    // Pro současnost používáme fallback - search v aktuálním tenantu
    log.warn("Cross-tenant search not fully implemented - falling back to current tenant search");

    try {
      // Fallback na standardní tenant-scoped search
      return searchWithPagination(query, source, pageable);

    } catch (Exception e) {
      log.error("Cross-tenant search failed: {}", e.getMessage(), e);
      // Fallback - return empty page
      return Page.empty(pageable);
    }
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
   * Save user entity
   */
  public UserDirectoryEntity save(UserDirectoryEntity user) {
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

  /**
   * 📊 COUNT USERS BY TENANT: Spočítá uživatele v tenantu
   */
  public long countUsersByTenant(UUID tenantId) {
    try {
      return userDirectoryRepository.countByTenantId(tenantId);
    } catch (Exception e) {
      log.warn("Failed to count users for tenant {}: {}", tenantId, e.getMessage());
      return 0;
    }
  }

  /**
   * 📊 COUNT USERS BY TENANT KEY: Spočítá uživatele v tenantu podle klíče
   */
  public long countUsersByTenantKey(String tenantKey) {
    try {
      // Get tenant by key first
      Optional<Tenant> tenant = tenantService.findTenantByKey(tenantKey);
      if (tenant.isEmpty()) {
        log.warn("Tenant not found for key: {}", tenantKey);
        return 0;
      }

      return userDirectoryRepository.countByTenantId(tenant.get().getId());

    } catch (Exception e) {
      log.warn("Failed to count users for tenant key {}: {}", tenantKey, e.getMessage());
      return 0;
    }
  }

  // Private helper methods

  private Page<UserDirectoryEntity> searchBySource(String source, Pageable pageable) {
    boolean isFederated = "AD".equalsIgnoreCase(source);
    return userDirectoryRepository.findByIsFederated(isFederated, pageable);
  }

  private Page<UserDirectoryEntity> searchByQueryAndSource(String query, String source,
      Pageable pageable) {
    boolean isFederated = "AD".equalsIgnoreCase(source);
    return userDirectoryRepository.searchByQueryAndSource(query, isFederated, pageable);
  }
}
