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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest @ActiveProfiles("test") @Transactional
class TenantFilterIntegrationTest {

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private UserDirectoryRepository userDirectoryRepository;

  @Autowired
  private UserDirectoryService userDirectoryService;

  private UUID tenant1Id;
  private UUID tenant2Id;

  @BeforeEach
  void setUp() {
    // Create test tenants - 🎯 SIMPLIFIED: using only key field
    Tenant tenant1 = Tenant.builder().key("tenant1").build();
    tenant1 = tenantRepository.save(tenant1);
    tenant1Id = tenant1.getId();

    Tenant tenant2 = Tenant.builder().key("tenant2").build();
    tenant2 = tenantRepository.save(tenant2);
    tenant2Id = tenant2.getId();

    // Create users for tenant 1
    createUser("user1-t1", "user1@tenant1.com", tenant1Id);
    createUser("user2-t1", "user2@tenant1.com", tenant1Id);

    // Create users for tenant 2
    createUser("user1-t2", "user1@tenant2.com", tenant2Id);
    createUser("user2-t2", "user2@tenant2.com", tenant2Id);
  }

  @Test
  void shouldFilterUsersByTenant() {
    // Given - Set tenant context to tenant 1
    TenantContext.setTenantKey("tenant-1");

    try {
      // When - Search all users
      List<UserDirectoryEntity> users = userDirectoryService.search("");

      // Then - Should only return tenant 1 users
      assertThat(users).hasSize(2);
      assertThat(users).extracting(UserDirectoryEntity::getUsername)
          .containsExactlyInAnyOrder("user1-t1", "user2-t1");
      assertThat(users).extracting(UserDirectoryEntity::getTenantId).containsOnly(tenant1Id);

    } finally {
      TenantContext.clear();
    }
  }

  @Test
  void shouldFilterUsersByDifferentTenant() {
    // Given - Set tenant context to tenant 2
    TenantContext.setTenantKey("tenant-2");

    try {
      // When - Search all users
      List<UserDirectoryEntity> users = userDirectoryService.search("");

      // Then - Should only return tenant 2 users
      assertThat(users).hasSize(2);
      assertThat(users).extracting(UserDirectoryEntity::getUsername)
          .containsExactlyInAnyOrder("user1-t2", "user2-t2");
      assertThat(users).extracting(UserDirectoryEntity::getTenantId).containsOnly(tenant2Id);

    } finally {
      TenantContext.clear();
    }
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
      assertThat(user.get().getTenantId()).isEqualTo(tenant1Id);

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
    TenantContext.setTenantKey("tenant-2");

    try {
      // When - Create new user
      UserDirectoryEntity newUser = UserDirectoryEntity.builder().username("new-user")
          .email("new@tenant2.com").firstName("New").lastName("User").build();

      UserDirectoryEntity savedUser = userDirectoryService.createOrUpdate(newUser);

      // Then - Should be created in tenant 2
      assertThat(savedUser.getTenantId()).isEqualTo(tenant2Id);
      assertThat(savedUser.getUsername()).isEqualTo("new-user");

    } finally {
      TenantContext.clear();
    }
  }

  private void createUser(String username, String email, UUID tenantId) {
    UserDirectoryEntity user = UserDirectoryEntity.builder().username(username).email(email)
        .firstName("Test").lastName("User").tenantId(tenantId).build();
    userDirectoryRepository.save(user);
  }
}
