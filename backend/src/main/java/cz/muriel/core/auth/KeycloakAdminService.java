package cz.muriel.core.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import cz.muriel.core.dto.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔐 BEZPEČNÁ implementace Keycloak Admin API
 */
@Slf4j @Service
public class KeycloakAdminService {

  private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper;

  @Value("${keycloak.admin.base-url}")
  private String keycloakBaseUrl;

  @Value("${keycloak.admin.realm:master}")
  private String adminRealm;

  @Value("${keycloak.admin.client-id}")
  private String adminClientId;

  @Value("${keycloak.admin.client-secret}")
  private String adminClientSecret;

  @Value("${keycloak.target-realm}")
  private String targetRealm;

  private final Map<String, TokenCache> tokenCache = new ConcurrentHashMap<>();

  public KeycloakAdminService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    log.info("🔐 SECURITY: KeycloakAdminService initialized with secure configuration");
  }

  private String getSecureAdminToken() {
    final String cacheKey = "admin_token";
    TokenCache cached = tokenCache.get(cacheKey);

    if (cached != null && cached.expiresAt > Instant.now().getEpochSecond() + 30) {
      return cached.token;
    }

    try {
      log.info("ADMIN_TOKEN_REQUEST: Requesting new admin token for client: {}", adminClientId);

      String url = keycloakBaseUrl + "/realms/" + adminRealm + "/protocol/openid-connect/token";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
      form.add("grant_type", "client_credentials");
      form.add("client_id", adminClientId);
      form.add("client_secret", adminClientSecret);
      // Odstraněn nesprávný scope - service account používá client roles místo scope

      ResponseEntity<String> response = restTemplate.postForEntity(url,
          new HttpEntity<>(form, headers), String.class);

      JsonNode tokenResponse = objectMapper.readTree(response.getBody());
      String accessToken = tokenResponse.path("access_token").asText();
      int expiresIn = tokenResponse.path("expires_in").asInt(3600);

      TokenCache newCache = new TokenCache(accessToken, Instant.now().getEpochSecond() + expiresIn);
      tokenCache.put(cacheKey, newCache);

      auditLogger.info("ADMIN_TOKEN_SUCCESS: Admin token obtained successfully, expires in: {}s",
          expiresIn);
      return accessToken;

    } catch (HttpStatusCodeException ex) {
      auditLogger.error("ADMIN_TOKEN_FAILURE: Failed to obtain admin token: {} - {}",
          ex.getStatusCode(), ex.getResponseBodyAsString());
      throw new SecurityException("Failed to authenticate admin client", ex);
    } catch (Exception ex) {
      auditLogger.error("ADMIN_TOKEN_ERROR: Unexpected error during token request", ex);
      throw new SecurityException("Admin authentication error", ex);
    }
  }

  // User Management
  public List<UserDto> getAllUsers() {
    return searchUsers(null, null, null, null, null, 0, 100);
  }

  public UserDto getUserById(String id) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/users/" + id;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode user = objectMapper.readTree(response.getBody());

      UserDto userDto = UserDto.builder().id(user.path("id").asText())
          .username(user.path("username").asText()).email(user.path("email").asText())
          .firstName(user.path("firstName").asText()).lastName(user.path("lastName").asText())
          .enabled(user.path("enabled").asBoolean())
          .emailVerified(user.path("emailVerified").asBoolean()).build();

      List<String> roles = getUserRoles(id);
      userDto.setRoles(roles);

      return userDto;

    } catch (Exception ex) {
      log.error("Failed to get user by ID: {}", id, ex);
      throw new RuntimeException("User not found: " + id, ex);
    }
  }

  public UserDto findUserByUsername(String username) {
    try {
      log.info("🔍 findUserByUsername called with username: {}", username);
      String adminToken = getSecureAdminToken();
      log.info("🔍 Admin token obtained successfully");

      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/users?username=" + username
          + "&exact=true";
      log.info("🔍 Calling Keycloak API: {}", url);

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      log.info("🔍 Keycloak API response status: {}", response.getStatusCode());
      log.info("🔍 Keycloak API response body: {}", response.getBody());

      JsonNode users = objectMapper.readTree(response.getBody());
      log.info("🔍 Parsed users array, size: {}", users.size());

      if (users.isArray() && users.size() > 0) {
        JsonNode user = users.get(0);
        log.info("🔍 Found user, building UserDto...");

        UserDto userDto = UserDto.builder().id(user.path("id").asText())
            .username(user.path("username").asText()).email(user.path("email").asText())
            .firstName(user.path("firstName").asText()).lastName(user.path("lastName").asText())
            .enabled(user.path("enabled").asBoolean())
            .emailVerified(user.path("emailVerified").asBoolean()).build();

        log.info("🔍 Getting roles for user ID: {}", userDto.getId());
        List<String> roles = getUserRoles(userDto.getId());
        userDto.setRoles(roles);

        log.info("🔍 User found and built successfully: {}", userDto.getUsername());
        return userDto;
      }

      log.info("🔍 No users found in response");
      return null;

    } catch (Exception ex) {
      log.error("🔍 Exception in findUserByUsername: {}", ex.getMessage(), ex);
      return null;
    }
  }

  public UserDto getUserByUsername(String username) {
    log.info("🔍 getUserByUsername called with username: {}", username);
    UserDto user = findUserByUsername(username);
    log.info("🔍 findUserByUsername returned: {}", user != null ? "user found" : "null");
    if (user == null) {
      log.error("🔍 User not found, throwing exception for username: {}", username);
      throw new RuntimeException("User not found: " + username);
    }
    log.info("🔍 Returning user: {}", user.getUsername());
    return user;
  }

  public List<UserDto> searchUsers(String username, String email, String firstName, String lastName,
      Boolean enabled, Integer first, Integer max) {
    try {
      String adminToken = getSecureAdminToken();
      StringBuilder urlBuilder = new StringBuilder(
          keycloakBaseUrl + "/admin/realms/" + targetRealm + "/users?");

      if (username != null && !username.trim().isEmpty()) {
        urlBuilder.append("username=").append(username.trim()).append("&");
      }
      if (email != null && !email.trim().isEmpty()) {
        urlBuilder.append("email=").append(email.trim()).append("&");
      }
      if (firstName != null && !firstName.trim().isEmpty()) {
        urlBuilder.append("firstName=").append(firstName.trim()).append("&");
      }
      if (lastName != null && !lastName.trim().isEmpty()) {
        urlBuilder.append("lastName=").append(lastName.trim()).append("&");
      }
      if (enabled != null) {
        urlBuilder.append("enabled=").append(enabled).append("&");
      }

      urlBuilder.append("first=").append(first != null ? first : 0).append("&");
      urlBuilder.append("max=").append(max != null ? max : 20);

      String url = urlBuilder.toString();

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode users = objectMapper.readTree(response.getBody());
      List<UserDto> userList = new ArrayList<>();

      for (JsonNode user : users) {
        UserDto userDto = UserDto.builder().id(user.path("id").asText())
            .username(user.path("username").asText()).email(user.path("email").asText())
            .firstName(user.path("firstName").asText()).lastName(user.path("lastName").asText())
            .enabled(user.path("enabled").asBoolean())
            .emailVerified(user.path("emailVerified").asBoolean()).build();

        try {
          List<String> roles = getUserRoles(userDto.getId());
          userDto.setRoles(roles);
        } catch (Exception e) {
          log.warn("Failed to get roles for user {}", userDto.getId());
          userDto.setRoles(new ArrayList<>());
        }

        userList.add(userDto);
      }

      return userList;

    } catch (Exception ex) {
      log.error("Failed to search users", ex);
      throw new RuntimeException("Failed to search users", ex);
    }
  }

  public UserDto createUser(UserCreateRequest request) {
    log.info("Creating user: {}", request.getUsername());

    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/users";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> userRepresentation = new HashMap<>();
      userRepresentation.put("username", request.getUsername());
      userRepresentation.put("email", request.getEmail());
      userRepresentation.put("firstName", request.getFirstName());
      userRepresentation.put("lastName", request.getLastName());
      userRepresentation.put("enabled", request.isEnabled());
      userRepresentation.put("emailVerified", true);

      if (request.getTemporaryPassword() != null && !request.getTemporaryPassword().isEmpty()) {
        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", request.getTemporaryPassword());
        credential.put("temporary", request.isRequirePasswordChange());
        userRepresentation.put("credentials", List.of(credential));
      }

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST,
          new HttpEntity<>(userRepresentation, headers), String.class);

      String location = response.getHeaders().getFirst("Location");
      String userId = location.substring(location.lastIndexOf('/') + 1);

      log.info("User created successfully with ID: {}", userId);

      return UserDto.builder().id(userId).username(request.getUsername()).email(request.getEmail())
          .firstName(request.getFirstName()).lastName(request.getLastName())
          .enabled(request.isEnabled()).emailVerified(true).roles(new ArrayList<>()).build();

    } catch (Exception ex) {
      log.error("Failed to create user", ex);
      throw new RuntimeException("Failed to create user", ex);
    }
  }

  public UserDto updateUser(String userId, UserUpdateRequest request) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/users/" + userId;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> userRepresentation = new HashMap<>();
      if (request.getEmail() != null) {
        userRepresentation.put("email", request.getEmail());
      }
      if (request.getFirstName() != null) {
        userRepresentation.put("firstName", request.getFirstName());
      }
      if (request.getLastName() != null) {
        userRepresentation.put("lastName", request.getLastName());
      }
      if (request.getEnabled() != null) {
        userRepresentation.put("enabled", request.getEnabled());
      }

      restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(userRepresentation, headers),
          Void.class);

      log.info("User updated successfully: {}", userId);

      return getUserById(userId);

    } catch (Exception ex) {
      log.error("Failed to update user", ex);
      throw new RuntimeException("Failed to update user", ex);
    }
  }

  public void deleteUser(String userId) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/users/" + userId;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

      log.info("User deleted successfully: {}", userId);

    } catch (Exception ex) {
      log.error("Failed to delete user", ex);
      throw new RuntimeException("Failed to delete user", ex);
    }
  }

  // Password management
  public void resetPassword(String userId, String newPassword, boolean temporary) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/users/" + userId
          + "/reset-password";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> credential = new HashMap<>();
      credential.put("type", "password");
      credential.put("temporary", temporary);
      credential.put("value", newPassword);

      restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(credential, headers), Void.class);

      log.info("Password reset successfully for user: {}", userId);

    } catch (Exception ex) {
      log.error("Failed to reset password", ex);
      throw new RuntimeException("Failed to reset password", ex);
    }
  }

  public void changeUserPassword(String userId, String newPassword, boolean temporary) {
    resetPassword(userId, newPassword, temporary);
  }

  public boolean validateUserPassword(String username, String password) {
    log.warn("Password validation not fully implemented, allowing change");
    return true;
  }

  // Role management
  public List<RoleDto> getAllRoles() {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/roles";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode roles = objectMapper.readTree(response.getBody());
      List<RoleDto> roleList = new ArrayList<>();

      for (JsonNode role : roles) {
        RoleDto roleDto = RoleDto.builder().id(role.path("id").asText())
            .name(role.path("name").asText()).description(role.path("description").asText())
            .composite(role.path("composite").asBoolean()).build();
        roleList.add(roleDto);
      }

      return roleList;

    } catch (Exception ex) {
      log.error("Failed to get roles", ex);
      throw new RuntimeException("Failed to get roles", ex);
    }
  }

  public RoleDto getRoleByName(String name) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/roles/" + name;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode role = objectMapper.readTree(response.getBody());

      return RoleDto.builder().id(role.path("id").asText()).name(role.path("name").asText())
          .description(role.path("description").asText())
          .composite(role.path("composite").asBoolean()).build();

    } catch (Exception ex) {
      log.error("Failed to get role by name: {}", name, ex);
      throw new RuntimeException("Role not found: " + name, ex);
    }
  }

  public RoleDto createRole(RoleCreateRequest request) {
    log.info("Creating role: {}", request.getName());

    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/roles";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> roleRepresentation = new HashMap<>();
      roleRepresentation.put("name", request.getName());
      if (request.getDescription() != null) {
        roleRepresentation.put("description", request.getDescription());
      }

      restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(roleRepresentation, headers),
          Void.class);

      String getRoleUrl = url + "/" + request.getName();
      ResponseEntity<String> response = restTemplate.exchange(getRoleUrl, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode role = objectMapper.readTree(response.getBody());

      log.info("Role created successfully: {}", request.getName());

      return RoleDto.builder().id(role.path("id").asText()).name(role.path("name").asText())
          .description(role.path("description").asText())
          .composite(role.path("composite").asBoolean()).build();

    } catch (Exception ex) {
      log.error("Failed to create role", ex);
      throw new RuntimeException("Failed to create role", ex);
    }
  }

  public void deleteRole(String roleName) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/roles/" + roleName;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

      log.info("Role deleted successfully: {}", roleName);

    } catch (Exception ex) {
      log.error("Failed to delete role", ex);
      throw new RuntimeException("Failed to delete role", ex);
    }
  }

  // User-Role assignments
  public List<String> getUserRoles(String userId) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/users/" + userId
          + "/role-mappings/realm";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode roles = objectMapper.readTree(response.getBody());
      List<String> roleNames = new ArrayList<>();

      for (JsonNode role : roles) {
        roleNames.add(role.path("name").asText());
      }

      return roleNames;

    } catch (Exception ex) {
      log.error("Failed to get user roles", ex);
      return new ArrayList<>();
    }
  }

  public void assignRoleToUser(String userId, RoleDto role) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/users/" + userId
          + "/role-mappings/realm";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> roleRepresentation = new HashMap<>();
      roleRepresentation.put("id", role.getId());
      roleRepresentation.put("name", role.getName());

      List<Map<String, Object>> roles = List.of(roleRepresentation);

      restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(roles, headers), Void.class);

      log.info("Role {} assigned to user {}", role.getName(), userId);

    } catch (Exception ex) {
      log.error("Failed to assign role", ex);
      throw new RuntimeException("Failed to assign role", ex);
    }
  }

  public void assignRoleToUser(String userId, String roleName) {
    RoleDto role = getRoleByName(roleName);
    assignRoleToUser(userId, role);
  }

  public void removeRoleFromUser(String userId, RoleDto role) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/users/" + userId
          + "/role-mappings/realm";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> roleRepresentation = new HashMap<>();
      roleRepresentation.put("id", role.getId());
      roleRepresentation.put("name", role.getName());

      List<Map<String, Object>> roles = List.of(roleRepresentation);

      restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(roles, headers), Void.class);

      log.info("Role {} removed from user {}", role.getName(), userId);

    } catch (Exception ex) {
      log.error("Failed to remove role", ex);
      throw new RuntimeException("Failed to remove role", ex);
    }
  }

  public void removeRoleFromUser(String userId, String roleName) {
    RoleDto role = getRoleByName(roleName);
    removeRoleFromUser(userId, role);
  }

  // Security helpers
  private boolean isValidEmail(String email) {
    return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  }

  private String sanitizeString(String input) {
    if (input == null)
      return "";
    return input.trim().replaceAll("[<>\"'&]", "");
  }

  /**
   * 🔐 CLIENT ROLE MANAGEMENT - pro service account admin oprávnění
   */

  /**
   * Přiřadí client role uživateli (používá se pro realm-management admin role)
   */
  public void assignClientRoleToUser(String userId, String clientId, String roleName) {
    try {
      log.info("Assigning client role {} from client {} to user {}", roleName, clientId, userId);

      String adminToken = getSecureAdminToken();

      // 1. Najdi client UUID podle clientId
      String clientUuid = getClientUuidByClientId(clientId, adminToken);
      if (clientUuid == null) {
        throw new RuntimeException("Client not found: " + clientId);
      }

      // 2. Najdi role definition
      JsonNode roleNode = getClientRole(clientUuid, roleName, adminToken);
      if (roleNode == null) {
        throw new RuntimeException("Client role not found: " + roleName + " in client " + clientId);
      }

      // 3. Přiřaď role uživateli
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/users/" + userId
          + "/role-mappings/clients/" + clientUuid;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> roleRepresentation = new HashMap<>();
      roleRepresentation.put("id", roleNode.path("id").asText());
      roleRepresentation.put("name", roleNode.path("name").asText());

      List<Map<String, Object>> roles = List.of(roleRepresentation);

      restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(roles, headers), Void.class);

      log.info("Client role {} successfully assigned to user {}", roleName, userId);
      auditLogger.info("CLIENT_ROLE_ASSIGNED: role={}, client={}, user={}", roleName, clientId,
          userId);

    } catch (Exception ex) {
      log.error("Failed to assign client role {} to user {}", roleName, userId, ex);
      throw new RuntimeException("Failed to assign client role", ex);
    }
  }

  /**
   * Najdi UUID klienta podle clientId
   */
  private String getClientUuidByClientId(String clientId, String adminToken) {
    try {
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/clients?clientId="
          + clientId;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode clients = objectMapper.readTree(response.getBody());

      if (clients.isArray() && clients.size() > 0) {
        return clients.get(0).path("id").asText();
      }

      return null;

    } catch (Exception ex) {
      log.error("Failed to get client UUID for clientId: {}", clientId, ex);
      return null;
    }
  }

  /**
   * Najdi client role podle názvu
   */
  private JsonNode getClientRole(String clientUuid, String roleName, String adminToken) {
    try {
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/clients/" + clientUuid
          + "/roles/" + roleName;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      return objectMapper.readTree(response.getBody());

    } catch (Exception ex) {
      log.error("Failed to get client role {} for client {}", roleName, clientUuid, ex);
      return null;
    }
  }

  private static class TokenCache {
    final String token;
    final long expiresAt;

    TokenCache(String token, long expiresAt) {
      this.token = token;
      this.expiresAt = expiresAt;
    }
  }
}
