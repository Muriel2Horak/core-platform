package cz.muriel.core.metamodel.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 🔄 Cache Invalidation Listener - Listens to PostgreSQL NOTIFY events
 * 
 * PostgreSQL triggers send NOTIFY on entity changes:
 * - INSERT: CREATE event
 * - UPDATE: UPDATE event
 * - DELETE: DELETE event
 * 
 * This listener invalidates Redis cache keys on changes.
 * 
 * Cache key format: entity:{type}:{id}:{version}
 * 
 * Integration:
 * 1. PostgreSQL trigger sends: NOTIFY change_events, '{"type":"UPDATE","table":"entity","id":"uuid"}'
 * 2. This listener receives event
 * 3. Invalidates Redis cache key
 * 4. Next API call fetches fresh data from DB
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationListener implements MessageListener {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String payload = new String(message.getBody());

            log.debug("Received cache invalidation event from {}: {}", channel, payload);

            // Parse event
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            
            String eventType = (String) event.get("type");
            String table = (String) event.get("table");
            String entityId = (String) event.get("id");
            String tenantId = (String) event.get("tenant_id");

            // Invalidate cache
            invalidateEntity(table, entityId, tenantId, eventType);

        } catch (Exception e) {
            log.error("Failed to process cache invalidation event", e);
        }
    }

    /**
     * Invalidate entity cache
     */
    private void invalidateEntity(String entityType, String entityId, String tenantId, String eventType) {
        // Pattern: entity:{type}:{id}:*
        String pattern = String.format("entity:%s:%s:*", entityType, entityId);
        
        // Delete all versions of this entity
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Invalidated cache for entity: type={}, id={}, keys={}, event={}", 
                entityType, entityId, keys.size(), eventType);
        }

        // Also invalidate list caches for this entity type
        String listPattern = String.format("list:%s:*", entityType);
        var listKeys = redisTemplate.keys(listPattern);
        if (listKeys != null && !listKeys.isEmpty()) {
            redisTemplate.delete(listKeys);
            log.debug("Invalidated list cache for entity type: {}, keys={}", entityType, listKeys.size());
        }
    }

    /**
     * Manual cache invalidation (for programmatic use)
     */
    public void invalidate(String entityType, String entityId) {
        String tenantId = TenantContext.getTenantKey();
        invalidateEntity(entityType, entityId, tenantId, "MANUAL");
    }

    /**
     * Clear all cache for tenant
     */
    public void clearTenantCache(String tenantId) {
        String pattern = String.format("*:*:%s:*", tenantId);
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cleared all cache for tenant: {}, keys={}", tenantId, keys.size());
        }
    }
}
