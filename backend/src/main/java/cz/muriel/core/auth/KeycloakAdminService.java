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

  // 🔐 Master realm credentials for realm management
  @Value("${keycloak.master.username:admin}")
  private String masterUsername;

  @Value("${keycloak.master.password:admin123}")
  private String masterPassword;

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

  /**
   * 🔐 Get Master Realm Admin Token for realm management operations Uses direct
   * admin credentials instead of service account
   */
  private String getMasterAdminToken() {
    final String cacheKey = "master_admin_token";
    TokenCache cached = tokenCache.get(cacheKey);

    if (cached != null && cached.expiresAt > Instant.now().getEpochSecond() + 30) {
      return cached.token;
    }

    try {
      log.info("MASTER_TOKEN_REQUEST: Requesting master realm admin token for user: {}",
          masterUsername);

      String url = keycloakBaseUrl + "/realms/master/protocol/openid-connect/token";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
      form.add("grant_type", "password");
      form.add("client_id", "admin-cli");
      form.add("username", masterUsername);
      form.add("password", masterPassword);

      ResponseEntity<String> response = restTemplate.postForEntity(url,
          new HttpEntity<>(form, headers), String.class);

      JsonNode tokenResponse = objectMapper.readTree(response.getBody());
      String accessToken = tokenResponse.path("access_token").asText();
      int expiresIn = tokenResponse.path("expires_in").asInt(3600);

      TokenCache newCache = new TokenCache(accessToken, Instant.now().getEpochSecond() + expiresIn);
      tokenCache.put(cacheKey, newCache);

      auditLogger.info(
          "MASTER_TOKEN_SUCCESS: Master admin token obtained successfully, expires in: {}s",
          expiresIn);
      return accessToken;

    } catch (HttpStatusCodeException ex) {
      auditLogger.error("MASTER_TOKEN_FAILURE: Failed to obtain master admin token: {} - {}",
          ex.getStatusCode(), ex.getResponseBodyAsString());
      throw new SecurityException("Failed to authenticate master admin", ex);
    } catch (Exception ex) {
      auditLogger.error("MASTER_TOKEN_ERROR: Unexpected error during master token request", ex);
      throw new SecurityException("Master admin authentication error", ex);
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

      UserDto userDto = buildUserDtoFromJson(user);

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

        UserDto userDto = buildUserDtoFromJson(user);

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
        UserDto userDto = buildUserDtoFromJson(user);

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
      if (location == null) {
        throw new RuntimeException("Failed to get user location from response headers");
      }
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

      // Základní atributy
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

      // 🏢 Custom atributy pro organizační strukturu
      Map<String, List<String>> attributes = new HashMap<>();

      if (request.getDepartment() != null) {
        attributes.put("department", List.of(request.getDepartment()));
      }
      if (request.getPosition() != null) {
        attributes.put("position", List.of(request.getPosition()));
      }
      if (request.getManager() != null) {
        attributes.put("manager", List.of(request.getManager()));
      }
      if (request.getCostCenter() != null) {
        attributes.put("costCenter", List.of(request.getCostCenter()));
      }
      if (request.getLocation() != null) {
        attributes.put("location", List.of(request.getLocation()));
      }
      if (request.getPhone() != null) {
        attributes.put("phone", List.of(request.getPhone()));
      }

      // 👥 Zástupství
      if (request.getDeputy() != null) {
        attributes.put("deputy", List.of(request.getDeputy()));
      }
      if (request.getDeputyFrom() != null) {
        attributes.put("deputyFrom", List.of(request.getDeputyFrom().toString()));
      }
      if (request.getDeputyTo() != null) {
        attributes.put("deputyTo", List.of(request.getDeputyTo().toString()));
      }
      if (request.getDeputyReason() != null) {
        attributes.put("deputyReason", List.of(request.getDeputyReason()));
      }

      // 📷 Profilová fotka
      if (request.getProfilePicture() != null) {
        attributes.put("profilePicture", List.of(request.getProfilePicture()));
      }

      if (!attributes.isEmpty()) {
        userRepresentation.put("attributes", attributes);
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

  /**
   * 🏢 HELPER METHODS pro organizační strukturu a custom atributy
   */

  /**
   * Převede Keycloak JsonNode na UserDto s kompletními organizačními atributy
   */
  private UserDto buildUserDtoFromJson(JsonNode user) {
    try {
      // Základní atributy
      UserDto.UserDtoBuilder builder = UserDto.builder().id(user.path("id").asText())
          .username(user.path("username").asText()).email(user.path("email").asText())
          .firstName(user.path("firstName").asText()).lastName(user.path("lastName").asText())
          .enabled(user.path("enabled").asBoolean())
          .emailVerified(user.path("emailVerified").asBoolean());

      // Timestamp
      if (user.has("createdTimestamp")) {
        long timestamp = user.path("createdTimestamp").asLong();
        builder.createdTimestamp(Instant.ofEpochMilli(timestamp));
      }

      // Custom atributy z Keycloak attributes pole
      JsonNode attributes = user.path("attributes");
      if (attributes != null && !attributes.isMissingNode()) {
        // 🏢 Organizační struktura
        builder.department(getAttributeValue(attributes, "department"))
            .position(getAttributeValue(attributes, "position"))
            .manager(getAttributeValue(attributes, "manager"))
            .costCenter(getAttributeValue(attributes, "costCenter"))
            .location(getAttributeValue(attributes, "location"))
            .phone(getAttributeValue(attributes, "phone"));

        // 👥 Zástupství
        builder.deputy(getAttributeValue(attributes, "deputy"))
            .deputyReason(getAttributeValue(attributes, "deputyReason"));

        // 📷 Profilová fotka a identita
        builder.profilePicture(getAttributeValue(attributes, "profilePicture"));

        // Datum atributy
        String deputyFromStr = getAttributeValue(attributes, "deputyFrom");
        if (deputyFromStr != null && !deputyFromStr.isEmpty()) {
          try {
            builder.deputyFrom(java.time.LocalDate.parse(deputyFromStr));
          } catch (Exception e) {
            log.warn("Failed to parse deputyFrom date: {}", deputyFromStr);
          }
        }

        String deputyToStr = getAttributeValue(attributes, "deputyTo");
        if (deputyToStr != null && !deputyToStr.isEmpty()) {
          try {
            builder.deputyTo(java.time.LocalDate.parse(deputyToStr));
          } catch (Exception e) {
            log.warn("Failed to parse deputyTo date: {}", deputyToStr);
          }
        }
      }

      UserDto userDto = builder.build();

      // 🔍 Identifikace zdroje uživatele (lokální vs federovaný)
      detectUserIdentitySource(user, userDto);

      // Rozšířené informace - najdi jména pro manager a deputy
      if (userDto.getManager() != null && !userDto.getManager().isEmpty()) {
        userDto.setManagerName(resolveUserDisplayName(userDto.getManager()));
      }
      if (userDto.getDeputy() != null && !userDto.getDeputy().isEmpty()) {
        userDto.setDeputyName(resolveUserDisplayName(userDto.getDeputy()));
      }

      return userDto;

    } catch (Exception ex) {
      log.error("Failed to build UserDto from JSON", ex);
      throw new RuntimeException("Failed to parse user data", ex);
    }
  }

  /**
   * Extrahuje hodnotu z Keycloak attributes pole (které je array stringů)
   */
  private String getAttributeValue(JsonNode attributes, String attributeName) {
    JsonNode attributeNode = attributes.path(attributeName);
    if (attributeNode != null && attributeNode.isArray() && attributeNode.size() > 0) {
      return attributeNode.get(0).asText();
    }
    return null;
  }

  /**
   * 🔍 IDENTITY SOURCE DETECTION - detekuje zdroj uživatele (lokální vs
   * federovaný)
   */
  private void detectUserIdentitySource(JsonNode user, UserDto userDto) {
    try {
      // Zkontroluj federatedIdentities pro federované uživatele
      JsonNode federatedIdentities = user.path("federatedIdentities");

      if (federatedIdentities != null && federatedIdentities.isArray()
          && federatedIdentities.size() > 0) {
        // Uživatel je federovaný (z AD, SAML, atd.)
        JsonNode firstIdentity = federatedIdentities.get(0);
        String identityProvider = firstIdentity.path("identityProvider").asText();
        String federatedUsername = firstIdentity.path("userName").asText();

        // Použijeme reflection pro nastavení isLocalUser
        setUserIdentityInfo(userDto, false, identityProvider, federatedUsername);

        // Detekuj typ providera podle názvu
        if (identityProvider.toLowerCase().contains("ldap")
            || identityProvider.toLowerCase().contains("ad")) {
          userDto.setIdentityProviderAlias("Active Directory");
        } else if (identityProvider.toLowerCase().contains("saml")) {
          userDto.setIdentityProviderAlias("SAML Provider");
        } else if (identityProvider.toLowerCase().contains("oidc")) {
          userDto.setIdentityProviderAlias("OIDC Provider");
        } else {
          userDto.setIdentityProviderAlias(identityProvider);
        }

        log.debug("User {} is federated from provider: {} ({})", userDto.getUsername(),
            identityProvider, federatedUsername);
      } else {
        // Uživatel je lokální v Keycloak
        setUserIdentityInfo(userDto, true, "keycloak-local", null);
        userDto.setIdentityProviderAlias("Lokální Keycloak");

        log.debug("User {} is local Keycloak user", userDto.getUsername());
      }

    } catch (Exception ex) {
      log.warn("Failed to detect identity source for user {}: {}", userDto.getUsername(),
          ex.getMessage());

      // Fallback - předpokládej lokálního uživatele
      setUserIdentityInfo(userDto, true, "keycloak-local", null);
      userDto.setIdentityProviderAlias("Lokální Keycloak");
    }
  }

  /**
   * Helper metoda pro nastavení identity informací pomocí reflection
   */
  private void setUserIdentityInfo(UserDto userDto, boolean isLocal, String provider,
      String federatedUsername) {
    try {
      // Použijeme reflection pro nastavení isLocalUser property
      java.lang.reflect.Field isLocalUserField = UserDto.class.getDeclaredField("isLocalUser");
      isLocalUserField.setAccessible(true);
      isLocalUserField.set(userDto, isLocal);

      userDto.setIdentityProvider(provider);
      userDto.setFederatedUsername(federatedUsername);
    } catch (Exception e) {
      log.warn("Failed to set identity info using reflection: {}", e.getMessage());
      // Fallback - necháme isLocalUser na default hodnotě
    }
  }

  /**
   * Najde zobrazovací jméno uživatele podle username
   */
  private String resolveUserDisplayName(String username) {
    try {
      UserDto user = findUserByUsername(username);
      if (user != null) {
        if (user.getFirstName() != null && user.getLastName() != null) {
          return user.getFirstName() + " " + user.getLastName();
        }
        return user.getUsername();
      }
    } catch (Exception e) {
      log.debug("Could not resolve display name for username: {}", username);
    }
    return username; // fallback
  }

  /**
   * 🆕 REALM MANAGEMENT: Create new Keycloak realm
   */
  public void createRealm(Map<String, Object> realmConfig) {
    try {
      // 🔐 Use master realm admin token for realm creation
      String adminToken = getMasterAdminToken();
      String url = keycloakBaseUrl + "/admin/realms";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      String requestBody = objectMapper.writeValueAsString(realmConfig);
      HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

      ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request,
          Void.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("✅ Realm created successfully: {}", realmConfig.get("realm"));
        auditLogger.info("REALM_CREATED: realm={}", realmConfig.get("realm"));
      } else {
        throw new RuntimeException("Failed to create realm: " + response.getStatusCode());
      }

    } catch (Exception ex) {
      log.error("❌ Failed to create realm: {}", realmConfig.get("realm"), ex);
      throw new RuntimeException("Failed to create realm: " + ex.getMessage(), ex);
    }
  }

  /**
   * 🆕 REALM MANAGEMENT: Delete Keycloak realm
   */
  public void deleteRealm(String realmName) {
    try {
      // 🔐 Use master realm admin token for realm deletion
      String adminToken = getMasterAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + realmName;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE,
          new HttpEntity<>(headers), Void.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("✅ Realm deleted successfully: {}", realmName);
        auditLogger.info("REALM_DELETED: realm={}", realmName);
      } else {
        throw new RuntimeException("Failed to delete realm: " + response.getStatusCode());
      }

    } catch (Exception ex) {
      log.error("❌ Failed to delete realm: {}", realmName, ex);
      throw new RuntimeException("Failed to delete realm: " + ex.getMessage(), ex);
    }
  }

  /**
   * 🆕 REALM MANAGEMENT: List all realms
   */
  public List<Map<String, Object>> getAllRealms() {
    try {
      // 🔐 Use master realm admin token for listing realms
      String adminToken = getMasterAdminToken();
      String url = keycloakBaseUrl + "/admin/realms";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode realms = objectMapper.readTree(response.getBody());
      List<Map<String, Object>> realmList = new ArrayList<>();

      for (JsonNode realm : realms) {
        Map<String, Object> realmInfo = Map.of("realm", realm.path("realm").asText(), "displayName",
            realm.path("displayName").asText(""), "enabled", realm.path("enabled").asBoolean(),
            "id", realm.path("id").asText());
        realmList.add(realmInfo);
      }

      return realmList;

    } catch (Exception ex) {
      log.error("❌ Failed to get realms list", ex);
      throw new RuntimeException("Failed to get realms: " + ex.getMessage(), ex);
    }
  }

  /**
   * 🆕 USER MANAGEMENT: Create user in specific realm
   */
  public void createUser(String realmName, Map<String, Object> userData) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + realmName + "/users";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      String requestBody = objectMapper.writeValueAsString(userData);
      HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

      ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request,
          Void.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("✅ User created successfully in realm {}: {}", realmName,
            userData.get("username"));
        auditLogger.info("USER_CREATED: realm={}, username={}", realmName,
            userData.get("username"));
      } else {
        throw new RuntimeException("Failed to create user: " + response.getStatusCode());
      }

    } catch (Exception ex) {
      log.error("❌ Failed to create user in realm {}: {}", realmName, userData.get("username"), ex);
      throw new RuntimeException("Failed to create user: " + ex.getMessage(), ex);
    }
  }

  /**
   * 🆕 CLIENT MANAGEMENT: Update protocol mapper for client
   */
  public void updateClientProtocolMapper(String realmName, String clientId, String mapperName,
      Map<String, Object> config) {
    try {
      log.debug("Updating protocol mapper '{}' for client '{}' in realm '{}'", mapperName, clientId,
          realmName);

      String adminToken = getSecureAdminToken();

      // First, get client UUID
      String clientUuid = getClientUuid(realmName, clientId, adminToken);
      if (clientUuid == null) {
        log.warn("Client '{}' not found in realm '{}'", clientId, realmName);
        return;
      }

      // Get existing protocol mappers
      String mappersUrl = keycloakBaseUrl + "/admin/realms/" + realmName + "/clients/" + clientUuid
          + "/protocol-mappers/models";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> mappersResponse = restTemplate.exchange(mappersUrl, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      if (!mappersResponse.getStatusCode().is2xxSuccessful()) {
        log.warn("Failed to get protocol mappers for client '{}' in realm '{}'", clientId,
            realmName);
        return;
      }

      // Find the specific mapper
      JsonNode mappers = objectMapper.readTree(mappersResponse.getBody());
      String mapperId = null;

      for (JsonNode mapper : mappers) {
        if (mapperName.equals(mapper.path("name").asText())) {
          mapperId = mapper.path("id").asText();
          break;
        }
      }

      if (mapperId == null) {
        log.warn("Protocol mapper '{}' not found for client '{}' in realm '{}'", mapperName,
            clientId, realmName);
        return;
      }

      // Update the mapper
      String updateUrl = mappersUrl + "/" + mapperId;

      // Get current mapper config and update it
      Map<String, Object> mapperUpdate = new HashMap<>();
      mapperUpdate.put("id", mapperId);
      mapperUpdate.put("name", mapperName);
      mapperUpdate.put("protocol", "openid-connect");
      mapperUpdate.put("protocolMapper", "oidc-hardcoded-claim-mapper");
      mapperUpdate.put("consentRequired", false);
      mapperUpdate.put("config", config);

      String requestBody = objectMapper.writeValueAsString(mapperUpdate);
      HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

      ResponseEntity<Void> updateResponse = restTemplate.exchange(updateUrl, HttpMethod.PUT,
          request, Void.class);

      if (updateResponse.getStatusCode().is2xxSuccessful()) {
        log.debug("✅ Protocol mapper '{}' updated successfully", mapperName);
      } else {
        log.warn("Failed to update protocol mapper '{}': {}", mapperName,
            updateResponse.getStatusCode());
      }

    } catch (Exception ex) {
      log.warn("Failed to update protocol mapper '{}' for client '{}' in realm '{}': {}",
          mapperName, clientId, realmName, ex.getMessage());
    }
  }

  /**
   * 🔍 HELPER: Get client UUID by client ID
   */
  private String getClientUuid(String realmName, String clientId, String adminToken) {
    try {
      String url = keycloakBaseUrl + "/admin/realms/" + realmName + "/clients?clientId=" + clientId;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        JsonNode clients = objectMapper.readTree(response.getBody());
        if (clients.size() > 0) {
          return clients.get(0).path("id").asText();
        }
      }

    } catch (Exception ex) {
      log.debug("Failed to get client UUID for '{}' in realm '{}': {}", clientId, realmName,
          ex.getMessage());
    }

    return null;
  }

  /**
   * 🔍 USER SEARCH: Find user by email
   */
  public JsonNode findUserByEmail(String email) {
    // Note: This is a simplified version - in real implementation,
    // you'd need to specify the realm or search across all tenant realms
    try {
      String adminToken = getSecureAdminToken();
      // This would need to be adapted for multi-realm search
      String url = keycloakBaseUrl + "/admin/realms/master/users?email=" + email;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        JsonNode users = objectMapper.readTree(response.getBody());
        return users.size() > 0 ? users.get(0) : null;
      }

    } catch (Exception ex) {
      log.debug("Failed to find user by email '{}': {}", email, ex.getMessage());
    }

    return null;
  }

  /**
   * 🆕 REALM MANAGEMENT: Create realm from JSON string
   */
  public void createRealm(String realmJson) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> realmConfig = objectMapper.readValue(realmJson, Map.class);
      createRealm(realmConfig);
    } catch (Exception ex) {
      log.error("❌ Failed to parse realm JSON", ex);
      throw new RuntimeException("Failed to parse realm configuration: " + ex.getMessage(), ex);
    }
  }

  /**
   * 🔍 REALM MANAGEMENT: Check if realm exists
   */
  public boolean realmExists(String realmName) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + realmName;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      return response.getStatusCode().is2xxSuccessful();

    } catch (Exception ex) {
      log.debug("Realm '{}' does not exist or is not accessible", realmName);
      return false;
    }
  }

  /**
   * 🔍 REALM MANAGEMENT: Get realm info
   */
  public Map<String, Object> getRealmInfo(String realmName) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + realmName;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        JsonNode realmInfo = objectMapper.readTree(response.getBody());

        return Map.of("realm", realmInfo.path("realm").asText(), "displayName",
            realmInfo.path("displayName").asText(""), "enabled",
            realmInfo.path("enabled").asBoolean(), "id", realmInfo.path("id").asText(),
            "registrationAllowed", realmInfo.path("registrationAllowed").asBoolean(),
            "loginWithEmailAllowed", realmInfo.path("loginWithEmailAllowed").asBoolean());
      }

      return Map.of();

    } catch (Exception ex) {
      log.error("❌ Failed to get realm info for {}: {}", realmName, ex.getMessage());
      return Map.of("error", ex.getMessage());
    }
  }

  /**
   * ✏️ REALM MANAGEMENT: Update realm display name
   */
  public void updateRealmDisplayName(String realmName, String displayName) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + realmName;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      // Update only displayName field
      Map<String, Object> updates = Map.of("displayName", displayName);
      String requestBody = objectMapper.writeValueAsString(updates);

      HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
      restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

      log.info("✅ Updated realm display name: {} -> {}", realmName, displayName);

    } catch (Exception ex) {
      log.error("❌ Failed to update realm display name for {}: {}", realmName, ex.getMessage());
      throw new RuntimeException("Failed to update realm display name: " + ex.getMessage(), ex);
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

  // =====================================================
  // ✅ V5 NOVÉ METODY - Role a Groups API
  // =====================================================

  /**
   * Získá roli podle ID (pro V5 sync)
   */
  public JsonNode getRoleById(String roleId) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/roles-by-id/" + roleId;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      return objectMapper.readTree(response.getBody());

    } catch (Exception ex) {
      log.error("Failed to get role by ID: {}", roleId, ex);
      return null;
    }
  }

  /**
   * 🔗 Získá KOMPOZITNÍ ROLE které jsou obsaženy v dané roli Endpoint: GET
   * /admin/realms/{realm}/roles-by-id/{role-id}/composites
   */
  public JsonNode getRoleComposites(String roleId) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/roles-by-id/" + roleId
          + "/composites";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode composites = objectMapper.readTree(response.getBody());
      log.debug("✅ Retrieved {} composite roles for roleId: {}", composites.size(), roleId);
      return composites;

    } catch (Exception ex) {
      log.warn("Failed to get role composites for roleId: {} - {}", roleId, ex.getMessage());
      return objectMapper.createArrayNode(); // Vrať prázdné pole
    }
  }

  /**
   * 🔗 Získá KOMPOZITNÍ ROLE podle role NAME Endpoint: GET
   * /admin/realms/{realm}/roles/{role-name}/composites
   */
  public JsonNode getRoleCompositesByName(String roleName) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/roles/" + roleName
          + "/composites";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode composites = objectMapper.readTree(response.getBody());
      log.debug("✅ Retrieved {} composite roles for role: {}", composites.size(), roleName);
      return composites;

    } catch (Exception ex) {
      log.warn("Failed to get role composites for role: {} - {}", roleName, ex.getMessage());
      return objectMapper.createArrayNode(); // Vrať prázdné pole
    }
  }

  /**
   * Získá skupinu podle ID (pro V5 sync)
   */
  public JsonNode getGroupById(String groupId) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/groups/" + groupId;

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      return objectMapper.readTree(response.getBody());

    } catch (Exception ex) {
      log.error("Failed to get group by ID: {}", groupId, ex);
      return null;
    }
  }

  /**
   * 📁 Získá CHILD GROUPS (subgroups) dané skupiny Endpoint: GET
   * /admin/realms/{realm}/groups/{group-id}/children
   */
  public JsonNode getGroupChildren(String groupId) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/groups/" + groupId
          + "/children";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode children = objectMapper.readTree(response.getBody());
      log.debug("✅ Retrieved {} child groups for groupId: {}", children.size(), groupId);
      return children;

    } catch (Exception ex) {
      log.warn("Failed to get group children for groupId: {} - {}", groupId, ex.getMessage());
      return objectMapper.createArrayNode(); // Vrať prázdné pole
    }
  }

  /**
   * 📁 Získá ROOT GROUPS (top-level skupiny bez parent) Endpoint: GET
   * /admin/realms/{realm}/groups?briefRepresentation=false
   */
  public JsonNode getRootGroups() {
    try {
      String adminToken = getSecureAdminToken();
      // briefRepresentation=false vrací plné detaily včetně subGroups
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm
          + "/groups?briefRepresentation=false";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode rootGroups = objectMapper.readTree(response.getBody());
      log.debug("✅ Retrieved {} root groups", rootGroups.size());
      return rootGroups;

    } catch (Exception ex) {
      log.error("Failed to get root groups", ex);
      return objectMapper.createArrayNode(); // Vrať prázdné pole
    }
  }

  /**
   * 👤 Získá EFFECTIVE ROLES uživatele (včetně kompozitních) Endpoint: GET
   * /admin/realms/{realm}/users/{user-id}/role-mappings/realm/composite
   */
  public JsonNode getUserEffectiveRoles(String userId) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/users/" + userId
          + "/role-mappings/realm/composite";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode effectiveRoles = objectMapper.readTree(response.getBody());
      log.debug("✅ Retrieved {} effective roles for userId: {}", effectiveRoles.size(), userId);
      return effectiveRoles;

    } catch (Exception ex) {
      log.warn("Failed to get effective roles for userId: {} - {}", userId, ex.getMessage());
      return objectMapper.createArrayNode(); // Vrať prázdné pole
    }
  }

  /**
   * Získá skupiny uživatele (pro V5 sync)
   */
  public JsonNode getUserGroups(String userId) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/users/" + userId
          + "/groups";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      return objectMapper.readTree(response.getBody());

    } catch (Exception ex) {
      log.error("Failed to get user groups for userId: {}", userId, ex);
      return objectMapper.createArrayNode(); // Vrať prázdné pole místo null
    }
  }

  // =====================================================
  // 🎭 EXTENDED ROLE MANAGEMENT - Composite Roles & Users
  // =====================================================

  /**
   * ✏️ UPDATE ROLE - Aktualizace role (název, popis)
   */
  public RoleDto updateRole(String roleName, RoleCreateRequest request) {
    try {
      String adminToken = getSecureAdminToken();

      // Get role ID first
      RoleDto existingRole = getRoleByName(roleName);

      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/roles-by-id/"
          + existingRole.getId();

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> roleUpdate = new HashMap<>();
      roleUpdate.put("name", request.getName());
      roleUpdate.put("description", request.getDescription());

      restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(roleUpdate, headers), Void.class);

      log.info("✅ Role updated: {} → {}", roleName, request.getName());

      // Return updated role
      return getRoleByName(request.getName());

    } catch (Exception ex) {
      log.error("❌ Failed to update role: {}", roleName, ex);
      throw new RuntimeException("Failed to update role: " + ex.getMessage(), ex);
    }
  }

  /**
   * 🔗 GET ROLE COMPOSITES LIST - Vrátí seznam child rolí jako RoleDto
   */
  public List<RoleDto> getRoleCompositesList(String roleName) {
    try {
      JsonNode composites = getRoleCompositesByName(roleName);
      List<RoleDto> roles = new ArrayList<>();

      for (JsonNode role : composites) {
        RoleDto roleDto = RoleDto.builder().id(role.path("id").asText())
            .name(role.path("name").asText()).description(role.path("description").asText())
            .composite(role.path("composite").asBoolean()).build();
        roles.add(roleDto);
      }

      return roles;

    } catch (Exception ex) {
      log.error("❌ Failed to get role composites: {}", roleName, ex);
      return new ArrayList<>();
    }
  }

  /**
   * ➕ ADD COMPOSITE ROLE - Přidá child role do parent composite role
   */
  public void addCompositeRole(String parentRoleName, String childRoleName) {
    try {
      String adminToken = getSecureAdminToken();

      // Get parent role
      RoleDto parentRole = getRoleByName(parentRoleName);
      RoleDto childRole = getRoleByName(childRoleName);

      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/roles-by-id/"
          + parentRole.getId() + "/composites";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      // Create role representation
      Map<String, Object> childRoleRep = new HashMap<>();
      childRoleRep.put("id", childRole.getId());
      childRoleRep.put("name", childRole.getName());

      List<Map<String, Object>> roles = List.of(childRoleRep);

      restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(roles, headers), Void.class);

      log.info("✅ Added composite role: {} → {}", parentRoleName, childRoleName);

    } catch (Exception ex) {
      log.error("❌ Failed to add composite role {} to {}", childRoleName, parentRoleName, ex);
      throw new RuntimeException("Failed to add composite role: " + ex.getMessage(), ex);
    }
  }

  /**
   * ➖ REMOVE COMPOSITE ROLE - Odebere child role z parent composite role
   */
  public void removeCompositeRole(String parentRoleName, String childRoleName) {
    try {
      String adminToken = getSecureAdminToken();

      // Get parent role
      RoleDto parentRole = getRoleByName(parentRoleName);
      RoleDto childRole = getRoleByName(childRoleName);

      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/roles-by-id/"
          + parentRole.getId() + "/composites";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      headers.setContentType(MediaType.APPLICATION_JSON);

      // Create role representation
      Map<String, Object> childRoleRep = new HashMap<>();
      childRoleRep.put("id", childRole.getId());
      childRoleRep.put("name", childRole.getName());

      List<Map<String, Object>> roles = List.of(childRoleRep);

      // Keycloak uses DELETE with body
      HttpEntity<List<Map<String, Object>>> requestEntity = new HttpEntity<>(roles, headers);
      restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, Void.class);

      log.info("✅ Removed composite role: {} ← {}", parentRoleName, childRoleName);

    } catch (Exception ex) {
      log.error("❌ Failed to remove composite role {} from {}", childRoleName, parentRoleName, ex);
      throw new RuntimeException("Failed to remove composite role: " + ex.getMessage(), ex);
    }
  }

  /**
   * 👥 GET USERS BY ROLE - Vrátí seznam uživatelů s danou rolí
   */
  public List<UserDto> getUsersByRole(String roleName) {
    try {
      String adminToken = getSecureAdminToken();

      // Get role ID
      RoleDto role = getRoleByName(roleName);

      String url = keycloakBaseUrl + "/admin/realms/" + targetRealm + "/roles-by-id/" + role.getId()
          + "/users";

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
        userList.add(userDto);
      }

      return userList;

    } catch (Exception ex) {
      log.error("❌ Failed to get users by role: {}", roleName, ex);
      return new ArrayList<>();
    }
  }

  /**
   * 🏢 GET ROLES BY TENANT - Vrátí seznam rolí pro konkrétní tenant (realm)
   */
  public List<RoleDto> getRolesByTenant(String tenantKey) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + tenantKey + "/roles";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode roles = objectMapper.readTree(response.getBody());
      List<RoleDto> roleList = new ArrayList<>();

      for (JsonNode role : roles) {
        // Skip default Keycloak roles
        String roleName = role.path("name").asText();
        if (roleName.startsWith("default-") || roleName.startsWith("offline_") 
            || roleName.equals("uma_authorization")) {
          continue;
        }

        RoleDto roleDto = RoleDto.builder()
            .id(role.path("id").asText())
            .name(roleName)
            .description(role.path("description").asText())
            .composite(role.path("composite").asBoolean())
            .build();
        roleList.add(roleDto);
      }

      log.info("✅ Found {} roles for tenant: {}", roleList.size(), tenantKey);
      return roleList;

    } catch (Exception ex) {
      log.error("❌ Failed to get roles for tenant: {}", tenantKey, ex);
      throw new RuntimeException("Failed to get roles for tenant: " + tenantKey, ex);
    }
  }

  /**
   * 👥 GET USERS BY TENANT - Vrátí seznam uživatelů pro konkrétní tenant (realm)
   */
  public List<UserDto> getUsersByTenant(String tenantKey) {
    try {
      String adminToken = getSecureAdminToken();
      String url = keycloakBaseUrl + "/admin/realms/" + tenantKey + "/users?max=1000";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode users = objectMapper.readTree(response.getBody());
      List<UserDto> userList = new ArrayList<>();

      for (JsonNode user : users) {
        UserDto userDto = UserDto.builder()
            .id(user.path("id").asText())
            .username(user.path("username").asText())
            .email(user.path("email").asText())
            .firstName(user.path("firstName").asText())
            .lastName(user.path("lastName").asText())
            .enabled(user.path("enabled").asBoolean())
            .emailVerified(user.path("emailVerified").asBoolean())
            .build();
        userList.add(userDto);
      }

      log.info("✅ Found {} users for tenant: {}", userList.size(), tenantKey);
      return userList;

    } catch (Exception ex) {
      log.error("❌ Failed to get users for tenant: {}", tenantKey, ex);
      throw new RuntimeException("Failed to get users for tenant: " + tenantKey, ex);
    }
  }

  /**
   * 👥 GET USERS BY ROLE AND TENANT - Vrátí uživatele s danou rolí v konkrétním tenantu
   */
  public List<UserDto> getUsersByRoleAndTenant(String roleName, String tenantKey) {
    try {
      String adminToken = getSecureAdminToken();

      // Get role from specific realm
      String roleUrl = keycloakBaseUrl + "/admin/realms/" + tenantKey + "/roles/" + roleName;
      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> roleResponse = restTemplate.exchange(roleUrl, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);
      JsonNode roleNode = objectMapper.readTree(roleResponse.getBody());
      String roleId = roleNode.path("id").asText();

      // Get users with this role
      String usersUrl = keycloakBaseUrl + "/admin/realms/" + tenantKey + "/roles-by-id/" + roleId + "/users";
      ResponseEntity<String> response = restTemplate.exchange(usersUrl, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);

      JsonNode users = objectMapper.readTree(response.getBody());
      List<UserDto> userList = new ArrayList<>();

      for (JsonNode user : users) {
        UserDto userDto = UserDto.builder()
            .id(user.path("id").asText())
            .username(user.path("username").asText())
            .email(user.path("email").asText())
            .firstName(user.path("firstName").asText())
            .lastName(user.path("lastName").asText())
            .enabled(user.path("enabled").asBoolean())
            .emailVerified(user.path("emailVerified").asBoolean())
            .build();
        userList.add(userDto);
      }

      log.info("✅ Found {} users with role {} in tenant {}", userList.size(), roleName, tenantKey);
      return userList;

    } catch (Exception ex) {
      log.error("❌ Failed to get users by role {} for tenant {}", roleName, tenantKey, ex);
      return new ArrayList<>();
    }
  }

  /**
   * ➕ ADD ROLE TO USER IN TENANT - Přidá roli uživateli v konkrétním tenantu
   */
  public void addRoleToUser(String userId, String roleName, String tenantKey) {
    try {
      String adminToken = getSecureAdminToken();

      // Get role from specific realm
      String roleUrl = keycloakBaseUrl + "/admin/realms/" + tenantKey + "/roles/" + roleName;
      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> roleResponse = restTemplate.exchange(roleUrl, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);
      JsonNode roleNode = objectMapper.readTree(roleResponse.getBody());

      Map<String, Object> roleRep = new HashMap<>();
      roleRep.put("id", roleNode.path("id").asText());
      roleRep.put("name", roleNode.path("name").asText());

      // Add role to user
      String url = keycloakBaseUrl + "/admin/realms/" + tenantKey + "/users/" + userId + "/role-mappings/realm";
      headers.setContentType(MediaType.APPLICATION_JSON);

      restTemplate.exchange(url, HttpMethod.POST, 
          new HttpEntity<>(List.of(roleRep), headers), Void.class);

      log.info("✅ Added role {} to user {} in tenant {}", roleName, userId, tenantKey);

    } catch (Exception ex) {
      log.error("❌ Failed to add role {} to user {} in tenant {}", roleName, userId, tenantKey, ex);
      throw new RuntimeException("Failed to add role to user: " + ex.getMessage(), ex);
    }
  }

  /**
   * ➖ REMOVE ROLE FROM USER IN TENANT - Odebere roli uživateli v konkrétním tenantu
   */
  public void removeRoleFromUser(String userId, String roleName, String tenantKey) {
    try {
      String adminToken = getSecureAdminToken();

      // Get role from specific realm
      String roleUrl = keycloakBaseUrl + "/admin/realms/" + tenantKey + "/roles/" + roleName;
      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      ResponseEntity<String> roleResponse = restTemplate.exchange(roleUrl, HttpMethod.GET,
          new HttpEntity<>(headers), String.class);
      JsonNode roleNode = objectMapper.readTree(roleResponse.getBody());

      Map<String, Object> roleRep = new HashMap<>();
      roleRep.put("id", roleNode.path("id").asText());
      roleRep.put("name", roleNode.path("name").asText());

      // Remove role from user
      String url = keycloakBaseUrl + "/admin/realms/" + tenantKey + "/users/" + userId + "/role-mappings/realm";
      headers.setContentType(MediaType.APPLICATION_JSON);

      // Keycloak uses DELETE with body
      restTemplate.exchange(url, HttpMethod.DELETE, 
          new HttpEntity<>(List.of(roleRep), headers), Void.class);

      log.info("✅ Removed role {} from user {} in tenant {}", roleName, userId, tenantKey);

    } catch (Exception ex) {
      log.error("❌ Failed to remove role {} from user {} in tenant {}", roleName, userId, tenantKey, ex);
      throw new RuntimeException("Failed to remove role from user: " + ex.getMessage(), ex);
    }
  }
}
