package cz.muriel.core.reporting.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SecurityHeadersFilter. Tests OWASP-required security
 * headers enforcement.
 */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class SecurityHeadersFilterIT {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldIncludeContentSecurityPolicyHeader() throws Exception {
    // Act
    mockMvc
        .perform(post("/api/reports/query").header("Authorization", "Bearer test-token")
            .header("Content-Type", "application/json")
            .content("{\"query\":{\"dimensions\":[\"User.id\"]}}"))
        .andExpect(header().exists("Content-Security-Policy"))
        .andExpect(header().string("Content-Security-Policy",
            "default-src 'self'; script-src 'self'; frame-ancestors 'none'"));
  }

  @Test
  void shouldIncludeXContentTypeOptionsHeader() throws Exception {
    // Act
    mockMvc
        .perform(
            get("/api/reports/metadata/User/spec").header("Authorization", "Bearer test-token"))
        .andExpect(header().exists("X-Content-Type-Options"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"));
  }

  @Test
  void shouldIncludeXFrameOptionsHeader() throws Exception {
    // Act
    mockMvc
        .perform(
            get("/api/reports/metadata/User/spec").header("Authorization", "Bearer test-token"))
        .andExpect(header().exists("X-Frame-Options"))
        .andExpect(header().string("X-Frame-Options", "DENY"));
  }

  @Test
  void shouldIncludeXXSSProtectionHeader() throws Exception {
    // Act
    mockMvc
        .perform(post("/api/reports/query").header("Authorization", "Bearer test-token")
            .header("Content-Type", "application/json")
            .content("{\"query\":{\"dimensions\":[\"User.id\"]}}"))
        .andExpect(header().exists("X-XSS-Protection"))
        .andExpect(header().string("X-XSS-Protection", "1; mode=block"));
  }

  @Test
  void shouldIncludeStrictTransportSecurityHeader() throws Exception {
    // Act
    mockMvc
        .perform(
            get("/api/reports/metadata/User/spec").header("Authorization", "Bearer test-token"))
        .andExpect(header().exists("Strict-Transport-Security"))
        .andExpect(header().string("Strict-Transport-Security", "max-age=31536000"));
  }

  @Test
  void shouldIncludeReferrerPolicyHeader() throws Exception {
    // Act
    mockMvc
        .perform(post("/api/reports/query").header("Authorization", "Bearer test-token")
            .header("Content-Type", "application/json")
            .content("{\"query\":{\"dimensions\":[\"User.id\"]}}"))
        .andExpect(header().exists("Referrer-Policy"))
        .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
  }

  @Test
  void shouldIncludeCacheControlForSensitiveEndpoints() throws Exception {
    // Act: /api/reports/** should have no-store
    mockMvc
        .perform(post("/api/reports/query").header("Authorization", "Bearer test-token")
            .header("Content-Type", "application/json")
            .content("{\"query\":{\"dimensions\":[\"User.id\"]}}"))
        .andExpect(header().exists("Cache-Control"))
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  void shouldIncludeAllSecurityHeadersTogether() throws Exception {
    // Act: Validate all headers in one request
    mockMvc
        .perform(post("/api/reports/query").header("Authorization", "Bearer test-token")
            .header("Content-Type", "application/json")
            .content("{\"query\":{\"dimensions\":[\"User.id\"]}}"))
        .andExpect(header().exists("Content-Security-Policy"))
        .andExpect(header().exists("X-Content-Type-Options"))
        .andExpect(header().exists("X-Frame-Options"))
        .andExpect(header().exists("X-XSS-Protection"))
        .andExpect(header().exists("Strict-Transport-Security"))
        .andExpect(header().exists("Referrer-Policy")).andExpect(header().exists("Cache-Control"));
  }

  @Test
  void shouldApplySecurityHeadersToEntitiesEndpoint() throws Exception {
    // Act: Security headers should apply to /api/entities/** too
    mockMvc.perform(get("/api/entities/User/1").header("Authorization", "Bearer test-token"))
        .andExpect(header().exists("X-Content-Type-Options"))
        .andExpect(header().exists("X-Frame-Options"))
        .andExpect(header().exists("Content-Security-Policy"));
  }
}
