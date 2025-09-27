package cz.muriel.core.controller;

import cz.muriel.core.auth.security.HmacUtils;
import cz.muriel.core.dto.KeycloakWebhookEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureWebMvc @TestPropertySource(properties = {
    "keycloak.webhook.hmac-secret=test-secret-key" }) @Transactional
public class KeycloakWebhookControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  public void shouldProcessValidWebhookEvent() throws Exception {
    // Vytvoř test payload s realm="core-platform" a user alice
    KeycloakWebhookEventDto event = KeycloakWebhookEventDto.builder().type("USER_UPDATED")
        .realm("core-platform").userId("alice-user-id").username("alice").email("alice@example.com")
        .firstName("Alice").lastName("Smith").timestamp(System.currentTimeMillis()).build();

    String jsonPayload = objectMapper.writeValueAsString(event);

    // Spočítej validní HMAC
    String validHmac = HmacUtils.computeHmacSha256(jsonPayload.getBytes(), "test-secret-key");

    // Pošli webhook event s validním HMAC
    mockMvc
        .perform(post("/internal/keycloak/events").contentType(MediaType.APPLICATION_JSON)
            .header("X-KC-Signature", "sha256=" + validHmac).content(jsonPayload))
        .andExpect(status().isOk());

    // Další ověření by zde mohlo zkontrolovat, že se uživatel uložil do
    // users_directory
    // To by vyžadovalo mock nebo skutečnou databázi s tenants tabulkou
  }

  @Test
  public void shouldRejectInvalidHmacSignature() throws Exception {
    KeycloakWebhookEventDto event = KeycloakWebhookEventDto.builder().type("USER_CREATED")
        .realm("core-platform").userId("bob-user-id").username("bob").email("bob@example.com")
        .build();

    String jsonPayload = objectMapper.writeValueAsString(event);

    // Použij špatný HMAC
    mockMvc.perform(post("/internal/keycloak/events").contentType(MediaType.APPLICATION_JSON)
        .header("X-KC-Signature", "sha256=invalid_hmac_signature_1234567890abcdef")
        .content(jsonPayload)).andExpect(status().isUnauthorized());
  }

  @Test
  public void shouldRejectMissingSignatureHeader() throws Exception {
    KeycloakWebhookEventDto event = KeycloakWebhookEventDto.builder().type("USER_DELETED")
        .realm("core-platform").userId("charlie-user-id").username("charlie").build();

    String jsonPayload = objectMapper.writeValueAsString(event);

    // Pošli bez X-KC-Signature header
    mockMvc.perform(post("/internal/keycloak/events").contentType(MediaType.APPLICATION_JSON)
        .content(jsonPayload)).andExpect(status().isUnauthorized());
  }

  @Test
  public void shouldProcessEventFromCoreRealm() throws Exception {
    // Test že webhook správně zpracuje event pro náš realm
    KeycloakWebhookEventDto event = KeycloakWebhookEventDto.builder().type("USER_CREATED")
        .realm("core-platform").userId("test-user-id").username("testuser")
        .email("testuser@core.local").firstName("Test").lastName("User")
        .timestamp(System.currentTimeMillis()).build();

    String jsonPayload = objectMapper.writeValueAsString(event);
    String validHmac = HmacUtils.computeHmacSha256(jsonPayload.getBytes(), "test-secret-key");

    mockMvc
        .perform(post("/internal/keycloak/events").contentType(MediaType.APPLICATION_JSON)
            .header("X-KC-Signature", "sha256=" + validHmac).content(jsonPayload))
        .andExpect(status().isOk());
  }
}
