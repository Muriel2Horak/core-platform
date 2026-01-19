package cz.muriel.core.service.ai;

import cz.muriel.core.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpCapabilitiesServiceTest {

  @Test
  void getCapabilities_returnsViewAndEdit_whenReadAndUpdateAllowed() {
    PermissionService permissionService = mock(PermissionService.class);
    McpCapabilitiesService service = new McpCapabilitiesService(permissionService);
    Authentication auth = mock(Authentication.class);

    doReturn(List.of(new SimpleGrantedAuthority("CORE_ROLE_ADMIN")))
        .when(auth).getAuthorities();
    when(permissionService.hasPermission(anyList(), anyString())).thenAnswer(invocation -> {
      String permission = invocation.getArgument(1, String.class);
      return permission.startsWith("users:read") || permission.startsWith("users:update");
    });

    Map<String, Object> result = service.getCapabilities(auth, "users.detail");

    assertEquals(true, result.get("canView"));
    assertEquals(true, result.get("canEdit"));
    assertTrue(((List<?>) result.get("canExecute")).contains("update"));
  }

  @Test
  void getCapabilities_returnsViewOnly_whenReadAllowed() {
    PermissionService permissionService = mock(PermissionService.class);
    McpCapabilitiesService service = new McpCapabilitiesService(permissionService);
    Authentication auth = mock(Authentication.class);

    doReturn(List.of(new SimpleGrantedAuthority("CORE_ROLE_TENANT_ADMIN")))
        .when(auth).getAuthorities();
    when(permissionService.hasPermission(anyList(), anyString())).thenAnswer(invocation -> {
      String permission = invocation.getArgument(1, String.class);
      return permission.startsWith("users:read");
    });

    Map<String, Object> result = service.getCapabilities(auth, "users.list");

    assertEquals(true, result.get("canView"));
    assertEquals(false, result.get("canEdit"));
    assertTrue(((List<?>) result.get("canExecute")).isEmpty());
  }

  @Test
  void getCapabilities_throwsOnMissingRoute() {
    PermissionService permissionService = mock(PermissionService.class);
    McpCapabilitiesService service = new McpCapabilitiesService(permissionService);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> service.getCapabilities(null, " "));
    assertTrue(ex.getMessage().contains("routeId"));
  }
}
