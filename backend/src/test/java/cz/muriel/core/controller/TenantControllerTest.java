package cz.muriel.core.controller;

import cz.muriel.core.entity.Tenant;
import cz.muriel.core.service.TenantService;
import cz.muriel.core.test.MockMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Optional;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(classes = {TenantController.class, TenantControllerTest.WebConfig.class,
    TenantControllerTest.SecurityTestConfig.class, MockMvcTestConfig.class})
@Execution(ExecutionMode.SAME_THREAD)
class TenantControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TenantService tenantService;

  @Test
  void shouldReturnCurrentTenant() throws Exception {
    // Given - 🎯 SIMPLIFIED: Using only key field, name is derived from key
    Tenant tenant = Tenant.builder().key("test-tenant").build();

    when(tenantService.getCurrentTenant()).thenReturn(Optional.of(tenant));
    when(tenantService.getTenantDisplayName("test-tenant")).thenReturn("Tenant test-tenant");

    // When & Then
    mockMvc
        .perform(get("/api/tenants/me").with(jwt()
            .jwt(jwt -> jwt.claim("tenant", "test-tenant").claim("sub", "user123")
                .claim("preferred_username", "testuser"))
            .authorities(List.of(new SimpleGrantedAuthority("CORE_ROLE_USER")))))
        .andExpect(status().isOk()).andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.key").value("test-tenant"))
        .andExpect(jsonPath("$.name").value("Tenant test-tenant")) // getName() returns "Tenant " +
                                                                   // key
        .andExpect(jsonPath("$.realm").value("test-tenant")); // realm = key
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

  @TestConfiguration @EnableWebSecurity @EnableMethodSecurity(prePostEnabled = true)
  static class SecurityTestConfig {
    @Bean
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
      http.csrf(csrf -> csrf.disable())
          .anonymous(anonymous -> anonymous.disable())
          .exceptionHandling(
              exceptions -> exceptions.authenticationEntryPoint(
                  new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
          .authorizeHttpRequests(authz -> authz.requestMatchers("/api/**").authenticated()
              .anyRequest().permitAll());
      return http.build();
    }
  }

  @Configuration @EnableWebMvc
  static class WebConfig {
  }
}
