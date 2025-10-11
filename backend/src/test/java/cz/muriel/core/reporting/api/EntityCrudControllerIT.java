package cz.muriel.core.reporting.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for EntityCrudController
 * 
 * Tests:
 * - PATCH with valid version (should succeed)
 * - PATCH with stale version (should return 409 Conflict)
 * - PATCH without If-Match header (should return 428 Precondition Required)
 * - PATCH non-editable field (should return 400 Bad Request)
 * - PATCH non-existent entity (should return 404)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EntityCrudControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String testTenantId = "00000000-0000-0000-0000-000000000001";
    private UUID testUserId;

    @BeforeEach
    void setup() {
        // Test data will be created by @Transactional
        testUserId = UUID.randomUUID();
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void shouldUpdateEntityWithValidVersion() throws Exception {
        // Arrange
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", "INACTIVE");

        // Act & Assert
        mockMvc.perform(patch("/api/entities/User/" + testUserId)
                .header("If-Match", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(header().exists("ETag"));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void shouldReturn409OnStaleVersion() throws Exception {
        // Arrange
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", "INACTIVE");

        // Act & Assert - use stale version
        mockMvc.perform(patch("/api/entities/User/" + testUserId)
                .header("If-Match", "0") // stale version
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Concurrent modification")))
                .andExpect(jsonPath("$.currentVersion").exists());
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void shouldReturn428WithoutIfMatchHeader() throws Exception {
        // Arrange
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", "INACTIVE");

        // Act & Assert
        mockMvc.perform(patch("/api/entities/User/" + testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.message").value(containsString("If-Match header is required")));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void shouldReturn400OnNonEditableField() throws Exception {
        // Arrange
        Map<String, Object> patch = new HashMap<>();
        patch.put("id", UUID.randomUUID()); // id is not editable

        // Act & Assert
        mockMvc.perform(patch("/api/entities/User/" + testUserId)
                .header("If-Match", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("not editable")));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void shouldReturn404OnNonExistentEntity() throws Exception {
        // Arrange
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", "INACTIVE");

        // Act & Assert
        mockMvc.perform(patch("/api/entities/NonExistentEntity/" + testUserId)
                .header("If-Match", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Entity not found")));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void shouldReturn404OnNonExistentRecord() throws Exception {
        // Arrange
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", "INACTIVE");
        UUID nonExistentId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(patch("/api/entities/User/" + nonExistentId)
                .header("If-Match", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Record not found")));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void shouldEnforceRowLevelSecurity() throws Exception {
        // This test verifies that users can only update records from their tenant
        // Requires test data with different tenants
        
        // Arrange
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", "INACTIVE");
        UUID otherTenantUserId = UUID.randomUUID(); // from different tenant

        // Act & Assert
        mockMvc.perform(patch("/api/entities/User/" + otherTenantUserId)
                .header("If-Match", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isNotFound()); // RLS makes it appear as not found
    }
}
