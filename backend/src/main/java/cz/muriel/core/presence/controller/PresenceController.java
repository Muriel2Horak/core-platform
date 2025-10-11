package cz.muriel.core.presence.controller;

import cz.muriel.core.presence.PresenceService;
import cz.muriel.core.presence.dto.PresenceStateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * REST API for presence tracking (for testing and debugging)
 * 
 * Endpoints:
 * - GET /api/presence/{tenantId}/{entity}/{id} → Get current presence state
 * - POST /api/presence/{tenantId}/{entity}/{id}/subscribe → Subscribe user
 * - POST /api/presence/{tenantId}/{entity}/{id}/unsubscribe → Unsubscribe user
 * - POST /api/presence/{tenantId}/{entity}/{id}/heartbeat → Send heartbeat
 * 
 * Note: Production use should rely on WebSocket, this is for debugging/monitoring
 */
@Slf4j
@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = false)
@Tag(name = "Presence", description = "Real-time presence tracking and field locks")
public class PresenceController {

  private final PresenceService presenceService;

  @Operation(summary = "Get presence state", description = "Get current presence state for an entity")
  @GetMapping("/{tenantId}/{entity}/{id}")
  public ResponseEntity<PresenceStateDto> getPresence(
      @Parameter(description = "Tenant ID") @PathVariable String tenantId,
      @Parameter(description = "Entity name") @PathVariable String entity,
      @Parameter(description = "Entity ID") @PathVariable String id
  ) {
    Set<Object> users = presenceService.getPresence(tenantId, entity, id);
    boolean stale = presenceService.isStale(tenantId, entity, id);
    String busyBy = presenceService.getBusyBy(tenantId, entity, id);
    Long version = presenceService.getVersion(tenantId, entity, id);

    PresenceStateDto state = PresenceStateDto.builder()
        .tenantId(tenantId)
        .entity(entity)
        .id(id)
        .users(users)
        .stale(stale)
        .busyBy(busyBy)
        .version(version)
        .build();

    return ResponseEntity.ok(state);
  }

  @Operation(summary = "Subscribe to presence", description = "Subscribe user to entity presence tracking")
  @PostMapping("/{tenantId}/{entity}/{id}/subscribe")
  public ResponseEntity<Void> subscribe(
      @PathVariable String tenantId,
      @PathVariable String entity,
      @PathVariable String id,
      Authentication authentication
  ) {
    String userId = authentication.getName();
    presenceService.subscribe(userId, tenantId, entity, id);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Unsubscribe from presence", description = "Unsubscribe user from entity presence tracking")
  @PostMapping("/{tenantId}/{entity}/{id}/unsubscribe")
  public ResponseEntity<Void> unsubscribe(
      @PathVariable String tenantId,
      @PathVariable String entity,
      @PathVariable String id,
      Authentication authentication
  ) {
    String userId = authentication.getName();
    presenceService.unsubscribe(userId, tenantId, entity, id);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Send heartbeat", description = "Send heartbeat to keep presence alive")
  @PostMapping("/{tenantId}/{entity}/{id}/heartbeat")
  public ResponseEntity<Void> heartbeat(
      @PathVariable String tenantId,
      @PathVariable String entity,
      @PathVariable String id,
      Authentication authentication
  ) {
    String userId = authentication.getName();
    presenceService.heartbeat(userId, tenantId, entity, id);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Acquire lock", description = "Try to acquire lock on a specific field")
  @PostMapping("/{tenantId}/{entity}/{id}/lock/{field}")
  public ResponseEntity<Boolean> acquireLock(
      @PathVariable String tenantId,
      @PathVariable String entity,
      @PathVariable String id,
      @PathVariable String field,
      Authentication authentication
  ) {
    String userId = authentication.getName();
    boolean success = presenceService.acquireLock(userId, tenantId, entity, id, field);
    return ResponseEntity.ok(success);
  }

  @Operation(summary = "Release lock", description = "Release lock on a specific field")
  @PostMapping("/{tenantId}/{entity}/{id}/unlock/{field}")
  public ResponseEntity<Void> releaseLock(
      @PathVariable String tenantId,
      @PathVariable String entity,
      @PathVariable String id,
      @PathVariable String field,
      Authentication authentication
  ) {
    String userId = authentication.getName();
    presenceService.releaseLock(userId, tenantId, entity, id, field);
    return ResponseEntity.ok().build();
  }
}
