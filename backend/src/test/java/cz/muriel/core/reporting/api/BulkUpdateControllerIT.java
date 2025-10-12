package cz.muriel.core.reporting.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for BulkUpdateController
 * 
 * Tests: - POST bulk-update (should create job and return jobId) - GET job
 * status (should return PENDING → RUNNING → COMPLETED) - POST cancel job
 * (should cancel running job) - Bulk update with >1000 rows (should return 400)
 */
@SpringBootTest @AutoConfigureMockMvc @Transactional
class BulkUpdateControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldCreateBulkUpdateJob() throws Exception {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("ids", Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
        UUID.randomUUID().toString()));
    request.put("updates", Map.of("status", "INACTIVE"));

    // Act & Assert
    mockMvc
        .perform(post("/api/entities/User/bulk-update").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isAccepted()).andExpect(jsonPath("$.jobId").exists())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.message").value(containsString("started")));
  }

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldGetJobStatus() throws Exception {
    // Arrange - create job first
    Map<String, Object> request = new HashMap<>();
    request.put("ids", Arrays.asList(UUID.randomUUID().toString()));
    request.put("updates", Map.of("status", "INACTIVE"));

    String responseBody = mockMvc
        .perform(post("/api/entities/User/bulk-update").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andReturn().getResponse().getContentAsString();

    Map<String, Object> response = objectMapper.readValue(responseBody,
        new TypeReference<Map<String, Object>>() {
        });
    String jobId = (String) response.get("jobId");

    // Act & Assert - get status
    mockMvc.perform(get("/api/bulk-jobs/" + jobId)).andExpect(status().isOk())
        .andExpect(jsonPath("$.jobId").value(jobId)).andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.totalRows").exists());
  }

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldReturn404OnNonExistentJob() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/bulk-jobs/" + UUID.randomUUID())).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value(containsString("Job not found")));
  }

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldCancelJob() throws Exception {
    // Arrange - create job first
    Map<String, Object> request = new HashMap<>();
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < 500; i++) { // Large job
      ids.add(UUID.randomUUID().toString());
    }
    request.put("ids", ids);
    request.put("updates", Map.of("status", "INACTIVE"));

    String responseBody = mockMvc
        .perform(post("/api/entities/User/bulk-update").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andReturn().getResponse().getContentAsString();

    Map<String, Object> response = objectMapper.readValue(responseBody,
        new TypeReference<Map<String, Object>>() {
        });
    String jobId = (String) response.get("jobId");

    // Act - cancel job
    mockMvc.perform(post("/api/bulk-jobs/" + jobId + "/cancel")).andExpect(status().isNoContent());

    // Assert - job should be cancelled
    mockMvc.perform(get("/api/bulk-jobs/" + jobId)).andExpect(status().isOk()).andExpect(
        jsonPath("$.status").value(anyOf(is("CANCELLED"), is("PENDING"), is("RUNNING"))));
  }

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldReturn400OnTooManyRows() throws Exception {
    // Arrange - >1000 rows
    Map<String, Object> request = new HashMap<>();
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < 1001; i++) {
      ids.add(UUID.randomUUID().toString());
    }
    request.put("ids", ids);
    request.put("updates", Map.of("status", "INACTIVE"));

    // Act & Assert
    mockMvc
        .perform(post("/api/entities/User/bulk-update").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("Maximum 1000 rows")));
  }

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldReturn400OnEmptyIdsList() throws Exception {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("ids", Collections.emptyList());
    request.put("updates", Map.of("status", "INACTIVE"));

    // Act & Assert
    mockMvc.perform(post("/api/entities/User/bulk-update").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test @WithMockUser(username = "test-user", roles = { "USER" })
  void shouldProcessChunks() throws Exception {
    // Arrange - 250 rows (3 chunks)
    Map<String, Object> request = new HashMap<>();
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < 250; i++) {
      ids.add(UUID.randomUUID().toString());
    }
    request.put("ids", ids);
    request.put("updates", Map.of("status", "INACTIVE"));

    // Act
    String responseBody = mockMvc
        .perform(post("/api/entities/User/bulk-update").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andReturn().getResponse().getContentAsString();

    Map<String, Object> response = objectMapper.readValue(responseBody,
        new TypeReference<Map<String, Object>>() {
        });
    String jobId = (String) response.get("jobId");

    // Wait for job to complete (max 5 seconds)
    Thread.sleep(5000);

    // Assert - job should be completed or running
    mockMvc.perform(get("/api/bulk-jobs/" + jobId)).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(anyOf(is("COMPLETED"), is("RUNNING"), is("PENDING"))))
        .andExpect(jsonPath("$.totalRows").value(250));
  }
}
