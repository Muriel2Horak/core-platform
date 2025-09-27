package cz.muriel.core.controller;

import cz.muriel.core.dto.KeycloakWebhookEventDto;
import cz.muriel.core.service.KeycloakEventProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 🔐 Keycloak Webhook Controller Autentizace je řešena WebhookHmacFilter -
 * tento controller už jen zpracovává validované eventy
 */
@RestController @RequestMapping("/internal/keycloak") @RequiredArgsConstructor @Slf4j
public class KeycloakWebhookController {

  private final KeycloakEventProjectionService projectionService;

  @PostMapping(value = "/events", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> handleWebhookEvent(@RequestBody KeycloakWebhookEventDto event) {

    log.info("🔗 Received Keycloak webhook event: type={}, tenant={}, user={}",
        event.getEventType(), event.getTenantKey(), event.getUsername());

    try {
      projectionService.processEvent(event);
      log.info("✅ Successfully processed webhook event: {}", event.getEventType());
      return ResponseEntity.ok().build();

    } catch (Exception e) {
      log.error("❌ Failed to process webhook event: {}", event.getEventType(), e);
      return ResponseEntity.status(500).build();
    }
  }
}
