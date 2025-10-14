package cz.muriel.core.reporting.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import cz.muriel.core.test.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RateLimitFilter. Tests rate limiting enforcement (120
 * req/min per tenant).
 */
@SpringBootTest @AutoConfigureMockMvc
class RateLimitFilterIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldAllowRequestsUnderRateLimit() throws Exception {
    // Act: Make 10 requests (well under 120/min limit)
    for (int i = 0; i < 10; i++) {
      mockMvc.perform(post("/api/reports/query").header("Authorization", "Bearer test-token")
          .header("Content-Type", "application/json")
          .content("{\"query\":{\"dimensions\":[\"User.id\"]}}")).andExpect(status().isOk());
    }
  }

  @Test
  void shouldReturn429AfterExceedingRateLimit() throws Exception {
    // Act: Make 121 requests to exceed 120/min limit
    for (int i = 0; i < 121; i++) {
      var result = mockMvc.perform(post("/api/reports/query")
          .header("Authorization", "Bearer test-token").header("Content-Type", "application/json")
          .content("{\"query\":{\"dimensions\":[\"User.id\"]}}"));

      if (i < 120) {
        result.andExpect(status().isOk());
      } else {
        // 121st request should be rate limited
        result.andExpect(status().isTooManyRequests())
            .andExpect(header().exists("X-RateLimit-Retry-After"))
            .andExpect(header().exists("Retry-After"));
      }
    }
  }

  @Test
  void shouldIncludeRateLimitHeaders() throws Exception {
    // Act
    mockMvc
        .perform(post("/api/reports/query").header("Authorization", "Bearer test-token")
            .header("Content-Type", "application/json")
            .content("{\"query\":{\"dimensions\":[\"User.id\"]}}"))
        .andExpect(status().isOk()).andExpect(header().exists("X-RateLimit-Limit"))
        .andExpect(header().exists("X-RateLimit-Remaining"))
        .andExpect(header().exists("X-RateLimit-Reset"))
        .andExpect(header().string("X-RateLimit-Limit", "120"));
  }

  @Test
  void shouldIsolateRateLimitPerTenant() throws Exception {
    // Act: Make 60 requests for tenant-1
    for (int i = 0; i < 60; i++) {
      mockMvc.perform(post("/api/reports/query").header("Authorization", "Bearer tenant-1-token")
          .header("Content-Type", "application/json")
          .content("{\"query\":{\"dimensions\":[\"User.id\"]}}")).andExpect(status().isOk());
    }

    // tenant-2 should have full 120 req/min quota (isolation)
    mockMvc
        .perform(post("/api/reports/query").header("Authorization", "Bearer tenant-2-token")
            .header("Content-Type", "application/json")
            .content("{\"query\":{\"dimensions\":[\"User.id\"]}}"))
        .andExpect(status().isOk()).andExpect(header().string("X-RateLimit-Remaining", "119"));
  }

  @Test
  void shouldApplyRateLimitToEntitiesEndpoint() throws Exception {
    // Act: Rate limit should apply to /api/entities/** too
    for (int i = 0; i < 10; i++) {
      mockMvc
          .perform(
              post("/api/entities/User/bulk-update").header("Authorization", "Bearer test-token")
                  .header("Content-Type", "application/json")
                  .content("{\"ids\":[1],\"updates\":{\"name\":\"Test\"}}"))
          .andExpect(status().is4xxClientError()); // May fail validation, but not rate limit
    }

    // Check headers
    mockMvc
        .perform(post("/api/entities/User/bulk-update").header("Authorization", "Bearer test-token")
            .header("Content-Type", "application/json")
            .content("{\"ids\":[1],\"updates\":{\"name\":\"Test\"}}"))
        .andExpect(header().exists("X-RateLimit-Limit"));
  }
}
