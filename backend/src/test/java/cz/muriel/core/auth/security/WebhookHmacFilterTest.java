package cz.muriel.core.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WebhookHmacFilterTest.TestController.class) @TestPropertySource(properties = {
    "keycloak.webhook.hmac-secret=test-secret-key" })
public class WebhookHmacFilterTest {

  @Autowired
  private MockMvc mockMvc;

  private static final String TEST_JSON = "{\"ping\":\"pong\"}";
  private static final String WEBHOOK_PATH = "/internal/keycloak/events";

  @Test
  public void shouldAllowRequestWithValidHmac() throws Exception {
    // Spočítej validní HMAC pro test JSON
    String validHmac = HmacUtils.computeHmacSha256(TEST_JSON.getBytes(), "test-secret-key");

    mockMvc
        .perform(post(WEBHOOK_PATH).contentType(MediaType.APPLICATION_JSON)
            .header("X-KC-Signature", "sha256=" + validHmac).content(TEST_JSON))
        .andExpect(status().isOk()).andExpect(content().string("Request processed successfully"));
  }

  @Test
  public void shouldRejectRequestWithInvalidHmac() throws Exception {
    mockMvc.perform(post(WEBHOOK_PATH).contentType(MediaType.APPLICATION_JSON)
        .header("X-KC-Signature", "sha256=invalid_hmac_signature_1234567890abcdef")
        .content(TEST_JSON)).andExpect(status().isUnauthorized());
  }

  @Test
  public void shouldRejectRequestWithMissingSignature() throws Exception {
    mockMvc.perform(post(WEBHOOK_PATH).contentType(MediaType.APPLICATION_JSON).content(TEST_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void shouldRejectRequestWithInvalidSignatureFormat() throws Exception {
    mockMvc
        .perform(post(WEBHOOK_PATH).contentType(MediaType.APPLICATION_JSON)
            .header("X-KC-Signature", "invalid-format").content(TEST_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void shouldNotFilterNonWebhookPaths() throws Exception {
    mockMvc.perform(post("/other/path").contentType(MediaType.APPLICATION_JSON).content(TEST_JSON))
        .andExpect(status().isOk());
  }

  @Test
  public void shouldNotFilterNonPostRequests() throws Exception {
    mockMvc.perform(get(WEBHOOK_PATH)).andExpect(status().isOk()); // TestController má GET mapping
  }

  // Test controller pro testování
  @RestController
  public static class TestController {

    @PostMapping("/internal/keycloak/events")
    public String handleWebhookEvent(@RequestBody Map<String, Object> data) {
      // Ověříme, že @RequestBody funguje
      if (data == null || data.isEmpty()) {
        throw new RuntimeException("Request body is null or empty");
      }
      return "Request processed successfully";
    }

    @PostMapping("/other/path")
    public String handleOtherPath(@RequestBody(required = false) Map<String, Object> data) {
      return "Other path processed";
    }

    @GetMapping("/internal/keycloak/events")
    public String handleGetRequest() {
      return "GET request processed";
    }
  }

  @Configuration
  public static class TestConfig {
    @Bean
    public WebhookHmacFilter webhookHmacFilter() {
      return new WebhookHmacFilter("test-secret-key");
    }
  }
}
