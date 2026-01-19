package cz.muriel.core.dto;

import cz.muriel.core.metamodel.schema.ai.GlobalAiConfig;

/**
 * Event payload for config change notifications.
 */
public record ConfigChangeEvent(String eventId, String eventType, String timestamp,
    GlobalAiConfig config) {}
