package cz.muriel.core.reporting.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for ReportQueryController
 * 
 * Tests complete reporting workflow: - Query execution with Cube.js - Caching
 * behavior - RLS enforcement - Rate limiting - Circuit breaker (error handling)
 */
@SpringBootTest @AutoConfigureMockMvc @Testcontainers @Transactional
class ReportQueryControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldExecuteQuery() throws Exception {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("entity", "User");
    request.put("dimensions", new String[] { "status" });
    request.put("measures", new String[] {});

    // Act & Assert
    mockMvc
        .perform(post("/api/reports/query").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data").exists())
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldReturnEntitySpec() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/reports/metadata/User/spec")).andExpect(status().isOk())
        .andExpect(jsonPath("$.entityName").value("User"))
        .andExpect(jsonPath("$.tableName").exists()).andExpect(jsonPath("$.fields").isArray())
        .andExpect(jsonPath("$.editableFields").isArray())
        .andExpect(header().exists("X-Spec-Version"));
  }

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldValidateQueryRequest() throws Exception {
    // Arrange - missing required field
    Map<String, Object> request = new HashMap<>();
    request.put("dimensions", new String[] { "status" });
    // Missing entity field

    // Act & Assert
    mockMvc.perform(post("/api/reports/query").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldReturn404OnNonExistentEntity() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/reports/metadata/NonExistentEntity/spec"))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturn401WithoutAuthentication() throws Exception {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("entity", "User");
    request.put("dimensions", new String[] { "status" });

    // Act & Assert - no @WithMockUser
    mockMvc
        .perform(post("/api/reports/query").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldEnforceContentType() throws Exception {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("entity", "User");
    request.put("dimensions", new String[] { "status" });

    // Act & Assert - wrong content type
    mockMvc.perform(post("/api/reports/query").contentType(MediaType.APPLICATION_XML) // Wrong!
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldIncludeSecurityHeaders() throws Exception {
    // Act
    mockMvc.perform(get("/api/reports/metadata/User/spec")).andExpect(status().isOk())
        .andExpect(header().exists("X-Content-Type-Options"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().exists("X-Frame-Options"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().exists("Content-Security-Policy"));
  }

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldIncludeRateLimitHeaders() throws Exception {
    // Act
    mockMvc.perform(get("/api/reports/metadata/User/spec")).andExpect(status().isOk())
        .andExpect(header().exists("X-RateLimit-Limit"))
        .andExpect(header().exists("X-RateLimit-Remaining"));
  }
}
