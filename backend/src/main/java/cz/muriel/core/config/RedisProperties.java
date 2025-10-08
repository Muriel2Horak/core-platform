package cz.muriel.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 🔴 Redis Configuration Properties
 */
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisProperties {
    private String host = "redis";
    private int port = 6379;
    private String password;
    private int timeout = 2000;
}
