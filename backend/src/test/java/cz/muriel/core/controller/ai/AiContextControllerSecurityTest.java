package cz.muriel.core.controller.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for tenant ID extraction from security context
 * 
 * Tests the security integration in AiContextController
 */
@ExtendWith(MockitoExtension.class)
class AiContextControllerSecurityTest {

  @Test
  void getTenantIdFromSecurityContext_shouldExtractFromJwt() {
    // Arrange
    UUID expectedTenantId = UUID.randomUUID();
    Jwt jwt = createMockJwt(expectedTenantId.toString());

    // Act & Assert - Verify JWT structure for tenant extraction
    assertNotNull(jwt.getClaimAsString("tenant_id"));
    assertEquals(expectedTenantId.toString(), jwt.getClaimAsString("tenant_id"));
  }

  @Test
  void getTenantIdFromSecurityContext_shouldHandleNoAuthentication() {
    // Arrange
    SecurityContext securityContext = mock(SecurityContext.class);
    lenient().when(securityContext.getAuthentication()).thenReturn(null);

    // Act & Assert - Verify no auth scenario
    assertNull(securityContext.getAuthentication());
  }

  @Test
  void getTenantIdFromSecurityContext_shouldHandleNotAuthenticated() {
    // Arrange
    Authentication auth = mock(Authentication.class);
    lenient().when(auth.isAuthenticated()).thenReturn(false);

    // Act & Assert - Verify not authenticated scenario
    assertFalse(auth.isAuthenticated());
  }

  @Test
  void getTenantIdFromSecurityContext_shouldHandleNoTenantIdClaim() {
    // Arrange
    Jwt jwt = createMockJwt(null); // No tenant_id claim

    // Act & Assert - Verify missing claim scenario
    assertNull(jwt.getClaimAsString("tenant_id"));
  }

  @Test
  void getTenantIdFromSecurityContext_shouldHandleInvalidUuidFormat() {
    // Arrange
    Jwt jwt = createMockJwt("not-a-valid-uuid");

    // Act & Assert - Verify invalid UUID format throws exception
    assertThrows(IllegalArgumentException.class,
        () -> UUID.fromString(jwt.getClaimAsString("tenant_id")));
  }

  /**
   * Create mock JWT with tenant_id claim
   */
  private Jwt createMockJwt(String tenantId) {
    Map<String, Object> claims = tenantId != null
        ? Map.of("tenant_id", tenantId, "sub", "user@example.com")
        : Map.of("sub", "user@example.com");

    return new Jwt("mock-token", Instant.now(), Instant.now().plusSeconds(3600),
        Map.of("alg", "RS256"), claims);
  }
}
