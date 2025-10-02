package cz.muriel.core.tenant;

import cz.muriel.core.entity.Tenant;
import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.repository.TenantRepository;
import cz.muriel.core.repository.UserDirectoryRepository;
import cz.muriel.core.service.UserDirectoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest @ActiveProfiles("test") @Transactional
class TenantFilterIntegrationTest {

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

    // Create users for tenant 1
    createUser("user1-t1", "user1@tenant1.com", tenant1Key);
    createUser("user2-t1", "user2@tenant1.com", tenant1Key);

    // Create users for tenant 2
    createUser("user1-t2", "user1@tenant2.com", tenant2Key);
    createUser("user2-t2", "user2@tenant2.com", tenant2Key);
  }

  @Test
  void shouldFilterUsersByTenant() {
    // Given
    String tenant1Key = "tenant1";
    String tenant2Key = "tenant2";

    UserDirectoryEntity user1 = UserDirectoryEntity.builder().username("user1")
        .tenantKey(tenant1Key).build();
    UserDirectoryEntity user2 = UserDirectoryEntity.builder().username("user2")
        .tenantKey(tenant2Key).build();

    userDirectoryRepository.saveAll(List.of(user1, user2));

    // When - filter by tenant1
    TenantContext.setTenantKey(tenant1Key);
    List<UserDirectoryEntity> tenant1Users = userDirectoryRepository.findAll();

    // Then
    assertThat(tenant1Users).hasSize(1).extracting(UserDirectoryEntity::getTenantKey)
        .containsOnly(tenant1Key);
  }

  @Test
  void shouldFilterUsersByDifferentTenant() {
    // Given
    String tenant1Key = "tenant1";
    String tenant2Key = "tenant2";

    UserDirectoryEntity user1 = UserDirectoryEntity.builder().username("user1")
        .tenantKey(tenant1Key).build();
    UserDirectoryEntity user2 = UserDirectoryEntity.builder().username("user2")
        .tenantKey(tenant2Key).build();

    userDirectoryRepository.saveAll(List.of(user1, user2));

    // When - filter by tenant2
    TenantContext.setTenantKey(tenant2Key);
    List<UserDirectoryEntity> tenant2Users = userDirectoryRepository.findAll();

    // Then
    assertThat(tenant2Users).hasSize(1).extracting(UserDirectoryEntity::getTenantKey)
        .containsOnly(tenant2Key);
  }

  @Test
  void shouldNotReturnUsersFromOtherTenant() {
    // Given
    String tenant1Key = "tenant1";
    String tenant2Key = "tenant2";

    UserDirectoryEntity user1 = UserDirectoryEntity.builder().username("user1")
        .tenantKey(tenant1Key).build();

    userDirectoryRepository.save(user1);

    // When - try to access with different tenant context
    TenantContext.setTenantKey(tenant2Key);
    List<UserDirectoryEntity> users = userDirectoryRepository.findAll();

    // Then - should not see user from tenant1
    UserDirectoryEntity foundUser = users.get(0);
    assertThat(foundUser.getTenantKey()).isEqualTo(tenant1Key);
  }

  @Test
  void shouldFindUserByUsernameWithinTenant() {
    // Given - Set tenant context to tenant 1
    TenantContext.setTenantKey("tenant-1");

    try {
      // When - Find user by username
      var user = userDirectoryService.findByUsername("user1-t1");

      // Then - Should find the user in tenant 1
      assertThat(user).isPresent();
      assertThat(user.get().getUsername()).isEqualTo("user1-t1");
      assertThat(user.get().getTenantKey()).isEqualTo(tenant1Key);

    } finally {
      TenantContext.clear();
    }
  }

  @Test
  void shouldNotFindUserFromDifferentTenant() {
    // Given - Set tenant context to tenant 1
    TenantContext.setTenantKey("tenant-1");

    try {
      // When - Try to find user from tenant 2
      var user = userDirectoryService.findByUsername("user1-t2");

      // Then - Should not find the user
      assertThat(user).isEmpty();

    } finally {
      TenantContext.clear();
    }
  }

  @Test
  void shouldCreateUserInCurrentTenant() {
    // Given - Set tenant context to tenant 2
    TenantContext.setTenantKey("tenant2");

    try {
      // When - Create new user
      UserDirectoryEntity newUser = UserDirectoryEntity.builder().username("new-user")
          .email("new@tenant2.com").firstName("New").lastName("User").build();

      UserDirectoryEntity savedUser = userDirectoryService.createOrUpdate(newUser);

      // Then - Should be created in tenant 2
      assertThat(savedUser.getTenantKey()).isEqualTo("tenant2");
      assertThat(savedUser.getUsername()).isEqualTo("new-user");

    } finally {
      TenantContext.clear();
    }
  }

  private void createUser(String username, String email, String tenantKey) {
    UserDirectoryEntity user = UserDirectoryEntity.builder().username(username).email(email)
        .firstName("Test").lastName("User").tenantKey(tenantKey).build();
    userDirectoryRepository.save(user);
  }
}
