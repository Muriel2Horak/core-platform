package cz.muriel.core.service;

import cz.muriel.core.dto.RoleCreateRequest;
import cz.muriel.core.dto.UserCreateRequest;
import cz.muriel.core.dto.UserDto;
import cz.muriel.core.dto.UserUpdateRequest;
import cz.muriel.core.auth.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j @Service @RequiredArgsConstructor
public class KeycloakInitializationService implements ApplicationRunner {

  private final KeycloakAdminService keycloakAdminService;

  @Value("${keycloak.init.admin.username:CORE_SYSTEM_ADMIN}")
  private String systemAdminUsername;

  @Value("${keycloak.init.admin.email:system@core.local}")
  private String systemAdminEmail;

  @Value("${keycloak.init.admin.password:}")
  private String systemAdminPassword;

  @Value("${keycloak.init.admin.first-name:System}")
  private String systemAdminFirstName;

  @Value("${keycloak.init.admin.last-name:Administrator}")
  private String systemAdminLastName;

  private static final String CORE_USER_ADMIN_ROLE = "CORE_ROLE_ADMIN";

  @Override
  public void run(ApplicationArguments args) throws Exception {
    log.info("Starting Keycloak initialization...");

    try {
      // 1. Ensure CORE_USER_ADMIN role exists
      ensureAdminRoleExists();

      // 2. Ensure system administrator user exists
      ensureSystemAdminExists();

      // 3. Ensure backend service account has proper admin roles
      ensureServiceAccountRoles();

      // 4. Initialize demo organizational data
      initializeDemoData();

      log.info("Keycloak initialization completed successfully");
    } catch (Exception e) {
      log.error("Failed to initialize Keycloak setup", e);
      // Don't fail application startup, but log the error
    }
  }

  private void ensureAdminRoleExists() {
    try {
      // Try to get the role first
      keycloakAdminService.getRoleByName(CORE_USER_ADMIN_ROLE);
      log.info("Role {} already exists", CORE_USER_ADMIN_ROLE);
    } catch (RuntimeException e) {
      if (e.getMessage().contains("Role not found")) {
        log.info("Creating role: {}", CORE_USER_ADMIN_ROLE);

        RoleCreateRequest roleRequest = new RoleCreateRequest();
        roleRequest.setName(CORE_USER_ADMIN_ROLE);
        roleRequest.setDescription("Core platform user administrator role");

        keycloakAdminService.createRole(roleRequest);
        log.info("Role {} created successfully", CORE_USER_ADMIN_ROLE);
      } else {
        log.error("Failed to check if role exists", e);
        throw e;
      }
    }
  }

  private void ensureSystemAdminExists() {
    try {
      // Try to find existing system admin
      UserDto existingAdmin = keycloakAdminService.findUserByUsername(systemAdminUsername);

      if (existingAdmin != null) {
        log.info("System admin user {} already exists", systemAdminUsername);

        // Ensure the user has the admin role
        if (!existingAdmin.getRoles().contains(CORE_USER_ADMIN_ROLE)) {
          log.info("Assigning {} role to existing system admin", CORE_USER_ADMIN_ROLE);
          keycloakAdminService.assignRoleToUser(existingAdmin.getId(), CORE_USER_ADMIN_ROLE);
        }

        return;
      }

      // Create system admin user
      log.info("Creating system admin user: {}", systemAdminUsername);

      String password = generateSystemAdminPassword();

      UserCreateRequest userRequest = new UserCreateRequest();
      userRequest.setUsername(systemAdminUsername);
      userRequest.setEmail(systemAdminEmail);
      userRequest.setFirstName(systemAdminFirstName);
      userRequest.setLastName(systemAdminLastName);
      userRequest.setTemporaryPassword(password);
      userRequest.setEnabled(true);
      userRequest.setRequirePasswordChange(true);

      UserDto createdAdmin = keycloakAdminService.createUser(userRequest);

      // Assign admin role
      keycloakAdminService.assignRoleToUser(createdAdmin.getId(), CORE_USER_ADMIN_ROLE);

      log.info("System admin user {} created successfully", systemAdminUsername);
      log.warn("IMPORTANT: System admin temporary password: {}", password);
      log.warn("IMPORTANT: Please change this password on first login!");

    } catch (Exception e) {
      log.error("Failed to ensure system admin exists", e);
      throw new RuntimeException("Failed to create system administrator", e);
    }
  }

  private String generateSystemAdminPassword() {
    if (systemAdminPassword != null && !systemAdminPassword.trim().isEmpty()) {
      return systemAdminPassword.trim();
    }

    // Generate a secure random password
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    SecureRandom random = new SecureRandom();
    StringBuilder password = new StringBuilder();

    for (int i = 0; i < 16; i++) {
      password.append(chars.charAt(random.nextInt(chars.length())));
    }

    return password.toString();
  }

  /**
   * Automaticky přiřadí admin role backend service accountu
   */
  private void ensureServiceAccountRoles() {
    try {
      log.info("Ensuring backend service account has proper admin roles...");

      // Najdi service account uživatele pro backend-admin-service klienta
      // Service account uživatel má username ve formátu "service-account-{clientId}"
      String serviceAccountUsername = "service-account-backend-admin-service";

      UserDto serviceAccount = keycloakAdminService.findUserByUsername(serviceAccountUsername);

      if (serviceAccount == null) {
        log.warn("Service account user {} not found. Attempting to create it...",
            serviceAccountUsername);

        // Pokus se vytvořit service account uživatele
        // Toto je backup řešení pro případy, kdy import realmu nevytvoří service
        // account
        try {
          serviceAccount = createServiceAccountUser(serviceAccountUsername);
          log.info("Successfully created service account user: {}", serviceAccountUsername);
        } catch (Exception e) {
          log.error("Failed to create service account user: {}", e.getMessage());
          log.warn(
              "Service account configuration skipped. Please check Keycloak client configuration.");
          return;
        }
      }

      log.info("Found service account user: {} (ID: {})", serviceAccount.getUsername(),
          serviceAccount.getId());

      // Seznam potřebných admin rolí pro Keycloak Admin API
      String[] requiredRoles = { "manage-users", "view-users", "view-realm", "manage-realm" };

      for (String roleName : requiredRoles) {
        try {
          assignClientRoleToServiceAccount(serviceAccount.getId(), "realm-management", roleName);
        } catch (Exception e) {
          log.warn("Failed to assign role {} to service account: {}", roleName, e.getMessage());
        }
      }

      log.info("Service account roles configuration completed");

    } catch (Exception e) {
      log.error("Failed to ensure service account roles", e);
      // Don't fail application startup
    }
  }

  /**
   * 🎯 DEMO DATA INITIALIZATION - vytvoří demo organizační strukturu
   */
  private void initializeDemoData() {
    try {
      log.info("🎯 Initializing demo organizational data...");

      // Najdi test_admin (nadřízený)
      UserDto testAdmin = keycloakAdminService.findUserByUsername("test_admin");
      if (testAdmin == null) {
        log.warn("test_admin user not found, skipping demo data initialization");
        return;
      }

      // Najdi test (podřízený)
      UserDto testUser = keycloakAdminService.findUserByUsername("test");
      if (testUser == null) {
        log.warn("test user not found, skipping demo data initialization");
        return;
      }

      // Aktualizuj test_admin s organizačními daty
      log.info("🏢 Setting up test_admin as department manager...");
      UserUpdateRequest adminUpdate = new UserUpdateRequest();
      adminUpdate.setDepartment("IT - Vývoj");
      adminUpdate.setPosition("Vedoucí vývojového týmu");
      adminUpdate.setCostCenter("IT-001");
      adminUpdate.setLocation("Praha - Karlín");
      adminUpdate.setPhone("+420 123 456 789");

      keycloakAdminService.updateUser(testAdmin.getId(), adminUpdate);
      log.info("✅ test_admin organizational data updated");

      // Aktualizuj test jako podřízeného test_admina
      log.info("👥 Setting up test as subordinate of test_admin...");
      UserUpdateRequest userUpdate = new UserUpdateRequest();
      userUpdate.setDepartment("IT - Vývoj");
      userUpdate.setPosition("Senior Developer");
      userUpdate.setManager("test_admin"); // Klíčové propojení!
      userUpdate.setCostCenter("IT-001");
      userUpdate.setLocation("Praha - Karlín");
      userUpdate.setPhone("+420 987 654 321");

      // Přidej zástupství (test_admin zastupuje test při dovolené)
      userUpdate.setDeputy("test_admin");
      userUpdate.setDeputyReason("dovolená");
      userUpdate.setDeputyFrom(java.time.LocalDate.now().plusDays(30)); // Dovolená za měsíc
      userUpdate.setDeputyTo(java.time.LocalDate.now().plusDays(44)); // 2 týdny dovolené

      keycloakAdminService.updateUser(testUser.getId(), userUpdate);
      log.info("✅ test user organizational data updated with manager relationship");

      // Výpis vytvořené hierarchie
      log.info("🎯 Demo organizational hierarchy created:");
      log.info("   👨‍💼 test_admin (Vedoucí vývojového týmu) - IT Vývoj");
      log.info("   └── 👨‍💻 test (Senior Developer) - podřízený test_admina");
      log.info("   📅 Plánované zastupování: test_admin zastupuje test při dovolené");

    } catch (Exception e) {
      log.warn("Failed to initialize demo data: {}", e.getMessage());
      // Don't fail application startup
    }
  }

  /**
   * Vytvoří service account uživatele pro backend-admin-service klienta Toto je
   * backup řešení pro případy, kdy import realmu nevytvoří service account
   * automaticky
   */
  private UserDto createServiceAccountUser(String serviceAccountUsername) {
    try {
      log.info("Creating service account user: {}", serviceAccountUsername);

      UserCreateRequest userRequest = new UserCreateRequest();
      userRequest.setUsername(serviceAccountUsername);
      userRequest.setEmail(serviceAccountUsername + "@service.local");
      userRequest.setFirstName("Backend");
      userRequest.setLastName("Service Account");
      userRequest.setEnabled(true);
      userRequest.setRequirePasswordChange(false);

      UserDto createdUser = keycloakAdminService.createUser(userRequest);
      log.info("Service account user created with ID: {}", createdUser.getId());

      return createdUser;

    } catch (Exception e) {
      log.error("Failed to create service account user: {}", e.getMessage());
      throw new RuntimeException("Could not create service account user", e);
    }
  }

  /**
   * Přiřadí client role service accountu (rozšíření pro realm-management role)
   */
  private void assignClientRoleToServiceAccount(String userId, String clientId, String roleName) {
    try {
      keycloakAdminService.assignClientRoleToUser(userId, clientId, roleName);
      log.info("Successfully assigned {} role from {} client to service account user {}", roleName,
          clientId, userId);
    } catch (Exception e) {
      log.error("Failed to assign {} role from {} client to user {}: {}", roleName, clientId,
          userId, e.getMessage());
      throw e;
    }
  }
}
