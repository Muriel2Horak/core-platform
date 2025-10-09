package cz.muriel.core.service;

import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.repository.UserDirectoryRepository;
import cz.muriel.core.tenant.TenantContext;
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
   * Search users with fulltext search (DEPRECATED - use searchWithPagination)
   */
  @Deprecated
  public List<UserDirectoryEntity> search(String query) {
    if (query == null || query.trim().isEmpty()) {
      // Return first 100 users if no query with proper sorting
      return userDirectoryRepository.findAll(PageRequest.of(0, 100, Sort.by("username")))
          .getContent();
    }

    return userDirectoryRepository.search(query.trim(),
        PageRequest.of(0, 100, Sort.by("username")));
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
   * Search across all tenants (pouze pro core-admin) Používá speciální query bez
   * tenant filtru
   */
  public Page<UserDirectoryEntity> searchAllTenantsWithPagination(String query, String source,
      Pageable pageable) {
    log.debug("Cross-tenant search: query='{}', source='{}', pageable={}", query, source, pageable);

    try {
      // Pro cross-tenant search použijeme repository metody bez tenant filtru
      if (query == null || query.trim().isEmpty()) {
        if (source != null && !source.trim().isEmpty()) {
          boolean isFederated = "AD".equalsIgnoreCase(source);
          return userDirectoryRepository.findByIsFederated(isFederated, pageable);
        }
        return userDirectoryRepository.findAll(pageable);
      }

      // Search with source filter if provided
      if (source != null && !source.trim().isEmpty()) {
        boolean isFederated = "AD".equalsIgnoreCase(source);
        return userDirectoryRepository.searchByQueryAndSource(query.trim(), isFederated, pageable);
      }

      return userDirectoryRepository.searchWithPagination(query.trim(), pageable);

    } catch (Exception e) {
      log.error("Cross-tenant search failed: {}", e.getMessage(), e);
      // Return empty page instead of throwing exception
      return Page.empty(pageable);
    }
  }

  /**
   * 🆕 CREATE/UPDATE: Vytvoří nebo aktualizuje uživatele v current tenant
   * kontextu
   */
  public UserDirectoryEntity createOrUpdate(UserDirectoryEntity user) {
    String currentTenantKey = TenantContext.getTenantKey();

    if (currentTenantKey == null) {
      throw new IllegalStateException("No tenant context available");
    }

    // 🎯 AUTO-SET: Automatically set tenant ID from context
    UUID tenantId = tenantService.getTenantIdFromKey(currentTenantKey);
    user.setTenantId(tenantId);

    log.debug("Creating/updating user {} in tenant {}", user.getUsername(), currentTenantKey);

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
   * 📊 STATS: Počet uživatelů v tenantu podle tenant key
   */
  public long countUsersByTenantKey(String tenantKey) {
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);
    return userDirectoryRepository.countByTenantId(tenantId);
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
