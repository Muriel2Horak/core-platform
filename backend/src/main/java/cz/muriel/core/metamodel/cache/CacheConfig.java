package cz.muriel.core.metamodel.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * 🔧 Cache Configuration - Redis listener for PostgreSQL NOTIFY events
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CacheConfig {

    private final CacheInvalidationListener cacheInvalidationListener;

    @Bean
    RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // Listen to change_events channel (PostgreSQL NOTIFY)
        container.addMessageListener(
            new MessageListenerAdapter(cacheInvalidationListener),
            new ChannelTopic("change_events")
        );
        
        log.info("Redis message listener container configured for cache invalidation");
        return container;
    }
}
