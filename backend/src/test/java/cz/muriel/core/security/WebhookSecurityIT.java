package cz.muriel.core.security;

import cz.muriel.core.dto.KeycloakWebhookEventDto;
import cz.muriel.core.service.KeycloakEventProjectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 🔐 Integrační testy pro webhook HMAC security
 */
@SpringBootTest @AutoConfigureMockMvc @TestPropertySource(properties = {
    "keycloak.webhook.hmac-secret=test-secret-for-hmac",
    "logging.level.cz.muriel.core.auth.security=DEBUG" })
class WebhookSecurityIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private KeycloakEventProjectionService projectionService;

  private static final String WEBHOOK_ENDPOINT = "/internal/keycloak/events";
  private static final String TEST_SECRET = "test-secret-for-hmac";

  @Test
  void shouldRejectRequestWithoutSignatureHeader() throws Exception {
    // Given
    KeycloakWebhookEventDto event = createTestEvent();
    String jsonBody = objectMapper.writeValueAsString(event);

    // When & Then
    mockMvc
        .perform(post(WEBHOOK_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(jsonBody))
        .andExpect(status().isUnauthorized());

    // Verify service was not called
    verify(projectionService, never()).processEvent(any());
  }

  @Test
  void shouldRejectRequestWithInvalidSignatureFormat() throws Exception {
    // Given
    KeycloakWebhookEventDto event = createTestEvent();
    String jsonBody = objectMapper.writeValueAsString(event);

    // When & Then - invalid format (missing sha256= prefix)
    mockMvc
        .perform(post(WEBHOOK_ENDPOINT).contentType(MediaType.APPLICATION_JSON)
            .header("X-KC-Signature", "invalidformat123").content(jsonBody))
        .andExpect(status().isUnauthorized());

    // When & Then - invalid hex length
    mockMvc
        .perform(post(WEBHOOK_ENDPOINT).contentType(MediaType.APPLICATION_JSON)
            .header("X-KC-Signature", "sha256=tooshort").content(jsonBody))
        .andExpect(status().isUnauthorized());

    verify(projectionService, never()).processEvent(any());
  }

  @Test
  void shouldRejectRequestWithIncorrectSignature() throws Exception {
    // Given
    KeycloakWebhookEventDto event = createTestEvent();
    String jsonBody = objectMapper.writeValueAsString(event);
    String wrongSignature = "sha256=" + "0".repeat(64); // Wrong signature

    // When & Then
    mockMvc
        .perform(post(WEBHOOK_ENDPOINT).contentType(MediaType.APPLICATION_JSON)
            .header("X-KC-Signature", wrongSignature).content(jsonBody))
        .andExpect(status().isUnauthorized());

    verify(projectionService, never()).processEvent(any());
  }

  @Test
  void shouldAcceptRequestWithValidSignature() throws Exception {
    // Given
    KeycloakWebhookEventDto event = createTestEvent();
    String jsonBody = objectMapper.writeValueAsString(event);
    String validSignature = "sha256=" + computeHmacSha256(jsonBody, TEST_SECRET);

    // When & Then
    mockMvc.perform(post(WEBHOOK_ENDPOINT).contentType(MediaType.APPLICATION_JSON)
        .header("X-KC-Signature", validSignature).content(jsonBody)).andExpect(status().isOk());

    // Verify service was called
    verify(projectionService).processEvent(any(KeycloakWebhookEventDto.class));
  }

  @Test
  void shouldProtectStandardApiEndpointsWithJWT() throws Exception {
    // Given - request to standard API endpoint without JWT

    // When & Then
    mockMvc.perform(get("/api/users/search").param("q", "test"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldOnlyApplyHmacFilterToWebhookEndpoint() throws Exception {
    // Given - request to different endpoint with X-KC-Signature header

    // When & Then - header should be ignored for non-webhook endpoints
    mockMvc.perform(get("/api/health").header("X-KC-Signature", "sha256=" + "0".repeat(64)))
        .andExpect(status().isNotFound()); // nebo jiný expected status pro /api/health
  }

  private KeycloakWebhookEventDto createTestEvent() {
    return KeycloakWebhookEventDto.builder().eventType("USER_UPDATED").tenantKey("test-tenant")
        .username("testuser").userId("user-123").email("test@example.com").build();
  }

  private String computeHmacSha256(String data, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

      StringBuilder hexString = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hexString.append(String.format("%02x", b));
      }
      return hexString.toString();
    } catch (Exception e) {
      throw new RuntimeException("Failed to compute HMAC", e);
    }
  }

  @TestConfiguration
  static class TestConfig {
    @Bean @Primary
    public KeycloakEventProjectionService projectionService() {
      return mock(KeycloakEventProjectionService.class);
    }
  }
}
