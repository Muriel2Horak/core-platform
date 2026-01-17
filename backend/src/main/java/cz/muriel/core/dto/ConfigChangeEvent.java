package cz.muriel.core.dto;

/**
 * Event payload for config change notifications.
 */
public record ConfigChangeEvent(String eventId, String eventType, String timestamp,
    AiConfigPayload config) {

  public record AiConfigPayload(boolean enabled, String mode) {}
}
