package cz.muriel.core.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 🚀 CRITICAL: Flyway EARLY Initializer
 * 
 * Runs Flyway migrations using Spring properties BEFORE JPA entity scanning.
 * This ensures database schema exists BEFORE JPA tries to validate entities.
 */
@Configuration @Slf4j @Order(Integer.MIN_VALUE) // Run FIRST
public class FlywayInitializer {

  @Value("${spring.datasource.url}")
  private String datasourceUrl;

  @Value("${spring.datasource.username}")
  private String datasourceUsername;

  @Value("${spring.datasource.password}")
  private String datasourcePassword;

  @PostConstruct
  public void runMigrations() {
    log.info("🚀 CRITICAL: Running Flyway migrations BEFORE component scanning");
    log.info("🔧 Flyway connecting to: {} (user: {})", datasourceUrl, datasourceUsername);

    try {
      Flyway flyway = Flyway.configure()
          .dataSource(datasourceUrl, datasourceUsername, datasourcePassword)
          .locations("classpath:db/migration").baselineOnMigrate(true).baselineVersion("0").load();

      int migrationsRun = flyway.migrate().migrationsExecuted;

      log.info("✅ Flyway migrations completed: {} migrations executed", migrationsRun);

    } catch (Exception e) {
      log.error("❌ Flyway migration FAILED during early initialization", e);
      throw new RuntimeException("Flyway migration failed", e);
    }
  }
}
