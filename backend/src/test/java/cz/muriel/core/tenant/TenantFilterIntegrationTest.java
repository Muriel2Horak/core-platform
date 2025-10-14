package cz.muriel.core.tenant;

import cz.muriel.core.entity.Tenant;
import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.repository.TenantRepository;
import cz.muriel.core.repository.UserDirectoryRepository;
import cz.muriel.core.service.UserDirectoryService;
import cz.muriel.core.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TenantFilterIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private UserDirectoryRepository userDirectoryRepository;

  @Autowired
  private UserDirectoryService userDirectoryService;

  private String tenant1Key;
  private String tenant2Key;

  @BeforeEach
  void setUp() {
    // Create test tenants - 🎯 SIMPLIFIED: using only key field
    Tenant tenant1 = Tenant.builder().key("tenant1").build();
    tenant1 = tenantRepository.save(tenant1);
    tenant1Key = tenant1.getKey();

    Tenant tenant2 = Tenant.builder().key("tenant2").build();
    tenant2 = tenantRepository.save(tenant2);
    tenant2Key = tenant2.getKey();

    // Create users for tenant 1 (set context first)
    TenantContext.setTenantKey(tenant1Key);
    createUser("user1-t1", "user1@tenant1.com", tenant1.getId());
    createUser("user2-t1", "user2@tenant1.com", tenant1.getId());
    TenantContext.clear();

    // Create users for tenant 2
    TenantContext.setTenantKey(tenant2Key);
    createUser("user1-t2", "user1@tenant2.com", tenant2.getId());
    createUser("user2-t2", "user2@tenant2.com", tenant2.getId());
    TenantContext.clear();
  }

  @Test
  void shouldFilterUsersByTenant() {
    // Given - Set tenant context first
    TenantContext.setTenantKey(tenant1Key);

    try {
      // When - Query users (AOP will enable tenant filter)
      List<UserDirectoryEntity> users = userDirectoryService.findAll();

      // Then - Should only see tenant1 users
      assertThat(users).hasSize(2); // Only tenant1 users from setUp
      assertThat(users).allMatch(user -> user.getTenantId() != null);
      assertThat(users).extracting(UserDirectoryEntity::getUsername).containsExactlyInAnyOrder(
          "user1-t1", "user2-t1");

    } finally {
      TenantContext.clear();
    }
  }

  @Test
  void shouldFilterUsersByDifferentTenant() {
    // Given - Set tenant context to tenant2
    TenantContext.setTenantKey(tenant2Key);

    try {
      // When - filter by tenant2
      List<UserDirectoryEntity> tenant2Users = userDirectoryService.findAll();

      // Then - should only see tenant2 users
      assertThat(tenant2Users).hasSize(2); // user1-t2 and user2-t2 from setUp

    } finally {
      TenantContext.clear();
    }
  }

  @Test
  void shouldNotReturnUsersFromOtherTenant() {
    // Given - Set tenant context to tenant 2
    TenantContext.setTenantKey(tenant2Key);

    try {
      // When - Query users (should not see tenant1 users due to RLS)
      List<UserDirectoryEntity> users = userDirectoryService.findAll();

      // Then - should not see users from tenant1, only tenant2
      assertThat(users).hasSize(2); // Only tenant2 users from setUp
      assertThat(users).allMatch(user -> user.getTenantId() != null);

    } finally {
      TenantContext.clear();
    }
  }

  @Test
  void shouldFindUserByUsernameWithinTenant() {
    // Given - Set tenant context to tenant 1
    TenantContext.setTenantKey(tenant1Key);

    try {
      // When - Find user by username
      var user = userDirectoryService.findByUsername("user1-t1");

      // Then - Should find the user in tenant 1
      assertThat(user).isPresent();
      assertThat(user.get().getUsername()).isEqualTo("user1-t1");
      assertThat(user.get().getTenantId()).isNotNull();

    } finally {
      TenantContext.clear();
    }
  }

  @Test
  void shouldNotFindUserFromDifferentTenant() {
    // Given - Set tenant context to tenant 1
    TenantContext.setTenantKey(tenant1Key);

    try {
      // When - Try to find user from tenant 2
      var user = userDirectoryService.findByUsername("user1-t2");

      // Then - Should not find the user (RLS blocks it)
      assertThat(user).isEmpty();

    } finally {
      TenantContext.clear();
    }
  }

  @Test
  void shouldCreateUserInCurrentTenant() {
    // Given - Set tenant context to tenant 2
    TenantContext.setTenantKey(tenant2Key);

    try {
      // When - Create new user
      UserDirectoryEntity newUser = UserDirectoryEntity.builder().username("new-user")
          .email("new@tenant2.com").firstName("New").lastName("User").build();

      UserDirectoryEntity savedUser = userDirectoryService.createOrUpdate(newUser);

      // Then - Should be created in tenant 2
      assertThat(savedUser.getTenantId()).isNotNull();
      assertThat(savedUser.getUsername()).isEqualTo("new-user");

    } finally {
      TenantContext.clear();
    }
  }

  private void createUser(String username, String email, java.util.UUID tenantId) {
    // Note: tenantId must be set explicitly in tests
    UserDirectoryEntity user = UserDirectoryEntity.builder()
        .id(java.util.UUID.randomUUID()) // ID must be set manually
        .tenantId(tenantId) // Set tenant ID explicitly
        .username(username)
        .email(email)
        .firstName("Test")
        .lastName("User")
        .build();
    userDirectoryRepository.save(user);
  }
}
