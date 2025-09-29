package cz.muriel.core.service;

import cz.muriel.core.entity.Tenant;
import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.repository.TenantRepository;
import cz.muriel.core.repository.UserDirectoryRepository;
import cz.muriel.core.auth.KeycloakAdminService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class TenantDiscoveryService {

  private final TenantRepository tenantRepository;
  private final UserDirectoryRepository userDirectoryRepository;
  private final KeycloakAdminService keycloakAdminService;

  /**
   * 🔍 MAIN DISCOVERY: Najde tenant(y) pro uživatele podle emailu/username/jména
   */
  public List<TenantInfo> findTenantsForUser(String identifier) {
    log.debug("🔍 Finding tenants for identifier: {}", identifier);

    List<TenantInfo> foundTenants = new ArrayList<>();

    // 1. Hledání v Users Directory (rychlé, lokální)
    List<TenantInfo> localTenants = findTenantsInUserDirectory(identifier);
    foundTenants.addAll(localTenants);

    // 2. Email domain heuristics (pokud je to email)
    if (identifier.contains("@") && foundTenants.isEmpty()) {
      log.debug("🔍 Trying email domain heuristics...");
      List<TenantInfo> domainTenants = guessTenantByEmailDomain(identifier);
      foundTenants.addAll(domainTenants);
    }

    // 3. Hledání v Keycloak (pomalejší, ale aktuální)
    if (foundTenants.isEmpty()) {
      log.debug("🔍 No tenants found locally, searching in Keycloak...");
      List<TenantInfo> keycloakTenants = findTenantsInKeycloak(identifier);
      foundTenants.addAll(keycloakTenants);
    }

    // 4. Remove duplicates and sort by relevance
    List<TenantInfo> uniqueTenants = foundTenants.stream().distinct()
        .sorted((a, b) -> a.getTenantName().compareToIgnoreCase(b.getTenantName())).toList();

    log.info("🔍 Found {} unique tenant(s) for {}: {}", uniqueTenants.size(), identifier,
        uniqueTenants.stream().map(TenantInfo::getTenantKey).toList());

    return uniqueTenants;
  }

  /**
   * 📂 FAST LOCAL SEARCH: Rozšířené hledání v lokální Users Directory
   */
  private List<TenantInfo> findTenantsInUserDirectory(String identifier) {
    try {
      List<UserDirectoryEntity> users = new ArrayList<>();

      String lowerIdentifier = identifier.toLowerCase().trim();

      if (lowerIdentifier.contains("@")) {
        // Email search - FIXED: Correct Optional handling
        userDirectoryRepository.findByEmailIgnoreCase(lowerIdentifier).ifPresent(users::add);
      } else if (lowerIdentifier.contains(" ")) {
        // Full name search (firstName + lastName)
        String[] nameParts = lowerIdentifier.split("\\s+");
        if (nameParts.length >= 2) {
          String firstName = nameParts[0];
          String lastName = nameParts[nameParts.length - 1];

          // Search by first name and last name combination
          users.addAll(userDirectoryRepository
              .findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(firstName,
                  lastName));
        }

        // Also try as display name
        users
            .addAll(userDirectoryRepository.findByDisplayNameContainingIgnoreCase(lowerIdentifier));
      } else {
        // Username search - Optional handling
        userDirectoryRepository.findByUsernameIgnoreCase(lowerIdentifier).ifPresent(users::add);

        // Also try partial matches in first/last name
        users.addAll(userDirectoryRepository.findByFirstNameContainingIgnoreCase(lowerIdentifier));
        users.addAll(userDirectoryRepository.findByLastNameContainingIgnoreCase(lowerIdentifier));
        users
            .addAll(userDirectoryRepository.findByDisplayNameContainingIgnoreCase(lowerIdentifier));
      }

      // Convert to TenantInfo and remove duplicates
      return users.stream().filter(user -> user.getTenantId() != null).map(user -> {
        Tenant tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
        return tenant != null ? new TenantInfo(tenant.getKey(), tenant.getName()) : null;
      }).filter(tenantInfo -> tenantInfo != null).distinct().toList();

    } catch (Exception e) {
      log.warn("⚠️ Failed to search in user directory: {}", e.getMessage());
      return List.of();
    }
  }

  /**
   * 🔐 KEYCLOAK SEARCH: Hledání napříč všemi Keycloak realmy
   */
  private List<TenantInfo> findTenantsInKeycloak(String identifier) {
    List<TenantInfo> foundTenants = new ArrayList<>();

    try {
      // Get all available tenants (realms)
      List<Tenant> allTenants = tenantRepository.findAll();

      for (Tenant tenant : allTenants) {
        // Skip core-platform realm (admin realm)
        if ("core-platform".equals(tenant.getKey())) {
          continue;
        }

        try {
          // Search for user in this tenant's realm
          boolean userExists = searchUserInKeycloakRealm(tenant.getKey(), identifier);

          if (userExists) {
            foundTenants.add(new TenantInfo(tenant.getKey(), tenant.getName()));
            log.debug("🎯 Found user in Keycloak realm: {}", tenant.getKey());
          }

        } catch (Exception e) {
          log.debug("⚠️ Failed to search in realm {}: {}", tenant.getKey(), e.getMessage());
          // Continue with other realms
        }
      }

    } catch (Exception e) {
      log.warn("⚠️ Keycloak search failed: {}", e.getMessage());
    }

    return foundTenants;
  }

  /**
   * 🔍 KEYCLOAK REALM SEARCH: Ověří existence uživatele v konkrétním realmu
   */
  private boolean searchUserInKeycloakRealm(String realmName, String identifier) {
    try {
      // Switch Keycloak Admin Service to target realm temporarily
      // This is a simplified check - in real implementation, you'd need
      // to modify KeycloakAdminService to support multi-realm operations

      if (identifier.contains("@")) {
        // Search by email
        return keycloakAdminService.findUserByEmail(identifier) != null;
      } else {
        // Search by username
        return keycloakAdminService.findUserByUsername(identifier) != null;
      }

    } catch (Exception e) {
      log.debug("User not found in realm {}: {}", realmName, e.getMessage());
      return false;
    }
  }

  /**
   * 📋 AVAILABLE TENANTS: Seznam všech dostupných tenantů
   */
  public List<TenantInfo> getAvailableTenants() {
    try {
      return tenantRepository.findAll().stream()
          .filter(tenant -> !"core-platform".equals(tenant.getKey())) // Skip admin realm
          .map(tenant -> new TenantInfo(tenant.getKey(), tenant.getName())).toList();

    } catch (Exception e) {
      log.error("❌ Failed to get available tenants", e);
      return List.of();
    }
  }

  /**
   * 🔍 EMAIL DOMAIN HEURISTICS: Pokus o uhádnutí tenantu podle email domény
   */
  public List<TenantInfo> guessTenantByEmailDomain(String email) {
    if (!email.contains("@")) {
      return List.of();
    }

    String domain = email.substring(email.indexOf("@") + 1).toLowerCase();

    // Simple domain-based guessing
    // In real implementation, you might have a domain -> tenant mapping table
    List<Tenant> allTenants = tenantRepository.findAll();

    return allTenants.stream().filter(tenant -> {
      String tenantKey = tenant.getKey().toLowerCase();
      // Simple heuristics: check if domain contains tenant key or vice versa
      return domain.contains(tenantKey)
          || tenantKey.contains(domain.replace(".com", "").replace(".cz", ""));
    }).map(tenant -> new TenantInfo(tenant.getKey(), tenant.getName())).toList();
  }

  // Data class for tenant information
  public static class TenantInfo {
    private final String tenantKey;
    private final String tenantName;

    public TenantInfo(String tenantKey, String tenantName) {
      this.tenantKey = tenantKey;
      this.tenantName = tenantName;
    }

    public String getTenantKey() {
      return tenantKey;
    }

    public String getTenantName() {
      return tenantName;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null || getClass() != obj.getClass())
        return false;
      TenantInfo that = (TenantInfo) obj;
      return tenantKey.equals(that.tenantKey);
    }

    @Override
    public int hashCode() {
      return tenantKey.hashCode();
    }

    @Override
    public String toString() {
      return "TenantInfo{tenantKey='" + tenantKey + "', tenantName='" + tenantName + "'}";
    }
  }
}
