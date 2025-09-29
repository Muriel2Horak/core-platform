package cz.muriel.core.service;

import cz.muriel.core.dto.KeycloakWebhookEventDto;
import cz.muriel.core.entity.Tenant;
import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.repository.UserDirectoryRepository;
import cz.muriel.core.repository.KeycloakEventLogRepository;
import cz.muriel.core.entity.KeycloakEventLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service @RequiredArgsConstructor @Slf4j @Transactional
public class KeycloakEventProjectionService {

  private final UserDirectoryRepository userDirectoryRepository;
  private final KeycloakEventLogRepository eventLogRepository;
  private final TenantService tenantService; // 🆕 Added for simplified tenant management

  public void processEvent(KeycloakWebhookEventDto event) {
    // Set tenant context for logging
    MDC.put("tenant", event.getTenantKey());

    try {
      // Check for idempotence
      String eventHash = calculateEventHash(event);
      if (eventLogRepository.existsByEventHash(eventHash)) {
        log.debug("Event already processed (duplicate): {}", event.getEventType());
        return;
      }

      // 🎯 CLEAN ARCHITECTURE: Tenant must exist in DB registry
      // We don't auto-create tenants - they must be properly set up first
      Optional<Tenant> tenant = tenantService.findTenantByKey(event.getTenantKey());

      if (tenant.isEmpty()) {
        log.warn("🚫 Event rejected - tenant not found in registry: {}", event.getTenantKey());
        log.warn("💡 Tenant must be created first through admin interface");
        return;
      }

      log.debug("Processing event for registered tenant: {}", tenant.get().getName());

      // Process the event
      processUserEvent(event, tenant.get());

      // Log event as processed
      KeycloakEventLog eventLog = new KeycloakEventLog();
      eventLog.setEventHash(eventHash);
      eventLog.setCreatedAt(LocalDateTime.now());
      eventLogRepository.save(eventLog);

      log.info("Successfully processed Keycloak event: type={}, user={}, tenant={}",
          event.getEventType(), event.getUsername(), event.getTenantKey());

    } catch (Exception e) {
      log.error("Failed to process Keycloak event: type={}, user={}, tenant={}",
          event.getEventType(), event.getUsername(), event.getTenantKey(), e);
      throw e;
    } finally {
      MDC.remove("tenant");
    }
  }

  private void processUserEvent(KeycloakWebhookEventDto event, Tenant tenant) {
    switch (event.getEventType()) {
    case "USER_CREATED", "USER_UPDATED" -> upsertUser(event, tenant);
    case "USER_DELETED" -> softDeleteUser(event, tenant);
    case "ROLE_CREATED", "ROLE_UPDATED", "ROLE_DELETED" -> updateUserRoles(event, tenant);
    case "GROUP_MEMBERSHIP_CREATED", "GROUP_MEMBERSHIP_DELETED" -> updateUserGroups(event, tenant);
    default -> log.debug("Ignoring event type: {}", event.getEventType());
    }
  }

  private void upsertUser(KeycloakWebhookEventDto event, Tenant tenant) {
    if (event.getUserId() == null || event.getUsername() == null) {
      log.warn("Missing required user data in event: userId={}, username={}", event.getUserId(),
          event.getUsername());
      return;
    }

    // Find existing user by Keycloak ID or username
    Optional<UserDirectoryEntity> existingUser = userDirectoryRepository
        .findByKeycloakUserId(event.getUserId());

    if (existingUser.isEmpty()) {
      existingUser = userDirectoryRepository.findByUsernameIgnoreCase(event.getUsername());
    }

    UserDirectoryEntity user;
    if (existingUser.isPresent()) {
      user = existingUser.get();
      log.debug("Updating existing user: {}", user.getUsername());
    } else {
      user = new UserDirectoryEntity();
      user.setTenantId(tenant.getId());
      user.setCreatedAt(LocalDateTime.now());
      log.debug("Creating new user: {}", event.getUsername());
    }

    // Update user fields
    user.setKeycloakUserId(event.getUserId());
    user.setUsername(event.getUsername());
    user.setEmail(event.getEmail());
    user.setFirstName(event.getFirstName());
    user.setLastName(event.getLastName());
    user.setUpdatedAt(LocalDateTime.now());

    // Set active status based on enabled flag
    user.setActive(event.getEnabled() != null ? event.getEnabled() : true);
    user.setDeletedAt(null); // Clear soft delete if re-enabling

    // Update display name
    if (event.getFirstName() != null || event.getLastName() != null) {
      String displayName = buildDisplayName(event.getFirstName(), event.getLastName());
      user.setDisplayName(displayName);
    }

    // Store roles and groups as JSON
    try {
      if (event.getRoles() != null) {
        // Roles is a Map<String, Object>, need to serialize it properly
        StringBuilder rolesJson = new StringBuilder();
        event.getRoles().forEach((key, value) -> {
          if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> roleList = (List<String>) value;
            if (!roleList.isEmpty()) {
              rolesJson.append(key).append(":").append(String.join(",", roleList)).append(";");
            }
          }
        });
        user.setRolesJson(rolesJson.toString());
      }
      if (event.getGroups() != null) {
        user.setGroupsJson(String.join(",", event.getGroups()));
      }
    } catch (Exception e) {
      log.warn("Failed to serialize roles/groups for user {}: {}", user.getUsername(),
          e.getMessage());
    }

    // Extract and store selected attributes
    if (event.getAttributes() != null) {
      user.setPhoneNumber(event.getAttributes().get("phoneNumber"));
      user.setDepartment(event.getAttributes().get("department"));
      user.setTitle(event.getAttributes().get("title"));
    }

    userDirectoryRepository.save(user);
  }

  private void softDeleteUser(KeycloakWebhookEventDto event, Tenant tenant) {
    if (event.getUserId() == null) {
      log.warn("Missing userId for DELETE event");
      return;
    }

    Optional<UserDirectoryEntity> user = userDirectoryRepository
        .findByKeycloakUserId(event.getUserId());

    if (user.isPresent()) {
      UserDirectoryEntity userEntity = user.get();
      userEntity.setActive(false);
      userEntity.setDeletedAt(LocalDateTime.now());
      userEntity.setUpdatedAt(LocalDateTime.now());

      userDirectoryRepository.save(userEntity);
      log.debug("Soft deleted user: {}", userEntity.getUsername());
    } else {
      log.debug("User not found for deletion: keycloakId={}", event.getUserId());
    }
  }

  private void updateUserRoles(KeycloakWebhookEventDto event, Tenant tenant) {
    // For role events, we need to update all affected users
    // This is a simplified implementation - in production you might want to
    // fetch the user list from the event or do a targeted update
    if (event.getUserId() != null) {
      upsertUser(event, tenant); // Reuse the upsert logic
    }
  }

  private void updateUserGroups(KeycloakWebhookEventDto event, Tenant tenant) {
    // Similar to roles - update the specific user's group membership
    if (event.getUserId() != null) {
      upsertUser(event, tenant); // Reuse the upsert logic
    }
  }

  private String buildDisplayName(String firstName, String lastName) {
    if (firstName != null && lastName != null) {
      return firstName + " " + lastName;
    } else if (firstName != null) {
      return firstName;
    } else if (lastName != null) {
      return lastName;
    }
    return null;
  }

  private String calculateEventHash(KeycloakWebhookEventDto event) {
    try {
      // Create hash from key event properties to ensure idempotence
      String hashInput = String.format("%s:%s:%s:%s:%d", event.getTenantKey(), event.getUserId(),
          event.getEventType(), event.getUsername(),
          event.getTime() != null ? event.getTime() : System.currentTimeMillis());

      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(hashInput.getBytes(StandardCharsets.UTF_8));

      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }
}
