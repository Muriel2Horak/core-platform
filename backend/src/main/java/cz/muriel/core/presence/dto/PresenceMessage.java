package cz.muriel.core.presence.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * WebSocket message for presence tracking
 * 
 * Message Types: - PRESENCE: Current state (users, stale, busyBy, version) -
 * LOCK_ACK: Lock acquisition result - UNLOCK_ACK: Lock release confirmation -
 * HB_ACK: Heartbeat acknowledgment - UNSUB_ACK: Unsubscribe confirmation -
 * ERROR: Error message
 */
@Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
public class PresenceMessage {

  private String type;

  // For PRESENCE messages
  private Set<Object> users;
  private Boolean stale;
  private String busyBy;
  private Long version;

  // For LOCK/UNLOCK messages
  private String field;
  private Boolean success;

  // For ERROR messages
  private String error;
}
