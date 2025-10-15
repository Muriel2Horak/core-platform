package cz.muriel.core.controller.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminAiConfigController
 * 
 * Step F: Admin nastavení - backend tests
 */
@SpringBootTest @AutoConfigureMockMvc @DisplayName("AdminAiConfigController IT")
class AdminAiConfigControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Test @DisplayName("GET /api/admin/ai/config - should return AI config for PLATFORM_ADMIN") @WithMockUser(roles = {
      "PLATFORM_ADMIN" })
  void getAiConfig_platformAdmin_returnsConfig() throws Exception {
    mockMvc.perform(get("/api/admin/ai/config")).andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").exists()).andExpect(jsonPath("$.mode").exists());
  }

  @Test @DisplayName("GET /api/admin/ai/config - should return AI config for OPS") @WithMockUser(roles = {
      "OPS" })
  void getAiConfig_ops_returnsConfig() throws Exception {
    mockMvc.perform(get("/api/admin/ai/config")).andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").exists());
  }

  @Test @DisplayName("GET /api/admin/ai/config - should return AI config for TENANT_ADMIN (read-only)") @WithMockUser(roles = {
      "TENANT_ADMIN" })
  void getAiConfig_tenantAdmin_returnsConfig() throws Exception {
    mockMvc.perform(get("/api/admin/ai/config")).andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").exists());
  }

  @Test @DisplayName("GET /api/admin/ai/config - should deny access for regular user") @WithMockUser(roles = {
      "USER" })
  void getAiConfig_regularUser_deniesAccess() throws Exception {
    mockMvc.perform(get("/api/admin/ai/config")).andExpect(status().isForbidden());
  }

  @Test @DisplayName("PUT /api/admin/ai/config - should validate config for PLATFORM_ADMIN") @WithMockUser(roles = {
      "PLATFORM_ADMIN" })
  void updateAiConfig_platformAdmin_validatesConfig() throws Exception {
    String configJson = """
        {
          "enabled": true,
          "mode": "META_ONLY"
        }
        """;

    mockMvc.perform(put("/api/admin/ai/config").contentType("application/json").content(configJson))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("validated"));
  }

  @Test @DisplayName("PUT /api/admin/ai/config - should validate config for OPS") @WithMockUser(roles = {
      "OPS" })
  void updateAiConfig_ops_validatesConfig() throws Exception {
    String configJson = """
        {
          "enabled": false,
          "mode": "META_ONLY"
        }
        """;

    mockMvc.perform(put("/api/admin/ai/config").contentType("application/json").content(configJson))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("validated"));
  }

  @Test @DisplayName("PUT /api/admin/ai/config - should deny write access for TENANT_ADMIN") @WithMockUser(roles = {
      "TENANT_ADMIN" })
  void updateAiConfig_tenantAdmin_deniesAccess() throws Exception {
    String configJson = """
        {
          "enabled": true,
          "mode": "META_ONLY"
        }
        """;

    mockMvc.perform(put("/api/admin/ai/config").contentType("application/json").content(configJson))
        .andExpect(status().isForbidden());
  }

  @Test @DisplayName("GET /api/admin/ai/status - should return AI status for authenticated user") @WithMockUser(roles = {
      "USER" })
  void getAiStatus_authenticatedUser_returnsStatus() throws Exception {
    mockMvc.perform(get("/api/admin/ai/status")).andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").exists()).andExpect(jsonPath("$.mode").exists())
        .andExpect(jsonPath("$.status").exists());
  }

  @Test @DisplayName("GET /api/admin/ai/status - should deny access for unauthenticated user")
  void getAiStatus_unauthenticated_deniesAccess() throws Exception {
    mockMvc.perform(get("/api/admin/ai/status")).andExpect(status().isUnauthorized());
  }
}
