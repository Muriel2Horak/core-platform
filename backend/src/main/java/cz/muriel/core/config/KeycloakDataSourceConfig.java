package cz.muriel.core.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 🔧 Keycloak DataSource Configuration
 * 
 * Samostatný DataSource pro přístup k Keycloak DB (change_events polling).
 * Používá connection pool pro efektivní polling.
 */
@Configuration @Slf4j @ConditionalOnProperty(name = "keycloak.datasource.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakDataSourceConfig {

    @Value("${keycloak.datasource.url}")
    private String jdbcUrl;

    @Value("${keycloak.datasource.username}")
    private String username;

    @Value("${keycloak.datasource.password}")
    private String password;

    @Value("${keycloak.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    public HikariConfig keycloakHikariConfig() {
        HikariConfig config = new HikariConfig();

        // 🔧 FIX: Explicitně nastavit JDBC URL a credentials
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // Pool sizing pro polling
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);

        // Pool name
        config.setPoolName("KeycloakDB-Pool");

        // Health check
        config.setConnectionTestQuery("SELECT 1");

        log.info("🔧 Configuring Keycloak DataSource pool");

        return config;
    }

    @Bean(name = "keycloakDataSource") @Qualifier("keycloakDataSource")
    public DataSource keycloakDataSource() {
        log.info("✅ Creating Keycloak DataSource for change_events polling");
        return new HikariDataSource(keycloakHikariConfig());
    }

    @Bean(name = "keycloakJdbcTemplate")
    public JdbcTemplate keycloakJdbcTemplate(
            @Qualifier("keycloakDataSource") DataSource dataSource) {
        log.info("✅ Creating Keycloak JdbcTemplate");
        return new JdbcTemplate(dataSource);
    }
}
