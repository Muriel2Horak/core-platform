package cz.muriel.core.controller.admin;

import cz.muriel.core.metamodel.MetamodelRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminMetamodelController
 * 
 * Tests: - Hot reload metamodel - Get metamodel status - RBAC enforcement
 */
@SpringBootTest @AutoConfigureMockMvc @DisplayName("AdminMetamodelController IT")
class AdminMetamodelControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private MetamodelRegistry metamodelRegistry;

  @Test @DisplayName("POST /api/admin/metamodel/reload - should reload metamodel for PLATFORM_ADMIN") @WithMockUser(roles = "PLATFORM_ADMIN")
  void reloadMetamodel_platformAdmin_success() throws Exception {
    // Given: Initial schema count
    int initialCount = metamodelRegistry.getAllSchemas().size();

    // When: Trigger reload
    mockMvc.perform(post("/api/admin/metamodel/reload")).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").value("Metamodel reloaded successfully"))
        .andExpect(jsonPath("$.schemaCount").exists()).andExpect(jsonPath("$.durationMs").exists());

    // Then: Schema count should be same (no schema changes)
    int afterCount = metamodelRegistry.getAllSchemas().size();
    assertThat(afterCount).isEqualTo(initialCount);
  }

  @Test @DisplayName("POST /api/admin/metamodel/reload - should reload metamodel for OPS") @WithMockUser(roles = "OPS")
  void reloadMetamodel_ops_success() throws Exception {
    mockMvc.perform(post("/api/admin/metamodel/reload")).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  @Test @DisplayName("POST /api/admin/metamodel/reload - should deny access for TENANT_ADMIN") @WithMockUser(roles = "TENANT_ADMIN")
  void reloadMetamodel_tenantAdmin_forbidden() throws Exception {
    mockMvc.perform(post("/api/admin/metamodel/reload")).andExpect(status().isForbidden());
  }

  @Test @DisplayName("POST /api/admin/metamodel/reload - should deny access for regular user") @WithMockUser(roles = "USER")
  void reloadMetamodel_regularUser_forbidden() throws Exception {
    mockMvc.perform(post("/api/admin/metamodel/reload")).andExpect(status().isForbidden());
  }

  @Test @DisplayName("GET /api/admin/metamodel/status - should return status for authenticated user") @WithMockUser
  void getMetamodelStatus_authenticated_success() throws Exception {
    mockMvc.perform(get("/api/admin/metamodel/status")).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("loaded"))
        .andExpect(jsonPath("$.schemaCount").exists()).andExpect(jsonPath("$.schemas").isArray());
  }

  @Test @DisplayName("GET /api/admin/metamodel/status - should deny access for unauthenticated user")
  void getMetamodelStatus_unauthenticated_forbidden() throws Exception {
    mockMvc.perform(get("/api/admin/metamodel/status")).andExpect(status().isUnauthorized());
  }

  @Test @DisplayName("Reload should preserve schema integrity") @WithMockUser(roles = "PLATFORM_ADMIN")
  void reloadMetamodel_preservesSchemaIntegrity() throws Exception {
    // Given: Get schema before reload
    var schemasBefore = metamodelRegistry.getAllSchemas();

    // When: Trigger reload
    mockMvc.perform(post("/api/admin/metamodel/reload")).andExpect(status().isOk());

    // Then: Schemas should be same
    var schemasAfter = metamodelRegistry.getAllSchemas();
    assertThat(schemasAfter.size()).isEqualTo(schemasBefore.size());
    assertThat(schemasAfter.keySet()).containsAll(schemasBefore.keySet());
  }
}
