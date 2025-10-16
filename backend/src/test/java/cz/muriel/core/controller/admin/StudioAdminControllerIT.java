package cz.muriel.core.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import cz.muriel.core.test.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * S10-B: Integration test for Studio Admin API - Export Entities
 */
@SpringBootTest @AutoConfigureMockMvc @TestPropertySource(properties = {
    "metamodel.schema.auto-generate=false" // Disable schema generator in tests
})
class StudioAdminControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  /**
   * S10-B: Test export all entities endpoint
   */
  @Test @WithMockUser(authorities = { "CORE_ROLE_STUDIO" })
  void testExportEntities_Success() throws Exception {
    mockMvc.perform(get("/api/admin/studio/entities")).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.entitiesCount").exists()).andExpect(jsonPath("$.entities").isArray())
        // Check at least User entity exists
        .andExpect(jsonPath("$.entities[?(@.name == 'User')]").exists())
        .andExpect(jsonPath("$.entities[?(@.name == 'User')].table").value("users_directory"))
        .andExpect(jsonPath("$.entities[?(@.name == 'User')].fields").isArray());
  }

  /**
   * S10-B: Test export entities without RBAC - should deny
   */
  @Test @WithMockUser(authorities = { "CORE_ROLE_USER" })
  void testExportEntities_Forbidden() throws Exception {
    mockMvc.perform(get("/api/admin/studio/entities")).andExpect(status().isForbidden());
  }

  /**
   * S10-B: Test get single entity detail
   */
  @Test @WithMockUser(authorities = { "CORE_ROLE_STUDIO" })
  void testGetEntity_Success() throws Exception {
    mockMvc.perform(get("/api/admin/studio/entities/User")).andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("User")).andExpect(jsonPath("$.entity").value("User"))
        .andExpect(jsonPath("$.table").value("users_directory"))
        .andExpect(jsonPath("$.fields").isArray()).andExpect(jsonPath("$.idField").exists())
        .andExpect(jsonPath("$.versionField").exists())
        .andExpect(jsonPath("$.tenantField").exists());
  }

  /**
   * S10-B: Test get non-existent entity - should return 404
   */
  @Test @WithMockUser(authorities = { "CORE_ROLE_STUDIO" })
  void testGetEntity_NotFound() throws Exception {
    mockMvc.perform(get("/api/admin/studio/entities/NonExistentEntity"))
        .andExpect(status().isNotFound());
  }

  /**
   * S10-B: Test health check
   */
  @Test @WithMockUser(authorities = { "CORE_ROLE_STUDIO" })
  void testHealth() throws Exception {
    mockMvc.perform(get("/api/admin/studio/health")).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok")).andExpect(jsonPath("$.phase").value("S10-B"));
  }
}
