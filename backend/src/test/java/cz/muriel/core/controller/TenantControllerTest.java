package cz.muriel.core.controller;

import cz.muriel.core.entity.Tenant;
import cz.muriel.core.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantController.class)
class TenantControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TenantService tenantService;

  @Test
  void shouldReturnCurrentTenant() throws Exception {
    // Given
    Tenant tenant = Tenant.builder().key("test-tenant").name("Test Tenant").realm("core-platform")
        .build();

    when(tenantService.getCurrentTenant()).thenReturn(Optional.of(tenant));

    // When & Then
    mockMvc
        .perform(get("/api/tenants/me").with(jwt()
            .jwt(jwt -> jwt.claim("tenant", "test-tenant").claim("sub", "user123")
                .claim("preferred_username", "testuser"))
            .authorities(List.of(new SimpleGrantedAuthority("CORE_ROLE_USER")))))
        .andExpect(status().isOk()).andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.key").value("test-tenant"))
        .andExpect(jsonPath("$.name").value("Test Tenant"))
        .andExpect(jsonPath("$.realm").value("core-platform"));
  }

  @Test
  void shouldReturnNotFoundWhenTenantMissing() throws Exception {
    // Given
    when(tenantService.getCurrentTenant()).thenReturn(Optional.empty());

    // When & Then
    mockMvc
        .perform(get("/api/tenants/me").with(
            jwt().jwt(jwt -> jwt.claim("sub", "user123").claim("preferred_username", "testuser"))
                .authorities(List.of(new SimpleGrantedAuthority("CORE_ROLE_USER")))))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldRequireAuthentication() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/tenants/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRequireValidRole() throws Exception {
    // When & Then
    mockMvc
        .perform(get("/api/tenants/me").with(jwt().jwt(jwt -> jwt.claim("sub", "user123"))
            .authorities(List.of(new SimpleGrantedAuthority("INVALID_ROLE")))))
        .andExpect(status().isForbidden());
  }
}
