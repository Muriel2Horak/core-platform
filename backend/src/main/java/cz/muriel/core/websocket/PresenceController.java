package cz.muriel.core.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 👥 REST API for Presence (Fallback)
 */
@RestController @RequestMapping("/api/presence") @RequiredArgsConstructor @Slf4j
public class PresenceController {

  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * Get online users for entity
   */
  @GetMapping("/{entityType}/{entityId}")
  public ResponseEntity<Map<String, Object>> getPresence(@PathVariable String entityType,
      @PathVariable String entityId, Authentication auth) {
    String tenantId = extractTenantId(auth);
    String channel = "presence:" + tenantId + ":" + entityType + ":" + entityId;

    Set<Object> members = redisTemplate.opsForSet().members("presence_members:" + channel);

    List<Map<String, Object>> onlineUsers = new ArrayList<>();
    if (members != null) {
      for (Object userId : members) {
        String presenceKey = channel + ":" + userId;
        @SuppressWarnings("unchecked")
        Map<String, Object> presenceData = (Map<String, Object>) redisTemplate.opsForValue()
            .get(presenceKey);

        if (presenceData != null) {
          onlineUsers.add(presenceData);
        }
      }
    }

    return ResponseEntity.ok(Map.of("entityType", entityType, "entityId", entityId, "onlineUsers",
        onlineUsers, "count", onlineUsers.size()));
  }

  /**
   * Get editing indicators for entity
   */
  @GetMapping("/{entityType}/{entityId}/editing")
  public ResponseEntity<Map<String, Object>> getEditingIndicators(@PathVariable String entityType,
      @PathVariable String entityId, Authentication auth) {
    String tenantId = extractTenantId(auth);
    String pattern = "editing:" + tenantId + ":" + entityType + ":" + entityId + ":*";

    Set<String> keys = redisTemplate.keys(pattern);

    List<Map<String, Object>> editors = new ArrayList<>();
    if (keys != null) {
      for (String key : keys) {
        @SuppressWarnings("unchecked")
        Map<String, Object> editData = (Map<String, Object>) redisTemplate.opsForValue().get(key);
        if (editData != null) {
          editors.add(editData);
        }
      }
    }

    return ResponseEntity.ok(Map.of("entityType", entityType, "entityId", entityId, "editors",
        editors, "count", editors.size()));
  }

  private String extractTenantId(Authentication auth) {
    // Extract tenant ID from JWT claims
    if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
      return jwtAuth.getToken().getClaim("tenant_id");
    }
    return "unknown";
  }
}
