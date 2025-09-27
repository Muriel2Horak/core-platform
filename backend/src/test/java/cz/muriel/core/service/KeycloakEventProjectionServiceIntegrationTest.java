package cz.muriel.core.service;

import cz.muriel.core.dto.KeycloakWebhookEventDto;
import cz.muriel.core.entity.Tenant;
import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.repository.TenantRepository;
import cz.muriel.core.repository.UserDirectoryRepository;
import cz.muriel.core.repository.KeycloakEventLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest @ActiveProfiles("test") @Transactional
class KeycloakEventProjectionServiceIntegrationTest {

  @Autowired
  private KeycloakEventProjectionService projectionService;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private UserDirectoryRepository userDirectoryRepository;

  @Autowired
  private KeycloakEventLogRepository eventLogRepository;

  private Tenant testTenant;

  @BeforeEach
  void setUp() {
    // Create test tenant
    testTenant = Tenant.builder().key("test-tenant").name("Test Tenant").realm("core-platform")
        .build();
    testTenant = tenantRepository.save(testTenant);
  }

  @Test
  void shouldCreateNewUserFromUserCreatedEvent() {
    // Given
    KeycloakWebhookEventDto event = createTestEvent("USER_CREATED");

    // When
    projectionService.processEvent(event);

    // Then
    Optional<UserDirectoryEntity> user = userDirectoryRepository
        .findByKeycloakUserId("kc-user-123");
    assertThat(user).isPresent();
    assertThat(user.get().getUsername()).isEqualTo("testuser");
    assertThat(user.get().getEmail()).isEqualTo("test@example.com");
    assertThat(user.get().getFirstName()).isEqualTo("Test");
    assertThat(user.get().getLastName()).isEqualTo("User");
    assertThat(user.get().getActive()).isTrue();
    assertThat(user.get().getTenantId()).isEqualTo(testTenant.getId());
    assertThat(user.get().getDisplayName()).isEqualTo("Test User");
  }

  @Test
  void shouldUpdateExistingUserFromUserUpdatedEvent() {
    // Given - create existing user
    UserDirectoryEntity existingUser = UserDirectoryEntity.builder().tenantId(testTenant.getId())
        .keycloakUserId("kc-user-123").username("testuser").email("old@example.com")
        .firstName("Old").lastName("Name").active(true).build();
    userDirectoryRepository.save(existingUser);

    KeycloakWebhookEventDto event = createTestEvent("USER_UPDATED");
    event.setEmail("updated@example.com");
    event.setFirstName("Updated");

    // When
    projectionService.processEvent(event);

    // Then
    Optional<UserDirectoryEntity> user = userDirectoryRepository
        .findByKeycloakUserId("kc-user-123");
    assertThat(user).isPresent();
    assertThat(user.get().getEmail()).isEqualTo("updated@example.com");
    assertThat(user.get().getFirstName()).isEqualTo("Updated");
    assertThat(user.get().getLastName()).isEqualTo("User"); // Updated from event
    assertThat(user.get().getDisplayName()).isEqualTo("Updated User");
  }

  @Test
  void shouldSoftDeleteUserFromUserDeletedEvent() {
    // Given - create existing user
    UserDirectoryEntity existingUser = UserDirectoryEntity.builder().tenantId(testTenant.getId())
        .keycloakUserId("kc-user-123").username("testuser").email("test@example.com").active(true)
        .build();
    userDirectoryRepository.save(existingUser);

    KeycloakWebhookEventDto event = createTestEvent("USER_DELETED");

    // When
    projectionService.processEvent(event);

    // Then
    Optional<UserDirectoryEntity> user = userDirectoryRepository
        .findByKeycloakUserId("kc-user-123");
    assertThat(user).isPresent();
    assertThat(user.get().getActive()).isFalse();
    assertThat(user.get().getDeletedAt()).isNotNull();
    assertThat(user.get().getDeletedAt()).isBefore(LocalDateTime.now().plusMinutes(1));
  }

  @Test
  void shouldStoreRolesAndGroupsAsJson() {
    // Given
    KeycloakWebhookEventDto event = createTestEvent("USER_UPDATED");
    event.setRoles(Map.of("realm", List.of("CORE_ROLE_USER", "CORE_ROLE_ADMIN"), "clients",
        Map.of("api", List.of("read", "write"))));
    event.setGroups(List.of("/admin", "/users/managers"));

    // When
    projectionService.processEvent(event);

    // Then
    Optional<UserDirectoryEntity> user = userDirectoryRepository
        .findByKeycloakUserId("kc-user-123");
    assertThat(user).isPresent();
    assertThat(user.get().getRolesJson()).contains("CORE_ROLE_USER");
    assertThat(user.get().getRolesJson()).contains("CORE_ROLE_ADMIN");
    assertThat(user.get().getGroupsJson()).contains("/admin");
    assertThat(user.get().getGroupsJson()).contains("/users/managers");
  }

  @Test
  void shouldStoreUserAttributes() {
    // Given
    KeycloakWebhookEventDto event = createTestEvent("USER_UPDATED");
    event.setAttributes(Map.of("phoneNumber", "+420123456789", "department", "Engineering", "title",
        "Senior Developer"));

    // When
    projectionService.processEvent(event);

    // Then
    Optional<UserDirectoryEntity> user = userDirectoryRepository
        .findByKeycloakUserId("kc-user-123");
    assertThat(user).isPresent();
    assertThat(user.get().getPhoneNumber()).isEqualTo("+420123456789");
    assertThat(user.get().getDepartment()).isEqualTo("Engineering");
    assertThat(user.get().getTitle()).isEqualTo("Senior Developer");
  }

  @Test
  void shouldBeIdempotent() {
    // Given
    KeycloakWebhookEventDto event = createTestEvent("USER_CREATED");

    // When - process same event twice
    projectionService.processEvent(event);
    projectionService.processEvent(event);

    // Then - should have only one user and one event log entry
    List<UserDirectoryEntity> users = userDirectoryRepository.findAll();
    assertThat(users).hasSize(1);

    long eventLogCount = eventLogRepository.count();
    assertThat(eventLogCount).isEqualTo(1);
  }

  @Test
  void shouldIgnoreUnknownEventTypes() {
    // Given
    KeycloakWebhookEventDto event = createTestEvent("UNKNOWN_EVENT");

    // When
    projectionService.processEvent(event);

    // Then - no user should be created
    List<UserDirectoryEntity> users = userDirectoryRepository.findAll();
    assertThat(users).isEmpty();

    // But event should still be logged for idempotence
    long eventLogCount = eventLogRepository.count();
    assertThat(eventLogCount).isEqualTo(1);
  }

  @Test
  void shouldIgnoreEventForNonExistentTenant() {
    // Given
    KeycloakWebhookEventDto event = createTestEvent("USER_CREATED");
    event.setTenantKey("non-existent-tenant");

    // When
    projectionService.processEvent(event);

    // Then
    List<UserDirectoryEntity> users = userDirectoryRepository.findAll();
    assertThat(users).isEmpty();
  }

  private KeycloakWebhookEventDto createTestEvent(String eventType) {
    KeycloakWebhookEventDto event = new KeycloakWebhookEventDto();
    event.setEventType(eventType);
    event.setTime(System.currentTimeMillis());
    event.setRealmId(testTenant.getId().toString());
    event.setRealmName(testTenant.getRealm());
    event.setTenantKey(testTenant.getKey());
    event.setUserId("kc-user-123");
    event.setUsername("testuser");
    event.setEmail("test@example.com");
    event.setFirstName("Test");
    event.setLastName("User");
    event.setEnabled(true);
    return event;
  }
}
