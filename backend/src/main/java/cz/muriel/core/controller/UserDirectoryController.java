package cz.muriel.core.controller;

import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.service.UserDirectoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController @RequestMapping("/api/users") @RequiredArgsConstructor @Slf4j @PreAuthorize("hasAnyAuthority('CORE_ROLE_USER', 'CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN')")
public class UserDirectoryController {

  private final UserDirectoryService userDirectoryService;

  @GetMapping("/me")
  public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
    String sub = jwt.getSubject();
    String preferredUsername = jwt.getClaimAsString("preferred_username");

    // Try to find by Keycloak user ID first
    Optional<UserDirectoryEntity> user = userDirectoryService.findByKeycloakUserId(sub);

    // If not found, try by username
    if (user.isEmpty() && preferredUsername != null) {
      user = userDirectoryService.findByUsername(preferredUsername);
    }

    if (user.isEmpty()) {
      log.debug("User not found in directory for sub: {}, username: {}", sub, preferredUsername);
      return ResponseEntity.noContent().build();
    }

    var userEntity = user.get();
    var response = Map.<String, Object>of("id", userEntity.getId(), "username",
        userEntity.getUsername(), "email",
        userEntity.getEmail() != null ? userEntity.getEmail() : "", "firstName",
        userEntity.getFirstName() != null ? userEntity.getFirstName() : "", "lastName",
        userEntity.getLastName() != null ? userEntity.getLastName() : "", "displayName",
        userEntity.getDisplayName() != null ? userEntity.getDisplayName() : "", "status",
        userEntity.getStatus(), "isFederated", userEntity.getIsFederated());

    log.debug("Returning current user from directory: {}", userEntity.getUsername());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/search")
  public ResponseEntity<List<Map<String, Object>>> searchUsers(
      @RequestParam(required = false) String q) {
    List<UserDirectoryEntity> users = userDirectoryService.search(q);

    List<Map<String, Object>> response = users.stream()
        .map(user -> Map.<String, Object>of("id", user.getId(), "username", user.getUsername(),
            "email", user.getEmail() != null ? user.getEmail() : "", "firstName",
            user.getFirstName() != null ? user.getFirstName() : "", "lastName",
            user.getLastName() != null ? user.getLastName() : "", "displayName",
            user.getDisplayName() != null ? user.getDisplayName() : "", "status", user.getStatus(),
            "isFederated", user.getIsFederated()))
        .toList();

    log.debug("Search returned {} users for query: '{}'", response.size(), q);
    return ResponseEntity.ok(response);
  }
}
